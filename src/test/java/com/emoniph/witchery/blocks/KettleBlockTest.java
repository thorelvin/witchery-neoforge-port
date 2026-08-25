package com.emoniph.witchery.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class KettleBlockTest {
    @Test
    void sourceAmbientCuePriorityIsPreserved() {
        assertEquals(KettleBlock.AmbientCue.NONE,
                KettleBlock.ambientCue(false, false, false, false));
        assertEquals(KettleBlock.AmbientCue.BREWING,
                KettleBlock.ambientCue(false, false, false, true));
        assertEquals(KettleBlock.AmbientCue.READY_UNPOWERED,
                KettleBlock.ambientCue(false, true, false, true));
        assertEquals(KettleBlock.AmbientCue.READY_POWERED,
                KettleBlock.ambientCue(false, true, true, true));
        assertEquals(KettleBlock.AmbientCue.RUINED,
                KettleBlock.ambientCue(true, true, true, true));
    }
}
