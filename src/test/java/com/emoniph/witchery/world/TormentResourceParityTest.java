package com.emoniph.witchery.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TormentResourceParityTest {

    @Test
    void atmosphereKeepsSourceColorsAndNocturnalServerTime() throws IOException {
        JsonObject dimension = resource("data/witchery/dimension_type/torment.json");

        assertEquals(18000L, dimension.get("fixed_time").getAsLong());
        assertEquals(0xFF0000, TormentAtmosphere.SKY_RGB);
        assertEquals(0xA03232, TormentAtmosphere.FOG_RGB);
        assertEquals(0xFF0022, TormentAtmosphere.PORTAL_RGB);
        assertEquals((float) (Math.PI * 2.0D), TormentAtmosphere.FIXED_SUN_ANGLE_RADIANS);
    }

    @Test
    void tormentStoneUsesTheOriginalMyceliumTopArt() throws IOException {
        JsonObject model = resource("assets/witchery/models/block/torment_stone.json");

        assertEquals("minecraft:block/mycelium_top",
                model.getAsJsonObject("textures").get("all").getAsString());
    }

    @Test
    void bothPortalPlanesExposeTheirSourceRedTintIndex() throws IOException {
        assertPortalTint("assets/witchery/models/block/torment_portal_ns.json");
        assertPortalTint("assets/witchery/models/block/torment_portal_ew.json");
    }

    private static void assertPortalTint(String path) throws IOException {
        JsonObject model = resource(path);
        int faces = 0;
        for (var element : model.getAsJsonArray("elements")) {
            for (var face : element.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                assertTrue(face.getValue().getAsJsonObject().has("tintindex"),
                        () -> path + " face " + face.getKey() + " has no tint index");
                assertEquals(0, face.getValue().getAsJsonObject().get("tintindex").getAsInt());
                faces++;
            }
        }
        assertEquals(2, faces, path + " must retain the source two-sided portal plane");
    }

    private static JsonObject resource(String path) throws IOException {
        InputStream stream = TormentResourceParityTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "Missing resource " + path);
        try (stream; InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
