package com.emoniph.witchery.fluid;

import com.emoniph.witchery.blocks.entity.BrewFluidBlockEntity;
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

public abstract class BrewLiquidFluid extends BaseFlowingFluid {
    private static final ThreadLocal<BrewFluidBlockEntity> SPREAD_SOURCE = new ThreadLocal<>();

    protected BrewLiquidFluid() {
        super(new BaseFlowingFluid.Properties(
                WitcheryFluids.BREW_LIQUID_TYPE,
                WitcheryFluids.BREW_LIQUID_SOURCE,
                WitcheryFluids.BREW_LIQUID_FLOWING)
                .bucket(WitcheryItems.BREW_BUCKET)
                .block(WitcheryBlocks.BREW_LIQUID)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(5)
                .explosionResistance(100.0F));
    }

    @Override
    public void tick(ServerLevel level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (!(level.getBlockEntity(pos) instanceof BrewFluidBlockEntity brew) || brew.isEmpty()) {
            level.removeBlock(pos, false);
            return;
        }

        brew.applyAdjacentBlockPulse(level);
        if (fluidState.isSource() && !brew.isPersistentSource()) {
            brew.incUpdateCount();
            if (brew.getUpdateCount() > 3 && brew.getDuration() != 0
                    && level.random.nextInt(brew.getDuration()) == 0) {
                level.removeBlock(pos, false);
                return;
            }
        }

        BrewFluidBlockEntity previous = SPREAD_SOURCE.get();
        SPREAD_SOURCE.set(brew);
        try {
            super.tick(level, pos, state, fluidState);
            if (level.getBlockState(pos).is(WitcheryBlocks.BREW_LIQUID.get())
                    && level.getBlockEntity(pos) instanceof BrewFluidBlockEntity current
                    && current.isEmpty()) {
                current.copyFrom(brew);
            }
        } finally {
            if (previous == null) {
                SPREAD_SOURCE.remove();
            } else {
                SPREAD_SOURCE.set(previous);
            }
        }
    }

    @Override
    protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState state, Direction direction,
                            FluidState fluidState) {
        FluidState existingFluid = state.getFluidState();
        if (!existingFluid.isEmpty() && !isSame(existingFluid.getType())) {
            return;
        }

        BrewFluidBlockEntity source = SPREAD_SOURCE.get();
        super.spreadTo(level, pos, state, direction, fluidState);
        if (source != null && level.getBlockEntity(pos) instanceof BrewFluidBlockEntity target) {
            target.copyFrom(source);
        }
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
        level.addParticle(new DustParticleOptions(brewColor(level, pos), 0.75F),
                pos.getX() + random.nextDouble(), y, pos.getZ() + random.nextDouble(),
                0.0D, 0.015D, 0.0D);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SPLASH,
                    pos.getX() + random.nextDouble(), y, pos.getZ() + random.nextDouble(),
                    0.0D, 0.0D, 0.0D);
        }
    }

    public static int brewColor(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof BrewFluidBlockEntity brew && brew.getColor() != 0) {
            return brew.getColor() & 0xFFFFFF;
        }
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof BrewFluidBlockEntity brew
                    && brew.getColor() != 0) {
                return brew.getColor() & 0xFFFFFF;
            }
        }
        return 0x8A4FCF;
    }

    public static final class Source extends BrewLiquidFluid {
        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static final class Flowing extends BrewLiquidFluid {
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
