package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.blocks.ChaliceBlock;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ChaliceBlockEntity extends BlockEntity {
    private static final String FILLED_KEY = "WITCFilled";

    private boolean loadedFilledState;

    public ChaliceBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.CHALICE.get(), pos, state);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        applyLoadedFilledState();
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        BlockState state = getBlockState();
        tag.putBoolean(FILLED_KEY, state.hasProperty(ChaliceBlock.FILLED) && state.getValue(ChaliceBlock.FILLED));
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        loadedFilledState = tag.getBooleanOr(FILLED_KEY, false);
        applyLoadedFilledState();
    }

    private void applyLoadedFilledState() {
        if (!loadedFilledState || level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(ChaliceBlock.FILLED) && !state.getValue(ChaliceBlock.FILLED)) {
            level.setBlock(worldPosition, state.setValue(ChaliceBlock.FILLED, true), 3);
        }
        loadedFilledState = false;
    }
}
