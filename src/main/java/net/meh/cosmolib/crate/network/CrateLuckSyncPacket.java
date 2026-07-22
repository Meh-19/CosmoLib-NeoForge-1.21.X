package net.meh.cosmolib.crate.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S → C: syncs the player's current crate luck bonus to their client.
 *
 * <p>Sent whenever the server updates the player's luck attachment so the
 * client can display and use the current value.
 */
public record CrateLuckSyncPacket(float luckBonus) implements CustomPacketPayload {

    public static final Type<CrateLuckSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("cosmolib", "crate_luck_sync"));

    public static final StreamCodec<FriendlyByteBuf, CrateLuckSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, CrateLuckSyncPacket::luckBonus,
                    CrateLuckSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    @OnlyIn(Dist.CLIENT)
    public static void handle(CrateLuckSyncPacket payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> net.meh.cosmolib.crate.CrateLuckAttachment.setClientLuck(payload.luckBonus()));
    }
}
