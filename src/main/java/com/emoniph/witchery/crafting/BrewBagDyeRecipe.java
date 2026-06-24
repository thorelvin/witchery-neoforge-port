package com.emoniph.witchery.crafting;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryRecipeSerializers;
import com.emoniph.witchery.item.BrewBagItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.OptionalInt;

public class BrewBagDyeRecipe extends CustomRecipe {
    public BrewBagDyeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findInputs(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Inputs inputs = findInputs(input);
        if (inputs == null) {
            return ItemStack.EMPTY;
        }

        ItemStack result = inputs.bag.copyWithCount(1);
        BrewBagItem.setColor(result, inputs.color);
        return result;
    }

    @Override
    public RecipeSerializer<BrewBagDyeRecipe> getSerializer() {
        return WitcheryRecipeSerializers.BREW_BAG_DYE.get();
    }

    private static Inputs findInputs(CraftingInput input) {
        ItemStack bag = ItemStack.EMPTY;
        int bagCount = 0;
        int dyeCount = 0;
        int color = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(WitcheryItems.BREW_BAG.get())) {
                bag = stack;
                bagCount++;
                continue;
            }

            OptionalInt dyeColor = BrewBagItem.colorForDye(stack);
            if (dyeColor.isPresent()) {
                color = dyeColor.getAsInt();
                dyeCount++;
                continue;
            }

            return null;
        }

        return bagCount == 1 && dyeCount == 1 ? new Inputs(bag, color) : null;
    }

    private record Inputs(ItemStack bag, int color) {}
}
