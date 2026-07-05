package com.emoniph.witchery.ai;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcKnowledgeBaseTest {
    @Test
    void loadsMarkdownKnowledgeForAltarQuestions() {
        String context = NpcKnowledgeBase.relevantContext("how do I power an altar?", "Plains", "day");
        String lower = context.toLowerCase(Locale.ROOT);

        assertTrue(lower.contains("altar"));
        assertTrue(lower.contains("power"));
    }

    @Test
    void skipsLoreForGenericGreeting() {
        String context = NpcKnowledgeBase.relevantContext("hi", "Plains", "day");

        assertTrue(context.isBlank());
    }

    @Test
    void retrievesItemKnowledgeForSaltQuestions() {
        String context = NpcKnowledgeBase.relevantContext("what is witches salt for?", "Forest", "night");
        String lower = context.toLowerCase(Locale.ROOT);

        assertTrue(lower.contains("witches' salt") || lower.contains("witches salt"));
        assertTrue(lower.contains("protective") || lower.contains("cleansing"));
    }

    @Test
    void diagnosticsReportLoadedCardsAndLastLookupReason() {
        String context = NpcKnowledgeBase.relevantContext("what is witches salt for?", "Forest", "night");

        assertTrue(!context.isBlank());
        String diagnostics = NpcKnowledgeBase.diagnosticsLine();
        assertTrue(diagnostics.matches(".*sectionsLoaded=[1-9][0-9]*.*"), diagnostics);
        assertTrue(diagnostics.matches(".*cardsLoaded=[1-9][0-9]*.*"), diagnostics);
        assertTrue(diagnostics.matches(".*targetedNotesLoaded=[1-9][0-9]*.*"), diagnostics);
        assertTrue(diagnostics.contains("lastKnowledgeReason=targeted_match:")
                || diagnostics.contains("lastKnowledgeReason=section_match:"), diagnostics);
        assertTrue(diagnostics.contains("lastCards="), diagnostics);
    }

    @Test
    void retrievesCardByTagsWhenTriggerWordsAreAbsent() {
        String context = NpcKnowledgeBase.relevantContext(
                "which mystic branch shadow vessel handles brew focus?", "Plains", "night",
                new NpcCardRequest("leonard", new UUID(71L, 72L), new UUID(81L, 82L), 0L, 0, 3, 1));
        String lower = context.toLowerCase(Locale.ROOT);

        assertTrue(lower.contains("leonard's urn"), context);
        assertTrue(lower.contains("without consuming it"), context);
    }

    @Test
    void retrievesCompassCardByTrackingToolTag() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer(
                "what does the bound player compass do?", "Plains", "day");

        assertTrue(answer.isPresent());
        assertTrue(answer.get().contains("Player Compass"), answer.get());
    }

    @Test
    void retrievesCardFactsByFilenameAlias() {
        String context = NpcKnowledgeBase.relevantContext("what are special tools compasses?", "Plains", "day");
        String lower = context.toLowerCase(Locale.ROOT);

        assertTrue(lower.contains("targeted fact"), context);
        assertTrue(lower.contains("book of biomes")
                || lower.contains("player compass")
                || lower.contains("poppet shelf compass"), context);
    }

    @Test
    void retrievesNatureKnowledgeForHerbQuestions() {
        String context = NpcKnowledgeBase.relevantContext("where do I find mandrake and belladonna?", "Forest", "day");
        String lower = context.toLowerCase(Locale.ROOT);

        assertTrue(lower.contains("mandrake"));
        assertTrue(lower.contains("belladonna"));
    }

    @Test
    void skipsLoreForFavoriteHerbSmallTalk() {
        String context = NpcKnowledgeBase.relevantContext("what are your favorite herb?", "Forest", "day");
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("what are your favorite herb?", "Forest", "day");

        assertTrue(context.isBlank());
        assertTrue(answer.isEmpty());
    }

    @Test
    void retrievesTargetedLoreForKeywordMention() {
        String context = NpcKnowledgeBase.relevantContext("spirit world sounds dangerous", "Plains", "night");
        String lower = context.toLowerCase(Locale.ROOT);

        assertTrue(lower.contains("targeted fact"));
        assertTrue(lower.contains("spirit world"));
        assertTrue(lower.contains("nightmare"));
    }

    @Test
    void personaCardRequestCanUseSocialCueWithoutGenericLookupBecomingChatty() {
        String generic = NpcKnowledgeBase.relevantContext("hi", "Plains", "day");
        String persona = NpcKnowledgeBase.relevantContext("hi", "Plains", "day",
                new NpcCardRequest("coven_witch", new UUID(1L, 2L), new UUID(3L, 4L), 0L, 0, 2, 1));

        assertTrue(generic.isBlank());
        assertTrue(persona.toLowerCase(Locale.ROOT).contains("targeted fact"), persona);
        assertTrue(persona.toLowerCase(Locale.ROOT).contains("greet"), persona);
    }

    @Test
    void genericLookupDoesNotUsePersonaOnlyBossCards() {
        String context = NpcKnowledgeBase.relevantContext("Evening, Lilith, the fog feels strange tonight.",
                "Plains", "night");
        String lower = context.toLowerCase(Locale.ROOT);

        assertTrue(!lower.contains("regal, hungry"), context);
        assertTrue(!lower.contains("vampire fire"), context);
        assertTrue(!lower.contains("current run"), context);
    }

    @Test
    void personaCardRequestRotatesVariantsByConversationTurn() {
        UUID npc = new UUID(11L, 12L);
        UUID player = new UUID(21L, 22L);
        Set<String> contexts = new LinkedHashSet<>();
        for (int turn = 0; turn < 8; turn++) {
            String context = NpcKnowledgeBase.relevantContext("where were you summoned from?", "Plains", "night",
                    new NpcCardRequest("leonard", npc, player, 1200L, turn, 1, 1));
            String lower = context.toLowerCase(Locale.ROOT);
            assertTrue(lower.contains("targeted fact"), context);
            assertTrue(!lower.contains("[topic:"), context);
            assertTrue(!lower.contains("[persona:"), context);
            assertTrue(!lower.contains("[weight:"), context);
            contexts.add(context);
        }

        assertTrue(contexts.size() > 1, "Expected Leonard origin variants to rotate by turn: " + contexts);
    }

    @Test
    void personaCardRequestFiltersOtherPersonaFacts() {
        String context = NpcKnowledgeBase.relevantContext(
                "where were you summoned from goat legs sabbath", "Plains", "night",
                new NpcCardRequest("coven_witch", new UUID(31L, 32L), new UUID(41L, 42L), 0L, 0, 3, 1));
        String lower = context.toLowerCase(Locale.ROOT);

        assertTrue(!lower.contains("goat-legged"), context);
        assertTrue(!lower.contains("sabbath threshold"), context);
    }

    @Test
    void retrievesAmbientChestAndTheftCardsForWitchPersona() {
        String warning = NpcKnowledgeBase.relevantContext(
                "ambient looking into the witch's chest", "Plains", "night",
                new NpcCardRequest("coven_witch", new UUID(51L, 52L), new UUID(61L, 62L), 0L, 0, 3, 1));
        String warningLower = warning.toLowerCase(Locale.ROOT);

        assertTrue(warningLower.contains("targeted fact"), warning);
        assertTrue(warningLower.contains("chest"), warning);
        assertTrue(warningLower.contains("fingers") || warningLower.contains("pockets"), warning);

        String theft = NpcKnowledgeBase.relevantContext(
                "ambient sneaking into the witch chest darkness curse", "Plains", "night",
                new NpcCardRequest("coven_witch", new UUID(53L, 54L), new UUID(63L, 64L), 0L, 0, 3, 1));
        String theftLower = theft.toLowerCase(Locale.ROOT);

        assertTrue(theftLower.contains("curse"), theft);
        assertTrue(theftLower.contains("theft")
                || theftLower.contains("stolen")
                || theftLower.contains("punishment"), theft);
    }

    @Test
    void retrievesAmbientPrimerCardWithoutVisiblePageLanguage() {
        String context = NpcKnowledgeBase.relevantContext(
                "ambient reading the Witches Primer", "Plains", "day",
                new NpcCardRequest("coven_witch", new UUID(55L, 56L), new UUID(65L, 66L), 0L, 0, 3, 1));
        String lower = context.toLowerCase(Locale.ROOT);

        assertTrue(lower.contains("targeted fact"), context);
        assertTrue(lower.contains("primer"), context);
        assertTrue(!lower.contains("visible pages"), context);
        assertTrue(lower.contains("book") || lower.contains("read"), context);
    }

    @Test
    void retrievesAmbientInvisiblePlayerCardForCreepyCroneRemark() {
        String context = NpcKnowledgeBase.relevantContext(
                "ambient invisible player lingering near the witch", "Plains", "night",
                new NpcCardRequest("coven_witch", new UUID(57L, 58L), new UUID(67L, 68L), 0L, 0, 3, 1));
        String lower = context.toLowerCase(Locale.ROOT);

        assertTrue(lower.contains("targeted fact"), context);
        assertTrue(lower.contains("invisible"), context);
        assertTrue(lower.contains("absence") || lower.contains("breath") || lower.contains("draft"), context);
        assertTrue(!lower.contains("perfect sight"), context);
    }

    @Test
    void retrievesSharedAtmosphereCardsForPersonaSceneCues() {
        String witch = NpcKnowledgeBase.relevantContext(
                "The rain and crows make your hut feel restless.", "Plains", "night",
                new NpcCardRequest("coven_witch", new UUID(59L, 60L), new UUID(69L, 70L), 0L, 0, 3, 1));
        String baba = NpcKnowledgeBase.relevantContext(
                "The old road and hungry night found your door.", "Swamp", "night",
                new NpcCardRequest("baba_yaga", new UUID(71L, 72L), new UUID(73L, 74L), 0L, 0, 3, 1));
        String leonard = NpcKnowledgeBase.relevantContext(
                "The threshold is cold and the silence is heavy.", "Plains", "night",
                new NpcCardRequest("leonard", new UUID(75L, 76L), new UUID(77L, 78L), 0L, 0, 3, 1));
        String villager = NpcKnowledgeBase.relevantContext(
                "The harvest looks thin and the roads are mud.", "Plains", "day",
                new NpcCardRequest("villager", new UUID(79L, 80L), new UUID(81L, 82L), 0L, 0, 3, 1));

        assertTrue(witch.toLowerCase(Locale.ROOT).contains("reagent pressure"), witch);
        assertTrue(baba.toLowerCase(Locale.ROOT).contains("old roads"), baba);
        assertTrue(leonard.toLowerCase(Locale.ROOT).contains("debt language"), leonard);
        String villagerLower = villager.toLowerCase(Locale.ROOT);
        assertTrue(villagerLower.contains("harvest") || villagerLower.contains("weather as work")
                || villagerLower.contains("roads"), villager);
    }

    @Test
    void retrievesTargetedHerbLoreWithoutQuestionShape() {
        String context = NpcKnowledgeBase.relevantContext("belladonna then", "Plains", "day");
        String lower = context.toLowerCase(Locale.ROOT);

        assertTrue(lower.contains("targeted fact"));
        assertTrue(lower.contains("belladonna"));
        assertTrue(lower.contains("trade good") || lower.contains("ingredient"));
    }

    @Test
    void retrievesMarkdownTargetedLoreForEarlyOvenQuestions() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("what does a witches oven do?", "Plains", "day");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("oven"));
        assertTrue(lower.contains("fume"));
    }

    @Test
    void retrievesWhiffOfMagicSource() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("how do I get whif of magic?", "Plains", "night");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("whiff of magic"));
        assertTrue(lower.contains("rowan"));
        assertTrue(lower.contains("oven"));
    }

    @Test
    void retrievesKnownSleepingBrewRecipeWhenAskedDirectly() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer(
                "tell me the exact recipe for Brew of Sleeping", "Plains", "night");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("purified milk"), answer.get());
        assertTrue(lower.contains("whiff of magic"), answer.get());
        assertTrue(lower.contains("icy needle"), answer.get());
    }

    @Test
    void sleepingBrewCardMatchesRecipeJson() throws Exception {
        assertCardMatchesRecipe(
                "tell me the exact recipe for Brew of Sleeping",
                "/data/witchery/recipe/brew_of_sleeping.json");
    }

    @Test
    void mainBrewRecipeCardsMatchRecipeJson() throws Exception {
        assertCardMatchesRecipe("what is the recipe for Brew of Love?", "/data/witchery/recipe/brew_of_love.json");
        assertCardMatchesRecipe("what is the recipe for Brew of Revealing?", "/data/witchery/recipe/brew_of_revealing.json");
        assertCardMatchesRecipe("what is the recipe for Brew of Poison?", "/data/witchery/recipe/brew_of_poison.json");
        assertCardMatchesRecipe("what is the recipe for Brew of Cleansing?", "/data/witchery/recipe/brew_of_cleansing.json");
        assertCardMatchesRecipe("what is the recipe for Brew of Sprouting?", "/data/witchery/recipe/brew_of_sprouting.json");
        assertCardMatchesRecipe("what is the recipe for Brew of Flowing Spirit?", "/data/witchery/recipe/brew_of_flowing_spirit.json");
    }

    @Test
    void naturalBrewRecipeQuestionsHitExactRecipeCards() throws Exception {
        assertCardMatchesRecipe("how do I make Brew of Sleeping?", "/data/witchery/recipe/brew_of_sleeping.json");
        assertCardMatchesRecipe("what do I need to brew Revealing Brew?", "/data/witchery/recipe/brew_of_revealing.json");
        assertCardMatchesRecipe("how do I brew Poison Brew?", "/data/witchery/recipe/brew_of_poison.json");
        assertCardMatchesRecipe("what do I need for Brew of Cleansing?", "/data/witchery/recipe/brew_of_cleansing.json");
        assertCardMatchesRecipe("how do I make Sprouting Brew?", "/data/witchery/recipe/brew_of_sprouting.json");
        assertCardMatchesRecipe("what do I need to brew Flowing Spirit?", "/data/witchery/recipe/brew_of_flowing_spirit.json");
    }

    @Test
    void basicCraftRecipeCardsMatchRecipeJson() throws Exception {
        assertCardMatchesRecipe("how do I craft anointing paste?", "/data/witchery/recipe/anointing_paste.json");
        assertCardMatchesRecipe("how do I craft chalk?", "/data/witchery/recipe/chalk.json");
        assertCardMatchesRecipe("how do I craft a Witch's Oven?", "/data/witchery/recipe/witches_oven.json");
        assertCardMatchesRecipe("how do I craft a Kettle?", "/data/witchery/recipe/kettle.json");
        assertCardMatchesRecipe("how do I craft a Distillery?", "/data/witchery/recipe/distillery.json");
        assertCardMatchesRecipe("how do I craft an Altar?", "/data/witchery/recipe/altar.json");
    }

    @Test
    void naturalMachineRecipeQuestionsHitExactRecipeCards() throws Exception {
        assertCardMatchesRecipe("what do I need to make a witch oven?", "/data/witchery/recipe/witches_oven.json");
        assertCardMatchesRecipe("what do I need to make a kettle?", "/data/witchery/recipe/kettle.json");
        assertCardMatchesRecipe("how do I make a distillery?", "/data/witchery/recipe/distillery.json");
        assertCardMatchesRecipe("what do I need to make an altar?", "/data/witchery/recipe/altar.json");
    }

    @Test
    void starterReagentRecipeCardsMatchRecipeJson() throws Exception {
        assertCardMatchesRecipe("what do I need to make attuned stone?", "/data/witchery/recipe/attuned_stone.json");
        assertCardMatchesRecipe("how do I make mutandis?", "/data/witchery/recipe/mutandis.json");
        assertCardMatchesRecipe("how do I make mutandis extremis?", "/data/witchery/recipe/mutandis_extremis.json");
    }

    @Test
    void terseStarterReagentMentionsRetrievePrimerCards() {
        Optional<String> whiff = NpcKnowledgeBase.compactAnswer("Whiff?", "Plains", "day");

        assertTrue(whiff.isPresent());
        assertTrue(whiff.get().contains("Whiff of Magic"), whiff.get());
        assertTrue(whiff.get().contains("Rowan Sapling"), whiff.get());

        Optional<String> mutandis = NpcKnowledgeBase.compactAnswer("Mutandis?", "Plains", "day");

        assertTrue(mutandis.isPresent());
        assertTrue(mutandis.get().contains("Mutandis"), mutandis.get());
        assertTrue(mutandis.get().contains("mutates") || mutandis.get().contains("mutation"), mutandis.get());

        Optional<String> attuned = NpcKnowledgeBase.compactAnswer("Attuned Stone?", "Plains", "day");

        assertTrue(attuned.isPresent());
        assertTrue(attuned.get().contains("Attuned Stone"), attuned.get());
        assertTrue(attuned.get().contains("Whiff of Magic"), attuned.get());

        Optional<String> exhale = NpcKnowledgeBase.compactAnswer("Exhale of the Horned One?", "Plains", "day");

        assertTrue(exhale.isPresent());
        assertTrue(exhale.get().contains("Oak Sapling"), exhale.get());
        assertTrue(exhale.get().contains("Clay Jar"), exhale.get());

        Optional<String> shortExhale = NpcKnowledgeBase.compactAnswer("Exhale?", "Plains", "day");

        assertTrue(shortExhale.isPresent());
        assertTrue(shortExhale.get().contains("Oak Sapling"), shortExhale.get());

        Optional<String> odour = NpcKnowledgeBase.compactAnswer("Odour?", "Plains", "day");

        assertTrue(odour.isPresent());
        assertTrue(odour.get().contains("Hawthorn Sapling"), odour.get());

        Optional<String> reek = NpcKnowledgeBase.compactAnswer("Reek?", "Plains", "day");

        assertTrue(reek.isPresent());
        assertTrue(reek.get().contains("Alder Sapling"), reek.get());
    }

    @Test
    void clayJarCardMatchesRecipeJson() throws Exception {
        assertCardMatchesRecipe("how do I get clay jars?", "/data/witchery/recipe/clay_jar_soft.json");
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("how do I get clay jars?", "Plains", "day");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("smelt"), answer.get());
        assertTrue(lower.contains("clay jar"), answer.get());
    }

    @Test
    void ovenFumeCardsMatchOvenRecipeJson() throws Exception {
        assertOvenCardMatchesRecipe("how do I get Exhale of the Horned One?", "/data/witchery/recipe/oven/oak_sapling_to_exhale.json", true);
        assertOvenCardMatchesRecipe("how do I get Breath of the Goddess?", "/data/witchery/recipe/oven/birch_sapling_to_breath.json", true);
        assertOvenCardMatchesRecipe("how do I get Hint of Rebirth?", "/data/witchery/recipe/oven/spruce_sapling_to_hint.json", true);
        assertOvenCardMatchesRecipe("how do I get Reek of Misfortune?", "/data/witchery/recipe/oven/alder_sapling_to_reek.json", true);
        assertOvenCardMatchesRecipe("how do I get Odour of Purity?", "/data/witchery/recipe/oven/hawthorn_sapling_to_odour.json", true);
        assertOvenCardMatchesRecipe("how do I get Witches' Salt?", "/data/witchery/recipe/oven/kelp_to_salt.json", false);
    }

    @Test
    void primerCardIndexOnlyNamesValidMarkdownCards() throws Exception {
        String index = resourceText("/data/witchery/ai/cards/index.txt");
        Set<String> files = new LinkedHashSet<>();
        for (String rawLine : index.split("\\R")) {
            String line = rawLine.strip();
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            assertTrue(line.endsWith(".md"), "Card index entry must be markdown: " + line);
            assertTrue(files.add(line), "Duplicate card index entry: " + line);
            String card = resourceText("/data/witchery/ai/cards/" + line);
            assertTrue(!card.isBlank(), "Empty or missing card: " + line);
            assertTrue(card.contains("Targeted fact:"), "Card has no targeted facts: " + line);
        }
        assertTrue(!files.isEmpty(), "Card index should name at least one primer card");
    }

    @Test
    void primerCardIndexIncludesEveryMarkdownCardFile() throws Exception {
        Set<String> indexed = new LinkedHashSet<>(primerCardFiles());
        var indexResource = NpcKnowledgeBaseTest.class.getResource("/data/witchery/ai/cards/index.txt");

        assertTrue(indexResource != null, "Missing packaged card index resource");
        Path cardsDir = Path.of(indexResource.toURI()).getParent();
        assertTrue(Files.isDirectory(cardsDir), "Missing packaged card resource directory: " + cardsDir);
        try (var files = Files.list(cardsDir)) {
            files.filter(path -> path.getFileName().toString().endsWith(".md"))
                    .map(path -> path.getFileName().toString())
                    .forEach(file -> assertTrue(indexed.contains(file), "Card file is not listed in index.txt: " + file));
        }
    }

    @Test
    void primerTargetedFactsAreSingleLineAndWellFormed() throws Exception {
        for (String file : primerCardFiles()) {
            String card = resourceText("/data/witchery/ai/cards/" + file);
            for (String rawLine : card.split("\\R")) {
                String line = rawLine.strip();
                if (!line.startsWith("- [triggers:")) {
                    continue;
                }
                assertTrue(line.contains("] Targeted fact:"), "Malformed targeted fact in " + file + ": " + line);
                String triggers = line.substring("- [triggers:".length(), line.indexOf(']')).strip();
                String fact = line.substring(line.indexOf("] Targeted fact:") + "] Targeted fact:".length()).strip();
                assertTrue(!triggers.isBlank(), "Missing triggers in " + file + ": " + line);
                assertTrue(!fact.isBlank(), "Missing fact text in " + file + ": " + line);
                assertTrue(fact.length() <= 260, "Targeted fact is too long for chat grounding in " + file + ": " + fact);
            }
        }
    }

    @Test
    void primerCardMarkdownDoesNotLeakAiImplementationLanguage() throws Exception {
        List<String> forbidden = List.of(
                "model", "prompt", "fallback", "json", "api",
                "visible page", "visible pages", "visible recipe", "visible brew",
                "chat promise", "chatty witch", "by chat", "knowledge card", "card file");
        for (String file : primerCardFiles()) {
            String lower = resourceText("/data/witchery/ai/cards/" + file).toLowerCase(Locale.ROOT);
            for (String phrase : forbidden) {
                assertTrue(!containsForbiddenImplementationPhrase(lower, phrase),
                        "Implementation phrase '" + phrase + "' leaked into " + file);
            }
        }
    }

    @Test
    void whiffCardMatchesOvenRecipeJson() throws Exception {
        JsonObject recipe = resourceJson("/data/witchery/recipe/oven/rowan_sapling_to_whiff.json");
        String ingredient = itemName(recipe.get("ingredient").getAsString());
        String result = itemName(recipe.getAsJsonObject("result").get("id").getAsString());
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("how do I get Whiff of Magic?", "Plains", "night");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains(ingredient), ingredient + " missing from: " + answer.get());
        assertTrue(lower.contains(result), result + " missing from: " + answer.get());
    }

    @Test
    void whiffUsageQuestionNamesRecipesInsteadOfSource() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer(
                "What specific recipe uses Whiff of Magic?", "Plains", "night");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("brew of sleeping"), answer.get());
        assertTrue(lower.contains("brew of love"), answer.get());
        assertTrue(!lower.contains("cooking a rowan sapling"), answer.get());
    }

    @Test
    void specificRecipeCardWinsOverGenericRecipeCard() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer(
                "what is the recipe for Brew of Revealing?", "Plains", "night");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("carrot"), answer.get());
        assertTrue(lower.contains("spider eye"), answer.get());
        assertTrue(lower.contains("night vision"), answer.get());
        assertTrue(!lower.contains("must come from"), answer.get());
    }

    @Test
    void retrievesMachineRecipeCard() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("how do I craft a kettle?", "Plains", "day");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("stick"), answer.get());
        assertTrue(lower.contains("string"), answer.get());
        assertTrue(lower.contains("attuned stone"), answer.get());
        assertTrue(lower.contains("cauldron"), answer.get());
    }

    @Test
    void retrievesRiteSafetyCard() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("what should I never do during a rite?", "Plains", "night");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("broken chalk"), answer.get());
        assertTrue(lower.contains("offerings"), answer.get());
        assertTrue(lower.contains("altar"), answer.get());
    }

    @Test
    void practicalTeachingCardsCoverStationFailures() {
        Optional<String> kettle = NpcKnowledgeBase.compactAnswer("why did the kettle fail?", "Plains", "night");
        Optional<String> distillery = NpcKnowledgeBase.compactAnswer("why did distillery fail?", "Plains", "night");
        Optional<String> spinning = NpcKnowledgeBase.compactAnswer("why did spinning wheel fail?", "Plains", "night");

        assertTrue(kettle.isPresent(), "No Kettle troubleshooting answer");
        assertTrue(kettle.get().toLowerCase(Locale.ROOT).contains("water"), kettle.get());
        assertTrue(kettle.get().toLowerCase(Locale.ROOT).contains("heat"), kettle.get());

        assertTrue(distillery.isPresent(), "No Distillery troubleshooting answer");
        assertTrue(distillery.get().toLowerCase(Locale.ROOT).contains("clay jar"), distillery.get());
        assertTrue(distillery.get().toLowerCase(Locale.ROOT).contains("altar"), distillery.get());

        assertTrue(spinning.isPresent(), "No Spinning Wheel troubleshooting answer");
        assertTrue(spinning.get().toLowerCase(Locale.ROOT).contains("altar"), spinning.get());
        assertTrue(spinning.get().toLowerCase(Locale.ROOT).contains("fibre")
                || spinning.get().toLowerCase(Locale.ROOT).contains("fiber"), spinning.get());
    }

    @Test
    void practicalTeachingCardsCarryPersonaSpecificVoice() {
        String witch = NpcKnowledgeBase.relevantContext("how do I use the oven?", "Plains", "day",
                NpcCardRequest.persona("coven_witch")).toLowerCase(Locale.ROOT);
        String villager = NpcKnowledgeBase.relevantContext("how do I use the oven?", "Plains", "day",
                NpcCardRequest.persona("villager")).toLowerCase(Locale.ROOT);
        String leonard = NpcKnowledgeBase.relevantContext("how do I use the oven?", "Plains", "day",
                NpcCardRequest.persona("leonard")).toLowerCase(Locale.ROOT);
        String baba = NpcKnowledgeBase.relevantContext("teach me a rite", "Swamp", "night",
                NpcCardRequest.persona("baba_yaga")).toLowerCase(Locale.ROOT);

        assertTrue(witch.contains("coven witch teaches the oven plainly"), witch);
        assertTrue(witch.contains("wood ash"), witch);
        assertTrue(villager.contains("witch business"), villager);
        assertTrue(villager.contains("primer") || villager.contains("coven witch"), villager);
        assertTrue(leonard.contains("lesser smoke-work"), leonard);
        assertTrue(leonard.contains("larger powers"), leonard);
        assertTrue(baba.contains("chalk makes the plate"), baba);
        assertTrue(baba.contains("fools stand too close"), baba);
    }

    @Test
    void indexedCardsWinLegacyCueTies() {
        assertFirstTargetedTopic("witches_oven", "witches oven", NpcCardRequest.any());
        assertFirstTargetedTopic("clay_jar", "clay jar", NpcCardRequest.any());
        assertFirstTargetedTopic("spinning_wheel", "spinning wheel", NpcCardRequest.any());
    }

    @Test
    void requestedPersonaCardsWinGenericCueTies() {
        assertFirstTargetedTopic("villager_witch_morality", "good witch", NpcCardRequest.persona("villager"));
        assertFirstTargetedTopic("hobgoblin_trade", "trade", NpcCardRequest.persona("hobgoblin"));
        assertFirstTargetedTopic("reflection_hearing", "can you hear me", NpcCardRequest.persona("reflection"));
        assertFirstTargetedTopic("leonard_world_contempt", "weather", NpcCardRequest.persona("leonard"));
        assertFirstTargetedTopic("witch_village_reputation", "good witch", NpcCardRequest.persona("coven_witch"));
    }

    @Test
    void retrievesRumorCueForRumorQuestions() {
        String context = NpcKnowledgeBase.relevantContext("can you tell me a village rumor?", "Plains", "night");

        assertTrue(context.toLowerCase(Locale.ROOT).contains("rumor"));
    }

    @Test
    void compactAnswerUsesSpecificNamedBrewBeforeBroadSpiritLore() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("Can you brew Sleeping Brew?", "Plains", "night");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("brew of sleeping"));
        assertTrue(lower.contains("not ordinary sleep"));
    }

    @Test
    void retrievesTargetedRiteLoreForRiteQuestions() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("can you teach me a rite?", "Plains", "night");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("circle"));
        assertTrue(lower.contains("offerings") || lower.contains("altar power"));
    }

    @Test
    void retrievesFamiliarLoreForOwlQuestions() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("why do witches care about owls?", "Plains", "night");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("owls"));
        assertTrue(lower.contains("night") || lower.contains("flight"));
    }

    @Test
    void retrievesThreatKnowledgeForDemonQuestions() {
        String context = NpcKnowledgeBase.relevantContext("how do demons behave?", "Nether Wastes", "night");
        String lower = context.toLowerCase(Locale.ROOT);

        assertTrue(lower.contains("demon"));
        assertTrue(lower.contains("summon") || lower.contains("bargain") || lower.contains("fire"));
    }

    @Test
    void compactAnswerFindsSpecificItemNotes() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("what does arthana do?", "Plains", "day");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("arthana"));
        assertTrue(lower.contains("ritual blade") || lower.contains("altar range"));
    }

    @Test
    void primerPathAnswersBeginnerProgressionQuestions() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("what should I build first?", "Plains", "day");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("oven"), answer.get());
        assertTrue(lower.contains("wood ash") || lower.contains("fumes"), answer.get());
        assertTrue(lower.contains("mutandis"), answer.get());
    }

    @Test
    void primerPathGivesOneTeachingStepForVagueLessons() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("teach me something", "Plains", "day");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("one useful next step"), answer.get());
        assertTrue(lower.contains("station") || lower.contains("reagent") || lower.contains("page"), answer.get());
    }

    @Test
    void circleMagicBookCardMatchesRecipeJson() throws Exception {
        assertCardMatchesRecipe("how do I start circle magic?", "/data/witchery/recipe/book_circle_magic.json");
    }

    @Test
    void taglockKitCardMatchesRecipeJson() throws Exception {
        assertCardMatchesRecipe("how do I make a taglock kit?", "/data/witchery/recipe/taglock_kit.json");
    }

    @Test
    void retrievesMachineUseCards() {
        Optional<String> kettle = NpcKnowledgeBase.compactAnswer("how do I use the kettle?", "Plains", "day");

        assertTrue(kettle.isPresent());
        String kettleLower = kettle.get().toLowerCase(Locale.ROOT);
        assertTrue(kettleLower.contains("water"), kettle.get());
        assertTrue(kettleLower.contains("heat"), kettle.get());
        assertTrue(kettleLower.contains("altar"), kettle.get());

        Optional<String> distillery = NpcKnowledgeBase.compactAnswer("how do I use the distillery?", "Plains", "day");

        assertTrue(distillery.isPresent());
        String distilleryLower = distillery.get().toLowerCase(Locale.ROOT);
        assertTrue(distilleryLower.contains("clay jar"), distillery.get());
        assertTrue(distilleryLower.contains("altar"), distillery.get());
        assertTrue(distilleryLower.contains("power"), distillery.get());

        Optional<String> spinning = NpcKnowledgeBase.compactAnswer("how do I use the spinning wheel?", "Plains", "day");

        assertTrue(spinning.isPresent());
        String spinningLower = spinning.get().toLowerCase(Locale.ROOT);
        assertTrue(spinningLower.contains("fibre") || spinningLower.contains("fiber"), spinning.get());
        assertTrue(spinningLower.contains("altar"), spinning.get());
        assertTrue(spinningLower.contains("power"), spinning.get());
    }

    @Test
    void distilleryProductCardsMatchRecipeJson() throws Exception {
        assertDistilleryCardMatchesRecipe(
                "how do I make Diamond Vapour?", "/data/witchery/recipe/distillery/diamond_to_vapour.json");
        assertDistilleryCardMatchesRecipe(
                "how do I make Oil of Vitriol?", "/data/witchery/recipe/distillery/oil_of_vitriol.json");
        assertDistilleryCardMatchesRecipe(
                "how do I get Tear of the Goddess?", "/data/witchery/recipe/distillery/tear_of_goddess.json");
        assertDistilleryCardMatchesRecipe(
                "how do I make Refined Evil?", "/data/witchery/recipe/distillery/refined_evil.json");
        assertDistilleryCardMatchesRecipe(
                "how do I make Condensed Fear?", "/data/witchery/recipe/distillery/condensed_fear.json");
        assertDistilleryCardMatchesRecipe(
                "how do I get Ender Dew?", "/data/witchery/recipe/distillery/ender_dew.json");
        assertDistilleryCardMatchesRecipe(
                "how do I make Infernal Blood?", "/data/witchery/recipe/distillery/infernal_blood.json");
        assertDistilleryCardMatchesRecipe(
                "how do I get netherrack Infernal Blood?", "/data/witchery/recipe/distillery/infernal_blood_netherrack.json");
        assertDistilleryCardMatchesRecipe(
                "how do I distill Glowstone Dust?", "/data/witchery/recipe/distillery/glowstone_dust.json");
    }

    @Test
    void distilleryEssenceCardsAnswerUseAndFailureQuestions() {
        Optional<String> refined = NpcKnowledgeBase.compactAnswer("what is refined evil for?", "Plains", "night");

        assertTrue(refined.isPresent());
        String refinedLower = refined.get().toLowerCase(Locale.ROOT);
        assertTrue(refinedLower.contains("drop of luck"), refined.get());
        assertTrue(refinedLower.contains("darker workings"), refined.get());

        Optional<String> infernal = NpcKnowledgeBase.compactAnswer("what is infernal blood for?", "Plains", "night");

        assertTrue(infernal.isPresent());
        String infernalLower = infernal.get().toLowerCase(Locale.ROOT);
        assertTrue(infernalLower.contains("demonic craft"), infernal.get());
        assertTrue(infernalLower.contains("mystic unguent"), infernal.get());

        Optional<String> failed = NpcKnowledgeBase.compactAnswer("why did my distillery fail?", "Plains", "night");

        assertTrue(failed.isPresent());
        String failedLower = failed.get().toLowerCase(Locale.ROOT);
        assertTrue(failedLower.contains("clay jars"), failed.get());
        assertTrue(failedLower.contains("altar power"), failed.get());
        assertTrue(failedLower.contains("wrong two inputs"), failed.get());
    }

    @Test
    void spinningProductCardsMatchRecipeJson() throws Exception {
        assertSpinningCardMatchesRecipe(
                "how do I make Fanciful Thread?", "/data/witchery/recipe/spinning/cotton_to_thread.json");
        assertSpinningCardMatchesRecipe(
                "how do I make Golden Thread?", "/data/witchery/recipe/spinning/golden_thread.json");
        assertSpinningCardMatchesRecipe(
                "how do I make Golden Thread from hay?", "/data/witchery/recipe/spinning/hay_bale_to_golden_thread.json");
        assertSpinningCardMatchesRecipe(
                "how do I make Tormented Twine?", "/data/witchery/recipe/spinning/berries_to_thread.json");
    }

    @Test
    void threadworkAndFunnelCardsMatchRecipeJson() throws Exception {
        assertCardMatchesRecipe("how do I make impregnated leather?", "/data/witchery/recipe/impregnated_leather.json");
        assertCardMatchesRecipe("how do I make a fume funnel?", "/data/witchery/recipe/fume_funnel.json");
        assertCardMatchesRecipe("how do I make a fume filter?", "/data/witchery/recipe/fume_filter.json");
        assertCardMatchesRecipe("how do I make a filtered fume funnel?", "/data/witchery/recipe/filtered_fume_funnel.json");
        assertCardMatchesRecipe("how do I make golden chalk?", "/data/witchery/recipe/chalk_golden.json");
        assertCardMatchesRecipe("how do I turn thread to wool?", "/data/witchery/recipe/cloth_from_thread.json");
    }

    @Test
    void threadworkCardsAnswerUseAndFailureQuestions() {
        Optional<String> golden = NpcKnowledgeBase.compactAnswer("what is golden thread for?", "Plains", "night");

        assertTrue(golden.isPresent());
        String goldenLower = golden.get().toLowerCase(Locale.ROOT);
        assertTrue(goldenLower.contains("brew bags"), golden.get());
        assertTrue(goldenLower.contains("witches' robes"), golden.get());

        Optional<String> tormented = NpcKnowledgeBase.compactAnswer("what is tormented twine for?", "Plains", "night");

        assertTrue(tormented.isPresent());
        String tormentedLower = tormented.get().toLowerCase(Locale.ROOT);
        assertTrue(tormentedLower.contains("dream weavers"), tormented.get());
        assertTrue(tormentedLower.contains("nightmare"), tormented.get());

        Optional<String> failed = NpcKnowledgeBase.compactAnswer("why did my spinning wheel fail?", "Plains", "night");

        assertTrue(failed.isPresent());
        String failedLower = failed.get().toLowerCase(Locale.ROOT);
        assertTrue(failedLower.contains("wrong fibre"), failed.get());
        assertTrue(failedLower.contains("missing modifiers"), failed.get());
        assertTrue(failedLower.contains("altar power"), failed.get());

        Optional<String> funnel = NpcKnowledgeBase.compactAnswer("how do fume funnels work?", "Plains", "night");

        assertTrue(funnel.isPresent());
        String funnelLower = funnel.get().toLowerCase(Locale.ROOT);
        assertTrue(funnelLower.contains("top placement"), funnel.get());
        assertTrue(funnelLower.contains("side placement"), funnel.get());
        assertTrue(funnelLower.contains("filtered"), funnel.get());
    }

    @Test
    void circleChalkCardsMatchRecipeJson() throws Exception {
        assertCardMatchesRecipe("how do I make ritual chalk?", "/data/witchery/recipe/chalk.json");
        assertCardMatchesRecipe("what do I need for golden chalk?", "/data/witchery/recipe/chalk_golden.json");
        assertCardMatchesRecipe("what do I need for infernal chalk?", "/data/witchery/recipe/chalk_infernal.json");
        assertCardMatchesRecipe("what do I need for otherwhere chalk?", "/data/witchery/recipe/chalk_otherwhere.json");
        assertCardMatchesRecipe("what do I need for circle talisman?", "/data/witchery/recipe/circle_talisman.json");
        assertCardMatchesRecipe("what do I need for waystone?", "/data/witchery/recipe/waystone.json");
    }

    @Test
    void circleChalkCardsAnswerSetupQuestions() {
        Optional<String> chalk = NpcKnowledgeBase.compactAnswer("which chalk do I need?", "Plains", "night");

        assertTrue(chalk.isPresent());
        String chalkLower = chalk.get().toLowerCase(Locale.ROOT);
        assertTrue(chalkLower.contains("white ritual chalk"), chalk.get());
        assertTrue(chalkLower.contains("golden chalk"), chalk.get());
        assertTrue(chalkLower.contains("infernal chalk"), chalk.get());
        assertTrue(chalkLower.contains("otherwhere chalk"), chalk.get());

        Optional<String> failed = NpcKnowledgeBase.compactAnswer("why did my circle fail?", "Plains", "night");

        assertTrue(failed.isPresent());
        String failedLower = failed.get().toLowerCase(Locale.ROOT);
        assertTrue(failedLower.contains("wrong chalk color"), failed.get());
        assertTrue(failedLower.contains("weak altar power"), failed.get());
        assertTrue(failedLower.contains("bad offerings"), failed.get());

        Optional<String> size = NpcKnowledgeBase.compactAnswer("how big should the circle be?", "Plains", "night");

        assertTrue(size.isPresent());
        String sizeLower = size.get().toLowerCase(Locale.ROOT);
        assertTrue(sizeLower.contains("opened rite lesson"), size.get());
        assertTrue(sizeLower.contains("copy the glyph layout exactly"), size.get());
    }

    @Test
    void brazierCardMatchesRecipeJson() throws Exception {
        assertCardMatchesRecipe("how do I make a brazier?", "/data/witchery/recipe/brazier.json");
    }

    @Test
    void brazierCardsAnswerSpectralConjurationQuestions() {
        Optional<String> spectre = NpcKnowledgeBase.compactAnswer(
                "how do I conjure Spectres in a brazier?", "Plains", "night");

        assertTrue(spectre.isPresent());
        String spectreLower = spectre.get().toLowerCase(Locale.ROOT);
        assertTrue(spectreLower.contains("wormwood"), spectre.get());
        assertTrue(spectreLower.contains("bat wool"), spectre.get());
        assertTrue(spectreLower.contains("graveyard dust"), spectre.get());
        assertTrue(spectreLower.contains("30 seconds"), spectre.get());
        assertTrue(spectreLower.contains("altar"), spectre.get());

        Optional<String> poltergeist = NpcKnowledgeBase.compactAnswer(
                "how do I conjure a Poltergeist in a brazier?", "Plains", "night");

        assertTrue(poltergeist.isPresent());
        String poltergeistLower = poltergeist.get().toLowerCase(Locale.ROOT);
        assertTrue(poltergeistLower.contains("wormwood"), poltergeist.get());
        assertTrue(poltergeistLower.contains("refined evil"), poltergeist.get());
        assertTrue(poltergeistLower.contains("focused will"), poltergeist.get());
        assertTrue(poltergeistLower.contains("45 seconds"), poltergeist.get());
        assertTrue(poltergeistLower.contains("no altar") || poltergeistLower.contains("needs no altar"), poltergeist.get());
    }

    @Test
    void brazierCardsAnswerMistAndTroubleshootingQuestions() {
        Optional<String> fizzing = NpcKnowledgeBase.compactAnswer(
                "why is my brazier fizzing?", "Plains", "night");

        assertTrue(fizzing.isPresent());
        String fizzingLower = fizzing.get().toLowerCase(Locale.ROOT);
        assertTrue(fizzingLower.contains("altar"), fizzing.get());
        assertTrue(fizzingLower.contains("16 blocks"), fizzing.get());
        assertTrue(fizzingLower.contains("timer pauses"), fizzing.get());

        Optional<String> clear = NpcKnowledgeBase.compactAnswer(
                "how do I clear a brazier?", "Plains", "night");

        assertTrue(clear.isPresent());
        String clearLower = clear.get().toLowerCase(Locale.ROOT);
        assertTrue(clearLower.contains("water bucket") || clearLower.contains("water bottle"), clear.get());
        assertTrue(clearLower.contains("wood ash"), clear.get());
        assertTrue(clearLower.contains("cancel"), clear.get());

        Optional<String> mist = NpcKnowledgeBase.compactAnswer(
                "what does Graveyard Mist need?", "Plains", "night");

        assertTrue(mist.isPresent());
        String mistLower = mist.get().toLowerCase(Locale.ROOT);
        assertTrue(mistLower.contains("nether wart"), mist.get());
        assertTrue(mistLower.contains("quicklime"), mist.get());
        assertTrue(mistLower.contains("blaze powder"), mist.get());
        assertTrue(mistLower.contains("smoke"), mist.get());
    }

    @Test
    void brazierCardsAnswerAuraAndQuickBurningQuestions() {
        Optional<String> deathlyVeil = NpcKnowledgeBase.compactAnswer(
                "what is Deathly Veil?", "Plains", "night");

        assertTrue(deathlyVeil.isPresent());
        String veilLower = deathlyVeil.get().toLowerCase(Locale.ROOT);
        assertTrue(veilLower.contains("fermented spider eye"), deathlyVeil.get());
        assertTrue(veilLower.contains("ender pearl"), deathlyVeil.get());
        assertTrue(veilLower.contains("diamond"), deathlyVeil.get());
        assertTrue(veilLower.contains("invisibility"), deathlyVeil.get());

        Optional<String> gunpowder = NpcKnowledgeBase.compactAnswer(
                "what quick brazier buff uses gunpowder?", "Plains", "night");

        assertTrue(gunpowder.isPresent());
        String gunpowderLower = gunpowder.get().toLowerCase(Locale.ROOT);
        assertTrue(gunpowderLower.contains("gunpowder"), gunpowder.get());
        assertTrue(gunpowderLower.contains("strength"), gunpowder.get());
        assertTrue(gunpowderLower.contains("speed"), gunpowder.get());
    }

    @Test
    void cauldronCardsAnswerStationSetupQuestions() {
        Optional<String> make = NpcKnowledgeBase.compactAnswer(
                "how do I make a Witch's Cauldron?", "Plains", "day");

        assertTrue(make.isPresent());
        String makeLower = make.get().toLowerCase(Locale.ROOT);
        assertTrue(makeLower.contains("anointing paste"), make.get());
        assertTrue(makeLower.contains("vanilla cauldron"), make.get());
        assertTrue(makeLower.contains("heat"), make.get());
        assertTrue(makeLower.contains("water"), make.get());

        Optional<String> use = NpcKnowledgeBase.compactAnswer(
                "how do I use a Witch's Cauldron?", "Plains", "day");

        assertTrue(use.isPresent());
        String useLower = use.get().toLowerCase(Locale.ROOT);
        assertTrue(useLower.contains("heat"), use.get());
        assertTrue(useLower.contains("water"), use.get());
        assertTrue(useLower.contains("dropped reagents"), use.get());
        assertTrue(useLower.contains("trigger item"), use.get());
    }

    @Test
    void cauldronCardsAnswerGreenWorkQuestions() {
        Optional<String> mutandis = NpcKnowledgeBase.compactAnswer(
                "how do I make Mutandis in a cauldron?", "Plains", "day");

        assertTrue(mutandis.isPresent());
        String mutandisLower = mutandis.get().toLowerCase(Locale.ROOT);
        assertTrue(mutandisLower.contains("mandrake root"), mutandis.get());
        assertTrue(mutandisLower.contains("egg"), mutandis.get());
        assertTrue(mutandisLower.contains("exhale of the horned one"), mutandis.get());
        assertTrue(mutandisLower.contains("six mutandis"), mutandis.get());

        Optional<String> luck = NpcKnowledgeBase.compactAnswer(
                "how do I make Drop of Luck in the cauldron?", "Plains", "day");

        assertTrue(luck.isPresent());
        String luckLower = luck.get().toLowerCase(Locale.ROOT);
        assertTrue(luckLower.contains("mandrake root"), luck.get());
        assertTrue(luckLower.contains("nether wart"), luck.get());
        assertTrue(luckLower.contains("tear of the goddess"), luck.get());
        assertTrue(luckLower.contains("refined evil"), luck.get());
        assertTrue(luckLower.contains("mutandis extremis"), luck.get());
        assertTrue(luckLower.contains("6930"), luck.get());
    }

    @Test
    void cauldronCardsAnswerChalkAndPitQuestions() {
        Optional<String> chalk = NpcKnowledgeBase.compactAnswer(
                "how do I upgrade chalk in the cauldron?", "Plains", "day");

        assertTrue(chalk.isPresent());
        String chalkLower = chalk.get().toLowerCase(Locale.ROOT);
        assertTrue(chalkLower.contains("golden"), chalk.get());
        assertTrue(chalkLower.contains("mandrake root"), chalk.get());
        assertTrue(chalkLower.contains("infernal"), chalk.get());
        assertTrue(chalkLower.contains("blaze powder"), chalk.get());
        assertTrue(chalkLower.contains("otherwhere"), chalk.get());
        assertTrue(chalkLower.contains("ender pearl"), chalk.get());

        Optional<String> pit = NpcKnowledgeBase.compactAnswer(
                "how do I make a false floor with the cauldron?", "Plains", "day");

        assertTrue(pit.isPresent());
        String pitLower = pit.get().toLowerCase(Locale.ROOT);
        assertTrue(pitLower.contains("tormented twine"), pit.get());
        assertTrue(pitLower.contains("pit dirt"), pit.get());
        assertTrue(pitLower.contains("pit grass"), pit.get());
        assertTrue(pitLower.contains("4000"), pit.get());
    }

    @Test
    void cauldronCardsAnswerLeonardQuestions() {
        Optional<String> leonard = NpcKnowledgeBase.compactAnswer(
                "how do I summon Leonard with the cauldron?", "Plains", "night");

        assertTrue(leonard.isPresent());
        String leonardLower = leonard.get().toLowerCase(Locale.ROOT);
        assertTrue(leonardLower.contains("nether wart"), leonard.get());
        assertTrue(leonardLower.contains("tear of the goddess"), leonard.get());
        assertTrue(leonardLower.contains("diamond vapour") || leonardLower.contains("diamond vapor"), leonard.get());
        assertTrue(leonardLower.contains("nether star"), leonard.get());
        assertTrue(leonardLower.contains("witches' hat") || leonardLower.contains("witches hat"), leonard.get());
        assertTrue(leonardLower.contains("10000"), leonard.get());

        Optional<String> urn = NpcKnowledgeBase.compactAnswer(
                "what is Leonard's Urn for?", "Plains", "night");

        assertTrue(urn.isPresent());
        String urnLower = urn.get().toLowerCase(Locale.ROOT);
        assertTrue(urnLower.contains("stores thrown brews"), urn.get());
        assertTrue(urnLower.contains("sneak-use"), urn.get());
        assertTrue(urnLower.contains("four slots"), urn.get());
    }

    @Test
    void symbologyCardsAnswerBranchSetupAndUseQuestions() {
        Optional<String> branch = NpcKnowledgeBase.compactAnswer(
                "how do I get a Mystic Branch?", "Plains", "night");

        assertTrue(branch.isPresent());
        String branchLower = branch.get().toLowerCase(Locale.ROOT);
        assertTrue(branchLower.contains("night"), branch.get());
        assertTrue(branchLower.contains("ent twig"), branch.get());
        assertTrue(branchLower.contains("mystic unguent"), branch.get());
        assertTrue(branchLower.contains("3000"), branch.get());

        Optional<String> use = NpcKnowledgeBase.compactAnswer(
                "how do I use the Mystic Branch?", "Plains", "night");

        assertTrue(use.isPresent());
        String useLower = use.get().toLowerCase(Locale.ROOT);
        assertTrue(useLower.contains("hold use"), use.get());
        assertTrue(useLower.contains("up"), use.get());
        assertTrue(useLower.contains("down"), use.get());
        assertTrue(useLower.contains("left"), use.get());
        assertTrue(useLower.contains("right"), use.get());
        assertTrue(useLower.contains("hud"), use.get());
        assertTrue(useLower.contains("infused energy"), use.get());

        Optional<String> fail = NpcKnowledgeBase.compactAnswer(
                "why does my Mystic Branch fizzle?", "Plains", "night");

        assertTrue(fail.isPresent());
        String failLower = fail.get().toLowerCase(Locale.ROOT);
        assertTrue(failLower.contains("no symbol"), fail.get());
        assertTrue(failLower.contains("infused energy"), fail.get());
        assertTrue(failLower.contains("infusion"), fail.get());
        assertTrue(failLower.contains("demon-soul") || failLower.contains("demon soul"), fail.get());
    }

    @Test
    void symbologyCardsAnswerSpellAndDemonicGlyphQuestions() {
        Optional<String> spells = NpcKnowledgeBase.compactAnswer(
                "what can the Mystic Branch do?", "Plains", "night");

        assertTrue(spells.isPresent());
        String spellsLower = spells.get().toLowerCase(Locale.ROOT);
        assertTrue(spellsLower.contains("accio"), spells.get());
        assertTrue(spellsLower.contains("episkey"), spells.get());
        assertTrue(spellsLower.contains("expelliarmus"), spells.get());
        assertTrue(spellsLower.contains("flipendo"), spells.get());
        assertTrue(spellsLower.contains("incendio"), spells.get());

        Optional<String> utility = NpcKnowledgeBase.compactAnswer(
                "what does Alohomora do?", "Plains", "night");

        assertTrue(utility.isPresent());
        String utilityLower = utility.get().toLowerCase(Locale.ROOT);
        assertTrue(utilityLower.contains("alohomora"), utility.get());
        assertTrue(utilityLower.contains("doors"), utility.get());
        assertTrue(utilityLower.contains("defodio"), utility.get());
        assertTrue(utilityLower.contains("protego"), utility.get());

        Optional<String> learn = NpcKnowledgeBase.compactAnswer(
                "how do I learn demonic glyphs?", "Plains", "night");

        assertTrue(learn.isPresent());
        String learnLower = learn.get().toLowerCase(Locale.ROOT);
        assertTrue(learnLower.contains("right-click"), learn.get());
        assertTrue(learnLower.contains("demon soul"), learn.get());
        assertTrue(learnLower.contains("consumes"), learn.get());

        Optional<String> morsmordre = NpcKnowledgeBase.compactAnswer(
                "which demon soul teaches Morsmordre?", "Plains", "night");

        assertTrue(morsmordre.isPresent());
        String morsLower = morsmordre.get().toLowerCase(Locale.ROOT);
        assertTrue(morsLower.contains("fear soul"), morsmordre.get());
        assertTrue(morsLower.contains("morsmordre"), morsmordre.get());
        assertTrue(morsLower.contains("torment soul"), morsmordre.get());
        assertTrue(morsLower.contains("tormentum"), morsmordre.get());
    }

    @Test
    void symbologyCardsAnswerInfusionCreaturePowerAndUrnQuestions() {
        Optional<String> hand = NpcKnowledgeBase.compactAnswer(
                "what is a Witches Hand?", "Plains", "night");

        assertTrue(hand.isPresent());
        String handLower = hand.get().toLowerCase(Locale.ROOT);
        assertTrue(handLower.contains("light"), hand.get());
        assertTrue(handLower.contains("overworld"), hand.get());
        assertTrue(handLower.contains("otherwhere"), hand.get());
        assertTrue(handLower.contains("infernal"), hand.get());

        Optional<String> powers = NpcKnowledgeBase.compactAnswer(
                "how do creature powers work?", "Plains", "night");

        assertTrue(powers.isPresent());
        String powersLower = powers.get().toLowerCase(Locale.ROOT);
        assertTrue(powersLower.contains("altar"), powers.get());
        assertTrue(powersLower.contains("50 power"), powers.get());
        assertTrue(powersLower.contains("thralls"), powers.get());

        Optional<String> list = NpcKnowledgeBase.compactAnswer(
                "which creature powers can I get?", "Plains", "night");

        assertTrue(list.isPresent());
        String listLower = list.get().toLowerCase(Locale.ROOT);
        assertTrue(listLower.contains("spider"), list.get());
        assertTrue(listLower.contains("creeper"), list.get());
        assertTrue(listLower.contains("enderman"), list.get());
        assertTrue(listLower.contains("heal"), list.get());

        Optional<String> urn = NpcKnowledgeBase.compactAnswer(
                "what are Caelum and Baratrum?", "Plains", "night");

        assertTrue(urn.isPresent());
        String urnLower = urn.get().toLowerCase(Locale.ROOT);
        assertTrue(urnLower.contains("caelum"), urn.get());
        assertTrue(urnLower.contains("baratrum"), urn.get());
        assertTrue(urnLower.contains("leonard's urn") || urnLower.contains("leonards urn"), urn.get());
        assertTrue(urnLower.contains("one through four"), urn.get());
    }

    @Test
    void teachingPrimerAnswersRecipeDisciplineQuestions() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("how do I learn real recipes?", "Plains", "day");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("witches' primer") || lower.contains("witches primer"), answer.get());
        assertTrue(lower.contains("recipe pages") && lower.contains("opened"), answer.get());
        assertTrue(lower.contains("exact layout"), answer.get());
    }

    @Test
    void teachingPrimerAnswersStationChoiceQuestions() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("which machine do I need for this?", "Plains", "day");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("oven"), answer.get());
        assertTrue(lower.contains("kettle"), answer.get());
        assertTrue(lower.contains("distillery"), answer.get());
        assertTrue(lower.contains("spinning wheel"), answer.get());
        assertTrue(!lower.contains("belladonna"), answer.get());
    }

    @Test
    void teachingPrimerAnswersBrewTroubleshootingQuestions() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("my brew failed in the kettle", "Plains", "day");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("water"), answer.get());
        assertTrue(lower.contains("heat"), answer.get());
        assertTrue(lower.contains("ingredients from the opened recipe"), answer.get());
        assertTrue(lower.contains("altar power"), answer.get());
    }

    @Test
    void teachingPrimerAnswersRiteTroubleshootingQuestions() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("why won't my rite start?", "Plains", "night");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("chalk"), answer.get());
        assertTrue(lower.contains("offerings"), answer.get());
        assertTrue(lower.contains("altar power"), answer.get());
        assertTrue(lower.contains("unlock"), answer.get());
    }

    @Test
    void newCovenCardsAnswerRecruitingAndFamiliarQuestions() {
        Optional<String> coven = NpcKnowledgeBase.compactAnswer(
                "where do I find people for a coven?", "Plains", "night");

        assertTrue(coven.isPresent());
        String covenLower = coven.get().toLowerCase(Locale.ROOT);
        assertTrue(covenLower.contains("wandering coven witches"), coven.get());
        assertTrue(covenLower.contains("bound familiar"), coven.get());
        assertTrue(covenLower.contains("trial"), coven.get());

        Optional<String> familiar = NpcKnowledgeBase.compactAnswer(
                "how do I approach a familiar without angering it?", "Forest", "day");

        assertTrue(familiar.isPresent());
        String familiarLower = familiar.get().toLowerCase(Locale.ROOT);
        assertTrue(familiarLower.contains("patience"), familiar.get());
        assertTrue(familiarLower.contains("proper binding lesson"), familiar.get());
    }

    @Test
    void newSpiritCardsAnswerWellAndNightmareQuestions() {
        Optional<String> well = NpcKnowledgeBase.compactAnswer(
                "I heard whispers by the well. Should I answer?", "Plains", "night");

        assertTrue(well.isPresent());
        String wellLower = well.get().toLowerCase(Locale.ROOT);
        assertTrue(wellLower.contains("well"), well.get());
        assertTrue(wellLower.contains("salt"), well.get());
        assertTrue(wellLower.contains("not answer alone") || wellLower.contains("do not answer alone"), well.get());

        Optional<String> nightmares = NpcKnowledgeBase.compactAnswer(
                "I am afraid of nightmares.", "Plains", "night");

        assertTrue(nightmares.isPresent());
        String nightmareLower = nightmares.get().toLowerCase(Locale.ROOT);
        assertTrue(nightmareLower.contains("nightmares are real"), nightmares.get());
        assertTrue(nightmareLower.contains("spirit world"), nightmares.get());
    }

    @Test
    void newPoppetCardsAnswerTaglockAndCurseProtectionQuestions() {
        Optional<String> taglock = NpcKnowledgeBase.compactAnswer(
                "can someone use my taglock against me?", "Plains", "night");

        assertTrue(taglock.isPresent());
        String taglockLower = taglock.get().toLowerCase(Locale.ROOT);
        assertTrue(taglockLower.contains("hostile sympathetic magic"), taglock.get());
        assertTrue(taglockLower.contains("protection poppets"), taglock.get());

        Optional<String> protection = NpcKnowledgeBase.compactAnswer(
                "how do I protect against curses?", "Plains", "night");

        assertTrue(protection.isPresent());
        String protectionLower = protection.get().toLowerCase(Locale.ROOT);
        assertTrue(protectionLower.contains("voodoo protection poppets"), protection.get());
        assertTrue(protectionLower.contains("cat familiars"), protection.get());
    }

    @Test
    void newDangerAndOmenCardsAnswerImpAndCursedRainQuestions() {
        Optional<String> imp = NpcKnowledgeBase.compactAnswer(
                "what is a flame imp?", "Plains", "night");

        assertTrue(imp.isPresent());
        String impLower = imp.get().toLowerCase(Locale.ROOT);
        assertTrue(impLower.contains("demonic contract"), imp.get());
        assertTrue(impLower.contains("25 experience"), imp.get());

        Optional<String> omen = NpcKnowledgeBase.compactAnswer(
                "the rain makes this place feel cursed. is it?", "Plains", "night");

        assertTrue(omen.isPresent());
        String omenLower = omen.get().toLowerCase(Locale.ROOT);
        assertTrue(omenLower.contains("rain is not a curse by itself"), omen.get());
        assertTrue(omenLower.contains("strange wells"), omen.get());
    }

    @Test
    void newBookCardsMatchRecipeJson() throws Exception {
        assertCardMatchesRecipe("how do I make Book of Herbology?", "/data/witchery/recipe/book_herbology.json");
        assertCardMatchesRecipe("how do I make Book of the Oven?", "/data/witchery/recipe/book_oven.json");
        assertCardMatchesRecipe("how do I make Book of Distilling?", "/data/witchery/recipe/book_distilling.json");
        assertCardMatchesRecipe("how do I make Book of Circle Magic?", "/data/witchery/recipe/book_circle_magic.json");
        assertCardMatchesRecipe("how do I make Book of Poppets?", "/data/witchery/recipe/book_poppets.json");
        assertCardMatchesRecipe("how do I make Book of Spirit World?", "/data/witchery/recipe/book_spirit_world.json");
        assertCardMatchesRecipe("how do I make Book of Creatures?", "/data/witchery/recipe/book_creatures.json");
        assertCardMatchesRecipe("how do I make Book of Infusions?", "/data/witchery/recipe/book_infusions.json");
    }

    @Test
    void newScryingCardsMatchRecipeJson() throws Exception {
        assertCardMatchesRecipe("how do I make a waystone?", "/data/witchery/recipe/waystone.json");
        assertCardMatchesRecipe("how do I make a poppet shelf?", "/data/witchery/recipe/poppet_shelf.json");
        assertCardMatchesRecipe("how do I make a poppet shelf compass?", "/data/witchery/recipe/poppet_shelf_compass.json");
        assertCardMatchesRecipe("how do I make a charm of fanciful thinking?", "/data/witchery/recipe/charm_of_disrupted_dreams.json");
    }

    @Test
    void newScryingCardsAnswerUseQuestions() {
        Optional<String> crystalBall = NpcKnowledgeBase.compactAnswer(
                "how do I scry with a crystal ball?", "Plains", "night");

        assertTrue(crystalBall.isPresent());
        String crystalLower = crystalBall.get().toLowerCase(Locale.ROOT);
        assertTrue(crystalLower.contains("powered altar"), crystalBall.get());
        assertTrue(crystalLower.contains("taglock"), crystalBall.get());
        assertTrue(crystalLower.contains("waystone"), crystalBall.get());

        Optional<String> bound = NpcKnowledgeBase.compactAnswer(
                "what is a bound waystone?", "Plains", "night");

        assertTrue(bound.isPresent());
        String boundLower = bound.get().toLowerCase(Locale.ROOT);
        assertTrue(boundLower.contains("location"), bound.get());
        assertTrue(boundLower.contains("scrying"), bound.get());
    }

    @Test
    void newCreatureCardsAnswerWorldQuestions() {
        Optional<String> hobgoblin = NpcKnowledgeBase.compactAnswer(
                "what are hobgoblins?", "Forest", "day");

        assertTrue(hobgoblin.isPresent());
        String hobLower = hobgoblin.get().toLowerCase(Locale.ROOT);
        assertTrue(hobLower.contains("koboldite"), hobgoblin.get());
        assertTrue(hobLower.contains("villagers"), hobgoblin.get());

        Optional<String> treefyd = NpcKnowledgeBase.compactAnswer(
                "what is a treefyd?", "Forest", "day");

        assertTrue(treefyd.isPresent());
        String treefydLower = treefyd.get().toLowerCase(Locale.ROOT);
        assertTrue(treefydLower.contains("guardian"), treefyd.get());
        assertTrue(treefydLower.contains("grass"), treefyd.get());

        Optional<String> ent = NpcKnowledgeBase.compactAnswer(
                "will angry trees attack if I cut rowan?", "Forest", "day");

        assertTrue(ent.isPresent());
        String entLower = ent.get().toLowerCase(Locale.ROOT);
        assertTrue(entLower.contains("rowan"), ent.get());
        assertTrue(entLower.contains("hawthorn"), ent.get());
    }

    @Test
    void settlementHunterCardsAnswerHunterAndGuardQuestions() {
        Optional<String> hunters = NpcKnowledgeBase.compactAnswer(
                "why are witch hunters after me?", "Plains", "night");

        assertTrue(hunters.isPresent());
        String huntersLower = hunters.get().toLowerCase(Locale.ROOT);
        assertTrue(huntersLower.contains("vampires"), hunters.get());
        assertTrue(huntersLower.contains("werewolves"), hunters.get());
        assertTrue(huntersLower.contains("silver bolts"), hunters.get());

        Optional<String> transformed = NpcKnowledgeBase.compactAnswer(
                "why are witch hunters after transformed folk?", "Plains", "night");

        assertTrue(transformed.isPresent());
        assertTrue(transformed.get().toLowerCase(Locale.ROOT).contains("silver bolts"), transformed.get());

        Optional<String> silver = NpcKnowledgeBase.compactAnswer(
                "what does silver damage do to werewolves?", "Plains", "night");

        assertTrue(silver.isPresent());
        String silverLower = silver.get().toLowerCase(Locale.ROOT);
        assertTrue(silverLower.contains("silver weapons"), silver.get());
        assertTrue(silverLower.contains("silver bolts"), silver.get());
        assertTrue(silverLower.contains("bonus damage"), silver.get());

        Optional<String> silverPlain = NpcKnowledgeBase.compactAnswer(
                "what does silver do to werewolves?", "Plains", "night");

        assertTrue(silverPlain.isPresent());
        assertTrue(silverPlain.get().toLowerCase(Locale.ROOT).contains("bonus damage"), silverPlain.get());

        Optional<String> armor = NpcKnowledgeBase.compactAnswer(
                "what does silvered hunter armor do?", "Plains", "night");

        assertTrue(armor.isPresent());
        String armorLower = armor.get().toLowerCase(Locale.ROOT);
        assertTrue(armorLower.contains("werewolf"), armor.get());
        assertTrue(armorLower.contains("1 damage"), armor.get());

        Optional<String> guards = NpcKnowledgeBase.compactAnswer(
                "why are the village guards shouting at me?", "Plains", "night");

        assertTrue(guards.isPresent());
        String guardsLower = guards.get().toLowerCase(Locale.ROOT);
        assertTrue(guardsLower.contains("monsters"), guards.get());
        assertTrue(guardsLower.contains("vampires"), guards.get());
        assertTrue(guardsLower.contains("werewolves"), guards.get());

        Optional<String> shouted = NpcKnowledgeBase.compactAnswer(
                "a village guard shouted at me. what did i do?", "Plains", "night");

        assertTrue(shouted.isPresent());
        assertTrue(shouted.get().toLowerCase(Locale.ROOT).contains("guards"), shouted.get());

        Optional<String> apothecaries = NpcKnowledgeBase.compactAnswer(
                "are apothecaries coven witches?", "Plains", "night");

        assertTrue(apothecaries.isPresent());
        assertTrue(apothecaries.get().toLowerCase(Locale.ROOT).contains("not coven witches"), apothecaries.get());
    }

    @Test
    void settlementHunterCardsAnswerHobgoblinAndRecipeQuestions() throws Exception {
        Optional<String> trade = NpcKnowledgeBase.compactAnswer(
                "how do hobgoblin trades work?", "Forest", "day");

        assertTrue(trade.isPresent());
        String tradeLower = trade.get().toLowerCase(Locale.ROOT);
        assertTrue(tradeLower.contains("five steps"), trade.get());
        assertTrue(tradeLower.contains("coal"), trade.get());
        assertTrue(tradeLower.contains("koboldite"), trade.get());

        Optional<String> patron = NpcKnowledgeBase.compactAnswer(
                "what happens if I use the statue of hobgoblin patron?", "Forest", "night");

        assertTrue(patron.isPresent());
        String patronLower = patron.get().toLowerCase(Locale.ROOT);
        assertTrue(patronLower.contains("gulg"), patron.get());
        assertTrue(patronLower.contains("mog"), patron.get());
        assertTrue(patronLower.contains("hostile"), patron.get());

        assertCardMatchesRecipe("how do I make a koboldite pickaxe?", "/data/witchery/recipe/koboldite_pickaxe.json");
        assertCardMatchesRecipe("how do I make a twisting band?", "/data/witchery/recipe/twisting_band.json");
    }

    @Test
    void creatureReagentCardsAnswerDropQuestions() {
        Optional<String> bat = NpcKnowledgeBase.compactAnswer(
                "how do I get Bat Wool?", "Forest", "night");

        assertTrue(bat.isPresent());
        String batLower = bat.get().toLowerCase(Locale.ROOT);
        assertTrue(batLower.contains("bats"), bat.get());
        assertTrue(batLower.contains("arthana"), bat.get());
        assertTrue(batLower.contains("redstone soup"), bat.get());

        Optional<String> tongue = NpcKnowledgeBase.compactAnswer(
                "where do I get Tongue of Dog?", "Forest", "night");

        assertTrue(tongue.isPresent());
        String tongueLower = tongue.get().toLowerCase(Locale.ROOT);
        assertTrue(tongueLower.contains("wolves"), tongue.get());
        assertTrue(tongueLower.contains("hellhounds"), tongue.get());
        assertTrue(tongueLower.contains("arthana"), tongue.get());

        Optional<String> frog = NpcKnowledgeBase.compactAnswer(
                "where do I get Toe of Frog?", "Swamp", "night");

        assertTrue(frog.isPresent());
        String frogLower = frog.get().toLowerCase(Locale.ROOT);
        assertTrue(frogLower.contains("toads"), frog.get());
        assertTrue(frogLower.contains("arthana"), frog.get());

        Optional<String> spectral = NpcKnowledgeBase.compactAnswer(
                "how do I get Spectral Dust?", "Plains", "night");

        assertTrue(spectral.isPresent());
        String spectralLower = spectral.get().toLowerCase(Locale.ROOT);
        assertTrue(spectralLower.contains("spectral familiars"), spectral.get());
        assertTrue(spectralLower.contains("undead"), spectral.get());
        assertTrue(spectralLower.contains("arthana"), spectral.get());

        Optional<String> demonHeart = NpcKnowledgeBase.compactAnswer(
                "where do I get Demon Heart?", "Nether Wastes", "night");

        assertTrue(demonHeart.isPresent());
        String demonLower = demonHeart.get().toLowerCase(Locale.ROOT);
        assertTrue(demonLower.contains("killing") || demonLower.contains("trading"), demonHeart.get());
        assertTrue(demonLower.contains("arthana"), demonHeart.get());
    }

    @Test
    void creatureReagentCardsMatchRecipeJson() throws Exception {
        assertCardMatchesRecipe("what do I need for Polynesia Charm?", "/data/witchery/recipe/polynesia_charm.json");
        assertCardMatchesRecipe("what do I need for Devil's Tongue Charm?", "/data/witchery/recipe/devils_tongue_charm.json");
        assertCardMatchesRecipe("what do I need for Demonic Contract?", "/data/witchery/recipe/demonic_contract.json");
        assertCardMatchesRecipe("what do I need for Frozen Heart?", "/data/witchery/recipe/frozen_heart.json");
        assertCardMatchesRecipe("what do I need for Brew of Frogs Tongue?", "/data/witchery/recipe/brew_of_frogs_tongue.json");
    }

    @Test
    void creatureReagentCardsAnswerTradeAndArthanaQuestions() {
        Optional<String> animalTrade = NpcKnowledgeBase.compactAnswer(
                "how do I trade with animals?", "Forest", "day");

        assertTrue(animalTrade.isPresent());
        String tradeLower = animalTrade.get().toLowerCase(Locale.ROOT);
        assertTrue(tradeLower.contains("calm adult"), animalTrade.get());
        assertTrue(tradeLower.contains("bat wool"), animalTrade.get());
        assertTrue(tradeLower.contains("spectral dust"), animalTrade.get());

        Optional<String> arthana = NpcKnowledgeBase.compactAnswer(
                "what should I kill with Arthana?", "Forest", "night");

        assertTrue(arthana.isPresent());
        String arthanaLower = arthana.get().toLowerCase(Locale.ROOT);
        assertTrue(arthanaLower.contains("bat wool"), arthana.get());
        assertTrue(arthanaLower.contains("tongue of dog"), arthana.get());
        assertTrue(arthanaLower.contains("creeper heart"), arthana.get());
    }

    @Test
    void newPracticalRiteCardsAnswerCommonRiteQuestions() throws Exception {
        assertCardMatchesRecipe("how do I make a circle talisman?", "/data/witchery/recipe/circle_talisman.json");

        Optional<String> remove = NpcKnowledgeBase.compactAnswer(
                "how do I remove a curse?", "Plains", "night");

        assertTrue(remove.isPresent());
        String removeLower = remove.get().toLowerCase(Locale.ROOT);
        assertTrue(removeLower.contains("2000 altar power"), remove.get());
        assertTrue(removeLower.contains("taglock"), remove.get());
        assertTrue(removeLower.contains("small ritual"), remove.get());
        assertTrue(removeLower.contains("breath of the goddess"), remove.get());
        assertTrue(removeLower.contains("vampire removal"), remove.get());

        Optional<String> transpose = NpcKnowledgeBase.compactAnswer(
                "how does rite of transposition move a player?", "Plains", "night");

        assertTrue(transpose.isPresent());
        String transposeLower = transpose.get().toLowerCase(Locale.ROOT);
        assertTrue(transposeLower.contains("taglock"), transpose.get());
        assertTrue(transposeLower.contains("3000 altar power"), transpose.get());

        Optional<String> protection = NpcKnowledgeBase.compactAnswer(
                "what does a rite of protection do?", "Plains", "night");

        assertTrue(protection.isPresent());
        String protectionLower = protection.get().toLowerCase(Locale.ROOT);
        assertTrue(protectionLower.contains("barrier"), protection.get());
        assertTrue(protectionLower.contains("obsidian"), protection.get());
    }

    @Test
    void newDreamWeaverCardsAnswerSleepQuestions() {
        Optional<String> general = NpcKnowledgeBase.compactAnswer(
                "what does a dream weaver do?", "Plains", "night");

        assertTrue(general.isPresent());
        String generalLower = general.get().toLowerCase(Locale.ROOT);
        assertTrue(generalLower.contains("fleet foot"), general.get());
        assertTrue(generalLower.contains("iron arm"), general.get());
        assertTrue(generalLower.contains("intensity"), general.get());

        Optional<String> oneCatcher = NpcKnowledgeBase.compactAnswer(
                "can I stack many dream catchers?", "Plains", "night");

        assertTrue(oneCatcher.isPresent());
        String oneLower = oneCatcher.get().toLowerCase(Locale.ROOT);
        assertTrue(oneLower.contains("only one"), oneCatcher.get());

        Optional<String> intensity = NpcKnowledgeBase.compactAnswer(
                "how do I make an intensity dream weave?", "Plains", "night");

        assertTrue(intensity.isPresent());
        String intensityLower = intensity.get().toLowerCase(Locale.ROOT);
        assertTrue(intensityLower.contains("brew of flowing spirit"), intensity.get());
        assertTrue(intensityLower.contains("brew of sleeping"), intensity.get());
    }

    @Test
    void newTransformationCardsAnswerVampireWerewolfAndInfusionQuestions() {
        Optional<String> vampire = NpcKnowledgeBase.compactAnswer(
                "what are vampire weaknesses?", "Plains", "day");

        assertTrue(vampire.isPresent());
        String vampireLower = vampire.get().toLowerCase(Locale.ROOT);
        assertTrue(vampireLower.contains("sunlight"), vampire.get());
        assertTrue(vampireLower.contains("silver"), vampire.get());
        assertTrue(vampireLower.contains("witch hunter"), vampire.get());

        Optional<String> werewolf = NpcKnowledgeBase.compactAnswer(
                "what does lycanthropy do at the full moon?", "Plains", "night");

        assertTrue(werewolf.isPresent());
        String wolfLower = werewolf.get().toLowerCase(Locale.ROOT);
        assertTrue(wolfLower.contains("full moon"), werewolf.get());
        assertTrue(wolfLower.contains("wolf altar"), werewolf.get());

        Optional<String> infusion = NpcKnowledgeBase.compactAnswer(
                "how do lasting infusions begin?", "Plains", "night");

        assertTrue(infusion.isPresent());
        String infusionLower = infusion.get().toLowerCase(Locale.ROOT);
        assertTrue(infusionLower.contains("redstone soup"), infusion.get());
        assertTrue(infusionLower.contains("4000 altar power"), infusion.get());
    }

    @Test
    void infusionPowerCardsMatchSourceItemRecipeJson() throws Exception {
        assertCardMatchesRecipe("what do I need for Ghost of the Light?", "/data/witchery/recipe/ghost_of_the_light.json");
        assertCardMatchesRecipe("what do I need for Soul of the World?", "/data/witchery/recipe/soul_of_the_world.json");
        assertCardMatchesRecipe("what do I need for Spirit of Otherwhere?", "/data/witchery/recipe/spirit_of_otherwhere.json");
        assertCardMatchesRecipe("what do I need for Infernal Animus?", "/data/witchery/recipe/infernal_animus.json");
    }

    @Test
    void infusionPowerCardsAnswerChoiceRechargeAndUseQuestions() {
        Optional<String> choose = NpcKnowledgeBase.compactAnswer(
                "which infusion should I choose?", "Plains", "night");

        assertTrue(choose.isPresent());
        String chooseLower = choose.get().toLowerCase(Locale.ROOT);
        assertTrue(chooseLower.contains("light"), choose.get());
        assertTrue(chooseLower.contains("overworld"), choose.get());
        assertTrue(chooseLower.contains("otherwhere"), choose.get());
        assertTrue(chooseLower.contains("infernal"), choose.get());

        Optional<String> recharge = NpcKnowledgeBase.compactAnswer(
                "how do I recharge infusion energy?", "Plains", "night");

        assertTrue(recharge.isPresent());
        String rechargeLower = recharge.get().toLowerCase(Locale.ROOT);
        assertTrue(rechargeLower.contains("medium ritual circle"), recharge.get());
        assertTrue(rechargeLower.contains("regeneration potion"), recharge.get());
        assertTrue(rechargeLower.contains("4000 altar power"), recharge.get());

        Optional<String> hand = NpcKnowledgeBase.compactAnswer(
                "what can a witches hand do with infusion?", "Plains", "night");

        assertTrue(hand.isPresent());
        String handLower = hand.get().toLowerCase(Locale.ROOT);
        assertTrue(handLower.contains("light hides"), hand.get());
        assertTrue(handLower.contains("overworld shapes earth"), hand.get());
        assertTrue(handLower.contains("infernal commands"), hand.get());
    }

    @Test
    void infusionPowerCardsAnswerNamedPowerQuestions() {
        Optional<String> overworld = NpcKnowledgeBase.compactAnswer(
                "what does Overworld infusion do?", "Plains", "night");

        assertTrue(overworld.isPresent());
        String overworldLower = overworld.get().toLowerCase(Locale.ROOT);
        assertTrue(overworldLower.contains("natural ground"), overworld.get());
        assertTrue(overworldLower.contains("smelt ores"), overworld.get());
        assertTrue(overworldLower.contains("pull metal"), overworld.get());

        Optional<String> otherwhere = NpcKnowledgeBase.compactAnswer(
                "what does Otherwhere infusion do?", "Plains", "night");

        assertTrue(otherwhere.isPresent());
        String otherwhereLower = otherwhere.get().toLowerCase(Locale.ROOT);
        assertTrue(otherwhereLower.contains("slow falling"), otherwhere.get());
        assertTrue(otherwhereLower.contains("teleports"), otherwhere.get());
        assertTrue(otherwhereLower.contains("recall"), otherwhere.get());

        Optional<String> infernal = NpcKnowledgeBase.compactAnswer(
                "what does Infernal infusion do?", "Plains", "night");

        assertTrue(infernal.isPresent());
        String infernalLower = infernal.get().toLowerCase(Locale.ROOT);
        assertTrue(infernalLower.contains("fire resistance"), infernal.get());
        assertTrue(infernalLower.contains("creature powers"), infernal.get());
        assertTrue(infernalLower.contains("not beginner work"), infernal.get());
    }

    @Test
    void practicalTransformationCardsMatchCoreRecipeJson() throws Exception {
        assertCardMatchesRecipe("how do I make a coffin?", "/data/witchery/recipe/coffin.json");
        assertCardMatchesRecipe("how do I make a stake?", "/data/witchery/recipe/stake.json");
        assertCardMatchesRecipe("how do I make a glass goblet?", "/data/witchery/recipe/glass_goblet.json");
        assertCardMatchesRecipe("how do I make a wolftrap?", "/data/witchery/recipe/wolftrap.json");
        assertCardMatchesRecipe("how do I make a silver sword?", "/data/witchery/recipe/silver_sword.json");
        assertCardMatchesRecipe("how do I make a silver bolt?", "/data/witchery/recipe/silver_bolt.json");
        assertCardMatchesRecipe("how do I make Observations of an Immortal?", "/data/witchery/recipe/vampire_book.json");
    }

    @Test
    void practicalTransformationCardsAnswerVampireSurvivalQuestions() {
        Optional<String> blood = NpcKnowledgeBase.compactAnswer(
                "how does vampire blood power work?", "Plains", "night");

        assertTrue(blood.isPresent());
        String bloodLower = blood.get().toLowerCase(Locale.ROOT);
        assertTrue(bloodLower.contains("speed"), blood.get());
        assertTrue(bloodLower.contains("resistance"), blood.get());
        assertTrue(bloodLower.contains("frenzy"), blood.get());

        Optional<String> coffin = NpcKnowledgeBase.compactAnswer(
                "how does a vampire coffin work?", "Plains", "day");

        assertTrue(coffin.isPresent());
        String coffinLower = coffin.get().toLowerCase(Locale.ROOT);
        assertTrue(coffinLower.contains("shift-right-click"), coffin.get());
        assertTrue(coffinLower.contains("sleep safely through daylight"), coffin.get());

        Optional<String> mesmerise = NpcKnowledgeBase.compactAnswer(
                "can vampires mesmerise villagers?", "Plains", "night");

        assertTrue(mesmerise.isPresent());
        String mesmeriseLower = mesmerise.get().toLowerCase(Locale.ROOT);
        assertTrue(mesmeriseLower.contains("level-two"), mesmerise.get());
        assertTrue(mesmeriseLower.contains("50 blood power"), mesmerise.get());
    }

    @Test
    void practicalTransformationCardsAnswerWerewolfAndCounterplayQuestions() {
        Optional<String> moon = NpcKnowledgeBase.compactAnswer(
                "will the full moon force me to shift?", "Plains", "night");

        assertTrue(moon.isPresent());
        String moonLower = moon.get().toLowerCase(Locale.ROOT);
        assertTrue(moonLower.contains("full moon nights"), moon.get());
        assertTrue(moonLower.contains("moon charm"), moon.get());

        Optional<String> wolftrap = NpcKnowledgeBase.compactAnswer(
                "how do wolftraps work?", "Plains", "night");

        assertTrue(wolftrap.isPresent());
        String trapLower = wolftrap.get().toLowerCase(Locale.ROOT);
        assertTrue(trapLower.contains("beartrap"), wolftrap.get());
        assertTrue(trapLower.contains("silver"), wolftrap.get());
        assertTrue(trapLower.contains("full moon"), wolftrap.get());

        Optional<String> silver = NpcKnowledgeBase.compactAnswer(
                "does silver hurt werewolves and vampires?", "Plains", "night");

        assertTrue(silver.isPresent());
        String silverLower = silver.get().toLowerCase(Locale.ROOT);
        assertTrue(silverLower.contains("silver swords"), silver.get());
        assertTrue(silverLower.contains("silver bolts"), silver.get());
        assertTrue(silverLower.contains("transformed creatures"), silver.get());
    }

    @Test
    void tradeAndMannersCardsAnswerCommonSocialQuestions() {
        Optional<String> trade = NpcKnowledgeBase.compactAnswer(
                "what do you trade?", "Plains", "night");

        assertTrue(trade.isPresent());
        String tradeLower = trade.get().toLowerCase(Locale.ROOT);
        assertTrue(tradeLower.contains("belladonna"), trade.get());
        assertTrue(tradeLower.contains("mandrake"), trade.get());
        assertTrue(tradeLower.contains("right-click"), trade.get());

        Optional<String> herbGift = NpcKnowledgeBase.compactAnswer(
                "can I have a herb?", "Plains", "night");

        assertTrue(herbGift.isPresent());
        String herbLower = herbGift.get().toLowerCase(Locale.ROOT);
        assertTrue(herbLower.contains("one small herb gift"), herbGift.get());
        assertTrue(herbLower.contains("trade or grow"), herbGift.get());

        Optional<String> blessing = NpcKnowledgeBase.compactAnswer(
                "can you bless me?", "Plains", "night");

        assertTrue(blessing.isPresent());
        String blessingLower = blessing.get().toLowerCase(Locale.ROOT);
        assertTrue(blessingLower.contains("luck blessing"), blessing.get());
        assertTrue(blessingLower.contains("not endless miracles"), blessing.get());
    }

    @Test
    void newToolAndAltarRecipeCardsMatchRecipeJson() throws Exception {
        assertCardMatchesRecipe("how do I make an arthana?", "/data/witchery/recipe/arthana.json");
        assertCardMatchesRecipe("how do I make a boline?", "/data/witchery/recipe/boline.json");
        assertCardMatchesRecipe("how do I make a brew bag?", "/data/witchery/recipe/brew_bag.json");
        assertCardMatchesRecipe("how do I make a candelabra?", "/data/witchery/recipe/candelabra.json");
        assertCardMatchesRecipe("how do I make a chalice?", "/data/witchery/recipe/chalice.json");
        assertCardMatchesRecipe("how do I make a filled chalice?", "/data/witchery/recipe/chalice_filled.json");
        assertCardMatchesRecipe("how do I make a pentacle?", "/data/witchery/recipe/koboldite_pentacle.json");
        assertCardMatchesRecipe("how do I make a garlic garland?", "/data/witchery/recipe/garlic_garland.json");
        assertCardMatchesRecipe("how do I make a witch hat?", "/data/witchery/recipe/witches_hat.json");
        assertCardMatchesRecipe("how do I make witch robes?", "/data/witchery/recipe/witches_robes.json");
    }

    @Test
    void newToolAndAltarCardsAnswerUseQuestions() {
        Optional<String> weakAltar = NpcKnowledgeBase.compactAnswer(
                "why is my altar too weak?", "Plains", "night");

        assertTrue(weakAltar.isPresent());
        String altarLower = weakAltar.get().toLowerCase(Locale.ROOT);
        assertTrue(altarLower.contains("varied nearby nature"), weakAltar.get());
        assertTrue(altarLower.contains("filled chalices"), weakAltar.get());

        Optional<String> arthana = NpcKnowledgeBase.compactAnswer(
                "what does an arthana do?", "Plains", "night");

        assertTrue(arthana.isPresent());
        String arthanaLower = arthana.get().toLowerCase(Locale.ROOT);
        assertTrue(arthanaLower.contains("altar range"), arthana.get());
        assertTrue(arthanaLower.contains("rare witchery drops"), arthana.get());

        Optional<String> brewBag = NpcKnowledgeBase.compactAnswer(
                "how does a brew bag work?", "Plains", "night");

        assertTrue(brewBag.isPresent());
        String brewBagLower = brewBag.get().toLowerCase(Locale.ROOT);
        assertTrue(brewBagLower.contains("sneak-use"), brewBag.get());
        assertTrue(brewBagLower.contains("throw"), brewBag.get());

        Optional<String> leechChest = NpcKnowledgeBase.compactAnswer(
                "what is a leech chest?", "Plains", "night");

        assertTrue(leechChest.isPresent());
        String leechLower = leechChest.get().toLowerCase(Locale.ROOT);
        assertTrue(leechLower.contains("visitor samples"), leechChest.get());
        assertTrue(leechLower.contains("taglock kits"), leechChest.get());
    }

    @Test
    void herbologyCardsAnswerPlantSourceQuestions() {
        Optional<String> rowan = NpcKnowledgeBase.compactAnswer(
                "how do I get rowan saplings?", "Plains", "day");

        assertTrue(rowan.isPresent());
        String rowanLower = rowan.get().toLowerCase(Locale.ROOT);
        assertTrue(rowanLower.contains("mutandis"), rowan.get());
        assertTrue(rowanLower.contains("whiff of magic"), rowan.get());

        Optional<String> mandrake = NpcKnowledgeBase.compactAnswer(
                "where do I get mandrake seeds?", "Plains", "day");

        assertTrue(mandrake.isPresent());
        String mandrakeLower = mandrake.get().toLowerCase(Locale.ROOT);
        assertTrue(mandrakeLower.contains("tall grass"), mandrake.get());
        assertTrue(mandrakeLower.contains("earmuffs"), mandrake.get());

        Optional<String> moss = NpcKnowledgeBase.compactAnswer(
                "how do I get Spanish Moss?", "Forest", "day");

        assertTrue(moss.isPresent());
        String mossLower = moss.get().toLowerCase(Locale.ROOT);
        assertTrue(mossLower.contains("mutandis"), moss.get());
        assertTrue(mossLower.contains("shears"), moss.get());
        assertTrue(mossLower.contains("poppets"), moss.get());
    }

    @Test
    void herbologyCardsAnswerMutationAndSpiritPlantQuestions() throws Exception {
        assertCardMatchesRecipe("how do I make a mutating sprig?", "/data/witchery/recipe/mutating_sprig.json");
        assertCardMatchesRecipe("what do I need for treefyd seeds?", "/data/witchery/recipe/treefyd_seeds.json");

        Optional<String> sprig = NpcKnowledgeBase.compactAnswer(
                "what does a mutating sprig do?", "Plains", "day");

        assertTrue(sprig.isPresent());
        String sprigLower = sprig.get().toLowerCase(Locale.ROOT);
        assertTrue(sprigLower.contains("grasspers"), sprig.get());
        assertTrue(sprigLower.contains("leech chests"), sprig.get());
        assertTrue(sprigLower.contains("blood roses"), sprig.get());

        Optional<String> cotton = NpcKnowledgeBase.compactAnswer(
                "how do I get Wispy Cotton?", "Plains", "night");

        assertTrue(cotton.isPresent());
        String cottonLower = cotton.get().toLowerCase(Locale.ROOT);
        assertTrue(cottonLower.contains("spirit world"), cotton.get());
        assertTrue(cottonLower.contains("flowing spirit"), cotton.get());

        Optional<String> wormwood = NpcKnowledgeBase.compactAnswer(
                "how do I get Wormwood?", "Plains", "night");

        assertTrue(wormwood.isPresent());
        String wormwoodLower = wormwood.get().toLowerCase(Locale.ROOT);
        assertTrue(wormwoodLower.contains("wheat"), wormwood.get());
        assertTrue(wormwoodLower.contains("wispy cotton"), wormwood.get());
        assertTrue(wormwoodLower.contains("brazier"), wormwood.get());
    }

    @Test
    void advancedBrewBaseCardsMatchRecipeJson() throws Exception {
        assertCardMatchesRecipe("what do I need for redstone soup?", "/data/witchery/recipe/redstone_soup.json");
        assertCardMatchesRecipe("how do I make flying ointment?", "/data/witchery/recipe/flying_ointment.json");
        assertCardMatchesRecipe("how do I make mystic unguent?", "/data/witchery/recipe/mystic_unguent.json");
        assertCardMatchesRecipe("how do I make quicklime?", "/data/witchery/recipe/quicklime.json");
        assertCardMatchesRecipe("how do I make drop of luck?", "/data/witchery/recipe/drop_of_luck.json");
        assertCardMatchesRecipe("how do I make graveyard dust?", "/data/witchery/recipe/graveyard_dust.json");
    }

    @Test
    void advancedBrewRecipeCardsMatchRecipeJson() throws Exception {
        assertCardMatchesRecipe("what do I need for Brew of Bats?", "/data/witchery/recipe/brew_of_bats.json");
        assertCardMatchesRecipe("what do I need for Brew of Erosion?", "/data/witchery/recipe/brew_of_erosion.json");
        assertCardMatchesRecipe("what do I need for Brew of Webs?", "/data/witchery/recipe/brew_of_webs.json");
        assertCardMatchesRecipe("what do I need for Brew of Ice?", "/data/witchery/recipe/brew_of_ice.json");
        assertCardMatchesRecipe("what do I need for Brew of Vines?", "/data/witchery/recipe/brew_of_vines.json");
        assertCardMatchesRecipe("what do I need for Brew of Thorns?", "/data/witchery/recipe/brew_of_thorns.json");
        assertCardMatchesRecipe("what do I need for Combustion Brew?", "/data/witchery/recipe/combustion_brew.json");
        assertCardMatchesRecipe("what do I need for Brew of the Depths?", "/data/witchery/recipe/brew_of_the_depths.json");
        assertCardMatchesRecipe("what do I need for Brew of Raising?", "/data/witchery/recipe/brew_of_raising.json");
        assertCardMatchesRecipe("what do I need for Brew of Substitution?", "/data/witchery/recipe/brew_of_substitution.json");
    }

    @Test
    void advancedBrewCardsAnswerFormAndFailureQuestions() {
        Optional<String> modifiers = NpcKnowledgeBase.compactAnswer(
                "how do brew modifiers work?", "Plains", "night");

        assertTrue(modifiers.isPresent());
        String modifiersLower = modifiers.get().toLowerCase(Locale.ROOT);
        assertTrue(modifiersLower.contains("strength"), modifiers.get());
        assertTrue(modifiersLower.contains("duration"), modifiers.get());
        assertTrue(modifiersLower.contains("opened recipe"), modifiers.get());

        Optional<String> splash = NpcKnowledgeBase.compactAnswer(
                "can I make this brew splash or gas?", "Plains", "night");

        assertTrue(splash.isPresent());
        String splashLower = splash.get().toLowerCase(Locale.ROOT);
        assertTrue(splashLower.contains("splash"), splash.get());
        assertTrue(splashLower.contains("gas"), splash.get());
        assertTrue(splashLower.contains("ask by brew name"), splash.get());

        Optional<String> failed = NpcKnowledgeBase.compactAnswer(
                "why did my brew fail?", "Plains", "night");

        assertTrue(failed.isPresent());
        String failedLower = failed.get().toLowerCase(Locale.ROOT);
        assertTrue(failedLower.contains("wrong opened page"), failed.get());
        assertTrue(failedLower.contains("heat"), failed.get());
        assertTrue(failedLower.contains("altar power"), failed.get());
    }

    @Test
    void poppetFoundationRecipeCardsMatchRecipeJson() throws Exception {
        assertCardMatchesRecipe("how do I make a bone needle?", "/data/witchery/recipe/bone_needle.json");
        assertCardMatchesRecipe("what do I need for a poppet?", "/data/witchery/recipe/poppet.json");
        assertCardMatchesRecipe("how do I make a taglock kit?", "/data/witchery/recipe/taglock_kit.json");
        assertCardMatchesRecipe("what do I need for a poppet shelf?", "/data/witchery/recipe/poppet_shelf.json");
    }

    @Test
    void protectionPoppetRecipeCardsMatchRecipeJson() throws Exception {
        assertCardMatchesRecipe("what do I need for death protection poppet?", "/data/witchery/recipe/death_protection_poppet.json");
        assertCardMatchesRecipe("what do I need for fire protection poppet?", "/data/witchery/recipe/fire_protection_poppet.json");
        assertCardMatchesRecipe("what do I need for water protection poppet?", "/data/witchery/recipe/water_protection_poppet.json");
        assertCardMatchesRecipe("what do I need for earth protection poppet?", "/data/witchery/recipe/earth_protection_poppet.json");
        assertCardMatchesRecipe("what do I need for hunger protection poppet?", "/data/witchery/recipe/hunger_protection_poppet.json");
        assertCardMatchesRecipe("what do I need for tool protection poppet?", "/data/witchery/recipe/tool_protection_poppet.json");
        assertCardMatchesRecipe("what do I need for armor protection poppet?", "/data/witchery/recipe/armor_protection_poppet.json");
        assertCardMatchesRecipe("what do I need for poppet protection poppet?", "/data/witchery/recipe/poppet_protection_poppet.json");
    }

    @Test
    void hostilePoppetRecipeCardsMatchRecipeJson() throws Exception {
        assertCardMatchesRecipe("what do I need for voodoo poppet?", "/data/witchery/recipe/voodoo_poppet.json");
        assertCardMatchesRecipe("what do I need for vampiric poppet?", "/data/witchery/recipe/vampiric_poppet.json");
        assertCardMatchesRecipe("what do I need for voodoo protection poppet?", "/data/witchery/recipe/voodoo_protection_poppet.json");
    }

    @Test
    void poppetCardsAnswerBindingAndChoiceQuestions() {
        Optional<String> binding = NpcKnowledgeBase.compactAnswer(
                "how do I bind a poppet?", "Plains", "night");

        assertTrue(binding.isPresent());
        String bindingLower = binding.get().toLowerCase(Locale.ROOT);
        assertTrue(bindingLower.contains("filled taglock kit"), binding.get());
        assertTrue(bindingLower.contains("unbound poppet"), binding.get());

        Optional<String> choose = NpcKnowledgeBase.compactAnswer(
                "which poppet should I make?", "Plains", "night");

        assertTrue(choose.isPresent());
        String chooseLower = choose.get().toLowerCase(Locale.ROOT);
        assertTrue(chooseLower.contains("death"), choose.get());
        assertTrue(chooseLower.contains("fire"), choose.get());
        assertTrue(chooseLower.contains("bind"), choose.get());

        Optional<String> counter = NpcKnowledgeBase.compactAnswer(
                "how do I protect from voodoo?", "Plains", "night");

        assertTrue(counter.isPresent());
        String counterLower = counter.get().toLowerCase(Locale.ROOT);
        assertTrue(counterLower.contains("voodoo protection poppets"), counter.get());
        assertTrue(counterLower.contains("cat familiar"), counter.get());
    }

    @Test
    void curseCounterplayCardsAnswerBlockedAndDetectionQuestions() {
        Optional<String> blocked = NpcKnowledgeBase.compactAnswer(
                "why was my curse blocked?", "Plains", "night");

        assertTrue(blocked.isPresent());
        String blockedLower = blocked.get().toLowerCase(Locale.ROOT);
        assertTrue(blockedLower.contains("witch hunter"), blocked.get());
        assertTrue(blockedLower.contains("voodoo protection poppet"), blocked.get());
        assertTrue(blockedLower.contains("protected"), blocked.get());

        Optional<String> detection = NpcKnowledgeBase.compactAnswer(
                "am I cursed?", "Plains", "night");

        assertTrue(detection.isPresent());
        String detectionLower = detection.get().toLowerCase(Locale.ROOT);
        assertTrue(detectionLower.contains("seer stone"), detection.get());
        assertTrue(detectionLower.contains("misfortune"), detection.get());
        assertTrue(detectionLower.contains("waking nightmare"), detection.get());
    }

    @Test
    void curseCounterplayCardsAnswerNamedCurseQuestions() {
        Optional<String> sinking = NpcKnowledgeBase.compactAnswer(
                "what does curse of sinking need?", "Plains", "night");

        assertTrue(sinking.isPresent());
        String sinkingLower = sinking.get().toLowerCase(Locale.ROOT);
        assertTrue(sinkingLower.contains("medium infernal"), sinking.get());
        assertTrue(sinkingLower.contains("ink sac"), sinking.get());
        assertTrue(sinkingLower.contains("brew of the grotesque"), sinking.get());

        Optional<String> cureSinking = NpcKnowledgeBase.compactAnswer(
                "how do I cure sinking?", "Plains", "night");

        assertTrue(cureSinking.isPresent());
        String cureSinkingLower = cureSinking.get().toLowerCase(Locale.ROOT);
        assertTrue(cureSinkingLower.contains("breath of the goddess"), cureSinking.get());
        assertTrue(cureSinkingLower.contains("bone meal"), cureSinking.get());
        assertTrue(cureSinkingLower.contains("brew of the depths"), cureSinking.get());

        Optional<String> nightmare = NpcKnowledgeBase.compactAnswer(
                "tell me about waking nightmare curse", "Plains", "night");

        assertTrue(nightmare.isPresent());
        String nightmareLower = nightmare.get().toLowerCase(Locale.ROOT);
        assertTrue(nightmareLower.contains("large infernal"), nightmare.get());
        assertTrue(nightmareLower.contains("mellifluous hunger"), nightmare.get());
        assertTrue(nightmareLower.contains("tormented twine"), nightmare.get());
    }

    @Test
    void curseCounterplayCardsAnswerAdvancedCurseQuestions() {
        Optional<String> corrupt = NpcKnowledgeBase.compactAnswer(
                "how does Corrupt Poppet work?", "Plains", "night");

        assertTrue(corrupt.isPresent());
        String corruptLower = corrupt.get().toLowerCase(Locale.ROOT);
        assertTrue(corruptLower.contains("cat familiar"), corrupt.get());
        assertTrue(corruptLower.contains("spectral dust"), corrupt.get());
        assertTrue(corruptLower.contains("poppet protection"), corrupt.get());

        Optional<String> vampire = NpcKnowledgeBase.compactAnswer(
                "how do I remove vampirism?", "Plains", "night");

        assertTrue(vampire.isPresent());
        String vampireLower = vampire.get().toLowerCase(Locale.ROOT);
        assertTrue(vampireLower.contains("coven of six"), vampire.get());
        assertTrue(vampireLower.contains("cat familiar"), vampire.get());
        assertTrue(vampireLower.contains("silver sword"), vampire.get());
        assertTrue(vampireLower.contains("10000 power"), vampire.get());

        Optional<String> hell = NpcKnowledgeBase.compactAnswer(
                "what is Hell on Earth?", "Plains", "night");

        assertTrue(hell.isPresent());
        String hellLower = hell.get().toLowerCase(Locale.ROOT);
        assertTrue(hellLower.contains("large infernal"), hell.get());
        assertTrue(hellLower.contains("nether star"), hell.get());
        assertTrue(hellLower.contains("villager"), hell.get());
    }

    @Test
    void compactAnswerSkipsSmallTalk() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("hi", "Plains", "day");

        assertTrue(answer.isEmpty());
    }

    @Test
    void skipsKnowledgeForMemoryQuestions() {
        String context = NpcKnowledgeBase.relevantContext("what do you remember about me?", "Plains", "night");
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("what do you remember about me?", "Plains", "night");

        assertTrue(context.isBlank());
        assertTrue(answer.isEmpty());
    }

    private static void assertCardMatchesRecipe(String question, String resource) throws Exception {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer(question, "Plains", "night");

        assertTrue(answer.isPresent(), "No card answer for " + question);
        String lower = answer.get().toLowerCase(Locale.ROOT);
        for (String ingredient : recipeIngredients(resource)) {
            assertTrue(containsIngredient(lower, ingredient), ingredient + " missing from: " + answer.get());
        }
    }

    private static void assertOvenCardMatchesRecipe(String question, String resource, boolean expectClayJar) throws Exception {
        JsonObject recipe = resourceJson(resource);
        String ingredient = itemName(recipe.get("ingredient").getAsString());
        String result = itemName(recipe.getAsJsonObject("result").get("id").getAsString());
        Optional<String> answer = NpcKnowledgeBase.compactAnswer(question, "Plains", "night");

        assertTrue(answer.isPresent(), "No card answer for " + question);
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(containsIngredient(lower, ingredient), ingredient + " missing from: " + answer.get());
        assertTrue(containsIngredient(lower, result), result + " missing from: " + answer.get());
        assertTrue(lower.contains("oven"), answer.get());
        if (expectClayJar) {
            assertTrue(lower.contains("clay jar"), answer.get());
        }
    }

    private static void assertDistilleryCardMatchesRecipe(String question, String resource) throws Exception {
        JsonObject recipe = resourceJson(resource);
        Optional<String> answer = NpcKnowledgeBase.compactAnswer(question, "Plains", "night");

        assertTrue(answer.isPresent(), "No distillery card answer for " + question);
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(containsIngredient(lower, ingredientName(recipe.get("ingredient1"))), answer.get());
        if (recipe.has("ingredient2")) {
            assertTrue(containsIngredient(lower, ingredientName(recipe.get("ingredient2"))), answer.get());
        }
        assertTrue(lower.contains("clay jar"), answer.get());
        assertTrue(lower.contains("altar"), answer.get());
        for (JsonElement output : recipe.getAsJsonArray("outputs")) {
            assertTrue(containsIngredient(lower, ingredientName(output)), ingredientName(output) + " missing from: " + answer.get());
        }
    }

    private static void assertSpinningCardMatchesRecipe(String question, String resource) throws Exception {
        JsonObject recipe = resourceJson(resource);
        Optional<String> answer = NpcKnowledgeBase.compactAnswer(question, "Plains", "night");

        assertTrue(answer.isPresent(), "No spinning card answer for " + question);
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(containsIngredient(lower, ingredientName(recipe.get("fibre"))), answer.get());
        assertTrue(containsIngredient(lower, ingredientName(recipe.get("result"))), answer.get());
        assertTrue(lower.contains("altar"), answer.get());
        for (JsonElement modifier : recipe.getAsJsonArray("modifiers")) {
            assertTrue(containsIngredient(lower, ingredientName(modifier)), ingredientName(modifier) + " missing from: " + answer.get());
        }
    }

    private static List<String> recipeIngredients(String resource) throws Exception {
        JsonObject recipe = resourceJson(resource);
        if (recipe.has("ingredients")) {
            return shapelessOrStationIngredients(recipe);
        }
        if (recipe.has("key")) {
            return shapedRecipeIngredients(recipe);
        }
        throw new IllegalArgumentException("No recognizable ingredients in " + resource);
    }

    private static List<String> shapelessOrStationIngredients(JsonObject recipe) {
        List<String> ingredients = new ArrayList<>();
        for (JsonElement element : recipe.getAsJsonArray("ingredients")) {
            ingredients.add(ingredientName(element));
        }
        return ingredients;
    }

    private static List<String> shapedRecipeIngredients(JsonObject recipe) {
        List<String> ingredients = new ArrayList<>();
        for (String key : recipe.getAsJsonObject("key").keySet()) {
            JsonElement element = recipe.getAsJsonObject("key").get(key);
            ingredients.add(ingredientName(element));
        }
        return ingredients;
    }

    private static String ingredientName(JsonElement element) {
        if (element.isJsonPrimitive()) {
            return itemName(element.getAsString());
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("id")) {
            if (object.has("components")) {
                JsonObject components = object.getAsJsonObject("components");
                if (components.has("minecraft:potion_contents")) {
                    JsonObject contents = components.getAsJsonObject("minecraft:potion_contents");
                    if (contents.has("potion")) {
                        return itemName(contents.get("potion").getAsString());
                    }
                }
            }
            return itemName(object.get("id").getAsString());
        }
        if (object.has("items")) {
            return itemName(object.get("items").getAsString());
        }
        throw new IllegalArgumentException("Unsupported ingredient shape: " + object);
    }

    private static JsonObject resourceJson(String resource) throws Exception {
        try (InputStream stream = NpcKnowledgeBaseTest.class.getResourceAsStream(resource)) {
            assertTrue(stream != null, "Missing resource " + resource);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static String resourceText(String resource) throws Exception {
        try (InputStream stream = NpcKnowledgeBaseTest.class.getResourceAsStream(resource)) {
            assertTrue(stream != null, "Missing resource " + resource);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<String> primerCardFiles() throws Exception {
        List<String> files = new ArrayList<>();
        for (String rawLine : resourceText("/data/witchery/ai/cards/index.txt").split("\\R")) {
            String line = rawLine.strip();
            if (!line.isBlank() && !line.startsWith("#")) {
                files.add(line);
            }
        }
        return files;
    }

    private static boolean containsForbiddenImplementationPhrase(String lower, String phrase) {
        if (phrase.indexOf(' ') >= 0) {
            return lower.contains(phrase);
        }
        return lower.matches("(?s).*\\b" + java.util.regex.Pattern.quote(phrase) + "\\b.*");
    }

    private static void assertFirstTargetedTopic(String expectedTopic, String message, NpcCardRequest request) {
        String context = NpcKnowledgeBase.relevantContext(message, "Plains", "night", request);
        String diagnostics = NpcKnowledgeBase.diagnosticsLine();
        assertTrue(!context.isBlank(), "No context for " + message + ": " + diagnostics);
        assertTrue(diagnostics.contains("lastKnowledgeReason=targeted_match:" + expectedTopic),
                "Expected first targeted topic " + expectedTopic + " for '" + message + "' but got "
                        + diagnostics + "\nContext:\n" + context);
    }

    private static String itemName(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        return path.replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private static boolean containsIngredient(String answer, String ingredient) {
        return answer.contains(ingredient)
                || stripArticles(answer).contains(stripArticles(ingredient))
                || normalizeWords(answer).contains(normalizeWords(ingredient));
    }

    private static String stripArticles(String text) {
        return text.replaceAll("\\b(a|an|the)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String normalizeWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
