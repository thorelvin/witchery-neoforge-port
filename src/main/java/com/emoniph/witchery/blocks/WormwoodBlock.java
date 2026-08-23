package com.emoniph.witchery.blocks;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Wormwood — 5 stages (0-4). Key brewing ingredient; seed and product are the same item. */
public class WormwoodBlock extends WitchCropBlock {
    public static final IntegerProperty WORMWOOD_AGE = BlockStateProperties.AGE_4;

    public WormwoodBlock(BlockBehaviour.Properties props) { super(props); }

    @Override public IntegerProperty getAgeProperty() { return WORMWOOD_AGE; }
    @Override public int getMaxAge() { return 4; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WORMWOOD_AGE);
    }

    @Override
    protected ItemLike getBaseSeedId() { return WitcheryItems.WORMWOOD_SEEDS.get(); }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        if (!level.isAreaLoaded(pos, 1)) return;

        BlockState below = level.getBlockState(pos.below());
        if (sourceCanExtend(isMaxAge(state), below.getBlock() instanceof WormwoodBlock,
                level.isEmptyBlock(pos.above()), level.getRawBrightness(pos, 0))) {
            level.setBlock(pos.above(), defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    static boolean sourceCanExtend(boolean mature, boolean belowWormwood, boolean aboveAir, int brightness) {
        return mature && !belowWormwood && aboveAir && brightness >= 9;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return WitchCropSoilRules.supports(this, state);
    }
}
