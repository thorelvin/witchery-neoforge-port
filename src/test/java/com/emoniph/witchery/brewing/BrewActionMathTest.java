package com.emoniph.witchery.brewing;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain-JVM regression lock for the custom-brew <em>action</em> roster's source-parity math
 * ({@link Brews} {@code source*} helpers). These are pure functions transcribed from 1.7.10
 * {@code ModifiersEffect}/{@code BrewActionList}, so they can be pinned without a world — complementing
 * {@link BrewSplashMathTest} (splash falloff) and {@code BrewLogicTest} (the compose engine).
 */
class BrewActionMathTest {

    // ── Bane families (Undead/Arthropods/Demons): base<<min(strength, ceil) * powerFactor ──
    @Test
    void baneDamageShiftsByCappedStrengthAndMatchTier() {
        // Matching target = base 10, non-matching = base 3; strength capped at 1 unless ceiling disabled (3).
        assertEquals(10, Brews.sourceBaneDamage(true, 0, false, 1.0));
        assertEquals(20, Brews.sourceBaneDamage(true, 1, false, 1.0));
        assertEquals(20, Brews.sourceBaneDamage(true, 5, false, 1.0));   // strength capped at 1
        assertEquals(80, Brews.sourceBaneDamage(true, 5, true, 1.0));    // ceiling disabled -> cap 3 -> 10<<3
        assertEquals(3, Brews.sourceBaneDamage(false, 0, false, 1.0));   // non-matching
        assertEquals(10, Brews.sourceBaneDamage(true, 1, false, 0.5));   // splash falloff (ceil(20*0.5))
    }

    @Test
    void harmWerewolvesUsesLowerBaseTiers() {
        assertEquals(3, Brews.sourceHarmWerewolvesDamage(true, 0, false, 1.0));
        assertEquals(6, Brews.sourceHarmWerewolvesDamage(true, 1, false, 1.0));
        assertEquals(24, Brews.sourceHarmWerewolvesDamage(true, 5, true, 1.0)); // 3<<3
        assertEquals(1, Brews.sourceHarmWerewolvesDamage(false, 0, false, 1.0));
    }

    // ── Extinguish / Dissipate ──
    @Test
    void extinguishGatedByNetherUnlessStrong() {
        assertTrue(Brews.sourceExtinguishAppliesInDimension(false, 0));  // overworld always
        assertFalse(Brews.sourceExtinguishAppliesInDimension(true, 0));  // nether needs strength > 1
        assertFalse(Brews.sourceExtinguishAppliesInDimension(true, 1));
        assertTrue(Brews.sourceExtinguishAppliesInDimension(true, 2));
    }

    @Test
    void extinguishBlazeDamageAndDissipateScale() {
        assertEquals(0.0F, Brews.sourceExtinguishBlazeDamage(0));
        assertEquals(8.0F, Brews.sourceExtinguishBlazeDamage(1));
        assertEquals(12.0F, Brews.sourceExtinguishBlazeDamage(2));
        assertEquals(5.0F, Brews.sourceDissipateUndeadDamage(0));
        assertEquals(15.0F, Brews.sourceDissipateUndeadDamage(2));
    }

    @Test
    void dissipateRemovesWitcheryGasAlwaysButFluidGasOnlyWhenStrong() {
        assertTrue(Brews.sourceDissipateRemovesGas(true, false, 0));
        assertFalse(Brews.sourceDissipateRemovesGas(false, true, 0));
        assertTrue(Brews.sourceDissipateRemovesGas(false, true, 1));
        assertFalse(Brews.sourceDissipateRemovesGas(false, false, 5));
    }

    // ── Block-action radii / counts ──
    @Test
    void blockHitAndCircleRadiiScaleWithStrength() {
        assertEquals(2, Brews.sourceBlockHitRadius(0));
        assertEquals(4, Brews.sourceBlockHitRadius(1));
        assertEquals(6, Brews.sourceBlockHitRadius(2));
        assertEquals(7, Brews.sourceBlockCircleRadius(4, 3));
        assertEquals(8, Brews.sourceGrowFlowersChanceBound(0)); // 1-in-(8-strength)
        assertEquals(1, Brews.sourceGrowFlowersChanceBound(7));
        assertEquals(1, Brews.sourceFertilizationApplications(0));
        assertEquals(4, Brews.sourceFertilizationApplications(3));
    }

