package net.meh.cosmolib.furniture.tool;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Server-side session state for one developer using the
 * {@link BoundingBoxSelectorItem}.
 *
 * <p>Sessions are stored in {@link #SESSIONS} keyed by player UUID.
 * Only one session per player is active at a time; clearing a session
 * discards all unsaved work.
 *
 * <h3>Tool flow</h3>
 * <ol>
 *   <li>Right-click a furniture block → {@link #start}</li>
 *   <li>Right-click the same furniture block again → {@link #setAnchor}</li>
 *   <li>Right-click other blocks → {@link #addPosition} / {@link #removePosition}</li>
 *   <li>Ctrl+Z → {@link #undo}</li>
 *   <li>Right-click the furniture block once more → save and {@link #clear}</li>
 *   <li>Sneak-right-click / right-click air → {@link #clear} (cancel)</li>
 * </ol>
 */
public final class BoundingBoxSelectorSession {

    /** Maximum undo states retained. */
    private static final int MAX_UNDO = 10;

    /** Active sessions, keyed by player UUID string. */
    public static final Map<String, BoundingBoxSelectorSession> SESSIONS = new HashMap<>();

    /**
     * Undo snapshot capturing selected, slab, and seat positions at a point in time.
     */
    public record SelectionSnapshot(List<BlockPos> positions, Set<BlockPos> slabs, Set<BlockPos> seats) {}

    // ------------------------------------------------------------------
    // Session fields
    // ------------------------------------------------------------------

    /** Registry path of the target furniture block (e.g. {@code "ancient_arbor"}). */
    public String targetFurnitureId = "";

    /** World position of the anchor (root) furniture block; null until set. */
    @Nullable
    public BlockPos anchorWorldPos = null;

    /** Block rotation (0–7) read from the furniture block at session start. */
    public int furnitureRotation = 0;

    /**
     * Whether this session is defining a diagonal (45°) layout.
     * Set automatically based on whether the furniture's rotation is odd.
     */
    public boolean inDiagonalMode = false;

    /** Whether the anchor has been confirmed by the second right-click. */
    public boolean anchorSet = false;

    /** Currently selected world positions (these will be converted to layout-relative on save). */
    public final List<BlockPos> selectedPositions = new ArrayList<>();

    /**
     * Subset of {@link #selectedPositions} that should use a slab (half-height, 8/16) hitbox.
     * Populated when {@link #selectionMode} is {@link BBSelectionMode#SLAB}.
     */
    public final Set<BlockPos> slabPositions = new HashSet<>();

    /**
     * Subset of {@link #selectedPositions} that are sittable seat positions.
     * Populated when {@link #selectionMode} is {@link BBSelectionMode#SEAT}.
     */
    public final Set<BlockPos> seatPositions = new HashSet<>();

    /**
     * Current hitbox mode.  Cycled by the BB_SLAB keybind (Left Alt):
     * {@code FULL → SLAB → SEAT → FULL}.
     */
    public BBSelectionMode selectionMode = BBSelectionMode.FULL;

    /**
     * Sit height (in blocks) read from the furniture block when the session starts.
     * Used as the {@link net.meh.cosmolib.entity.SeatEntity} offset for seat positions.
     * Defaults to {@code 5/16} — same default as {@link net.meh.cosmolib.furniture.FurnitureOptions}.
     */
    public float furnitureSeatHeight = 5f / 16f;

    /**
     * Stack of previous selection snapshots for undo (includes slab positions).
     * Capped at {@link #MAX_UNDO} entries.
     */
    public final Deque<SelectionSnapshot> undoStack = new ArrayDeque<>();

    // ------------------------------------------------------------------
    // Factory / lifecycle
    // ------------------------------------------------------------------

    private BoundingBoxSelectorSession() {}

    /**
     * Starts a new session for the given player UUID.
     * Any existing session for that player is discarded.
     *
     * @param playerUUID   string form of the player's UUID
     * @param furnitureId  registry path of the target furniture block
     * @param rotation     block rotation state (0–7)
     * @return the newly created session
     */
    public static BoundingBoxSelectorSession start(String playerUUID,
                                                    String furnitureId,
                                                    int rotation) {
        BoundingBoxSelectorSession session = new BoundingBoxSelectorSession();
        session.targetFurnitureId = furnitureId;
        session.furnitureRotation = rotation;
        session.inDiagonalMode    = (rotation % 2 != 0);
        SESSIONS.put(playerUUID, session);
        return session;
    }

    /**
     * Returns the active session for the given player UUID, or {@code null}
     * if no session is active.
     */
    @Nullable
    public static BoundingBoxSelectorSession get(String playerUUID) {
        return SESSIONS.get(playerUUID);
    }

    /**
     * Ends and discards the session for the given player UUID.
     * Does nothing if no session is active.
     */
    public static void clear(String playerUUID) {
        SESSIONS.remove(playerUUID);
    }

    // ------------------------------------------------------------------
    // Mutation helpers
    // ------------------------------------------------------------------

    /**
     * Confirms the anchor world position.
     *
     * @param anchor the world position of the furniture block used as origin
     */
    public void setAnchor(BlockPos anchor) {
        this.anchorWorldPos = anchor;
        this.anchorSet      = true;
    }

    /**
     * Adds a world position to the selection, respecting the current {@link #selectionMode}.
     *
     * <ul>
     *   <li>New position: added to {@link #selectedPositions} and, in SLAB/SEAT mode, to the
     *       corresponding type set.</li>
     *   <li>Existing position: its type changes to match the current mode (FULL removes it from
     *       both type sets; SLAB/SEAT swaps it to the new type).</li>
     * </ul>
     *
     * @param pos world position to add or re-type
     * @return {@code true} if any change was made
     */
    public boolean addPosition(BlockPos pos) {
        if (selectedPositions.contains(pos)) {
            boolean isSlab = slabPositions.contains(pos);
            boolean isSeat = seatPositions.contains(pos);
            return switch (selectionMode) {
                case SLAB -> {
                    if (!isSlab) { pushUndo(); slabPositions.add(pos); seatPositions.remove(pos); yield true; }
                    yield false;
                }
                case SEAT -> {
                    if (!isSeat) { pushUndo(); seatPositions.add(pos); slabPositions.remove(pos); yield true; }
                    yield false;
                }
                case FULL -> {
                    if (isSlab || isSeat) { pushUndo(); slabPositions.remove(pos); seatPositions.remove(pos); yield true; }
                    yield false;
                }
            };
        }
        pushUndo();
        selectedPositions.add(pos);
        switch (selectionMode) {
            case SLAB -> slabPositions.add(pos);
            case SEAT -> seatPositions.add(pos);
            default   -> {} // FULL: no special set
        }
        return true;
    }

    /**
     * Removes a world position from the selection and from all type sets.
     *
     * @param pos world position to remove
     * @return {@code true} if the position was in the selection
     */
    public boolean removePosition(BlockPos pos) {
        boolean removed = selectedPositions.remove(pos);
        slabPositions.remove(pos);
        seatPositions.remove(pos);
        return removed;
    }

    /**
     * Restores the previous selection state (positions + slab + seat) from the undo stack.
     *
     * @return {@code true} if there was a state to restore
     */
    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        SelectionSnapshot prev = undoStack.pollLast();
        selectedPositions.clear();
        selectedPositions.addAll(prev.positions());
        slabPositions.clear();
        slabPositions.addAll(prev.slabs());
        seatPositions.clear();
        seatPositions.addAll(prev.seats());
        return true;
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private void pushUndo() {
        undoStack.addLast(new SelectionSnapshot(
                new ArrayList<>(selectedPositions),
                new HashSet<>(slabPositions),
                new HashSet<>(seatPositions)));
        while (undoStack.size() > MAX_UNDO) undoStack.pollFirst();
    }
}
