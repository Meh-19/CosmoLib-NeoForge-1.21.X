package net.meh.cosmolib.furniture.block;

import com.mojang.serialization.MapCodec;
import net.meh.cosmolib.entity.SeatEntity;
import net.meh.cosmolib.furniture.blockentity.FurnitureBlockEntity;
import net.meh.cosmolib.furniture.blockentity.FurnitureChildBlockEntity;
import net.meh.cosmolib.furniture.layout.MultiBlockLayoutManager;
import net.meh.cosmolib.paint.PaintData;
import net.meh.cosmolib.registry.CosmoLibBlockEntityTypes;
import net.meh.cosmolib.registry.CosmoLibItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Invisible placeholder block placed at every non-anchor position of a multi-block
 * furniture piece.
 *
 * <p>Child blocks are <em>never</em> obtainable by players — no {@link net.minecraft.world.item.BlockItem}
 * is registered for this block.  They are placed and removed automatically by
 * {@link AbstractFurnitureBlock} when a multi-block layout is present.
 *
 * <h3>Removal behaviour</h3>
 * When any child block is broken or otherwise removed, it looks up the root block
 * via its {@link FurnitureChildBlockEntity}, then removes <em>all</em> other
 * child positions and the root.  A static guard prevents cascading re-entry.
 */
public class FurnitureChildBlock extends BaseEntityBlock {

    public static final MapCodec<FurnitureChildBlock> CODEC = simpleCodec(FurnitureChildBlock::new);

    /**
     * A guard flag to prevent re-entrant cleanup.  The server is single-threaded
     * so a simple boolean suffices; it is set while a multi-block teardown is in
     * progress and cleared when it completes.
     */
    private static boolean cleaningUp = false;

    /**
     * Half-block (slab) collision / outline shape: full XZ, 0–8/16 in Y.
     */
    private static final VoxelShape SLAB_SHAPE = Block.box(0, 0, 0, 16, 8, 16);

    /**
     * Holds the root furniture item to be dropped in {@link #playerDestroy}.
     *
     * <p>Set in {@link #onRemove} just before the cascade destroys the root block
     * entity (which holds the paint-color data).  Cleared in {@link #playerDestroy}
     * once consumed.  Because the server is single-threaded and the cascade completes
     * synchronously before {@code playerDestroy} is called, a simple static field
     * is sufficient.
     */
    @Nullable
    public static ItemStack pendingPlayerDrop = null;

    public FurnitureChildBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends FurnitureChildBlock> codec() {
        return CODEC;
    }

    // ------------------------------------------------------------------
    // Shape / rendering
    // ------------------------------------------------------------------

