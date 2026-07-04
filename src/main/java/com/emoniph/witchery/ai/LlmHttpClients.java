package com.emoniph.witchery.ai;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

final class LlmHttpClients {
    private static final ConcurrentHashMap<Integer, HttpClient> CLIENTS = new ConcurrentHashMap<>();

    private LlmHttpClients() {
    }

    static HttpClient clientFor(int timeoutMillis) {
        int safeTimeout = Math.max(250, timeoutMillis);
        return CLIENTS.computeIfAbsent(safeTimeout, timeout ->
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(timeout))
                        .build());
    }

    static void clear() {
        CLIENTS.clear();
    }
}
