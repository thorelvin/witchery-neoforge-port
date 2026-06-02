package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for {@link com.emoniph.witchery.blocks.WolfHeadBlock} / {@code HellhoundHeadBlock}.
 * Stores only the floor-placement rotation segment (wall facing comes from the block state), and
 * syncs it to the client for the BESR. Mirrors {@code AlluringSkullBlockEntity} sans the tick.
 */
public class WolfHeadBlockEntity extends BlockEntity {

    private static final String KEY_ROTATION = "Rotation";

    private int rotationSegment;

    public WolfHeadBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.WOLF_HEAD.get(), pos, state);
    }

    public int getRotationSegment() {
        return rotationSegment;
    }

    public void setRotationSegment(int rotationSegment) {
        if (this.rotationSegment == rotationSegment) {
            return;
        }
        this.rotationSegment = rotationSegment;
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
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

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putInt(KEY_ROTATION, rotationSegment);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        rotationSegment = tag.getIntOr(KEY_ROTATION, 0);
    }
}
