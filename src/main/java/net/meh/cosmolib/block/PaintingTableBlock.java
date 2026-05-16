package net.meh.cosmolib.block;

import com.mojang.serialization.MapCodec;
import net.meh.cosmolib.screen.PaintingTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class PaintingTableBlock extends Block {

    public static final MapCodec<PaintingTableBlock> CODEC = simpleCodec(PaintingTableBlock::new);

    public PaintingTableBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new PaintingTableMenu(id, inv),
                Component.translatable("container.cosmolib.painting_table")
        ));
        return InteractionResult.CONSUME;
    }
}
