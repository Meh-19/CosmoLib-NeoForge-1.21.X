package net.meh.cosmolib.screen;

import net.meh.cosmolib.cosmetic.CosmeticItem;
import net.meh.cosmolib.paint.PaintColor;
import net.meh.cosmolib.paint.PaintData;
import net.meh.cosmolib.registry.CosmoLibMenuTypes;
import net.meh.cosmolib.tag.CosmoLibTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class PaintingTableMenu extends AbstractContainerMenu {

    private static final int DATA_SELECTED_COLOR = 0;
    private static final int DATA_SELECTED_SHADE  = 1;
    private static final int DATA_COUNT           = 2;

    private final Player          player;
    private final Container       inventory;
    private final ContainerData   containerData;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    public PaintingTableMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(2), new SimpleContainerData(DATA_COUNT));
    }

    public PaintingTableMenu(int syncId, Inventory playerInventory,
                              Container inventory, ContainerData data) {
        super(CosmoLibMenuTypes.PAINTING_TABLE.get(), syncId);
        this.inventory     = inventory;
        this.player        = playerInventory.player;
        this.containerData = data;

        addDataSlots(containerData);
        containerData.set(DATA_SELECTED_COLOR, -1);
        containerData.set(DATA_SELECTED_SHADE,  -1);

        // Input slot — accepts paintable items
        addSlot(new Slot(inventory, 0, 26, 42) {
            @Override public boolean mayPlace(ItemStack s)  { return isPaintable(s); }
            @Override public void setChanged() { super.setChanged(); onInputChanged(); }
        });

        // Output slot — take only
        addSlot(new Slot(inventory, 1, 134, 42) {
            @Override public boolean mayPlace(ItemStack s)  { return false; }
            @Override public boolean mayPickup(Player p)    { return !getItem().isEmpty(); }
            @Override public void onTake(Player p, ItemStack s) { consumeInput(); super.onTake(p, s); }
        });

        // Player inventory
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 119 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 177));
    }

    // -----------------------------------------------------------------------
    // Accessors (used by the screen)
    // -----------------------------------------------------------------------

    public boolean hasPaintableInput()  { return isPaintable(inventory.getItem(0)); }
    public int  getSelectedColorIndex() { return containerData.get(DATA_SELECTED_COLOR); }
    public int  getSelectedShadeIndex() { return containerData.get(DATA_SELECTED_SHADE); }

    public PaintColor getSelectedColor() {
        int idx = containerData.get(DATA_SELECTED_COLOR);
        return (idx >= 0 && idx < PaintColor.values().length) ? PaintColor.values()[idx] : null;
    }

    public int getSelectedShadeColor() {
        PaintColor c = getSelectedColor();
        int shade    = containerData.get(DATA_SELECTED_SHADE);
        if (c == null || shade < 0 || shade >= c.getShades().length) return -1;
        return c.getShade(shade);
    }

    // -----------------------------------------------------------------------
    // Button clicks (called by the screen via clickMenuButton)
    // -----------------------------------------------------------------------

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < PaintColor.values().length) {
            selectColor(PaintColor.values()[id]);
            return true;
        }
        int shade = id - PaintColor.values().length;
        if (shade >= 0 && shade < 7) {
            selectShade(shade);
            return true;
        }
        return false;
    }

    public void selectColor(PaintColor color) {
        if (!hasPaintableInput()) return;
        containerData.set(DATA_SELECTED_COLOR, color.ordinal());
        containerData.set(DATA_SELECTED_SHADE, color.getDefaultIndex());
        updateResult();
    }

    public void selectShade(int shadeIndex) {
        if (!hasPaintableInput() || containerData.get(DATA_SELECTED_COLOR) < 0) return;
        PaintColor c = getSelectedColor();
        if (c == null || shadeIndex < 0 || shadeIndex >= c.getShades().length) return;
        containerData.set(DATA_SELECTED_SHADE, shadeIndex);
        updateResult();
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private void onInputChanged() {
        if (!hasPaintableInput()) {
            containerData.set(DATA_SELECTED_COLOR, -1);
            containerData.set(DATA_SELECTED_SHADE,  -1);
        }
        updateResult();
    }

    private void updateResult() {
        ItemStack input    = inventory.getItem(0);
        int       shade    = getSelectedShadeColor();
        if (input.isEmpty() || shade < 0) { inventory.setItem(1, ItemStack.EMPTY); return; }

        ItemStack result = input.copyWithCount(1);

        // Paintbrush: store color in NBT directly, not as PaintData
        if (result.getItem() instanceof net.meh.cosmolib.item.PaintbrushItem) {
            CustomData existing = result.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = existing.copyTag();
            tag.putInt(PaintData.KEY, shade);
            result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        } else {
            PaintData.applyColor(result, shade);
        }
        inventory.setItem(1, result);
    }

    private void consumeInput() {
        ItemStack input = inventory.getItem(0);
        if (!input.isEmpty()) input.shrink(1);
        if (input.isEmpty()) {
            containerData.set(DATA_SELECTED_COLOR, -1);
            containerData.set(DATA_SELECTED_SHADE,  -1);
        }
        updateResult();
    }

    private static boolean isPaintable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof net.meh.cosmolib.item.PaintbrushItem) return true;
        if (stack.getItem() instanceof CosmeticItem c && c.isPaintable()) return true;
        if (stack.getItem() instanceof BlockItem bi)
            return bi.getBlock().defaultBlockState().is(CosmoLibTags.Blocks.PAINTABLE);
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) { return true; }
}
