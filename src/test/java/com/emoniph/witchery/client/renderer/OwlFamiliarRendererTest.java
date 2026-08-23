package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class OwlFamiliarRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void sourceLandedGateRequiresExactRestAndLiquidOrSolidBelow() {
        assertTrue(OwlFamiliarRenderer.sourceLanded(Vec3.ZERO, true));
        assertFalse(OwlFamiliarRenderer.sourceLanded(Vec3.ZERO, false));
        assertFalse(OwlFamiliarRenderer.sourceLanded(new Vec3(0.000001D, 0.0D, 0.0D), true));
        assertFalse(OwlFamiliarRenderer.sourceLanded(new Vec3(0.0D, -0.01D, 0.0D), true));
    }

    @Test
    void sourceFeatherTintScaleShadowTextureAndAnchorArePreserved() {
        assertEquals(-1, OwlFamiliarRenderer.featherTint(0));
        assertEquals(0xFF825845, OwlFamiliarRenderer.featherTint(14));
        assertEquals(OwlFamiliarRenderer.featherTint(15), OwlFamiliarRenderer.featherTint(31));
        assertEquals(0.5F, OwlFamiliarRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertEquals("witchery:textures/entity/owl.png", OwlFamiliarRenderer.TEXTURE.toString());

        PoseStack pose = new PoseStack();
        pose.translate(0.0F, OwlFamiliarRenderer.LEGACY_BASE_DELTA_Y, 0.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        assertEquals(-1.5078125F, pose.last().pose().get(3, 1), EPSILON);
    }
}
