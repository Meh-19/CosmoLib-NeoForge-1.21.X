package net.meh.cosmolib.toolskin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central catalogue of all registered {@link ToolSkinItem}s across all mods.
 *
 * <p>Dependent mods call {@link #register} during their mod constructor or
 * {@code FMLCommonSetupEvent} so that their skins participate in atlas
 * stitching, creative tab display, and the smithing table recipe.
 *
 * <p>Usage:
 * <pre>{@code
 * public static final DeferredItem<ToolSkinItem> PUNK_AXE_SKIN =
 *     ITEMS.register("punk_axe_skin", () -> ToolSkinItem.builder()
 *         .rarity(CosmeticRarity.EPIC)
 *         .type(ToolSkinType.AXE)
 *         .texture(ResourceLocation.fromNamespaceAndPath("mymod", "tool_skins/punk_axe"))
 *         .skinSetName("Punk")
 *         .build());
 *
 * // In your mod constructor:
 * ToolSkinRegistry.register(PUNK_AXE_SKIN.get());
 * }</pre>
 */
public final class ToolSkinRegistry {

    private static final List<ToolSkinItem> ENTRIES = new ArrayList<>();

    private ToolSkinRegistry() {}

    /** Registers a {@link ToolSkinItem} so it participates in all CosmoLib systems. */
    public static void register(ToolSkinItem item) {
        ENTRIES.add(item);
    }

    /** Returns an unmodifiable snapshot of all registered skin items. */
    public static List<ToolSkinItem> getAll() {
        return Collections.unmodifiableList(ENTRIES);
    }
}
