package net.meh.cosmolib.entity.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.meh.cosmolib.cosmetic.CosmeticItem;
import net.meh.cosmolib.cosmetic.offset.BackOffsetManager;
import net.meh.cosmolib.cosmetic.offset.HandOffsetData;
import net.meh.cosmolib.cosmetic.offset.HandOffsetManager;
import net.meh.cosmolib.entity.CosmeticMannequinEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * GeckoLib entity renderer for {@link CosmeticMannequinEntity}.
 *
 * <p>After rendering the base GeckoLib model, a {@link CosmeticMannequinLayer} draws
 * the three cosmetic items attached to the mannequin's bones.
 */
public class CosmeticMannequinRenderer extends GeoEntityRenderer<CosmeticMannequinEntity> {

    public CosmeticMannequinRenderer(EntityRendererProvider.Context context) {
        super(context, new CosmeticMannequinModel());
        addRenderLayer(new CosmeticMannequinLayer(this, context.getItemRenderer()));
    }

    /**
     * Applies a sinusoidal Y-axis wobble for ~10 ticks after the mannequin is first-hit,
     * matching the feel of the vanilla armor-stand shake.
     *
     * <p>The wobble angle decays from ±8° at impact to 0° over 10 ticks.
     */
    @Override
    public void render(CosmeticMannequinEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        long lastHit = entity.getLastHitByPlayerTime();
        if (lastHit != Long.MIN_VALUE) {
            long timeSinceHit = entity.level().getGameTime() - lastHit;
            if (timeSinceHit >= 0 && timeSinceHit < 10L) {
                float t = (timeSinceHit + partialTick) / 10.0f;          // 0.0 → 1.0
                float wobble = (float)(Math.sin(t * Math.PI * 3) * (1.0f - t) * 8.0f);
                poseStack.mulPose(Axis.YP.rotationDegrees(wobble));
            }
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldShowName(CosmeticMannequinEntity entity) {
        return false;
    }

    // ------------------------------------------------------------------
    // GeoModel
    // ------------------------------------------------------------------

    public static class CosmeticMannequinModel extends GeoModel<CosmeticMannequinEntity> {

        private static final ResourceLocation MODEL =
                ResourceLocation.fromNamespaceAndPath("cosmolib", "geo/entity/mannequin.geo.json");
        private static final ResourceLocation TEXTURE =
                ResourceLocation.fromNamespaceAndPath("cosmolib", "textures/entity/mannequin.png");
        private static final ResourceLocation ANIMATIONS =
                ResourceLocation.fromNamespaceAndPath("cosmolib", "animations/entity/mannequin.animation.json");

        @Override
        public ResourceLocation getModelResource(CosmeticMannequinEntity entity) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(CosmeticMannequinEntity entity) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(CosmeticMannequinEntity entity) {
            return ANIMATIONS;
        }
    }

    // ------------------------------------------------------------------
    // Cosmetic render layer
    // ------------------------------------------------------------------

    /**
     * Renders the hat, back, and hand cosmetics anchored to the mannequin's GeckoLib bones.
     *
     * <p>Uses {@link #renderForBone} so the poseStack already contains the entity's body
     * rotation when we draw each cosmetic — meaning cosmetics rotate correctly with the
     * mannequin at any facing angle.
     */
    public static class CosmeticMannequinLayer extends GeoRenderLayer<CosmeticMannequinEntity> {

        /** Finish atlas — bound to Sampler3 before any finish-enabled cosmetic is rendered. */
        private static final ResourceLocation FINISH_ATLAS =
                ResourceLocation.fromNamespaceAndPath("cosmolib", "textures/misc/finish_atlas.png");

        private final ItemRenderer itemRenderer;

        public CosmeticMannequinLayer(GeoEntityRenderer<CosmeticMannequinEntity> renderer,
                                      ItemRenderer itemRenderer) {
            super(renderer);
            this.itemRenderer = itemRenderer;
        }

        // render() is intentionally empty — all cosmetic rendering happens in renderForBone()

        /**
         * Called once per bone while GeckoLib draws the model. The poseStack is already
         * positioned at the bone's world-space origin (entity translation + body rotation +
         * bone-local transform), so cosmetics placed here will rotate with the mannequin.
         */
        @Override
        public void renderForBone(PoseStack poseStack, CosmeticMannequinEntity entity,
                                  GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer,
                                  float partialTick, int packedLight, int packedOverlay) {

            switch (bone.getName()) {
                case "head" -> {
                    ItemStack hat = entity.getHatItem();
                    if (!hat.isEmpty()) {
                        RenderSystem.setShaderTexture(3, FINISH_ATLAS);
                        renderHat(poseStack, bufferSource, hat, packedLight);
                    }
                }
                case "body" -> {
                    ItemStack back = entity.getBackItem();
                    if (!back.isEmpty()) {
                        RenderSystem.setShaderTexture(3, FINISH_ATLAS);
                        renderBack(poseStack, bufferSource, entity, back, packedLight);
                    }
                }
                case "right_arm" -> {
                    ItemStack handRight = entity.getHandItem();
                    if (!handRight.isEmpty()) {
                        RenderSystem.setShaderTexture(3, FINISH_ATLAS);
                        renderHand(poseStack, bufferSource, handRight, packedLight);
                    }
                }
                case "left_arm" -> {
                    ItemStack handLeft = entity.getHandLeftItem();
                    if (!handLeft.isEmpty()) {
                        RenderSystem.setShaderTexture(3, FINISH_ATLAS);
                        renderHandLeft(poseStack, bufferSource, handLeft, packedLight);
                    }
                }
            }
        }

        // ------------------------------------------------------------------
        // HAT — poseStack is at the "head" bone pivot in world space.
        // The placement offset (+4 steps / +180°) means GeckoLib now applies
        // 0° at the entity level instead of the old 180°.  The compensating
        // YP.180 that was previously here is no longer needed.
        // ------------------------------------------------------------------
        private void renderHat(PoseStack ps, MultiBufferSource buf, ItemStack stack, int light) {
            ps.pushPose();
            ps.translate(0.0, 0.75 - 0.625 + 0.125 + 22.0 / 16.0, 0.0);
            ps.scale(0.625f, 0.625f, 0.625f);
            renderItem(stack, ItemDisplayContext.HEAD, false, ps, buf, light);
            ps.popPose();
        }

        // ------------------------------------------------------------------
        // BACK — poseStack is at the "body" bone pivot in world space.
        // Same poseStack-orientation change as HAT — YP.180 removed.
        // ------------------------------------------------------------------
        private void renderBack(PoseStack ps, MultiBufferSource buf,
                                 CosmeticMannequinEntity entity, ItemStack stack, int light) {
            ps.pushPose();
            double yOff = BackOffsetManager.getY(
                    BuiltInRegistries.ITEM.getKey(stack.getItem()));
            ps.translate(0.0, -yOff + 22.0 / 16.0, -(0.25 - 0.1875 - 0.0625));
            ps.scale(0.625f, 0.625f, 0.625f);
            renderItem(stack, ItemDisplayContext.HEAD, false, ps, buf, light);
            ps.popPose();
        }

        // ------------------------------------------------------------------
        // HAND (right) — poseStack is at the "right_arm" bone pivot, already
        // including the bone rotation [10, 180, 10] that GeckoLib now propagates
        // automatically (rotation is on the bone, not the cube).
        //
        // The Y=180° component of the bone rotation flips local +X → world -X AND
        // local +Z → world -Z.  Vanilla's right-arm item frame is (-1,-1,+1) in
        // world axes (entity scale(-1,-1,1), no extra arm scale).  Matching that:
        //   local_X = -world_X  ✓ (already from Y=180°, no X scale needed)
        //   local_Y = +world_Y  → need scale(1,-1,…) to flip Y-down
        //   local_Z = -world_Z  → need scale(…,…,-1) to un-flip Z back to +world_Z
        // Resulting scale(1,-1,-1): det=+1, handedness preserved, leftHand=false
        // so ItemTransform.apply does not double-mirror translation.x / rotation.z.
        // ------------------------------------------------------------------
        private void renderHand(PoseStack ps, MultiBufferSource buf, ItemStack stack, int light) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            HandOffsetData off = HandOffsetManager.getRight(id);
            ps.pushPose();
            ps.translate(off.x, off.y, off.z);
            applyRotation(ps, off);
            // Right arm bone has rotation [10, 180, 10]: Y=180° flips local X and Z.
            // scale(1,-1,-1) un-flips Z and flips Y → matches vanilla right-arm frame.
            ps.scale(1, -1, -1);
            renderItem(stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, ps, buf, light);
            ps.popPose();
        }

        // ------------------------------------------------------------------
        // HAND (left) — bone rotation [-10, 0, -10] propagated by GeckoLib.
        // ------------------------------------------------------------------
        private void renderHandLeft(PoseStack ps, MultiBufferSource buf, ItemStack stack, int light) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            HandOffsetData off = HandOffsetManager.getLeft(id);
            ps.pushPose();
            // Offset in GeckoLib Y-up bone-local space (tunable via HandTunerItem).
            // Default Y=0.425 compensates for mannequin arm pivot being ~0.425 blocks
            // lower than a vanilla player's arm pivot.
            ps.translate(off.x, off.y, off.z);
            applyRotation(ps, off);
            // Vanilla's left-arm item frame = (-1,-1,+1): scale(-1,-1,1) matches that.
            // leftHand=false so ItemTransform.apply doesn't double-mirror the authored values.
            ps.scale(-1, -1, 1);
            renderItem(stack, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, false, ps, buf, light);
            ps.popPose();
        }

        /** Applies rotX → rotY → rotZ rotations from {@link HandOffsetData}, in degrees. */
        private static void applyRotation(PoseStack ps, HandOffsetData off) {
            if (off.rotX != 0) ps.mulPose(Axis.XP.rotationDegrees((float) off.rotX));
            if (off.rotY != 0) ps.mulPose(Axis.YP.rotationDegrees((float) off.rotY));
            if (off.rotZ != 0) ps.mulPose(Axis.ZP.rotationDegrees((float) off.rotZ));
        }

        // ------------------------------------------------------------------
        // Shared
        // ------------------------------------------------------------------

        /**
         * Renders {@code stack} at the current pose-stack position.
         *
         * <p>If the item is a {@link CosmeticItem} with a registered 3-D model, that
         * model is looked up directly from the model manager so the mannequin always
         * shows the cosmetic geometry — even though the item's registered model now
         * points to the flat token sprite used in the inventory / on the ground.
         */
        private void renderItem(ItemStack stack, ItemDisplayContext ctx, boolean leftHand,
                                 PoseStack ps, MultiBufferSource buf, int light) {
            BakedModel model;
            if (stack.getItem() instanceof CosmeticItem ci && ci.getCosmeticModelId() != null) {
                model = Minecraft.getInstance().getModelManager()
                        .getModel(new ModelResourceLocation(ci.getCosmeticModelId(), "standalone"));
            } else {
                model = itemRenderer.getModel(stack, Minecraft.getInstance().level, null, 0);
            }
            itemRenderer.render(stack, ctx, leftHand, ps, buf, light,
                    OverlayTexture.NO_OVERLAY, model);
        }
    }
}
