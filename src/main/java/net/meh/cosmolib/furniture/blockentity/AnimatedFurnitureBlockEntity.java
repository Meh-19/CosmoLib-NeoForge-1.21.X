package net.meh.cosmolib.furniture.blockentity;

import net.meh.cosmolib.registry.CosmoLibBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Block entity for animated furniture using GeckoLib.
 *
 * Register animation names before world load via
 * {@link #registerAnimation(ResourceLocation, String)}.
 */
public class AnimatedFurnitureBlockEntity extends FurnitureBlockEntity implements GeoBlockEntity {

    private static final Map<ResourceLocation, String> ANIMATIONS = new ConcurrentHashMap<>();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AnimatedFurnitureBlockEntity(BlockPos pos, BlockState state) {
        super(CosmoLibBlockEntityTypes.ANIMATED_FURNITURE_ENTITY.get(), pos, state);
    }

    /** Map a block's registry ID to its looping animation name. */
    public static void registerAnimation(ResourceLocation blockId, String animationName) {
        ANIMATIONS.put(blockId, animationName);
    }

    protected String getAnimationName() {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock());
        return ANIMATIONS.getOrDefault(id, "idle");
    }

    // ------------------------------------------------------------------
    // GeoBlockEntity
    // ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "main", 0, state -> {
            state.getController().setAnimation(
                    RawAnimation.begin().thenLoop(getAnimationName()));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ------------------------------------------------------------------
    // Client-side tick (called by AnimatedFurnitureBlock.getTicker)
    // ------------------------------------------------------------------
    public static void clientTick(Level level, BlockPos pos, BlockState state,
                                   AnimatedFurnitureBlockEntity entity) {
        // GeckoLib drives animation from the renderer; no extra work needed here.
    }
}
