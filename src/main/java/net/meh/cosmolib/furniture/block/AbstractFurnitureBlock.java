package net.meh.cosmolib.furniture.block;

import net.meh.cosmolib.furniture.blockentity.FurnitureBlockEntity;
import net.meh.cosmolib.paint.PaintData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for all CosmoLib furniture blocks.
 *
 * Provides:
 *   • 8-way ROTATION property (set automatically on placement)
 *   • Optional FRAGILE flag (warn players to use assembly table)
 *   • Paint-color transfer: stack → block entity on place, block entity → stack on break/pick
 *
 * For optional light:  override {@link #getLightEmission(BlockState, BlockGetter, BlockPos)}
 * For optional particles: override {@link #animateTick}
 *
 * Subclasses must implement {@link #newBlockEntity} and {@link #codec}.
 */
public abstract class AbstractFurnitureBlock extends BaseEntityBlock {

    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 7);
    public static final BooleanProperty FRAGILE  = BooleanProperty.create("fragile");

    private static final int FACE_OFFSET  = 4;
    private static final int MODEL_OFFSET = 0;

    protected AbstractFurnitureBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0).setValue(FRAGILE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(ROTATION, FRAGILE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        float yaw = ctx.getRotation();
        int rot = Math.floorMod(Math.round(yaw / 45.0f) + FACE_OFFSET + MODEL_OFFSET, 8);
        return defaultBlockState().setValue(ROTATION, rot).setValue(FRAGILE, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                             @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        int color = PaintData.getColor(stack);
        if (color >= 0 && level.getBlockEntity(pos) instanceof FurnitureBlockEntity be) {
            be.setPaintColor(color);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof FurnitureBlockEntity be) {
            int color = be.getPaintColor();
            if (color >= 0) PaintData.applyColor(stack, color);
        }
        return stack;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                               @Nullable BlockEntity be, ItemStack tool) {
        if (!level.isClientSide && be instanceof FurnitureBlockEntity fbe) {
            ItemStack drop = new ItemStack(this);
            int color = fbe.getPaintColor();
            if (color >= 0) PaintData.applyColor(drop, color);
            popResource(level, pos, drop);
        }
        super.playerDestroy(level, player, pos, state, be, tool);
    }
}
