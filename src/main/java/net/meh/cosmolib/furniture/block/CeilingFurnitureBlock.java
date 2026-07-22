package net.meh.cosmolib.furniture.block;

import com.mojang.serialization.MapCodec;
import net.meh.cosmolib.furniture.FurnitureOptions;
import net.meh.cosmolib.furniture.FurnitureShape;
import net.meh.cosmolib.registry.CosmoLibBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ceiling-only furniture block. Can only be placed on the underside of a solid block;
 * returns {@code null} from {@link #getStateForPlacement} for any other face.
 */
public class CeilingFurnitureBlock extends AbstractFurnitureBlock {

    public static final MapCodec<CeilingFurnitureBlock> CODEC =
            simpleCodec(CeilingFurnitureBlock::new);

    public CeilingFurnitureBlock(BlockBehaviour.Properties props) {
        this(props, FurnitureShape.FULL, FurnitureOptions.defaults());
    }

    public CeilingFurnitureBlock(BlockBehaviour.Properties props, FurnitureShape shape) {
        this(props, shape, FurnitureOptions.defaults());
    }

    public CeilingFurnitureBlock(BlockBehaviour.Properties props, FurnitureOptions opts) {
        this(props, FurnitureShape.FULL, opts);
    }

    public CeilingFurnitureBlock(BlockBehaviour.Properties props, FurnitureShape shape, FurnitureOptions opts) {
        super(props, shape, opts);
    }

    @Override
    protected MapCodec<? extends CeilingFurnitureBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return ctx.getClickedFace() == Direction.DOWN ? super.getStateForPlacement(ctx) : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (beTypeSupplier != null) return beTypeSupplier.get().create(pos, state);
        return CosmoLibBlockEntityTypes.FURNITURE_ENTITY.get().create(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }
}
