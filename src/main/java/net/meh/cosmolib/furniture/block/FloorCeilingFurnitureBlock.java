package net.meh.cosmolib.furniture.block;

import com.mojang.serialization.MapCodec;
import net.meh.cosmolib.furniture.FurnitureOptions;
import net.meh.cosmolib.furniture.FurnitureShape;
import net.meh.cosmolib.furniture.blockentity.FurnitureBlockEntity;
import net.meh.cosmolib.registry.CosmoLibBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Floor-or-ceiling furniture block. Placing on the underside of a block (DOWN face)
 * redirects to a separate ceiling-mounted variant.
 */
public class FloorCeilingFurnitureBlock extends AbstractFurnitureBlock {

    private final Supplier<Block> ceilingVariant;

    public FloorCeilingFurnitureBlock(BlockBehaviour.Properties props, Supplier<Block> ceilingVariant) {
        this(props, FurnitureShape.FULL, FurnitureOptions.defaults(), ceilingVariant);
    }

    public FloorCeilingFurnitureBlock(BlockBehaviour.Properties props, FurnitureShape shape,
                                       Supplier<Block> ceilingVariant) {
        this(props, shape, FurnitureOptions.defaults(), ceilingVariant);
    }

    public FloorCeilingFurnitureBlock(BlockBehaviour.Properties props, FurnitureOptions opts,
                                       Supplier<Block> ceilingVariant) {
        this(props, FurnitureShape.FULL, opts, ceilingVariant);
    }

    public FloorCeilingFurnitureBlock(BlockBehaviour.Properties props, FurnitureShape shape,
                                       FurnitureOptions opts, Supplier<Block> ceilingVariant) {
        super(props, shape, opts);
        this.ceilingVariant = ceilingVariant;
    }

    @Override
    protected MapCodec<? extends FloorCeilingFurnitureBlock> codec() {
        throw new UnsupportedOperationException("FloorCeilingFurnitureBlock has no standalone codec");
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (ctx.getClickedFace() == Direction.DOWN) {
            int rot = Math.floorMod(Math.round(ctx.getRotation() / 45.0f) + 4, 8);
            return ceilingVariant.get().defaultBlockState().setValue(ROTATION, rot).setValue(WATERLOGGED, false);
        }
        return super.getStateForPlacement(ctx);
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
