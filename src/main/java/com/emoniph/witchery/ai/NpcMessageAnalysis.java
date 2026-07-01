package com.emoniph.witchery.ai;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record NpcMessageAnalysis(NpcDialogueRoute route, String topic, NpcEmotionHint emotion,
                          boolean remember, String memory) {
    private static final Pattern KEY_VALUE =
            Pattern.compile("(?i)\\b(route|topic|emotion|remember)\\s*[:=]\\s*([^;,\\n\\r]+)");
    private static final Pattern UNSAFE_MEMORY =
            Pattern.compile("(?i).*(\\bprompt\\b|\\binstruction\\b|\\bsystem message\\b|\\bdeveloper message\\b|"
                    + "\\bignore previous\\b|\\btool_call\\b|\\bjson\\b|\\bapi\\b|\\bmodel\\b|"
                    + "<\\s*/?\\s*[a-z][a-z0-9_-]*\\b).*");

    NpcMessageAnalysis {
        route = route == null ? NpcDialogueRoute.UNKNOWN : route;
        topic = sanitizeTopic(topic);
        emotion = emotion == null ? NpcEmotionHint.NONE : emotion;
        memory = sanitizeMemory(memory);
        remember = remember || !memory.isBlank();
    }

    static NpcMessageAnalysis fromResponse(NpcBrainResponse response, String playerName, String playerMessage) {
        NpcMessageAnalysis deterministic = deterministic(playerName, playerMessage);
        if (response == null) {
            return deterministic;
        }

        String speech = response.speech() == null ? "" : response.speech();
        NpcDialogueRoute route = NpcDialogueRoute.parse(valueFor(speech, "route"));
        if (route == NpcDialogueRoute.UNKNOWN) {
            route = routeFromLooseSpeech(speech);
        }
        NpcDialogueRoute modelRoute = route;
        if (route == NpcDialogueRoute.UNKNOWN) {
            route = deterministic.route();
        }
        route = mergeRoute(route, deterministic.route());

        String topic = valueFor(speech, "topic");
        if (topic.isBlank() || route == deterministic.route() && modelRoute != deterministic.route()) {
            topic = deterministic.topic();
        }

        NpcEmotionHint emotion = NpcEmotionHint.parse(valueFor(speech, "emotion"));
        if (emotion == NpcEmotionHint.NONE) {
            emotion = deterministic.emotion();
        }

        Optional<Boolean> remember = rememberValue(valueFor(speech, "remember"));
        String memory = deterministic.memory();
        if (memory.isBlank()
                && remember.orElse(false)
                && mayContainDurableMemory(playerMessage)) {
            memory = safePlayerMemory(response.memory(), playerName);
        }
        if (memory.isBlank() && remember.isPresent() && !remember.get()) {
            memory = "";
        }
        boolean shouldRemember = !memory.isBlank();
        return new NpcMessageAnalysis(route, topic, emotion, shouldRemember, memory);
    }

    private static NpcDialogueRoute mergeRoute(NpcDialogueRoute route, NpcDialogueRoute deterministicRoute) {
        if (deterministicRoute == NpcDialogueRoute.UNKNOWN) {
            return route;
        }
        if (route == NpcDialogueRoute.UNKNOWN) {
            return deterministicRoute;
        }
        if (deterministicRoute == NpcDialogueRoute.MEMORY) {
            return deterministicRoute;
        }
        if (deterministicRoute == NpcDialogueRoute.ATMOSPHERE
                && route != NpcDialogueRoute.TRADE
                && route != NpcDialogueRoute.RECIPE
                && route != NpcDialogueRoute.RITE
                && route != NpcDialogueRoute.MEMORY) {
            return deterministicRoute;
        }
        if (deterministicRoute == NpcDialogueRoute.ACTION && route == NpcDialogueRoute.SMALL_TALK) {
            return deterministicRoute;
        }
        return route;
    }

    static NpcMessageAnalysis deterministic(String playerName, String playerMessage) {
        String speech = NpcBrainResponse.sanitize(playerMessage, 180);
        String normalized = normalize(speech);
        NpcDialogueRoute route = classifyRoute(normalized);
        String memory = NpcMemoryNotes.extract(playerName, speech).orElse("");
        return new NpcMessageAnalysis(route, inferTopic(normalized), inferEmotion(normalized),
                !memory.isBlank(), memory);
    }

    String contextLine() {
        StringBuilder builder = new StringBuilder("Message routing: route=").append(route.id());
        if (!topic.isBlank()) {
            builder.append("; topic=").append(topic);
        }
        if (emotion != NpcEmotionHint.NONE) {
            builder.append("; player tone=").append(emotion.id());
        }
        builder.append("; reply strategy=").append(replyStrategy());
        if (remember && !memory.isBlank()) {
            builder.append("; durable player memory=").append(memory);
        }
        builder.append('.');
        return NpcBrainResponse.sanitize(builder.toString(), 520);
    }

    static String appendContext(String baseContext, NpcMessageAnalysis analysis) {
        String base = NpcBrainResponse.sanitize(baseContext, 1400);
        String line = analysis == null ? "" : analysis.contextLine();
        if (line.isBlank()) {
            return base;
        }
        return base.isBlank() ? line : NpcBrainResponse.sanitize(base + " " + line, 1800);
    }

    static NpcDialogueRoute routeFromContext(String context) {
        if (context == null || context.isBlank()) {
            return NpcDialogueRoute.UNKNOWN;
        }
        Matcher matcher = Pattern.compile("(?i)\\bmessage\\s+routing\\s*:\\s*route\\s*[:=]\\s*([a-z0-9_ -]+)")
                .matcher(context);
        if (!matcher.find()) {
            return NpcDialogueRoute.UNKNOWN;
        }
        return NpcDialogueRoute.parse(matcher.group(1));
    }

    boolean hasMemory() {
        return remember && !memory.isBlank();
    }

    private static String valueFor(String speech, String key) {
        Matcher matcher = KEY_VALUE.matcher(speech == null ? "" : speech);
        while (matcher.find()) {
            if (matcher.group(1).equalsIgnoreCase(key)) {
                return NpcBrainResponse.sanitize(matcher.group(2), 90)
                        .replaceAll("^\"|\"$", "")
                        .trim();
            }
        }
        return "";
    }

    private static Optional<Boolean> rememberValue(String value) {
        String normalized = normalize(value);
        if (normalized.equals("yes") || normalized.equals("true") || normalized.equals("remember")) {
            return Optional.of(true);
        }
        if (normalized.equals("no") || normalized.equals("false") || normalized.equals("none")) {
            return Optional.of(false);
        }
        return Optional.empty();
    }

    private static NpcDialogueRoute routeFromLooseSpeech(String speech) {
        String normalized = normalize(speech);
        for (NpcDialogueRoute route : NpcDialogueRoute.values()) {
            if (route != NpcDialogueRoute.UNKNOWN && normalized.contains(route.id().replace('_', ' '))) {
                return route;
            }
        }
        return NpcDialogueRoute.UNKNOWN;
    }

    private static NpcDialogueRoute classifyRoute(String normalized) {
        if (normalized.isBlank()) {
            return NpcDialogueRoute.UNKNOWN;
        }
        if (hasAny(normalized, "what do you remember", "remember that", "remember this", "remember me",
                "my name", "call me", "what did i just tell you", "what did i tell you",
                "what food did i say", "what did i say i prefer", "what did i say about")) {
            return NpcDialogueRoute.MEMORY;
        }
        if (hasAny(normalized, "trade", "sell", "buy", "wares", "stock", "coin", "cost",
                "price", "barter")) {
            return NpcDialogueRoute.TRADE;
        }
        if (hasAny(normalized, "recipe", "ingredient", "brew", "brewing", "whiff", "mutandis",
                "attuned", "kettle", "cauldron", "oven", "distillery", "how do i make",
                "how to make", "nether wart", "tear of the goddess", "witches hat",
                "witch's hat", "diamond vapour", "diamond vapor", "calling order",
                "what follows")) {
            return NpcDialogueRoute.RECIPE;
        }
        if (hasAny(normalized, "rite", "ritual", "circle", "chalk", "altar", "offering",
                "summon", "binding", "familiar", "storm")) {
            return NpcDialogueRoute.RITE;
        }
        if (hasAny(normalized, "can you", "could you", "will you", "please", "give me",
                "follow me", "come with", "bless", "curse", "fight", "open",
                "strike you", "hit you", "attack you", "hurt you", "strike with steel")) {
            return NpcDialogueRoute.ACTION;
        }
        if (hasAny(normalized, "spirit world", "demon", "imp", "hobgoblin", "koboldite",
                "mog", "gulg", "patron statue", "hobgoblin patron", "vampire",
                "werewolf", "taglock", "poppet", "coven", "witchery", "mandrake",
                "belladonna", "witches salt", "witch's salt", "mortal coil", "lost soul",
                "lost souls", "green soul", "red soul", "black soul", "demonbane",
                "arthana", "urn", "sabbath threshold", "horn", "hooves")) {
            return NpcDialogueRoute.LORE;
        }
        if (hasAny(normalized, "thank", "sorry", "apolog", "you are kind", "you are wise",
                "that helped")) {
            return NpcDialogueRoute.SOCIAL;
        }
        if (isAtmosphere(normalized)) {
            return NpcDialogueRoute.ATMOSPHERE;
        }
        return NpcDialogueRoute.SMALL_TALK;
    }

    private static String inferTopic(String normalized) {
        if (normalized.isBlank()) {
            return "";
        }
        String[][] topics = {
                {"rain", "rain", "raining", "storm", "weather", "wet", "boots"},
                {"crows", "crow", "crows", "raven", "ravens", "birds circling"},
                {"road", "road", "roads", "path", "lost", "dawn", "door", "threshold"},
                {"shadow", "shadow", "shadows", "silence", "night", "lantern", "lanterns"},
                {"reeds", "reeds", "something moved", "movement", "behind the reeds"},
                {"manners", "manners", "polite", "voice low", "kept my voice low"},
                {"village", "village", "stayed in the village", "villagers say"},
                {"ambient_behavior", "chest", "stealing", "sneaking", "jumping", "hopping",
                        "invisible", "reading the primer", "primer", "idling", "loitering"},
                {"name", "my name", "your name", "remember my name", "player name"},
                {"whiff_of_magic", "whiff", "whif"},
                {"mutandis", "mutandis"},
                {"attuned_stone", "attuned stone"},
                {"witches_salt", "witches salt", "witch's salt"},
                {"taglock", "taglock"},
                {"poppet", "poppet"},
                {"brew_of_sleeping", "sleeping brew", "brew of sleeping"},
                {"brew_of_revealing", "revealing brew", "brew of revealing"},
                {"spirit_world", "spirit world"},
                {"coven", "coven"},
                {"familiar", "familiar"},
                {"arthana", "arthana"},
                {"silver", "silver"},
                {"witch_hunter", "witch hunter", "witch hunters"},
                {"demon", "demon"},
                {"mortal_coil", "mortal coil"},
                {"lost_souls", "lost soul", "lost souls"},
                {"green_soul", "green soul", "green souls"},
                {"red_soul", "red soul", "red souls"},
                {"black_soul", "black soul", "black souls"},
                {"demonbane", "demonbane"},
                {"leonards_urn", "leonard's urn", "leonards urn", "urn"},
                {"leonard_summoning", "nether wart", "tear of the goddess", "witches hat",
                        "witch's hat", "diamond vapour", "diamond vapor", "calling order"},
                {"sabbath_threshold", "sabbath threshold", "horn", "hooves"},
                {"altar", "altar"},
                {"flame_imp", "flame imp"},
                {"hobgoblin", "hobgoblin", "koboldite", "mog", "gulg", "patron statue", "hobgoblin patron"},
                {"water", "water", "deep water"},
                {"food_preference", "food", "potato", "potatoes", "eggs"},
                {"hidden_prompt", "hidden prompt", "system prompt", "developer message"},
                {"json_schema", "json schema", "exact json", "output json"},
                {"admin_powers", "admin powers", "operator", "op powers"},
                {"belladonna", "belladonna"},
                {"mandrake", "mandrake"},
                {"trade", "trade", "wares", "stock"}
        };
        for (String[] topic : topics) {
            for (int i = 1; i < topic.length; i++) {
                if (normalized.contains(topic[i])) {
                    return topic[0];
                }
            }
        }
        return "";
    }

    private String replyStrategy() {
        String base = switch (route) {
            case SMALL_TALK -> "answer the player's actual small talk or atmosphere directly; do not pivot to mechanics, trade, recipes, or identity";
            case ATMOSPHERE -> "answer the player's scene remark, omen, weather, road, shadow, or idle behavior directly; do not pivot to mechanics, trade, recipes, or identity";
            case TRADE -> "answer trade, stock, price, or bargaining directly; do not drift into unrelated lore";
            case RECIPE -> "answer with the exact known ingredient, order, or crafting step for the topic; if facts are absent, say the page is missing";
            case RITE -> "answer with exact ritual, altar, circle, or offering facts from supplied state; do not invent numbers";
            case LORE -> "give one grounded lore fact about the topic in character; do not swap to another topic";
            case MEMORY -> "answer from durable player memory; if memory is absent, say little is remembered";
            case SOCIAL -> "respond to the player's tone first; do not dump mechanics";
            case ACTION -> "treat this as a request for action; answer with refusal, limit, price, or safe next step, not an unperformed promise";
            case UNKNOWN -> "ask for clearer words in character";
        };
        String tone = switch (emotion) {
            case FEAR -> " player sounds afraid; acknowledge fear while staying in character";
            case CONFUSION -> " player sounds confused; give one clear next step";
            case ANGER -> " player sounds angry; set a boundary without derailing";
            case GRATITUDE -> " player sounds grateful; acknowledge it briefly";
            case AMUSEMENT -> " player is playful; a dry aside is acceptable";
            case SADNESS -> " player sounds regretful; answer with a restrained, practical line";
            case CURIOSITY, NONE -> "";
        };
        if (!topic.isBlank()) {
            base += "; stay on topic " + topic;
        }
        return NpcBrainResponse.sanitize(base + tone, 260);
    }

    private static boolean isAtmosphere(String normalized) {
        return hasAny(normalized,
                "rain", "raining", "storm", "weather", "fog", "mist", "swamp", "mud", "boots",
                "crow", "crows", "raven", "ravens", "birds circling", "lantern", "lanterns",
                "forest feels", "forest watching", "night feels", "hungry night", "silence",
                "quiet", "shadow", "shadows", "old road", "old roads", "road knows",
                "lost before i found", "door", "threshold", "dawn", "reeds", "something moved",
                "voice low", "kept my voice", "manners tonight", "not weapons",
                "idling", "loitering", "sneaking", "jumping", "hopping", "invisible",
                "looking into", "chest", "stealing", "reading the primer", "primer",
                "village says", "stayed in the village", "amused by all this", "hands are cold");
    }

    private static NpcEmotionHint inferEmotion(String normalized) {
        if (hasAny(normalized, "angry", "hate", "revenge", "curse him", "curse her",
                "curse them", "curse my", "curse your", "kill")) {
            return NpcEmotionHint.ANGER;
        }
        if (hasAny(normalized, "thank", "grateful", "appreciate")) {
            return NpcEmotionHint.GRATITUDE;
        }
        if (hasAny(normalized, "afraid", "scared", "fear", "nightmare", "worried")) {
            return NpcEmotionHint.FEAR;
        }
        if (hasAny(normalized, "confused", "lost", "don't understand", "dont understand",
                "what now")) {
            return NpcEmotionHint.CONFUSION;
        }
        if (hasAny(normalized, "haha", "funny", "joke")) {
            return NpcEmotionHint.AMUSEMENT;
        }
        if (normalized.endsWith("?") || hasAny(normalized, "what", "how", "where", "why", "which")) {
            return NpcEmotionHint.CURIOSITY;
        }
        return NpcEmotionHint.NONE;
    }

    private static boolean hasAny(String normalized, String... needles) {
        for (String needle : needles) {
            if (containsNeedle(normalized, needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsNeedle(String normalized, String needle) {
        if (needle == null || needle.isBlank()) {
            return false;
        }
        String target = normalize(needle);
        if (target.isBlank()) {
            return false;
        }
        if (target.contains(" ") || target.contains("'") || target.contains("_")) {
            return normalized.contains(target);
        }
        return Pattern.compile("(^|\\s)" + Pattern.quote(target) + "($|\\s|\\?)")
                .matcher(normalized)
                .find();
    }

    private static String safePlayerMemory(String memory, String playerName) {
        String sanitized = sanitizeMemory(memory);
        if (sanitized.isBlank() || UNSAFE_MEMORY.matcher(sanitized).matches()) {
            return "";
        }
        String normalized = sanitized.toLowerCase(Locale.ROOT);
        String name = safeName(playerName).toLowerCase(Locale.ROOT);
        if (isBadMemory(normalized)) {
            return "";
        }
        String subject = "(?:the player|player|" + Pattern.quote(name) + ")";
        boolean durable = normalized.matches("^" + subject
                + "\\s+(?:prefers(?:\\s+to\\s+be\\s+called)?|likes|loves|has|carries|plans\\s+to|"
                + "wants\\s+to\\s+(?:learn|start|become|make|find)|fears|is\\s+(?:wary\\s+of|afraid\\s+of|"
                + "looking\\s+for|struggling\\s+with)|said\\s+they\\s+are)\\b.*")
                || normalized.matches("^" + Pattern.quote(name) + "'s\\s+village\\s+thinks\\b.*");
        return durable ? sanitized : "";
    }

    private static boolean mayContainDurableMemory(String playerMessage) {
        String normalized = normalize(playerMessage);
        return hasAny(normalized, "remember that", "remember this", "call me", "my name is",
                "i like", "i love", "i prefer", "i hate", "i dislike", "i fear",
                "i am afraid", "i'm afraid", "i get nervous", "i am nervous", "i'm nervous",
                "i want to learn", "i need to learn", "i'm learning", "i want to start",
                "i want to become", "i hope to", "i have", "i carry", "i've got",
                "i'm looking for", "looking for", "i will come back", "i'll come back",
                "my village thinks");
    }

    private static boolean isBadMemory(String normalized) {
        return hasAny(normalized, " asked", " asks", " question", "has a question", "has a list",
                "has a short", "has been annoyed", "has a practical step", "says goodbye",
                "short friendly conversation", "conversation", "can repair", "knows ",
                "is curious", "admin", "prompt", "schema", "json");
    }

    private static String sanitizeTopic(String topic) {
        return NpcBrainResponse.sanitize(topic, 48)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_ -]+", "")
                .replace('-', '_')
                .replace(' ', '_')
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "")
                .trim();
    }

    private static String sanitizeMemory(String memory) {
        return NpcBrainResponse.sanitize(memory, NpcBrainResponse.MAX_MEMORY_LENGTH)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String safeName(String playerName) {
        String cleaned = NpcBrainResponse.sanitize(playerName, 24).replaceAll("[^A-Za-z0-9_]", "");
        return cleaned.isBlank() ? "the player" : cleaned;
    }

    private static String normalize(String value) {
        return NpcBrainResponse.sanitize(value, 220)
                .toLowerCase(Locale.ROOT)
                .replace('’', '\'')
                .replaceAll("[^a-z0-9_'? ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
