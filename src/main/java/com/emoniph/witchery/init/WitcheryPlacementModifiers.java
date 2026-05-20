package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.worldgen.TreeClearancePlacement;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WitcheryPlacementModifiers {
    private WitcheryPlacementModifiers() {}

    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, WitcheryMod.MODID);

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<TreeClearancePlacement>> TREE_CLEARANCE =
            PLACEMENT_MODIFIERS.register("tree_clearance", () -> () -> TreeClearancePlacement.CODEC);
}
