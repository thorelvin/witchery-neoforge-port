package com.emoniph.witchery.ai;

import net.minecraft.server.MinecraftServer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

final class AiRequestRunner {
    private static final long TIMEOUT_GRACE_MILLIS = 500L;

    private AiRequestRunner() {
    }

    static void run(MinecraftServer server, LlmProvider provider, NpcBrainPrompt prompt, int timeoutMillis,
                    Runnable release, BooleanSupplier acceptCompletion,
                    BiConsumer<NpcBrainResponse, Throwable> onMainThread) {
        CompletableFuture<NpcBrainResponse> request;
        try {
            request = provider.generate(prompt);
        } catch (RuntimeException error) {
            release.run();
            if (acceptCompletion.getAsBoolean()) {
                server.execute(() -> {
                    if (acceptCompletion.getAsBoolean()) onMainThread.accept(null, error);
                });
            }
            return;
        }
        request
                .orTimeout(timeoutMillis + TIMEOUT_GRACE_MILLIS, TimeUnit.MILLISECONDS)
                .whenComplete((response, error) -> {
                    release.run();
                    if (acceptCompletion.getAsBoolean()) {
                        server.execute(() -> {
                            if (acceptCompletion.getAsBoolean()) onMainThread.accept(response, error);
                        });
                    }
                });
    }
}
