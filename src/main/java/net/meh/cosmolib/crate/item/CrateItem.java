package net.meh.cosmolib.crate.item;

import net.meh.cosmolib.crate.CrateType;
import net.meh.cosmolib.crate.entity.CrateEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The placeable item for a crate type.
 *
 * <p>Right-clicking on any solid block face spawns the associated {@link CrateEntity}
 * on top of or adjacent to that face. In creative mode the item is not consumed.
 */
public class CrateItem extends Item {

    private final CrateType crateType;

    /**
     * Creates a new CrateItem for the given crate type.
     *
     * @param crateType the crate type this item places
     * @param props     item properties (stack size etc.)
     */
    public CrateItem(CrateType crateType, Item.Properties props) {
        super(props.stacksTo(16));
        this.crateType = crateType;
    }

    /** Returns the {@link CrateType} associated with this item. */
    public CrateType getCrateType() { return crateType; }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        BlockState targetState = level.getBlockState(pos);

        if (!targetState.canBeReplaced()) {
            return InteractionResult.FAIL;
        }

        float playerYaw = ctx.getPlayer() != null ? ctx.getPlayer().getYRot() : 0f;
        CrateEntity entity = CrateEntity.place(
                level, pos, playerYaw,
                crateType.getId(),
                crateType.getRerollCount());

        level.addFreshEntity(entity);

        if (ctx.getPlayer() != null && !ctx.getPlayer().isCreative()) {
            ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
