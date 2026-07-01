package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcBrainServiceTest {
    @Test
    void acceptsOnlyPlainYesForSocialIntentClassifier() {
        assertTrue(NpcBrainService.socialIntentAccepted(new NpcBrainResponse("yes", NpcIntent.NONE, "")));
        assertTrue(NpcBrainService.socialIntentAccepted(new NpcBrainResponse("true", NpcIntent.NONE, "")));
        assertFalse(NpcBrainService.socialIntentAccepted(new NpcBrainResponse("probably yes", NpcIntent.NONE, "")));
        assertFalse(NpcBrainService.socialIntentAccepted(new NpcBrainResponse("no", NpcIntent.NONE, "")));
        assertFalse(NpcBrainService.socialIntentAccepted(null));
    }

    @Test
    void commandIntentUsesSameStrictAcceptance() {
        assertTrue(NpcBrainService.commandIntentAccepted(new NpcBrainResponse("yes", NpcIntent.NONE, "")));
        assertTrue(NpcBrainService.commandIntentAccepted(new NpcBrainResponse("\"yes\"", NpcIntent.NONE, "")));
        assertFalse(NpcBrainService.commandIntentAccepted(new NpcBrainResponse("yes, master", NpcIntent.NONE, "")));
        assertFalse(NpcBrainService.commandIntentAccepted(new NpcBrainResponse("no", NpcIntent.NONE, "")));
        assertFalse(NpcBrainService.commandIntentAccepted(null));
    }
}
