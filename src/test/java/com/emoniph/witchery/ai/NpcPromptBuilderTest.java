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
    void smokePromptIncludesExactRecipeCardWhenKnown() {
        NpcBrainPrompt prompt = NpcPromptBuilder.covenWitchSmoke(
                "tell me the exact recipe for Brew of Sleeping", "");
        String user = prompt.userPrompt().toLowerCase();

        assertTrue(user.contains("purified milk"), user);
        assertTrue(user.contains("whiff of magic"), user);
        assertTrue(user.contains("icy needle"), user);
        assertTrue(user.contains("you may name those ingredients exactly"), user);
    }

    @Test
    void smokePromptIncludesWhiffSourceCard() {
        NpcBrainPrompt prompt = NpcPromptBuilder.covenWitchSmoke("how do I get whif of magic?", "");
        String user = prompt.userPrompt().toLowerCase();

        assertTrue(user.contains("rowan sapling"), user);
        assertTrue(user.contains("witch's oven"), user);
        assertTrue(user.contains("clay jar"), user);
    }

    @Test
    void smokePromptIncludesWhiffUsageCardForUseQuestions() {
        NpcBrainPrompt prompt = NpcPromptBuilder.covenWitchSmoke(
                "what specific recipe uses Whiff of Magic?", "");
        String user = prompt.userPrompt().toLowerCase();

        assertTrue(user.contains("brew of sleeping"), user);
        assertTrue(user.contains("brew of love"), user);
        assertTrue(user.contains("attuned stone"), user);
    }

    @Test
    void smokePromptIncludesStarterReagentCards() {
        NpcBrainPrompt mutandis = NpcPromptBuilder.covenWitchSmoke("how do I make Mutandis?", "");
        String mutandisUser = mutandis.userPrompt().toLowerCase();

        assertTrue(mutandisUser.contains("wood ash"), mutandisUser);
        assertTrue(mutandisUser.contains("hint of rebirth"), mutandisUser);
        assertTrue(mutandisUser.contains("mandrake root"), mutandisUser);
        assertTrue(mutandisUser.contains("bone meal"), mutandisUser);

        NpcBrainPrompt attuned = NpcPromptBuilder.covenWitchSmoke("what do I need for Attuned Stone?", "");
        String attunedUser = attuned.userPrompt().toLowerCase();

        assertTrue(attunedUser.contains("whiff of magic"), attunedUser);
        assertTrue(attunedUser.contains("diamond"), attunedUser);
        assertTrue(attunedUser.contains("lava bucket"), attunedUser);
    }

    @Test
    void smokePromptUsesSaltRecipeCardForHowToGetSaltQuestions() {
        NpcBrainPrompt prompt = NpcPromptBuilder.covenWitchSmoke("how do I get witches' salt?", "");
        String user = prompt.userPrompt().toLowerCase();

        assertTrue(user.contains("kelp"), user);
        assertTrue(user.contains("witch's oven"), user);
        assertTrue(user.contains("two salt"), user);
    }

    @Test
    void smokePromptIncludesStarterCardsForTerseMentions() {
        NpcBrainPrompt whiff = NpcPromptBuilder.covenWitchSmoke("Whiff?", "");
        String whiffUser = whiff.userPrompt().toLowerCase();

        assertTrue(whiffUser.contains("whiff of magic"), whiffUser);
        assertTrue(whiffUser.contains("rowan sapling"), whiffUser);

        NpcBrainPrompt mutandis = NpcPromptBuilder.covenWitchSmoke("Mutandis?", "");
        String mutandisUser = mutandis.userPrompt().toLowerCase();

        assertTrue(mutandisUser.contains("mutandis"), mutandisUser);
        assertTrue(mutandisUser.contains("mutates") || mutandisUser.contains("mutation"), mutandisUser);

        NpcBrainPrompt attuned = NpcPromptBuilder.covenWitchSmoke("Attuned Stone?", "");
        String attunedUser = attuned.userPrompt().toLowerCase();

        assertTrue(attunedUser.contains("attuned stone"), attunedUser);
        assertTrue(attunedUser.contains("whiff of magic"), attunedUser);
        assertTrue(attunedUser.contains("lava bucket"), attunedUser);

        NpcBrainPrompt exhale = NpcPromptBuilder.covenWitchSmoke("Exhale?", "");
        String exhaleUser = exhale.userPrompt().toLowerCase();

        assertTrue(exhaleUser.contains("exhale of the horned one"), exhaleUser);
        assertTrue(exhaleUser.contains("oak sapling"), exhaleUser);
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

    @Test
    void smokePromptDoesNotLeakBossPersonaCardsIntoCovenWitch() {
        NpcBrainPrompt prompt = NpcPromptBuilder.covenWitchSmoke(
                "Evening, Lilith, the fog feels strange tonight.", "");
        String user = prompt.userPrompt().toLowerCase();

        assertTrue(!user.contains("regal, hungry"), user);
        assertTrue(!user.contains("vampire fire"), user);
        assertTrue(!user.contains("current run"), user);
    }

    @Test
    void warmupPromptUsesInvisibleReadyJson() {
        NpcBrainPrompt prompt = NpcPromptBuilder.warmup();

        assertTrue(prompt.systemPrompt().contains("{\"speech\":\"ready\",\"intent\":\"none\",\"memory\":\"\"}"));
        assertTrue(prompt.userPrompt().toLowerCase().contains("warm up"));
        assertEquals(0, prompt.examples().size());
    }

    @Test
    void socialIntentPromptUsesStrictYesNoSchema() {
        NpcBrainPrompt prompt = NpcPromptBuilder.socialIntentCheck("witch", NpcSocialAction.INSULT,
                "How should I harvest mandrake without making a fool of myself?");
        String system = prompt.systemPrompt().toLowerCase();
        String user = prompt.userPrompt().toLowerCase();

        assertTrue(system.contains("{\"speech\":\"yes\",\"intent\":\"none\",\"memory\":\"\"}"), prompt.systemPrompt());
        assertTrue(system.contains("{\"speech\":\"no\",\"intent\":\"none\",\"memory\":\"\"}"), prompt.systemPrompt());
        assertTrue(system.contains("self-deprecation"), prompt.systemPrompt());
        assertTrue(user.contains("insult_npc"), prompt.userPrompt());
        assertTrue(user.contains("not magic requests"), prompt.userPrompt());
        assertTrue(user.contains("fool of myself"), prompt.userPrompt());
        assertTrue(prompt.examples().stream().anyMatch(example ->
                example.userPrompt().contains("fool of myself")
                        && example.assistantResponse().contains("\"speech\":\"no\"")));
        assertTrue(prompt.examples().stream().anyMatch(example ->
                example.userPrompt().contains("Can you fool the guards")
                        && example.assistantResponse().contains("\"speech\":\"no\"")));
    }

    @Test
    void messageAnalysisPromptUsesRouterAndMemorySchema() {
        NpcBrainPrompt prompt = NpcPromptBuilder.messageAnalysis("witch", "Dev",
                "Remember that I fear nightmares.");
        String system = prompt.systemPrompt().toLowerCase();
        String user = prompt.userPrompt().toLowerCase();

        assertTrue(system.contains("routing and memory extractor"), prompt.systemPrompt());
        assertTrue(system.contains("route=<route>;topic=<topic>;emotion=<emotion>;remember=<yes/no>"),
                prompt.systemPrompt());
        assertTrue(system.contains("routes: small_talk, trade, recipe, rite, lore, memory, social, action, unknown"),
                prompt.systemPrompt());
        assertTrue(system.contains("write memory only for durable facts about dev"), prompt.systemPrompt());
        assertTrue(user.contains("fear nightmares"), prompt.userPrompt());
        assertTrue(prompt.examples().stream().anyMatch(example ->
                example.assistantResponse().contains("route=recipe")
                        && example.assistantResponse().contains("whiff_of_magic")));
        assertTrue(prompt.examples().stream().anyMatch(example ->
                example.assistantResponse().contains("Dev is wary of nightmares.")));
    }
}
