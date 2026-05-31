package com.emoniph.witchery.blocks;

import com.emoniph.witchery.init.WitcheryBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.util.RandomSource;

/**
 * Source BlockStockade parity slice for the ice variant: low fence-like collision,
 * ice translucency, cactus-style contact damage, and source connection targets.
 */
public class StockadeBlock extends CrossCollisionBlock {
    public static final MapCodec<StockadeBlock> CODEC = simpleCodec(StockadeBlock::new);

    public StockadeBlock(BlockBehaviour.Properties properties) {
        super(3.2F, 3.2F, 14.4F, 14.4F, 14.4F, properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends CrossCollisionBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState fluid = level.getFluidState(pos);
        return defaultBlockState()
                .setValue(NORTH, connectsTo(level.getBlockState(pos.north()), Direction.SOUTH))
                .setValue(EAST, connectsTo(level.getBlockState(pos.east()), Direction.WEST))
                .setValue(SOUTH, connectsTo(level.getBlockState(pos.south()), Direction.NORTH))
                .setValue(WEST, connectsTo(level.getBlockState(pos.west()), Direction.EAST))
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess,
                                     BlockPos currentPos, Direction facing, BlockPos facingPos,
                                     BlockState facingState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            scheduledTickAccess.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return facing.getAxis().isHorizontal()
                ? state.setValue(PROPERTY_BY_DIRECTION.get(facing), connectsTo(facingState, facing.getOpposite()))
                : super.updateShape(state, level, scheduledTickAccess, currentPos, facing, facingPos, facingState, random);
    }

    private boolean connectsTo(BlockState state, Direction direction) {
        Block block = state.getBlock();
        if (block == this || state.is(Blocks.IRON_BARS)) {
            return true;
        }
        return block == WitcheryBlocks.ICE_GATE.get()
                && FenceGateBlock.connectsToDirection(state, direction);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier effectApplier, boolean isMoved) {
        if (!level.isClientSide() && entity instanceof LivingEntity) {
            entity.hurt(level.damageSources().cactus(), 3.0F);
        }
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return !state.getValue(WATERLOGGED);
    }

    @Override
    protected int getLightBlock(BlockState state) {
        return 1;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, WATERLOGGED);
    }
}
