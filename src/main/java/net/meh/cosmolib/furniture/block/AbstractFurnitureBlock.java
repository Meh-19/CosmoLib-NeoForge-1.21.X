package net.meh.cosmolib.furniture.block;

import net.meh.cosmolib.cosmetic.CosmeticDefault;
import net.meh.cosmolib.furniture.FurnitureOptions;
import net.meh.cosmolib.furniture.FurnitureShape;
import net.meh.cosmolib.furniture.blockentity.FurnitureBlockEntity;
import net.meh.cosmolib.paint.PaintData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
public abstract class AbstractFurnitureBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {

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

    // ------------------------------------------------------------------
    // Internal constants
    // ------------------------------------------------------------------

    /**
     * Added to the raw yaw-to-rotation mapping so that rotation=0 corresponds
     * to a player facing north, matching Minecraft's sign/skull convention.
     */
    private static final int FACE_OFFSET  = 4;
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
        registerDefaultState(stateDefinition.any()
                .setValue(ROTATION, 0)
                .setValue(WATERLOGGED, false));
    }

    /** {@code true} if this block can be painted with a paintbrush / in the painting table. */
    public boolean isPaintable() { return paintable; }

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
        builder.add(ROTATION, WATERLOGGED);
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

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return furnitureShape.getShape();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return furnitureShape.getShape();
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    // ------------------------------------------------------------------
    // Placement
    // ------------------------------------------------------------------

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        float yaw   = ctx.getRotation();
        int   rot   = Math.floorMod(Math.round(yaw / 45.0f) + FACE_OFFSET + MODEL_OFFSET, 8);
        boolean inWater = waterloggable
                && ctx.getLevel().getFluidState(ctx.getClickedPos()).is(FluidTags.WATER);
        return defaultBlockState()
                .setValue(ROTATION, rot)
                .setValue(WATERLOGGED, inWater);
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

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof FurnitureBlockEntity be) {
            int color = be.getPaintColor();
            if (color >= 0) PaintData.applyColor(stack, color);
        }
        return stack;
    }

    /**
     * Drops the block item with its paint color preserved.
     * Fragile blocks are silently destroyed with no drop.
     */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                               @Nullable BlockEntity be, ItemStack tool) {
        if (!fragile && !level.isClientSide && be instanceof FurnitureBlockEntity fbe) {
            ItemStack drop = new ItemStack(this);
            int color = fbe.getPaintColor();
            if (color >= 0) PaintData.applyColor(drop, color);
            popResource(level, pos, drop);
        }
        // super handles XP, food exhaustion, and stat tracking — but NOT loot-table
        // drops (no loot table is provided for furniture; drops are handled above).
        super.playerDestroy(level, player, pos, state, be, tool);
    }
}
