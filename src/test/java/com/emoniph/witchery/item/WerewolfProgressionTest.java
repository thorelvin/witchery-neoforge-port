package com.emoniph.witchery.item;

import com.emoniph.witchery.data.WitcheryPlayerData.WolfForm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WerewolfProgressionTest {

    @Test
    void questTargetsMatchTheTenLevelSourcePath() {
        assertEquals(10, WerewolfProgression.questTarget(5));
        assertEquals(16, WerewolfProgression.questTarget(6));
        assertEquals(6, WerewolfProgression.questTarget(7));
        assertEquals(30, WerewolfProgression.questTarget(8));
        assertEquals(1, WerewolfProgression.questTarget(9));
        assertEquals(0, WerewolfProgression.questTarget(4));
        assertEquals(0, WerewolfProgression.questTarget(10));
    }

    @Test
    void sourceStatTablesPreserveFormAndLevelGates() {
        var wolfOne = WerewolfProgression.stats(WolfForm.WOLF, 1);
        assertEquals(0.5F, wolfOne.speed());
        assertEquals(1.0F, wolfOne.damage());
        assertEquals(4.0F, wolfOne.damageCap());

        var wolfTen = WerewolfProgression.stats(WolfForm.WOLF, 10);
        assertEquals(1.75F, wolfTen.speed());
        assertEquals(1.0D, wolfTen.leap());
        assertEquals(12, wolfTen.health());
        assertEquals(3.0F, wolfTen.resistance());
        assertEquals(5, wolfTen.fall());

        var lockedWolfman = WerewolfProgression.stats(WolfForm.WOLFMAN, 4);
        assertEquals(0.0F, lockedWolfman.speed());
        assertEquals(0, lockedWolfman.health());

        var wolfmanFive = WerewolfProgression.stats(WolfForm.WOLFMAN, 5);
        assertEquals(0.2F, wolfmanFive.speed());
        assertEquals(20, wolfmanFive.health());
        assertEquals(4.0F, wolfmanFive.damage());
        assertEquals(3.0F, wolfmanFive.resistance());
    }

    @Test
    void moonCharmStateMachineMatchesSourceControls() {
        assertEquals(WolfForm.WOLF,
                WerewolfProgression.moonCharmNextForm(WolfForm.NONE, false, 2));
        assertEquals(WolfForm.WOLF,
                WerewolfProgression.moonCharmNextForm(WolfForm.NONE, true, 4));
        assertEquals(WolfForm.WOLFMAN,
                WerewolfProgression.moonCharmNextForm(WolfForm.NONE, true, 5));
        assertEquals(WolfForm.NONE,
                WerewolfProgression.moonCharmNextForm(WolfForm.WOLF, false, 10));
        assertEquals(WolfForm.WOLFMAN,
                WerewolfProgression.moonCharmNextForm(WolfForm.WOLF, true, 5));
        assertEquals(WolfForm.WOLF,
                WerewolfProgression.moonCharmNextForm(WolfForm.WOLFMAN, false, 10));
        assertEquals(WolfForm.NONE,
                WerewolfProgression.moonCharmNextForm(WolfForm.WOLFMAN, true, 10));
    }

    @Test
    void wolfsbaneStrengthAndMoonWindowMatchSource() {
        assertEquals(1, WerewolfProgression.wolfsbaneStrength(0));
        assertEquals(3, WerewolfProgression.wolfsbaneStrength(1));
        assertEquals(6, WerewolfProgression.wolfsbaneStrength(2));
        assertEquals(9, WerewolfProgression.wolfsbaneStrength(3));

        assertTrue(WerewolfProgression.isFullMoonNight(15000L));
        assertFalse(WerewolfProgression.isFullMoonNight(0L));
        assertFalse(WerewolfProgression.isFullMoonNight(24000L + 15000L));
    }

    @Test
    void powerHowlUsesThePersistedSourceMinuteAndCreativeBypass() {
        long now = 1_000_000L;
        assertTrue(WerewolfProgression.canPowerHowl(now, Long.MIN_VALUE, false));
        assertFalse(WerewolfProgression.canPowerHowl(now, now - 59_999L, false));
        assertFalse(WerewolfProgression.canPowerHowl(now, now - 60_000L, false));
        assertTrue(WerewolfProgression.canPowerHowl(now, now - 60_001L, false));
        assertTrue(WerewolfProgression.canPowerHowl(now, now, true));
        assertEquals(1200, WerewolfProgression.remainingHowlCooldownTicks(now, now));
        assertEquals(1, WerewolfProgression.remainingHowlCooldownTicks(now, now - 59_999L));
        assertEquals(0, WerewolfProgression.remainingHowlCooldownTicks(now, now - 60_001L));
    }
}
