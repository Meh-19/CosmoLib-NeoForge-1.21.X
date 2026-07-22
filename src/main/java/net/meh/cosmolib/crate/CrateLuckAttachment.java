package net.meh.cosmolib.crate;

import net.meh.cosmolib.crate.network.CrateLuckSyncPacket;
import net.meh.cosmolib.cosmetic.CosmeticRarity;
import net.meh.cosmolib.registry.CosmoLibAttachments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Manages the per-player luck bonus (loyalty system) for crate rolls.
 *
 * <p>The luck value is stored as a NeoForge attachment on the player entity and synced to
 * the client via {@link CrateLuckSyncPacket} whenever it changes.
 *
 * <p>Luck update rules after collecting rewards:
 * <ul>
 *   <li>Any LEGENDARY or LIMITED → reset to 0</li>
 *   <li>All 3 COMMON → +15</li>
 *   <li>Exactly 1 RARE and 2 COMMON → +8</li>
 *   <li>2+ RARE and none above → +3</li>
 *   <li>Has EPIC or higher but no LEGENDARY/LIMITED → +0 (no change)</li>
 * </ul>
 * Maximum value is capped at 60.
 */
public final class CrateLuckAttachment {

    /** Maximum luck bonus a player can accumulate. */
    public static final float MAX_LUCK = 60.0f;

    /** Client-side cached luck value — set via {@link CrateLuckSyncPacket}. */
    private static float clientLuck = 0.0f;

    private CrateLuckAttachment() {}

    /**
     * Returns the current luck bonus for the given player (server-side authoritative).
     *
     * @param player the player to query
     * @return the player's luck bonus, or {@code 0.0f} if no attachment is set
     */
    public static float getLuck(Player player) {
        return player.getData(CosmoLibAttachments.CRATE_LUCK);
    }

    /**
     * Updates the player's luck bonus based on the rarities of the items they collected from a crate.
     * Syncs the new value to the player's client.
     *
     * @param player the player who collected rewards
     * @param items  the three item stacks they received (may contain empty stacks)
     */
    public static void onCrateCollected(Player player, List<ItemStack> items) {
        float current = getLuck(player);
        float delta   = computeDelta(items);

        float next;
        if (delta < 0) {
            // Reset
            next = 0.0f;
        } else {
            next = Math.min(MAX_LUCK, current + delta);
        }

        player.setData(CosmoLibAttachments.CRATE_LUCK, next);
        PacketDistributor.sendToPlayer(
                (net.minecraft.server.level.ServerPlayer) player,
                new CrateLuckSyncPacket(next));
    }

    // ------------------------------------------------------------------
    // Client-side cache (populated by CrateLuckSyncPacket on the client)
    // ------------------------------------------------------------------

    /**
     * Stores the client-side luck value received from the server.
     * Called from {@link CrateLuckSyncPacket} on the render thread.
     *
     * @param luck the new luck value from the server
     */
    public static void setClientLuck(float luck) {
        clientLuck = luck;
    }

    /**
     * Returns the client-side cached luck value.
     *
     * @return the client's cached luck bonus
     */
    public static float getClientLuck() {
        return clientLuck;
    }

    // ------------------------------------------------------------------
    // Delta calculation
    // ------------------------------------------------------------------

    /**
     * Returns the luck change for the given set of items, or a negative sentinel to indicate
     * a reset.
     *
     * @return a positive delta to add, 0 for no change, or {@code -1} to signal a full reset
     */
    private static float computeDelta(List<ItemStack> items) {
        int common    = 0;
        int rare      = 0;
        int epic      = 0;
        int legendary = 0;
        int limited   = 0;

        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            CosmeticRarity rarity = CrateLootTable.getRarity(stack);
            switch (rarity) {
                case COMMON    -> common++;
                case RARE      -> rare++;
                case EPIC      -> epic++;
                case LEGENDARY -> legendary++;
                case LIMITED   -> limited++;
            }
        }

        if (legendary > 0 || limited > 0) return -1f;       // reset
        if (common == 3)                   return 15.0f;
        if (rare == 1 && common == 2)      return 8.0f;
        if (rare >= 2 && epic == 0)        return 3.0f;
        return 0.0f;                                         // has epic — no change
    }
}
