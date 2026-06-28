package com.emoniph.witchery.ai;

import java.util.Locale;
import java.util.UUID;

public record NpcCardRequest(String personaId, UUID entityId, UUID playerId, long gameTime,
                             int conversationTurn, int maxTopics, int maxFactsPerTopic) {
    public static final String ANY_PERSONA = "any";
    private static final UUID ZERO_UUID = new UUID(0L, 0L);

    public NpcCardRequest {
        personaId = normalizePersona(personaId);
        entityId = entityId == null ? ZERO_UUID : entityId;
        playerId = playerId == null ? ZERO_UUID : playerId;
        conversationTurn = Math.max(0, conversationTurn);
        maxTopics = maxTopics <= 0 ? 6 : maxTopics;
        maxFactsPerTopic = maxFactsPerTopic <= 0 ? 1 : maxFactsPerTopic;
    }

    public static NpcCardRequest any() {
        return new NpcCardRequest(ANY_PERSONA, ZERO_UUID, ZERO_UUID, 0L, 0, 6, 1);
    }

    public static NpcCardRequest persona(String personaId) {
        return new NpcCardRequest(personaId, ZERO_UUID, ZERO_UUID, 0L, 0, 6, 1);
    }

    static String normalizePersona(String personaId) {
        if (personaId == null || personaId.isBlank()) {
            return ANY_PERSONA;
        }
        String normalized = personaId.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return normalized.isBlank() ? ANY_PERSONA : normalized;
    }
}
