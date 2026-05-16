package net.meh.cosmolib;

import net.meh.cosmolib.cosmetic.CosmeticRegistry;
import net.meh.cosmolib.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * CosmoLib — a shared library for cosmetics, furniture, and the painting/finish system.
 *
 * ─────────────────────────────────────────────────────────────────────
 *  USING COSMOLIB IN YOUR MOD
 * ─────────────────────────────────────────────────────────────────────
 *
 *  1.  Add cosmolib as a dependency in your build.gradle.
 *
 *  2.  Declare a dependency in neoforge.mods.toml:
 *        [[dependencies.yourmod]]
 *          modId   = "cosmolib"
 *          type    = "required"
 *          versionRange = "[1,)"
 *          ordering = "AFTER"
 *          side = "BOTH"
 *
 *  ── COSMETICS ──
 *  3.  Create your cosmetic items using {@link net.meh.cosmolib.cosmetic.CosmeticItem}.
 *  4.  Call {@link CosmeticRegistry#register} during your mod constructor so they
 *      appear in the cosmetic creative tab and the wardrobe screen.
 *
 *  ── FURNITURE ──
 *  5.  Extend {@link net.meh.cosmolib.furniture.block.DecorationBlock} (or
 *      {@link net.meh.cosmolib.furniture.block.SittableBlock},
 *      {@link net.meh.cosmolib.furniture.block.WallFurnitureBlock}, or
 *      {@link net.meh.cosmolib.furniture.block.AnimatedFurnitureBlock}) for your block.
 *  6.  Register a {@link net.minecraft.world.level.block.entity.BlockEntityType} whose
 *      factory is {@link net.meh.cosmolib.furniture.blockentity.FurnitureBlockEntity}
 *      (or {@link net.meh.cosmolib.furniture.blockentity.AnimatedFurnitureBlockEntity}
 *       for animated pieces).
 *  7.  In your client setup, call:
 *        event.registerBlockEntityRenderer(MY_FURNITURE_BE.get(),
 *            ctx -> new FurnitureBlockEntityRenderer(ctx));
 *  8.  Provide  assets/yourmod/geo/block/my_block.geo.json
 *               assets/yourmod/textures/block/my_block.png
 *      (and     assets/yourmod/animations/block/my_block.animation.json  for animated).
 *
 *  ── PAINTING ──
 *  9.  Add your paintable blocks/items to the  cosmolib:paintable  block tag.
 * 10.  Register {@link net.meh.cosmolib.paint.client.PaintColorProvider#INSTANCE}
 *      for any item whose model has a tint layer (tintIndex 1).
 */
@Mod(CosmoLib.MOD_ID)
public class CosmoLib {

    public static final String MOD_ID = "cosmolib";

    public CosmoLib(IEventBus modEventBus) {
        // Register all deferred registers to the mod event bus
        CosmoLibBlocks.BLOCKS.register(modEventBus);
        CosmoLibItems.ITEMS.register(modEventBus);
        CosmoLibMenuTypes.MENUS.register(modEventBus);
        CosmoLibBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
        CosmoLibAttachments.ATTACHMENT_TYPES.register(modEventBus);
        CosmoLibEntityTypes.ENTITY_TYPES.register(modEventBus);
        CosmoLibCreativeTabs.CREATIVE_TABS.register(modEventBus);

        // Register the library's own starter cosmetic
        modEventBus.addListener(this::onSetup);
    }

    private void onSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CosmeticRegistry.register(CosmoLibItems.COSMO_HAT.get(), true);
            CosmeticRegistry.register(CosmoLibItems.COSMO_ROBE.get(), true);
            CosmeticRegistry.register(CosmoLibItems.COSMO_CANE.get(), true);
        });
    }
}
