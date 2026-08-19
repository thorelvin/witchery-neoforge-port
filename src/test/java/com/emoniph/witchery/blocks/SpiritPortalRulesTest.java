package com.emoniph.witchery.blocks;

import net.minecraft.world.effect.MobEffects;
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
    void fastingUsesTheSourceHungerNightmarePenalty() {
        assertEquals(MobEffects.HUNGER, DreamWeaveType.FASTING.nightmareEffect());
    }
}
