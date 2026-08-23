package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

class GoblinGulgRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void sourceShadowTextureStandingAndCrouchingAnchorsArePreserved() {
        assertEquals(0.5F, GoblinGulgRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertEquals("witchery:textures/entity/gulg.png", GoblinGulgRenderer.TEXTURE.toString());
        assertAnchor(false, -1.5078125F);
        assertAnchor(true, -1.6328125F);
    }

    private static void assertAnchor(boolean crouching, float expected) {
        PoseStack pose = new PoseStack();
        pose.translate(0, GoblinGulgRenderer.LEGACY_BASE_DELTA_Y - (crouching ? 0.125F : 0), 0);
        pose.translate(0, -1.501F, 0);
        assertEquals(expected, pose.last().pose().get(3, 1), EPSILON);
    }
}
