package com.emoniph.witchery.ai;

final class NpcFallbackAsides {
    private NpcFallbackAsides() {
    }

    static String rotatingLine(String normalized, String[] lines) {
        if (lines.length == 0) {
            return "";
        }
        return lines[Math.floorMod(normalized.hashCode(), lines.length)];
    }

    static String oldWitch(String normalized, String playerName) {
        return oldWitch(normalized, playerName, "");
    }

    static String oldWitch(String normalized, String playerName, String avoidSpeech) {
        String[] lines = {
                "*leans closer* \"Speak louder, " + playerName + "; these old ears hear spirits better than mumbling.\"",
                "May your boots miss graves and your pockets find honest coin.",
                "The kettle says three bubbles and a cracked moon; make of that what you can.",
                "*shrugs* \"A crow once told me the same thing, or maybe it was a turnip.\"",
                "Mind your shadow after midnight; it learns bad habits.",
                "If the moon answers first, pretend you did not hear it.",
                "*narrows her eyes* \"Never trust a quiet jar; silence ferments.\"",
                "Ash at the hearth, jars in a row, and questions where I can see them.",
                "A good omen arrives late and complains about the road.",
                "If you mean trouble, stand downwind so I can smell it coming.",
                "Some questions need rainwater. Some need teeth.",
                "Keep one pocket for coin and one for consequences.",
                "The old paths open for patience, not stomping.",
                "*yawns* \"I have seen braver faces on boiled cabbage, but yours may yet improve.\"",
                "Ask again with fewer elbows in the sentence."
        };
        int start = Math.floorMod(normalized.hashCode(), lines.length);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[(start + i) % lines.length];
            if (avoidSpeech == null || avoidSpeech.isBlank()
                    || !NpcBrainResponse.sameMeaningForRepetition(line, avoidSpeech)) {
                return line;
            }
        }
        return lines[start];
    }

    static String villager(String normalized, String playerName, String avoidSpeech) {
        String[] lines = {
                "Keep your voice low, " + playerName + "; walls have ears when witches visit.",
                "Ask at the bell if you want panic. Ask me if you want gossip.",
                "Road dust, bad moon, strange visitor. Villages learn to count all three.",
                "If you mean trouble, stand where the golem can see you.",
                "Close doors at dusk. It is boring advice because it works.",
                "Salt by the threshold, bread in your pocket, and no promises after sunset.",
                "*shrugs* \"I have heard worse questions from better-dressed fools.\"",
                "*touches a charm* \"My gran said odd questions come before odd weather.\"",
                "Good witches leave cures and fair trades. Bad ones leave bottles with your name in them.",
                "Harvest first, heroics later. Empty bellies make poor guards."
        };
        return firstNonRepeated(lines, normalized, avoidSpeech,
                "Ask again, " + playerName + "; make it useful this time.");
    }

    static String leonard(String normalized, String playerName, String avoidSpeech) {
        String[] lines = {
                "Speak plainly, " + playerName + ". Even debts tire of weak mouths.",
                "Careful. Not every silence is empty; some are measuring you.",
                "Your courage has a smell. I have not decided whether it is rot or ripeness.",
                "A thin question. Bring a sharper one before I lose patience.",
                "The crossroads heard you. It found you small."
        };
        return firstNonRepeated(lines, normalized, avoidSpeech,
                "Ask again, " + playerName + "; this time bring a sharper question.");
    }

    static String reflection(String normalized, String playerName, String avoidSpeech) {
        String[] lines = {
                "Your echo is late, " + playerName + ". I arrived in its place.",
                "*copies your stance* \"Ask again. This side of the glass likes clean shapes.\"",
                "The room heard you. It answered with your outline.",
                "Your face looks braver from over here. That will not last.",
                "*smiles with your mouth* \"The mirror keeps poor questions too.\"",
                "Speak plainly, " + playerName + ". Reflections blur when words do.",
                "The silver is listening. Try not to sound breakable."
        };
        return firstNonRepeated(lines, normalized, avoidSpeech,
                "Ask again, " + playerName + "; the glass missed the useful part.");
    }

    static String babaYaga(String normalized, String playerName, String avoidSpeech) {
        String[] lines = {
                "*sniffs* \"You brought words, " + playerName + ". I was hoping for bones.\"",
                "Careful, " + playerName + ". Old pots remember careless mouths.",
                "Manners first. Then perhaps I do not put you in the thin soup.",
                "*taps her broom* \"Ask with teeth, or hush before mine answer.\"",
                "The forest has kinder old women. It did not send you to one."
        };
        return firstNonRepeated(lines, normalized, avoidSpeech,
                "Ask again, " + playerName + "; season the question with sense.");
    }

    static String hobgoblin(String normalized, String playerName, String avoidSpeech) {
        String[] lines = {
                "*shrugs* \"Big words. Small ears. Try shiny words.\"",
                "*gestures* \"Say trade, mine, koboldite, hut, Mog, or Gulg.\"",
                "Hobgoblin hears noise. Pockets maybe louder?",
                "*taps a finger* \"Question too slippery. Hold it still.\"",
                "No bite in that talk, " + playerName + ". Try useful."
        };
        return firstNonRepeated(lines, normalized, avoidSpeech,
                "Ask again, " + playerName + ". Small words. Shiny words.");
    }

    private static String firstNonRepeated(String[] lines, String normalized, String avoidSpeech, String fallback) {
        int start = Math.floorMod(normalized.hashCode(), lines.length);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[(start + i) % lines.length];
            if (!NpcBrainResponse.sameMeaningForRepetition(line, avoidSpeech)) {
                return line;
            }
        }
        return fallback;
    }
}
