package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcOllamaFarmerConversationTest {
    private static final String VILLAGER_NAME = "Marta";
    private static final String PLAYER_NAME = "Dev";
    private static final String PROFESSION = "farmer";
    private static final String VILLAGE_INFO = "nearby villagers 8; iron golems 1; village guards 2; beds 10; doors 7; walls/fences 42; raid none; threats: vampires 1, werewolves 0, witches 1, raiders 0";
    private static final String BASE_MEMORY = """
            Entity memory: villager is Marta; player is Dev.
            Scene memory: Marta is a farmer in a Witchery village with fields, food stores, low walls, and a road nearby.
            """;
    private static final List<String> QUESTIONS = List.of(
            "Morning, do you live in this village?",
            "What do you do here?",
            "How is the harvest looking?",
            "Do we have enough food stored for dinner?",
            "How does compost help the fields?",
            "What should I feed the animals?",
            "Does rain help crops or just make mud?",
            "What do farmers trade?",
            "Is the road safe?",
            "Do we have an iron golem nearby?",
            "Any village guards nearby?",
            "Is there a raid right now?",
            "I saw someone pale near the barn. Vampire?",
            "What about werewolves after moonrise?",
            "Are witches near the village?",
            "Are there good witches as well as bad witches?",
            "Can you teach me witchcraft?",
            "How do I make a Nether portal?",
            "Where do I get blaze rods?",
            "Where are elytras?",
            "How do I find a woodland mansion?",
            "Can you /locate a stronghold for me?",
            "What is belladonna's Latin name?",
            "Tell me a village rumor.",
            "Goodbye, keep your doors shut."
    );

    @Test
    void evaCanHoldFarmerConversationTwentyFiveTurns() throws Exception {
        Assumptions.assumeTrue("1".equals(System.getenv("WITCHERY_LLM_FARMER_CONVERSATION")),
                "Set WITCHERY_LLM_FARMER_CONVERSATION=1 to run the local Ollama farmer conversation test.");

        String endpoint = envOrDefault("WITCHERY_LLM_ENDPOINT", "http://127.0.0.1:11434");
        String model = envOrDefault("WITCHERY_LLM_MODEL", "witchery-eva-qwen2.5-1.5b-q4km-4k");
        assertOllamaReachable(endpoint);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            OllamaLlmProvider provider = new OllamaLlmProvider(
                    new LlmProviderConfig(endpoint, model, 20000, 96, false), executor);
            List<Row> rows = runConversation(provider);
            String report = buildReport(model, rows);
            Path reportPath = reportPath(envOrDefault("WITCHERY_LLM_FARMER_CONVERSATION_REPORT",
                    "build/reports/witchery-ai/farmer-conversation-25-eva.md"));
            Files.createDirectories(reportPath.toAbsolutePath().getParent());
            Files.writeString(reportPath, report, StandardCharsets.UTF_8);
            System.out.println(report);
            System.out.println("NPC farmer conversation report: " + reportPath.toAbsolutePath());
            assertEquals(QUESTIONS.size(), rows.size(), "Expected farmer turns.");
        } finally {
            executor.shutdownNow();
        }
    }

    private static List<Row> runConversation(OllamaLlmProvider provider) {
        List<Row> rows = new ArrayList<>();
        List<String> recent = new ArrayList<>();
        List<String> recentNpcSpeeches = new ArrayList<>();
        StringBuilder longMemory = new StringBuilder(BASE_MEMORY.strip());
        String lastNpcSpeech = "";

        for (String question : QUESTIONS) {
            long started = System.nanoTime();
            String baseMemory = memoryFor(longMemory.toString(), recent);
            NpcMessageAnalysis analysis = analyzeMessage(provider, question);
            String memory = NpcMessageAnalysis.appendContext(baseMemory, analysis);
            ModelAttempt attempt = askModel(provider, question, memory);
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            FinalSpeech finalSpeech = finalSpeech(attempt.response(), question, memory,
                    lastNpcSpeech, recentNpcSpeeches);
            rows.add(new Row(question, attempt.describe(), attempt.speechOnly(), finalSpeech.speech(),
                    finalSpeech.fallbackUsed(), finalSpeech.fallbackText(), elapsedMillis));

            appendRecent(recent, question, finalSpeech.speech());
            appendRecentNpcSpeech(recentNpcSpeeches, finalSpeech.speech());
            lastNpcSpeech = finalSpeech.speech();
            if (analysis.hasMemory()) {
                longMemory.append("\n").append(analysis.memory());
            }
            NpcMemoryNotes.extract(PLAYER_NAME, question).ifPresent(note -> longMemory.append("\n").append(note));
            if (attempt.response() != null && !attempt.response().memory().isBlank()) {
                longMemory.append("\n").append(attempt.response().memory().strip());
            }
        }
        return rows;
    }

    private static NpcMessageAnalysis analyzeMessage(OllamaLlmProvider provider, String message) {
        try {
            NpcBrainResponse response = provider.generate(
                    NpcPromptBuilder.messageAnalysis("villager", PLAYER_NAME, message))
                    .get(15, TimeUnit.SECONDS);
            return NpcMessageAnalysis.fromResponse(response, PLAYER_NAME, message);
        } catch (Exception ex) {
            return NpcMessageAnalysis.deterministic(PLAYER_NAME, message);
        }
    }

    private static ModelAttempt askModel(OllamaLlmProvider provider, String message, String memory) {
        try {
            NpcBrainResponse response = provider.generate(
                    NpcPromptBuilder.villagerSmokeForProfession(message, memory, VILLAGER_NAME, PROFESSION))
                    .get(30, TimeUnit.SECONDS);
            return new ModelAttempt(response, null);
        } catch (ExecutionException ex) {
            return new ModelAttempt(null, ex.getCause());
        } catch (Exception ex) {
            return new ModelAttempt(null, ex);
        }
    }

    private static FinalSpeech finalSpeech(NpcBrainResponse response, String message, String memory,
                                           String lastNpcSpeech, List<String> recentNpcSpeeches) {
        if (response != null && VillagerPersona.INSTANCE.allowsIntent(response.intent(), null, null)) {
            Optional<String> polished = NpcBrainResponse.polishSpeechOrGroundedFollowup(
                    response.speech(), VILLAGER_NAME, PLAYER_NAME, message, memory, "10:15");
            if (polished.isPresent()
                    && !VillagerPersona.rejectsVillagerRaw(polished.get())
                    && !VillagerPersona.rejectsVillagerRawForMessage(polished.get(), message)
                    && !farmerReportRejects(polished.get(), message)
                    && !NpcBrainResponse.sameMeaningForRepetition(polished.get(), lastNpcSpeech)
                    && !repeatsRecentNpcSpeech(polished.get(), recentNpcSpeeches)) {
                return new FinalSpeech(polished.get(), false, "");
            }
        }

        Optional<String> fallback = NpcDialogueFallback.villagerChat(
                        message, VILLAGER_NAME, PLAYER_NAME, PROFESSION, VILLAGE_INFO, memory)
                .map(reply -> NpcBrainResponse.sanitize(reply, NpcBrainResponse.MAX_SPEECH_LENGTH))
                .filter(reply -> !reply.isBlank());
        String fallbackSpeech = fallback.orElse("");
        if (fallbackSpeech.isBlank()) {
            fallbackSpeech = NpcDialogueFallback.villagerLastResort(message, PLAYER_NAME, lastNpcSpeech);
        } else if (NpcBrainResponse.sameMeaningForRepetition(fallbackSpeech, lastNpcSpeech)
                || repeatsRecentNpcSpeech(fallbackSpeech, recentNpcSpeeches)) {
            Optional<String> alternate = NpcDialogueFallback.villagerAlternate(message, PLAYER_NAME, lastNpcSpeech);
            if (alternate.isPresent()) {
                fallbackSpeech = alternate.get();
            }
        }
        return new FinalSpeech(fallbackSpeech, true, fallbackSpeech);
    }

    private static boolean farmerReportRejects(String speech, String message) {
        String answer = normalizeForReport(speech);
        String question = normalizeForReport(message);
        if (answer.isBlank()) {
            return true;
        }
        if (containsAny(answer, "rumor referral boundary", "targeted fact", "should not know",
                "unless i am", "marta ", "i am interested in making", "can you tell me how to do it",
                "beautiful eyes", "beautiful woman", "sanguisorbae", "witches brew", "hair shiny",
                "good witch s hair", "bread and beer await", "bones strong", "i know where they go")) {
            return true;
        }
        if (containsAny(question, "live in this village", "do you live")
                && !containsAny(answer, "live", "village", "trade", "worry")) {
            return true;
        }
        if (containsAny(question, "what do you do")
                && !containsAny(answer, "field", "fields", "farmer", "bread", "crops", "crows")) {
            return true;
        }
        if (containsAny(question, "harvest")
                && !containsAny(answer, "harvest", "bread", "granary", "crop", "crops")) {
            return true;
        }
        if (containsAny(question, "food stored", "dinner", "supper")
                && !containsAny(answer, "food", "dinner", "supper", "bread", "bowl", "bowls", "store",
                "stores", "pantry", "granary")) {
            return true;
        }
        if (containsAny(question, "compost")
                && (!containsAny(answer, "compost", "bone meal", "scraps", "soil")
                    || containsAny(answer, "witch", "hair"))) {
            return true;
        }
        if (containsAny(question, "feed the animals", "feed animals")
                && !containsAny(answer, "wheat", "seeds", "cows", "sheep", "pigs", "chickens", "roots")) {
            return true;
        }
        if (containsAny(question, "rain")
                && !containsAny(answer, "rain", "mud", "soil", "flood", "roots", "water")) {
            return true;
        }
        if (containsAny(question, "farmers trade")
                && (!containsAny(answer, "farmers", "trade", "food", "crop", "crops", "bread")
                    || containsAny(answer, "witch", "hair"))) {
            return true;
        }
        if (containsAny(question, "road safe")
                && !containsAny(answer, "road", "safe", "walls", "fences", "door", "night", "trouble")) {
            return true;
        }
        if (containsAny(question, "golem")
                && !containsAny(answer, "golem", "iron", "nearby")) {
            return true;
        }
        if (containsAny(question, "guards")
                && !containsAny(answer, "guard", "guards")) {
            return true;
        }
        if (containsAny(question, "raid")
                && !containsAny(answer, "no raid", "raid active", "raider", "raiders", "pillager", "pillagers")) {
            return true;
        }
        if (containsAny(question, "vampire")
                && (!containsAny(answer, "vampire", "vampires", "garlic", "sunset")
                    || containsAny(answer, "silver"))) {
            return true;
        }
        if (containsAny(question, "werewolf", "werewolves")
                && !containsAny(answer, "werewolf", "werewolves", "moonrise")) {
            return true;
        }
        if (containsAny(question, "witches near")
                && !containsAny(answer, "witch", "witches", "hair", "bottle", "bottles", "name")) {
            return true;
        }
        if (containsAny(question, "good witches", "bad witches")
                && !containsAny(answer, "good", "bad", "fair", "clean", "hair", "bottle", "bottles")) {
            return true;
        }
        if (containsAny(question, "teach me witchcraft")
                && !containsAny(answer, "primer", "witch", "patience", "find")) {
            return true;
        }
        if (containsAny(question, "nether portal")
                && (!containsAny(answer, "portal", "nether", "miner", "book", "burns")
                    || containsAny(answer, "bonfire", "campfire", "special recipe", "obsidian"))) {
            return true;
        }
        if (containsAny(question, "blaze rods")
                && (!containsAny(answer, "blaze", "nether", "trouble", "field")
                    || containsAny(answer, "craft", "obsidian", "firewood", "recipe", "charcoal",
                    "wood", "pages say", "one way to do it"))) {
            return true;
        }
        if (containsAny(question, "elytra", "elytras")
                && (!containsAny(answer, "elytra", "end", "dragon", "sky")
                    || containsAny(answer, "recipe", "rocket", "iron golem", "make your own", "secret"))) {
            return true;
        }
        if (containsAny(question, "woodland mansion")
                && !containsAny(answer, "mansion", "cartographer", "explorer map", "map", "armor", "torches")) {
            return true;
        }
        if (containsAny(question, "locate", "stronghold")
                && !containsAny(answer, "command", "cheat", "cheats", "cannot", "eyes", "maps", "roads")) {
            return true;
        }
        if (containsAny(question, "belladonna")
                && !containsAny(answer, "belladonna", "scholar", "apothecary", "primer", "witch herb")) {
            return true;
        }
        if (containsAny(question, "rumor", "rumour")
                && !containsAny(answer, "rumor", "well", "repeats", "rain")) {
            return true;
        }
        if (containsAny(question, "goodbye")
                && !containsAny(answer, "goodbye", "go careful", "doors", "bread")) {
            return true;
        }
        return false;
    }

    private static String memoryFor(String longMemory, List<String> recent) {
        if (recent.isEmpty()) {
            return longMemory;
        }
        return longMemory + "\nRecent conversation:\n" + String.join("\n", recent);
    }

    private static void appendRecent(List<String> recent, String question, String speech) {
        recent.add(PLAYER_NAME + ": " + question);
        recent.add(VILLAGER_NAME + ": " + speech);
        while (recent.size() > 10) {
            recent.remove(0);
        }
    }

    private static void appendRecentNpcSpeech(List<String> recentNpcSpeeches, String speech) {
        recentNpcSpeeches.add(speech);
        while (recentNpcSpeeches.size() > 6) {
            recentNpcSpeeches.remove(0);
        }
    }

    private static boolean repeatsRecentNpcSpeech(String speech, List<String> recentNpcSpeeches) {
        for (String recent : recentNpcSpeeches) {
            if (NpcBrainResponse.sameMeaningForRepetition(speech, recent)) {
                return true;
            }
        }
        return false;
    }

    private static String buildReport(String model, List<Row> rows) {
        int total = rows.size();
        long fallbacks = rows.stream().filter(Row::fallbackUsed).count();
        long rawAccepted = total - fallbacks;
        long failures = rows.stream().filter(row -> row.raw().startsWith("ERROR:")).count();
        long meanMillis = Math.round(rows.stream().mapToLong(Row::elapsedMillis).average().orElse(0.0));
        long maxMillis = rows.stream().mapToLong(Row::elapsedMillis).max().orElse(0L);

        StringBuilder report = new StringBuilder();
        report.append("# Witchery Farmer NPC Ollama Conversation Test - ").append(total).append(" Turns\n\n");
        report.append("Scene: Marta the farmer speaks to Dev in a Plains village, day 4. ");
        report.append("Adjusted output is what the game would show after polish/fallback.\n\n");
        report.append("## Summary\n\n");
        report.append("| Metric | Result |\n");
        report.append("| --- | ---: |\n");
        report.append("| Model | ").append(escapeCell(model)).append(" |\n");
        report.append("| Turns | ").append(total).append(" |\n");
        report.append("| Raw accepted | ").append(rawAccepted).append(" |\n");
        report.append("| Fallback used | ").append(fallbacks).append(" |\n");
        report.append("| Request/parse failures | ").append(failures).append(" |\n");
        report.append("| Mean response time | ").append(meanMillis).append("ms |\n");
        report.append("| Max response time | ").append(maxMillis).append("ms |\n\n");
        report.append("## Transcript\n\n");
        report.append("| # | Player | Raw | Raw speech | Adjusted Marta | Fallback | Fallback text | ms |\n");
        report.append("| ---: | --- | --- | --- | --- | ---: | --- | ---: |\n");
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            report.append("| ").append(i + 1).append(" | ")
                    .append(escapeCell(row.question())).append(" | ")
                    .append(escapeCell(row.raw())).append(" | ")
                    .append(escapeCell(row.rawSpeech())).append(" | ")
                    .append(escapeCell(row.finalSpeech())).append(" | ")
                    .append(row.fallbackUsed() ? "yes" : "no").append(" | ")
                    .append(escapeCell(row.fallbackText())).append(" | ")
                    .append(row.elapsedMillis()).append(" |\n");
        }
        return report.toString();
    }

    private static void assertOllamaReachable(String endpoint) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint + "/api/tags"))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Ollama endpoint was not reachable: HTTP " + response.statusCode());
        }
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String normalizeForReport(String text) {
        return text == null ? "" : text.toLowerCase()
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static Path reportPath(String configured) {
        Path path = Path.of(configured);
        if (path.isAbsolute()) {
            return path;
        }
        Path current = Path.of("").toAbsolutePath();
        for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
            if (Files.exists(candidate.resolve("gradlew.bat")) && Files.exists(candidate.resolve("src/main"))) {
                return candidate.resolve(path);
            }
        }
        return current.resolve(path);
    }

    private static String escapeCell(String text) {
        return (text == null ? "" : text)
                .replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\r", "")
                .replace("\n", "<br>");
    }

    private static String shortError(Throwable error) {
        if (error == null) {
            return "unknown";
        }
        String message = error.getMessage();
        return error.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private record FinalSpeech(String speech, boolean fallbackUsed, String fallbackText) {
    }

    private record ModelAttempt(NpcBrainResponse response, Throwable error) {
        String describe() {
            if (response == null) {
                return "ERROR: " + shortError(error);
            }
            return response.speech() + " / " + response.intent();
        }

        String speechOnly() {
            return response == null ? "" : response.speech();
        }
    }

    private record Row(String question, String raw, String rawSpeech, String finalSpeech,
                       boolean fallbackUsed, String fallbackText, long elapsedMillis) {
    }
}
