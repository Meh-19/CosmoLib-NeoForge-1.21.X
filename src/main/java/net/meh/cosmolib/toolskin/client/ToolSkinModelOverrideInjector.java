package net.meh.cosmolib.toolskin.client;

import net.meh.cosmolib.CosmoLib;
import net.meh.cosmolib.toolskin.ToolSkinItem;
import net.meh.cosmolib.toolskin.ToolSkinRegistry;
import net.meh.cosmolib.toolskin.ToolSkinType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Automatically injects CustomModelData overrides for all registered tool skins.
 * Mod authors only need to register their ToolSkinItem - overrides are added automatically.
 */
@EventBusSubscriber(modid = CosmoLib.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ToolSkinModelOverrideInjector {

    /** Builds the applied model path, respecting the optional modelFolder. */
    private static String appliedModelPath(ToolSkinItem skinItem) {
        String skinSetName = skinItem.getSkinSetName().toLowerCase().replace(" ", "_");
        String toolType    = skinItem.getSkinType().name().toLowerCase();
        String folder      = skinItem.getModelFolder() != null ? skinItem.getModelFolder() : "item";
        return folder + "/" + skinSetName + "_" + toolType + "_applied";
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        // Register all skin applied models so they're baked
        for (ToolSkinItem skinItem : ToolSkinRegistry.getAll()) {
            ResourceLocation modelLocation = ResourceLocation.fromNamespaceAndPath(
                    skinItem.getSkinTexture().getNamespace(),
                    appliedModelPath(skinItem)
            );
            event.register(ModelResourceLocation.standalone(modelLocation));
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();

        // Build a map of tool type -> skin CustomModelData values
        Map<ToolSkinType, Map<Integer, ResourceLocation>> skinsByType = new HashMap<>();
        for (ToolSkinItem skinItem : ToolSkinRegistry.getAll()) {
            int customModelData = skinItem.getSkinSetName().hashCode() & 0x7FFFFFFF;

            ResourceLocation appliedModel = ResourceLocation.fromNamespaceAndPath(
                    skinItem.getSkinTexture().getNamespace(),
                    appliedModelPath(skinItem)
            );

            skinsByType.computeIfAbsent(skinItem.getSkinType(), k -> new HashMap<>())
                       .put(customModelData, appliedModel);
        }

        // Inject overrides into all tool models
        for (Map.Entry<ModelResourceLocation, BakedModel> entry : new ArrayList<>(models.entrySet())) {
            ModelResourceLocation mrl = entry.getKey();
            Item item = BuiltInRegistries.ITEM.getOptional(mrl.id()).orElse(null);

            if (item == null) continue;

            ToolSkinType.fromItem(item).ifPresent(toolType -> {
                Map<Integer, ResourceLocation> skins = skinsByType.get(toolType);
                if (skins == null || skins.isEmpty()) return;

                BakedModel originalModel = entry.getValue();
                BakedModel wrappedModel = new ToolSkinModelWrapper(originalModel, skins, models);
                models.put(mrl, wrappedModel);
            });
        }
    }
}
