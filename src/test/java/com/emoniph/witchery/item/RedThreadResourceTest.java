package com.emoniph.witchery.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedThreadResourceTest {
    @Test
    void spinningRecipeUsesTheApprovedNarrowIngredientSet() throws Exception {
        JsonObject recipe = readJson("/data/witchery/recipe/spinning/red_thread.json");

        assertEquals("witchery:spinning", recipe.get("type").getAsString());
        assertEquals("minecraft:string", recipe.getAsJsonObject("fibre").get("id").getAsString());
        assertEquals(4, recipe.getAsJsonObject("fibre").get("count").getAsInt());
        assertEquals(List.of("minecraft:red_dye", "witchery:reek_of_misfortune"),
                recipe.getAsJsonArray("modifiers").asList().stream()
                        .map(element -> element.getAsJsonObject().get("id").getAsString())
                        .toList());
        assertEquals("witchery:red_thread", recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals(4, recipe.getAsJsonObject("result").get("count").getAsInt());
        assertEquals(300, recipe.get("cookingtime").getAsInt());
    }

    @Test
    void iconIsACrispTransparentCrimsonSprite() throws Exception {
        BufferedImage red = readImage("/assets/witchery/textures/item/ingredient.red_thread.png");

        assertEquals(16, red.getWidth());
        assertEquals(16, red.getHeight());
        int opaquePixels = 0;
        int transparentPixels = 0;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int redArgb = red.getRGB(x, y);
                int alpha = redArgb >>> 24;
                assertTrue(alpha == 0 || alpha == 255, "hard alpha edge at " + x + "," + y);
                if (alpha == 0) {
                    transparentPixels++;
                } else {
                    opaquePixels++;
                    int redChannel = redArgb >> 16 & 0xFF;
                    int greenChannel = redArgb >> 8 & 0xFF;
                    int blueChannel = redArgb & 0xFF;
                    assertTrue(redChannel > greenChannel && redChannel > blueChannel,
                            "crimson palette at " + x + "," + y);
                }
            }
        }
        assertTrue(opaquePixels >= 60 && opaquePixels <= 110);
        assertTrue(transparentPixels >= 140);
    }

    @Test
    void modelsAndPlayerFacingLanguagePointAtTheNewItem() throws Exception {
        JsonObject model = readJson("/assets/witchery/models/item/red_thread.json");
        assertEquals("witchery:item/ingredient.red_thread",
                model.getAsJsonObject("textures").get("layer0").getAsString());
        JsonObject lang = readJson("/assets/witchery/lang/en_us.json");
        assertEquals("Red Thread", lang.get("item.witchery.red_thread").getAsString());
        assertNotEquals("", lang.get("witchery.anchored_curse.sinking.relief").getAsString());
    }

    private JsonObject readJson(String path) throws Exception {
        InputStream stream = getClass().getResourceAsStream(path);
        assertNotNull(stream, path);
        try (stream; InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private BufferedImage readImage(String path) throws Exception {
        InputStream stream = getClass().getResourceAsStream(path);
        assertNotNull(stream, path);
        try (stream) {
            return ImageIO.read(stream);
        }
    }
}
