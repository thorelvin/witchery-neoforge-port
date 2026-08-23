package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class LordOfTormentRendererTest {
    @Test
    void sourceOneSidedMotionGateAndTexturePathArePreserved() {
        assertTrue(LordOfTormentRenderer.sourceInMotion(new Vec3(0.1D, 0.0D, 0.0D)));
        assertTrue(LordOfTormentRenderer.sourceInMotion(new Vec3(0.0D, 0.0D, 0.1D)));
        assertFalse(LordOfTormentRenderer.sourceInMotion(new Vec3(-0.1D, 0.0D, -0.1D)));
        assertEquals("witchery:textures/entity/lordoftorment.png",
                LordOfTormentRenderer.TEXTURE.toString());
    }
}
