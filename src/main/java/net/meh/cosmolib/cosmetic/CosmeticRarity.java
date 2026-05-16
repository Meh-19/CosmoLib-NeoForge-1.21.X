package net.meh.cosmolib.cosmetic;

public enum CosmeticRarity {
    COMMON   ("ꑲ", 0xAAAAAA),
    RARE     ("ꑳ", 0x5599FF),
    EPIC     ("ꑴ", 0xCC44CC),
    LEGENDARY("ꑵ", 0xFFAA00),
    LIMITED  ("ꑶ", 0xFF5555);

    private final String symbol;
    private final int    color;

    CosmeticRarity(String symbol, int color) {
        this.symbol = symbol;
        this.color  = color;
    }

    public String getSymbol() { return symbol; }
    public int    getColor()  { return color;  }
}
