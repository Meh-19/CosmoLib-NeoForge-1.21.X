package net.meh.cosmolib.registry;

import net.meh.cosmolib.toolskin.ToolSkinSmithingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.meh.cosmolib.CosmoLib.MOD_ID;

/**
 * Recipe types and serializers registered by CosmoLib.
 *
 * <p>Register both deferred registers on the mod event bus in {@code CosmoLib}:
 * <pre>{@code
 * CosmoLibRecipeTypes.RECIPE_TYPES.register(modEventBus);
 * CosmoLibRecipeTypes.RECIPE_SERIALIZERS.register(modEventBus);
 * }</pre>
 */
public final class CosmoLibRecipeTypes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MOD_ID);

    /**
     * Recipe type for the tool skin smithing recipe.
     *
     * <p>Note: {@link ToolSkinSmithingRecipe#getType()} returns
     * {@link net.minecraft.world.item.crafting.RecipeType#SMITHING} so that the
     * smithing table menu discovers it.  This custom type exists for dependent mods
     * that need to query specifically for tool-skin recipes.
     */
    public static final DeferredHolder<RecipeType<?>, RecipeType<ToolSkinSmithingRecipe>>
            TOOL_SKIN_SMITHING = RECIPE_TYPES.register("tool_skin_smithing",
            () -> new RecipeType<>() {
                @Override
                public String toString() { return MOD_ID + ":tool_skin_smithing"; }
            });

    /**
     * Serializer for {@link ToolSkinSmithingRecipe}.
     *
     * <p>The recipe JSON only needs {@code "type": "cosmolib:tool_skin_smithing"};
     * all matching logic is handled entirely in code.
     */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ToolSkinSmithingRecipe>>
            TOOL_SKIN_SMITHING_SERIALIZER = RECIPE_SERIALIZERS.register("tool_skin_smithing",
            () -> new RecipeSerializer<>() {
                @Override
                public com.mojang.serialization.MapCodec<ToolSkinSmithingRecipe> codec() {
                    return ToolSkinSmithingRecipe.CODEC;
                }

                @Override
                public net.minecraft.network.codec.StreamCodec<
                        net.minecraft.network.RegistryFriendlyByteBuf,
                        ToolSkinSmithingRecipe> streamCodec() {
                    return ToolSkinSmithingRecipe.STREAM_CODEC;
                }
            });

    private CosmoLibRecipeTypes() {}
}
