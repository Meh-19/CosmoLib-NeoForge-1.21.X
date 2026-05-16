package net.meh.cosmolib.cosmetic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.meh.cosmolib.cosmetic.CosmeticSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.item.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CosmeticPlayerLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private final ItemRenderer itemRenderer;

    public CosmeticPlayerLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
            ItemRenderer itemRenderer
    ) {
        super(parent);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(
            PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
            AbstractClientPlayer entity,
            float limbSwing, float limbSwingAmount,
            float partialTick, float ageInTicks,
            float netHeadYaw, float headPitch
    ) {
        ItemStack hat  = CosmeticClientCache.getEquipped(entity, CosmeticSlot.HAT);
        ItemStack back = CosmeticClientCache.getEquipped(entity, CosmeticSlot.BACK);
        ItemStack hand = CosmeticClientCache.getEquipped(entity, CosmeticSlot.HAND);

        if (!hat.isEmpty())  renderHat (poseStack, bufferSource, packedLight, entity, hat);
        if (!back.isEmpty()) renderBack(poseStack, bufferSource, packedLight, entity, back);
        if (!hand.isEmpty() && entity.getOffhandItem().isEmpty())
            renderHand(poseStack, bufferSource, packedLight, entity, hand);
    }

    // ------------------------------------------------------------------
    // HAT
    // ------------------------------------------------------------------
    private void renderHat(PoseStack ps, MultiBufferSource buf, int light,
                            AbstractClientPlayer entity, ItemStack stack) {
        ps.pushPose();
        getParentModel().head.translateAndRotate(ps);
        ps.translate(0.0, -0.75 + 0.625 - 0.125, 0.0);
        ps.mulPose(Axis.XP.rotationDegrees(180));
        ps.mulPose(Axis.YP.rotationDegrees(180));
        ps.scale(0.625f, 0.625f, 0.625f);
        renderItem(stack, ItemDisplayContext.HEAD, false, ps, buf, light);
        ps.popPose();
    }

    // ------------------------------------------------------------------
    // BACK
    // ------------------------------------------------------------------
    private void renderBack(PoseStack ps, MultiBufferSource buf, int light,
                             AbstractClientPlayer entity, ItemStack stack) {
        ps.pushPose();
        getParentModel().body.translateAndRotate(ps);
        ps.translate(0.0, -0.4 - 2.0 - 0.03125 - 0.0625, 0.25 - 0.1875 - 0.0625);
        ps.mulPose(Axis.XP.rotationDegrees(180));
        ps.mulPose(Axis.YP.rotationDegrees(180));
        ps.scale(0.75f, 0.75f, 0.75f);
        renderItem(stack, ItemDisplayContext.HEAD, false, ps, buf, light);
        ps.popPose();
    }

    // ------------------------------------------------------------------
    // HAND
    // ------------------------------------------------------------------
    private void renderHand(PoseStack ps, MultiBufferSource buf, int light,
                             AbstractClientPlayer entity, ItemStack stack) {
        ps.pushPose();
        boolean rightHanded = entity.getMainArm() == HumanoidArm.RIGHT;
        ModelPart arm = rightHanded ? getParentModel().leftArm : getParentModel().rightArm;
        arm.translateAndRotate(ps);
        ps.translate(0.0, 0.625, 0.0);
        float side = rightHanded ? 0.03125f : -0.03125f;
        ps.translate(side, 0.0, -0.125f);
        ps.mulPose(Axis.XP.rotationDegrees(90));
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ItemDisplayContext mode = rightHanded
                ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        renderItem(stack, mode, rightHanded, ps, buf, light);
        ps.popPose();
    }

    // ------------------------------------------------------------------
    // Shared
    // ------------------------------------------------------------------
    private void renderItem(ItemStack stack, ItemDisplayContext ctx, boolean leftHand,
                             PoseStack ps, MultiBufferSource buf, int light) {
        BakedModel model = itemRenderer.getModel(
                stack,
                Minecraft.getInstance().level,
                null, 0
        );
        itemRenderer.renderItem(stack, ctx, leftHand, ps, buf, light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, model);
    }
}
