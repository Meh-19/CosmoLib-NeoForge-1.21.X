package net.meh.cosmolib.registry;

import net.meh.cosmolib.cosmetic.CosmeticRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.meh.cosmolib.CosmoLib.MOD_ID;

public final class CosmoLibCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    /** Main CosmoLib tab: painting table, paintbrush, test hat. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COSMOLIB_TAB =
            CREATIVE_TABS.register("cosmolib_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cosmolib"))
                    .icon(() -> new ItemStack(CosmoLibItems.PAINTBRUSH.get()))
                    .displayItems((params, output) -> {
                        output.accept(CosmoLibItems.PAINTING_TABLE.get());
                        output.accept(CosmoLibItems.PAINTBRUSH.get());
                        output.accept(CosmoLibItems.COSMO_RUG.get());
                        output.accept(CosmoLibItems.AGED_FLAG.get());
                    })
                    .build());

    /** Cosmetics tab: all registered CosmeticItems across all mods. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COSMETICS_TAB =
            CREATIVE_TABS.register("cosmetics_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cosmolib.cosmetics"))
                    .icon(() -> {
                        var entries = CosmeticRegistry.getTabEntries();
                        return entries.isEmpty() ? ItemStack.EMPTY : new ItemStack(entries.get(0));
                    })
                    .displayItems((params, output) ->
                            CosmeticRegistry.getTabEntries().forEach(output::accept))
                    .build());

    private CosmoLibCreativeTabs() {}
}
