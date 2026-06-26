package com.emoniph.witchery.ai;

import java.util.List;

public record NpcBrainPrompt(String systemPrompt, String userPrompt, List<Example> examples) {
    public NpcBrainPrompt(String systemPrompt, String userPrompt) {
        this(systemPrompt, userPrompt, List.of());
    }

    public NpcBrainPrompt {
        examples = examples == null ? List.of() : List.copyOf(examples);
    }

    public record Example(String userPrompt, String assistantResponse) {
    }
}
