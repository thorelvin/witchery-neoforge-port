package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

class MirrorFaceRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void rebasedModernMatrixEqualsSourcePostBaseScaleAndTranslation() {
        PoseStack modern = new PoseStack();
        modern.translate(0.0F, MirrorFaceRenderer.SOURCE_PRE_BASE_TRANSLATE_Y, 0.0F);
        modern.scale(MirrorFaceRenderer.SOURCE_MODEL_SCALE,
                MirrorFaceRenderer.SOURCE_MODEL_SCALE, MirrorFaceRenderer.SOURCE_MODEL_SCALE);
        modern.translate(0.0F, -1.501F, 0.0F);

        PoseStack source = new PoseStack();
        source.translate(0.0F, -1.5078125F, 0.0F);
        source.scale(0.75F, 0.75F, 0.75F);
        source.translate(0.0F, 0.4F, 0.0F);

        assertEquals(source.last().pose().get(3, 1), modern.last().pose().get(3, 1), EPSILON);
        assertEquals(-1.2078125F, modern.last().pose().get(3, 1), EPSILON);
        assertEquals(0.75F, modern.last().pose().get(0, 0), EPSILON);
        assertEquals(0.0F, MirrorFaceRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertEquals(0xCCFFFFFF, MirrorFaceRenderer.SOURCE_ALPHA_WHITE);
        assertEquals("witchery:textures/entity/mirrorface.png", MirrorFaceRenderer.TEXTURE.toString());
    }
}
