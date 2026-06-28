package com.emoniph.witchery.ai;

import java.util.Locale;

enum NpcEmotionHint {
    NONE("none"),
    CURIOSITY("curiosity"),
    GRATITUDE("gratitude"),
    ANGER("anger"),
    FEAR("fear"),
    CONFUSION("confusion"),
    AMUSEMENT("amusement"),
    SADNESS("sadness");

    private final String id;

    NpcEmotionHint(String id) {
        this.id = id;
    }

    String id() {
        return id;
    }

    static NpcEmotionHint parse(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        String normalized = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_ -]+", "")
                .replace('-', '_')
                .replace(' ', '_')
                .trim();
        for (NpcEmotionHint hint : values()) {
            if (hint.id.equals(normalized) || hint.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return hint;
            }
        }
        return NONE;
    }
}
