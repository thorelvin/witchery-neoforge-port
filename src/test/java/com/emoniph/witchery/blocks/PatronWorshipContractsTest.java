package com.emoniph.witchery.blocks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatronWorshipContractsTest {
    @Test
    void sourceWorshipThresholdsAreExact() {
        assertFalse(PatronWorshipContracts.canManuallySummon(4));
        assertTrue(PatronWorshipContracts.canManuallySummon(5));
        assertFalse(PatronWorshipContracts.canRechargeInfusion(4));
        assertTrue(PatronWorshipContracts.canRechargeInfusion(5));
        assertEquals(-1, PatronWorshipContracts.worshipAmplifier(9));
        assertEquals(0, PatronWorshipContracts.worshipAmplifier(10));
        assertEquals(1, PatronWorshipContracts.worshipAmplifier(15));
    }

    @Test
    void sourceAutomaticSummonChanceUsesConfiguredPercent() {
        assertEquals(0.0D, PatronWorshipContracts.automaticSummonChance(-1));
        assertEquals(0.001D, PatronWorshipContracts.automaticSummonChance(10), 0.0000001D);
        assertEquals(0.01D, PatronWorshipContracts.automaticSummonChance(100), 0.0000001D);
        assertEquals(0.01D, PatronWorshipContracts.automaticSummonChance(101), 0.0000001D);
    }
}
