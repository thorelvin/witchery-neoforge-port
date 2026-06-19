package com.emoniph.witchery.blocks;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
