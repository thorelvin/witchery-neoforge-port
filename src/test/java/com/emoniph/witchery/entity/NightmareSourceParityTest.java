package com.emoniph.witchery.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NightmareSourceParityTest {
    @Test
    void defenseDurationsMatchEntityNightmare() {
        assertEquals(80, NightmareEntity.sourceDefenseTicks(true, false));
        assertEquals(40, NightmareEntity.sourceDefenseTicks(true, true));
        assertEquals(40, NightmareEntity.sourceDefenseTicks(false, false));
        assertEquals(30, NightmareEntity.sourceDefenseTicks(false, true));
    }

    @Test
    void mellifluousHungerLootingMathMatchesEntityNightmare() {
        assertEquals(10, NightmareEntity.sourceHungerRollBound(0));
        assertEquals(9, NightmareEntity.sourceHungerRollBound(1));
        assertEquals(5, NightmareEntity.sourceHungerRollBound(5));
        assertEquals(5, NightmareEntity.sourceHungerRollBound(20));

        assertEquals(1, NightmareEntity.sourceHungerQuantity(0, 0));
        assertEquals(2, NightmareEntity.sourceHungerQuantity(1, 0));
        assertEquals(1, NightmareEntity.sourceHungerQuantity(1, 1));
    }

    @Test
    void lifecycleCombatAndRenderConstantsMatchEntityNightmare() {
        assertEquals(256.0D, NightmareEntity.SOURCE_VICTIM_RANGE_SQR);
        assertEquals(15.0F, NightmareEntity.SOURCE_DAMAGE_CAP);
        assertEquals(15, NightmareEntity.SOURCE_ATTACK_TIMER_TICKS);
        assertEquals(0.5F, NightmareEntity.SOURCE_SHADOW_RADIUS);
        assertEquals(0xE6FFFFFF, NightmareEntity.SOURCE_NORMAL_ALPHA);
        assertEquals(0x99FFFFFF, NightmareEntity.SOURCE_DEFENDED_ALPHA);
    }

    @Test
    void wakingNightmareSpawnCadenceMatchesInfusionUpdate() {
        assertEquals(180, NightmareEntity.sourceWakingSpawnBound(1));
        assertEquals(180, NightmareEntity.sourceWakingSpawnBound(2));
        assertEquals(60, NightmareEntity.sourceWakingSpawnBound(3));
        assertEquals(60, NightmareEntity.sourceWakingSpawnBound(4));
        assertEquals(30, NightmareEntity.sourceWakingSpawnBound(5));
    }
}
