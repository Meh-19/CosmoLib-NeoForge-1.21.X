package net.meh.cosmolib.cosmetic.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class CosmeticScreen extends AbstractContainerScreen<CosmeticMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "textures/gui/cosmetic_screen.png");

    // Mannequin preview region relative to GUI top-left: (50,10) → (125,109)
    private static final int MANNEQUIN_X1 = 50;
    private static final int MANNEQUIN_Y1 = 10;
    private static final int MANNEQUIN_X2 = 125;
    private static final int MANNEQUIN_Y2 = 109;
    private static final float MANNEQUIN_SCALE = 40.0f;

    // One full rotation per 8 seconds
    private static final float ROTATION_SPEED = (float) (Math.PI * 2 / 8.0);

    private final long openTime = System.currentTimeMillis();

    public CosmeticScreen(CosmeticMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth  = 176;
        imageHeight = 200;
        // Push vanilla labels off-screen so they don't render over the texture
        titleLabelX        = -999;
        titleLabelY        = -999;
        inventoryLabelX    = -999;
        inventoryLabelY    = -999;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        drawMannequin(graphics, mouseX, mouseY);
    }

    private void drawMannequin(GuiGraphics graphics, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int centreX = leftPos + (MANNEQUIN_X1 + MANNEQUIN_X2) / 2;
        int entityY = topPos  + MANNEQUIN_Y2 - 10;

        float elapsed   = (System.currentTimeMillis() - openTime) / 1000f;
        float yRotation = elapsed * ROTATION_SPEED;

        Quaternionf bodyRot = new Quaternionf()
                .rotationZ((float) Math.PI)
                .rotateY(-yRotation);

        float deltaX = mouseX - centreX;
        float deltaY = mouseY - (topPos + MANNEQUIN_Y1 + 20);
        Quaternionf headRot = new Quaternionf()
                .rotationX((float) Math.atan(deltaY / 40f) * 0.5f)
                .rotateY(  (float) Math.atan(deltaX / 40f) * -0.5f);

        InventoryScreen.renderEntityInInventory(graphics, centreX, entityY, MANNEQUIN_SCALE,
                new Vector3f(0f, 0f, 0f), bodyRot, headRot, mc.player);

        // drawEntity leaves depth testing on, which breaks 2D slot hover rendering
        RenderSystem.disableDepthTest();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // intentionally empty — labels hidden via -999 offsets
    }
}
