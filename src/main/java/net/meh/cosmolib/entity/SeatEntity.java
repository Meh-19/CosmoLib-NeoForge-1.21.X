package net.meh.cosmolib.entity;

import net.meh.cosmolib.registry.CosmoLibEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Invisible entity used as a riding vehicle so players can sit on furniture.
 * Auto-discards when it has no passengers.
 */
public class SeatEntity extends Entity {

    public SeatEntity(EntityType<SeatEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setInvisible(true);
    }

    public SeatEntity(Level level, BlockPos pos, float heightOffset) {
        this(CosmoLibEntityTypes.SEAT.get(), level);
        setPos(pos.getX() + 0.5, pos.getY() + heightOffset, pos.getZ() + 0.5);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && getPassengers().isEmpty()) {
            discard();
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(net.minecraft.world.entity.LivingEntity passenger) {
        return new Vec3(getX(), getY() + 0.5, getZ());
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
