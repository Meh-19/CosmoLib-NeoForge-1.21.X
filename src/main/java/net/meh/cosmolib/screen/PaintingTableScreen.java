package net.meh.cosmolib.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.meh.cosmolib.paint.PaintColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

public class PaintingTableScreen extends AbstractContainerScreen<PaintingTableMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "textures/gui/painting_table.png");
    private static final ResourceLocation BUTTON =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "textures/gui/paint_button.png");
    private static final ResourceLocation SLOT =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "textures/gui/inventory_slot_carpentry.png");

    private static final int PANEL_W = 175;
    private static final int PANEL_H = 200;

    // 3×3 color grid positions relative to GUI origin (matches Argon texture)
    private static final int[][] COLOR_POSITIONS = {
            {62, 24}, {80, 24}, {98, 24},
            {62, 42}, {80, 42}, {98, 42},
            {62, 60}, {80, 60}, {98, 60}
    };

    // Shade row positions
    private static final int[] SHADE_X = {26, 44, 62, 80, 98, 116, 134};
    private static final int   SHADE_Y = 96;

    public PaintingTableScreen(PaintingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth      = PANEL_W;
        imageHeight     = PANEL_H;
        titleLabelX     = 8;
        titleLabelY     = 6;
        inventoryLabelX = 8;
        inventoryLabelY = PANEL_H - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        drawColorButtons(graphics);
        drawShadeButtons(graphics);
    }

    private void drawColorButtons(GuiGraphics graphics) {
        PaintColor[] colors  = PaintColor.values();
        int          selected = menu.getSelectedColorIndex();

        for (int i = 0; i < colors.length && i < COLOR_POSITIONS.length; i++) {
            int x = leftPos + COLOR_POSITIONS[i][0];
            int y = topPos  + COLOR_POSITIONS[i][1];

            if (selected == i)
                graphics.blit(SLOT, x - 2, y - 2, 0f, 0f, 20, 20, 20, 20);

            tintedBlit(graphics, BUTTON, x, y, colors[i].getShades()[colors[i].getDefaultIndex()]);
        }
    }

    private void drawShadeButtons(GuiGraphics graphics) {
        if (!menu.hasPaintableInput() || menu.getSelectedColorIndex() < 0) return;
        PaintColor color = menu.getSelectedColor();
        if (color == null) return;

        int   selected = menu.getSelectedShadeIndex();
        int[] shades   = color.getShades();

        for (int i = 0; i < shades.length && i < SHADE_X.length; i++) {
            int x = leftPos + SHADE_X[i];
            int y = topPos  + SHADE_Y;

            if (selected == i)
                graphics.blit(SLOT, x - 2, y - 2, 0f, 0f, 20, 20, 20, 20);

            tintedBlit(graphics, BUTTON, x, y, shades[i]);
        }
    }

    private void tintedBlit(GuiGraphics graphics, ResourceLocation tex, int x, int y, int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8)  & 0xFF) / 255f;
        float b = ( rgb        & 0xFF) / 255f;
        RenderSystem.setShaderColor(r, g, b, 1f);
        graphics.blit(tex, x, y, 0f, 0f, 16, 16, 16, 16);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        PaintColor[] colors = PaintColor.values();
        for (int i = 0; i < colors.length && i < COLOR_POSITIONS.length; i++) {
            int x = leftPos + COLOR_POSITIONS[i][0];
            int y = topPos  + COLOR_POSITIONS[i][1];
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                playClick();
                if (minecraft != null && minecraft.gameMode != null)
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                return true;
            }
        }

        if (menu.hasPaintableInput() && menu.getSelectedColorIndex() >= 0) {
            for (int i = 0; i < SHADE_X.length; i++) {
                int x = leftPos + SHADE_X[i];
                int y = topPos  + SHADE_Y;
                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                    playClick();
                    if (minecraft != null && minecraft.gameMode != null)
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, PaintColor.values().length + i);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playClick() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0f));
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
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }
}
