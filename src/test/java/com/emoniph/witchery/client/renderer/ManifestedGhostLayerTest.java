package com.emoniph.witchery.client.renderer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManifestedGhostLayerTest {
    @Test
    void sourcePlayerSkinLayerUsesTheOriginalManifestationOpacity() {
        assertEquals(0.51F, ManifestedGhostLayer.SOURCE_ALPHA);
    }
}
