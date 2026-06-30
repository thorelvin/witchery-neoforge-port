package com.emoniph.witchery.ai;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.Optional;

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
    void smokePromptTeachesBroadlyFromProvidedFactsWithoutUnlockStateClaim() {
        NpcBrainPrompt prompt = NpcPromptBuilder.covenWitchSmoke(
                "tell me about vampire sunlight", "");
        String user = prompt.userPrompt().toLowerCase();

        assertTrue(user.contains("may teach broadly from the facts provided here"), prompt.userPrompt());
        assertTrue(!user.contains("unless the facts say it is unlocked for this player"), prompt.userPrompt());
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
    void cardRepairPromptUsesRelevantFactsAndBadDraft() {
        Optional<NpcBrainPrompt> prompt = NpcPromptBuilder.cardRepair("coven_witch", "Lilith Poe", "Dev",
                "how do I use the kettle?", "Message routing: route=recipe;topic=kettle;remember=no",
                "Hello again, Dev.");

        assertTrue(prompt.isPresent());
        String system = prompt.get().systemPrompt().toLowerCase();
        String user = prompt.get().userPrompt().toLowerCase();

        assertTrue(system.contains("repair one minecraft witchery npc line"), prompt.get().systemPrompt());
        assertTrue(system.contains("supplied in-world facts"), prompt.get().systemPrompt());
        assertTrue(user.contains("lilith poe is speaking to dev"), prompt.get().userPrompt());
        assertTrue(user.contains("fill the kettle with water"), prompt.get().userPrompt());
        assertTrue(user.contains("bad draft to avoid"), prompt.get().userPrompt());
        assertTrue(user.contains("hello again, dev"), prompt.get().userPrompt());
        assertTrue(prompt.get().examples().stream().anyMatch(example ->
                example.assistantResponse().contains("Fill it, heat it")));
    }

    @Test
    void cardRepairPromptSkipsSmallTalkWithoutFacts() {
        Optional<NpcBrainPrompt> prompt = NpcPromptBuilder.cardRepair("coven_witch", "Lilith Poe", "Dev",
                "hello there", "", "Welcome to the Witchery world.");

        assertTrue(prompt.isEmpty());
    }

    @Test
    void cardRepairUsesRoutingToAvoidSmallTalkAndMemoryCollisions() {
        Optional<NpcBrainPrompt> smallTalk = NpcPromptBuilder.cardRepair("coven_witch", "Lilith Poe", "Dev",
                "Do you like eggs?",
                "Message routing: route=small_talk; topic=eggs; reply strategy=answer small talk.",
                "Witches' salt is protective.");
        Optional<NpcBrainPrompt> memory = NpcPromptBuilder.cardRepair("coven_witch", "Lilith Poe", "Dev",
                "What is my name?",
                "Message routing: route=memory; topic=name; reply strategy=answer remembered player facts.",
                "Belladonna grows in old gardens.");
        Optional<NpcBrainPrompt> social = NpcPromptBuilder.cardRepair("coven_witch", "Lilith Poe", "Dev",
                "Sorry, that was rude.",
                "Message routing: route=social; topic=apology; reply strategy=respond to tone first.",
                "Ask for a brew by name.");

        assertTrue(smallTalk.isEmpty());
        assertTrue(memory.isEmpty());
        assertTrue(social.isEmpty());
    }

    @Test
    void cardRepairUsesRoutingToAvoidGenericAtmosphereCollisions() {
        Optional<NpcBrainPrompt> prompt = NpcPromptBuilder.cardRepair("coven_witch", "Lilith Poe", "Dev",
                "The rain feels strange tonight.",
                "Message routing: route=atmosphere; topic=rain; reply strategy=answer scene remark.",
                "Witches' salt is protective.");

        assertTrue(prompt.isEmpty());
    }

    @Test
    void cardRepairStillAllowsConcreteRoutesAndAmbientBehavior() {
        Optional<NpcBrainPrompt> recipe = NpcPromptBuilder.cardRepair("coven_witch", "Lilith Poe", "Dev",
                "how do I use the kettle?",
                "Message routing: route=recipe; topic=kettle; reply strategy=answer exact known ingredient.",
                "Hello again, Dev.");
        Optional<NpcBrainPrompt> lore = NpcPromptBuilder.cardRepair("coven_witch", "Lilith Poe", "Dev",
                "What is witches salt for?",
                "Message routing: route=lore; topic=witches_salt; reply strategy=stay on topic.",
                "The rain has teeth.");
        Optional<NpcBrainPrompt> trade = NpcPromptBuilder.cardRepair("coven_witch", "Lilith Poe", "Dev",
                "what do you trade?",
                "Message routing: route=trade; topic=trade; reply strategy=answer trade directly.",
                "The witch says hi.");
        Optional<NpcBrainPrompt> ambient = NpcPromptBuilder.cardRepair("coven_witch", "Lilith Poe", "Dev",
                "ambient reading the witches primer",
                "Message routing: route=atmosphere; topic=ambient_behavior; reply strategy=answer scene remark.",
                "Visible pages are on the screen.");

        assertTrue(recipe.isPresent());
        assertTrue(recipe.get().userPrompt().toLowerCase().contains("fill the kettle with water"),
                recipe.get().userPrompt());
        assertTrue(lore.isPresent());
        assertTrue(lore.get().userPrompt().toLowerCase().contains("witches' salt")
                || lore.get().userPrompt().toLowerCase().contains("witches salt"), lore.get().userPrompt());
        assertTrue(trade.isPresent());
        assertTrue(trade.get().userPrompt().toLowerCase().contains("right-click")
                || trade.get().userPrompt().toLowerCase().contains("stock"), trade.get().userPrompt());
        assertTrue(ambient.isPresent());
        assertTrue(ambient.get().userPrompt().toLowerCase().contains("primer"), ambient.get().userPrompt());
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
        assertTrue(system.contains("routes: small_talk, atmosphere, trade, recipe, rite, lore, memory, social, action, unknown"),
                prompt.systemPrompt());
        assertTrue(system.contains("use atmosphere for weather"), prompt.systemPrompt());
        assertTrue(system.contains("write memory only for durable facts about dev"), prompt.systemPrompt());
        assertTrue(user.contains("fear nightmares"), prompt.userPrompt());
        assertTrue(prompt.examples().stream().anyMatch(example ->
                example.assistantResponse().contains("route=recipe")
                        && example.assistantResponse().contains("whiff_of_magic")));
        assertTrue(prompt.examples().stream().anyMatch(example ->
                example.assistantResponse().contains("Dev is wary of nightmares.")));
        assertTrue(prompt.examples().stream().anyMatch(example ->
                example.assistantResponse().contains("route=atmosphere")
                        && example.assistantResponse().contains("crows")));
    }

    @Test
    void npcChatPromptsTreatRoutingAsCurrentIntent() {
        String memory = "Message routing: route=small_talk; topic=debt; reply strategy=answer the player's actual small talk.";
        NpcBrainPrompt witch = NpcPromptBuilder.covenWitchSmoke("Why call everything a debt?", memory);
        NpcBrainPrompt leonard = NpcPromptBuilder.leonardSmoke("Why call everything a debt?", memory);
        NpcBrainPrompt villager = NpcPromptBuilder.villagerSmoke("Why is everyone worried?", memory);

        assertTrue(witch.userPrompt().contains("treat it as current player intent"), witch.userPrompt());
        assertTrue(witch.userPrompt().contains("atmosphere answers scene or behavior cues"), witch.userPrompt());
        assertTrue(leonard.userPrompt().contains("treat it as current player intent"), leonard.userPrompt());
        assertTrue(leonard.userPrompt().contains("atmosphere answers scene or behavior cues"), leonard.userPrompt());
        assertTrue(villager.userPrompt().contains("treat it as current player intent"), villager.userPrompt());
        assertTrue(villager.userPrompt().contains("atmosphere answers scene or behavior cues"), villager.userPrompt());
    }

    @Test
    void villagerSmokePromptIncludesVillageFactsAndJobCard() {
        NpcBrainPrompt prompt = NpcPromptBuilder.villagerSmoke(
                "do we have a golem and are vampires nearby?", "Entity memory: villager is Marta; player is Dev.");
        String user = prompt.userPrompt().toLowerCase();

        assertTrue(user.contains("the villager is named apothecary"), prompt.userPrompt());
        assertTrue(user.contains("profession: apothecary"), prompt.userPrompt());
        assertTrue(user.contains("village state nearby"), prompt.userPrompt());
        assertTrue(user.contains("iron golems 1"), prompt.userPrompt());
        assertTrue(user.contains("walls/fences 42"), prompt.userPrompt());
        assertTrue(user.contains("vampires 1"), prompt.userPrompt());
        assertTrue(user.contains("apothecary card"), prompt.userPrompt());
        assertTrue(user.contains("herbs"), prompt.userPrompt());
        assertTrue(user.contains("not a coven witch"), prompt.userPrompt());
        assertTrue(user.contains("do not claim to brew witchcraft"), prompt.userPrompt());
        assertTrue(user.contains("sell fumes"), prompt.userPrompt());
    }

    @Test
    void villagerSmokePromptDoesNotSoundLikeWitchOrLeonard() {
        NpcBrainPrompt prompt = NpcPromptBuilder.villagerSmoke("who are you?", "");
        String system = prompt.systemPrompt().toLowerCase();

        assertTrue(system.contains("speak as the villager only"), prompt.systemPrompt());
        assertTrue(system.contains("never claim to be a witch"), prompt.systemPrompt());
        assertTrue(system.contains("shade of leonard"), prompt.systemPrompt());
        assertTrue(system.contains("peasant local"), prompt.systemPrompt());
        assertTrue(system.contains("do not claim to brew witchcraft"), prompt.systemPrompt());
        assertTrue(system.contains("demons are worse than witches"), prompt.systemPrompt());
    }

    @Test
    void villagerSmokePromptUsesPeasantFolkloreVoice() {
        NpcBrainPrompt prompt = NpcPromptBuilder.villagerSmoke("is the road safe?", "");
        String system = prompt.systemPrompt().toLowerCase();
        String user = prompt.userPrompt().toLowerCase();

        assertTrue(system.contains("superstitious"), prompt.systemPrompt());
        assertTrue(system.contains("folklore-minded"), prompt.systemPrompt());
        assertTrue(system.contains("crows"), prompt.systemPrompt());
        assertTrue(system.contains("threshold"), prompt.systemPrompt());
        assertTrue(user.contains("peasant folklore"), prompt.userPrompt());
        assertTrue(user.contains("wary local"), prompt.userPrompt());
    }

    @Test
    void villagerSmokePromptIncludesWitchWeatherAndHarvestCards() {
        NpcBrainPrompt witches = NpcPromptBuilder.villagerSmoke(
                "Are there good witches as well as bad witches?", "");
        String witchUser = witches.userPrompt().toLowerCase();
        assertTrue(witchUser.contains("not all alike") || witchUser.contains("fair trade"),
                witches.userPrompt());
        assertTrue(witchUser.contains("others curse") || witchUser.contains("hidden taglocks"),
                witches.userPrompt());

        NpcBrainPrompt harvest = NpcPromptBuilder.villagerSmoke(
                "How is the harvest looking if the weather turns rainy?", "");
        String harvestUser = harvest.userPrompt().toLowerCase();
        assertTrue(harvestUser.contains("harvest"), harvest.userPrompt());
        assertTrue(harvestUser.contains("weather"), harvest.userPrompt());
        assertTrue(harvestUser.contains("bread") || harvestUser.contains("crops"), harvest.userPrompt());
    }

    @Test
    void personasDeclareDistinctChatColors() {
        assertEquals(0xFF55FF, NpcPersonas.COVEN_WITCH.chatColorRgb());
        assertEquals(0xFF5555, NpcPersonas.LEONARD.chatColorRgb());
        assertEquals(0xFFAA00, NpcPersonas.VILLAGER.chatColorRgb());

        Component line = NpcBrainService.speechComponent("Marta", NpcPersonas.VILLAGER.chatColorRgb(),
                "Mind the road.");
        assertEquals("<Marta> Mind the road.", line.getString());
        assertEquals(0xFFAA00, line.getStyle().getColor().getValue());
    }
}
