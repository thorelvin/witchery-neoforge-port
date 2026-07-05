package com.emoniph.witchery.ai;

import java.util.Locale;

final class NpcPersonaSpeechGuards {
    private NpcPersonaSpeechGuards() {
    }

    static boolean rejectsNarrationOnly(String speech) {
        String normalized = normalize(speech);
        return normalized.startsWith("as you depart")
                || normalized.startsWith("you see ")
                || normalized.startsWith("you feel ")
                || normalized.startsWith("her words ")
                || normalized.startsWith("his words ")
                || normalized.startsWith("the old witch s warning")
                || normalized.startsWith("the air is ")
                || normalized.startsWith("the air grows ")
                || normalized.startsWith("the darkness ")
                || normalized.startsWith("the night grows ")
                || normalized.startsWith("the night s embrace ")
                || normalized.startsWith("nightfall descends ")
                || normalized.startsWith("the moonlight ")
                || normalized.startsWith("the shadowy man ")
                || normalized.startsWith("the shadowman ")
                || normalized.startsWith("in the shadowed realms ")
                || normalized.startsWith("the flame imp offers")
                || normalized.startsWith("the village s fate")
                || normalized.startsWith("the queen of night")
                || normalized.contains("echoes in your ears")
                || normalized.contains("voice echoes through the night")
                || normalized.contains("i ask in a firm voice")
                || normalized.contains("eyes never leaving dev")
                || normalized.contains("shadows lengthening")
                || normalized.contains("you stand alone amidst")
                || normalized.contains("tread through shadows")
                || normalized.contains("prepare to bid farewell")
                || normalized.contains("offers a final warning")
                || normalized.contains("hangs in the balance")
                || normalized.contains("your actions echo")
                || normalized.contains("mirror world guardian is displeased");
    }

    static boolean rejectsSoftCompanion(String speech) {
        String normalized = normalize(speech);
        return containsAny(normalized,
                "my friend",
                "friend",
                "old friend",
                "dear friend",
                "humble servant",
                "faithful little",
                "always be here for you",
                "i will always be here",
                "friendship",
                "potential alliance",
                "shared past",
                "comfort from your kind words",
                "comfort from my words",
                "you are welcome little one",
                "my dear little one",
                "your servant",
                "serve and protect",
                "we shall face it together",
                "ready to serve",
                "serve and satisfy",
                "sole purpose is to provide",
                "warm embrace",
                "you may leave in peace",
                "leave you alone in peace",
                "farewell my friend",
                "what can i do for you");
    }

    static boolean rejectsUnsupportedProtection(String speech, String playerMessage) {
        String player = normalize(playerMessage);
        if (!containsAny(player, "protect", "safe", "trust")) {
            return false;
        }
        String normalized = normalize(speech);
        return containsAny(normalized,
                "protect yourself from nightmares",
                "i will protect",
                "i can protect",
                "i will look out for you",
                "we ve got protection",
                "you can hide behind me",
                "always be here for you",
                "safe ride");
    }

    static boolean rejectsPlayerSpeakerInversion(String speech) {
        String normalized = normalize(speech);
        return containsAny(normalized,
                "i am dev",
                "i m dev",
                "i ll be calling you",
                "i will be calling you",
                "the wizard dev");
    }

    static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
