package com.emoniph.witchery.crafting;

import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WitcheryKettleRecipes {
    private static final WitcheryKettleRecipes INSTANCE = new WitcheryKettleRecipes();
    public final List<KettleRecipe> recipes = new ArrayList<>();

    public static WitcheryKettleRecipes instance() { return INSTANCE; }
    private WitcheryKettleRecipes() {}

    public KettleRecipe addRecipe(ItemStack output, int hatBonus, float power, int color, ItemStack... inputs) {
        KettleRecipe recipe = new KettleRecipe(output, hatBonus, power, color, inputs);
        recipes.add(recipe);
        return recipe;
    }

    /** Find a matching recipe. If partial=true, accepts subsets of ingredients. */
    public KettleRecipe getResult(ItemStack[] current, int length, boolean partial) {
        for (KettleRecipe recipe : recipes) {
            if (recipe.isMatch(current, length, partial)) return recipe;
        }
        return null;
    }

    public KettleRecipe findRecipeFor(ItemStack result) {
        for (KettleRecipe recipe : recipes) {
            if (ItemStack.isSameItem(recipe.output, result)) return recipe;
        }
        return null;
    }

    public int getHatBonus(ItemStack stack) {
        for (KettleRecipe recipe : recipes) {
            if (ItemStack.isSameItem(recipe.output, stack)) return recipe.hatBonus;
        }
        return 0;
    }

    public static class KettleRecipe {
        public final ItemStack[] inputs;
        public final ItemStack output;
        public final float power;
        public final int color;
        public final int hatBonus;

        KettleRecipe(ItemStack output, int hatBonus, float power, int color, ItemStack... inputs) {
            this.output = output;
            this.hatBonus = hatBonus;
            this.power = power;
            this.color = color;
            this.inputs = inputs;
        }

        public boolean isMatch(ItemStack[] current, int length, boolean partial) {
            List<ItemStack> needed = new ArrayList<>(Arrays.asList(inputs));
            int matched = 0;
            for (int j = 0; j < length; j++) {
                ItemStack stack = current[j];
                if (stack == null || stack.isEmpty()) break;
                boolean found = false;
                for (int i = 0; i < needed.size(); i++) {
                    if (needed.get(i) != null && ItemStack.isSameItem(stack, needed.get(i))) {
                        needed.remove(i);
                        matched++;
                        found = true;
                        break;
                    }
                }
                if (!found) return false; // wrong ingredient
            }
            if (partial) return matched > 0 && needed.size() < inputs.length;
            return needed.isEmpty();
        }

        public ItemStack getOutput() { return output.copy(); }
        public int getColor() { return color; }
        public float getRequiredPower() { return power; }
    }
}
