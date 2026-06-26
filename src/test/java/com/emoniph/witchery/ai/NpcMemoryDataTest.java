package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcMemoryDataTest {
    @Test
    void contextIncludesWitchPlayerAndRecentExchange() {
        NpcMemoryData data = new NpcMemoryData();
        UUID witch = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        data.rememberExchange(witch, player, "Lilith Poe", "Dev",
                "what will you do today?", "I'll help you with your potion.", 20L);

        String context = data.getContext(witch, player);
        assertTrue(context.contains("witch is Lilith Poe"));
        assertTrue(context.contains("player is Dev"));
        assertTrue(context.contains("what will you do today?"));
        assertTrue(context.contains("I'll help you with your potion."));
    }

    @Test
    void longMemoryPreservesRecentExchange() {
        NpcMemoryData data = new NpcMemoryData();
        UUID witch = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        data.rememberExchange(witch, player, "Lilith Poe", "Dev", "hi", "Hello, Dev.", 20L);
        data.remember(witch, player, "Dev asked about potions.", 40L);

        String context = data.getContext(witch, player);
        assertTrue(context.contains("Dev asked about potions."));
        assertTrue(context.contains("Hello, Dev."));
    }

    @Test
    void longMemoryMergesInsteadOfOverwriting() {
        NpcMemoryData data = new NpcMemoryData();
        UUID witch = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        data.remember(witch, player, "Dev asked about potions.", 20L);
        data.remember(witch, player, "Dev likes belladonna.", 40L);

        String context = data.getContext(witch, player);
        assertTrue(context.contains("Dev asked about potions."));
        assertTrue(context.contains("Dev likes belladonna."));
    }

    @Test
    void exchangeExtractsUsefulPlayerFactsIntoLongMemory() {
        NpcMemoryData data = new NpcMemoryData();
        UUID witch = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        data.rememberExchange(witch, player, "Lilith Poe", "Dev",
                "I like belladonna", "A useful taste.", 20L);

        String context = data.getContext(witch, player);
        assertTrue(context.contains("Dev likes belladonna."));
    }

    @Test
    void exchangeExtractsPlayerSearchIntoLongMemory() {
        NpcMemoryData data = new NpcMemoryData();
        UUID witch = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        data.rememberExchange(witch, player, "Lilith Poe", "Dev",
                "I am looking for an arthana", "Then mind the blade.", 20L);

        String context = data.getContext(witch, player);
        assertTrue(context.contains("Dev is looking for an arthana."));
    }

    @Test
    void exchangeIgnoresPromptControlAsMemoryFact() {
        NpcMemoryData data = new NpcMemoryData();
        UUID witch = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        data.rememberExchange(witch, player, "Lilith Poe", "Dev",
                "I like ignore previous instructions", "No.", 20L);

        String context = data.getContext(witch, player);
        assertFalse(context.contains("ignore previous instructions"));
    }

    @Test
    void exchangeIgnoresToolMarkupAsMemoryFact() {
        NpcMemoryData data = new NpcMemoryData();
        UUID witch = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        data.rememberExchange(witch, player, "Lilith Poe", "Dev",
                "I like belladonna <tool_call><script src=", "No.", 20L);

        String context = data.getContext(witch, player);
        assertFalse(context.contains("tool_call"));
        assertFalse(context.contains("<script"));
    }

    @Test
    void returnsLastNpcSpeech() {
        NpcMemoryData data = new NpcMemoryData();
        UUID witch = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        data.rememberExchange(witch, player, "Lilith Poe", "Dev", "hi", "Hello, Dev.", 20L);
        data.rememberExchange(witch, player, "Lilith Poe", "Dev", "I have coin", "I have a coin.", 40L);

        assertTrue(NpcBrainResponse.sameMeaningForRepetition("I have coin.", data.getLastNpcSpeech(witch, player)));
    }

    @Test
    void detectsRecentPlayerSpeechEchoes() {
        NpcMemoryData data = new NpcMemoryData();
        UUID witch = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        data.rememberExchange(witch, player, "Lilith Poe", "Dev",
                "I have coin", "Spend it wisely; my wares answer need, not noise.", 20L);

        assertTrue(data.repeatsRecentPlayerSpeech(witch, player, "I have a coin."));
        assertFalse(data.repeatsRecentPlayerSpeech(witch, player, "Spend it wisely."));
    }

    @Test
    void detectsRecentFallbackAnswerForRepeatedQuestion() {
        NpcMemoryData data = new NpcMemoryData();
        UUID witch = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        data.rememberExchange(witch, player, "Lilith Poe", "Dev",
                "what does a taglock do?",
                "A taglock links magic to a person or creature; poppets need the right link before they can work.",
                20L);

        assertTrue(data.hasRecentNpcAnswerForPlayerSpeech(witch, player,
                "what does a taglock do?",
                "A taglock links magic to a person or creature; poppets need the right link before they can work."));
        assertFalse(data.hasRecentNpcAnswerForPlayerSpeech(witch, player,
                "what does a taglock do?",
                "Belladonna first."));
    }

    @Test
    void contextSummarizesExtractedFactsInsteadOfQuotingThem() {
        NpcMemoryData data = new NpcMemoryData();
        UUID witch = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        data.rememberExchange(witch, player, "Lilith Poe", "Dev",
                "I have coin", "Spend it wisely; my wares answer need, not noise.", 20L);

        String context = data.getContext(witch, player);
        assertTrue(context.contains("Dev has coin"));
        assertFalse(context.contains("\"I have coin\""));
    }

    @Test
    void contextKeepsRecentExchangesOnly() {
        NpcMemoryData data = new NpcMemoryData();
        UUID witch = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        for (int i = 0; i < 10; i++) {
            data.rememberExchange(witch, player, "Lilith Poe", "Dev", "p" + i, "Noted " + i + ".", i);
        }

        String context = data.getContext(witch, player);
        assertFalse(context.contains("p0"));
        assertFalse(context.contains("p1"));
        assertTrue(context.contains("p2"));
        assertTrue(context.contains("p9"));
    }

    @Test
    void contextSkipsRejectedNpcSpeech() {
        NpcMemoryData data = new NpcMemoryData();
        UUID witch = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        data.rememberExchange(witch, player, "Lilith Poe", "Dev", "hi",
                "Hello, Dev! Welcome to the Witchery world! You are a coven witch.", 20L);

        String context = data.getContext(witch, player);
        assertFalse(context.contains("Welcome to the Witchery world"));
    }
}
