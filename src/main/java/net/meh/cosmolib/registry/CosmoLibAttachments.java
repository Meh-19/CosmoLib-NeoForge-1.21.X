package net.meh.cosmolib.registry;

import net.meh.cosmolib.cosmetic.PlayerCosmeticsData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

import static net.meh.cosmolib.CosmoLib.MOD_ID;

public final class CosmoLibAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MOD_ID);

    /**
     * Per-player cosmetic loadout attachment.
     * Persistent (survives restarts) and copied on death (cosmetics don't drop).
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerCosmeticsData>>
            COSMETICS = ATTACHMENT_TYPES.register("cosmetics",
            () -> AttachmentType.builder(PlayerCosmeticsData::new)
                    .serialize(PlayerCosmeticsData.CODEC)
                    .copyOnDeath()
                    .build());

    private CosmoLibAttachments() {}
}
