package net.meh.cosmolib.screen;

import net.meh.cosmolib.cosmetic.CosmeticItem;
import net.meh.cosmolib.furniture.block.AbstractFurnitureBlock;
import net.meh.cosmolib.paint.FinishType;
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

    // -----------------------------------------------------------------------
    // ContainerData indices
    // -----------------------------------------------------------------------
    private static final int DATA_SELECTED_COLOR  = 0;
    private static final int DATA_SELECTED_SHADE  = 1;
    private static final int DATA_FINISH_MODE     = 2;  // 0=color/shade, 1=finish
    private static final int DATA_SELECTED_FINISH = 3;  // global index into FINISH_DISPLAY_ORDER, -1=none
    private static final int DATA_FINISH_PAGE     = 4;  // 0 or 1
    private static final int DATA_COUNT           = 5;

    // -----------------------------------------------------------------------
    // Button IDs  (sent via handleInventoryButtonClick)
    // -----------------------------------------------------------------------
    /** Color buttons: IDs 0 .. PaintColor.values().length - 1 */
    /** Shade buttons: IDs PaintColor.values().length .. PaintColor.values().length + 6 */
    private static final int SHADE_BASE = PaintColor.values().length;

    /** Toggle finish / color mode. */
    public static final int BUTTON_FINISH_TOGGLE = SHADE_BASE + 7;       // 16
    /** Scroll to previous page of finishes. */
    public static final int BUTTON_PAGE_LEFT     = BUTTON_FINISH_TOGGLE + 1; // 17
    /** Scroll to next page of finishes. */
    public static final int BUTTON_PAGE_RIGHT    = BUTTON_FINISH_TOGGLE + 2; // 18
    /** First finish slot on the current page; slots go up to +6. */
    public static final int BUTTON_FINISH_BASE   = BUTTON_FINISH_TOGGLE + 3; // 19

    // -----------------------------------------------------------------------
    // Finish display order  (11 shown; 3 enum entries hidden from UI for now)
    // -----------------------------------------------------------------------
    /** Finishes shown in the UI, in page order. 7 per page. */
    public static final FinishType[] FINISH_DISPLAY_ORDER = {
        // Page 0
        FinishType.RAINBOW, FinishType.GOLD,   FinishType.MOLTEN,
        FinishType.BUBBLE,  FinishType.FLORAL, FinishType.GALAXY, FinishType.MATRIX,
        // Page 1  (slots 7-10 filled; 11-13 are empty)
        FinishType.CHROME, FinishType.GLITCH, FinishType.IRIDESCENT, FinishType.VOID
    };
    public static final int FINISHES_PER_PAGE = 7;

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------
    private final Player        player;
    private final Container     inventory;
    private final ContainerData containerData;

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
        containerData.set(DATA_SELECTED_COLOR,  -1);
        containerData.set(DATA_SELECTED_SHADE,  -1);
        containerData.set(DATA_FINISH_MODE,      0);
        containerData.set(DATA_SELECTED_FINISH, -1);
        containerData.set(DATA_FINISH_PAGE,      0);

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

    public boolean hasPaintableInput()   { return isPaintable(inventory.getItem(0)); }
    public int  getSelectedColorIndex()  { return containerData.get(DATA_SELECTED_COLOR); }
    public int  getSelectedShadeIndex()  { return containerData.get(DATA_SELECTED_SHADE); }
    public boolean isFinishMode()        { return containerData.get(DATA_FINISH_MODE) == 1; }
    public int  getSelectedFinishIndex() { return containerData.get(DATA_SELECTED_FINISH); }
    public int  getFinishPage()          { return containerData.get(DATA_FINISH_PAGE); }

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

    /**
     * Returns the finish that belongs in a given slot on the current page,
     * or {@code null} if the slot is past the end of the display list.
     */
    public FinishType getFinishForSlot(int slotIndex) {
        int globalIndex = containerData.get(DATA_FINISH_PAGE) * FINISHES_PER_PAGE + slotIndex;
        if (globalIndex < 0 || globalIndex >= FINISH_DISPLAY_ORDER.length) return null;
        return FINISH_DISPLAY_ORDER[globalIndex];
    }

    /**
     * Which local slot (0-6) on the current page is selected, or -1 if none
     * (either no finish selected, or selected finish is on a different page).
     */
    public int getSelectedFinishLocalSlot() {
        int fi = containerData.get(DATA_SELECTED_FINISH);
        if (fi < 0) return -1;
        int local = fi - containerData.get(DATA_FINISH_PAGE) * FINISHES_PER_PAGE;
        return (local >= 0 && local < FINISHES_PER_PAGE) ? local : -1;
    }

    // -----------------------------------------------------------------------
    // Button clicks (called by the screen via clickMenuButton)
    // -----------------------------------------------------------------------

    @Override
    public boolean clickMenuButton(Player player, int id) {
        // Color buttons
        if (id >= 0 && id < PaintColor.values().length) {
            selectColor(PaintColor.values()[id]);
            return true;
        }
        // Shade buttons
        int shade = id - SHADE_BASE;
        if (shade >= 0 && shade < 7) {
            selectShade(shade);
            return true;
        }
        // Finish / page controls
        if (id == BUTTON_FINISH_TOGGLE) { toggleFinishMode();  return true; }
        if (id == BUTTON_PAGE_LEFT)     { pageLeft();          return true; }
        if (id == BUTTON_PAGE_RIGHT)    { pageRight();         return true; }
        int finishSlot = id - BUTTON_FINISH_BASE;
        if (finishSlot >= 0 && finishSlot < FINISHES_PER_PAGE) {
            selectFinishSlot(finishSlot);
            return true;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Color / shade selection
    // -----------------------------------------------------------------------

    public void selectColor(PaintColor color) {
        if (!hasPaintableInput()) return;
        // Switching to color mode clears any finish selection
        containerData.set(DATA_FINISH_MODE,      0);
        containerData.set(DATA_SELECTED_FINISH, -1);
        containerData.set(DATA_SELECTED_COLOR, color.ordinal());
        containerData.set(DATA_SELECTED_SHADE, color.getDefaultIndex());
        updateResult();
    }

    public void selectShade(int shadeIndex) {
        if (!hasPaintableInput() || containerData.get(DATA_SELECTED_COLOR) < 0) return;
        PaintColor c = getSelectedColor();
        if (c == null || shadeIndex < 0 || shadeIndex >= c.getShades().length) return;
        containerData.set(DATA_FINISH_MODE,      0);
        containerData.set(DATA_SELECTED_FINISH, -1);
        containerData.set(DATA_SELECTED_SHADE, shadeIndex);
        updateResult();
    }

    // -----------------------------------------------------------------------
    // Finish selection
    // -----------------------------------------------------------------------

    private void toggleFinishMode() {
        if (!hasPaintableInput()) return;
        boolean enteringFinish = containerData.get(DATA_FINISH_MODE) == 0;
        if (enteringFinish) {
            containerData.set(DATA_FINISH_MODE,     1);
            containerData.set(DATA_SELECTED_COLOR, -1);
            containerData.set(DATA_SELECTED_SHADE, -1);
        } else {
            containerData.set(DATA_FINISH_MODE,      0);
            containerData.set(DATA_SELECTED_FINISH, -1);
        }
        updateResult();
    }

    private void pageLeft() {
        if (containerData.get(DATA_FINISH_PAGE) > 0)
            containerData.set(DATA_FINISH_PAGE, 0);
    }

    private void pageRight() {
        int maxPage = (FINISH_DISPLAY_ORDER.length - 1) / FINISHES_PER_PAGE;
        if (containerData.get(DATA_FINISH_PAGE) < maxPage)
            containerData.set(DATA_FINISH_PAGE, containerData.get(DATA_FINISH_PAGE) + 1);
    }

    private void selectFinishSlot(int slotIndex) {
        if (!hasPaintableInput()) return;
        int globalIndex = containerData.get(DATA_FINISH_PAGE) * FINISHES_PER_PAGE + slotIndex;
        if (globalIndex >= FINISH_DISPLAY_ORDER.length) return; // empty slot
        containerData.set(DATA_FINISH_MODE,     1);
        containerData.set(DATA_SELECTED_FINISH, globalIndex);
        containerData.set(DATA_SELECTED_COLOR, -1);
        containerData.set(DATA_SELECTED_SHADE, -1);
        updateResult();
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private void onInputChanged() {
        if (!hasPaintableInput()) {
            containerData.set(DATA_SELECTED_COLOR,  -1);
            containerData.set(DATA_SELECTED_SHADE,  -1);
            containerData.set(DATA_FINISH_MODE,      0);
            containerData.set(DATA_SELECTED_FINISH, -1);
            containerData.set(DATA_FINISH_PAGE,      0);
        }
        updateResult();
    }

    private void updateResult() {
        ItemStack input = inventory.getItem(0);
        if (input.isEmpty()) { inventory.setItem(1, ItemStack.EMPTY); return; }

        ItemStack result = input.copyWithCount(1);
        int colorToApply;

        if (containerData.get(DATA_FINISH_MODE) == 1) {
            // Finish mode — use the finish's magic RGB as the stored color value
            int fi = containerData.get(DATA_SELECTED_FINISH);
            if (fi < 0 || fi >= FINISH_DISPLAY_ORDER.length) {
                inventory.setItem(1, ItemStack.EMPTY);
                return;
            }
            colorToApply = FINISH_DISPLAY_ORDER[fi].getMagicRgb();
        } else {
            // Color / shade mode
            colorToApply = getSelectedShadeColor();
            if (colorToApply < 0) { inventory.setItem(1, ItemStack.EMPTY); return; }
        }

        // Apply color — paintbrush stores it in NBT, everything else via PaintData
        if (result.getItem() instanceof net.meh.cosmolib.item.PaintbrushItem) {
            CustomData existing = result.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = existing.copyTag();
            tag.putInt(PaintData.KEY, colorToApply);
            result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        } else {
            PaintData.applyColor(result, colorToApply);
        }
        inventory.setItem(1, result);
    }

    private void consumeInput() {
        ItemStack input = inventory.getItem(0);
        if (!input.isEmpty()) input.shrink(1);
        onInputChanged();
    }

    private static boolean isPaintable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof net.meh.cosmolib.item.PaintbrushItem) return true;
        if (stack.getItem() instanceof CosmeticItem c && c.isPaintable()) return true;
        if (stack.getItem() instanceof BlockItem bi) {
            var block = bi.getBlock();
            return (block instanceof AbstractFurnitureBlock afb && afb.isPaintable())
                    || block.defaultBlockState().is(CosmoLibTags.Blocks.PAINTABLE);
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Shift-click
    // -----------------------------------------------------------------------

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack    = slot.getItem();
        ItemStack original = stack.copy();

        if (index == 1) {
            // Output → player inventory
            if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else if (index == 0) {
            // Input → player inventory
            if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // Player inventory → input slot (only if paintable)
            if (!isPaintable(stack)) return ItemStack.EMPTY;
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) { return true; }
}
