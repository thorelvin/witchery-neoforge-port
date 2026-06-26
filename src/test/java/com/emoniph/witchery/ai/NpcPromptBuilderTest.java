package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcPromptBuilderTest {
    @Test
    void truncatesPromptWithoutGrowingPastLimit() {
        String truncated = NpcPromptBuilder.truncate("x".repeat(2000), 1600);

        assertEquals(1600, truncated.length());
        assertTrue(truncated.endsWith("..."));
    }

    @Test
    void keepsShortPromptUntouched() {
        assertEquals("hello", NpcPromptBuilder.truncate("hello", 1600));
    }

    @Test
    void smokePromptIncludesRelevantKnowledgeAndSceneFacts() {
        NpcBrainPrompt prompt = NpcPromptBuilder.covenWitchSmoke(
                "what is witches salt for?", "Entity memory: witch is Lilith Poe; player is Dev.");
        String user = prompt.userPrompt().toLowerCase();

        assertTrue(user.contains("the witch is named lilith poe"));
        assertTrue(user.contains("the player is named dev"));
        assertTrue(user.contains("biome plains"));
        assertTrue(user.contains("witches' salt") || user.contains("witches salt"));
        assertTrue(user.contains("entity memory"));
    }

    @Test
    void smokePromptIncludesTargetedLoreForKeywordMention() {
        NpcBrainPrompt prompt = NpcPromptBuilder.covenWitchSmoke("spirit world sounds dangerous", "");
        String user = prompt.userPrompt().toLowerCase();

        assertTrue(user.contains("targeted fact"));
        assertTrue(user.contains("spirit world"));
        assertTrue(user.contains("nightmares"));
    }

    @Test
    void smokePromptDoesNotUseLeakProneLegacyLabels() {
        NpcBrainPrompt prompt = NpcPromptBuilder.covenWitchSmoke("hi", "");
        String user = prompt.userPrompt().toLowerCase();

        assertTrue(!user.contains("npc_name:"));
        assertTrue(!user.contains("player_name:"));
        assertTrue(!user.contains("witchery knowledge:"));
        assertTrue(!user.contains("entity/player memory file:"));
        assertTrue(!user.contains("player chat message:"));
    }
}
