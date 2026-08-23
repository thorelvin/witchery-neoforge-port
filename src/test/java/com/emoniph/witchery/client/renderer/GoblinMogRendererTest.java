package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

class GoblinMogRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void sourceShadowTextureStandingAndCrouchingAnchorsArePreserved() {
        assertEquals(0.5F, GoblinMogRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertEquals("witchery:textures/entity/mog.png", GoblinMogRenderer.TEXTURE.toString());
        assertAnchor(false, -1.5078125F);
        assertAnchor(true, -1.6328125F);
    }

    private static void assertAnchor(boolean crouching, float expected) {
        PoseStack pose = new PoseStack();
        pose.translate(0, GoblinMogRenderer.LEGACY_BASE_DELTA_Y - (crouching ? 0.125F : 0), 0);
        pose.translate(0, -1.501F, 0);
        assertEquals(expected, pose.last().pose().get(3, 1), EPSILON);
    }
}
