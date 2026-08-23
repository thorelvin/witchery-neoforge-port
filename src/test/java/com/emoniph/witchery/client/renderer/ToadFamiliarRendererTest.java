package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

class ToadFamiliarRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void sourceTintShadowTextureAndLegacyAnchorArePreserved() {
        assertEquals(-1, ToadFamiliarRenderer.skinTint(0));
        assertEquals(0xFFA93F3C, ToadFamiliarRenderer.skinTint(14));
        assertEquals(ToadFamiliarRenderer.skinTint(15), ToadFamiliarRenderer.skinTint(31));
        assertEquals(0.3F, ToadFamiliarRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertEquals("witchery:textures/entity/toad.png", ToadFamiliarRenderer.TEXTURE.toString());

        PoseStack pose = new PoseStack();
        pose.translate(0.0F, ToadFamiliarRenderer.LEGACY_BASE_DELTA_Y, 0.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        assertEquals(-1.5078125F, pose.last().pose().get(3, 1), EPSILON);
    }
}
