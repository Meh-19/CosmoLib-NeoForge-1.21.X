package net.meh.cosmolib.furniture.blockentity;

import net.meh.cosmolib.paint.PaintData;
import net.meh.cosmolib.registry.CosmoLibBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base block entity for all static furniture blocks.
 * Stores the paint color (-1 = unpainted).
 *
 * Rendering is handled by {@link net.meh.cosmolib.furniture.client.FurnitureBlockEntityRenderer}
 * which uses the vanilla ItemRenderer — no GeckoLib required for static pieces.
 */
public class FurnitureBlockEntity extends BlockEntity {

    // ------------------------------------------------------------------
    // Global client-side render tracking
    //
    // NeoForge 21.1.x does not reliably honour shouldRenderOffScreen() /
    // getRenderBoundingBox() for RenderShape.INVISIBLE blocks, so vanilla's
    // globalBlockEntities mechanism never fires for furniture.  We maintain
    // our own set of active (loaded, client-side) static furniture BEs and
    // re-render any that the section-visibility check skipped.
    //
    // GLOBAL_CLIENT_RENDERS  — populated in onLoad(), removed in setRemoved().
    // clientFrameCounter     — incremented once per frame by the render event.
    // lastRenderedClientFrame — stamped on the BE by the renderer each frame;
    //                           the event handler skips BEs already stamped.
    // ------------------------------------------------------------------

    /**
     * All active (loaded) static furniture block entities on the client.
     * Thread-safe: written from the main thread (chunk load/unload),
     * read from the render thread (global render pass).
     */
    public static final Set<FurnitureBlockEntity> GLOBAL_CLIENT_RENDERS =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    /** Incremented once per render frame. Render-thread only. */
    public static int clientFrameCounter = 0;

    /** Stamped each frame by the renderer.  Render-thread only. */
    public transient int lastRenderedClientFrame = -1;

    private static final String TAG_PAINT_COLOR = "PaintColor";

    private int paintColor = -1;

    /**
     * Render-thread cache for the painted ItemStack.
     * Rebuilt whenever the paint color changes (tracked by {@link #cachedPaintColor}).
     * Avoids allocating a new ItemStack on every render frame for furniture that
     * hasn't been repainted.
     */
    @Nullable private transient ItemStack cachedRenderStack = null;
    private transient int cachedPaintColor = Integer.MIN_VALUE;

    /** Used by CosmoLib's own FURNITURE_ENTITY type registration. */
    public FurnitureBlockEntity(BlockPos pos, BlockState state) {
        super(CosmoLibBlockEntityTypes.FURNITURE_ENTITY.get(), pos, state);
    }

    /** Used by AnimatedFurnitureBlockEntity and dependent mods that register their own type. */
    public FurnitureBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ------------------------------------------------------------------
    // Global render-set lifecycle
    // ------------------------------------------------------------------

    /**
     * Whether this block entity should participate in the global frustum-culling
     * render pass managed by {@link net.meh.cosmolib.event.ClientRenderEventHandler}.
     *
     * <p>Returns {@code true} by default so that all static furniture subclasses
     * (including those in dependent mods) opt in automatically without any extra
     * code.  Override and return {@code false} for animated furniture variants
     * that handle their own rendering (e.g. via GeckoLib) and must not be drawn
     * a second time by the global pass.
     *
     * <p><b>Client-side only.</b> This method is only called from
     * {@link #onLoad()} when {@code level.isClientSide} is {@code true}.
     */
    protected boolean shouldRegisterForGlobalRender() {
        return true;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide && shouldRegisterForGlobalRender()) {
            GLOBAL_CLIENT_RENDERS.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        GLOBAL_CLIENT_RENDERS.remove(this);
    }

    // ------------------------------------------------------------------
    // Paint color
    // ------------------------------------------------------------------

    public int getPaintColor()         { return paintColor; }

    public void setPaintColor(int color) {
        this.paintColor = color;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isPainted()         { return paintColor >= 0; }

    public void clearPaintColor()      { setPaintColor(-1); }

    /**
     * Returns a cached, paint-applied {@link ItemStack} for rendering, avoiding a
     * fresh allocation every frame.  The cache is invalidated automatically if the
     * paint color changes between calls (checked each frame, costs one int comparison).
     *
     * <p><b>Render thread only.</b>  Do not call from server logic.
     *
     * @param item the block's associated item (from {@code block.asItem()})
     */
    public ItemStack getCachedRenderStack(Item item) {
        if (cachedRenderStack == null || cachedPaintColor != paintColor) {
            cachedRenderStack = new ItemStack(item);
            if (paintColor >= 0) PaintData.applyColor(cachedRenderStack, paintColor);
            cachedPaintColor = paintColor;
        }
        return cachedRenderStack;
    }

    // ------------------------------------------------------------------
    // Render bounding box — prevents large/multi-block furniture from
    // being frustum-culled while still partially in view.
    // ------------------------------------------------------------------

    /**
     * Expands the frustum-culling AABB to 5 blocks in every direction around the
     * anchor block, so that large furniture models remain visible even when the
     * player looks slightly away from the anchor block's 1×1×1 position.
     *
     * <p>NeoForge patches {@code BlockEntity} to implement {@code IBlockEntityExtension}
     * at runtime; the {@code @Override} check therefore fails at compile time in some
     * 21.1.x builds.  The method still overrides correctly via Java virtual dispatch.
     */
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(5.0);
    }

    // ------------------------------------------------------------------
    // Serialization
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (paintColor >= 0) tag.putInt(TAG_PAINT_COLOR, paintColor);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        paintColor = tag.contains(TAG_PAINT_COLOR) ? tag.getInt(TAG_PAINT_COLOR) : -1;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (paintColor >= 0) tag.putInt(TAG_PAINT_COLOR, paintColor);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net,
                              net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt,
                              HolderLookup.Provider registries) {
        loadAdditional(pkt.getTag() != null ? pkt.getTag() : new CompoundTag(), registries);
    }
}
