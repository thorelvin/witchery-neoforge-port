package com.emoniph.witchery;

import com.emoniph.witchery.brewing.Brews;
import com.emoniph.witchery.effect.AttractProjectilesMobEffect;
import com.emoniph.witchery.effect.EnderInhibitionMobEffect;
import com.emoniph.witchery.effect.FeatherFallMobEffect;
import com.emoniph.witchery.effect.FloatingMobEffect;
import com.emoniph.witchery.effect.MoonshineMobEffect;
import com.emoniph.witchery.effect.PoisonWeaponsMobEffect;
import com.emoniph.witchery.effect.ReflectProjectilesMobEffect;
import com.emoniph.witchery.effect.RepellAttackerMobEffect;
import com.emoniph.witchery.effect.SnowTrailMobEffect;
import com.emoniph.witchery.effect.StoutBellyMobEffect;
import com.emoniph.witchery.effect.SwimmingMobEffect;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
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

    private static List<Item> level1(Item... ingredients) {
        return withCapacity(List.of(WitcheryItems.MANDRAKE_ROOT.get()), ingredients);
    }

    private static List<Item> level2(Item... ingredients) {
        return withCapacity(List.of(Items.NETHER_WART), ingredients);
    }

    private static List<Item> level4(Item... ingredients) {
        return withCapacity(List.of(Items.NETHER_WART, WitcheryItems.TEAR_OF_GODDESS.get()), ingredients);
    }

    private static List<Item> withCapacity(List<Item> capacity, Item... ingredients) {
        java.util.ArrayList<Item> items = new java.util.ArrayList<>(capacity);
        items.addAll(List.of(ingredients));
        return List.copyOf(items);
    }

    @Test
    void glowstoneStrengthensNextEffect() {
        Brews.bootstrap();
        List<MobEffectInstance> fx = Brews.buildEffects(level2(Items.GLOWSTONE_DUST, Items.SPIDER_EYE));
        assertEquals(1, fx.size(), "one effect");
        assertSame(MobEffects.POISON.value(), fx.get(0).getEffect().value(), "poison");
        assertEquals(1, fx.get(0).getAmplifier(), "amplifier raised by one glowstone");
    }

    @Test
    void redstoneExtendsDuration() {
        Brews.bootstrap();
        // Spider Eye source poison duration is 900 ticks; one Redstone doubles to 1800.
        assertEquals(1800, Brews.buildEffects(level2(Items.REDSTONE, Items.SPIDER_EYE)).get(0).getDuration());
    }

    @Test
    void sourceStrengthModifiersUseCappedTiers() {
        Brews.bootstrap();
        assertEquals(1, Brews.buildEffects(level2(Items.GLOWSTONE_DUST, Items.GLOWSTONE_DUST, Items.SUGAR))
                        .get(0).getAmplifier(),
                "repeated Glowstone Dust is capped at source strength tier 1");
        assertEquals(2, Brews.buildEffects(level2(Items.GLOWSTONE_DUST, Items.BLAZE_ROD, Items.SUGAR))
                        .get(0).getAmplifier(),
                "Blaze Rod opens source strength tier 2");
        assertEquals(3, Brews.buildEffects(level2(Items.GLOWSTONE_DUST, Items.BLAZE_ROD,
                        WitcheryItems.ATTUNED_STONE_CHARGED.get(), Items.SUGAR))
                        .get(0).getAmplifier(),
                "vanilla potion effects use the old default strength ceiling");
        assertEquals(4, Brews.power(level2(Items.GLOWSTONE_DUST, Items.BLAZE_ROD,
                        WitcheryItems.ATTUNED_STONE_CHARGED.get(), Items.SUGAR)),
                "Charged Attuned Stone contributes the third source strength modifier tier");
    }

    @Test
    void sourceDurationModifiersUseCappedTiers() {
        Brews.bootstrap();
        assertEquals(1800, Brews.buildEffects(level2(Items.REDSTONE, Items.REDSTONE, Items.SPIDER_EYE))
                        .get(0).getDuration(),
                "repeated Redstone is capped at source duration tier 1");
        assertEquals(2700, Brews.buildEffects(level2(Items.REDSTONE, Items.OBSIDIAN, Items.SPIDER_EYE))
                        .get(0).getDuration(),
                "Obsidian opens source duration tier 2");
        assertEquals(3600, Brews.buildEffects(level2(Items.REDSTONE, Items.OBSIDIAN,
                        WitcheryItems.MINDRAKE_BULB.get(), Items.SPIDER_EYE))
                        .get(0).getDuration(),
                "Mindrake Bulb represents the old Mindrake seed duration tier 3");
    }

    @Test
    void fermentedSpiderEyeInverts() {
        Brews.bootstrap();
        assertSame(MobEffects.POISON.value(),
                Brews.buildEffects(level2(Items.FERMENTED_SPIDER_EYE, Items.SPIDER_EYE)).get(0).getEffect().value(),
                "source Spider Eye has no separate inverted potion");
        assertSame(MobEffects.POISON.value(),
                Brews.buildEffects(level2(Items.FERMENTED_SPIDER_EYE, Items.FERMENTED_SPIDER_EYE, Items.SPIDER_EYE))
                        .get(0).getEffect().value(),
                "source Fermented Spider Eye sets inversion instead of toggling it off");
        assertSame(MobEffects.POISON.value(),
                Brews.buildEffects(level2(Items.FERMENTED_SPIDER_EYE, Items.GHAST_TEAR)).get(0).getEffect().value(),
                "Ghast Tear owns the source Regeneration/Poison inversion pair");
        assertEquals(25, Brews.powerCost(List.of(Items.FERMENTED_SPIDER_EYE, Items.SPIDER_EYE)),
                "source Fermented Spider Eye modifier costs 25 altar power");
    }

    @Test
    void sourceVanillaPotionIngredientsUseOldDurationsInversionsAndCosts() {
        Brews.bootstrap();
        List<MobEffectInstance> sugar = Brews.buildEffects(level2(Items.SUGAR));
        assertSame(MobEffects.SPEED.value(), sugar.get(0).getEffect().value(), "Sugar makes Move Speed");
        assertEquals(3600, sugar.get(0).getDuration(), "Sugar source duration is three minutes");
        assertEquals(100, Brews.powerCost(List.of(Items.SUGAR)), "Sugar source altar cost");

        List<MobEffectInstance> slow = Brews.buildEffects(level2(Items.FERMENTED_SPIDER_EYE, Items.SUGAR));
        assertSame(MobEffects.SLOWNESS.value(), slow.get(0).getEffect().value(), "inverted Sugar makes Slowness");
        assertEquals(1800, slow.get(0).getDuration(), "source Slowness duration is ninety seconds");
        assertEquals(125, Brews.powerCost(List.of(Items.FERMENTED_SPIDER_EYE, Items.SUGAR)),
                "inverted Sugar counts Fermented Spider Eye plus Sugar power");

        assertEquals(900, Brews.buildEffects(level2(Items.SPIDER_EYE)).get(0).getDuration(),
                "Spider Eye source poison duration is forty-five seconds");
        assertEquals(100, Brews.powerCost(List.of(Items.PUFFERFISH)), "Pufferfish source altar cost");
        assertEquals(100, Brews.powerCost(List.of(Items.MAGMA_CREAM)), "Magma Cream source altar cost");
        assertSame(MobEffects.INVISIBILITY.value(),
                Brews.buildEffects(level2(Items.FERMENTED_SPIDER_EYE, Items.GOLDEN_CARROT)).get(0).getEffect().value(),
                "Golden Carrot inverts to Invisibility in source");
        assertEquals(200, Brews.powerCost(List.of(Items.GOLDEN_CARROT)), "Golden Carrot source altar cost");
        assertEquals(200, Brews.powerCost(List.of(Items.GHAST_TEAR)), "Ghast Tear source altar cost");
    }

    @Test
    void glisteringMelonUsesSourceInstantHealingAndHarming() {
        Brews.bootstrap();
        List<MobEffectInstance> healing = Brews.buildEffects(level2(Items.GLISTERING_MELON_SLICE));
        assertSame(MobEffects.INSTANT_HEALTH.value(), healing.get(0).getEffect().value(),
                "Glistering Melon makes source Healing");
        assertEquals(0, healing.get(0).getDuration(), "source Healing is instant");
        assertEquals(200, Brews.powerCost(List.of(Items.GLISTERING_MELON_SLICE)),
                "Glistering Melon source altar cost");

        List<MobEffectInstance> harming = Brews.buildEffects(level2(Items.FERMENTED_SPIDER_EYE,
                Items.GLISTERING_MELON_SLICE));
        assertSame(MobEffects.INSTANT_DAMAGE.value(), harming.get(0).getEffect().value(),
                "inverted Glistering Melon makes source Harming");
        assertEquals(225, Brews.powerCost(List.of(Items.FERMENTED_SPIDER_EYE, Items.GLISTERING_MELON_SLICE)),
                "inverted Glistering Melon counts Fermented Spider Eye plus source effect power");

        assertEquals(1, Brews.buildEffects(level2(Items.GLOWSTONE_DUST, Items.BLAZE_ROD,
                        Items.GLISTERING_MELON_SLICE)).get(0).getAmplifier(),
                "source Healing/Harming keeps the old strength ceiling of one");
    }

    @Test
    void blazePowderRestoresSourceDamageBoostAndWeakness() {
        Brews.bootstrap();
        List<MobEffectInstance> strength = Brews.buildEffects(level2(Items.BLAZE_POWDER));
        assertEquals(1, strength.size(), "Blaze Powder resolves as an effect ingredient");
        assertSame(MobEffects.STRENGTH.value(), strength.get(0).getEffect().value(),
                "Blaze Powder makes source Damage Boost / Strength");
        assertEquals(3600, strength.get(0).getDuration(), "source Damage Boost lasts three minutes");
        assertEquals(200, Brews.powerCost(List.of(Items.BLAZE_POWDER)), "Blaze Powder source effect power");

        List<MobEffectInstance> weakness = Brews.buildEffects(level2(Items.FERMENTED_SPIDER_EYE, Items.BLAZE_POWDER));
        assertSame(MobEffects.WEAKNESS.value(), weakness.get(0).getEffect().value(),
                "inverted Blaze Powder makes Weakness");
        assertEquals(1800, weakness.get(0).getDuration(), "source Weakness duration is ninety seconds");
        assertEquals(225, Brews.powerCost(List.of(Items.FERMENTED_SPIDER_EYE, Items.BLAZE_POWDER)),
                "inverted Blaze Powder counts Fermented Spider Eye plus source effect power");
    }

    @Test
    void leatherRestoresSourceJumpBrew() {
        Brews.bootstrap();
        List<MobEffectInstance> jump = Brews.buildEffects(level2(Items.LEATHER));
        assertEquals(1, jump.size(), "Leather resolves as the old Jump effect ingredient");
        assertSame(MobEffects.JUMP_BOOST.value(), jump.get(0).getEffect().value(),
                "source Jump uses the vanilla Jump Boost potion");
        assertEquals(3600, jump.get(0).getDuration(), "source Jump lasts three minutes");
        assertEquals(200, Brews.powerCost(List.of(Items.LEATHER)), "Leather source effect power");

        assertEquals(1, Brews.buildEffects(level2(Items.GLOWSTONE_DUST, Items.LEATHER)).get(0).getAmplifier(),
                "source Jump accepts the normal strength modifier path");
        assertFalse(Brews.isIngredient(Items.RABBIT_FOOT),
                "Rabbit Foot is a modern vanilla brewing item, not the old Witchery Jump ingredient");
    }

    @Test
    void featherRestoresSourceFeatherFallBrew() {
        Brews.bootstrap();
        List<MobEffectInstance> featherFall = Brews.buildEffects(level2(Items.FEATHER));
        assertEquals(1, featherFall.size(), "Feather resolves as the old Feather Fall effect ingredient");
        assertSame(WitcheryMobEffects.FEATHER_FALL.value(), featherFall.get(0).getEffect().value(),
                "Feather uses Witchery's source Feather Fall effect instead of vanilla Slow Falling");
        assertEquals(1200, featherFall.get(0).getDuration(), "source Feather Fall lasts one minute");
        assertEquals(100, Brews.powerCost(List.of(Items.FEATHER)), "Feather Fall source effect power");

        assertEquals(1, Brews.buildEffects(level2(Items.GLOWSTONE_DUST, Items.FEATHER)).get(0).getAmplifier(),
                "source Feather Fall accepts the normal strength modifier path");
        assertFalse(Brews.isIngredient(Items.PHANTOM_MEMBRANE),
                "Phantom Membrane is a modern placeholder, not the old Witchery Feather Fall ingredient");
    }

    @Test
    void featherFallEffectUsesSourceStrengthThresholds() {
        assertEquals(5, FeatherFallMobEffect.activationDistance(0));
        assertEquals(4, FeatherFallMobEffect.activationDistance(1));
        assertEquals(3, FeatherFallMobEffect.activationDistance(2));

        assertEquals(6, FeatherFallMobEffect.maxFallDistance(0));
        assertEquals(5, FeatherFallMobEffect.maxFallDistance(1));
        assertEquals(4, FeatherFallMobEffect.maxFallDistance(2));
        assertEquals(3, FeatherFallMobEffect.maxFallDistance(3));
    }

    @Test
    void brambleRestoresSourceRepellAttackerBrew() {
        Brews.bootstrap();
        List<MobEffectInstance> repell = Brews.buildEffects(level2(WitcheryItems.BRAMBLE.get()));
        assertEquals(1, repell.size(), "Bramble resolves as the old Repell Attacker effect ingredient");
        assertSame(WitcheryMobEffects.REPELL_ATTACKER.value(), repell.get(0).getEffect().value(),
                "source Bramble uses Witchery's Repell Attacker potion");
        assertEquals(1800, repell.get(0).getDuration(), "source Repell Attacker lasts ninety seconds");
        assertEquals(250, Brews.powerCost(List.of(WitcheryItems.BRAMBLE.get())),
                "Repell Attacker source effect power");

        assertEquals(1, Brews.buildEffects(level2(Items.GLOWSTONE_DUST, WitcheryItems.BRAMBLE.get()))
                        .get(0).getAmplifier(),
                "source Repell Attacker accepts the normal strength modifier path");
    }

    @Test
    void sugarCaneRestoresSourceFloatingBrew() {
        Brews.bootstrap();
        List<MobEffectInstance> floating = Brews.buildEffects(level2(Items.SUGAR_CANE));
        assertEquals(1, floating.size(), "Sugar Cane resolves as the old Floating effect ingredient");
        assertSame(WitcheryMobEffects.FLOATING.value(), floating.get(0).getEffect().value(),
                "source Sugar Cane uses Witchery's Floating potion");
        assertEquals(1800, floating.get(0).getDuration(), "source Floating lasts ninety seconds");
        assertEquals(250, Brews.powerCost(List.of(Items.SUGAR_CANE)), "Floating source effect power");

        assertEquals(1, Brews.buildEffects(level2(Items.GLOWSTONE_DUST, Items.SUGAR_CANE))
                        .get(0).getAmplifier(),
                "source Floating accepts the normal strength modifier path");
    }

    @Test
    void floatingEffectUsesSourceHoverConstants() {
        assertEquals(3, FloatingMobEffect.hoverHeight(0));
        assertEquals(4, FloatingMobEffect.hoverHeight(1));
        assertEquals(5, FloatingMobEffect.hoverHeight(2));
        assertEquals(0.25D, FloatingMobEffect.RAISED_Y_VELOCITY);
        assertEquals(-0.05D, FloatingMobEffect.UNSUPPORTED_DRIFT_Y_VELOCITY);
    }

    @Test
    void redMushroomRestoresSourcePoisonWeaponsBrew() {
        Brews.bootstrap();
        List<MobEffectInstance> poisonWeapons = Brews.buildEffects(level2(Items.RED_MUSHROOM));
        assertEquals(1, poisonWeapons.size(), "Red Mushroom resolves as the old Poison Weapons effect ingredient");
        assertSame(WitcheryMobEffects.POISON_WEAPONS.value(), poisonWeapons.get(0).getEffect().value(),
                "source Red Mushroom uses Witchery's Poison Weapons potion");
        assertEquals(1800, poisonWeapons.get(0).getDuration(), "source Poison Weapons lasts ninety seconds");
        assertEquals(200, Brews.powerCost(List.of(Items.RED_MUSHROOM)), "Poison Weapons source effect power");

        assertEquals(1, Brews.buildEffects(level2(Items.GLOWSTONE_DUST, Items.RED_MUSHROOM))
                        .get(0).getAmplifier(),
                "source Poison Weapons accepts the normal strength modifier path");
    }

    @Test
    void poisonWeaponsEffectUsesSourceAttackEffectTable() {
        MobEffectInstance tier0 = PoisonWeaponsMobEffect.attackEffect(0);
        assertSame(MobEffects.POISON.value(), tier0.getEffect().value());
        assertEquals(100, tier0.getDuration());
        assertEquals(0, tier0.getAmplifier());

        MobEffectInstance tier1 = PoisonWeaponsMobEffect.attackEffect(1);
        assertSame(MobEffects.POISON.value(), tier1.getEffect().value());
        assertEquals(100, tier1.getDuration());
        assertEquals(1, tier1.getAmplifier());

        MobEffectInstance tier2 = PoisonWeaponsMobEffect.attackEffect(2);
        assertSame(MobEffects.POISON.value(), tier2.getEffect().value());
        assertEquals(300, tier2.getDuration());
        assertEquals(1, tier2.getAmplifier());

        MobEffectInstance tier3 = PoisonWeaponsMobEffect.attackEffect(3);
        assertSame(MobEffects.WITHER.value(), tier3.getEffect().value());
        assertEquals(400, tier3.getDuration());
        assertEquals(0, tier3.getAmplifier());
    }

    @Test
    void cobwebRestoresSourceProjectileBrewPair() {
        Brews.bootstrap();
        List<MobEffectInstance> reflect = Brews.buildEffects(level2(Items.COBWEB));
        assertEquals(1, reflect.size(), "Cobweb resolves as the old Reflect Projectiles effect ingredient");
        assertSame(WitcheryMobEffects.REFLECT_PROJECTILES.value(), reflect.get(0).getEffect().value(),
                "source Cobweb uses Witchery's Reflect Projectiles potion");
        assertEquals(1800, reflect.get(0).getDuration(), "source Reflect Projectiles lasts ninety seconds");
        assertEquals(250, Brews.powerCost(List.of(Items.COBWEB)), "Reflect Projectiles source effect power");

        List<MobEffectInstance> attract = Brews.buildEffects(level2(Items.FERMENTED_SPIDER_EYE, Items.COBWEB));
        assertSame(WitcheryMobEffects.ATTRACT_PROJECTILES.value(), attract.get(0).getEffect().value(),
                "inverted source Cobweb uses Witchery's Attract Projectiles potion");
        assertEquals(900, attract.get(0).getDuration(), "source Attract Projectiles lasts forty-five seconds");
        assertEquals(275, Brews.powerCost(List.of(Items.FERMENTED_SPIDER_EYE, Items.COBWEB)),
                "inverted Cobweb counts Fermented Spider Eye plus source effect power");
    }

    @Test
    void enderDewRestoresSourceEnderInhibitionBrew() {
        Brews.bootstrap();
        List<MobEffectInstance> effects = Brews.buildEffects(level1(WitcheryItems.ENDER_DEW.get()));
        assertEquals(1, effects.size(), "Ender Dew resolves as the old Ender Inhibition effect ingredient");
        assertSame(WitcheryMobEffects.ENDER_INHIBITION.value(), effects.get(0).getEffect().value(),
                "source Ender Dew uses Witchery's Ender Inhibition potion");
        assertEquals(1800, effects.get(0).getDuration(), "source Ender Inhibition lasts ninety seconds");
        assertEquals(200, Brews.powerCost(List.of(WitcheryItems.ENDER_DEW.get())),
                "Ender Inhibition source effect power");
    }

    @Test
    void skullAndApplesRestoreSourceVanillaEffectBrews() {
        Brews.bootstrap();
        List<MobEffectInstance> wither = Brews.buildEffects(level4(Items.WITHER_SKELETON_SKULL));
        assertEquals(1, wither.size(), "Wither Skeleton Skull resolves as the old Wither brew ingredient");
        assertSame(MobEffects.WITHER.value(), wither.get(0).getEffect().value(),
                "source skull metadata 1 makes vanilla Wither");
        assertEquals(300, wither.get(0).getDuration(), "source Wither brew lasts fifteen seconds");
        assertEquals(200, Brews.powerCost(List.of(Items.WITHER_SKELETON_SKULL)), "Wither source effect power");

        List<MobEffectInstance> absorption = Brews.buildEffects(level4(Items.GOLDEN_APPLE));
        assertSame(MobEffects.ABSORPTION.value(), absorption.get(0).getEffect().value(),
                "source Golden Apple metadata 0 makes Absorption");
        assertEquals(600, absorption.get(0).getDuration(), "source Absorbsion brew lasts thirty seconds");
        assertEquals(1000, Brews.powerCost(List.of(Items.GOLDEN_APPLE)), "Absorption source effect power");

        List<MobEffectInstance> healthBoost = Brews.buildEffects(level4(Items.ENCHANTED_GOLDEN_APPLE));
        assertSame(MobEffects.HEALTH_BOOST.value(), healthBoost.get(0).getEffect().value(),
                "source Golden Apple metadata 1 maps to modern Enchanted Golden Apple and Health Boost");
        assertEquals(2400, healthBoost.get(0).getDuration(), "source Health Boost brew lasts two minutes");
        assertEquals(1000, Brews.powerCost(List.of(Items.ENCHANTED_GOLDEN_APPLE)),
                "Health Boost source effect power");

        assertEquals(1, Brews.buildEffects(level4(Items.GLOWSTONE_DUST, Items.WITHER_SKELETON_SKULL))
                        .get(0).getAmplifier(),
                "these vanilla effect brews use the normal source strength modifier path");
    }

    @Test
    void inkSacRestoresSourceBlindnessBrew() {
        Brews.bootstrap();
        List<MobEffectInstance> blindness = Brews.buildEffects(level4(Items.INK_SAC));
        assertEquals(1, blindness.size(), "Ink Sac resolves as the old Blindness brew ingredient");
        assertSame(MobEffects.BLINDNESS.value(), blindness.get(0).getEffect().value(),
                "source Dye.INK_SAC makes vanilla Blindness");
        assertEquals(300, blindness.get(0).getDuration(), "source Blindness brew lasts fifteen seconds");
        assertEquals(1000, Brews.powerCost(List.of(Items.INK_SAC)), "Blindness source effect power");

        assertEquals(1, Brews.buildEffects(level4(Items.GLOWSTONE_DUST, Items.INK_SAC)).get(0).getAmplifier(),
                "source Blindness accepts the normal strength modifier path");
    }

    @Test
    void enderInhibitionUsesSourceAmplifierGate() {
        assertFalse(EnderInhibitionMobEffect.canTeleport(0, 0),
                "an active level-I effect blocks ordinary ender teleport checks");
        assertTrue(EnderInhibitionMobEffect.canTeleport(0, 1),
                "source helper only blocks checks at or below the active amplifier");
        assertFalse(EnderInhibitionMobEffect.canTeleport(2, 2));
        assertTrue(EnderInhibitionMobEffect.canTeleport(2, 3));
    }

    @Test
    void wheatRestoresSourceMoonshineBrew() {
        Brews.bootstrap();
        List<MobEffectInstance> moonshine = Brews.buildEffects(level1(Items.WHEAT));
        assertEquals(1, moonshine.size(), "Wheat resolves as the old Moonshine effect ingredient");
        assertSame(WitcheryMobEffects.MOONSHINE.value(), moonshine.get(0).getEffect().value(),
                "source Moonshine uses Witchery's Feel No Pain potion behavior");
        assertEquals(1800, moonshine.get(0).getDuration(), "source Moonshine lasts ninety seconds");
        assertEquals(0, Brews.powerCost(List.of(Items.WHEAT)), "Moonshine has no source altar cost");

        assertEquals(1, Brews.buildEffects(level1(Items.GLOWSTONE_DUST, Items.WHEAT)).get(0).getAmplifier(),
                "source Moonshine accepts the normal strength modifier path");
        assertEquals(50, Brews.powerCost(List.of(Items.GLOWSTONE_DUST, Items.WHEAT)),
                "only the source Glowstone modifier costs altar power");
    }

    @Test
    void wolfsbaneRestoresSourceBrewMarker() {
        Brews.bootstrap();
        List<MobEffectInstance> wolfsbane = Brews.buildEffects(level1(WitcheryItems.WOLFSBANE_FLOWER.get()));
        assertEquals(1, wolfsbane.size(), "Wolfsbane resolves as the old source potion marker");
        assertSame(WitcheryMobEffects.WOLFSBANE.value(), wolfsbane.get(0).getEffect().value(),
                "source Wolfsbane uses Witchery's plain Wolfsbane potion marker");
        assertEquals(1200, wolfsbane.get(0).getDuration(), "source Wolfsbane lasts sixty seconds");
        assertEquals(0, Brews.powerCost(List.of(WitcheryItems.WOLFSBANE_FLOWER.get())),
                "Wolfsbane has no source altar cost");
    }

    @Test
    void snowballRestoresSourceSnowTrailBrew() {
        Brews.bootstrap();
        List<MobEffectInstance> snowTrail = Brews.buildEffects(level1(Items.SNOWBALL));
        assertEquals(1, snowTrail.size(), "Snowball resolves as the old Snow Trail effect ingredient");
        assertSame(WitcheryMobEffects.SNOW_TRAIL.value(), snowTrail.get(0).getEffect().value(),
                "source Snowball uses Witchery's Snow Trail potion");
        assertEquals(3600, snowTrail.get(0).getDuration(), "source Snow Trail lasts three minutes");
        assertEquals(0, Brews.powerCost(List.of(Items.SNOWBALL)), "Snow Trail has no source altar cost");

        assertEquals(1, Brews.buildEffects(level1(Items.GLOWSTONE_DUST, Items.SNOWBALL)).get(0).getAmplifier(),
                "source Snow Trail accepts the normal strength modifier path");
    }

    @Test
    void snowTrailEffectUsesSourceConstants() {
        SnowTrailMobEffect effect = new SnowTrailMobEffect(0x3F825D);
        assertTrue(effect.shouldApplyEffectTickThisTick(20, 0));
        assertFalse(effect.shouldApplyEffectTickThisTick(21, 0));
        assertEquals(10, SnowTrailMobEffect.TICK_INTERVAL);
        assertEquals(0.25D, SnowTrailMobEffect.footprintOffsetX(1));
        assertEquals(-0.25D, SnowTrailMobEffect.footprintOffsetX(2));
        assertEquals(-0.25D, SnowTrailMobEffect.footprintOffsetZ(1));
        assertEquals(0.25D, SnowTrailMobEffect.footprintOffsetZ(2));
        assertTrue(SnowTrailMobEffect.allowsSnowTrail(1.599F));
        assertFalse(SnowTrailMobEffect.allowsSnowTrail(1.6F));
        assertEquals(8, SnowTrailMobEffect.SNOW_GOLEM_COVER_RADIUS);
        assertEquals(3.0F, SnowTrailMobEffect.SNOW_GOLEM_EXPLOSION_POWER);
        assertEquals(20, SnowTrailMobEffect.SNOW_GOLEM_EXPLOSION_ROLL);
    }

    @Test
    void codRestoresSourceSwimBoostBrew() {
        Brews.bootstrap();
        List<MobEffectInstance> swimming = Brews.buildEffects(level1(Items.COD));
        assertEquals(1, swimming.size(), "Cod resolves as the old raw-fish Swim Boost effect ingredient");
        assertSame(WitcheryMobEffects.SWIMMING.value(), swimming.get(0).getEffect().value(),
                "source raw fish metadata 0 uses Witchery's Swimming potion");
        assertEquals(3600, swimming.get(0).getDuration(), "source Swim Boost lasts three minutes");
        assertEquals(0, Brews.powerCost(List.of(Items.COD)), "Swim Boost has no source altar cost");

        assertEquals(1, Brews.buildEffects(level1(Items.GLOWSTONE_DUST, Items.COD)).get(0).getAmplifier(),
                "source Swim Boost accepts the normal strength modifier path");
    }

    @Test
    void swimBoostEffectUsesSourceMotionConstants() {
        assertEquals(1.24D, SwimmingMobEffect.horizontalMultiplier(SwimmingMobEffect.PLAYER_SOURCE_STRENGTH),
                0.000001D);
        assertEquals(new Vec3(1.24D, 2.0D, -2.48D),
                SwimmingMobEffect.boostHorizontal(new Vec3(1.0D, 2.0D, -2.0D),
                        SwimmingMobEffect.PLAYER_SOURCE_STRENGTH));
        assertTrue(SwimmingMobEffect.shouldBoostPlayer(true, true, false));
        assertFalse(SwimmingMobEffect.shouldBoostPlayer(false, true, false));
        assertFalse(SwimmingMobEffect.shouldBoostPlayer(true, false, false));
        assertFalse(SwimmingMobEffect.shouldBoostPlayer(true, true, true));
        assertTrue(SwimmingMobEffect.shouldBoostNonPlayer(13L, true, true));
        assertFalse(SwimmingMobEffect.shouldBoostNonPlayer(12L, true, true));
    }

    @Test
    void foulFumeRestoresSourceStoutBellyBrew() {
        Brews.bootstrap();
        List<MobEffectInstance> stoutBelly = Brews.buildEffects(level4(WitcheryItems.FOUL_FUME.get()));
        assertEquals(1, stoutBelly.size(), "Foul Fume resolves as the old Stout Belly effect ingredient");
        assertSame(WitcheryMobEffects.STOUT_BELLY.value(), stoutBelly.get(0).getEffect().value(),
                "source Foul Fume uses Witchery's Stout Belly potion");
        assertEquals(1800, stoutBelly.get(0).getDuration(), "source Stout Belly lasts ninety seconds");
        assertEquals(1000, Brews.powerCost(List.of(WitcheryItems.FOUL_FUME.get())),
                "Stout Belly source effect power");

        assertEquals(1, Brews.buildEffects(level4(Items.GLOWSTONE_DUST, WitcheryItems.FOUL_FUME.get()))
                        .get(0).getAmplifier(),
                "source Stout Belly accepts the normal strength modifier path");
    }

    @Test
    void moonshineEffectUsesSourceDamageAndHungerRules() {
        assertEquals(8, MoonshineMobEffect.foodPenalty(4.0F, 0, 20),
                "level I doubles damage into food loss with a minimum of three");
        assertEquals(4, MoonshineMobEffect.foodPenalty(4.0F, 1, 20),
                "level II uses damage divided by amplifier with a minimum of two");
        assertEquals(2, MoonshineMobEffect.foodPenalty(4.0F, 2, 20),
                "level III uses damage divided by amplifier with a minimum of one");
        assertEquals(1, MoonshineMobEffect.foodPenalty(0.5F, 2, 20),
                "amplifier above one keeps the old one-food minimum");
        assertEquals(3, MoonshineMobEffect.foodPenalty(10.0F, 0, 3),
                "food loss is capped by current food");
        assertEquals(0, MoonshineMobEffect.foodPenalty(10.0F, 0, 0),
                "source only cancels hurt when the player has food");

        assertFalse(MoonshineMobEffect.shouldRollHunger(2L, 0, false, false),
                "base Moonshine has no random Hunger side effect");
        assertTrue(MoonshineMobEffect.shouldRollHunger(2L, 1, false, false),
                "amplified Moonshine rolls Hunger on source tick phase two");
        assertFalse(MoonshineMobEffect.shouldRollHunger(3L, 1, false, false));
        assertFalse(MoonshineMobEffect.shouldRollHunger(2L, 1, true, false));
        assertFalse(MoonshineMobEffect.shouldRollHunger(2L, 1, false, true));
        assertEquals(160, MoonshineMobEffect.hungerDurationTicks(1));
        assertEquals(240, MoonshineMobEffect.hungerDurationTicks(3));
        assertEquals(4, MoonshineMobEffect.hungerRollBound(1));
        assertEquals(2, MoonshineMobEffect.hungerRollBound(3));
        assertEquals(2, MoonshineMobEffect.hungerRollBound(4));
    }

    @Test
    void stoutBellyEffectUsesSourceHungerGate() {
        assertFalse(StoutBellyMobEffect.shouldRemoveHunger(3L, 0, true),
                "source Stout Belly I does not clear Hunger because amplifier must be above zero");
        assertTrue(StoutBellyMobEffect.shouldRemoveHunger(3L, 1, true),
                "amplified Stout Belly clears Hunger on source tick phase three");
        assertFalse(StoutBellyMobEffect.shouldRemoveHunger(2L, 1, true));
        assertFalse(StoutBellyMobEffect.shouldRemoveHunger(3L, 1, false));
    }

    @Test
    void projectileEffectsUseSourceConstants() {
        assertEquals(2.0D, ReflectProjectilesMobEffect.RADIUS);
        assertEquals(-0.25D, ReflectProjectilesMobEffect.reflectFactor(0));
        assertEquals(-0.5D, ReflectProjectilesMobEffect.reflectFactor(1));
        assertEquals(new Vec3(0.25D, -0.5D, -0.75D),
                ReflectProjectilesMobEffect.reflectedMotion(new Vec3(-1.0D, 2.0D, 3.0D), 0),
                "source reflect flips Y only after X/Z direction check");
        assertEquals(new Vec3(-0.25D, 2.0D, 0.75D),
                ReflectProjectilesMobEffect.reflectedMotion(new Vec3(1.0D, 2.0D, -3.0D), 0),
                "source reflect leaves Y alone when reflected X and original Z are not positive");

        assertEquals(3.0D, AttractProjectilesMobEffect.radius(0));
        assertEquals(6.0D, AttractProjectilesMobEffect.radius(1));
        assertTrue(AttractProjectilesMobEffect.canRedirect(1, 0.26D),
                "fast projectiles redirect after one tick");
        assertFalse(AttractProjectilesMobEffect.canRedirect(9, 0.25D),
                "slow projectiles wait until ten ticks");
        assertTrue(AttractProjectilesMobEffect.canRedirect(10, 0.25D));
    }

    @Test
    void repellAttackerEffectUsesSourcePushScaling() {
        assertEquals(9.0D, RepellAttackerMobEffect.MAX_RANGE_SQ);
        assertEquals(1.0D, RepellAttackerMobEffect.horizontalStrength(0));
        assertEquals(1.75D, RepellAttackerMobEffect.horizontalStrength(1));
        assertEquals(0.5D, RepellAttackerMobEffect.verticalStrength(0));
        assertEquals(0.7D, RepellAttackerMobEffect.verticalStrength(1));
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
        assertEquals(12464176, Brews.color(level2(Items.RED_WOOL, Items.SPIDER_EYE)),
                "red wool uses the old Witchery dye RGB");
        assertEquals(2247599, Brews.color(level2(Items.RED_WOOL, Items.BLUE_WOOL, Items.SPIDER_EYE)),
                "latest wool colour modifier wins");
    }

    @Test
    void modifiersResetBetweenEffects() {
        Brews.bootstrap();
        // Glowstone strengthens only the FIRST effect; the second resets to amplifier 0.
        List<MobEffectInstance> fx = Brews.buildEffects(level4(Items.GLOWSTONE_DUST, Items.SPIDER_EYE, Items.SUGAR));
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
    void sourceCapacityIngredientsGatePotionEffects() {
        Brews.bootstrap();
        Item tear = WitcheryItems.TEAR_OF_GODDESS.get();

        assertFalse(Brews.canAdd(List.of(), Items.SPIDER_EYE),
                "source level-2 potion effects cannot be stirred before any capacity modifier");
        assertFalse(Brews.canAdd(List.of(WitcheryItems.MANDRAKE_ROOT.get()), Items.SPIDER_EYE),
                "Mandrake Root alone only opens level 1, below Spider Eye's level-2 cost");
        assertTrue(Brews.canAdd(List.of(Items.NETHER_WART), Items.SPIDER_EYE),
                "Nether Wart opens enough level capacity for common level-2 potion effects");
        assertFalse(Brews.canAdd(List.of(Items.NETHER_WART), Items.INK_SAC),
                "Nether Wart alone is below the source level-4 Blindness cost");
        assertTrue(Brews.canAdd(List.of(Items.NETHER_WART, tear), Items.INK_SAC),
                "Nether Wart plus Tear of the Goddess reaches source level 4");

        assertTrue(Brews.buildEffects(List.of(Items.SPIDER_EYE)).isEmpty(),
                "under-capacity potion effects resolve inertly like source application skips");
        assertTrue(Brews.blockedByCapacity(List.of(Items.SPIDER_EYE)));
        assertEquals(2, Brews.availableCapacity(level2()));
        assertEquals(2, Brews.usedCapacity(level2(Items.SPIDER_EYE)));
        assertEquals(0, Brews.remainingCapacity(level2(Items.SPIDER_EYE)));
    }

    @Test
    void sourceModifierPowerCostsAreCounted() {
        Brews.bootstrap();
        assertEquals(250, Brews.powerCost(List.of(Items.GLOWSTONE_DUST, Items.BLAZE_ROD, Items.SUGAR)),
                "Glowstone Dust plus Blaze Rod plus Sugar use source altar costs");
        assertEquals(300, Brews.powerCost(List.of(Items.REDSTONE, Items.OBSIDIAN,
                        WitcheryItems.MINDRAKE_BULB.get(), Items.SPIDER_EYE)),
                "Redstone, Obsidian, and old Mindrake seed tier costs are counted");
    }

    @Test
    void sourceDrinkSpeedModifiersAreCounted() {
        Brews.bootstrap();
        assertEquals(24, Brews.drinkDuration(level2(WitcheryItems.ROWAN_BERRIES.get(), Items.SPIDER_EYE)),
                "Rowan Berries subtract eight ticks from the source drink time");
        assertEquals(16, Brews.drinkDuration(level2(WitcheryItems.ROWAN_BERRIES.get(),
                        WitcheryItems.EXHALE_OF_HORNED_ONE.get(), WitcheryItems.SPANISH_MOSS.get(), Items.SPIDER_EYE)),
                "Rowan Berries, Exhale of the Horned One, and Spanish Moss stack as source fast-drink modifiers");
        assertEquals(100, Brews.powerCost(List.of(WitcheryItems.ROWAN_BERRIES.get(),
                        WitcheryItems.EXHALE_OF_HORNED_ONE.get(), WitcheryItems.SPANISH_MOSS.get(), Items.SPIDER_EYE)),
                "Rowan Berries and Spanish Moss cost 50 altar power each, while Exhale is free");
        assertTrue(Brews.requiresExplicitStir(WitcheryItems.EXHALE_OF_HORNED_ONE.get()),
                "Exhale stays available to the Mutandis cauldron trigger unless explicitly stirred");
    }

    @Test
    void sourceImpactExtentModifiersUseCappedTiers() {
        Brews.bootstrap();
        assertEquals(1, Brews.impactExtent(level2(WitcheryItems.WOOD_ASH.get(), WitcheryItems.WOOD_ASH.get(),
                        Items.SPIDER_EYE)),
                "repeated Wood Ash is capped at source extent tier 1");
        assertEquals(2, Brews.impactExtent(level2(WitcheryItems.WOOD_ASH.get(), Items.COCOA_BEANS,
                        Items.SPIDER_EYE)),
                "Cocoa Beans open source extent tier 2");
        assertEquals(3, Brews.impactExtent(level2(WitcheryItems.WOOD_ASH.get(), Items.COCOA_BEANS,
                        WitcheryItems.WISPY_COTTON.get(), Items.SPIDER_EYE)),
                "Wispy Cotton opens source extent tier 3");
        assertEquals(400, Brews.powerCost(List.of(WitcheryItems.WOOD_ASH.get(), Items.COCOA_BEANS,
                        WitcheryItems.WISPY_COTTON.get(), Items.SUGAR)),
                "extent tiers count source altar costs alongside the potion effect");
    }

    @Test
    void sourceNoParticlesModifierMarksNextEffectAmbientOnly() {
        Brews.bootstrap();
        List<MobEffectInstance> fx = Brews.buildEffects(level4(Items.GOLD_NUGGET, Items.SPIDER_EYE, Items.SUGAR));
        assertEquals(2, fx.size(), "Gold Nugget modifies the next effect without removing later effects");
        assertTrue(fx.get(0).isAmbient(), "source noParticles flag maps to the old ambient potion-effect flag");
        assertTrue(fx.get(0).isVisible(), "Resurrected preserved visible potion particles while marking the effect ambient");
        assertTrue(fx.get(0).showIcon(), "the modern tooltip/status icon remains visible");
        assertFalse(fx.get(1).isAmbient(), "the noParticles/ambient flag resets after the modified effect");
        assertEquals(150, Brews.powerCost(List.of(Items.GOLD_NUGGET, Items.SUGAR)),
                "Gold Nugget costs 50 altar power on top of the source Sugar cost");
        assertTrue(Brews.requiresExplicitStir(Items.GOLD_NUGGET),
                "Gold Nugget stays available to Golden Chalk cauldron offerings unless explicitly stirred");
        assertFalse(Brews.canAddAutomatically(List.of(), Items.GOLD_NUGGET),
                "dropped Gold Nugget must remain available to hardcoded cauldron offerings");
    }

    @Test
    void sourceDisablePowerLimitBypassesEffectStrengthCeiling() {
        Brews.bootstrap();
        assertEquals(1, Brews.buildEffects(level2(Items.GLOWSTONE_DUST, Items.BLAZE_ROD,
                        Items.GLISTERING_MELON_SLICE)).get(0).getAmplifier(),
                "source Healing/Harming normally caps at strength ceiling one");

        List<MobEffectInstance> fx = Brews.buildEffects(level2(Items.TROPICAL_FISH, Items.GLOWSTONE_DUST,
                Items.BLAZE_ROD, WitcheryItems.ATTUNED_STONE_CHARGED.get(), Items.GLISTERING_MELON_SLICE));
        assertEquals(3, fx.get(0).getAmplifier(),
                "old fish metadata 2 disables the effect strength ceiling for the following potion effect");
        assertEquals(700, Brews.powerCost(List.of(Items.TROPICAL_FISH, Items.GLOWSTONE_DUST,
                        Items.BLAZE_ROD, WitcheryItems.ATTUNED_STONE_CHARGED.get(), Items.GLISTERING_MELON_SLICE)),
                "the secret source ceiling-bypass modifier costs 200 altar power");
    }

    @Test
    void sourceDisablePowerLimitPersistsThroughEffectReset() {
        Brews.bootstrap();
        List<MobEffectInstance> fx = Brews.buildEffects(level4(Items.TROPICAL_FISH,
                Items.GLOWSTONE_DUST, Items.BLAZE_ROD, Items.GLISTERING_MELON_SLICE,
                Items.GLOWSTONE_DUST, Items.BLAZE_ROD, Items.GLISTERING_MELON_SLICE));
        assertEquals(2, fx.size(), "two Healing effects resolve");
        assertEquals(2, fx.get(0).getAmplifier(),
                "the first Healing effect uses the disabled ceiling");
        assertEquals(2, fx.get(1).getAmplifier(),
                "source ModifiersEffect.reset does not clear strengthCeilingDisabled");
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
        assertTrue(Brews.isIngredient(Items.BLAZE_ROD), "Blaze Rod is the source strength tier 2 modifier");
        assertFalse(Brews.requiresExplicitStir(Items.BLAZE_ROD),
                "Blaze Rod has no current cauldron-offering conflict");
        assertTrue(Brews.canAddAutomatically(List.of(), Items.BLAZE_ROD),
                "dropped Blaze Rod can auto-stir as the source strength modifier");
        assertTrue(Brews.isIngredient(Items.BLAZE_POWDER), "Blaze Powder is the source Damage Boost effect");
        assertTrue(Brews.requiresExplicitStir(Items.BLAZE_POWDER),
                "Blaze Powder stays available to the Infernal Chalk cauldron offering path when dropped");
        assertFalse(Brews.canAddAutomatically(List.of(), Items.BLAZE_POWDER),
                "dropped Blaze Powder must remain available to hardcoded cauldron offerings");
    }
}
