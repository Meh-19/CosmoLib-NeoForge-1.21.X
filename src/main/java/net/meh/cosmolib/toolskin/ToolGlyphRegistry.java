package net.meh.cosmolib.toolskin;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps item registry keys to their dedicated {@code cosmolib:default} font glyph characters.
 *
 * <p>The built-in mappings cover every vanilla tool tier.  Dependent mods that register
 * custom tool items can add their own mappings via {@link #register(ResourceLocation, String)}
 * during {@code FMLCommonSetupEvent} (or any time before tooltips are rendered):
 *
 * <pre>{@code
 * // In your mod setup:
 * ToolGlyphRegistry.register(
 *     ResourceLocation.fromNamespaceAndPath("mymod", "platinum_sword"),
 *     "ꈯ");   // whichever codepoint you added to cosmolib:default
 * }</pre>
 *
 * <p>Used by {@link net.meh.cosmolib.event.ClientGameEventHandler} to resolve the
 * specific glyph shown in a skinned tool's tooltip (e.g. a stone sword skin token
 * shows {@code ꈬ} — the stone sword glyph — not the generic diamond sword glyph).
 * Falls back to {@link ToolSkinType#getGlyph()} when no mapping is found.
 */
public final class ToolGlyphRegistry {

    private static final Map<ResourceLocation, String> GLYPHS = new HashMap<>();

    static {
        // ── Swords ──────────────────────────────────────────────────────────
        register("minecraft:wooden_sword",    "ꈫ");
        register("minecraft:stone_sword",     "ꈬ");
        register("minecraft:iron_sword",      "ꈭ");
        register("minecraft:golden_sword",    "ꈮ");
        register("minecraft:diamond_sword",   "ꈅ");
        register("minecraft:netherite_sword", "ꈶ");

        // ── Pickaxes ─────────────────────────────────────────────────────────
        register("minecraft:wooden_pickaxe",    "ꈤ");
        register("minecraft:stone_pickaxe",     "ꈥ");
        register("minecraft:iron_pickaxe",      "ꈦ");
        register("minecraft:golden_pickaxe",    "ꈧ");
        register("minecraft:diamond_pickaxe",   "ꈆ");
        register("minecraft:netherite_pickaxe", "ꈹ");

        // ── Axes ─────────────────────────────────────────────────────────────
        register("minecraft:wooden_axe",    "ꈖ");
        register("minecraft:stone_axe",     "ꈗ");
        register("minecraft:iron_axe",      "ꈘ");
        register("minecraft:golden_axe",    "ꈙ");
        register("minecraft:diamond_axe",   "ꈈ");
        register("minecraft:netherite_axe", "ꈺ");

        // ── Shovels ──────────────────────────────────────────────────────────
        register("minecraft:stone_shovel",     "ꉇ");
        register("minecraft:iron_shovel",      "ꈑ");
        register("minecraft:golden_shovel",    "ꈒ");
        register("minecraft:diamond_shovel",   "ꈇ");
        register("minecraft:netherite_shovel", "ꈷ");

        // ── Hoes ─────────────────────────────────────────────────────────────
        register("minecraft:wooden_hoe",    "ꈝ");
        register("minecraft:stone_hoe",     "ꈞ");
        register("minecraft:iron_hoe",      "ꈟ");
        register("minecraft:golden_hoe",    "ꈠ");
        register("minecraft:diamond_hoe",   "ꈉ");
        register("minecraft:netherite_hoe", "ꈸ");

        // ── Ranged / other ───────────────────────────────────────────────────
        register("minecraft:bow",    "ꈊ");
        register("minecraft:shield", "ꈳ");
    }

    private ToolGlyphRegistry() {}

    /**
     * Registers a glyph mapping for a modded item.
     *
     * @param itemKey the item's registry key (e.g. {@code mymod:platinum_sword})
     * @param glyph   the single-character string whose codepoint is defined in
     *                {@code cosmolib:default} font
     */
    public static void register(ResourceLocation itemKey, String glyph) {
        GLYPHS.put(itemKey, glyph);
    }

    /** Convenience overload accepting a raw namespaced string. */
    public static void register(String itemKey, String glyph) {
        GLYPHS.put(ResourceLocation.parse(itemKey), glyph);
    }

    /**
     * Returns the glyph for {@code itemKey}, or {@code null} if none is registered.
     * Callers should fall back to {@link ToolSkinType#getGlyph()} when {@code null}.
     */
    @Nullable
    public static String getGlyph(ResourceLocation itemKey) {
        return GLYPHS.get(itemKey);
    }
}
