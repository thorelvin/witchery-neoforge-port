package com.emoniph.witchery.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class PerpetualIceBlock extends IceBlock {
    public static final MapCodec<PerpetualIceBlock> CODEC = simpleCodec(PerpetualIceBlock::new);

    public PerpetualIceBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends IceBlock> codec() {
        return CODEC;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Source BlockPerpetualIce intentionally never melts during random ticks.
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return false;
    }

    @Override
    protected int getLightBlock(BlockState state) {
        return 3;
    }
}
