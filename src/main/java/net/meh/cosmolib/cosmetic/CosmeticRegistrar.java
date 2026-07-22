package net.meh.cosmolib.cosmetic;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder-style helper that collapses all cosmetic registration for a mod
 * into a single static field and one {@link #register} call.
 *
 * <h3>Usage (in your mod class)</h3>
 * <pre>{@code
 * public static final CosmeticRegistrar COSMETICS = CosmeticRegistrar.create(MOD_ID);
 *
 * public static final DeferredItem<CosmeticItem> MY_HAT =
 *     COSMETICS.add("my_hat", CosmeticSlot.HAT, CosmeticRarity.COMMON, false);
 *
 * public static final DeferredItem<CosmeticItem> MY_ROBE =
 *     COSMETICS.add("my_robe", CosmeticSlot.BACK, CosmeticRarity.LIMITED, true,
 *         CosmeticDefault.color(PaintColor.LIGHT_BLUE, 5));
 *
 * // In your mod constructor:
 * public MyMod(IEventBus bus) {
 *     COSMETICS.register(bus);
 * }
 * }</pre>
 *
 * <p>Each added cosmetic is automatically:
 * <ul>
 *   <li>Registered to the item registry (via an internal {@link DeferredRegister}).</li>
 *   <li>Registered with {@link CosmeticRegistry} so it appears in the cosmetic wardrobe
 *       screen and the CosmoLib cosmetics creative tab.</li>
 *   <li>Auto-picked up by {@link net.meh.cosmolib.event.ClientEventHandler} for
 *       paint-colour tinting if {@code paintable} is {@code true}.</li>
 * </ul>
 *
 * <p>This class does <em>not</em> replace a mod's existing {@link DeferredRegister} —
 * it creates its own internal one for cosmetics only.  Any items that aren't cosmetics
 * (e.g. tools, blocks) still need their own register.
 */
public final class CosmeticRegistrar {

    private final DeferredRegister.Items items;
    private final String modId;
    private final List<PendingEntry> pending = new ArrayList<>();

    private record PendingEntry(DeferredItem<CosmeticItem> item, boolean showInTab) {}

    private CosmeticRegistrar(String modId) {
        this.items = DeferredRegister.createItems(modId);
        this.modId = modId;
    }

    /**
     * Creates a new registrar for the given mod ID.
     *
     * @param modId the mod's namespace (e.g. {@code "mymod"})
     */
    public static CosmeticRegistrar create(String modId) {
        return new CosmeticRegistrar(modId);
    }

    // ------------------------------------------------------------------
    // add — visible in cosmetics tab
    // ------------------------------------------------------------------

    /**
     * Registers a cosmetic that appears in the cosmetics creative tab.
     * No default paint colour — renders white until painted.
     *
     * @param name      registry name, e.g. {@code "my_hat"}
     * @param slot      which equipment slot this cosmetic occupies
     * @param rarity    display rarity (controls name colour and rarity symbol)
     * @param paintable whether this cosmetic can be painted and has a paint tooltip
     * @return a {@link DeferredItem} handle for the registered item
     */
    public DeferredItem<CosmeticItem> add(String name, CosmeticSlot slot,
                                          CosmeticRarity rarity, boolean paintable) {
        return addEntry(name, slot, rarity, paintable, null, true);
    }

    /**
     * Registers a cosmetic with a baked-in default appearance that appears
     * on every stack regardless of how it was obtained.
     *
     * @param name              registry name
     * @param slot              which equipment slot this cosmetic occupies
     * @param rarity            display rarity
     * @param paintable         whether this cosmetic can be painted
     * @param defaultAppearance pre-baked colour or finish; use
     *                          {@link CosmeticDefault#color} or
     *                          {@link CosmeticDefault#finish}, or pass
     *                          {@code null} for no default
     * @return a {@link DeferredItem} handle for the registered item
     */
    public DeferredItem<CosmeticItem> add(String name, CosmeticSlot slot,
                                          CosmeticRarity rarity, boolean paintable,
                                          @Nullable CosmeticDefault defaultAppearance) {
        return addEntry(name, slot, rarity, paintable, defaultAppearance, true);
    }

    // ------------------------------------------------------------------
    // addHidden — registered but not shown in the cosmetics creative tab
    // ------------------------------------------------------------------

    /**
     * Registers a cosmetic that is <em>not</em> shown in the cosmetics creative tab.
     * Useful for cosmetics that are obtained through other means (loot, quests, etc.)
     * and should not be freely grabbable in creative mode.
     *
     * @param name      registry name
     * @param slot      which equipment slot this cosmetic occupies
     * @param rarity    display rarity
     * @param paintable whether this cosmetic can be painted
     * @return a {@link DeferredItem} handle for the registered item
     */
    public DeferredItem<CosmeticItem> addHidden(String name, CosmeticSlot slot,
                                                CosmeticRarity rarity, boolean paintable) {
        return addEntry(name, slot, rarity, paintable, null, false);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private DeferredItem<CosmeticItem> addEntry(String name, CosmeticSlot slot,
                                                CosmeticRarity rarity, boolean paintable,
                                                @Nullable CosmeticDefault def, boolean showInTab) {
        ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(modId, "cosmetics/" + name);
        var entry = items.register(name,
                () -> new CosmeticItem(slot, rarity, paintable, def, modelId, new Item.Properties()));
        pending.add(new PendingEntry(entry, showInTab));
        return entry;
    }

    /**
     * Subscribes the internal item register and a setup listener to the mod event bus.
     * Call this once from your mod constructor.
     *
     * @param bus your mod's {@link IEventBus}
     */
    public void register(IEventBus bus) {
        items.register(bus);
        bus.addListener(this::onSetup);
    }

    private void onSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            for (PendingEntry e : pending) {
                CosmeticRegistry.register(e.item().get(), e.showInTab());
            }
        });
    }
}
