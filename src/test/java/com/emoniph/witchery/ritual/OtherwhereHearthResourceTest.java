package com.emoniph.witchery.ritual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class OtherwhereHearthResourceTest {
    @Test
    void permanentBindingBrewHasTheApprovedEndgameRecipe() throws Exception {
        InputStream stream = getClass().getResourceAsStream(
                "/data/witchery/recipe/otherwhere_hearth_brew.json");
        assertNotNull(stream);

        JsonObject recipe;
        try (stream; InputStreamReader reader =
                new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            recipe = JsonParser.parseReader(reader).getAsJsonObject();
        }

        assertEquals("witchery:kettle_brewing", recipe.get("type").getAsString());
        assertEquals(List.of(
                        "witchery:soul_of_torment",
                        "witchery:demon_heart",
                        "witchery:brew_of_sleeping",
                        "witchery:spirit_of_otherwhere",
                        "witchery:attuned_stone_charged",
                        "witchery:drop_of_luck"),
                recipe.getAsJsonArray("ingredients").asList().stream()
                        .map(element -> element.getAsJsonObject().get("id").getAsString())
                        .toList());
        assertEquals("witchery:otherwhere_hearth_brew",
                recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt());
        assertEquals(10_000.0D, recipe.get("power").getAsDouble());
    }
}
