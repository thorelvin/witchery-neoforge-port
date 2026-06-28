package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeonardPersonaTest {
    @Test
    void leonardPromptSmokeIncludesBossSceneFacts() {
        NpcBrainPrompt prompt = NpcPromptBuilder.leonardSmoke(
                "what do you think of my urn and coven?", "Dev survived one bargain.");
        String user = prompt.userPrompt().toLowerCase();

        assertTrue(user.contains("the shade is named shade of leonard"), user);
        assertTrue(user.contains("player coven 2/6"), user);
        assertTrue(user.contains("nearby altar"), user);
        assertTrue(user.contains("leonard's urn") || user.contains("leonards urn"), user);
        assertTrue(user.contains("player gear"), user);
        assertTrue(user.contains("dev survived one bargain"), user);
    }

    @Test
    void leonardPromptUsesLeonardVoiceAndIntentSet() {
        NpcBrainPrompt prompt = NpcPromptBuilder.leonardSmoke("hi", "");
        String system = prompt.systemPrompt().toLowerCase();

        assertTrue(system.contains("speak as the shade of leonard only"), prompt.systemPrompt());
        assertTrue(system.contains("allowed intents: none, quest_hint, warn_threat, combat_taunt"),
                prompt.systemPrompt());
        assertTrue(system.contains("never claim to be a coven witch"), prompt.systemPrompt());
        assertTrue(system.contains("horned goat-legged sabbath lord"), prompt.systemPrompt());
        assertTrue(system.contains("do not call the player friend"), prompt.systemPrompt());
    }

    @Test
    void leonardFallbackAnswersSpecificBossMechanics() {
        Optional<String> souls = NpcDialogueFallback.leonardChat(
                "how do I deal with lost souls?", "Shade of Leonard", "Dev",
                "shielded by nearby Lost Souls", "no valid altar in range",
                "player coven 0/6; no coven witches nearby", "main hand Iron Sword", "");
        Optional<String> altar = NpcDialogueFallback.leonardChat(
                "how is my altar?", "Shade of Leonard", "Dev",
                "neutral", "valid altar nearby: power 12000/16000, recharge 20/t, range 32, distance 6.0 blocks",
                "player coven 0/6; no coven witches nearby", "main hand Arthana", "");
        Optional<String> arthana = NpcDialogueFallback.leonardChat(
                "what do you think of my arthana?", "Shade of Leonard", "Dev",
                "neutral", "valid altar nearby: power 12000/16000, recharge 20/t, range 32, distance 6.0 blocks",
                "player coven 0/6; no coven witches nearby", "main hand Arthana; offhand Leonard's Urn", "");
        Optional<String> origin = NpcDialogueFallback.leonardChat(
                "where were you summoned from?", "Shade of Leonard", "Dev",
                "neutral", "valid altar nearby: power 12000/16000, recharge 20/t, range 32, distance 6.0 blocks",
                "player coven 0/6; no coven witches nearby", "main hand Arthana", "");
        Optional<String> bargain = NpcDialogueFallback.leonardChat(
                "can we bargain?", "Shade of Leonard", "Dev",
                "neutral", "valid altar nearby: power 12000/16000, recharge 20/t, range 32, distance 6.0 blocks",
                "player coven 0/6; no coven witches nearby", "main hand Arthana", "");

        assertTrue(souls.orElse("").contains("Green"), souls::toString);
        assertTrue(souls.orElse("").contains("red"), souls::toString);
        assertTrue(souls.orElse("").contains("black"), souls::toString);
        assertTrue(altar.orElse("").contains("12000/16000"), altar::toString);
        assertTrue(arthana.orElse("").toLowerCase().contains("arthana"), arthana::toString);
        assertFalse(arthana.orElse("").toLowerCase().contains("urn"), arthana::toString);
        assertTrue(origin.orElse("").toLowerCase().contains("sabbath threshold"), origin::toString);
        assertFalse(bargain.orElse("").toLowerCase().contains("three souls"), bargain::toString);
    }

    @Test
    void leonardPersonaDoesNotAllowTradeOrFollowIntents() {
        assertTrue(LeonardPersona.INSTANCE.allowsIntent(NpcIntent.NONE, null, null));
        assertTrue(LeonardPersona.INSTANCE.allowsIntent(NpcIntent.WARN_THREAT, null, null));
        assertFalse(LeonardPersona.INSTANCE.allowsIntent(NpcIntent.OPEN_TRADE, null, null));
        assertFalse(LeonardPersona.INSTANCE.allowsIntent(NpcIntent.FOLLOW_OWNER, null, null));
    }

    @Test
    void knowledgeBaseRoutesLeonardCard() {
        String context = NpcKnowledgeBase.relevantContext("how do I summon Leonard?", "Plains", "night");

        assertTrue(context.contains("Nether Wart"), context);
        assertTrue(context.contains("10000 altar power"), context);

        String origin = NpcKnowledgeBase.relevantContext("where were you summoned from?", "Plains", "night");

        assertTrue(origin.toLowerCase().contains("sabbath threshold"), origin);
        assertTrue(origin.toLowerCase().contains("goat"), origin);
    }
}
