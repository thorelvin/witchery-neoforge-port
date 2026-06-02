package com.emoniph.witchery;

import com.emoniph.witchery.brewing.Brews;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
    void modifiersResetBetweenEffects() {
        Brews.bootstrap();
        // Glowstone strengthens only the FIRST effect; the second resets to amplifier 0.
        List<MobEffectInstance> fx = Brews.buildEffects(List.of(Items.GLOWSTONE_DUST, Items.SPIDER_EYE, Items.SUGAR));
        assertEquals(2, fx.size(), "two effects");
        assertEquals(1, fx.get(0).getAmplifier(), "first strengthened");
        assertEquals(0, fx.get(1).getAmplifier(), "second reset");
    }
}
