package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class BrewFluidHoldBlockEntity extends BlockEntity {
    private static final String KEY_ORIGINAL_STATE = "OriginalState";
    private BlockState originalState;

    public BrewFluidHoldBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.BREW_FLUID_HOLD.get(), pos, state);
    }

    public void setOriginalState(BlockState originalState) {
        this.originalState = originalState;
        setChanged();
    }

    public BlockState originalState() {
        return originalState;
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        if (originalState != null) {
            tag.store(KEY_ORIGINAL_STATE, BlockState.CODEC, originalState);
        }
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        originalState = tag.read(KEY_ORIGINAL_STATE, BlockState.CODEC).orElse(null);
    }
}
