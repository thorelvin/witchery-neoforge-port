package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

class EntRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void sourceWalkingRollShadowTextureAndLegacyAnchorArePreserved() {
        assertEquals(0.0F, EntRenderer.sourceWalkingRollDegrees(3.0F, 0.009F), EPSILON);
        float shifted = 3.0F + 6.0F;
        float triangle = (Math.abs(shifted % 13.0F - 6.5F) - 3.25F) / 3.25F;
        assertEquals(6.5F * triangle, EntRenderer.sourceWalkingRollDegrees(3.0F, 0.4F), EPSILON);
        assertEquals(0.5F, EntRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertEquals("witchery:textures/entity/ent.png", EntRenderer.TEXTURE.toString());

        PoseStack pose = new PoseStack();
        pose.translate(0.0F, EntRenderer.LEGACY_BASE_DELTA_Y, 0.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        assertEquals(-1.5078125F, pose.last().pose().get(3, 1), EPSILON);
    }
}
