package net.meh.cosmolib.furniture.block;

import com.mojang.serialization.MapCodec;
import net.meh.cosmolib.furniture.FurnitureOptions;
import net.meh.cosmolib.furniture.FurnitureShape;
import net.meh.cosmolib.registry.CosmoLibBlockEntityTypes;
import net.minecraft.core.BlockPos;
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
 * Floor-or-wall furniture block. Placing on a horizontal face redirects to a
 * separate wall-mounted variant.
 */
public class FloorWallFurnitureBlock extends AbstractFurnitureBlock {

    private final Supplier<Block> wallVariant;

    public FloorWallFurnitureBlock(BlockBehaviour.Properties props, Supplier<Block> wallVariant) {
        this(props, FurnitureShape.FULL, FurnitureOptions.defaults(), wallVariant);
    }

    public FloorWallFurnitureBlock(BlockBehaviour.Properties props, FurnitureShape shape,
                                    Supplier<Block> wallVariant) {
        this(props, shape, FurnitureOptions.defaults(), wallVariant);
    }

    public FloorWallFurnitureBlock(BlockBehaviour.Properties props, FurnitureOptions opts,
                                    Supplier<Block> wallVariant) {
        this(props, FurnitureShape.FULL, opts, wallVariant);
    }

    public FloorWallFurnitureBlock(BlockBehaviour.Properties props, FurnitureShape shape,
                                    FurnitureOptions opts, Supplier<Block> wallVariant) {
        super(props, shape, opts);
        this.wallVariant = wallVariant;
    }

    @Override
    protected MapCodec<? extends FloorWallFurnitureBlock> codec() {
        throw new UnsupportedOperationException("FloorWallFurnitureBlock has no standalone codec");
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (ctx.getClickedFace().getAxis().isHorizontal()) {
            return wallVariant.get().getStateForPlacement(ctx);
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
