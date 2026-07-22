package net.meh.cosmolib.furniture.block;

/**
 * Controls which pre-transforms the block-entity renderer applies.
 *
 * <ul>
 *   <li>{@link #TOP_FACE}  — item sits flat on the TOP face of its block.</li>
 *   <li>{@link #FLOOR}     — item sits on the floor (same as TOP_FACE but at y=0).</li>
 *   <li>{@link #WALL}      — item is mounted flat against a wall surface.</li>
 *   <li>{@link #BLOCK_UP}  — item lies flat against the ceiling (attached to the
 *                             underside of the block above).</li>
 *   <li>{@link #CEILING}   — item hangs DOWN from the ceiling surface (y=16 = ceiling
 *                             attachment, y=0 = bottom of block, y&lt;0 = below block).
 *                             No extra X/Z rotations; only Y-axis orientation is applied.
 *                             Uses {@code ItemDisplayContext.NONE} so the model's own
 *                             "fixed" display transform is ignored.</li>
 * </ul>
 */
public enum FurnitureDisplayMode {
    TOP_FACE,
    FLOOR,
    WALL,
    BLOCK_UP,
    CEILING
}
