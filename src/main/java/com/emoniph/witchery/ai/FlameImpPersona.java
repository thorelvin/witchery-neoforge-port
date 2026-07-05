package com.emoniph.witchery.ai;

import com.emoniph.witchery.entity.FlameImpEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Optional;

public final class FlameImpPersona implements NpcPersona<FlameImpEntity> {
    public static final FlameImpPersona INSTANCE = new FlameImpPersona();

    private FlameImpPersona() {
    }

    @Override
    public String id() {
        return "flame_imp";
    }

    @Override
    public String roleName() {
        return "flame imp";
    }

    @Override
    public String defaultName() {
        return "Flame Imp";
    }

    @Override
    public Class<FlameImpEntity> entityClass() {
        return FlameImpEntity.class;
    }

    @Override
    public int chatColorRgb() {
        return 0xFF6600;
    }

    @Override
    public void beginConversation(FlameImpEntity entity, ServerPlayer player) {
        holdAttention(entity, player);
    }

    @Override
    public void completeConversation(FlameImpEntity entity, ServerPlayer player) {
        holdAttention(entity, player);
    }

    @Override
    public void nudgeAttention(FlameImpEntity entity, ServerPlayer player) {
        holdAttention(entity, player);
    }

    @Override
    public NpcBrainPrompt greetingPrompt(FlameImpEntity entity, ServerPlayer player, String memory) {
        return NpcPromptBuilder.flameImpGreeting(entity, player, memory);
    }

    @Override
    public NpcBrainPrompt chatPrompt(FlameImpEntity entity, ServerPlayer player, String memory,
                                     String playerMessage) {
        return NpcPromptBuilder.flameImpChat(entity, player, memory, playerMessage);
    }

    @Override
    public boolean allowsIntent(NpcIntent intent, FlameImpEntity entity, ServerPlayer player) {
        return switch (intent) {
            case NONE, QUEST_HINT, WARN_THREAT, COMBAT_TAUNT -> true;
            case OPEN_TRADE, FOLLOW_OWNER -> false;
        };
    }

    @Override
    public Optional<String> fallback(ServerLevel level, FlameImpEntity entity, ServerPlayer player,
                                     String playerMessage, String memoryContext) {
        return NpcDialogueFallback.flameImpChat(
                playerMessage,
                displayName(entity),
                player.getName().getString(),
                NpcPromptBuilder.flameImpState(entity, player),
                memoryContext);
    }

    @Override
    public Optional<String> polishRaw(String rawSpeech, FlameImpEntity entity, ServerPlayer player,
                                      String playerMessage, String memoryContext, String expectedClock) {
        Optional<String> polished = NpcBrainResponse.polishSpeechOrGroundedFollowup(rawSpeech, displayName(entity),
                player.getName().getString(), playerMessage, memoryContext, expectedClock);
        if (polished.isEmpty() || rejectsFlameImpRaw(polished.get(), playerMessage)) {
            return Optional.empty();
        }
        return polished;
    }

    @Override
    public Optional<String> polishFallback(String speech, FlameImpEntity entity, ServerPlayer player,
                                           String playerMessage, String memoryContext, String expectedClock) {
        String cleaned = NpcBrainResponse.sanitize(speech, NpcBrainResponse.MAX_SPEECH_LENGTH);
        return cleaned.isBlank() ? Optional.empty() : Optional.of(cleaned);
    }

    @Override
    public String lastResortFallback(FlameImpEntity entity, ServerPlayer player, String playerMessage,
                                     String avoidSpeech) {
        return NpcDialogueFallback.flameImpLastResort(playerMessage, player.getName().getString(),
                NpcPromptBuilder.flameImpState(entity, player), avoidSpeech);
    }

    @Override
    public Optional<String> alternateForRepeatedFallback(FlameImpEntity entity, ServerPlayer player,
                                                         String playerMessage, String avoidSpeech) {
        return NpcDialogueFallback.flameImpAlternate(playerMessage, player.getName().getString(),
                NpcPromptBuilder.flameImpState(entity, player), avoidSpeech);
    }

    static boolean rejectsFlameImpRaw(String speech, String playerMessage) {
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
                "hobgoblin trader", "villager here", "i am a villager", "shopkeeper",
                "full demon", "demon merchant", "boss")) {
            return true;
        }
        if (containsAny(normalized, "lord grimorius", "serve lord", "commands me to be here",
                "guide your mind into slumber", "keep your warmth close", "ready to serve",
                "serve and satisfy", "sole purpose is to provide")) {
            return true;
        }
        if (containsAny(normalized, "my coven", "i can brew", "i sell fumes", "open trade",
                "buy from me", "my wares", "protect you", "i will help you safely",
                "how are you today", "what brings you here")) {
            return true;
        }
        if (containsAny(player, "shiny", "gift", "what do you want")
                && !containsAny(normalized, "shiny", "gift", "spark", "fire", "contract", "want")) {
            return true;
        }
        if (containsAny(player, "contract", "tame", "own", "owner", "pet")
                && !containsAny(normalized, "contract", "signed", "wild", "owned", "pet", "tame", "xp",
                "cost")) {
            return true;
        }
        return normalized.split("\\s+").length > 30;
    }

    private static void holdAttention(FlameImpEntity entity, ServerPlayer player) {
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
