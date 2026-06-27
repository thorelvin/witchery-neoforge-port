package com.emoniph.witchery.ai;

public record LlmProviderConfig(String endpoint, String model, int timeoutMillis, int maxTokens,
                                boolean allowLanEndpoint, int keepAliveMinutes) {
    public LlmProviderConfig(String endpoint, String model, int timeoutMillis, int maxTokens,
                             boolean allowLanEndpoint) {
        this(endpoint, model, timeoutMillis, maxTokens, allowLanEndpoint, 30);
    }
}
