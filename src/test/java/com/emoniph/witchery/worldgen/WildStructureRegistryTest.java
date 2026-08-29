package com.emoniph.witchery.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WildStructureRegistryTest {

    private static final List<Spec> SPECS = List.of(
            new Spec("coven_circle", "coven_circles", "#minecraft:is_overworld",
                    "surface_structures", "beard_thin", 32, 8, 502938471, true),
            new Spec("straw_man", "straw_men", "#minecraft:is_overworld",
                    "surface_structures", "none", 32, 8, 744019231, true),
            new Spec("hobgoblin_hut", "hobgoblin_huts", "#witchery:has_hobgoblin_hut",
                    "surface_structures", "none", 32, 8, 847291035, false),
            new Spec("witch_hut", "witch_huts", "#minecraft:is_forest",
                    "surface_structures", "none", 36, 10, 1630924712, false),
            new Spec("warped_coven", "warped_covens", "#witchery:has_warped_coven",
                    "top_layer_modification", "none", 48, 16, 604183902, false),
            new Spec("vanishing_hut", "vanishing_huts", "#witchery:has_vanishing_hut",
                    "surface_structures", "none", 44, 20, 681240973, false)
    );

    @Test
    void allSixStructuresHaveExactRegistryAndPlacementContracts() throws IOException {
        for (Spec spec : SPECS) {
            JsonObject structure = json("data/witchery/worldgen/structure/" + spec.id + ".json");
            assertEquals("witchery:" + spec.id, structure.get("type").getAsString(), spec.id);
            assertEquals(spec.biomes, structure.get("biomes").getAsString(), spec.id);
            assertEquals(spec.step, structure.get("step").getAsString(), spec.id);
            assertEquals(spec.terrainAdaptation,
                    structure.get("terrain_adaptation").getAsString(), spec.id);
            assertEquals(80, structure.get("max_distance_from_center").getAsInt(), spec.id);
            assertTrue(structure.getAsJsonObject("spawn_overrides").isEmpty(),
                    spec.id + " must own explicit residents in its piece, not multiply them through spawn overrides");

            JsonObject set = json("data/witchery/worldgen/structure_set/" + spec.setId + ".json");
            JsonArray structures = set.getAsJsonArray("structures");
            assertEquals(1, structures.size(), spec.id);
            assertEquals("witchery:" + spec.id,
                    structures.get(0).getAsJsonObject().get("structure").getAsString(), spec.id);
            assertEquals(1, structures.get(0).getAsJsonObject().get("weight").getAsInt(), spec.id);
            JsonObject placement = set.getAsJsonObject("placement");
            assertEquals("minecraft:random_spread", placement.get("type").getAsString(), spec.id);
            assertEquals(spec.spacing, placement.get("spacing").getAsInt(), spec.id);
            assertEquals(spec.separation, placement.get("separation").getAsInt(), spec.id);
            assertEquals(spec.salt, placement.get("salt").getAsInt(), spec.id);
            if (spec.excludesVillages) {
                JsonObject exclusion = placement.getAsJsonObject("exclusion_zone");
                assertEquals(4, exclusion.get("chunk_count").getAsInt(), spec.id);
                assertEquals("minecraft:villages", exclusion.get("other_set").getAsString(), spec.id);
            } else {
                assertFalse(placement.has("exclusion_zone"), spec.id);
            }
        }
    }

    @Test
    void customBiomeTagsMatchTheCertifiedRoster() throws IOException {
        assertTag("has_hobgoblin_hut", Set.of(
                "#minecraft:is_forest", "minecraft:swamp", "minecraft:mangrove_swamp"));
        assertTag("has_vanishing_hut", Set.of(
                "#minecraft:is_forest", "minecraft:swamp", "minecraft:mangrove_swamp"));
        assertTag("has_warped_coven", Set.of("minecraft:warped_forest"));
    }

    @Test
    void everyDeclaredLootFamilyIsPackaged() throws IOException {
        for (String loot : List.of("witch_hut", "warped_coven", "vanishing_hut")) {
            JsonObject table = json("data/witchery/loot_table/chests/" + loot + ".json");
            assertEquals("minecraft:chest", table.get("type").getAsString(), loot);
            assertFalse(table.getAsJsonArray("pools").isEmpty(), loot);
        }
    }

    private static void assertTag(String id, Set<String> expected) throws IOException {
        JsonArray values = json("data/witchery/tags/worldgen/biome/" + id + ".json")
                .getAsJsonArray("values");
        Set<String> actual = new java.util.HashSet<>();
        values.forEach(value -> actual.add(value.getAsString()));
        assertEquals(expected, actual, id);
    }

    private static JsonObject json(String resource) throws IOException {
        InputStream input = WildStructureRegistryTest.class.getClassLoader().getResourceAsStream(resource);
        assertNotNull(input, "Missing " + resource);
        try (InputStream stream = input;
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private record Spec(String id, String setId, String biomes, String step,
                        String terrainAdaptation, int spacing, int separation, int salt,
                        boolean excludesVillages) {}
}
