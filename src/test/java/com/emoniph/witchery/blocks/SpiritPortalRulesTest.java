package com.emoniph.witchery.blocks;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritPortalRulesTest {

    @Test
    void snowFrameLeavesOnlyItsFourCornersOptional() {
        assertTrue(SpiritPortalBlock.isFrameCorner(-1, -1));
        assertTrue(SpiritPortalBlock.isFrameCorner(-1, 2));
        assertTrue(SpiritPortalBlock.isFrameCorner(2, -1));
        assertTrue(SpiritPortalBlock.isFrameCorner(2, 2));
        assertFalse(SpiritPortalBlock.isFrameCorner(-1, 0));
        assertFalse(SpiritPortalBlock.isFrameCorner(0, -1));
        assertFalse(SpiritPortalBlock.isFrameCorner(0, 0));
    }

    @Test
    void portalPresentationKeepsTheSourceAxisTintAndParticleCount() {
        assertTrue(SpiritPortalBlock.AXIS.getPossibleValues().contains(Direction.Axis.X));
        assertTrue(SpiritPortalBlock.AXIS.getPossibleValues().contains(Direction.Axis.Z));
        assertEquals(0x00FF66, SpiritPortalBlock.SOURCE_PORTAL_RGB);
        assertEquals(2, SpiritPortalBlock.SOURCE_PARTICLES_PER_ANIMATE_TICK);
    }

    @Test
    void fastingUsesTheSourceHungerNightmarePenalty() {
        assertEquals(MobEffects.HUNGER, DreamWeaveType.FASTING.nightmareEffect());
    }
}
