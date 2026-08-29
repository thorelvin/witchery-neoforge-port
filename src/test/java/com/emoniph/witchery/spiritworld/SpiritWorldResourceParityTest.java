package com.emoniph.witchery.spiritworld;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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
    void dimensionUsesModernOverworldDaylightAndDisablesBeds() throws IOException {
        JsonObject dimensionType = resource("data/witchery/dimension_type/spirit_world.json");
        JsonObject attributes = dimensionType.getAsJsonObject("attributes");
        JsonObject bedRule = attributes.getAsJsonObject("minecraft:gameplay/bed_rule");

        assertEquals("#minecraft:in_overworld", dimensionType.get("timelines").getAsString(),
                "The daylight timeline darkens nightmare sky and terrain at the forced midnight time");
        assertEquals("never", bedRule.get("can_set_spawn").getAsString());
        assertEquals("never", bedRule.get("can_sleep").getAsString());
        assertEquals(0.0F, attributes.get("minecraft:visual/cloud_height").getAsFloat());
        assertFalse(dimensionType.has("effects"), "The pre-1.21 effects field is ignored by the current codec");
        assertFalse(dimensionType.has("bed_works"), "The pre-1.21 bed field is ignored by the current codec");
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

    @Test
    void spiritPortalUsesSourceThinPlanesAndAnimatedVanillaPortalSprite() throws IOException {
        JsonObject blockState = resource("assets/witchery/blockstates/spirit_portal.json");
        JsonObject variants = blockState.getAsJsonObject("variants");
        assertEquals("witchery:block/spirit_portal_ns",
                variants.getAsJsonObject("axis=x").get("model").getAsString());
        assertEquals("witchery:block/spirit_portal_ew",
                variants.getAsJsonObject("axis=z").get("model").getAsString());

        assertPortalPlane(resource("assets/witchery/models/block/spirit_portal_ns.json"),
                "north", "south", 0, 0, 6, 16, 16, 10);
        assertPortalPlane(resource("assets/witchery/models/block/spirit_portal_ew.json"),
                "east", "west", 6, 0, 0, 10, 16, 16);
    }

    @Test
    void spiritAndNightmareKeepSourceTexturesAndNightmareSoundAssets() throws IOException {
        assertEquals("4c2ea1d52d0cd5a0912ecd3be9844bda3355d6e201c636530ea0e6090d6fc735",
                sha256("assets/witchery/textures/entity/spirit.png"));
        assertEquals("fcaa502aa68c051fd082b14c42be2e186066dbdee2fb996803485edae15dc202",
                sha256("assets/witchery/textures/entity/nightmare.png"));
        assertEquals("b4edf63522a696f7225ff1aa2d524a602a18f425395e0b371911cdbb86ef6aa4",
                sha256("assets/witchery/sounds/mob/nightmare/nightmare_live.ogg"));
        assertEquals("f67fa266a2baedb50e43fc1263f8d6ea3cafa0c88c525e983a4c362aef7ec5ad",
                sha256("assets/witchery/sounds/mob/nightmare/nightmare_dead.ogg"));
        assertEquals("27feb5d820e251a3a9338ad428d725b1406b4376209d7afb9e03f550573f6c6f",
                sha256("assets/witchery/sounds/mob/nightmare/nightmare_hit.ogg"));

        JsonObject sounds = resource("assets/witchery/sounds.json");
        assertEquals("witchery:mob/nightmare/nightmare_live",
                sounds.getAsJsonObject("mob.nightmare.nightmare_live")
                        .getAsJsonArray("sounds").get(0).getAsString());
        assertEquals("witchery:mob/nightmare/nightmare_dead",
                sounds.getAsJsonObject("mob.nightmare.nightmare_dead")
                        .getAsJsonArray("sounds").get(0).getAsString());
        assertEquals("witchery:mob/nightmare/nightmare_hit",
                sounds.getAsJsonObject("mob.nightmare.nightmare_hit")
                        .getAsJsonArray("sounds").get(0).getAsString());
    }

    private static void assertPortalPlane(JsonObject model, String faceA, String faceB,
                                          int fromX, int fromY, int fromZ,
                                          int toX, int toY, int toZ) {
        assertEquals("minecraft:block/nether_portal",
                model.getAsJsonObject("textures").get("portal").getAsString());
        JsonObject element = model.getAsJsonArray("elements").get(0).getAsJsonObject();
        assertEquals(java.util.List.of(fromX, fromY, fromZ), integers(element, "from"));
        assertEquals(java.util.List.of(toX, toY, toZ), integers(element, "to"));
        JsonObject faces = element.getAsJsonObject("faces");
        assertEquals(Set.of(faceA, faceB), faces.keySet());
        assertEquals(0, faces.getAsJsonObject(faceA).get("tintindex").getAsInt());
        assertEquals(0, faces.getAsJsonObject(faceB).get("tintindex").getAsInt());
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

    private static java.util.List<Integer> integers(JsonObject json, String key) {
        java.util.List<Integer> values = new java.util.ArrayList<>();
        json.getAsJsonArray(key).forEach(element -> values.add(element.getAsInt()));
        return values;
    }

    private static String sha256(String path) throws IOException {
        try (InputStream stream = SpiritWorldResourceParityTest.class.getClassLoader()
                .getResourceAsStream(path)) {
            assertNotNull(stream, "Missing resource " + path);
            try {
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(stream.readAllBytes()));
            } catch (NoSuchAlgorithmException exception) {
                throw new AssertionError("SHA-256 must be available", exception);
            }
        }
    }
}
