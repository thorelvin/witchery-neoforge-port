package com.emoniph.witchery.events;

import com.emoniph.witchery.data.WitcheryPlayerData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VampirePowerParityTest {
    @Test
    void sourcePowerLadderCostsAndUnlocksAreExact() {
        assertEquals(1, WitcheryPlayerData.VampirePower.DRINK.unlockLevel());
        assertEquals(50, WitcheryPlayerData.VampirePower.MESMERIZE.initialCost());
        assertEquals(10, WitcheryPlayerData.VampirePower.SPEED.initialCost());
        assertEquals(50, WitcheryPlayerData.VampirePower.BAT.initialCost());
        assertEquals(1, WitcheryPlayerData.VampirePower.BAT.upkeepCost());
        assertEquals(10, WitcheryPlayerData.VampirePower.ULTIMATE.unlockLevel());

        int[] expectedMaxOrdinals = {0, 1, 2, 2, 3, 3, 3, 4, 4, 4, 5};
        for (int level = 0; level < expectedMaxOrdinals.length; level++) {
            assertEquals(expectedMaxOrdinals[level],
                    WitcheryPlayerData.VampirePower.maxOrdinalForLevel(level));
        }
    }

    @Test
    void becomingAndLeavingAVampireResetsSourceState() {
        WitcheryPlayerData data = new WitcheryPlayerData();
        data.setHumanBlood(500);
        data.setVampire(true);
        assertEquals(1, data.getVampireLevel());
        assertEquals(125, data.getBloodPower());
        assertEquals(0, data.getHumanBlood());

        data.setVampireLevel(10);
        data.setSelectedVampirePower(WitcheryPlayerData.VampirePower.ULTIMATE);
        data.setVampireUltimate(WitcheryPlayerData.VampireUltimate.SWARM, 5);
        data.setVampireBatForm(true);
        data.setVampireVisionActive(true);
        data.setVampirePowerCooldown(10);
        data.setVampire(false);

        assertEquals(0, data.getVampireLevel());
        assertEquals(0, data.getBloodPower());
        assertEquals(50, data.getHumanBlood());
        assertEquals(0, data.getVampireUltimateCharges());
        assertEquals(WitcheryPlayerData.VampirePower.NONE, data.getSelectedVampirePower());
        assertFalse(data.isVampireBatForm());
        assertFalse(data.isVampireVisionActive());
        assertEquals(0, data.getVampirePowerCooldown());
    }

    @Test
    void mortalHumanBloodUsesSourceAwakeDrainAndRecoveryRules() {
        WitcheryPlayerData data = new WitcheryPlayerData();
        data.setHumanBlood(500);

        assertEquals(7, data.takeHumanBlood(10, false));
        assertEquals(493, data.getHumanBlood());
        assertEquals(10, data.takeHumanBlood(10, true));
        assertEquals(483, data.getHumanBlood());

        data.giveHumanBlood(2);
        assertEquals(485, data.getHumanBlood());
        data.giveHumanBlood(100);
        assertEquals(500, data.getHumanBlood());
    }

    @Test
    void lockedSelectionsAndBatFormAreRejected() {
        WitcheryPlayerData data = new WitcheryPlayerData();
        data.setVampire(true);
        data.setVampireLevel(6);
        data.setSelectedVampirePower(WitcheryPlayerData.VampirePower.BAT);
        data.setVampireBatForm(true);
        assertEquals(WitcheryPlayerData.VampirePower.NONE, data.getSelectedVampirePower());
        assertFalse(data.isVampireBatForm());

        data.setVampireLevel(7);
        data.setSelectedVampirePower(WitcheryPlayerData.VampirePower.BAT);
        data.setVampireBatForm(true);
        assertEquals(WitcheryPlayerData.VampirePower.BAT, data.getSelectedVampirePower());
        assertTrue(data.isVampireBatForm());
    }

    @Test
    void exactAndPartialBloodPowerSpendingMatchSource() {
        WitcheryPlayerData data = new WitcheryPlayerData();
        data.setVampire(true);
        data.setBloodPower(30);
        assertFalse(data.spendBloodPower(60, true));
        assertEquals(30, data.getBloodPower());
        assertTrue(data.spendBloodPower(60, false));
        assertEquals(0, data.getBloodPower());
        assertFalse(data.spendBloodPower(60, false));
    }

    @Test
    void mesmeriseDurationAndLureAmplifierMatchSourceLevels() {
        assertEquals(6 * 20, VampireEvents.sourceMesmeriseDuration(2, false));
        assertEquals(7 * 20, VampireEvents.sourceMesmeriseDuration(4, false));
        assertEquals(11 * 20, VampireEvents.sourceMesmeriseDuration(8, false));
        assertEquals(14 * 20, VampireEvents.sourceMesmeriseDuration(8, true));
        assertEquals(13 * 20, VampireEvents.sourceMesmeriseDuration(10, false));
        assertEquals(4, VampireEvents.sourceMesmeriseAmplifier(7));
        assertEquals(5, VampireEvents.sourceMesmeriseAmplifier(8));
    }

    @Test
    void mesmeriseTargetsMatchSourceEntityClasses() {
        assertTrue(VampireEvents.sourceMesmeriseTarget(true, false, false));
        assertTrue(VampireEvents.sourceMesmeriseTarget(false, true, false));
        assertTrue(VampireEvents.sourceMesmeriseTarget(false, false, true));
        assertFalse(VampireEvents.sourceMesmeriseTarget(false, false, false));
    }

    @Test
    void creatureFeedingStopsAtQuarterBloodPowerCapacity() {
        assertEquals(2, VampireEvents.sourceCreatureBloodPowerGain(0, 500));
        assertEquals(1, VampireEvents.sourceCreatureBloodPowerGain(124, 500));
        assertEquals(0, VampireEvents.sourceCreatureBloodPowerGain(125, 500));
    }

    @Test
    void fullVillagerBiteUsesSourceTransfixAmplifier() {
        assertFalse(VampireEvents.sourceVampireTransfixAmplifier(3));
        assertTrue(VampireEvents.sourceVampireTransfixAmplifier(4));
        assertTrue(VampireEvents.sourceVampireTransfixAmplifier(5));
    }

    @Test
    void speedStackingUsesTheSourceLogarithmicGate() {
        assertEquals(0, VampireEvents.sourceSpeedStage(-1));
        assertEquals(0, VampireEvents.sourceSpeedStage(0));
        assertEquals(1, VampireEvents.sourceSpeedStage(1));
        assertEquals(2, VampireEvents.sourceSpeedStage(2));
        assertEquals(2, VampireEvents.sourceSpeedStage(3));
        assertEquals(3, VampireEvents.sourceSpeedStage(5));

        assertEquals(1, VampireEvents.sourceMaxSpeedStage(4));
        assertEquals(2, VampireEvents.sourceMaxSpeedStage(7));
        assertEquals(4, VampireEvents.sourceMaxSpeedStage(10));
    }
}
