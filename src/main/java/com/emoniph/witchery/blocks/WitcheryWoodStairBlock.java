package com.emoniph.witchery.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WitcheryWoodStairBlock extends StairBlock {
    private final int fireSpreadSpeed;
    private final int flammability;

    public WitcheryWoodStairBlock(BlockState baseState, BlockBehaviour.Properties properties) {
        this(baseState, properties, 5, 20);
    }

    public WitcheryWoodStairBlock(BlockState baseState, BlockBehaviour.Properties properties,
                                  int fireSpreadSpeed, int flammability) {
        super(baseState, properties);
        this.fireSpreadSpeed = fireSpreadSpeed;
        this.flammability = flammability;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return fireSpreadSpeed;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return flammability;
    }
}
