package com.emoniph.witchery.ritual;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A declarative multi-sacrifice rite recipe: the complete set of ingredients a rite consumes,
 * each as a labelled {@link Predicate} over an offered {@link ItemStack}.
 *
 * <p>Today the {@code RitualExecutor} dispatch still keys on a single representative trigger item
 * and then hand-validates the rest via its {@code find*Offerings} helpers. This type captures the
 * <em>full</em> source {@code SacrificeItem} set as data so the recipes can be regression-locked
 * (see {@code RitualRecipeMatchTest}) and so a future {@code RitualDefinition} registry can consume
 * them directly. The {@link #matches} logic is the canonical "do the dropped offerings satisfy this
 * rite?" check: every requirement must be met by a <em>distinct</em> offered stack (two requirements
 * for the same item need two separate stacks), and extra offerings are ignored.</p>
 *
 * <p>Source parity: the {@code SacrificeItem(ItemStack...)} lists registered via
 * {@code RiteRegistry.addRecipe} (1.7.10-0.24.1).</p>
 */
public record RitualRecipe(String name, List<Ingredient> ingredients) {

    /** One required offering: a human label plus the stack test that satisfies it. */
    public record Ingredient(String label, Predicate<ItemStack> test) {
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
     * <p>Uses a greedy assignment that, by ordering requirements most-restrictive-first, is exact
     * for the small, low-overlap ingredient sets rites actually use.</p>
     */
    public boolean matches(List<ItemStack> offered) {
        return assign(offered) != null;
    }

    /**
     * Like {@link #matches} but returns the chosen offered-stack index for each ingredient (in
     * ingredient order), or {@code null} if the offerings cannot satisfy the recipe.
     */
    public int[] assign(List<ItemStack> offered) {
        // Order ingredients by how few offered stacks can satisfy each (fail-fast, fewer dead ends).
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < ingredients.size(); i++) {
            order.add(i);
        }
        order.sort((a, b) -> Integer.compare(countCandidates(ingredients.get(a), offered),
                countCandidates(ingredients.get(b), offered)));

        int[] result = new int[ingredients.size()];
        boolean[] used = new boolean[offered.size()];
        for (int idx : order) {
            Ingredient ingredient = ingredients.get(idx);
            int chosen = -1;
            for (int j = 0; j < offered.size(); j++) {
                if (!used[j] && ingredient.accepts(offered.get(j))) {
                    chosen = j;
                    break;
                }
            }
            if (chosen < 0) {
                return null;
            }
            used[chosen] = true;
            result[idx] = chosen;
        }
        return result;
    }

    private static int countCandidates(Ingredient ingredient, List<ItemStack> offered) {
        int n = 0;
        for (ItemStack stack : offered) {
            if (ingredient.accepts(stack)) {
                n++;
            }
        }
        return n;
    }

    public static final class Builder {
        private final String name;
        private final List<Ingredient> ingredients = new ArrayList<>();

        private Builder(String name) {
            this.name = name;
        }

        public Builder require(String label, Predicate<ItemStack> test) {
            ingredients.add(new Ingredient(label, test));
            return this;
        }

        public RitualRecipe build() {
            return new RitualRecipe(name, List.copyOf(ingredients));
        }
    }
}
