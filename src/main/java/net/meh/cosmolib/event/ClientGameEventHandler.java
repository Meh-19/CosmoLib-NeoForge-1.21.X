package net.meh.cosmolib.event;

import net.meh.cosmolib.CosmoLib;
import net.meh.cosmolib.cosmetic.client.MobHatClientCache;
import net.meh.cosmolib.cosmetic.network.OpenCosmeticScreenPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
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
     * Polls the cosmetic-screen keybind each tick and asks the server to open
     * the menu when it is consumed.  Must run on the GAME bus so it fires
     * during active gameplay, not just during mod setup.
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        // Don't fire if no player or a screen is already open
        if (mc.player == null || mc.screen != null) return;

        while (ClientEventHandler.OPEN_COSMETICS.consumeClick()) {
            PacketDistributor.sendToServer(new OpenCosmeticScreenPayload());
        }
    }

    /** Clear the mob hat cache whenever this client disconnects from a world. */
    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        MobHatClientCache.clear();
    }
}
