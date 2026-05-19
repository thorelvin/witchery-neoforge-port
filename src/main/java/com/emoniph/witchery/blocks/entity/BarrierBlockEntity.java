package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class BarrierBlockEntity extends BlockEntity {
    private static final String KEY_REMAINING_TICKS = "remainingTicks";
    private static final String KEY_BLOCKS_PLAYERS = "blocksPlayers";

    private int remainingTicks = 60;
    private boolean blocksPlayers;

    public BarrierBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.BARRIER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BarrierBlockEntity be) {
        if (--be.remainingTicks <= 0) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return;
        }
        if (be.remainingTicks % 10 == 0) {
            be.setChanged();
        }
    }

    public void refresh(int ticksUntilExpiration, boolean blocksPlayers) {
        this.remainingTicks = ticksUntilExpiration;
        this.blocksPlayers = blocksPlayers;
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public boolean blocksPlayers() {
        return blocksPlayers;
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
        tag.putInt(KEY_REMAINING_TICKS, remainingTicks);
        tag.putBoolean(KEY_BLOCKS_PLAYERS, blocksPlayers);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        remainingTicks = tag.getIntOr(KEY_REMAINING_TICKS, 60);
        blocksPlayers = tag.getBooleanOr(KEY_BLOCKS_PLAYERS, false);
    }
}
