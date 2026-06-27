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
