package net.meh.cosmolib.mixin;

import net.meh.cosmolib.toolskin.IToolSkinRemoval;
import net.meh.cosmolib.toolskin.ToolSkinData;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects a second output slot into the smithing table for returning
 * the skin token during a removal operation.
 *
 * <p>Slot layout after injection:
 * <ul>
 *   <li>0 – template input</li>
 *   <li>1 – base input</li>
 *   <li>2 – addition input</li>
 *   <li>3 – result (clean tool)</li>
 *   <li>4-39 – player inventory</li>
 *   <li>40 – token result (our new slot, positioned above the output)</li>
 * </ul>
 *
 * <p>Shift-click handling for both output slots lives in
 * {@link ItemCombinerMenuMixin} because {@code quickMoveStack} is defined on
 * {@code ItemCombinerMenu}, not {@code SmithingMenu}.
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin extends AbstractContainerMenu implements IToolSkinRemoval {

    @Unique private final SimpleContainer cosmolib$tokenContainer = new SimpleContainer(1);
    @Unique private ItemStack cosmolib$pendingToken = ItemStack.EMPTY;
    @Unique private int cosmolib$tokenSlotIndex = -1;

    protected SmithingMenuMixin() {
        super(null, 0);
    }

    // ------------------------------------------------------------------
    // IToolSkinRemoval
    // ------------------------------------------------------------------

    @Override
    public ItemStack cosmolib$takePendingToken() {
        ItemStack t = cosmolib$pendingToken;
        cosmolib$pendingToken = ItemStack.EMPTY;
        return t;
    }

    @Override
    public int cosmolib$getTokenSlotIndex() {
        return cosmolib$tokenSlotIndex;
    }

    // ------------------------------------------------------------------
    // Constructor – add the token display slot
    // ------------------------------------------------------------------

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("TAIL"))
    private void cosmolib$addTokenSlot(int containerId, Inventory playerInventory,
                                        ContainerLevelAccess access, CallbackInfo ci) {
        Slot resultSlot = this.getSlot(3);
        cosmolib$tokenSlotIndex = this.slots.size(); // will be 40
        this.addSlot(new Slot(cosmolib$tokenContainer, 0, resultSlot.x, resultSlot.y - 18) {

            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean isActive() {
                return !cosmolib$tokenContainer.getItem(0).isEmpty();
            }

            /**
             * Path A: player left-clicks the token slot.
             * Atomically completes the removal — gives the clean tool and
             * consumes the base so no dupe is possible.
             */
            @Override
            public void onTake(net.minecraft.world.entity.player.Player player, ItemStack stack) {
                cosmolib$pendingToken = ItemStack.EMPTY; // prevent double-give via event
                Slot outputSlot = SmithingMenuMixin.this.getSlot(3);
                ItemStack cleanTool = outputSlot.getItem().copy();
                if (!cleanTool.isEmpty()) {
                    outputSlot.set(ItemStack.EMPTY);
                    SmithingMenuMixin.this.getSlot(1).set(ItemStack.EMPTY); // consume base
                    if (!player.getInventory().add(cleanTool)) {
                        player.drop(cleanTool, false);
                    }
                }
                super.onTake(player, stack);
            }
        });
    }

    // ------------------------------------------------------------------
    // onTake – give the pending token when the clean tool is taken (Path B)
    // ------------------------------------------------------------------

    /**
     * {@code SmithingMenu.onTake} is the concrete implementation of the abstract
     * {@code ItemCombinerMenu.onTake}. It fires when the result slot is taken via
     * both regular left-click and shift-click, before inputs are consumed.
     * This is the reliable hook for Path B (player takes clean tool first).
     */
    @Inject(method = "onTake", at = @At("HEAD"))
    private void cosmolib$giveTokenOnResultTake(Player player, ItemStack stack, CallbackInfo ci) {
        ItemStack pending = cosmolib$pendingToken;
        if (pending.isEmpty()) return;
        cosmolib$pendingToken = ItemStack.EMPTY;
        if (!player.getInventory().add(pending)) {
            player.drop(pending, false);
        }
    }

    // ------------------------------------------------------------------
    // createResult – keep the token slot and pending state in sync
    // ------------------------------------------------------------------

    @Inject(method = "createResult", at = @At("TAIL"))
    private void cosmolib$updateTokenSlot(CallbackInfo ci) {
        ItemStack template = this.getSlot(0).getItem();
        ItemStack base     = this.getSlot(1).getItem();

        if (template.isEmpty() && ToolSkinData.get(base).isPresent()) {
            ItemStack token = ToolSkinData.get(base).get().createToken();
            cosmolib$tokenContainer.setItem(0, token);
            cosmolib$pendingToken = token.copy();
        } else {
            cosmolib$tokenContainer.setItem(0, ItemStack.EMPTY);
            cosmolib$pendingToken = ItemStack.EMPTY;
        }
    }
}
