package net.meh.cosmolib.registry;

import net.meh.cosmolib.cosmetic.CosmeticDefault;
import net.meh.cosmolib.cosmetic.CosmeticItem;
import net.meh.cosmolib.cosmetic.CosmeticRarity;
import net.meh.cosmolib.cosmetic.CosmeticSlot;
import net.meh.cosmolib.cosmetic.tool.BackswagTunerItem;
import net.meh.cosmolib.furniture.item.FurnitureBlockItem;
import net.meh.cosmolib.furniture.tool.BoundingBoxSelectorItem;
import net.meh.cosmolib.item.CosmeticMannequinItem;
import net.meh.cosmolib.item.PaintbrushItem;
import net.meh.cosmolib.paint.PaintColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.meh.cosmolib.CosmoLib.MOD_ID;

public final class CosmoLibItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    // ------------------------------------------------------------------
    // Painting table block item
    // ------------------------------------------------------------------
    public static final DeferredItem<BlockItem> PAINTING_TABLE =
            ITEMS.registerSimpleBlockItem("painting_table", CosmoLibBlocks.PAINTING_TABLE);

    // ------------------------------------------------------------------
    // Paintbrush — 300 uses; accepts Mending and Unbreaking via anvil
    // ------------------------------------------------------------------
    public static final DeferredItem<PaintbrushItem> PAINTBRUSH =
            ITEMS.register("paintbrush", () -> new PaintbrushItem(
                    new Item.Properties().durability(300)
            ));

    // ------------------------------------------------------------------
    // Starter cosmetics: one per slot for development testing
    // ------------------------------------------------------------------
    public static final DeferredItem<CosmeticItem> COSMO_HAT =
            ITEMS.register("cosmo_hat", () -> new CosmeticItem(
                    CosmeticSlot.HAT,
                    CosmeticRarity.LIMITED,
                    true,
                    CosmeticDefault.color(PaintColor.LIGHT_BLUE, 7),
                    new Item.Properties()
            ));

    public static final DeferredItem<CosmeticItem> COSMO_ROBE =
            ITEMS.register("cosmo_robe", () -> new CosmeticItem(
                    CosmeticSlot.BACK,
                    CosmeticRarity.LIMITED,
                    true,
                    CosmeticDefault.color(PaintColor.LIGHT_BLUE, 7),
                    new Item.Properties()
            ));

    public static final DeferredItem<CosmeticItem> COSMO_CANE =
            ITEMS.register("cosmo_cane", () -> new CosmeticItem(
                    CosmeticSlot.HAND,
                    CosmeticRarity.LIMITED,
                    true,
                    CosmeticDefault.color(PaintColor.LIGHT_BLUE, 7),
                    new Item.Properties()
            ));

    // ------------------------------------------------------------------
    // Furniture block items — use FurnitureBlockItem to bake default color
    // ------------------------------------------------------------------
    public static final DeferredItem<FurnitureBlockItem> COSMO_RUG =
            ITEMS.register("cosmo_rug", () -> new FurnitureBlockItem(
                    CosmoLibBlocks.COSMO_RUG.get(), new Item.Properties()));

    /** Example animated furniture — remove alongside {@link CosmoLibBlocks#AGED_FLAG}. */
    public static final DeferredItem<FurnitureBlockItem> AGED_FLAG =
            ITEMS.register("aged_flag", () -> new FurnitureBlockItem(
                    CosmoLibBlocks.AGED_FLAG.get(), new Item.Properties()));

    // ------------------------------------------------------------------
    // Cosmetic Mannequin placement item
    // ------------------------------------------------------------------
    public static final DeferredItem<CosmeticMannequinItem> COSMETIC_MANNEQUIN =
            ITEMS.register("cosmetic_mannequin", () -> new CosmeticMannequinItem(
                    new Item.Properties().stacksTo(16)
            ));

    // ------------------------------------------------------------------
    // Internal GUI-only item used to render finish previews in the
    // painting table screen.  Not obtainable; never appears in-world.
    // ------------------------------------------------------------------
    public static final DeferredItem<Item> FINISH_PREVIEW =
            ITEMS.register("finish_preview", () -> new Item(
                    new Item.Properties().stacksTo(1)
            ));

    // ------------------------------------------------------------------
    // Developer tool — Bounding Box Selector
    // Creative-mode + operator only; used to define multi-block hitbox layouts.
    // ------------------------------------------------------------------

    /**
     * In-game developer tool for defining and saving multi-block furniture layouts.
     * See {@link BoundingBoxSelectorItem} for the full usage workflow.
     */
    public static final DeferredItem<BoundingBoxSelectorItem> BOUNDING_BOX_SELECTOR =
            ITEMS.register("bounding_box_selector", () -> new BoundingBoxSelectorItem(
                    new Item.Properties().stacksTo(1)
            ));

    /**
     * Developer tool for tuning the Y render offset of back cosmetics in-game.
     * Equip a back cosmetic, hold this item, and scroll to adjust the Y translation.
     * Ctrl+Click saves to {@code config/cosmolib/back_offsets.json};
     * Shift+Click cancels without saving.
     * See {@link BackswagTunerItem} for the full usage workflow.
     */
    public static final DeferredItem<BackswagTunerItem> BACKSWAG_TUNER =
            ITEMS.register("backswag_tuner", () -> new BackswagTunerItem(
                    new Item.Properties().stacksTo(1)
            ));

    private CosmoLibItems() {}
}
