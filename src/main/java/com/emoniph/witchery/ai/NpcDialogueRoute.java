package com.emoniph.witchery.ai;

import java.util.Locale;

enum NpcDialogueRoute {
    SMALL_TALK("small_talk"),
    TRADE("trade"),
    RECIPE("recipe"),
    RITE("rite"),
    LORE("lore"),
    MEMORY("memory"),
    SOCIAL("social"),
    ACTION("action"),
    UNKNOWN("unknown");

    private final String id;

    NpcDialogueRoute(String id) {
        this.id = id;
    }

    String id() {
        return id;
    }

    static NpcDialogueRoute parse(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String normalized = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_ -]+", "")
                .replace('-', '_')
                .replace(' ', '_')
                .trim();
        for (NpcDialogueRoute route : values()) {
            if (route.id.equals(normalized) || route.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return route;
            }
        }
        return UNKNOWN;
    }
}
