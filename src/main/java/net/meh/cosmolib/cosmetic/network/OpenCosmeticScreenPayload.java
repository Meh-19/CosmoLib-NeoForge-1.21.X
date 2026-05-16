package net.meh.cosmolib.cosmetic.network;

import net.meh.cosmolib.cosmetic.screen.CosmeticMenu;
import net.meh.cosmolib.registry.CosmoLibMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C→S: client asks the server to open the cosmetic screen. */
public record OpenCosmeticScreenPayload() implements CustomPacketPayload {

    public static final Type<OpenCosmeticScreenPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("cosmolib", "open_cosmetic_screen"));

    public static final StreamCodec<FriendlyByteBuf, OpenCosmeticScreenPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenCosmeticScreenPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(OpenCosmeticScreenPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                sp.openMenu(
                        new net.minecraft.world.SimpleMenuProvider(
                                (id, inv, p) -> new CosmeticMenu(id, inv),
                                Component.translatable("container.cosmolib.cosmetics")
                        )
                );
            }
        });
    }
}
