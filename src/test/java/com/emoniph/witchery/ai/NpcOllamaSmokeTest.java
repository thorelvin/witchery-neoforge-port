package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcOllamaSmokeTest {
    private static final String WITCH_NAME = "Lilith Poe";
    private static final String PLAYER_NAME = "Dev";
    private static final String MEMORY = """
            Entity memory: witch is Lilith Poe; player is Dev.
            Long memory: Dev is wary of nightmares.
            Recent conversation: Dev has coin; witch answered "Spend it wisely; my wares answer need, not noise."
            """;

    @Test
    void evaDialogueHasUsableFinalSpeechForKnownHardCases() throws Exception {
        Assumptions.assumeTrue("1".equals(System.getenv("WITCHERY_LLM_SMOKE")),
                "Set WITCHERY_LLM_SMOKE=1 to run the local Ollama smoke test.");

        String endpoint = envOrDefault("WITCHERY_LLM_ENDPOINT", "http://127.0.0.1:11434");
        String model = envOrDefault("WITCHERY_LLM_MODEL", "witchery-eva-qwen2.5-1.5b-q4km-4k");
        assertOllamaReachable(endpoint);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            OllamaLlmProvider provider = new OllamaLlmProvider(
                    new LlmProviderConfig(endpoint, model, 15000, 80, false), executor);
            List<Case> cases = List.of(
                    new Case("hi", "", ""),
                    new Case("I have coin", "Welcome, traveler. I have wares if you have coin.", ""),
                    new Case("trade", "Spend it wisely; my wares answer need, not noise.", ""),
                    new Case("what do you trade?", "", ""),
                    new Case("are you parroting me?", "I have a coin.", ""),
                    new Case("i want to brew", "", "brew"),
                    new Case("do you know potions?", "", "potions"),
                    new Case("what is witches salt for?", "No. The last answer was poor craft; ask again, Dev.", "salt"),
                    new Case("what does arthana do?", "", "arthana"),
                    new Case("what time is it?", "Witches' salt is for warding, cleansing, and keeping hostile magic at a distance.", "22:30"),
                    new Case("what do you remember about me?", "It is night, about 22:30 on day 1.", "nightmares"),
                    new Case("Can you brew Sleeping Brew?", "", "spirit world"),
                    new Case("where do I find mandrake?", "", "mandrake"),
                    new Case("how do I power an altar?", "", "altar"),
                    new Case("what does a taglock do?", "", "taglock")
            );

            for (Case testCase : cases) {
                ModelAttempt attempt = askModel(provider, testCase.message());
                Optional<String> finalSpeech = finalSpeech(attempt.response(), testCase);
                boolean fallbackUsed = finalSpeech.isEmpty();
                if (fallbackUsed) {
                    finalSpeech = fallback(testCase.message(), testCase.lastNpcSpeech());
                }

                assertTrue(finalSpeech.isPresent(), "No usable final speech for: " + testCase.message());
                assertFalse(NpcBrainResponse.sameMeaningForRepetition(finalSpeech.get(), testCase.message()),
                        "Final speech echoed player: " + finalSpeech.get());
                assertLooksLikeDialogue(finalSpeech.get());
                if (!testCase.expectedTerm().isBlank()) {
                    assertTrue(finalSpeech.get().toLowerCase().contains(testCase.expectedTerm()),
                            "Final speech missed expected term '" + testCase.expectedTerm() + "': " + finalSpeech.get());
                }
                if (testCase.message().contains("witches salt")) {
                    String lower = finalSpeech.get().toLowerCase();
                    assertTrue(lower.contains("warding") || lower.contains("cleansing") || lower.contains("protect"),
                            "Salt answer was too vague: " + finalSpeech.get());
                }
                System.out.printf("NPC smoke: message=\"%s\" model=\"%s\" final=\"%s\" fallback=%s%n",
                        testCase.message(), attempt.describe(), finalSpeech.get(), fallbackUsed);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static ModelAttempt askModel(OllamaLlmProvider provider, String message) {
        try {
            NpcBrainResponse response = provider.generate(NpcPromptBuilder.covenWitchSmoke(message, MEMORY))
                    .get(25, TimeUnit.SECONDS);
            return new ModelAttempt(response, null);
        } catch (ExecutionException ex) {
            return new ModelAttempt(null, ex.getCause());
        } catch (Exception ex) {
            return new ModelAttempt(null, ex);
        }
    }

    private static Optional<String> finalSpeech(NpcBrainResponse response, Case testCase) {
        if (response == null || !CovenWitchPersona.allowsIntent(response.intent(), false)) {
            return Optional.empty();
        }
        Optional<String> polished = NpcBrainResponse.polishSpeech(
                response.speech(), WITCH_NAME, PLAYER_NAME, testCase.message(), MEMORY, "22:30");
        if (polished.isEmpty()
                || NpcBrainResponse.sameMeaningForRepetition(polished.get(), testCase.lastNpcSpeech())) {
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

    private static void assertLooksLikeDialogue(String speech) {
        String normalized = speech.toLowerCase();
        assertFalse(normalized.contains("welcome to the witchery world"), speech);
        assertFalse(normalized.startsWith("you see"), speech);
        assertFalse(normalized.contains("lilith poe is"), speech);
        assertFalse(normalized.contains("lilith poe says"), speech);
        assertFalse(normalized.contains("lilith poe asks"), speech);
        assertFalse(normalized.startsWith("the witch"), speech);
        assertFalse(normalized.contains("not a coven witch"), speech);
        assertFalse(normalized.contains("can't help you with"), speech);
        assertFalse(normalized.contains("witchery knowledge:"), speech);
        assertFalse(normalized.contains("entity/player memory file:"), speech);
        assertFalse(normalized.contains("private witchery reference notes:"), speech);
        assertFalse(normalized.contains("known memory for"), speech);
        assertFalse(normalized.contains("scene facts for private use:"), speech);
        assertFalse(normalized.contains("the player said:"), speech);
        assertFalse(normalized.contains("witch="), speech);
        assertFalse(normalized.contains("player="), speech);
        assertFalse(normalized.startsWith("you speak to"), speech);
        assertFalse(normalized.contains("you are a witch"), speech);
        assertFalse(normalized.contains("you are a coven witch"), speech);
    }

    private static void assertOllamaReachable(String endpoint) throws Exception {
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
