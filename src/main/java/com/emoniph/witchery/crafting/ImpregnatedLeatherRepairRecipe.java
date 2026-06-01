package com.emoniph.witchery.crafting;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryRecipeSerializers;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class ImpregnatedLeatherRepairRecipe extends CustomRecipe {
    public ImpregnatedLeatherRepairRecipe(CraftingBookCategory category) {
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

        ItemStack result = inputs.repairable().copyWithCount(1);
        result.setDamageValue(0);
        return result;
    }

    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 4;
    }

    @Override
    public RecipeSerializer<ImpregnatedLeatherRepairRecipe> getSerializer() {
        return WitcheryRecipeSerializers.IMPREGNATED_LEATHER_REPAIR.get();
    }

    private static Inputs findInputs(CraftingInput input) {
        ItemStack repairable = ItemStack.EMPTY;
        int requiredLeather = 0;
        int leatherCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(WitcheryItems.IMPREGNATED_LEATHER.get())) {
                leatherCount++;
                continue;
            }

            int repairCost = repairLeatherRequired(stack);
            if (repairCost <= 0 || !repairable.isEmpty()) {
                return null;
            }

            repairable = stack;
            requiredLeather = repairCost;
        }

        if (repairable.isEmpty() || leatherCount != requiredLeather) {
            return null;
        }
        return new Inputs(repairable);
    }

    private static int repairLeatherRequired(ItemStack stack) {
        if (WitcheryItems.isWitchesRobe(stack)
                || stack.is(WitcheryItems.NECROMANCER_ROBES.get())
                || stack.is(WitcheryItems.BITING_BELT.get())
                || stack.is(WitcheryItems.BARK_BELT.get())) {
            return 4;
        }
        if (stack.is(WitcheryItems.WITCHES_HAT.get())
                || stack.is(WitcheryItems.BABAS_HAT.get())
                || WitcheryItems.isIcySlippers(stack)
                || stack.is(WitcheryItems.RUBY_SLIPPERS.get())
                || stack.is(WitcheryItems.SEEPING_SHOES.get())) {
            return 3;
        }
        return 0;
    }

    private record Inputs(ItemStack repairable) {
    }
}
