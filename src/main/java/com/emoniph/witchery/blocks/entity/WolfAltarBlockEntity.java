package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Marker block entity for the Wolf Altar, so its statue can be drawn by a {@code BlockEntityRenderer}. */
public class WolfAltarBlockEntity extends BlockEntity {
    public WolfAltarBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.WOLF_ALTAR.get(), pos, state);
    }
}
