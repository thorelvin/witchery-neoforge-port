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

public class VoidBrambleBlockEntity extends BlockEntity {
    private static final String KEY_OWNER = "WITCPlacer";

    private String owner = "";

    public VoidBrambleBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.VOID_BRAMBLE.get(), pos, state);
    }

    public void setOwner(String owner) {
        this.owner = owner;
        setChanged();
    }

    public boolean isOwner(Player player) {
        return !owner.isEmpty() && owner.equals(player.getScoreboardName());
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
        tag.putString(KEY_OWNER, owner);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        owner = tag.getStringOr(KEY_OWNER, "");
    }
}
