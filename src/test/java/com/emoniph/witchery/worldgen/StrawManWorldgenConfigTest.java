package com.emoniph.witchery.worldgen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrawManWorldgenConfigTest {
    private static final String STRUCTURE_RESOURCE = "data/witchery/worldgen/structure/straw_man.json";

    @Test
    void strawManDoesNotAskMinecraftToBuildATerrainShelf() throws IOException {
        InputStream input = StrawManWorldgenConfigTest.class.getClassLoader().getResourceAsStream(STRUCTURE_RESOURCE);
        assertNotNull(input, "Missing " + STRUCTURE_RESOURCE);

        try (InputStream stream = input) {
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"terrain_adaptation\": \"none\""));
            assertFalse(json.contains("beard_thin"));
        }
    }
}
