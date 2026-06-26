package com.emoniph.witchery.ai;

public record LlmProviderConfig(String endpoint, String model, int timeoutMillis, int maxTokens,
                                boolean allowLanEndpoint) {
}
