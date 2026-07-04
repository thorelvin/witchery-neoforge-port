package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    @Test
    void deterministicChatAnalysisSkipsOnlyLowRiskRoutes() {
        assertTrue(NpcBrainService.useDeterministicChatAnalysis(
                NpcMessageAnalysis.deterministic("Dev", "how do I use the kettle?")));
        assertTrue(NpcBrainService.useDeterministicChatAnalysis(
                NpcMessageAnalysis.deterministic("Dev", "hello there")));
        assertTrue(NpcBrainService.useDeterministicChatAnalysis(
                NpcMessageAnalysis.deterministic("Dev", "remember that I fear nightmares")));

        assertFalse(NpcBrainService.useDeterministicChatAnalysis(
                NpcMessageAnalysis.deterministic("Dev", "can you open this for me?")));
    }

    @Test
    void sharedHttpClientsReuseTimeoutBuckets() {
        LlmHttpClients.clear();

        var first = LlmHttpClients.clientFor(3000);
        var sameTimeout = LlmHttpClients.clientFor(3000);
        var differentTimeout = LlmHttpClients.clientFor(1500);

        assertSame(first, sameTimeout);
        assertNotSame(first, differentTimeout);

        LlmHttpClients.clear();
        assertNotSame(first, LlmHttpClients.clientFor(3000));
    }

    @Test
    void prunesExpiredCooldownEntries() {
        ConcurrentHashMap<String, Long> cooldowns = new ConcurrentHashMap<>();
        cooldowns.put("expired", 9L);
        cooldowns.put("boundary", 10L);
        cooldowns.put("future", 11L);

        NpcBrainService.pruneExpiredCooldowns(cooldowns, 10L);

        assertFalse(cooldowns.containsKey("expired"));
        assertFalse(cooldowns.containsKey("boundary"));
        assertTrue(cooldowns.containsKey("future"));
    }

    @Test
    void chatCooldownUsesSeparateConfigBucket() {
        assertEquals(20L, NpcBrainService.cooldownTicks("chat", 20L, 100L));
        assertEquals(100L, NpcBrainService.cooldownTicks("greeting", 20L, 100L));
        assertEquals(100L, NpcBrainService.cooldownTicks("ambient", 20L, 100L));
    }

    @Test
    void parsesExtraHeadersButIgnoresProtectedHeaders() {
        var headers = NpcBrainService.parseExtraHeaders("""
                X-Test: one
                Authorization: Bearer nope
                Content-Type: text/plain
                X-Other: two; malformed
                """);

        assertEquals(2, headers.size());
        assertEquals("one", headers.get("X-Test"));
        assertEquals("two", headers.get("X-Other"));
        assertFalse(headers.containsKey("Authorization"));
        assertFalse(headers.containsKey("Content-Type"));
    }

    @Test
    void statusLineIncludesAndReloadClearsRecentRejections() {
        NpcBrainService service = NpcBrainService.INSTANCE;
        service.reload();

        service.rememberRejection("persona_response:validation:prompt_leak");

        assertTrue(service.statusLine().contains("recentRejections=persona_response:validation:prompt_leak"));

        service.reload();

        assertFalse(service.statusLine().contains("recentRejections="));
    }
}
