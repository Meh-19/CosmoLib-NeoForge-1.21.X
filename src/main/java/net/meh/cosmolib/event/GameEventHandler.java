package net.meh.cosmolib.event;

import net.meh.cosmolib.CosmoLib;
import net.meh.cosmolib.cosmetic.CosmeticManager;
import net.meh.cosmolib.crate.entity.CrateEntity;
import net.meh.cosmolib.furniture.tool.BoundingBoxSelectorSession;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
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

    /**
     * Cancels all incoming attacks against a player who is currently in a crate session.
     *
     * <p>Cancelling {@link LivingIncomingDamageEvent} prevents the damage container from
     * being processed — no HP loss, no armour durability drain. This is a much cleaner
     * guarantee than resistance effects, which only reduce the damage value after the hit
     * has already been registered.
     */
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player
                && CrateEntity.isPlayerInCrateSession(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    /**
     * Clears any active {@link BoundingBoxSelectorSession} when a player logs out,
     * so their in-progress work does not linger and block other server operations.
     * The temp crash-recovery file is intentionally left on disk for inspection.
     */
    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            BoundingBoxSelectorSession.clear(sp.getStringUUID());
        }
    }
}
