package net.meh.cosmolib.cosmetic.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.meh.cosmolib.cosmetic.CosmeticItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Render layer that draws a cosmetic hat on any mob whose model implements
 * {@link HeadedModel} or has a root child named {@code "head"}.  The hat
 * ItemStack is pulled from {@link MobHatClientCache} — populated by the
 * server sync packet on spawn or when the player enters tracking range.
 *
 * <p>The finish atlas is bound to Sampler3 before rendering so that the
 * entity shader's {@code finishGet()} can sample it, matching the behaviour
 * in {@link CosmeticPlayerLayer}.
 */
public class MobCosmeticHatLayer<T extends LivingEntity, M extends EntityModel<T>>
        extends RenderLayer<T, M> {

    private static final ResourceLocation FINISH_ATLAS =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "textures/misc/finish_atlas.png");

    /** Y translation applied after head.translateAndRotate() to sit the hat on top of the head. */
    private static final float HAT_Y_OFFSET = -0.25f;

    private final ItemRenderer itemRenderer;

    public MobCosmeticHatLayer(RenderLayerParent<T, M> parent, ItemRenderer itemRenderer) {
        super(parent);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int packedLight,
                       T entity,
                       float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {

        // Baby mobs have a scaled-down, offset head that makes hats render oddly.
        if (entity instanceof AgeableMob ageable && ageable.isBaby()) return;

        ItemStack hat = MobHatClientCache.getHat(entity.getId());
        if (hat.isEmpty()) return;

        Optional<ModelPart> headOpt = findHead(getParentModel());
        if (headOpt.isEmpty()) return;

        // Bind the finish atlas so atlas-dependent finishes (Rainbow, Gold, etc.)
        // can sample it from the entity shader.
        RenderSystem.setShaderTexture(3, FINISH_ATLAS);

        ps.pushPose();
        headOpt.get().translateAndRotate(ps);
        ps.translate(0.0, HAT_Y_OFFSET, 0.0);
        ps.mulPose(Axis.XP.rotationDegrees(180));
        ps.mulPose(Axis.YP.rotationDegrees(180));
        ps.scale(0.625f, 0.625f, 0.625f);

        BakedModel model = resolveModel(hat);
        itemRenderer.render(hat, ItemDisplayContext.HEAD, false, ps, buf, packedLight,
                OverlayTexture.NO_OVERLAY, model);
        ps.popPose();
    }

    /**
     * Returns the 3-D cosmetic model for {@link CosmeticItem}s (same lookup used by
     * {@link CosmeticPlayerLayer}), or falls back to the normal item model for anything else.
     */
    private BakedModel resolveModel(ItemStack stack) {
        if (stack.getItem() instanceof CosmeticItem ci && ci.getCosmeticModelId() != null) {
            return Minecraft.getInstance().getModelManager()
                    .getModel(new ModelResourceLocation(ci.getCosmeticModelId(), "standalone"));
        }
        return itemRenderer.getModel(stack, Minecraft.getInstance().level, null, 0);
    }

    /**
     * Locates the head {@link ModelPart} using the standard MC interfaces:
     * <ol>
     *   <li>{@link HeadedModel#getHead()} — the canonical approach; implemented
     *       by {@code HumanoidModel} (zombie, skeleton, piglin, creeper, …) and
     *       many other vanilla mob models.</li>
     *   <li>Fallback: {@link HierarchicalModel#root()}{@code .getChild("head")} —
     *       catches any remaining hierarchical models whose head is a direct
     *       root child but that don't implement {@code HeadedModel}.</li>
     * </ol>
     */
    private static Optional<ModelPart> findHead(EntityModel<?> model) {
        // Primary: HeadedModel is the official API for "give me the head part"
        if (model instanceof HeadedModel hm) {
            return Optional.of(hm.getHead());
        }
        // Fallback: HierarchicalModel with a root child literally named "head"
        if (model instanceof HierarchicalModel<?> hier) {
            try {
                return Optional.of(hier.root().getChild("head"));
            } catch (NoSuchElementException ignored) {
                // Model doesn't have a standard "head" child — hat not rendered.
            }
        }
        return Optional.empty();
    }
}
