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
 * Standard floor-placed display block. Renders via the item model's "fixed" display
 * transform using TOP_FACE mode (translate to top of the base block, then apply item
 * rotations). Default shape is a full 16×16×16 cube.
 *
 * <p>Argon equivalent: {@code FurnitureDisplayBlock} / {@code FurnitureBlock}.</p>
 */
public class FurnitureDisplayBlock extends AbstractFurnitureBlock {

    public static final MapCodec<FurnitureDisplayBlock> CODEC = simpleCodec(FurnitureDisplayBlock::new);

    /** Defaults: {@link FurnitureShape#FULL}, no fragile, no waterloggable. */
    public FurnitureDisplayBlock(BlockBehaviour.Properties props) {
        super(props, FurnitureShape.FULL, FurnitureOptions.defaults());
    }

    /** Explicit options; shape stays {@link FurnitureShape#FULL}. */
    public FurnitureDisplayBlock(BlockBehaviour.Properties props, FurnitureOptions opts) {
        super(props, FurnitureShape.FULL, opts);
    }

    /** Explicit shape; default options. */
    public FurnitureDisplayBlock(BlockBehaviour.Properties props, FurnitureShape shape) {
        super(props, shape, FurnitureOptions.defaults());
    }

    /** Full constructor. */
    public FurnitureDisplayBlock(BlockBehaviour.Properties props, FurnitureShape shape, FurnitureOptions opts) {
        super(props, shape, opts);
    }

    @Override
    public FurnitureDisplayMode getDisplayMode(BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(this);
        if (key != null) {
            String path = key.getPath();
            if (path.contains("_wall"))    return FurnitureDisplayMode.WALL;
            if (path.contains("_ceiling")) return FurnitureDisplayMode.CEILING;
            if (path.contains("_side"))    return FurnitureDisplayMode.FLOOR;
        }
        return FurnitureDisplayMode.TOP_FACE;
    }

    @Override
    protected MapCodec<? extends FurnitureDisplayBlock> codec() {
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
