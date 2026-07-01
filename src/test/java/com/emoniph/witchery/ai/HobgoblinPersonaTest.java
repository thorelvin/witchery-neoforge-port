package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HobgoblinPersonaTest {
    @Test
    void hobgoblinPromptSmokeIncludesCreatureVoiceAndFacts() {
        NpcBrainPrompt prompt = NpcPromptBuilder.hobgoblinSmoke(
                "what do you know about Koboldite and the patron statue?", "");
        String system = prompt.systemPrompt().toLowerCase();
        String user = prompt.userPrompt().toLowerCase();

        assertTrue(system.contains("speak as the hobgoblin only"), prompt.systemPrompt());
        assertTrue(system.contains("small forest hobgoblin"), prompt.systemPrompt());
        assertTrue(system.contains("prefer 4 to 16 words"), prompt.systemPrompt());
        assertTrue(system.contains("never claim to be a witch"), prompt.systemPrompt());
        assertTrue(user.contains("the hobgoblin is named hobgoblin"), prompt.userPrompt());
        assertTrue(user.contains("work state"), prompt.userPrompt());
        assertTrue(user.contains("patron state"), prompt.userPrompt());
        assertTrue(user.contains("trade state"), prompt.userPrompt());
        assertTrue(user.contains("koboldite"), prompt.userPrompt());
        assertTrue(user.contains("stored prayers"), prompt.userPrompt());
        assertTrue(user.contains("patron statue") || user.contains("hobgoblin patron"), prompt.userPrompt());
        assertTrue(user.contains("mog") && user.contains("gulg"), prompt.userPrompt());
    }

    @Test
    void knowledgeBaseRoutesHobgoblinCards() {
        NpcCardRequest request = NpcCardRequest.persona("hobgoblin");
        String koboldite = NpcKnowledgeBase.relevantContext(
                "how do I get koboldite dust?", "Forest", "day", request);
        String gods = NpcKnowledgeBase.relevantContext(
                "tell me about Mog and Gulg", "Forest", "night", request);
        String limits = NpcKnowledgeBase.relevantContext(
                "can you teach me circle magic?", "Forest", "night", request);

        assertTrue(koboldite.toLowerCase().contains("koboldite dust"), koboldite);
        assertTrue(koboldite.toLowerCase().contains("pickaxe"), koboldite);
        assertTrue(gods.toLowerCase().contains("mog") && gods.toLowerCase().contains("gulg"), gods);
        assertTrue(limits.toLowerCase().contains("do not know expert witchcraft"), limits);
    }

    @Test
    void hobgoblinFallbackAnswersCoreTopics() {
        Optional<String> trade = NpcDialogueFallback.hobgoblinChat(
                "what do you trade?", "Hobgoblin", "Dev",
                "free; holding nothing visible; will not mine unless leashed and given a pickaxe",
                "no goblin altar within 32 blocks", "");
        Optional<String> work = NpcDialogueFallback.hobgoblinChat(
                "will you mine for me?", "Hobgoblin", "Dev",
                "leashed; holding Iron Pickaxe; can dig for koboldite dust and deposit goods in nearby chests",
                "no goblin altar within 32 blocks", "");
        Optional<String> patron = NpcDialogueFallback.hobgoblinChat(
                "what does the patron statue do?", "Hobgoblin", "Dev",
                "free; holding nothing visible",
                "nearby goblin altar: Statue of Hobgoblin Patron 5.0 blocks away; stored prayers 3; lifetime prayers 9",
                "");
        Optional<String> witchcraft = NpcDialogueFallback.hobgoblinChat(
                "teach me a rite", "Hobgoblin", "Dev",
                "free; holding nothing visible", "no goblin altar within 32 blocks", "");

        assertTrue(trade.isPresent());
        assertTrue(trade.get().toLowerCase().contains("coal"), trade::toString);
        assertTrue(work.isPresent());
        assertTrue(work.get().toLowerCase().contains("leash"), work::toString);
        assertTrue(work.get().toLowerCase().contains("pickaxe"), work::toString);
        assertTrue(patron.isPresent());
        assertTrue(patron.get().toLowerCase().contains("stone god"), patron::toString);
        assertTrue(patron.get().toLowerCase().contains("3 fresh"), patron::toString);
        assertTrue(witchcraft.isPresent());
        assertTrue(witchcraft.get().toLowerCase().contains("witch words"), witchcraft::toString);
    }

    @Test
    void hobgoblinSocialIntentFindsInsultsWithoutCatchingFoolTheGuard() {
        assertTrue(HobgoblinPersona.INSTANCE.socialAction("you stupid hobgoblin")
                .filter(action -> action == NpcSocialAction.INSULT).isPresent());
        assertFalse(HobgoblinPersona.INSTANCE.socialAction("how do I fool the guards?").isPresent());
    }

    @Test
    void hobgoblinRejectsBadRawDrift() {
        assertTrue(HobgoblinPersona.rejectsHobgoblinRaw(
                "As an AI language model, I cannot answer.", "hello"));
        assertTrue(HobgoblinPersona.rejectsHobgoblinRaw(
                "I am a coven witch and I can brew fumes.", "who are you?"));
        assertTrue(HobgoblinPersona.rejectsHobgoblinRaw(
                "The Shade of Leonard knows your bargain.", "who are you?"));
        assertTrue(HobgoblinPersona.rejectsHobgoblinRaw(
                "I sell fumes and teach circle rites.", "what do you trade?"));
        assertTrue(HobgoblinPersona.rejectsHobgoblinRaw(
                "Wood ash is useful for witches.", "what is koboldite?"));
        assertFalse(HobgoblinPersona.rejectsHobgoblinRaw(
                "Koboldite is best shiny. Dust becomes nugget, nugget becomes ingot.", "what is koboldite?"));
    }
}
