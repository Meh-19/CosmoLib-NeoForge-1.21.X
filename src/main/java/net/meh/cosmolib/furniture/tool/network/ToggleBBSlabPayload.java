package net.meh.cosmolib.furniture.tool.network;

import net.meh.cosmolib.furniture.tool.BBSelectionMode;
import net.meh.cosmolib.furniture.tool.BoundingBoxSelectorSession;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C→S: cycles the BB Selector hitbox mode for the active session.
 *
 * <p>Each press of the BB_SLAB keybind (Left Alt by default) advances the mode:
 * {@code FULL → SLAB → SEAT → FULL}.
 *
 * <ul>
 *   <li>{@link BBSelectionMode#FULL} — normal full-block collision (default).</li>
 *   <li>{@link BBSelectionMode#SLAB} — half-height (8/16) collision.</li>
 *   <li>{@link BBSelectionMode#SEAT} — sittable position; right-click mounts the player.</li>
 * </ul>
 *
 * <p>Bound to {@link net.meh.cosmolib.event.ClientEventHandler#BB_SLAB} and polled in
 * {@link net.meh.cosmolib.event.ClientGameEventHandler}.
 */
public record ToggleBBSlabPayload() implements CustomPacketPayload {

    public static final Type<ToggleBBSlabPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("cosmolib", "bb_toggle_slab"));

    public static final StreamCodec<FriendlyByteBuf, ToggleBBSlabPayload> STREAM_CODEC =
            StreamCodec.unit(new ToggleBBSlabPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Server-side handler: advances the selection mode and re-syncs the session. */
    public static void handle(ToggleBBSlabPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            BoundingBoxSelectorSession session = BoundingBoxSelectorSession.get(sp.getStringUUID());
            if (session == null) return;

            session.selectionMode = session.selectionMode.next();
            String msg = switch (session.selectionMode) {
                case FULL -> "§7□ Full mode §7— standard block collision";
                case SLAB -> "§e◧ Slab mode §7— half-height (8/16) collision";
                case SEAT -> "§d⊕ Seat mode §7— sittable, height §f" + Math.round(session.furnitureSeatHeight * 16) + "/16";
            };
            sp.displayClientMessage(Component.literal(msg), true);
            SyncBBSessionPayload.sendTo(sp, session);
        });
    }
}
