package com.emoniph.witchery.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VillageGuardBloodRecoveryTest {
    @Test
    void sourceVillageRecoveryCadenceMatchesOldHomeCheckRange() {
        assertEquals(70, VillageGuardEntity.sourceVillageRecoveryCooldown(-20));
        assertEquals(70, VillageGuardEntity.sourceVillageRecoveryCooldown(0));
        assertEquals(94, VillageGuardEntity.sourceVillageRecoveryCooldown(24));
        assertEquals(119, VillageGuardEntity.sourceVillageRecoveryCooldown(99));
    }

    @Test
    void sourceVillageRecoveryBloodRequiresVillageNoTargetAndSuccessfulRoll() {
        assertEquals(251, VillageGuardEntity.sourceVillageRecoveryBlood(250, true, false, true));
        assertEquals(250, VillageGuardEntity.sourceVillageRecoveryBlood(250, false, false, true));
        assertEquals(250, VillageGuardEntity.sourceVillageRecoveryBlood(250, true, true, true));
        assertEquals(250, VillageGuardEntity.sourceVillageRecoveryBlood(250, true, false, false));
    }

    @Test
    void sourceVillageRecoveryBloodClampsToHumanBloodRange() {
        assertEquals(1, VillageGuardEntity.sourceVillageRecoveryBlood(-50, true, false, true));
        assertEquals(500, VillageGuardEntity.sourceVillageRecoveryBlood(500, true, false, true));
        assertEquals(500, VillageGuardEntity.sourceVillageRecoveryBlood(999, true, false, true));
    }
}
