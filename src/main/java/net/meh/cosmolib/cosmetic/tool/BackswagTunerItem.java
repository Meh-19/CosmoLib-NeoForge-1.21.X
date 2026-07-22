package net.meh.cosmolib.cosmetic.tool;

import net.meh.cosmolib.cosmetic.CosmeticSlot;
import net.meh.cosmolib.cosmetic.client.CosmeticClientCache;
import net.meh.cosmolib.cosmetic.offset.BackOffsetManager;
import net.meh.cosmolib.cosmetic.tool.client.BackswagTunerSession;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Developer-only tool for tuning the Y-axis render offset of back cosmetics
 * without restarting the game.
 *
 * <h3>Gates</h3>
 * Only usable in creative mode with operator permission level ≥ 2.
 * If either gate fails, an action-bar message explains the reason and the
 * interaction is ignored.
 *
 * <h3>Workflow</h3>
 * <ol>
 *   <li><b>Equip a back cosmetic</b> via the wardrobe, then hold this item.</li>
 *   <li><b>Right-click</b> → starts a session for that cosmetic.  The current
 *       Y offset is snapshotted so it can be restored on cancel.</li>
 *   <li><b>Scroll up/down</b> → adjusts Y by ±0.01 per tick.
 *       Hold Ctrl while scrolling for ±0.001 fine adjustment.
 *       Changes take effect immediately in the render.</li>
 *   <li><b>Ctrl+Right-click</b> → saves the current Y to
 *       {@code config/cosmolib/back_offsets.json} and ends the session.</li>
 *   <li><b>Shift+Right-click</b> → reverts to the original Y (in memory) and
 *       ends the session without writing to disk.</li>
 * </ol>
 *
 * <p>All session state is managed by {@link BackswagTunerSession}.
 * Offset persistence is handled by {@link BackOffsetManager}.
 * The live HUD overlay is rendered by
 * {@link net.meh.cosmolib.cosmetic.tool.client.BackswagTunerHud}.
 *
 * <p>This tool is purely client-side — no packets are sent and no server state
 * is modified.  The {@code back_offsets.json} file is a client config file.
 */
public class BackswagTunerItem extends Item {

    public BackswagTunerItem(Properties props) {
        super(props);
    }

    // ------------------------------------------------------------------
    // Right-click interaction
    // ------------------------------------------------------------------

    /**
     * Handles right-click interactions for the tuner.
     *
     * <p>All session logic runs on the <em>client side only</em>.  The server
     * returns {@link net.minecraft.world.InteractionResult#PASS PASS} immediately
     * so no server state is touched.
     *
     * <h4>Client branch behaviour</h4>
     * <ul>
     *   <li>Gate failure → action-bar message, {@code FAIL}.</li>
     *   <li>Ctrl+Click (session active) → save, clear session, {@code SUCCESS}.</li>
     *   <li>Shift+Click (session active) → cancel, clear session, {@code SUCCESS}.</li>
     *   <li>Any click (session already active, no modifier) → reminder message,
     *       {@code SUCCESS}.</li>
     *   <li>Plain click (no session) → start session for equipped back cosmetic,
     *       {@code SUCCESS}, or {@code FAIL} if none equipped.</li>
     * </ul>
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // All logic is client-side only — this tool has no server involvement.
        if (!level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        // Gate: creative mode
        if (!player.isCreative()) {
            player.displayClientMessage(
                    Component.literal("§cBackswag Tuner requires creative mode."), true);
            return InteractionResultHolder.fail(stack);
        }

        // Gate: operator permission level 2+
        if (!player.hasPermissions(2)) {
            player.displayClientMessage(
                    Component.literal("§cBackswag Tuner requires operator level 2+."), true);
            return InteractionResultHolder.fail(stack);
        }

        // Detect modifiers — client-only classes accessed only inside the isClientSide branch
        boolean ctrl  = net.minecraft.client.gui.screens.Screen.hasControlDown();
        boolean shift = net.minecraft.client.gui.screens.Screen.hasShiftDown();

        BackswagTunerSession session = BackswagTunerSession.INSTANCE;

        // ── Active session ────────────────────────────────────────────
        if (session.sessionActive) {
            if (ctrl) {
                // Ctrl+Click → SAVE
                BackOffsetManager.save();
                player.displayClientMessage(Component.literal(
                        "§a✓ Saved Y offset for §f" + session.activeCosmeticId
                                + "§a: " + String.format("%.5f", session.currentY)), true);
                session.clear();
            } else if (shift) {
                // Shift+Click → CANCEL
                BackOffsetManager.cancelChanges(session.activeCosmeticId, session.originalY);
                player.displayClientMessage(Component.literal(
                        "§cCancelled — reverted to "
                                + String.format("%.5f", session.originalY)), true);
                session.clear();
            } else {
                // Plain click while session active → remind
                player.displayClientMessage(Component.literal(
                        "§7Tuning §f" + session.activeCosmeticId
                                + " §7| Ctrl+Click: Save  Shift+Click: Cancel"), true);
            }
            return InteractionResultHolder.success(stack);
        }

        // ── No active session: start one ─────────────────────────────
        ItemStack back = CosmeticClientCache.getEquipped(player, CosmeticSlot.BACK);
        if (back.isEmpty()) {
            player.displayClientMessage(
                    Component.literal("§eEquip a back cosmetic first."), true);
            return InteractionResultHolder.fail(stack);
        }

        ResourceLocation cosmeticId = BuiltInRegistries.ITEM.getKey(back.getItem());
        double originalY = BackOffsetManager.getY(cosmeticId);
        session.start(cosmeticId, originalY);

        player.displayClientMessage(Component.literal(
                "§aTuning §f" + cosmeticId
                        + " §7— scroll to adjust Y | Ctrl+Click to save | Shift+Click to cancel"),
                true);

        return InteractionResultHolder.success(stack);
    }
}
