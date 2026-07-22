package net.meh.cosmolib.furniture.block;

import com.mojang.serialization.MapCodec;
import net.meh.cosmolib.furniture.FurnitureOptions;
import net.meh.cosmolib.furniture.blockentity.AnimatedFurnitureBlockEntity;
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
 * Animated floor-or-wall furniture block. Placing on a horizontal face redirects
 * to a separate wall-mounted variant; GeckoLib animation is ticked on the client.
 */
public class AnimatedFloorWallFurnitureBlock extends AnimatedFurnitureBlock {

    private final Supplier<Block> wallVariant;

    public AnimatedFloorWallFurnitureBlock(BlockBehaviour.Properties props, Supplier<Block> wallVariant) {
        this(props, FurnitureOptions.defaults(), wallVariant);
    }

    public AnimatedFloorWallFurnitureBlock(BlockBehaviour.Properties props, FurnitureOptions opts,
                                            Supplier<Block> wallVariant) {
        super(props, opts);
        this.wallVariant = wallVariant;
    }

    @Override
    protected MapCodec<? extends AnimatedFloorWallFurnitureBlock> codec() {
        throw new UnsupportedOperationException("AnimatedFloorWallFurnitureBlock has no standalone codec");
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
        return CosmoLibBlockEntityTypes.ANIMATED_FURNITURE_ENTITY.get().create(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            BlockEntityType<AnimatedFurnitureBlockEntity> animType = beTypeSupplier != null
                    ? (BlockEntityType<AnimatedFurnitureBlockEntity>) beTypeSupplier.get()
                    : CosmoLibBlockEntityTypes.ANIMATED_FURNITURE_ENTITY.get();
            return createTickerHelper(type, animType, AnimatedFurnitureBlockEntity::clientTick);
        }
        return null;
    }
}
