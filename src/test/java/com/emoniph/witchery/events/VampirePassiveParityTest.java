package com.emoniph.witchery.events;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VampirePassiveParityTest {
    @Test
    void humanFormDamageUsesTheSourceMultiplierTiers() {
        assertEquals(0.0D, VampireEvents.sourceVampireDamageBoost(0));
        assertEquals(1.0D, VampireEvents.sourceVampireDamageBoost(1));
        assertEquals(1.0D, VampireEvents.sourceVampireDamageBoost(3));
        assertEquals(2.0D, VampireEvents.sourceVampireDamageBoost(4));
        assertEquals(2.0D, VampireEvents.sourceVampireDamageBoost(6));
        assertEquals(3.0D, VampireEvents.sourceVampireDamageBoost(7));
        assertEquals(3.0D, VampireEvents.sourceVampireDamageBoost(10));
    }

    @Test
    void levelThreeSneakStrikeUsesTheSourceLaunchVector() {
        assertFalse(VampireEvents.sourceCanLaunch(2, true, true));
        assertFalse(VampireEvents.sourceCanLaunch(3, false, true));
        assertFalse(VampireEvents.sourceCanLaunch(3, true, false));
        assertTrue(VampireEvents.sourceCanLaunch(3, true, true));

        Vec3 motion = VampireEvents.sourceLaunchMotion(new Vec3(0.5D, -0.75D, -0.25D));
        assertEquals(0.9D, motion.x, 0.0001D);
        assertEquals(0.9D, motion.y, 0.0001D);
        assertEquals(-0.45D, motion.z, 0.0001D);
    }

    @Test
    void levelSixCrushingKeepsTheExactSourceGateAndBlockSet() {
        assertFalse(VampireEvents.sourceCanCrushBlock(5, true, true, false, 20,
                Blocks.STONE.defaultBlockState()));
        assertFalse(VampireEvents.sourceCanCrushBlock(6, false, true, false, 20,
                Blocks.STONE.defaultBlockState()));
        assertFalse(VampireEvents.sourceCanCrushBlock(6, true, false, false, 20,
                Blocks.STONE.defaultBlockState()));
        assertFalse(VampireEvents.sourceCanCrushBlock(6, true, true, true, 20,
                Blocks.STONE.defaultBlockState()));
        assertFalse(VampireEvents.sourceCanCrushBlock(6, true, true, false, 0,
                Blocks.STONE.defaultBlockState()));

        assertTrue(VampireEvents.sourceCanCrushBlock(6, true, true, false, 20,
                Blocks.STONE.defaultBlockState()));
        assertTrue(VampireEvents.sourceCanCrushBlock(6, true, true, false, 20,
                Blocks.NETHERRACK.defaultBlockState()));
        assertTrue(VampireEvents.sourceCanCrushBlock(6, true, true, false, 20,
                Blocks.COBBLESTONE.defaultBlockState()));
        assertFalse(VampireEvents.sourceCanCrushBlock(10, true, true, false, 20,
                Blocks.GRAVEL.defaultBlockState()));
    }
}
