package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.blocks.entity.BrazierBlockEntity.BrazierMode;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BrazierRecipeMatchTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void keepsSourceRecipesAndSixModernUseCases() {
        assertEquals(14, BrazierBlockEntity.RECIPES.size(),
                "8 source brazier recipes plus the 6 modern completion recipes");
    }

    @Test
    void sourceThreeItemRecipesMatchInAnySlotOrder() {
        assertTrue(matches(
                WitcheryItems.GRAVEYARD_DUST.get(),
                WitcheryItems.WORMWOOD.get(),
                WitcheryItems.BAT_WOOL.get()));
    }

    @Test
    void everyRecipeMatchesUniquelyAndRoutesToAWorkingMode() {
        List<ExpectedRecipe> expectedRecipes = expectedRecipes();
        assertEquals(expectedRecipes.size(), BrazierBlockEntity.RECIPES.size());

        Set<BrazierBlockEntity.BrazierRecipe> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ExpectedRecipe expected : expectedRecipes) {
            BrazierBlockEntity.BrazierRecipe recipe = matchingRecipe(expected.ingredients());
            assertTrue(seen.add(recipe), expected.name() + " should resolve to a unique recipe object");
            assertRecipeShape(expected, recipe);
            assertEveryIngredientOrderMatches(expected, recipe);
            assertExtraIngredientsDoNotMatch(expected);
        }
        assertEquals(BrazierBlockEntity.RECIPES.size(), seen.size());
    }

    @Test
    void spectreConjurationRequiresAltarPowerForThirtySeconds() {
        BrazierBlockEntity.BrazierRecipe spectre = matchingRecipe(
                WitcheryItems.WORMWOOD.get(),
                WitcheryItems.BAT_WOOL.get(),
                WitcheryItems.GRAVEYARD_DUST.get());

        assertTrue(spectre.needsAltar());
        assertEquals(600, spectre.burnTicks());
    }

    @Test
    void newSingleAndTwoItemRecipesRejectExtraIngredients() {
        assertTrue(matches(Items.GUNPOWDER));
        assertTrue(matches(WitcheryItems.DIAMOND_VAPOUR.get(), WitcheryItems.FANCIFUL_THREAD.get()));

        assertEquals(0, matchCount(Items.GUNPOWDER, Items.REDSTONE),
                "single ingredient use cases must not trigger when another slot is filled");
        assertEquals(0, matchCount(WitcheryItems.DIAMOND_VAPOUR.get(), WitcheryItems.FANCIFUL_THREAD.get(), Items.REDSTONE),
                "two ingredient use cases must not trigger when another slot is filled");
    }

    @Test
    void modernCompletionUseCasesResolveQuickly() {
        assertEquals(20, matchingRecipe(Items.GUNPOWDER).burnTicks());
        assertEquals(20, matchingRecipe(WitcheryItems.DIAMOND_VAPOUR.get(), WitcheryItems.FANCIFUL_THREAD.get()).burnTicks());
    }

    private static boolean matches(Item... items) {
        return matchCount(items) == 1;
    }

    private static void assertRecipeShape(ExpectedRecipe expected, BrazierBlockEntity.BrazierRecipe recipe) {
        assertEquals(expected.burnTicks(), recipe.burnTicks(), expected.name());
        assertEquals(expected.needsAltar(), recipe.needsAltar(), expected.name());
        assertEquals(expected.mode(), recipe.mode(), expected.name());
        assertEquals(expected.effectRadius(), recipe.effectRadius(), expected.name());
        assertEquals(expected.pulseInterval(), recipe.pulseInterval(), expected.name());
        assertEquals(expected.effectCount(), recipe.effects().size(), expected.name());
        assertEquals(expected.hasEntity(), recipe.summonEntity() != null, expected.name());
        assertTrue(recipe.exactIngredients(), expected.name() + " should reject accidental extra ingredients");
        assertTrue(recipe.burnTicks() > 0, expected.name() + " must be able to finish");
        assertTrue(recipe.ingredients().length >= 1 && recipe.ingredients().length <= 3,
                expected.name() + " must fit the brazier's three ingredient slots");
        assertEquals(recipe.ingredients().length, new HashSet<>(List.of(recipe.ingredients())).size(),
                expected.name() + " should not require duplicate items in a three-slot matcher");
        for (Item ingredient : recipe.ingredients()) {
            assertNotNull(ingredient, expected.name() + " contains a missing ingredient");
        }

        switch (recipe.mode()) {
            case SUMMON -> {
                assertNotNull(recipe.summonEntity(), expected.name() + " must have something to summon");
                assertTrue(recipe.effects().isEmpty(), expected.name() + " summon recipes should not carry mob effects");
                assertTrue(recipe.pulseInterval() > 0, expected.name() + " should pulse while burning");
            }
            case AURA -> {
                assertNull(recipe.summonEntity(), expected.name() + " aura recipes should not summon directly");
                assertFalse(recipe.effects().isEmpty(), expected.name() + " aura recipes need effects");
                assertTrue(recipe.effectRadius() > 0, expected.name() + " aura recipes need a radius");
                assertTrue(recipe.pulseInterval() > 0, expected.name() + " aura recipes need a pulse");
            }
            case WILTING, SMOKE -> {
                assertNull(recipe.summonEntity(), expected.name() + " environmental recipes should not summon directly");
                assertTrue(recipe.effects().isEmpty(), expected.name() + " environmental recipes use custom logic");
                assertTrue(recipe.effectRadius() > 0, expected.name() + " environmental recipes need a radius");
                assertTrue(recipe.pulseInterval() > 0, expected.name() + " environmental recipes need a pulse");
            }
            case COMPLETION_BUFF -> {
                assertNull(recipe.summonEntity(), expected.name() + " completion buffs should not summon directly");
                assertFalse(recipe.effects().isEmpty(), expected.name() + " completion buffs need effects");
                assertEquals(0, recipe.pulseInterval(), expected.name() + " completion buffs should fire once");
            }
        }
    }

    private static void assertEveryIngredientOrderMatches(ExpectedRecipe expected,
                                                           BrazierBlockEntity.BrazierRecipe recipe) {
        for (Item[] permutation : permutations(expected.ingredients())) {
            assertEquals(1, matchCount(permutation), expected.name() + " should match exactly once in any order");
            assertSame(recipe, matchingRecipe(permutation), expected.name() + " should resolve to the same recipe");
        }
    }

    private static void assertExtraIngredientsDoNotMatch(ExpectedRecipe expected) {
        if (expected.ingredients().length >= 3) {
            return;
        }
        Item[] withExtraIngredient = Arrays.copyOf(expected.ingredients(), expected.ingredients().length + 1);
        withExtraIngredient[withExtraIngredient.length - 1] = Items.REDSTONE;
        assertEquals(0, matchCount(withExtraIngredient),
                expected.name() + " should reject extra ingredients");
    }

    private static List<Item[]> permutations(Item[] items) {
        List<Item[]> result = new ArrayList<>();
        permute(items.clone(), 0, result);
        return result;
    }

    private static void permute(Item[] items, int index, List<Item[]> result) {
        if (index == items.length) {
            result.add(items.clone());
            return;
        }
        for (int i = index; i < items.length; i++) {
            swap(items, index, i);
            permute(items, index + 1, result);
            swap(items, index, i);
        }
    }

    private static void swap(Item[] items, int first, int second) {
        Item temp = items[first];
        items[first] = items[second];
        items[second] = temp;
    }

    private static List<ExpectedRecipe> expectedRecipes() {
        return List.of(
                expected("Conjure Spectres", 600, true, BrazierMode.SUMMON, true, 0, 0, 100,
                        WitcheryItems.WORMWOOD.get(), WitcheryItems.BAT_WOOL.get(), WitcheryItems.GRAVEYARD_DUST.get()),
                expected("Conjure Banshees", 600, true, BrazierMode.SUMMON, true, 0, 0, 100,
                        WitcheryItems.WORMWOOD.get(), WitcheryItems.CONDENSED_FEAR.get(), WitcheryItems.GRAVEYARD_DUST.get()),
                expected("Conjure Poltergeist", 900, false, BrazierMode.SUMMON, true, 0, 0, 100,
                        WitcheryItems.WORMWOOD.get(), WitcheryItems.REFINED_EVIL.get(), WitcheryItems.FOCUSED_WILL.get()),
                expected("Anguish of the Dead", 6000, true, BrazierMode.AURA, false, 1, 4, 60,
                        WitcheryItems.TEAR_OF_GODDESS.get(), Items.BLAZE_ROD, Items.GOLDEN_APPLE),
                expected("Fortification of the Corpse", 6000, true, BrazierMode.AURA, false, 1, 4, 60,
                        WitcheryItems.TEAR_OF_GODDESS.get(), Items.DIAMOND, Items.GOLDEN_APPLE),
                expected("Deathly Veil", 12000, true, BrazierMode.AURA, false, 1, 6, 60,
                        Items.FERMENTED_SPIDER_EYE, Items.ENDER_PEARL, Items.DIAMOND),
                expected("Drain Growth", 1200, false, BrazierMode.WILTING, false, 0, 3, 5,
                        WitcheryItems.CONDENSED_FEAR.get(), WitcheryItems.WORMY_APPLE.get(), WitcheryItems.GRAVEYARD_DUST.get()),
                expected("Graveyard Mist", 6000, true, BrazierMode.SMOKE, false, 0, 16, 1,
                        Items.NETHER_WART, WitcheryItems.QUICKLIME.get(), Items.BLAZE_POWDER),
                expected("Infernal Animus", 20, true, BrazierMode.COMPLETION_BUFF, false, 2, 8, 0,
                        WitcheryItems.INFERNAL_ANIMUS.get()),
                expected("Exhale of Horned One", 20, true, BrazierMode.COMPLETION_BUFF, false, 1, 8, 0,
                        WitcheryItems.EXHALE_OF_HORNED_ONE.get()),
                expected("Rowan Berries", 20, false, BrazierMode.COMPLETION_BUFF, false, 1, 8, 0,
                        WitcheryItems.ROWAN_BERRIES.get()),
                expected("Fanciful Thread", 20, false, BrazierMode.COMPLETION_BUFF, false, 1, 8, 0,
                        WitcheryItems.FANCIFUL_THREAD.get()),
                expected("Diamond Vapour and Fanciful Thread", 20, true, BrazierMode.COMPLETION_BUFF, false, 2, 8, 0,
                        WitcheryItems.DIAMOND_VAPOUR.get(), WitcheryItems.FANCIFUL_THREAD.get()),
                expected("Gunpowder", 20, false, BrazierMode.COMPLETION_BUFF, false, 2, 8, 0,
                        Items.GUNPOWDER)
        );
    }

    private static ExpectedRecipe expected(String name, int burnTicks, boolean needsAltar, BrazierMode mode,
                                           boolean hasEntity, int effectCount, int effectRadius,
                                           int pulseInterval, Item... ingredients) {
        return new ExpectedRecipe(name, burnTicks, needsAltar, mode, hasEntity, effectCount, effectRadius,
                pulseInterval, ingredients);
    }

    private static BrazierBlockEntity.BrazierRecipe matchingRecipe(Item... items) {
        ItemStack first = stack(items, 0);
        ItemStack second = stack(items, 1);
        ItemStack third = stack(items, 2);
        for (BrazierBlockEntity.BrazierRecipe recipe : BrazierBlockEntity.RECIPES) {
            if (recipe.matches(first, second, third)) {
                return recipe;
            }
        }
        throw new AssertionError("No matching brazier recipe");
    }

    private static int matchCount(Item... items) {
        ItemStack first = stack(items, 0);
        ItemStack second = stack(items, 1);
        ItemStack third = stack(items, 2);
        int count = 0;
        for (BrazierBlockEntity.BrazierRecipe recipe : BrazierBlockEntity.RECIPES) {
            if (recipe.matches(first, second, third)) {
                count++;
            }
        }
        return count;
    }

    private static ItemStack stack(Item[] items, int index) {
        return index < items.length ? new ItemStack(items[index]) : ItemStack.EMPTY;
    }

    private record ExpectedRecipe(String name, int burnTicks, boolean needsAltar, BrazierMode mode,
                                  boolean hasEntity, int effectCount, int effectRadius, int pulseInterval,
                                  Item[] ingredients) {
    }
}
