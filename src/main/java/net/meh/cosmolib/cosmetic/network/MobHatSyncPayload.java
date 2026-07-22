package net.meh.cosmolib.cosmetic.network;

import net.meh.cosmolib.cosmetic.client.MobHatClientCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S → C: tells a client which cosmetic hat a specific mob entity is wearing.
 * Sent when the mob spawns (to nearby clients) and when a player starts
 * tracking the entity (on chunk load / entity tracking range entry).
 */
public record MobHatSyncPayload(int entityId, ItemStack hat)
        implements CustomPacketPayload {

    public static final Type<MobHatSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("cosmolib", "mob_hat_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobHatSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,     MobHatSyncPayload::entityId,
                    ItemStack.STREAM_CODEC,    MobHatSyncPayload::hat,
                    MobHatSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(MobHatSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> MobHatClientCache.set(payload.entityId(), payload.hat()));
    }
}
