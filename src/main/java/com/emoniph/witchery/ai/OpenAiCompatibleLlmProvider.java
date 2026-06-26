package com.emoniph.witchery.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public final class OpenAiCompatibleLlmProvider implements LlmProvider {
    private final LlmProviderConfig config;
    private final Executor executor;
    private final HttpClient client;

    public OpenAiCompatibleLlmProvider(LlmProviderConfig config, Executor executor) {
        this.config = config;
        this.executor = executor;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.timeoutMillis()))
                .build();
    }

    @Override
    public CompletableFuture<NpcBrainResponse> generate(NpcBrainPrompt prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return send(prompt);
            } catch (IOException ex) {
                throw new CompletionException(ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new CompletionException(ex);
            }
        }, executor);
    }

    private NpcBrainResponse send(NpcBrainPrompt prompt) throws IOException, InterruptedException {
        URI uri = LlmEndpointValidator.appendPath(config.endpoint(), "/v1/chat/completions");
        JsonObject body = new JsonObject();
        body.addProperty("model", config.model());
        body.addProperty("temperature", 0.35);
        body.addProperty("max_tokens", config.maxTokens());

        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        body.add("response_format", responseFormat);

        JsonArray messages = new JsonArray();
        messages.add(message("system", prompt.systemPrompt()));
        for (NpcBrainPrompt.Example example : prompt.examples()) {
            messages.add(message("user", example.userPrompt()));
            messages.add(message("assistant", example.assistantResponse()));
        }
        messages.add(message("user", prompt.userPrompt()));
        body.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(config.timeoutMillis()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OpenAI-compatible endpoint returned HTTP " + response.statusCode());
        }
        return parseResponse(response.body());
    }

    private static JsonObject message(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    static NpcBrainResponse parseResponse(String body) throws IOException {
        JsonElement parsed = JsonParser.parseString(body);
        if (!parsed.isJsonObject()) {
            throw new IOException("Provider response was not a JSON object.");
        }
        JsonArray choices = parsed.getAsJsonObject().getAsJsonArray("choices");
        if (choices == null || choices.isEmpty() || !choices.get(0).isJsonObject()) {
            throw new IOException("Provider response did not contain choices.");
        }
        JsonObject first = choices.get(0).getAsJsonObject();
        JsonObject message = first.getAsJsonObject("message");
        if (message == null || !message.has("content")) {
            throw new IOException("Provider response did not contain message content.");
        }
        return NpcBrainResponse.parseLenient(message.get("content").getAsString())
                .orElseThrow(() -> new IOException("Provider response did not contain valid NPC JSON."));
    }
}
