package net.meh.cosmolib.furniture.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.meh.cosmolib.furniture.block.AbstractFurnitureBlock;
import net.meh.cosmolib.furniture.blockentity.AnimatedFurnitureBlockEntity;
import net.meh.cosmolib.furniture.blockentity.FurnitureBlockEntity;
import net.meh.cosmolib.furniture.client.model.IdBasedFurnitureModel;
import net.meh.cosmolib.paint.PaintFinish;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * GeckoLib renderer for animated furniture.
 *
 * <h3>Rotation</h3>
 * Reads the {@link AbstractFurnitureBlock#ROTATION} block-state property and
 * rotates the model around the block's horizontal centre before GeckoLib renders
 * it.  rot=4 → south (default facing), increasing clockwise.
 *
 * <h3>Paint tinting</h3>
 * Only bones whose name contains {@code _paintable} receive the furniture's paint
 * colour.  All other bones are left at full white so only the intended parts of
 * the model change colour.  Both solid colours and animated finishes are supported.
 *
 * <h3>Finish effects</h3>
 * The finish atlas is bound to Sampler3 before each render call so the
 * {@code rendertype_entity_cutout_no_cull} fragment shader can sample it.
 *
 * <p>Static furniture uses {@link FurnitureBlockEntityRenderer} instead.
 */
public class AnimatedFurnitureBlockEntityRenderer extends GeoBlockRenderer<AnimatedFurnitureBlockEntity> {

    private static final ResourceLocation FINISH_ATLAS =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "textures/misc/finish_atlas.png");

    public AnimatedFurnitureBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        super(new IdBasedFurnitureModel<>());
    }

    // ------------------------------------------------------------------
    // Top-level render — rotation + finish atlas binding
    // ------------------------------------------------------------------

    @Override
    public void render(AnimatedFurnitureBlockEntity tile, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        // Stamp the frame counter so the global fallback pass (ClientRenderEventHandler)
        // knows this BE was already rendered and can skip it.
        tile.lastRenderedClientFrame = FurnitureBlockEntity.clientFrameCounter;

        // Bind the finish atlas to Sampler3 so the fragment shader's finishGet()
        // can sample it.  Harmless when no finish is active (finish=0 → sampler
        // is never read by the shader).
        RenderSystem.setShaderTexture(3, FINISH_ATLAS);

        poseStack.pushPose();
        applyRotation(poseStack, tile.getBlockState());
        super.render(tile, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    // ------------------------------------------------------------------
    // Per-bone paint tinting
    // ------------------------------------------------------------------

    /**
     * Applies the furniture's paint colour only to bones whose name contains
     * {@code _paintable}.  All other bones receive 0xFFFFFFFF (no tint).
     *
     * <p>Passing the magic ARGB for a finish colour lets the vertex shader detect
     * the finish ID and hand off to the fragment-shader effects.
     */
    @Override
    public void renderRecursively(PoseStack poseStack, AnimatedFurnitureBlockEntity animatable,
                                  GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        int boneColour;
        if (bone.getName().contains("_tintable")) {
            int paint = animatable.getPaintColor();
            boneColour = paint >= 0 ? PaintFinish.getArgbTintFromRaw(paint) : 0xFFFFFFFF;
        } else {
            boneColour = 0xFFFFFFFF;
        }
        super.renderRecursively(poseStack, animatable, bone, renderType,
                bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, boneColour);
    }

    // ------------------------------------------------------------------
    // Rotation helper
    // ------------------------------------------------------------------

    private static void applyRotation(PoseStack poseStack, BlockState state) {
        if (!state.hasProperty(AbstractFurnitureBlock.ROTATION)) return;
        int rot   = state.getValue(AbstractFurnitureBlock.ROTATION);
        float deg = (4 - rot) * 45.0f;
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(deg));
        poseStack.translate(-0.5, 0.0, -0.5);
    }
}
