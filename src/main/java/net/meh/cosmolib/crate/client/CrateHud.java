package net.meh.cosmolib.crate.client;

import net.meh.cosmolib.crate.entity.CrateEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Renders the crate action-bar HUD while the player has an active crate lock.
 *
 * <p>Displayed only when {@link CrateCameraController#isLocked()} is true and the
 * locked entity is a {@link CrateEntity}.
 *
 * <ul>
 *   <li>OPENING / REROLLING state → "Opening crate…" hint</li>
 *   <li>LOOP state → crouch-to-collect hint + reroll count</li>
 * </ul>
 *
 * <p>Register {@link #onRenderGui(RenderGuiEvent.Post)} on the NeoForge GAME bus
 * (client side, {@link Dist#CLIENT}).
 */
@OnlyIn(Dist.CLIENT)
public final class CrateHud {

    private static final int TEXT_WHITE  = 0xFFFFFF;
    private static final int TEXT_GREEN  = 0x55FF55;
    private static final int TEXT_GREY   = 0xAAAAAA;
    private static final int BG_COLOR    = 0x99000000; // ~60 % alpha black

    private CrateHud() {}

    /**
     * Called on {@link RenderGuiEvent.Post}. Renders the crate HUD when applicable.
     *
     * @param event the post-render GUI event
     */
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!CrateCameraController.isLocked()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        int entityId = CrateCameraController.getTargetEntityId();
        Entity rawEntity = mc.level.getEntity(entityId);
        if (!(rawEntity instanceof CrateEntity crate)) return;

        GuiGraphics g    = event.getGuiGraphics();
        Font        font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        int state = crate.getState();

        // Vanilla survival bars (health, hunger, armor, XP) occupy roughly the bottom 60 px.
        // Render above them: primary hint at sh-85, secondary info at sh-70.
        if (state == CrateEntity.STATE_OPENING || state == CrateEntity.STATE_REROLLING) {
            renderSingleLine(g, font, sw, sh - 85, "Opening crate...", TEXT_WHITE);
        } else if (state == CrateEntity.STATE_LOOP) {
            renderSingleLine(g, font, sw, sh - 85, "Crouch to collect rewards!", TEXT_GREEN);

            int rerolls = crate.getRerollsLeft();
            if (rerolls > 0) {
                String left  = "Punch to re-roll ";
                String right = "(" + rerolls + " Remaining)";
                int total = font.width(left) + font.width(right);
                int startX = sw / 2 - total / 2;
                int y      = sh - 70;

                // Background behind both segments
                g.fill(startX - 3, y - 2, startX + total + 3, y + font.lineHeight + 2, BG_COLOR);
                g.drawString(font, left,  startX,                    y, TEXT_GREY,  false);
                g.drawString(font, right, startX + font.width(left), y, TEXT_WHITE, false);
            } else {
                renderSingleLine(g, font, sw, sh - 70, "No Rerolls Remaining", TEXT_GREY);
            }
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static void renderSingleLine(GuiGraphics g, Font font,
                                          int screenWidth, int y,
                                          String text, int color) {
        int tw    = font.width(text);
        int startX = screenWidth / 2 - tw / 2;
        g.fill(startX - 3, y - 2, startX + tw + 3, y + font.lineHeight + 2, BG_COLOR);
        g.drawCenteredString(font, text, screenWidth / 2, y, color);
    }
}
