package net.meh.cosmolib.cosmetic;

import net.meh.cosmolib.CosmoLib;
import net.meh.cosmolib.cosmetic.network.MobHatSyncPayload;
import net.meh.cosmolib.paint.PaintColor;
import net.meh.cosmolib.paint.PaintData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Set;

/**
 * Server-side handler that gives mobs a random cosmetic hat on spawn.
 *
 * <p>On {@link EntityJoinLevelEvent}: fires for every entity entering a level,
 * including {@code /summon}.  An empty {@link CompoundTag} marker is written
 * immediately so that chunk-reload never re-rolls the same mob.  If the random
 * check passes, the marker is overwritten with the real hat compound.
 *
 * <p>On {@link PlayerEvent.StartTracking}: when a player enters tracking range
 * of a mob that has a non-empty hat compound, send a {@link MobHatSyncPayload}
 * to that player so the client-side render layer can display it.
 */
@EventBusSubscriber(modid = CosmoLib.MOD_ID)
public final class MobCosmeticSpawnHandler {

    /** NBT key inside Entity#getPersistentData(). */
    public static final String NBT_HAT = "CosmoHat";

    /** ~8 % chance per spawn — comparable to a zombie spawning with a tool on Normal difficulty. */
    private static final float SPAWN_CHANCE = 0.08f;

    /**
     * Entity types that support a cosmetic hat.  Any mob not listed here is
     * skipped entirely, keeping the roll cost negligible.
     */
    private static final Set<EntityType<?>> HAT_MOBS = Set.of(
            EntityType.BOGGED,
            EntityType.COW,
            EntityType.CREEPER,
            EntityType.DROWNED,
            EntityType.ENDERMAN,
            EntityType.FOX,
            EntityType.HUSK,
            EntityType.IRON_GOLEM,
            EntityType.PIG,
            EntityType.PIGLIN,
            EntityType.PIGLIN_BRUTE,
            EntityType.POLAR_BEAR,
            EntityType.SHEEP,
            EntityType.SKELETON,
            EntityType.STRAY,
            EntityType.WITHER_SKELETON,
            EntityType.ZOMBIE,
            EntityType.ZOMBIFIED_PIGLIN
    );

    private MobCosmeticSpawnHandler() {}

    // -----------------------------------------------------------------------
    // Spawn roll
    // -----------------------------------------------------------------------

    @SubscribeEvent
    @SuppressWarnings("resource")
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // Server side only.
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!HAT_MOBS.contains(mob.getType())) return;
        // Already decided (either has a hat or was explicitly marked as hatless).
        if (mob.getPersistentData().contains(NBT_HAT)) return;

        // Write an empty marker immediately so this mob is never re-rolled
        // (e.g. on chunk reload after a failed roll).
        mob.getPersistentData().put(NBT_HAT, new CompoundTag());

        if (mob.getRandom().nextFloat() >= SPAWN_CHANCE) return;

        List<CosmeticItem> candidates = CosmeticItem.getBySlot(CosmeticSlot.HAT);
        if (candidates.isEmpty()) return;

        CosmeticItem chosen = candidates.get(mob.getRandom().nextInt(candidates.size()));
        ItemStack hat = new ItemStack(chosen);

        // If the cosmetic has a configured default appearance it is already baked
        // into the ItemStack via the item's default data component — nothing to do.
        // For cosmetics with no default, apply a fully random color so mob hats
        // have natural variety instead of always appearing white.
        if (chosen.getDefaultAppearance() == null && chosen.isPaintable()) {
            PaintColor[] colors = PaintColor.values();
            PaintColor color = colors[mob.getRandom().nextInt(colors.length)];
            int shade = mob.getRandom().nextInt(color.getShades().length);
            PaintData.applyColor(hat, color.getShade(shade));
        }

        // Overwrite the empty marker with the real hat compound.
        // ItemStack.save() declares Tag as its return type but always produces a CompoundTag.
        CompoundTag saved = (CompoundTag) hat.save(mob.level().registryAccess());
        mob.getPersistentData().put(NBT_HAT, saved);

        // Broadcast to all players currently tracking this mob.
        // (StartTracking will catch late-joining players.)
        PacketDistributor.sendToPlayersTrackingEntity(mob, new MobHatSyncPayload(mob.getId(), hat));
    }

    // -----------------------------------------------------------------------
    // Tracking sync — re-sends the hat when a player enters range
    // -----------------------------------------------------------------------

    @SubscribeEvent
    @SuppressWarnings("resource")
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof Mob mob)) return;
        if (!HAT_MOBS.contains(mob.getType())) return;

        if (!(mob.getPersistentData().get(NBT_HAT) instanceof CompoundTag saved)) return;
        // An empty CompoundTag is the "no hat" marker — nothing to sync.
        if (saved.isEmpty()) return;

        ItemStack hat = ItemStack.parseOptional(mob.level().registryAccess(), saved);
        if (hat.isEmpty()) return;

        if (event.getEntity() instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new MobHatSyncPayload(mob.getId(), hat));
        }
    }

}
