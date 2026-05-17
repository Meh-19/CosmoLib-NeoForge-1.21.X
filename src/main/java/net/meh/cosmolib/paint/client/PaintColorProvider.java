package net.meh.cosmolib.paint.client;

import net.meh.cosmolib.paint.PaintFinish;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;

/**
 * Register this for any item that supports paint tinting.
 *
 * tintIndex 0 — used by paintbrush and simple item models
 * tintIndex 1 — used by cosmetic models (base layer has no tintindex; paint layer uses 1)
 *
 * Both indexes receive the same colour so a single provider covers all model styles.
 */
public final class PaintColorProvider implements ItemColor {

    public static final PaintColorProvider INSTANCE = new PaintColorProvider();

    private PaintColorProvider() {}

    @Override
    public int getColor(ItemStack stack, int tintIndex) {
        if (tintIndex < 0 || tintIndex > 1) return -1;
        return PaintFinish.getArgbTint(stack);
    }
}
