package com.emoniph.witchery.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class WitcheryConfigTest {
    @Test
    void llmDefaultsAreOptInForReleaseInstalls() {
        assertFalse(WitcheryConfig.LLM_ENABLED.getDefault());
        assertFalse(WitcheryConfig.LLM_PREWARM_ENABLED.getDefault());
    }
}
