package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

class WolfmanRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void sourceTextureShadowAndStandingAndCrouchingAnchorsArePreserved() {
        assertEquals("witchery:textures/entity/wolfman_entity.png",
                WolfmanRenderer.TEXTURE.toString());
        assertEquals(0.5F, WolfmanRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertAnchor(false, -1.5078125F);
        assertAnchor(true, -1.6328125F);
    }

    @Test
    void playerRoutesUseExactSourceTexturesAndNoModernVanillaWolfDependency() {
        assertEquals("witchery:textures/entity/wolfman_entity.png",
                WerewolfLayer.WOLFMAN_TEXTURE.toString());
        assertEquals("witchery:textures/entity/werewolf_wolf.png",
                WerewolfLayer.WOLF_TEXTURE.toString());
    }

    private static void assertAnchor(boolean crouching, float expected) {
        PoseStack pose = new PoseStack();
        pose.translate(0.0F, WolfmanRenderer.LEGACY_BASE_DELTA_Y
                - (crouching ? 0.125F : 0.0F), 0.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        assertEquals(expected, pose.last().pose().get(3, 1), EPSILON);
    }
}
