package com.emoniph.witchery.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.Locale;
import java.util.Optional;

public final class BossPersona implements NpcPersona<LivingEntity> {
    public static final BossPersona INSTANCE = new BossPersona();

    private BossPersona() {
    }

    @Override
    public String id() {
        return "boss";
    }

    @Override
    public String roleName() {
        return "witchery boss";
    }

    @Override
    public String defaultName() {
        return "Boss";
    }

    @Override
    public Class<LivingEntity> entityClass() {
        return LivingEntity.class;
    }

    @Override
    public int chatColorRgb() {
        return 0xCC3355;
    }

    @Override
    public void beginConversation(LivingEntity entity, ServerPlayer player) {
        holdAttention(entity, player);
    }

    @Override
    public void completeConversation(LivingEntity entity, ServerPlayer player) {
        holdAttention(entity, player);
    }

    @Override
    public void nudgeAttention(LivingEntity entity, ServerPlayer player) {
        holdAttention(entity, player);
    }

    @Override
    public NpcBrainPrompt greetingPrompt(LivingEntity entity, ServerPlayer player, String memory) {
        return NpcPromptBuilder.bossGreeting(entity, player, memory);
    }

    @Override
    public NpcBrainPrompt chatPrompt(LivingEntity entity, ServerPlayer player, String memory,
                                     String playerMessage) {
        return NpcPromptBuilder.bossChat(entity, player, memory, playerMessage);
    }

    @Override
    public boolean allowsIntent(NpcIntent intent, LivingEntity entity, ServerPlayer player) {
        return switch (intent) {
            case NONE, QUEST_HINT, WARN_THREAT, COMBAT_TAUNT -> true;
            case OPEN_TRADE, FOLLOW_OWNER -> false;
        };
    }

    @Override
    public Optional<String> fallback(ServerLevel level, LivingEntity entity, ServerPlayer player,
                                     String playerMessage, String memoryContext) {
        return NpcDialogueFallback.bossChat(
                playerMessage,
                displayName(entity),
                player.getName().getString(),
                NpcPromptBuilder.bossKind(entity),
                NpcPromptBuilder.bossState(entity),
                NpcPromptBuilder.playerGearSummary(player),
                memoryContext);
    }

    @Override
    public Optional<String> polishRaw(String rawSpeech, LivingEntity entity, ServerPlayer player,
                                      String playerMessage, String memoryContext, String expectedClock) {
        Optional<String> polished = NpcBrainResponse.polishSpeechOrGroundedFollowup(rawSpeech, displayName(entity),
                player.getName().getString(), playerMessage, memoryContext, expectedClock);
        if (polished.isEmpty() || rejectsBossRaw(polished.get(), playerMessage)) {
            return Optional.empty();
        }
        return polished;
    }

    @Override
    public Optional<String> polishFallback(String speech, LivingEntity entity, ServerPlayer player,
                                           String playerMessage, String memoryContext, String expectedClock) {
        String cleaned = NpcBrainResponse.sanitize(speech, NpcBrainResponse.MAX_SPEECH_LENGTH);
        return cleaned.isBlank() ? Optional.empty() : Optional.of(cleaned);
    }

    @Override
    public String lastResortFallback(LivingEntity entity, ServerPlayer player, String playerMessage,
                                     String avoidSpeech) {
        return NpcDialogueFallback.bossLastResort(playerMessage, player.getName().getString(),
                NpcPromptBuilder.bossKind(entity), NpcPromptBuilder.bossState(entity), avoidSpeech);
    }

    @Override
    public Optional<String> alternateForRepeatedFallback(LivingEntity entity, ServerPlayer player,
                                                         String playerMessage, String avoidSpeech) {
        return NpcDialogueFallback.bossAlternate(playerMessage, player.getName().getString(),
                NpcPromptBuilder.bossKind(entity), NpcPromptBuilder.bossState(entity), avoidSpeech);
    }

    static boolean rejectsBossRaw(String speech, String playerMessage) {
        String normalized = normalize(speech);
        String player = normalize(playerMessage);
        if (normalized.isBlank()) {
            return true;
        }
        if (containsAny(normalized, "prompt", "json", "schema", "as an ai", "language model",
                "targeted fact", "npc dialogue brain", "server state", "in this game")) {
            return true;
        }
        if (containsAny(normalized, "i am a witch", "i m a witch", "coven witch", "apothecary",
                "i am a villager", "i m a villager", "hobgoblin trader", "shade of leonard",
                "i am leonard", "baba yaga", "friendly merchant", "shopkeeper", "customer")) {
            return true;
        }
        if (containsAny(normalized, "my coven", "i sell fumes", "i can brew", "open trade",
                "buy from me", "my wares", "serve your coven", "follow you", "protect you",
                "i will help you", "how are you today", "what brings you here")) {
            return true;
        }
        if (containsAny(player, "fight", "beat", "kill", "weakness", "survive")
                && containsAny(normalized, "harmless", "no danger", "cannot hurt", "easy to defeat")) {
            return true;
        }
        return normalized.split("\\s+").length > 38;
    }

    private static void holdAttention(LivingEntity entity, ServerPlayer player) {
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
        }
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
