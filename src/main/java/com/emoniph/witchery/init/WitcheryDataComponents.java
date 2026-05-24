package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class WitcheryDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, WitcheryMod.MODID);

    /** Power tier of a custom brew (1–5). Tier 3 is the baseline (3-bottle output). */
    public static final Supplier<DataComponentType<Integer>> BREW_POWER =
            DATA_COMPONENTS.register("brew_power", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
                            .build());
}
