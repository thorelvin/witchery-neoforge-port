package com.emoniph.witchery.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.Arrays;
import java.util.List;

/** Wraps the kettle's ingredient array for recipe lookup. */
public record KettleRecipeInput(List<ItemStack> ingredients) implements RecipeInput {

    public static KettleRecipeInput of(ItemStack[] items, int count) {
        List<ItemStack> filled = Arrays.stream(items, 0, count)
                .filter(s -> s != null && !s.isEmpty())
                .toList();
        return new KettleRecipeInput(filled);
    }

    @Override
    public ItemStack getItem(int index) {
        return ingredients.get(index);
    }

    @Override
    public int size() {
        return ingredients.size();
    }
}
