package net.meh.cosmolib.crate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.meh.cosmolib.crate.entity.CrateEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * GeckoLib render layer for {@link CrateEntity} that renders the three rolled items on the
 * crate's {@code item1}, {@code item2}, and {@code item3} bones, and draws a small
 * always-visible name label above each item during {@link CrateEntity#STATE_LOOP}.
 *
 * <p>Labels are rendered at fixed entity-root-relative offsets (not bone matrices) to
 * avoid jitter caused by GeckoLib's per-frame animation transforms.
 */
@OnlyIn(Dist.CLIENT)
public class CrateItemLayer extends GeoRenderLayer<CrateEntity> {

    /**
     * Fixed label positions in entity-local model space (GeckoLib units ÷ 16 = blocks).
     * Derived from the average resting positions in the {@code open_loop} animation, with
     * a small upward offset so labels float above the items without following their bob.
     *
     * <p>Layout: item1 (left), item2 (centre, slightly higher), item3 (right).
     */
    private static final float[][] LABEL_OFFSETS = {
            {-0.5f, 1.15f, -0.1875f},  // item1
            { 0.0f, 1.25f,  0.0625f},  // item2
            { 0.5f, 1.15f, -0.1875f}   // item3
    };

    private final ItemRenderer itemRenderer;

    public CrateItemLayer(GeoEntityRenderer<CrateEntity> renderer, ItemRenderer itemRenderer) {
        super(renderer);
        this.itemRenderer = itemRenderer;
    }

    // ------------------------------------------------------------------
    // Per-bone: render item
    // ------------------------------------------------------------------

    @Override
    public void renderForBone(PoseStack poseStack, CrateEntity entity, GeoBone bone,
                               RenderType renderType, MultiBufferSource bufferSource,
                               VertexConsumer buffer, float partialTick,
                               int packedLight, int packedOverlay) {
        int idx = boneIndex(bone.getName());
        if (idx < 0) return;

        int state = entity.getState();
        if (state != CrateEntity.STATE_OPENING
                && state != CrateEntity.STATE_LOOP
                && state != CrateEntity.STATE_REROLLING) {
            return;
        }

        ItemStack item = itemForIndex(entity, idx);
        if (item.isEmpty()) return;

        poseStack.pushPose();
        poseStack.scale(0.7f, 0.7f, 0.7f);
        BakedModel baked = itemRenderer.getModel(item, entity.level(), null, 0);
        itemRenderer.render(item, ItemDisplayContext.GROUND, false,
                poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY, baked);
        poseStack.popPose();
    }

    // ------------------------------------------------------------------
    // Post-render: always-visible item labels (STATE_LOOP only)
    // ------------------------------------------------------------------

    @Override
    public void render(PoseStack poseStack, CrateEntity entity, BakedGeoModel model,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        // Labels only appear when items are fully presented and floating
        if (entity.getState() != CrateEntity.STATE_LOOP) return;

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

        ItemStack[] items = {entity.getItem1(), entity.getItem2(), entity.getItem3()};
        for (int i = 0; i < 3; i++) {
            if (!items[i].isEmpty()) {
                renderLabel(poseStack, bufferSource, items[i],
                        LABEL_OFFSETS[i][0], LABEL_OFFSETS[i][1], LABEL_OFFSETS[i][2],
                        packedLight, camera);
            }
        }
    }

    // ------------------------------------------------------------------
    // Label rendering
    // ------------------------------------------------------------------

    /**
     * Renders a small billboard name label at a fixed entity-local offset.
     *
     * <p>The pose stack is at the entity root when this is called (after GeckoLib's
     * {@code applyRotations}), so translating by model-space coordinates positions
     * labels correctly regardless of entity facing or animation state — no jitter.
     */
    private static void renderLabel(PoseStack ps, MultiBufferSource buf, ItemStack item,
                                     float dx, float dy, float dz, int light, Camera camera) {
        Component label = Component.translatable(item.getDescriptionId())
                .withStyle(Style.EMPTY.withColor(0xFFFFFF));

        Font  font  = Minecraft.getInstance().font;
        float halfW = font.width(label) / 2.0f;

        ps.pushPose();
        ps.translate(dx, dy, dz);
        ps.mulPose(camera.rotation());      // billboard — always face the camera

        float scale = 0.015f;
        ps.scale(-scale, -scale, scale);

        Matrix4f m = ps.last().pose();

        VertexConsumer bg = buf.getBuffer(RenderType.textBackground());
        bg.addVertex(m, -halfW - 1.5f, -1.0f, 0.0f).setColor(0, 0, 0, 100).setLight(light);
        bg.addVertex(m, -halfW - 1.5f,  9.0f, 0.0f).setColor(0, 0, 0, 100).setLight(light);
        bg.addVertex(m,  halfW + 1.5f,  9.0f, 0.0f).setColor(0, 0, 0, 100).setLight(light);
        bg.addVertex(m,  halfW + 1.5f, -1.0f, 0.0f).setColor(0, 0, 0, 100).setLight(light);

        font.drawInBatch(label, -halfW, 0.0f, 0xFFFFFF, false, m, buf,
                Font.DisplayMode.NORMAL, 0, light);

        ps.popPose();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static int boneIndex(String boneName) {
        return switch (boneName) {
            case "item1" -> 0;
            case "item2" -> 1;
            case "item3" -> 2;
            default -> -1;
        };
    }

    private static ItemStack itemForIndex(CrateEntity entity, int idx) {
        return switch (idx) {
            case 0 -> entity.getItem1();
            case 1 -> entity.getItem2();
            case 2 -> entity.getItem3();
            default -> ItemStack.EMPTY;
        };
    }
}
