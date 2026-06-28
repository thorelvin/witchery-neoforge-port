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
        assertTrue(system.contains("do not say i will accept your trade"), prompt.systemPrompt());
        assertTrue(system.contains("do not say prayers summoned leonard"), prompt.systemPrompt());
        assertTrue(system.contains("do not mention game logic"), prompt.systemPrompt());
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
        Optional<String> call = NpcDialogueFallback.leonardChat(
                "what should I call you?", "Shade of Leonard", "Dev",
                "neutral", "valid altar nearby: power 12000/16000, recharge 20/t, range 32, distance 6.0 blocks",
                "player coven 0/6; no coven witches nearby", "main hand Arthana", "");
        Optional<String> redSoul = NpcDialogueFallback.leonardChat(
                "how do I break red souls?", "Shade of Leonard", "Dev",
                "neutral", "valid altar nearby: power 12000/16000, recharge 20/t, range 32, distance 6.0 blocks",
                "player coven 0/6; no coven witches nearby", "main hand Arthana", "");
        Optional<String> blackSoul = NpcDialogueFallback.leonardChat(
                "and black souls?", "Shade of Leonard", "Dev",
                "neutral", "valid altar nearby: power 12000/16000, recharge 20/t, range 32, distance 6.0 blocks",
                "player coven 0/6; no coven witches nearby", "main hand Arthana", "");
        Optional<String> strike = NpcDialogueFallback.leonardChat(
                "what happens if I strike you?", "Shade of Leonard", "Dev",
                "neutral", "valid altar nearby: power 12000/16000, recharge 20/t, range 32, distance 6.0 blocks",
                "player coven 0/6; no coven witches nearby", "main hand Arthana", "");
        Optional<String> finalWarning = NpcDialogueFallback.leonardChat(
                "give me a final warning before I leave", "Shade of Leonard", "Dev",
                "neutral", "valid altar nearby: power 12000/16000, recharge 20/t, range 32, distance 6.0 blocks",
                "player coven 0/6; no coven witches nearby", "main hand Arthana", "");
        Optional<String> mistake = NpcDialogueFallback.leonardChat(
                "are you just waiting for me to make a mistake?", "Shade of Leonard", "Dev",
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
        assertTrue(call.orElse("").toLowerCase().contains("call me"), call::toString);
        assertTrue(redSoul.orElse("").toLowerCase().contains("red"), redSoul::toString);
        assertTrue(redSoul.orElse("").toLowerCase().contains("fire"), redSoul::toString);
        assertTrue(blackSoul.orElse("").toLowerCase().contains("black"), blackSoul::toString);
        assertTrue(blackSoul.orElse("").toLowerCase().contains("magic"), blackSoul::toString);
        assertTrue(strike.orElse("").toLowerCase().contains("mortal coil"), strike::toString);
        assertTrue(finalWarning.orElse("").toLowerCase().contains("souls"), finalWarning::toString);
        assertTrue(mistake.orElse("").toLowerCase().contains("mistakes"), mistake::toString);
    }

    @Test
    void leonardRejectsBadRawDriftFromSmallModel() {
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I am Leonard. I will accept your trade, Dev.", "Can we make a bargain instead of fighting?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I am a Witcher of the Coven of Dev. You can join our ranks by offering us a trade.",
                "What do you know about my coven witches?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I was dragged by the game's logic.", "Why do you have horns?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "The Shade of Leonard:", "What did my altar pay for?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "The Shade of Leonard: The urn is an ancient tool for shadows.", "What does Leonard's Urn do?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "The altar has gathered 12000 power points and recharges in 20 turns.", "What did my altar pay for?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I am neutral until struck. Do not ask me to bless arithmetic.", "Can we make a bargain?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I hear you. I am Leonard of Infernal Trade, but do not call me by name.", "Leonard, can you hear me?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I am Shade of Leonard, brought here by your prayers to pay a debt.", "What should I call you?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I have come to aid your coven. My summoning is a boon for your witches.",
                "What do you think of the witches who helped summon you?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "Lest your soul be lost to the night.", "How do Lost Souls protect you?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "Speak plainly, Dev. I am here.", "Could a careful witch learn from you?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "Leonard's Shade, I see.", "I do not want to fight you."));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I am summoned here by your cauldron, Dev. Speak plainly.", "Where were you summoned from?"));
        assertFalse(LeonardPersona.rejectsLeonardRaw(
                "The cauldron dragged my Shade from an infernal sabbath threshold.", "Where were you summoned from?"));
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

        String coven = NpcKnowledgeBase.relevantContext("what do you think of my coven witches? leonard coven danger witches",
                "Plains", "night");

        assertTrue(coven.toLowerCase().contains("does not aid"), coven);
    }
}
