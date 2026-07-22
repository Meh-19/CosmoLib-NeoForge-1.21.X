package net.meh.cosmolib.crate.network;

import net.meh.cosmolib.crate.client.CrateCameraController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S → C: tells the client to release the crate camera lock.
 *
 * <p>Sent by the server when the crate interaction ends — either because the player
 * collected rewards, timed out, or moved too far away. The client releases the
 * camera and removes the movement-speed penalty.
 */
public record CrateUnlockPacket() implements CustomPacketPayload {

    public static final Type<CrateUnlockPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("cosmolib", "crate_unlock"));

    public static final StreamCodec<FriendlyByteBuf, CrateUnlockPacket> STREAM_CODEC =
            StreamCodec.of((buf, pkt) -> {}, buf -> new CrateUnlockPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    @OnlyIn(Dist.CLIENT)
    public static void handle(CrateUnlockPacket payload, IPayloadContext ctx) {
        ctx.enqueueWork(CrateCameraController::onCrateUnlock);
    }
}
