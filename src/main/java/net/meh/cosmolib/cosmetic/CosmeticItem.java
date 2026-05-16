package net.meh.cosmolib.cosmetic;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class CosmeticItem extends Item {

    private static final ResourceLocation COSMOLIB_FONT =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "default");

    private final CosmeticSlot   slot;
    private final CosmeticRarity rarity;
    private final boolean        paintable;

    public CosmeticItem(CosmeticSlot slot, CosmeticRarity rarity, boolean paintable, Properties props) {
        super(props.stacksTo(1));
        this.slot      = slot;
        this.rarity    = rarity;
        this.paintable = paintable;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.empty()
                .append(Component.literal(rarity.getSymbol())
                        .withStyle(Style.EMPTY.withFont(COSMOLIB_FONT)))
                .append(Component.literal(" "))
                .append(Component.translatable(getDescriptionId(stack))
                        .withStyle(Style.EMPTY.withColor(rarity.getColor())));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        if (paintable) {
            tooltip.add(Component.literal("ꑞ")
                    .withStyle(Style.EMPTY.withFont(COSMOLIB_FONT)));
        }
    }

    public CosmeticSlot   getSlot()      { return slot;      }
    public CosmeticRarity getRarity()    { return rarity;    }
    public boolean        isPaintable()  { return paintable; }
}
