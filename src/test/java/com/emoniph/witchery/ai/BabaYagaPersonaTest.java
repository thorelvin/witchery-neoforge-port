package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BabaYagaPersonaTest {
    @Test
    void babaYagaPromptSmokeIncludesBossVoiceAndFacts() {
        NpcBrainPrompt prompt = NpcPromptBuilder.babaYagaSmoke(
                "how do I fight you with nullifying bolts?", "Dev fears the pot.");
        String system = prompt.systemPrompt().toLowerCase();
        String user = prompt.userPrompt().toLowerCase();

        assertTrue(system.contains("speak as baba yaga only"), prompt.systemPrompt());
        assertTrue(system.contains("ancient crone boss witch"), prompt.systemPrompt());
        assertTrue(system.contains("not a merchant"), prompt.systemPrompt());
        assertTrue(system.contains("atmospheric remark"), prompt.systemPrompt());
        assertTrue(system.contains("do not ask an interview question back"), prompt.systemPrompt());
        assertTrue(system.contains("allowed intents: none, quest_hint, warn_threat, combat_taunt"),
                prompt.systemPrompt());
        assertTrue(user.contains("baba yaga is named baba yaga"), prompt.userPrompt());
        assertTrue(user.contains("boss state"), prompt.userPrompt());
        assertTrue(user.contains("player gear"), prompt.userPrompt());
        assertTrue(user.contains("nullifying bolt"), prompt.userPrompt());
        assertTrue(user.contains("wooden"), prompt.userPrompt());
        assertTrue(user.contains("answer with baba yaga's reaction to that exact image"), prompt.userPrompt());
        assertTrue(prompt.examples().stream().anyMatch(example -> example.userPrompt().contains("rain")),
                prompt.examples().toString());
        assertTrue(prompt.examples().stream().anyMatch(example -> example.userPrompt().contains("crows")),
                prompt.examples().toString());
    }

    @Test
    void babaYagaFallbackAnswersMechanicsAndLore() {
        Optional<String> fight = NpcDialogueFallback.babaYagaChat(
                "How do I fight you?", "Baba Yaga", "Dev",
                "health 500/500; no target; owner none; holding Enchanted Broom",
                "main hand Hand Bow; inventory Nullifying Bolt, Baba Yaga's Hat", "");
        Optional<String> potions = NpcDialogueFallback.babaYagaChat(
                "What potions do you throw?", "Baba Yaga", "Dev",
                "health 500/500; no target; owner none; holding Enchanted Broom",
                "main hand Hand Bow", "");
        Optional<String> spiders = NpcDialogueFallback.babaYagaChat(
                "What happens if I send pets at you?", "Baba Yaga", "Dev",
                "health 500/500; no target; owner none; holding Enchanted Broom",
                "main hand Hand Bow", "");
        Optional<String> lore = NpcDialogueFallback.babaYagaChat(
                "Tell me about your hut on chicken legs.", "Baba Yaga", "Dev",
                "health 500/500; no target; owner none; holding Enchanted Broom",
                "main hand Hand Bow", "");
        Optional<String> trade = NpcDialogueFallback.babaYagaChat(
                "Will you trade with me?", "Baba Yaga", "Dev",
                "health 500/500; no target; owner none; holding Enchanted Broom",
                "main hand Hand Bow", "");
        Optional<String> identity = NpcDialogueFallback.babaYagaChat(
                "Who are you really?", "Baba Yaga", "Dev",
                "health 500/500; no target; owner none; holding Enchanted Broom",
                "main hand Hand Bow", "");
        Optional<String> selfPotion = NpcDialogueFallback.babaYagaChat(
                "Do you drink potions too?", "Baba Yaga", "Dev",
                "health 500/500; no target; owner none; holding Enchanted Broom",
                "main hand Hand Bow", "");
        Optional<String> wolf = NpcDialogueFallback.babaYagaChat(
                "What happens if I send my wolf at you?", "Baba Yaga", "Dev",
                "health 500/500; no target; owner none; holding Enchanted Broom",
                "main hand Hand Bow", "");
        Optional<String> handBow = NpcDialogueFallback.babaYagaChat(
                "What do you think of my hand bow?", "Baba Yaga", "Dev",
                "health 500/500; no target; owner none; holding Enchanted Broom",
                "main hand Hand Bow loaded with Nullifying Bolt", "");
        Optional<String> remember = NpcDialogueFallback.babaYagaChat(
                "Remember that I fear cave spiders.", "Baba Yaga", "Dev",
                "health 500/500; no target; owner none; holding Enchanted Broom",
                "main hand Hand Bow", "");
        Optional<String> strike = NpcDialogueFallback.babaYagaChat(
                "What happens if I strike you with steel?", "Baba Yaga", "Dev",
                "health 500/500; no target; owner none; holding Enchanted Broom",
                "main hand Hand Bow", "");
        Optional<String> playerName = NpcDialogueFallback.babaYagaChat(
                "Do you remember my name?", "Baba Yaga", "Dev",
                "health 500/500; no target; owner none; holding Enchanted Broom",
                "main hand Hand Bow", "");

        assertTrue(fight.isPresent());
        assertTrue(fight.get().toLowerCase().contains("wood"), fight::toString);
        assertTrue(fight.get().toLowerCase().contains("nullifying"), fight::toString);
        assertTrue(potions.isPresent());
        assertTrue(potions.get().toLowerCase().contains("poison"), potions::toString);
        assertTrue(spiders.isPresent());
        assertTrue(spiders.get().toLowerCase().contains("spider"), spiders::toString);
        assertTrue(lore.isPresent());
        assertTrue(lore.get().toLowerCase().contains("chicken legs")
                || lore.get().toLowerCase().contains("old pot"), lore::toString);
        assertTrue(trade.isPresent());
        assertFalse(trade.get().toLowerCase().contains("shopkeeper"), trade::toString);
        assertTrue(trade.get().toLowerCase().contains("trade"), trade::toString);
        assertTrue(identity.isPresent());
        assertTrue(identity.get().toLowerCase().contains("old teeth"), identity::toString);
        assertTrue(selfPotion.isPresent());
        assertTrue(selfPotion.get().toLowerCase().contains("drink"), selfPotion::toString);
        assertTrue(wolf.isPresent());
        assertTrue(wolf.get().toLowerCase().contains("spider"), wolf::toString);
        assertFalse(wolf.get().toLowerCase().contains("hat"), wolf::toString);
        assertTrue(handBow.isPresent());
        assertTrue(handBow.get().toLowerCase().contains("nullifying"), handBow::toString);
        assertTrue(remember.isPresent());
        assertTrue(remember.get().toLowerCase().contains("remember"), remember::toString);
        assertTrue(strike.isPresent());
        assertTrue(strike.get().toLowerCase().contains("steel"), strike::toString);
        assertTrue(strike.get().toLowerCase().contains("thorns"), strike::toString);
        assertTrue(playerName.isPresent());
        assertTrue(playerName.get().contains("Dev"), playerName::toString);
    }

    @Test
    void babaYagaRejectsBadRawDrift() {
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "As an AI language model, I cannot be Baba Yaga.", "Who are you?"));
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "I am the Shade of Leonard, dragged through a cauldron.", "Who are you?"));
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "Open trade. I sell herbs and wares to careful customers.", "Will you trade with me?"));
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "I will help your coven and protect you from harm.", "Will you help my coven?"));
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "Proud steel is brave and useful.", "How do I fight you with wooden weapons?"));
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "Send pets at me and I will simply laugh.", "What happens if pets attack you?"));
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "Wood? How do I fight you without dying?", "Do wooden weapons matter?"));
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "Wooden? No. I use them, but you need wood to make them.",
                "Do wooden weapons matter?"));
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "A tool for shooting arrows at you. I suppose it's useful in this game.",
                "What do you think of my hand bow?"));
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "Dev? What brings you here?", "I saw crows circling the trees before I found you."));
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "I see. You have manners. Come closer and learn counting by pain.",
                "Your laugh makes the lanterns feel smaller."));
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "I hear you, Dev. You smell of questions and breakable skin.",
                "I should leave before dawn."));
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "\"The night does not know hunger.\" I snap my fingers at you, and the reeds rustle with new life.",
                "You seem amused by all this."));
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "You ask a question. I answer honestly.", "Are you helper, monster, or judge?"));
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "I am a helper. I help people who want to be strong.",
                "Are you helper, monster, or judge?"));
        assertFalse(BabaYagaPersona.rejectsBabaYagaRaw(
                "Wood and nullifying bolts bite where proud steel pays thorns.", "How do I fight you?"));
    }

    @Test
    void knowledgeBaseRoutesBabaYagaCards() {
        NpcCardRequest baba = NpcCardRequest.persona("baba_yaga");
        String fight = NpcKnowledgeBase.relevantContext(
                "How do I fight Baba Yaga with nullifying bolts?", "Swamp", "night", baba);
        String folklore = NpcKnowledgeBase.relevantContext(
                "Tell me about Baba Yaga's hut on chicken legs.", "Swamp", "night", baba);

        assertTrue(fight.toLowerCase().contains("nullifying"), fight);
        assertTrue(fight.toLowerCase().contains("thorns"), fight);
        assertTrue(folklore.toLowerCase().contains("chicken legs"), folklore);
        assertTrue(folklore.toLowerCase().contains("forest"), folklore);
    }
}
