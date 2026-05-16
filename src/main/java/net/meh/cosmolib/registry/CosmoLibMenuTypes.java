package net.meh.cosmolib.registry;

import net.meh.cosmolib.cosmetic.screen.CosmeticMenu;
import net.meh.cosmolib.screen.PaintingTableMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static net.meh.cosmolib.CosmoLib.MOD_ID;

public final class CosmoLibMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<PaintingTableMenu>> PAINTING_TABLE =
            MENUS.register("painting_table",
                    () -> IMenuTypeExtension.create((id, inv, data) -> new PaintingTableMenu(id, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<CosmeticMenu>> COSMETIC_SCREEN =
            MENUS.register("cosmetic_screen",
                    () -> IMenuTypeExtension.create((id, inv, data) -> new CosmeticMenu(id, inv)));

    private CosmoLibMenuTypes() {}
}
