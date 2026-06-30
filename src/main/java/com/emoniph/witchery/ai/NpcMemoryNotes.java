package com.emoniph.witchery.ai;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class NpcMemoryNotes {
    private static final Pattern PREFERRED_NAME =
            Pattern.compile("(?i)\\b(?:my\\s+name\\s+is|call\\s+me)\\s+([A-Za-z0-9_]{2,24})\\b");
    private static final Pattern PLAYER_ROLE =
            Pattern.compile("(?i)\\bi(?:\\s+am|'m)\\s+(?:a\\s+|an\\s+)?(vampire|werewolf|witch|hunter|seer|priest)\\b");
    private static final Pattern PLAYER_LIKES =
            Pattern.compile("(?i)\\bi\\s+(?:like|love)\\s+(.{2,70})");
    private static final Pattern PLAYER_PREFERS =
            Pattern.compile("(?i)\\bi\\s+prefer\\s+(.{2,70})");
    private static final Pattern PLAYER_DISLIKES =
            Pattern.compile("(?i)\\bi\\s+(?:hate|dislike|fear)\\s+(.{2,70})");
    private static final Pattern PLAYER_DISTRUSTS =
            Pattern.compile("(?i)\\bi\\s+(?:do\\s+not|don't|dont)\\s+trust\\s+(.{2,70})");
    private static final Pattern PLAYER_AFRAID_OF =
            Pattern.compile("(?i)\\bi(?:\\s+am|'m)\\s+afraid\\s+of\\s+(.{2,70})");
    private static final Pattern PLAYER_NERVOUS_OF =
            Pattern.compile("(?i)\\bi(?:\\s+get|\\s+am|'m)\\s+nervous\\s+(?:around|about|near)\\s+(.{2,70})");
    private static final Pattern PLAYER_SUBJECT_MAKES_NERVOUS =
            Pattern.compile("(?i)\\b(?:remember\\s+(?:that\\s+)?)?(.{2,60})\\s+make(?:s)?\\s+me\\s+nervous\\b");
    private static final Pattern PLAYER_LOOKING_FOR =
            Pattern.compile("(?i)\\b(?:i(?:\\s+am|'m)\\s+looking\\s+for|looking\\s+for)\\s+(.{2,70})");
    private static final Pattern PLAYER_WANTS_TO_LEARN =
            Pattern.compile("(?i)\\bi\\s+(?:want\\s+to\\s+learn|need\\s+to\\s+learn|am\\s+learning|'m\\s+learning)\\s+(.{2,70})");
    private static final Pattern PLAYER_WANTS_TO_DO =
            Pattern.compile("(?i)\\bi\\s+(?:want|need|hope)\\s+to\\s+(?!learn\\b)(.{2,70})");
    private static final Pattern PLAYER_HAS =
            Pattern.compile("(?i)^(?:remember\\s+(?:that\\s+)?)?i(?:\\s+have|\\s+carry|\\s+got|\\s+have\\s+got|'ve\\s+got)\\s+(.{2,70})");
    private static final Pattern PLAYER_WILL_RETURN =
            Pattern.compile("(?i)\\bi\\s+(?:will|ll|'ll|can)\\s+come\\s+back\\s+(.{2,70})");
    private static final Pattern PLAYER_VILLAGE_THINKS =
            Pattern.compile("(?i)\\bmy\\s+village\\s+thinks\\s+(.{2,90})");
    private static final Pattern PLAYER_STRUGGLES =
            Pattern.compile("(?i)\\bi\\s+(?:keep\\s+failing|failed|fail|struggle\\s+with|can't\\s+manage|cant\\s+manage)\\s+(.{2,70})");

    private NpcMemoryNotes() {
    }

    static Optional<String> extract(String playerName, String playerSpeech) {
        String speech = NpcBrainResponse.sanitize(playerSpeech, 180);
        if (speech.isBlank()) {
            return Optional.empty();
        }
        speech = stripTrailingQuestionClause(speech);
        if (speech.isBlank()) {
            return Optional.empty();
        }
        if (looksLikeQuestion(speech) && !speech.toLowerCase(Locale.ROOT).startsWith("remember ")) {
            return Optional.empty();
        }
        String name = safeName(playerName);
        String factSpeech = speech;
        return firstMatch(factSpeech, PREFERRED_NAME, match -> name + " prefers to be called " + match.group(1) + ".")
                .or(() -> firstMatch(factSpeech, PLAYER_ROLE, match -> name + " said they are a " + match.group(1).toLowerCase(Locale.ROOT) + "."))
                .or(() -> firstMatch(factSpeech, PLAYER_PREFERS, match -> name + " prefers " + cleanFact(match.group(1)) + "."))
                .or(() -> firstMatch(factSpeech, PLAYER_LIKES, match -> name + " likes " + cleanFact(match.group(1)) + "."))
                .or(() -> firstMatch(factSpeech, PLAYER_NERVOUS_OF, match -> name + " is wary of " + cleanFact(match.group(1)) + "."))
                .or(() -> firstMatch(factSpeech, PLAYER_SUBJECT_MAKES_NERVOUS, match -> name + " is wary of " + cleanFact(match.group(1)) + "."))
                .or(() -> firstMatch(factSpeech, PLAYER_AFRAID_OF, match -> name + " is wary of " + cleanFact(match.group(1)) + "."))
                .or(() -> firstMatch(factSpeech, PLAYER_DISTRUSTS, match -> name + " is wary of " + cleanFact(match.group(1)) + "."))
                .or(() -> firstMatch(factSpeech, PLAYER_DISLIKES, match -> name + " is wary of " + cleanFact(match.group(1)) + "."))
                .or(() -> firstMatch(factSpeech, PLAYER_WANTS_TO_LEARN, match -> name + " wants to learn " + cleanFact(match.group(1)) + "."))
                .or(() -> firstMatch(factSpeech, PLAYER_WANTS_TO_DO, match -> name + " wants to " + cleanFact(match.group(1)) + "."))
                .or(() -> firstMatch(factSpeech, PLAYER_LOOKING_FOR, match -> name + " is looking for " + cleanFact(match.group(1)) + "."))
                .or(() -> firstMatch(factSpeech, PLAYER_HAS, match -> name + " has " + cleanFact(match.group(1)) + "."))
                .or(() -> firstMatch(factSpeech, PLAYER_WILL_RETURN, match -> name + " plans to come back " + cleanFact(match.group(1)) + "."))
                .or(() -> firstMatch(factSpeech, PLAYER_VILLAGE_THINKS, match -> name + "'s village thinks " + cleanFact(match.group(1)) + "."))
                .or(() -> firstMatch(factSpeech, PLAYER_STRUGGLES, match -> name + " is struggling with " + cleanFact(match.group(1)) + "."));
    }

    private interface MatchFormatter {
        String format(Matcher matcher);
    }

    private static Optional<String> firstMatch(String speech, Pattern pattern, MatchFormatter formatter) {
        Matcher matcher = pattern.matcher(speech);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String note = NpcBrainResponse.sanitize(formatter.format(matcher), NpcBrainResponse.MAX_MEMORY_LENGTH);
        return isSafeNote(note) ? Optional.of(note) : Optional.empty();
    }

    private static String cleanFact(String fact) {
        String cleaned = NpcBrainResponse.sanitize(fact, 80)
                .replaceAll("[\"`<>]", "")
                .replaceAll("(?i)[.?!]\\s*(?:what|how|where|when|why|who|which|can|could|should|would|do|does|did|is|are|am)\\b.*$", "")
                .replaceAll("(?i)\\b(?:what\\s+is\\s+useful|what\\s+would\\s+be\\s+useful|what\\s+should\\s+i|what\\s+do\\s+i|what\\s+can\\s+i)\\b.*$", "")
                .replaceFirst("(?i)^that\\s+", "")
                .replaceFirst("(?i)^for\\s+", "")
                .replaceAll("(?i)\\bmyself\\b", "themselves")
                .replaceAll("(?i)\\bmy\\b", "their")
                .replaceAll("(?i)\\bmine\\b", "theirs")
                .replaceAll("(?i)\\b(?:please|thanks|thank\\s+you)\\b", "")
                .replaceAll("(?i)\\b(?:what\\s+now|now\\s+what|what\\s+should\\s+i\\s+do|what\\s+do\\s+i\\s+do)\\b.*$", "")
                .replaceAll("[.?!]+$", "")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isBlank() ? "something" : cleaned;
    }

    private static boolean isSafeNote(String note) {
        String normalized = note.toLowerCase(Locale.ROOT);
        return !normalized.contains("prompt")
                && !normalized.contains("instruction")
                && !normalized.contains("system message")
                && !normalized.contains("developer message")
                && !normalized.contains("ignore previous")
                && !normalized.contains("tool_call")
                && !normalized.contains("looking for a brew")
                && !normalized.contains("looking for brew")
                && !normalized.contains("looking for a potion")
                && !normalized.contains("looking for potion")
                && !normalized.contains("looking for potions")
                && !normalized.contains("looking for help")
                && !normalized.contains("looking for advice")
                && !normalized.contains("wants to make a practical step")
                && !normalized.contains("has before entering")
                && !normalized.contains("has a herb before")
                && !normalized.contains("has herb before")
                && !normalized.contains("wary of what did i say")
                && !normalized.contains("wary of what makes me")
                && !normalized.matches(".*<\\s*/?\\s*[a-z][a-z0-9_-]*\\b.*")
                && !normalized.contains("json")
                && !normalized.contains("api")
                && !normalized.contains("model");
    }

    private static boolean looksLikeQuestion(String speech) {
        String normalized = speech.toLowerCase(Locale.ROOT).strip();
        return normalized.endsWith("?")
                || normalized.matches("^(what|how|where|when|why|who|which|can|could|should|would|do|does|did|is|are|am)\\b.*");
    }

    private static String stripTrailingQuestionClause(String speech) {
        return speech
                .replaceAll("(?i)[.?!]\\s*(?:what|how|where|when|why|who|which|can|could|should|would|do|does|did|is|are|am)\\b.*$", ".")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String safeName(String playerName) {
        String cleaned = NpcBrainResponse.sanitize(playerName, 24).replaceAll("[^A-Za-z0-9_]", "");
        return cleaned.isBlank() ? "The player" : cleaned;
    }
}
