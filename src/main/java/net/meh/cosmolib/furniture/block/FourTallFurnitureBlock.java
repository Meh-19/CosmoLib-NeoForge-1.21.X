package net.meh.cosmolib.furniture.block;

import com.mojang.serialization.MapCodec;
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
 * Four-block-tall floor/wall/ceiling furniture. Redirects wall placements to the
 * wall variant and DOWN placements to the ceiling variant; shape spans 4 blocks tall.
 */
public class FourTallFurnitureBlock extends FloorWallCeilingFurnitureBlock {

    public static final MapCodec<FourTallFurnitureBlock> CODEC =
            simpleCodec(p -> new FourTallFurnitureBlock(p, () -> null, () -> null));

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public FourTallFurnitureBlock(BlockBehaviour.Properties props,
                                   Supplier<Block> wallVariant,
                                   Supplier<Block> ceilingVariant) {
        this(props, FurnitureOptions.defaults(), wallVariant, ceilingVariant);
    }

    public FourTallFurnitureBlock(BlockBehaviour.Properties props, FurnitureOptions opts,
                                   Supplier<Block> wallVariant,
                                   Supplier<Block> ceilingVariant) {
        super(props, FurnitureShape.FULL, opts, wallVariant, ceilingVariant);
    }

    @Override
    protected MapCodec<? extends FourTallFurnitureBlock> codec() {
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
