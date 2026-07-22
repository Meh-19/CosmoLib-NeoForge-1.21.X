package net.meh.cosmolib.cosmetic;

import net.meh.cosmolib.paint.FinishType;
import net.meh.cosmolib.paint.PaintColor;
import net.meh.cosmolib.paint.PaintData;
import net.meh.cosmolib.paint.PaintFinish;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Describes the default paint appearance that a {@link CosmeticItem} starts with
 * when a new stack is created.  Holds either a solid paint colour (with shade) or
 * an animated finish — never both.
 *
 * <p>Create instances via the static factory methods:
 * <pre>{@code
 * // Shade 1–7 maps directly to the seven shades in each PaintColor
 * CosmeticDefault.color(PaintColor.BLUE, 3)
 * CosmeticDefault.finish(FinishType.RAINBOW)
 * }</pre>
 */
public final class CosmeticDefault {

    @Nullable private final PaintColor color;
    private final int                  shadeIndex; // 0-based internally
    @Nullable private final FinishType finish;

    private CosmeticDefault(@Nullable PaintColor color, int shadeIndex, @Nullable FinishType finish) {
        this.color      = color;
        this.shadeIndex = shadeIndex;
        this.finish     = finish;
    }

    // ------------------------------------------------------------------
    // Factories
    // ------------------------------------------------------------------

    /**
     * A solid paint colour default.
     *
     * @param color  one of the nine {@link PaintColor} values
     * @param shade  shade number from 1 (lightest) to 7 (darkest), matching
     *               the painting table's column order
     */
    public static CosmeticDefault color(PaintColor color, int shade) {
        if (shade < 1 || shade > 7) throw new IllegalArgumentException("shade must be 1–7, got " + shade);
        return new CosmeticDefault(color, shade - 1, null);
    }

    /**
     * An animated finish default.
     *
     * @param finish one of the {@link FinishType} values (e.g. RAINBOW, GOLD, …)
     */
    public static CosmeticDefault finish(FinishType finish) {
        return new CosmeticDefault(null, 0, finish);
    }

    // ------------------------------------------------------------------
    // Application
    // ------------------------------------------------------------------

    /**
     * Writes this default appearance into {@code stack}'s paint data,
     * replacing any existing paint value.
     */
    public void applyTo(ItemStack stack) {
        if (color != null) {
            PaintData.applyColor(stack, color.getShade(shadeIndex));
        } else if (finish != null) {
            PaintFinish.setFinish(stack, finish);
        }
    }

    // ------------------------------------------------------------------
    // Raw value (used to set default Item components)
    // ------------------------------------------------------------------

    /**
     * Returns the raw 24-bit RGB value that {@link net.meh.cosmolib.paint.PaintData}
     * stores for this appearance — either a plain colour int or a finish magic-RGB.
     * Use this when you need the value without an {@link net.minecraft.world.item.ItemStack}.
     */
    public int getRawRgb() {
        if (color != null) {
            return color.getShade(shadeIndex) & 0xFFFFFF;
        }
        assert finish != null;
        return finish.getMagicRgb();
    }

    // ------------------------------------------------------------------
    // Accessors (for introspection if needed)
    // ------------------------------------------------------------------

    /** Returns the {@link PaintColor}, or {@code null} if this is a finish default. */
    @Nullable
    public PaintColor getColor() { return color; }

    /** Returns the shade index (0-based), only meaningful when {@link #getColor()} is non-null. */
    public int getShadeIndex() { return shadeIndex; }

    /** Returns the {@link FinishType}, or {@code null} if this is a color default. */
    @Nullable
    public FinishType getFinish() { return finish; }

    public boolean isFinish() { return finish != null; }
}
