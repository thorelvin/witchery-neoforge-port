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

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        // Herbalism Primer: tilled farmland only. (1.7.10 BlockWitchCrop also accepted grass/dirt,
        // but the in-game primer — the canonical herbalism guide — calls for tilled farmland.)
        return state.is(Blocks.FARMLAND);
    }
}
