package com.emoniph.witchery.ritual;

import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistentExpandingRiteParityTest {
    @Test
    void fertilityKeepsSourceExpansionGeometryAndCadence() {
        assertEquals(50, RitualExecutor.FERTILITY_BASE_RADIUS);
        assertEquals(15, RitualExecutor.FERTILITY_HEIGHT);
        assertEquals(5, RitualExecutor.FERTILITY_PULSE_INTERVAL);
        assertEquals(50, RitualExecutor.fertilityMaxRadius(0));
        assertEquals(62, RitualExecutor.fertilityMaxRadius(6));
    }

    @Test
    void forestationKeepsSourcePulseGeometryAndCovenScaling() {
        assertEquals(20, RitualExecutor.FORESTATION_BASE_RADIUS);
        assertEquals(8, RitualExecutor.FORESTATION_HEIGHT);
        assertEquals(60, RitualExecutor.FORESTATION_BASE_DURATION);
        assertEquals(20, RitualExecutor.FORESTATION_PULSE_INTERVAL);
        assertEquals(11, RitualExecutor.FORESTATION_BONEMEAL_ATTEMPTS);
        assertEquals(20, RitualExecutor.forestationRadius(0));
        assertEquals(32, RitualExecutor.forestationRadius(6));
        assertEquals(60, RitualExecutor.forestationDuration(0));
        assertEquals(90, RitualExecutor.forestationDuration(6));
    }

    @Test
    void expandingCursesKeepSourceGeometryCadenceAndUpkeep() {
        assertEquals(15, RitualExecutor.EXPANDING_CURSE_HEIGHT);
        assertEquals(5, RitualExecutor.EXPANDING_CURSE_PULSE_INTERVAL);
        assertEquals(80, RitualExecutor.BLINDNESS_BASE_RADIUS);
        assertEquals(80, RitualExecutor.BLIGHT_BASE_RADIUS);
        assertEquals(20, RitualExecutor.HELL_ON_EARTH_BASE_RADIUS);
        assertEquals(200.0F, RitualExecutor.HELL_ON_EARTH_UPKEEP);
    }

    @Test
    void blightClassifiesOnlySourceTerrainFamilies() {
        assertEquals(RitualExecutor.BlightTerrainOutcome.REMOVE_PLANT,
                RitualExecutor.blightTerrainOutcome(Blocks.SHORT_GRASS.defaultBlockState()));
        assertEquals(RitualExecutor.BlightTerrainOutcome.WITHER_TO_DEAD_BUSH,
                RitualExecutor.blightTerrainOutcome(Blocks.POPPY.defaultBlockState()));
        assertEquals(RitualExecutor.BlightTerrainOutcome.WITHER_TO_DEAD_BUSH,
                RitualExecutor.blightTerrainOutcome(Blocks.CARROTS.defaultBlockState()));
        assertEquals(RitualExecutor.BlightTerrainOutcome.FARMLAND_TO_SAND,
                RitualExecutor.blightTerrainOutcome(Blocks.FARMLAND.defaultBlockState()));
        assertEquals(RitualExecutor.BlightTerrainOutcome.DECAY_GROUND,
                RitualExecutor.blightTerrainOutcome(Blocks.PODZOL.defaultBlockState()));
        assertEquals(RitualExecutor.BlightTerrainOutcome.NONE,
                RitualExecutor.blightTerrainOutcome(Blocks.WITHER_ROSE.defaultBlockState()));
    }

    @Test
    void hellOnEarthKeepsSourceGroundBandsAndMobWeights() {
        assertEquals(2, RitualExecutor.hellOnEarthGroundChanceBound(5));
        assertEquals(4, RitualExecutor.hellOnEarthGroundChanceBound(6));
        assertEquals(4, RitualExecutor.hellOnEarthGroundChanceBound(9));
        assertEquals(6, RitualExecutor.hellOnEarthGroundChanceBound(10));
        assertEquals(RitualExecutor.HellOnEarthMobKind.DEMON, RitualExecutor.hellOnEarthMobKind(0.0199D));
        assertEquals(RitualExecutor.HellOnEarthMobKind.GHAST, RitualExecutor.hellOnEarthMobKind(0.02D));
        assertEquals(RitualExecutor.HellOnEarthMobKind.BLAZE, RitualExecutor.hellOnEarthMobKind(0.10D));
        assertEquals(RitualExecutor.HellOnEarthMobKind.MAGMA_CUBE, RitualExecutor.hellOnEarthMobKind(0.40D));
        assertEquals(RitualExecutor.HellOnEarthMobKind.ZOMBIFIED_PIGLIN,
                RitualExecutor.hellOnEarthMobKind(0.60D));
    }
}
