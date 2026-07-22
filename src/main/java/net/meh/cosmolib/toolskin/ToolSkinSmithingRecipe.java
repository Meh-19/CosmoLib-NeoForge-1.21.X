package net.meh.cosmolib.toolskin;

import com.mojang.serialization.MapCodec;
import net.meh.cosmolib.registry.CosmoLibRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Smithing table recipe that handles both <b>applying</b> and <b>removing</b> a tool skin.
 *
 * <p>This is a programmatic, logic-only recipe.  A single instance is loaded via the
 * JSON at {@code data/cosmolib/recipe/tool_skin_smithing.json} whose sole content is
 * {@code "type": "cosmolib:tool_skin_smithing"}.  All matching logic is code-driven
 * and therefore automatically covers every registered {@link ToolSkinItem}.
 *
 * <h3>Apply operation</h3>
 * <ul>
 *   <li>Template slot: a {@link ToolSkinItem} token</li>
 *   <li>Base slot: a tool whose {@link ToolSkinType} matches the token, without an
 *       existing {@link ToolSkinData} component</li>
 *   <li>Addition slot: empty or ignored</li>
 *   <li>Result: the tool with {@link ToolSkinData} applied</li>
 * </ul>
 *
 * <h3>Remove operation</h3>
 * <ul>
 *   <li>Template slot: <em>empty</em></li>
 *   <li>Base slot: a tool that already has a {@link ToolSkinData} component</li>
 *   <li>Addition slot: empty or ignored</li>
 *   <li>Result: the clean tool (skin component removed)</li>
 *   <li>The original {@link ToolSkinItem} token is returned to the player's inventory
 *       via an {@code ItemCraftedEvent} listener in
 *       {@link net.meh.cosmolib.event.GameEventHandler}.</li>
 * </ul>
 */
public class ToolSkinSmithingRecipe implements SmithingRecipe {

    /**
     * MapCodec for a no-field singleton recipe.
     * The JSON only needs {@code "type": "cosmolib:tool_skin_smithing"}.
     */
    public static final MapCodec<ToolSkinSmithingRecipe> CODEC =
            MapCodec.unit(new ToolSkinSmithingRecipe());

    /** Network codec — no state to serialize; always creates a fresh singleton. */
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolSkinSmithingRecipe> STREAM_CODEC =
            StreamCodec.of((buf, recipe) -> { /* no fields */ }, buf -> new ToolSkinSmithingRecipe());

    // ------------------------------------------------------------------
    // SmithingRecipe slot-query methods (used by the smithing screen UI
    // to highlight valid items in each slot)
    // ------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code true} for any {@link ToolSkinItem} (the apply path).
     * The remove path has an empty template slot, which is handled directly
     * in {@link #matches}.
     */
    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return stack.getItem() instanceof ToolSkinItem;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code true} for any tool matched by {@link ToolSkinType#fromItem}
     * (can be skinned) or any item that already carries {@link ToolSkinData}
     * (can be unskinned).
     */
    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return ToolSkinType.fromItem(stack.getItem()).isPresent()
                || ToolSkinData.get(stack).isPresent();
    }

    /** Always {@code true}; the addition slot is not used by this recipe. */
    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return true;
    }

    // ------------------------------------------------------------------
    // Recipe matching and assembly
    // ------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Accepts two input configurations:
     * <ol>
     *   <li><b>Apply</b>: template is a {@link ToolSkinItem}, base is an unskinned
     *       tool of the matching {@link ToolSkinType}.</li>
     *   <li><b>Remove</b>: template is empty, base is a skinned tool carrying
     *       {@link ToolSkinData}.</li>
     * </ol>
     */
    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return isApply(input) || isRemove(input);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link ToolSkinData#apply} or {@link ToolSkinData#remove}
     * based on which path {@link #matches} accepted.
     *
     * <p>For removal, returns the clean tool in the smithing output slot.
     * The token is returned to inventory via ItemCraftedEvent.
     */
    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        if (isApply(input)) {
            ToolSkinItem skinItem = (ToolSkinItem) input.template().getItem();
            return ToolSkinData.apply(input.base(), skinItem);
        }
        if (isRemove(input)) {
            // Return the clean tool in the output slot
            return ToolSkinData.remove(input.base());
        }
        return ItemStack.EMPTY;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@link ItemStack#EMPTY} because the result depends on the
     * actual input stacks and cannot be determined without them.
     * The smithing table screen uses {@link #assemble} for the live output preview.
     */
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CosmoLibRecipeTypes.TOOL_SKIN_SMITHING_SERIALIZER.get();
    }

    /**
     * Returns {@link RecipeType#SMITHING} so that the vanilla smithing table menu
     * discovers this recipe when querying for all smithing recipes.
     */
    @Override
    public RecipeType<?> getType() {
        return RecipeType.SMITHING;
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private static boolean isApply(SmithingRecipeInput input) {
        if (!(input.template().getItem() instanceof ToolSkinItem skinItem)) return false;
        ItemStack base = input.base();
        if (base.isEmpty()) return false;
        if (ToolSkinData.get(base).isPresent()) return false; // already has a skin
        return skinItem.getSkinType().getMatcher().test(base.getItem());
    }

    private static boolean isRemove(SmithingRecipeInput input) {
        if (!input.template().isEmpty()) return false;
        ItemStack base = input.base();
        if (base.isEmpty()) return false;
        return ToolSkinData.get(base).isPresent();
    }
}
