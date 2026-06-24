package com.emoniph.witchery.blocks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritLiquidBlockTest {

    @Test
    void spiritLiquidsNeverDisplaceRealFluids() {
        assertFalse(SpiritLiquidBlock.canDisplaceForSpirit(false, true, false, false, true));
    }

    @Test
    void spiritLiquidsDisplaceAirAndReplaceableNonFluids() {
        assertTrue(SpiritLiquidBlock.canDisplaceForSpirit(true, false, false, false, false));
        assertTrue(SpiritLiquidBlock.canDisplaceForSpirit(false, true, false, false, false));
    }

    @Test
    void spiritLiquidsDoNotDisplaceSolidsOrBlockEntities() {
        assertFalse(SpiritLiquidBlock.canDisplaceForSpirit(false, false, true, false, false));
        assertFalse(SpiritLiquidBlock.canDisplaceForSpirit(false, false, false, true, false));
    }
}
