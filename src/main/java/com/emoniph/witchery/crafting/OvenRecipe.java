package com.emoniph.witchery.crafting;

import com.emoniph.witchery.init.WitcheryRecipeSerializers;
import com.emoniph.witchery.init.WitcheryRecipeTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

public class OvenRecipe extends AbstractCookingRecipe {

    public OvenRecipe(String group, CookingBookCategory category,
                      Ingredient ingredient, ItemStack result,
                      float experience, int cookingTime) {
        super(group, category, ingredient, result, experience, cookingTime);
    }

    @Override public net.minecraft.world.item.crafting.RecipeSerializer<OvenRecipe> getSerializer() { return (net.minecraft.world.item.crafting.RecipeSerializer<OvenRecipe>) WitcheryRecipeSerializers.OVEN.get(); }
    @Override public net.minecraft.world.item.crafting.RecipeType<OvenRecipe> getType() { return WitcheryRecipeTypes.OVEN.get(); }
    @Override protected Item furnaceIcon() { return Items.CAULDRON; }

    @Override
    public net.minecraft.world.item.crafting.RecipeBookCategory recipeBookCategory() {
        return net.minecraft.world.item.crafting.RecipeBookCategories.FURNACE_MISC;
    }

    public ItemStack getResultItem() { return result(); }
}
