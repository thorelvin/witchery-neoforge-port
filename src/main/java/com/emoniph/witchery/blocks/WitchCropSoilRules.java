package com.emoniph.witchery.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Shared 1.7.10 survival contract for non-water Witchery crops. */
final class WitchCropSoilRules {
    private WitchCropSoilRules() {
    }

    static boolean supports(Block crop, BlockState soil) {
        if (soil.is(Blocks.FARMLAND) || soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.DIRT)) {
            return true;
        }
        if (crop instanceof SnowbellBlock && (soil.is(Blocks.PODZOL) || soil.is(Blocks.SNOW_BLOCK))) {
            return true;
        }
        return soil.getBlock() == crop || soil.getBlock() instanceof WormwoodBlock;
    }
}
