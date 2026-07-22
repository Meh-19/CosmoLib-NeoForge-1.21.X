package net.meh.cosmolib.furniture.tool.network;

import net.meh.cosmolib.furniture.tool.BoundingBoxSelectorSession;
import net.meh.cosmolib.furniture.tool.client.BoundingBoxClientState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * S→C: synchronises the full {@link BoundingBoxSelectorSession} state to the
 * client so {@link net.meh.cosmolib.furniture.tool.client.BoundingBoxRenderer}
 * can draw the live selection overlay.
 *
 * <p>Sent after every selection change (add, remove, undo) and when the session
 * starts, changes mode, or ends.
 */
public record SyncBBSessionPayload(
        boolean        sessionActive,
        String         furnitureId,
        long           anchorPosLong,    // BlockPos.asLong(), or Long.MIN_VALUE if not set
        int            furnitureRotation,
        boolean        inDiagonalMode,
        List<Long>     selectedPositions,
        int            selectionMode,    // BBSelectionMode ordinal: 0=FULL 1=SLAB 2=SEAT
        List<Long>     slabPositions,
        List<Long>     seatPositions,
        float          furnitureSeatHeight
) implements CustomPacketPayload {

    public static final Type<SyncBBSessionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("cosmolib", "sync_bb_session"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBBSessionPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public SyncBBSessionPayload decode(RegistryFriendlyByteBuf buf) {
                    boolean active    = buf.readBoolean();
                    String  fid       = buf.readUtf();
                    long    anchor    = buf.readLong();
                    int     rotation  = buf.readVarInt();
                    boolean diagonal  = buf.readBoolean();
                    int     count     = buf.readVarInt();
                    List<Long> positions = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) positions.add(buf.readLong());
                    int     mode      = buf.readVarInt();
                    int     slabCount = buf.readVarInt();
                    List<Long> slabs  = new ArrayList<>(slabCount);
                    for (int i = 0; i < slabCount; i++) slabs.add(buf.readLong());
                    int     seatCount = buf.readVarInt();
                    List<Long> seats  = new ArrayList<>(seatCount);
                    for (int i = 0; i < seatCount; i++) seats.add(buf.readLong());
                    float   seatH     = buf.readFloat();
                    return new SyncBBSessionPayload(active, fid, anchor, rotation, diagonal, positions, mode, slabs, seats, seatH);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, SyncBBSessionPayload v) {
                    buf.writeBoolean(v.sessionActive());
                    buf.writeUtf(v.furnitureId());
                    buf.writeLong(v.anchorPosLong());
                    buf.writeVarInt(v.furnitureRotation());
                    buf.writeBoolean(v.inDiagonalMode());
                    buf.writeVarInt(v.selectedPositions().size());
                    for (long l : v.selectedPositions()) buf.writeLong(l);
                    buf.writeVarInt(v.selectionMode());
                    buf.writeVarInt(v.slabPositions().size());
                    for (long l : v.slabPositions()) buf.writeLong(l);
                    buf.writeVarInt(v.seatPositions().size());
                    for (long l : v.seatPositions()) buf.writeLong(l);
                    buf.writeFloat(v.furnitureSeatHeight());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    // ------------------------------------------------------------------
    // Factory helpers
    // ------------------------------------------------------------------

    /**
     * Builds and sends this payload to the given player from the current session state.
     *
     * @param player  the player to send to
     * @param session the active session (non-null = active, null = clear/end session)
     */
    public static void sendTo(ServerPlayer player, @Nullable BoundingBoxSelectorSession session) {
        SyncBBSessionPayload pkt;
        if (session == null) {
            pkt = new SyncBBSessionPayload(false, "", Long.MIN_VALUE, 0, false,
                    List.of(), 0, List.of(), List.of(), 5f / 16f);
        } else {
            long anchorLong = session.anchorWorldPos != null
                    ? session.anchorWorldPos.asLong()
                    : Long.MIN_VALUE;
            List<Long> positions = session.selectedPositions.stream().map(BlockPos::asLong).toList();
            List<Long> slabs     = session.slabPositions.stream().map(BlockPos::asLong).toList();
            List<Long> seats     = session.seatPositions.stream().map(BlockPos::asLong).toList();
            pkt = new SyncBBSessionPayload(
                    true,
                    session.targetFurnitureId,
                    anchorLong,
                    session.furnitureRotation,
                    session.inDiagonalMode,
                    positions,
                    session.selectionMode.ordinal(),
                    slabs,
                    seats,
                    session.furnitureSeatHeight
            );
        }
        PacketDistributor.sendToPlayer(player, pkt);
    }

    // ------------------------------------------------------------------
    // Client-side handler
    // ------------------------------------------------------------------

    /** Client-side handler: updates {@link BoundingBoxClientState}. */
    public static void handle(SyncBBSessionPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> BoundingBoxClientState.update(payload));
    }
}
