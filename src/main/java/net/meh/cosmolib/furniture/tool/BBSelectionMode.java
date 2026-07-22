package net.meh.cosmolib.furniture.tool;

/**
 * The three hitbox modes available in the
 * {@link BoundingBoxSelectorItem}.
 *
 * <p>Cycled in order by the BB_SLAB keybind (Left Alt by default):
 * {@code FULL → SLAB → SEAT → FULL}.
 *
 * <ul>
 *   <li>{@link #FULL}  — standard full-block (16/16) collision.</li>
 *   <li>{@link #SLAB}  — half-height (8/16) collision.</li>
 *   <li>{@link #SEAT}  — sittable: spawns a {@link net.meh.cosmolib.entity.SeatEntity}
 *                        when right-clicked; no collision change.</li>
 * </ul>
 */
public enum BBSelectionMode {
    /** Normal full-block hitbox (default). */
    FULL,
    /** Half-height (8/16) hitbox — players can walk over. */
    SLAB,
    /** Sittable position — right-click mounts the player. */
    SEAT;

    /** Returns the mode that follows this one in the cycle. */
    public BBSelectionMode next() {
        BBSelectionMode[] vals = values();
        return vals[(this.ordinal() + 1) % vals.length];
    }
}
