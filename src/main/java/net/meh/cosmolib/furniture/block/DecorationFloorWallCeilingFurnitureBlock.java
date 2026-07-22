package net.meh.cosmolib.furniture.block;

import net.meh.cosmolib.furniture.FurnitureOptions;
import net.meh.cosmolib.furniture.FurnitureShape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * Floor/wall/ceiling rug or hide block.
 *
 * <p>Shape is flat (1 px) with no collision, displays in FLOOR mode so the BER
 * lays the item flat on the surface. Matches Argon's DecorationFloorWallCeilingFurnitureBlock.</p>
 */
public class DecorationFloorWallCeilingFurnitureBlock extends FloorWallCeilingFurnitureBlock {

    private static final VoxelShape FLAT = Block.box(0, 0, 0, 16, 1, 16);

    public DecorationFloorWallCeilingFurnitureBlock(BlockBehaviour.Properties props,
                                                     Supplier<Block> wallVariant,
                                                     Supplier<Block> ceilingVariant) {
        super(props, FurnitureShape.FLAT, FurnitureOptions.defaults(), wallVariant, ceilingVariant);
    }

    public DecorationFloorWallCeilingFurnitureBlock(BlockBehaviour.Properties props,
                                                     FurnitureOptions opts,
                                                     Supplier<Block> wallVariant,
                                                     Supplier<Block> ceilingVariant) {
        super(props, FurnitureShape.FLAT, opts, wallVariant, ceilingVariant);
    }

    @Override
    public FurnitureDisplayMode getDisplayMode(BlockState state) {
        return FurnitureDisplayMode.FLOOR;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return FLAT;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }
}
