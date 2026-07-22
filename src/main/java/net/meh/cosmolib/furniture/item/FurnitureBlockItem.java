package net.meh.cosmolib.furniture.item;

import net.meh.cosmolib.cosmetic.CosmeticDefault;
import net.meh.cosmolib.furniture.block.AbstractFurnitureBlock;
import net.meh.cosmolib.paint.PaintData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;

/**
 * BlockItem for furniture that bakes a default paint colour or finish into
 * every stack — the same mechanism used by CosmeticItem.
 *
 * <p>Usage:
 * <pre>{@code
 * ITEMS.register("my_chair", () -> new FurnitureBlockItem(
 *         MY_CHAIR.get(), new Item.Properties()));
 * }</pre>
 *
 * The default appearance is taken from the block's {@link AbstractFurnitureBlock#getDefaultAppearance()},
 * which is configured via {@link net.meh.cosmolib.furniture.FurnitureOptions#defaultColor} or
 * {@link net.meh.cosmolib.furniture.FurnitureOptions#defaultFinish}.
 */
public class FurnitureBlockItem extends BlockItem {

    public FurnitureBlockItem(AbstractFurnitureBlock block, Item.Properties props) {
        super(block, bakeDefault(block, props));
    }

    private static Properties bakeDefault(AbstractFurnitureBlock block, Properties props) {
        CosmeticDefault def = block.getDefaultAppearance();
        if (def == null) return props;
        CompoundTag tag = new CompoundTag();
        tag.putInt(PaintData.KEY, def.getRawRgb());
        return props.component(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
