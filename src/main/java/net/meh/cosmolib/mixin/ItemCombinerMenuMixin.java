package net.meh.cosmolib.mixin;

import net.meh.cosmolib.toolskin.IToolSkinRemoval;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Handles shift-clicking the token result slot (slot 40) injected by
 * {@link SmithingMenuMixin}.
 *
 * <p>{@code quickMoveStack} is defined on {@link ItemCombinerMenu}, not on
 * {@link net.minecraft.world.inventory.SmithingMenu}, so it must be targeted here.
 *
 * <p>Shift-clicking the <em>clean tool</em> result slot is handled by
 * {@link net.meh.cosmolib.event.GameEventHandler#onItemCrafted} via the
 * {@link IToolSkinRemoval} interface — {@code ItemCraftedEvent} fires when
 * the result slot's {@code onTake} is called during {@code quickMoveStack}.
 *
 * <p>The {@code instanceof IToolSkinRemoval} guard ensures non-smithing menus
 * (e.g. {@link net.minecraft.world.inventory.AnvilMenu}) are unaffected.
 */
@Mixin(ItemCombinerMenu.class)
public abstract class ItemCombinerMenuMixin {

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void cosmolib$shiftClickTokenSlot(Player player, int index,
                                               CallbackInfoReturnable<ItemStack> cir) {
        if (!(this instanceof IToolSkinRemoval removal)) return;

        int tokenIdx = removal.cosmolib$getTokenSlotIndex();
        if (tokenIdx < 0 || index != tokenIdx) return;

        // Slot 40 is not handled by vanilla's quickMoveStack — take over entirely.
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        Slot tokenSlot = menu.getSlot(tokenIdx);

        if (!tokenSlot.hasItem()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        ItemStack token = tokenSlot.getItem().copy();
        tokenSlot.onTake(player, token); // gives clean tool + consumes base (Path A logic)
        tokenSlot.set(ItemStack.EMPTY);  // ensure slot is cleared

        if (!player.getInventory().add(token)) {
            player.drop(token, false);
        }

        cir.setReturnValue(token);
    }
}
