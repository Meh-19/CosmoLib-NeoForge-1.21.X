package net.meh.cosmolib.cosmetic.client;

import net.meh.cosmolib.cosmetic.CosmeticSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CosmeticClientCache {

    private static final Map<UUID, Map<CosmeticSlot, ItemStack>> CACHE = new HashMap<>();

    private CosmeticClientCache() {}

    public static void update(UUID uuid, Map<CosmeticSlot, ItemStack> cosmetics) {
        Map<CosmeticSlot, ItemStack> copy = new EnumMap<>(CosmeticSlot.class);
        cosmetics.forEach((slot, stack) -> {
            if (!stack.isEmpty()) copy.put(slot, stack.copy());
        });
        CACHE.put(uuid, copy);
    }

    public static void remove(UUID uuid) {
        CACHE.remove(uuid);
    }

    public static void clear() {
        CACHE.clear();
    }

    public static ItemStack getEquipped(Entity entity, CosmeticSlot slot) {
        Map<CosmeticSlot, ItemStack> data = CACHE.get(entity.getUUID());
        if (data == null) return ItemStack.EMPTY;
        return data.getOrDefault(slot, ItemStack.EMPTY);
    }

    public static boolean hasCosmetic(Entity entity, CosmeticSlot slot) {
        return !getEquipped(entity, slot).isEmpty();
    }
}
