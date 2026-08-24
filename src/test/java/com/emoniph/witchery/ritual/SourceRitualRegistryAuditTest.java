package com.emoniph.witchery.ritual;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks the exact 96-row source registry and its current modern dispatch ownership audit. */
class SourceRitualRegistryAuditTest {

    private static final String FIXTURE = "/witchery/ritual/source_ritual_registrations.json";
    private static final String SOURCE_SHA256 =
            "2e25c794439becf119691af254e3bb5cc666a42961e27767800668a706a82ca0";
    private static final Pattern SOURCE_CIRCLE = Pattern.compile(
            "new Circle\\((\\d+), (\\d+), (\\d+)\\)");

    @Test
    void sourceFixtureIsCompleteOrderedAndTraceable() {
        JsonObject root = fixture();
        assertEquals(96, root.get("registrationCount").getAsInt());
        assertEquals(SOURCE_SHA256, root.get("sourceSha256").getAsString());

        JsonArray registrations = root.getAsJsonArray("registrations");
        assertEquals(96, registrations.size());
        int previousLine = 0;
        Set<Integer> bookIndexes = new HashSet<>();
        for (int index = 0; index < registrations.size(); index++) {
            JsonObject row = registrations.get(index).getAsJsonObject();
            assertEquals(index + 1, row.get("sourceId").getAsInt());
            assertTrue(row.get("sourceLine").getAsInt() > previousLine);
            previousLine = row.get("sourceLine").getAsInt();
            bookIndexes.add(row.get("bookIndex").getAsInt());
            assertTrue(row.get("translationKey").getAsString().startsWith("witchery.rite."));
            assertFalse(row.get("effect").getAsString().isBlank());
            assertFalse(row.get("sacrifice").getAsString().isBlank());
            assertFalse(row.get("traits").getAsString().isBlank());
            assertFalse(row.get("modernHome").getAsString().isBlank());
        }
        assertTrue(bookIndexes.size() > 40, "fixture must retain source book-index coverage");
    }

    @Test
    void everyImplementedSourceRowPointsAtItsActualDispatcherHome() {
        Set<String> full = new HashSet<>(RitualExecutor.allFullRecipeDefinitionNames());
        Set<String> legacy = new HashSet<>(RitualExecutor.allRitualDefinitionNames());
        Set<String> direct = Set.of("bind_circle_talisman", "glyphic_transformation");
        Set<String> sourceFullHomes = new HashSet<>();
        List<Integer> unresolved = new ArrayList<>();
        int fullRows = 0;
        int directRows = 0;

        for (var element : fixture().getAsJsonArray("registrations")) {
            JsonObject row = element.getAsJsonObject();
            int sourceId = row.get("sourceId").getAsInt();
            String home = row.get("modernHome").getAsString();
            switch (row.get("mode").getAsString()) {
                case "full" -> {
                    fullRows++;
                    sourceFullHomes.add(home);
                    assertTrue(full.contains(home), sourceId + " missing full home " + home);
                }
                case "direct" -> {
                    directRows++;
                    assertTrue(direct.contains(home), sourceId + " missing direct home " + home);
                }
                case "fallback", "missing" -> unresolved.add(sourceId);
                default -> throw new AssertionError("Unknown mode for source rite " + sourceId);
            }
        }

        assertEquals(93, fullRows);
        assertEquals(3, directRows);
        assertTrue(unresolved.isEmpty(), "source rites may not remain on fallback dispatch: " + unresolved);
        sourceFullHomes.retainAll(legacy);
        assertTrue(sourceFullHomes.isEmpty(),
                "exact source recipes may not also have representative-trigger routes: " + sourceFullHomes);
    }

    @Test
    void everyFullRecipeUsesTheExactSourceCircleSet() {
        Set<String> signatures = new HashSet<>(RitualExecutor.fullRecipeRingSignatures());
        Set<String> optionalSignatures = new HashSet<>(RitualExecutor.fullRecipeOptionalSignatures());
        for (var element : fixture().getAsJsonArray("registrations")) {
            JsonObject row = element.getAsJsonObject();
            if (!row.get("mode").getAsString().equals("full")) continue;
            List<String> rings = new ArrayList<>();
            for (var circleElement : row.getAsJsonArray("circles")) {
                Matcher matcher = SOURCE_CIRCLE.matcher(circleElement.getAsString());
                assertTrue(matcher.matches(), "unparsed source circle: " + circleElement.getAsString());
                int ritual = Integer.parseInt(matcher.group(1));
                int otherwhere = Integer.parseInt(matcher.group(2));
                int infernal = Integer.parseInt(matcher.group(3));
                int radius = Math.max(ritual, Math.max(otherwhere, infernal));
                String size = switch (radius) {
                    case 16 -> "SMALL";
                    case 28 -> "MEDIUM";
                    case 40 -> "LARGE";
                    default -> throw new AssertionError("unknown source circle radius " + radius);
                };
                int variant = ritual > 0 ? 1 : otherwhere > 0 ? 2 : 3;
                rings.add(size + ":" + variant);
            }
            rings.sort(String::compareTo);
            String expected = row.get("modernHome").getAsString() + "|" + String.join(",", rings);
            assertTrue(signatures.contains(expected),
                    "source rite " + row.get("sourceId").getAsInt() + " missing ring signature " + expected);
            String expectedOptional = expected + "|" + row.getAsJsonArray("optionalItems").size();
            assertTrue(optionalSignatures.contains(expectedOptional),
                    "source rite " + row.get("sourceId").getAsInt()
                            + " missing optional-offering signature " + expectedOptional);
        }
    }

    private static JsonObject fixture() {
        var stream = SourceRitualRegistryAuditTest.class.getResourceAsStream(FIXTURE);
        assertNotNull(stream, "missing source ritual fixture");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException exception) {
            throw new AssertionError("Unable to read source ritual fixture", exception);
        }
    }
}
