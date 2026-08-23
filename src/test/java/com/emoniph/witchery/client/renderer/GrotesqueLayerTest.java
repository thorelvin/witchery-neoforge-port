package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

class GrotesqueLayerTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void potionAndFixedAuraMatricesMatchTheirSourceRoutes() {
        assertSourceMatrix(true, 1.313F, -0.7654625F);
        assertSourceMatrix(false, 1.3F, -0.7728125F);
        assertEquals("witchery:textures/entity/demon.png", GrotesqueLayer.TEXTURE.toString());
        assertEquals(0x26FFFFFF, GrotesqueLayer.INVISIBLE_TINT);
    }

    @Test
    void sourceVisibilityRulesArePreservedForBothRoutes() {
        GrotesqueLayer.OverlayStatus inactive = new GrotesqueLayer.OverlayStatus(false, false);
        GrotesqueLayer.OverlayStatus potion = new GrotesqueLayer.OverlayStatus(true, false);
        GrotesqueLayer.OverlayStatus fixed = new GrotesqueLayer.OverlayStatus(false, true);

        assertFalse(GrotesqueLayer.shouldRender(null, false, false));
        assertFalse(GrotesqueLayer.shouldRender(inactive, false, false));
        assertTrue(GrotesqueLayer.shouldRender(potion, false, false));
        assertTrue(GrotesqueLayer.shouldRender(fixed, false, false));
        assertTrue(GrotesqueLayer.shouldRender(potion, true, false));
        assertFalse(GrotesqueLayer.shouldRender(fixed, true, false));
        assertFalse(GrotesqueLayer.shouldRender(potion, true, true));
    }

    private static void assertSourceMatrix(boolean effectActive, float expectedScale, float expectedY) {
        PoseStack pose = new PoseStack();
        pose.translate(0.0F, -1.501F, 0.0F);
        GrotesqueLayer.applySourceTransform(pose, effectActive);
        assertEquals(expectedScale, pose.last().pose().get(0, 0), EPSILON);
        assertEquals(expectedScale, pose.last().pose().get(1, 1), EPSILON);
        assertEquals(expectedScale, pose.last().pose().get(2, 2), EPSILON);
        assertEquals(expectedY, pose.last().pose().get(3, 1), EPSILON);
    }
}
