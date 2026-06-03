package com.emoniph.witchery.blocks;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Mindrake crop — grown from a Mindrake Bulb. Unlike the Mandrake crop, harvesting it does not
 * raise a creature; it simply yields bulbs (a bulb left lying on the ground hatches into a Mindrake
 * on its own — see {@code MindrakeEvents}). Grows more slowly than ordinary crops in the source.
 */
public class MindrakeBlock extends CropBlock {
    public static final IntegerProperty MINDRAKE_AGE = BlockStateProperties.AGE_4;

    public MindrakeBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public IntegerProperty getAgeProperty() { return MINDRAKE_AGE; }

    @Override
    public int getMaxAge() { return 4; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MINDRAKE_AGE);
    }

    @Override
    protected ItemLike getBaseSeedId() { return WitcheryItems.MINDRAKE_BULB.get(); }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.FARMLAND) || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT);
    }
}
