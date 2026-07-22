package net.meh.cosmolib.furniture;

import net.meh.cosmolib.furniture.block.AbstractFurnitureBlock;
import net.meh.cosmolib.furniture.item.FurnitureBlockItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Pair of deferred handles returned by every {@link FurnitureRegistrar} factory method.
 *
 * <p>Store it as a static field in your mod class to access the block or item
 * handle wherever needed:
 * <pre>{@code
 * public static final FurnitureEntry MY_CHAIR =
 *     FURNITURE.sittable("my_chair", BlockBehaviour.Properties.of().strength(2f));
 *
 * // Access the block:
 * Block block = MY_CHAIR.getBlock().get();
 *
 * // Access the item (e.g. for a recipe):
 * Item item = MY_CHAIR.getItem().get();
 * }</pre>
 */
public final class FurnitureEntry {

    private final DeferredBlock<? extends AbstractFurnitureBlock> block;
    private final DeferredItem<FurnitureBlockItem> item;

    FurnitureEntry(DeferredBlock<? extends AbstractFurnitureBlock> block,
                   DeferredItem<FurnitureBlockItem> item) {
        this.block = block;
        this.item  = item;
    }

    /**
     * Returns the deferred block handle.
     * Call {@link DeferredBlock#get()} after registration completes to get the actual block.
     */
    public DeferredBlock<? extends AbstractFurnitureBlock> getBlock() {
        return block;
    }

    /**
     * Returns the deferred item handle.
     * Call {@link DeferredItem#get()} after registration completes to get the actual item.
     */
    public DeferredItem<FurnitureBlockItem> getItem() {
        return item;
    }
}
