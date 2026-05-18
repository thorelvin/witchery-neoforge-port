package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.crafting.KettleBrewingRecipe;
import com.emoniph.witchery.crafting.OvenRecipe;
import com.emoniph.witchery.crafting.DistilleryRecipe;
import com.emoniph.witchery.crafting.SpinningRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class WitcheryRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, WitcheryMod.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<KettleBrewingRecipe>> KETTLE_BREWING =
            RECIPE_TYPES.register("kettle_brewing", () -> RecipeType.simple(
                    Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "kettle_brewing")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<OvenRecipe>> OVEN =
            RECIPE_TYPES.register("oven", () -> RecipeType.simple(
                    Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "oven")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<DistilleryRecipe>> DISTILLERY =
            RECIPE_TYPES.register("distillery", () -> RecipeType.simple(
                    Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "distillery")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<SpinningRecipe>> SPINNING =
            RECIPE_TYPES.register("spinning", () -> RecipeType.simple(
                    Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "spinning")));
}
