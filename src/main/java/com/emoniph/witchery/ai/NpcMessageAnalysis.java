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
        if (route == NpcDialogueRoute.UNKNOWN) {
            route = deterministic.route();
        }

        String topic = valueFor(speech, "topic");
        if (topic.isBlank()) {
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
        if (remember && !memory.isBlank()) {
            builder.append("; durable player memory=").append(memory);
        }
        builder.append('.');
        return NpcBrainResponse.sanitize(builder.toString(), 360);
    }

    static String appendContext(String baseContext, NpcMessageAnalysis analysis) {
        String base = NpcBrainResponse.sanitize(baseContext, 1400);
        String line = analysis == null ? "" : analysis.contextLine();
        if (line.isBlank()) {
            return base;
        }
        return base.isBlank() ? line : NpcBrainResponse.sanitize(base + " " + line, 1600);
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
                "how to make")) {
            return NpcDialogueRoute.RECIPE;
        }
        if (hasAny(normalized, "rite", "ritual", "circle", "chalk", "altar", "offering",
                "summon", "binding", "familiar", "storm")) {
            return NpcDialogueRoute.RITE;
        }
        if (hasAny(normalized, "can you", "could you", "will you", "please", "give me",
                "follow me", "come with", "bless", "curse", "fight", "open")) {
            return NpcDialogueRoute.ACTION;
        }
        if (hasAny(normalized, "spirit world", "demon", "imp", "hobgoblin", "vampire",
                "werewolf", "taglock", "poppet", "coven", "witchery", "mandrake",
                "belladonna", "witches salt", "witch's salt")) {
            return NpcDialogueRoute.LORE;
        }
        if (hasAny(normalized, "thank", "sorry", "apolog", "you are kind", "you are wise",
                "that helped")) {
            return NpcDialogueRoute.SOCIAL;
        }
        return NpcDialogueRoute.SMALL_TALK;
    }

    private static String inferTopic(String normalized) {
        if (normalized.isBlank()) {
            return "";
        }
        String[][] topics = {
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
                {"flame_imp", "flame imp"},
                {"hobgoblin", "hobgoblin"},
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
