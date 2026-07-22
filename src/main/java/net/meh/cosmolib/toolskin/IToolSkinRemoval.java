package net.meh.cosmolib.toolskin;

import net.minecraft.world.item.ItemStack;

/**
 * Duck interface implemented on {@code SmithingMenu} via mixin.
 * Lets {@link net.meh.cosmolib.mixin.ItemCombinerMenuMixin} retrieve the
 * pending skin token without needing a shared static map.
 */
public interface IToolSkinRemoval {

    /**
     * Returns the token that should be given to the player when the clean tool
     * is taken from the output slot, then clears the pending state so it cannot
     * be given a second time.
     */
    ItemStack cosmolib$takePendingToken();

    /**
     * Returns the slot index of the injected token display slot, or {@code -1}
     * if it has not been added yet.
     */
    int cosmolib$getTokenSlotIndex();
}
