package net.meh.cosmolib.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.meh.cosmolib.paint.PaintColor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Painting Table GUI.
 *
 * Layout (176 × 196 background):
 *   Input slot:  26, 42
 *   Output slot: 134, 42
 *   Color row (9 buttons, 14×14 each): y=10, x=8, 8+16, …
 *   Shade row (7 buttons, 14×14 each): y=26, centered
 *   Player inv:  y=119  (3 rows) + y=177 (hotbar)
 *
 * The actual button textures live in the GUI texture atlas.
 * Color button IDs: 0–8 (PaintColor ordinals).
 * Shade button IDs: 9–15 (shade index + PaintColor.values().length).
 */
public class PaintingTableScreen extends AbstractContainerScreen<PaintingTableMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "textures/gui/painting_table.png");

    private static final int GUI_W = 176;
    private static final int GUI_H = 210;

    // Color button grid
    private static final int COLOR_BTN_SIZE = 14;
    private static final int COLOR_BTN_Y    = 10;
    private static final int SHADE_BTN_Y    = 27;

    public PaintingTableScreen(PaintingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth  = GUI_W;
        imageHeight = GUI_H;
    }

    @Override
    protected void init() {
        super.init();
        addColorButtons();
        addShadeButtons();
    }

    private void addColorButtons() {
        PaintColor[] colors = PaintColor.values();
        int totalW = colors.length * (COLOR_BTN_SIZE + 2);
        int startX = leftPos + (GUI_W - totalW) / 2;

        for (int i = 0; i < colors.length; i++) {
            final int colorIdx = i;
            int x = startX + i * (COLOR_BTN_SIZE + 2);
            int y = topPos + COLOR_BTN_Y;
            int[] shades = colors[i].getShades();
            int rgb = shades[colors[i].getDefaultIndex()];

            addRenderableWidget(new ColorButton(x, y, COLOR_BTN_SIZE, rgb, () ->
                    handleColorClick(colorIdx)));
        }
    }

    private void addShadeButtons() {
        for (int i = 0; i < 7; i++) {
            final int shadeIdx = i;
            int x = leftPos + 8 + i * (COLOR_BTN_SIZE + 2);
            int y = topPos + SHADE_BTN_Y;
            addRenderableWidget(new ShadeButton(x, y, COLOR_BTN_SIZE, shadeIdx, menu, () ->
                    handleShadeClick(shadeIdx)));
        }
    }

    private void handleColorClick(int colorIdx) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, colorIdx);
        }
    }

    private void handleShadeClick(int shadeIdx) {
        if (minecraft != null && minecraft.gameMode != null) {
            int id = PaintColor.values().length + shadeIdx;
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY + 14, 0x404040, false);
    }

    // ------------------------------------------------------------------
    // Inline button widgets
    // ------------------------------------------------------------------

    private static class ColorButton extends net.minecraft.client.gui.components.AbstractWidget {
        private final int rgb;
        private final Runnable onClick;

        ColorButton(int x, int y, int size, int rgb, Runnable onClick) {
            super(x, y, size, size, Component.empty());
            this.rgb     = rgb;
            this.onClick = onClick;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            int argb = 0xFF000000 | (rgb & 0xFFFFFF);
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), argb);
            if (isHovered()) graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFFFFFFFF);
        }

        @Override
        public void onPress() { onClick.run(); }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput neo) {}
    }

    private static class ShadeButton extends net.minecraft.client.gui.components.AbstractWidget {
        private final int              shadeIdx;
        private final PaintingTableMenu menu;
        private final Runnable          onClick;

        ShadeButton(int x, int y, int size, int shadeIdx, PaintingTableMenu menu, Runnable onClick) {
            super(x, y, size, size, Component.empty());
            this.shadeIdx = shadeIdx;
            this.menu     = menu;
            this.onClick  = onClick;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            PaintColor selected = menu.getSelectedColor();
            if (selected == null) {
                graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF555555);
                return;
            }
            int[] shades = selected.getShades();
            if (shadeIdx >= shades.length) return;
            int argb = 0xFF000000 | (shades[shadeIdx] & 0xFFFFFF);
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), argb);

            // Highlight currently selected shade
            if (menu.getSelectedShadeIndex() == shadeIdx) {
                graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFFFFFFFF);
            }
        }

        @Override
        public void onPress() { onClick.run(); }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput neo) {}
    }
}
