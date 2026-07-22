package net.meh.cosmolib.furniture.block;

import com.mojang.serialization.MapCodec;
import net.meh.cosmolib.furniture.FurnitureOptions;
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
 * Two-block-tall floor-or-ceiling furniture. DOWN placement redirects to a ceiling
 * variant; shape spans 2 blocks tall.
 */
public class TwoTallFloorCeilingFurnitureBlock extends FloorCeilingFurnitureBlock {

    public static final MapCodec<TwoTallFloorCeilingFurnitureBlock> CODEC =
            simpleCodec(p -> new TwoTallFloorCeilingFurnitureBlock(p, () -> null));

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public TwoTallFloorCeilingFurnitureBlock(BlockBehaviour.Properties props,
                                              Supplier<Block> ceilingVariant) {
        this(props, FurnitureOptions.defaults(), ceilingVariant);
    }

    public TwoTallFloorCeilingFurnitureBlock(BlockBehaviour.Properties props, FurnitureOptions opts,
                                              Supplier<Block> ceilingVariant) {
        super(props, opts, ceilingVariant);
    }

    @Override
    protected MapCodec<? extends TwoTallFloorCeilingFurnitureBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }
}
