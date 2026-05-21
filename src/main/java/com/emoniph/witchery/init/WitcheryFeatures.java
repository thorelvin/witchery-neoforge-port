package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.worldgen.WildRowanTreeFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WitcheryFeatures {
    private WitcheryFeatures() {}

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, WitcheryMod.MODID);

    public static final DeferredHolder<Feature<?>, WildRowanTreeFeature> WILD_ROWAN_TREE =
            FEATURES.register("wild_rowan_tree", WildRowanTreeFeature::new);
}