    /** Full-block outline — kept as full block so child positions are always selectable/breakable. */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.block();
    }

    /**
     * Returns a slab (half-height) collision shape when the child's
     * {@link FurnitureChildBlockEntity#isSlab()} flag is set; otherwise full-block.
     */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (level.getBlockEntity(pos) instanceof FurnitureChildBlockEntity child && child.isSlab()) {
            return SLAB_SHAPE;
        }
        return Shapes.block();
    }

    /** Invisible — the root block renders the entire model, including the space of children. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // ------------------------------------------------------------------
    // Sitting interaction
    // ------------------------------------------------------------------

    /**
     * When a player right-clicks a child block that has been marked as a seat position,
     * spawn a {@link SeatEntity} at this position and mount the player onto it.
     *
     * <p>If the player is holding the bounding box selector, the click is passed through
     * so the tool can process it instead of triggering the sit action.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FurnitureChildBlockEntity child) || !child.isSeat()) {
            return InteractionResult.PASS;
        }

        // Let the bounding-box selector handle the click — don't sit
        if (player.getMainHandItem().is(CosmoLibItems.BOUNDING_BOX_SELECTOR.get())
                || player.getOffhandItem().is(CosmoLibItems.BOUNDING_BOX_SELECTOR.get())) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) return InteractionResult.SUCCESS;

        // Seat already occupied?
        for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(pos).inflate(0.1))) {
            if (e instanceof net.minecraft.world.entity.decoration.ArmorStand) continue;
            return InteractionResult.CONSUME; // occupied
        }

        SeatEntity seat = new SeatEntity(level, pos, child.getSeatHeight());
        level.addFreshEntity(seat);
        player.startRiding(seat);
        return InteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------------
    // Removal cascade
    // ------------------------------------------------------------------

    /**
     * When a child block is removed (and the removal is not already part of a
     * larger multi-block teardown), finds the root block, iterates all layout
     * positions for the current rotation, and removes every remaining sibling
     * child block and the root itself.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                          BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !newState.is(this) && !cleaningUp) {
            if (level.getBlockEntity(pos) instanceof FurnitureChildBlockEntity child) {
                BlockPos rootPos      = child.getRootPos();
                String   furnitureId  = child.getFurnitureId();
                BlockState rootState  = level.getBlockState(rootPos);

                // Only cascade if the root is still a furniture block (not already torn down)
                if (rootState.getBlock() instanceof AbstractFurnitureBlock afb) {
                    // Capture the drop NOW, before the cascade destroys the root block entity.
                    // playerDestroy (called after onRemove) will consume this.
                    pendingPlayerDrop = afb.getCloneItemStack(level, rootPos, rootState);

                    int rotation = rootState.getValue(AbstractFurnitureBlock.ROTATION);
                    cleaningUp = true;
                    try {
                        MultiBlockLayoutManager.get(furnitureId).ifPresent(layout -> {
                            List<BlockPos> rotated = layout.getRotatedPositions(rotation);
                            // Remove all sibling children (not ourselves — already being removed)
                            for (BlockPos rel : rotated) {
                                if (rel.equals(BlockPos.ZERO)) continue; // skip anchor
                                BlockPos worldPos = rootPos.offset(rel);
                                if (!worldPos.equals(pos)
                                        && level.getBlockState(worldPos).is(this)) {
                                    level.removeBlock(worldPos, false);
                                }
                            }
                            // Remove the root furniture block
                            if (level.getBlockState(rootPos).is(rootState.getBlock())) {
                                level.removeBlock(rootPos, false);
                            }
                        });
                    } finally {
                        cleaningUp = false;
                    }
                }
            }
        }

        // Eject any rider sitting on this seat child (runs regardless of cleaningUp)
        if (!level.isClientSide && !newState.is(this)) {
            level.getEntitiesOfClass(SeatEntity.class, new AABB(pos).inflate(0.1))
                    .forEach(Entity::discard);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Returns whether a multi-block teardown is currently in progress. */
    public static boolean isCleaningUp() {
        return cleaningUp;
    }

    // ------------------------------------------------------------------
    // Player-break drop (via pending drop captured in onRemove)
    // ------------------------------------------------------------------

    /**
     * Spawns the item captured in {@link #pendingPlayerDrop} when a player directly
     * breaks a child block.  The pending drop is set in {@link #onRemove} before the
     * cascade removes the root block entity (which holds paint color data).
     */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                               @Nullable BlockEntity be, ItemStack tool) {
        if (!level.isClientSide) {
            ItemStack drop = pendingPlayerDrop;
            pendingPlayerDrop = null; // always clear, even if null
            if (drop != null && !drop.isEmpty()) {
                popResource(level, pos, drop);
            }
        }
        super.playerDestroy(level, player, pos, state, be, tool);
    }

    // ------------------------------------------------------------------
    // Pick block — delegate to root
    // ------------------------------------------------------------------

    /**
     * Returns the same item a player would get from pick-blocking the root
     * furniture block, including any applied paint color.
     */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof FurnitureChildBlockEntity child) {
            BlockPos rootPos   = child.getRootPos();
            BlockState rootSt  = level.getBlockState(rootPos);
            if (rootSt.getBlock() instanceof AbstractFurnitureBlock afb) {
                return afb.getCloneItemStack(level, rootPos, rootSt);
            }
        }
        return ItemStack.EMPTY;
    }

    // ------------------------------------------------------------------
    // Client extensions (particle suppression)
    // ------------------------------------------------------------------

    /**
     * Suppresses break / hit particles.
     * Child blocks are invisible ({@link RenderShape#INVISIBLE}) and have no block model,
     * so without this override Minecraft would render missing-texture (pink/black) particles.
     */
    @Override
    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions() {
            @Override
            public boolean addDestroyEffects(BlockState state,
                                              net.minecraft.world.level.Level level,
                                              BlockPos pos,
                                              net.minecraft.client.particle.ParticleEngine manager) {
                return true; // suppress default particles
            }
            @Override
            public boolean addHitEffects(BlockState state,
                                          net.minecraft.world.level.Level level,
                                          net.minecraft.world.phys.HitResult target,
                                          net.minecraft.client.particle.ParticleEngine manager) {
                return true; // suppress default particles
            }
        });
    }

    // ------------------------------------------------------------------
    // Block entity
    // ------------------------------------------------------------------

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return CosmoLibBlockEntityTypes.FURNITURE_CHILD_BE.get().create(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }
}
