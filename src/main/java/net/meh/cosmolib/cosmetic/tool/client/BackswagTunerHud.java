package net.meh.cosmolib.cosmetic.tool.client;

import net.meh.cosmolib.registry.CosmoLibItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Renders the live Backswag Tuner overlay while a tuning session is active.
 *
 * <h3>Content (top-left corner at x=4, y=4)</h3>
 * <pre>
 *   Backswag Tuner | cosmolib:cosmo_robe
 *   Y: -0.57625
 *   Scroll: ±0.01  |  Ctrl+Scroll: ±0.001
 *   Ctrl+Click: Save  |  Shift+Click: Cancel  |  Ctrl+C: Copy  |  Ctrl+P: Paste
 *   Clipboard: -0.42000        ← only shown when a value has been copied
 * </pre>
 *
 * <p>Nothing is drawn when:
 * <ul>
 *   <li>{@link BackswagTunerSession#sessionActive} is {@code false}.</li>
 *   <li>The local player is not holding a
 *       {@link net.meh.cosmolib.cosmetic.tool.BackswagTunerItem} in either hand.</li>
 * </ul>
 *
 * <p>Registered to {@link RenderGuiEvent.Post} on the client game bus by
 * {@link net.meh.cosmolib.cosmetic.tool.client.BackswagTunerEventHandler}.
 */
public final class BackswagTunerHud {

    /** ARGB white — fully opaque, no shadow. */
    private static final int TEXT_COLOR = 0xFFFFFF;

    /** Vertical spacing between lines, in screen pixels. */
    private static final int LINE_HEIGHT = 10;

    /** Left edge of the overlay. */
    private static final int ORIGIN_X = 4;

    /** Top edge of the overlay. */
    private static final int ORIGIN_Y = 4;

    private BackswagTunerHud() {}

    /**
     * Entry point called from the {@link RenderGuiEvent.Post} subscriber in
     * {@link BackswagTunerEventHandler}.
     *
     * <p>Draws four lines of white text in the top-left corner of the screen.
     * Uses {@link GuiGraphics#drawString} without drop shadow for a clean look.
     *
     * @param event the post-GUI render event providing the {@link GuiGraphics} context
     */
    public static void render(RenderGuiEvent.Post event) {
        BackswagTunerSession session = BackswagTunerSession.INSTANCE;
        if (!session.sessionActive) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Only render while the tool is actually held
        if (!mc.player.getMainHandItem().is(CosmoLibItems.BACKSWAG_TUNER.get())
                && !mc.player.getOffhandItem().is(CosmoLibItems.BACKSWAG_TUNER.get())) {
            return;
        }

        GuiGraphics gfx = event.getGuiGraphics();
        String idStr = session.activeCosmeticId != null
                ? session.activeCosmeticId.toString() : "?";

        int y = ORIGIN_Y;
        gfx.drawString(mc.font,
                "Backswag Tuner | " + idStr,
                ORIGIN_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;
        gfx.drawString(mc.font,
                String.format("Y: %.5f", session.currentY),
                ORIGIN_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;
        // ± encoded as the Unicode plus-minus sign ±
        gfx.drawString(mc.font,
                "Scroll: ±0.01  |  Ctrl+Scroll: ±0.001",
                ORIGIN_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;
        gfx.drawString(mc.font,
                "Ctrl+Click: Save  |  Shift+Click: Cancel  |  Ctrl+C: Copy  |  Ctrl+P: Paste",
                ORIGIN_X, y, TEXT_COLOR, false);

        // Fifth line: clipboard value — only shown once something has been copied
        if (session.hasClipboard) {
            y += LINE_HEIGHT;
            gfx.drawString(mc.font,
                    String.format("Clipboard: %.5f", session.clipboardY),
                    ORIGIN_X, y, 0xAAAAAA, false); // grey — clearly distinct from live Y
        }
    }
}
