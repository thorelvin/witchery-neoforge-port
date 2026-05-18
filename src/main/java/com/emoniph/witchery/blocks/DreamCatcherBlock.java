package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.DreamCatcherBlockEntity;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class DreamCatcherBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Map.of(
        Direction.NORTH, Block.box( 1, 0, 13, 15, 14, 16),
        Direction.SOUTH, Block.box( 1, 0,  0, 15, 14,  3),
        Direction.EAST,  Block.box( 0, 0,  1,  3, 14, 15),
        Direction.WEST,  Block.box(13, 0,  1, 16, 14, 15)
    ));

    public static final MapCodec<DreamCatcherBlock> CODEC = simpleCodec(DreamCatcherBlock::new);
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public DreamCatcherBlock(BlockBehaviour.Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        for (Direction dir : ctx.getNearestLookingDirections()) {
            if (dir.getAxis().isHorizontal()) {
                BlockState candidate = defaultBlockState().setValue(FACING, dir);
                if (candidate.canSurvive(ctx.getLevel(), ctx.getClickedPos()))
                    return candidate;
            }
        }
        return null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos wallPos = pos.relative(facing.getOpposite());
        return level.getBlockState(wallPos).isFaceSturdy(level, wallPos, facing);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                net.minecraft.world.level.block.Block block, net.minecraft.world.level.redstone.@org.jspecify.annotations.Nullable Orientation fromPos, boolean isMoving) {
        if (!canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
        }
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }

    // Right-click with empty hand → retrieve weave
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof DreamCatcherBlockEntity be)) return InteractionResult.PASS;
        DreamWeaveType weave = be.getWeave();
        if (weave == null) return InteractionResult.PASS;
        be.setWeave(null);
        Block.popResource(level, pos, WitcheryItems.getDreamWeaveItem(weave).getDefaultInstance());
        return InteractionResult.SUCCESS;
    }

    // Drop weave item on block break
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof DreamCatcherBlockEntity be && be.getWeave() != null) {
                Block.popResource(level, pos, WitcheryItems.getDreamWeaveItem(be.getWeave()).getDefaultInstance());
            }
        }
        // super.onRemove removed - method no longer exists in 1.21.11
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DreamCatcherBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, WitcheryBlockEntities.DREAM_CATCHER.get(),
                                     DreamCatcherBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
}
