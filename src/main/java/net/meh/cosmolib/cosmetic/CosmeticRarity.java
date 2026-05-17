package net.meh.cosmolib.cosmetic;

public enum CosmeticRarity {
    COMMON   ("ꑲ", 0xAAAAAA),
    RARE     ("ꑳ", 0x0099DB),
    EPIC     ("ꑴ", 0x8B5CF6),
    LEGENDARY("ꑵ", 0xF49E0B),
    LIMITED  ("ꑶ", 0x7EBE1B);

    private final String symbol;
    private final int    color;

    CosmeticRarity(String symbol, int color) {
        this.symbol = symbol;
        this.color  = color;
    }

    public String getSymbol() { return symbol; }
    public int    getColor()  { return color;  }
}
