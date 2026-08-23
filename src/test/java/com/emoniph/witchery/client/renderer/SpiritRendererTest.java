package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

class SpiritRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void sourceModelLocalTranslationAndHalfScaleAreRebasedBeforeModernBaseTranslation() {
        assertSourceMatrix(SpiritRenderer.SOURCE_PRE_BASE_TRANSLATE_Y);
        assertSourceMatrix(LostSoulRenderer.SOURCE_PRE_BASE_TRANSLATE_Y);
        assertEquals(0.0F, SpiritRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertEquals(0.0F, LostSoulRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertEquals("witchery:textures/entity/spirit.png", SpiritRenderer.TEXTURE.toString());
        assertEquals(SpiritRenderer.TEXTURE, LostSoulRenderer.TEXTURE);
    }

    @Test
    void lostSoulUsesSourceRedGreenBluePackedTintsAtSixtyPercentAlpha() {
        assertEquals(0x99FF0000, LostSoulRenderer.sourceTint(0));
        assertEquals(0x9900FF00, LostSoulRenderer.sourceTint(1));
        assertEquals(0x990000FF, LostSoulRenderer.sourceTint(2));
        assertEquals(0x99FF0000, LostSoulRenderer.sourceTint(99));
        assertEquals(0x99FFFFFF, SpiritRenderer.SOURCE_ALPHA_WHITE);
    }

    private static void assertSourceMatrix(float preBaseY) {
        PoseStack pose = new PoseStack();
        pose.translate(0, preBaseY, 0);
        pose.scale(0.5F, 0.5F, 0.5F);
        pose.translate(0, -1.501F, 0);
        assertEquals(-1.5078125F + 0.65F, pose.last().pose().get(3, 1), EPSILON);
        assertEquals(0.5F, pose.last().pose().get(0, 0), EPSILON);
    }
}
