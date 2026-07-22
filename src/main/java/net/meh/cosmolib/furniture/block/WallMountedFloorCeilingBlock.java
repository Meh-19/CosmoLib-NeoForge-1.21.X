package net.meh.cosmolib.furniture.block;

import net.meh.cosmolib.furniture.FurnitureOptions;
import net.meh.cosmolib.furniture.FurnitureShape;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * Floor-or-ceiling banner/tapestry block that always renders in WALL display mode
 * regardless of which surface it's placed on.
 *
 * <p>Placement still follows floor/ceiling logic (UP face → floor, DOWN face →
 * ceiling variant), but the block entity renderer treats it like a wall-mounted
 * hanging — translate(0.5,0.5,0.5) + face rotation + translate(0,0,0.45).</p>
 *
 * <p>Use this for banner-post blocks where the fabric hangs downward from a
 * post that rests on the floor or ceiling.</p>
 */
public class WallMountedFloorCeilingBlock extends FloorCeilingFurnitureBlock {

    public WallMountedFloorCeilingBlock(BlockBehaviour.Properties props,
                                         Supplier<Block> ceilingVariant) {
        super(props, FurnitureShape.FULL, FurnitureOptions.defaults(), ceilingVariant);
    }

    public WallMountedFloorCeilingBlock(BlockBehaviour.Properties props, FurnitureOptions opts,
                                         Supplier<Block> ceilingVariant) {
        super(props, FurnitureShape.FULL, opts, ceilingVariant);
    }

    public WallMountedFloorCeilingBlock(BlockBehaviour.Properties props, FurnitureShape shape,
                                         FurnitureOptions opts, Supplier<Block> ceilingVariant) {
        super(props, shape, opts, ceilingVariant);
    }

    @Override
    public FurnitureDisplayMode getDisplayMode(BlockState state) {
        return FurnitureDisplayMode.WALL;
    }
}
