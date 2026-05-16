package net.meh.cosmolib.registry;

import net.meh.cosmolib.furniture.blockentity.AnimatedFurnitureBlockEntity;
import net.meh.cosmolib.furniture.blockentity.FurnitureBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.meh.cosmolib.CosmoLib.MOD_ID;

public final class CosmoLibBlockEntityTypes {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);

    /**
     * Generic static furniture block entity — used by {@link net.meh.cosmolib.furniture.block.DecorationBlock},
     * {@link net.meh.cosmolib.furniture.block.SittableBlock}, and {@link net.meh.cosmolib.furniture.block.WallFurnitureBlock}.
     *
     * Dependent mods may also register their own types that use {@link FurnitureBlockEntity} as the impl.
     */
    /**
     * Pass no blocks here — dependent mods register their own block entity types
     * using FurnitureBlockEntity::new and list their specific blocks there.
     * This type is used as a fallback by DecorationBlock/SittableBlock/WallFurnitureBlock
     * when no custom type is provided.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FurnitureBlockEntity>>
            FURNITURE_ENTITY = BLOCK_ENTITY_TYPES.register("furniture_entity",
            () -> BlockEntityType.Builder
                    .of(FurnitureBlockEntity::new)
                    .build(null));

    /**
     * Animated furniture block entity — used by {@link net.meh.cosmolib.furniture.block.AnimatedFurnitureBlock}.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnimatedFurnitureBlockEntity>>
            ANIMATED_FURNITURE_ENTITY = BLOCK_ENTITY_TYPES.register("animated_furniture_entity",
            () -> BlockEntityType.Builder
                    .of(AnimatedFurnitureBlockEntity::new)
                    .build(null));

    private CosmoLibBlockEntityTypes() {}
}
