package net.meh.cosmolib.tag;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class CosmoLibTags {

    private CosmoLibTags() {}

    public static final class Blocks {
        /** All blocks that accept paint from the paintbrush or painting table. */
        public static final TagKey<Block> PAINTABLE =
                BlockTags.create(ResourceLocation.fromNamespaceAndPath("cosmolib", "paintable"));

        private Blocks() {}
    }

    public static final class Items {
        /** Cosmetic items shown in the wardrobe screen. */
        public static final TagKey<Item> COSMETICS =
                ItemTags.create(ResourceLocation.fromNamespaceAndPath("cosmolib", "cosmetics"));

        private Items() {}
    }
}
