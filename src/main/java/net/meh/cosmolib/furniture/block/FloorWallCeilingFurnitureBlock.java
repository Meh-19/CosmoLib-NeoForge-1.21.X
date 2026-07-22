package net.meh.cosmolib.furniture.block;

import com.mojang.serialization.MapCodec;
import net.meh.cosmolib.furniture.FurnitureOptions;
import net.meh.cosmolib.furniture.FurnitureShape;
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
 * Floor, wall, or ceiling furniture block. DOWN face → ceiling variant; horizontal
 * face → wall variant; UP face → floor (this block).
 */
public class FloorWallCeilingFurnitureBlock extends AbstractFurnitureBlock {

    private final Supplier<Block> wallVariant;
    private final Supplier<Block> ceilingVariant;

    public FloorWallCeilingFurnitureBlock(BlockBehaviour.Properties props,
                                           Supplier<Block> wallVariant,
                                           Supplier<Block> ceilingVariant) {
        this(props, FurnitureShape.FULL, FurnitureOptions.defaults(), wallVariant, ceilingVariant);
    }

    public FloorWallCeilingFurnitureBlock(BlockBehaviour.Properties props, FurnitureShape shape,
                                           Supplier<Block> wallVariant,
                                           Supplier<Block> ceilingVariant) {
        this(props, shape, FurnitureOptions.defaults(), wallVariant, ceilingVariant);
    }

    public FloorWallCeilingFurnitureBlock(BlockBehaviour.Properties props, FurnitureOptions opts,
                                           Supplier<Block> wallVariant,
                                           Supplier<Block> ceilingVariant) {
        this(props, FurnitureShape.FULL, opts, wallVariant, ceilingVariant);
    }

    public FloorWallCeilingFurnitureBlock(BlockBehaviour.Properties props, FurnitureShape shape,
                                           FurnitureOptions opts,
                                           Supplier<Block> wallVariant,
                                           Supplier<Block> ceilingVariant) {
        super(props, shape, opts);
        this.wallVariant     = wallVariant;
        this.ceilingVariant  = ceilingVariant;
    }

    @Override
    protected MapCodec<? extends FloorWallCeilingFurnitureBlock> codec() {
        throw new UnsupportedOperationException("FloorWallCeilingFurnitureBlock has no standalone codec");
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction face = ctx.getClickedFace();
        if (face == Direction.DOWN) {
            int rot = Math.floorMod(Math.round(ctx.getRotation() / 45.0f) + 4, 8);
            return ceilingVariant.get().defaultBlockState().setValue(ROTATION, rot).setValue(WATERLOGGED, false);
        } else if (face.getAxis().isHorizontal()) {
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
