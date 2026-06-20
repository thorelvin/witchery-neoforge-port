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

/** Snowbell — 5 stages (0-4). Grows in cold biomes; drops snowbell bulbs. */
public class SnowbellBlock extends CropBlock {
    public static final IntegerProperty SNOWBELL_AGE = BlockStateProperties.AGE_4;

    public SnowbellBlock(BlockBehaviour.Properties props) { super(props); }

    @Override public IntegerProperty getAgeProperty() { return SNOWBELL_AGE; }
    @Override public int getMaxAge() { return 4; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SNOWBELL_AGE);
    }

    @Override
    protected ItemLike getBaseSeedId() { return WitcheryItems.SNOWBELL_SEEDS.get(); }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        // Source BlockWitchCrop (waterPlant=false): grass, dirt, or farmland.
        return state.is(Blocks.FARMLAND) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT);
    }
}
