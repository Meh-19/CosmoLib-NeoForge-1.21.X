package net.meh.cosmolib.registry;

import net.meh.cosmolib.block.PaintingTableBlock;
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

    private CosmoLibBlocks() {}
}
