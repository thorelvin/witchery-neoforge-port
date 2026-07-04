package com.emoniph.witchery.ai;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class NpcVisibleBeat {
    private static final Pattern LEADING_ACTION = Pattern.compile("^\\*([^*]{1,64})\\*");

    private NpcVisibleBeat() {
    }

    static Optional<String> visibleBeat(String speech) {
        if (speech == null || speech.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = LEADING_ACTION.matcher(speech.trim());
        if (!matcher.find()) {
            return Optional.empty();
        }
        String stage = canonical(matcher.group(1));
        return stage.isBlank() || !isSafe(stage) ? Optional.empty() : Optional.of(stage);
    }

    static String canonical(String action) {
        String text = NpcSpeechSimilarity.normalizeForComparison(action);
        if (text.isBlank()) {
            return "";
        }
        if ((text.contains("hand") && !text.contains("spread"))
                || text.contains("give")
                || text.contains("take")
                || text.contains("throw")
                || text.contains("cast")
                || text.contains("drink")
                || text.contains("attack")
                || text.contains("open")
                || text.contains("summon")
                || text.contains("teleport")) {
            return "";
        }
        if (text.contains("eye") && (text.contains("narrow") || text.contains("flicker"))) {
            return "narrows her eyes";
        }
        if (text.contains("lean") && (text.contains("closer") || text.contains("in"))) {
            return "leans closer";
        }
        if (text.contains("lower") && text.contains("voice")) {
            return text.contains("his") ? "lowers his voice" : "lowers her voice";
        }
        if (text.contains("sigh")) {
            return "sighs";
        }
        if (text.contains("shrug")) {
            return "shrugs";
        }
        if (text.contains("yawn")) {
            return "yawns";
        }
        if (text.contains("raise") && (text.contains("brow") || text.contains("eyebrow"))) {
            return "raises an eyebrow";
        }
        if (text.contains("fold") && text.contains("arms")) {
            return "folds her arms";
        }
        if (text.contains("nod")) {
            return "nods";
        }
        if (text.contains("smile")) {
            return "smiles thinly";
        }
        if (text.contains("gesture")) {
            return "gestures";
        }
        if (text.contains("glance") && text.contains("road")) {
            return "glances at the road";
        }
        if (text.contains("touch") && text.contains("charm")) {
            return "touches a charm";
        }
        if (text.contains("coin") && (text.contains("study") || text.contains("examine") || text.contains("regard"))) {
            return "studies your coin";
        }
        if (text.contains("blade") && (text.contains("study") || text.contains("examine") || text.contains("regard"))) {
            return "studies your blade";
        }
        if (text.contains("study") || text.contains("examine") || text.contains("regard")) {
            return "studies you";
        }
        if (text.contains("tap")) {
            return "taps a finger";
        }
        if (text.contains("tilt") && text.contains("head")) {
            return text.contains("his") ? "tilts his head" : "tilts her head";
        }
        if (text.contains("turn") && text.contains("gaze")) {
            return "turns his gaze";
        }
        if (text.contains("spread") && text.contains("hand")) {
            return "spreads his hands";
        }
        if (text.contains("adjust") && text.contains("robe")) {
            return "adjusts her robe";
        }
        return switch (text) {
            case "narrows her eyes", "leans closer", "raises an eyebrow", "folds her arms", "nods",
                    "smiles thinly", "gestures", "studies your coin", "studies you", "taps a finger",
                    "tilts her head", "shrugs", "yawns", "lowers her voice", "sighs", "adjusts her robe",
                    "lowers his voice", "studies your blade", "tilts his head", "turns his gaze",
                    "spreads his hands", "glances at the road", "touches a charm" -> text;
            default -> "";
        };
    }

    static boolean isSafe(String action) {
        String normalized = NpcSpeechSimilarity.normalizeForComparison(action);
        return normalized.equals("narrows her eyes")
                || normalized.equals("leans closer")
                || normalized.equals("raises an eyebrow")
                || normalized.equals("folds her arms")
                || normalized.equals("nods")
                || normalized.equals("smiles thinly")
                || normalized.equals("gestures")
                || normalized.equals("glances at the road")
                || normalized.equals("touches a charm")
                || normalized.equals("studies your coin")
                || normalized.equals("studies your blade")
                || normalized.equals("studies you")
                || normalized.equals("taps a finger")
                || normalized.equals("tilts her head")
                || normalized.equals("tilts his head")
                || normalized.equals("adjusts her robe")
                || normalized.equals("turns his gaze")
                || normalized.equals("spreads his hands")
                || normalized.equals("shrugs")
                || normalized.equals("yawns")
                || normalized.equals("lowers her voice")
                || normalized.equals("lowers his voice")
                || normalized.equals("sighs");
    }
}
