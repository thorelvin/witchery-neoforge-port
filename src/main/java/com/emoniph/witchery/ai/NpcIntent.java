package com.emoniph.witchery.ai;

import java.util.Locale;
import java.util.Optional;

public enum NpcIntent {
    NONE("none"),
    OPEN_TRADE("open_trade"),
    QUEST_HINT("quest_hint"),
    FOLLOW_OWNER("follow_owner"),
    WARN_THREAT("warn_threat"),
    COMBAT_TAUNT("combat_taunt");

    private final String id;

    NpcIntent(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<NpcIntent> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.of(NONE);
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (NpcIntent intent : values()) {
            if (intent.id.equals(normalized)) {
                return Optional.of(intent);
            }
        }
        return Optional.empty();
    }

    public boolean allowedForCovenWitch(boolean recruitedByPlayer) {
        return switch (this) {
            case NONE, OPEN_TRADE, QUEST_HINT, WARN_THREAT, COMBAT_TAUNT -> true;
            case FOLLOW_OWNER -> recruitedByPlayer;
        };
    }
}
