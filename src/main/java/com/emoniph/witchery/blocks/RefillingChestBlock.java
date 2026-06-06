package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.RefillingChestBlockEntity;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * Witchery Refilling Chest using the vanilla chest visuals/lid path while keeping
 * Witchery's refill mechanics in its custom block entity.
 */
public class RefillingChestBlock extends ChestBlock {

    public static final MapCodec<RefillingChestBlock> CODEC = simpleCodec(RefillingChestBlock::new);

    @Override
    public MapCodec<? extends ChestBlock> codec() {
        return CODEC;
    }

    public RefillingChestBlock(Properties props) {
        super(() -> WitcheryBlockEntities.REFILLING_CHEST.get(), SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, props);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        return state == null ? null : state.setValue(WATERLOGGED, false);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess scheduledTickAccess,
            BlockPos currentPos,
            Direction facing,
            BlockPos facingPos,
            BlockState facingState,
            RandomSource random
    ) {
        BlockState updated = super.updateShape(state, level, scheduledTickAccess, currentPos, facing, facingPos, facingState, random);
        if (updated.hasProperty(WATERLOGGED) && updated.getValue(WATERLOGGED)) {
            updated = updated.setValue(WATERLOGGED, false);
        }
        return updated;
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        return false;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RefillingChestBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return createTickerHelper(type, WitcheryBlockEntities.REFILLING_CHEST.get(), ChestBlockEntity::lidAnimateTick);
        }
        return createTickerHelper(type, WitcheryBlockEntities.REFILLING_CHEST.get(), RefillingChestBlockEntity::serverTick);
    }
}
