package net.meh.cosmolib.furniture.blockentity;

import net.meh.cosmolib.registry.CosmoLibBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Base block entity for all static furniture blocks.
 * Stores the paint color (-1 = unpainted) and is extended by animated furniture.
 */
public class FurnitureBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final String TAG_PAINT_COLOR = "PaintColor";

    private int paintColor = -1;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** Used by CosmoLib's own FURNITURE_ENTITY type registration. */
    public FurnitureBlockEntity(BlockPos pos, BlockState state) {
        super(CosmoLibBlockEntityTypes.FURNITURE_ENTITY.get(), pos, state);
    }

    /** Used by AnimatedFurnitureBlockEntity and dependent mods that register their own type. */
    public FurnitureBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ------------------------------------------------------------------
    // GeoBlockEntity — static furniture plays no animations
    // ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
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
