package net.meh.cosmolib.entity;

import net.meh.cosmolib.cosmetic.CosmeticItem;
import net.meh.cosmolib.cosmetic.CosmeticSlot;
import net.meh.cosmolib.item.PaintbrushItem;
import net.meh.cosmolib.paint.PaintData;
import net.meh.cosmolib.paint.PaintFinish;
import net.meh.cosmolib.registry.CosmoLibEntityTypes;
import net.meh.cosmolib.registry.CosmoLibItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

/**
 * A stationary display entity that holds cosmetics in HAT, BACK, and two HAND slots
 * (right arm and left arm).
 *
 * <p>Players can equip cosmetics by right-clicking the appropriate region of the mannequin,
 * and retrieve them by attacking the entity (which drops all cosmetics plus the mannequin item).
 *
 * <h3>Slot hit detection (based on {@code vec.y / 1.95f} hit fraction):</h3>
 * <ul>
 *   <li>{@code > 0.75f} → HAT</li>
 *   <li>{@code > 0.40f} → BACK</li>
 *   <li>{@code >= 0.20f} → HAND — entity-local X sign selects right vs left arm</li>
 *   <li>{@code < 0.20f} → base region, pass through</li>
 * </ul>
 */
public class CosmeticMannequinEntity extends LivingEntity implements GeoEntity {

    // ------------------------------------------------------------------
    // SynchedEntityData keys
    // ------------------------------------------------------------------

