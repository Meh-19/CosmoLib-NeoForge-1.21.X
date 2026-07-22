package net.meh.cosmolib.toolskin.client;

import net.meh.cosmolib.toolskin.OriginalItemTooltipComponent;
import net.meh.cosmolib.toolskin.ToolSkinData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-side renderer for {@link OriginalItemTooltipComponent}.
 *
 * <p>Renders a compact single-block tooltip entry immediately below the item name:
 * <pre>
 *   [8×8 item icon]  + Easter  Skin
 *   2021 Easter Exclusive          ← only when exclusiveTag is set
 * </pre>
 *
 * <p>The item icon is rendered by scaling the standard 16×16 item render down to
 * 8×8, so it works for <b>any</b> item — vanilla, modded, or custom — with no
 * font glyph or texture-registration requirement.
 *
 * <p>Registered via
 * {@link net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent}
 * in {@link ToolSkinClientEventHandler}.
 */
@OnlyIn(Dist.CLIENT)
public class ClientOriginalItemTooltipComponent implements ClientTooltipComponent {

    private static final int ICON_SIZE    = 8;   // rendered px (scaled from 16)
    private static final int ICON_PADDING = 3;   // gap between icon and text
    private static final int LINE_HEIGHT  = 11;  // height of the skin info line
    private static final int TAG_HEIGHT   = 10;  // height of the exclusive tag line
    private static final int GREY         = 0x888888;

    private final ToolSkinData data;

    public ClientOriginalItemTooltipComponent(ToolSkinData data) {
        this.data = data;
    }

    /** Factory method used by the NeoForge tooltip component registry. */
    public static ClientOriginalItemTooltipComponent of(OriginalItemTooltipComponent component) {
        return new ClientOriginalItemTooltipComponent(component.data());
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    @Override
    public int getHeight() {
        return LINE_HEIGHT + (data.exclusiveTag() != null ? TAG_HEIGHT : 0);
    }

    @Override
    public int getWidth(Font font) {
        int skinLine = ICON_SIZE + ICON_PADDING
                + font.width(" + " + data.skinSetName() + " Skin");
        if (data.exclusiveTag() == null) return skinLine;
        return Math.max(skinLine, font.width(data.exclusiveTag()));
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics g) {
        // ── Item icon (16×16 scaled to 8×8) ──────────────────────────
        ItemStack original = data.createOriginalStack();
        if (!original.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(x, y + 1.5f, 0);
            g.pose().scale(0.5f, 0.5f, 1.0f);
            g.renderItem(original, 0, 0);
            g.pose().popPose();
        }

        // ── " + SetName Skin" text (all grey) ─────────────────────────
        int tx = x + ICON_SIZE + ICON_PADDING;
        int ty = y + 2;

        g.drawString(font, " + " + data.skinSetName() + " Skin", tx, ty, GREY);

        // ── Exclusive tag ─────────────────────────────────────────────
        if (data.exclusiveTag() != null) {
            g.drawString(font, data.exclusiveTag(), x, y + LINE_HEIGHT, data.exclusiveTagColor());
        }
    }
}
