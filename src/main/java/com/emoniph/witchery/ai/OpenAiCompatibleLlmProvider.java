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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Function;

public final class OpenAiCompatibleLlmProvider implements LlmProvider {
    private final LlmProviderConfig config;
    private final Executor executor;
    private final HttpClient client;
    private final Function<String, String> environment;

    public OpenAiCompatibleLlmProvider(LlmProviderConfig config, Executor executor) {
        this(config, executor, LlmHttpClients.clientFor(config.timeoutMillis()));
    }

    OpenAiCompatibleLlmProvider(LlmProviderConfig config, Executor executor, HttpClient client) {
        this(config, executor, client, System::getenv);
    }

    OpenAiCompatibleLlmProvider(LlmProviderConfig config, Executor executor, HttpClient client,
                                Function<String, String> environment) {
        this.config = config;
        this.executor = executor;
        this.client = client == null ? LlmHttpClients.clientFor(config.timeoutMillis()) : client;
        this.environment = environment == null ? ignored -> "" : environment;
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

        JsonObject responseFormat = responseFormat();
        if (responseFormat != null) {
            body.add("response_format", responseFormat);
        }

        JsonArray messages = new JsonArray();
        messages.add(message("system", prompt.systemPrompt()));
        for (NpcBrainPrompt.Example example : prompt.examples()) {
            messages.add(message("user", example.userPrompt()));
            messages.add(message("assistant", example.assistantResponse()));
        }
        messages.add(message("user", prompt.userPrompt()));
        body.add("messages", messages);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(config.timeoutMillis()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
        addConfiguredHeaders(requestBuilder);
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OpenAI-compatible endpoint returned HTTP " + response.statusCode());
        }
        return parseResponse(response.body(), isStrictResponseFormat());
    }

    JsonObject responseFormat() {
        String format = config.responseFormat().trim().toLowerCase(Locale.ROOT);
        if (format.equals("none") || format.equals("off")) {
            return null;
        }
        if (format.equals("json_schema") || format.equals("schema")) {
            JsonObject responseFormat = new JsonObject();
            responseFormat.addProperty("type", "json_schema");
            JsonObject jsonSchema = new JsonObject();
            jsonSchema.addProperty("name", "witchery_npc_response");
            jsonSchema.addProperty("strict", true);
            jsonSchema.add("schema", npcResponseSchema());
            responseFormat.add("json_schema", jsonSchema);
            return responseFormat;
        }
        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        return responseFormat;
    }

    private boolean isStrictResponseFormat() {
        String format = config.responseFormat().trim().toLowerCase(Locale.ROOT);
        return format.equals("json_schema") || format.equals("schema");
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

    void addConfiguredHeaders(HttpRequest.Builder builder) {
        String apiKey = apiKey();
        if (!apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        for (Map.Entry<String, String> entry : config.extraHeaders().entrySet()) {
            if (isProtectedExtraHeader(entry.getKey())) {
                continue;
            }
            builder.header(entry.getKey(), entry.getValue());
        }
    }

    private String apiKey() {
        String env = config.apiKeyEnv();
        if (env.isBlank()) {
            return "";
        }
        String value = environment.apply(env);
        return value == null ? "" : value.trim();
    }

    private static boolean isProtectedExtraHeader(String name) {
        return name.equalsIgnoreCase("authorization") || name.equalsIgnoreCase("content-type");
    }

    private static JsonObject message(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    static NpcBrainResponse parseResponse(String body) throws IOException {
        return parseResponse(body, false);
    }

    static NpcBrainResponse parseResponse(String body, boolean strictContent) throws IOException {
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
        JsonElement contentElement = message.get("content");
        if (contentElement == null || contentElement.isJsonNull()) {
            if (message.has("refusal")) {
                throw new IOException("Provider refused NPC response.");
            }
            throw new IOException("Provider response did not contain message content.");
        }
        if (!contentElement.isJsonPrimitive()) {
            throw new IOException("Provider response message content was not text.");
        }
        String content = contentElement.getAsString();
        Optional<NpcBrainResponse> parsedContent = strictContent
                ? NpcBrainResponse.parse(content)
                : NpcBrainResponse.parseLenient(content);
        return parsedContent
                .orElseThrow(() -> new IOException("Provider response did not contain valid NPC JSON."));
    }
}
