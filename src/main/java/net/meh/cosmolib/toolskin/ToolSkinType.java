package net.meh.cosmolib.toolskin;

import net.minecraft.world.item.*;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Enum of tool categories that can receive a {@link ToolSkinItem}.
 *
 * <p>Each value carries a human-readable display name, a font glyph character,
 * and a predicate that determines whether a given {@link Item} belongs to this
 * category.  Vanilla tool sub-classes are matched via {@code instanceof}; BOW and
 * SHIELD are matched by identity since vanilla does not sub-class them.
 *
 * <p>Font glyphs are rendered using the {@code cosmolib:default} bitmap font.
 * Each maps to a dedicated 8×8 icon image that must be provided at:
 * {@code assets/cosmolib/textures/gui/lore_tool_<type>.png}
 * <ul>
 *   <li>SWORD   — U+A460 {@code ꑠ} ({@code cosmolib:gui/lore_tool_sword.png})</li>
 *   <li>PICKAXE — U+A461 {@code ꑡ} ({@code cosmolib:gui/lore_tool_pickaxe.png})</li>
 *   <li>AXE     — U+A462 {@code ꑢ} ({@code cosmolib:gui/lore_tool_axe.png})</li>
 *   <li>SHOVEL  — U+A463 {@code ꑣ} ({@code cosmolib:gui/lore_tool_shovel.png})</li>
 *   <li>HOE     — U+A464 {@code ꑤ} ({@code cosmolib:gui/lore_tool_hoe.png})</li>
 *   <li>BOW      — U+A465 {@code ꑥ} ({@code cosmolib:gui/lore_tool_bow.png})</li>
 *   <li>SHIELD   — U+A466 {@code ꑦ} ({@code cosmolib:gui/lore_tool_shield.png})</li>
 *   <li>CROSSBOW — U+A467 {@code ꑧ} ({@code cosmolib:gui/lore_tool_crossbow.png})</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * ToolSkinType.fromItem(Items.DIAMOND_AXE);   // Optional.of(AXE)
 * ToolSkinType.fromItem(Items.STICK);          // Optional.empty()
 * ToolSkinType.AXE.getMatcher().test(item);    // boolean
 * ToolSkinType.AXE.getGlyph();                 // "ꑢ"
 * }</pre>
 */
public enum ToolSkinType {

    //                  displayName    glyph — cosmolib:gui/lore_tool_<type>.png
    SWORD   ("Sword",    "ꑠ", item -> item instanceof SwordItem),
    PICKAXE ("Pickaxe",  "ꑡ", item -> item instanceof PickaxeItem),
    AXE     ("Axe",      "ꑢ", item -> item instanceof AxeItem),
    SHOVEL  ("Shovel",   "ꑣ", item -> item instanceof ShovelItem),
    HOE     ("Hoe",      "ꑤ", item -> item instanceof HoeItem),
    BOW     ("Bow",      "ꑥ", item -> item == Items.BOW),
    SHIELD  ("Shield",   "ꑦ", item -> item == Items.SHIELD),
    CROSSBOW("Crossbow", "ꑧ", item -> item == Items.CROSSBOW);

    private final String displayName;
    private final String glyph;
    private final Predicate<Item> matcher;

    ToolSkinType(String displayName, String glyph, Predicate<Item> matcher) {
        this.displayName = displayName;
        this.glyph       = glyph;
        this.matcher     = matcher;
    }

    /** Human-readable name shown in item tooltips and names, e.g. {@code "Axe"}. */
    public String getDisplayName() { return displayName; }

    /**
     * Single-character string rendered via the {@code cosmolib:default} font as the
     * tool-type icon in {@link ToolSkinItem} tooltips.
     * Each type has a unique codepoint in the Yi Syllables block (see class javadoc).
     */
    public String getGlyph() { return glyph; }

    /**
     * Returns {@code true} if {@code item} belongs to this tool category.
     * Tool sub-class checks are performed via {@code instanceof}; BOW and SHIELD
     * use identity comparison.
     */
    public Predicate<Item> getMatcher() { return matcher; }

    /**
     * Returns the first {@link ToolSkinType} whose matcher accepts {@code item},
     * or {@link Optional#empty()} if the item is not a skinnable tool type.
     */
    public static Optional<ToolSkinType> fromItem(Item item) {
        for (ToolSkinType type : values()) {
            if (type.matcher.test(item)) return Optional.of(type);
        }
        return Optional.empty();
    }
}
