package net.meh.cosmolib.registry;

import net.meh.cosmolib.block.PaintingTableBlock;
import net.meh.cosmolib.furniture.FurnitureOptions;
import net.meh.cosmolib.furniture.FurnitureShape;
import net.meh.cosmolib.furniture.block.AnimatedFurnitureBlock;
import net.meh.cosmolib.furniture.block.DecorationBlock;
import net.meh.cosmolib.furniture.block.FurnitureChildBlock;
import net.meh.cosmolib.paint.PaintColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.meh.cosmolib.CosmoLib.MOD_ID;

public final class CosmoLibBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);

    public static final DeferredBlock<PaintingTableBlock> PAINTING_TABLE =
            BLOCKS.register("painting_table", () -> new PaintingTableBlock(
                    BlockBehaviour.Properties.of()
                            .strength(2.5f)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
            ));

    public static final DeferredBlock<DecorationBlock> COSMO_RUG =
            BLOCKS.register("cosmo_rug", () -> new DecorationBlock(
                    BlockBehaviour.Properties.of()
                            .strength(1.5f)
                            .sound(SoundType.WOOD)
                            .noCollission()
                            .noOcclusion(),
                    FurnitureShape.FLAT,
                    FurnitureOptions.defaults().defaultColor(PaintColor.LIGHT_BLUE, 7)
            ));

    // -----------------------------------------------------------------------
    // Example animated furniture (remove when a real mod supplies its own)
    // -----------------------------------------------------------------------

    public static final DeferredBlock<AnimatedFurnitureBlock> AGED_FLAG =
            BLOCKS.register("aged_flag", () -> new AnimatedFurnitureBlock(
                    BlockBehaviour.Properties.of()
                            .strength(1.0f)
                            .sound(SoundType.WOOD)
                            .noOcclusion(),
                    FurnitureOptions.defaults()
                            .paintable()
                            .defaultColor(PaintColor.LIGHT_BLUE, 7)
            ));

    // -----------------------------------------------------------------------
    // Multi-block child placeholder (never obtainable by players)
    // -----------------------------------------------------------------------

    /**
     * Invisible placeholder block placed at every non-anchor position of a
     * multi-block furniture piece.  Not obtainable — no BlockItem is registered.
     */
    public static final DeferredBlock<FurnitureChildBlock> FURNITURE_CHILD =
            BLOCKS.register("furniture_child", () -> new FurnitureChildBlock(
                    BlockBehaviour.Properties.of()
                            .strength(0.0f)
                            .noOcclusion()
                            .isSuffocating((s, l, p) -> false)
                            .isViewBlocking((s, l, p) -> false)
            ));

    private CosmoLibBlocks() {}
}
