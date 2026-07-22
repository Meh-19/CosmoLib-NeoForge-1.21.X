package net.meh.cosmolib.cosmetic.tool.client;

import net.meh.cosmolib.CosmoLib;
import net.meh.cosmolib.cosmetic.CosmeticSlot;
import net.meh.cosmolib.cosmetic.client.CosmeticClientCache;
import net.meh.cosmolib.cosmetic.offset.BackOffsetManager;
import net.meh.cosmolib.registry.CosmoLibItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only game-bus event handler for the Backswag Tuner tool.
 *
 * <p>Handles three events:
 * <ul>
 *   <li>{@link InputEvent.MouseScrollingEvent} — intercepts scroll wheel input
 *       while a tuning session is active and the player holds the tool, adjusting
 *       the Y offset live and preventing hotbar slot cycling.</li>
 *   <li>{@link InputEvent.Key} — handles Ctrl+C (copy current Y to clipboard) and
 *       Ctrl+P (paste clipboard Y into the active or a new session).</li>
 *   <li>{@link RenderGuiEvent.Post} — delegates to {@link BackswagTunerHud} to draw
 *       the overlay in the top-left corner of the screen.</li>
 * </ul>
 *
 * <p>Registered on the {@link net.neoforged.bus.api.IEventBus GAME bus} (default for
 * {@link EventBusSubscriber}) with {@link Dist#CLIENT} so it is never loaded on
 * dedicated servers.
 */
@EventBusSubscriber(modid = CosmoLib.MOD_ID, value = Dist.CLIENT)
public final class BackswagTunerEventHandler {

    private BackswagTunerEventHandler() {}

    // ------------------------------------------------------------------
    // Scroll — live Y adjustment
    // ------------------------------------------------------------------

    /**
     * Intercepts scroll wheel input while a Backswag Tuner session is active.
     *
     * <h4>Behaviour</h4>
     * <ul>
     *   <li>Normal scroll: adjusts {@link BackswagTunerSession#currentY} by ±0.01 per
     *       scroll notch (scroll up = increase Y).</li>
     *   <li>Ctrl held: adjusts by ±0.001 per notch for fine-grained positioning.</li>
     *   <li>After each adjustment, {@link BackOffsetManager#setY} is called so the
     *       render updates immediately without a save.</li>
     *   <li>The event is cancelled so the hotbar slot does not change.</li>
     * </ul>
     *
     * <p>Silently ignored when:
     * <ul>
     *   <li>No session is active.</li>
     *   <li>The local player is not holding the tuner in either hand.</li>
     *   <li>A GUI screen is open (e.g. inventory, chat).</li>
     *   <li>The vertical scroll delta is zero.</li>
     * </ul>
     *
     * @param event the mouse-scrolling event
     */
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        BackswagTunerSession session = BackswagTunerSession.INSTANCE;
        if (!session.sessionActive) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Only intercept while the tool is held
        if (!mc.player.getMainHandItem().is(CosmoLibItems.BACKSWAG_TUNER.get())
                && !mc.player.getOffhandItem().is(CosmoLibItems.BACKSWAG_TUNER.get())) {
            return;
        }

        // Don't intercept scroll if a screen (inventory, chat, etc.) is open
        if (mc.screen != null) return;

        double delta = event.getScrollDeltaY();
        if (delta == 0.0) return;

        // Ctrl held = fine adjustment (0.001 per notch), otherwise coarse (0.01)
        double step = Screen.hasControlDown() ? 0.001 : 0.01;
        // Scroll up (positive delta) → increase Y; scroll down → decrease Y
        session.currentY += (delta > 0.0 ? step : -step);

        BackOffsetManager.setY(session.activeCosmeticId, session.currentY);

        mc.player.displayClientMessage(
                Component.literal(String.format("Y: %.5f", session.currentY)), true);

        // Prevent hotbar slot cycling
        event.setCanceled(true);
    }

    // ------------------------------------------------------------------
    // Keyboard — Ctrl+C copy / Ctrl+P paste
    // ------------------------------------------------------------------

    /**
     * Handles Ctrl+C and Ctrl+P keyboard shortcuts for the offset clipboard.
     *
     * <h4>Ctrl+C — Copy</h4>
     * <ul>
     *   <li>Session active: copies {@link BackswagTunerSession#currentY} (the live
     *       tuned value) to the clipboard.</li>
     *   <li>No session: copies whatever Y offset the equipped back cosmetic currently
     *       has in {@link BackOffsetManager} (useful for reading an existing entry
     *       without starting a tuning session).</li>
     *   <li>In both cases the value is also written to the system clipboard as a
     *       ready-to-paste JSON snippet, e.g.
     *       {@code "cosmolib:cosmo_robe": -0.57625}.</li>
     * </ul>
     *
     * <h4>Ctrl+P — Paste</h4>
     * <ul>
     *   <li>Session active: overwrites {@link BackswagTunerSession#currentY} with the
     *       clipboard value and pushes it into {@link BackOffsetManager} so the render
     *       updates immediately.</li>
     *   <li>No session: automatically starts a session for the currently equipped back
     *       cosmetic and applies the clipboard value, as if the player had right-clicked
     *       and then pasted.</li>
     *   <li>Does nothing if the clipboard is empty ({@link BackswagTunerSession#hasClipboard}
     *       is {@code false}).</li>
     * </ul>
     *
     * <p>Silently ignored when no screen is open, the tool is not held, or
     * (for Ctrl+C with no session) no back cosmetic is equipped.
     *
     * @param event the keyboard input event
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // Only fire on key-press (not release or repeat) with Ctrl held
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        if (!Screen.hasControlDown()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        // Only active while the tool is held in either hand
        if (!mc.player.getMainHandItem().is(CosmoLibItems.BACKSWAG_TUNER.get())
                && !mc.player.getOffhandItem().is(CosmoLibItems.BACKSWAG_TUNER.get())) {
            return;
        }

        BackswagTunerSession session = BackswagTunerSession.INSTANCE;

        // ── Ctrl+C — copy ────────────────────────────────────────────
        if (event.getKey() == GLFW.GLFW_KEY_C) {
            double valueToCopy;
            ResourceLocation sourceId;

            if (session.sessionActive) {
                // Copy the live tuned value
                valueToCopy = session.currentY;
                sourceId    = session.activeCosmeticId;
            } else {
                // Copy the stored value for the equipped cosmetic (no session needed)
                ItemStack back = CosmeticClientCache.getEquipped(mc.player, CosmeticSlot.BACK);
                if (back.isEmpty()) return; // nothing to copy
                sourceId    = BuiltInRegistries.ITEM.getKey(back.getItem());
                valueToCopy = BackOffsetManager.getY(sourceId);
            }

            session.clipboardY  = valueToCopy;
            session.hasClipboard = true;

            // Also push a JSON snippet to the system clipboard for external use
            String snippet = "\"" + sourceId + "\": " + String.format("%.5f", valueToCopy);
            mc.keyboardHandler.setClipboard(snippet);

            mc.player.displayClientMessage(
                    Component.literal("§aCopied §fY: " + String.format("%.5f", valueToCopy)
                            + " §7from §f" + sourceId), true);
            return;
        }

        // ── Ctrl+P — paste ───────────────────────────────────────────
        if (event.getKey() == GLFW.GLFW_KEY_P) {
            if (!session.hasClipboard) {
                mc.player.displayClientMessage(
                        Component.literal("§eClipboard is empty — use Ctrl+C to copy a value first."),
                        true);
                return;
            }

            if (!session.sessionActive) {
                // Auto-start a session for the equipped cosmetic
                ItemStack back = CosmeticClientCache.getEquipped(mc.player, CosmeticSlot.BACK);
                if (back.isEmpty()) {
                    mc.player.displayClientMessage(
                            Component.literal("§eEquip a back cosmetic first."), true);
                    return;
                }
                ResourceLocation cosmeticId = BuiltInRegistries.ITEM.getKey(back.getItem());
                double existingY = BackOffsetManager.getY(cosmeticId);
                session.start(cosmeticId, existingY);
            }

            // Apply clipboard value to the active session
            session.currentY = session.clipboardY;
            BackOffsetManager.setY(session.activeCosmeticId, session.currentY);

            mc.player.displayClientMessage(
                    Component.literal("§aPasted §fY: " + String.format("%.5f", session.currentY)
                            + " §7→ §f" + session.activeCosmeticId
                            + " §7(Ctrl+Click to save)"), true);
        }
    }

    // ------------------------------------------------------------------
    // HUD overlay
    // ------------------------------------------------------------------

    /**
     * Delegates to {@link BackswagTunerHud#render} to draw the status overlay
     * when a session is active and the tool is held.
     *
     * @param event the post-GUI render event
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        BackswagTunerHud.render(event);
    }
}
