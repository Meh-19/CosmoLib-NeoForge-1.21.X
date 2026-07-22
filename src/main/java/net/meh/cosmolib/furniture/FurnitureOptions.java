package net.meh.cosmolib.furniture;

import net.meh.cosmolib.cosmetic.CosmeticDefault;
import net.meh.cosmolib.furniture.blockentity.FurnitureBlockEntity;
import net.meh.cosmolib.paint.FinishType;
import net.meh.cosmolib.paint.PaintColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Opt-in behavioural flags for furniture blocks.
 *
 * <p>Create with {@link #defaults()} then chain the desired flags:
 * <pre>{@code
 * new DecorationBlock(props, FurnitureShape.COLUMN, FurnitureOptions.defaults().waterloggable())
 * new DecorationBlock(props, FurnitureShape.FLAT,   FurnitureOptions.defaults().fragile())
 * new AnimatedFurnitureBlock(props, FurnitureOptions.defaults()
 *         .defaultColor(PaintColor.LIGHT_BLUE, 7))
 * }</pre>
 */
public final class FurnitureOptions {

    private boolean fragile;
    private boolean waterloggable;
    private boolean paintable;
    @Nullable private CosmeticDefault defaultAppearance;
    private int seatLevel = -1;
    @Nullable private Supplier<BlockEntityType<? extends FurnitureBlockEntity>> beType;
    @Nullable private Supplier<Block> dropAsBlock;

    private FurnitureOptions() {}

    /** Returns a new options object with all flags disabled. */
    public static FurnitureOptions defaults() {
        return new FurnitureOptions();
    }

    /**
     * The block shatters on break — no item drop, like glass.
     * Works for player mining; the block is simply destroyed without dropping itself.
     */
    public FurnitureOptions fragile() {
        this.fragile = true;
        return this;
    }

    /**
     * The block can be placed inside water and retains water when waterlogged.
     * Players can also right-click with a water bucket to waterlog after placement.
     */
    public FurnitureOptions waterloggable() {
        this.waterloggable = true;
        return this;
    }

    /**
     * The block can be painted with a {@link net.meh.cosmolib.item.PaintbrushItem} and
     * can be placed in the painting table to choose a colour before placing.
     * Shows the paint-brush icon in the item tooltip.
     */
    public FurnitureOptions paintable() {
        this.paintable = true;
        return this;
    }

    /**
     * Bakes a default solid paint colour into every item stack of this furniture,
     * the same way {@link net.meh.cosmolib.cosmetic.CosmeticItem} works.
     * Shade 1 = lightest, 7 = darkest.
     */
    public FurnitureOptions defaultColor(PaintColor color, int shade) {
        this.defaultAppearance = CosmeticDefault.color(color, shade);
        return this;
    }

    /**
     * Bakes a default animated finish into every item stack of this furniture.
     */
    public FurnitureOptions defaultFinish(FinishType finish) {
        this.defaultAppearance = CosmeticDefault.finish(finish);
        return this;
    }

    /**
     * Sets how high the player sits when using this furniture, in 1/16-block increments.
     * Level 1 = 1/16 block above the origin, level 16 = 1 full block.
     * Default (no call) uses level 5 (~0.3 blocks, a comfortable chair height).
     */
    public FurnitureOptions seatHeight(int level) {
        if (level < 1 || level > 16) throw new IllegalArgumentException("seatHeight level must be 1–16, got " + level);
        this.seatLevel = level;
        return this;
    }

    /**
     * Overrides which {@link BlockEntityType} is used when the block creates its block entity.
     * If not set, each concrete block class falls back to its own default type.
     *
     * <p>Use this when a dependent mod needs all its furniture blocks to share a single
     * block entity type (required for correct NBT deserialization in NeoForge):
     * <pre>{@code
     * FurnitureOptions.defaults()
     *     .fragile()
     *     .blockEntityType(() -> MyModBlockEntityTypes.ARTIFACT_BE.get())
     * }</pre>
     */
    public FurnitureOptions blockEntityType(Supplier<BlockEntityType<? extends FurnitureBlockEntity>> supplier) {
        this.beType = supplier;
        return this;
    }

    /**
     * When this block (typically a wall or ceiling variant) is broken, drop the specified
     * block's item instead of this block's own item. Use this so that breaking a ceiling
     * variant gives back the canonical floor-placed item.
     */
    public FurnitureOptions dropAs(Supplier<Block> blockToDrop) {
        this.dropAsBlock = blockToDrop;
        return this;
    }

    public boolean isFragile()                        { return fragile;           }
    public boolean isWaterloggable()                  { return waterloggable;     }
    public boolean isPaintable()                      { return paintable;         }
    @Nullable public CosmeticDefault getDefaultAppearance() { return defaultAppearance; }
    /** Returns the seat height in blocks (1/16 increments), or -1 if not set. */
    public float getSeatHeight()                      { return seatLevel < 0 ? 5f / 16f : seatLevel / 16f; }
    /** Returns the block entity type supplier, or {@code null} if no override was set. */
    @Nullable public Supplier<BlockEntityType<? extends FurnitureBlockEntity>> getBlockEntityType() { return beType; }
    /** Returns the block whose item should be dropped on break, or {@code null} to drop this block's item. */
    @Nullable public Supplier<Block> getDropAsBlock() { return dropAsBlock; }
}
