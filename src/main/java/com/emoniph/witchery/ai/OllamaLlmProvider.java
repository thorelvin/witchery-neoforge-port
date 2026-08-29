package com.emoniph.witchery.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public final class OllamaLlmProvider implements LlmProvider {
    private final LlmProviderConfig config;
    private final Executor executor;
    private final HttpClient client;

    public OllamaLlmProvider(LlmProviderConfig config, Executor executor) {
        this(config, executor, LlmHttpClients.clientFor(config.timeoutMillis()));
    }

    OllamaLlmProvider(LlmProviderConfig config, Executor executor, HttpClient client) {
        this.config = config;
        this.executor = executor;
        this.client = client == null ? LlmHttpClients.clientFor(config.timeoutMillis()) : client;
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
        URI uri = LlmEndpointValidator.appendPath(config.endpoint(), "/api/chat");
        JsonObject body = new JsonObject();
        body.addProperty("model", config.model());
        body.addProperty("stream", false);
        body.add("format", npcResponseSchema());
        if (config.keepAliveMinutes() > 0) {
            body.addProperty("keep_alive", config.keepAliveMinutes() + "m");
        }

        JsonObject options = new JsonObject();
        options.addProperty("temperature", 0.5);
        options.addProperty("top_p", 0.85);
        options.addProperty("repeat_penalty", 1.08);
        options.addProperty("num_ctx", 4096);
        options.addProperty("num_predict", config.maxTokens());
        body.add("options", options);

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
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        String responseBody = LlmHttpBodies.readBounded(response.body(), config.maxResponseBytes());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Ollama returned HTTP " + response.statusCode());
        }
        return parseResponse(responseBody);
    }

    private static JsonObject message(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    private static JsonObject npcResponseSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.addProperty("additionalProperties", false);

        JsonObject properties = new JsonObject();
        properties.add("speech", stringSchema(NpcBrainResponse.MAX_SPEECH_LENGTH));
        properties.add("memory", stringSchema(NpcBrainResponse.MAX_MEMORY_LENGTH));

        JsonObject intent = new JsonObject();
        intent.addProperty("type", "string");
        JsonArray allowedIntents = new JsonArray();
        for (NpcIntent value : NpcIntent.values()) {
            allowedIntents.add(value.id());
        }
        intent.add("enum", allowedIntents);
        properties.add("intent", intent);

        JsonArray required = new JsonArray();
        required.add("speech");
        required.add("intent");
        required.add("memory");

        schema.add("properties", properties);
        schema.add("required", required);
        return schema;
    }

    private static JsonObject stringSchema(int maxLength) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "string");
        schema.addProperty("maxLength", maxLength);
        return schema;
    }

    static NpcBrainResponse parseResponse(String body) throws IOException {
        JsonElement parsed = JsonParser.parseString(body);
        if (!parsed.isJsonObject()) {
            throw new IOException("Ollama response was not a JSON object.");
        }
        JsonObject root = parsed.getAsJsonObject();
        String content = "";
        JsonElement message = root.get("message");
        if (message != null && message.isJsonObject()) {
            JsonElement messageContent = message.getAsJsonObject().get("content");
            content = messageContent == null || messageContent.isJsonNull() ? "" : messageContent.getAsString();
        } else if (root.has("response")) {
            content = root.get("response").getAsString();
        }
        return NpcBrainResponse.parseLenient(content)
                .orElseThrow(() -> new IOException("Ollama response did not contain valid NPC JSON."));
    }
}
