package net.meh.cosmolib.cosmetic.screen;

import net.meh.cosmolib.cosmetic.CosmeticItem;
import net.meh.cosmolib.cosmetic.CosmeticSlot;
import net.meh.cosmolib.cosmetic.PlayerCosmeticsData;
import net.meh.cosmolib.cosmetic.network.EquipCosmeticPayload;
import net.meh.cosmolib.registry.CosmoLibAttachments;
import net.meh.cosmolib.registry.CosmoLibMenuTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Cosmetic wardrobe: 3 equip slots (HAT/BACK/HAND) + player inventory.
 *
 * On open, slots are pre-filled from the player's attachment so currently
 * equipped cosmetics are visible. Slot changes send equip packets to the server.
 * On close the virtual container is discarded; the attachment holds the truth.
 */
public class CosmeticMenu extends AbstractContainerMenu {

    private static final int SLOT_HAT  = 0;
    private static final int SLOT_BACK = 1;
    private static final int SLOT_HAND = 2;
    // slots 3-38 = player inventory

    private final SimpleContainer cosmeticContainer = new SimpleContainer(3);

    public CosmeticMenu(int containerId, Inventory playerInventory) {
        super(CosmoLibMenuTypes.COSMETIC_SCREEN.get(), containerId);

        // Cosmetic slots — positions match cosmetics.png texture
        addSlot(new CosmeticEquipSlot(cosmeticContainer, SLOT_HAT,  17, 10,  CosmeticSlot.HAT));
        addSlot(new CosmeticEquipSlot(cosmeticContainer, SLOT_BACK, 17, 52,  CosmeticSlot.BACK));
        addSlot(new CosmeticEquipSlot(cosmeticContainer, SLOT_HAND, 17, 94,  CosmeticSlot.HAND));

        // Player inventory
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 119 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 177));

        // Pre-fill cosmetic slots from the player's attachment (server-side only).
        // Use setItem() directly so slot.setChanged() (and the equip packet) is not triggered.
        if (playerInventory.player instanceof ServerPlayer sp) {
            PlayerCosmeticsData data = sp.getData(CosmoLibAttachments.COSMETICS);
            cosmeticContainer.setItem(SLOT_HAT,  data.getEquipped(CosmeticSlot.HAT).copy());
            cosmeticContainer.setItem(SLOT_BACK, data.getEquipped(CosmeticSlot.BACK).copy());
            cosmeticContainer.setItem(SLOT_HAND, data.getEquipped(CosmeticSlot.HAND).copy());
        }
    }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack    = slot.getItem();
        ItemStack original = stack.copy();

        if (index < 3) {
            // Cosmetic slot → player inventory
            if (!moveItemStackTo(stack, 3, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof CosmeticItem cosmetic) {
            // Player inventory → matching cosmetic slot (if empty)
            int target = cosmetic.getSlot().getIndex();
            if (!slots.get(target).hasItem() && !moveItemStackTo(stack, target, target + 1, false))
                return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    // ------------------------------------------------------------------
    // Custom slot: validates type, fires equip packet on change
    // ------------------------------------------------------------------
    private static class CosmeticEquipSlot extends Slot {

        private final CosmeticSlot cosmeticSlot;

        CosmeticEquipSlot(Container container, int index, int x, int y, CosmeticSlot cs) {
            super(container, index, x, y);
            this.cosmeticSlot = cs;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof CosmeticItem cosmetic
                    && cosmetic.getSlot() == cosmeticSlot;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            PacketDistributor.sendToServer(new EquipCosmeticPayload(cosmeticSlot, getItem().copy()));
        }
    }
}
