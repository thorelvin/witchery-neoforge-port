package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

class GhostFamilyRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void spectreBansheeAndPoltergeistPreserveSourceAlphaShadowTextureAndAnchor() {
        assertEquals(0.0F, SpectreRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertEquals(0x99FFFFFF, SpectreRenderer.SOURCE_NORMAL_TINT);
        assertEquals(0x26FFFFFF, SpectreRenderer.SOURCE_OBSCURED_TINT);
        assertEquals("witchery:textures/entity/spectre.png", SpectreRenderer.TEXTURE.toString());
        assertLegacyAnchor(SpectreRenderer.LEGACY_BASE_DELTA_Y);

        assertEquals(0.0F, BansheeRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertEquals(0xB3FFFFFF, BansheeRenderer.SOURCE_TINT);
        assertEquals("witchery:textures/entity/banshee.png", BansheeRenderer.TEXTURE.toString());
        assertLegacyAnchor(BansheeRenderer.LEGACY_BASE_DELTA_Y);

        assertEquals(0.0F, PoltergeistRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertEquals(0x66FFFFFF, PoltergeistRenderer.SOURCE_TINT);
        assertEquals("witchery:textures/entity/poltergeist.png", PoltergeistRenderer.TEXTURE.toString());
        assertLegacyAnchor(PoltergeistRenderer.LEGACY_BASE_DELTA_Y);
    }

    private static void assertLegacyAnchor(float correction) {
        PoseStack pose = new PoseStack();
        pose.translate(0, correction, 0);
        pose.translate(0, -1.501F, 0);
        assertEquals(-1.5078125F, pose.last().pose().get(3, 1), EPSILON);
    }
}
