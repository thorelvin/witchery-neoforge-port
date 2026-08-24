package com.emoniph.witchery.brewing;

import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrewingYieldTest {

    @Test
    void reproducesEverySourceYieldTier() {
        int[] expectedBottles = {1, 2, 3, 4, 5, 6, 8, 10, 15, 30};
        int[] drainUnits = {3000, 1500, 1000, 750, 600, 500, 375, 300, 200, 100};
        for (int i = 0; i < drainUnits.length; i++) {
            assertEquals(expectedBottles[i], BrewingYield.bottlesPerCauldron(drainUnits[i]));
        }

        assertEquals(3000, BrewingYield.drainAmount(0, 3, 4, true, 0));
        assertEquals(1500, BrewingYield.drainAmount(30, 0, 0, false, 0));
        assertEquals(1000, BrewingYield.drainAmount(30, 1, 0, false, 0));
        assertEquals(1000, BrewingYield.drainAmount(60, 0, 0, false, 0));
        assertEquals(750, BrewingYield.drainAmount(60, 0, 1, false, 0));
        assertEquals(600, BrewingYield.drainAmount(60, 1, 0, false, 0));
        assertEquals(500, BrewingYield.drainAmount(60, 1, 2, false, 0));
        assertEquals(375, BrewingYield.drainAmount(60, 2, 2, false, 0));
        assertEquals(300, BrewingYield.drainAmount(60, 3, 2, false, 0));
        assertEquals(200, BrewingYield.drainAmount(60, 3, 2, true, 0));
        assertEquals(100, BrewingYield.drainAmount(60, 3, 3, true, 0));
    }

    @Test
    void diamondAppliesTheSourceTwoTierYieldPenalty() {
        assertEquals(2, BrewingYield.yieldPenalty(List.of(Items.NETHER_WART, Items.DIAMOND)));
        assertEquals(750, BrewingYield.drainAmount(60, 1, 2, false, 2));
    }

    @Test
    void skillTrainingUsesSourceCapacityThresholds() {
        assertTrue(BrewingYield.canIncreaseSkill(0, 2, 2));
        assertFalse(BrewingYield.canIncreaseSkill(0, 1, 1));
        assertTrue(BrewingYield.canIncreaseSkill(30, 4, 4));
        assertFalse(BrewingYield.canIncreaseSkill(30, 4, 3));
        assertTrue(BrewingYield.canIncreaseSkill(60, 6, 6));
        assertTrue(BrewingYield.canIncreaseSkill(90, 8, 8));
        assertFalse(BrewingYield.canIncreaseSkill(100, 10, 10));
    }
}
