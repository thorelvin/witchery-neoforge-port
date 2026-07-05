package com.emoniph.witchery.ai;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class NpcSpeechNarrationNormalizer {
    private static final Pattern QUOTED_SPEECH =
            Pattern.compile("[\"\\u201c]([^\"\\u201d]{2,220})[\"\\u201d]");
    private static final Pattern SAFE_ACTION_ONLY =
            Pattern.compile("^\\*([a-z][a-z0-9 ',;-]{1,56})\\*$");
    private static final Pattern STARRED_ACTION_PREFIX =
            Pattern.compile("^\\*([^*]{1,64})\\*\\s*(.{2,220})$");

    private NpcSpeechNarrationNormalizer() {
    }

    static String normalizeNarratedDialogue(String speech, String npcName) {
        if (speech == null || speech.isBlank()) {
            return "";
        }
        Optional<String> starred = normalizeStarredActionDialogue(speech);
        if (starred.isPresent()) {
            return starred.get();
        }
        if (speech.contains("*")) {
            return speech;
        }
        Optional<String> namedAction = normalizeBareNamedAction(speech, npcName);
        if (namedAction.isPresent()) {
            return namedAction.get();
        }
        Matcher matcher = QUOTED_SPEECH.matcher(speech);
        if (matcher.find()) {
            String quote = matcher.group(1).trim();
            String stage = stageDirection(speech.substring(0, matcher.start()), npcName);
            if (!stage.isBlank()) {
                return "*" + stage + "* \"" + quote + "\"";
            }
            return quote;
        }
        String stripped = stripNarrationSpeakerPrefix(speech, npcName);
        return stripped.isBlank() ? speech : stripped;
    }

    private static Optional<String> normalizeBareNamedAction(String speech, String npcName) {
        if (speech == null || speech.isBlank() || npcName == null || npcName.isBlank()) {
            return Optional.empty();
        }
        String cleaned = speech.trim();
        String name = Pattern.quote(npcName.trim());
        String firstName = npcName.trim().contains(" ")
                ? Pattern.quote(npcName.trim().substring(0, npcName.trim().indexOf(' ')))
                : "";
        String names = firstName.isBlank() ? name : name + "|" + firstName;
        String action = cleaned.replaceFirst("(?i)^\\s*(?:" + names + ")\\s+", "").trim();
        if (action.equals(cleaned)) {
            return Optional.empty();
        }
        String normalizedAction = NpcSpeechSimilarity.normalizeForComparison(action);
        if (!isBareSafeActionCandidate(normalizedAction)) {
            return Optional.empty();
        }
        String stage = NpcVisibleBeat.canonical(action);
        return stage.isBlank() ? Optional.empty() : Optional.of("*" + stage + "*");
    }

    private static boolean isBareSafeActionCandidate(String normalizedAction) {
        return normalizedAction.startsWith("nod")
                || normalizedAction.startsWith("eye")
                || normalizedAction.startsWith("narrow")
                || normalizedAction.startsWith("lean")
                || normalizedAction.startsWith("lower")
                || normalizedAction.startsWith("sigh")
                || normalizedAction.startsWith("shrug")
                || normalizedAction.startsWith("yawn")
                || normalizedAction.startsWith("gesture");
    }

    private static Optional<String> normalizeStarredActionDialogue(String speech) {
        Matcher matcher = STARRED_ACTION_PREFIX.matcher(speech);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String stage = NpcVisibleBeat.canonical(matcher.group(1));
        if (stage.isBlank()) {
            return Optional.of(speech);
        }
        String spoken = extractSpokenAfterBeat(matcher.group(2));
        if (spoken.isBlank()) {
            return Optional.of(speech);
        }
        return Optional.of("*" + stage + "* \"" + spoken + "\"");
    }

    static String stripDanglingTrailingAsterisk(String speech) {
        String text = speech == null ? "" : speech.stripTrailing();
        if (!text.endsWith("*")) {
            return speech == null ? "" : speech;
        }
        long starCount = text.chars().filter(ch -> ch == '*').count();
        if (starCount % 2 == 1) {
            return text.substring(0, text.length() - 1).stripTrailing();
        }
        return speech;
    }

    static Optional<String> normalizeSafeActionOnly(String speech) {
        Matcher matcher = SAFE_ACTION_ONLY.matcher(speech == null ? "" : speech.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String stage = NpcVisibleBeat.canonical(matcher.group(1));
        return stage.isBlank() ? Optional.empty() : Optional.of("*" + stage + "*");
    }

    static boolean isSafeActionOnly(String speech) {
        Matcher matcher = SAFE_ACTION_ONLY.matcher(speech == null ? "" : speech.trim());
        return matcher.matches() && NpcVisibleBeat.isSafe(matcher.group(1));
    }

    private static String extractSpokenAfterBeat(String rest) {
        String cleaned = rest == null ? "" : rest.trim();
        if (startsWithDoubleQuote(cleaned)) {
            int end = Math.max(cleaned.lastIndexOf('"'), cleaned.lastIndexOf('\u201d'));
            return end > 0 ? cleaned.substring(1, end).trim() : cleaned.substring(1).trim();
        }
        if (startsWithSingleQuote(cleaned)) {
            int end = closingSingleQuote(cleaned);
            return end > 0 ? cleaned.substring(1, end).trim() : cleaned.substring(1).trim();
        }
        cleaned = cleaned.replaceFirst("\\s*\\*[^*]{1,64}\\*\\s*$", "").trim();
        cleaned = stripSafeInlineActions(cleaned);
        if (cleaned.contains("*")) {
            return "";
        }
        return cleaned.replaceAll("^[:\\-\\s]+", "")
                .replaceAll("^[\"'\\u2018\\u201c]|[\"'\\u2019\\u201d]$", "")
                .trim();
    }

    private static String stripSafeInlineActions(String text) {
        Matcher matcher = Pattern.compile("\\*([^*]{1,64})\\*").matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String stage = NpcVisibleBeat.canonical(matcher.group(1));
            if (stage.isBlank()) {
                return text;
            }
            matcher.appendReplacement(result, " ");
        }
        matcher.appendTail(result);
        return result.toString().replaceAll("\\s+", " ").trim();
    }

    private static boolean startsWithDoubleQuote(String text) {
        return !text.isBlank() && (text.charAt(0) == '"' || text.charAt(0) == '\u201c');
    }

    private static boolean startsWithSingleQuote(String text) {
        return !text.isBlank() && (text.charAt(0) == '\'' || text.charAt(0) == '\u2018');
    }

    private static int closingSingleQuote(String text) {
        for (int i = 1; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch != '\'' && ch != '\u2019') {
                continue;
            }
            String tail = text.substring(i + 1).stripLeading();
            char previous = text.charAt(i - 1);
            if (tail.isEmpty()
                    || tail.startsWith("*")
                    || (isTerminalPunctuation(previous) && Character.isUpperCase(tail.codePointAt(0)))) {
                return i;
            }
        }
        return -1;
    }

    private static String stripNarrationSpeakerPrefix(String speech, String npcName) {
        String cleaned = speech.trim();
        if (npcName != null && !npcName.isBlank()) {
            String name = Pattern.quote(npcName.trim());
            String firstName = npcName.trim().contains(" ")
                    ? Pattern.quote(npcName.trim().substring(0, npcName.trim().indexOf(' ')))
                    : "";
            String names = firstName.isBlank() ? name : name + "|" + firstName;
            cleaned = cleaned.replaceFirst("(?i)^\\s*(?:" + names + ")\\s*[:\\-]\\s*", "");
            cleaned = cleaned.replaceFirst("(?i)^\\s*(?:" + names + ")\\s+"
                    + "(?:says|said|speaks|replies|responds|answers|mutters|whispers)\\s*[:,]\\s*", "");
        }
        cleaned = cleaned.replaceFirst("(?i)^\\s*(?:the\\s+)?(?:coven\\s+)?witch\\s+"
                + "(?:says|said|speaks|replies|responds|answers|mutters|whispers)\\s*[:,]\\s*", "");
        return cleaned.trim();
    }

    private static String stageDirection(String prefix, String npcName) {
        String text = NpcSpeechSimilarity.normalizeForComparison(stripNarrationSpeakerPrefix(prefix, npcName));
        if (text.isBlank()
                || text.contains("sun")
                || text.contains("moon")
                || text.contains("window")
                || text.contains("horizon")
                || text.contains("shop")
                || text.contains("floor")
                || text.contains("spirit world")
                || text.contains("necromancy")) {
            return "";
        }
        return NpcVisibleBeat.canonical(text);
    }

    private static boolean isTerminalPunctuation(char ch) {
        return ch == '.' || ch == '!' || ch == '?' || ch == '"' || ch == '\u201d' || ch == '\'';
    }
}
