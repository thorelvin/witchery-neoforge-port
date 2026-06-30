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
        polished = polished.map(LeonardPersona::naturalizeSceneTerms);
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
                || normalized.contains("i am not leonard")
                || normalized.contains("i m not leonard")
                || normalized.contains("not leonard")
                || normalized.contains("name of your primer")
                || normalized.contains("shade of the player")
                || normalized.contains("shade of player")
                || normalized.contains("shadow of the player")
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
        if (containsAny(normalized, "i am dev", "i m dev", "i have summoned me",
                "i have summoned my shade", "you speak to him", "speaking to him",
                "how may i serve your purpose")) {
            return true;
        }
        if (containsAny(normalized, "prayer", "prayers", "pray", "prayed")) {
            return true;
        }
        if (containsAny(normalized, "accept your trade", "accept your trade dev", "here to trade",
                "trade with me", "will trade with you", "trade with you if", "no choice but to trade",
                "offering us a trade", "join our ranks",
                "i am a witcher", "coven of dev", "you are an apprentice", "shade of dev",
                "infernal trade", "infernal commerce", "do not call me by name", "grant you boon",
                "bring me your blade and i will grant", "satisfied with my price your blade",
                "answer your questions", "tell me what you need", "serve your purpose")) {
            return true;
        }
        if (containsAny(normalized, "i will bring it to you", "i ll bring it to you")) {
            return true;
        }
        if (normalized.contains("you fear") && !containsAny(player, "fear", "afraid", "scare", "scared",
                "nightmare", "lost soul", "lost souls")) {
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
        if (containsAny(normalized, "do not fear me", "you have no choice", "willing to bargain when your blade is bleeding")) {
            return true;
        }
        if (containsAny(normalized, "the shade s", "dev s soul", "black souls fear nothing",
                "i am mortal coil", "mark you as my slave", "nearby players with")) {
            return true;
        }
        if (containsAny(normalized, "world stands still", "leonard s blood gushes", "shadow fades")
                || containsAny(normalized, "avoid it by striking", "avoid it by striking leonard",
                "avoid it by striking me", "strike leonard first")) {
            return true;
        }
        if (containsAny(normalized, "take three more", "three more to drag", "three more power")) {
            return true;
        }
        if (containsAny(normalized, "nightmares if the player", "if the player is frightened", "targeted fact")) {
            return true;
        }
        if ((player.contains("hear me") || player.contains("can you hear"))
                && (normalized.split(" ").length > 22 || !containsAny(normalized, "hear", "listen", "listening"))) {
            return true;
        }
        if (containsAny(player, "what did i say", "what do you remember", "what did i say i fear")
                && containsAny(normalized, "i am the shade of leonard", "dragged through a cauldron", "owed a price")) {
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
        if ((containsAny(player, "strong enough", "is it strong", "altar strong") && player.contains("altar"))
                && (!containsAny(normalized, "12000", "16000", "ten thousand", "power", "recharge", "range", "distance")
                    || containsAny(normalized, "needs more than it has", "few adjustments"))) {
            return true;
        }
        if (containsAny(player, "what did my altar pay", "altar pay for")
                && containsAny(normalized, "your presence")) {
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
        if (containsAny(player, "strike", "hit you", "attack you", "touch you with steel",
                "touch you with blade", "touch you with my blade", "touch you with my steel")
                && !containsAny(normalized, "mortal coil", "fight", "blood", "struck", "violence", "attacker")) {
            return true;
        }
        if (player.contains("mortal coil")
                && (!containsAny(normalized, "withering", "curse", "mark")
                    || !containsAny(normalized, "attacker", "attackers", "mark", "fight", "struck")
                    || containsAny(normalized, "effective at first", "fades over time", "few minutes",
                    "grow stronger"))) {
            return true;
        }
        if (containsAny(player, "lost soul", "lost souls")
                && !containsAny(normalized, "green", "red", "black", "break", "shield", "cannot be harmed")) {
            return true;
        }
        if (containsAny(player, "red soul", "red souls")
                && (!containsAny(normalized, "red")
                    || !containsAny(normalized, "fire", "blast", "explosion"))) {
            return true;
        }
        if (containsAny(player, "green soul", "green souls")
                && !containsAny(normalized, "green", "steel", "break", "shield")) {
            return true;
        }
        if (containsAny(player, "black soul", "black souls")
                && (!containsAny(normalized, "black")
                    || !containsAny(normalized, "magic", "potion"))) {
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
                && (!containsAny(normalized, "neutral", "struck", "keep your hand", "walk away", "peace")
                    || containsAny(normalized, "rain blood", "blood from your blade", "make haste or die"))) {
            return true;
        }
        if (containsAny(player, "neutral until struck", "until struck")
                && (!containsAny(normalized, "neutral", "struck", "blood", "fight")
                    || containsAny(normalized, "not neutral until struck", "not neutral"))) {
            return true;
        }
        if (containsAny(player, "nether wart first", "need nether wart first")
                && (normalized.startsWith("no ")
                    || !containsAny(normalized, "yes", "nether wart", "first", "begins", "calling", "threshold"))) {
            return true;
        }
        if (containsAny(player, "calling order", "after nether wart", "remind me of the calling")
                && (!containsAny(normalized, "nether wart", "tear of the goddess", "diamond vapour",
                    "diamond vapor", "diamond", "nether star", "hat")
                    || containsAny(normalized, "softer names", "called shade", "leave softer"))) {
            return true;
        }
        if (containsAny(player, "after tear", "comes after tear", "follows the tear",
                "after tear of the goddess")
                && !containsAny(normalized, "diamond vapour", "diamond vapor", "diamond", "nether star")) {
            return true;
        }
        if (containsAny(player, "hat last step", "hat the last step", "witches hat", "witch s hat")
                && !containsAny(normalized, "yes", "hat", "last", "end", "after the five")) {
            return true;
        }
        if (containsAny(player, "how much altar power", "altar power does", "calling need")
                && (!containsAny(normalized, "10000", "ten thousand")
                    || containsAny(normalized, "altar blocks", "circle", "28000", "36000", "48000",
                    "58000", "ten nether warts"))) {
            return true;
        }
        if (containsAny(player, "should i fight", "fight you tonight", "fight tonight")
                && (!containsAny(normalized, "neutral", "struck", "blood", "fight", "walk away", "keep your hand")
                    || containsAny(normalized, "bring me your blade", "bring your blade"))) {
            return true;
        }
        if (containsAny(player, "bow", "arrow", "arrows", "projectile")
                && (!containsAny(normalized, "own hand", "melee", "blade", "distance", "arrow", "bow")
                    || containsAny(normalized, "demonbane bites", "demonbane matters", "demonbane is"))) {
            return true;
        }
        if (containsAny(player, "good night", "goodbye", "farewell", "leaving slowly", "i am leaving")
                && (containsAny(normalized, "i am leaving", "i m leaving", "leaving slowly")
                    || !containsAny(normalized, "good night", "go", "leave", "farewell", "blood",
                    "debt", "behind"))) {
            return true;
        }
        if (containsAny(player, "safe while", "lilith", "mara")
                && (!containsAny(normalized, "safe", "witness", "witch", "lilith", "mara", "sheathed")
                    || containsAny(normalized, "bring your bow", "not your blade"))) {
            return true;
        }
        if (containsAny(player, "learn from you", "teach me", "careful witch")
                && !containsAny(normalized, "warning", "souls", "demonbane", "circle", "shortcut", "interest")) {
            return true;
        }
        if (containsAny(player, "what did i say i fear", "what do i fear", "what am i afraid")
                && !containsAny(normalized, "lost soul", "lost souls", "fear", "afraid")) {
            return true;
        }
        if (containsAny(player, "waiting for me to make a mistake", "waiting for a mistake", "make a mistake")
                && !containsAny(normalized, "wait", "mistake", "payment", "violence", "measuring")) {
            return true;
        }
        if (containsAny(player, "final warning", "one final warning", "last warning")
                && !containsAny(normalized, "souls", "coil", "debt", "blood", "warning", "strike")) {
            return true;
        }
        if (containsAny(player, "bargain", "price", "deal")
                && containsAny(normalized, "accept your trade", "trade with me", "offering us a trade",
                "i am neutral until struck")) {
            return true;
        }
        return false;
    }

    static String naturalizeSceneTerms(String speech) {
        return speech
                .replaceAll("(?i)\\bplayer's own hand\\b", "your own hand")
                .replaceAll("(?i)\\bplayer coven\\s+\\d+/\\d+\\b", "your coven")
                .replaceAll("(?i)\\b(none|zero)\\s+of\\s+\\w+\\s+sworn\\s+to\\s+[^;,.]+",
                        "no witches carry your oath")
                .replaceAll("(?i)\\bone\\s+of\\s+\\w+\\s+sworn\\s+to\\s+[^;,.]+",
                        "one witch carries your oath")
                .replaceAll("(?i)\\btwo\\s+of\\s+\\w+\\s+sworn\\s+to\\s+[^;,.]+",
                        "two witches carry your oath")
                .replaceAll("(?i)\\bthree\\s+of\\s+\\w+\\s+sworn\\s+to\\s+[^;,.]+",
                        "three witches carry your oath")
                .replaceAll("(?i)\\bfour\\s+of\\s+\\w+\\s+sworn\\s+to\\s+[^;,.]+",
                        "four witches carry your oath")
                .replaceAll("(?i)\\bfive\\s+of\\s+\\w+\\s+sworn\\s+to\\s+[^;,.]+",
                        "five witches carry your oath")
                .replaceAll("(?i)\\bsix\\s+of\\s+\\w+\\s+sworn\\s+to\\s+[^;,.]+",
                        "six witches carry your oath")
                .replaceAll("(?i)\\b(none|zero)\\s+of\\s+\\w+\\s+sworn\\s+to\\s+(?:the\\s+)?player(?:'s)?\\s+coven\\b",
                        "no witches carry your oath")
                .replaceAll("(?i)\\bone\\s+of\\s+\\w+\\s+sworn\\s+to\\s+(?:the\\s+)?player(?:'s)?\\s+coven\\b",
                        "one witch carries your oath")
                .replaceAll("(?i)\\btwo\\s+of\\s+\\w+\\s+sworn\\s+to\\s+(?:the\\s+)?player(?:'s)?\\s+coven\\b",
                        "two witches carry your oath")
                .replaceAll("(?i)\\bthree\\s+of\\s+\\w+\\s+sworn\\s+to\\s+(?:the\\s+)?player(?:'s)?\\s+coven\\b",
                        "three witches carry your oath")
                .replaceAll("(?i)\\bfour\\s+of\\s+\\w+\\s+sworn\\s+to\\s+(?:the\\s+)?player(?:'s)?\\s+coven\\b",
                        "four witches carry your oath")
                .replaceAll("(?i)\\bfive\\s+of\\s+\\w+\\s+sworn\\s+to\\s+(?:the\\s+)?player(?:'s)?\\s+coven\\b",
                        "five witches carry your oath")
                .replaceAll("(?i)\\bsix\\s+of\\s+\\w+\\s+sworn\\s+to\\s+(?:the\\s+)?player(?:'s)?\\s+coven\\b",
                        "six witches carry your oath")
                .replaceAll("(?i)\\bnearby coven witches\\b", "nearby witches")
                .replaceAll("\\s+", " ")
                .trim();
    }

    static boolean isSpecificLeonardQuestion(String playerMessage) {
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
