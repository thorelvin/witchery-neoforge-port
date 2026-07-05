package com.emoniph.witchery.ai;

import com.emoniph.witchery.entity.DemonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Optional;

public final class DemonPersona implements NpcPersona<DemonEntity> {
    public static final DemonPersona INSTANCE = new DemonPersona();

    private DemonPersona() {
    }

    @Override
    public String id() {
        return "demon";
    }

    @Override
    public String roleName() {
        return "demon";
    }

    @Override
    public String defaultName() {
        return "Demon";
    }

    @Override
    public Class<DemonEntity> entityClass() {
        return DemonEntity.class;
    }

    @Override
    public int chatColorRgb() {
        return 0xDD3300;
    }

    @Override
    public void beginConversation(DemonEntity entity, ServerPlayer player) {
        holdAttention(entity, player);
    }

    @Override
    public void completeConversation(DemonEntity entity, ServerPlayer player) {
        holdAttention(entity, player);
    }

    @Override
    public void nudgeAttention(DemonEntity entity, ServerPlayer player) {
        holdAttention(entity, player);
    }

    @Override
    public NpcBrainPrompt greetingPrompt(DemonEntity entity, ServerPlayer player, String memory) {
        return NpcPromptBuilder.demonGreeting(entity, player, memory);
    }

    @Override
    public NpcBrainPrompt chatPrompt(DemonEntity entity, ServerPlayer player, String memory,
                                     String playerMessage) {
        return NpcPromptBuilder.demonChat(entity, player, memory, playerMessage);
    }

    @Override
    public boolean allowsIntent(NpcIntent intent, DemonEntity entity, ServerPlayer player) {
        return switch (intent) {
            case NONE, OPEN_TRADE, QUEST_HINT, WARN_THREAT, COMBAT_TAUNT -> true;
            case FOLLOW_OWNER -> false;
        };
    }

    @Override
    public Optional<String> fallback(ServerLevel level, DemonEntity entity, ServerPlayer player,
                                     String playerMessage, String memoryContext) {
        return NpcDialogueFallback.demonChat(
                playerMessage,
                displayName(entity),
                player.getName().getString(),
                NpcPromptBuilder.demonState(entity, player),
                memoryContext);
    }

    @Override
    public Optional<String> polishRaw(String rawSpeech, DemonEntity entity, ServerPlayer player,
                                      String playerMessage, String memoryContext, String expectedClock) {
        Optional<String> polished = NpcBrainResponse.polishSpeechOrGroundedFollowup(rawSpeech, displayName(entity),
                player.getName().getString(), playerMessage, memoryContext, expectedClock);
        if (polished.isEmpty() || rejectsDemonRaw(polished.get(), playerMessage)) {
            return Optional.empty();
        }
        return polished;
    }

    @Override
    public Optional<String> polishFallback(String speech, DemonEntity entity, ServerPlayer player,
                                           String playerMessage, String memoryContext, String expectedClock) {
        String cleaned = NpcBrainResponse.sanitize(speech, NpcBrainResponse.MAX_SPEECH_LENGTH);
        return cleaned.isBlank() ? Optional.empty() : Optional.of(cleaned);
    }

    @Override
    public String lastResortFallback(DemonEntity entity, ServerPlayer player, String playerMessage,
                                     String avoidSpeech) {
        return NpcDialogueFallback.demonLastResort(playerMessage, player.getName().getString(),
                NpcPromptBuilder.demonState(entity, player), avoidSpeech);
    }

    @Override
    public Optional<String> alternateForRepeatedFallback(DemonEntity entity, ServerPlayer player,
                                                         String playerMessage, String avoidSpeech) {
        return NpcDialogueFallback.demonAlternate(playerMessage, player.getName().getString(),
                NpcPromptBuilder.demonState(entity, player), avoidSpeech);
    }

    static boolean rejectsDemonRaw(String speech, String playerMessage) {
        String normalized = normalize(speech);
        String player = normalize(playerMessage);
        if (normalized.isBlank()) {
            return true;
        }
        if (containsAny(normalized, "prompt", "json", "schema", "as an ai", "language model",
                "targeted fact", "npc dialogue brain", "server state", "in this game")) {
            return true;
        }
        if (NpcPersonaSpeechGuards.rejectsNarrationOnly(speech)
                || NpcPersonaSpeechGuards.rejectsSoftCompanion(speech)
                || NpcPersonaSpeechGuards.rejectsUnsupportedProtection(speech, playerMessage)) {
            return true;
        }
        if (containsAny(normalized, "coven witch", "shade of leonard", "baba yaga", "apothecary",
                "hobgoblin", "villager here", "i am a villager", "i am a flame imp", "i m a flame imp",
                "i am a little imp", "i m a little imp", "friendly merchant", "shopkeeper", "customer")) {
            return true;
        }
        if (containsAny(normalized, "only sell devil", "all i have to offer", "only hope",
                "faithful little demon trader", "safe ride")) {
            return true;
        }
        if (containsAny(normalized, "my coven", "i can brew", "i sell fumes", "serve your coven",
                "follow you", "protect you", "i will help you safely", "how are you today",
                "what brings you here")) {
            return true;
        }
        if (containsAny(player, "trade", "bargain", "deal", "price", "what do you want")
                && !containsAny(normalized, "trade", "bargain", "deal", "price", "coin", "cost",
                "charm", "offering", "pay")) {
            return true;
        }
        if (containsAny(player, "devil", "tongue", "charm")
                && !containsAny(normalized, "charm", "tongue", "bargain", "safe", "safety", "muzzle")) {
            return true;
        }
        return normalized.split("\\s+").length > 36;
    }

    private static void holdAttention(DemonEntity entity, ServerPlayer player) {
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
