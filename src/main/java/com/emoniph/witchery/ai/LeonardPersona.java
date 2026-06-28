package com.emoniph.witchery.ai;

import com.emoniph.witchery.entity.LeonardEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

public final class LeonardPersona implements NpcPersona<LeonardEntity> {
    public static final LeonardPersona INSTANCE = new LeonardPersona();

    private LeonardPersona() {
    }

    @Override
    public String id() {
        return "leonard";
    }

    @Override
    public String roleName() {
        return "shade of leonard";
    }

    @Override
    public String defaultName() {
        return "Shade of Leonard";
    }

    @Override
    public Class<LeonardEntity> entityClass() {
        return LeonardEntity.class;
    }

    @Override
    public void beginConversation(LeonardEntity entity, ServerPlayer player) {
        entity.beginPlayerConversation(player);
    }

    @Override
    public void completeConversation(LeonardEntity entity, ServerPlayer player) {
        entity.completePlayerConversation(player);
    }

    @Override
    public void nudgeAttention(LeonardEntity entity, ServerPlayer player) {
        entity.nudgePlayerAttention(player);
    }

    @Override
    public NpcBrainPrompt greetingPrompt(LeonardEntity entity, ServerPlayer player, String memory) {
        return NpcPromptBuilder.leonardGreeting(entity, player, memory);
    }

    @Override
    public NpcBrainPrompt chatPrompt(LeonardEntity entity, ServerPlayer player, String memory,
                                    String playerMessage) {
        return NpcPromptBuilder.leonardChat(entity, player, memory, playerMessage);
    }

    @Override
    public boolean allowsIntent(NpcIntent intent, LeonardEntity entity, ServerPlayer player) {
        return switch (intent) {
            case NONE, QUEST_HINT, WARN_THREAT, COMBAT_TAUNT -> true;
            case OPEN_TRADE, FOLLOW_OWNER -> false;
        };
    }

    @Override
    public Optional<String> fallback(ServerLevel level, LeonardEntity entity, ServerPlayer player,
                                     String playerMessage, String memoryContext) {
        return NpcDialogueFallback.leonardChat(
                playerMessage,
                displayName(entity),
                player.getName().getString(),
                NpcPromptBuilder.leonardPhase(level, entity),
                NpcPromptBuilder.leonardAltarInfo(level, entity),
                NpcPromptBuilder.leonardCovenInfo(level, entity, player),
                NpcPromptBuilder.leonardGearInfo(player),
                memoryContext);
    }

    @Override
    public Optional<String> polishRaw(String rawSpeech, LeonardEntity entity, ServerPlayer player,
                                      String playerMessage, String memoryContext, String expectedClock) {
        Optional<String> polished = NpcBrainResponse.polishSpeechOrGroundedFollowup(rawSpeech, displayName(entity),
                player.getName().getString(), playerMessage, memoryContext, expectedClock);
        if (polished.isEmpty() || rejectsLeonardRaw(polished.get(), playerMessage)) {
            return Optional.empty();
        }
        return polished;
    }

    @Override
    public Optional<String> polishFallback(String speech, LeonardEntity entity, ServerPlayer player,
                                           String playerMessage, String memoryContext, String expectedClock) {
        String cleaned = NpcBrainResponse.sanitize(speech, NpcBrainResponse.MAX_SPEECH_LENGTH);
        return cleaned.isBlank() ? Optional.empty() : Optional.of(cleaned);
    }

    @Override
    public String relationshipNotes(String playerName, List<String> flags) {
        return "";
    }

    @Override
    public String lastResortFallback(LeonardEntity entity, ServerPlayer player, String playerMessage,
                                     String avoidSpeech) {
        return NpcDialogueFallback.leonardLastResort(playerMessage, player.getName().getString(), avoidSpeech);
    }

    @Override
    public Optional<String> alternateForRepeatedFallback(LeonardEntity entity, ServerPlayer player,
                                                         String playerMessage, String avoidSpeech) {
        if (isSpecificLeonardQuestion(playerMessage)) {
            return Optional.empty();
        }
        return NpcDialogueFallback.leonardAlternate(playerMessage, player.getName().getString(), avoidSpeech);
    }

