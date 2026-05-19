package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DemonHeartBlockEntity extends BlockEntity {

    public DemonHeartBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.DEMON_HEART.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, DemonHeartBlockEntity blockEntity) {
        if (level.getGameTime() % 25 != 3) {
            return;
        }
        level.playLocalSound(
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5,
                SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.BLOCKS,
                0.35f,
                1.6f,
                false
        );
    }
}
