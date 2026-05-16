package net.meh.cosmolib.furniture.block;

import com.mojang.serialization.MapCodec;
import net.meh.cosmolib.furniture.blockentity.FurnitureBlockEntity;
import net.meh.cosmolib.registry.CosmoLibBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Wall-mounted furniture block. Rotates to face away from the wall it's placed on.
 * The ROTATION property encodes the facing direction (0=South, 2=West, 4=North, 6=East).
 */
public class WallFurnitureBlock extends AbstractFurnitureBlock {

    public static final MapCodec<WallFurnitureBlock> CODEC = simpleCodec(WallFurnitureBlock::new);

    public WallFurnitureBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends WallFurnitureBlock> codec() {
        return CODEC;
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
        return defaultBlockState().setValue(ROTATION, rot).setValue(FRAGILE, false);
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
