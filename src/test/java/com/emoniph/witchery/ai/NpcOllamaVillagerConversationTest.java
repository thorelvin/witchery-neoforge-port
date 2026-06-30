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

class NpcOllamaVillagerConversationTest {
    private static final String VILLAGER_NAME = "Marta";
    private static final String PLAYER_NAME = "Dev";
    private static final String PROFESSION = "apothecary";
    private static final String VILLAGE_INFO = "nearby villagers 8; iron golems 1; village guards 2; beds 10; doors 7; walls/fences 42; raid none; threats: vampires 1, werewolves 0, witches 1, raiders 0";
    private static final String BASE_MEMORY = """
            Entity memory: villager is Marta; player is Dev.
            Scene memory: Marta is an apothecary in a Witchery village near a road and low walls.
            """;
    private static final List<String> QUESTIONS = List.of(
            "Hello there, do you live in this village?",
            "What do you do here?",
            "What do you sell as an apothecary?",
            "How big is this village?",
            "Do we have an iron golem?",
            "Are the walls good enough?",
            "Any guards nearby?",
            "Is there a raid right now?",
            "I saw someone pale near the road. Any vampires nearby?",
            "What about werewolves?",
            "Are witches near the village?",
            "Should I trust the witch at the edge of town?",
            "I want to buy something for travel.",
            "Do you know Whiff of Magic?",
            "Can you teach me witchcraft?",
            "Remember that I am afraid of vampires.",
            "What did I say I fear?",
            "If I ring the bell, what happens?",
            "Should I sleep here tonight?",
            "Goodbye, Marta. Keep your doors shut."
    );

    @Test
    void evaCanHoldVillagerConversationTwentyTurns() throws Exception {
        Assumptions.assumeTrue("1".equals(System.getenv("WITCHERY_LLM_VILLAGER_CONVERSATION")),
                "Set WITCHERY_LLM_VILLAGER_CONVERSATION=1 to run the local Ollama villager conversation test.");

        String endpoint = envOrDefault("WITCHERY_LLM_ENDPOINT", "http://127.0.0.1:11434");
        String model = envOrDefault("WITCHERY_LLM_MODEL", "witchery-eva-qwen2.5-1.5b-q4km-4k");
        assertOllamaReachable(endpoint);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            OllamaLlmProvider provider = new OllamaLlmProvider(
                    new LlmProviderConfig(endpoint, model, 20000, 96, false), executor);
            List<Row> rows = runConversation(provider);
            String report = buildReport(model, rows);
            Path reportPath = Path.of(envOrDefault("WITCHERY_LLM_VILLAGER_CONVERSATION_REPORT",
                    "build/reports/witchery-ai/villager-conversation-20.md"));
            Files.createDirectories(reportPath.toAbsolutePath().getParent());
            Files.writeString(reportPath, report, StandardCharsets.UTF_8);
            System.out.println(report);
            System.out.println("NPC villager conversation report: " + reportPath.toAbsolutePath());
            assertEquals(QUESTIONS.size(), rows.size(), "Expected villager turns.");
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
            FinalSpeech finalSpeech = finalSpeech(attempt.response(), question, memory, lastNpcSpeech, recentNpcSpeeches);
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
            NpcBrainResponse response = provider.generate(NpcPromptBuilder.villagerSmoke(message, memory))
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
        report.append("# Witchery Villager NPC Conversation Test - ").append(total).append(" Turns\n\n");
        report.append("Scene: Marta the apothecary speaks to Dev in a Plains village, day 4. ");
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
