package net.meh.cosmolib.furniture.block;

import com.mojang.serialization.MapCodec;
import net.meh.cosmolib.furniture.FurnitureOptions;
import net.meh.cosmolib.furniture.blockentity.AnimatedFurnitureBlockEntity;
import net.meh.cosmolib.registry.CosmoLibBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A furniture block with a GeckoLib animation.
 *
 * Register the animation name before the world loads (e.g. during FMLCommonSetupEvent):
 * <pre>{@code
 * AnimatedFurnitureBlock.registerAnimation(
 *     ResourceLocation.fromNamespaceAndPath("mymod", "my_clock"),
 *     "clock_ticking"   // animation name in the .animation.json file
 * );
 * }</pre>
 */
public class AnimatedFurnitureBlock extends AbstractFurnitureBlock {

    public static final MapCodec<AnimatedFurnitureBlock> CODEC = simpleCodec(AnimatedFurnitureBlock::new);

    /** Defaults: {@link net.meh.cosmolib.furniture.FurnitureShape#FULL}, no fragile, no waterloggable. */
    public AnimatedFurnitureBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    /** {@link net.meh.cosmolib.furniture.FurnitureShape#FULL} with explicit options. */
    public AnimatedFurnitureBlock(BlockBehaviour.Properties props, FurnitureOptions opts) {
        super(props, opts);
    }

    @Override
    protected MapCodec<? extends AnimatedFurnitureBlock> codec() {
        return CODEC;
    }

    /** Register the default looping animation name for a block. */
    public static void registerAnimation(ResourceLocation blockId, String animationName) {
        AnimatedFurnitureBlockEntity.registerAnimation(blockId, animationName);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return CosmoLibBlockEntityTypes.ANIMATED_FURNITURE_ENTITY.get().create(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return createTickerHelper(type,
                    CosmoLibBlockEntityTypes.ANIMATED_FURNITURE_ENTITY.get(),
                    AnimatedFurnitureBlockEntity::clientTick);
        }
        return null;
    }
}
