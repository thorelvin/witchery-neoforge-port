package com.emoniph.witchery.ai;

final class NpcTextSanitizer {
    private NpcTextSanitizer() {
    }

    static String sanitize(String raw, int maxLength) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.replace("\\n", " ")
                .replace("\\r", " ")
                .replace("\\t", " ")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        cleaned = cleaned.replaceAll("\"{2,}$", "\"");
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        if (maxLength <= 3) {
            return cleaned.substring(0, maxLength);
        }
        return cleaned.substring(0, maxLength - 3).stripTrailing() + "...";
    }
}
