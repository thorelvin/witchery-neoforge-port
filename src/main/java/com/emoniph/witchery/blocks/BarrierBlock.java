package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.BarrierBlockEntity;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BarrierBlock extends BaseEntityBlock {
    public static final MapCodec<BarrierBlock> CODEC = simpleCodec(BarrierBlock::new);
    private static final VoxelShape COLLISION_SHAPE = box(1, 1, 1, 15, 15, 15);

    public BarrierBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected int getLightBlock(BlockState state) {
        return 0;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Entity entity = context instanceof EntityCollisionContext entityContext ? entityContext.getEntity() : null;
        if (entity instanceof Player player
                && level.getBlockEntity(pos) instanceof BarrierBlockEntity barrier
                && (!barrier.blocksPlayers() || player.isCreative() && player.isShiftKeyDown())) {
            return Shapes.empty();
        }
        return COLLISION_SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BarrierBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, WitcheryBlockEntities.BARRIER.get(), BarrierBlockEntity::serverTick);
    }

    public static void setBarrier(Level level, BlockPos pos, int ticksUntilExpiration, boolean blocksPlayers) {
        BlockState existing = level.getBlockState(pos);
        FluidState fluidState = existing.getFluidState();
        boolean alreadyBarrier = existing.is(WitcheryBlocks.BARRIER.get());
        if (!alreadyBarrier && !existing.isAir() && fluidState.isEmpty()) {
            return;
        }

        if (!alreadyBarrier) {
            level.setBlock(pos, WitcheryBlocks.BARRIER.get().defaultBlockState(), 3);
        }

        if (level.getBlockEntity(pos) instanceof BarrierBlockEntity barrier) {
            barrier.refresh(ticksUntilExpiration, blocksPlayers);
        }
    }
}
