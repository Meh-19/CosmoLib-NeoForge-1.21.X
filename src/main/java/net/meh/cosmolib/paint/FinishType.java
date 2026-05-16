package net.meh.cosmolib.paint;

/**
 * The 14 animated paint finishes driven by the entity shader system.
 *
 * Encoding: the shader detects a vertex colour where R=1.0, B=1.0, and G
 * encodes the finish ID:  G = (id - 1) * 2 / 255.0  (normalised).
 * Packed int: 0xFF_FF_gg_FF  where gg = (id - 1) * 2.
 */
public enum FinishType {

    RAINBOW (1,  "Rainbow"),
    PIXEL   (2,  "Pixel"),
    GALAXY  (3,  "Galaxy"),
    MAGMA   (4,  "Magma"),
    OCEAN   (5,  "Ocean"),
    FLOWER  (6,  "Flower"),
    MATRIX  (7,  "Matrix"),
    CHROME  (8,  "Chrome"),
    GLITCH  (9,  "Glitch"),
    TILE    (10, "Tile"),
    CRATE   (11, "Crate"),
    PEARL   (12, "Pearl"),
    PHANTOM (13, "Phantom"),
    SNOW    (14, "Snow");

    private final int    id;
    private final String displayName;

    FinishType(int id, String displayName) {
        this.id          = id;
        this.displayName = displayName;
    }

    public int    getId()          { return id; }
    public String getDisplayName() { return displayName; }

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
