package com.emoniph.witchery.ritual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void playerFacingHearthTextRequiresTheOwnersLivingCoven() throws Exception {
        JsonObject lang = readJson("/assets/witchery/lang/en_us.json");
        assertEquals("The crossing needs six sworn, living witches within the hearth. Only %2$s answer.",
                lang.get("witchery.otherwhere_hearth.coven").getAsString());

        JsonObject entry = readJson(
                "/data/witchery/modonomicon/books/witches_primer/entries/spirit_world/otherwhere_hearth.json");
        String pages = entry.getAsJsonArray("pages").toString();
        assertTrue(pages.contains("six living witches sworn to the hearth's owner"));
        assertTrue(pages.contains("Remembered dead may lend their voices to lesser rites"));
        assertTrue(pages.contains("Touch the bound Kettle with a **Seer Stone**"));
        assertTrue(pages.contains("without taking offerings or altar power"));
        assertFalse(pages.contains("additive endgame working"));

        assertEquals("witchery:guide/obtained/soul_of_torment",
                entry.getAsJsonObject("condition").get("advancement_id").getAsString());
        assertTrue(entry.get("hide_while_locked").getAsBoolean());
        JsonObject unlock = readJson(
                "/data/witchery/advancement/guide/obtained/soul_of_torment.json");
        assertTrue(unlock.toString().contains("witchery:soul_of_torment"));

        assertEquals("This Kettle remembers no hearth.",
                lang.get("item.witchery.seer_stone.hearth_not_bound").getAsString());
        assertTrue(lang.get("item.witchery.seer_stone.hearth_far_veiled").getAsString()
                .contains("judged only when the crossing is attempted"));
    }

    private JsonObject readJson(String path) throws Exception {
        InputStream stream = getClass().getResourceAsStream(path);
        assertNotNull(stream, path);
        try (stream; InputStreamReader reader =
                new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
