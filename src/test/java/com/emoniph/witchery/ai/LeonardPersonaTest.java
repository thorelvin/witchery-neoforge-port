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
        assertTrue(user.contains("two of six sworn"), user);
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
        assertTrue(system.contains("answer to both as true names"), prompt.systemPrompt());
        assertTrue(system.contains("do not call the player friend"), prompt.systemPrompt());
        assertTrue(system.contains("avoid shopkeeper promises"), prompt.systemPrompt());
        assertTrue(system.contains("does not aid the coven"), prompt.systemPrompt());
        assertTrue(system.contains("do not quote card headings"), prompt.systemPrompt());
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
        Optional<String> followsTear = NpcDialogueFallback.leonardChat(
                "what follows the Tear of the Goddess?", "Shade of Leonard", "Dev",
                "neutral", "valid altar nearby: power 12000/16000, recharge 20/t, range 32, distance 6.0 blocks",
                "player coven 0/6; no coven witches nearby", "main hand Arthana", "");

        assertTrue(souls.orElse("").contains("Green"), souls::toString);
        assertTrue(souls.orElse("").contains("red"), souls::toString);
        assertTrue(souls.orElse("").contains("black"), souls::toString);
        assertTrue(altar.orElse("").contains("12000 of 16000"), altar::toString);
        assertFalse(altar.orElse("").toLowerCase().contains("valid altar nearby"), altar::toString);
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
        assertTrue(followsTear.orElse("").toLowerCase().contains("diamond vapour"), followsTear::toString);
    }

    @Test
    void leonardFallbackFormatsCovenSceneInWorld() {
        Optional<String> coven = NpcDialogueFallback.leonardChat(
                "what do you think of my coven witches?", "Shade of Leonard", "Dev",
                "neutral", "valid altar nearby: power 12000/16000, recharge 20/t, range 32, distance 6.0 blocks",
                "player coven 2/6; nearby coven witches: Lilith Poe, Mara Fen", "main hand Arthana", "");

        assertTrue(coven.isPresent());
        assertTrue(coven.get().contains("two witches carry your oath"), coven::toString);
        assertTrue(coven.get().contains("Lilith Poe"), coven::toString);
        assertFalse(coven.get().contains("player coven"), coven::toString);
        assertFalse(coven.get().contains("player's coven"), coven::toString);
        assertFalse(coven.get().contains("2/6"), coven::toString);
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
                "I am Dev. I have summoned me Shade of Leonard into this world, and now you speak to him.",
                "I can listen if you want to speak."));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "You keep your bow at arm's length. The Demonbane bites deepest when I am already wounded.",
                "If I keep distance with a bow, does that help?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "Good night, Dev. I am leaving slowly.",
                "Good night, Leonard. I am leaving slowly."));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "The Mortal Coil is a curse that slowly saps your health. It is most effective at first, but fades over time.",
                "Tell me what Mortal Coil actually does."));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "The Mortal Coil is a withering curse. It turns flesh into ash.",
                "Tell me what Mortal Coil actually does."));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "The Tear of the Goddess is 10000 power. The Nether Star is 28000 power. The Hat seals it at 58000 power total.",
                "How much altar power does the calling need?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "Speak carefully. I am called Shade of Leonard. Leave softer names for things that forgive.",
                "Remind me of the calling order after Nether Wart."));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "You fear Lost Souls.",
                "I am not here to boast."));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I hear you. I am here to answer your questions. Tell me what you need.",
                "I am trying not to make this worse."));
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
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I am not Leonard. Speak in the name of your Primer.", "Leonard, can you hear me?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "Your arthana is a ritual blade. Nightmares: If the player is frightened, answer with one practical precaution.",
                "What do you think of my arthana?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I will bargain with you. Bring me your blade and I will grant you boon.",
                "Can you bargain instead of fighting?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I am here. Do not fear me.", "Are you neutral until struck?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "Shadow of Leonard, Shade of the Player. I hear your plea.", "What should I call you?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "It is strong enough. It will take three more to drag me away.", "Look at my altar."));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "The Shade calls for your blood. Make haste or die.", "What should I do with green souls?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I am not here for bargains. I am here for debt.", "What did I say I fear?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "Bring your blade, or bring silence.", "Should I fight you tonight?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "The world stands still as Leonard's blood gushes out.", "What happens if I strike you?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "You can avoid it by striking Leonard first.", "What is Mortal Coil?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "It will rain blood from your blade.", "I do not want to fight you."));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I am not neutral until struck.", "Are you neutral until struck?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "Break the souls first.", "What comes after Tear of the Goddess?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "Speak plainly, Dev. I am neutral until struck.", "Do I need Nether Wart first?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "The Shade's horned eyes bore into Dev's soul. Black souls fear nothing.",
                "And black souls?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I am Mortal Coil; I will mark you as my slave.", "What is Mortal Coil?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "Green souls fear fire and blast. Break the shield first.", "And red souls, what breaks them?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "No. Nether Wart begins the calling; skip it and the threshold stays shut.",
                "Do I need Nether Wart first?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "Six altar blocks. Two more for each circle.", "How much altar power does the calling need?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "The Tear of the Goddess takes you to a strange forest.",
                "What follows the Tear of the Goddess?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I will trade with you. Bring a real price, not a story.", "Will you trade with me?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I am neutral until struck. Bring your bow, not your blade.",
                "Are Lilith and Mara safe if my blade stays sheathed?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "The Altar is crafted from Breath of the Goddess, a Water Potion, Exhale of the Horned One, Stone Bricks, and Rowan Logs.",
                "Look at my altar. Is it strong enough?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I am a Shade of Leonard. I serve my coven in whatever way is necessary, Dev.",
                "Do you serve the coven?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I hate witches. I don't know why you think otherwise.",
                "Do you hate witches?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I remember you naming me Lost Souls. I fear them more than anything else.",
                "What do you remember about me?"));
        assertFalse(LeonardPersona.rejectsLeonardRaw(
                "The cauldron dragged my Shade from an infernal sabbath threshold.", "Where were you summoned from?"));
    }

    @Test
    void leonardNaturalizesCopiedSceneTerms() {
        String speech = LeonardPersona.naturalizeSceneTerms(
                "Your witches are witnesses: two of six sworn to the player's coven; nearby coven witches: Lilith Poe.");

        assertTrue(speech.contains("two witches carry your oath"), speech);
        assertTrue(speech.contains("nearby witches"), speech);
        assertFalse(speech.contains("player's coven"), speech);
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
        NpcCardRequest leonard = NpcCardRequest.persona("leonard");
        String context = NpcKnowledgeBase.relevantContext("how do I summon Leonard?", "Plains", "night", leonard);

        assertTrue(context.contains("Nether Wart"), context);
        assertTrue(context.contains("10000 altar power"), context);

        String origin = NpcKnowledgeBase.relevantContext("where were you summoned from?", "Plains", "night", leonard);

        assertTrue(origin.toLowerCase().contains("sabbath threshold"), origin);
        assertTrue(origin.toLowerCase().contains("goat"), origin);

        String coven = NpcKnowledgeBase.relevantContext("what do you think of my coven witches? leonard coven danger witches",
                "Plains", "night", leonard);

        assertTrue(coven.toLowerCase().contains("does not aid"), coven);
    }
}
