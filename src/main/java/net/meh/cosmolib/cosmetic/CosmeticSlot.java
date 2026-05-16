package net.meh.cosmolib.cosmetic;

public enum CosmeticSlot {
    HAT,
    BACK,
    HAND;

    public String getId()    { return name().toLowerCase(); }
    public int    getIndex() { return ordinal(); }
}
