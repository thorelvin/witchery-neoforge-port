package com.emoniph.witchery.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlightedLogBlock extends RotatedPillarBlock {
    public BlightedLogBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlightHelper.decayIfOrphaned(level, pos, state, random);
        if (level.getBlockState(pos).is(this)) {
            BlightHelper.trySpreadNear(level, pos, random, false);
        }
    }
}
