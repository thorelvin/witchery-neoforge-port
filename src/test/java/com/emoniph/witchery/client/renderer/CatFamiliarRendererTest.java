package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

class CatFamiliarRendererTest {
    private static final float EPSILON = 0.00001F;
    private static final float MODERN_BASE_Y = -1.501F;
    private static final float LEGACY_BASE_Y = -1.5078125F;

    @Test
    void sourceShadowTextureTamedScaleAndLegacyAnchorArePreserved() {
        assertEquals(0.5F, CatFamiliarRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertEquals(0.8F, CatFamiliarRenderer.SOURCE_TAMED_SCALE, EPSILON);
        assertEquals("witchery:textures/entity/blackcat.png", CatFamiliarRenderer.TEXTURE.toString());

        PoseStack tamed = new PoseStack();
        CatFamiliarRenderer.applySourceScaleAndAnchor(tamed, true);
        tamed.translate(0.0F, MODERN_BASE_Y, 0.0F);
        assertEquals(0.8F, tamed.last().pose().get(0, 0), EPSILON);
        assertEquals(0.8F * LEGACY_BASE_Y, tamed.last().pose().get(3, 1), EPSILON);

        PoseStack untamed = new PoseStack();
        CatFamiliarRenderer.applySourceScaleAndAnchor(untamed, false);
        untamed.translate(0.0F, MODERN_BASE_Y, 0.0F);
        assertEquals(1.0F, untamed.last().pose().get(0, 0), EPSILON);
        assertEquals(LEGACY_BASE_Y, untamed.last().pose().get(3, 1), EPSILON);
    }
}
