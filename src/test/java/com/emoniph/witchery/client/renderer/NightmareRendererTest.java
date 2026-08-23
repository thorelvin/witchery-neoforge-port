package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.emoniph.witchery.entity.NightmareEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

class NightmareRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void sourceTextureAlphaShadowAndLegacyAnchorArePreserved() {
        assertEquals("witchery:textures/entity/nightmare.png", NightmareRenderer.TEXTURE.toString());
        assertEquals(0.5F, NightmareRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertEquals(0xE6FFFFFF, NightmareEntity.SOURCE_NORMAL_ALPHA);
        assertEquals(0x99FFFFFF, NightmareEntity.SOURCE_DEFENDED_ALPHA);
        PoseStack pose = new PoseStack();
        pose.translate(0.0F, NightmareRenderer.LEGACY_BASE_DELTA_Y, 0.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        assertEquals(-1.5078125F, pose.last().pose().get(3, 1), EPSILON);
    }
}
