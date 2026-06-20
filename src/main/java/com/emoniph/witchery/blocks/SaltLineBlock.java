package com.emoniph.witchery.blocks;

import com.emoniph.witchery.init.WitcheryBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A thin trace of Witches' Salt laid on the ground. It carries no power — only the redstone wire's
 * <em>connection</em> behaviour is borrowed (adapted from {@code RedStoneWireBlock}): the NESW
 * {@link RedstoneSide} properties let salt link into continuous lines and climb the side of blocks,
 * so a laid-down "salt line" reads as one connected trace. Its only function is folklore counterplay
 * — a salt line across a {@link com.emoniph.witchery.worldgen.VanishingHut}'s threshold holds the hut
 * in place. Like redstone dust it sits on a sturdy top face and pops off when its support is removed.
 */
public class SaltLineBlock extends Block {

    public static final MapCodec<SaltLineBlock> CODEC = simpleCodec(SaltLineBlock::new);

    public static final EnumProperty<RedstoneSide> NORTH = BlockStateProperties.NORTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> EAST = BlockStateProperties.EAST_REDSTONE;
    public static final EnumProperty<RedstoneSide> SOUTH = BlockStateProperties.SOUTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> WEST = BlockStateProperties.WEST_REDSTONE;

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

    public SaltLineBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, RedstoneSide.NONE)
                .setValue(EAST, RedstoneSide.NONE)
                .setValue(SOUTH, RedstoneSide.NONE)
                .setValue(WEST, RedstoneSide.NONE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connectionState(context.getLevel(), defaultBlockState(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction facing, BlockPos facingPos,
                                     BlockState facingState, RandomSource random) {
        if (facing == Direction.DOWN) {
            return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
        }
        if (facing == Direction.UP) {
            return connectionState(level, state, pos);
        }
        // A horizontal neighbour changed: recompute just that side's connection.
        return state.setValue(propertyFor(facing), connectingSide(level, pos, facing));
    }

    private BlockState connectionState(LevelReader level, BlockState base, BlockPos pos) {
        return base
                .setValue(NORTH, connectingSide(level, pos, Direction.NORTH))
                .setValue(EAST, connectingSide(level, pos, Direction.EAST))
                .setValue(SOUTH, connectingSide(level, pos, Direction.SOUTH))
                .setValue(WEST, connectingSide(level, pos, Direction.WEST));
    }

    /** Adapted from {@code RedStoneWireBlock.getConnectingSide}: connect to salt across or up a block. */
    private RedstoneSide connectingSide(LevelReader level, BlockPos pos, Direction direction) {
        BlockPos sidePos = pos.relative(direction);
        BlockState sideState = level.getBlockState(sidePos);
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        if (!aboveState.isRedstoneConductor(level, abovePos)) {
            boolean sturdy = sideState.isFaceSturdy(level, sidePos, Direction.UP) || sideState.is(this);
            if (sturdy && isSalt(level.getBlockState(sidePos.above()))) {
                return RedstoneSide.UP;
            }
        }
        if (isSalt(sideState)) {
            return RedstoneSide.SIDE;
        }
        if (sideState.isRedstoneConductor(level, sidePos)) {
            return RedstoneSide.NONE;
        }
        return isSalt(level.getBlockState(sidePos.below())) ? RedstoneSide.SIDE : RedstoneSide.NONE;
    }

    private boolean isSalt(BlockState state) {
        return state.is(this) || state.is(WitcheryBlocks.SALT_LINE.get());
    }

    private static EnumProperty<RedstoneSide> propertyFor(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            default -> WEST;
        };
    }
}
