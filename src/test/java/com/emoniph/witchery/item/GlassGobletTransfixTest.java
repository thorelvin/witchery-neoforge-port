package com.emoniph.witchery.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlassGobletTransfixTest {
    @Test
    void conversionRequiresSourceParalysisAmplifier() {
        assertTrue(GlassGobletItem.sourceTransfixState(true, 5));
        assertFalse(GlassGobletItem.sourceTransfixState(true, 4));
        assertFalse(GlassGobletItem.sourceTransfixState(false, 5));
    }
}
