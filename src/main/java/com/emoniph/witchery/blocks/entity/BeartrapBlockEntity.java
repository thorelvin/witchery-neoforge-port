package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BeartrapBlockEntity extends BlockEntity {
    private static final String KEY_OWNER_UUID = "OwnerUUID";
    private static final String KEY_OWNER_NAME = "OwnerName";

    private String ownerUuid = "";
    private String ownerName = "";

    public BeartrapBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.BEARTRAP.get(), pos, state);
    }

    public void setOwner(Player player) {
        ownerUuid = player.getStringUUID();
        ownerName = player.getScoreboardName();
        setChanged();
    }

    public boolean isOwner(Player player) {
        return player.getStringUUID().equals(ownerUuid);
    }

    public boolean hasOwner() {
        return !ownerUuid.isEmpty();
    }

    @Nullable
    public UUID ownerUuid() {
        if (ownerUuid.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(ownerUuid);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public String ownerName() {
        return ownerName;
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
        tag.putString(KEY_OWNER_UUID, ownerUuid);
        tag.putString(KEY_OWNER_NAME, ownerName);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        ownerUuid = tag.getStringOr(KEY_OWNER_UUID, "");
        ownerName = tag.getStringOr(KEY_OWNER_NAME, "");
    }
}
