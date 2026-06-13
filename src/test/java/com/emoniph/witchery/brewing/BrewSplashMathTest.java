package com.emoniph.witchery.brewing;

import com.emoniph.witchery.entity.ThrownBrewEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrewSplashMathTest {

    @Test
    void sourceInstantSplashUsesHalfDurationOnDirectEntityHits() {
        assertEquals(1.0, Brews.splashPowerFactor(0.0, true), 0.00001);
        assertEquals(1800, Brews.scaledSplashDuration(3600, Brews.splashPowerFactor(0.0, true)));
    }

    @Test
    void sourceInstantSplashFallsOffAcrossThreeBlockRadius() {
        assertEquals(0.5, Brews.splashPowerFactor(1.5, false), 0.00001);
        assertEquals(900, Brews.scaledSplashDuration(3600, Brews.splashPowerFactor(1.5, false)));
        assertEquals(0.0, Brews.splashPowerFactor(3.0, false), 0.00001);
        assertEquals(0, Brews.scaledSplashDuration(3600, Brews.splashPowerFactor(3.0, false)));
    }

    @Test
    void sourceInstantSplashExtentWidensFalloffRadius() {
        assertEquals(0.25, Brews.splashPowerFactor(3.0, false, 4.0), 0.00001);
        assertEquals(450, Brews.scaledSplashDuration(3600, Brews.splashPowerFactor(3.0, false, 4.0)));
        assertEquals(0.0, Brews.splashPowerFactor(4.0, false, 4.0), 0.00001);
    }

    @Test
    void thrownBrewsUseSourceProjectileConstants() {
        assertEquals(-20.0F, ThrownBrewEntity.SOURCE_THROW_PITCH_OFFSET, 0.00001F);
        assertEquals(0.75F, ThrownBrewEntity.SOURCE_THROW_VELOCITY, 0.00001F);
        assertEquals(1.0F, ThrownBrewEntity.SOURCE_THROW_INACCURACY, 0.00001F);
        assertEquals(0.05D, ThrownBrewEntity.SOURCE_GRAVITY, 0.00001D);
    }

    @Test
    void sourceMovingBrewFluidPlacementUsesHitFaceAdjacentBlock() {
        BlockPos hit = new BlockPos(10, 64, -3);
        assertEquals(hit.relative(Direction.UP), Brews.sourceMovingBrewFluidPos(
                new BlockHitResult(Vec3.atCenterOf(hit), Direction.UP, hit, false)));
        assertEquals(hit.relative(Direction.WEST), Brews.sourceMovingBrewFluidPos(
                new BlockHitResult(Vec3.atCenterOf(hit), Direction.WEST, hit, false)));
    }
}
