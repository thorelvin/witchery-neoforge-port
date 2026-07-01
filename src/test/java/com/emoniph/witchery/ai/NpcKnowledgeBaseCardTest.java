package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcKnowledgeBaseCardTest {
    private static final String INDEX = "/data/witchery/ai/cards/index.txt";
    private static final String CARD_DIR = "/data/witchery/ai/cards/";
    private static final String TARGETED_PREFIX = "- [triggers:";
    private static final String TARGETED_MARKER = "Targeted fact:";
    private static final int MAX_TARGETED_LINE_LENGTH = 520;
    private static final Pattern METADATA = Pattern.compile("\\[([a-zA-Z0-9_]+):\\s*([^\\]]+)]");
    private static final Set<String> KNOWN_METADATA = Set.of("triggers", "topic", "persona", "weight", "unlock", "tags");
    private static final Set<String> ACTIVE_PERSONAS = Set.of("any", "coven_witch", "leonard", "baba_yaga",
            "hobgoblin", "reflection");
    private static final Set<String> FUTURE_PERSONAS = Set.of("boss", "demon", "flame_imp", "villager");
    private static final List<String> STALE_PHRASES = List.of(
            "spends the stored brew",
            "Brew of Bodega",
            "medium circle, 2000 altar power per curse type"
    );

    @Test
    void indexedCardsExistAndTargetedFactsParse() {
        List<String> cardPaths = readIndex();
        assertFalse(cardPaths.isEmpty(), "Card index is empty.");

        for (String path : cardPaths) {
            String text = readRequired(CARD_DIR + path);
            assertFalse(text.isBlank(), path + " is empty.");
            assertTrue(text.contains("## "), path + " has no section headings.");
            String[] lines = text.split("\\R");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].strip();
                int lineNumber = i + 1;
                if (line.contains(TARGETED_MARKER)) {
                    assertTrue(line.startsWith(TARGETED_PREFIX),
                            path + ":" + lineNumber + " targeted fact must start with " + TARGETED_PREFIX);
                }
                if (!line.startsWith(TARGETED_PREFIX)) {
                    continue;
                }
                assertTrue(line.contains(TARGETED_MARKER),
                        path + ":" + lineNumber + " malformed targeted fact: " + line);
                assertTrue(line.length() <= MAX_TARGETED_LINE_LENGTH,
                        path + ":" + lineNumber + " targeted fact line is too long (" + line.length() + " > "
                                + MAX_TARGETED_LINE_LENGTH + "): " + line);
                assertKnownMetadata(line, path, lineNumber);
                assertNoStalePhrases(line, path, lineNumber);
            }
        }
    }

    private static void assertKnownMetadata(String line, String path, int lineNumber) {
        Matcher matcher = METADATA.matcher(line);
        boolean sawTriggers = false;
        while (matcher.find()) {
            String key = matcher.group(1).toLowerCase(Locale.ROOT);
            String value = matcher.group(2).strip();
            assertTrue(KNOWN_METADATA.contains(key),
                    path + ":" + lineNumber + " unknown metadata key [" + key + "]");
            assertFalse(value.isBlank(), path + ":" + lineNumber + " blank metadata value for [" + key + "]");
            if ("triggers".equals(key)) {
                sawTriggers = true;
            } else if ("weight".equals(key)) {
                assertWeight(value, path, lineNumber);
            } else if ("persona".equals(key)) {
                assertKnownPersona(value, path, lineNumber);
            } else if ("tags".equals(key)) {
                assertTags(value, path, lineNumber);
            }
        }
        assertTrue(sawTriggers, path + ":" + lineNumber + " targeted fact did not expose trigger metadata.");
    }

    private static void assertWeight(String value, String path, int lineNumber) {
        try {
            int weight = Integer.parseInt(value);
            assertTrue(weight >= 1 && weight <= 20,
                    path + ":" + lineNumber + " weight out of range 1..20: " + value);
        } catch (NumberFormatException ex) {
            throw new AssertionError(path + ":" + lineNumber + " weight is not an integer: " + value, ex);
        }
    }

    private static void assertKnownPersona(String value, String path, int lineNumber) {
        String persona = NpcCardRequest.normalizePersona(value);
        assertTrue(ACTIVE_PERSONAS.contains(persona) || FUTURE_PERSONAS.contains(persona),
                path + ":" + lineNumber + " persona is neither registered nor marked as future: " + value);
    }

    private static void assertTags(String value, String path, int lineNumber) {
        for (String tag : value.split("[,|]")) {
            assertFalse(tag.strip().isBlank(),
                    path + ":" + lineNumber + " contains a blank tag in [tags: " + value + "]");
        }
    }

    private static void assertNoStalePhrases(String line, String path, int lineNumber) {
        String normalized = line.toLowerCase(Locale.ROOT);
        for (String phrase : STALE_PHRASES) {
            assertTrue(!normalized.contains(phrase.toLowerCase(Locale.ROOT)),
                    path + ":" + lineNumber + " contains stale phrase: " + phrase);
        }
    }

    private static List<String> readIndex() {
        return readRequired(INDEX).lines()
                .map(String::strip)
                .filter(line -> !line.isBlank())
                .filter(line -> !line.startsWith("#"))
                .toList();
    }

    private static String readRequired(String resource) {
        try (InputStream stream = NpcKnowledgeBaseCardTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, "Missing resource " + resource);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new AssertionError("Could not read resource " + resource, ex);
        }
    }
}
