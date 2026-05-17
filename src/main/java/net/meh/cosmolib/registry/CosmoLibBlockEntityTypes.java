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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FurnitureBlockEntity>>
            FURNITURE_ENTITY = BLOCK_ENTITY_TYPES.register("furniture_entity",
            () -> BlockEntityType.Builder
                    .of(FurnitureBlockEntity::new, CosmoLibBlocks.COSMO_RUG.get())
                    .build(null));

    /**
     * Animated furniture block entity — used by {@link net.meh.cosmolib.furniture.block.AnimatedFurnitureBlock}.
     * All animated furniture blocks must be listed here.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnimatedFurnitureBlockEntity>>
            ANIMATED_FURNITURE_ENTITY = BLOCK_ENTITY_TYPES.register("animated_furniture_entity",
            () -> BlockEntityType.Builder
                    .of(AnimatedFurnitureBlockEntity::new,
                            CosmoLibBlocks.AGED_FLAG.get())
                    .build(null));

    private CosmoLibBlockEntityTypes() {}
}
