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
        // Spider Eye source poison duration is 900 ticks; one Redstone doubles to 1800.
        assertEquals(1800, Brews.buildEffects(List.of(Items.REDSTONE, Items.SPIDER_EYE)).get(0).getDuration());
    }

    @Test
    void sourceStrengthModifiersUseCappedTiers() {
        Brews.bootstrap();
        assertEquals(1, Brews.buildEffects(List.of(Items.GLOWSTONE_DUST, Items.GLOWSTONE_DUST, Items.SUGAR))
                        .get(0).getAmplifier(),
                "repeated Glowstone Dust is capped at source strength tier 1");
        assertEquals(2, Brews.buildEffects(List.of(Items.GLOWSTONE_DUST, Items.BLAZE_POWDER, Items.SUGAR))
                        .get(0).getAmplifier(),
                "Blaze Powder opens source strength tier 2");
        assertEquals(3, Brews.buildEffects(List.of(Items.GLOWSTONE_DUST, Items.BLAZE_POWDER,
                        WitcheryItems.ATTUNED_STONE_CHARGED.get(), Items.SUGAR))
                        .get(0).getAmplifier(),
                "vanilla potion effects use the old default strength ceiling");
        assertEquals(4, Brews.power(List.of(Items.GLOWSTONE_DUST, Items.BLAZE_POWDER,
                        WitcheryItems.ATTUNED_STONE_CHARGED.get(), Items.SUGAR)),
                "Charged Attuned Stone contributes the third source strength modifier tier");
    }

    @Test
    void sourceDurationModifiersUseCappedTiers() {
        Brews.bootstrap();
        assertEquals(1800, Brews.buildEffects(List.of(Items.REDSTONE, Items.REDSTONE, Items.SPIDER_EYE))
                        .get(0).getDuration(),
                "repeated Redstone is capped at source duration tier 1");
        assertEquals(2700, Brews.buildEffects(List.of(Items.REDSTONE, Items.OBSIDIAN, Items.SPIDER_EYE))
                        .get(0).getDuration(),
                "Obsidian opens source duration tier 2");
        assertEquals(3600, Brews.buildEffects(List.of(Items.REDSTONE, Items.OBSIDIAN,
                        WitcheryItems.MINDRAKE_BULB.get(), Items.SPIDER_EYE))
                        .get(0).getDuration(),
                "Mindrake Bulb represents the old Mindrake seed duration tier 3");
    }

    @Test
    void fermentedSpiderEyeInverts() {
        Brews.bootstrap();
        assertSame(MobEffects.POISON.value(),
                Brews.buildEffects(List.of(Items.FERMENTED_SPIDER_EYE, Items.SPIDER_EYE)).get(0).getEffect().value(),
                "source Spider Eye has no separate inverted potion");
        assertSame(MobEffects.POISON.value(),
                Brews.buildEffects(List.of(Items.FERMENTED_SPIDER_EYE, Items.FERMENTED_SPIDER_EYE, Items.SPIDER_EYE))
                        .get(0).getEffect().value(),
                "source Fermented Spider Eye sets inversion instead of toggling it off");
        assertSame(MobEffects.POISON.value(),
                Brews.buildEffects(List.of(Items.FERMENTED_SPIDER_EYE, Items.GHAST_TEAR)).get(0).getEffect().value(),
                "Ghast Tear owns the source Regeneration/Poison inversion pair");
        assertEquals(25, Brews.powerCost(List.of(Items.FERMENTED_SPIDER_EYE, Items.SPIDER_EYE)),
                "source Fermented Spider Eye modifier costs 25 altar power");
    }

    @Test
    void sourceVanillaPotionIngredientsUseOldDurationsInversionsAndCosts() {
        Brews.bootstrap();
        List<MobEffectInstance> sugar = Brews.buildEffects(List.of(Items.SUGAR));
        assertSame(MobEffects.SPEED.value(), sugar.get(0).getEffect().value(), "Sugar makes Move Speed");
        assertEquals(3600, sugar.get(0).getDuration(), "Sugar source duration is three minutes");
        assertEquals(100, Brews.powerCost(List.of(Items.SUGAR)), "Sugar source altar cost");

        List<MobEffectInstance> slow = Brews.buildEffects(List.of(Items.FERMENTED_SPIDER_EYE, Items.SUGAR));
        assertSame(MobEffects.SLOWNESS.value(), slow.get(0).getEffect().value(), "inverted Sugar makes Slowness");
        assertEquals(1800, slow.get(0).getDuration(), "source Slowness duration is ninety seconds");
        assertEquals(125, Brews.powerCost(List.of(Items.FERMENTED_SPIDER_EYE, Items.SUGAR)),
                "inverted Sugar counts Fermented Spider Eye plus Sugar power");

        assertEquals(900, Brews.buildEffects(List.of(Items.SPIDER_EYE)).get(0).getDuration(),
                "Spider Eye source poison duration is forty-five seconds");
        assertEquals(100, Brews.powerCost(List.of(Items.PUFFERFISH)), "Pufferfish source altar cost");
        assertEquals(100, Brews.powerCost(List.of(Items.MAGMA_CREAM)), "Magma Cream source altar cost");
        assertSame(MobEffects.INVISIBILITY.value(),
                Brews.buildEffects(List.of(Items.FERMENTED_SPIDER_EYE, Items.GOLDEN_CARROT)).get(0).getEffect().value(),
                "Golden Carrot inverts to Invisibility in source");
        assertEquals(200, Brews.powerCost(List.of(Items.GOLDEN_CARROT)), "Golden Carrot source altar cost");
        assertEquals(200, Brews.powerCost(List.of(Items.GHAST_TEAR)), "Ghast Tear source altar cost");
    }

    @Test
    void glisteringMelonUsesSourceInstantHealingAndHarming() {
        Brews.bootstrap();
        List<MobEffectInstance> healing = Brews.buildEffects(List.of(Items.GLISTERING_MELON_SLICE));
        assertSame(MobEffects.INSTANT_HEALTH.value(), healing.get(0).getEffect().value(),
                "Glistering Melon makes source Healing");
        assertEquals(0, healing.get(0).getDuration(), "source Healing is instant");
        assertEquals(200, Brews.powerCost(List.of(Items.GLISTERING_MELON_SLICE)),
                "Glistering Melon source altar cost");

        List<MobEffectInstance> harming = Brews.buildEffects(List.of(Items.FERMENTED_SPIDER_EYE,
                Items.GLISTERING_MELON_SLICE));
        assertSame(MobEffects.INSTANT_DAMAGE.value(), harming.get(0).getEffect().value(),
                "inverted Glistering Melon makes source Harming");
        assertEquals(225, Brews.powerCost(List.of(Items.FERMENTED_SPIDER_EYE, Items.GLISTERING_MELON_SLICE)),
                "inverted Glistering Melon counts Fermented Spider Eye plus source effect power");

        assertEquals(1, Brews.buildEffects(List.of(Items.GLOWSTONE_DUST, Items.BLAZE_POWDER,
                        Items.GLISTERING_MELON_SLICE)).get(0).getAmplifier(),
                "source Healing/Harming keeps the old strength ceiling of one");
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
    void sourceModifierPowerCostsAreCounted() {
        Brews.bootstrap();
        assertEquals(250, Brews.powerCost(List.of(Items.GLOWSTONE_DUST, Items.BLAZE_POWDER, Items.SUGAR)),
                "Glowstone Dust plus Blaze Powder plus Sugar use source altar costs");
        assertEquals(300, Brews.powerCost(List.of(Items.REDSTONE, Items.OBSIDIAN,
                        WitcheryItems.MINDRAKE_BULB.get(), Items.SPIDER_EYE)),
                "Redstone, Obsidian, and old Mindrake seed tier costs are counted");
    }

    @Test
    void sourceDrinkSpeedModifiersAreCounted() {
        Brews.bootstrap();
        assertEquals(24, Brews.drinkDuration(List.of(WitcheryItems.ROWAN_BERRIES.get(), Items.SPIDER_EYE)),
                "Rowan Berries subtract eight ticks from the source drink time");
        assertEquals(16, Brews.drinkDuration(List.of(WitcheryItems.ROWAN_BERRIES.get(),
                        WitcheryItems.EXHALE_OF_HORNED_ONE.get(), WitcheryItems.SPANISH_MOSS.get(), Items.SPIDER_EYE)),
                "Rowan Berries, Exhale of the Horned One, and Spanish Moss stack as source fast-drink modifiers");
        assertEquals(100, Brews.powerCost(List.of(WitcheryItems.ROWAN_BERRIES.get(),
                        WitcheryItems.EXHALE_OF_HORNED_ONE.get(), WitcheryItems.SPANISH_MOSS.get(), Items.SPIDER_EYE)),
                "Rowan Berries and Spanish Moss cost 50 altar power each, while Exhale is free");
        assertTrue(Brews.requiresExplicitStir(WitcheryItems.EXHALE_OF_HORNED_ONE.get()),
                "Exhale stays available to the Mutandis cauldron trigger unless explicitly stirred");
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
        assertTrue(Brews.isIngredient(Items.BLAZE_POWDER), "Blaze Powder is a source strength modifier");
        assertTrue(Brews.requiresExplicitStir(Items.BLAZE_POWDER),
                "Blaze Powder stays available to the Infernal Chalk cauldron offering path when dropped");
    }
}
