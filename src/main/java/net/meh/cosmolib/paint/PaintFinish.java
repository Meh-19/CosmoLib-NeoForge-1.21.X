package net.meh.cosmolib.paint;

import net.minecraft.world.item.ItemStack;

/**
 * Utilities for the finish (animated shader) system that sits on top of {@link PaintData}.
 *
 * Plain colour  – any 24-bit int where R≠255 OR B≠255.
 * Finish colour – R=255, B=255, G=(finishId-1)*2.
 */
public final class PaintFinish {

    private PaintFinish() {}

    // ------------------------------------------------------------------
    // Writing
    // ------------------------------------------------------------------

    public static void setFinish(ItemStack stack, FinishType finish) {
        PaintData.applyColor(stack, finish.getMagicRgb());
    }

    public static boolean setFinishById(ItemStack stack, int id) {
        FinishType f = FinishType.fromId(id);
        if (f == null) return false;
        setFinish(stack, f);
        return true;
    }

    // ------------------------------------------------------------------
    // Reading → ARGB tint for color providers
    // ------------------------------------------------------------------

    /**
     * Returns the ARGB tint for this stack's paint value.
     * -1 = no paint, magic ARGB = finish, opaque solid = plain colour.
     */
    public static int getArgbTint(ItemStack stack) {
        int stored = PaintData.getColor(stack);
        if (stored < 0) return -1;
        FinishType finish = FinishType.fromStoredColor(stored);
        return finish != null ? finish.getMagicArgb() : (0xFF000000 | (stored & 0xFFFFFF));
    }

    /** Same as {@link #getArgbTint(ItemStack)} but for a raw stored-color int. */
    public static int getArgbTintFromRaw(int stored) {
        if (stored < 0) return -1;
        FinishType finish = FinishType.fromStoredColor(stored);
        return finish != null ? finish.getMagicArgb() : (0xFF000000 | (stored & 0xFFFFFF));
    }

    // ------------------------------------------------------------------
    // Introspection
    // ------------------------------------------------------------------

    public static boolean hasFinish(ItemStack stack) {
        int stored = PaintData.getColor(stack);
        return stored >= 0 && FinishType.isFinishColor(stored);
    }

    public static FinishType getFinish(ItemStack stack) {
        int stored = PaintData.getColor(stack);
        return stored < 0 ? null : FinishType.fromStoredColor(stored);
    }
}
