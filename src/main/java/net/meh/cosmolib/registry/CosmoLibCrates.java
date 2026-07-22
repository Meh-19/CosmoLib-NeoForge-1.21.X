package net.meh.cosmolib.registry;

import net.meh.cosmolib.crate.CrateRegistry;
import net.meh.cosmolib.crate.CrateType;
import net.minecraft.resources.ResourceLocation;

import static net.meh.cosmolib.CosmoLib.MOD_ID;

/**
 * Registry constants for CosmoLib's built-in crate types.
 *
 * <p>Dependent mods that want to add their own crates should call
 * {@link CrateRegistry#register} with a {@link CrateType} built via
 * {@link CrateType#builder()} — they do not need to touch this class.
 *
 * <p>Asset paths follow the convention:
 * <ul>
 *   <li>Geo model:   {@code assets/<ns>/geo/entity/<name>.geo.json}</li>
 *   <li>Texture:     {@code assets/<ns>/textures/entity/crates/<name>.png}</li>
 *   <li>Animations:  {@code assets/<ns>/animations/entity/crate.animation.json}</li>
 *   <li>Loot table:  {@code data/<ns>/loot_tables/crates/<name>.json}</li>
 * </ul>
 */
public final class CosmoLibCrates {

    /**
     * The default cosmetic crate — awards cosmetic items from the
     * {@code cosmolib:cosmetic_crate} loot table.
     */
    public static final CrateType COSMETIC_CRATE = CrateType.builder()
            .id(rl("cosmetic_crate"))
            .lootTable(rl("cosmetic_crate"))
            .model(rl("geo/entity/cosmetic_crate.geo.json"))
            .texture(rl("textures/entity/crates/cosmetic_crate.png"))
            .animations(rl("animations/entity/crate.animation.json"))
            .rerolls(3)
            .item(() -> CosmoLibItems.COSMETIC_CRATE.get())
            .build();

    /**
     * Registers all built-in CosmoLib crates into {@link CrateRegistry}.
     * Called from the {@code CosmoLib} mod constructor so crate types are
     * available before {@code FMLCommonSetupEvent}.
     */
    public static void registerAll() {
        CrateRegistry.register(COSMETIC_CRATE);
    }

    private CosmoLibCrates() {}

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
