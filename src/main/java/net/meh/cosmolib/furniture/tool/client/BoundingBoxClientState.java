package net.meh.cosmolib.furniture.tool.client;

import net.meh.cosmolib.furniture.tool.network.SyncBBSessionPayload;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side mirror of the developer's active
 * {@link net.meh.cosmolib.furniture.tool.BoundingBoxSelectorSession}.
 *
 * <p>Updated by {@link SyncBBSessionPayload} whenever the server-side session
 * changes.  Read by {@link BoundingBoxRenderer} each frame to draw the overlay.
 *
 * <p>All fields are {@code public} for direct renderer access; no synchronization
 * is needed since Minecraft's client is single-threaded and all packet handling
 * runs on the main thread via {@code enqueueWork}.
 */
public final class BoundingBoxClientState {

    /** Whether a session is currently active for this client's player. */
    public static boolean sessionActive = false;

    /** Registry path of the target furniture block. */
    public static String furnitureId = "";

    /** World position of the confirmed anchor, or {@code null} if not yet set. */
    @Nullable
    public static BlockPos anchorPos = null;

    /** Block rotation (0–7) from the furniture block at session start. */
    public static int furnitureRotation = 0;

    /** Whether the session is in diagonal (45°) mode. */
    public static boolean inDiagonalMode = false;

    /** Currently selected world positions. */
    public static final List<BlockPos> selectedPositions = new ArrayList<>();

    /**
     * Current selection mode ordinal (0 = FULL, 1 = SLAB, 2 = SEAT).
     * Mirrors {@link net.meh.cosmolib.furniture.tool.BBSelectionMode#ordinal()}.
     */
    public static int selectionMode = 0;

    /** Selected world positions that are marked as slab (half-height) hitboxes. */
    public static final List<BlockPos> slabPositions = new ArrayList<>();

    /** Selected world positions that are marked as sittable seat positions. */
    public static final List<BlockPos> seatPositions = new ArrayList<>();

    /** Sit height (in blocks) for seat positions in this session. */
    public static float furnitureSeatHeight = 5f / 16f;

    private BoundingBoxClientState() {}

    // ------------------------------------------------------------------
    // Update from network
    // ------------------------------------------------------------------

    /**
     * Applies an incoming sync payload, replacing all state fields atomically.
     *
     * @param payload the payload received from the server
     */
    public static void update(SyncBBSessionPayload payload) {
        sessionActive    = payload.sessionActive();
        furnitureId      = payload.furnitureId();
        furnitureRotation = payload.furnitureRotation();
        inDiagonalMode   = payload.inDiagonalMode();

        long anchorLong = payload.anchorPosLong();
        anchorPos = (anchorLong != Long.MIN_VALUE) ? BlockPos.of(anchorLong) : null;

        selectedPositions.clear();
        for (long l : payload.selectedPositions()) {
            selectedPositions.add(BlockPos.of(l));
        }

        selectionMode = payload.selectionMode();
        slabPositions.clear();
        for (long l : payload.slabPositions()) slabPositions.add(BlockPos.of(l));
        seatPositions.clear();
        for (long l : payload.seatPositions()) seatPositions.add(BlockPos.of(l));
        furnitureSeatHeight = payload.furnitureSeatHeight();
    }

    /** Resets all state to defaults (called on client disconnect). */
    public static void clear() {
        sessionActive    = false;
        furnitureId      = "";
        anchorPos        = null;
        furnitureRotation = 0;
        inDiagonalMode   = false;
        selectedPositions.clear();
        selectionMode    = 0;
        slabPositions.clear();
        seatPositions.clear();
        furnitureSeatHeight = 5f / 16f;
    }
}
