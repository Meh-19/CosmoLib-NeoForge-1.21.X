package net.meh.cosmolib.toolskin.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Wraps a tool model to add CustomModelData overrides for tool skins.
 */
class ToolSkinModelWrapper implements BakedModel {
    private final BakedModel wrapped;
    private final Map<Integer, ResourceLocation> skinModels;
    private final Map<ModelResourceLocation, BakedModel> allModels;

    private final ItemOverrides overrides = new ItemOverrides() {
        @Nullable
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack,
                                  @Nullable net.minecraft.client.multiplayer.ClientLevel level,
                                  @Nullable net.minecraft.world.entity.LivingEntity entity,
                                  int seed) {
            // Check for CustomModelData
            if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA)) {
                int customModelData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA).value();
                ResourceLocation skinModelLoc = skinModels.get(customModelData);

                if (skinModelLoc != null) {
                    // Find the baked model for this skin
                    BakedModel skinModel = allModels.get(ModelResourceLocation.standalone(skinModelLoc));
                    if (skinModel != null) {
                        // Let the skin model's overrides run (for bow pulling, etc.)
                        BakedModel resolved = skinModel.getOverrides().resolve(skinModel, stack, level, entity, seed);
                        return resolved != null ? resolved : skinModel;
                    }
                }
            }

            // Delegate to original overrides
            BakedModel original = wrapped.getOverrides().resolve(wrapped, stack, level, entity, seed);
            return original != null ? original : wrapped;
        }
    };

    public ToolSkinModelWrapper(BakedModel wrapped, Map<Integer, ResourceLocation> skinModels,
                                Map<ModelResourceLocation, BakedModel> allModels) {
        this.wrapped = wrapped;
        this.skinModels = skinModels;
        this.allModels = allModels;
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    // Delegation
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return wrapped.getQuads(state, side, rand);
    }

    @Override
    public boolean useAmbientOcclusion() { return wrapped.useAmbientOcclusion(); }
    @Override
    public boolean isGui3d() { return wrapped.isGui3d(); }
    @Override
    public boolean usesBlockLight() { return wrapped.usesBlockLight(); }
    @Override
    public boolean isCustomRenderer() { return false; }
    @Override
    public TextureAtlasSprite getParticleIcon() { return wrapped.getParticleIcon(); }
    @Override
    public ItemTransforms getTransforms() { return wrapped.getTransforms(); }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        return wrapped.getQuads(state, side, rand, data, renderType);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return wrapped.getParticleIcon(data);
    }

    @Override
    public net.neoforged.neoforge.client.ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return wrapped.getRenderTypes(state, rand, data);
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
        return wrapped.getRenderTypes(stack, fabulous);
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        return wrapped.getRenderPasses(stack, fabulous);
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext displayContext, com.mojang.blaze3d.vertex.PoseStack poseStack, boolean applyLeftHandTransform) {
        wrapped.applyTransform(displayContext, poseStack, applyLeftHandTransform);
        return this;
    }
}
