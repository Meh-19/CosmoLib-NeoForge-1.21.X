package net.meh.cosmolib.furniture.block;

import com.mojang.serialization.MapCodec;
import net.meh.cosmolib.furniture.FurnitureOptions;
import net.meh.cosmolib.furniture.FurnitureShape;
import net.meh.cosmolib.furniture.blockentity.FurnitureBlockEntity;
import net.meh.cosmolib.registry.CosmoLibBlockEntityTypes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Standard floor/ceiling decoration block backed by a {@link FurnitureBlockEntity}.
 * Place on any surface, rotates with player yaw, supports paint tinting.
 *
 * Usage in a dependent mod:
 * <pre>{@code
 * public static final DeferredBlock<DecorationBlock> MY_VASE =
 *     BLOCKS.register("my_vase", () -> new DecorationBlock(BlockBehaviour.Properties.of()
 *         .strength(1.5f).noOcclusion()));
 *
 * // Register block entity type referencing this block:
 * public static final Supplier<BlockEntityType<FurnitureBlockEntity>> MY_VASE_BE =
 *     BLOCK_ENTITY_TYPES.register("my_vase",
 *         () -> BlockEntityType.Builder.of(FurnitureBlockEntity::new, MY_VASE.get()).build(null));
 *
 * // Register renderer (client setup):
 * event.registerBlockEntityRenderer(MY_VASE_BE.get(), ctx -> new FurnitureBlockEntityRenderer(ctx));
 * }</pre>
 */
public class DecorationBlock extends AbstractFurnitureBlock {

    public static final MapCodec<DecorationBlock> CODEC = simpleCodec(DecorationBlock::new);

    /** Defaults: {@link FurnitureShape#FULL}, no fragile, no waterloggable. */
    public DecorationBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    /** Explicit shape; no fragile, no waterloggable. */
    public DecorationBlock(BlockBehaviour.Properties props, FurnitureShape shape) {
        super(props, shape);
    }

    /** {@link FurnitureShape#FULL} with explicit options. */
    public DecorationBlock(BlockBehaviour.Properties props, FurnitureOptions opts) {
        super(props, opts);
    }

    /** Full constructor — shape + options. */
    public DecorationBlock(BlockBehaviour.Properties props, FurnitureShape shape, FurnitureOptions opts) {
        super(props, shape, opts);
    }

    @Override
    protected MapCodec<? extends DecorationBlock> codec() {
        return CODEC;
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
