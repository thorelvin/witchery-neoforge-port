package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.menu.DistilleryMenu;
import com.emoniph.witchery.menu.SpinningWheelMenu;
import com.emoniph.witchery.menu.WitchesOvenMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class WitcheryMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, WitcheryMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<WitchesOvenMenu>> WITCHES_OVEN =
            MENU_TYPES.register("witches_oven", () ->
                    IMenuTypeExtension.create(WitchesOvenMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SpinningWheelMenu>> SPINNING_WHEEL =
            MENU_TYPES.register("spinning_wheel", () ->
                    IMenuTypeExtension.create(SpinningWheelMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<DistilleryMenu>> DISTILLERY =
            MENU_TYPES.register("distillery", () ->
                    IMenuTypeExtension.create(DistilleryMenu::new));

}
