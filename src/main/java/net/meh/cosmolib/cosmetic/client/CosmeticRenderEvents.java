package net.meh.cosmolib.cosmetic.client;

import net.meh.cosmolib.CosmoLib;
import net.meh.cosmolib.cosmetic.CosmeticItem;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Client mod-bus events that support the cosmetic token rendering system.
 *
 * <p>The 3-D cosmetic models (e.g. {@code cosmolib:cosmetics/cosmo_cane}) are no
 * longer referenced by item model JSONs (which now point to the flat token sprites),
 * so they must be explicitly registered here so the model manager loads them.
 *
 * <p>Token model JSONs are referenced directly by the item model JSONs and therefore
 * loaded automatically — they do not need to be registered here.z`
 */
@EventBusSubscriber(modid = CosmoLib.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CosmeticRenderEvents {

    private CosmeticRenderEvents() {}

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        // Register every CosmeticItem's 3-D model so it is available for
        // the mannequin renderer even though no item model JSON references it.
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof CosmeticItem ci) {
                ResourceLocation modelId = ci.getCosmeticModelId();
                if (modelId != null) {
                    event.register(new ModelResourceLocation(modelId, "standalone"));
                }
            }
        }
    }
}
