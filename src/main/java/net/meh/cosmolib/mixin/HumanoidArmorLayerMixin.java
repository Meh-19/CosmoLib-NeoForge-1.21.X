package net.meh.cosmolib.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.meh.cosmolib.cosmetic.CosmeticSlot;
import net.meh.cosmolib.cosmetic.client.CosmeticClientCache;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels the vanilla helmet render when the player has a HAT cosmetic equipped,
 * preventing z-fighting between the cosmetic model and the armor layer.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {

    @Inject(
            method = "renderArmorPiece",
            at = @At("HEAD"),
            cancellable = true
    )
    private <T extends LivingEntity, A extends HumanoidModel<T>> void cosmolib$suppressHelmet(
            PoseStack poseStack, MultiBufferSource bufferSource,
            T entity, EquipmentSlot slot,
            int packedLight, A model,
            CallbackInfo ci
    ) {
        if (slot == EquipmentSlot.HEAD
                && entity instanceof Player
                && CosmeticClientCache.hasCosmetic(entity, CosmeticSlot.HAT)) {
            ci.cancel();
        }
    }
}
