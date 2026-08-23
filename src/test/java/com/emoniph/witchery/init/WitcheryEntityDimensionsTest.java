package com.emoniph.witchery.init;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WitcheryEntityDimensionsTest {
    @Test
    void preservesSourceEntityDimensionFamilies() {
        assertDimensions(0.6F, 1.8F,
                WitcheryEntities.SOURCE_DEFAULT_WIDTH,
                WitcheryEntities.SOURCE_DEFAULT_HEIGHT);
        assertDimensions(0.6F, 0.8F,
                WitcheryEntities.SOURCE_OWL_WIDTH,
                WitcheryEntities.SOURCE_OWL_HEIGHT);
        assertDimensions(0.6F, 0.8F,
                WitcheryEntities.SOURCE_OCELOT_WIDTH,
                WitcheryEntities.SOURCE_OCELOT_HEIGHT);
        assertDimensions(0.8F, 0.8F,
                WitcheryEntities.SOURCE_TOAD_WIDTH,
                WitcheryEntities.SOURCE_TOAD_HEIGHT);
        assertDimensions(0.6F, 0.95F,
                WitcheryEntities.SOURCE_HOBGOBLIN_WIDTH,
                WitcheryEntities.SOURCE_HOBGOBLIN_HEIGHT);
        assertDimensions(0.25F, 0.25F,
                WitcheryEntities.SOURCE_SPIRIT_WIDTH,
                WitcheryEntities.SOURCE_SPIRIT_HEIGHT);
        assertDimensions(1.0F, 1.0F,
                WitcheryEntities.SOURCE_ACTIVE_SPELL_EFFECT_WIDTH,
                WitcheryEntities.SOURCE_ACTIVE_SPELL_EFFECT_HEIGHT);
    }

    private static void assertDimensions(float expectedWidth, float expectedHeight,
                                         float actualWidth, float actualHeight) {
        assertEquals(expectedWidth, actualWidth);
        assertEquals(expectedHeight, actualHeight);
    }
}
