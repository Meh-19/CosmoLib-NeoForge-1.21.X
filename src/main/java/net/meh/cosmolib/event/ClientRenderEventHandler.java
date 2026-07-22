package net.meh.cosmolib.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.meh.cosmolib.CosmoLib;
import net.meh.cosmolib.furniture.blockentity.FurnitureBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Handles the global furniture render pass that fixes frustum-culling pop-in.
 *
 * <h3>Why this exists</h3>
 * {@link net.meh.cosmolib.furniture.block.AbstractFurnitureBlock} uses
 * {@code RenderShape.INVISIBLE}, which in NeoForge 21.1.x causes the
 * section-compilation code to skip the normal {@code shouldRenderOffScreen} /
 * {@code getRenderBoundingBox} categorisation.  As a result, furniture block
 * entities always end up in the section-local render list instead of the global
 * one, so they stop rendering the moment their anchor block's chunk section
 * drifts outside the camera frustum.
 *
 * <h3>Solution</h3>
 * {@link FurnitureBlockEntity} self-registers into
 * {@link FurnitureBlockEntity#GLOBAL_CLIENT_RENDERS} when loaded on the client.
 * Each frame, after the vanilla BER pass, {@link #onRenderLevelStage} checks
 * every furniture BE that was <em>not</em> stamped as rendered by the vanilla
 * path, tests its <em>inflated</em> render bounding box against the actual
 * camera frustum, and only re-renders those that are genuinely visible.
 *
 * <h3>Performance</h3>
 * Three layers of culling prevent unnecessary work:
 * <ol>
 *   <li><b>Frame stamp</b> — BEs the vanilla section path already drew are
 *       skipped immediately (one int comparison).
 *   <li><b>Frustum test</b> — BEs whose inflated AABB lies entirely outside
 *       the camera frustum are skipped without touching the GPU at all.  This
 *       means looking 180° away from a dense furniture layout costs only the
 *       AABB test per BE, not a full render call.
 *   <li><b>GPU clip</b> — geometry that passes the frustum test but is
 *       partially off-screen is clipped normally by the rasteriser.
 * </ol>
 *
 * <p>This class is registered on the <em>NeoForge game event bus</em>
 * ({@code Bus.GAME}) because render events are fired there, not on the
 * mod-lifecycle bus.
 */
@EventBusSubscriber(modid = CosmoLib.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientRenderEventHandler {

    private ClientRenderEventHandler() {}

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;

        // Advance the frame counter.  FurnitureBlockEntityRenderer.render() stamps
        // this value onto each BE it draws so we can skip them below.
        FurnitureBlockEntity.clientFrameCounter++;

        if (FurnitureBlockEntity.GLOBAL_CLIENT_RENDERS.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 cam                               = event.getCamera().getPosition();
        Frustum frustum                        = event.getFrustum();
        PoseStack poseStack                    = event.getPoseStack();
        BlockEntityRenderDispatcher dispatcher = mc.getBlockEntityRenderDispatcher();
        MultiBufferSource.BufferSource buffer  = mc.renderBuffers().bufferSource();
        float partialTick                      = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        boolean drewAnything = false;
        for (FurnitureBlockEntity be : FurnitureBlockEntity.GLOBAL_CLIENT_RENDERS) {
            if (be.isRemoved() || be.getLevel() == null) continue;

            // Layer 1 — already rendered by vanilla's section path this frame.
            if (be.lastRenderedClientFrame == FurnitureBlockEntity.clientFrameCounter) continue;

            // Layer 2 — frustum test on the inflated render bounding box.
            // This is the critical optimisation: furniture that is truly off-screen
            // (e.g. player looks 180° away from a dense layout) is rejected here
            // with only an AABB test, not a full BE render call.
            AABB renderAABB = be.getRenderBoundingBox();
            if (!frustum.isVisible(renderAABB)) continue;

            // The model is (at least partially) visible — render it.
            BlockPos pos = be.getBlockPos();
            poseStack.pushPose();
            poseStack.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
            dispatcher.render(be, partialTick, poseStack, buffer);
            poseStack.popPose();
            drewAnything = true;
        }

        // Flush any geometry written by the global pass to the GPU.
        if (drewAnything) {
            buffer.endBatch();
        }
    }
}
