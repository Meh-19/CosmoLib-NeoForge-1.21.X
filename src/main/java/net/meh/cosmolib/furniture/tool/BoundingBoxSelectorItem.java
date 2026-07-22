package net.meh.cosmolib.furniture.tool;

import net.meh.cosmolib.furniture.block.AbstractFurnitureBlock;
import net.meh.cosmolib.furniture.block.SittableBlock;
import net.meh.cosmolib.furniture.layout.MultiBlockLayout;
import net.meh.cosmolib.furniture.layout.MultiBlockLayoutManager;
import net.meh.cosmolib.furniture.tool.network.SyncBBSessionPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Developer-only item for defining multi-block furniture hitbox layouts in-game.
 *
 * <h3>Gates</h3>
 * Only usable in creative mode and with operator permission level ≥ 2.
 *
 * <h3>Workflow</h3>
 * <ol>
 *   <li><b>Right-click a furniture block</b> → starts a session (reads rotation for
 *       straight vs diagonal mode).</li>
 *   <li><b>Right-click the same furniture block again</b> → confirms it as the anchor
 *       (origin = relative [0,0,0]). The anchor is NOT added to the hitbox automatically.</li>
 *   <li><b>Right-click the anchor block</b> (after step 2):
 *       <ul>
 *         <li>Anchor not yet in selection → adds it to the hitbox.</li>
 *         <li>Anchor already in selection → <b>saves</b> the layout.</li>
 *       </ul>
 *       This lets you choose whether the anchor block itself is part of the physical hitbox.</li>
 *   <li><b>Right-click any other block</b> → adds it to the selection.</li>
 *   <li><b>Sneak + right-click any block</b> → removes it from the selection.</li>
 *   <li><b>Sneak + right-click any furniture block</b> → cancels the session.</li>
 *   <li><b>Right-click air</b> → cancels the session.</li>
 *   <li><b>Z key (BB Undo)</b> → undoes the last selection change.</li>
 * </ol>
 *
 * <p>If you don't want the anchor in the hitbox at all, right-click a non-anchor
 * block to build your selection, then right-click the anchor once to save immediately
 * (it enters the "add" branch but you can skip straight to the "save" branch on the
 * next click if you've already added it — or just never click it and use a secondary
 * save: sneak+right-click any non-furniture block triggers nothing, so to save without
 * the anchor just right-click the anchor twice: first adds, second saves, then undo
 * the add before saving... actually: simply right-click anchor without it being in the
 * list = first click adds it; if you don't want it, sneak+right-click to remove it,
 * then right-click anchor again to save.)
 *
 * <h3>Cross-mod support</h3>
 * Layouts are saved to {@code config/cosmolib/furniture_layouts/<furnitureId>.json}
 * and loaded by {@link MultiBlockLayoutManager} at startup.  Any mod extending
 * {@link AbstractFurnitureBlock} picks up multi-block behaviour automatically — no
 * per-mod code changes needed.
 */
public class BoundingBoxSelectorItem extends Item {

    public BoundingBoxSelectorItem(Properties props) {
        super(props);
    }

    // ------------------------------------------------------------------
    // Right-click on a block face
    // ------------------------------------------------------------------

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        // Gate checks
        if (!player.isCreative()) {
            actionBar(sp, "§cRequires creative mode.");
            return InteractionResult.FAIL;
        }
        if (!player.hasPermissions(2)) {
            actionBar(sp, "§cRequires operator level 2+.");
            return InteractionResult.FAIL;
        }

        BlockPos   clickedPos   = ctx.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        boolean    isFurniture  = clickedState.getBlock() instanceof AbstractFurnitureBlock;
        boolean    isSneaking   = player.isShiftKeyDown();
        String     uuid         = sp.getStringUUID();
        BoundingBoxSelectorSession session = BoundingBoxSelectorSession.get(uuid);

        // ── Sneak + the confirmed anchor block → save without adding anchor ──
        if (isSneaking && session != null && session.anchorSet
                && isFurniture && clickedPos.equals(session.anchorWorldPos)) {
            AbstractFurnitureBlock afbCheck = (AbstractFurnitureBlock) clickedState.getBlock();
            if (afbCheck.getRegistryName().equals(session.targetFurnitureId)) {
                return doSave(sp, session, uuid);
            }
        }

