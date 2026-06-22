package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.BrewFluidBlockEntity;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public abstract class BrewFluidBlock extends BaseEntityBlock {
    public static final IntegerProperty DISTANCE = IntegerProperty.create("distance", 0, 10);
    public static final EnumProperty<Direction> FLOW = BlockStateProperties.HORIZONTAL_FACING;
    public static final int SOURCE_GAS_TICK_RATE = 5;
    public static final int SOURCE_LIQUID_TICK_RATE = 5;
    public static final int SOURCE_GAS_BASE_MAX_RUN_TICKS = 120;
    public static final int SOURCE_LIQUID_QUANTA_PER_BLOCK = 6;
    public static final int SOURCE_FLOW_COST_MAX = 1000;
    public static final int SOURCE_FLOW_RECURSE_LIMIT = 4;
    public static final int DEFAULT_BREW_COLOR = 0xFFFFFF;
    private static final double[] SOURCE_GAS_EXPANSION_CHANCES = {0.2D, 0.4D, 0.8D, 0.8D, 0.8D, 0.8D};
    private static final Direction[] SOURCE_GAS_DIRECTIONS = {
            Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH
    };
    private static final int[] SOURCE_LIQUID_HEIGHT_PIXELS = {16, 14, 12, 10, 8, 6, 4, 3, 2, 1, 1};
    private static final VoxelShape[] SOURCE_LIQUID_SHAPES = createLiquidShapes();

    private final boolean gas;

    protected BrewFluidBlock(BlockBehaviour.Properties properties, boolean gas) {
        super(properties);
        this.gas = gas;
        registerDefaultState(stateDefinition.any()
                .setValue(DISTANCE, 0)
                .setValue(FLOW, Direction.NORTH));
    }

    public boolean isGas() {
        return gas;
    }

    public int tickRate() {
        return gas ? SOURCE_GAS_TICK_RATE : SOURCE_LIQUID_TICK_RATE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DISTANCE, FLOW);
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
        int runTicks = brew.incRunTicks();
        if (runTicks > brew.getMaxRunTicks()) {
            level.removeBlock(pos, false);
            return;
        }
        applyGasContactPulse(level, pos, brew, runTicks);

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

    private void applyGasContactPulse(ServerLevel level, BlockPos pos, BrewFluidBlockEntity brew, int runTicks) {
        if (!shouldApplyGasContactOnRunTick(runTicks)) {
            return;
        }
        AABB contactBox = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, contactBox, LivingEntity::isAlive)) {
            brew.applyContactEffect(living);
        }
    }

    static boolean shouldApplyGasContactOnRunTick(int runTicks) {
        return runTicks > 0 && (runTicks & 1) == 1;
    }

    private void tickLiquid(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, BrewFluidBlockEntity brew) {
        int distance = state.getValue(DISTANCE);
        if (distance > 0) {
            int supportedDistance = supportedLiquidDistance(level, pos);
            if (supportedDistance < 0) {
                level.removeBlock(pos, false);
                return;
            }
            if (supportedDistance != distance) {
                distance = supportedDistance;
                state = state.setValue(DISTANCE, distance);
                level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            }
        }
        int runTicks = brew.incRunTicks();
        applyLiquidContactPulse(level, pos, state, brew, runTicks);

        // Source BlockBrewLiquidEffect: each tick the pool may apply its block actions to neighbours.
        brew.applyAdjacentBlockPulse(level);
        if (distance == 0 && !brew.isPersistentSource()) {
            brew.incUpdateCount();
            if (brew.getUpdateCount() > 3 && brew.getDuration() != 0 && random.nextInt(brew.getDuration()) == 0) {
                level.removeBlock(pos, false);
                return;
            }
        }

        int nextDistance = Math.min(distance + 1, SOURCE_LIQUID_QUANTA_PER_BLOCK - 1);
        BlockPos below = pos.below();
        if (trySpread(level, below, 1, brew, false, state.getValue(FLOW))) {
            level.scheduleTick(pos, this, tickRate());
            return;
        }
        if (distance < SOURCE_LIQUID_QUANTA_PER_BLOCK - 1) {
            // Source optimal flow: only spread toward the nearest reachable drop (vanilla-water style).
            // With no drop in range every direction ties, so it falls back to an even spread.
            for (Direction direction : optimalFlowDirections(pos,
                    target -> canFlowInto(level, target), target -> isSourceBlock(level, target))) {
                trySpread(level, pos.relative(direction), nextDistance, brew, false, direction);
            }
        }
        level.scheduleTick(pos, this, tickRate());
    }

    private void applyLiquidContactPulse(ServerLevel level, BlockPos pos, BlockState state,
                                         BrewFluidBlockEntity brew, int runTicks) {
        if (!shouldApplyLiquidContactOnRunTick(runTicks)) {
            return;
        }
        double height = liquidHeightPixels(state.getValue(DISTANCE)) / 16.0D + 0.05D;
        AABB contactBox = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0D, pos.getY() + Math.min(1.0D, height), pos.getZ() + 1.0D);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, contactBox, LivingEntity::isAlive)) {
            brew.applyContactEffect(living);
        }
    }

    static boolean shouldApplyLiquidContactOnRunTick(int runTicks) {
        return shouldApplyGasContactOnRunTick(runTicks);
    }

    /** Whether brew liquid can flow into a position (air / replaceable / same-fluid). */
    private boolean canFlowInto(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BrewFluidBlock fluid && !fluid.isGas()) {
            return true;
        }
        return canDisplace(state);
    }

    /** A brew-liquid source block (distance 0) — never flowed back into. */
    private boolean isSourceBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof BrewFluidBlock fluid && !fluid.isGas()
                && state.getValue(DISTANCE) == 0;
    }

    private int supportedLiquidDistance(ServerLevel level, BlockPos pos) {
        if (isBrewLiquid(level.getBlockState(pos.above()))) {
            return 1;
        }

        int bestDistance = Integer.MAX_VALUE;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (isBrewLiquid(neighbor)) {
                bestDistance = Math.min(bestDistance, neighbor.getValue(DISTANCE) + 1);
            }
        }
        return bestDistance < SOURCE_LIQUID_QUANTA_PER_BLOCK ? Math.max(1, bestDistance) : -1;
    }

    private static boolean isBrewLiquid(BlockState state) {
        return state.getBlock() instanceof BrewFluidBlock fluid && !fluid.isGas();
    }

    /**
     * Source {@code getOptimalFlowDirections}: returns the horizontal directions whose flow cost (the
     * shortest horizontal distance, up to {@link #SOURCE_FLOW_RECURSE_LIMIT}, to a block the liquid can
     * fall through) is minimal. Pure for testability — the world is supplied via the two predicates.
     */
    static List<Direction> optimalFlowDirections(BlockPos origin, Predicate<BlockPos> canFlowInto,
                                                 Predicate<BlockPos> isSource) {
        Direction[] dirs = {Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};
        int[] cost = new int[4];
        int min = SOURCE_FLOW_COST_MAX;
        for (int i = 0; i < dirs.length; i++) {
            cost[i] = SOURCE_FLOW_COST_MAX;
            BlockPos target = origin.relative(dirs[i]);
            if (canFlowInto.test(target) && !isSource.test(target)) {
                cost[i] = canFlowInto.test(target.below())
                        ? 0
                        : calculateFlowCost(target, 1, dirs[i], canFlowInto, isSource);
            }
            min = Math.min(min, cost[i]);
        }
        List<Direction> optimal = new ArrayList<>(4);
        for (int i = 0; i < dirs.length; i++) {
            if (cost[i] == min) {
                optimal.add(dirs[i]);
            }
        }
        return optimal;
    }

    /** Source {@code calculateFlowCost}: BFS toward the nearest drop, never reversing back the way we came. */
    private static int calculateFlowCost(BlockPos pos, int depth, Direction fromDir,
                                         Predicate<BlockPos> canFlowInto, Predicate<BlockPos> isSource) {
        int cost = SOURCE_FLOW_COST_MAX;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (dir == fromDir.getOpposite()) {
                continue;
            }
            BlockPos target = pos.relative(dir);
            if (canFlowInto.test(target) && !isSource.test(target)) {
                if (canFlowInto.test(target.below())) {
                    return depth;
                }
                if (depth < SOURCE_FLOW_RECURSE_LIMIT) {
                    cost = Math.min(cost, calculateFlowCost(target, depth + 1, dir, canFlowInto, isSource));
                }
            }
        }
        return cost;
    }

    private boolean trySpread(ServerLevel level, BlockPos target, int distance, BrewFluidBlockEntity source, boolean gas) {
        return trySpread(level, target, distance, source, gas, Direction.NORTH);
    }

    private boolean trySpread(ServerLevel level, BlockPos target, int distance, BrewFluidBlockEntity source, boolean gas,
                              Direction flowDirection) {
        BlockState targetState = level.getBlockState(target);
        if (targetState.getBlock() instanceof BrewFluidBlock fluid && fluid.isGas() == gas) {
            if (targetState.getValue(DISTANCE) <= distance) {
                return false;
            }
            level.setBlock(target, targetState
                    .setValue(DISTANCE, distance)
                    .setValue(FLOW, horizontal(flowDirection)), Block.UPDATE_ALL);
        } else {
            if (!canDisplace(targetState)) {
                return false;
            }
            level.setBlock(target, defaultBlockState()
                    .setValue(DISTANCE, distance)
                    .setValue(FLOW, horizontal(flowDirection)), Block.UPDATE_ALL);
        }
        if (level.getBlockEntity(target) instanceof BrewFluidBlockEntity targetFluid) {
            targetFluid.copyFrom(source);
        }
        level.scheduleTick(target, this, tickRate());
        return true;
    }

    private static Direction horizontal(Direction direction) {
        return direction.getAxis().isHorizontal() ? direction : Direction.NORTH;
    }

    public static boolean canDisplace(BlockState state) {
        return canDisplaceForBrew(state.isAir(), state.canBeReplaced(), state.isSolid(),
                state.hasBlockEntity(), !state.getFluidState().isEmpty());
    }

    static boolean canDisplaceForBrew(boolean air, boolean replaceable, boolean solid,
                                      boolean hasBlockEntity, boolean hasFluid) {
        if (hasFluid) {
            return false;
        }
        return air || replaceable || (!solid && !hasBlockEntity);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier effectApplier, boolean isMoved) {
        // Scheduled ticks handle gas and liquid contact deterministically at the source cadence.
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
        return gas ? Shapes.empty() : SOURCE_LIQUID_SHAPES[liquidHeightIndex(state.getValue(DISTANCE))];
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        return adjacentState.is(this) && adjacentState.getValue(DISTANCE).equals(state.getValue(DISTANCE))
                || super.skipRendering(state, adjacentState, direction);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        return true;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (gas && random.nextInt(2) == 0) {
            DustParticleOptions brewDust = new DustParticleOptions(brewColor(level, pos), 1.1F);
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(brewDust, x, y, z,
                    0.0D, 0.01D, 0.0D);
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.WITCH, x, y, z, 0.0D, 0.01D, 0.0D);
            }
        } else if (!gas && random.nextInt(4) == 0) {
            double y = pos.getY() + (liquidHeightPixels(state.getValue(DISTANCE)) / 16.0D) + 0.02D;
            level.addParticle(new DustParticleOptions(brewColor(level, pos), 0.75F),
                    pos.getX() + random.nextDouble(),
                    y,
                    pos.getZ() + random.nextDouble(),
                    0.0D, 0.015D, 0.0D);
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.SPLASH,
                        pos.getX() + random.nextDouble(),
                        y,
                        pos.getZ() + random.nextDouble(),
                        0.0D, 0.0D, 0.0D);
            }
        }
    }

    private static VoxelShape[] createLiquidShapes() {
        VoxelShape[] shapes = new VoxelShape[SOURCE_LIQUID_HEIGHT_PIXELS.length];
        for (int i = 0; i < shapes.length; i++) {
            shapes[i] = Block.box(0.0D, 0.0D, 0.0D, 16.0D, SOURCE_LIQUID_HEIGHT_PIXELS[i], 16.0D);
        }
        return shapes;
    }

    private static int liquidHeightIndex(int distance) {
        return Math.clamp(distance, 0, SOURCE_LIQUID_HEIGHT_PIXELS.length - 1);
    }

    private static int liquidHeightPixels(int distance) {
        return SOURCE_LIQUID_HEIGHT_PIXELS[liquidHeightIndex(distance)];
    }

    private static int brewColor(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof BrewFluidBlockEntity brew && brew.getColor() != 0) {
            return brew.getColor() & 0xFFFFFF;
        }
        return DEFAULT_BREW_COLOR;
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
