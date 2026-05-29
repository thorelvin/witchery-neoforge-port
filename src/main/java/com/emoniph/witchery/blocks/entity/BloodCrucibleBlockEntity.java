package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class BloodCrucibleBlockEntity extends BlockEntity {

    public static final int MAX_BLOOD = 20;
    private static final int FILL_STEP = 5;

    private int bloodLevel = 0;

    public BloodCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.BLOOD_CRUCIBLE.get(), pos, state);
    }

    public int getBloodLevel() { return bloodLevel; }
    public boolean isFull() { return bloodLevel >= MAX_BLOOD; }
    public float getPercentFilled() { return bloodLevel / (float) MAX_BLOOD; }

    public void increaseBloodLevel(Level level) {
        if (bloodLevel >= MAX_BLOOD) return;
        bloodLevel = Math.min(MAX_BLOOD, bloodLevel + FILL_STEP);
        setChanged();
        sync(level);
    }

    public void drainAll(Level level) {
        if (bloodLevel == 0) return;
        bloodLevel = 0;
        setChanged();
        sync(level);
    }

    private void sync(@Nullable Level level) {
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putInt("BloodLevel", bloodLevel);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        bloodLevel = Math.clamp(tag.getIntOr("BloodLevel", 0), 0, MAX_BLOOD);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
