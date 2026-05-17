package net.meh.cosmolib.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.meh.cosmolib.paint.FinishType;
import net.meh.cosmolib.paint.PaintColor;
import net.meh.cosmolib.paint.PaintData;
import net.meh.cosmolib.registry.CosmoLibItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

public class PaintingTableScreen extends AbstractContainerScreen<PaintingTableMenu> {

    // -----------------------------------------------------------------------
    // Textures
    // -----------------------------------------------------------------------
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "textures/gui/painting_table.png");
    private static final ResourceLocation BUTTON =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "textures/gui/paint_button.png");
    private static final ResourceLocation SLOT =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "textures/gui/inventory_slot_carpentry.png");

    /** Finish atlas bound to Sampler3 so the entity shader can sample it. */
    private static final ResourceLocation FINISH_ATLAS =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "textures/misc/finish_atlas.png");

    // Full-panel overlay sheets (256×256; content starts at UV y=6)
    private static final ResourceLocation FINISHES_OFF =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "textures/gui/carpentry_table_finishes_off.png");
    private static final ResourceLocation FINISHES_ON =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "textures/gui/carpentry_table_finishes_on.png");
    private static final ResourceLocation ARROW_LEFT_ON =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "textures/gui/carpentry_table_arrow_left_on.png");
    private static final ResourceLocation ARROW_LEFT_OFF =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "textures/gui/carpentry_table_arrow_left_off.png");
    private static final ResourceLocation ARROW_RIGHT_ON =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "textures/gui/carpentry_table_arrow_right_on.png");
    private static final ResourceLocation ARROW_RIGHT_OFF =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "textures/gui/carpentry_table_arrow_right_off.png");

    /** All overlay sheets are 256×256 with panel content starting at UV y=6. */
    private static final int OVERLAY_TEX_SIZE = 256;
    private static final int OVERLAY_UV_Y     = 6;

    // -----------------------------------------------------------------------
    // Layout — panel
    // -----------------------------------------------------------------------
    private static final int PANEL_W = 175;
    private static final int PANEL_H = 200;

    // 3×3 color grid (panel-relative)
    private static final int[][] COLOR_POSITIONS = {
            {62, 24}, {80, 24}, {98, 24},
            {62, 42}, {80, 42}, {98, 42},
            {62, 60}, {80, 60}, {98, 60}
    };

    // Shade / finish slot row (panel-relative; shared by both modes)
    private static final int[] SHADE_X = {26, 44, 62, 80, 98, 116, 134};
    private static final int   SHADE_Y = 96;

    // -----------------------------------------------------------------------
    // Layout — interactive overlay elements (panel-relative)
    // Measured from pixel-diff analysis of the 256×256 overlay sheets.
    // UV y=6 in the sheet maps to panel y=0, so panel_y = image_y - 6.
    // -----------------------------------------------------------------------

    /** Finish-mode toggle button (the paint icon). */
    private static final int FINISH_BTN_X = 79;
    private static final int FINISH_BTN_Y = 84;
    private static final int FINISH_BTN_W = 18;
    private static final int FINISH_BTN_H = 13;

    /** Left pagination arrow. */
    private static final int ARROW_LEFT_X = 14;
    private static final int ARROW_Y      = 102;
    private static final int ARROW_W      = 10;
    private static final int ARROW_H      = 16;

    /** Right pagination arrow. */
    private static final int ARROW_RIGHT_X = 152;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public PaintingTableScreen(PaintingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth      = PANEL_W;
        imageHeight     = PANEL_H;
        titleLabelX     = 8;
        titleLabelY     = 6;
        inventoryLabelX = 8;
        inventoryLabelY = PANEL_H - 94;
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        // 1. Main background panel
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // 2. Finish overlay — always drawn (shows toggle button + shade row bg)
        drawOverlay(graphics, menu.isFinishMode() ? FINISHES_ON : FINISHES_OFF);

        // 3. In finish mode: draw arrow overlays and finish slot buttons
        if (menu.isFinishMode()) {
            int page    = menu.getFinishPage();
            int maxPage = (PaintingTableMenu.FINISH_DISPLAY_ORDER.length - 1)
                          / PaintingTableMenu.FINISHES_PER_PAGE;
            drawOverlay(graphics, page > 0       ? ARROW_LEFT_ON  : ARROW_LEFT_OFF);
            drawOverlay(graphics, page < maxPage  ? ARROW_RIGHT_ON : ARROW_RIGHT_OFF);
            drawFinishSlots(graphics);
        } else {
            // 4. Color mode: color grid + shade row
            drawColorButtons(graphics);
            drawShadeButtons(graphics);
        }
    }

    /**
     * Blit a 256×256 full-panel overlay sheet.
     * Content starts at UV y=6; we render it 6 px below the panel top so the
     * icon aligns with the shade row area rather than the title bar.
     */
    private void drawOverlay(GuiGraphics graphics, ResourceLocation tex) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        graphics.blit(tex,
                leftPos, topPos + 6,
                0f, OVERLAY_UV_Y,
                imageWidth, imageHeight - 6,
                OVERLAY_TEX_SIZE, OVERLAY_TEX_SIZE);
    }

    /** 3×3 color grid. */
    private void drawColorButtons(GuiGraphics graphics) {
        PaintColor[] colors   = PaintColor.values();
        int          selected = menu.getSelectedColorIndex();

        for (int i = 0; i < colors.length && i < COLOR_POSITIONS.length; i++) {
            int x = leftPos + COLOR_POSITIONS[i][0];
            int y = topPos  + COLOR_POSITIONS[i][1];

            if (selected == i)
                graphics.blit(SLOT, x - 2, y - 2, 0f, 0f, 20, 20, 20, 20);

            tintedBlit(graphics, BUTTON, x, y, 16, 16,
                       colors[i].getShades()[colors[i].getDefaultIndex()]);
        }
    }

    /** Shade row — only rendered in color mode when a color is selected. */
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

            tintedBlit(graphics, BUTTON, x, y, 16, 16, shades[i]);
        }
    }

    /**
     * Finish slots — replace the shade row in finish mode.
     * Renders a {@code finish_preview} ItemStack for each slot so the item
     * rendering pipeline (including the PaintColorProvider and any finish
     * shader hooks) fires exactly as it would on a real painted item.
     * The model is a flat 16×16 square matching the paint button sprite.
     */
    private void drawFinishSlots(GuiGraphics graphics) {
        if (!menu.hasPaintableInput()) return;

        // Bind the finish atlas to Sampler3 before any renderItem() call so the
        // entity shader's finishGet() can sample it (otherwise all atlas-dependent
        // finishes — Rainbow, Gold, Galaxy, Bubble, Floral, Chrome, Matrix, Snow —
        // would multiply by black and appear solid black).
        RenderSystem.setShaderTexture(3, FINISH_ATLAS);

        int selectedLocal = menu.getSelectedFinishLocalSlot();

        for (int i = 0; i < PaintingTableMenu.FINISHES_PER_PAGE; i++) {
            FinishType finish = menu.getFinishForSlot(i);
            if (finish == null) continue;

            int x = leftPos + SHADE_X[i];
            int y = topPos  + SHADE_Y;

            if (selectedLocal == i)
                graphics.blit(SLOT, x - 2, y - 2, 0f, 0f, 20, 20, 20, 20);

            ItemStack preview = new ItemStack(CosmoLibItems.FINISH_PREVIEW.get());
            PaintData.applyColor(preview, finish.getMagicRgb());
            graphics.renderItem(preview, x, y);
        }
    }

    private void tintedBlit(GuiGraphics graphics, ResourceLocation tex,
                             int x, int y, int w, int h, int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >>  8) & 0xFF) / 255f;
        float b = ( rgb        & 0xFF) / 255f;
        RenderSystem.setShaderColor(r, g, b, 1f);
        graphics.blit(tex, x, y, 0f, 0f, w, h, w, h);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    // -----------------------------------------------------------------------
    // Input handling
    // -----------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        // Finish toggle — always active
        if (isHovered(mouseX, mouseY, leftPos + FINISH_BTN_X, topPos + FINISH_BTN_Y,
                      FINISH_BTN_W, FINISH_BTN_H)) {
            playClick();
            sendButton(PaintingTableMenu.BUTTON_FINISH_TOGGLE);
            return true;
        }

        if (menu.isFinishMode()) {
            // Left arrow
            if (isHovered(mouseX, mouseY, leftPos + ARROW_LEFT_X, topPos + ARROW_Y,
                          ARROW_W, ARROW_H)) {
                if (menu.getFinishPage() > 0) { playClick(); sendButton(PaintingTableMenu.BUTTON_PAGE_LEFT); }
                return true;
            }
            // Right arrow
            int maxPage = (PaintingTableMenu.FINISH_DISPLAY_ORDER.length - 1)
                          / PaintingTableMenu.FINISHES_PER_PAGE;
            if (isHovered(mouseX, mouseY, leftPos + ARROW_RIGHT_X, topPos + ARROW_Y,
                          ARROW_W, ARROW_H)) {
                if (menu.getFinishPage() < maxPage) { playClick(); sendButton(PaintingTableMenu.BUTTON_PAGE_RIGHT); }
                return true;
            }
            // Finish slots
            if (menu.hasPaintableInput()) {
                for (int i = 0; i < PaintingTableMenu.FINISHES_PER_PAGE; i++) {
                    if (menu.getFinishForSlot(i) == null) continue;
                    if (isHovered(mouseX, mouseY, leftPos + SHADE_X[i], topPos + SHADE_Y, 16, 16)) {
                        playClick();
                        sendButton(PaintingTableMenu.BUTTON_FINISH_BASE + i);
                        return true;
                    }
                }
            }
        } else {
            // Color buttons
            PaintColor[] colors = PaintColor.values();
            for (int i = 0; i < colors.length && i < COLOR_POSITIONS.length; i++) {
                if (isHovered(mouseX, mouseY,
                              leftPos + COLOR_POSITIONS[i][0], topPos + COLOR_POSITIONS[i][1],
                              16, 16)) {
                    playClick();
                    sendButton(i);
                    return true;
                }
            }
            // Shade buttons
            if (menu.hasPaintableInput() && menu.getSelectedColorIndex() >= 0) {
                for (int i = 0; i < SHADE_X.length; i++) {
                    if (isHovered(mouseX, mouseY, leftPos + SHADE_X[i], topPos + SHADE_Y, 16, 16)) {
                        playClick();
                        sendButton(PaintColor.values().length + i);
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static boolean isHovered(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private void playClick() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0f));
    }
}
