package com.emoniph.witchery.ai;

import java.util.Locale;

final class NpcSpeechSimilarity {
    private NpcSpeechSimilarity() {
    }

    static String normalizeForComparison(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    static boolean sameMeaningForRepetition(String first, String second) {
        String a = normalizeForComparison(first);
        String b = normalizeForComparison(second);
        if (a.isBlank() || b.isBlank()) {
            return false;
        }
        return a.equals(b) || contentFingerprint(a).equals(contentFingerprint(b));
    }

    static String contentFingerprint(String normalized) {
        if (normalized == null) {
            return "";
        }
        return normalized
                .replaceAll("\\b(a|an|the|please|just|really|very|quite)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
