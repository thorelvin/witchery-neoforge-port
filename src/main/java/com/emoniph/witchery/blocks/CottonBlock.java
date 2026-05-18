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

/** Somnian Cotton — 4 stages (0-3). Matures to disturbed cotton. */
public class CottonBlock extends CropBlock {
    public static final IntegerProperty COTTON_AGE = BlockStateProperties.AGE_3;

    public CottonBlock(BlockBehaviour.Properties props) { super(props); }

    @Override public IntegerProperty getAgeProperty() { return COTTON_AGE; }
    @Override public int getMaxAge() { return 3; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COTTON_AGE);
    }

    @Override
    protected ItemLike getBaseSeedId() { return WitcheryItems.COTTON.get(); }
}
