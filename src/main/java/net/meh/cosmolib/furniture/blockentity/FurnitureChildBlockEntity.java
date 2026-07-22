package net.meh.cosmolib.furniture.blockentity;

import net.meh.cosmolib.registry.CosmoLibBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for {@link net.meh.cosmolib.furniture.block.FurnitureChildBlock}.
 *
 * <p>Stores the world position of the root (anchor) furniture block and the
 * furniture id so that the child can find the correct {@link net.meh.cosmolib.furniture.layout.MultiBlockLayout}
 * and clean up all sibling positions when it is removed.
 */
public class FurnitureChildBlockEntity extends BlockEntity {

    private static final String TAG_ROOT_POS     = "RootPos";
    private static final String TAG_FURNITURE_ID = "FurnitureId";
    private static final String TAG_SLAB         = "Slab";
    private static final String TAG_SEAT         = "Seat";
    private static final String TAG_SEAT_HEIGHT  = "SeatHeight";

    /** World position of the root (anchor) furniture block. */
    private BlockPos rootPos = BlockPos.ZERO;

    /** Registry path of the parent furniture block (e.g. {@code "ancient_arbor"}). */
    private String furnitureId = "";

    /**
     * Whether this child block occupies a "slab" (half-height) hitbox position.
     * Set at placement time via {@link #init(BlockPos, String, boolean, boolean, float)}.
     */
    private boolean slab = false;

    /**
     * Whether this child block is a sittable seat position.
     * Set at placement time via {@link #init(BlockPos, String, boolean, boolean, float)}.
     */
    private boolean seat = false;

    /**
     * The Y offset (in blocks) at which the rider entity sits when using this seat position.
     * Ignored if {@link #seat} is {@code false}.
     */
    private float seatHeight = 5f / 16f;

    // ------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------

    /** Used by the block entity type factory and by block entity creation. */
    public FurnitureChildBlockEntity(BlockPos pos, BlockState state) {
        super(CosmoLibBlockEntityTypes.FURNITURE_CHILD_BE.get(), pos, state);
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    /** Returns the world position of the root furniture block. */
    public BlockPos getRootPos()     { return rootPos;     }

    /** Returns the registry path of the owning furniture block. */
    public String getFurnitureId()   { return furnitureId; }

    /** Returns {@code true} if this child occupies a slab (half-height) hitbox position. */
    public boolean isSlab()          { return slab;        }

    /** Returns {@code true} if this child is a sittable seat position. */
    public boolean isSeat()          { return seat;        }

    /** Returns the Y sit-height offset (in blocks) for this seat position. */
    public float getSeatHeight()     { return seatHeight;  }

    /**
     * Sets the root world position, furniture id, slab flag, seat flag, and seat height
     * for this child. Marks the entity dirty and triggers a block update so clients are notified.
     */
    public void init(BlockPos root, String id, boolean isSlab, boolean isSeat, float seatH) {
        this.rootPos    = root;
        this.furnitureId = id;
        this.slab       = isSlab;
        this.seat       = isSeat;
        this.seatHeight = seatH;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ------------------------------------------------------------------
    // Serialization
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(TAG_ROOT_POS, NbtUtils.writeBlockPos(rootPos));
        tag.putString(TAG_FURNITURE_ID, furnitureId);
        tag.putBoolean(TAG_SLAB, slab);
        tag.putBoolean(TAG_SEAT, seat);
        tag.putFloat(TAG_SEAT_HEIGHT, seatHeight);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_ROOT_POS)) {
            rootPos = NbtUtils.readBlockPos(tag, TAG_ROOT_POS).orElse(BlockPos.ZERO);
        }
        furnitureId = tag.getString(TAG_FURNITURE_ID);
        slab        = tag.getBoolean(TAG_SLAB);
        seat        = tag.getBoolean(TAG_SEAT);
        if (tag.contains(TAG_SEAT_HEIGHT)) {
            seatHeight = tag.getFloat(TAG_SEAT_HEIGHT);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put(TAG_ROOT_POS, NbtUtils.writeBlockPos(rootPos));
        tag.putString(TAG_FURNITURE_ID, furnitureId);
        tag.putBoolean(TAG_SLAB, slab);
        tag.putBoolean(TAG_SEAT, seat);
        tag.putFloat(TAG_SEAT_HEIGHT, seatHeight);
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
