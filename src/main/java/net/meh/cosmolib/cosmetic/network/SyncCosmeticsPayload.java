package net.meh.cosmolib.cosmetic.network;

import net.meh.cosmolib.cosmetic.CosmeticSlot;
import net.meh.cosmolib.cosmetic.client.CosmeticClientCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** S→C: broadcasts one player's full cosmetic loadout. */
public record SyncCosmeticsPayload(
        UUID playerUuid,
        Map<CosmeticSlot, ItemStack> cosmetics
) implements CustomPacketPayload {

    public static final Type<SyncCosmeticsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("cosmolib", "sync_cosmetics"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCosmeticsPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public SyncCosmeticsPayload decode(RegistryFriendlyByteBuf buf) {
                    UUID uuid = buf.readUUID();
                    int  size = buf.readVarInt();
                    Map<CosmeticSlot, ItemStack> map = new EnumMap<>(CosmeticSlot.class);
                    for (int i = 0; i < size; i++) {
                        CosmeticSlot slot  = CosmeticSlot.values()[buf.readVarInt()];
                        ItemStack    stack = ItemStack.STREAM_CODEC.decode(buf);
                        if (!stack.isEmpty()) map.put(slot, stack);
                    }
                    return new SyncCosmeticsPayload(uuid, map);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, SyncCosmeticsPayload value) {
                    buf.writeUUID(value.playerUuid());
                    Map<CosmeticSlot, ItemStack> map = value.cosmetics();
                    buf.writeVarInt(map.size());
                    map.forEach((slot, stack) -> {
                        buf.writeVarInt(slot.ordinal());
                        ItemStack.STREAM_CODEC.encode(buf, stack);
                    });
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SyncCosmeticsPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CosmeticClientCache.update(payload.playerUuid(), payload.cosmetics()));
    }
}
