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

public class BelladonnaBlock extends CropBlock {
    // Must NOT be named AGE — that would shadow CropBlock.AGE but createBlockStateDefinition
    // would still reference CropBlock.AGE (static fields are not polymorphic).
    // Override createBlockStateDefinition instead to register this property.
    public static final IntegerProperty BELLADONNA_AGE = BlockStateProperties.AGE_4;

    public BelladonnaBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public IntegerProperty getAgeProperty() { return BELLADONNA_AGE; }

    @Override
    public int getMaxAge() { return 4; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BELLADONNA_AGE);
    }

    @Override
    protected ItemLike getBaseSeedId() { return WitcheryItems.BELLADONNA.get(); }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.FARMLAND) || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT);
    }
}
