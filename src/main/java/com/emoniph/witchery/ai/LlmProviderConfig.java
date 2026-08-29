package com.emoniph.witchery.ai;

import java.util.Map;

public record LlmProviderConfig(String endpoint, String model, int timeoutMillis, int maxTokens,
                                boolean allowLanEndpoint, int keepAliveMinutes, int maxResponseBytes,
                                String apiKeyEnv, Map<String, String> extraHeaders,
                                String responseFormat) {
    public static final String DEFAULT_RESPONSE_FORMAT = "json_schema";
    public static final int DEFAULT_MAX_RESPONSE_BYTES = 131_072;

    public LlmProviderConfig {
        apiKeyEnv = apiKeyEnv == null ? "" : apiKeyEnv.trim();
        extraHeaders = extraHeaders == null ? Map.of() : Map.copyOf(extraHeaders);
        responseFormat = responseFormat == null || responseFormat.isBlank()
                ? DEFAULT_RESPONSE_FORMAT
                : responseFormat.trim();
        maxResponseBytes = Math.clamp(maxResponseBytes, 16_384, 1_048_576);
    }

    public LlmProviderConfig(String endpoint, String model, int timeoutMillis, int maxTokens,
                             boolean allowLanEndpoint, int keepAliveMinutes, String apiKeyEnv,
                             Map<String, String> extraHeaders, String responseFormat) {
        this(endpoint, model, timeoutMillis, maxTokens, allowLanEndpoint, keepAliveMinutes,
                DEFAULT_MAX_RESPONSE_BYTES, apiKeyEnv, extraHeaders, responseFormat);
    }

    public LlmProviderConfig(String endpoint, String model, int timeoutMillis, int maxTokens,
                             boolean allowLanEndpoint, int keepAliveMinutes) {
        this(endpoint, model, timeoutMillis, maxTokens, allowLanEndpoint, keepAliveMinutes,
                DEFAULT_MAX_RESPONSE_BYTES, "", Map.of(), DEFAULT_RESPONSE_FORMAT);
    }

    public LlmProviderConfig(String endpoint, String model, int timeoutMillis, int maxTokens,
                             boolean allowLanEndpoint) {
        this(endpoint, model, timeoutMillis, maxTokens, allowLanEndpoint, 30,
                DEFAULT_MAX_RESPONSE_BYTES, "", Map.of(), DEFAULT_RESPONSE_FORMAT);
    }
}
