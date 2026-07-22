package net.meh.cosmolib.mixin;

import net.meh.cosmolib.toolskin.OriginalItemTooltipComponent;
import net.meh.cosmolib.toolskin.ToolSkinData;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Injects an {@link OriginalItemTooltipComponent} into the tooltip of any
 * {@link ItemStack} that has a {@link ToolSkinData} component.
 *
 * <p>MC inserts the result of {@code getTooltipImage()} at index&nbsp;1
 * (immediately below the item name), so the skin block always appears before
 * vanilla attribute/stat lines.
 *
 * <p>The component renders the original tool's actual item icon at 8×8 alongside
 * the skin set name and optional exclusive tag — no font glyphs required, so it
 * works for any item regardless of mod or tier.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {

    @Inject(method = "getTooltipImage", at = @At("HEAD"), cancellable = true)
    private void cosmolib$skinDataTooltip(
            CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        ItemStack self = (ItemStack) (Object) this;
        ToolSkinData.get(self).ifPresent(data ->
                cir.setReturnValue(Optional.of(new OriginalItemTooltipComponent(data)))
        );
    }
}
