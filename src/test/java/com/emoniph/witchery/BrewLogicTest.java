package com.emoniph.witchery;

import com.emoniph.witchery.brewing.Brews;
import com.emoniph.witchery.effect.AttractProjectilesMobEffect;
import com.emoniph.witchery.effect.ChilledMobEffect;
import com.emoniph.witchery.effect.DiseasedMobEffect;
import com.emoniph.witchery.effect.EnderInhibitionMobEffect;
import com.emoniph.witchery.effect.FeatherFallMobEffect;
import com.emoniph.witchery.effect.FloatingMobEffect;
import com.emoniph.witchery.effect.FortuneMobEffect;
import com.emoniph.witchery.effect.GrotesqueMobEffect;
import com.emoniph.witchery.effect.MoonshineMobEffect;
import com.emoniph.witchery.effect.ParalyzedMobEffect;
import com.emoniph.witchery.effect.PoisonWeaponsMobEffect;
import com.emoniph.witchery.effect.QueasyMobEffect;
import com.emoniph.witchery.effect.ReflectProjectilesMobEffect;
import com.emoniph.witchery.effect.RepellAttackerMobEffect;
import com.emoniph.witchery.effect.SnowTrailMobEffect;
import com.emoniph.witchery.effect.StoutBellyMobEffect;
import com.emoniph.witchery.effect.SunAllergyMobEffect;
import com.emoniph.witchery.effect.SwimmingMobEffect;
import com.emoniph.witchery.entity.LeonardEntity;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
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

    private static List<Item> level6(Item... ingredients) {
        return withCapacity(List.of(WitcheryItems.PENTACLE.get()), ingredients);
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
        assertEquals(2.0F, SnowTrailMobEffect.sourceHeightAdjustedTemperature(2.0F, 64, 0.0F));
        assertEquals(1.6F, SnowTrailMobEffect.sourceHeightAdjustedTemperature(2.0F, 304, 0.0F), 0.000001F,
                "source high-altitude temperature adjustment uses the old y > 64 formula");
        BlockPos noiseSample = new BlockPos(16, 304, 24);
        float sourceNoise = SnowTrailMobEffect.sourceTemperatureNoise(noiseSample);
        assertTrue(Math.abs(sourceNoise) <= 1.0F,
                "source temperature noise should stay in the vanilla simplex range");
        assertEquals(sourceNoise, SnowTrailMobEffect.sourceTemperatureNoise(noiseSample),
                "source temperature noise is deterministic for a coordinate");
        assertEquals(2.0F - (sourceNoise * 4.0F + 240.0F) * 0.05F / 30.0F,
                SnowTrailMobEffect.sourceHeightAdjustedTemperature(2.0F, 304, sourceNoise), 0.000001F,
                "source temperature noise is scaled with the old 1.7 snow-trail formula");
        assertTrue(SnowTrailMobEffect.sourceFootprintAllowsSnow(true, 1.599F, true),
                "source footprints need air, cool-enough biome temperature, and snow survivability");
        assertFalse(SnowTrailMobEffect.sourceFootprintAllowsSnow(false, 1.599F, true));
        assertFalse(SnowTrailMobEffect.sourceFootprintAllowsSnow(true, 1.6F, true));
        assertFalse(SnowTrailMobEffect.sourceFootprintAllowsSnow(true, 1.599F, false));
        assertEquals(8, SnowTrailMobEffect.SNOW_COVERING_SEARCH_LIMIT,
                "source snow covering searches i = 1..7");
        assertTrue(SnowTrailMobEffect.sourceSnowCoveringSupportAllowsSnow(true, false),
                "source snow covering accepts opaque-cube support");
        assertTrue(SnowTrailMobEffect.sourceSnowCoveringSupportAllowsSnow(false, true),
                "source snow covering accepts ice-material support");
        assertFalse(SnowTrailMobEffect.sourceSnowCoveringSupportAllowsSnow(false, false));
        assertEquals(8, SnowTrailMobEffect.SNOW_GOLEM_COVER_RADIUS);
        assertEquals(3.0F, SnowTrailMobEffect.SNOW_GOLEM_EXPLOSION_POWER);
        assertEquals(20, SnowTrailMobEffect.SNOW_GOLEM_EXPLOSION_ROLL);
        assertEquals(2, Brews.sourceBlockHitRadius(0),
                "source Snow Trail block-hit burst starts at radius 2");
        assertEquals(4, Brews.sourceBlockHitRadius(1),
                "Glowstone strength expands source block-hit bursts by two blocks");
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
    void clayBallRestoresSourceFortuneBrew() {
        Brews.bootstrap();
        List<MobEffectInstance> fortune = Brews.buildEffects(level6(Items.CLAY_BALL));
        assertEquals(1, fortune.size(), "Clay Ball resolves as the old Fortune effect ingredient");
        assertSame(WitcheryMobEffects.FORTUNE.value(), fortune.get(0).getEffect().value(),
                "source Clay Ball uses Witchery's Fortune potion");
        assertEquals(3600, fortune.get(0).getDuration(), "source Fortune lasts three minutes");
        assertEquals(2000, Brews.powerCost(level6(Items.CLAY_BALL)),
                "Fortune source effect power plus Pentacle capacity power");
        assertTrue(Brews.blockedByCapacity(level4(Items.CLAY_BALL)),
                "source Fortune requires effect level 6 capacity");

        assertEquals(1, Brews.buildEffects(level6(Items.GLOWSTONE_DUST, Items.CLAY_BALL)).get(0).getAmplifier(),
                "source Fortune accepts the normal strength modifier path");
    }

    @Test
    void fortuneEffectUsesSourceDropRules() {
        assertTrue(FortuneMobEffect.shouldReplaceDrops(false, false, true));
        assertFalse(FortuneMobEffect.shouldReplaceDrops(true, false, true));
        assertFalse(FortuneMobEffect.shouldReplaceDrops(false, true, true));
        assertFalse(FortuneMobEffect.shouldReplaceDrops(false, false, false));
        assertEquals(1, FortuneMobEffect.sourceFortuneBonus(0));
        assertEquals(1, FortuneMobEffect.sourceFortuneBonus(2));
        assertEquals(2, FortuneMobEffect.sourceFortuneBonus(3));
        assertEquals(4, FortuneMobEffect.boostedFortuneLevel(2, 3));
    }

    @Test
    void salmonRestoresSourceSunAllergyBrew() {
        Brews.bootstrap();
        List<MobEffectInstance> sunAllergy = Brews.buildEffects(level6(Items.SALMON));
        assertEquals(1, sunAllergy.size(), "Salmon resolves as the old fish-metadata-1 Sun Allergy ingredient");
        assertSame(WitcheryMobEffects.SUN_ALLERGY.value(), sunAllergy.get(0).getEffect().value(),
                "source fish metadata 1 uses Witchery's Sun Allergy potion");
        assertEquals(1200, sunAllergy.get(0).getDuration(), "source Sun Allergy lasts sixty seconds");
        assertEquals(2000, Brews.powerCost(level6(Items.SALMON)),
                "Sun Allergy source effect power plus Pentacle capacity power");
        assertTrue(Brews.blockedByCapacity(level4(Items.SALMON)),
                "source Sun Allergy requires effect level 6 capacity");

        assertEquals(1, Brews.buildEffects(level6(Items.GLOWSTONE_DUST, Items.SALMON)).get(0).getAmplifier(),
                "source Sun Allergy accepts the normal strength modifier path");
    }

    @Test
    void sunAllergyEffectUsesSourceDaylightRules() {
        assertTrue(SunAllergyMobEffect.shouldApplySun(40L, true, 0.9F, false, true, 0.44F));
        assertFalse(SunAllergyMobEffect.shouldApplySun(41L, true, 0.9F, false, true, 0.44F));
        assertFalse(SunAllergyMobEffect.shouldApplySun(40L, false, 0.9F, false, true, 0.44F));
        assertFalse(SunAllergyMobEffect.shouldApplySun(40L, true, 0.5F, false, true, 0.0F));
        assertFalse(SunAllergyMobEffect.shouldApplySun(40L, true, 0.9F, true, true, 0.44F));
        assertFalse(SunAllergyMobEffect.shouldApplySun(40L, true, 0.9F, false, false, 0.44F));
        assertFalse(SunAllergyMobEffect.shouldApplySun(40L, true, 0.9F, false, true, 0.45F));
        assertEquals(1.0F, SunAllergyMobEffect.playerDamage(2));
        assertEquals(2.0F, SunAllergyMobEffect.playerDamage(3));
    }

    @Test
    void rottenFleshRestoresSourceDiseaseBrew() {
        Brews.bootstrap();
        List<MobEffectInstance> disease = Brews.buildEffects(level4(Items.ROTTEN_FLESH));
        assertEquals(1, disease.size(), "Rotten Flesh resolves as the old Disease ingredient");
        assertSame(WitcheryMobEffects.DISEASED.value(), disease.get(0).getEffect().value(),
                "source Rotten Flesh uses Witchery's Diseased potion");
        assertEquals(DiseasedMobEffect.BREW_DURATION_TICKS, disease.get(0).getDuration(),
                "source Disease lasts three minutes");
        assertEquals(2150, Brews.powerCost(level4(Items.ROTTEN_FLESH)),
                "Disease source effect power plus level-4 capacity power");
        assertTrue(Brews.blockedByCapacity(level2(Items.ROTTEN_FLESH)),
                "source Disease requires effect level 4 capacity");

        assertEquals(1, Brews.buildEffects(level4(Items.GLOWSTONE_DUST, Items.ROTTEN_FLESH))
                        .get(0).getAmplifier(),
                "source Disease accepts the normal strength modifier path");
    }

    @Test
    void diseasedEffectUsesSourceSpreadAndWeaknessRules() {
        assertTrue(DiseasedMobEffect.shouldSourceSpreadOnTick(4));
        assertTrue(DiseasedMobEffect.shouldSourceSpreadOnTick(44));
        assertFalse(DiseasedMobEffect.shouldSourceSpreadOnTick(5));
        assertEquals(-0.5D, DiseasedMobEffect.sourceAttackDamagePenalty(0), 1e-9);
        assertEquals(-1.0D, DiseasedMobEffect.sourceAttackDamagePenalty(1), 1e-9);
        assertEquals(-1.0D, DiseasedMobEffect.sourceAttackDamagePenalty(6), 1e-9);
        assertEquals(1200, DiseasedMobEffect.sourceContactDurationTicks(0));
        assertEquals(4800, DiseasedMobEffect.sourceContactDurationTicks(3));
    }

    @Test
    void demonHeartRestoresSourceParalysisBrew() {
        Brews.bootstrap();
        List<MobEffectInstance> paralysis = Brews.buildEffects(level4(WitcheryItems.DEMON_HEART.get()));
        assertEquals(1, paralysis.size(), "Demon Heart resolves as the old Paralysis ingredient");
        assertSame(WitcheryMobEffects.PARALYSED.value(), paralysis.get(0).getEffect().value(),
                "source Demon Heart uses Witchery's Paralyzed potion");
        assertEquals(200, paralysis.get(0).getDuration(), "source Paralysis lasts ten seconds");
        assertEquals(900, Brews.powerCost(level4(WitcheryItems.DEMON_HEART.get())),
                "Paralysis source effect power plus level-4 capacity power");
        assertTrue(Brews.blockedByCapacity(level2(WitcheryItems.DEMON_HEART.get())),
                "source Paralysis requires effect level 4 capacity");

        assertEquals(1, Brews.buildEffects(level4(Items.GLOWSTONE_DUST, WitcheryItems.DEMON_HEART.get()))
                        .get(0).getAmplifier(),
                "source Paralysis accepts the normal strength modifier path");
    }

    @Test
    void icyNeedleRestoresSourceChilledBrew() {
        Brews.bootstrap();
        List<MobEffectInstance> chilled = Brews.buildEffects(level2(WitcheryItems.ICY_NEEDLE.get()));
        assertEquals(1, chilled.size(), "Icy Needle resolves as the old Cold ingredient entity effect");
        assertSame(WitcheryMobEffects.CHILLED.value(), chilled.get(0).getEffect().value(),
                "source Icy Needle applies Witchery's Chilled potion to entities");
        assertEquals(ChilledMobEffect.BREW_DURATION_TICKS, chilled.get(0).getDuration(),
                "source Cold lasts three minutes");
        assertEquals(250, Brews.powerCost(level2(WitcheryItems.ICY_NEEDLE.get())),
                "Cold source effect power plus level-2 capacity power");
        assertTrue(Brews.blockedByCapacity(level1(WitcheryItems.ICY_NEEDLE.get())),
                "source Cold requires effect level 2 capacity");

        assertEquals(1, Brews.buildEffects(level2(Items.GLOWSTONE_DUST, WitcheryItems.ICY_NEEDLE.get()))
                        .get(0).getAmplifier(),
                "source Cold accepts the normal strength modifier path");
    }

    @Test
    void chilledEffectUsesSourceFireAndColdRules() {
        assertEquals(25, ChilledMobEffect.coldDamageTickInterval(0));
        assertEquals(12, ChilledMobEffect.coldDamageTickInterval(1));
        assertEquals(6, ChilledMobEffect.coldDamageTickInterval(2));
        assertEquals(1, ChilledMobEffect.coldDamageTickInterval(4));
        assertEquals(0, ChilledMobEffect.coldDamageTickInterval(5));
        assertTrue(ChilledMobEffect.shouldTakeColdDamage(true, 0));
        assertFalse(ChilledMobEffect.shouldTakeColdDamage(false, 1));
        assertTrue(ChilledMobEffect.shouldTakeColdDamage(false, 2));
        assertEquals(3.0F, ChilledMobEffect.fireDamageAfterReduction(4.0F, 0));
        assertEquals(1.0F, ChilledMobEffect.fireDamageAfterReduction(1.5F, 1));
        assertEquals(1.0F, ChilledMobEffect.fireDamageAfterReduction(4.0F, 2));
        assertEquals(0.0F, ChilledMobEffect.fireDamageAfterReduction(2.0F, 2));
        assertTrue(Brews.sourceColdBlockHitShouldFreeze(Blocks.WATER.defaultBlockState()),
                "source Icy Needle block hit replaces water material");
        assertFalse(Brews.sourceColdBlockHitShouldFreeze(Blocks.ICE.defaultBlockState()),
                "source Icy Needle block hit does not recursively replace existing ice");
        assertFalse(Brews.sourceColdBlockHitShouldFreeze(Blocks.AIR.defaultBlockState()),
                "source Icy Needle block hit only follows water");
        assertTrue(Brews.sourceReplaceSphereCanRecurse(BlockPos.ZERO, BlockPos.ZERO, 2));
        assertTrue(Brews.sourceReplaceSphereCanRecurse(BlockPos.ZERO, new BlockPos(1, 0, 0), 2));
        assertFalse(Brews.sourceReplaceSphereCanRecurse(BlockPos.ZERO, new BlockPos(2, 0, 0), 2),
                "source sphere recursion stops at exact radius");
    }

    @Test
    void infernalBloodRestoresSourceGrotesqueBrew() {
        Brews.bootstrap();
        List<MobEffectInstance> grotesque = Brews.buildEffects(level2(WitcheryItems.INFERNAL_BLOOD.get()));
        assertEquals(1, grotesque.size(), "Infernal Blood resolves as the old Fear/Grotesque ingredient");
        assertSame(WitcheryMobEffects.GROTESQUE.value(), grotesque.get(0).getEffect().value(),
                "source Infernal Blood applies Witchery's Grotesque potion");
        assertEquals(GrotesqueMobEffect.BREW_DURATION_TICKS, grotesque.get(0).getDuration(),
                "source Grotesque lasts three minutes");
        assertEquals(550, Brews.powerCost(level2(WitcheryItems.INFERNAL_BLOOD.get())),
                "Grotesque source effect power plus level-2 capacity power");
        assertTrue(Brews.blockedByCapacity(level1(WitcheryItems.INFERNAL_BLOOD.get())),
                "source Grotesque requires effect level 2 capacity");

        assertEquals(1, Brews.buildEffects(level2(Items.GLOWSTONE_DUST, WitcheryItems.INFERNAL_BLOOD.get()))
                        .get(0).getAmplifier(),
                "source Grotesque accepts the normal strength modifier path");
    }

    @Test
    void grotesqueEffectUsesSourceRepulsionRules() {
        assertFalse(GrotesqueMobEffect.isSourceTickPhase(2L));
        assertTrue(GrotesqueMobEffect.isSourceTickPhase(3L));
        assertFalse(GrotesqueMobEffect.shouldRepulseMob(true, false, 1.0D));
        assertFalse(GrotesqueMobEffect.shouldRepulseMob(false, true, 1.0D));
        assertFalse(GrotesqueMobEffect.shouldRepulseMob(false, false, GrotesqueMobEffect.SOURCE_RADIUS_SQ));
        assertTrue(GrotesqueMobEffect.shouldRepulseMob(false, false, 15.99D));
        assertEquals(Vec3.ZERO, GrotesqueMobEffect.repulsionPush(Vec3.ZERO, Vec3.ZERO));
        Vec3 push = GrotesqueMobEffect.repulsionPush(Vec3.ZERO, new Vec3(1.0D, 0.0D, 0.0D));
        assertEquals(0.22D, push.x, 0.000001D);
        assertEquals(0.0D, push.y, 0.000001D);
        assertEquals(0.0D, push.z, 0.000001D);
    }

    @Test
    void paralysisEffectUsesSourceGates() {
        assertEquals(-40.0D, ParalyzedMobEffect.sourceSpeedModifierAmount(0));
        assertEquals(-120.0D, ParalyzedMobEffect.sourceSpeedModifierAmount(2));
        assertTrue(ParalyzedMobEffect.shouldPinVertical(1, true, 5));
        assertFalse(ParalyzedMobEffect.shouldPinVertical(2, true, 5));
        assertTrue(ParalyzedMobEffect.shouldPinVertical(2, true, 4));
        assertFalse(ParalyzedMobEffect.shouldBreakOnHurt(false, 3, 1.0F));
        assertFalse(ParalyzedMobEffect.shouldBreakOnHurt(true, 4, 1.0F));
        assertFalse(ParalyzedMobEffect.shouldBreakOnHurt(false, 4, 0.5F));
        assertTrue(ParalyzedMobEffect.shouldBreakOnHurt(false, 4, 1.0F));
        assertFalse(ParalyzedMobEffect.shouldApplyQueasyOnExpiry(true, true, 4));
        assertFalse(ParalyzedMobEffect.shouldApplyQueasyOnExpiry(false, false, 4));
        assertFalse(ParalyzedMobEffect.shouldApplyQueasyOnExpiry(false, true, 3));
        assertTrue(ParalyzedMobEffect.shouldApplyQueasyOnExpiry(false, true, 4));
        assertEquals(QueasyMobEffect.PARALYSED_EXPIRY_DURATION_TICKS, ParalyzedMobEffect.QUEASY_EXPIRY_DURATION_TICKS,
                "source amplified player expiry applies Queasy for ninety seconds");
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
        assertEquals(Brews.Dispersal.LIQUID,
                Brews.dispersal(List.of(WitcheryItems.WORMWOOD.get(), Items.SPIDER_EYE)),
                "Wormwood is the old liquid dispersal selector");
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
        assertEquals(Brews.Dispersal.LIQUID,
                Brews.dispersal(List.of(Items.GUNPOWDER, WitcheryItems.WORMWOOD.get(), Items.SPIDER_EYE)),
                "later Wormwood replaces earlier instant dispersal");
        assertEquals(Brews.Dispersal.GAS,
                Brews.dispersal(List.of(WitcheryItems.WORMWOOD.get(), WitcheryItems.BAT_WOOL.get(), Items.SPIDER_EYE)),
                "later Bat Wool replaces earlier liquid dispersal");
        assertEquals(Brews.Dispersal.SPLASH,
                Brews.dispersal(List.of(WitcheryItems.WORMWOOD.get(), WitcheryItems.ARTICHOKE.get(), Items.SPIDER_EYE)),
                "later Artichoke replaces earlier liquid dispersal");
    }

    @Test
    void triggeredDispersalFromZombieHeadAlwaysWins() {
        Brews.bootstrap();
        // A Zombie Head selects the source triggered dispersal...
        assertEquals(Brews.Dispersal.TRIGGERED,
                Brews.dispersal(List.of(Items.ZOMBIE_HEAD, Items.SPIDER_EYE)));
        // ...and it nullifies/overrides any other dispersal modifier regardless of order.
        assertEquals(Brews.Dispersal.TRIGGERED,
                Brews.dispersal(List.of(Items.GUNPOWDER, Items.ZOMBIE_HEAD, Items.SPIDER_EYE)),
                "triggered beats splash");
        assertEquals(Brews.Dispersal.TRIGGERED,
                Brews.dispersal(List.of(Items.ZOMBIE_HEAD, WitcheryItems.WORMWOOD.get(), Items.SPIDER_EYE)),
                "triggered beats liquid even when added first");
        assertEquals(Brews.Dispersal.TRIGGERED,
                Brews.bottledDispersal(List.of(Items.ZOMBIE_HEAD, Items.SPIDER_EYE)));
    }

    @Test
    void wormwoodIsNotPotionEffectIngredient() {
        Brews.bootstrap();
        assertFalse(Brews.hasEffect(List.of(WitcheryItems.WORMWOOD.get())));
        assertTrue(Brews.buildEffects(List.of(WitcheryItems.WORMWOOD.get())).isEmpty());
    }

    @Test
    void liquidDispersalBottlesAsLiquidBrew() {
        Brews.bootstrap();
        assertEquals(Brews.Dispersal.LIQUID,
                Brews.dispersal(List.of(WitcheryItems.WORMWOOD.get(), Items.SPIDER_EYE)));
        assertEquals(Brews.Dispersal.LIQUID,
                Brews.bottledDispersal(List.of(WitcheryItems.WORMWOOD.get(), Items.SPIDER_EYE)),
                "Wormwood-selected brews now use the moving brew liquid block spine");
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
    void lowLevelBlockActionsRestoreSourceEntries() {
        Brews.bootstrap();
        Item stone = Blocks.STONE.asItem();

        assertFalse(Brews.canAdd(List.of(), Items.COAL),
                "source Extinguish requires level-1 capacity");
        assertFalse(Brews.canAdd(List.of(), stone),
                "source Dissipate requires level-1 capacity");
        assertFalse(Brews.canAdd(List.of(), Items.DANDELION),
                "source Grow Flowers requires level-1 capacity");
        assertFalse(Brews.canAdd(List.of(), Items.BONE_MEAL),
                "source Fertilization requires level-1 capacity");

        List<Brews.ActionApplication> extinguish = Brews.buildActions(level1(Items.COAL));
        assertEquals(1, extinguish.size(), "Coal resolves as source Extinguish");
        assertEquals(Brews.Ingredient.BrewAction.EXTINGUISH, extinguish.get(0).action());
        assertEquals("witchery.brew.extinguish", extinguish.get(0).nameKey());
        assertEquals(0, Brews.powerCost(level1(Items.COAL)),
                "Mandrake Root capacity plus Coal has no source altar cost");

        List<Brews.ActionApplication> dissipate = Brews.buildActions(level1(stone));
        assertEquals(Brews.Ingredient.BrewAction.DISSIPATE, dissipate.get(0).action());
        assertEquals("witchery.brew.dissipate", dissipate.get(0).nameKey());
        assertEquals(0, Brews.powerCost(level1(stone)),
                "Stone Dissipate has no source altar cost beyond Mandrake capacity");

        List<Brews.ActionApplication> flowers = Brews.buildActions(level1(Items.DANDELION));
        assertEquals(Brews.Ingredient.BrewAction.GROW_FLOWERS, flowers.get(0).action());
        assertEquals("witchery.brew.flowers", flowers.get(0).nameKey());
        assertEquals(200, Brews.powerCost(level1(Items.DANDELION)),
                "Dandelion Grow Flowers keeps source 200 altar power");

        List<Brews.ActionApplication> fertilize = Brews.buildActions(level1(Items.BONE_MEAL));
        assertEquals(Brews.Ingredient.BrewAction.FERTILIZE, fertilize.get(0).action());
        assertEquals("witchery.brew.fertilization", fertilize.get(0).nameKey());
        assertEquals(250, Brews.powerCost(level1(Items.BONE_MEAL)),
                "Bone Meal Fertilization keeps source 250 altar power");

        assertEquals(1, Brews.buildActions(level1(Items.GLOWSTONE_DUST, Items.COAL)).get(0).strength(),
                "Glowstone carries into low-level source actions");
        assertTrue(Brews.requiresExplicitStir(Items.COAL));
        assertTrue(Brews.requiresExplicitStir(stone));
        assertTrue(Brews.requiresExplicitStir(Items.DANDELION));
        assertTrue(Brews.requiresExplicitStir(Items.BONE_MEAL));
        assertFalse(Brews.canAddAutomatically(level1(), Items.DANDELION),
                "dropped Dandelions remain available to current Pit Grass cauldron offerings");
    }

    @Test
    void lowLevelBlockActionsUseSourceConstants() {
        assertTrue(Brews.sourceExtinguishAppliesInDimension(false, 0),
                "base Extinguish works outside the Nether");
        assertFalse(Brews.sourceExtinguishAppliesInDimension(true, 0),
                "base Extinguish is suppressed in the Nether like source");
        assertFalse(Brews.sourceExtinguishAppliesInDimension(true, 1),
                "one strength tier is still suppressed in the Nether");
        assertTrue(Brews.sourceExtinguishAppliesInDimension(true, 2),
                "source Extinguish breaks the Nether gate above strength I");
        assertEquals(0.0F, Brews.sourceExtinguishBlazeDamage(0));
        assertEquals(8.0F, Brews.sourceExtinguishBlazeDamage(1));
        assertEquals(12.0F, Brews.sourceExtinguishBlazeDamage(2));

        assertEquals(5.0F, Brews.sourceDissipateUndeadDamage(0));
        assertEquals(10.0F, Brews.sourceDissipateUndeadDamage(1));
        assertTrue(Brews.sourceDissipateRemovesGas(true, false, 0),
                "base source Dissipate removes Witchery brew gas");
        assertFalse(Brews.sourceDissipateRemovesGas(false, true, 0),
                "base source Dissipate leaves non-Witchery gas alone");
        assertTrue(Brews.sourceDissipateRemovesGas(false, true, 1),
                "powered source Dissipate removes other gaseous fluid blocks");
        assertFalse(Brews.sourceDissipateRemovesGas(false, false, 1),
                "powered source Dissipate does not invent removal for unrelated cloud entities");

        assertEquals(3, Brews.sourceBlockCircleRadius(3, 0));
        assertEquals(4, Brews.sourceBlockCircleRadius(3, 1));
        assertEquals(8, Brews.sourceGrowFlowersChanceBound(0));
        assertEquals(7, Brews.sourceGrowFlowersChanceBound(1));
        assertEquals(10, Brews.sourceFlowerVariantCount(),
                "source Grow Flowers chooses Dandelion plus nine old red-flower variants");
        assertEquals(1, Brews.sourceFertilizationApplications(0));
        assertEquals(2, Brews.sourceFertilizationApplications(1));
    }

    @Test
    void reekOfMisfortuneRestoresSourceRemoveBuffsAction() {
        Brews.bootstrap();
        Item reek = WitcheryItems.REEK_OF_MISFORTUNE.get();

        assertFalse(Brews.canAdd(List.of(), reek),
                "source Remove Buffs requires level-2 capacity");
        assertTrue(Brews.canAdd(level2(), reek),
                "Nether Wart opens enough capacity for the source Reek action");
        assertTrue(Brews.blockedByCapacity(List.of(reek)),
                "under-capacity Reek resolves inertly like other source actions");

        List<Brews.ActionApplication> actions = Brews.buildActions(level2(reek));
        assertEquals(1, actions.size(), "Reek resolves as the old Cure Buffs action");
        assertEquals(Brews.Ingredient.BrewAction.REMOVE_BUFFS, actions.get(0).action());
        assertFalse(actions.get(0).inverted(), "source Reek action has no inverted Cure Debuffs branch");
        assertEquals(300, Brews.powerCost(level2(reek)),
                "source Nether Wart capacity cost plus Reek altar cost");

        assertEquals(1, Brews.buildActions(level2(Items.GLOWSTONE_DUST, reek)).get(0).strength(),
                "Glowstone raises the removable buff amplifier by one");
        assertTrue(Brews.requiresExplicitStir(reek),
                "Reek remains available to existing dropped-item kettle recipes unless explicitly stirred");
        assertFalse(Brews.canAddAutomatically(level2(), reek),
                "dropped Reek must not be stolen from current kettle recipes");
    }

    @Test
    void removeBuffsUsesSourceCurablePositiveLevelGate() {
        MobEffectInstance speedOne = new MobEffectInstance(MobEffects.SPEED, 200, 0);
        MobEffectInstance speedTwo = new MobEffectInstance(MobEffects.SPEED, 200, 1);
        MobEffectInstance poison = new MobEffectInstance(MobEffects.POISON, 200, 0);

        assertTrue(Brews.shouldRemoveBuff(speedOne, 0),
                "base source strength removes level-I positive curable effects");
        assertFalse(Brews.shouldRemoveBuff(speedTwo, 0),
                "higher-amplifier positive effects survive unless the brew is strengthened");
        assertTrue(Brews.shouldRemoveBuff(speedTwo, 1),
                "strengthened Cure Buffs removes matching higher amplifiers");
        assertFalse(Brews.shouldRemoveBuff(poison, 3),
                "negative effects are not buffs");
        assertFalse(Brews.shouldRemoveBuff(MobEffects.SPEED, 0, false, 3),
                "source Cure Buffs does not remove incurable effects");
    }

    @Test
    void purifiedMilkRestoresSourceRemoveDebuffsAction() {
        Brews.bootstrap();
        Item milk = WitcheryItems.PURIFIED_MILK.get();

        assertFalse(Brews.canAdd(List.of(), milk),
                "source Remove Debuffs requires level-2 capacity");
        assertTrue(Brews.canAdd(level2(), milk),
                "Nether Wart opens enough capacity for the source Purified Milk action");
        assertTrue(Brews.blockedByCapacity(List.of(milk)),
                "under-capacity Purified Milk resolves inertly like other source actions");

        List<Brews.ActionApplication> actions = Brews.buildActions(level2(milk));
        assertEquals(1, actions.size(), "Purified Milk resolves as the old Cure Debuffs action");
        assertEquals(Brews.Ingredient.BrewAction.REMOVE_DEBUFFS, actions.get(0).action());
        assertFalse(actions.get(0).inverted(), "source Purified Milk action has no inverted Cure Buffs branch");
        assertEquals(250, Brews.powerCost(level2(milk)),
                "source Nether Wart capacity cost plus Purified Milk altar cost");

        assertEquals(1, Brews.buildActions(level2(Items.GLOWSTONE_DUST, milk)).get(0).strength(),
                "Glowstone raises the removable debuff amplifier by one");
        assertTrue(Brews.requiresExplicitStir(milk),
                "Purified Milk remains available to existing dropped-item kettle recipes unless explicitly stirred");
        assertFalse(Brews.canAddAutomatically(level2(), milk),
                "dropped Purified Milk must not be stolen from current kettle recipes");
    }

    @Test
    void removeDebuffsUsesSourceCurableNegativeLevelGate() {
        MobEffectInstance poisonOne = new MobEffectInstance(MobEffects.POISON, 200, 0);
        MobEffectInstance poisonTwo = new MobEffectInstance(MobEffects.POISON, 200, 1);
        MobEffectInstance speed = new MobEffectInstance(MobEffects.SPEED, 200, 0);

        assertTrue(Brews.shouldRemoveDebuff(poisonOne, 0),
                "base source strength removes level-I negative curable effects");
        assertFalse(Brews.shouldRemoveDebuff(poisonTwo, 0),
                "higher-amplifier negative effects survive unless the brew is strengthened");
        assertTrue(Brews.shouldRemoveDebuff(poisonTwo, 1),
                "strengthened Cure Debuffs removes matching higher amplifiers");
        assertFalse(Brews.shouldRemoveDebuff(speed, 3),
                "positive effects are not debuffs");
        assertFalse(Brews.shouldRemoveDebuff(MobEffects.POISON, 0, false, 3),
                "source Cure Debuffs does not remove incurable effects on the normal entity path");
        assertFalse(Brews.shouldRemoveDebuff(WitcheryMobEffects.DISEASED, 0, false, 1),
                "source Diseased is the special Purified Milk branch, not a base-strength debuff cure");
        assertTrue(Brews.shouldRemoveDebuff(WitcheryMobEffects.DISEASED, 5, false, 2),
                "source Purified Milk II+ clears Diseased even though Diseased is incurable");
        assertTrue(Brews.isSourceDiseaseEffect(WitcheryMobEffects.DISEASED));
    }

    @Test
    void pumpkinAndBlueOrchidRestoreSourceBaneActions() {
        Brews.bootstrap();

        assertFalse(Brews.canAdd(List.of(), Items.PUMPKIN),
                "source Undeadbane requires level-2 capacity");
        assertTrue(Brews.canAdd(level2(), Items.PUMPKIN),
                "Nether Wart opens enough capacity for source Undeadbane");
        assertTrue(Brews.blockedByCapacity(List.of(Items.PUMPKIN)),
                "under-capacity Undeadbane resolves inertly like other source actions");

        List<Brews.ActionApplication> undead = Brews.buildActions(level2(Items.PUMPKIN));
        assertEquals(1, undead.size(), "Pumpkin resolves as source Undeadbane");
        assertEquals(Brews.Ingredient.BrewAction.BANE_UNDEAD, undead.get(0).action());
        assertEquals("witchery.brew.harmundead", undead.get(0).nameKey());
        assertEquals(250, Brews.powerCost(level2(Items.PUMPKIN)),
                "source Nether Wart capacity cost plus Pumpkin altar cost");

        assertFalse(Brews.canAdd(List.of(), Items.BLUE_ORCHID),
                "source Insectbane requires level-2 capacity");
        List<Brews.ActionApplication> insects = Brews.buildActions(level2(Items.BLUE_ORCHID));
        assertEquals(1, insects.size(), "Blue Orchid resolves as source Insectbane");
        assertEquals(Brews.Ingredient.BrewAction.BANE_ARTHROPODS, insects.get(0).action());
        assertEquals("witchery.brew.harminsects", insects.get(0).nameKey());
        assertEquals(250, Brews.powerCost(level2(Items.BLUE_ORCHID)),
                "source Nether Wart capacity cost plus Blue Orchid altar cost");

        assertEquals(1, Brews.buildActions(level2(Items.GLOWSTONE_DUST, Items.PUMPKIN)).get(0).strength(),
                "Glowstone raises bane action strength");
        assertTrue(Brews.buildActions(level2(Items.TROPICAL_FISH, Items.GLOWSTONE_DUST,
                        Items.BLAZE_ROD, WitcheryItems.ATTUNED_STONE_CHARGED.get(), Items.PUMPKIN))
                .get(0).strengthCeilingDisabled(),
                "Tropical Fish carries source strength-ceiling bypass into bane actions");
    }

    @Test
    void baneDamageUsesSourceStrengthCeilingAndSplashScaling() {
        assertEquals(10, Brews.sourceBaneDamage(true, 0, false, 1.0),
                "base matching bane damage is 10");
        assertEquals(20, Brews.sourceBaneDamage(true, 3, false, 1.0),
                "ordinary bane strength caps at level II damage");
        assertEquals(80, Brews.sourceBaneDamage(true, 3, true, 1.0),
                "ceiling-disabled bane strength reaches source level IV damage");
        assertEquals(3, Brews.sourceBaneDamage(false, 0, false, 1.0),
                "nonmatching targets still take the source lesser damage");
        assertEquals(6, Brews.sourceBaneDamage(false, 3, false, 1.0),
                "nonmatching ordinary strength also caps at level II damage");
        assertEquals(40, Brews.sourceBaneDamage(true, 3, true, 0.5),
                "source splash power factor scales bane damage before ceiling");
        assertEquals(2, Brews.sourceBaneDamage(false, 0, false, 0.5),
                "source fractional lesser damage is rounded up");
    }

    @Test
    void breathOfGoddessRestoresSourceAirHikeAction() {
        Brews.bootstrap();
        Item breath = WitcheryItems.BREATH_OF_GODDESS.get();

        assertFalse(Brews.canAdd(List.of(), breath),
                "source Air Hike requires level-2 capacity");
        assertTrue(Brews.canAdd(level2(), breath),
                "Nether Wart opens enough capacity for source Air Hike");
        assertTrue(Brews.blockedByCapacity(List.of(breath)),
                "under-capacity Air Hike resolves inertly like other source actions");

        List<Brews.ActionApplication> actions = Brews.buildActions(level2(breath));
        assertEquals(1, actions.size(), "Breath of the Goddess resolves as source Air Hike");
        assertEquals(Brews.Ingredient.BrewAction.AIR_HIKE, actions.get(0).action());
        assertEquals("witchery.brew.airhike", actions.get(0).nameKey());
        assertEquals(800, Brews.powerCost(level2(breath)),
                "source Nether Wart capacity cost plus Breath of the Goddess altar cost");

        Brews.ActionApplication modified = Brews.buildActions(level2(Items.TROPICAL_FISH,
                Items.GLOWSTONE_DUST, Items.BLAZE_ROD,
                WitcheryItems.ATTUNED_STONE_CHARGED.get(), breath)).get(0);
        assertEquals(3, modified.strength(),
                "Air Hike carries the normal source strength modifiers");
        assertTrue(modified.strengthCeilingDisabled(),
                "Tropical Fish still marks the source strength ceiling bypass for Air Hike");
    }

    @Test
    void airHikeUsesSourceVerticalMotion() {
        assertEquals(0.6D, Brews.sourceAirHikeMotionY(0), 0.0001D,
                "base source Air Hike launches at 0.6 Y motion");
        assertEquals(0.8D, Brews.sourceAirHikeMotionY(1), 0.0001D,
                "Glowstone raises Air Hike by 0.2 Y motion");
        assertEquals(1.2D, Brews.sourceAirHikeMotionY(3), 0.0001D,
                "full source strength reaches 1.2 Y motion");
    }

    @Test
    void slimeBallRestoresSourceFrogsTonguePullAction() {
        Brews.bootstrap();

        assertFalse(Brews.canAdd(List.of(), Items.SLIME_BALL),
                "source Pull requires level-2 capacity");
        assertTrue(Brews.canAdd(level2(), Items.SLIME_BALL),
                "Nether Wart opens enough capacity for source Pull");
        assertTrue(Brews.blockedByCapacity(List.of(Items.SLIME_BALL)),
                "under-capacity Pull resolves inertly like other source actions");

        List<Brews.ActionApplication> actions = Brews.buildActions(level2(Items.SLIME_BALL));
        assertEquals(1, actions.size(), "Slime Ball resolves as source Frog's Tongue / Pull");
        assertEquals(Brews.Ingredient.BrewAction.PULL, actions.get(0).action());
        assertEquals("witchery.brew.frogtongue", actions.get(0).nameKey());
        assertEquals(200, Brews.powerCost(level2(Items.SLIME_BALL)),
                "source Nether Wart capacity cost plus Slime Ball altar cost");

        assertEquals(1, Brews.buildActions(level2(Items.GLOWSTONE_DUST, Items.SLIME_BALL)).get(0).strength(),
                "Glowstone raises Pull strength and switches source targeting toward the caster");
    }

    @Test
    void pullUsesSourceTargetingAndMotionMath() {
        assertFalse(Brews.sourcePullUsesCaster(0, false),
                "unpowered source Pull targets the impact location");
        assertTrue(Brews.sourcePullUsesCaster(1, false),
                "powered source Pull targets the caster");
        assertFalse(Brews.sourcePullUsesCaster(1, true),
                "FakePlayer casters use the impact-location branch");

        assertEquals(new Vec3(0.3D, 0.4D, -0.6D),
                Brews.sourcePullMotion(new Vec3(1.0D, 2.0D, 3.0D), new Vec3(3.0D, 99.0D, -1.0D)),
                "source Pull horizontal motion is delta * 0.15 with fixed 0.4 upward motion");
        assertEquals(Vec3.ZERO,
                Brews.sourcePullMotion(new Vec3(1.0D, 2.0D, 3.0D), new Vec3(1.0D, 2.0D, 3.0D)),
                "source Pull skips occupied target positions");
    }

    @Test
    void iceRestoresSourceDemonbaneAction() {
        Brews.bootstrap();
        List<Item> level5 = withCapacity(List.of(Items.NETHER_WART,
                WitcheryItems.TEAR_OF_GODDESS.get(),
                WitcheryItems.DIAMOND_VAPOUR.get()), Items.ICE);

        assertFalse(Brews.canAdd(level4(), Items.ICE),
                "source Demonbane requires level-5 capacity");
        assertTrue(Brews.canAdd(withCapacity(List.of(Items.NETHER_WART,
                WitcheryItems.TEAR_OF_GODDESS.get(),
                WitcheryItems.DIAMOND_VAPOUR.get())), Items.ICE),
                "Nether Wart + Tear of the Goddess + Diamond Vapour opens enough capacity for Demonbane");
        assertTrue(Brews.blockedByCapacity(List.of(Items.ICE)),
                "under-capacity Demonbane resolves inertly like other source actions");

        List<Brews.ActionApplication> actions = Brews.buildActions(level5);
        assertEquals(1, actions.size(), "Ice resolves as source Demonbane");
        assertEquals(Brews.Ingredient.BrewAction.BANE_DEMONS, actions.get(0).action());
        assertEquals("witchery.brew.harmdemons", actions.get(0).nameKey());
        assertEquals(800, Brews.powerCost(level5),
                "source capacity route cost plus Ice altar cost");

        Brews.ActionApplication modified = Brews.buildActions(withCapacity(List.of(Items.NETHER_WART,
                WitcheryItems.TEAR_OF_GODDESS.get(),
                WitcheryItems.DIAMOND_VAPOUR.get()), Items.TROPICAL_FISH, Items.GLOWSTONE_DUST,
                Items.BLAZE_ROD, WitcheryItems.ATTUNED_STONE_CHARGED.get(), Items.ICE)).get(0);
        assertEquals(3, modified.strength(),
                "Demonbane carries the normal source strength modifiers");
        assertTrue(modified.strengthCeilingDisabled(),
                "Tropical Fish opens the old Demonbane strength ceiling to level IV damage");
    }

    @Test
    void leonardDemonbaneWeaknessUsesSourceGateAndCap() {
        assertFalse(LeonardEntity.sourceDemonbaneWeaknessCanHurt(200.0F, 500.0F),
                "source Leonard weakness does not hurt at exactly 40 percent health");
        assertTrue(LeonardEntity.sourceDemonbaneWeaknessCanHurt(199.0F, 500.0F),
                "source Leonard weakness starts below 40 percent health");
        assertEquals(15.0F, LeonardEntity.sourceDemonbaneWeaknessDamageCap(false),
                "normal source Leonard Demonbane weakness caps at 15 damage");
        assertEquals(8.0F, LeonardEntity.sourceDemonbaneWeaknessDamageCap(true),
                "large Resizing source Leonard Demonbane weakness caps at 8 damage");
    }

    @Test
    void silverDepositsRestoresSourceHarmWerewolvesAction() {
        Brews.bootstrap();
        Item deposits = WitcheryItems.SILVER_DEPOSITS.get();

        assertFalse(Brews.canAdd(level2(), deposits),
                "source Harm Werewolves requires level-4 capacity");
        assertTrue(Brews.canAdd(level4(), deposits),
                "Nether Wart plus Tear of the Goddess reaches the old level-4 silver-deposit action");
        assertTrue(Brews.blockedByCapacity(List.of(deposits)),
                "under-capacity Harm Werewolves resolves inertly like other source actions");

        List<Brews.ActionApplication> actions = Brews.buildActions(level4(deposits));
        assertEquals(1, actions.size(), "Silver Deposits resolves as source Harm Werewolves");
        assertEquals(Brews.Ingredient.BrewAction.HARM_WEREWOLVES, actions.get(0).action());
        assertEquals("witchery.brew.harmwerewolves", actions.get(0).nameKey());
        assertEquals(1150, Brews.powerCost(level4(deposits)),
                "source capacity route cost plus Silver Deposits altar cost");

        Brews.ActionApplication modified = Brews.buildActions(level4(Items.TROPICAL_FISH,
                Items.GLOWSTONE_DUST, Items.BLAZE_ROD,
                WitcheryItems.ATTUNED_STONE_CHARGED.get(), deposits)).get(0);
        assertEquals(3, modified.strength(),
                "Harm Werewolves carries the normal source strength modifiers");
        assertTrue(modified.strengthCeilingDisabled(),
                "Tropical Fish opens the old Harm Werewolves strength ceiling to level IV damage");
    }

    @Test
    void harmWerewolvesUsesSourceDamageAndTransformedPlayerGate() {
        assertEquals(3, Brews.sourceHarmWerewolvesDamage(true, 0, false, 1.0),
                "base matching Harm Werewolves damage is 3 before silver hooks");
        assertEquals(6, Brews.sourceHarmWerewolvesDamage(true, 3, false, 1.0),
                "ordinary Harm Werewolves strength caps at level II damage");
        assertEquals(24, Brews.sourceHarmWerewolvesDamage(true, 3, true, 1.0),
                "ceiling-disabled Harm Werewolves reaches source level IV damage");
        assertEquals(1, Brews.sourceHarmWerewolvesDamage(false, 0, false, 1.0),
                "non-werewolf targets take the source minor damage");
        assertEquals(2, Brews.sourceHarmWerewolvesDamage(false, 3, false, 1.0),
                "nonmatching ordinary strength also caps at level II damage");
        assertEquals(12, Brews.sourceHarmWerewolvesDamage(true, 3, true, 0.5),
                "source splash power factor scales Harm Werewolves damage before ceiling");
        assertEquals(1, Brews.sourceHarmWerewolvesDamage(false, 0, false, 0.5),
                "source fractional minor damage is rounded up");

        assertTrue(Brews.sourceWerewolfPlayerMatches(true, true),
                "source includeUnshifted=false route matches transformed werewolf players");
        assertFalse(Brews.sourceWerewolfPlayerMatches(true, false),
                "untransformed werewolf players are not on the strong source branch");
        assertFalse(Brews.sourceWerewolfPlayerMatches(false, true),
                "transformation state alone is not enough without lycanthropy");
    }

    @Test
    void skeletonSkullRestoresSourceStealBuffsAction() {
        Brews.bootstrap();
        List<Item> level6 = withCapacity(List.of(Items.NETHER_WART,
                WitcheryItems.TEAR_OF_GODDESS.get(),
                WitcheryItems.DIAMOND_VAPOUR.get()), Items.SKELETON_SKULL);

        assertFalse(Brews.canAdd(level4(), Items.SKELETON_SKULL),
                "source Steal Buffs requires level-6 capacity");
        assertTrue(Brews.canAdd(withCapacity(List.of(Items.NETHER_WART,
                WitcheryItems.TEAR_OF_GODDESS.get(),
                WitcheryItems.DIAMOND_VAPOUR.get())), Items.SKELETON_SKULL),
                "Nether Wart + Tear of the Goddess + Diamond Vapour opens enough capacity for Steal Buffs");
        assertTrue(Brews.blockedByCapacity(List.of(Items.SKELETON_SKULL)),
                "under-capacity Steal Buffs resolves inertly like other source actions");

        List<Brews.ActionApplication> actions = Brews.buildActions(level6);
        assertEquals(1, actions.size(), "Skeleton Skull resolves as source Steal Buffs");
        assertEquals(Brews.Ingredient.BrewAction.STEAL_BUFFS, actions.get(0).action());
        assertEquals("witchery.brew.stealbuffs", actions.get(0).nameKey());
        assertEquals(400, Brews.powerCost(level6),
                "source capacity route cost plus Skeleton Skull altar cost");

        assertEquals(1, Brews.buildActions(withCapacity(List.of(Items.NETHER_WART,
                        WitcheryItems.TEAR_OF_GODDESS.get(), WitcheryItems.DIAMOND_VAPOUR.get()),
                Items.GLOWSTONE_DUST, Items.SKELETON_SKULL)).get(0).strength(),
                "Glowstone raises the stolen-buff amplifier gate");
    }

    @Test
    void stealBuffsUsesSourceRadiusMaxAndReplacementGate() {
        MobEffectInstance speedOne = new MobEffectInstance(MobEffects.SPEED, 200, 0);
        MobEffectInstance speedTwo = new MobEffectInstance(MobEffects.SPEED, 200, 1);
        MobEffectInstance poison = new MobEffectInstance(MobEffects.POISON, 200, 0);

        assertEquals(8.0, Brews.sourceStealBuffsRadius(0),
                "base source Steal Buffs radius is 8 blocks");
        assertEquals(16.0, Brews.sourceStealBuffsRadius(1),
                "strengthened source Steal Buffs radius grows by 8 blocks per tier");
        assertEquals(2, Brews.sourceStealBuffsMaxBuffs(0, 1.0),
                "base source Steal Buffs takes up to two effects");
        assertEquals(4, Brews.sourceStealBuffsMaxBuffs(1, 1.0),
                "strengthened source Steal Buffs takes more effects");
        assertEquals(1, Brews.sourceStealBuffsMaxBuffs(0, 0.5),
                "source splash power factor scales the maximum stolen effects");
        assertEquals(0, Brews.sourceStealBuffsMaxBuffs(0, 0.0),
                "zero source splash power steals no effects");

        assertTrue(Brews.shouldStealBuff(speedOne, null, 0),
                "base source Steal Buffs steals level-I positive curable effects");
        assertFalse(Brews.shouldStealBuff(speedTwo, null, 0),
                "higher-amplifier positive effects survive unless the brew is strengthened");
        assertTrue(Brews.shouldStealBuff(speedTwo, null, 1),
                "strengthened Steal Buffs steals matching higher amplifiers");
        assertFalse(Brews.shouldStealBuff(poison, null, 3),
                "negative effects are not stolen");
        assertFalse(Brews.shouldStealBuff(MobEffects.SPEED, 200, 0, false, -1, -1, 3),
                "source Steal Buffs does not steal incurable effects");
        assertTrue(Brews.shouldStealBuff(MobEffects.SPEED, 400, 1, true, 200, 0, 1),
                "source Steal Buffs replaces weaker or shorter matching buffs");
        assertFalse(Brews.shouldStealBuff(MobEffects.SPEED, 200, 0, true, 400, 0, 1),
                "source Steal Buffs keeps a longer existing same-amplifier buff");
        assertFalse(Brews.shouldStealBuff(MobEffects.SPEED, 400, 0, true, 200, 1, 1),
                "source Steal Buffs keeps a stronger existing buff");
    }

    @Test
    void creeperHeartRestoresSourcePotionLongevityAction() {
        Brews.bootstrap();
        Item heart = WitcheryItems.CREEPER_HEART.get();
        List<Item> level6 = withCapacity(List.of(Items.NETHER_WART,
                WitcheryItems.TEAR_OF_GODDESS.get(),
                WitcheryItems.DIAMOND_VAPOUR.get()), heart);

        assertFalse(Brews.canAdd(level4(), heart),
                "source Potion Longevity requires level-6 capacity");
        assertTrue(Brews.canAdd(withCapacity(List.of(Items.NETHER_WART,
                WitcheryItems.TEAR_OF_GODDESS.get(),
                WitcheryItems.DIAMOND_VAPOUR.get())), heart),
                "Nether Wart + Tear of the Goddess + Diamond Vapour opens enough capacity for Potion Longevity");
        assertTrue(Brews.blockedByCapacity(List.of(heart)),
                "under-capacity Potion Longevity resolves inertly like other source actions");

        List<Brews.ActionApplication> actions = Brews.buildActions(level6);
        assertEquals(1, actions.size(), "Creeper Heart resolves as source Potion Longevity");
        assertEquals(Brews.Ingredient.BrewAction.DURATION_BOOST, actions.get(0).action());
        assertEquals("witchery.brew.durationboost", actions.get(0).nameKey());
        assertEquals(5300, Brews.powerCost(level6),
                "source capacity route cost plus Creeper Heart altar cost");

        assertEquals(1, Brews.buildActions(withCapacity(List.of(Items.NETHER_WART,
                        WitcheryItems.TEAR_OF_GODDESS.get(), WitcheryItems.DIAMOND_VAPOUR.get()),
                Items.GLOWSTONE_DUST, heart)).get(0).strength(),
                "Glowstone raises Potion Longevity strength for shorter Queasy aftermath");
    }

    @Test
    void potionLongevityUsesSourceDurationAndQueasyMath() {
        assertEquals(3600, Brews.sourcePotionLongevityBoostTicks(1.0),
                "source Potion Longevity can add up to three minutes on full power");
        assertEquals(1800, Brews.sourcePotionLongevityBoostTicks(0.5),
                "splash power factor scales the maximum added duration");
        assertEquals(2000, Brews.sourcePotionLongevityDuration(1000, 1.0),
                "short effects can at most double their remaining duration");
        assertEquals(9600, Brews.sourcePotionLongevityDuration(6000, 1.0),
                "long effects gain at most the three-minute source cap");
        assertEquals(7800, Brews.sourcePotionLongevityDuration(6000, 0.5),
                "non-direct splash falloff lowers the added duration cap");

        assertEquals(14400, Brews.sourcePotionLongevityQueasyDuration(0),
                "base Potion Longevity leaves twelve minutes of Queasy");
        assertEquals(10800, Brews.sourcePotionLongevityQueasyDuration(1),
                "Glowstone strength lowers Queasy to nine minutes");
        assertEquals(7200, Brews.sourcePotionLongevityQueasyDuration(2),
                "higher source strength lowers Queasy to six minutes");
        assertEquals(3600, Brews.sourcePotionLongevityQueasyDuration(3),
                "full source strength floors Queasy at three minutes");
        assertEquals(3600, Brews.sourcePotionLongevityQueasyDuration(99),
                "extra strength cannot reduce Queasy below the source floor");

        assertEquals(2400, Brews.sourcePotionLongevityBackfireDuration(true),
                "using Potion Longevity while Queasy can add two minutes of Hunger");
        assertEquals(6000, Brews.sourcePotionLongevityBackfireDuration(false),
                "using Potion Longevity while Queasy otherwise adds five minutes of Nausea");
    }

    @Test
    void garlicRestoresSourceWeakenVampiresAction() {
        Brews.bootstrap();
        Item garlic = WitcheryItems.GARLIC.get();

        assertFalse(Brews.canAdd(level2(), garlic),
                "source Weaken Vampires requires level-4 capacity");
        assertTrue(Brews.canAdd(level4(), garlic),
                "Nether Wart plus Tear of the Goddess reaches the old level-4 garlic action");
        assertTrue(Brews.blockedByCapacity(List.of(garlic)),
                "under-capacity Weaken Vampires resolves inertly like other source actions");

        List<Brews.ActionApplication> actions = Brews.buildActions(level4(garlic));
        assertEquals(1, actions.size(), "Garlic resolves as source Weaken Vampires");
        assertEquals(Brews.Ingredient.BrewAction.WEAKEN_VAMPIRES, actions.get(0).action());
        assertEquals("witchery.brew.weakenvampires", actions.get(0).nameKey());
        assertEquals(650, Brews.powerCost(level4(garlic)),
                "source Nether Wart + Tear capacity cost plus Garlic altar cost");

        Brews.ActionApplication modified = Brews.buildActions(level4(Items.REDSTONE, Items.GLOWSTONE_DUST,
                Items.GOLD_NUGGET, garlic)).get(0);
        assertEquals(1, modified.duration(),
                "Redstone carries the source duration modifier into action effects");
        assertEquals(1, modified.strength(),
                "Glowstone carries the source strength modifier into action effects");
        assertTrue(modified.noParticles(),
                "Gold Nugget carries the source ambient/no-particles flag into action effects");
    }

    @Test
    void weakenVampiresUsesSourceBloodAndWeaknessMath() {
        assertEquals(50, Brews.sourceWeakenVampiresBloodLoss(0),
                "base Weaken Vampires drains 50 blood power");
        assertEquals(70, Brews.sourceWeakenVampiresBloodLoss(1),
                "each source strength tier adds 20 blood power drain");
        assertEquals(0, Brews.sourceWeakenVampiresBloodAfter(30, 0),
                "source exact=false drain spends any positive blood and clamps through setBloodPower");
        assertEquals(0, Brews.sourceWeakenVampiresBloodAfter(0, 3),
                "empty blood power is not reduced");
        assertEquals(50, Brews.sourceWeakenVampiresBloodAfter(100, 0),
                "normal blood drain subtracts the source base amount");

        assertEquals(1800, Brews.sourceModifiedActionDuration(1800, 0, 1.0),
                "drink delivery applies the source 90-second Weakness duration");
        assertEquals(3600, Brews.sourceModifiedActionDuration(1800, 1, 1.0),
                "Redstone doubles the garlic action Weakness duration");
        assertEquals(900, Brews.sourceModifiedActionDuration(1800, 0, 0.5),
                "instant splash delivery halves action durations like source DispersalInstant");
        assertEquals(3, Brews.sourceWeakenVampiresWeaknessAmplifier(3),
                "Weaken Vampires uses the normal source potion-effect strength path");
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
    void sourceImpactLifetimeModifiersUseCappedTiers() {
        Brews.bootstrap();
        assertEquals(1, Brews.impactLifetime(level2(WitcheryItems.BELLADONNA.get(),
                        WitcheryItems.BELLADONNA.get(), Items.SPIDER_EYE)),
                "repeated Belladonna is capped at source lifetime tier 1");
        assertEquals(2, Brews.impactLifetime(level2(WitcheryItems.BELLADONNA.get(), Items.LAPIS_LAZULI,
                        Items.SPIDER_EYE)),
                "Lapis Lazuli opens source lifetime tier 2");
        assertEquals(3, Brews.impactLifetime(level2(WitcheryItems.BELLADONNA.get(), Items.LAPIS_LAZULI,
                        Items.HAY_BLOCK, Items.SPIDER_EYE)),
                "Hay Block opens source lifetime tier 3");
        assertEquals(400, Brews.powerCost(List.of(WitcheryItems.BELLADONNA.get(), Items.LAPIS_LAZULI,
                        Items.HAY_BLOCK, Items.SUGAR)),
                "lifetime tiers count source altar costs alongside the potion effect");
    }

    @Test
    void sourceImpactLifetimeFeedsFluidDecayAndLegacyCloudHelper() {
        Brews.bootstrap();
        assertEquals(5, Brews.sourceBrewFluidDecayDenominator(0),
                "source TileEntityBrewFluid default decay denominator is 5");
        assertEquals(10, Brews.sourceBrewFluidDecayDenominator(1),
                "source lifetime tier 1 yields 5 + 1^2 * 5");
        assertEquals(25, Brews.sourceBrewFluidDecayDenominator(2),
                "source lifetime tier 2 yields 5 + 2^2 * 5");
        assertEquals(50, Brews.sourceBrewFluidDecayDenominator(3),
                "source lifetime tier 3 yields 5 + 3^2 * 5");
        assertEquals(50, Brews.sourceBrewFluidDecayDenominator(4),
                "modern helper clamps to the source maximum lifetime tier");
        assertEquals(4, Brews.sourceBrewFluidExpansion(0),
                "source TileEntityBrewFluid default expansion is 4");
        assertEquals(5, Brews.sourceBrewFluidExpansion(1),
                "source expansion is 4 + extent tier");
        assertEquals(7, Brews.sourceBrewFluidExpansion(3),
                "source extent tier 3 expands to distance 7");
        assertEquals(10, Brews.sourceBrewFluidExpansion(9),
                "modern helper clamps to the source maximum expansion");

        assertEquals(600, Brews.gasCloudDurationTicks(List.of(Items.SUGAR)),
                "the legacy gas-cloud helper keeps the existing base lifetime without impact modifiers");
        assertEquals(1200, Brews.gasCloudDurationTicks(level2(WitcheryItems.BELLADONNA.get(), Items.SUGAR)),
                "Belladonna scales legacy cloud duration by the source tier-1 decay ratio");
        assertEquals(3000, Brews.gasCloudDurationTicks(level2(WitcheryItems.BELLADONNA.get(),
                        Items.LAPIS_LAZULI, Items.SUGAR)),
                "Lapis Lazuli scales legacy cloud duration by the source tier-2 decay ratio");
        assertEquals(6000, Brews.gasCloudDurationTicks(level2(WitcheryItems.BELLADONNA.get(),
                        Items.LAPIS_LAZULI, Items.HAY_BLOCK, Items.SUGAR)),
                "Hay Block scales legacy cloud duration by the source tier-3 decay ratio");
        assertEquals(-0.001F, Brews.gasCloudRadiusPerTick(3000), 0.000001F,
                "longer fallback clouds shrink over their full configured lifetime");
    }

    @Test
    void ironOreRestoresSourceGasMaskEffect() {
        Brews.bootstrap();
        List<Item> brew = level2(Items.IRON_ORE);

        List<MobEffectInstance> fx = Brews.buildEffects(brew);
        assertEquals(1, fx.size());
        assertSame(WitcheryMobEffects.GAS_MASK.value(), fx.get(0).getEffect().value());
        assertEquals(1800, fx.get(0).getDuration(), "source Gas Mask lasts 90 seconds");
        assertEquals(150, Brews.powerCost(brew), "Nether Wart capacity plus Iron Ore's source cost");
    }

    @Test
    void gasMaskProtectionFiltersOnlyNegativeGasPotionEffects() {
        assertFalse(Brews.shouldApplyGasContactEffect(new MobEffectInstance(MobEffects.POISON, 100), true),
                "Gas Mask blocks harmful potion effects from gas contact");
        assertTrue(Brews.shouldApplyGasContactEffect(new MobEffectInstance(MobEffects.SPEED, 100), true),
                "Gas Mask does not block beneficial potion effects");
        assertTrue(Brews.shouldApplyGasContactEffect(new MobEffectInstance(MobEffects.POISON, 100), false),
                "unmasked targets still receive harmful gas effects");
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
    void sourceBrickTargetModifiersPersistThroughEffectReset() {
        Brews.bootstrap();
        List<Brews.EffectApplication> blockOnlyEffects = Brews.buildEffectApplications(
                level4(Items.BRICK, Items.SNOWBALL, Items.SUGAR));
        assertEquals(2, blockOnlyEffects.size(), "two effects resolve after the Brick target modifier");
        assertFalse(blockOnlyEffects.get(0).disableBlockTarget(), "Brick keeps block targeting available");
        assertTrue(blockOnlyEffects.get(0).disableEntityTarget(), "Brick disables entity targeting");
        assertTrue(blockOnlyEffects.get(1).disableEntityTarget(),
                "source ModifiersEffect.reset does not clear disableEntityTarget");

        List<Brews.EffectApplication> entityOnlyEffects = Brews.buildEffectApplications(
                level4(Items.NETHER_BRICK, Items.SNOWBALL, Items.SUGAR));
        assertEquals(2, entityOnlyEffects.size(), "two effects resolve after the Nether Brick target modifier");
        assertTrue(entityOnlyEffects.get(0).disableBlockTarget(), "Nether Brick disables block targeting");
        assertFalse(entityOnlyEffects.get(0).disableEntityTarget(), "Nether Brick keeps entity targeting available");
        assertTrue(entityOnlyEffects.get(1).disableBlockTarget(),
                "source ModifiersEffect.reset does not clear disableBlockTarget");

        Brews.ActionApplication blockOnlyAction = Brews.buildActions(level2(Items.BRICK, Items.SLIME_BALL)).get(0);
        assertFalse(blockOnlyAction.disableBlockTarget(), "Brick keeps block-target action delivery available");
        assertTrue(blockOnlyAction.disableEntityTarget(), "Brick disables entity-target action delivery");

        Brews.ActionApplication entityOnlyAction = Brews.buildActions(level2(Items.NETHER_BRICK, Items.SLIME_BALL)).get(0);
        assertTrue(entityOnlyAction.disableBlockTarget(), "Nether Brick disables block-target action delivery");
        assertFalse(entityOnlyAction.disableEntityTarget(), "Nether Brick keeps entity-target action delivery available");

        assertEquals(100, Brews.powerCost(List.of(Items.BRICK, Items.NETHER_BRICK)),
                "Brick and Nether Brick each cost 50 source altar power as target modifiers");
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
