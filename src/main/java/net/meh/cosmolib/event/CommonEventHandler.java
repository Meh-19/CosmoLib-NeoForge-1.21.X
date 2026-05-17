package net.meh.cosmolib.event;

import net.meh.cosmolib.CosmoLib;
import net.meh.cosmolib.cosmetic.CosmeticManager;
import net.meh.cosmolib.cosmetic.network.EquipCosmeticPayload;
import net.meh.cosmolib.cosmetic.network.MobHatSyncPayload;
import net.meh.cosmolib.cosmetic.network.OpenCosmeticScreenPayload;
import net.meh.cosmolib.cosmetic.network.SyncCosmeticsPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = CosmoLib.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class CommonEventHandler {

    private CommonEventHandler() {}

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var reg = event.registrar("1");

        reg.playToClient(
                SyncCosmeticsPayload.TYPE,
                SyncCosmeticsPayload.STREAM_CODEC,
                SyncCosmeticsPayload::handle);

        reg.playToServer(
                EquipCosmeticPayload.TYPE,
                EquipCosmeticPayload.STREAM_CODEC,
                EquipCosmeticPayload::handle);

        reg.playToServer(
                OpenCosmeticScreenPayload.TYPE,
                OpenCosmeticScreenPayload.STREAM_CODEC,
                OpenCosmeticScreenPayload::handle);

        reg.playToClient(
                MobHatSyncPayload.TYPE,
                MobHatSyncPayload.STREAM_CODEC,
                MobHatSyncPayload::handle);
    }
}
