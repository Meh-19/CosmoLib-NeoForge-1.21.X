package net.meh.cosmolib.cosmetic.network;

import net.meh.cosmolib.cosmetic.CosmeticManager;
import net.meh.cosmolib.cosmetic.CosmeticSlot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C→S: player equips or unequips a cosmetic. Empty stack = unequip. */
public record EquipCosmeticPayload(
        CosmeticSlot slot,
        ItemStack    stack
) implements CustomPacketPayload {

    public static final Type<EquipCosmeticPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("cosmolib", "equip_cosmetic"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EquipCosmeticPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public EquipCosmeticPayload decode(RegistryFriendlyByteBuf buf) {
                    CosmeticSlot slot  = CosmeticSlot.values()[buf.readVarInt()];
                    ItemStack    stack = ItemStack.STREAM_CODEC.decode(buf);
                    return new EquipCosmeticPayload(slot, stack);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, EquipCosmeticPayload value) {
                    buf.writeVarInt(value.slot().ordinal());
                    ItemStack.STREAM_CODEC.encode(buf, value.stack());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(EquipCosmeticPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                CosmeticManager.applyEquip(sp, payload.slot(), payload.stack());
            }
        });
    }
}
