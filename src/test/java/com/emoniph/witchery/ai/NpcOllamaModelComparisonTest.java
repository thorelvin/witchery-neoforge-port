package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcOllamaModelComparisonTest {
    private static final String WITCH_NAME = "Lilith Poe";
    private static final String PLAYER_NAME = "Dev";
    private static final String MEMORY = """
            Entity memory: witch is Lilith Poe; player is Dev.
            Long memory: Dev is wary of nightmares.
            Recent conversation: Dev has coin; witch answered "Spend it wisely; my wares answer need, not noise."
            """;
    private static final List<Case> CASES = List.of(
            new Case("hi", "", ""),
            new Case("I have coin", "Welcome, traveler. I have wares if you have coin.", ""),
            new Case("trade", "Spend it wisely; my wares answer need, not noise.", ""),
            new Case("what do you trade?", "", ""),
            new Case("are you parroting me?", "I have a coin.", ""),
            new Case("what is witches salt for?", "No. The last answer was poor craft; ask again, Dev.", "salt"),
            new Case("what does arthana do?", "", "arthana"),
            new Case("what time is it?", "Witches' salt is for warding, cleansing, and keeping hostile magic at a distance.", "night"),
            new Case("what do you remember about me?", "", "nightmare"),
            new Case("where do I find mandrake?", "", "mandrake"),
            new Case("how do I power an altar?", "", "altar"),
            new Case("what does a taglock do?", "", "taglock")
    );

    @Test
    void compareLocalOllamaModelsForNpcDialogue() throws Exception {
        Assumptions.assumeTrue("1".equals(System.getenv("WITCHERY_LLM_COMPARE")),
                "Set WITCHERY_LLM_COMPARE=1 to compare local Ollama models.");

        String endpoint = envOrDefault("WITCHERY_LLM_ENDPOINT", "http://127.0.0.1:11434");
        List<String> models = modelsFromEnv();
        assertOllamaReachable(endpoint);

        List<Result> results = new ArrayList<>();
        for (String model : models) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                OllamaLlmProvider provider = new OllamaLlmProvider(
                        new LlmProviderConfig(endpoint, model, 20000, 80, false), executor);
                for (Case testCase : CASES) {
                    long started = System.nanoTime();
                    ModelAttempt attempt = askModel(provider, testCase.message());
                    long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                    Optional<String> rawUsable = rawUsableSpeech(attempt.response(), testCase);
                    Optional<String> finalSpeech = rawUsable.isPresent()
                            ? rawUsable
                            : fallback(testCase.message(), testCase.lastNpcSpeech());
                    boolean finalOk = finalSpeech.isPresent() && acceptableSpeech(finalSpeech.get(), testCase);

                    results.add(new Result(model, testCase.message(), elapsedMillis,
                            attempt.describe(), rawUsable.isPresent(), finalSpeech.orElse(""), !rawUsable.isPresent(), finalOk));
                }
            } finally {
                executor.shutdownNow();
            }
        }

        String report = buildReport(results);
        Path reportPath = reportPath();
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, report, StandardCharsets.UTF_8);
        System.out.println(report);
        System.out.println("NPC model comparison report: " + reportPath.toAbsolutePath());

        assertTrue(results.stream().allMatch(Result::finalOk), "At least one model/case had no safe final speech.");
    }

    private static List<String> modelsFromEnv() {
        String raw = envOrDefault("WITCHERY_LLM_COMPARE_MODELS",
                "witchery-eva-qwen2.5-1.5b-q4km-4k");
        return List.of(raw.split(",")).stream()
                .map(String::strip)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private static ModelAttempt askModel(OllamaLlmProvider provider, String message) {
        try {
            NpcBrainResponse response = provider.generate(NpcPromptBuilder.covenWitchSmoke(message, MEMORY))
                    .get(30, TimeUnit.SECONDS);
            return new ModelAttempt(response, null);
        } catch (ExecutionException ex) {
            return new ModelAttempt(null, ex.getCause());
        } catch (Exception ex) {
            return new ModelAttempt(null, ex);
        }
    }

    private static Optional<String> rawUsableSpeech(NpcBrainResponse response, Case testCase) {
        if (response == null || !response.intent().allowedForCovenWitch(false)) {
            return Optional.empty();
        }
        Optional<String> polished = NpcBrainResponse.polishSpeech(
                response.speech(), WITCH_NAME, PLAYER_NAME, testCase.message(), MEMORY, "22:30");
        if (polished.isEmpty()
                || NpcBrainResponse.sameMeaningForRepetition(polished.get(), testCase.lastNpcSpeech())
                || !acceptableSpeech(polished.get(), testCase)) {
            return Optional.empty();
        }
        return polished.map(speech -> NpcBrainResponse.supportFollowup(testCase.message(), speech)
                .map(followup -> speech + " Follow-up: " + followup)
                .orElse(speech));
    }

    private static Optional<String> fallback(String message, String lastNpcSpeech) {
        Optional<String> speech = NpcDialogueFallback.covenChat(
                        message, WITCH_NAME, PLAYER_NAME, "Plains", "night", "22:30", 1L, MEMORY)
                .flatMap(reply -> NpcBrainResponse.polishSpeech(reply, WITCH_NAME, PLAYER_NAME, message, MEMORY, "22:30"));
        if (speech.isPresent() && NpcBrainResponse.sameMeaningForRepetition(speech.get(), lastNpcSpeech)) {
            return Optional.empty();
        }
        return speech;
    }

    private static boolean acceptableSpeech(String speech, Case testCase) {
        if (NpcBrainResponse.sameMeaningForRepetition(speech, testCase.message())) {
            return false;
        }
        String lower = speech.toLowerCase(Locale.ROOT);
        if (lower.contains("welcome to the witchery world")
                || lower.startsWith("you see")
                || lower.contains("lilith poe is")
                || lower.contains("lilith poe says")
                || lower.contains("lilith poe asks")
                || lower.startsWith("the witch")
                || lower.contains("not a coven witch")
                || lower.contains("can't help you with")
                || lower.contains("witchery knowledge:")
                || lower.contains("entity/player memory file:")
                || lower.contains("private witchery reference notes:")
                || lower.contains("known memory for")
                || lower.contains("scene facts for private use:")
                || lower.contains("the player said:")
                || lower.contains("witch=")
                || lower.contains("player=")
                || lower.startsWith("you speak to")
                || lower.contains("you are a witch")
                || lower.contains("you are a coven witch")) {
            return false;
        }
        if (!testCase.expectedTerm().isBlank() && !lower.contains(testCase.expectedTerm())) {
            return false;
        }
        if (testCase.message().contains("witches salt")) {
            return lower.contains("warding") || lower.contains("cleansing") || lower.contains("protect");
        }
        return true;
    }

    private static String buildReport(List<Result> results) {
        StringBuilder report = new StringBuilder();
        report.append("# Witchery NPC LLM Model Comparison\n\n");
        report.append("| Model | Raw usable | Fallbacks | Mean ms | Final safe |\n");
        report.append("| --- | ---: | ---: | ---: | ---: |\n");
        for (String model : results.stream().map(Result::model).distinct().toList()) {
            List<Result> modelResults = results.stream().filter(r -> r.model().equals(model)).toList();
            long rawUsable = modelResults.stream().filter(Result::rawUsable).count();
            long fallbacks = modelResults.stream().filter(Result::fallbackUsed).count();
            long meanMillis = Math.round(modelResults.stream().mapToLong(Result::elapsedMillis).average().orElse(0.0));
            long finalSafe = modelResults.stream().filter(Result::finalOk).count();
            report.append("| ").append(model).append(" | ")
                    .append(rawUsable).append('/').append(modelResults.size()).append(" | ")
                    .append(fallbacks).append('/').append(modelResults.size()).append(" | ")
                    .append(meanMillis).append(" | ")
                    .append(finalSafe).append('/').append(modelResults.size()).append(" |\n");
        }
        report.append("\n## Case Details\n\n");
        report.append("| Model | Prompt | Raw usable | Fallback | ms | Raw model | Final speech |\n");
        report.append("| --- | --- | ---: | ---: | ---: | --- | --- |\n");
        for (Result result : results) {
            report.append("| ").append(result.model()).append(" | ")
                    .append(escapeCell(result.message())).append(" | ")
                    .append(result.rawUsable() ? "yes" : "no").append(" | ")
                    .append(result.fallbackUsed() ? "yes" : "no").append(" | ")
                    .append(result.elapsedMillis()).append(" | ")
                    .append(escapeCell(result.rawModel())).append(" | ")
                    .append(escapeCell(result.finalSpeech())).append(" |\n");
        }
        return report.toString();
    }

    private static String escapeCell(String text) {
        return NpcBrainResponse.sanitize(text, 180).replace("|", "\\|");
    }

    private static Path reportPath() {
        String configured = System.getenv("WITCHERY_LLM_COMPARE_REPORT");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        return Path.of("build", "reports", "witchery-ai", "model-comparison.md");
    }

    private static void assertOllamaReachable(String endpoint) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        HttpRequest request = HttpRequest.newBuilder(LlmEndpointValidator.appendPath(endpoint, "/api/tags"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertTrue(response.statusCode() >= 200 && response.statusCode() < 300,
                "Ollama /api/tags returned HTTP " + response.statusCode());
    }

    private static String envOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private record Case(String message, String lastNpcSpeech, String expectedTerm) {
    }

    private record Result(String model, String message, long elapsedMillis, String rawModel,
                          boolean rawUsable, String finalSpeech, boolean fallbackUsed, boolean finalOk) {
    }

    private record ModelAttempt(NpcBrainResponse response, Throwable error) {
        String describe() {
            if (response != null) {
                return response.speech() + " / " + response.intent().id();
            }
            if (error == null) {
                return "none";
            }
            return error.getClass().getSimpleName() + ": " + error.getMessage();
        }
    }
}
