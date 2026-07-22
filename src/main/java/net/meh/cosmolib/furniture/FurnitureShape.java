package net.meh.cosmolib.furniture;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Describes the hitbox (outline + collision) for a furniture block.
 *
 * <h3>Design note — forward compatibility</h3>
 * This is a plain class rather than an enum so that future fields can be added
 * (e.g. a multi-block footprint list, a rotation strategy, neighbour offsets)
 * without changing the constructor signature visible to dependent mods.  When
 * that time comes, add the new field with a sensible default and a builder
 * method; existing callers that use the static presets or {@link #of} will
 * continue to compile and behave identically.
 *
 * <h3>Presets</h3>
 * All presets use the block's local coordinate space (0–16 on each axis).
 * Shapes are rotation-invariant: the visual model handles orientation; the
 * hitbox is an intentional approximation.  Rotation-aware shapes can be
 * implemented by overriding {@code getShape(BlockState)} in the block subclass.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // In a dependent mod — pass a preset:
 * new DecorationBlock(props, FurnitureShape.FLAT)
 * new SittableBlock(props, FurnitureShape.STOOL)
 *
 * // Custom shape:
 * new DecorationBlock(props, FurnitureShape.of(Block.box(3, 0, 3, 13, 12, 13)))
 * }</pre>
 */
public final class FurnitureShape {

    // -----------------------------------------------------------------------
    // Preset shapes
    // -----------------------------------------------------------------------

    /** No hitbox (e.g. particle effects, invisible anchors). */
    public static final FurnitureShape NONE = new FurnitureShape(Shapes.empty());

    /** Full block — solid 16×16×16 cube. */
    public static final FurnitureShape FULL = new FurnitureShape(Shapes.block());

    /** Flat rug — 1 px tall. */
    public static final FurnitureShape FLAT = new FurnitureShape(Block.box(0, 0, 0, 16, 1, 16));

    /** Low object (floor mat, coaster) — 4 px tall. */
    public static final FurnitureShape LOW = new FurnitureShape(Block.box(0, 0, 0, 16, 4, 16));

    /** Half-height slab — 8 px tall. */
    public static final FurnitureShape SLAB = new FurnitureShape(Block.box(0, 0, 0, 16, 8, 16));

    /** Stool / pedestal — slightly inset, 10 px tall. */
    public static final FurnitureShape STOOL = new FurnitureShape(Block.box(2, 0, 2, 14, 10, 14));

    /**
     * Chair bounding box — covers a typical seat + back silhouette.
     * Rotation-invariant approximation; the visual model conveys orientation.
     */
    public static final FurnitureShape CHAIR = new FurnitureShape(
            Shapes.or(
                    Block.box(1,  0,  1, 15,  9, 15),   // seat
                    Block.box(1,  9, 10, 15, 16, 12)    // backrest (south-facing default)
            )
    );

    /** Narrow column — lamp post, torch stand, 6×16×6. */
    public static final FurnitureShape COLUMN = new FurnitureShape(Block.box(5, 0, 5, 11, 16, 11));

    /** Thin ceiling mount — flush with the ceiling (y 15–16). */
    public static final FurnitureShape CEILING_MOUNT =
            new FurnitureShape(Block.box(0, 15, 0, 16, 16, 16));

    /** Hanging ceiling lamp — ceiling plate + pendant body. */
    public static final FurnitureShape CEILING_LAMP = new FurnitureShape(
            Shapes.or(
                    Block.box(0,  15, 0, 16, 16, 16),   // ceiling plate
                    Block.box(5,   6, 5, 11, 15, 11)    // pendant
            )
    );

    /**
     * Wall-mounted shelf — thin slab projecting from one face.
     * Default faces the SOUTH side (z 0–4).  Override {@code getShape(BlockState)}
     * in {@code WallFurnitureBlock} for rotation-aware behaviour.
     */
    public static final FurnitureShape SHELF =
            new FurnitureShape(Block.box(0, 5, 0, 16, 11, 4));

    /**
     * Thin wall art / picture frame — nearly flush with one face.
     * Default faces the SOUTH side (z 0–2).
     */
    public static final FurnitureShape WALL_ART =
            new FurnitureShape(Block.box(1, 1, 0, 15, 15, 2));

    // -----------------------------------------------------------------------
    // Instance
    // -----------------------------------------------------------------------

    private final VoxelShape shape;

    /** Private — use a preset or {@link #of}. */
    private FurnitureShape(VoxelShape shape) {
        this.shape = shape;
    }

    // -----------------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------------

    /**
     * Wrap a custom {@link VoxelShape}.
     *
     * <pre>{@code
     * FurnitureShape.of(Block.box(3, 0, 3, 13, 14, 13))
     * }</pre>
     */
    public static FurnitureShape of(VoxelShape shape) {
        return new FurnitureShape(shape);
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /** The raw {@link VoxelShape} used for outline and collision. */
    public VoxelShape getShape() {
        return shape;
    }

    /** {@code true} when this shape covers the full 16×16×16 block volume. */
    public boolean isFull() {
        return shape == Shapes.block();
    }

    /** {@code true} when this shape has no collision (empty). */
    public boolean isEmpty() {
        return shape == Shapes.empty();
    }
}
