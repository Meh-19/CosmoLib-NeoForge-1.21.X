package net.meh.cosmolib.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into {@link AbstractContainerScreen#slotClicked} (the declaring class)
 * for any smithing-screen-specific behaviour that cannot live in
 * {@link SmithingScreenMixin} due to Mixin's restriction on injecting into
 * methods not declared by the target class.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    /**
     * Plays the smithing-table use sound when the player clicks the token result
     * slot (slot 40) during a tool-skin removal — i.e., the injected skin-token
     * output slot added by {@link SmithingMenuMixin}.
     */
    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void cosmolib$playTokenSlotSound(Slot slot, int slotId, int mouseButton,
                                              ClickType clickType, CallbackInfo ci) {
        if (!((Object) this instanceof SmithingScreen self)) return;
        if (self.getMenu().slots.size() <= 40) return;
        Slot tokenSlot = self.getMenu().getSlot(40);
        if (slot == tokenSlot && tokenSlot.hasItem()) {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0f));
        }
    }
}
