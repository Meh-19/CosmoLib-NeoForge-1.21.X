package net.meh.cosmolib.toolskin;

import net.meh.cosmolib.cosmetic.CosmeticRarity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The "token" item a player holds and applies at the smithing table to skin a tool.
 *
 * <p>One {@code ToolSkinItem} instance represents one specific skin for one tool category.
 * Use the {@link Builder} to construct instances:
 * <pre>{@code
 * public static final DeferredItem<ToolSkinItem> PUNK_AXE_SKIN =
 *     ITEMS.register("punk_axe_skin", () -> ToolSkinItem.builder()
 *         .rarity(CosmeticRarity.EPIC)
 *         .type(ToolSkinType.AXE)
 *         .texture(ResourceLocation.fromNamespaceAndPath("mymod", "item/punk_axe"))
 *         .skinSetName("Punk")
 *         .exclusiveTag("2021 Event Exclusive")           // optional
 *         .addParticle(ParticleTypes.FLAME)               // optional
 *         .build());
 * }</pre>
 *
 * <p><b>Required Assets (3 files):</b>
 * <ol>
 *   <li><b>Token texture</b>: {@code assets/mymod/textures/item/punk_axe_token.png} (16×16)</li>
 *   <li><b>Applied texture</b>: {@code assets/mymod/textures/item/punk_axe.png} (16×16)</li>
 *   <li><b>Applied model</b>: {@code assets/mymod/models/item/punk_axe_applied.json}</li>
 * </ol>
 *
 * <p><b>Example Applied Model</b> (for axes/swords/etc):
 * <pre>{@code
 * {
 *   "parent": "minecraft:item/handheld",
 *   "textures": {
 *     "layer0": "mymod:item/punk_axe"
 *   }
 * }
 * }</pre>
 *
 * <p><b>Registration</b>: call {@link ToolSkinRegistry#register} during your mod
 * constructor or {@code FMLCommonSetupEvent}.
 *
 * <p><b>How it works:</b> Players combine the token + tool at a smithing table.
 * The system automatically swaps the tool's model using CustomModelData. No manual
 * override JSON files needed in {@code minecraft:models/item/} — the system injects
 * overrides at runtime!
 */
public class ToolSkinItem extends Item {

    /** Default colour for the exclusive tag line when no explicit colour is supplied. */
    public static final int DEFAULT_EXCLUSIVE_COLOR = 0xFCFC54;

    private static final ResourceLocation COSMOLIB_FONT =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "default");

    private final CosmeticRarity  rarity;
    private final ToolSkinType    skinType;
    private final ResourceLocation skinTexture;
    private final String          skinSetName;
    @Nullable private final String modelFolder;
    @Nullable private final String exclusiveTag;
    private final int             exclusiveTagColor;
    @Nullable private final ParticleOptions particle;

    private ToolSkinItem(Builder builder) {
        super(new Properties().stacksTo(1));
        this.rarity            = builder.rarity;
        this.skinType          = builder.skinType;
        this.skinTexture       = builder.skinTexture;
        this.skinSetName       = builder.skinSetName;
        this.modelFolder       = builder.modelFolder;
        this.exclusiveTag      = builder.exclusiveTag;
        this.exclusiveTagColor = builder.exclusiveTagColor;
        this.particle          = builder.particle;
    }

    // ------------------------------------------------------------------
    // Display overrides
    // ------------------------------------------------------------------

    /**
     * Returns the token item name:
     * {@code <rarity symbol> <skinSetName> <tool type> Skin}
     * with the symbol via {@code cosmolib:default} font and the text in rarity colour.
     */
    @Override
    public Component getName(ItemStack stack) {
        return Component.empty()
                .append(Component.literal(rarity.getSymbol())
                        .withStyle(Style.EMPTY.withFont(COSMOLIB_FONT)))
                .append(Component.literal(" "))
                .append(Component.literal(skinSetName + " " + skinType.getDisplayName() + " Token")
                        .withStyle(Style.EMPTY.withColor(rarity.getColor())));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        // Tokens show no tooltip - only applied tools do
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public CosmeticRarity  getItemRarity()      { return rarity; }
    public ToolSkinType    getSkinType()         { return skinType; }
    public ResourceLocation getSkinTexture()     { return skinTexture; }
    public String          getSkinSetName()      { return skinSetName; }
    @Nullable public String getModelFolder()     { return modelFolder; }
    @Nullable public String getExclusiveTag()    { return exclusiveTag; }
    public int             getExclusiveTagColor(){ return exclusiveTagColor; }
    @Nullable public ParticleOptions getParticle(){ return particle; }

    // ------------------------------------------------------------------
    // Builder
    // ------------------------------------------------------------------

    public static Builder builder() { return new Builder(); }

    /**
     * Fluent builder for {@link ToolSkinItem}.
     * {@code rarity}, {@code type}, {@code texture}, and {@code skinSetName} are required.
     * {@code exclusiveTag} and its colour are optional.
     */
    public static final class Builder {

        @Nullable private CosmeticRarity   rarity;
        @Nullable private ToolSkinType     skinType;
        @Nullable private ResourceLocation skinTexture;
        @Nullable private String           skinSetName;
        @Nullable private String           modelFolder;
        @Nullable private String           exclusiveTag;
        private int                        exclusiveTagColor = DEFAULT_EXCLUSIVE_COLOR;
        @Nullable private ParticleOptions  particle;

        private Builder() {}

        public Builder rarity(CosmeticRarity rarity) {
            this.rarity = rarity;
            return this;
        }

        public Builder type(ToolSkinType type) {
            this.skinType = type;
            return this;
        }

        public Builder texture(ResourceLocation texture) {
            this.skinTexture = texture;
            return this;
        }

        public Builder skinSetName(String name) {
            this.skinSetName = name;
            return this;
        }

        /**
         * Optional folder prefix prepended to the model path.
         * For example, {@code modelFolder("item/tool_skins")} will look for models at
         * {@code item/tool_skins/{skinset}_{type}_applied} instead of {@code item/{skinset}_{type}_applied}.
         *
         * @param folder path prefix, e.g. {@code "item/tool_skins"}
         */
        public Builder modelFolder(String folder) {
            this.modelFolder = folder;
            return this;
        }

        /**
         * Sets an exclusive tag shown in gold ({@value DEFAULT_EXCLUSIVE_COLOR}) in the tooltip.
         */
        public Builder exclusiveTag(String tag) {
            this.exclusiveTag      = tag;
            this.exclusiveTagColor = DEFAULT_EXCLUSIVE_COLOR;
            return this;
        }

        /**
         * Sets an exclusive tag shown in a custom ARGB colour in the tooltip.
         *
         * @param tag   the label text, e.g. {@code "2021 Easter Exclusive"}
         * @param color 24-bit RGB int, e.g. {@code 0xFF55FF}
         */
        public Builder exclusiveTag(String tag, int color) {
            this.exclusiveTag      = tag;
            this.exclusiveTagColor = color;
            return this;
        }

        /**
         * Sets a custom color for the exclusive tag (must be called after exclusiveTag).
         *
         * @param color 24-bit RGB int, e.g. {@code 0xFFFFFF}
         */
        public Builder tagColor(int color) {
            this.exclusiveTagColor = color;
            return this;
        }

        /**
         * Sets a particle effect that appears when using the tool.
         * - Sword/Axe: spawns on entity hit
         * - Bow: trails the arrow
         * - Pickaxe/Shovel/Axe: spawns when breaking blocks
         *
         * @param particle the particle type, e.g. {@code ParticleTypes.HEART}
         */
        public Builder addParticle(ParticleOptions particle) {
            this.particle = particle;
            return this;
        }

        public ToolSkinItem build() {
            if (rarity == null)      throw new IllegalStateException("ToolSkinItem.Builder: rarity not set");
            if (skinType == null)    throw new IllegalStateException("ToolSkinItem.Builder: type not set");
            if (skinTexture == null) throw new IllegalStateException("ToolSkinItem.Builder: texture not set");
            if (skinSetName == null) throw new IllegalStateException("ToolSkinItem.Builder: skinSetName not set");
            return new ToolSkinItem(this);
        }
    }
}
