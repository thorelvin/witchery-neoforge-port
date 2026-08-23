package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

class FlameImpRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void poweredScaleAndSourceShadowArePreserved() {
        PoseStack powered = new PoseStack();
        FlameImpRenderer.applySourcePoweredScale(powered, true);
        assertEquals(1.5F, powered.last().pose().get(0, 0), EPSILON);
        assertEquals(1.0F, powered.last().pose().get(1, 1), EPSILON);
        assertEquals(1.5F, powered.last().pose().get(2, 2), EPSILON);

        PoseStack normal = new PoseStack();
        FlameImpRenderer.applySourcePoweredScale(normal, false);
        assertEquals(1.0F, normal.last().pose().get(0, 0), EPSILON);
        assertEquals(0.5F, FlameImpRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
    }
}
