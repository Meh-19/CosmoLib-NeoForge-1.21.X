package net.meh.cosmolib.paint;

/**
 * The animated paint finishes driven by the entity shader system.
 *
 * Encoding: the shader detects a vertex colour where R=1.0, B=1.0, and G
 * encodes the finish ID:  G = (id - 1) * 2 / 255.0  (normalised).
 * Packed int: 0xFF_gg_FF  where gg = (id - 1) * 2.
 *
 * UI display order is defined by {@link net.meh.cosmolib.screen.PaintingTableMenu#FINISH_DISPLAY_ORDER}.
 */
public enum FinishType {

    RAINBOW    (1,  "Rainbow",    0xFF4444),
    GOLD       (2,  "Gold",       0xFFD700),
    GALAXY     (3,  "Galaxy",     0x5500CC),
    MOLTEN     (4,  "Molten",     0xFF4500),
    BUBBLE     (5,  "Bubble",     0x00BFFF),
    FLORAL     (6,  "Floral",     0xFF80C0),
    MATRIX     (7,  "Matrix",     0x00FF41),
    CHROME     (8,  "Chrome",     0xB8B8B8),
    GLITCH     (9,  "Glitch",     0xFF00CC),
    IRIDESCENT (10, "Iridescent", 0x80FFEE),
    VOID       (11, "Void",       0x100820),
    PEARL      (12, "Pearl",      0xFFF5E0),
    PHANTOM    (13, "Phantom",    0x8B00FF),
    SNOW       (14, "Snow",       0xE0F0FF);

    private final int    id;
    private final String displayName;
    /** Static representative colour shown on GUI buttons (24-bit RGB). */
    private final int    representativeColor;

    FinishType(int id, String displayName, int representativeColor) {
        this.id                 = id;
        this.displayName        = displayName;
        this.representativeColor = representativeColor;
    }

    public int    getId()                  { return id; }
    public String getDisplayName()         { return displayName; }
    /**
     * A thematic static colour (24-bit RGB) used to represent this finish in the
     * GUI, where the animated entity shader cannot run.
     */
    public int    getRepresentativeColor() { return representativeColor; }

    /** ARGB tint returned to color providers to trigger the shader branch. */
    public int getMagicArgb() {
        int gg = (id - 1) * 2;
        return 0xFF000000 | (0xFF << 16) | (gg << 8) | 0xFF;
    }

    /** 24-bit RGB stored in the PaintColor NBT key. */
    public int getMagicRgb() {
        int gg = (id - 1) * 2;
        return (0xFF << 16) | (gg << 8) | 0xFF;
    }

    // ------------------------------------------------------------------
    // Static helpers
    // ------------------------------------------------------------------

    public static FinishType fromStoredColor(int rgb24) {
        int r = (rgb24 >> 16) & 0xFF;
        int b =  rgb24        & 0xFF;
        if (r != 0xFF || b != 0xFF) return null;

        int g = (rgb24 >> 8) & 0xFF;
        if ((g & 1) != 0) return null;

        return fromId((g >> 1) + 1);
    }

    public static FinishType fromId(int id) {
        for (FinishType f : values()) {
            if (f.id == id) return f;
        }
        return null;
    }

    public static boolean isFinishColor(int rgb24) {
        return fromStoredColor(rgb24) != null;
    }
}
