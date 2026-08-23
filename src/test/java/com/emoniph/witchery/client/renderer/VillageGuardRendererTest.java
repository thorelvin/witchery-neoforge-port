package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

class VillageGuardRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void guardAndVampireUseTheSourceShadowAndLegacyLivingAnchor() {
        assertEquals(0.5F, VillageGuardRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertEquals(0.5F, VampireRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertAnchor(VillageGuardRenderer.LEGACY_BASE_DELTA_Y, false, -1.5078125F);
        assertAnchor(VillageGuardRenderer.LEGACY_BASE_DELTA_Y, true, -1.6328125F);
        assertAnchor(VampireRenderer.LEGACY_BASE_DELTA_Y, false, -1.5078125F);
        assertAnchor(VampireRenderer.LEGACY_BASE_DELTA_Y, true, -1.6328125F);
    }

    private static void assertAnchor(float correction, boolean crouching, float expected) {
        PoseStack pose = new PoseStack();
        pose.translate(0.0F, correction - (crouching ? 0.125F : 0.0F), 0.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        assertEquals(expected, pose.last().pose().get(3, 1), EPSILON);
    }
}
