package com.emoniph.witchery.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class LegacyHeldItemResourceTest {
    private static final Path PROJECT_ROOT = projectRoot();
    private static final Path ASSETS = PROJECT_ROOT.resolve(Path.of("src", "main", "resources", "assets", "witchery"));
    private static final Path ITEMS = ASSETS.resolve("items");
    private static final Path MODELS = ASSETS.resolve(Path.of("models", "item"));
    private static final Path ENTITY_TEXTURES = ASSETS.resolve(Path.of("textures", "entity"));
    private static final Set<String> NON_HELD_CONTEXTS =
            Set.of("gui", "ground", "fixed", "on_shelf", "head");

    @Test
    void staticHeldItemsKeepTheirTwoDimensionalIconsOutsideHeldContexts() throws IOException {
        assertHeldDefinition("brew_bag", "witchery:item/brew_bag",
                "witchery:item/brew_bag_in_hand_base", "witchery:brew_bag_held", 1);
        assertHeldDefinition("cane_sword", "witchery:item/cane_sword",
                "witchery:item/cane_sword_in_hand_base", "witchery:cane_sword_held", 1);
        assertHeldDefinition("drawn_cane_sword", "witchery:item/drawn_cane_sword",
                "witchery:item/drawn_cane_sword_in_hand_base", "witchery:drawn_cane_sword_held", 1);
        assertHeldDefinition("huntsman_spear", "witchery:item/huntsman_spear",
                "witchery:item/huntsman_spear_in_hand_base", "witchery:huntsman_spear_held", 1);
        assertHeldDefinition("mystic_branch", "witchery:item/mystic_branch",
                "witchery:item/mystic_branch_in_hand_base", "witchery:mystic_branch_held", 1);
    }

    @Test
    void bothHandBowIdsUseTheSourceThreeDimensionalDrawStatesOnlyWhileHeld() throws IOException {
        assertHandBowDefinition("hand_bow", "witchery:item/hand_bow");
        assertHandBowDefinition("crossbow_pistol", "witchery:item/crossbow_pistol");
    }

    @Test
    void heldBaseModelsCannotAddASecondTransformLayer() throws IOException {
        for (String modelName : List.of("brew_bag_in_hand_base", "cane_sword_in_hand_base",
                "drawn_cane_sword_in_hand_base", "hand_bow_in_hand_base",
                "huntsman_spear_in_hand_base", "mystic_branch_in_hand_base")) {
            JsonObject root = readJson(MODELS.resolve(modelName + ".json"));
            JsonObject display = root.getAsJsonObject("display");
            assertEquals(Set.of("thirdperson_righthand", "thirdperson_lefthand",
                    "firstperson_righthand", "firstperson_lefthand"), display.keySet(), modelName + " contexts");
            for (var entry : display.entrySet()) {
                JsonObject transform = entry.getValue().getAsJsonObject();
                assertVector(transform, "rotation", 0.0F, 0.0F, 0.0F, modelName + " " + entry.getKey());
                assertVector(transform, "translation", 0.0F, 0.0F, 0.0F, modelName + " " + entry.getKey());
                assertVector(transform, "scale", 1.0F, 1.0F, 1.0F, modelName + " " + entry.getKey());
            }
        }
    }

    @Test
    void heldModelsUseTheExactSourceTextureSheets() throws IOException, NoSuchAlgorithmException {
        assertTexture("brewbottle.png", 32, 32,
                "48e89aa77275112cbca43c0a25eba3bd13ae9bbdc0b6fb9cd6be52f416b84951");
        assertTexture("canesword.png", 32, 32,
                "42c88723cb8c8d04520cd05715cecafb1f6e285ca72db10b446b363a81252889");
        assertTexture("handbow.png", 64, 32,
                "8da672b8ed9ba250beae8d20a8b63a068678a1d5680c72c52fddcdb6807198f0");
        assertTexture("huntsmanspear.png", 64, 64,
                "5c68ff78ddcfb3c4e299502cd831fa60aec8b124d8dd3cb4a8a7b3a3c156a23b");
        assertTexture("mysticbranch.png", 32, 32,
                "5a9a5d2fdbdd8836c7852e004b88d0c14804202f80a04f6002c5182b4621f568");
    }

    private static void assertHandBowDefinition(String itemId, String flatModel) throws IOException {
        JsonObject root = assertDisplaySelect(itemId, flatModel);
        List<JsonObject> specialNodes = objectsWithType(root, "minecraft:special");
        assertEquals(6, specialNodes.size(), itemId + " special state count");

        int[] stages = new int[3];
        int pullingStates = 0;
        for (JsonObject special : specialNodes) {
            assertEquals("witchery:item/hand_bow_in_hand_base", special.get("base").getAsString(),
                    itemId + " special base");
            JsonObject renderer = special.getAsJsonObject("model");
            assertEquals("witchery:hand_bow_held", renderer.get("type").getAsString(),
                    itemId + " special renderer");
            int stage = renderer.has("stage") ? renderer.get("stage").getAsInt() : 0;
            stages[stage]++;
            if (renderer.has("pulling") && renderer.get("pulling").getAsBoolean()) pullingStates++;
        }
        assertEquals(2, stages[0], itemId + " stage 0 routes");
        assertEquals(1, stages[1], itemId + " stage 1 routes");
        assertEquals(3, stages[2], itemId + " stage 2 routes");
        assertEquals(4, pullingStates, itemId + " pulling routes");
    }

    private static void assertHeldDefinition(String itemId, String flatModel,
                                             String specialBase, String rendererType,
                                             int specialCount) throws IOException {
        JsonObject root = assertDisplaySelect(itemId, flatModel);
        List<JsonObject> specialNodes = objectsWithType(root, "minecraft:special");
        assertEquals(specialCount, specialNodes.size(), itemId + " special state count");
        for (JsonObject special : specialNodes) {
            assertEquals(specialBase, special.get("base").getAsString(), itemId + " special base");
            assertEquals(rendererType, special.getAsJsonObject("model").get("type").getAsString(),
                    itemId + " renderer type");
        }
    }

    private static JsonObject assertDisplaySelect(String itemId, String flatModel) throws IOException {
        JsonObject root = readJson(ITEMS.resolve(itemId + ".json"));
        JsonObject select = root.getAsJsonObject("model");
        assertEquals("minecraft:select", select.get("type").getAsString(), itemId + " root type");
        assertEquals("minecraft:display_context", select.get("property").getAsString(), itemId + " property");
        JsonObject firstCase = select.getAsJsonArray("cases").get(0).getAsJsonObject();
        Set<String> contexts = new HashSet<>();
        firstCase.getAsJsonArray("when").forEach(element -> contexts.add(element.getAsString()));
        assertEquals(NON_HELD_CONTEXTS, contexts, itemId + " non-held contexts");
        assertEquals(flatModel, firstCase.getAsJsonObject("model").get("model").getAsString(),
                itemId + " flat model");
        return root;
    }

    private static List<JsonObject> objectsWithType(JsonElement element, String type) {
        List<JsonObject> matches = new ArrayList<>();
        collectObjectsWithType(element, type, matches);
        return matches;
    }

    private static void collectObjectsWithType(JsonElement element, String type, List<JsonObject> matches) {
        if (element == null || element.isJsonNull()) return;
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("type") && object.get("type").isJsonPrimitive()
                    && type.equals(object.get("type").getAsString())) {
                matches.add(object);
            }
            object.entrySet().forEach(entry -> collectObjectsWithType(entry.getValue(), type, matches));
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectObjectsWithType(child, type, matches));
        }
    }

    private static void assertVector(JsonObject object, String name,
                                     float x, float y, float z, String message) {
        var values = object.getAsJsonArray(name);
        assertEquals(x, values.get(0).getAsFloat(), 0.0001F, message + " " + name + " x");
        assertEquals(y, values.get(1).getAsFloat(), 0.0001F, message + " " + name + " y");
        assertEquals(z, values.get(2).getAsFloat(), 0.0001F, message + " " + name + " z");
    }

    private static void assertTexture(String fileName, int width, int height, String sha256)
            throws IOException, NoSuchAlgorithmException {
        Path texture = ENTITY_TEXTURES.resolve(fileName);
        BufferedImage image = ImageIO.read(texture.toFile());
        assertNotNull(image, fileName + " is not a readable PNG");
        assertEquals(width, image.getWidth(), fileName + " width");
        assertEquals(height, image.getHeight(), fileName + " height");
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(texture));
        assertEquals(sha256, HexFormat.of().formatHex(digest), fileName + " source hash");
    }

    private static JsonObject readJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve(Path.of("src", "main", "resources", "assets", "witchery")))) {
                return current;
            }
            current = current.getParent();
        }
        return Path.of("").toAbsolutePath();
    }
}
