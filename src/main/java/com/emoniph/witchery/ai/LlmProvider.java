package com.emoniph.witchery.ai;

import java.util.concurrent.CompletableFuture;

public interface LlmProvider {
    CompletableFuture<NpcBrainResponse> generate(NpcBrainPrompt prompt);
}
