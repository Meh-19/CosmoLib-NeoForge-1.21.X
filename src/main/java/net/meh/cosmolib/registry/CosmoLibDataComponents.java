package net.meh.cosmolib.registry;

import net.meh.cosmolib.toolskin.ToolSkinData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.meh.cosmolib.CosmoLib.MOD_ID;

/**
 * Data component types registered by CosmoLib.
 *
 * <p>Register the {@link DeferredRegister} on the mod event bus in {@code CosmoLib}:
 * <pre>{@code
 * CosmoLibDataComponents.DATA_COMPONENTS.register(modEventBus);
 * }</pre>
 */
public final class CosmoLibDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MOD_ID);

    /**
     * Attached to any tool item that has had a {@link net.meh.cosmolib.toolskin.ToolSkinItem}
     * applied at the smithing table.  Carries the skin texture, set name, rarity,
     * tool type, optional exclusive tag, and the registry key of the originating
     * {@link net.meh.cosmolib.toolskin.ToolSkinItem} (used to return the token on removal).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ToolSkinData>>
            TOOL_SKIN = DATA_COMPONENTS.register("tool_skin",
            () -> DataComponentType.<ToolSkinData>builder()
                    .persistent(ToolSkinData.CODEC)
                    .networkSynchronized(ToolSkinData.STREAM_CODEC)
                    .build());

    private CosmoLibDataComponents() {}
}
