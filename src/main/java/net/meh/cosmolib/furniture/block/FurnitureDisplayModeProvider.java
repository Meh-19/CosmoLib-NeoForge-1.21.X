package net.meh.cosmolib.furniture.block;

import net.minecraft.world.level.block.state.BlockState;

/** Implement on a block to tell the renderer which display mode to use. Default is TOP_FACE. */
public interface FurnitureDisplayModeProvider {
    FurnitureDisplayMode getDisplayMode(BlockState state);
}