    /** Rotation index 0–7, where 0 = north, values increase clockwise in 45° steps. */
    private static final EntityDataAccessor<Integer> ROTATION =
            SynchedEntityData.defineId(CosmeticMannequinEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<ItemStack> HAT_ITEM =
            SynchedEntityData.defineId(CosmeticMannequinEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final EntityDataAccessor<ItemStack> BACK_ITEM =
            SynchedEntityData.defineId(CosmeticMannequinEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final EntityDataAccessor<ItemStack> HAND_ITEM =
            SynchedEntityData.defineId(CosmeticMannequinEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final EntityDataAccessor<ItemStack> HAND_LEFT_ITEM =
            SynchedEntityData.defineId(CosmeticMannequinEntity.class, EntityDataSerializers.ITEM_STACK);

    // ------------------------------------------------------------------
    // GeckoLib
    // ------------------------------------------------------------------

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // ------------------------------------------------------------------
    // Hit tracking — two-hit break like armor stand
    // ------------------------------------------------------------------

    /**
     * Game-time tick of the last player hit. Not saved — resets on chunk reload.
     *
     * <p>Initialised to {@code 0L} (not {@code Long.MIN_VALUE}) because
     * {@code gameTime - Long.MIN_VALUE} overflows to a large negative number, making the
     * {@code gap > 5} check fail on the very first hit and causing an immediate break.
     */
    private long lastHit = 0L;

    /**
     * Game-time tick of the last time entity event 32 was received on the client.
     * Used by the renderer to drive the wobble animation. Never saved or synced.
     */
    private long lastHitByPlayerTime = Long.MIN_VALUE;

    /** Returns the last client-side hit time for wobble rendering. */
    public long getLastHitByPlayerTime() { return lastHitByPlayerTime; }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    public CosmeticMannequinEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    // ------------------------------------------------------------------
    // SynchedEntityData
    // ------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROTATION, 0);
        builder.define(HAT_ITEM,       ItemStack.EMPTY);
        builder.define(BACK_ITEM,      ItemStack.EMPTY);
        builder.define(HAND_ITEM,      ItemStack.EMPTY);
        builder.define(HAND_LEFT_ITEM, ItemStack.EMPTY);
    }

    // ------------------------------------------------------------------
    // Rotation helpers
    // ------------------------------------------------------------------

    /** Returns the current rotation index (0–7, 0 = north, clockwise). */
    public int getRotationIndex() {
        return entityData.get(ROTATION);
    }

    public void setRotationIndex(int index) {
        int clamped = Math.floorMod(index, 8);
        float yaw = clamped * 45f;
        entityData.set(ROTATION, clamped);
        setYRot(yaw);
        yRotO    = yaw;
        yBodyRot = yaw;
        yBodyRotO = yaw;
        yHeadRot = yaw;
        yHeadRotO = yaw;
    }

    // ------------------------------------------------------------------
    // Cosmetic item accessors
    // ------------------------------------------------------------------

    public ItemStack getHatItem()      { return entityData.get(HAT_ITEM);       }
    public ItemStack getBackItem()     { return entityData.get(BACK_ITEM);      }
    public ItemStack getHandItem()     { return entityData.get(HAND_ITEM);      }
    public ItemStack getHandLeftItem() { return entityData.get(HAND_LEFT_ITEM); }

    public void setHatItem(ItemStack stack)      { entityData.set(HAT_ITEM,       stack.copy()); }
    public void setBackItem(ItemStack stack)     { entityData.set(BACK_ITEM,      stack.copy()); }
    public void setHandItem(ItemStack stack)     { entityData.set(HAND_ITEM,      stack.copy()); }
    public void setHandLeftItem(ItemStack stack) { entityData.set(HAND_LEFT_ITEM, stack.copy()); }

    /** Gets the entity-storage stack for a non-HAND slot. Use {@link #getHandItem} / {@link #getHandLeftItem} for arms. */
    private ItemStack getSlotStack(CosmeticSlot slot) {
        return switch (slot) {
            case HAT  -> getHatItem();
            case BACK -> getBackItem();
            case HAND -> getHandItem(); // fallback — arm detection handled in interactAt
        };
    }

    private void setSlotStack(CosmeticSlot slot, ItemStack stack) {
        switch (slot) {
            case HAT  -> setHatItem(stack);
            case BACK -> setBackItem(stack);
            case HAND -> setHandItem(stack);
        }
    }

    /**
     * Returns {@code true} if the world-space hit offset {@code vec} is on the mannequin's
     * anatomical right side (the arm rendered on the viewer's left).
     *
     * <p>Rotates the hit vector into entity-local space and checks the sign of the
     * local X axis (positive = entity's own right).
     */
    private boolean isClickOnRightArm(Vec3 vec) {
        float yawRad = getYRot() * (float) Math.PI / 180f;
        // Entity's own right vector is (-cos yaw, 0, -sin yaw)
        double localRight = -Math.cos(yawRad) * vec.x - Math.sin(yawRad) * vec.z;
        return localRight >= 0;
    }

    // ------------------------------------------------------------------
    // Interaction — slot detection via interactAt
    // ------------------------------------------------------------------

    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;

        ItemStack heldStack = player.getItemInHand(hand);

        // ── Vertical zone ────────────────────────────────────────────────
        float yFraction = (float) (vec.y / 1.95);
        if (yFraction < 0.2f) return InteractionResult.PASS; // base / feet

        CosmeticSlot targetSlot;
        boolean leftArm = false; // only meaningful when targetSlot == HAND

        if (yFraction > 0.75f) {
            targetSlot = CosmeticSlot.HAT;
        } else if (yFraction > 0.40f) {
            targetSlot = CosmeticSlot.BACK;
        } else {
            targetSlot = CosmeticSlot.HAND;
            leftArm = !isClickOnRightArm(vec);
        }

        // ── Storage lookup ───────────────────────────────────────────────
        ItemStack currentInSlot = (targetSlot == CosmeticSlot.HAND)
                ? (leftArm ? getHandLeftItem() : getHandItem())
                : getSlotStack(targetSlot);

        // ── Paint — paintbrush applies its loaded colour TO the cosmetic ─
        if (!player.isCrouching() && heldStack.getItem() instanceof PaintbrushItem) {
            int color = PaintbrushItem.getPaintColor(heldStack);
            if (color >= 0 && !currentInSlot.isEmpty()
                    && currentInSlot.getItem() instanceof CosmeticItem ci && ci.isPaintable()) {

                ItemStack painted = currentInSlot.copy();
                PaintData.applyColor(painted, color);

                if (targetSlot == CosmeticSlot.HAND) {
                    if (leftArm) setHandLeftItem(painted);
                    else         setHandItem(painted);
                } else {
                    setSlotStack(targetSlot, painted);
                }

                level().playSound(null, getX(), getY(), getZ(),
                        net.minecraft.sounds.SoundEvents.DYE_USE,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

                if (player instanceof net.minecraft.server.level.ServerPlayer sp
                        && !sp.getAbilities().instabuild) {
                    heldStack.hurtAndBreak(1,
                            (net.minecraft.server.level.ServerLevel) level(), sp,
                            item -> sp.onEquippedItemBroken(
                                    item, net.minecraft.world.entity.EquipmentSlot.MAINHAND));
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // ── Eyedropper — sneak + paintbrush copies cosmetic colour ──────
        if (player.isCrouching() && heldStack.getItem() instanceof PaintbrushItem) {
            if (!currentInSlot.isEmpty()) {
                int color = PaintData.getColor(currentInSlot);
                if (color >= 0) {
                    PaintData.applyColor(heldStack, color);
                    level().playSound(null, getX(), getY(), getZ(),
                            net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                            net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.4f);
                    net.minecraft.network.chat.Component line = PaintFinish.buildTooltipLine(color);
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("Copied: ").append(
                                    line != null ? line
                                            : net.minecraft.network.chat.Component.literal(
                                                    "#" + Integer.toHexString(color))),
                            true);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        }

        // ── Place ────────────────────────────────────────────────────────
        if (!heldStack.isEmpty() && heldStack.getItem() instanceof CosmeticItem cosmeticItem
                && cosmeticItem.getSlot() == targetSlot) {

            if (!currentInSlot.isEmpty()) {
                if (!player.addItem(currentInSlot.copy())) player.drop(currentInSlot.copy(), false);
            }

            if (targetSlot == CosmeticSlot.HAND) {
                if (leftArm) setHandLeftItem(heldStack.copyWithCount(1));
                else         setHandItem(heldStack.copyWithCount(1));
            } else {
                setSlotStack(targetSlot, heldStack.copyWithCount(1));
            }

            if (!player.isCreative()) heldStack.shrink(1);
            level().playSound((Player) null, getX(), getY(), getZ(),
                    SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.0f, 1.0f);
            return InteractionResult.SUCCESS;
        }

        // ── Remove ───────────────────────────────────────────────────────
        if (heldStack.isEmpty() && !currentInSlot.isEmpty()) {
            if (!player.addItem(currentInSlot.copy())) player.drop(currentInSlot.copy(), false);

            if (targetSlot == CosmeticSlot.HAND) {
                if (leftArm) setHandLeftItem(ItemStack.EMPTY);
                else         setHandItem(ItemStack.EMPTY);
            } else {
                setSlotStack(targetSlot, ItemStack.EMPTY);
            }

            level().playSound((Player) null, getX(), getY(), getZ(),
                    SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.0f, 0.8f);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // ------------------------------------------------------------------
    // Combat — two-hit break like armor stand
    // ------------------------------------------------------------------

    /**
     * Two-hit break mechanic identical to the vanilla armor stand.
     *
     * <ul>
     *   <li>First hit (or a hit after more than 5 ticks): sends event 32 (wobble + sound client-side).</li>
     *   <li>Second hit within 5 ticks: drops cosmetics + mannequin item, plays
     *       {@link SoundEvents#ARMOR_STAND_BREAK} and spawns POOF particles via
     *       {@link ServerLevel} directly (avoids entity event 35 which is the totem-of-undying
     *       event on {@link LivingEntity}), then discards the entity.</li>
     * </ul>
     *
     * <p>{@code lastHit} is always updated on every hit, matching the armor stand's tracking logic.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (!(source.getEntity() instanceof Player)) return false;

        long now  = level().getGameTime();
        long gap  = now - lastHit;
        lastHit   = now; // always update — matching armor stand behavior

        if (gap > 5L) {
            // First hit — wobble.  Sound + shake handled client-side via event 32.
            level().broadcastEntityEvent(this, (byte) 32);
        } else {
            // Second hit within 5 ticks — break.
            // Use ServerLevel directly so we never touch entity event 35
            // (which is the totem-of-undying event on LivingEntity).
            dropCosmetics();
            // Don't return the mannequin item in creative — matches armor-stand behaviour
            if (!(source.getEntity() instanceof Player p) || !p.isCreative()) {
                spawnAtLocation(new ItemStack(CosmoLibItems.COSMETIC_MANNEQUIN.get()));
            }
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, getX(), getY(), getZ(),
                        SoundEvents.ARMOR_STAND_BREAK, SoundSource.PLAYERS, 0.8f, 1.0f);
                serverLevel.sendParticles(ParticleTypes.POOF,
                        getX(), getY() + getBbHeight() / 2.0, getZ(),
                        10, 0.3, 0.4, 0.3, 0.02);
            }
            discard();
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Entity events — wobble (32) and break particles (35)
    // ------------------------------------------------------------------

    /**
     * Receives entity events on the client.
     *
     * <ul>
     *   <li>Event 32 — hit/wobble: records the hit time for the renderer and plays
     *       {@link SoundEvents#ARMOR_STAND_HIT} locally.</li>
     * </ul>
     *
     * <p>Break particles and sound are sent via {@link ServerLevel#sendParticles} and
     * {@link ServerLevel#playSound} directly in {@link #hurt}, so no client-side event
     * is needed for the break effect.
     */
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 32) {
            // Hit / wobble
            if (level().isClientSide) {
                lastHitByPlayerTime = level().getGameTime();
                level().playLocalSound(getX(), getY(), getZ(),
                        SoundEvents.ARMOR_STAND_HIT, SoundSource.PLAYERS, 0.3f, 1.0f, false);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    private void dropCosmetics() {
        if (!getHatItem().isEmpty())      spawnAtLocation(getHatItem().copy());
        if (!getBackItem().isEmpty())     spawnAtLocation(getBackItem().copy());
        if (!getHandItem().isEmpty())     spawnAtLocation(getHandItem().copy());
        if (!getHandLeftItem().isEmpty()) spawnAtLocation(getHandLeftItem().copy());
    }

    // ------------------------------------------------------------------
    // Synched data — keep body rotation in sync on the client
    // ------------------------------------------------------------------

    @Override
    public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ROTATION.equals(key)) {
            float yaw = getRotationIndex() * 45f;
            setYRot(yaw);
            yRotO    = yaw;
            yBodyRot = yaw;
            yBodyRotO = yaw;
            yHeadRot = yaw;
            yHeadRotO = yaw;
        }
    }

    // ------------------------------------------------------------------
    // Entity behaviour overrides — falls with gravity, non-pushable
    // ------------------------------------------------------------------

    @Override
    public void aiStep() {
        // No pathfinding, but we must let the parent run so it calls travel(),
        // which is what applies gravity and fluid physics each tick.
        super.aiStep();
    }

    @Override
    public void travel(Vec3 travelVector) {
        // Apply gravity so the mannequin falls when placed in mid-air,
        // but pass a zero movement vector so it never walks anywhere.
        super.travel(Vec3.ZERO);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        // Mannequins don't take fall damage (matches armor stand behaviour)
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isAlwaysTicking() {
        return true;
    }

    @Override
    public boolean isAffectedByFluids() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    /** Middle-click returns the mannequin item, matching armor-stand pick-block behaviour. */
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(CosmoLibItems.COSMETIC_MANNEQUIN.get());
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return java.util.List.of();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {}

    @Override
    public net.minecraft.world.entity.HumanoidArm getMainArm() {
        return net.minecraft.world.entity.HumanoidArm.RIGHT;
    }

    // ------------------------------------------------------------------
    // NBT save/load
    // ------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Rotation", getRotationIndex());
        if (!getHatItem().isEmpty())      tag.put("HatItem",      getHatItem().save(registryAccess()));
        if (!getBackItem().isEmpty())     tag.put("BackItem",     getBackItem().save(registryAccess()));
        if (!getHandItem().isEmpty())     tag.put("HandItem",     getHandItem().save(registryAccess()));
        if (!getHandLeftItem().isEmpty()) tag.put("HandLeftItem", getHandLeftItem().save(registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setRotationIndex(tag.getInt("Rotation"));
        if (tag.contains("HatItem"))      setHatItem(ItemStack.parseOptional(registryAccess(), tag.getCompound("HatItem")));
        if (tag.contains("BackItem"))     setBackItem(ItemStack.parseOptional(registryAccess(), tag.getCompound("BackItem")));
        if (tag.contains("HandItem"))     setHandItem(ItemStack.parseOptional(registryAccess(), tag.getCompound("HandItem")));
        if (tag.contains("HandLeftItem")) setHandLeftItem(ItemStack.parseOptional(registryAccess(), tag.getCompound("HandLeftItem")));
    }

    // ------------------------------------------------------------------
    // GeckoLib — GeoEntity
    // ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // No animations — mannequin is static
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
