package com.emoniph.witchery.ai;

import com.emoniph.witchery.entity.ReflectionEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class ReflectionPersona implements NpcPersona<ReflectionEntity> {
    public static final ReflectionPersona INSTANCE = new ReflectionPersona();

    private ReflectionPersona() {
    }

    @Override
    public String id() {
        return "reflection";
    }

    @Override
    public String roleName() {
        return "reflection";
    }

    @Override
    public String defaultName() {
        return "Reflection";
    }

    @Override
    public Class<ReflectionEntity> entityClass() {
        return ReflectionEntity.class;
    }

    @Override
    public int chatColorRgb() {
        return 0x55FFFF;
    }

    @Override
    public String displayName(ReflectionEntity entity) {
        return NpcPromptBuilder.reflectionDisplayName(entity, entity == null ? "" : entity.getOwnerName());
    }

    @Override
    public void beginConversation(ReflectionEntity entity, ServerPlayer player) {
        nudgeAttention(entity, player);
    }

    @Override
    public void completeConversation(ReflectionEntity entity, ServerPlayer player) {
        nudgeAttention(entity, player);
    }

    @Override
    public void nudgeAttention(ReflectionEntity entity, ServerPlayer player) {
        if (entity == null || player == null) {
            return;
        }
        entity.getLookControl().setLookAt(player, 30.0F, 30.0F);
    }

    @Override
    public NpcBrainPrompt greetingPrompt(ReflectionEntity entity, ServerPlayer player, String memory) {
        return NpcPromptBuilder.reflectionGreeting(entity, player, memory);
    }

    @Override
    public NpcBrainPrompt chatPrompt(ReflectionEntity entity, ServerPlayer player, String memory,
                                     String playerMessage) {
        return NpcPromptBuilder.reflectionChat(entity, player, memory, playerMessage);
    }

    @Override
    public boolean allowsIntent(NpcIntent intent, ReflectionEntity entity, ServerPlayer player) {
        return switch (intent) {
            case NONE, QUEST_HINT, WARN_THREAT, COMBAT_TAUNT -> true;
            case OPEN_TRADE, FOLLOW_OWNER -> false;
        };
    }

    @Override
    public Optional<String> fallback(ServerLevel level, ReflectionEntity entity, ServerPlayer player,
                                     String playerMessage, String memoryContext) {
        return NpcDialogueFallback.reflectionChat(
                playerMessage,
                displayName(entity),
                player.getName().getString(),
                entity == null ? "" : entity.getOwnerName(),
                NpcPromptBuilder.reflectionState(entity),
                NpcPromptBuilder.playerGearSummary(player),
                memoryContext);
    }

    @Override
    public Optional<String> polishRaw(String rawSpeech, ReflectionEntity entity, ServerPlayer player,
                                      String playerMessage, String memoryContext, String expectedClock) {
        Optional<String> polished = NpcBrainResponse.polishSpeechOrGroundedFollowup(rawSpeech, displayName(entity),
                player.getName().getString(), playerMessage, memoryContext, expectedClock);
        if (polished.isEmpty() || rejectsReflectionRaw(polished.get(), playerMessage)) {
            return Optional.empty();
        }
        return polished;
    }

    @Override
    public Optional<String> polishFallback(String speech, ReflectionEntity entity, ServerPlayer player,
                                           String playerMessage, String memoryContext, String expectedClock) {
        String cleaned = NpcBrainResponse.sanitize(speech, NpcBrainResponse.MAX_SPEECH_LENGTH);
        return cleaned.isBlank() ? Optional.empty() : Optional.of(cleaned);
    }

    @Override
    public String lastResortFallback(ReflectionEntity entity, ServerPlayer player, String playerMessage,
                                     String avoidSpeech) {
        return NpcDialogueFallback.reflectionLastResort(playerMessage, player.getName().getString(), avoidSpeech);
    }

    @Override
    public Optional<String> alternateForRepeatedFallback(ReflectionEntity entity, ServerPlayer player,
                                                         String playerMessage, String avoidSpeech) {
        return NpcDialogueFallback.reflectionAlternate(playerMessage, player.getName().getString(), avoidSpeech);
    }

    static boolean rejectsReflectionRaw(String speech, String playerMessage) {
        String normalized = normalize(speech);
        String player = normalize(playerMessage);
        if (normalized.isBlank()) {
            return true;
        }
        if (containsAny(normalized, "prompt", "json", "schema", "as an ai", "language model",
                "targeted fact", "npc dialogue brain", "server state", "in minecraft chat")) {
            return true;
        }
        if (NpcPersonaSpeechGuards.rejectsNarrationOnly(speech)
                || NpcPersonaSpeechGuards.rejectsSoftCompanion(speech)
                || NpcPersonaSpeechGuards.rejectsUnsupportedProtection(speech, playerMessage)
                || NpcPersonaSpeechGuards.rejectsPlayerSpeakerInversion(speech)) {
            return true;
        }
        if (containsAny(normalized, "i am a witch", "coven witch", "shade of leonard", "i am leonard",
                "baba yaga", "i am a villager", "apothecary", "merchant", "shopkeeper",
                "i trade", "my wares", "coin", "customer", "friend", "my friend")) {
            return true;
        }
        if (containsAny(normalized, "shared past", "offer some insight", "witness of your past",
                "i shall refer to myself", "old friend", "my reflection")) {
            return true;
        }
        if (containsAny(normalized, "how are you today", "what brings you here", "welcome traveler",
                "hello adventurer", "may i assist", "glad you asked")) {
            return true;
        }
        if (containsAny(normalized, "i will help you", "i can teach you", "join your coven",
                "i will bless", "i bless", "i give you", "take this herb")) {
            return true;
        }
        if (containsAny(player, "who are you", "what are you", "your name", "call you")
                && !containsAny(normalized, "reflection", "mirror", "glass", "shape", "face")) {
            return true;
        }
        if (containsAny(player, "why do you look like me", "look like me", "my face", "my shape")
                && !containsAny(normalized, "mirror", "glass", "shape", "face", "reflection", "borrowed")) {
            return true;
        }
        return false;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9_ ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
