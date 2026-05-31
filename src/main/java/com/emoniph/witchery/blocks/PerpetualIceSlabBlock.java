package com.emoniph.witchery.blocks;

import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class PerpetualIceSlabBlock extends SlabBlock {
    public PerpetualIceSlabBlock(BlockBehaviour.Properties properties) {
        super(properties);
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
