package com.emoniph.witchery.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HobgoblinVillageEcologyTest {

    @Test
    void threeHobgoblinsMakeTheSourceRaidPack() {
        assertFalse(HobgoblinVillageEcology.hasRaidPack(0));
        assertFalse(HobgoblinVillageEcology.hasRaidPack(2));
        assertTrue(HobgoblinVillageEcology.hasRaidPack(3));
        assertTrue(HobgoblinVillageEcology.hasRaidPack(4));
    }

    @Test
    void housingCapsPopulationAtThirtyFivePercent() {
        assertEquals(0, HobgoblinVillageEcology.breedingCapacity(0));
        assertEquals(0, HobgoblinVillageEcology.breedingCapacity(2));
        assertEquals(1, HobgoblinVillageEcology.breedingCapacity(3));
        assertEquals(2, HobgoblinVillageEcology.breedingCapacity(6));
        assertEquals(3, HobgoblinVillageEcology.breedingCapacity(9));
        assertEquals(35, HobgoblinVillageEcology.breedingCapacity(100));

        assertTrue(HobgoblinVillageEcology.hasBreedingCapacity(9, 2));
        assertFalse(HobgoblinVillageEcology.hasBreedingCapacity(9, 3));
        assertFalse(HobgoblinVillageEcology.hasBreedingCapacity(0, 0));
    }

    @Test
    void sourceCadenceAgeAndRestrictionConstantsRemainStable() {
        assertEquals(70, HobgoblinVillageEcology.villageRefreshCooldown(0));
        assertEquals(119, HobgoblinVillageEcology.villageRefreshCooldown(49));
        assertEquals(70, HobgoblinVillageEcology.villageRefreshCooldown(-20));
        assertEquals(119, HobgoblinVillageEcology.villageRefreshCooldown(500));
        assertEquals(19, HobgoblinVillageEcology.villageRestrictionRadius(32));
        assertEquals(300, HobgoblinVillageEcology.MATING_DURATION_TICKS);
        assertEquals(6000, HobgoblinVillageEcology.PARENT_COOLDOWN_TICKS);
        assertEquals(-24000, HobgoblinVillageEcology.BABY_AGE_TICKS);
    }
}
