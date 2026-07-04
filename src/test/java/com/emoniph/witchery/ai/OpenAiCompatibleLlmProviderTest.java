package com.emoniph.witchery.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleLlmProviderTest {
    @Test
    void jsonSchemaResponseFormatUsesStrictNpcSchema() {
        OpenAiCompatibleLlmProvider provider = provider("json_schema");

        JsonObject responseFormat = provider.responseFormat();

        assertNotNull(responseFormat);
        assertEquals("json_schema", responseFormat.get("type").getAsString());
        JsonObject jsonSchema = responseFormat.getAsJsonObject("json_schema");
        assertEquals("witchery_npc_response", jsonSchema.get("name").getAsString());
        assertTrue(jsonSchema.get("strict").getAsBoolean());
        JsonObject schema = jsonSchema.getAsJsonObject("schema");
        assertFalse(schema.get("additionalProperties").getAsBoolean());
        assertTrue(schema.getAsJsonArray("required").contains(JsonParser.parseString("\"speech\"")));
        assertTrue(schema.getAsJsonArray("required").contains(JsonParser.parseString("\"intent\"")));
        assertTrue(schema.getAsJsonArray("required").contains(JsonParser.parseString("\"memory\"")));
        assertTrue(schema.getAsJsonObject("properties")
                .getAsJsonObject("intent")
                .getAsJsonArray("enum")
                .contains(JsonParser.parseString("\"none\"")));
    }

    @Test
    void responseFormatCanBeDisabledForLooseCompatibleServers() {
        assertNull(provider("none").responseFormat());
    }

    @Test
    void configuredHeadersIncludeBearerKeyAndIgnoreProtectedExtras() {
        OpenAiCompatibleLlmProvider provider = new OpenAiCompatibleLlmProvider(
                new LlmProviderConfig("http://127.0.0.1:9999", "test-model", 1000, 80,
                        false, 0, "WITCHERY_TEST_API_KEY", Map.of(
                                "X-Witchery", "local",
                                "Authorization", "Bearer wrong",
                                "Content-Type", "text/plain"), "json_object"),
                Runnable::run,
                LlmHttpClients.clientFor(1000),
                name -> name.equals("WITCHERY_TEST_API_KEY") ? " test-token " : "");
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:9999/v1/chat/completions"));

        provider.addConfiguredHeaders(builder);

        var headers = builder.build().headers();
        assertEquals("Bearer test-token", headers.firstValue("Authorization").orElseThrow());
        assertEquals("local", headers.firstValue("X-Witchery").orElseThrow());
        assertFalse(headers.firstValue("Content-Type").isPresent());
    }

    @Test
    void strictParsingRejectsPlainSpeechContent() throws IOException {
        String body = completionBody("The moon is awake, Dev.");

        assertThrows(IOException.class, () -> OpenAiCompatibleLlmProvider.parseResponse(body, true));
        assertEquals("The moon is awake, Dev.", OpenAiCompatibleLlmProvider.parseResponse(body, false).speech());
    }

    @Test
    void strictParsingAcceptsNpcJsonContent() throws IOException {
        String body = completionBody("{\"speech\":\"Ready, Dev.\",\"intent\":\"none\",\"memory\":\"\"}");

        NpcBrainResponse response = OpenAiCompatibleLlmProvider.parseResponse(body, true);

        assertEquals("Ready, Dev.", response.speech());
        assertEquals(NpcIntent.NONE, response.intent());
    }

    @Test
    void strictParsingReportsRefusalWithoutRuntimeException() {
        String body = "{\"choices\":[{\"message\":{\"content\":null,\"refusal\":\"no\"}}]}";

        IOException error = assertThrows(IOException.class,
                () -> OpenAiCompatibleLlmProvider.parseResponse(body, true));
        assertTrue(error.getMessage().contains("refused"));
    }

    private static OpenAiCompatibleLlmProvider provider(String responseFormat) {
        return new OpenAiCompatibleLlmProvider(
                new LlmProviderConfig("http://127.0.0.1:9999", "test-model", 1000, 80,
                        false, 0, "", Map.of(), responseFormat),
                Runnable::run,
                LlmHttpClients.clientFor(1000));
    }

    private static String completionBody(String content) {
        String escaped = content.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"choices\":[{\"message\":{\"content\":\"" + escaped + "\"}}]}";
    }
}
