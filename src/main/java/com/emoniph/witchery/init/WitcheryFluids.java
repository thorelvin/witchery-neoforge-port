package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.fluid.BrewLiquidFluid;
import com.emoniph.witchery.fluid.SpiritFluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class WitcheryFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, WitcheryMod.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, WitcheryMod.MODID);

    public static final DeferredHolder<FluidType, FluidType> BREW_LIQUID_TYPE = FLUID_TYPES.register(
            "brew_liquid",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.witchery.brew_liquid")
                    .canConvertToSource(false)
                    .canExtinguish(false)
                    .supportsBoating(false)));

    public static final DeferredHolder<Fluid, BrewLiquidFluid.Source> BREW_LIQUID_SOURCE =
            FLUIDS.register("brew_liquid", BrewLiquidFluid.Source::new);
    public static final DeferredHolder<Fluid, BrewLiquidFluid.Flowing> BREW_LIQUID_FLOWING =
            FLUIDS.register("flowing_brew_liquid", BrewLiquidFluid.Flowing::new);

    public static final DeferredHolder<FluidType, FluidType> FLOWING_SPIRIT_TYPE = FLUID_TYPES.register(
            "flowing_spirit",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.witchery.flowing_spirit")
                    .density(SpiritFluid.SOURCE_DENSITY)
                    .viscosity(SpiritFluid.SOURCE_VISCOSITY)
                    .canConvertToSource(false)
                    .canExtinguish(true)
                    .canHydrate(true)
                    .supportsBoating(true)));

    public static final DeferredHolder<Fluid, SpiritFluid.Source> FLOWING_SPIRIT_SOURCE =
            FLUIDS.register("flowing_spirit", SpiritFluid.Source::new);
    public static final DeferredHolder<Fluid, SpiritFluid.Flowing> FLOWING_SPIRIT_FLOWING =
            FLUIDS.register("flowing_spirit_flowing", SpiritFluid.Flowing::new);
}