    // ── Pull / Air-hike motion ──
    @Test
    void pullMotionScalesHorizontallyAndFixedVertical() {
        Vec3 motion = Brews.sourcePullMotion(new Vec3(0, 0, 0), new Vec3(10, 0, 0));
        assertEquals(1.5, motion.x, 1e-9);   // 10 * 0.15
        assertEquals(0.4, motion.y, 1e-9);   // fixed vertical lift
        assertEquals(0.0, motion.z, 1e-9);
        assertSame(Vec3.ZERO, Brews.sourcePullMotion(new Vec3(1, 2, 3), new Vec3(1, 2, 3)));
    }

    @Test
    void pullUsesCasterOnlyWhenStrongAndRealCaster() {
        assertFalse(Brews.sourcePullUsesCaster(0, false));
        assertTrue(Brews.sourcePullUsesCaster(1, false));
        assertFalse(Brews.sourcePullUsesCaster(1, true)); // fake caster (e.g. dispenser) -> pull to impact
    }

    @Test
    void airHikeMotionY() {
        assertEquals(0.6, Brews.sourceAirHikeMotionY(0), 1e-9);
        assertEquals(0.8, Brews.sourceAirHikeMotionY(1), 1e-9);
        assertEquals(1.2, Brews.sourceAirHikeMotionY(3), 1e-9);
    }

    // ── Weaken Vampires ──
    @Test
    void weakenVampiresDrainsBloodAndClampsWeakness() {
        assertEquals(50, Brews.sourceWeakenVampiresBloodLoss(0));
        assertEquals(70, Brews.sourceWeakenVampiresBloodLoss(1));
        assertEquals(50, Brews.sourceWeakenVampiresBloodAfter(100, 0)); // 100 - 50
        assertEquals(0, Brews.sourceWeakenVampiresBloodAfter(60, 1));   // 60 - 70 floored at 0
        assertEquals(0, Brews.sourceWeakenVampiresBloodAfter(0, 0));    // already empty, untouched
        assertEquals(0, Brews.sourceWeakenVampiresWeaknessAmplifier(0));
        assertEquals(5, Brews.sourceWeakenVampiresWeaknessAmplifier(5));
        assertEquals(10, Brews.sourceWeakenVampiresWeaknessAmplifier(15)); // ceiling 10
    }

    @Test
    void modifiedActionDurationScalesByDurationModsAndFactor() {
        assertEquals(1800, Brews.sourceModifiedActionDuration(1800, 0, 1.0));
        assertEquals(3600, Brews.sourceModifiedActionDuration(1800, 1, 1.0)); // (mods+1) multiplier
        assertEquals(900, Brews.sourceModifiedActionDuration(1800, 0, 0.5));  // splash half
    }

    // ── Steal Buffs ──
    @Test
    void stealBuffsRadiusAndCount() {
        assertEquals(8.0, Brews.sourceStealBuffsRadius(0), 1e-9);
        assertEquals(48.0, Brews.sourceStealBuffsRadius(5), 1e-9);
        assertEquals(2, Brews.sourceStealBuffsMaxBuffs(0, 1.0));
        assertEquals(12, Brews.sourceStealBuffsMaxBuffs(5, 1.0));
        assertEquals(1, Brews.sourceStealBuffsMaxBuffs(0, 0.1)); // ceil(0.2)
    }

