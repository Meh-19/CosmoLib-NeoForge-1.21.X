package net.meh.cosmolib.furniture.client;

import net.meh.cosmolib.furniture.blockentity.AnimatedFurnitureBlockEntity;
import net.meh.cosmolib.furniture.client.model.IdBasedFurnitureModel;
import net.meh.cosmolib.paint.PaintFinish;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.util.Color;

/**
 * GeckoLib renderer for animated furniture only.
 * Static furniture uses {@link FurnitureBlockEntityRenderer} instead.
 */
public class AnimatedFurnitureBlockEntityRenderer extends GeoBlockRenderer<AnimatedFurnitureBlockEntity> {

    public AnimatedFurnitureBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        super(new IdBasedFurnitureModel<>());
    }

    @Override
    public Color getRenderColor(AnimatedFurnitureBlockEntity animatable, float partialTick, int packedLight) {
        int color = animatable.getPaintColor();
        return color >= 0 ? new Color(PaintFinish.getArgbTintFromRaw(color)) : Color.WHITE;
    }
}
