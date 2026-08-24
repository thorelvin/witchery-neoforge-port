package com.emoniph.witchery.book;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves that the player-facing compendium covers the audited source table without recipe drift. */
class RitualCompendiumParityTest {

    private static final String SOURCE_FIXTURE = "/witchery/ritual/source_ritual_registrations.json";
    private static final String COMPENDIUM = "/data/witchery/ritual/ritual_compendium.json";

    @Test
    void compendiumAssignsEverySourceRegistrationToOneFamily() {
        JsonObject root = resource(COMPENDIUM);
        assertEquals(1, root.get("schemaVersion").getAsInt());
        assertEquals(96, root.get("registrationCount").getAsInt());
        assertEquals(58, root.get("familyCount").getAsInt());
        assertEquals(58, root.getAsJsonArray("families").size());

        Set<String> familyIds = new HashSet<>();
        List<Integer> assigned = new ArrayList<>();
        int full = 0;
        int direct = 0;
        for (var familyElement : root.getAsJsonArray("families")) {
            JsonObject family = familyElement.getAsJsonObject();
            assertTrue(familyIds.add(family.get("id").getAsString()), "duplicate family ID");
            assertFalse(family.get("title").getAsString().isBlank());
            assertTrue(family.get("icon").getAsString().contains(":"));
            JsonArray sourceIds = family.getAsJsonArray("sourceIds");
            JsonArray variants = family.getAsJsonArray("variants");
            assertEquals(sourceIds.size(), variants.size());
            for (int index = 0; index < variants.size(); index++) {
                JsonObject variant = variants.get(index).getAsJsonObject();
                int sourceId = variant.get("sourceId").getAsInt();
                assertEquals(sourceIds.get(index).getAsInt(), sourceId);
                assigned.add(sourceId);
                if (variant.get("mode").getAsString().equals("full")) full++;
                if (variant.get("mode").getAsString().equals("direct")) direct++;
                assertTrue(variant.getAsJsonArray("required").size() > 0);
                assertTrue(variant.getAsJsonArray("required").size()
                        + variant.getAsJsonArray("optional").size() <= 10);
            }
        }
        assigned.sort(Integer::compareTo);
        assertEquals(IntStream.rangeClosed(1, 96).boxed().toList(), assigned);
        assertEquals(93, full);
        assertEquals(3, direct);
    }

    @Test
    void compendiumRetainsExactSourceEvidenceAndOfferingOrder() {
        JsonObject source = resource(SOURCE_FIXTURE);
        JsonObject compendium = resource(COMPENDIUM);
        assertEquals(source.get("source").getAsString(), compendium.get("source").getAsString());
        assertEquals(source.get("sourceSha256").getAsString(), compendium.get("sourceSha256").getAsString());

        Map<Integer, JsonObject> variants = variantsBySourceId(compendium);
        for (var rowElement : source.getAsJsonArray("registrations")) {
            JsonObject row = rowElement.getAsJsonObject();
            int sourceId = row.get("sourceId").getAsInt();
            JsonObject variant = variants.get(sourceId);
            assertNotNull(variant, "missing compendium source ID " + sourceId);
            assertEquals(row.get("sourceLine").getAsInt(), variant.get("sourceLine").getAsInt());
            assertEquals(row.get("bookIndex").getAsInt(), variant.get("sourceBookIndex").getAsInt());
            assertEquals(row.get("translationKey").getAsString(), variant.get("translationKey").getAsString());
            assertEquals(row.get("traits").getAsString(), variant.get("sourceTraits").getAsString());
            assertEquals(row.get("modernHome").getAsString(), variant.get("modernHome").getAsString());
            assertEquals(row.get("mode").getAsString(), variant.get("mode").getAsString());
            assertEquals(row.get("power").getAsDouble(), variant.get("power").getAsDouble());
            assertEquals(strings(row.getAsJsonArray("circles")),
                    sourceExpressions(variant.getAsJsonArray("circles")));
            assertEquals(strings(row.getAsJsonArray("requiredItems")),
                    sourceExpressions(variant.getAsJsonArray("required")));
            assertEquals(strings(row.getAsJsonArray("optionalItems")),
                    sourceExpressions(variant.getAsJsonArray("optional")));
            assertOfferingMetadata(variant.getAsJsonArray("required"), false, sourceId);
            assertOfferingMetadata(variant.getAsJsonArray("optional"), true, sourceId);
        }
    }

    @Test
    void compendiumUsesOnlyRenderableCircleAndConditionMetadata() {
        Set<String> colors = Set.of("white", "purple", "red");
        Set<String> sizes = Set.of("small", "medium", "large");
        Set<String> conditions = Set.of("Day", "Night", "Storm", "Overworld", "Full moon",
                "Active cat familiar");
        for (JsonObject variant : variantsBySourceId(resource(COMPENDIUM)).values()) {
            assertFalse(variant.get("title").getAsString().isBlank());
            assertFalse(variant.get("variantLabel").getAsString().isBlank());
            assertFalse(variant.get("description").getAsString().isBlank());
            assertTrue(variant.get("power").getAsInt() >= 0);
            assertTrue(variant.get("coven").getAsInt() >= 0 && variant.get("coven").getAsInt() <= 6);
            for (var circleElement : variant.getAsJsonArray("circles")) {
                JsonObject circle = circleElement.getAsJsonObject();
                assertTrue(colors.contains(circle.get("color").getAsString()));
                assertTrue(sizes.contains(circle.get("size").getAsString()));
            }
            for (var condition : variant.getAsJsonArray("conditions")) {
                assertTrue(conditions.contains(condition.getAsString()));
            }
        }
    }

    private static void assertOfferingMetadata(JsonArray offerings, boolean optional, int sourceId) {
        for (var offeringElement : offerings) {
            JsonObject offering = offeringElement.getAsJsonObject();
            assertEquals(optional, offering.get("optional").getAsBoolean());
            assertTrue(offering.get("item").getAsString().contains(":"), "invalid item in source " + sourceId);
            assertFalse(offering.get("label").getAsString().isBlank(), "blank label in source " + sourceId);
        }
    }

    private static Map<Integer, JsonObject> variantsBySourceId(JsonObject root) {
        Map<Integer, JsonObject> variants = new HashMap<>();
        for (var familyElement : root.getAsJsonArray("families")) {
            for (var variantElement : familyElement.getAsJsonObject().getAsJsonArray("variants")) {
                JsonObject variant = variantElement.getAsJsonObject();
                JsonObject previous = variants.put(variant.get("sourceId").getAsInt(), variant);
                assertTrue(previous == null, "source ID appears in more than one family");
            }
        }
        return variants;
    }

    private static List<String> sourceExpressions(JsonArray array) {
        List<String> values = new ArrayList<>();
        for (var element : array) {
            values.add(element.getAsJsonObject().get("sourceExpression").getAsString());
        }
        return values;
    }

    private static List<String> strings(JsonArray array) {
        List<String> values = new ArrayList<>();
        for (var element : array) {
            values.add(element.getAsString());
        }
        return values;
    }

    private static JsonObject resource(String path) {
        var stream = RitualCompendiumParityTest.class.getResourceAsStream(path);
        assertNotNull(stream, "missing test resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException exception) {
            throw new AssertionError("Unable to read " + path, exception);
        }
    }
}
