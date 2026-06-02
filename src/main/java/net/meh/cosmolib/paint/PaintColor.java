package net.meh.cosmolib.paint;

import org.jetbrains.annotations.Nullable;

public enum PaintColor {

    RED       ("Red",        new int[]{13324616, 11293523, 13319210, 11218731, 9900312, 8927807, 7152162}, 3),
    ORANGE    ("Orange",     new int[]{16423772, 15238200, 16613660, 15752739, 10511422, 8606770, 8932164}, 2),
    YELLOW    ("Yellow",     new int[]{16769669, 16762726, 16179281, 16236577, 15180071, 16293157, 12088320}, 3),
    PINK      ("Pink",       new int[]{16754910, 16486064, 16737932, 13522360, 11693210, 13649529, 12661577}, 1),
    WHITE     ("White",      new int[]{16777215, 16777215, 11711154, 10066329, 6710886, 5000268, 3355443}, 0),
    GREEN     ("Green",      new int[]{9681487, 6010202, 7582489, 5994518, 6584908, 5077600, 4347188}, 3),
    PURPLE    ("Purple",     new int[]{13330169, 8799149, 13933055, 9597668, 9266071, 9258373, 6109291}, 1),
    BLUE      ("Blue",       new int[]{6912482, 3832997, 3565217, 6118815, 5136766, 4279707, 3558760}, 5),
    LIGHT_BLUE("Light Blue", new int[]{11591653, 8509648, 4110557, 7644362, 1481628, 2331003, 5534851}, 2);

    private final String displayName;
    private final int[]  shades;
    private final int    defaultIndex;

    PaintColor(String displayName, int[] shades, int defaultIndex) {
        this.displayName  = displayName;
        this.shades       = shades;
        this.defaultIndex = defaultIndex;
    }

    public String getDisplayName()       { return displayName;          }
    public int[]  getShades()            { return shades;               }
    public int    getDefaultShade()      { return shades[defaultIndex]; }
    public int    getShade(int index)    { return shades[index];        }
    public int    getDefaultIndex()      { return defaultIndex;         }

    // ------------------------------------------------------------------
    // Reverse lookup
    // ------------------------------------------------------------------

    /** Returned by {@link #matchRgb(int)} when a solid-colour RGB is recognised. */
    public record PaintMatch(PaintColor color, int shadeIndex) {
        /** 1-based shade number for display (1 = lightest, 7 = darkest). */
        public int shadeNumber() { return shadeIndex + 1; }
    }

    /**
     * Returns the {@link PaintMatch} whose stored RGB equals {@code rgb},
     * or {@code null} if the value is not a known solid paint shade.
     */
    @Nullable
    public static PaintMatch matchRgb(int rgb) {
        for (PaintColor color : values()) {
            for (int i = 0; i < color.shades.length; i++) {
                if (color.shades[i] == rgb) return new PaintMatch(color, i);
            }
        }
        return null;
    }
}
