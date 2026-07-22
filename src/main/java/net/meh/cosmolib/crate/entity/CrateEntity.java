package net.meh.cosmolib.crate.entity;

import net.meh.cosmolib.crate.CrateLootTable;
import net.meh.cosmolib.crate.CrateLuckAttachment;
import net.meh.cosmolib.crate.CrateRegistry;
import net.meh.cosmolib.crate.network.CrateOpenPacket;
import net.meh.cosmolib.crate.network.CrateUnlockPacket;
import net.meh.cosmolib.registry.CosmoLibEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A non-living animated entity representing a placeable cosmetic crate.
 *
 * <p>The crate progresses through five states:
 * <ol>
 *   <li>{@link #STATE_CLOSED} — idle, awaiting interaction</li>
 *   <li>{@link #STATE_OPENING} — playing the open animation</li>
 *   <li>{@link #STATE_LOOP} — showing rolled items, waiting for player input</li>
 *   <li>{@link #STATE_REROLLING} — playing the reroll animation</li>
 *   <li>{@link #STATE_VANISHING} — playing the vanish animation before discarding</li>
 * </ol>
 *
 * <p>All state transitions are server-authoritative. The client reads {@link SynchedEntityData}
 * and plays the appropriate GeckoLib animations but never makes gameplay decisions.
 */
public class CrateEntity extends Entity implements GeoEntity {

    // ------------------------------------------------------------------
    // State constants
    // ------------------------------------------------------------------

    public static final int STATE_CLOSED    = 0;
    public static final int STATE_OPENING   = 1;
    public static final int STATE_LOOP      = 2;
    public static final int STATE_REROLLING = 3;
    public static final int STATE_VANISHING = 4;

    /**
     * Duration constants (server-side) for automatic state transitions in ticks.
     * These must match the lengths of the respective GeckoLib animation clips.
     */
    /** 3.8 s × 20 ticks/s — must match {@code animation.crate.open} length. */
    private static final long ANIM_OPEN_TICKS    = 76L;
    /** 3.0 s × 20 ticks/s — must match {@code animation.crate.reroll} length. */
    private static final long ANIM_REROLL_TICKS  = 60L;
    /** 0.8 s × 20 ticks/s — must match {@code animation.crate.vanish} length. */
    private static final long ANIM_VANISH_TICKS  = 16L;
    private static final long AUTO_COLLECT_DELAY = 40L;

    // ------------------------------------------------------------------
    // SynchedEntityData keys
    // ------------------------------------------------------------------

    private static final EntityDataAccessor<String>    CRATE_ID_KEY    =
            SynchedEntityData.defineId(CrateEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer>   STATE_KEY       =
            SynchedEntityData.defineId(CrateEntity.class, EntityDataSerializers.INT);
    /** Opener UUID serialised as a string; empty string = nobody. */
    private static final EntityDataAccessor<String>    OPENER_UUID_KEY =
            SynchedEntityData.defineId(CrateEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer>   REROLLS_LEFT_KEY =
            SynchedEntityData.defineId(CrateEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> ITEM_1_KEY =
            SynchedEntityData.defineId(CrateEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> ITEM_2_KEY =
            SynchedEntityData.defineId(CrateEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> ITEM_3_KEY =
            SynchedEntityData.defineId(CrateEntity.class, EntityDataSerializers.ITEM_STACK);
    /** Rotation index 0–7, 0 = north, increasing clockwise in 45° steps. */
    private static final EntityDataAccessor<Integer>   ROTATION_KEY    =
            SynchedEntityData.defineId(CrateEntity.class, EntityDataSerializers.INT);

    // ------------------------------------------------------------------
    // Server-side timing (not synced, not saved)
    // ------------------------------------------------------------------

    /** Game-tick when the current timed state was entered; -1 when unused. */
    private long stateEnterTick = -1L;

    /** Game-tick at which auto-collect should fire; -1 when not scheduled. */
    private long autoCollectTick = -1L;

    // ------------------------------------------------------------------
    // Hit tracking — two-hit break like armor stand (closed state only)
    // ------------------------------------------------------------------

    /**
     * Server-side game-tick of the last player hit on a closed crate.
     * Initialised to 0 (not Long.MIN_VALUE) to avoid overflow on the very first hit.
     */
    private long lastHit = 0L;

    /**
     * Client-side game-tick of the last wobble event (entity event 32).
     * Used by the renderer for a brief shake effect.
     */
    private long lastHitClientTime = Long.MIN_VALUE;

    /** Returns the last client-side hit time for wobble rendering. */
    public long getLastHitClientTime() { return lastHitClientTime; }

    // ------------------------------------------------------------------
    // GeckoLib
    // ------------------------------------------------------------------

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // ------------------------------------------------------------------
    // Session tracking — used server-side to cancel incoming attacks
    // ------------------------------------------------------------------

    /**
     * UUIDs of players currently in an active crate session (OPENING / LOOP / REROLLING).
     * Maintained server-side only; queried by {@link net.meh.cosmolib.event.GameEventHandler}
     * to cancel all incoming damage so the player is truly untouchable during the cutscene.
     */
    private static final Set<UUID> ACTIVE_OPENERS =
            Collections.synchronizedSet(new HashSet<>());

    /**
     * Returns {@code true} if the player with the given UUID is currently inside a crate
     * opening sequence and should be immune to all incoming attacks.
     */
    public static boolean isPlayerInCrateSession(UUID uuid) {
        return ACTIVE_OPENERS.contains(uuid);
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    public CrateEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    // ------------------------------------------------------------------
    // Placement factory
    // ------------------------------------------------------------------

    /**
     * Creates and positions a new {@link CrateEntity} in the world.
     * The caller must call {@link Level#addFreshEntity(Entity)} afterward.
     *
     * @param level        the server level
     * @param pos          the block position at which to spawn the crate
     * @param playerYaw    the spawning player's Y rotation (used for facing direction)
     * @param crateId      registry ID of the {@link net.meh.cosmolib.crate.CrateType}
     * @param rerollCount  number of rerolls the opener gets
     * @return the new, un-added entity
     */
    public static CrateEntity place(Level level, BlockPos pos, float playerYaw,
                                     ResourceLocation crateId, int rerollCount) {
        CrateEntity entity = new CrateEntity(CosmoLibEntityTypes.CRATE_ENTITY.get(), level);
        entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        // +4 flips 180° so the crate faces TOWARD the player (opposite of their look direction).
        // The model's front (lid) opens toward -Z (north) by default; GeckoLib applies
        // (180 - entityYaw), so entityYaw = playerYaw + 180 → (180 - (yaw+180)) = -yaw = correct facing.
        int rotation = Math.floorMod(Math.round(playerYaw / 45.0f) + 4, 8);
        entity.setRotationIndex(rotation);
        entity.entityData.set(CRATE_ID_KEY, crateId.toString());
        entity.entityData.set(REROLLS_LEFT_KEY, rerollCount);
        return entity;
    }

    // ------------------------------------------------------------------
    // SynchedEntityData
    // ------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CRATE_ID_KEY,     "cosmolib:missing");
        builder.define(STATE_KEY,        STATE_CLOSED);
        builder.define(OPENER_UUID_KEY,  "");
        builder.define(REROLLS_LEFT_KEY, 0);
        builder.define(ITEM_1_KEY,       ItemStack.EMPTY);
        builder.define(ITEM_2_KEY,       ItemStack.EMPTY);
        builder.define(ITEM_3_KEY,       ItemStack.EMPTY);
        builder.define(ROTATION_KEY,     0);
    }

    // ------------------------------------------------------------------
    // Public accessors
    // ------------------------------------------------------------------

    /** Returns the crate type's registry ID. */
    public ResourceLocation getCrateId() {
        return ResourceLocation.parse(entityData.get(CRATE_ID_KEY));
    }

    /** Returns the current state (one of the {@code STATE_*} constants). */
    public int getState() { return entityData.get(STATE_KEY); }

    /** Returns the UUID of the player currently opening this crate, if any. */
    public Optional<UUID> getOpenerUUID() {
        String s = entityData.get(OPENER_UUID_KEY);
        if (s.isEmpty()) return Optional.empty();
        try { return Optional.of(UUID.fromString(s)); }
        catch (IllegalArgumentException e) { return Optional.empty(); }
    }

    /** Returns how many rerolls the opener has left. */
    public int getRerollsLeft() { return entityData.get(REROLLS_LEFT_KEY); }

    public ItemStack getItem1() { return entityData.get(ITEM_1_KEY); }
    public ItemStack getItem2() { return entityData.get(ITEM_2_KEY); }
    public ItemStack getItem3() { return entityData.get(ITEM_3_KEY); }

    /** Returns the rotation index (0–7, 0 = north, clockwise). */
    public int getRotationIndex() { return entityData.get(ROTATION_KEY); }

    // ------------------------------------------------------------------
    // Private mutators
    // ------------------------------------------------------------------

    private void setRotationIndex(int index) {
        int clamped = Math.floorMod(index, 8);
        entityData.set(ROTATION_KEY, clamped);
        setYRot(clamped * 45f);
        yRotO = clamped * 45f;
    }

    private void setState(int newState) {
        entityData.set(STATE_KEY, newState);
        if (newState == STATE_OPENING || newState == STATE_REROLLING || newState == STATE_VANISHING) {
            stateEnterTick = level().getGameTime();
        } else {
            stateEnterTick = -1L;
        }
    }

    private void setItem1(ItemStack stack) { entityData.set(ITEM_1_KEY, stack.copy()); }
    private void setItem2(ItemStack stack) { entityData.set(ITEM_2_KEY, stack.copy()); }
    private void setItem3(ItemStack stack) { entityData.set(ITEM_3_KEY, stack.copy()); }

    private void setOpenerUUID(Optional<UUID> uuid) {
        entityData.set(OPENER_UUID_KEY, uuid.map(UUID::toString).orElse(""));
    }

    // ------------------------------------------------------------------
    // Client-side data sync
    // ------------------------------------------------------------------

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ROTATION_KEY.equals(key)) {
            float yaw = getRotationIndex() * 45f;
            setYRot(yaw);
            yRotO = yaw;
        }
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            serverTick();
        }
    }

    private void serverTick() {
        long now   = level().getGameTime();
        int  state = getState();

        // OPENING → LOOP after animation
        if (state == STATE_OPENING && stateEnterTick >= 0 && now - stateEnterTick >= ANIM_OPEN_TICKS) {
            stateEnterTick = -1L;
            setState(STATE_LOOP);
            return;
        }

        // REROLLING — handle animation completion
        if (state == STATE_REROLLING && stateEnterTick >= 0 && now - stateEnterTick >= ANIM_REROLL_TICKS) {
            stateEnterTick = -1L;
            if (getRerollsLeft() > 0) {
                setState(STATE_LOOP);
            } else {
                // Schedule auto-collect 40 ticks from now
                autoCollectTick = now + AUTO_COLLECT_DELAY;
            }
            return;
        }

        // Auto-collect fires after the last-reroll delay
        if (autoCollectTick > 0 && now >= autoCollectTick) {
            autoCollectTick = -1L;
            Optional<UUID> openerUUID = getOpenerUUID();
            if (openerUUID.isPresent() && level() instanceof ServerLevel sl) {
                ServerPlayer opener = sl.getServer().getPlayerList().getPlayer(openerUUID.get());
                if (opener != null) {
                    collectRewards(opener);
                } else {
                    dropHeldItems();
                    setOpenerUUID(Optional.empty());
                    setState(STATE_VANISHING);
                }
            } else {
                dropHeldItems();
                setState(STATE_VANISHING);
            }
            return;
        }

        // VANISHING → discard after animation
        if (state == STATE_VANISHING && stateEnterTick >= 0 && now - stateEnterTick >= ANIM_VANISH_TICKS) {
            discard();
            return;
        }

        // Keep the opener fully invincible (resistance level 5 = 100% damage reduction)
        // while an active crate session is in progress. This must run server-side so it
        // actually prevents damage — client-side effect application has no gameplay effect.
        if (state == STATE_OPENING || state == STATE_LOOP || state == STATE_REROLLING) {
            Optional<UUID> openerUUID = getOpenerUUID();
            if (openerUUID.isPresent() && level() instanceof ServerLevel sl) {
                ServerPlayer opener = sl.getServer().getPlayerList().getPlayer(openerUUID.get());
                if (opener != null) {
                    opener.addEffect(new MobEffectInstance(
                            MobEffects.DAMAGE_RESISTANCE, 40, 4, false, false));
                }
            }
        }

        // LOOP: verify opener proximity; collect on crouch
        if (state == STATE_LOOP) {
            Optional<UUID> openerUUID = getOpenerUUID();
            if (openerUUID.isPresent() && level() instanceof ServerLevel sl) {
                ServerPlayer opener = sl.getServer().getPlayerList().getPlayer(openerUUID.get());
                if (opener == null || opener.level() != level() || distanceTo(opener) > 8.0) {
                    getOpenerUUID().ifPresent(ACTIVE_OPENERS::remove); // restore vulnerability
                    dropHeldItems();
                    setOpenerUUID(Optional.empty());
                    if (opener != null) {
                        PacketDistributor.sendToPlayer(opener, new CrateUnlockPacket());
                    }
                    setState(STATE_VANISHING); // vanish so the empty crate doesn't persist
                } else if (opener.isShiftKeyDown()) {
                    // Crouch alone collects — no click required
                    collectRewards(opener);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Interaction
    // ------------------------------------------------------------------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;

        int state = getState();

        // Right-click to open a closed crate
        if (state == STATE_CLOSED) {
            setOpenerUUID(Optional.of(player.getUUID()));
            ACTIVE_OPENERS.add(player.getUUID()); // mark invincible for the session
            float luck = CrateLuckAttachment.getLuck(player);
            CrateLootTable table = CrateLootTable.load(getCrateId());
            List<ItemStack> items = table.rollItems(level().random, luck);
            setItem1(items.get(0));
            setItem2(items.get(1));
            setItem3(items.get(2));
            setState(STATE_OPENING);
            PacketDistributor.sendToPlayer((ServerPlayer) player, new CrateOpenPacket(getId()));
            return InteractionResult.SUCCESS;
        }

        // Block non-openers from interacting while the crate is open
        if (state == STATE_LOOP) {
            Optional<UUID> openerUUID = getOpenerUUID();
            if (openerUUID.isPresent() && !player.getUUID().equals(openerUUID.get())) {
                return InteractionResult.PASS;
            }
        }

        return InteractionResult.PASS;
    }

    // ------------------------------------------------------------------
    // Damage / reroll / break
    // ------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (!(source.getEntity() instanceof Player attacker)) return false;

        int state = getState();

        // Closed: two-hit break like the mannequin / armor stand
        if (state == STATE_CLOSED) {
            long now = level().getGameTime();
            long gap = now - lastHit;
            lastHit  = now;

            if (gap > 5L) {
                // First hit — wobble + sound via entity event 32
                level().broadcastEntityEvent(this, (byte) 32);
            } else {
                // Second hit within 5 ticks — break
                if (!attacker.isCreative()) {
                    spawnAtLocation(new ItemStack(CrateRegistry.get(getCrateId()).getItem()));
                }
                if (level() instanceof ServerLevel sl) {
                    sl.playSound(null, getX(), getY(), getZ(),
                            SoundEvents.ARMOR_STAND_BREAK, SoundSource.PLAYERS, 0.8f, 1.0f);
                    sl.sendParticles(ParticleTypes.POOF,
                            getX(), getY() + getBbHeight() / 2.0, getZ(),
                            10, 0.3, 0.4, 0.3, 0.02);
                }
                discard();
            }
            return false;
        }

        // LOOP: opener punches to spend a reroll
        if (state == STATE_LOOP) {
            Optional<UUID> openerUUID = getOpenerUUID();
            if (openerUUID.isEmpty() || !attacker.getUUID().equals(openerUUID.get())) return false;
            if (getRerollsLeft() <= 0) return false;

            entityData.set(REROLLS_LEFT_KEY, getRerollsLeft() - 1);
            float luck = CrateLuckAttachment.getLuck(attacker);
            CrateLootTable table = CrateLootTable.load(getCrateId());
            List<ItemStack> rerolled = table.rollItems(level().random, luck);
            setItem1(rerolled.get(0));
            setItem2(rerolled.get(1));
            setItem3(rerolled.get(2));
            setState(STATE_REROLLING);
            return false;
        }

        return false;
    }

    // ------------------------------------------------------------------
    // Reward collection
    // ------------------------------------------------------------------

    /**
     * Gives the three rolled items to the player, updates their crate luck, and
     * transitions the entity to {@link #STATE_VANISHING}.
     *
     * @param player the player collecting the rewards
     */
    private void collectRewards(Player player) {
        List<ItemStack> items = new ArrayList<>(3);
        if (!getItem1().isEmpty()) items.add(getItem1().copy());
        if (!getItem2().isEmpty()) items.add(getItem2().copy());
        if (!getItem3().isEmpty()) items.add(getItem3().copy());

        for (ItemStack stack : items) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }

        CrateLuckAttachment.onCrateCollected(player, items);

        ACTIVE_OPENERS.remove(player.getUUID()); // restore normal vulnerability
        setItem1(ItemStack.EMPTY);
        setItem2(ItemStack.EMPTY);
        setItem3(ItemStack.EMPTY);
        setOpenerUUID(Optional.empty());

        PacketDistributor.sendToPlayer((ServerPlayer) player, new CrateUnlockPacket());
        setState(STATE_VANISHING);
    }

    /** Drops all currently displayed items as world item entities and clears the slots. */
    private void dropHeldItems() {
        if (!getItem1().isEmpty()) spawnAtLocation(getItem1().copy());
        if (!getItem2().isEmpty()) spawnAtLocation(getItem2().copy());
        if (!getItem3().isEmpty()) spawnAtLocation(getItem3().copy());
        setItem1(ItemStack.EMPTY);
        setItem2(ItemStack.EMPTY);
        setItem3(ItemStack.EMPTY);
    }

    // ------------------------------------------------------------------
    // Entity behaviour
    // ------------------------------------------------------------------

    /**
     * Must return {@code true} so the game's raycast includes this entity for both
     * player attacks and right-click interactions. Plain {@link Entity} defaults to
     * {@code false}, which silently swallows all input.
     */
    @Override
    public boolean isPickable() { return true; }

    @Override
    public boolean canBeCollidedWith() { return true; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean isAlwaysTicking() { return true; }

    /**
     * Handles entity events broadcast from the server.
     * <ul>
     *   <li>Event {@code 32} — wobble: records the hit time for the renderer and plays
     *       the armor-stand-hit sound locally.</li>
     * </ul>
     */
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 32) {
            if (level().isClientSide) {
                lastHitClientTime = level().getGameTime();
                level().playLocalSound(getX(), getY(), getZ(),
                        SoundEvents.ARMOR_STAND_HIT, SoundSource.PLAYERS, 0.3f, 1.0f, false);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean shouldBeSaved() {
        // Don't save a vanishing crate — it will discard itself on the next tick anyway
        return getState() != STATE_VANISHING;
    }

    // ------------------------------------------------------------------
    // NBT save / load
    // ------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("CrateId",  entityData.get(CRATE_ID_KEY));
        tag.putInt("Rotation",    getRotationIndex());
        // STATE is intentionally not saved; always restores as CLOSED
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("CrateId"))  entityData.set(CRATE_ID_KEY, tag.getString("CrateId"));
        if (tag.contains("Rotation")) setRotationIndex(tag.getInt("Rotation"));
        entityData.set(STATE_KEY, STATE_CLOSED);
    }

    // ------------------------------------------------------------------
    // GeckoLib
    // ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> {
            switch (getState()) {
                case STATE_OPENING   -> state.getController().setAnimation(
                        RawAnimation.begin().thenPlay("animation.crate.open"));
                case STATE_LOOP      -> state.getController().setAnimation(
                        RawAnimation.begin().thenLoop("animation.crate.open_loop"));
                case STATE_REROLLING -> state.getController().setAnimation(
                        RawAnimation.begin().thenPlay("animation.crate.reroll"));
                case STATE_VANISHING -> state.getController().setAnimation(
                        RawAnimation.begin().thenPlay("animation.crate.vanish"));
                default -> { return PlayState.STOP; }
            }
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
