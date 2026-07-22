package net.meh.cosmolib.furniture.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.meh.cosmolib.furniture.block.AbstractFurnitureBlock;
import net.meh.cosmolib.furniture.block.FurnitureDisplayMode;
import net.meh.cosmolib.furniture.block.FurnitureDisplayModeProvider;
import net.meh.cosmolib.furniture.blockentity.FurnitureBlockEntity;
import net.meh.cosmolib.paint.PaintData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

/**
 * Renders static furniture items using {@link ItemDisplayContext#FIXED}.
 *
 * <h3>Frustum culling</h3>
 * {@link #shouldRenderOffScreen} returns {@code true} so that furniture whose anchor
 * block drifts just outside the camera frustum does not pop out of view while the rest
 * of the model is still visible.  The GPU still clips truly off-screen geometry before
 * rasterisation, so there is no visual or frame-rate downside.
 *
 * <h3>ItemStack caching</h3>
 * {@link FurnitureBlockEntity#getCachedRenderStack(Item)} is used instead of allocating
 * a fresh {@link ItemStack} on every frame, saving GC pressure for furniture with a
 * static paint color.
 *
 * Display modes:
 * <pre>
 *   TOP_FACE  — translate(0.5, 0.999, 0.5) + X+180 + X+90 + Z+180 + Z(-rot*45)
 *   FLOOR     — translate(0.5, 0.0,   0.5) + X+90  + Z+180 + Z(+rot*45)
 *   BLOCK_UP  — translate(0.5, 1.0,   0.5) + X+90  + Z+180 + Z(+rot*45)
 *   WALL      — translate(0.5, 0.5,   0.5) + Y(from facing) + translate(0,0,0.45)
 * </pre>
 * A 0.5 pre-scale counteracts the typical 2× scale in model "fixed" display transforms.
 */
public class FurnitureBlockEntityRenderer implements BlockEntityRenderer<FurnitureBlockEntity> {

    public FurnitureBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    /**
     * Disables the vanilla 1×1×1 anchor-block frustum check.
     *
     * <p>Vanilla's {@code LevelRenderer} culls block entities whose single anchor block
     * sits outside the camera frustum.  For furniture models that visually extend beyond
     * that block (tall cabinets, wide sofas, multi-block pieces) this causes the model to
     * vanish while it is still clearly in view.
     *
     * <p>Returning {@code true} here tells the engine to skip that check and always
     * dispatch this renderer when the containing chunk section is loaded.  The GPU still
     * clips geometry that is truly off-screen before rasterisation, so there is no
     * rendering artefact — only the unnecessary CPU frustum-cull is removed.
     */
    @Override
    public boolean shouldRenderOffScreen(FurnitureBlockEntity entity) {
        return true;
    }

    @Override
    public void render(FurnitureBlockEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (entity.getLevel() == null) return;

        BlockState state = entity.getBlockState();

        // Skip upper half of double-tall blocks — only the lower block renders.
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return;
        }

        // Mark this BE as rendered for the current frame so the global fallback
        // pass (ClientRenderEventHandler) knows it was already drawn and can skip it.
        entity.lastRenderedClientFrame = FurnitureBlockEntity.clientFrameCounter;

        Item item = state.getBlock().asItem();

        // ------------------------------------------------------------------
        // Fast path: block has an associated item.
        // Use the cached ItemStack to avoid a fresh allocation every frame.
        // ------------------------------------------------------------------
        if (item != Items.AIR) {
            ItemStack stack = entity.getCachedRenderStack(item);
            if (!stack.isEmpty()) {
                renderNormalWithItem(state, stack, poseStack, bufferSource,
                                     packedLight, packedOverlay, null, entity);
                return;
            }
        }