    static boolean rejectsLeonardRaw(String speech, String playerMessage) {
        String normalized = normalize(speech);
        String player = normalize(playerMessage);
        if (normalized.isBlank()) {
            return true;
        }
        if (normalized.equals("the shade of leonard")
                || normalized.equals("shade of leonard")
                || normalized.equals("leonard")
                || normalized.startsWith("the shade of leonard ")
                || normalized.startsWith("shade of leonard ")
                || normalized.endsWith(" shade of leonard")
                || normalized.endsWith(" the shade of leonard")) {
            return true;
        }
        if (containsAny(normalized, "game logic", "game s logic", "minecraft logic", "npc dialogue brain",
                "model", "prompt", "json", "api", "bless arithmetic")) {
            return true;
        }
        if (containsAny(normalized, "prayer", "prayers", "pray", "prayed")) {
            return true;
        }
        if (containsAny(normalized, "accept your trade", "accept your trade dev", "here to trade",
                "trade with me", "no choice but to trade", "offering us a trade", "join our ranks",
                "i am a witcher", "coven of dev", "you are an apprentice", "shade of dev",
                "infernal trade", "infernal commerce", "do not call me by name")) {
            return true;
        }
        if (containsAny(normalized, "aid your coven", "help your coven", "boon for your witches",
                "able to help", "conjured pet", "welcome")) {
            return true;
        }
        if (containsAny(normalized, "lest your soul be lost", "leonard s shade i see")) {
            return true;
        }
        if (isSpecificLeonardQuestion(playerMessage)
                && containsAny(normalized, "speak plainly dev i am here", "i am here dev", "i am here")) {
            return true;
        }
        if (normalized.endsWith("if you do")) {
            return true;
        }
        if (containsAny(normalized, "friendly merchant", "shopkeeper", "customer", "my friend")) {
            return true;
        }
        if ((player.contains("hear me") || player.contains("can you hear"))
                && normalized.split(" ").length > 22) {
            return true;
        }
        if (containsAny(player, "where were you summoned from", "where did you come from", "where are you from",
                "summoned from", "goat", "horns", "hooves", "satyr")
                && !containsAny(normalized, "sabbath", "threshold", "crossroads", "goat", "horn", "hooves")) {
            return true;
        }
        if ((player.contains("altar") || player.contains("paid for"))
                && !containsAny(normalized, "altar", "power", "recharge", "range", "distance", "12000", "16000")) {
            return true;
        }
        if ((player.contains("altar") || player.contains("paid for"))
                && containsAny(normalized, "power points", "turns")) {
            return true;
        }
        if (containsAny(player, "coven", "witches")
                && !containsAny(normalized, "coven", "witch", "lilith", "mara", "sworn", "free")) {
            return true;
        }
        if (containsAny(player, "coven", "witches")
                && containsAny(normalized, "aid", "boon", "help", "protect")) {
            return true;
        }
        if (containsAny(player, "urn", "leonard s urn", "leonards urn")
                && !containsAny(normalized, "thrown brew", "sneak use", "sneak", "four slot", "4 slot")) {
            return true;
        }
        if (containsAny(player, "strike", "hit you", "attack you")
                && !containsAny(normalized, "mortal coil", "fight", "blood", "struck", "violence", "attacker")) {
            return true;
        }
        if (player.contains("mortal coil") && !containsAny(normalized, "withering", "curse", "mark", "attacker")) {
            return true;
        }
        if (containsAny(player, "lost soul", "lost souls")
                && !containsAny(normalized, "green", "red", "black", "break", "shield", "cannot be harmed")) {
            return true;
        }
        if (containsAny(player, "red soul", "red souls")
                && !containsAny(normalized, "red", "fire", "blast", "explosion")) {
            return true;
        }
        if (containsAny(player, "black soul", "black souls")
                && !containsAny(normalized, "black", "magic", "potion")) {
            return true;
        }
        if (player.contains("demonbane") && !containsAny(normalized, "wounded", "late", "threshold", "below")) {
            return true;
        }
        if (player.contains("weakness")
                && !containsAny(normalized, "strike", "souls", "demonbane", "wounded", "break")) {
            return true;
        }
        if (containsAny(player, "do not want to fight", "dont want to fight", "avoid fighting")
                && !containsAny(normalized, "neutral", "struck", "blood", "fight")) {
            return true;
        }
        if (containsAny(player, "learn from you", "teach me", "careful witch")
                && !containsAny(normalized, "warning", "souls", "demonbane", "circle", "shortcut", "interest")) {
            return true;
        }
        if (containsAny(player, "bargain", "price", "deal")
                && containsAny(normalized, "accept your trade", "trade with me", "offering us a trade")) {
            return true;
        }
        return false;
    }

    private static boolean isSpecificLeonardQuestion(String playerMessage) {
        String player = normalize(playerMessage);
        return containsAny(player, "what should i call you", "what do i call you", "where were you summoned",
                "where did you come from", "where are you from", "goat", "horn", "hoof", "hooves",
                "cauldron pull", "how did the cauldron", "altar", "coven", "witches", "urn", "arthana",
                "strike you", "hit you", "mortal coil", "lost soul", "lost souls", "red soul", "black soul",
                "demonbane", "bargain", "price", "fight", "weakness", "final warning", "before i leave",
                "learn from you", "teach me");
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
        return text == null ? "" : text.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
