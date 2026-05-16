package net.meh.cosmolib.registry;

import net.meh.cosmolib.cosmetic.CosmeticItem;
import net.meh.cosmolib.cosmetic.CosmeticRarity;
import net.meh.cosmolib.cosmetic.CosmeticSlot;
import net.meh.cosmolib.item.PaintbrushItem;
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
    // Paintbrush (max damage 128 = lasts a while but does wear down)
    // ------------------------------------------------------------------
    public static final DeferredItem<PaintbrushItem> PAINTBRUSH =
            ITEMS.register("paintbrush", () -> new PaintbrushItem(
                    new Item.Properties().durability(128)
            ));

    // ------------------------------------------------------------------
    // Starter cosmetics: one per slot for development testing
    // ------------------------------------------------------------------
    public static final DeferredItem<CosmeticItem> COSMO_HAT =
            ITEMS.register("cosmo_hat", () -> new CosmeticItem(
                    CosmeticSlot.HAT,
                    CosmeticRarity.COMMON,
                    false,
                    new Item.Properties()
            ));

    public static final DeferredItem<CosmeticItem> COSMO_ROBE =
            ITEMS.register("cosmo_robe", () -> new CosmeticItem(
                    CosmeticSlot.BACK,
                    CosmeticRarity.COMMON,
                    false,
                    new Item.Properties()
            ));

    public static final DeferredItem<CosmeticItem> COSMO_CANE =
            ITEMS.register("cosmo_cane", () -> new CosmeticItem(
                    CosmeticSlot.HAND,
                    CosmeticRarity.COMMON,
                    false,
                    new Item.Properties()
            ));

    // ------------------------------------------------------------------
    // Test furniture block item
    // ------------------------------------------------------------------
    public static final DeferredItem<BlockItem> COSMO_RUG =
            ITEMS.registerSimpleBlockItem("cosmo_rug", CosmoLibBlocks.COSMO_RUG);

    private CosmoLibItems() {}
}
