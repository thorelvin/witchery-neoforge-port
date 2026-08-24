package com.emoniph.witchery.brewing;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryMobEffects;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks the complete extracted 1.7.10 brew registry and high-risk modern mappings. */
class SourceBrewRegistryAuditTest {
    private static final String FIXTURE = "/witchery/brewing/source_brew_registrations.json";
    private static final String SOURCE_SHA256 =
            "17db77abb73bab8b4d3cc5bf827e1d545c5d1a6ba095a79a9229be0891b7abe3";

    @Test
    void sourceFixtureIsCompleteOrderedAndTraceable() {
        JsonObject root = fixture();
        assertEquals(SOURCE_SHA256, root.get("sourceSha256").getAsString());
        assertEquals(158, root.get("registrationSiteCount").getAsInt());
        assertEquals(158, root.getAsJsonArray("registrations").size());

        int previousLine = 0;
        Map<String, Integer> histogram = new LinkedHashMap<>();
        for (int i = 0; i < 158; i++) {
            JsonObject row = root.getAsJsonArray("registrations").get(i).getAsJsonObject();
            assertEquals(i + 1, row.get("sourceIndex").getAsInt());
            assertTrue(row.get("sourceLine").getAsInt() > previousLine);
            previousLine = row.get("sourceLine").getAsInt();
            histogram.merge(row.get("actionClass").getAsString(), 1, Integer::sum);
        }
        assertEquals(48, histogram.get("BrewActionEffect"));
        assertEquals(40, histogram.get("BrewPotionEffect"));
        assertEquals(22, histogram.get("BrewActionModifier"));
        assertEquals(14, histogram.get("BrewActionRitualRecipe"));
        assertEquals(6, histogram.get("BrewCurseEffect"));
        assertEquals(21, histogram.size());
    }

    @Test
    void correctedHighRiskIngredientMappingsMatchSource() {
        Brews.bootstrap();
        assertSame(Brews.Ingredient.BrewAction.HARVEST, Brews.get(Items.APPLE).action());
        assertSame(Brews.Ingredient.BrewAction.TILL, Brews.get(Items.DIRT).action());
        assertSame(Brews.Ingredient.BrewAction.PRUNE, Brews.get(Items.BROWN_MUSHROOM).action());
        assertSame(Brews.Ingredient.BrewAction.PULVERIZE, Brews.get(Items.FLINT).action());
        assertSame(Brews.Ingredient.BrewAction.TIDE_HOLD, Brews.get(Items.SAND).action());
        assertSame(Brews.Ingredient.BrewAction.LAVA_HOLD, Brews.get(Items.COBBLESTONE).action());
        assertSame(WitcheryMobEffects.GAS_MASK.value(), Brews.get(Items.GRAVEL).effect().value());
        assertSame(WitcheryMobEffects.POISON_WEAPONS.value(), Brews.get(Items.RED_MUSHROOM).effect().value());
        assertSame(WitcheryMobEffects.VOLATILITY.value(), Brews.get(Items.DEAD_BUSH).effect().value());
        assertSame(WitcheryMobEffects.GROTESQUE.value(), Brews.get(WitcheryItems.INFERNAL_BLOOD.get()).effect().value());
        assertEquals(200, Brews.get(Items.STICK).powerCost());
        assertEquals(3000, Brews.get(WitcheryItems.REFINED_EVIL.get()).powerCost());
    }

    @Test
    void colorfulLoopRetainsExactTwelveNonConflictingDyeOrdinals() {
        assertEquals(1, Brews.get(Items.RED_DYE).modifierCeiling());
        assertEquals(2, Brews.get(Items.GREEN_DYE).modifierCeiling());
        assertEquals(5, Brews.get(Items.PURPLE_DYE).modifierCeiling());
        assertEquals(6, Brews.get(Items.CYAN_DYE).modifierCeiling());
        assertEquals(7, Brews.get(Items.LIGHT_GRAY_DYE).modifierCeiling());
        assertEquals(8, Brews.get(Items.GRAY_DYE).modifierCeiling());
        assertEquals(9, Brews.get(Items.PINK_DYE).modifierCeiling());
        assertEquals(10, Brews.get(Items.LIME_DYE).modifierCeiling());
        assertEquals(11, Brews.get(Items.YELLOW_DYE).modifierCeiling());
        assertEquals(12, Brews.get(Items.LIGHT_BLUE_DYE).modifierCeiling());
        assertEquals(13, Brews.get(Items.MAGENTA_DYE).modifierCeiling());
        assertEquals(14, Brews.get(Items.ORANGE_DYE).modifierCeiling());
    }

    private static JsonObject fixture() {
        var stream = SourceBrewRegistryAuditTest.class.getResourceAsStream(FIXTURE);
        assertNotNull(stream, "missing source brew registry fixture");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException exception) {
            throw new AssertionError("Unable to read source brew registry fixture", exception);
        }
    }
}
