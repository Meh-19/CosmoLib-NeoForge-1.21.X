package net.meh.cosmolib.crate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.meh.cosmolib.crate.CrateRegistry;
import net.meh.cosmolib.crate.CrateType;
import net.meh.cosmolib.crate.entity.CrateEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib entity renderer for {@link CrateEntity}.
 *
 * <p>Uses a single {@link DynamicCrateModel} instance that resolves model, texture and
 * animation file paths from the entity's {@link CrateType} at render time. GeckoLib
 * caches loaded models by {@link ResourceLocation}, so different crate types are handled
 * as separate cached models automatically.
 *
 * <p>A {@link CrateItemLayer} is added to draw the three rolled items on the crate's
 * {@code item1}/{@code item2}/{@code item3} bones, along with hover name tags.
 */
@OnlyIn(Dist.CLIENT)
public class CrateRenderer extends GeoEntityRenderer<CrateEntity> {

    public CrateRenderer(EntityRendererProvider.Context context) {
        super(context, new DynamicCrateModel());
        addRenderLayer(new CrateItemLayer(this, context.getItemRenderer()));
    }

    /**
     * GeckoLib reads {@code LivingEntity.yBodyRot} for the entity yaw passed to
     * {@link #applyRotations}. {@link CrateEntity} is a plain {@link net.minecraft.world.entity.Entity},
     * so that field is absent and GeckoLib falls back to {@code 0}, making the crate always
     * face south. We override to supply the correct interpolated yaw directly.
     *
     * <p>GeckoLib applies {@code Axis.YP.rotationDegrees(180 - entityYaw)} internally, so we
     * replicate that formula using the entity's actual rotation.
     */
    @Override
    protected void applyRotations(CrateEntity entity, PoseStack poseStack,
                                   float p1, float p2, float p3) {
        // Read the entity's yaw directly — crate yaw only changes at placement so
        // yRotO == yRot always and the parameter order ambiguity is irrelevant.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entity.getYRot()));
    }

    // ------------------------------------------------------------------
    // Dynamic GeoModel — resolves resources from CrateType
    // ------------------------------------------------------------------

    /**
     * GeckoLib model whose resource locations are determined by the entity's crate type.
     *
     * <p>GeckoLib caches baked models by the returned {@link ResourceLocation}s, so distinct
     * crate types that return different paths each get their own cached baked model — no
     * manual per-type model instances are needed.
     */
    @SuppressWarnings("removal")
    private static final class DynamicCrateModel extends GeoModel<CrateEntity> {

        @Override
        public ResourceLocation getModelResource(CrateEntity entity) {
            return safeGet(entity).getModelLocation();
        }

        @Override
        public ResourceLocation getTextureResource(CrateEntity entity) {
            return safeGet(entity).getTextureLocation();
        }

        @Override
        public ResourceLocation getAnimationResource(CrateEntity entity) {
            return safeGet(entity).getAnimationLocation();
        }

        private static CrateType safeGet(CrateEntity entity) {
            try {
                return CrateRegistry.get(entity.getCrateId());
            } catch (IllegalArgumentException e) {
                // Fallback to a missing crate type — avoids NPE during rendering
                // if the crate ID was registered on the server but not the client.
                return CrateRegistry.all().iterator().next();
            }
        }
    }
}
