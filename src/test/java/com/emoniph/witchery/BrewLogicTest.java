package com.emoniph.witchery;

import com.emoniph.witchery.brewing.Brews;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain-JVM verification of the custom-brew composition engine ({@link Brews}). Runs via
 * {@code ./gradlew test} with the game registries bootstrapped by ModDevGradle — no client,
 * no server. Verifies the faithful accumulate-modifiers-then-apply-effects behaviour.
 */
class BrewLogicTest {

    @Test
    void glowstoneStrengthensNextEffect() {
        Brews.bootstrap();
        List<MobEffectInstance> fx = Brews.buildEffects(List.of(Items.GLOWSTONE_DUST, Items.SPIDER_EYE));
        assertEquals(1, fx.size(), "one effect");
        assertSame(MobEffects.POISON.value(), fx.get(0).getEffect().value(), "poison");
        assertEquals(1, fx.get(0).getAmplifier(), "amplifier raised by one glowstone");
    }

    @Test
    void redstoneExtendsDuration() {
        Brews.bootstrap();
        // Spider Eye base poison duration 300; one redstone doubles to 600.
        assertEquals(600, Brews.buildEffects(List.of(Items.REDSTONE, Items.SPIDER_EYE)).get(0).getDuration());
    }

    @Test
    void fermentedSpiderEyeInverts() {
        Brews.bootstrap();
        assertSame(MobEffects.REGENERATION.value(),
                Brews.buildEffects(List.of(Items.FERMENTED_SPIDER_EYE, Items.SPIDER_EYE)).get(0).getEffect().value(),
                "poison inverted to regeneration");
    }

    @Test
    void dispersalDetectedFromModifiers() {
        Brews.bootstrap();
        assertEquals(Brews.Dispersal.SPLASH, Brews.dispersal(List.of(Items.GUNPOWDER, Items.SPIDER_EYE)));
        assertEquals(Brews.Dispersal.GAS, Brews.dispersal(List.of(Items.DRAGON_BREATH, Items.SPIDER_EYE)));
        assertEquals(Brews.Dispersal.DRINK, Brews.dispersal(List.of(Items.SPIDER_EYE)));
    }

    @Test
    void sourceDispersalIngredientsAreRecognized() {
        Brews.bootstrap();
        assertEquals(Brews.Dispersal.SPLASH,
                Brews.dispersal(List.of(WitcheryItems.ARTICHOKE.get(), Items.SPIDER_EYE)),
                "Water Artichoke is the old instant/splash dispersal");
        assertEquals(Brews.Dispersal.GAS,
                Brews.dispersal(List.of(WitcheryItems.BAT_WOOL.get(), Items.SPIDER_EYE)),
                "Bat Wool is the old gas dispersal");
    }

    @Test
    void latestCompetingDispersalWins() {
        Brews.bootstrap();
        assertEquals(Brews.Dispersal.GAS,
                Brews.dispersal(List.of(Items.GUNPOWDER, WitcheryItems.BAT_WOOL.get(), Items.SPIDER_EYE)),
                "later Bat Wool replaces earlier instant dispersal");
        assertEquals(Brews.Dispersal.SPLASH,
                Brews.dispersal(List.of(WitcheryItems.BAT_WOOL.get(), WitcheryItems.ARTICHOKE.get(), Items.SPIDER_EYE)),
                "later Artichoke replaces earlier gas dispersal");
    }

    @Test
    void woolColorModifiersForceBottleTint() {
        Brews.bootstrap();
        assertEquals(12464176, Brews.color(List.of(Items.RED_WOOL, Items.SPIDER_EYE)),
                "red wool uses the old Witchery dye RGB");
        assertEquals(2247599, Brews.color(List.of(Items.RED_WOOL, Items.BLUE_WOOL, Items.SPIDER_EYE)),
                "latest wool colour modifier wins");
    }

    @Test
    void modifiersResetBetweenEffects() {
        Brews.bootstrap();
        // Glowstone strengthens only the FIRST effect; the second resets to amplifier 0.
        List<MobEffectInstance> fx = Brews.buildEffects(List.of(Items.GLOWSTONE_DUST, Items.SPIDER_EYE, Items.SUGAR));
        assertEquals(2, fx.size(), "two effects");
        assertEquals(1, fx.get(0).getAmplifier(), "first strengthened");
        assertEquals(0, fx.get(1).getAmplifier(), "second reset");
    }

    @Test
    void sourceCapacityIngredientsUnlockHeartOfGold() {
        Brews.bootstrap();
        Item tear = WitcheryItems.TEAR_OF_GODDESS.get();
        Item heart = WitcheryItems.GOLDEN_HEART.get();

        assertFalse(Brews.canAdd(List.of(WitcheryItems.MANDRAKE_ROOT.get(), Items.NETHER_WART), heart),
                "Mandrake + Nether Wart leaves Heart of Gold below its level-4 requirement");
        assertTrue(Brews.canAdd(List.of(Items.NETHER_WART, tear), heart),
                "Nether Wart + Tear of the Goddess reaches the old level-4 capacity threshold");

        List<Brews.ActionApplication> actions = Brews.buildActions(List.of(Items.NETHER_WART, tear, heart));
        assertEquals(1, actions.size(), "Heart of Gold action resolves after old capacity modifiers");
        assertEquals(Brews.Ingredient.BrewAction.ANIMAL, actions.get(0).action(), "animal attraction action");
        assertEquals(650, Brews.powerCost(List.of(Items.NETHER_WART, tear, heart)), "source capacity + Heart altar cost");
    }

    @Test
    void dualPurposeCapacityIngredientsRequireExplicitStirring() {
        Brews.bootstrap();
        assertTrue(Brews.isIngredient(WitcheryItems.MANDRAKE_ROOT.get()), "Mandrake Root is a brew capacity ingredient");
        assertTrue(Brews.requiresExplicitStir(WitcheryItems.MANDRAKE_ROOT.get()), "Mandrake Root keeps dropped-offering behavior");
        assertFalse(Brews.canAddAutomatically(List.of(), WitcheryItems.MANDRAKE_ROOT.get()),
                "dropped Mandrake Root must remain available to hardcoded cauldron offerings");
        assertTrue(Brews.canAddAutomatically(List.of(), WitcheryItems.DIAMOND_VAPOUR.get()),
                "non-conflicting capacity ingredients still auto-stir when dropped");
    }
}
