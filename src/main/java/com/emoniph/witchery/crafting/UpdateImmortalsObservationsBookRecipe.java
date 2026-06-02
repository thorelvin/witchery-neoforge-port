package com.emoniph.witchery.crafting;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryRecipeSerializers;
import com.emoniph.witchery.item.ImmortalsObservationsBookItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class UpdateImmortalsObservationsBookRecipe extends CustomRecipe {
    public UpdateImmortalsObservationsBookRecipe(CraftingBookCategory category) {
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

        ItemStack result = inputs.book().copyWithCount(1);
        return ImmortalsObservationsBookItem.addTornPage(result) ? result : ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<UpdateImmortalsObservationsBookRecipe> getSerializer() {
        return WitcheryRecipeSerializers.UPDATE_IMMORTALS_OBSERVATIONS_BOOK.get();
    }

    private static Inputs findInputs(CraftingInput input) {
        ItemStack book = ItemStack.EMPTY;
        ItemStack tornPage = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (ImmortalsObservationsBookItem.canAddTornPage(stack)) {
                if (!book.isEmpty() || !ImmortalsObservationsBookItem.canAddTornPage(stack)) {
                    return null;
                }
                book = stack;
                continue;
            }
            if (stack.is(WitcheryItems.TORN_PAGE.get())) {
                if (!tornPage.isEmpty()) {
                    return null;
                }
                tornPage = stack;
                continue;
            }
            return null;
        }

        return !book.isEmpty() && !tornPage.isEmpty() ? new Inputs(book) : null;
    }

    private record Inputs(ItemStack book) {
    }
}
