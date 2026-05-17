package net.meh.cosmolib.furniture.block;

import com.mojang.serialization.MapCodec;
import net.meh.cosmolib.furniture.FurnitureOptions;
import net.meh.cosmolib.furniture.FurnitureShape;
import net.meh.cosmolib.furniture.blockentity.FurnitureBlockEntity;
import net.meh.cosmolib.registry.CosmoLibBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Wall-mounted furniture block. Rotates to face away from the wall it's placed on.
 * The ROTATION property encodes the facing direction (0=South, 2=West, 4=North, 6=East).
 */
public class WallFurnitureBlock extends AbstractFurnitureBlock {

    public static final MapCodec<WallFurnitureBlock> CODEC = simpleCodec(WallFurnitureBlock::new);

    /** Defaults: {@link FurnitureShape#FULL}, no fragile, no waterloggable. */
    public WallFurnitureBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    /** Explicit shape; no fragile, no waterloggable. */
    public WallFurnitureBlock(BlockBehaviour.Properties props, FurnitureShape shape) {
        super(props, shape);
    }

    /** {@link FurnitureShape#FULL} with explicit options. */
    public WallFurnitureBlock(BlockBehaviour.Properties props, FurnitureOptions opts) {
        super(props, opts);
    }

    /** Full constructor — shape + options. */
    public WallFurnitureBlock(BlockBehaviour.Properties props, FurnitureShape shape, FurnitureOptions opts) {
        super(props, shape, opts);
    }

    @Override
    protected MapCodec<? extends WallFurnitureBlock> codec() {
        return CODEC;
    }

    // -----------------------------------------------------------------------
    // Rotation-aware shape
    // -----------------------------------------------------------------------

    /**
     * Rotates the {@link FurnitureShape} to match the wall face stored in ROTATION.
     *
     * <p>The base shape is defined for SOUTH (rotation=4, facing south, z-axis).
     * We rotate it 90° steps using coordinate remapping so that the shape always
     * aligns with the wall the block is mounted on.</p>
     *
     * <p>Rotation values: 0=South, 2=West, 4=North, 6=East.</p>
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return rotateShape(furnitureShape.getShape(), state.getValue(ROTATION));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return rotateShape(furnitureShape.getShape(), state.getValue(ROTATION));
    }

    /**
     * Remaps a south-facing shape to match the given ROTATION index.
     *
     * <ul>
     *   <li>rot 0 → South  (no rotation needed, shape is already south-facing)</li>
     *   <li>rot 2 → West   (rotate 90° CCW)</li>
     *   <li>rot 4 → North  (rotate 180°)</li>
     *   <li>rot 6 → East   (rotate 90° CW)</li>
     *   <li>odd  → same as previous even (45° steps between; use nearest face)</li>
     * </ul>
     *
     * Coordinate remapping (all in 0–16 space, centre = 8):
     * <pre>
     *   South (default): z-depth shape
     *   West:  swap x→z, z→16-x
     *   North: x→16-x, z→16-z
     *   East:  swap x→16-z, z→x
     * </pre>
     */
    private static VoxelShape rotateShape(VoxelShape original, int rotation) {
        // Collect all AABB boxes, remap, and union them.
        // Use a holder array so the lambda can accumulate into it.
        net.minecraft.world.phys.shapes.VoxelShape[] result =
                { net.minecraft.world.phys.shapes.Shapes.empty() };

        original.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            VoxelShape rotated = switch (rotation & 6) {   // mask to even (0,2,4,6)
                case 0 ->  // South
                        net.minecraft.world.level.block.Block.box(
                                x1 * 16, y1 * 16, z1 * 16,
                                x2 * 16, y2 * 16, z2 * 16);
                case 2 ->  // West: x→z, z→16-x  (CCW 90°)
                        net.minecraft.world.level.block.Block.box(
                                (1 - z2) * 16, y1 * 16, x1 * 16,
                                (1 - z1) * 16, y2 * 16, x2 * 16);
                case 4 ->  // North: x→16-x, z→16-z  (180°)
                        net.minecraft.world.level.block.Block.box(
                                (1 - x2) * 16, y1 * 16, (1 - z2) * 16,
                                (1 - x1) * 16, y2 * 16, (1 - z1) * 16);
                case 6 ->  // East: x→16-z, z→x  (CW 90°)
                        net.minecraft.world.level.block.Block.box(
                                z1 * 16, y1 * 16, (1 - x2) * 16,
                                z2 * 16, y2 * 16, (1 - x1) * 16);
                default -> net.minecraft.world.level.block.Block.box(
                        x1 * 16, y1 * 16, z1 * 16,
                        x2 * 16, y2 * 16, z2 * 16);
            };
            result[0] = net.minecraft.world.phys.shapes.Shapes.or(result[0], rotated);
        });

        return result[0];
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getClickedFace();
        int rot = switch (facing) {
            case NORTH -> 0;
            case WEST  -> 2;
            case SOUTH -> 4;
            case EAST  -> 6;
            default    -> 0;
        };
        boolean inWater = waterloggable
                && ctx.getLevel().getFluidState(ctx.getClickedPos()).is(net.minecraft.tags.FluidTags.WATER);
        return defaultBlockState()
                .setValue(ROTATION, rot)
                .setValue(WATERLOGGED, inWater);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return CosmoLibBlockEntityTypes.FURNITURE_ENTITY.get().create(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }
}
