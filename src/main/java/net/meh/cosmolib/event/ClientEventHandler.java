package net.meh.cosmolib.event;

import net.meh.cosmolib.CosmoLib;
import net.meh.cosmolib.cosmetic.client.CosmeticPlayerLayer;
import net.meh.cosmolib.cosmetic.screen.CosmeticScreen;
import net.meh.cosmolib.cosmetic.network.OpenCosmeticScreenPayload;
import net.meh.cosmolib.cosmetic.screen.CosmeticMenu;
import net.meh.cosmolib.furniture.client.FurnitureBlockEntityRenderer;
import net.meh.cosmolib.paint.client.PaintColorProvider;
import net.meh.cosmolib.registry.CosmoLibBlockEntityTypes;
import net.meh.cosmolib.registry.CosmoLibItems;
import net.meh.cosmolib.registry.CosmoLibMenuTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CosmoLib.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEventHandler {

    // ------------------------------------------------------------------
    // Key bindings
    // ------------------------------------------------------------------
    public static net.minecraft.client.KeyMapping OPEN_COSMETICS;

    private ClientEventHandler() {}

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        OPEN_COSMETICS = new net.minecraft.client.KeyMapping(
                "key.cosmolib.open_cosmetics",
                org.lwjgl.glfw.GLFW.GLFW_KEY_PERIOD,
                "key.categories.cosmolib"
        );
        event.register(OPEN_COSMETICS);
    }

    // ------------------------------------------------------------------
    // Player cosmetic layer
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        Minecraft mc = Minecraft.getInstance();
        for (PlayerSkin.Model skin : event.getSkins()) {
            var renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer pr) {
                pr.addLayer(new CosmeticPlayerLayer(pr, mc.getItemRenderer()));
            }
        }
    }

    // ------------------------------------------------------------------
    // Block entity renderers
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void registerBERs(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                CosmoLibBlockEntityTypes.FURNITURE_ENTITY.get(),
                FurnitureBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                CosmoLibBlockEntityTypes.ANIMATED_FURNITURE_ENTITY.get(),
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
        event.register(PaintColorProvider.INSTANCE,
                CosmoLibItems.PAINTBRUSH.get(),
                CosmoLibItems.COSMO_HAT.get(),
                CosmoLibItems.COSMO_ROBE.get(),
                CosmoLibItems.COSMO_STAFF.get());
    }
}
