package net.meh.cosmolib.furniture.client.model;

import net.meh.cosmolib.furniture.blockentity.AnimatedFurnitureBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model that resolves all file paths from the block's registry ID.
 *
 * Given a block registered as {@code mymod:my_clock} the loader expects:
 *   geo:        assets/mymod/geo/block/my_clock.geo.json
 *   texture:    assets/mymod/textures/furniture/my_clock.png
 *   animation:  assets/mymod/animations/block/my_clock.animation.json
 */
public class IdBasedFurnitureModel<T extends AnimatedFurnitureBlockEntity> extends GeoModel<T> {

    @Override
    public ResourceLocation getModelResource(T animatable) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(animatable.getBlockState().getBlock());
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                "geo/block/" + id.getPath() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(animatable.getBlockState().getBlock());
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                "textures/furniture/" + id.getPath() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(animatable.getBlockState().getBlock());
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                "animations/block/" + id.getPath() + ".animation.json");
    }
}
