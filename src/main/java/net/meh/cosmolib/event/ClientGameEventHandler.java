package net.meh.cosmolib.event;

import net.meh.cosmolib.CosmoLib;
import net.meh.cosmolib.cosmetic.client.MobHatClientCache;
import net.meh.cosmolib.cosmetic.network.OpenCosmeticScreenPayload;
import net.meh.cosmolib.crate.client.CrateCameraController;
import net.meh.cosmolib.furniture.tool.client.BoundingBoxClientState;
import net.meh.cosmolib.furniture.tool.client.BoundingBoxRenderer;
import net.meh.cosmolib.furniture.tool.network.ToggleBBSlabPayload;
import net.meh.cosmolib.furniture.tool.network.UndoSelectionPayload;
import net.meh.cosmolib.registry.CosmoLibItems;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-side subscribers on the GAME bus.
 * Kept separate from {@link ClientEventHandler} (MOD bus) because NeoForge
 * requires different bus targets for mod-lifecycle vs. game events.
 */
@EventBusSubscriber(modid = CosmoLib.MOD_ID, value = Dist.CLIENT)
public final class ClientGameEventHandler {

    private ClientGameEventHandler() {}

    /**
     * Polls the cosmetic-screen keybind and the BB-undo keybind each tick.
     * Must run on the GAME bus so it fires during active gameplay.
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        // Don't fire if no player or a screen is already open
        if (mc.player == null || mc.screen != null) return;

        while (ClientEventHandler.OPEN_COSMETICS.consumeClick()) {
            PacketDistributor.sendToServer(new OpenCosmeticScreenPayload());
        }

        // BB Selector keybinds — only fire when session is active and player holds the tool
        if (BoundingBoxClientState.sessionActive
                && (mc.player.getMainHandItem().is(CosmoLibItems.BOUNDING_BOX_SELECTOR.get())
                        || mc.player.getOffhandItem().is(CosmoLibItems.BOUNDING_BOX_SELECTOR.get()))) {
            while (ClientEventHandler.BB_UNDO.consumeClick()) {
                PacketDistributor.sendToServer(new UndoSelectionPayload());
            }
            while (ClientEventHandler.BB_SLAB.consumeClick()) {
                PacketDistributor.sendToServer(new ToggleBBSlabPayload());
            }
        }

        // Advance crate camera lerp and maintain camera lock / movement penalties
        CrateCameraController.tick();
    }

    /** Clear the mob hat cache and BB client state whenever this client disconnects. */
    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        MobHatClientCache.clear();
        BoundingBoxClientState.clear();
    }

    /** Delegate world-level wireframe rendering to {@link BoundingBoxRenderer}. */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        BoundingBoxRenderer.render(event);
    }

    /**
     * Computes the locked camera angle per rendered frame.
     * Using a per-frame hook (rather than the per-tick {@code onClientTick}) means
     * the camera tracks the crate using the render partial-tick player position,
     * eliminating the tick-rate jitter that causes choppiness when moving.
     */
    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        CrateCameraController.onComputeCameraAngles(event);
    }

    /** Suppress scroll input while the crate camera is locked. */
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (CrateCameraController.shouldCancelScroll()) {
            event.setCanceled(true);
        }
    }
}
