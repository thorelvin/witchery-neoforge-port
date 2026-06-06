package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.BrewFluidBlockEntity;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public abstract class BrewFluidBlock extends BaseEntityBlock {
    public static final IntegerProperty DISTANCE = IntegerProperty.create("distance", 0, 10);
    public static final int SOURCE_GAS_TICK_RATE = 5;
    public static final int SOURCE_LIQUID_TICK_RATE = 5;
    public static final int SOURCE_GAS_MAX_RUN_TICKS = 120;
    public static final int SOURCE_LIQUID_QUANTA_PER_BLOCK = 6;
    private static final double[] SOURCE_GAS_EXPANSION_CHANCES = {0.2D, 0.4D, 0.8D, 0.8D, 0.8D, 0.8D};
    private static final Direction[] SOURCE_GAS_DIRECTIONS = {
            Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH
    };

    private final boolean gas;

    protected BrewFluidBlock(BlockBehaviour.Properties properties, boolean gas) {
        super(properties);
        this.gas = gas;
        registerDefaultState(stateDefinition.any().setValue(DISTANCE, 0));
    }

    public boolean isGas() {
        return gas;
    }

    public int tickRate() {
        return gas ? SOURCE_GAS_TICK_RATE : SOURCE_LIQUID_TICK_RATE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DISTANCE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BrewFluidBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            level.scheduleTick(pos, this, tickRate());
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess,
                                     BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState,
                                     RandomSource random) {
        if (level instanceof LevelAccessor accessor) {
            scheduledTickAccess.scheduleTick(pos, this, tickRate());
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, tickRate());
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof BrewFluidBlockEntity brew)) {
            level.removeBlock(pos, false);
            return;
        }
        if (brew.isEmpty()) {
            level.removeBlock(pos, false);
            return;
        }
        if (gas) {
            tickGas(state, level, pos, random, brew);
        } else {
            tickLiquid(state, level, pos, random, brew);
        }
    }

    private void tickGas(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, BrewFluidBlockEntity brew) {
        int distance = state.getValue(DISTANCE);
        int expansion = brew.getExpansion();
        if (brew.incRunTicks() > SOURCE_GAS_MAX_RUN_TICKS) {
            level.removeBlock(pos, false);
            return;
        }

        if (distance >= expansion) {
            int duration = brew.getDuration();
            if (duration == 0 || duration > 0 && random.nextInt(duration) == 0) {
                level.removeBlock(pos, false);
                return;
            }
        } else {
            boolean expanded = false;
            for (int i = 0; i < SOURCE_GAS_DIRECTIONS.length && distance < expansion; i++) {
                if (random.nextDouble() >= SOURCE_GAS_EXPANSION_CHANCES[i]) {
                    continue;
                }
                BlockPos target = pos.relative(SOURCE_GAS_DIRECTIONS[i]);
                if (trySpread(level, target, Math.min(distance + 1, expansion), brew, true)) {
                    expanded = true;
                }
            }
            if (expanded) {
                level.setBlock(pos, state.setValue(DISTANCE, Math.min(distance + 1, expansion)), Block.UPDATE_CLIENTS);
            }
        }
        level.scheduleTick(pos, this, tickRate());
    }

    private void tickLiquid(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, BrewFluidBlockEntity brew) {
        int distance = state.getValue(DISTANCE);
        if (distance == 0) {
            brew.incUpdateCount();
            if (brew.getUpdateCount() > 3 && brew.getDuration() != 0 && random.nextInt(brew.getDuration()) == 0) {
                level.removeBlock(pos, false);
                return;
            }
        }

        int nextDistance = Math.min(distance + 1, SOURCE_LIQUID_QUANTA_PER_BLOCK - 1);
        BlockPos below = pos.below();
        if (trySpread(level, below, 1, brew, false)) {
            level.scheduleTick(pos, this, tickRate());
            return;
        }
        if (distance < SOURCE_LIQUID_QUANTA_PER_BLOCK - 1) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                trySpread(level, pos.relative(direction), nextDistance, brew, false);
            }
        }
        level.scheduleTick(pos, this, tickRate());
    }

    private boolean trySpread(ServerLevel level, BlockPos target, int distance, BrewFluidBlockEntity source, boolean gas) {
        BlockState targetState = level.getBlockState(target);
        if (targetState.getBlock() instanceof BrewFluidBlock fluid && fluid.isGas() == gas) {
            if (targetState.getValue(DISTANCE) <= distance) {
                return false;
            }
            level.setBlock(target, targetState.setValue(DISTANCE, distance), Block.UPDATE_ALL);
        } else {
            if (!canDisplace(targetState)) {
                return false;
            }
            level.setBlock(target, defaultBlockState().setValue(DISTANCE, distance), Block.UPDATE_ALL);
        }
        if (level.getBlockEntity(target) instanceof BrewFluidBlockEntity targetFluid) {
            targetFluid.copyFrom(source);
        }
        level.scheduleTick(target, this, tickRate());
        return true;
    }

    public static boolean canDisplace(BlockState state) {
        return state.isAir()
                || state.canBeReplaced()
                || (!state.isSolid() && !state.hasBlockEntity());
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier effectApplier, boolean isMoved) {
        if (level.isClientSide()
                || !(entity instanceof LivingEntity living)
                || !(level.getBlockEntity(pos) instanceof BrewFluidBlockEntity brew)
                || level.random.nextInt(10) != 4) {
            return;
        }
        brew.applyContactEffect(living);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return gas ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        return adjacentState.is(this) || super.skipRendering(state, adjacentState, direction);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        return true;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (gas && random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.WITCH,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(),
                    0.0D, 0.01D, 0.0D);
        } else if (!gas && random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SPLASH,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + 0.05D,
                    pos.getZ() + random.nextDouble(),
                    0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return gas ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    public static final class Gas extends BrewFluidBlock {
        public static final MapCodec<Gas> CODEC = simpleCodec(Gas::new);

        public Gas(BlockBehaviour.Properties properties) {
            super(properties, true);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }
    }

    public static final class Liquid extends BrewFluidBlock {
        public static final MapCodec<Liquid> CODEC = simpleCodec(Liquid::new);

        public Liquid(BlockBehaviour.Properties properties) {
            super(properties, false);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }
    }
}
