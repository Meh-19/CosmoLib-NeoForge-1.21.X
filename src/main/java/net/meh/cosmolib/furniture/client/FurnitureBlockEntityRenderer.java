package net.meh.cosmolib.furniture.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.meh.cosmolib.furniture.blockentity.FurnitureBlockEntity;
import net.meh.cosmolib.furniture.client.model.IdBasedFurnitureModel;
import net.meh.cosmolib.paint.PaintFinish;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Renderer for all CosmoLib furniture block entities.
 *
 * Uses {@link IdBasedFurnitureModel} to auto-resolve the GeckoLib geo, texture,
 * and animation files from the block's registry ID.
 *
 * Paint tinting is applied via {@link #getRenderColor}: the magic ARGB value
 * from {@link PaintFinish} triggers the finish shader if present, otherwise it
 * applies a solid colour tint.
 *
 * Register this for your block entity type in client setup:
 * <pre>{@code
 * event.registerBlockEntityRenderer(MY_FURNITURE_BE.get(),
 *     ctx -> new FurnitureBlockEntityRenderer(ctx));
 * }</pre>
 */
public class FurnitureBlockEntityRenderer extends GeoBlockRenderer<FurnitureBlockEntity> {

    public FurnitureBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        super(new IdBasedFurnitureModel<>());
    }

    @Override
    public int getRenderColor(FurnitureBlockEntity animatable, float partialTick, int packedLight) {
        int color = animatable.getPaintColor();
        if (color >= 0) {
            return PaintFinish.getArgbTintFromRaw(color);
        }
        return 0xFFFFFFFF;
    }

    @Override
    public void render(FurnitureBlockEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Apply block rotation from the ROTATION block state property
        poseStack.pushPose();
        applyBlockRotation(entity, poseStack);
        super.render(entity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void applyBlockRotation(FurnitureBlockEntity entity, PoseStack poseStack) {
        net.minecraft.world.level.block.state.BlockState state = entity.getBlockState();
        if (!state.hasProperty(net.meh.cosmolib.furniture.block.AbstractFurnitureBlock.ROTATION)) return;

        int rot = state.getValue(net.meh.cosmolib.furniture.block.AbstractFurnitureBlock.ROTATION);
        float degrees = rot * 45.0f;

        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-degrees));
        poseStack.translate(-0.5, 0.0, -0.5);
    }
}
