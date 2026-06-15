package com.emoniph.witchery.brewing;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the block classification for the nature brew actions (Felling / Blight). Needs vanilla
 * registries (BlockState tags), so it bootstraps Minecraft like the ritual tests.
 */
class BrewNatureActionTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // NOTE: block *tags* (BlockTags.LOGS, SMALL_FLOWERS) aren't loaded by a bare Bootstrap, so those
    // branches (felling logs; flower withering) are only verifiable in-game and are not asserted here.

    @Test
    void blightClassifiesGrowthGroundAndFarmland() {
        assertSame(Brews.BlightOutcome.KILL_PLANT, Brews.blightOutcome(Blocks.SHORT_GRASS.defaultBlockState()));
        assertSame(Brews.BlightOutcome.KILL_PLANT, Brews.blightOutcome(Blocks.TALL_GRASS.defaultBlockState()));
        assertSame(Brews.BlightOutcome.WITHER_TO_DEAD_BUSH, Brews.blightOutcome(Blocks.WHEAT.defaultBlockState()));
        assertSame(Brews.BlightOutcome.WITHER_TO_DEAD_BUSH, Brews.blightOutcome(Blocks.CARROTS.defaultBlockState()));
        assertSame(Brews.BlightOutcome.WITHER_TO_DEAD_BUSH, Brews.blightOutcome(Blocks.LILY_PAD.defaultBlockState()));
        assertSame(Brews.BlightOutcome.FARMLAND_TO_SAND, Brews.blightOutcome(Blocks.FARMLAND.defaultBlockState()));
        assertSame(Brews.BlightOutcome.BLIGHT_GROUND, Brews.blightOutcome(Blocks.GRASS_BLOCK.defaultBlockState()));
        assertSame(Brews.BlightOutcome.BLIGHT_GROUND, Brews.blightOutcome(Blocks.MYCELIUM.defaultBlockState()));
        assertSame(Brews.BlightOutcome.NONE, Brews.blightOutcome(Blocks.STONE.defaultBlockState()));
        assertSame(Brews.BlightOutcome.NONE, Brews.blightOutcome(Blocks.DIRT.defaultBlockState()));
    }

    @Test
    void raiseLandHeightScalesWithStrengthAndRitual() {
        assertEquals(3, Brews.raiseLandHeight(0, false));
        assertEquals(6, Brews.raiseLandHeight(1, false));
        assertEquals(4, Brews.raiseLandHeight(1, true));  // ritualised lifts less
    }

    @Test
    void plantingMapsSeedsToCrops() {
        assertSame(Blocks.WHEAT, Brews.cropForSeed(net.minecraft.world.item.Items.WHEAT_SEEDS));
        assertSame(Blocks.CARROTS, Brews.cropForSeed(net.minecraft.world.item.Items.CARROT));
        assertSame(Blocks.POTATOES, Brews.cropForSeed(net.minecraft.world.item.Items.POTATO));
        assertSame(Blocks.NETHER_WART, Brews.cropForSeed(net.minecraft.world.item.Items.NETHER_WART));
        org.junit.jupiter.api.Assertions.assertNull(Brews.cropForSeed(net.minecraft.world.item.Items.DIAMOND));
    }

    @Test
    void natureIngredientsAreRegistered() {
        Brews.bootstrap();
        assertTrue(Brews.isIngredient(net.minecraft.world.item.Items.STRING));
        assertTrue(Brews.isIngredient(net.minecraft.world.item.Items.LILY_PAD));
        assertTrue(Brews.isIngredient(net.minecraft.world.item.Items.DEAD_BUSH));
        assertTrue(Brews.isIngredient(net.minecraft.world.item.Items.QUARTZ));
        assertTrue(Brews.isIngredient(net.minecraft.world.item.Items.WHEAT_SEEDS));
        assertTrue(Brews.isIngredient(com.emoniph.witchery.init.WitcheryItems.ENT_TWIG.get()));
    }

    @Test
    void deferredActionIngredientsAreRegistered() {
        Brews.bootstrap();
        assertTrue(Brews.isIngredient(net.minecraft.world.item.Items.ENDER_PEARL));   // Transpose
        assertTrue(Brews.isIngredient(net.minecraft.world.item.Items.BONE));          // Raising
        assertTrue(Brews.isIngredient(net.minecraft.world.item.Items.CREEPER_HEAD));  // Spread Debuffs
    }

    @Test
    void spreadDebuffsMathMatchesSource() {
        // radius = (strength + 1) * 4
        assertEquals(4.0, Brews.sourceSpreadDebuffsRadius(0));
        assertEquals(20.0, Brews.sourceSpreadDebuffsRadius(4));
        // maxBuffs = ceil((strength + 1) * powerFactor)
        assertEquals(1, Brews.sourceSpreadDebuffsMaxBuffs(0, 1.0));
        assertEquals(5, Brews.sourceSpreadDebuffsMaxBuffs(4, 1.0));
        assertEquals(8, Brews.sourceSpreadDebuffsMaxBuffs(4, 1.5));
    }

    @Test
    void spreadDebuffEligibilityMatchesSource() {
        // HARMFUL + curable + amplifier <= max(strength-1, 1)
        var slowness = net.minecraft.world.effect.MobEffects.SLOWNESS;
        assertTrue(Brews.shouldSpreadDebuff(slowness, 1, true, 1));   // amp 1 <= max(0,1)=1
        assertTrue(Brews.shouldSpreadDebuff(slowness, 2, true, 4));   // amp 2 <= max(3,1)=3
        org.junit.jupiter.api.Assertions.assertFalse(Brews.shouldSpreadDebuff(slowness, 4, true, 4)); // amp 4 > 3
        org.junit.jupiter.api.Assertions.assertFalse(Brews.shouldSpreadDebuff(slowness, 0, false, 4)); // not curable
        // beneficial effects never spread
        org.junit.jupiter.api.Assertions.assertFalse(
                Brews.shouldSpreadDebuff(net.minecraft.world.effect.MobEffects.SPEED, 0, true, 4));
    }

    @Test
    void raiseDeadExtraCountMatchesSource() {
        // strength 0 → never any extra
        assertEquals(0, Brews.sourceRaiseDeadExtraCount(0, 0.0, 0.0, 0.0));
        // strength 1: only the first roll matters, threshold 0.5
        assertEquals(1, Brews.sourceRaiseDeadExtraCount(1, 0.4, 0.9, 0.9));
        assertEquals(0, Brews.sourceRaiseDeadExtraCount(1, 0.6, 0.0, 0.0));
        // strength 4: all three roll, thresholds 2.0 / 1.0 / 1.0 (all rolls < 1.0 pass)
        assertEquals(3, Brews.sourceRaiseDeadExtraCount(4, 0.99, 0.99, 0.99));
    }
}
