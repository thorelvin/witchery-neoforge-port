package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CandelabraBlockEntity extends BlockEntity {
    public CandelabraBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.CANDELABRA.get(), pos, state);
    }
}
