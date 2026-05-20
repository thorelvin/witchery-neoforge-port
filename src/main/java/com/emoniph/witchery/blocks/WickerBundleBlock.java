package com.emoniph.witchery.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Plain wicker bundles are the flammable rotated-pillar blocks used to build
 * Witchery's wickerman structure. The bloodied variant/summon ritual is tracked
 * separately because it needs its own item/block and Horned Huntsman entity.
 */
public class WickerBundleBlock extends RotatedPillarBlock {

    public WickerBundleBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 20;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 20;
    }
}
