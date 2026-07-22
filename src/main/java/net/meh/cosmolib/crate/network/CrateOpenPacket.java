package net.meh.cosmolib.crate.network;

import net.meh.cosmolib.crate.client.CrateCameraController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S → C: tells the client to lock the camera onto the crate entity being opened.
 *
 * <p>Sent by the server immediately when a player begins opening a crate
 * (state transitions to OPENING). The client passes the entity ID to
 * {@link CrateCameraController} to begin the camera lock and panning effect.
 */
public record CrateOpenPacket(int entityId) implements CustomPacketPayload {

    public static final Type<CrateOpenPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("cosmolib", "crate_open"));

    public static final StreamCodec<FriendlyByteBuf, CrateOpenPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CrateOpenPacket::entityId,
                    CrateOpenPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    @OnlyIn(Dist.CLIENT)
    public static void handle(CrateOpenPacket payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CrateCameraController.onCrateOpen(payload.entityId()));
    }
}
