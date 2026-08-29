package com.emoniph.witchery.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MysticBranchChannelStateTest {

    @Test
    void requiresMatchingLiveNonceAndRejectsReplay() {
        WitcheryPlayerData data = new WitcheryPlayerData();
        data.beginMysticBranchChannel(42L, 120L);

        assertEquals(WitcheryPlayerData.MysticBranchPreparationResult.NO_SESSION,
                data.validateMysticBranchPreparation(41L, 100L));
        assertEquals(WitcheryPlayerData.MysticBranchPreparationResult.ACCEPT,
                data.validateMysticBranchPreparation(42L, 100L));
        data.acceptMysticBranchPreparation();
        assertEquals(WitcheryPlayerData.MysticBranchPreparationResult.REPLAYED,
                data.validateMysticBranchPreparation(42L, 104L));
    }

    @Test
    void expiresAndRateLimitsPreparationPackets() {
        WitcheryPlayerData data = new WitcheryPlayerData();
        data.beginMysticBranchChannel(7L, 120L);
        assertEquals(WitcheryPlayerData.MysticBranchPreparationResult.ACCEPT,
                data.validateMysticBranchPreparation(7L, 100L));
        assertEquals(WitcheryPlayerData.MysticBranchPreparationResult.RATE_LIMITED,
                data.validateMysticBranchPreparation(7L, 103L));
        assertEquals(WitcheryPlayerData.MysticBranchPreparationResult.ACCEPT,
                data.validateMysticBranchPreparation(7L, 104L));
        assertEquals(WitcheryPlayerData.MysticBranchPreparationResult.EXPIRED,
                data.validateMysticBranchPreparation(7L, 121L));
        assertFalse(data.hasActiveMysticBranchChannel());
        assertEquals(-1, data.getPreparedSpellId());
    }

    @Test
    void clearInvalidatesPreparedStateAndNonce() {
        WitcheryPlayerData data = new WitcheryPlayerData();
        data.beginMysticBranchChannel(9L, 120L);
        data.setPreparedSpell(3, 2);
        data.clearMysticBranchChannel();

        assertEquals(-1, data.getPreparedSpellId());
        assertEquals(WitcheryPlayerData.MysticBranchPreparationResult.NO_SESSION,
                data.validateMysticBranchPreparation(9L, 100L));
    }
}
