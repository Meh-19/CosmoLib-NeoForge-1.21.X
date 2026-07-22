package net.meh.cosmolib.cosmetic.tool.client;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side singleton tracking the active Backswag Tuner session and the
 * cross-cosmetic offset clipboard.
 *
 * <p>A session begins when the player right-clicks the
 * {@link net.meh.cosmolib.cosmetic.tool.BackswagTunerItem} while a
 * {@link net.meh.cosmolib.cosmetic.CosmeticSlot#BACK} cosmetic is equipped.
 * It ends on explicit save (Ctrl+Click) or cancel (Shift+Click).
 *
 * <p>The clipboard ({@link #clipboardY} / {@link #hasClipboard}) persists
 * across sessions for the lifetime of the game instance, allowing a value
 * tuned on one cosmetic to be pasted onto another.
 *
 * <p>All fields are {@code public} for direct access by
 * {@link BackswagTunerHud} and the event handler — no getters are needed.
 * No synchronisation is required; all access occurs on the client main thread.
 *
 * <p>This class has no client-only imports and can be referenced safely from
 * common code, but it is placed in the {@code tool.client} package to make
 * its intended scope explicit.
 */
public final class BackswagTunerSession {

    /** Singleton instance. */
    public static final BackswagTunerSession INSTANCE = new BackswagTunerSession();

    // ------------------------------------------------------------------
    // Active session state
    // ------------------------------------------------------------------

    /** Whether a tuning session is currently active. */
    public boolean sessionActive = false;

    /**
     * Registry ID of the cosmetic being tuned, or {@code null} when no session
     * is active.
     */
    @Nullable
    public ResourceLocation activeCosmeticId = null;

    /**
     * Y offset at session start, used to restore the original value on
     * Shift+Click cancel.
     */
    public double originalY = 0.0;

    /**
     * Live Y offset, updated on every scroll tick and written to
     * {@link net.meh.cosmolib.cosmetic.offset.BackOffsetManager} so renders
     * update immediately.
     */
    public double currentY = 0.0;

    // ------------------------------------------------------------------
    // Cross-session clipboard (persists until the game closes)
    // ------------------------------------------------------------------

    /**
     * The Y offset most recently copied with Ctrl+C.
     * Only valid when {@link #hasClipboard} is {@code true}.
     */
    public double clipboardY = 0.0;

    /**
     * {@code true} once the player has copied at least one value with Ctrl+C,
     * allowing Ctrl+P to paste it onto any subsequent cosmetic.
     */
    public boolean hasClipboard = false;

    private BackswagTunerSession() {}

    /**
     * Starts a new session for the given cosmetic, recording the original Y for
     * cancel support.  Replaces any previously active session without clean-up —
     * callers should only start a session when none is active.
     *
     * @param cosmeticId the full registry ID of the equipped back cosmetic
     * @param y          current Y offset snapshot from
     *                   {@link net.meh.cosmolib.cosmetic.offset.BackOffsetManager}
     */
    public void start(ResourceLocation cosmeticId, double y) {
        this.activeCosmeticId = cosmeticId;
        this.originalY        = y;
        this.currentY         = y;
        this.sessionActive    = true;
    }

    /**
     * Clears all session state.
     * Called after a successful save or a cancel operation.
     */
    public void clear() {
        this.sessionActive    = false;
        this.activeCosmeticId = null;
        this.originalY        = 0.0;
        this.currentY         = 0.0;
    }
}
