package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class FlyingMonkeyRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void sourceLandedGateRequiresExactRestAndLiquidOrSolidBelow() {
        assertTrue(FlyingMonkeyRenderer.sourceLanded(Vec3.ZERO, true));
        assertFalse(FlyingMonkeyRenderer.sourceLanded(Vec3.ZERO, false));
        assertFalse(FlyingMonkeyRenderer.sourceLanded(new Vec3(0.000001D, 0, 0), true));
        assertFalse(FlyingMonkeyRenderer.sourceLanded(new Vec3(0, -0.01D, 0), true));
    }

    @Test
    void sourceFeatherTintShadowTextureAndLegacyAnchorArePreserved() {
        assertEquals(-1, FlyingMonkeyRenderer.featherTint(DyeColor.WHITE));
        assertEquals(0xFF825845, FlyingMonkeyRenderer.featherTint(DyeColor.RED));
        assertEquals(0.5F, FlyingMonkeyRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertEquals("witchery:textures/entity/flying_monkey.png",
                FlyingMonkeyRenderer.TEXTURE.toString());

        PoseStack pose = new PoseStack();
        pose.translate(0.0F, FlyingMonkeyRenderer.LEGACY_BASE_DELTA_Y, 0.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        assertEquals(-1.5078125F, pose.last().pose().get(3, 1), EPSILON);
    }
}
