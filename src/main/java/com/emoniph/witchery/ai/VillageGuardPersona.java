package com.emoniph.witchery.ai;

import com.emoniph.witchery.entity.VillageGuardEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Optional;

public final class VillageGuardPersona implements NpcPersona<VillageGuardEntity> {
    public static final VillageGuardPersona INSTANCE = new VillageGuardPersona();

    private VillageGuardPersona() {
    }

    @Override
    public String id() {
        return "village_guard";
    }

    @Override
    public String roleName() {
        return "village guard";
    }

    @Override
    public String defaultName() {
        return "Village Guard";
    }

    @Override
    public Class<VillageGuardEntity> entityClass() {
        return VillageGuardEntity.class;
    }

    @Override
    public int chatColorRgb() {
        return 0xE6B84A;
    }

    @Override
    public void beginConversation(VillageGuardEntity entity, ServerPlayer player) {
        holdAttention(entity, player);
    }

    @Override
    public void completeConversation(VillageGuardEntity entity, ServerPlayer player) {
        holdAttention(entity, player);
    }

    @Override
    public void nudgeAttention(VillageGuardEntity entity, ServerPlayer player) {
        holdAttention(entity, player);
    }

    @Override
    public NpcBrainPrompt greetingPrompt(VillageGuardEntity entity, ServerPlayer player, String memory) {
        return NpcPromptBuilder.villageGuardGreeting(entity, player, memory);
    }

    @Override
    public NpcBrainPrompt chatPrompt(VillageGuardEntity entity, ServerPlayer player, String memory,
                                     String playerMessage) {
        return NpcPromptBuilder.villageGuardChat(entity, player, memory, playerMessage);
    }

    @Override
    public boolean allowsIntent(NpcIntent intent, VillageGuardEntity entity, ServerPlayer player) {
        return switch (intent) {
            case NONE, QUEST_HINT, WARN_THREAT, COMBAT_TAUNT -> true;
            case OPEN_TRADE, FOLLOW_OWNER -> false;
        };
    }

    @Override
    public Optional<String> fallback(ServerLevel level, VillageGuardEntity entity, ServerPlayer player,
                                     String playerMessage, String memoryContext) {
        return NpcDialogueFallback.villageGuardChat(
                playerMessage,
                displayName(entity),
                player.getName().getString(),
                NpcPromptBuilder.villageGuardState(entity),
                NpcPromptBuilder.villageGuardVillageInfo(level, entity),
                memoryContext);
    }

    @Override
    public Optional<String> polishRaw(String rawSpeech, VillageGuardEntity entity, ServerPlayer player,
                                      String playerMessage, String memoryContext, String expectedClock) {
        Optional<String> polished = NpcBrainResponse.polishSpeechAfterFallback(rawSpeech, displayName(entity),
                player.getName().getString(), playerMessage, memoryContext, expectedClock);
        if (polished.isEmpty() || rejectsVillageGuardRaw(polished.get(), playerMessage)) {
            return Optional.empty();
        }
        return polished;
    }

    @Override
    public Optional<String> polishFallback(String speech, VillageGuardEntity entity, ServerPlayer player,
                                           String playerMessage, String memoryContext, String expectedClock) {
        String cleaned = NpcBrainResponse.sanitize(speech, NpcBrainResponse.MAX_SPEECH_LENGTH);
        return cleaned.isBlank() ? Optional.empty() : Optional.of(cleaned);
    }

    @Override
    public String lastResortFallback(VillageGuardEntity entity, ServerPlayer player, String playerMessage,
                                     String avoidSpeech) {
        return NpcDialogueFallback.villageGuardLastResort(playerMessage, player.getName().getString(), avoidSpeech);
    }

    @Override
    public Optional<String> alternateForRepeatedFallback(VillageGuardEntity entity, ServerPlayer player,
                                                         String playerMessage, String avoidSpeech) {
        return NpcDialogueFallback.villageGuardAlternate(playerMessage, player.getName().getString(), avoidSpeech);
    }

    static boolean rejectsVillageGuardRaw(String speech, String playerMessage) {
        String normalized = normalize(speech);
        String player = normalize(playerMessage);
        if (normalized.isBlank()) {
            return true;
        }
        if (NpcPersonaSpeechGuards.rejectsNarrationOnly(speech)
                || NpcPersonaSpeechGuards.rejectsOutOfWorldMeta(speech)
                || NpcPersonaSpeechGuards.rejectsSoftCompanion(speech)
                || NpcPersonaSpeechGuards.rejectsPlayerSpeakerInversion(speech)) {
            return true;
        }
        if (containsAny(normalized,
                "prompt", "json", "schema", "as an ai", "language model", "targeted fact",
                "server state", "npc dialogue brain", "witchery boss", "i am death",
                "i am a witch", "i m a witch", "coven witch", "shade of leonard", "baba yaga",
                "i am a demon", "i m a demon", "flame imp", "hobgoblin", "i am just a villager",
                "i am a villager", "i m a villager", "apothecary", "shopkeeper", "customer")) {
            return true;
        }
        if (containsAny(normalized,
                "what brings you here", "how are you today", "happy to help", "happy to serve",
                "my friend", "dear friend", "chosen one", "apprentice", "master",
                "i will follow you", "i can follow you", "i will protect you", "i can protect you",
                "i am your shield", "you can hide behind me", "i ll keep watch over you",
                "i will keep watch over you", "serve and protect you", "open my trade",
                "trade with me", "i sell", "i buy", "my wares", "emeralds for")) {
            return true;
        }
        if (normalized.split("\\s+").length > 36) {
            return true;
        }
        if (containsAny(player, "what do you guard", "who do you guard", "what do you watch",
                "patrol", "gate", "wall", "walls", "road", "roads")
                && !containsAny(normalized, "guard", "watch", "patrol", "gate", "wall", "road",
                "village", "bell", "door")) {
            return true;
        }
        if (containsAny(player, "vampire", "vampires")
                && !containsAny(normalized, "vampire", "blood", "garlic", "sun", "stake", "bow",
                "arrow", "distance", "night")) {
            return true;
        }
        if (containsAny(player, "werewolf", "werewolves", "wolfman", "wolfmen")
                && !containsAny(normalized, "werewolf", "wolf", "moon", "silver", "bow", "arrow",
                "distance", "beast")) {
            return true;
        }
        if (containsAny(player, "demon", "demons", "devil")
                && !containsAny(normalized, "demon", "bell", "door", "witch", "bind", "bargain",
                "fire", "guard", "golem")) {
            return true;
        }
        if (containsAny(player, "trade", "buy", "sell", "stock", "wares")
                && containsAny(normalized, "i sell", "i buy", "my stock", "my wares", "open trade",
                "emeralds for")) {
            return true;
        }
        return false;
    }

    private static void holdAttention(VillageGuardEntity entity, ServerPlayer player) {
        if (entity == null || player == null || entity.level().isClientSide()
                || entity.level() != player.level() || !entity.isAlive() || !player.isAlive()) {
            return;
        }
        entity.getNavigation().stop();
        entity.getLookControl().setLookAt(player, 30.0F, 30.0F);
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
