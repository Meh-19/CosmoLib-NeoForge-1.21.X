package net.meh.cosmolib.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws a slot background for the token result slot (slot index 40)
 * injected by {@link SmithingMenuMixin}, so it gets the same visual
 * border as every other slot in the smithing table.
 */
@Mixin(SmithingScreen.class)
public abstract class SmithingScreenMixin {

    private static final ResourceLocation SLOT_SPRITE =
            ResourceLocation.withDefaultNamespace("container/slot");

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void cosmolib$renderTokenSlot(GuiGraphics guiGraphics, float partialTick,
                                           int mouseX, int mouseY, CallbackInfo ci) {
        SmithingScreen self = (SmithingScreen) (Object) this;

        // Slot 40 is our injected token slot
        if (self.getMenu().slots.size() <= 40) return;

        Slot tokenSlot = self.getMenu().getSlot(40);
        if (!tokenSlot.isActive()) return;

        // Draw the slot background sprite at the slot's screen position
        int x = self.getGuiLeft() + tokenSlot.x - 1;
        int y = self.getGuiTop()  + tokenSlot.y - 1;
        guiGraphics.blitSprite(SLOT_SPRITE, x, y, 18, 18);
    }
}
