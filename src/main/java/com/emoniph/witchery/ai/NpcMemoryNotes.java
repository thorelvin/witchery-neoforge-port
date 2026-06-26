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
    private static final Pattern PLAYER_DISLIKES =
            Pattern.compile("(?i)\\bi\\s+(?:hate|dislike|fear)\\s+(.{2,70})");
    private static final Pattern PLAYER_LOOKING_FOR =
            Pattern.compile("(?i)\\b(?:i\\s+(?:need|want)|i(?:\\s+am|'m)\\s+looking\\s+for|looking\\s+for)\\s+(.{2,70})");
    private static final Pattern PLAYER_HAS =
            Pattern.compile("(?i)\\bi(?:\\s+have|\\s+carry|\\s+got|\\s+have\\s+got|'ve\\s+got)\\s+(.{2,70})");

    private NpcMemoryNotes() {
    }

    static Optional<String> extract(String playerName, String playerSpeech) {
        String speech = NpcBrainResponse.sanitize(playerSpeech, 180);
        if (speech.isBlank()) {
            return Optional.empty();
        }
        String name = safeName(playerName);
        return firstMatch(speech, PREFERRED_NAME, match -> name + " prefers to be called " + match.group(1) + ".")
                .or(() -> firstMatch(speech, PLAYER_ROLE, match -> name + " said they are a " + match.group(1).toLowerCase(Locale.ROOT) + "."))
                .or(() -> firstMatch(speech, PLAYER_LIKES, match -> name + " likes " + cleanFact(match.group(1)) + "."))
                .or(() -> firstMatch(speech, PLAYER_DISLIKES, match -> name + " is wary of " + cleanFact(match.group(1)) + "."))
                .or(() -> firstMatch(speech, PLAYER_LOOKING_FOR, match -> name + " is looking for " + cleanFact(match.group(1)) + "."))
                .or(() -> firstMatch(speech, PLAYER_HAS, match -> name + " has " + cleanFact(match.group(1)) + "."));
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
                .replaceAll("(?i)\\b(?:please|thanks|thank\\s+you)\\b", "")
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
                && !normalized.matches(".*<\\s*/?\\s*[a-z][a-z0-9_-]*\\b.*")
                && !normalized.contains("json")
                && !normalized.contains("api")
                && !normalized.contains("model");
    }

    private static String safeName(String playerName) {
        String cleaned = NpcBrainResponse.sanitize(playerName, 24).replaceAll("[^A-Za-z0-9_]", "");
        return cleaned.isBlank() ? "The player" : cleaned;
    }
}
