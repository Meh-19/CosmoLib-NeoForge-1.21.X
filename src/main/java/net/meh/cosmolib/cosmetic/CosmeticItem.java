package net.meh.cosmolib.cosmetic;

import net.meh.cosmolib.paint.PaintData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CosmeticItem extends Item {

    private static final ResourceLocation COSMOLIB_FONT =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "default");

    /**
     * Per-slot pool populated automatically when any {@code CosmeticItem} is
     * constructed (during item registration).  Use {@link #getBySlot} to
     * retrieve a snapshot at spawn time — no tag JSON required.
     */
    private static final Map<CosmeticSlot, List<CosmeticItem>> SLOT_POOL =
            new EnumMap<>(CosmeticSlot.class);

    private final CosmeticSlot    slot;
    private final CosmeticRarity  rarity;
    private final boolean         paintable;
    @Nullable
    private final CosmeticDefault defaultAppearance;

    /**
     * Creates a cosmetic item with no default appearance (renders white until painted).
     */
    public CosmeticItem(CosmeticSlot slot, CosmeticRarity rarity, boolean paintable, Properties props) {
        this(slot, rarity, paintable, null, props);
    }

    /**
     * Creates a cosmetic item with a default paint appearance baked directly into
     * the item's data components, so it is present on <em>every</em> stack of this
     * item regardless of how it was created — creative tab, {@code /give}, loot
     * tables, {@code new ItemStack(item)}, etc.
     *
     * <p>Build the appearance with one of {@link CosmeticDefault}'s factories:
     * <pre>{@code
     * // Solid colour — shade 1 (lightest) to 7 (darkest)
     * CosmeticDefault.color(PaintColor.BLUE, 3)
     *
     * // Animated finish
     * CosmeticDefault.finish(FinishType.RAINBOW)
     * }</pre>
     *
     * Pass {@code null} for no default (same as the 4-argument constructor).
     */
    public CosmeticItem(CosmeticSlot slot, CosmeticRarity rarity, boolean paintable,
                        @Nullable CosmeticDefault defaultAppearance, Properties props) {
        super(bakeDefault(defaultAppearance, props.stacksTo(1)));
        this.slot              = slot;
        this.rarity            = rarity;
        this.paintable         = paintable;
        this.defaultAppearance = defaultAppearance;
        // Self-register so the mob spawn handler can find all hat cosmetics
        // without needing any tag JSON file.
        SLOT_POOL.computeIfAbsent(slot, s -> new ArrayList<>()).add(this);
    }

    /**
     * Writes the default paint value into {@code props} as a
     * {@link DataComponents#CUSTOM_DATA} component so it is present on every
     * stack of this item, not just those created via {@code getDefaultInstance()}.
     */
    private static Properties bakeDefault(@Nullable CosmeticDefault def, Properties props) {
        if (def == null) return props;
        CompoundTag tag = new CompoundTag();
        tag.putInt(PaintData.KEY, def.getRawRgb());
        return props.component(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Returns all registered {@link CosmeticItem}s for the given slot.
     * The list is unmodifiable and is fully populated once item registration
     * completes (before any gameplay).
     */
    public static List<CosmeticItem> getBySlot(CosmeticSlot slot) {
        List<CosmeticItem> list = SLOT_POOL.get(slot);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
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

    public CosmeticSlot      getSlot()               { return slot;              }
    public CosmeticRarity    getRarity()              { return rarity;            }
    public boolean           isPaintable()            { return paintable;         }
    @Nullable
    public CosmeticDefault   getDefaultAppearance()   { return defaultAppearance; }
}
