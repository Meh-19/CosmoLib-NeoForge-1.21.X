package net.meh.cosmolib;

import net.meh.cosmolib.cosmetic.CosmeticRegistry;
import net.meh.cosmolib.toolskin.ToolSkinRegistry;
import net.meh.cosmolib.cosmetic.offset.BackOffsetManager;
import net.meh.cosmolib.cosmetic.offset.HandOffsetData;
import net.meh.cosmolib.cosmetic.offset.HandOffsetManager;
import net.meh.cosmolib.crate.CrateRarityWeights;
import net.meh.cosmolib.registry.CosmoLibCrates;
import net.meh.cosmolib.entity.CosmeticMannequinEntity;
import net.meh.cosmolib.furniture.block.AnimatedFurnitureBlock;
import net.meh.cosmolib.furniture.layout.MultiBlockLayoutManager;
import net.meh.cosmolib.registry.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.lang.reflect.Field;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import net.meh.cosmolib.registry.CosmoLibDataComponents;
import net.meh.cosmolib.registry.CosmoLibRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public CosmoLib(IEventBus modEventBus) {
        // Register all deferred registers to the mod event bus
        CosmoLibBlocks.BLOCKS.register(modEventBus);
        CosmoLibItems.ITEMS.register(modEventBus);
        CosmoLibMenuTypes.MENUS.register(modEventBus);
        CosmoLibBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
        CosmoLibAttachments.ATTACHMENT_TYPES.register(modEventBus);
        CosmoLibEntityTypes.ENTITY_TYPES.register(modEventBus);
        CosmoLibCreativeTabs.CREATIVE_TABS.register(modEventBus);
        CosmoLibDataComponents.DATA_COMPONENTS.register(modEventBus);
        CosmoLibRecipeTypes.RECIPE_TYPES.register(modEventBus);
        CosmoLibRecipeTypes.RECIPE_SERIALIZERS.register(modEventBus);

        // Register built-in crate types before any world loads
        CosmoLibCrates.registerAll();

        // Register the library's own starter cosmetic
        modEventBus.addListener(this::onSetup);
    }

    private void onSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Patch furniture block entity types to accept any block.
            // BlockEntity.validateBlockState checks BlockEntityType.validBlocks (a Set.of()),
            // which only contains the single sentinel block used at registration time.
            // Dependent mods use their own block classes that must pass this check too.
            patchFurnitureEntityValidBlocks();

            // Load global crate rarity weight table from config/cosmolib/crate_rarity_weights.json
            CrateRarityWeights.load();

            CosmeticRegistry.register(CosmoLibItems.COSMO_HAT.get(), true);
            CosmeticRegistry.register(CosmoLibItems.COSMO_ROBE.get(), true);
            CosmeticRegistry.register(CosmoLibItems.COSMO_CANE.get(), true);

            // Example animated furniture — remove alongside CosmoLibBlocks.AGED_FLAG
            AnimatedFurnitureBlock.registerAnimation(
                    ResourceLocation.fromNamespaceAndPath(MOD_ID, "aged_flag"),
                    "flag_sway"
            );

            // Load all furniture layout JSONs from config/cosmolib/furniture_layouts/
            MultiBlockLayoutManager.load();

            // Load per-cosmetic back Y offsets from config/cosmolib/back_offsets.json
            BackOffsetManager.load();

            // Register known hand offsets in code, then load JSON overrides on top.
            // Offset values were tuned in-game with the Hand Tuner dev tool.
            HandOffsetManager.register(
                    ResourceLocation.fromNamespaceAndPath("cosmolib", "cosmo_cane"),
                    new HandOffsetData(-0.30, 0.625, -0.12, -90.0, 0.0, -180.0),  // left arm
                    new HandOffsetData( 0.26, 0.625,  0.12,  90.0, 0.0, -180.0)   // right arm
            );
            HandOffsetManager.load();

            // Dispenser behaviour: shoot a cosmetic mannequin into the world,
            // facing away from the dispenser (same convention as armor stands).
            DispenserBlock.registerBehavior(CosmoLibItems.COSMETIC_MANNEQUIN.get(),
                    new DefaultDispenseItemBehavior() {
                        @Override
                        protected ItemStack execute(BlockSource source, ItemStack stack) {
                            Direction facing = source.state().getValue(DispenserBlock.FACING);
                            BlockPos pos = source.pos().relative(facing);
                            ServerLevel level = source.level();

                            double cx = pos.getX() + 0.5;
                            double cy = pos.getY();
                            double cz = pos.getZ() + 0.5;
                            AABB spawnBox = CosmoLibEntityTypes.COSMETIC_MANNEQUIN.get()
                                    .getDimensions().makeBoundingBox(cx, cy, cz);
                            if (level.noCollision(spawnBox)) {
                                CosmeticMannequinEntity mannequin = new CosmeticMannequinEntity(
                                        CosmoLibEntityTypes.COSMETIC_MANNEQUIN.get(), level);
                                mannequin.setPos(cx, cy, cz);
                                // Face away from the dispenser
                                float yaw = facing.getOpposite().toYRot();
                                mannequin.setRotationIndex(Math.floorMod(Math.round(yaw / 45.0f), 8));
                                level.addFreshEntity(mannequin);
                                stack.shrink(1);
                            }
                            return stack;
                        }
                    });
        });
    }

    private static void patchFurnitureEntityValidBlocks() {
        Set<Block> allBlocks = new AbstractSet<>() {
            @Override public boolean contains(Object o) { return true; }
            @Override public Iterator<Block> iterator() { return Collections.emptyIterator(); }
            @Override public int size() { return 0; }
        };
        try {
            Field f = BlockEntityType.class.getDeclaredField("validBlocks");
            f.setAccessible(true);
            f.set(CosmoLibBlockEntityTypes.FURNITURE_ENTITY.get(), allBlocks);
            f.set(CosmoLibBlockEntityTypes.ANIMATED_FURNITURE_ENTITY.get(), allBlocks);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("[CosmoLib] Failed to patch furniture block entity valid-blocks set — dependent-mod furniture blocks will crash on placement", e);
        }
    }
}
