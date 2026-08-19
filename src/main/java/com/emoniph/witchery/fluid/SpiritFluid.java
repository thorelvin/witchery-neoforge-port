package com.emoniph.witchery.fluid;

import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryFluids;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public abstract class SpiritFluid extends BaseFlowingFluid {
    public static final int SOURCE_DENSITY = 500;
    public static final int SOURCE_VISCOSITY = 2000;
    static final int SOURCE_SLOPE_DISTANCE = 4;
    static final int SOURCE_LEVEL_DECREASE = 2;
    static final int SOURCE_TICK_RATE = 10;

    protected SpiritFluid() {
        super(new BaseFlowingFluid.Properties(
                WitcheryFluids.FLOWING_SPIRIT_TYPE,
                WitcheryFluids.FLOWING_SPIRIT_SOURCE,
                WitcheryFluids.FLOWING_SPIRIT_FLOWING)
                .bucket(WitcheryItems.BUCKET_SPIRIT)
                .block(WitcheryBlocks.FLOWING_SPIRIT)
                .slopeFindDistance(SOURCE_SLOPE_DISTANCE)
                .levelDecreasePerBlock(SOURCE_LEVEL_DECREASE)
                .tickRate(SOURCE_TICK_RATE)
                .explosionResistance(100.0F));
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        Block.dropResources(state, level, pos, state.hasBlockEntity() ? level.getBlockEntity(pos) : null);
    }

    @Override
    protected boolean canConvertToSource(ServerLevel level) {
        return false;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid,
                                        Direction direction) {
        return false;
    }

    public static final class Source extends SpiritFluid {
        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static final class Flowing extends SpiritFluid {
        public Flowing() {
            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
