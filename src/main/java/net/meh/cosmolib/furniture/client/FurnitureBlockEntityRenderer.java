package net.meh.cosmolib.furniture.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.meh.cosmolib.furniture.block.AbstractFurnitureBlock;
import net.meh.cosmolib.furniture.blockentity.FurnitureBlockEntity;
import net.meh.cosmolib.paint.PaintData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders static furniture by drawing the block's own item model via ItemRenderer.
 *
 * The block is laid flat on the ground (FLOOR mode), respecting the ROTATION
 * block state property. Paint color is applied via PaintData before rendering.
 *
 * No GeckoLib required — animated furniture uses AnimatedFurnitureBlockEntityRenderer.
 */
public class FurnitureBlockEntityRenderer implements BlockEntityRenderer<FurnitureBlockEntity> {

    public FurnitureBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(FurnitureBlockEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (entity.getLevel() == null) return;

        BlockState state = entity.getBlockState();
        ItemStack stack = new ItemStack(state.getBlock().asItem());
        if (stack.isEmpty()) return;

        int paintColor = entity.getPaintColor();
        if (paintColor >= 0) PaintData.applyColor(stack, paintColor);

        poseStack.pushPose();

        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));

        if (state.hasProperty(AbstractFurnitureBlock.ROTATION)) {
            int rot = state.getValue(AbstractFurnitureBlock.ROTATION);
            poseStack.mulPose(Axis.ZP.rotationDegrees(rot * 45.0f));
        }

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                entity.getLevel(),
                0
        );

        poseStack.popPose();
    }
}
