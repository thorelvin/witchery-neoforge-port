package com.emoniph.witchery.blocks;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WitchCauldronBlockTest {

    @Test
    void leonardRecipeMatchesExactDissolvedIngredientSequence() {
        assertTrue(WitchCauldronBlock.isLeonardSummonBrew(List.of(
                Items.NETHER_WART,
                WitcheryItems.TEAR_OF_GODDESS.get(),
                WitcheryItems.DIAMOND_VAPOUR.get(),
                Items.DIAMOND,
                Items.NETHER_STAR)));
    }

    @Test
    void leonardRecipeRejectsWrongOrderOrExtraIngredients() {
        assertFalse(WitchCauldronBlock.isLeonardSummonBrew(List.of(
                WitcheryItems.TEAR_OF_GODDESS.get(),
                Items.NETHER_WART,
                WitcheryItems.DIAMOND_VAPOUR.get(),
                Items.DIAMOND,
                Items.NETHER_STAR)));

        assertFalse(WitchCauldronBlock.isLeonardSummonBrew(List.of(
                Items.NETHER_WART,
                WitcheryItems.TEAR_OF_GODDESS.get(),
                WitcheryItems.DIAMOND_VAPOUR.get(),
                Items.DIAMOND,
                Items.NETHER_STAR,
                Items.GUNPOWDER)));
    }

    @Test
    void sourceCauldronCookingRoutesCoverEveryRegisteredMeat() {
        assertTrue(WitchCauldronBlock.sourceCookedResult(new net.minecraft.world.item.ItemStack(Items.PORKCHOP)).is(Items.COOKED_PORKCHOP));
        assertTrue(WitchCauldronBlock.sourceCookedResult(new net.minecraft.world.item.ItemStack(Items.CHICKEN)).is(Items.COOKED_CHICKEN));
        assertTrue(WitchCauldronBlock.sourceCookedResult(new net.minecraft.world.item.ItemStack(Items.BEEF)).is(Items.COOKED_BEEF));
        assertTrue(WitchCauldronBlock.sourceCookedResult(new net.minecraft.world.item.ItemStack(Items.MUTTON)).is(Items.COOKED_MUTTON));
        assertTrue(WitchCauldronBlock.sourceCookedResult(new net.minecraft.world.item.ItemStack(WitcheryItems.ODD_PORKCHOP_RAW.get()))
                .is(WitcheryItems.ODD_PORKCHOP_COOKED.get()));
        assertEquals(0, WitchCauldronBlock.sourceCookedResult(new net.minecraft.world.item.ItemStack(Items.COD)).getCount());
    }

    @Test
    void sourceUtilityBrewRecipesChooseTheMostCompleteTier() {
        assertEquals(3, WitchCauldronBlock.sourceEndlessWaterTier(Set.of(Items.NETHER_WART)));
        assertEquals(2, WitchCauldronBlock.sourceEndlessWaterTier(Set.of(Items.NETHER_WART, Items.GLOWSTONE_DUST)));
        assertEquals(1, WitchCauldronBlock.sourceEndlessWaterTier(Set.of(
                Items.NETHER_WART, Items.GLOWSTONE_DUST, Items.BLAZE_ROD)));
        assertEquals(0, WitchCauldronBlock.sourceEndlessWaterTier(Set.of(
                Items.NETHER_WART, Items.GLOWSTONE_DUST, Items.BLAZE_ROD,
                WitcheryItems.ATTUNED_STONE_CHARGED.get())));
        assertEquals(-1, WitchCauldronBlock.sourceEndlessWaterTier(Set.of(Items.GLOWSTONE_DUST)));

        assertEquals(0, WitchCauldronBlock.sourceCombustionLevel(Set.of(WitcheryItems.MANDRAKE_ROOT.get())));
        assertEquals(1, WitchCauldronBlock.sourceCombustionLevel(Set.of(
                WitcheryItems.MANDRAKE_ROOT.get(), Items.GLOWSTONE_DUST)));
        assertEquals(3, WitchCauldronBlock.sourceCombustionLevel(Set.of(
                WitcheryItems.MANDRAKE_ROOT.get(), Items.GLOWSTONE_DUST, Items.BLAZE_ROD,
                WitcheryItems.ATTUNED_STONE_CHARGED.get())));
    }

    @Test
    void sourcePlayerCompassOfferingsAreExact() {
        assertTrue(WitchCauldronBlock.hasSourcePlayerCompassOfferings(Set.of(
                Items.NETHER_WART, WitcheryItems.TEAR_OF_GODDESS.get(), Items.VINE, Items.RED_MUSHROOM)));
        assertFalse(WitchCauldronBlock.hasSourcePlayerCompassOfferings(Set.of(
                Items.NETHER_WART, WitcheryItems.TEAR_OF_GODDESS.get(), Items.RED_MUSHROOM)));
    }
}
