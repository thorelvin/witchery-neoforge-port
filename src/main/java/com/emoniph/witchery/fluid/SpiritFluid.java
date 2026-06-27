package com.emoniph.witchery.fluid;

import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryFluids;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public abstract class SpiritFluid extends BaseFlowingFluid {
    protected SpiritFluid() {
        super(new BaseFlowingFluid.Properties(
                WitcheryFluids.FLOWING_SPIRIT_TYPE,
                WitcheryFluids.FLOWING_SPIRIT_SOURCE,
                WitcheryFluids.FLOWING_SPIRIT_FLOWING)
                .bucket(WitcheryItems.BUCKET_SPIRIT)
                .block(WitcheryBlocks.FLOWING_SPIRIT)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(5)
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

    @Override
    public void animateTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
        if (random.nextInt(4) != 0) {
            return;
        }
        double y = pos.getY() + Math.max(0.05D, state.getOwnHeight()) + 0.02D;
        level.addParticle(new DustParticleOptions(0xB7F4D2, 0.65F),
                pos.getX() + random.nextDouble(), y, pos.getZ() + random.nextDouble(),
                0.0D, 0.015D, 0.0D);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.WITCH,
                    pos.getX() + random.nextDouble(), y, pos.getZ() + random.nextDouble(),
                    0.0D, 0.0D, 0.0D);
        }
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
