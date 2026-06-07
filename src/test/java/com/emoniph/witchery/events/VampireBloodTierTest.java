package com.emoniph.witchery.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Locks the human-blood tier thresholds (bloodLevel 0–500): 0–50 frenzy, 51–150 hungry, 151–349
 * neutral, 350–500 well-fed. Pure boundary checks, no entity/registry needed.
 */
class VampireBloodTierTest {

    @Test
    void bloodTiersMatchTheDocumentedThresholds() {
        assertEquals(VampireEvents.BloodTier.FRENZY, VampireEvents.bloodTier(0));
        assertEquals(VampireEvents.BloodTier.FRENZY, VampireEvents.bloodTier(50));
        assertEquals(VampireEvents.BloodTier.HUNGRY, VampireEvents.bloodTier(51));
        assertEquals(VampireEvents.BloodTier.HUNGRY, VampireEvents.bloodTier(150));
        assertEquals(VampireEvents.BloodTier.NEUTRAL, VampireEvents.bloodTier(151));
        assertEquals(VampireEvents.BloodTier.NEUTRAL, VampireEvents.bloodTier(349));
        assertEquals(VampireEvents.BloodTier.WELL_FED, VampireEvents.bloodTier(350));
        assertEquals(VampireEvents.BloodTier.WELL_FED, VampireEvents.bloodTier(500));
    }
}