    // ── Potion Longevity (Duration Boost) ──
    @Test
    void potionLongevityAddsCappedBoostAndQueasyAftermath() {
        assertEquals(3600, Brews.sourcePotionLongevityBoostTicks(1.0));
        assertEquals(1800, Brews.sourcePotionLongevityBoostTicks(0.5));
        assertEquals(2000, Brews.sourcePotionLongevityDuration(1000, 1.0)); // boost capped by remaining
        assertEquals(8600, Brews.sourcePotionLongevityDuration(5000, 1.0)); // 5000 + min(5000,3600)
        assertEquals(0, Brews.sourcePotionLongevityDuration(0, 1.0));
        assertEquals(14400, Brews.sourcePotionLongevityQueasyDuration(0)); // 3*4*60*20
        assertEquals(3600, Brews.sourcePotionLongevityQueasyDuration(4));  // floors at 3*1*60*20
        assertEquals(3600, Brews.sourcePotionLongevityQueasyDuration(5));
        assertEquals(2400, Brews.sourcePotionLongevityBackfireDuration(true));  // hunger
        assertEquals(6000, Brews.sourcePotionLongevityBackfireDuration(false)); // nausea
    }

    // ── Moving brew fluid: gas-cloud lifetime + liquid decay + extent expansion ──
    @Test
    void purifiedMilkDiseaseAndRitualDebuffCleanupUseSourceThresholds() {
        assertFalse(Brews.sourcePurifiedMilkClearsDisease(1));
        assertTrue(Brews.sourcePurifiedMilkClearsDisease(2));
        assertFalse(Brews.sourcePurifiedMilkClearsDiseaseBlocks(2));
        assertTrue(Brews.sourcePurifiedMilkClearsDiseaseBlocks(3));
        assertEquals(9, Brews.SOURCE_PURIFIED_MILK_DISEASE_BLOCK_CLEANUP_RADIUS);

        assertEquals(0.01D, Brews.sourceRitualCureDebuffReapplyChance(0, 1), 1e-9);
        assertEquals(0.25D, Brews.sourceRitualCureDebuffReapplyChance(1, 1), 1e-9);
        assertEquals(0.75D, Brews.sourceRitualCureDebuffReapplyChance(2, 1), 1e-9);
        assertEquals(3, Brews.sourceRitualCureDebuffAmplifierAfterBackfire(2));
    }

    @Test
    void fluidLifetimeDecayDenominatorAndGasDuration() {
        assertEquals(5, Brews.sourceBrewFluidDecayDenominator(0));
        assertEquals(10, Brews.sourceBrewFluidDecayDenominator(1));
        assertEquals(25, Brews.sourceBrewFluidDecayDenominator(2));
        assertEquals(50, Brews.sourceBrewFluidDecayDenominator(3));
        assertEquals(50, Brews.sourceBrewFluidDecayDenominator(5)); // clamped to 3
        assertEquals(600, Brews.gasCloudDurationTicks(0));
        assertEquals(1200, Brews.gasCloudDurationTicks(1));
        assertEquals(3000, Brews.gasCloudDurationTicks(2));
        assertEquals(6000, Brews.gasCloudDurationTicks(3));
        assertEquals(120, Brews.sourceBrewGasMaxRunTicks(0));
        assertEquals(240, Brews.sourceBrewGasMaxRunTicks(1));
        assertEquals(600, Brews.sourceBrewGasMaxRunTicks(2));
        assertEquals(1200, Brews.sourceBrewGasMaxRunTicks(3));
    }

    @Test
    void fluidExpansionGrowsWithExtentAndCaps() {
        assertEquals(4, Brews.sourceBrewFluidExpansion(0));
        assertEquals(7, Brews.sourceBrewFluidExpansion(3));
        assertEquals(10, Brews.sourceBrewFluidExpansion(6));
        assertEquals(10, Brews.sourceBrewFluidExpansion(10)); // capped at 10
    }

    // ── Adjacent-block action pulse (moving brew liquid) ──
    @Test
    void adjacentBlockPulseReducesStrengthByOneFlooredAtZero() {
        assertEquals(0, Brews.pulsePenalisedStrength(0)); // source strengthPenalty++ floors at 0
        assertEquals(0, Brews.pulsePenalisedStrength(1));
        assertEquals(4, Brews.pulsePenalisedStrength(5));
    }
}
