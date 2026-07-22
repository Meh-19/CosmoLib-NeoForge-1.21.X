package net.meh.cosmolib.crate;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.meh.cosmolib.CosmoLib;
import net.meh.cosmolib.cosmetic.CosmeticItem;
import net.meh.cosmolib.cosmetic.CosmeticRarity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import net.minecraft.util.RandomSource;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Per-crate loot table loaded from:
 * {@code data/<namespace>/loot_tables/crates/<path>.json}
 *
 * <p>Instances are cached by {@link ResourceLocation}. The cache is cleared automatically
 * when the server's data packs reload (register {@link #RELOAD_LISTENER} with
 * {@code AddReloadListenerEvent}).
 *
 * <p>JSON format:
 * <pre>{@code
 * {
 *   "common":    ["modid:item_a", "modid:item_b"],
 *   "rare":      ["modid:item_c"],
 *   "epic":      [],
 *   "legendary": ["modid:item_d"],
 *   "limited":   []
 * }
 * }</pre>
 */
public final class CrateLootTable {

    private static final Gson GSON = new Gson();

    /** Register with {@code AddReloadListenerEvent} to clear the cache on {@code /reload}. */
    public static final PreparableReloadListener RELOAD_LISTENER = new ReloadListener();

    private static final Map<ResourceLocation, CrateLootTable> CACHE = new HashMap<>();

    /**
     * Global reverse-lookup: item registry ID string → rarity, populated as loot tables load.
     * Used by {@link #getRarity(ItemStack)} for non-{@link CosmeticItem} entries.
     */
    private static final Map<String, CosmeticRarity> GLOBAL_ID_RARITY = new HashMap<>();

    @Nullable
    private static ResourceManager activeManager;

    // Items per rarity tier
    private final EnumMap<CosmeticRarity, List<ResourceLocation>> tiers;

    private CrateLootTable(EnumMap<CosmeticRarity, List<ResourceLocation>> tiers) {
        this.tiers = tiers;
    }

    // ------------------------------------------------------------------
    // Static access
    // ------------------------------------------------------------------

    /**
     * Returns (and caches) the loot table for the given ID, reading from the active
     * server resource manager.
     *
     * @param id the crate's loot-table ResourceLocation
     * @return the loaded loot table
     * @throws IllegalStateException if no resource manager is available or the file is missing
     */
    public static CrateLootTable load(ResourceLocation id) {
        return CACHE.computeIfAbsent(id, CrateLootTable::read);
    }

    /**
     * Returns the {@link CosmeticRarity} for an item stack.
     *
     * <ul>
     *   <li>If the item is a {@link CosmeticItem}, returns its own rarity.</li>
     *   <li>Otherwise, searches all loaded loot tables and returns the tier that contains
     *       this item's registry ID. Defaults to {@link CosmeticRarity#COMMON} if not found.</li>
     * </ul>
     *
     * @param stack the item stack to look up
     * @return the rarity, never null
     */
    public static CosmeticRarity getRarity(ItemStack stack) {
        if (stack.getItem() instanceof CosmeticItem c) return c.getRarity();
        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return GLOBAL_ID_RARITY.getOrDefault(key, CosmeticRarity.COMMON);
    }

    // ------------------------------------------------------------------
    // Rolling
    // ------------------------------------------------------------------

