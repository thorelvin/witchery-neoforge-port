package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

class WitchHunterRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void sourceSkinRoutingShadowAndLegacyAnchorsArePreserved() {
        assertEquals(3, WitchHunterRenderer.SKINS.length);
        assertEquals("witchery:textures/entity/witchhunter1.png",
                WitchHunterRenderer.SKINS[0].toString());
        assertEquals("witchery:textures/entity/witchhunter2.png",
                WitchHunterRenderer.SKINS[1].toString());
        assertEquals("witchery:textures/entity/witchhunter3.png",
                WitchHunterRenderer.SKINS[2].toString());
        assertEquals(0.5F, WitchHunterRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertAnchor(false, -1.5078125F);
        assertAnchor(true, -1.6328125F);
    }

    private static void assertAnchor(boolean crouching, float expected) {
        PoseStack pose = new PoseStack();
        pose.translate(0.0F, WitchHunterRenderer.LEGACY_BASE_DELTA_Y
                - (crouching ? 0.125F : 0.0F), 0.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        assertEquals(expected, pose.last().pose().get(3, 1), EPSILON);
    }
}
