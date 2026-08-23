package com.emoniph.witchery.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WitchCauldronModelTest {
    private static final Path MODEL_ROOT = projectRoot().resolve(Path.of("src", "main", "resources", "assets",
            "witchery", "models", "block"));
    private static final double EPSILON = 0.00001;
    private static final Map<String, Bounds> GROUNDED_FEET = Map.of(
            "northwest_leg_foot", new Bounds(0.0, 0.0, 0.0, 3.2, 1.4, 3.2),
            "northeast_leg_foot", new Bounds(12.8, 0.0, 0.0, 16.0, 1.4, 3.2),
            "southwest_leg_foot", new Bounds(0.0, 0.0, 12.8, 3.2, 1.4, 16.0),
            "southeast_leg_foot", new Bounds(12.8, 0.0, 12.8, 16.0, 1.4, 16.0));
    private static final Map<String, Bounds> SOURCE_BODY = Map.ofEntries(
            Map.entry("base", new Bounds(3, 2, 3, 13, 3, 13)),
            Map.entry("bottom_north", new Bounds(3, 3, 2, 13, 4, 3)),
            Map.entry("bottom_south", new Bounds(3, 3, 13, 13, 4, 14)),
            Map.entry("bottom_west", new Bounds(2, 3, 2, 3, 4, 14)),
            Map.entry("bottom_east", new Bounds(13, 3, 2, 14, 4, 14)),
            Map.entry("side_north", new Bounds(2, 4, 1, 14, 10, 2)),
            Map.entry("side_south", new Bounds(2, 4, 14, 14, 10, 15)),
            Map.entry("side_west", new Bounds(1, 4, 1, 2, 10, 15)),
            Map.entry("side_east", new Bounds(14, 4, 1, 15, 10, 15)),
            Map.entry("neck_north", new Bounds(3, 10, 2, 13, 12, 3)),
            Map.entry("neck_south", new Bounds(3, 10, 13, 13, 12, 14)),
            Map.entry("neck_west", new Bounds(2, 10, 2, 3, 12, 14)),
            Map.entry("neck_east", new Bounds(13, 10, 2, 14, 12, 14)),
            Map.entry("lip_north", new Bounds(2, 12, 1, 14, 13, 2)),
            Map.entry("lip_south", new Bounds(2, 12, 14, 14, 13, 15)),
            Map.entry("lip_west", new Bounds(1, 12, 1, 2, 13, 15)),
            Map.entry("lip_east", new Bounds(14, 12, 1, 15, 13, 15)));
    private static final Map<String, Double> SOURCE_LIQUID_HEIGHTS = Map.of(
            "filled1", 5.8666666667,
            "filled2", 8.5333333333,
            "filled3", 11.2);

    @Test
    void everyFillModelKeepsTheExactSourceBodyAndApprovedGroundedCornerFeet() throws IOException {
        for (String suffix : new String[] {"empty", "filled1", "filled2", "filled3"}) {
            JsonObject model = readModel("witch_cauldron_" + suffix + ".json");
            JsonArray elements = model.getAsJsonArray("elements");
            assertEquals(SOURCE_BODY.size() + 12 + (suffix.equals("empty") ? 0 : 1),
                    elements.size(), suffix + " element count");
            for (var expected : GROUNDED_FEET.entrySet()) {
                JsonObject foot = findElement(elements, expected.getKey());
                assertBounds(expected.getValue(), foot, suffix + "/" + expected.getKey());
            }
            for (var expected : SOURCE_BODY.entrySet()) {
                JsonObject body = findElement(elements, expected.getKey());
                assertBounds(expected.getValue(), body, suffix + "/" + expected.getKey());
            }
            if (!suffix.equals("empty")) {
                double height = SOURCE_LIQUID_HEIGHTS.get(suffix);
                assertBounds(new Bounds(2, height, 2, 14, height, 14),
                        findElement(elements, "water"), suffix + "/water");
            }
        }
    }

    private static JsonObject readModel(String fileName) throws IOException {
        try (Reader reader = Files.newBufferedReader(MODEL_ROOT.resolve(fileName))) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path models = current.resolve(Path.of("src", "main", "resources", "assets", "witchery", "models"));
            if (Files.isDirectory(models)) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate Witchery project root");
    }

    private static JsonObject findElement(JsonArray elements, String name) {
        for (var element : elements) {
            JsonObject object = element.getAsJsonObject();
            if (name.equals(object.get("name").getAsString())) {
                return object;
            }
        }
        assertNotNull(null, "Missing Witch Cauldron model element " + name);
        throw new AssertionError();
    }

    private static void assertBounds(Bounds expected, JsonObject element, String label) {
        JsonArray from = element.getAsJsonArray("from");
        JsonArray to = element.getAsJsonArray("to");
        assertEquals(expected.minX(), from.get(0).getAsDouble(), EPSILON, label + " minX");
        assertEquals(expected.minY(), from.get(1).getAsDouble(), EPSILON, label + " minY");
        assertEquals(expected.minZ(), from.get(2).getAsDouble(), EPSILON, label + " minZ");
        assertEquals(expected.maxX(), to.get(0).getAsDouble(), EPSILON, label + " maxX");
        assertEquals(expected.maxY(), to.get(1).getAsDouble(), EPSILON, label + " maxY");
        assertEquals(expected.maxZ(), to.get(2).getAsDouble(), EPSILON, label + " maxZ");
    }

    private record Bounds(double minX, double minY, double minZ,
                          double maxX, double maxY, double maxZ) {
    }
}
