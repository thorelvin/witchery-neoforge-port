package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcherySoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DemonHeartBlockEntity extends BlockEntity {
    private long ticks = 0L;

    public DemonHeartBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.DEMON_HEART.get(), pos, state);
    }

    public long getTicks() {
        return ticks;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, DemonHeartBlockEntity blockEntity) {
        blockEntity.ticks = blockEntity.ticks >= Long.MAX_VALUE ? 1L : blockEntity.ticks + 1L;
        if (blockEntity.ticks % 25L != 0L) {
            return;
        }
        level.playLocalSound(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                WitcherySoundEvents.RANDOM_HEARTBEAT.get(),
                SoundSource.BLOCKS,
                0.8f,
                1.0f,
                false
        );
    }
}
