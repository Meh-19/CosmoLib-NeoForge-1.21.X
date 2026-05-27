package net.meh.cosmolib.furniture.block;

import com.mojang.serialization.MapCodec;
import net.meh.cosmolib.furniture.FurnitureOptions;
import net.meh.cosmolib.furniture.FurnitureShape;
import net.meh.cosmolib.furniture.blockentity.FurnitureBlockEntity;
import net.meh.cosmolib.registry.CosmoLibBlockEntityTypes;
import net.meh.cosmolib.registry.CosmoLibItems;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A furniture block the player can sit on by right-clicking.
 * The player dismounts when they sneak or the block is broken.
 */
public class SittableBlock extends AbstractFurnitureBlock {

    public static final MapCodec<SittableBlock> CODEC = simpleCodec(SittableBlock::new);

    /** Height offset (in blocks) at which the player sits above the block origin. */
    protected final float sitHeight;

    /** Defaults: {@link FurnitureShape#FULL}, no fragile, no waterloggable, seat at level 5. */
    public SittableBlock(BlockBehaviour.Properties props) {
        this(props, FurnitureOptions.defaults());
    }

    /** Explicit shape; seat at level 5. */
    public SittableBlock(BlockBehaviour.Properties props, FurnitureShape shape) {
        this(props, shape, FurnitureOptions.defaults());
    }

    /** {@link FurnitureShape#FULL} with explicit options. */
    public SittableBlock(BlockBehaviour.Properties props, FurnitureOptions opts) {
        this(props, FurnitureShape.FULL, opts);
    }

    /** Full constructor — shape + options. */
    public SittableBlock(BlockBehaviour.Properties props, FurnitureShape shape, FurnitureOptions opts) {
        super(props, shape, opts);
        this.sitHeight = opts.getSeatHeight();
    }

    @Override
    protected MapCodec<? extends SittableBlock> codec() {
        return CODEC;
    }

    /** Returns the sit height (in blocks) configured for this sittable block. */
    public float getSitHeight() { return sitHeight; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        // Let the bounding-box selector handle the click — don't sit
        if (player.getMainHandItem().is(CosmoLibItems.BOUNDING_BOX_SELECTOR.get())
                || player.getOffhandItem().is(CosmoLibItems.BOUNDING_BOX_SELECTOR.get())) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) return InteractionResult.SUCCESS;

        // Find any existing rider
        for (Entity e : level.getEntitiesOfClass(Entity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(0.1))) {
            if (e instanceof net.minecraft.world.entity.decoration.ArmorStand) continue;
            // Seat is occupied
            return InteractionResult.CONSUME;
        }

        // Spawn a seat entity and mount the player
        net.meh.cosmolib.entity.SeatEntity seat =
                new net.meh.cosmolib.entity.SeatEntity(level, pos, sitHeight);
        level.addFreshEntity(seat);
        player.startRiding(seat);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                          BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            // Eject any riders
            level.getEntitiesOfClass(net.meh.cosmolib.entity.SeatEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(0.1))
                    .forEach(Entity::discard);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (beTypeSupplier != null) return beTypeSupplier.get().create(pos, state);
        return CosmoLibBlockEntityTypes.FURNITURE_ENTITY.get().create(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }
}
