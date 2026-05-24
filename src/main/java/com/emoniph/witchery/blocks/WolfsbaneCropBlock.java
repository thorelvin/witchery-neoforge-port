package com.emoniph.witchery.blocks;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class WolfsbaneCropBlock extends CropBlock {

    public static final IntegerProperty WOLFSBANE_AGE = BlockStateProperties.AGE_7;

    public WolfsbaneCropBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public IntegerProperty getAgeProperty() { return WOLFSBANE_AGE; }

    @Override
    public int getMaxAge() { return 7; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WOLFSBANE_AGE);
    }

    @Override
    protected ItemLike getBaseSeedId() { return WitcheryItems.WOLFSBANE_SEEDS.get(); }
}
