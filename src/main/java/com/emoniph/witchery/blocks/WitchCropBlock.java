package com.emoniph.witchery.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Common survival bridge for source soils and NeoForge-provided modded soils. */
abstract class WitchCropBlock extends CropBlock {
    protected WitchCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (WitchCropSoilRules.supports(this, level.getBlockState(pos.below()))) {
            return true;
        }
        return super.canSurvive(state, level, pos);
    }
}
