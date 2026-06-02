package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.WolfHeadBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Mounted Wolf Head trophy block (floor- or wall-placed, directional with a floor rotation).
 *
 * Source parity: {@code BlockWolfHead} + {@code ItemWolfHead} (1.7.10-0.24.1). The original was
 * one block with a {@code skullType} 0=wolf / 1=hellhound; the port splits it into two blocks
 * ({@link #isHellhound()} distinguishes them) sharing this class, the {@link WolfHeadBlockEntity},
 * and the BESR. Placement/shape mirror the port's {@code AlluringSkullBlock}.
 */
public class WolfHeadBlock extends BaseEntityBlock {

    public static final MapCodec<WolfHeadBlock> CODEC = simpleCodec(WolfHeadBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    private static final VoxelShape FLOOR_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0);
    private static final VoxelShape NORTH_SHAPE = Block.box(4.0, 4.0, 8.0, 12.0, 12.0, 16.0);
    private static final VoxelShape SOUTH_SHAPE = Block.box(4.0, 4.0, 0.0, 12.0, 12.0, 8.0);
    private static final VoxelShape WEST_SHAPE = Block.box(8.0, 4.0, 4.0, 16.0, 12.0, 12.0);
    private static final VoxelShape EAST_SHAPE = Block.box(0.0, 4.0, 4.0, 8.0, 12.0, 12.0);

    public WolfHeadBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** false = wolf (vanilla wolf texture), true = hellhound (witchery hellhound texture). */
    public boolean isHellhound() {
        return false;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        LevelReader level = context.getLevel();
        BlockState floorState = defaultBlockState().setValue(FACING, Direction.UP);

        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction == Direction.UP) {
                continue;
            }
            BlockState state = direction == Direction.DOWN
                    ? floorState
                    : defaultBlockState().setValue(FACING, direction.getOpposite());
            if (state.canSurvive(level, pos) && level.isUnobstructed(state, pos, CollisionContext.empty())) {
                return state;
            }
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || state.getValue(FACING) != Direction.UP
                || !(level.getBlockEntity(pos) instanceof WolfHeadBlockEntity head)) {
            return;
        }
        float rotation = placer != null ? placer.getYRot() : 0.0F;
        head.setRotationSegment(RotationSegment.convertToSegment(rotation));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos supportPos = facing == Direction.UP ? pos.below() : pos.relative(facing.getOpposite());
        return Block.canSupportCenter(level, supportPos, facing == Direction.UP ? Direction.UP : facing);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                net.minecraft.world.level.redstone.@org.jspecify.annotations.Nullable Orientation orientation,
                                boolean movedByPiston) {
        if (!canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> FLOOR_SHAPE;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WolfHeadBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
