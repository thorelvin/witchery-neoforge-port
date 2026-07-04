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

    @Test
    void adapterFailureDeniesProtectedAction() {
        WitcheryProtectionAdapter.Decision decision = WitcheryProtectionService.safeCanModifyBlock(
                new FailingProtectionAdapter(),
                null,
                null,
                null,
                WitcheryProtectionAction.BLOCK_BREAK
        );

        assertEquals(WitcheryProtectionAdapter.Decision.DENY, decision);
    }

    private static final class FailingProtectionAdapter implements WitcheryProtectionAdapter {
        @Override
        public String id() {
            return "failing-test";
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public Decision canModifyBlock(net.minecraft.server.level.ServerPlayer actor,
                                       net.minecraft.server.level.ServerLevel level,
                                       net.minecraft.core.BlockPos pos,
                                       WitcheryProtectionAction action) {
            throw new IllegalStateException("boom");
        }
    }
}
