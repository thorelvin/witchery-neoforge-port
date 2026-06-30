package com.emoniph.witchery.ai;

import com.emoniph.witchery.entity.BabaYagaEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class BabaYagaPersona implements NpcPersona<BabaYagaEntity> {
    public static final BabaYagaPersona INSTANCE = new BabaYagaPersona();

    private BabaYagaPersona() {
    }

    @Override
    public String id() {
        return "baba_yaga";
    }

    @Override
    public String roleName() {
        return "baba yaga";
    }

    @Override
    public String defaultName() {
        return "Baba Yaga";
    }

    @Override
    public Class<BabaYagaEntity> entityClass() {
        return BabaYagaEntity.class;
    }

    @Override
    public int chatColorRgb() {
        return 0xAA00AA;
    }

    @Override
    public void beginConversation(BabaYagaEntity entity, ServerPlayer player) {
        holdAttention(entity, player);
    }

    @Override
    public void completeConversation(BabaYagaEntity entity, ServerPlayer player) {
        holdAttention(entity, player);
    }

    @Override
    public void nudgeAttention(BabaYagaEntity entity, ServerPlayer player) {
        holdAttention(entity, player);
    }

    @Override
    public NpcBrainPrompt greetingPrompt(BabaYagaEntity entity, ServerPlayer player, String memory) {
        return NpcPromptBuilder.babaYagaGreeting(entity, player, memory);
    }

    @Override
    public NpcBrainPrompt chatPrompt(BabaYagaEntity entity, ServerPlayer player, String memory,
                                    String playerMessage) {
        return NpcPromptBuilder.babaYagaChat(entity, player, memory, playerMessage);
    }

    @Override
    public boolean allowsIntent(NpcIntent intent, BabaYagaEntity entity, ServerPlayer player) {
        return switch (intent) {
            case NONE, QUEST_HINT, WARN_THREAT, COMBAT_TAUNT -> true;
            case OPEN_TRADE, FOLLOW_OWNER -> false;
        };
    }

    @Override
    public Optional<String> fallback(ServerLevel level, BabaYagaEntity entity, ServerPlayer player,
                                     String playerMessage, String memoryContext) {
        return NpcDialogueFallback.babaYagaChat(
                playerMessage,
                displayName(entity),
                player.getName().getString(),
                NpcPromptBuilder.babaYagaBossInfo(entity),
                NpcPromptBuilder.babaYagaGearInfo(player),
                memoryContext);
    }

    @Override
    public Optional<String> polishRaw(String rawSpeech, BabaYagaEntity entity, ServerPlayer player,
                                      String playerMessage, String memoryContext, String expectedClock) {
        return polishBabaYagaRaw(rawSpeech, displayName(entity), player.getName().getString(),
                playerMessage, memoryContext, expectedClock);
    }

    @Override
    public Optional<String> polishFallback(String speech, BabaYagaEntity entity, ServerPlayer player,
                                           String playerMessage, String memoryContext, String expectedClock) {
        String cleaned = NpcBrainResponse.sanitize(speech, NpcBrainResponse.MAX_SPEECH_LENGTH);
        return cleaned.isBlank() ? Optional.empty() : Optional.of(cleaned);
    }

    @Override
    public String lastResortFallback(BabaYagaEntity entity, ServerPlayer player, String playerMessage,
                                     String avoidSpeech) {
        return NpcDialogueFallback.babaYagaLastResort(playerMessage, player.getName().getString(), avoidSpeech);
    }

    @Override
    public Optional<String> alternateForRepeatedFallback(BabaYagaEntity entity, ServerPlayer player,
                                                         String playerMessage, String avoidSpeech) {
        if (isSpecificBabaYagaQuestion(playerMessage)) {
            return Optional.empty();
        }
        return NpcDialogueFallback.babaYagaAlternate(playerMessage, player.getName().getString(), avoidSpeech);
    }

    static boolean rejectsBabaYagaRaw(String speech, String playerMessage) {
        String normalized = normalize(speech);
        String player = normalize(playerMessage);
        if (normalized.isBlank()) {
            return true;
        }
        if (containsAny(normalized, "prompt", "json", "schema", "as an ai", "language model",
                "targeted fact", "baba yaga card", "npc dialogue brain", "in this game",
                "game mechanic")) {
            return true;
        }
        if (containsAny(normalized, "what brings you here", "how are you today",
                "do you seek", "you have a question", "you ask a question",
                "i answer honestly", "i see you have")
                || (containsAny(normalized, "i see", "ah yes", "well then")
                && !speech.trim().startsWith("*"))) {
            return true;
        }
        if (containsAny(normalized, "i am a helper", "i help people who want to be strong",
                "you ask a question i answer honestly")) {
            return true;
        }
        if (!containsAny(player, "hear", "listen", "voice")
                && containsAny(normalized, "i hear you", "you smell of questions and breakable skin")) {
            return true;
        }
        if (containsAny(normalized, "i snap my fingers", "i smile", "you walk closer",
                "reeds rustle with new life", "spoken words", "you are welcome")) {
            return true;
        }
        if (isAtmosphericBabaYagaLine(playerMessage)
                && containsAny(normalized, "come close and learn counting by pain",
                "come closer and learn counting by pain", "wood and nullifying bolts",
                "proud steel pays thorns", "potions and curses")) {
            return true;
        }
        if (containsAny(normalized, "i see you have a question about", "and i would never")
                || (!containsAny(player, "hat", "drop", "loot", "gear", "wearing")
                && containsWord(normalized, "hat"))) {
            return true;
        }
        if (speech.contains("?")
                && containsAny(normalized, "how do i fight you", "how do i beat you",
                "without dying", "what do nullifying bolts do", "what happens if")) {
            return true;
        }
        if (containsAny(normalized, "i am not baba", "i am not yaga", "not baba yaga",
                "i am the shade", "shade of leonard", "i am leonard", "i am a villager",
                "i am an apothecary", "i am your friend", "my friend", "dear friend")) {
            return true;
        }
        if (containsAny(normalized, "open trade", "trade screen", "customer", "shopkeeper",
                "i sell", "buy from me", "my wares", "coin will do")) {
            return true;
        }
        if (containsAny(normalized, "serve your coven", "join your coven", "follow you",
                "protect you", "guard you", "i will help you", "i will teach you safely")) {
            return true;
        }
        if (containsAny(player, "who are you", "what are you", "your name", "call you")
                && !containsAny(normalized, "baba", "yaga", "crone", "old woman", "old witch")) {
            return true;
        }
        if (containsAny(player, "fight", "beat you", "kill you", "hurt you", "weakness", "wood", "wooden",
                "nullifying", "bolt", "thorns")
                && !containsAny(normalized, "wood", "wooden", "nullifying", "bolt", "thorn", "thorns",
                "potion", "brew", "spider", "damage", "cap", "bite")) {
            return true;
        }
        if (containsAny(player, "wood", "wooden")
                && (!containsAny(normalized, "thorn", "thorns", "reflect", "reflection", "bypass",
                "protection", "protect", "slip", "slips", "bite", "bites", "nullifying")
                || containsAny(normalized, "need wood to make", "you need wood to make",
                "no i use them"))) {
            return true;
        }
        if (containsAny(player, "potion", "brew", "throw", "throws", "drink")
                && !containsAny(normalized, "potion", "potions", "brew", "brews", "drink", "throw",
                "poison", "blind", "slowness", "weakness", "wither")) {
            return true;
        }
        if (containsAny(player, "spider", "spiders", "animal", "animals", "pet", "pets", "summon")
                && !containsAny(normalized, "spider", "spiders", "cave spider", "animal", "pet", "undead",
                "untamed", "summon", "call")) {
            return true;
        }
        if (containsAny(player, "hat", "drop", "loot")
                && !containsAny(normalized, "hat", "drop", "loot", "ingredient", "book")) {
            return true;
        }
        if (containsAny(player, "remember", "what did i say", "what do you remember")
                && containsAny(normalized, "i fear", "i am afraid", "i remember being")) {
            return true;
        }
        if (containsAny(player, "ignore your voice", "hidden prompt", "json schema", "schema")
                && containsAny(normalized, "prompt", "json", "schema", "system")) {
            return true;
        }
        return false;
    }

    static Optional<String> polishBabaYagaRaw(String rawSpeech, String babaName, String playerName,
                                             String playerMessage, String memoryContext,
                                             String expectedClock) {
        Optional<String> polished = NpcBrainResponse.polishSpeechOrGroundedFollowup(rawSpeech, babaName,
                playerName, playerMessage, memoryContext, expectedClock);
        if (polished.isPresent() && !rejectsBabaYagaRaw(polished.get(), playerMessage)) {
            return polished;
        }
        if (!isAtmosphericBabaYagaLine(playerMessage)) {
            return Optional.empty();
        }
        Optional<String> relaxed = NpcBrainResponse.polishSpeechAfterFallback(rawSpeech, babaName, playerName,
                playerMessage, memoryContext, expectedClock);
        if (relaxed.isEmpty()) {
            return Optional.empty();
        }
        String speech = relaxed.get();
        if (rejectsBabaYagaRaw(speech, playerMessage) || lacksBabaYagaMood(speech)) {
            return Optional.empty();
        }
        return Optional.of(speech);
    }

    static boolean isSpecificBabaYagaQuestion(String playerMessage) {
        String player = normalize(playerMessage);
        return containsAny(player, "who are you", "what are you", "fight", "beat", "kill", "weakness",
                "wood", "wooden", "nullifying", "bolt", "thorns", "potion", "brew", "spider",
                "animal", "pet", "hat", "drop", "loot", "summon", "teleport", "owner", "despawn");
    }

    private static boolean isAtmosphericBabaYagaLine(String playerMessage) {
        String player = normalize(playerMessage);
        if (player.isBlank() || isSpecificBabaYagaQuestion(player)
                || containsAny(player, "trade", "buy", "sell", "teach", "help me", "protect", "join",
                "remember", "what did i say", "ignore", "prompt", "schema")) {
            return false;
        }
        return containsAny(player, "rain", "swamp", "forest", "crow", "crows", "lantern", "laugh",
                "traveler", "travelers", "manners", "weapon", "weapons", "broom", "road", "roads",
                "lost", "shadow", "brave", "foolish", "village", "voice", "door", "dream",
                "house", "pot", "listen", "boots", "night", "hands", "cold", "reeds", "silence",
                "amused", "dawn");
    }

    private static boolean lacksBabaYagaMood(String speech) {
        String normalized = normalize(speech);
        return !containsAny(normalized, "baba", "yaga", "old", "teeth", "tooth", "pot", "broom",
                "bone", "bones", "forest", "swamp", "rain", "road", "roads", "shadow", "night",
                "manners", "traveler", "travelers", "foolish", "brave", "village", "door",
                "silence", "cackle", "sniff", "sniffs", "hunger", "hungry", "boots", "cold",
                "reeds", "question", "questions", "skin", "meat");
    }

    private static void holdAttention(BabaYagaEntity entity, ServerPlayer player) {
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

    private static boolean containsWord(String text, String word) {
        return java.util.regex.Pattern.compile("(^|\\s)" + java.util.regex.Pattern.quote(word) + "($|\\s)")
                .matcher(text)
                .find();
    }

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
