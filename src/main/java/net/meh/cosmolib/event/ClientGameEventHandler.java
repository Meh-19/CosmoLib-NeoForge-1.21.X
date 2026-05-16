package net.meh.cosmolib.event;

import net.meh.cosmolib.CosmoLib;
import net.meh.cosmolib.cosmetic.client.CosmeticClientCache;
import net.meh.cosmolib.cosmetic.network.OpenCosmeticScreenPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CosmoLib.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientGameEventHandler {

    private ClientGameEventHandler() {}

    /** Poll the cosmetic keybind once per client tick. */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (ClientEventHandler.OPEN_COSMETICS != null
                && ClientEventHandler.OPEN_COSMETICS.consumeClick()) {
            PacketDistributor.sendToServer(new OpenCosmeticScreenPayload());
        }
    }

    /** Clear the client cache when the player disconnects. */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CosmeticClientCache.clear();
    }
}
