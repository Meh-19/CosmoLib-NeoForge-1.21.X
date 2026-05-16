package net.meh.cosmolib.cosmetic.screen;

import net.meh.cosmolib.cosmetic.CosmeticItem;
import net.meh.cosmolib.cosmetic.CosmeticSlot;
import net.meh.cosmolib.cosmetic.network.EquipCosmeticPayload;
import net.meh.cosmolib.registry.CosmoLibMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The cosmetic wardrobe screen: 3 virtual cosmetic slots + player inventory.
 *
 * Items placed in cosmetic slots trigger an equip packet to the server.
 * Cosmetics are not physically moved from the player's inventory — the slots
 * are a display/equip surface only (items are returned on close).
 */
public class CosmeticMenu extends AbstractContainerMenu {

    private final Container cosmeticContainer = new SimpleContainer(3);

    public CosmeticMenu(int containerId, Inventory playerInventory) {
        super(CosmoLibMenuTypes.COSMETIC_SCREEN.get(), containerId);

        // Cosmetic slots (virtual display slots)
        for (CosmeticSlot cs : CosmeticSlot.values()) {
            int index = cs.getIndex();
            addSlot(new CosmeticEquipSlot(cosmeticContainer, index, 80, 18 + index * 22, cs));
        }

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 161));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    // ------------------------------------------------------------------
    // Custom slot that fires equip packets on change
    // ------------------------------------------------------------------
    private static class CosmeticEquipSlot extends Slot {

        private final CosmeticSlot cosmeticSlot;

        CosmeticEquipSlot(Container container, int index, int x, int y, CosmeticSlot cosmeticSlot) {
            super(container, index, x, y);
            this.cosmeticSlot = cosmeticSlot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof CosmeticItem cosmetic
                    && cosmetic.getSlot() == cosmeticSlot;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            // Send equip packet to server whenever this slot changes
            PacketDistributor.sendToServer(new EquipCosmeticPayload(cosmeticSlot, getItem().copy()));
        }
    }
}
