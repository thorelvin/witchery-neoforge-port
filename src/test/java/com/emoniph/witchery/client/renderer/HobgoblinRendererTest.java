package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.emoniph.witchery.client.model.HobgoblinModel;
import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

class HobgoblinRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void sourceProfessionTextureOrderShadowAndAnchorsArePreserved() {
        assertEquals("witchery:textures/entity/hobgoblin2.png", textureFor(0));
        assertEquals("witchery:textures/entity/hobgoblin.png", textureFor(1));
        assertEquals("witchery:textures/entity/hobgoblin4.png", textureFor(2));
        assertEquals("witchery:textures/entity/hobgoblin3.png", textureFor(3));
        assertEquals(0.5F, HobgoblinRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertAnchor(false, -1.5078125F);
        assertAnchor(true, -1.6328125F);
    }

    private static String textureFor(int profession) {
        return HobgoblinRenderer.TEXTURES_BY_PROFESSION[profession].toString();
    }

    private static void assertAnchor(boolean crouching, float expected) {
        HobgoblinModel.HobgoblinRenderState state = new HobgoblinModel.HobgoblinRenderState();
        state.sourceCrouching = crouching;
        PoseStack pose = new PoseStack();
        pose.translate(0, HobgoblinRenderer.LEGACY_BASE_DELTA_Y - (crouching ? 0.125F : 0), 0);
        pose.translate(0, -1.501F, 0);
        assertEquals(expected, pose.last().pose().get(3, 1), EPSILON);
    }
}
