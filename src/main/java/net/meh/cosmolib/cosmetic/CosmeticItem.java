package net.meh.cosmolib.cosmetic;

import net.meh.cosmolib.paint.PaintData;
import net.meh.cosmolib.paint.PaintFinish;
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
     * ResourceLocation of the 3-D cosmetic model rendered on mannequins / players
     * (e.g. {@code cosmolib:cosmetics/cosmo_cane}).
     *
     * <p>Null means "no 3-D model" — the token is shown in all contexts.
     */
    @Nullable
    private final ResourceLocation cosmeticModelId;

    // ------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------

    /** No default paint, no explicit 3-D model ID. */
    public CosmeticItem(CosmeticSlot slot, CosmeticRarity rarity, boolean paintable,
                        Properties props) {
        this(slot, rarity, paintable, null, null, props);
    }

    /** Default paint, no explicit 3-D model ID. */
    public CosmeticItem(CosmeticSlot slot, CosmeticRarity rarity, boolean paintable,
                        @Nullable CosmeticDefault defaultAppearance, Properties props) {
        this(slot, rarity, paintable, defaultAppearance, null, props);
    }

    /** No default paint, explicit 3-D model ID. */
    public CosmeticItem(CosmeticSlot slot, CosmeticRarity rarity, boolean paintable,
                        @Nullable ResourceLocation cosmeticModelId, Properties props) {
        this(slot, rarity, paintable, null, cosmeticModelId, props);
    }

    /**
     * Full constructor — default paint + explicit 3-D model ID.
     *
     * @param defaultAppearance paint baked into every stack (may be {@code null})
     * @param cosmeticModelId   model shown when worn on mannequin/player
     *                          (e.g. {@code cosmolib:cosmetics/cosmo_cane}); may be {@code null}
     */
    public CosmeticItem(CosmeticSlot slot, CosmeticRarity rarity, boolean paintable,
                        @Nullable CosmeticDefault defaultAppearance,
                        @Nullable ResourceLocation cosmeticModelId,
                        Properties props) {
        super(bakeDefault(defaultAppearance, props.stacksTo(1)));
        this.slot              = slot;
        this.rarity            = rarity;
        this.paintable         = paintable;
        this.defaultAppearance = defaultAppearance;
        this.cosmeticModelId   = cosmeticModelId;
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
        if (!paintable) return;

        tooltip.add(Component.literal("ꑞ")
                .withStyle(Style.EMPTY.withFont(COSMOLIB_FONT)));

        // Paint description line — skipped when:
        //  • no stored paint value
        //  • matches this item's baked-in default appearance
        //  • plain white (0xFFFFFF) — visually indistinguishable from unpainted
        int rgb = PaintData.getColor(stack);
        if (rgb < 0) return;
        if ((rgb & 0xFFFFFF) == 0xFFFFFF) return;
        if (defaultAppearance != null && rgb == defaultAppearance.getRawRgb()) return;

        Component line = PaintFinish.buildTooltipLine(rgb);
        if (line != null) tooltip.add(line);
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public CosmeticSlot      getSlot()               { return slot;              }
    public CosmeticRarity    getRarity()              { return rarity;            }
    public boolean           isPaintable()            { return paintable;         }
    @Nullable
    public CosmeticDefault   getDefaultAppearance()   { return defaultAppearance; }

    /**
     * The ResourceLocation of the 3-D cosmetic model (e.g. {@code cosmolib:cosmetics/cosmo_cane}).
     * Used by the mannequin renderer and the token BEWLR when not in a display context.
     * May be {@code null} if no 3-D model was registered.
     */
    @Nullable
    public ResourceLocation getCosmeticModelId()  { return cosmeticModelId; }

    /**
     * The ResourceLocation of this item's flat token model, derived from its
     * slot and rarity (e.g. {@code cosmolib:item/token/limited_hand_token}).
     */
    public ResourceLocation getTokenModelId() {
        return ResourceLocation.fromNamespaceAndPath("cosmolib",
                "item/token/" + rarity.name().toLowerCase()
                + "_" + slot.name().toLowerCase() + "_token");
    }
}
