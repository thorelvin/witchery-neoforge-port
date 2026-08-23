package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

class ParasyticLouseRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void sourceTextureShadowDeathFlipAndLegacyAnchorArePreserved() {
        assertEquals("witchery:textures/entity/louse.png", ParasyticLouseRenderer.TEXTURE.toString());
        assertEquals(0.3F, ParasyticLouseRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertEquals(180.0F, ParasyticLouseRenderer.SOURCE_DEATH_FLIP_DEGREES, EPSILON);
        PoseStack pose = new PoseStack();
        pose.translate(0.0F, ParasyticLouseRenderer.LEGACY_BASE_DELTA_Y, 0.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        assertEquals(-1.5078125F, pose.last().pose().get(3, 1), EPSILON);
    }
}
