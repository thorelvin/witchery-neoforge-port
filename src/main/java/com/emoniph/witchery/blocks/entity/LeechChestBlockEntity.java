package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LeechChestBlockEntity extends BlockEntity {
    public LeechChestBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.LEECH_CHEST.get(), pos, state);
    }
}
