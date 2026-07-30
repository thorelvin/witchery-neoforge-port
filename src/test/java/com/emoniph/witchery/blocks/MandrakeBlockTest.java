package com.emoniph.witchery.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emoniph.witchery.entity.MandrakeEntity;
import net.minecraft.world.Difficulty;
import org.junit.jupiter.api.Test;

class MandrakeBlockTest {
    @Test
    void entityPresentationConstantsMatchSource() {
        assertEquals(0.6F, MandrakeEntity.SOURCE_WIDTH);
        assertEquals(0.9F, MandrakeEntity.SOURCE_HEIGHT);
        assertEquals(0.5F, MandrakeEntity.SOURCE_SHADOW_RADIUS);
        assertEquals(32.0D, MandrakeEntity.SOURCE_FOLLOW_RANGE);
        assertEquals(20, MandrakeBlock.SOURCE_EMERGENCE_PARTICLES);
    }

    @Test
    void matureMandrakeUsesSourceDaytimeAwakeningBoundary() {
        assertTrue(MandrakeBlock.isSourceDaytime(0L));
        assertTrue(MandrakeBlock.isSourceDaytime(12999L));
        assertFalse(MandrakeBlock.isSourceDaytime(13000L));
        assertTrue(MandrakeBlock.shouldSummonMandrake(4, Difficulty.NORMAL, true, 0.0D));
        assertTrue(MandrakeBlock.shouldSummonMandrake(4, Difficulty.NORMAL, true, 0.9D));
        assertFalse(MandrakeBlock.shouldSummonMandrake(4, Difficulty.NORMAL, true, Math.nextUp(0.9D)));
    }

    @Test
    void matureMandrakeUsesSourceNighttimeAwakeningBoundary() {
        assertTrue(MandrakeBlock.shouldSummonMandrake(4, Difficulty.HARD, false, 0.1D));
        assertFalse(MandrakeBlock.shouldSummonMandrake(4, Difficulty.HARD, false, Math.nextUp(0.1D)));
    }

    @Test
    void immatureOrPeacefulMandrakeNeverAwakens() {
        assertFalse(MandrakeBlock.shouldSummonMandrake(3, Difficulty.NORMAL, true, 0.0D));
        assertFalse(MandrakeBlock.shouldSummonMandrake(4, Difficulty.PEACEFUL, true, 0.0D));
    }
}
