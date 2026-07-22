package net.meh.cosmolib.furniture.block;

import com.mojang.serialization.MapCodec;
import net.meh.cosmolib.furniture.FurnitureOptions;
import net.meh.cosmolib.furniture.FurnitureShape;
import net.meh.cosmolib.registry.CosmoLibBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Block-up decoration: renders the item model translated to the top face of the block
 * (y=1.0), oriented the same as FLOOR mode. Used for items that appear to sit above the
 * block they occupy — e.g. wall tarps or ceiling-hung decorations placed from below.
 *
 * <p>Argon equivalent: {@code BlockUpDecorationBlock}.</p>
 */
public class BlockUpFurnitureBlock extends AbstractFurnitureBlock {

    public static final MapCodec<BlockUpFurnitureBlock> CODEC = simpleCodec(BlockUpFurnitureBlock::new);

    /** Defaults: {@link FurnitureShape#FULL}, no fragile, no waterloggable. */
    public BlockUpFurnitureBlock(BlockBehaviour.Properties props) {
        super(props, FurnitureShape.FULL, FurnitureOptions.defaults());
    }

    /** Explicit options; shape stays {@link FurnitureShape#FULL}. */
    public BlockUpFurnitureBlock(BlockBehaviour.Properties props, FurnitureOptions opts) {
        super(props, FurnitureShape.FULL, opts);
    }

    /** Explicit shape; default options. */
    public BlockUpFurnitureBlock(BlockBehaviour.Properties props, FurnitureShape shape) {
        super(props, shape, FurnitureOptions.defaults());
    }

    /** Full constructor. */
    public BlockUpFurnitureBlock(BlockBehaviour.Properties props, FurnitureShape shape, FurnitureOptions opts) {
        super(props, shape, opts);
    }

    @Override
    public FurnitureDisplayMode getDisplayMode(BlockState state) {
        // Auto-detect ceiling blocks by registry path so that blocks registered
        // as BlockUpFurnitureBlock but named "*_ceiling" use the CEILING renderer
        // (ItemDisplayContext.NONE + dedicated item model) instead of BLOCK_UP.
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(this);
        if (key != null && key.getPath().contains("_ceiling")) {
            return FurnitureDisplayMode.CEILING;
        }
        return FurnitureDisplayMode.BLOCK_UP;
    }

    @Override
    protected MapCodec<? extends BlockUpFurnitureBlock> codec() {
        return CODEC;
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
