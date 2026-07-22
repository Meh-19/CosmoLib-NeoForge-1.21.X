package net.meh.cosmolib.cosmetic.offset;

/**
 * Immutable data class holding the Y-axis translation offset for a back cosmetic.
 *
 * <p>Loaded from and persisted to {@code config/cosmolib/back_offsets.json} by
 * {@link BackOffsetManager}.  Cosmetics absent from the file fall back to
 * {@link #DEFAULT_Y}, which matches the hardcoded value that was previously in
 * {@link net.meh.cosmolib.cosmetic.client.CosmeticPlayerLayer}.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * double y = BackOffsetManager.getY(cosmeticId);   // read
 * BackOffsetData data = BackOffsetData.of(y);      // wrap if needed
 * }</pre>
 */
public final class BackOffsetData {

    /**
     * Fallback Y offset — matches the original hardcoded value in
     * {@code CosmeticPlayerLayer.renderBack()}.  Any cosmetic with no entry in
     * {@code back_offsets.json} renders identically to before the system was added.
     */
    public static final double DEFAULT_Y = -0.57625;

    private final double yOffset;

    private BackOffsetData(double yOffset) {
        this.yOffset = yOffset;
    }

    /**
     * Creates a {@link BackOffsetData} with the given Y offset.
     *
     * @param y the Y translation to apply in {@code renderBack}
     * @return a new instance
     */
    public static BackOffsetData of(double y) {
        return new BackOffsetData(y);
    }

    /**
     * Creates a {@link BackOffsetData} using the {@link #DEFAULT_Y} fallback value.
     * Equivalent to {@code BackOffsetData.of(DEFAULT_Y)}.
     *
     * @return an instance with the default Y offset
     */
    public static BackOffsetData defaults() {
        return new BackOffsetData(DEFAULT_Y);
    }

    /**
     * Returns the Y translation offset for use in
     * {@link net.meh.cosmolib.cosmetic.client.CosmeticPlayerLayer}.
     *
     * @return the Y offset in block units
     */
    public double getYOffset() {
        return yOffset;
    }
}
