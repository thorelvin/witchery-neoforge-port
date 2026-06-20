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

    /** Composed contents of a custom brew (the ordered ingredient items stirred into the cauldron). */
    public static final Supplier<DataComponentType<com.emoniph.witchery.brewing.BrewContents>> BREW_CONTENTS =
            DATA_COMPONENTS.register("brew_contents", () ->
                    DataComponentType.<com.emoniph.witchery.brewing.BrewContents>builder()
                            .persistent(com.emoniph.witchery.brewing.BrewContents.CODEC)
                            .networkSynchronized(com.emoniph.witchery.brewing.BrewContents.STREAM_CODEC)
                            .build());

    /** A triggered-dispersal brew curse stored on an item: a delayed, charged release of the brew. */
    public static final Supplier<DataComponentType<com.emoniph.witchery.brewing.BrewCurse>> BREW_CURSE =
            DATA_COMPONENTS.register("brew_curse", () ->
                    DataComponentType.<com.emoniph.witchery.brewing.BrewCurse>builder()
                            .persistent(com.emoniph.witchery.brewing.BrewCurse.CODEC)
                            .networkSynchronized(com.emoniph.witchery.brewing.BrewCurse.STREAM_CODEC)
                            .build());

    /** Player-authored warning text bound into a composed brew bottle. */
    public static final Supplier<DataComponentType<com.emoniph.witchery.brewing.BrewWarningText>> BREW_WARNING =
            DATA_COMPONENTS.register("brew_warning", () ->
                    DataComponentType.<com.emoniph.witchery.brewing.BrewWarningText>builder()
                            .persistent(com.emoniph.witchery.brewing.BrewWarningText.CODEC)
                            .networkSynchronized(com.emoniph.witchery.brewing.BrewWarningText.STREAM_CODEC)
                            .build());

    /** A triggered warning curse stored on an item. */
    public static final Supplier<DataComponentType<com.emoniph.witchery.brewing.BrewWarningCurse>> BREW_WARNING_CURSE =
            DATA_COMPONENTS.register("brew_warning_curse", () ->
                    DataComponentType.<com.emoniph.witchery.brewing.BrewWarningCurse>builder()
                            .persistent(com.emoniph.witchery.brewing.BrewWarningCurse.CODEC)
                            .networkSynchronized(com.emoniph.witchery.brewing.BrewWarningCurse.STREAM_CODEC)
                            .build());
}
