package com.emoniph.witchery.ritual;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * A declarative multi-sacrifice rite recipe: the complete set of ingredients a rite consumes,
 * each as a labelled {@link Predicate} over an offered {@link ItemStack}.
 *
 * <p>This type captures the full source {@code SacrificeItem} set as data. The declarative dispatcher
 * matches it against both dropped entities and Grassper-held stacks. Every requirement must be met
 * by a distinct offering source (two requirements for the same item need two separate sources),
 * optional sacrifices are consumed only when present, and unrelated extra offerings are ignored.</p>
 *
 * <p>Source parity: the {@code SacrificeItem(ItemStack...)} lists registered via
 * {@code RiteRegistry.addRecipe} (1.7.10-0.24.1).</p>
 */
public record RitualRecipe(String name, List<Ingredient> ingredients) {

    /** One offering: a human label, stack test, and whether the rite may proceed without it. */
    public record Ingredient(String label, Predicate<ItemStack> test, boolean optional) {
        public boolean accepts(ItemStack stack) {
            return !stack.isEmpty() && test.test(stack);
        }
    }

    public static Builder named(String name) {
        return new Builder(name);
    }

    /**
     * Does the bag of {@code offered} stacks satisfy every ingredient with distinct stacks?
     *
     * <p>Required ingredients use deterministic bipartite matching. This remains exact when two
     * requirements accept overlapping item sets.</p>
     */
    public boolean matches(List<ItemStack> offered) {
        return assign(offered) != null;
    }

    /**
     * Like {@link #matches} but returns the chosen offered-stack index for each ingredient (in
     * ingredient order), or {@code null} if the offerings cannot satisfy the recipe.
     */
    public int[] assign(List<ItemStack> offered) {
        int[] result = new int[ingredients.size()];
        Arrays.fill(result, -1);
        int[] ingredientForOffering = new int[offered.size()];
        Arrays.fill(ingredientForOffering, -1);

        for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
            if (ingredients.get(ingredientIndex).optional()) {
                continue;
            }
            if (!assignRequired(ingredientIndex, offered, ingredientForOffering, result,
                    new boolean[offered.size()])) {
                return null;
            }
        }

        // Optional sacrifices never displace required ones and use the first remaining source.
        for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
            Ingredient ingredient = ingredients.get(ingredientIndex);
            if (!ingredient.optional()) {
                continue;
            }
            for (int offeringIndex = 0; offeringIndex < offered.size(); offeringIndex++) {
                if (ingredientForOffering[offeringIndex] < 0
                        && ingredient.accepts(offered.get(offeringIndex))) {
                    ingredientForOffering[offeringIndex] = ingredientIndex;
                    result[ingredientIndex] = offeringIndex;
                    break;
                }
            }
        }
        return result;
    }

    private boolean assignRequired(int ingredientIndex, List<ItemStack> offered,
                                   int[] ingredientForOffering, int[] offeringForIngredient,
                                   boolean[] visitedOfferings) {
        Ingredient ingredient = ingredients.get(ingredientIndex);
        for (int offeringIndex = 0; offeringIndex < offered.size(); offeringIndex++) {
            if (visitedOfferings[offeringIndex] || !ingredient.accepts(offered.get(offeringIndex))) {
                continue;
            }
            visitedOfferings[offeringIndex] = true;
            int displacedIngredient = ingredientForOffering[offeringIndex];
            if (displacedIngredient < 0
                    || assignRequired(displacedIngredient, offered, ingredientForOffering,
                    offeringForIngredient, visitedOfferings)) {
                ingredientForOffering[offeringIndex] = ingredientIndex;
                offeringForIngredient[ingredientIndex] = offeringIndex;
                return true;
            }
        }
        return false;
    }

    public static final class Builder {
        private final String name;
        private final List<Ingredient> ingredients = new ArrayList<>();

        private Builder(String name) {
            this.name = name;
        }

        public Builder require(String label, Predicate<ItemStack> test) {
            ingredients.add(new Ingredient(label, test, false));
            return this;
        }

        public Builder optional(String label, Predicate<ItemStack> test) {
            ingredients.add(new Ingredient(label, test, true));
            return this;
        }

        public RitualRecipe build() {
            return new RitualRecipe(name, List.copyOf(ingredients));
        }
    }
}
