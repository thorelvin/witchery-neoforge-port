package com.emoniph.witchery.ai;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class NpcBrainResponseParser {
    private static final Pattern SPEECH_FIELD =
            Pattern.compile("\"speech\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.CASE_INSENSITIVE);

    private NpcBrainResponseParser() {
    }

    static Optional<NpcBrainResponse> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String json = extractJsonObject(raw);
        if (json.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                return Optional.empty();
            }
            JsonObject object = parsed.getAsJsonObject();
            String speech = stringValue(object, "speech");
            if (speech.isBlank()) {
                return Optional.empty();
            }
            Optional<NpcIntent> intent = NpcIntent.parse(stringValue(object, "intent"));
            if (intent.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new NpcBrainResponse(speech, intent.get(), stringValue(object, "memory")));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    static Optional<NpcBrainResponse> parseLenient(String raw) {
        Optional<NpcBrainResponse> parsed = parse(raw);
        if (parsed.isPresent()) {
            return parsed;
        }
        String speech = extractPlainSpeech(raw);
        if (speech.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new NpcBrainResponse(speech, NpcIntent.NONE, ""));
    }

    private static String extractJsonObject(String raw) {
        String text = raw.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end < start) {
            return "";
        }
        return text.substring(start, end + 1);
    }

    private static String extractPlainSpeech(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String cleaned = raw.trim()
                .replaceAll("(?is)^```(?:json)?\\s*", "")
                .replaceAll("(?is)\\s*```$", "")
                .trim();
        if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
            return "";
        }
        Matcher speech = SPEECH_FIELD.matcher(cleaned);
        if (speech.find()) {
            return unescapeJsonString(speech.group(1));
        }
        String plain = cleaned.replaceAll("(?i)^speech\\s*[:=]\\s*", "")
                .replaceAll("(?i)^assistant\\s*[:=]\\s*", "")
                .trim();
        if (plain.length() >= 2 && plain.startsWith("\"") && plain.endsWith("\"")) {
            return unescapeJsonString(plain.substring(1, plain.length() - 1)).trim();
        }
        return plain.replaceAll("^\"|\"$", "").trim();
    }

    private static String unescapeJsonString(String value) {
        try {
            return JsonParser.parseString("\"" + value + "\"").getAsString();
        } catch (RuntimeException ex) {
            return value;
        }
    }

    private static String stringValue(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }
}