    /**
     * Rolls exactly 3 items from this loot table.
     *
     * <ul>
     *   <li>Slot 1 (guaranteed rare+): rolls via {@link CrateRarityWeights#rollRareOrHigher}</li>
     *   <li>Slots 2 &amp; 3: roll via {@link CrateRarityWeights#roll}</li>
     * </ul>
     *
     * @param random    source of randomness
     * @param luckBonus player luck bonus applied to rolls
     * @return list of exactly 3 non-null {@link ItemStack}s
     * @throws IllegalStateException if the loot table is completely empty
     */
    public List<ItemStack> rollItems(RandomSource random, float luckBonus) {
        ensureNotEmpty();
        List<ItemStack> result = new ArrayList<>(3);

        // Slot 1 — guaranteed rare+
        result.add(pickItem(random, CrateRarityWeights.rollRareOrHigher(random, luckBonus), true));

        // Slots 2 and 3 — standard roll
        for (int i = 0; i < 2; i++) {
            result.add(pickItem(random, CrateRarityWeights.roll(random, luckBonus), false));
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private ItemStack pickItem(RandomSource random, CosmeticRarity rolledRarity, boolean requireRarePlus) {
        List<ResourceLocation> pool = resolvePool(rolledRarity, requireRarePlus);
        ResourceLocation chosen = pool.get(random.nextInt(pool.size()));
        net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(chosen);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /**
     * Returns the item pool for the given rarity, falling back to adjacent tiers when empty.
     * If {@code requireRarePlus} is true, the fallback stays within RARE+ before expanding to any tier.
     */
    private List<ResourceLocation> resolvePool(CosmeticRarity rarity, boolean requireRarePlus) {
        CosmeticRarity[] order = CosmeticRarity.values();

        // Try the exact tier first
        List<ResourceLocation> pool = tiers.get(rarity);
        if (pool != null && !pool.isEmpty()) return pool;

        // Search downward (toward COMMON) within constraints
        int idx = rarity.ordinal();
        int minOrdinal = requireRarePlus ? CosmeticRarity.RARE.ordinal() : 0;
        for (int i = idx - 1; i >= minOrdinal; i--) {
            pool = tiers.get(order[i]);
            if (pool != null && !pool.isEmpty()) return pool;
        }

        // Search upward (toward LIMITED)
        for (int i = idx + 1; i < order.length; i++) {
            pool = tiers.get(order[i]);
            if (pool != null && !pool.isEmpty()) return pool;
        }

        // Last resort: any non-empty tier
        for (CosmeticRarity r : order) {
            pool = tiers.get(r);
            if (pool != null && !pool.isEmpty()) return pool;
        }

        throw new IllegalStateException("[CosmoLib] CrateLootTable has no items in any tier");
    }

    private void ensureNotEmpty() {
        for (List<ResourceLocation> list : tiers.values()) {
            if (!list.isEmpty()) return;
        }
        throw new IllegalStateException("[CosmoLib] CrateLootTable is completely empty");
    }

    // ------------------------------------------------------------------
    // Reading from resource manager
    // ------------------------------------------------------------------

    private static CrateLootTable read(ResourceLocation id) {
        if (activeManager == null) {
            throw new IllegalStateException(
                    "[CosmoLib] CrateLootTable.load() called before resource manager is available");
        }
        ResourceLocation resPath = ResourceLocation.fromNamespaceAndPath(
                id.getNamespace(), "loot_tables/crates/" + id.getPath() + ".json");
        try (Reader reader = activeManager.openAsReader(resPath)) {
            return parse(GSON.fromJson(reader, JsonObject.class));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "[CosmoLib] Could not read crate loot table " + resPath, e);
        }
    }

    private static CrateLootTable parse(JsonObject json) {
        EnumMap<CosmeticRarity, List<ResourceLocation>> tiers = new EnumMap<>(CosmeticRarity.class);
        for (CosmeticRarity rarity : CosmeticRarity.values()) {
            String key = rarity.name().toLowerCase();
            List<ResourceLocation> list = new ArrayList<>();
            if (json.has(key)) {
                JsonArray arr = json.getAsJsonArray(key);
                for (JsonElement el : arr) {
                    String idStr = el.getAsString();
                    ResourceLocation itemId = ResourceLocation.tryParse(idStr);
                    if (itemId != null) {
                        list.add(itemId);
                        GLOBAL_ID_RARITY.put(idStr, rarity); // populate reverse lookup
                    } else {
                        CosmoLib.LOGGER.warn("[CosmoLib] Invalid item ID '{}' in crate loot table", idStr);
                    }
                }
            }
            tiers.put(rarity, Collections.unmodifiableList(list));
        }
        return new CrateLootTable(tiers);
    }

    // ------------------------------------------------------------------
    // Reload listener
    // ------------------------------------------------------------------

    private static final class ReloadListener implements PreparableReloadListener {
        @Override
        public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager manager,
                ProfilerFiller prepProfiler, ProfilerFiller reloadProfiler,
                Executor backgroundExecutor, Executor gameExecutor) {
            return CompletableFuture.supplyAsync(() -> null, backgroundExecutor)
                    .thenCompose(stage::wait)
                    .thenRunAsync(() -> {
                        CACHE.clear();
                        GLOBAL_ID_RARITY.clear();
                        activeManager = manager;
                        CosmoLib.LOGGER.info("[CosmoLib] CrateLootTable cache cleared (data reload)");
                    }, gameExecutor);
        }
    }
}
