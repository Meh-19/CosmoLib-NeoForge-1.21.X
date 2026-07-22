package net.meh.cosmolib.furniture;

import net.meh.cosmolib.furniture.block.*;
import net.meh.cosmolib.furniture.item.FurnitureBlockItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder-style helper that collapses all furniture block + item registration for a mod
 * into a single static field and one {@link #register} call.
 *
 * <h3>Usage (in your mod class)</h3>
 * <pre>{@code
 * public static final FurnitureRegistrar FURNITURE = FurnitureRegistrar.create(MOD_ID);
 *
 * // Floor decoration (non-sittable)
 * public static final FurnitureEntry MY_VASE =
 *     FURNITURE.decoration("my_vase", BlockBehaviour.Properties.of().strength(1.5f).noOcclusion());
 *
 * // Sittable chair with default shape and options
 * public static final FurnitureEntry MY_CHAIR =
 *     FURNITURE.sittable("my_chair", BlockBehaviour.Properties.of().strength(2f));
 *
 * // Wall-mounted shelf with a specific shape
 * public static final FurnitureEntry MY_SHELF =
 *     FURNITURE.wall("my_shelf", BlockBehaviour.Properties.of().strength(1.5f), FurnitureShape.SHELF);
 *
 * // Animated clock
 * public static final FurnitureEntry MY_CLOCK =
 *     FURNITURE.animated("my_clock", BlockBehaviour.Properties.of().strength(2f), "clock_ticking");
 *
 * // In your mod constructor:
 * public MyMod(IEventBus bus) {
 *     FURNITURE.register(bus);
 * }
 * }</pre>
 *
 * <h3>What is registered automatically</h3>
 * <ul>
 *   <li>A block (the appropriate {@link AbstractFurnitureBlock} subclass).</li>
 *   <li>A {@link FurnitureBlockItem} block-item with the same name.</li>
 *   <li>For animated entries: the looping animation name is registered via
 *       {@link AnimatedFurnitureBlock#registerAnimation} during
 *       {@link FMLCommonSetupEvent}.</li>
 * </ul>
 *
 * <h3>What you still need to provide</h3>
 * <ul>
 *   <li>{@code assets/<modid>/geo/block/<name>.geo.json} — GeckoLib model.</li>
 *   <li>{@code assets/<modid>/textures/block/<name>.png} — texture atlas.</li>
 *   <li>{@code assets/<modid>/animations/block/<name>.animation.json} — for animated entries.</li>
 *   <li>A standard block state JSON and a simple item model that references the block model.</li>
 * </ul>
 *
 * <p>Block entity types and renderers do <em>not</em> need to be registered manually —
 * all entries use CosmoLib's shared {@code FURNITURE_ENTITY} /
 * {@code ANIMATED_FURNITURE_ENTITY} block entity types, whose renderers are already
 * registered globally by {@link net.meh.cosmolib.event.ClientEventHandler}.
 */
public final class FurnitureRegistrar {

    private final String modId;
    private final DeferredRegister.Blocks blocks;
    private final DeferredRegister.Items  items;
    private final List<AnimEntry> animEntries = new ArrayList<>();

    /** Captures the animation name for an animated block registered through this registrar. */
    private record AnimEntry(String blockName, String animationName) {}

    private FurnitureRegistrar(String modId) {
        this.modId   = modId;
        this.blocks  = DeferredRegister.createBlocks(modId);
        this.items   = DeferredRegister.createItems(modId);
    }

    /**
     * Creates a new registrar for the given mod ID.
     *
     * @param modId the mod's namespace (e.g. {@code "mymod"})
     */
    public static FurnitureRegistrar create(String modId) {
        return new FurnitureRegistrar(modId);
    }

    // ------------------------------------------------------------------
    // decoration — floor / ceiling placed, non-sittable
    // ------------------------------------------------------------------

    /**
     * Registers a {@link DecorationBlock} with the default {@link FurnitureShape#FLAT} shape
     * and no special options.
     *
     * @param name  registry name, e.g. {@code "my_vase"}
     * @param props block behaviour properties (strength, sound type, etc.)
     * @return a {@link FurnitureEntry} containing the block and item handles
     */
    public FurnitureEntry decoration(String name, BlockBehaviour.Properties props) {
        return wrap(name, blocks.register(name, () -> new DecorationBlock(props)));
    }

    /**
     * Registers a {@link DecorationBlock} with an explicit hitbox shape.
     *
     * @param name  registry name
     * @param props block behaviour properties
     * @param shape one of the {@link FurnitureShape} presets, or {@link FurnitureShape#of(net.minecraft.world.phys.shapes.VoxelShape)}
     * @return a {@link FurnitureEntry} containing the block and item handles
     */
    public FurnitureEntry decoration(String name, BlockBehaviour.Properties props,
                                     FurnitureShape shape) {
        return wrap(name, blocks.register(name, () -> new DecorationBlock(props, shape)));
    }

    /**
     * Registers a {@link DecorationBlock} with an explicit shape and full option set.
     *
     * @param name  registry name
     * @param props block behaviour properties
     * @param shape hitbox shape preset
     * @param opts  {@link FurnitureOptions} — chain flags like {@code .paintable()},
     *              {@code .waterloggable()}, {@code .defaultColor(…)}, etc.
     * @return a {@link FurnitureEntry} containing the block and item handles
     */
    public FurnitureEntry decoration(String name, BlockBehaviour.Properties props,
                                     FurnitureShape shape, FurnitureOptions opts) {
        return wrap(name, blocks.register(name, () -> new DecorationBlock(props, shape, opts)));
    }

    // ------------------------------------------------------------------
    // sittable — the player can right-click to sit
    // ------------------------------------------------------------------

    /**
     * Registers a {@link SittableBlock} with the default {@link FurnitureShape#FULL} shape
     * and sit height at level 5 (≈0.3 blocks above the floor).
     *
     * @param name  registry name, e.g. {@code "my_chair"}
     * @param props block behaviour properties
     * @return a {@link FurnitureEntry} containing the block and item handles
     */
    public FurnitureEntry sittable(String name, BlockBehaviour.Properties props) {
        return wrap(name, blocks.register(name, () -> new SittableBlock(props)));
    }

    /**
     * Registers a {@link SittableBlock} with explicit options.
     * Use {@link FurnitureOptions#seatHeight(int)} to override the sit height.
     *
     * @param name  registry name
     * @param props block behaviour properties
     * @param opts  furniture options; chain {@code .seatHeight(8)} etc.
     * @return a {@link FurnitureEntry} containing the block and item handles
     */
    public FurnitureEntry sittable(String name, BlockBehaviour.Properties props,
                                   FurnitureOptions opts) {
        return wrap(name, blocks.register(name, () -> new SittableBlock(props, opts)));
    }

    /**
     * Registers a {@link SittableBlock} with an explicit shape and options.
     *
     * @param name  registry name
     * @param props block behaviour properties
     * @param shape hitbox shape preset
     * @param opts  furniture options
     * @return a {@link FurnitureEntry} containing the block and item handles
     */
    public FurnitureEntry sittable(String name, BlockBehaviour.Properties props,
                                   FurnitureShape shape, FurnitureOptions opts) {
        return wrap(name, blocks.register(name, () -> new SittableBlock(props, shape, opts)));
    }

    // ------------------------------------------------------------------
    // wall — wall-mounted furniture, rotates to face away from the wall
    // ------------------------------------------------------------------

    /**
     * Registers a {@link WallFurnitureBlock} with the default {@link FurnitureShape#FULL}
     * shape and no special options.
     *
     * @param name  registry name, e.g. {@code "my_shelf"}
     * @param props block behaviour properties
     * @return a {@link FurnitureEntry} containing the block and item handles
     */
    public FurnitureEntry wall(String name, BlockBehaviour.Properties props) {
        return wrap(name, blocks.register(name, () -> new WallFurnitureBlock(props)));
    }

    /**
     * Registers a {@link WallFurnitureBlock} with an explicit shape.
     *
     * @param name  registry name
     * @param props block behaviour properties
     * @param shape hitbox shape preset (e.g. {@link FurnitureShape#SHELF})
     * @return a {@link FurnitureEntry} containing the block and item handles
     */
    public FurnitureEntry wall(String name, BlockBehaviour.Properties props,
                               FurnitureShape shape) {
        return wrap(name, blocks.register(name, () -> new WallFurnitureBlock(props, shape)));
    }

    /**
     * Registers a {@link WallFurnitureBlock} with an explicit shape and options.
     *
     * @param name  registry name
     * @param props block behaviour properties
     * @param shape hitbox shape preset
     * @param opts  furniture options
     * @return a {@link FurnitureEntry} containing the block and item handles
     */
    public FurnitureEntry wall(String name, BlockBehaviour.Properties props,
                               FurnitureShape shape, FurnitureOptions opts) {
        return wrap(name, blocks.register(name, () -> new WallFurnitureBlock(props, shape, opts)));
    }

    // ------------------------------------------------------------------
    // animated — GeckoLib animated block
    // ------------------------------------------------------------------

    /**
     * Registers an {@link AnimatedFurnitureBlock} with default options and the given
     * animation name.  The animation is registered via
     * {@link AnimatedFurnitureBlock#registerAnimation} during
     * {@link FMLCommonSetupEvent}.
     *
     * @param name          registry name, e.g. {@code "my_clock"}
     * @param props         block behaviour properties
     * @param animationName the looping animation name as it appears in the
     *                      {@code .animation.json} file, e.g. {@code "clock_ticking"}
     * @return a {@link FurnitureEntry} containing the block and item handles
     */
    public FurnitureEntry animated(String name, BlockBehaviour.Properties props,
                                   String animationName) {
        return animated(name, props, FurnitureOptions.defaults(), animationName);
    }

    /**
     * Registers an {@link AnimatedFurnitureBlock} with explicit options and the given
     * animation name.
     *
     * @param name          registry name
     * @param props         block behaviour properties
     * @param opts          furniture options
     * @param animationName looping animation name in the {@code .animation.json} file
     * @return a {@link FurnitureEntry} containing the block and item handles
     */
    public FurnitureEntry animated(String name, BlockBehaviour.Properties props,
                                   FurnitureOptions opts, String animationName) {
        animEntries.add(new AnimEntry(name, animationName));
        return wrap(name, blocks.register(name, () -> new AnimatedFurnitureBlock(props, opts)));
    }

    // ------------------------------------------------------------------
    // register
    // ------------------------------------------------------------------

    /**
     * Subscribes the internal block and item registers — and, if any animated entries
     * were added, a {@link FMLCommonSetupEvent} listener — to the mod event bus.
     * Call this once from your mod constructor.
     *
     * @param bus your mod's {@link IEventBus}
     */
    public void register(IEventBus bus) {
        blocks.register(bus);
        items.register(bus);
        if (!animEntries.isEmpty()) {
            bus.addListener(this::onSetup);
        }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    /** Pairs a deferred block with its matching deferred block-item. */
    private <B extends AbstractFurnitureBlock> FurnitureEntry wrap(
            String name, DeferredBlock<B> block) {
        var item = items.register(name,
                () -> new FurnitureBlockItem(block.get(), new Item.Properties()));
        return new FurnitureEntry(block, item);
    }

    private void onSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            for (AnimEntry e : animEntries) {
                AnimatedFurnitureBlock.registerAnimation(
                        ResourceLocation.fromNamespaceAndPath(modId, e.blockName()),
                        e.animationName());
            }
        });
    }
}
