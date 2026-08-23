package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import org.junit.jupiter.api.Test;

class LilithRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void sourceTextureShadowFullBrightnessAndLegacyAnchorArePreserved() {
        assertEquals("witchery:textures/entity/lilith.png", LilithRenderer.TEXTURE.toString());
        assertEquals(0.5F, LilithRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertEquals(LightTexture.FULL_BRIGHT, LilithRenderer.SOURCE_LIGHT);
        PoseStack pose = new PoseStack();
        pose.translate(0.0F, LilithRenderer.LEGACY_BASE_DELTA_Y, 0.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        assertEquals(-1.5078125F, pose.last().pose().get(3, 1), EPSILON);
    }
}
