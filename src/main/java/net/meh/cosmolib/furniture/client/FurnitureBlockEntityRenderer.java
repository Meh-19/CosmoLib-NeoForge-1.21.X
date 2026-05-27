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
 * Renders static furniture items using ItemDisplayContext.FIXED, mirroring
 * Argon's ArtifactDisplayBlockEntityRenderer exactly.
 *
 * Display mode is driven by FurnitureDisplayModeProvider (block implements it);
 * blocks that don't implement it fall back to TOP_FACE.
 *
 *   TOP_FACE  — translate(0.5, 0.999, 0.5) + X+180 + X+90 + Z+180 + Z(-rot*45)
 *   FLOOR     — translate(0.5, 0.0,   0.5) + X+90  + Z+180 + Z(+rot*45)
 *   BLOCK_UP  — translate(0.5, 1.0,   0.5) + X+90  + Z+180 + Z(+rot*45)
 *   WALL      — translate(0.5, 0.5,   0.5) + Y(from facing) + translate(0,0,0.45)
 *
 * A 0.5 pre-scale counteracts the typical 2x scale in model "fixed" display transforms.
 */
public class FurnitureBlockEntityRenderer implements BlockEntityRenderer<FurnitureBlockEntity> {

    public FurnitureBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

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

        // Resolve item and model. Ceiling/wall variants have no BlockItem of their own,
        // so we try to find their "item/<name>#standalone" model first. If found
        // (registered via ModelEvent.RegisterAdditional by the dependent mod) we render
        // that model directly while still using the floor item's stack for paint color
        // data. This lets mods provide a dedicated ceiling model without a separate Item.
        Item item = state.getBlock().asItem();
        ItemStack stack;
        BakedModel overrideModel = null;
        int paintColor = entity.getPaintColor();

        if (item != Items.AIR) {
            stack = new ItemStack(item);
            if (paintColor >= 0) PaintData.applyColor(stack, paintColor);
        } else {
            // No item for this block — check whether a dedicated model was registered as
            // "item/<name>#standalone" via ModelEvent.RegisterAdditional.
            // The full "item/" prefix is needed because that's how NeoForge resolves
            // the model file: assets/<ns>/models/item/<name>.json.
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
            // Fall back to the floor item (gives paint color + drop-as item).
            stack = state.getBlock().getCloneItemStack(entity.getLevel(), entity.getBlockPos(), state);
            if (stack.isEmpty()) return;
            // Apply paint color from the block entity onto the fallback stack.
            if (paintColor >= 0) PaintData.applyColor(stack, paintColor);
        }
        if (stack.isEmpty()) return;

        // Water variant: if the block is waterlogged and a <name>_water standalone model
        // exists (registered via ModelEvent.RegisterAdditional), switch to it.
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)
                && state.getValue(BlockStateProperties.WATERLOGGED)) {
            ResourceLocation bid = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (bid != null) {
                Minecraft mc = Minecraft.getInstance();
                BakedModel wm = mc.getModelManager().getModel(new ModelResourceLocation(
                        ResourceLocation.fromNamespaceAndPath(bid.getNamespace(), "item/" + bid.getPath() + "_water"),
                        "standalone"));
                if (wm != mc.getModelManager().getMissingModel()) overrideModel = wm;
            }
        }

        poseStack.pushPose();
        applyDisplayTransform(poseStack, state, stack);

        if (overrideModel != null) {
            // All display modes (including CEILING) use FIXED context with a 0.5 pre-scale
            // so ceiling models appear at the same visual size as floor furniture.
            // applyDisplayTransform already handled position and Y-rotation; FIXED then
            // applies the model's display.fixed scale (typically 2×), and the 0.5 pre-scale
            // in applyDisplayTransform brings the net result back to 1×.
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

    /**
     * Applies the display-mode-specific pre-transforms for this block state.
     * Call this inside a pushPose/popPose pair before the item render call.
     * Matches Argon's ArtifactDisplayBlockEntityRenderer transform logic exactly.
     */
    public static void applyDisplayTransform(PoseStack poseStack, BlockState state) {
        applyDisplayTransform(poseStack, state, null);
    }

    /**
     * Same as {@link #applyDisplayTransform(PoseStack, BlockState)} but accepts an
     * already-resolved ItemStack (unused; kept for API compatibility).
     */
    public static void applyDisplayTransform(PoseStack poseStack, BlockState state, @Nullable ItemStack stack) {
        FurnitureDisplayMode mode = state.getBlock() instanceof FurnitureDisplayModeProvider p
                ? p.getDisplayMode(state)
                : FurnitureDisplayMode.TOP_FACE;

        float scale = 0.5f;

        switch (mode) {
            case CEILING -> {
                // Uses FIXED context + 0.5 pre-scale (same as all other modes) so the
                // ceiling model appears at the same visual size as floor furniture.
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
                    // CosmoLib WallFurnitureBlock encodes direction in ROTATION.
                    // With FACE_OFFSET=0: 0=faces-North, 2=faces-East, 4=faces-South, 6=faces-West.
                    // The old formula was rot*45-180; with FACE_OFFSET shift of -4 steps (-180°)
                    // the net formula becomes rot*45-180+180 = rot*45.
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
            // +180° compensates for the FACE_OFFSET=0 shift: the rotation index is now
            // 4 lower than before (player-facing-South → rot=0 instead of rot=4), so
            // we add 180° back to keep the displayed model facing the correct direction.
            poseStack.mulPose(Axis.ZP.rotationDegrees(sign * rot * 45.0f + 180.0f));
        } else if (state.hasProperty(BlockStateProperties.ROTATION_16)) {
            int rot = state.getValue(BlockStateProperties.ROTATION_16);
            poseStack.mulPose(Axis.ZP.rotationDegrees(sign * rot * 45.0f + 180.0f));
        }
    }
}