        // ------------------------------------------------------------------
        // Fallback: no BlockItem (ceiling/wall variants) or override model.
        // ------------------------------------------------------------------
        renderUncached(entity, state, item, poseStack, bufferSource, packedLight, packedOverlay);
    }

    // ------------------------------------------------------------------
    // Fallback rendering (no-BlockItem / override-model path)
    // ------------------------------------------------------------------

    private void renderUncached(FurnitureBlockEntity entity, BlockState state, Item item,
                                 PoseStack poseStack, MultiBufferSource bufferSource,
                                 int packedLight, int packedOverlay) {

        BakedModel overrideModel = null;
        int paintColor = entity.getPaintColor();

        // No BlockItem — look for a dedicated "item/<name>#standalone" model.
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId != null) {
            Minecraft mc = Minecraft.getInstance();
            ResourceLocation itemModelId = ResourceLocation.fromNamespaceAndPath(
                    blockId.getNamespace(), "item/" + blockId.getPath());
            BakedModel candidate = mc.getModelManager()
                    .getModel(new ModelResourceLocation(itemModelId, "standalone"));
            if (candidate != mc.getModelManager().getMissingModel()) {
                overrideModel = candidate;
            }
        }

        ItemStack stack = state.getBlock().getCloneItemStack(entity.getLevel(), entity.getBlockPos(), state);
        if (stack.isEmpty()) return;
        if (paintColor >= 0) PaintData.applyColor(stack, paintColor);

        // Water variant override.
        if (blockId != null
                && state.hasProperty(BlockStateProperties.WATERLOGGED)
                && state.getValue(BlockStateProperties.WATERLOGGED)) {
            Minecraft mc = Minecraft.getInstance();
            BakedModel wm = mc.getModelManager().getModel(new ModelResourceLocation(
                    ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(),
                            "item/" + blockId.getPath() + "_water"), "standalone"));
            if (wm != mc.getModelManager().getMissingModel()) overrideModel = wm;
        }

        renderNormalWithItem(state, stack, poseStack, bufferSource, packedLight, packedOverlay,
                             overrideModel, entity);
    }

    /** Renders {@code stack} into the live buffer source with the correct display transform. */
    private static void renderNormalWithItem(BlockState state, ItemStack stack,
                                              PoseStack poseStack, MultiBufferSource bufferSource,
                                              int packedLight, int packedOverlay,
                                              @Nullable BakedModel overrideModel,
                                              FurnitureBlockEntity entity) {
        poseStack.pushPose();
        applyDisplayTransform(poseStack, state, stack);

        if (overrideModel != null) {
            Minecraft.getInstance().getItemRenderer().render(
                    stack, ItemDisplayContext.FIXED, false,
                    poseStack, bufferSource, packedLight, packedOverlay, overrideModel);
        } else {
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    stack, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                    poseStack, bufferSource, entity.getLevel(), 0);
        }

        poseStack.popPose();
    }

    // ------------------------------------------------------------------
    // Display transform helpers (public for use by animated renderer etc.)
    // ------------------------------------------------------------------

    /**
     * Applies the display-mode-specific pre-transforms for this block state.
     * Call inside a pushPose/popPose pair before the item render call.
     */
    public static void applyDisplayTransform(PoseStack poseStack, BlockState state) {
        applyDisplayTransform(poseStack, state, null);
    }

    /**
     * Same as {@link #applyDisplayTransform(PoseStack, BlockState)} but accepts an
     * already-resolved ItemStack (unused; kept for API compatibility).
     */
    public static void applyDisplayTransform(PoseStack poseStack, BlockState state,
                                              @Nullable ItemStack stack) {
        FurnitureDisplayMode mode = state.getBlock() instanceof FurnitureDisplayModeProvider p
                ? p.getDisplayMode(state)
                : FurnitureDisplayMode.TOP_FACE;

        float scale = 0.5f;

        switch (mode) {
            case CEILING -> {
                poseStack.translate(0.5, 1.0, 0.5);
                if (state.hasProperty(AbstractFurnitureBlock.ROTATION)) {
                    int rot = state.getValue(AbstractFurnitureBlock.ROTATION);
                    poseStack.mulPose(Axis.YP.rotationDegrees(-rot * 45.0f));
                }
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
                poseStack.scale(scale, scale, scale);
            }
            case WALL -> {
                poseStack.translate(0.5, 0.5, 0.5);
                if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                    Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                    poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
                } else if (state.hasProperty(AbstractFurnitureBlock.ROTATION)) {
                    int rot = state.getValue(AbstractFurnitureBlock.ROTATION);
                    poseStack.mulPose(Axis.YP.rotationDegrees(rot * 45.0f));
                }
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
                poseStack.translate(0, 0, 0.45);
                poseStack.scale(scale, scale, scale);
            }
            case FLOOR -> {
                poseStack.translate(0.5, 0.0, 0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
                poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
                applyRotation(poseStack, state, 1.0f);
                poseStack.scale(scale, scale, scale);
            }
            case BLOCK_UP -> {
                poseStack.translate(0.5, 1.0, 0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
                poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
                applyRotation(poseStack, state, 1.0f);
                poseStack.scale(scale, scale, scale);
            }
            default -> { // TOP_FACE
                poseStack.translate(0.5, 0.999, 0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
                poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
                applyRotation(poseStack, state, -1.0f);
                poseStack.scale(scale, scale, scale);
            }
        }
    }

    private static void applyRotation(PoseStack poseStack, BlockState state, float sign) {
        if (state.hasProperty(AbstractFurnitureBlock.ROTATION)) {
            int rot = state.getValue(AbstractFurnitureBlock.ROTATION);
            poseStack.mulPose(Axis.ZP.rotationDegrees(sign * rot * 45.0f + 180.0f));
        } else if (state.hasProperty(BlockStateProperties.ROTATION_16)) {
            int rot = state.getValue(BlockStateProperties.ROTATION_16);
            poseStack.mulPose(Axis.ZP.rotationDegrees(sign * rot * 45.0f + 180.0f));
        }
    }
}
