package com.emoniph.witchery.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Finite Witchery spirit liquid stand-in.
 * Source used Forge BlockFluidClassic with five quanta per block; this mirrors
 * that spread shape closely enough for gameplay without adding a full fluid.
 */
public abstract class SpiritLiquidBlock extends Block {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 4);
    public static final int SOURCE_QUANTA_PER_BLOCK = 5;
    public static final int SOURCE_TICK_RATE = 5;
    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };
    private static final VoxelShape[] SHAPES = {
            box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            box(0.0D, 0.0D, 0.0D, 16.0D, 13.0D, 16.0D),
            box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D),
            box(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D),
            box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D)
    };

    protected SpiritLiquidBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this)) {
            level.scheduleTick(pos, this, SOURCE_TICK_RATE);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess,
                                     BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState,
                                     RandomSource random) {
        scheduledTickAccess.scheduleTick(pos, this, SOURCE_TICK_RATE);
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, SOURCE_TICK_RATE);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int liquidLevel = state.getValue(LEVEL);
        if (liquidLevel > 0 && !hasFeed(level, pos, liquidLevel)) {
            level.removeBlock(pos, false);
            return;
        }

        boolean flowedDown = tryFlow(level, pos.below(), 0);
        if (!flowedDown && liquidLevel < SOURCE_QUANTA_PER_BLOCK - 1) {
            int nextLevel = liquidLevel + 1;
            for (Direction direction : HORIZONTAL) {
                tryFlow(level, pos.relative(direction), nextLevel);
            }
        }

        level.scheduleTick(pos, this, SOURCE_TICK_RATE);
    }

    private boolean hasFeed(ServerLevel level, BlockPos pos, int liquidLevel) {
        BlockState above = level.getBlockState(pos.above());
        if (above.is(this)) {
            return true;
        }
        for (Direction direction : HORIZONTAL) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (neighbor.is(this) && neighbor.getValue(LEVEL) < liquidLevel) {
                return true;
            }
        }
        return false;
    }

    private boolean tryFlow(ServerLevel level, BlockPos target, int liquidLevel) {
        if (!level.isInWorldBounds(target)) {
            return false;
        }
        BlockState targetState = level.getBlockState(target);
        if (targetState.is(this)) {
            if (targetState.getValue(LEVEL) <= liquidLevel) {
                return false;
            }
            level.setBlock(target, targetState.setValue(LEVEL, liquidLevel), Block.UPDATE_ALL);
            level.scheduleTick(target, this, SOURCE_TICK_RATE);
            return true;
        }
        if (!canDisplace(targetState)) {
            return false;
        }
        level.setBlock(target, defaultBlockState().setValue(LEVEL, liquidLevel), Block.UPDATE_ALL);
        level.scheduleTick(target, this, SOURCE_TICK_RATE);
        return true;
    }

    public static boolean canDisplace(BlockState state) {
        return canDisplaceForSpirit(state.isAir(), state.canBeReplaced(), state.isSolid(),
                state.hasBlockEntity(), !state.getFluidState().isEmpty());
    }

    static boolean canDisplaceForSpirit(boolean air, boolean replaceable, boolean solid,
                                        boolean hasBlockEntity, boolean hasFluid) {
        if (hasFluid) {
            return false;
        }
        return air || replaceable || (!solid && !hasBlockEntity);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LEVEL)];
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        return adjacentState.is(this) || super.skipRendering(state, adjacentState, direction);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        return true;
    }
}