        // ── Sneak + any other furniture block → cancel session ────────
        if (isSneaking && isFurniture) {
            if (session != null) {
                BoundingBoxSelectorSession.clear(uuid);
                MultiBlockLayoutManager.deleteTemp(uuid);
                SyncBBSessionPayload.sendTo(sp, null);
                actionBar(sp, "§cSession cancelled.");
            }
            return InteractionResult.SUCCESS;
        }

        // ── Sneak + non-furniture block (session active) → remove ─────
        if (isSneaking && session != null && session.anchorSet) {
            return doRemove(sp, session, uuid, clickedPos);
        }

        // ── No session: start one ─────────────────────────────────────
        if (session == null) {
            if (!isFurniture) {
                actionBar(sp, "§eRight-click a furniture block to start.");
                return InteractionResult.PASS;
            }
            AbstractFurnitureBlock afb = (AbstractFurnitureBlock) clickedState.getBlock();
            int    rotation = clickedState.getValue(AbstractFurnitureBlock.ROTATION);
            String fid      = afb.getRegistryName();
            session = BoundingBoxSelectorSession.start(uuid, fid, rotation);
            // Capture the sit height from SittableBlock so it can be stored in the layout
            if (afb instanceof SittableBlock sittable) {
                session.furnitureSeatHeight = sittable.getSitHeight();
            }
            SyncBBSessionPayload.sendTo(sp, session);

            boolean diagonal = (rotation % 2 != 0);
            actionBar(sp, diagonal
                    ? "§b✦ Diagonal mode — §f" + fid + " §7| Right-click it again to set anchor."
                    : "§a✦ Straight mode — §f" + fid + " §7| Right-click it again to set anchor.");
            return InteractionResult.SUCCESS;
        }

        // ── Session active ────────────────────────────────────────────

        if (isFurniture) {
            AbstractFurnitureBlock afb = (AbstractFurnitureBlock) clickedState.getBlock();
            String fid = afb.getRegistryName();

            if (!fid.equals(session.targetFurnitureId)) {
                actionBar(sp, "§cDifferent furniture type — sneak+click any furniture to cancel.");
                return InteractionResult.FAIL;
            }

            // Not anchored yet → set anchor (do NOT add to selection automatically)
            if (!session.anchorSet) {
                session.setAnchor(clickedPos);
                SyncBBSessionPayload.sendTo(sp, session);
                actionBar(sp, "§aAnchor set at " + fmt(clickedPos)
                        + " §7| Click blocks to select | Right-click anchor to add/save");
                return InteractionResult.SUCCESS;
            }

            // Clicking the confirmed anchor position
            if (clickedPos.equals(session.anchorWorldPos)) {
                if (!session.selectedPositions.contains(clickedPos)) {
                    // First click on anchor after anchoring → add it to the hitbox
                    session.addPosition(clickedPos);
                    writeTempFile(sp, session);
                    SyncBBSessionPayload.sendTo(sp, session);
                    actionBar(sp, "§aAnchor block added to hitbox §7(" + session.selectedPositions.size()
                            + " total) | Right-click anchor again to save.");
                } else {
                    // Anchor already in selection → save
                    return doSave(sp, session, uuid);
                }
                return InteractionResult.SUCCESS;
            }

            // Clicking a different block of the same furniture type → add it
            return doAdd(sp, session, uuid, clickedPos);
        }

