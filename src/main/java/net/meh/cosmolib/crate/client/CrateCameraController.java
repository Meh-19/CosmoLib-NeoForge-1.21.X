package net.meh.cosmolib.crate.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Client-side crate camera lock system.
 *
 * <p>When a crate opens, the server sends {@link net.meh.cosmolib.crate.network.CrateOpenPacket}
 * which calls {@link #onCrateOpen(int)}. From that point the camera slowly pans to face the
 * crate and the player's movement speed is reduced to 75 % until the crate is dismissed.
 *
 * <p>Only one crate lock may be active at a time (single-player assumption). All state is
 * client-local and safe to access on the render/game thread.
 */
@OnlyIn(Dist.CLIENT)
public final class CrateCameraController {

    /** ResourceLocation used to identify the transient MOVEMENT_SPEED attribute modifier. */
    private static final ResourceLocation CRATE_LOCK_MODIFIER =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "crate_lock");

    // ------------------------------------------------------------------
    // Lock state
    // ------------------------------------------------------------------

    private static boolean locked         = false;
    private static int     targetEntityId = -1;

    /** Lerp progress 0.0 → 1.0; advances ~0.05 per tick (reaches 1 in 20 ticks). */
    private static float lerpProgress  = 0.0f;
    private static float originalYaw   = 0.0f;
    private static float originalPitch = 0.0f;

    private CrateCameraController() {}

    // ------------------------------------------------------------------
    // Packet handlers
    // ------------------------------------------------------------------

    /**
     * Called when the server reports a crate has begun opening for this client.
     *
     * @param entityId the network entity ID of the crate being opened
     */
    public static void onCrateOpen(int entityId) {
        locked         = true;
        targetEntityId = entityId;
        lerpProgress   = 0.0f;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            originalYaw   = mc.player.getYRot();
            originalPitch = mc.player.getXRot();
        }

        applyMovementPenalty();
    }

    /**
     * Called when the server reports the crate interaction has ended.
     *
     * <p>Before releasing the lock, the player's actual yaw/pitch are snapped to the
     * current camera direction (facing the crate). Without this, the viewport override
     * stops and the camera jumps to wherever the player's real rotation drifted while
     * the lock was active.
     */
    public static void onCrateUnlock() {
        if (locked) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                Entity target = mc.level.getEntity(targetEntityId);
                if (target != null) {
                    LocalPlayer player = mc.player;
                    Vec3 eye    = player.getEyePosition(1.0f);
                    double dx   = target.getX() - eye.x;
                    double dy   = (target.getY() + 0.5) - eye.y;
                    double dz   = target.getZ() - eye.z;
                    double horiz = Math.sqrt(dx * dx + dz * dz);
                    player.setYRot((float)  Math.toDegrees(Math.atan2(-dx, dz)));
                    player.setXRot((float) -Math.toDegrees(Math.atan2(dy, horiz)));
                }
            }
        }
        locked         = false;
        targetEntityId = -1;
        removeMovementPenalty();
    }

    /** Returns {@code true} while the camera is locked to a crate entity. */
    public static boolean isLocked() { return locked; }

    /** Returns the entity ID of the current crate target, or -1 when not locked. */
    public static int getTargetEntityId() { return targetEntityId; }

    // ------------------------------------------------------------------
    // Per-tick update (register on ClientTickEvent.Post)
    // ------------------------------------------------------------------

    /**
     * Called every client tick while the lock is active.
     * Advances the lerp progress and maintains the invincibility/penalty effects.
     * Camera angles are applied per-frame in {@link #onComputeCameraAngles} instead
     * of here, which avoids choppiness caused by Minecraft's own partial-tick interpolation.
     */
    public static void tick() {
        if (!locked || targetEntityId < 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { onCrateUnlock(); return; }

        Entity target = mc.level.getEntity(targetEntityId);
        if (target == null) { onCrateUnlock(); return; }

        LocalPlayer player = mc.player;

        // Advance lerp (~20 ticks to fully pan)
        lerpProgress = Math.min(1.0f, lerpProgress + 0.05f);

        // Once the pan is complete, align the player's real yaw/pitch with the crate so
        // that Minecraft's attack raycast actually reaches the entity (enabling punches
        // to reroll regardless of where the mouse is pointing).
        if (lerpProgress >= 1.0f) {
            Vec3 eye    = player.getEyePosition(1.0f);
            double dx   = target.getX() - eye.x;
            double dy   = (target.getY() + 0.5) - eye.y;
            double dz   = target.getZ() - eye.z;
            double horiz = Math.sqrt(dx * dx + dz * dz);
            player.setYRot((float)  Math.toDegrees(Math.atan2(-dx, dz)));
            player.setXRot((float) -Math.toDegrees(Math.atan2(dy, horiz)));
        }

        // Keep movement penalty alive in case it was removed externally
        AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null && attr.getModifier(CRATE_LOCK_MODIFIER) == null) {
            applyMovementPenalty();
        }
    }

    /**
     * Called every rendered frame via {@link ViewportEvent.ComputeCameraAngles}.
     * Computes the exact direction to the crate using the render-frame partial tick,
     * giving perfectly smooth camera tracking without tick-rate jitter.
     *
     * <p>During the pan ({@link #lerpProgress} &lt; 1), the camera eases from the
     * player's original look direction to face the crate. After the pan completes it
     * tracks the crate continuously with no further lerp delay.
     */
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!locked || targetEntityId < 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Entity target = mc.level.getEntity(targetEntityId);
        if (target == null) return;

        LocalPlayer player = mc.player;
        float pt = (float) event.getPartialTick();

        Vec3 eye    = player.getEyePosition(pt);
        double dx   = target.getX() - eye.x;
        double dy   = (target.getY() + 0.5) - eye.y;
        double dz   = target.getZ() - eye.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);

        float desiredYaw   = (float)  Math.toDegrees(Math.atan2(-dx, dz));
        float desiredPitch = (float) -Math.toDegrees(Math.atan2(dy, horiz));

        if (lerpProgress >= 1.0f) {
            // Fully locked — track the crate directly with no lag
            event.setYaw(desiredYaw);
            event.setPitch(desiredPitch);
        } else {
            // Still panning — ease from original direction toward crate
            float t = easeInOut(lerpProgress);
            float yawDelta = Mth.wrapDegrees(desiredYaw - originalYaw);
            event.setYaw(originalYaw + t * yawDelta);
            event.setPitch(Mth.lerp(t, originalPitch, desiredPitch));
        }
    }

    /** Smooth ease-in-out curve (cubic). */
    private static float easeInOut(float t) {
        return t < 0.5f ? 4 * t * t * t : 1 - (-2 * t + 2) * (-2 * t + 2) * (-2 * t + 2) / 2;
    }

    // ------------------------------------------------------------------
    // Scroll suppression (register on InputEvent.MouseScrollingEvent)
    // ------------------------------------------------------------------

    /** Returns {@code true} when scroll input should be suppressed. */
    public static boolean shouldCancelScroll() { return locked; }

    // ------------------------------------------------------------------
    // Attribute helpers
    // ------------------------------------------------------------------

    private static void applyMovementPenalty() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        AttributeInstance attr = mc.player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;
        attr.addOrUpdateTransientModifier(new AttributeModifier(
                CRATE_LOCK_MODIFIER, -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeMovementPenalty() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        AttributeInstance attr = mc.player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) attr.removeModifier(CRATE_LOCK_MODIFIER);
    }
}
