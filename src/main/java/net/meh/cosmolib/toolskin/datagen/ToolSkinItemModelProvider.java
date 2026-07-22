package net.meh.cosmolib.toolskin.datagen;

import net.meh.cosmolib.toolskin.ToolSkinItem;
import net.meh.cosmolib.toolskin.ToolSkinRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Data generator that produces the single-layer item model JSON for every
 * {@link ToolSkinItem} registered in {@link ToolSkinRegistry}.
 *
 * <p>Each generated model uses {@code minecraft:item/generated} as its parent and
 * defines a single texture layer pointing to the token texture (e.g. easter_axe_token).
 *
 * <p>Registering this provider in your mod's data generation run is the recommended
 * way to supply item models for all registered tool skins.  Example:
 * <pre>{@code
 * // In your GatherDataEvent handler:
 * event.createProvider((output, lookupProvider) ->
 *     new ToolSkinItemModelProvider(output, existingFileHelper));
 * }</pre>
 *
 * <p>This provider uses {@code "cosmolib"} as the mod ID for the data output so
 * that the generated JSONs land in the cosmolib namespace.  Dependent mods that
 * register their own tool skins and want to generate models into their own namespace
 * should subclass this provider and override {@link #getModid()}.
 *
 * <p><b>Important</b>: the token texture must exist in your mod's assets at
 * {@code assets/<namespace>/textures/item/tool_skins/<name>_token.png}.
 */
public class ToolSkinItemModelProvider extends ItemModelProvider {

    public ToolSkinItemModelProvider(PackOutput output, String modid,
                                     ExistingFileHelper existingFileHelper) {
        super(output, modid, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (ToolSkinItem skinItem : ToolSkinRegistry.getAll()) {
            ResourceLocation itemId  = BuiltInRegistries.ITEM.getKey(skinItem);
            ResourceLocation texture = skinItem.getSkinTexture();

            // Token items use a single layer pointing to the _token texture variant
            // e.g. "cosmolib:tool_skins/easter_axe" -> "cosmolib:tool_skins/easter_axe_token"
            String tokenTexturePath = texture.getPath() + "_token";

            withExistingParent(itemId.getPath(), "minecraft:item/generated")
                    .texture("layer0", texture.getNamespace() + ":" + tokenTexturePath);
        }
    }
}
