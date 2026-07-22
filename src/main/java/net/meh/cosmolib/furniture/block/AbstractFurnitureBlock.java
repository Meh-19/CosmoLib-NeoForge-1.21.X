package net.meh.cosmolib.furniture.block;

import net.meh.cosmolib.cosmetic.CosmeticDefault;
import net.meh.cosmolib.furniture.FurnitureOptions;
import net.meh.cosmolib.furniture.FurnitureShape;
import net.meh.cosmolib.furniture.blockentity.FurnitureBlockEntity;
import net.meh.cosmolib.furniture.blockentity.FurnitureChildBlockEntity;
import net.meh.cosmolib.furniture.layout.MultiBlockLayoutManager;
import net.meh.cosmolib.paint.PaintData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.meh.cosmolib.registry.CosmoLibBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Base class for all CosmoLib furniture blocks.
 *
 * <h3>Built-in features</h3>
 * <ul>
 *   <li><b>8-way rotation</b> — set automatically from player yaw on placement.</li>
 *   <li><b>Paint-color transfer</b> — stack → block entity on place, block entity → stack on break/pick.</li>
 *   <li><b>{@link FurnitureShape}</b> — controls the outline + collision hitbox.</li>
 * </ul>
 *
 * <h3>Opt-in options (via {@link FurnitureOptions})</h3>
 * <ul>
 *   <li><b>fragile</b> — block shatters on break (no item drop), like glass.</li>
 *   <li><b>waterloggable</b> — block can hold water; supports bucket interaction.</li>
 * </ul>
 *
 * Subclasses must implement {@link #newBlockEntity} and {@link #codec}.
 */
public abstract class AbstractFurnitureBlock extends BaseEntityBlock
        implements SimpleWaterloggedBlock, FurnitureDisplayModeProvider {

    // ------------------------------------------------------------------
    // Block state properties
    // ------------------------------------------------------------------

    /** 8-way horizontal rotation (45° steps, 0–7). Set from player yaw on placement. */
    public static final IntegerProperty ROTATION  = IntegerProperty.create("rotation", 0, 7);

    /**
     * Always present in the state definition so the block state codec never
     * changes shape between waterloggable and non-waterloggable instances.
     * Only has any effect when {@link #waterloggable} is {@code true}.
     */
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /**
     * Runtime fragile flag — toggled with the debug stick by structure developers.
     * When {@code true}, the block is destroyed without dropping an item and the
     * player who broke it receives a chat message explaining how to obtain the item.
     *
     * <p>Defaults to {@code false} on placement. Structure developers set this to
     * {@code true} on individual placed blocks to mark furniture as non-obtainable
     * through normal mining — useful for decorating generated structures while still
     * allowing players to obtain the item through other means (loot, crafting, etc.).
     */
    public static final BooleanProperty FRAGILE = BooleanProperty.create("fragile");

    // ------------------------------------------------------------------
    // Internal constants
    // ------------------------------------------------------------------

    /**
     * No offset — rotation=0 is produced when the player faces South (yaw=0),
     * which makes the block face North (toward where the player was standing).
     * This is the conventional "block faces the placer" behaviour used by most
     * furniture mods, and keeps the values consistent with the ROTATION_DIRS
     * table in BoundingBoxRenderer (0 = North, 2 = East, 4 = South, 6 = West).
     */
    private static final int FACE_OFFSET  = 0;
    private static final int MODEL_OFFSET = 0;

    private static final ResourceLocation COSMOLIB_FONT =
            ResourceLocation.fromNamespaceAndPath("cosmolib", "default");

    // ------------------------------------------------------------------
    // Instance fields
    // ------------------------------------------------------------------

    protected final FurnitureShape furnitureShape;

    /**
     * When {@code true} the block drops nothing on break (shatters like glass).
     * Set via {@link FurnitureOptions#fragile()}.
     */
    protected final boolean fragile;

    /**
     * When {@code true} the block participates in Minecraft's waterlogging system.
     * Set via {@link FurnitureOptions#waterloggable()}.
     */
    protected final boolean waterloggable;

    /**
     * When {@code true} the paintbrush can paint this block and it is accepted
     * by the painting table.  Set via {@link FurnitureOptions#paintable()}.
     */
    protected final boolean paintable;

    /**
     * Default paint colour or finish baked into every item stack of this block.
     * {@code null} means no default (renders white until painted).
     * Set via {@link FurnitureOptions#defaultColor} or {@link FurnitureOptions#defaultFinish}.
     */
    @Nullable
    private final CosmeticDefault defaultAppearance;

    /**
     * Optional override for the block entity type created by {@link #newBlockEntity}.
     * Dependent mods should set this so their blocks are registered under their own
     * {@link BlockEntityType}, ensuring NeoForge's validity check passes on world load.
     */
    @Nullable
    protected final Supplier<BlockEntityType<? extends FurnitureBlockEntity>> beTypeSupplier;

    /**
     * When non-null, broken or pick-blocked, this block drops/returns the specified
     * block's item instead of its own. Used for wall/ceiling variants that should
     * give back the canonical floor item when destroyed.
     */
    @Nullable
    private final Supplier<net.minecraft.world.level.block.Block> dropAsBlock;

    // ------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------

    /** Defaults: {@link FurnitureShape#FULL}, no fragile, no waterloggable. */
    protected AbstractFurnitureBlock(BlockBehaviour.Properties props) {
        this(props, FurnitureShape.FULL, FurnitureOptions.defaults());
    }

    /** Explicit shape; no fragile, no waterloggable. */
    protected AbstractFurnitureBlock(BlockBehaviour.Properties props, FurnitureShape shape) {
        this(props, shape, FurnitureOptions.defaults());
    }

    /** {@link FurnitureShape#FULL} with explicit options. */
    protected AbstractFurnitureBlock(BlockBehaviour.Properties props, FurnitureOptions opts) {
        this(props, FurnitureShape.FULL, opts);
    }

    /** Full constructor — shape + options. */
    protected AbstractFurnitureBlock(BlockBehaviour.Properties props, FurnitureShape shape, FurnitureOptions opts) {
        super(props);
        this.furnitureShape    = shape;
        this.fragile           = opts.isFragile();
        this.waterloggable     = opts.isWaterloggable();
        this.paintable         = opts.isPaintable();
        this.defaultAppearance = opts.getDefaultAppearance();
        this.beTypeSupplier    = opts.getBlockEntityType();
        this.dropAsBlock       = opts.getDropAsBlock();
        registerDefaultState(stateDefinition.any()
                .setValue(ROTATION, 0)
                .setValue(WATERLOGGED, false)
                .setValue(FRAGILE, false));
    }

    // ------------------------------------------------------------------
    // Registry name helper
    // ------------------------------------------------------------------

    /**
     * Returns the registry path of this block (e.g. {@code "ancient_arbor"}).
     * Used by the multi-block system to look up the layout in
     * {@link MultiBlockLayoutManager}.
     *
     * <p>Only valid after all registries are frozen (i.e. during normal gameplay).
     */
    public String getRegistryName() {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(this);
        return key != null ? key.getPath() : "";
    }

    // ------------------------------------------------------------------
    // Multi-block layout helpers
    // ------------------------------------------------------------------

    /**
     * Returns {@code true} if a {@link net.meh.cosmolib.furniture.layout.MultiBlockLayout}
     * has been loaded for this block.  When {@code true}, placement and removal
     * will automatically manage child blocks.
     */
    public boolean hasMultiBlockLayout() {
        return MultiBlockLayoutManager.get(getRegistryName()).isPresent();
    }

    /** {@code true} if this block can be painted with a paintbrush / in the painting table. */
    public boolean isPaintable() { return paintable; }

    /** Default display mode — subclasses override to return FLOOR, WALL, or BLOCK_UP. */
    @Override
    public FurnitureDisplayMode getDisplayMode(BlockState state) {
        return FurnitureDisplayMode.TOP_FACE;
    }

    /** Returns the default paint appearance baked into every item stack, or {@code null} if none. */
    @Nullable
    public CosmeticDefault getDefaultAppearance() {
        return defaultAppearance;
    }

    // ------------------------------------------------------------------
    // Block state definition
    // ------------------------------------------------------------------

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION, WATERLOGGED, FRAGILE);
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // ------------------------------------------------------------------
    // Tooltip
    // ------------------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        if (isPaintable()) {
            tooltip.add(Component.literal("ꑞ")
                    .withStyle(Style.EMPTY.withFont(COSMOLIB_FONT)));
        }
    }

    // ------------------------------------------------------------------
    // Shape / collision
    // ------------------------------------------------------------------

    /** Half-block (slab) collision shape: 0–8/16 in Y. */
    private static final VoxelShape SLAB_SHAPE = Block.box(0, 0, 0, 16, 8, 16);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        // Always return the configured outline so the root block can be targeted/broken.
        return furnitureShape.getShape();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        var layoutOpt = MultiBlockLayoutManager.get(getRegistryName());
        if (layoutOpt.isPresent()) {
            int rotation = state.getValue(ROTATION);
            var layout   = layoutOpt.get();

            // If the anchor position (ZERO) is not in the layout, it's passable (no collision).
            if (!layout.getRotatedPositions(rotation).contains(BlockPos.ZERO)) {
                return Shapes.empty();
            }

            // If the anchor is flagged as a slab position, return half-height collision.
            if (layout.getSlabPositions(rotation).contains(BlockPos.ZERO)) {
                return SLAB_SHAPE;
            }
        }
        return furnitureShape.getShape();
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    // ------------------------------------------------------------------
    // Client extensions (particle suppression)
    // ------------------------------------------------------------------

    /**
     * Suppresses the default block-break / block-hit particles.
     * Without this override, {@link RenderShape#INVISIBLE} blocks fall back to the
     * missing-texture (pink/black) sprite for particles since they have no block model.
     */
    @Override
    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions() {
            @Override
            public boolean addDestroyEffects(BlockState state,
                                              net.minecraft.world.level.Level level,
                                              BlockPos pos,
                                              net.minecraft.client.particle.ParticleEngine manager) {
                return true; // suppress missing-texture particles on break
            }
            @Override
            public boolean addHitEffects(BlockState state,
                                          net.minecraft.world.level.Level level,
                                          net.minecraft.world.phys.HitResult target,
                                          net.minecraft.client.particle.ParticleEngine manager) {
                return true; // suppress missing-texture particles on hit
            }
        });
    }

    // ------------------------------------------------------------------
    // Placement
    // ------------------------------------------------------------------

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        float yaw   = ctx.getRotation();
        int   rot   = Math.floorMod(Math.round(yaw / 45.0f) + FACE_OFFSET + MODEL_OFFSET, 8);
        boolean inWater = waterloggable
                && ctx.getLevel().getFluidState(ctx.getClickedPos()).is(FluidTags.WATER);
        BlockState state = defaultBlockState()
                .setValue(ROTATION, rot)
                .setValue(WATERLOGGED, inWater);

        // Multi-block placement check — verify all child positions are clear
        var layoutOpt = MultiBlockLayoutManager.get(getRegistryName());
        if (layoutOpt.isPresent()) {
            Level level   = ctx.getLevel();
            BlockPos anchor = ctx.getClickedPos();
            for (BlockPos rel : layoutOpt.get().getRotatedPositions(rot)) {
                if (rel.equals(BlockPos.ZERO)) continue; // anchor already checked by vanilla
                BlockPos worldPos = anchor.offset(rel);
                if (!level.getBlockState(worldPos).canBeReplaced(ctx)) {
                    return null; // blocked — abort placement
                }
            }
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                             @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        int color = PaintData.getColor(stack);
        if (color >= 0 && level.getBlockEntity(pos) instanceof FurnitureBlockEntity be) {
            be.setPaintColor(color);
        }

        // Multi-block placement — fill child positions
        if (!level.isClientSide) {
            MultiBlockLayoutManager.get(getRegistryName()).ifPresent(layout -> {
                int rotation   = state.getValue(ROTATION);
                String fid     = getRegistryName();
                BlockState childState = CosmoLibBlocks.FURNITURE_CHILD.get().defaultBlockState();
                List<BlockPos> slabPosRel = layout.getSlabPositions(rotation);
                List<BlockPos> seatPosRel = layout.getSeatingPositions(rotation);
                float seatH = layout.getSeatHeight();
                for (BlockPos rel : layout.getRotatedPositions(rotation)) {
                    if (rel.equals(BlockPos.ZERO)) continue; // anchor is already the root block
                    BlockPos worldPos = pos.offset(rel);
                    level.setBlock(worldPos, childState, Block.UPDATE_ALL);
                    if (level.getBlockEntity(worldPos) instanceof FurnitureChildBlockEntity child) {
                        child.init(pos, fid, slabPosRel.contains(rel), seatPosRel.contains(rel), seatH);
                    }
                }
            });
        }
    }

    /**
     * When the root furniture block is removed (and not replaced by the same block),
     * tear down all associated child blocks.  Protected against re-entry via
     * {@link FurnitureChildBlock#isCleaningUp()}.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                          BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide
                && !newState.is(this)
                && !FurnitureChildBlock.isCleaningUp()) {
            MultiBlockLayoutManager.get(getRegistryName()).ifPresent(layout -> {
                int rotation = state.getValue(ROTATION);
                for (BlockPos rel : layout.getRotatedPositions(rotation)) {
                    if (rel.equals(BlockPos.ZERO)) continue;
                    BlockPos worldPos = pos.offset(rel);
                    if (level.getBlockState(worldPos).is(CosmoLibBlocks.FURNITURE_CHILD.get())) {
                        level.removeBlock(worldPos, false);
                    }
                }
            });
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ------------------------------------------------------------------
    // Waterlogging
    // ------------------------------------------------------------------

    @Override
    public FluidState getFluidState(BlockState state) {
        return waterloggable && state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                   LevelAccessor level, BlockPos pos, BlockPos facingPos) {
        if (waterloggable && state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, facing, facingState, level, pos, facingPos);
    }

    /**
     * Gates bucket-waterlogging on the {@link #waterloggable} flag.
     * Non-waterloggable furniture ignores water buckets even though the
     * WATERLOGGED property exists in the state definition.
     */
    @Override
    public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level,
                                   BlockPos pos, BlockState state, Fluid fluid) {
        return waterloggable
                && fluid == Fluids.WATER
                && !state.getValue(WATERLOGGED);
    }

    // ------------------------------------------------------------------
    // Paint-color drops
    // ------------------------------------------------------------------

    private net.minecraft.world.level.block.Block resolveDropBlock() {
        return dropAsBlock != null ? dropAsBlock.get() : this;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(resolveDropBlock());
        if (level.getBlockEntity(pos) instanceof FurnitureBlockEntity be) {
            int color = be.getPaintColor();
            if (color >= 0) PaintData.applyColor(stack, color);
        }
        return stack;
    }

    /**
     * Drops the block item with its paint color preserved.
     *
     * <p>Fragile blocks are destroyed with no item drop and send the player an
     * action-bar message explaining why nothing was dropped.
     *
     * <p>Wall/ceiling variants drop the floor item when {@code dropAsBlock} is set.
     *
     * <p>The drop is unconditional with respect to the block entity: if the BE is
     * present and typed correctly the paint color is carried over; if it is absent
     * (e.g. BE type validation failed because the block was not listed in its
     * {@link net.minecraft.world.level.block.entity.BlockEntityType}) the plain item
     * still drops so the player is not left with nothing.
     */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                               @Nullable BlockEntity be, ItemStack tool) {
        if (!level.isClientSide) {
            // Read fragility exclusively from the block state so the debug stick
            // is always authoritative.  The compile-time flag only sets the default
            // via registerDefaultState — it is not re-checked here.
            boolean isFragile = state.getValue(FRAGILE);
            if (isFragile) {
                // Send a chat message only to the player who broke it explaining why
                // nothing dropped. Sent as a system message so it appears in chat.
                player.sendSystemMessage(
                        Component.translatable("tooltip.cosmolib.fragile_broken")
                                .withStyle(Style.EMPTY.withColor(0xD92625)));
            } else {
                // Always drop the item; apply paint color when the BE is available.
                ItemStack drop = new ItemStack(resolveDropBlock());
                if (be instanceof FurnitureBlockEntity fbe) {
                    int color = fbe.getPaintColor();
                    if (color >= 0) PaintData.applyColor(drop, color);
                }
                if (!drop.isEmpty()) popResource(level, pos, drop);
            }
        }
        // super handles XP, food exhaustion, and stat tracking — but NOT loot-table
        // drops (no loot table is provided for furniture; drops are handled above).
        super.playerDestroy(level, player, pos, state, be, tool);
    }
}
