package net.meh.cosmolib.cosmetic;

import net.meh.cosmolib.cosmetic.network.SyncCosmeticsPayload;
import net.meh.cosmolib.registry.CosmoLibAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Server-side coordinator: equip/unequip cosmetics and broadcast changes.
 */
public final class CosmeticManager {

    private CosmeticManager() {}

    /**
     * Equip or unequip a cosmetic on the server, then sync to all online players.
     * Must be called on the server thread.
     */
    public static void applyEquip(ServerPlayer player, CosmeticSlot slot, ItemStack stack) {
        PlayerCosmeticsData data = player.getData(CosmoLibAttachments.COSMETICS);

        if (stack.isEmpty()) {
            data.unequip(slot);
        } else {
            if (!(stack.getItem() instanceof CosmeticItem cosmetic) || cosmetic.getSlot() != slot) return;
            data.equip(slot, stack);
        }

        player.setData(CosmoLibAttachments.COSMETICS, data);
        broadcastToAll(player, player.getServer().getPlayerList().getPlayers());
    }

    /** Send one player's full cosmetic loadout to all players in the list. */
    public static void broadcastToAll(ServerPlayer source, List<ServerPlayer> targets) {
        PlayerCosmeticsData data = source.getData(CosmoLibAttachments.COSMETICS);
        SyncCosmeticsPayload packet = new SyncCosmeticsPayload(source.getUUID(), data.getAllEquipped());
        for (ServerPlayer target : targets) {
            PacketDistributor.sendToPlayer(target, packet);
        }
    }

    /**
     * Called on player join: sync own cosmetics to them and everyone else's to them.
     */
    public static void onPlayerJoin(ServerPlayer joining) {
        // Send joining player their own cosmetics
        PlayerCosmeticsData selfData = joining.getData(CosmoLibAttachments.COSMETICS);
        PacketDistributor.sendToPlayer(joining,
                new SyncCosmeticsPayload(joining.getUUID(), selfData.getAllEquipped()));

        List<ServerPlayer> allPlayers = joining.getServer().getPlayerList().getPlayers();

        // Tell everyone about the joining player
        broadcastToAll(joining, allPlayers);

        // Tell the joining player about everyone else
        for (ServerPlayer other : allPlayers) {
            if (other == joining) continue;
            PlayerCosmeticsData otherData = other.getData(CosmoLibAttachments.COSMETICS);
            PacketDistributor.sendToPlayer(joining,
                    new SyncCosmeticsPayload(other.getUUID(), otherData.getAllEquipped()));
        }
    }
}
