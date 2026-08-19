package com.emoniph.witchery.spiritworld;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritWorldResourceParityTest {

    @Test
    void dimensionMirrorsOverworldTerrainAndBiomeGeography() throws IOException {
        JsonObject dimension = resource("data/witchery/dimension/spirit_world.json");
        JsonObject generator = dimension.getAsJsonObject("generator");
        JsonObject biomeSource = generator.getAsJsonObject("biome_source");

        assertEquals("minecraft:noise", generator.get("type").getAsString());
        assertEquals("minecraft:overworld", generator.get("settings").getAsString());
        assertEquals("minecraft:multi_noise", biomeSource.get("type").getAsString());
        assertEquals("minecraft:overworld", biomeSource.get("preset").getAsString());
        assertNull(SpiritWorldResourceParityTest.class.getClassLoader()
                .getResource("data/witchery/worldgen/biome/spirit_world.json"),
                "A fixed Spirit biome would erase the source's mirrored Overworld geography");
    }

    @Test
    void wispyCottonUsesTheSpiritOnlySourceChunkRoll() throws IOException {
        JsonObject placed = resource("data/witchery/worldgen/placed_feature/wispy_cotton.json");
        java.util.List<String> modifierOrder = new java.util.ArrayList<>();
        Set<String> modifiers = new HashSet<>();
        placed.getAsJsonArray("placement").forEach(element -> {
            String type = element.getAsJsonObject().get("type").getAsString();
            modifierOrder.add(type);
            modifiers.add(type);
        });

        assertEquals("witchery:wispy_cotton", placed.get("feature").getAsString());
        assertEquals("witchery:spirit_world", modifierOrder.get(0));
        assertTrue(modifiers.contains("witchery:spirit_world"));
        assertFalse(modifiers.contains("minecraft:in_square"));
        assertFalse(modifiers.contains("minecraft:heightmap"));
        assertTrue(modifiers.contains("minecraft:biome"));
        assertFalse(modifiers.contains("minecraft:rarity_filter"));

        JsonObject modifier = resource(
                "data/witchery/neoforge/biome_modifier/spirit_world_wispy_cotton.json");
        assertEquals("neoforge:add_features", modifier.get("type").getAsString());
        assertEquals("#witchery:wispy_cotton_spawns", modifier.get("biomes").getAsString());
        assertEquals("witchery:wispy_cotton", modifier.get("features").getAsString());
        assertEquals("vegetal_decoration", modifier.get("step").getAsString());
    }

    @Test
    void wispyCottonUsesSourceBiomeAndSoilRestrictions() throws IOException {
        JsonObject biomeTag = resource("data/witchery/tags/worldgen/biome/wispy_cotton_spawns.json");
        assertEquals(Set.of("#minecraft:is_overworld"), strings(biomeTag, "values"));
        assertEquals(Set.of(
                "#minecraft:is_ocean",
                "#minecraft:is_river",
                "#minecraft:is_mountain",
                "#minecraft:is_beach",
                "minecraft:swamp",
                "minecraft:mangrove_swamp"
        ), strings(biomeTag, "remove"));

        JsonObject configured = resource("data/witchery/worldgen/configured_feature/wispy_cotton.json");
        assertEquals(64, configured.getAsJsonObject("config").get("tries").getAsInt());
        JsonObject feature = configured.getAsJsonObject("config")
                .getAsJsonObject("feature");
        JsonObject predicate = feature.getAsJsonArray("placement")
                .get(0).getAsJsonObject()
                .getAsJsonObject("predicate");
        assertEquals(Set.of(
                "minecraft:grass_block",
                "minecraft:dirt",
                "minecraft:coarse_dirt",
                "minecraft:podzol",
                "minecraft:farmland"
        ), strings(predicate, "blocks"));
        assertFalse(strings(predicate, "blocks").contains("minecraft:stone"));
    }

    @Test
    void nativeSpiritsUseTheSourceWeightAndGroupSize() throws IOException {
        JsonObject modifier = resource(
                "data/witchery/neoforge/biome_modifier/spirit_world_spirits.json");
        JsonObject spawner = modifier.getAsJsonObject("spawners");

        assertEquals("neoforge:add_spawns", modifier.get("type").getAsString());
        assertEquals("#witchery:spirit_spawns", modifier.get("biomes").getAsString());
        assertEquals("witchery:spirit", spawner.get("type").getAsString());
        assertEquals(1, spawner.get("weight").getAsInt());
        assertEquals(2, spawner.get("minCount").getAsInt());
        assertEquals(5, spawner.get("maxCount").getAsInt());

        JsonObject biomeTag = resource("data/witchery/tags/worldgen/biome/spirit_spawns.json");
        assertEquals(Set.of("#minecraft:is_overworld"), strings(biomeTag, "values"));
        assertEquals(Set.of(
                "#minecraft:is_ocean",
                "#minecraft:is_river",
                "minecraft:mushroom_fields"
        ), strings(biomeTag, "remove"));
    }

    private static JsonObject resource(String path) throws IOException {
        InputStream stream = SpiritWorldResourceParityTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "Missing resource " + path);
        try (stream; InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static Set<String> strings(JsonObject json, String key) {
        Set<String> values = new HashSet<>();
        json.getAsJsonArray(key).forEach(element -> values.add(element.getAsString()));
        return values;
    }
}
