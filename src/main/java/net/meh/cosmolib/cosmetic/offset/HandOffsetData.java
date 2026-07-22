package net.meh.cosmolib.cosmetic.offset;

/**
 * Immutable X/Y/Z translation + rotation offset for a hand cosmetic on the mannequin's arm bone.
 *
 * <p>Loaded from and persisted to {@code config/cosmolib/hand_offsets.json} by
 * {@link HandOffsetManager}.  Cosmetics absent from the file use the defaults.
 *
 * <h3>Coordinate space</h3>
 * All values are in GeckoLib's Y-up bone-local space, applied at the arm pivot
 * <em>before</em> the axis-flip scale that converts to vanilla Y-down rendering.
 * Translation: positive Y = up, positive Z = entity forward, positive X = entity right.
 * Rotation: applied X → Y → Z (degrees) after translation, before scale.
 */
public final class HandOffsetData {

    public static final double DEFAULT_X    = 0.0;
    /** Compensates for mannequin arm pivot sitting ~0.425 blocks below a vanilla player's. */
    public static final double DEFAULT_Y    = 0.425;
    public static final double DEFAULT_Z    = 0.0;
    public static final double DEFAULT_ROTX = 0.0;
    public static final double DEFAULT_ROTY = 0.0;
    public static final double DEFAULT_ROTZ = 0.0;

    /** X translation in block units. */
    public final double x;
    /** Y translation in block units. */
    public final double y;
    /** Z translation in block units. */
    public final double z;
    /** Rotation around local X axis, in degrees (applied first). */
    public final double rotX;
    /** Rotation around local Y axis, in degrees (applied second). */
    public final double rotY;
    /** Rotation around local Z axis, in degrees (applied third). */
    public final double rotZ;

    public HandOffsetData(double x, double y, double z,
                          double rotX, double rotY, double rotZ) {
        this.x    = x;    this.y    = y;    this.z    = z;
        this.rotX = rotX; this.rotY = rotY; this.rotZ = rotZ;
    }

    /** Returns a {@link HandOffsetData} using all built-in defaults. */
    public static HandOffsetData defaults() {
        return new HandOffsetData(DEFAULT_X, DEFAULT_Y, DEFAULT_Z,
                                  DEFAULT_ROTX, DEFAULT_ROTY, DEFAULT_ROTZ);
    }
}
