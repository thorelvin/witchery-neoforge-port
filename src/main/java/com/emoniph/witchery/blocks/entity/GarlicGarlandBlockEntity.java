package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class GarlicGarlandBlockEntity extends BlockEntity {
    public GarlicGarlandBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.GARLIC_GARLAND.get(), pos, state);
    }
}
