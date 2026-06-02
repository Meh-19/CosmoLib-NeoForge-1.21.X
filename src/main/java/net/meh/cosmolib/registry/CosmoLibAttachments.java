package net.meh.cosmolib.registry;

import com.mojang.serialization.Codec;
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

    /**
     * Per-player crate luck bonus (loyalty system).
     * Persistent, NOT copied on death (losing everything resets luck intentionally).
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Float>>
            CRATE_LUCK = ATTACHMENT_TYPES.register("crate_luck",
            () -> AttachmentType.builder(() -> 0.0f)
                    .serialize(Codec.FLOAT)
                    .build());

    private CosmoLibAttachments() {}
}
