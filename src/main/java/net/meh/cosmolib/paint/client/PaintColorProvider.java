package net.meh.cosmolib.paint.client;

import net.meh.cosmolib.paint.PaintFinish;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;

/**
 * Item colour providers for the CosmoLib paint system.
 *
 * <ul>
 *   <li>{@link #INSTANCE} — responds to tintIndex 0 <em>and</em> 1.
 *       Use for the paintbrush, finish-preview, and any simple item whose
 *       model only has a single coloured layer at index 0.</li>
 *   <li>{@link #COSMETIC} — responds to tintIndex 1 <strong>only</strong>.
 *       Use for {@link net.meh.cosmolib.cosmetic.CosmeticItem}s so that the
 *       flat inventory token sprite (layer0 → tintIndex 0) is never tinted,
 *       while the paint layer of the 3-D cosmetic model (tintIndex 1) still
 *       receives the correct paint colour.</li>
 * </ul>
 */
public final class PaintColorProvider implements ItemColor {

    /** Tints both index 0 and 1 — suitable for paintbrush / finish-preview. */
    public static final PaintColorProvider INSTANCE = new PaintColorProvider();

    /**
     * Tints index 1 only — use for {@link net.meh.cosmolib.cosmetic.CosmeticItem}.
     * Leaves index 0 un-tinted so the flat token sprite shown in the inventory
     * is not affected by the paint colour.
     */
    public static final ItemColor COSMETIC =
            (stack, tintIndex) -> tintIndex == 1 ? PaintFinish.getArgbTint(stack) : -1;

    private PaintColorProvider() {}

    @Override
    public int getColor(ItemStack stack, int tintIndex) {
        if (tintIndex < 0 || tintIndex > 1) return -1;
        return PaintFinish.getArgbTint(stack);
    }
}
