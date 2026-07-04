package com.emoniph.witchery.ai;

import java.util.Map;

public record LlmProviderConfig(String endpoint, String model, int timeoutMillis, int maxTokens,
                                boolean allowLanEndpoint, int keepAliveMinutes,
                                String apiKeyEnv, Map<String, String> extraHeaders,
                                String responseFormat) {
    public LlmProviderConfig {
        apiKeyEnv = apiKeyEnv == null ? "" : apiKeyEnv.trim();
        extraHeaders = extraHeaders == null ? Map.of() : Map.copyOf(extraHeaders);
        responseFormat = responseFormat == null ? "" : responseFormat.trim();
    }

    public LlmProviderConfig(String endpoint, String model, int timeoutMillis, int maxTokens,
                             boolean allowLanEndpoint, int keepAliveMinutes) {
        this(endpoint, model, timeoutMillis, maxTokens, allowLanEndpoint, keepAliveMinutes, "", Map.of(),
                "json_object");
    }

    public LlmProviderConfig(String endpoint, String model, int timeoutMillis, int maxTokens,
                             boolean allowLanEndpoint) {
        this(endpoint, model, timeoutMillis, maxTokens, allowLanEndpoint, 30, "", Map.of(), "json_object");
    }
}
