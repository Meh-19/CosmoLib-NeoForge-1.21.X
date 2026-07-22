package net.meh.cosmolib.furniture.tool.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.meh.cosmolib.furniture.block.AbstractFurnitureBlock;
import net.meh.cosmolib.furniture.layout.MultiBlockLayout;
import net.meh.cosmolib.furniture.layout.MultiBlockLayoutManager;
import net.meh.cosmolib.registry.CosmoLibItems;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Renders wireframe overlays for the {@link net.meh.cosmolib.furniture.tool.BoundingBoxSelectorItem}.
 *
 * <h3>When a session is active</h3>
 * <ul>
 *   <li><b>Green</b> — every selected position</li>
 *   <li><b>Yellow</b> — the confirmed anchor position</li>
 *   <li><b>White</b> — direction arrow 1 block in front of the furniture's facing</li>
 * </ul>
 *
 * <h3>When hovering over a furniture block with an existing layout (no active session)</h3>
 * <ul>
 *   <li><b>Blue</b> — existing layout positions (preview)</li>
 *   <li><b>Blue arrow</b> — facing direction</li>
 * </ul>
 *
 * <p>Registered to {@link RenderLevelStageEvent.Stage#AFTER_TRANSLUCENT_BLOCKS} on the
 * client game bus.  Only fires when the local player holds a
 * {@link net.meh.cosmolib.registry.CosmoLibItems#BOUNDING_BOX_SELECTOR}.
 */
public final class BoundingBoxRenderer {

    // 45° step direction vectors (x, z) — index = rotation value 0-7
    private static final double[][] ROTATION_DIRS = {
        { 0, -1},  // 0: North  (-Z)
        { 1, -1},  // 1: NE
        { 1,  0},  // 2: East   (+X)
        { 1,  1},  // 3: SE
        { 0,  1},  // 4: South  (+Z)
        {-1,  1},  // 5: SW
        {-1,  0},  // 6: West   (-X)
        {-1, -1},  // 7: NW
    };

    private BoundingBoxRenderer() {}

    // ------------------------------------------------------------------
    // Main render entry-point
    // ------------------------------------------------------------------

    /**
     * Called from the {@link RenderLevelStageEvent} subscriber.
     * All drawing logic is contained here.
     */
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Only draw when holding the tool
        if (!mc.player.getMainHandItem().is(CosmoLibItems.BOUNDING_BOX_SELECTOR.get())
                && !mc.player.getOffhandItem().is(CosmoLibItems.BOUNDING_BOX_SELECTOR.get())) {
            return;
        }

        Camera camera    = event.getCamera();
        PoseStack ps     = event.getPoseStack();
        Vec3  camPos     = camera.getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        ps.pushPose();
        ps.translate(-camPos.x, -camPos.y, -camPos.z); // shift to world-space origin

        if (BoundingBoxClientState.sessionActive) {
            renderActiveSession(ps, lines, camPos);
        } else {
            renderHoveredPreview(ps, lines, mc);
        }

        ps.popPose();
        buffers.endBatch(RenderType.lines());
    }

    // ------------------------------------------------------------------
    // Active-session rendering
    // ------------------------------------------------------------------

    private static void renderActiveSession(PoseStack ps, VertexConsumer lines, Vec3 camPos) {
        // Selected positions — color by type
        for (BlockPos worldPos : BoundingBoxClientState.selectedPositions) {
            if (worldPos.equals(BoundingBoxClientState.anchorPos)) continue; // drawn separately
            drawTypedBox(ps, lines, worldPos);
        }

        // Anchor — yellow outline, with type-specific inner box
        BlockPos anchor = BoundingBoxClientState.anchorPos;
        if (anchor != null) {
            drawBox(ps, lines, anchor, 1f, 1f, 0f); // yellow anchor outline always
            if (BoundingBoxClientState.slabPositions.contains(anchor)) {
                drawSlabBox(ps, lines, anchor, 1f, 0.55f, 0f);
            } else if (BoundingBoxClientState.seatPositions.contains(anchor)) {
                drawBox(ps, lines, anchor, 1f, 0f, 1f); // magenta overlay
            }

            // Facing arrow — color reflects current mode: white=FULL, orange=SLAB, magenta=SEAT
            float[] arrowColor = arrowColorForMode(BoundingBoxClientState.selectionMode);
            drawFacingArrow(ps, lines,
                    anchor.getX() + 0.5, anchor.getY() + 0.5, anchor.getZ() + 0.5,
                    BoundingBoxClientState.furnitureRotation,
                    arrowColor[0], arrowColor[1], arrowColor[2]);
        }
    }

    /** Draws a box whose color/shape matches the position's type (slab/seat/full). */
    private static void drawTypedBox(PoseStack ps, VertexConsumer lines, BlockPos pos) {
        if (BoundingBoxClientState.slabPositions.contains(pos)) {
            drawSlabBox(ps, lines, pos, 1f, 0.55f, 0f); // orange
        } else if (BoundingBoxClientState.seatPositions.contains(pos)) {
            drawBox(ps, lines, pos, 1f, 0f, 1f);         // magenta
        } else {
            drawBox(ps, lines, pos, 0f, 1f, 0f);          // green
        }
    }

    /** Returns [r,g,b] for the facing arrow depending on the active selection mode ordinal. */
    private static float[] arrowColorForMode(int mode) {
        return switch (mode) {
            case 1  -> new float[]{1f, 0.55f, 0f}; // SLAB — orange
            case 2  -> new float[]{1f, 0f,    1f}; // SEAT — magenta
            default -> new float[]{1f, 1f,    1f}; // FULL — white
        };
    }

    // ------------------------------------------------------------------
    // Hovered-furniture preview rendering
    // ------------------------------------------------------------------

    private static void renderHoveredPreview(PoseStack ps, VertexConsumer lines, Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos hoveredPos = ((BlockHitResult) hit).getBlockPos();
        BlockState hoveredState = mc.level.getBlockState(hoveredPos);
        if (!(hoveredState.getBlock() instanceof AbstractFurnitureBlock afb)) return;

        String fid = afb.getRegistryName();
        Optional<MultiBlockLayout> layoutOpt = MultiBlockLayoutManager.get(fid);
        if (layoutOpt.isEmpty()) return;

        int rotation = hoveredState.getValue(AbstractFurnitureBlock.ROTATION);
        MultiBlockLayout layout = layoutOpt.get();
        List<BlockPos> positions = layout.getRotatedPositions(rotation);
        List<BlockPos> slabPosRel = layout.getSlabPositions(rotation);
        List<BlockPos> seatPosRel = layout.getSeatingPositions(rotation);

        for (BlockPos rel : positions) {
            BlockPos worldPos = hoveredPos.offset(rel);
            if (slabPosRel.contains(rel)) {
                drawSlabBox(ps, lines, worldPos, 0f, 0.39f, 1f);    // blue slab
            } else if (seatPosRel.contains(rel)) {
                drawBox(ps, lines, worldPos, 0.55f, 0f, 0.8f);       // purple seat
            } else {
                drawBox(ps, lines, worldPos, 0f, 0.39f, 1f);          // blue full
            }
        }

        // Facing arrow — blue
        drawFacingArrow(ps, lines,
                hoveredPos.getX() + 0.5, hoveredPos.getY() + 0.5, hoveredPos.getZ() + 0.5,
                rotation, 0f, 0.39f, 1f);
    }

    // ------------------------------------------------------------------
    // Primitive drawing helpers
    // ------------------------------------------------------------------

    /** Draws a full-block wireframe box at the given block position. */
    private static void drawBox(PoseStack ps, VertexConsumer lines, BlockPos pos,
                                  float r, float g, float b) {
        AABB aabb = new AABB(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
        ).inflate(0.002); // tiny outset so it doesn't z-fight with block faces
        LevelRenderer.renderLineBox(ps, lines, aabb, r, g, b, 1.0f);
    }

    /** Draws a half-height (slab) wireframe box at the given block position. */
    private static void drawSlabBox(PoseStack ps, VertexConsumer lines, BlockPos pos,
                                     float r, float g, float b) {
        AABB aabb = new AABB(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 0.5, pos.getZ() + 1
        ).inflate(0.002);
        LevelRenderer.renderLineBox(ps, lines, aabb, r, g, b, 1.0f);
    }

    /**
     * Draws a small direction-indicator box 1.5 blocks in front of the given world
     * centre, along the facing vector for the given rotation (0–7).
     */
    private static void drawFacingArrow(PoseStack ps, VertexConsumer lines,
                                         double cx, double cy, double cz,
                                         int rotation,
                                         float r, float g, float b) {
        if (rotation < 0 || rotation >= ROTATION_DIRS.length) return;
        double dx = ROTATION_DIRS[rotation][0];
        double dz = ROTATION_DIRS[rotation][1];
        // Normalize to length 1 (diagonal entries have length √2 already, so normalize)
        double len = Math.sqrt(dx * dx + dz * dz);
        dx /= len; dz /= len;

        double tipX = cx + dx * 1.5;
        double tipZ = cz + dz * 1.5;
        double half = 0.12;

        AABB arrowBox = new AABB(tipX - half, cy - half, tipZ - half,
                                  tipX + half, cy + half, tipZ + half);
        LevelRenderer.renderLineBox(ps, lines, arrowBox, r, g, b, 1.0f);
    }
}
