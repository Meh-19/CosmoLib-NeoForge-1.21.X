package net.meh.cosmolib.crate;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * Immutable descriptor for a registered crate type.
 *
 * <p>Create instances via the {@link Builder}:
 * <pre>{@code
 * CrateRegistry.register(CrateType.builder()
 *     .id(ResourceLocation.fromNamespaceAndPath("mymod", "treasure_crate"))
 *     .lootTable(ResourceLocation.fromNamespaceAndPath("mymod", "treasure_crate"))
 *     .model(ResourceLocation.fromNamespaceAndPath("mymod", "geo/entity/treasure_crate.geo.json"))
 *     .texture(ResourceLocation.fromNamespaceAndPath("mymod", "textures/entity/treasure_crate.png"))
 *     .animations(ResourceLocation.fromNamespaceAndPath("mymod", "animations/entity/treasure_crate.animation.json"))
 *     .rerolls(3)
 *     .item(() -> MyItems.TREASURE_CRATE.get())
 *     .build());
 * }</pre>
 */
public final class CrateType {

    private final ResourceLocation id;
    private final ResourceLocation lootTableId;
    private final ResourceLocation modelLocation;
    private final ResourceLocation textureLocation;
    private final ResourceLocation animationLocation;
    private final int rerollCount;
    private final Supplier<Item> itemSupplier;

    private CrateType(Builder b) {
        this.id                = b.id;
        this.lootTableId       = b.lootTableId;
        this.modelLocation     = b.modelLocation;
        this.textureLocation   = b.textureLocation;
        this.animationLocation = b.animationLocation;
        this.rerollCount       = b.rerollCount;
        this.itemSupplier      = b.itemSupplier;
    }

    /** Registry ID of this crate type (e.g. {@code mymod:treasure_crate}). */
    public ResourceLocation getId() { return id; }

    /** ResourceLocation used to look up {@link CrateLootTable} data. */
    public ResourceLocation getLootTableId() { return lootTableId; }

    /** Path to the GeckoLib {@code .geo.json} model file. */
    public ResourceLocation getModelLocation() { return modelLocation; }

    /** Path to the texture {@code .png} file. */
    public ResourceLocation getTextureLocation() { return textureLocation; }

    /** Path to the GeckoLib {@code .animation.json} file. */
    public ResourceLocation getAnimationLocation() { return animationLocation; }

    /** Number of rerolls the player gets when opening this crate type. */
    public int getRerollCount() { return rerollCount; }

    /** The placeable {@link Item} associated with this crate type. */
    public Item getItem() { return itemSupplier.get(); }

    /** Returns a new {@link Builder} for constructing a {@link CrateType}. */
    public static Builder builder() { return new Builder(); }

    // ------------------------------------------------------------------
    // Builder
    // ------------------------------------------------------------------

    /**
     * Fluent builder for {@link CrateType}.
     * All fields except {@code rerolls} (default 3) are required.
     */
    public static final class Builder {

        private ResourceLocation id;
        private ResourceLocation lootTableId;
        private ResourceLocation modelLocation;
        private ResourceLocation textureLocation;
        private ResourceLocation animationLocation;
        private int rerollCount = 3;
        private Supplier<Item> itemSupplier;

        private Builder() {}

        /** Sets the registry ID of this crate type. */
        public Builder id(ResourceLocation id) {
            this.id = id;
            return this;
        }

        /** Sets the loot-table ResourceLocation. */
        public Builder lootTable(ResourceLocation lootTable) {
            this.lootTableId = lootTable;
            return this;
        }

        /** Sets the path to the GeckoLib geo model JSON. */
        public Builder model(ResourceLocation model) {
            this.modelLocation = model;
            return this;
        }

        /** Sets the path to the texture PNG. */
        public Builder texture(ResourceLocation texture) {
            this.textureLocation = texture;
            return this;
        }

        /** Sets the path to the GeckoLib animation JSON. */
        public Builder animations(ResourceLocation animations) {
            this.animationLocation = animations;
            return this;
        }

        /** Overrides the default reroll count (3). */
        public Builder rerolls(int count) {
            this.rerollCount = count;
            return this;
        }

        /** Sets the supplier for the placeable crate item. */
        public Builder item(Supplier<Item> item) {
            this.itemSupplier = item;
            return this;
        }

        /**
         * Builds and returns the {@link CrateType}.
         *
         * @throws NullPointerException if any required field is missing
         */
        public CrateType build() {
            if (id == null)                throw new NullPointerException("CrateType.id is required");
            if (lootTableId == null)       throw new NullPointerException("CrateType.lootTable is required");
            if (modelLocation == null)     throw new NullPointerException("CrateType.model is required");
            if (textureLocation == null)   throw new NullPointerException("CrateType.texture is required");
            if (animationLocation == null) throw new NullPointerException("CrateType.animations is required");
            if (itemSupplier == null)      throw new NullPointerException("CrateType.item is required");
            return new CrateType(this);
        }
    }
}
