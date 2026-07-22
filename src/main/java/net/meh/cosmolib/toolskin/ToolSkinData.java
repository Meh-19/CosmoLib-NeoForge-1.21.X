package net.meh.cosmolib.toolskin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.meh.cosmolib.cosmetic.CosmeticRarity;
import net.meh.cosmolib.registry.CosmoLibDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Data component stored on a tool {@link ItemStack} when a {@link ToolSkinItem}
 * has been applied at the smithing table.
 *
 * <p>Two registry-key fields:
 * <ul>
 *   <li>{@code skinItemKey} — key of the {@link ToolSkinItem} token; used on removal to
 *       return the token to the player's inventory.</li>
 *   <li>{@code originalItemKey} — key of the base tool item (e.g. {@code minecraft:wooden_pickaxe});
 *       used to render the original item icon in the skinned tool's tooltip.</li>
 * </ul>
 *
 * <p>When applied, {@link #apply} also sets {@link DataComponents#CUSTOM_NAME} on the
 * stack so the skinned tool displays the skin name in-game.  {@link #remove} clears it.
 *
 * <p>Registered as {@link CosmoLibDataComponents#TOOL_SKIN}.
 */
public record ToolSkinData(
        ResourceLocation skinTexture,
        String skinSetName,
        CosmeticRarity rarity,
        ToolSkinType skinType,
        @Nullable String exclusiveTag,
        int exclusiveTagColor,
        ResourceLocation skinItemKey,
        ResourceLocation originalItemKey,
        @Nullable ParticleOptions particle
) {

    // ------------------------------------------------------------------
    // Codecs
    // ------------------------------------------------------------------

    public static final Codec<ToolSkinData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("skin_texture")
                    .forGetter(ToolSkinData::skinTexture),
            Codec.STRING.fieldOf("skin_set_name")
                    .forGetter(ToolSkinData::skinSetName),
            Codec.STRING.xmap(CosmeticRarity::valueOf, CosmeticRarity::name)
                    .fieldOf("rarity")
                    .forGetter(ToolSkinData::rarity),
            Codec.STRING.xmap(ToolSkinType::valueOf, ToolSkinType::name)
                    .fieldOf("skin_type")
                    .forGetter(ToolSkinData::skinType),
            Codec.STRING.optionalFieldOf("exclusive_tag")
                    .xmap(opt -> opt.orElse(null), Optional::ofNullable)
                    .forGetter(ToolSkinData::exclusiveTag),
            Codec.INT.optionalFieldOf("exclusive_tag_color", ToolSkinItem.DEFAULT_EXCLUSIVE_COLOR)
                    .forGetter(ToolSkinData::exclusiveTagColor),
            ResourceLocation.CODEC.fieldOf("skin_item_key")
                    .forGetter(ToolSkinData::skinItemKey),
            ResourceLocation.CODEC.fieldOf("original_item_key")
                    .forGetter(ToolSkinData::originalItemKey),
            ParticleTypes.CODEC.optionalFieldOf("particle")
                    .xmap(opt -> opt.orElse(null), Optional::ofNullable)
                    .forGetter(ToolSkinData::particle)
    ).apply(inst, ToolSkinData::new));

    public static final StreamCodec<FriendlyByteBuf, ToolSkinData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                buf.writeResourceLocation(data.skinTexture());
                buf.writeUtf(data.skinSetName());
                buf.writeUtf(data.rarity().name());
                buf.writeUtf(data.skinType().name());
                buf.writeBoolean(data.exclusiveTag() != null);
                if (data.exclusiveTag() != null) buf.writeUtf(data.exclusiveTag());
                buf.writeInt(data.exclusiveTagColor());
                buf.writeResourceLocation(data.skinItemKey());
                buf.writeResourceLocation(data.originalItemKey());
                buf.writeBoolean(data.particle() != null);
                if (data.particle() != null) {
                    ByteBufCodecs.fromCodec(ParticleTypes.CODEC).encode(buf, data.particle());
                }
            },
            buf -> {
                ResourceLocation skinTexture     = buf.readResourceLocation();
                String           skinSetName     = buf.readUtf();
                CosmeticRarity   rarity          = CosmeticRarity.valueOf(buf.readUtf());
                ToolSkinType     skinType        = ToolSkinType.valueOf(buf.readUtf());
                String           exclusiveTag    = buf.readBoolean() ? buf.readUtf() : null;
                int              exclusiveColor  = buf.readInt();
                ResourceLocation skinItemKey     = buf.readResourceLocation();
                ResourceLocation originalItemKey = buf.readResourceLocation();
                ParticleOptions  particle        = buf.readBoolean()
                        ? ByteBufCodecs.fromCodec(ParticleTypes.CODEC).decode(buf) : null;
                return new ToolSkinData(skinTexture, skinSetName, rarity, skinType,
                        exclusiveTag, exclusiveColor, skinItemKey, originalItemKey, particle);
            }
    );

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Returns the {@link ToolSkinData} on {@code stack}, or empty if absent. */
    public static Optional<ToolSkinData> get(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        return Optional.ofNullable(stack.get(CosmoLibDataComponents.TOOL_SKIN.get()));
    }

    /**
     * Returns a copy of {@code toolStack} with the skin component applied.
     * Changes the tool's name to "Easter Hoe" format, but only if the player
     * hasn't set a custom name.
     * Sets CustomModelData to trigger model override.
     * Does not modify the original stack.
     */
    public static ItemStack apply(ItemStack toolStack, ToolSkinItem skinItem) {
        ItemStack copy = toolStack.copy();
        copy.set(CosmoLibDataComponents.TOOL_SKIN.get(), new ToolSkinData(
                skinItem.getSkinTexture(),
                skinItem.getSkinSetName(),
                skinItem.getItemRarity(),
                skinItem.getSkinType(),
                skinItem.getExclusiveTag(),
                skinItem.getExclusiveTagColor(),
                BuiltInRegistries.ITEM.getKey(skinItem),
                BuiltInRegistries.ITEM.getKey(toolStack.getItem()),
                skinItem.getParticle()
        ));

        // Only change name if player hasn't set a custom name
        if (!toolStack.has(DataComponents.CUSTOM_NAME)) {
            copy.set(DataComponents.ITEM_NAME,
                    Component.literal(skinItem.getSkinSetName() + " " + skinItem.getSkinType().getDisplayName()));
        }

        // Set CustomModelData to trigger model override
        // Use a hash of the skin set name to generate a unique ID
        int customModelData = skinItem.getSkinSetName().hashCode() & 0x7FFFFFFF; // Make positive
        copy.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(customModelData));

        return copy;
    }

    /**
     * Returns a copy of {@code skinnedStack} with the skin component removed.
     * Also removes the item name and CustomModelData that was set during application.
     * Does not modify the original stack.
     */
    public static ItemStack remove(ItemStack skinnedStack) {
        ItemStack copy = skinnedStack.copy();
        copy.remove(CosmoLibDataComponents.TOOL_SKIN.get());
        copy.remove(DataComponents.ITEM_NAME);
        copy.remove(DataComponents.CUSTOM_MODEL_DATA);
        return copy;
    }

    /**
     * Returns a one-count stack of the {@link ToolSkinItem} token, or
     * {@link ItemStack#EMPTY} if no longer registered.
     */
    public ItemStack createToken() {
        Item item = BuiltInRegistries.ITEM.getOptional(skinItemKey).orElse(Items.AIR);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /**
     * Returns a one-count stack of the original tool (e.g. wooden pickaxe), or
     * {@link ItemStack#EMPTY} if no longer registered.  Used by the tooltip renderer.
     */
    public ItemStack createOriginalStack() {
        Item item = BuiltInRegistries.ITEM.getOptional(originalItemKey).orElse(Items.AIR);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
