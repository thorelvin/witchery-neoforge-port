package com.emoniph.witchery.blocks;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.common.CommonHooks;

/**
 * Mindrake crop — grown from a Mindrake Bulb. Unlike the Mandrake crop, harvesting it does not
 * raise a creature; it simply yields bulbs (a bulb left lying on the ground hatches into a Mindrake
 * on its own — see {@code MindrakeEvents}). Grows more slowly than ordinary crops in the source.
 */
public class MindrakeBlock extends WitchCropBlock {
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
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) return;
        if (level.getRawBrightness(pos, 0) < 9) return;

        int age = getAge(state);
        if (age >= getMaxAge()) return;
        float growthSpeed = sourceGrowthSpeed(getGrowthSpeed(state, level, pos));
        if (CommonHooks.canCropGrow(level, pos, state,
                random.nextInt((int) (25.0F / growthSpeed) + 1) == 0)) {
            level.setBlock(pos, getStateForAge(age + 1), Block.UPDATE_CLIENTS);
            CommonHooks.fireCropGrowPost(level, pos, state);
        }
    }

    @Override
    protected int getBonemealAgeIncrease(Level level) {
        return 1;
    }

    static float sourceGrowthSpeed(float ordinaryGrowthSpeed) {
        return ordinaryGrowthSpeed / 1.5F;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return WitchCropSoilRules.supports(this, state);
    }
}
