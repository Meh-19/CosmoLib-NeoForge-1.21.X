package net.meh.cosmolib.furniture.tool.network;

import net.meh.cosmolib.furniture.tool.BoundingBoxSelectorItem;
import net.meh.cosmolib.furniture.tool.BoundingBoxSelectorSession;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C→S: signals that the developer pressed the undo keybind while a
 * {@link BoundingBoxSelectorItem} session is active.
 *
 * <p>The server pops the last selection snapshot from
 * {@link BoundingBoxSelectorSession#undoStack} and broadcasts the new state
 * back to the client via {@link SyncBBSessionPayload}.
 */
public record UndoSelectionPayload() implements CustomPacketPayload {

    public static final Type<UndoSelectionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("cosmolib", "bb_undo"));

    public static final StreamCodec<FriendlyByteBuf, UndoSelectionPayload> STREAM_CODEC =
            StreamCodec.unit(new UndoSelectionPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Server-side handler: pops undo stack and re-syncs to the client. */
    public static void handle(UndoSelectionPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            BoundingBoxSelectorSession session = BoundingBoxSelectorSession.get(sp.getStringUUID());
            if (session == null) return;

            if (session.undo()) {
                sp.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                "§eUndo — " + session.selectedPositions.size() + " selected"),
                        true);
            } else {
                sp.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§cNothing to undo."),
                        true);
            }
            // Write crash-recovery temp file after undo
            BoundingBoxSelectorItem.writeTempFile(sp, session);
            // Sync updated state to client
            SyncBBSessionPayload.sendTo(sp, session);
        });
    }
}
