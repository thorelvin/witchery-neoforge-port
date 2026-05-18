package com.emoniph.witchery.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

public record SpinningRecipeInput(ItemStack fibre, List<ItemStack> modifiers) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> fibre;
            case 1 -> modifiers.size() > 0 ? modifiers.get(0) : ItemStack.EMPTY;
            case 2 -> modifiers.size() > 1 ? modifiers.get(1) : ItemStack.EMPTY;
            case 3 -> modifiers.size() > 2 ? modifiers.get(2) : ItemStack.EMPTY;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public int size() {
        return 1 + modifiers.size();
    }
}
