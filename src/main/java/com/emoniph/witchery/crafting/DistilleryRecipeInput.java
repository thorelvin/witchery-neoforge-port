package com.emoniph.witchery.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record DistilleryRecipeInput(ItemStack ingredient1, ItemStack ingredient2, ItemStack jars) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> ingredient1;
            case 1 -> ingredient2;
            case 2 -> jars;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public int size() {
        return 3;
    }

    public int jarCount() {
        return jars.isEmpty() ? 0 : jars.getCount();
    }
}