        // ── Non-furniture block, session active, not sneaking ─────────
        if (!session.anchorSet) {
            actionBar(sp, "§7Right-click §fthe furniture block §7again to set the anchor first.");
            return InteractionResult.FAIL;
        }
        return doAdd(sp, session, uuid, clickedPos);
    }

    // ------------------------------------------------------------------
    // Right-click on air → cancel
    // ------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide)
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        if (!(player instanceof ServerPlayer sp))
            return InteractionResultHolder.pass(player.getItemInHand(hand));

        HitResult hit = player.pick(5.0, 0f, false);
        if (hit.getType() == HitResult.Type.MISS) {
            String uuid = sp.getStringUUID();
            BoundingBoxSelectorSession session = BoundingBoxSelectorSession.get(uuid);
            if (session != null) {
                BoundingBoxSelectorSession.clear(uuid);
                MultiBlockLayoutManager.deleteTemp(uuid);
                SyncBBSessionPayload.sendTo(sp, null);
                actionBar(sp, "§cSession cancelled.");
            }
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    // ------------------------------------------------------------------
    // Selection helpers
    // ------------------------------------------------------------------

    private InteractionResult doAdd(ServerPlayer sp, BoundingBoxSelectorSession session,
                                     String uuid, BlockPos pos) {
        if (session.addPosition(pos)) {
            writeTempFile(sp, session);
            SyncBBSessionPayload.sendTo(sp, session);
            actionBar(sp, "§a+ " + fmt(pos) + " §7(" + session.selectedPositions.size() + " total)");
        } else {
            actionBar(sp, "§7" + fmt(pos) + " already selected.");
        }
        return InteractionResult.SUCCESS;
    }

    private InteractionResult doRemove(ServerPlayer sp, BoundingBoxSelectorSession session,
                                        String uuid, BlockPos pos) {
        if (session.removePosition(pos)) {
            writeTempFile(sp, session);
            SyncBBSessionPayload.sendTo(sp, session);
            actionBar(sp, "§c- " + fmt(pos) + " §7(" + session.selectedPositions.size() + " total)");
        } else {
            actionBar(sp, "§7" + fmt(pos) + " not in selection.");
        }
        return InteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------------
    // Save
    // ------------------------------------------------------------------

    private InteractionResult doSave(ServerPlayer sp, BoundingBoxSelectorSession session, String uuid) {
        if (session.selectedPositions.isEmpty()) {
            actionBar(sp, "§cNothing selected — click blocks first.");
            return InteractionResult.FAIL;
        }

        BlockPos       anchor      = session.anchorWorldPos;
        List<BlockPos> relPositions = new ArrayList<>();
        for (BlockPos wp : session.selectedPositions) {
            relPositions.add(wp.subtract(anchor));
        }

        // Normalize positions back to rotation-0 (straight) or rotation-1 (diagonal) space.
        // The captured world-relative positions are in "saved-rotation" space; getRotatedPositions
        // always rotates FROM rotation=0/1, so we must undo the anchor's rotation first.
        int cwSteps = session.inDiagonalMode
                ? (session.furnitureRotation - 1) / 2
                : session.furnitureRotation / 2;
        relPositions.replaceAll(pos -> unrotateCW(pos, cwSteps));

        // Normalize slab world positions to relative rotation-0 space as well.
        List<BlockPos> relSlabPositions = new ArrayList<>();
        for (BlockPos wp : session.slabPositions) {
            relSlabPositions.add(unrotateCW(wp.subtract(anchor), cwSteps));
        }

        // Normalize seat world positions to relative rotation-0 space.
        List<BlockPos> relSeatPositions = new ArrayList<>();
        for (BlockPos wp : session.seatPositions) {
            relSeatPositions.add(unrotateCW(wp.subtract(anchor), cwSteps));
        }
        float seatH = session.furnitureSeatHeight;

        String         fid      = session.targetFurnitureId;
        MultiBlockLayout existing = MultiBlockLayoutManager.get(fid).orElse(null);
        MultiBlockLayout saved;

        if (session.inDiagonalMode && existing != null) {
            // Updating only the diagonal part of an existing layout; preserve straight + slabs/seats.
            saved = MultiBlockLayout.of(fid,
                    existing.getStraightPositions(), relPositions,
                    existing.getStraightSlabPositions(), relSlabPositions,
                    existing.getStraightSeatPositions(), relSeatPositions,
                    seatH);
        } else if (session.inDiagonalMode) {
            // No existing layout — use same positions/slabs/seats for both straight and diagonal.
            saved = MultiBlockLayout.of(fid,
                    relPositions, relPositions,
                    relSlabPositions, relSlabPositions,
                    relSeatPositions, relSeatPositions,
                    seatH);
        } else {
            int[]   bounds    = computeBounds(relPositions);
            boolean symmetric = bounds[0] == bounds[2];
            if (symmetric) {
                // Auto-generate diagonal from straight (null = auto).
                saved = MultiBlockLayout.of(fid, relPositions, null,
                        relSlabPositions, null, relSeatPositions, null, seatH);
            } else if (existing != null && existing.getDiagonalPositions() != null) {
                saved = MultiBlockLayout.of(fid,
                        relPositions, existing.getDiagonalPositions(),
                        relSlabPositions, existing.getDiagonalSlabPositions(),
                        relSeatPositions, existing.getDiagonalSeatPositions(),
                        seatH);
            } else {
                saved = MultiBlockLayout.of(fid, relPositions, null,
                        relSlabPositions, null, relSeatPositions, null, seatH);
            }
            if (symmetric) {
                chat(sp, "§bHitbox is symmetric — diagonal auto-generated.");
            }
        }

        MultiBlockLayoutManager.save(saved);
        MultiBlockLayoutManager.reload();
        MultiBlockLayoutManager.deleteTemp(uuid);
        BoundingBoxSelectorSession.clear(uuid);
        SyncBBSessionPayload.sendTo(sp, null);

        chat(sp, "§a✔ Saved §f" + fid + " §a(" + relPositions.size() + " block"
                + (relPositions.size() == 1 ? "" : "s") + ")");
        return InteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------------
    // Crash-recovery temp file
    // ------------------------------------------------------------------

    /**
     * Writes the current in-progress selection to a temp JSON for crash recovery.
     * Called after every selection change.
     */
    public static void writeTempFile(ServerPlayer sp, BoundingBoxSelectorSession session) {
        if (session.anchorWorldPos == null || session.selectedPositions.isEmpty()) return;
        List<BlockPos> rel = new ArrayList<>();
        for (BlockPos wp : session.selectedPositions) rel.add(wp.subtract(session.anchorWorldPos));
        List<BlockPos> relSlabs = new ArrayList<>();
        for (BlockPos wp : session.slabPositions) relSlabs.add(wp.subtract(session.anchorWorldPos));
        List<BlockPos> relSeats = new ArrayList<>();
        for (BlockPos wp : session.seatPositions) relSeats.add(wp.subtract(session.anchorWorldPos));
        MultiBlockLayoutManager.saveTemp(sp.getStringUUID(),
                MultiBlockLayout.of(session.targetFurnitureId, rel, null,
                        relSlabs, null, relSeats, null, session.furnitureSeatHeight));
    }

    // ------------------------------------------------------------------
    // Utilities
    // ------------------------------------------------------------------

    /** Sends a message to the action bar (transient, non-intrusive). */
    private static void actionBar(ServerPlayer sp, String text) {
        sp.displayClientMessage(Component.literal(text), true);
    }

    /** Sends a message to chat (persistent — used for save confirmation only). */
    private static void chat(ServerPlayer sp, String text) {
        sp.displayClientMessage(Component.literal(text), false);
    }

    private static String fmt(BlockPos pos) {
        return "[" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "]";
    }

    private static int[] computeBounds(List<BlockPos> positions) {
        if (positions.isEmpty()) return new int[]{1, 1, 1};
        int minX = positions.stream().mapToInt(BlockPos::getX).min().orElse(0);
        int maxX = positions.stream().mapToInt(BlockPos::getX).max().orElse(0);
        int minY = positions.stream().mapToInt(BlockPos::getY).min().orElse(0);
        int maxY = positions.stream().mapToInt(BlockPos::getY).max().orElse(0);
        int minZ = positions.stream().mapToInt(BlockPos::getZ).min().orElse(0);
        int maxZ = positions.stream().mapToInt(BlockPos::getZ).max().orElse(0);
        return new int[]{maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1};
    }

    // ------------------------------------------------------------------
    // Rotation helpers (mirrors MultiBlockLayout.rotateCW)
    // ------------------------------------------------------------------

    /**
     * Rotates {@code pos} by {@code steps} × 90° clockwise around the Y-axis.
     * One CW step in Minecraft map-space: (x, z) → (−z, x).
     */
    private static BlockPos rotateCW(BlockPos pos, int steps) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        for (int i = 0; i < steps; i++) {
            int nx = -z; z = x; x = nx;
        }
        return new BlockPos(x, y, z);
    }

    /**
     * Undoes {@code cwSteps} clockwise 90° rotations — equivalent to rotating
     * {@code (4 − cwSteps) % 4} steps clockwise.
     */
    private static BlockPos unrotateCW(BlockPos pos, int cwSteps) {
        return rotateCW(pos, (4 - cwSteps) % 4);
    }
}
