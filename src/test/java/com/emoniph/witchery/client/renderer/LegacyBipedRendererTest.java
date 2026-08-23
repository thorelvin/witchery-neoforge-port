package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

class LegacyBipedRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void elleUsesTheSourceFollowerTextureShadowAndLegacyAnchors() {
        assertEquals("witchery:textures/entity/elle.png", ElleRenderer.TEXTURE.toString());
        assertEquals(0.5F, ElleRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertAnchor(ElleRenderer.LEGACY_BASE_DELTA_Y, false, -1.5078125F);
        assertAnchor(ElleRenderer.LEGACY_BASE_DELTA_Y, true, -1.6328125F);
    }

    @Test
    void reflectionUsesSourceTexturesShadowAndLegacyAnchors() {
        assertEquals("witchery:textures/entity/reflection.png",
                ReflectionRenderer.SOURCE_FALLBACK.toString());
        assertEquals("witchery:textures/entity/wolfman_entity.png",
                ReflectionRenderer.WOLFMAN_TEXTURE.toString());
        assertEquals(0.5F, ReflectionRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertAnchor(ReflectionRenderer.LEGACY_BASE_DELTA_Y, false, -1.5078125F);
        assertAnchor(ReflectionRenderer.LEGACY_BASE_DELTA_Y, true, -1.6328125F);
    }

    private static void assertAnchor(float delta, boolean crouching, float expected) {
        PoseStack pose = new PoseStack();
        pose.translate(0.0F, delta - (crouching ? 0.125F : 0.0F), 0.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        assertEquals(expected, pose.last().pose().get(3, 1), EPSILON);
    }
}
