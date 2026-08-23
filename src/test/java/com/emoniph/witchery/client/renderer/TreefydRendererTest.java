package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

class TreefydRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void sourceWholeEntityRollTextureShadowAndLegacyAnchorArePreserved() {
        assertEquals("witchery:textures/entity/treefyd.png", TreefydRenderer.TEXTURE.toString());
        assertEquals(0.5F, TreefydRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertEquals(0.0F, TreefydRenderer.sourceWalkingRoll(4.0F, 0.009F), EPSILON);
        assertEquals(5.0F * sourceTriangle(10.0F, 13.0F),
                TreefydRenderer.sourceWalkingRoll(4.0F, 0.5F), EPSILON);
        PoseStack pose = new PoseStack();
        pose.translate(0.0F, TreefydRenderer.LEGACY_BASE_DELTA_Y, 0.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        assertEquals(-1.5078125F, pose.last().pose().get(3, 1), EPSILON);
    }

    private static float sourceTriangle(float value, float limit) {
        return (Math.abs(value % limit - limit * 0.5F) - limit * 0.25F) / (limit * 0.25F);
    }
}
