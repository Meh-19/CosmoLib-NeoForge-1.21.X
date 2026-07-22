package net.meh.cosmolib.event;

import net.meh.cosmolib.CosmoLib;
import net.meh.cosmolib.cosmetic.CosmeticItem;
import net.meh.cosmolib.cosmetic.CosmeticSlot;
import net.meh.cosmolib.cosmetic.client.CosmeticPlayerLayer;
import net.meh.cosmolib.cosmetic.client.MobCosmeticHatLayer;
import net.meh.cosmolib.cosmetic.client.MobHatClientCache;
import net.meh.cosmolib.cosmetic.screen.CosmeticScreen;
import net.meh.cosmolib.cosmetic.network.OpenCosmeticScreenPayload;
import net.meh.cosmolib.cosmetic.screen.CosmeticMenu;
import net.meh.cosmolib.crate.client.CrateHud;
import net.meh.cosmolib.crate.client.CrateRenderer;
import net.meh.cosmolib.entity.client.CosmeticMannequinRenderer;
import net.meh.cosmolib.furniture.block.AbstractFurnitureBlock;
import net.meh.cosmolib.entity.SeatEntity;
import net.meh.cosmolib.furniture.client.AnimatedFurnitureBlockEntityRenderer;
import net.meh.cosmolib.furniture.client.FurnitureBlockEntityRenderer;
import net.meh.cosmolib.paint.client.PaintColorProvider;
import net.meh.cosmolib.registry.CosmoLibBlockEntityTypes;
import net.meh.cosmolib.registry.CosmoLibEntityTypes;
import net.meh.cosmolib.registry.CosmoLibItems;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.meh.cosmolib.registry.CosmoLibMenuTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CosmoLib.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEventHandler {

    // ------------------------------------------------------------------
    // Key bindings
    // ------------------------------------------------------------------
    public static net.minecraft.client.KeyMapping OPEN_COSMETICS;
    /** Ctrl+Z equivalent for undoing the last selection change in the BB Selector. */
    public static net.minecraft.client.KeyMapping BB_UNDO;
    /**
     * Toggles slab-hitbox mode in the BB Selector.
     * While active, added positions use a half-height (8/16) collision shape.
     * Default: Left Alt.
     */
    public static net.minecraft.client.KeyMapping BB_SLAB;

    private ClientEventHandler() {}

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        OPEN_COSMETICS = new net.minecraft.client.KeyMapping(
                "key.cosmolib.open_cosmetics",
                org.lwjgl.glfw.GLFW.GLFW_KEY_PERIOD,
                "key.categories.cosmolib"
        );
        BB_UNDO = new net.minecraft.client.KeyMapping(
                "key.cosmolib.bb_undo",
                org.lwjgl.glfw.GLFW.GLFW_KEY_Z,
                "key.categories.cosmolib"
        );
        BB_SLAB = new net.minecraft.client.KeyMapping(
                "key.cosmolib.bb_slab",
                org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT,
                "key.categories.cosmolib"
        );
        event.register(OPEN_COSMETICS);
        event.register(BB_UNDO);
        event.register(BB_SLAB);
    }

    // ------------------------------------------------------------------
    // Entity render layers — players + mobs
    // ------------------------------------------------------------------
    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        Minecraft mc = Minecraft.getInstance();

        // --- Player cosmetic layer ---
        for (PlayerSkin.Model skin : event.getSkins()) {
            var renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer pr) {
                pr.addLayer(new CosmeticPlayerLayer(pr, mc.getItemRenderer()));
            }
        }

        // --- Mob cosmetic hat layer ---
        // Registered for every entity type that supports a head cosmetic.
        // MobCosmeticHatLayer uses root().getChild("head") to locate the head
        // part, so it gracefully skips any mob whose model doesn't have one.
        EntityType<?>[] hatMobs = {
                EntityType.ALLAY,
                EntityType.BLAZE,
                EntityType.BOGGED,
                EntityType.BREEZE,
                EntityType.COW,
                EntityType.CREEPER,
                EntityType.DROWNED,
                EntityType.ENDERMAN,
                EntityType.FOX,
                EntityType.HUSK,
                EntityType.IRON_GOLEM,
                EntityType.PIG,
                EntityType.PIGLIN,
                EntityType.PIGLIN_BRUTE,
                EntityType.POLAR_BEAR,
                EntityType.SHEEP,
                EntityType.SKELETON,
                EntityType.STRAY,
                EntityType.WITHER_SKELETON,
                EntityType.ZOMBIE,
                EntityType.ZOMBIFIED_PIGLIN,
        };

        for (EntityType<?> type : hatMobs) {
            EntityRenderer<?> renderer = event.getRenderer(type);
            if (renderer instanceof LivingEntityRenderer ler) {
                ler.addLayer(new MobCosmeticHatLayer(ler, mc.getItemRenderer()));
            }
        }
    }

    // ------------------------------------------------------------------
    // Block entity renderers
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void registerBERs(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CosmoLibEntityTypes.SEAT.get(), NoopRenderer::new);
        event.registerEntityRenderer(CosmoLibEntityTypes.COSMETIC_MANNEQUIN.get(),
                CosmeticMannequinRenderer::new);
        event.registerEntityRenderer(CosmoLibEntityTypes.CRATE_ENTITY.get(),
                CrateRenderer::new);
        event.registerBlockEntityRenderer(
                CosmoLibBlockEntityTypes.FURNITURE_ENTITY.get(),
                FurnitureBlockEntityRenderer::new);
    }

    // ------------------------------------------------------------------
    // Screen factories
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(CosmoLibMenuTypes.PAINTING_TABLE.get(),
                net.meh.cosmolib.screen.PaintingTableScreen::new);
        event.register(CosmoLibMenuTypes.COSMETIC_SCREEN.get(),
                CosmeticScreen::new);
    }

    // ------------------------------------------------------------------
    // Paint color providers
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // Tools and utility items — not CosmeticItems, registered manually.
        event.register(PaintColorProvider.INSTANCE,
                CosmoLibItems.PAINTBRUSH.get(),
                CosmoLibItems.FINISH_PREVIEW.get());

        // Auto-discover every paintable CosmeticItem registered by any mod.
        // CosmeticItem self-registers into SLOT_POOL on construction, so this
        // picks up cosmetics from CosmeticRegistrar and manual registrations alike.
        for (CosmeticSlot slot : CosmeticSlot.values()) {
            for (CosmeticItem cosmeticItem : CosmeticItem.getBySlot(slot)) {
                if (cosmeticItem.isPaintable()) {
                    event.register(PaintColorProvider.COSMETIC, cosmeticItem);
                }
            }
        }

        // Register paint tinting for every furniture block item — covers both
        // CosmoLib's own furniture and any furniture added by dependent mods.
        for (var block : BuiltInRegistries.BLOCK) {
            if (block instanceof AbstractFurnitureBlock) {
                Item item = block.asItem();
                if (item != Items.AIR) {
                    event.register(PaintColorProvider.INSTANCE, item);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Crate HUD
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        CrateHud.onRenderGui(event);
    }

}
