package net.meh.cosmolib.registry;

import net.meh.cosmolib.entity.CosmeticMannequinEntity;
import net.meh.cosmolib.entity.SeatEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.meh.cosmolib.CosmoLib.MOD_ID;

public final class CosmoLibEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<SeatEntity>> SEAT =
            ENTITY_TYPES.register("seat",
                    () -> EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
                            .sized(0.001f, 0.001f)
                            .noSave()
                            .noSummon()
                            .build("seat"));

    public static final DeferredHolder<EntityType<?>, EntityType<CosmeticMannequinEntity>> COSMETIC_MANNEQUIN =
            ENTITY_TYPES.register("cosmetic_mannequin",
                    () -> EntityType.Builder.<CosmeticMannequinEntity>of(CosmeticMannequinEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.95f)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .build("cosmetic_mannequin"));

    private CosmoLibEntityTypes() {}
}
