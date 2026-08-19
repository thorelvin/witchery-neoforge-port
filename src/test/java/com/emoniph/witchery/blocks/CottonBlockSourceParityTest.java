package com.emoniph.witchery.blocks;

import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CottonBlockSourceParityTest {
    @Test
    void sourcePlacementSupportsBushSoilsButNeverStone() {
        assertTrue(CottonBlock.isSourcePlacementSupport(Blocks.GRASS_BLOCK.defaultBlockState()));
        assertTrue(CottonBlock.isSourcePlacementSupport(Blocks.DIRT.defaultBlockState()));
        assertTrue(CottonBlock.isSourcePlacementSupport(Blocks.COARSE_DIRT.defaultBlockState()));
        assertTrue(CottonBlock.isSourcePlacementSupport(Blocks.PODZOL.defaultBlockState()));
        assertTrue(CottonBlock.isSourcePlacementSupport(Blocks.FARMLAND.defaultBlockState()));
        assertFalse(CottonBlock.isSourcePlacementSupport(Blocks.STONE.defaultBlockState()));
    }

    @Test
    void sourceSpreadDoesNotUseFarmlandOrStone() {
        assertTrue(CottonBlock.isSourceSpreadSupport(Blocks.GRASS_BLOCK.defaultBlockState()));
        assertTrue(CottonBlock.isSourceSpreadSupport(Blocks.DIRT.defaultBlockState()));
        assertTrue(CottonBlock.isSourceSpreadSupport(Blocks.COARSE_DIRT.defaultBlockState()));
        assertTrue(CottonBlock.isSourceSpreadSupport(Blocks.PODZOL.defaultBlockState()));
        assertFalse(CottonBlock.isSourceSpreadSupport(Blocks.FARMLAND.defaultBlockState()));
        assertFalse(CottonBlock.isSourceSpreadSupport(Blocks.STONE.defaultBlockState()));
    }

    @Test
    void sourceBushRequiresLightOrSkyAccess() {
        assertFalse(CottonBlock.hasSourceBushLight(7, false));
        assertTrue(CottonBlock.hasSourceBushLight(8, false));
        assertTrue(CottonBlock.hasSourceBushLight(0, true));
    }
}
