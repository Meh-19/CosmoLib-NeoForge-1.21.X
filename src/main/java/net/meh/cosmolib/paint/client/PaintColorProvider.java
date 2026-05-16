package net.meh.cosmolib.paint.client;

import net.meh.cosmolib.paint.PaintFinish;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;

/**
 * Register this for any item that supports paint tinting.
 * tintIndex 0 = base texture (no tint), tintIndex 1 = paint layer.
 */
public final class PaintColorProvider implements ItemColor {

    public static final PaintColorProvider INSTANCE = new PaintColorProvider();

    private PaintColorProvider() {}

    @Override
    public int getColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 1) return -1;
        return PaintFinish.getArgbTint(stack);
    }
}
