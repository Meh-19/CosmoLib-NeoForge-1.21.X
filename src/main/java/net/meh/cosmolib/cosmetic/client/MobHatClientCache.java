package net.meh.cosmolib.cosmetic.client;

import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side cache mapping entity network ID → cosmetic hat ItemStack.
 * Populated by {@link net.meh.cosmolib.cosmetic.network.MobHatSyncPayload}
 * whenever the server sends mob hat data to this client.
 * Cleared on logout.
 */
public final class MobHatClientCache {

    private static final Map<Integer, ItemStack> CACHE = new HashMap<>();

    private MobHatClientCache() {}

    public static void set(int entityId, ItemStack hat) {
        if (hat.isEmpty()) {
            CACHE.remove(entityId);
        } else {
            CACHE.put(entityId, hat.copy());
        }
    }

    public static ItemStack getHat(int entityId) {
        return CACHE.getOrDefault(entityId, ItemStack.EMPTY);
    }

    public static void remove(int entityId) {
        CACHE.remove(entityId);
    }

    public static void clear() {
        CACHE.clear();
    }
}
