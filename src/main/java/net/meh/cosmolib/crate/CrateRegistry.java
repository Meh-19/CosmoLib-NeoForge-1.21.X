package net.meh.cosmolib.crate;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central registry for all {@link CrateType} instances.
 *
 * <p>Dependent mods register their crate types during mod initialisation:
 * <pre>{@code
 * CrateRegistry.register(CrateType.builder()
 *     .id(ResourceLocation.fromNamespaceAndPath("mymod", "treasure_crate"))
 *     // ... other fields ...
 *     .build());
 * }</pre>
 *
 * <p>Registration should happen before {@code FMLCommonSetupEvent} (e.g. during the mod
 * constructor or early lifecycle) so that the entity type and renderer can find the crate
 * type by the time the world loads.
 */
public final class CrateRegistry {

    private static final Map<ResourceLocation, CrateType> REGISTRY = new LinkedHashMap<>();

    private CrateRegistry() {}

    /**
     * Registers a {@link CrateType}. Throws if a crate with the same ID is already registered.
     *
     * @param type the crate type to register
     * @throws IllegalStateException if the ID is already taken
     */
    public static void register(CrateType type) {
        if (REGISTRY.containsKey(type.getId())) {
            throw new IllegalStateException(
                    "[CosmoLib] Duplicate CrateType registration: " + type.getId());
        }
        REGISTRY.put(type.getId(), type);
    }

    /**
     * Returns the {@link CrateType} for the given ID.
     *
     * @param id the crate type's registry ID
     * @return the registered crate type
     * @throws IllegalArgumentException if no crate type is registered under this ID
     */
    public static CrateType get(ResourceLocation id) {
        CrateType type = REGISTRY.get(id);
        if (type == null) {
            throw new IllegalArgumentException(
                    "[CosmoLib] Unknown CrateType: " + id + ". Was it registered before world load?");
        }
        return type;
    }

    /**
     * Returns an unmodifiable view of all registered crate types.
     *
     * @return all registered crate types in registration order
     */
    public static Collection<CrateType> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }
}
