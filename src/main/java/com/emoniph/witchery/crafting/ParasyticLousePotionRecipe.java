package com.emoniph.witchery.crafting;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryRecipeSerializers;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class ParasyticLousePotionRecipe extends CustomRecipe {
    public ParasyticLousePotionRecipe(CraftingBookCategory category) {
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

        ItemStack result = inputs.louse.copyWithCount(1);
        result.set(DataComponents.POTION_CONTENTS, inputs.potionContents);
        return result;
    }

    @Override
    public RecipeSerializer<ParasyticLousePotionRecipe> getSerializer() {
        return WitcheryRecipeSerializers.PARASYTIC_LOUSE_POTION.get();
    }

    private static Inputs findInputs(CraftingInput input) {
        ItemStack louse = ItemStack.EMPTY;
        PotionContents potionContents = PotionContents.EMPTY;
        int louseCount = 0;
        int potionCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(WitcheryItems.PARASYTIC_LOUSE.get())) {
                louse = stack;
                louseCount++;
                continue;
            }

            if (stack.is(Items.POTION)) {
                PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                if (!contents.hasEffects()) {
                    return null;
                }
                potionContents = contents;
                potionCount++;
                continue;
            }

            return null;
        }

        return louseCount == 1 && potionCount == 1 ? new Inputs(louse, potionContents) : null;
    }

    private record Inputs(ItemStack louse, PotionContents potionContents) {}
}
