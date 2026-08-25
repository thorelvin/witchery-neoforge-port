package com.emoniph.witchery.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatronStatueResourceTest {
    private static final Path ROOT = projectRoot().resolve("src/main/resources/assets/witchery");

    @Test
    void itemUsesTheDedicatedSpecialRenderer() throws IOException {
        JsonObject item = JsonParser.parseReader(Files.newBufferedReader(
                ROOT.resolve("items/statue_hobgoblin_patron.json"))).getAsJsonObject();
        JsonObject model = item.getAsJsonObject("model");
        assertEquals("minecraft:special", model.get("type").getAsString());
        assertEquals("witchery:statue_hobgoblin_patron",
                model.getAsJsonObject("model").get("type").getAsString());
    }

    @Test
    void translucentOverlayIsTheOriginalWitcheryAsset() throws IOException, NoSuchAlgorithmException {
        Path texture = ROOT.resolve("textures/block/statueofworship.png");
        assertTrue(Files.isRegularFile(texture));
        byte[] bytes = Files.readAllBytes(texture);
        assertEquals("25de981665055454721117b542779ec5221ef106abd049689ed5ca91033fafb4",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
    }

    @Test
    void statueCraftingRecipeMatchesTheSourceShape() throws IOException {
        Path recipePath = projectRoot().resolve(
                "src/main/resources/data/witchery/recipe/statue_hobgoblin_patron.json");
        JsonObject recipe = JsonParser.parseReader(Files.newBufferedReader(recipePath)).getAsJsonObject();
        assertEquals("SKS", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals(" S ", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("S S", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("minecraft:stone", recipe.getAsJsonObject("key").get("S").getAsString());
        assertEquals("witchery:koboldite_ingot", recipe.getAsJsonObject("key").get("K").getAsString());
        assertEquals("witchery:statue_hobgoblin_patron",
                recipe.getAsJsonObject("result").get("id").getAsString());
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("src/main/resources/assets/witchery"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate Witchery project root");
    }
}
