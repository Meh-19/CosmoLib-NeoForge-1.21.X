package net.meh.cosmolib.furniture.client;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Per-block-entity pre-baked render cache for static furniture.
 *
 * <h3>How it works</h3>
 * On the first render (or after invalidation) the renderer captures all vertex writes
 * into {@link RecordingSource} — a {@link MultiBufferSource} backed by per-RenderType
 * {@link BufferBuilder}s.  The captured geometry is uploaded to per-RenderType
 * {@link VertexBuffer}s on the GPU.  Subsequent frames replay those buffers directly,
 * skipping all Java-side quad iteration and ItemRenderer traversal.
 *
 * <h3>Coordinate space</h3>
 * Geometry is baked in <em>display-transformed block-local space</em>: the recording
 * PoseStack starts at identity and only has the furniture's display transform applied
 * (translation, rotation, scale for the piece's facing/mode).  At replay time the
 * BER framework's matrix — which encodes {@code translate(blockPos − cameraPos)} —
 * is supplied as the model-view matrix, so the GPU correctly maps the baked geometry
 * to its world position for the current camera without any CPU rebake.
 *
 * <h3>Invalidation</h3>
 * <ul>
 *   <li>Paint color change — vertex tint colours are different.</li>
 *   <li>Packed light change — per-vertex lightmap UVs are different.</li>
 * </ul>
 * Both are detected via cheap int comparisons at the start of each render call.
 *
 * <h3>Thread safety</h3>
 * All methods (except {@link #scheduleClose}) must be called from the render thread.
 * {@link #scheduleClose} is safe to call from any thread and is drained from the
 * render thread via {@link #drainCloseQueue()}.
 */
public final class FurnitureBakedBuffer {

    // ------------------------------------------------------------------
    // Static close queue — render thread cleanup for removed block entities
    // ------------------------------------------------------------------

    private static final ConcurrentLinkedQueue<VertexBuffer> CLOSE_QUEUE =
            new ConcurrentLinkedQueue<>();

    /**
     * Queues a {@link VertexBuffer} for closure on the render thread.
     * Safe to call from any thread (e.g. from the main thread when a BE is removed).
     */
    public static void scheduleClose(VertexBuffer vb) {
        CLOSE_QUEUE.add(vb);
    }

    /**
     * Closes all queued {@link VertexBuffer}s.
     * Call this at the top of {@link FurnitureBlockEntityRenderer#render} — it runs
     * on the render thread, which is the only thread allowed to call OpenGL.
     */
    public static void drainCloseQueue() {
        VertexBuffer vb;
        while ((vb = CLOSE_QUEUE.poll()) != null) vb.close();
    }

    // ------------------------------------------------------------------
    // Instance state
    // ------------------------------------------------------------------

    /** Per-RenderType GPU buffers.  LinkedHashMap preserves insertion order for deterministic draws. */
    private final Map<RenderType, VertexBuffer> buffers = new LinkedHashMap<>();

    private int     lastPackedLight = Integer.MIN_VALUE;
    private int     lastPaintColor  = Integer.MIN_VALUE;
    private boolean dirty           = true;

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Returns {@code true} when the GPU buffers must be rebuilt this frame.
     * Checks only two ints when the cache is warm — negligible cost per BE.
     */
    public boolean needsRebuild(int packedLight, int paintColor) {
        return dirty || lastPackedLight != packedLight || lastPaintColor != paintColor;
    }

    /**
     * Creates a fresh {@link RecordingSource} ready to receive one frame's worth of
     * render output.  Render the furniture into it, then pass it to
     * {@link #upload(RecordingSource, int, int)}.
     */
    public static RecordingSource beginRecording() {
        return new RecordingSource();
    }

    /**
     * Uploads captured geometry to the GPU and marks the cache clean.
     *
     * <p>Disposes the {@link RecordingSource}'s CPU-side byte buffers after upload.
     *
     * @param recording   the recording source that was rendered into
     * @param packedLight packed light value used during recording
     * @param paintColor  paint color of the block entity during recording
     */
    public void upload(RecordingSource recording, int packedLight, int paintColor) {
        // Release old GPU buffers
        for (VertexBuffer vb : buffers.values()) vb.close();
        buffers.clear();

        // Upload each captured render type
        for (Map.Entry<RenderType, BufferBuilder> entry : recording.builders.entrySet()) {
            @Nullable MeshData mesh = entry.getValue().build();
            if (mesh == null) continue; // render type produced no quads — skip

            VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
            vb.bind();
            vb.upload(mesh);
            VertexBuffer.unbind();
            mesh.close();

            buffers.put(entry.getKey(), vb);
        }

        // Release CPU-side byte buffers used during recording
        for (ByteBufferBuilder bb : recording.byteBuffers.values()) bb.close();

        lastPackedLight = packedLight;
        lastPaintColor  = paintColor;
        dirty           = false;
    }

    /**
     * Returns {@code true} if at least one render type produced geometry during the last
     * upload.  A {@code false} result means the item was empty or produced no quads;
     * the caller should fall back to uncached normal rendering.
     */
    public boolean hasGeometry() {
        return !buffers.isEmpty();
    }

    /**
     * Replays each captured {@link VertexBuffer} using its original {@link RenderType}.
     *
     * <p>{@link RenderType#setupRenderState()} sets the correct shader, texture, and
     * blend state.  In MC 1.21.1 {@link VertexBuffer#draw()} takes no arguments and
     * reads the {@code ModelViewMat} uniform from the currently-bound shader.  We
     * temporarily override that uniform with the per-block combined matrix, draw, then
     * restore it so subsequent renders are unaffected.
     *
     * @param combinedMV the full model-view matrix for this block: camera-view rotation
     *                   pre-multiplied by the block-to-camera translation
     *                   ({@code RenderSystem.getModelViewMatrix() × poseStack.last().pose()})
     */
    public void draw(Matrix4f combinedMV) {
        for (Map.Entry<RenderType, VertexBuffer> entry : buffers.entrySet()) {
            RenderType rt = entry.getKey();
            VertexBuffer vb = entry.getValue();

            rt.setupRenderState();

            // Override ModelViewMat so the pre-baked block-local geometry is placed
            // at the correct world position.  VertexBuffer.draw() in 1.21.1 uses the
            // uniform rather than accepting matrices as parameters.
            ShaderInstance shader = RenderSystem.getShader();
            @Nullable Matrix4f savedMV = null;
            @Nullable Uniform mvUniform = null;
            if (shader != null) {
                mvUniform = shader.getUniform("ModelViewMat");
                if (mvUniform != null) {
                    savedMV = new Matrix4f(RenderSystem.getModelViewMatrix());
                    mvUniform.set(combinedMV);
                    mvUniform.upload();
                }
            }

            vb.bind();
            vb.draw();
            VertexBuffer.unbind();

            // Restore the global camera-only matrix so subsequent renders are correct.
            if (mvUniform != null && savedMV != null) {
                mvUniform.set(savedMV);
                mvUniform.upload();
            }

            rt.clearRenderState();
        }
    }

    /** Marks the cache dirty so it rebuilds on the next render call. */
    public void invalidate() {
        dirty = true;
    }

    /**
     * Schedules all held {@link VertexBuffer}s for render-thread closure and clears
     * the internal buffer map.  Safe to call from any thread — GPU cleanup is deferred
     * to the close queue, which is drained from the render thread.
     *
     * <p>Use this instead of {@link #close()} when calling from the main thread
     * (e.g. in a resource-reload event handler).
     */
    public void scheduleAllForClose() {
        for (VertexBuffer vb : buffers.values()) scheduleClose(vb);
        buffers.clear();
        dirty = true;
    }

    /**
     * Releases all GPU resources immediately.
     * Must be called from the render thread.
     */
    public void close() {
        for (VertexBuffer vb : buffers.values()) vb.close();
        buffers.clear();
        dirty = true;
    }

    // ------------------------------------------------------------------
    // Recording source
    // ------------------------------------------------------------------

    /**
     * A {@link MultiBufferSource} that captures all vertex writes into per-RenderType
     * {@link BufferBuilder}s backed by their own {@link ByteBufferBuilder}s.
     *
     * <p>Obtain via {@link FurnitureBakedBuffer#beginRecording()}.  After rendering,
     * pass to {@link FurnitureBakedBuffer#upload(RecordingSource, int, int)} — that
     * method handles closing the byte buffers.
     */
    public static final class RecordingSource implements MultiBufferSource {

        /** Initial byte capacity per render type.  128 KB covers most furniture models comfortably. */
        private static final int INITIAL_PER_TYPE_BYTES = 131072;

        final Map<RenderType, BufferBuilder>     builders    = new LinkedHashMap<>();
        final Map<RenderType, ByteBufferBuilder> byteBuffers = new LinkedHashMap<>();

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer getBuffer(RenderType renderType) {
            return builders.computeIfAbsent(renderType, rt -> {
                ByteBufferBuilder bb = new ByteBufferBuilder(INITIAL_PER_TYPE_BYTES);
                byteBuffers.put(rt, bb);
                // All item/block rendering in 1.21.1 uses QUADS mode.
                return new BufferBuilder(bb, VertexFormat.Mode.QUADS, rt.format());
            });
        }
    }
}
