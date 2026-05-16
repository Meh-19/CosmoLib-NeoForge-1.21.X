package net.meh.cosmolib.event;

import net.meh.cosmolib.CosmoLib;
import net.meh.cosmolib.cosmetic.CosmeticManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Listens on the NeoForge game bus for in-game events. */
@EventBusSubscriber(modid = CosmoLib.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class GameEventHandler {

    private GameEventHandler() {}

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            CosmeticManager.onPlayerJoin(sp);
        }
    }
}
