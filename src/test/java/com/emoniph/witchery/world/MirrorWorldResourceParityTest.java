package com.emoniph.witchery.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MirrorWorldResourceParityTest {
    @Test
    void dimensionUsesFeaturelessBiomeInsteadOfVoidStartPlatform() throws Exception {
        JsonObject dimension = resource("data/witchery/dimension/mirror_world.json");
        assertEquals("witchery:mirror_world", dimension.getAsJsonObject("generator")
                .getAsJsonObject("biome_source").get("biome").getAsString());

        JsonObject biome = resource("data/witchery/worldgen/biome/mirror_world.json");
        JsonArray features = biome.getAsJsonArray("features");
        assertEquals(10, features.size());
        features.forEach(step -> assertTrue(step.getAsJsonArray().isEmpty()));
    }

    @Test
    void dimensionRetainsTheSourceHeight() throws Exception {
        JsonObject dimensionType = resource("data/witchery/dimension_type/mirror_world.json");
        assertEquals(0, dimensionType.get("min_y").getAsInt());
        assertEquals(256, dimensionType.get("height").getAsInt());
        assertEquals(256, dimensionType.get("logical_height").getAsInt());
    }

    @Test
    void reflectionLootIsAwardedExplicitlyWithoutCopiedEquipment() throws Exception {
        JsonObject loot = resource("data/witchery/loot_table/entities/reflection.json");
        assertTrue(loot.getAsJsonArray("pools").isEmpty(),
                "Reflection death code awards one heart while the separate mirror decoy has no loot");
    }

    private static JsonObject resource(String path) throws Exception {
        var stream = MirrorWorldResourceParityTest.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Missing test resource " + path);
        }
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
