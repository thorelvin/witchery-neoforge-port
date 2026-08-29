package com.emoniph.witchery.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CorpseSourceParityTest {
    @Test
    void bodyDimensionsAndRenderConstantsMatchEntityCorpse() {
        assertEquals(1.2F, CorpseEntity.SOURCE_WIDTH);
        assertEquals(0.5F, CorpseEntity.SOURCE_HEIGHT);
        assertEquals(0.0F, CorpseEntity.SOURCE_SHADOW_RADIUS);
        assertEquals(0.9F, CorpseEntity.SOURCE_RENDER_X_OFFSET);
        assertEquals(0.25F, CorpseEntity.SOURCE_RENDER_Y_OFFSET);
        assertEquals(90.0F, CorpseEntity.SOURCE_PRONE_ROTATION_DEGREES);
        assertEquals(1.0D, CorpseEntity.SOURCE_MOVEMENT_SPEED);
        assertTrue(CorpseEntity.OWNER_NAMEPLATE_VISIBLE,
                "The port must present the owner custom name assigned by the source entry path");
    }

    @Test
    void spawnCoordinatesUseTheSourceBlockCenterRule() {
        assertEquals(12.5D, CorpseEntity.sourceSpawnCenter(12.99D));
        assertEquals(12.5D, CorpseEntity.sourceSpawnCenter(12.0D));
        assertEquals(-0.5D, CorpseEntity.sourceSpawnCenter(-0.01D));
        assertEquals(-0.5D, CorpseEntity.sourceSpawnCenter(-1.0D));
    }
}
