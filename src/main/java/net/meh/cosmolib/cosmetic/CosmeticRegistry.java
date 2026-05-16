package net.meh.cosmolib.cosmetic;

import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central catalogue of all registered cosmetics.
 *
 * Dependent mods call {@link #register} to add their cosmetics here so they
 * automatically appear in the CosmoLib creative tab and cosmetic screen.
 *
 * Usage:
 * <pre>{@code
 * public static final DeferredItem<CosmeticItem> MY_HAT =
 *     ITEMS.register("my_hat", () -> new CosmeticItem(
 *         CosmeticSlot.HAT, CosmeticRarity.COMMON, false, new Item.Properties()));
 *
 * // In your mod constructor or setup:
 * CosmeticRegistry.register(MY_HAT.get(), true);
 * }</pre>
 */
public final class CosmeticRegistry {

    public record Entry(CosmeticItem item, boolean showInTab) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private CosmeticRegistry() {}

    /** Register a cosmetic to appear in the global cosmetic catalogue. */
    public static void register(CosmeticItem item, boolean showInTab) {
        ENTRIES.add(new Entry(item, showInTab));
    }

    public static List<Entry> getAll() {
        return Collections.unmodifiableList(ENTRIES);
    }

    /** All cosmetics that should appear in the cosmetic creative tab. */
    public static List<CosmeticItem> getTabEntries() {
        return ENTRIES.stream()
                .filter(Entry::showInTab)
                .map(Entry::item)
                .toList();
    }
}
