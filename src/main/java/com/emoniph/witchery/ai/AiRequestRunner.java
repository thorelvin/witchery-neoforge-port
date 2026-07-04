package com.emoniph.witchery.ai;

import net.minecraft.server.MinecraftServer;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

final class AiRequestRunner {
    private static final long TIMEOUT_GRACE_MILLIS = 500L;

    private AiRequestRunner() {
    }

    static void run(MinecraftServer server, LlmProvider provider, NpcBrainPrompt prompt, int timeoutMillis,
                    Runnable release, BiConsumer<NpcBrainResponse, Throwable> onMainThread) {
        provider.generate(prompt)
                .orTimeout(timeoutMillis + TIMEOUT_GRACE_MILLIS, TimeUnit.MILLISECONDS)
                .whenComplete((response, error) -> {
                    release.run();
                    server.execute(() -> onMainThread.accept(response, error));
                });
    }
}
