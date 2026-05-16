package net.meh.cosmolib.paint;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class PaintData {

    public static final String KEY = "PaintColor";
    private static final int NO_COLOR = -1;

    private PaintData() {}

    public static int getColor(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return NO_COLOR;
        CompoundTag tag = data.copyTag();
        return tag.contains(KEY) ? tag.getInt(KEY) : NO_COLOR;
    }

    public static void applyColor(ItemStack stack, int color) {
        CustomData data  = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag  = data.copyTag();
        tag.putInt(KEY, color & 0xFFFFFF);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void clearColor(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return;
        CompoundTag tag = data.copyTag();
        tag.remove(KEY);
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static boolean hasColor(ItemStack stack) {
        return getColor(stack) >= 0;
    }
}
