package com.emoniph.witchery.protection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class WitcheryProtectionServiceTest {

    @Test
    void denyWinsOverEarlierAllow() {
        WitcheryProtectionAdapter.Decision merged = WitcheryProtectionService.mergeDecision(
                WitcheryProtectionAdapter.Decision.ALLOW,
                WitcheryProtectionAdapter.Decision.DENY
        );

        assertEquals(WitcheryProtectionAdapter.Decision.DENY, merged);
    }

    @Test
    void passDoesNotEraseAllow() {
        WitcheryProtectionAdapter.Decision merged = WitcheryProtectionService.mergeDecision(
                WitcheryProtectionAdapter.Decision.ALLOW,
                WitcheryProtectionAdapter.Decision.PASS
        );

        assertEquals(WitcheryProtectionAdapter.Decision.ALLOW, merged);
    }

    @Test
    void optionalAdaptersDoNotRequireOptionalModsOnClasspath() {
        assertDoesNotThrow(() -> new OpenPacProtectionAdapter().isAvailable());
        assertDoesNotThrow(() -> new FtbChunksProtectionAdapter().isAvailable());
        assertDoesNotThrow(() -> new CommonProtectionApiAdapter().isAvailable());
    }
}
