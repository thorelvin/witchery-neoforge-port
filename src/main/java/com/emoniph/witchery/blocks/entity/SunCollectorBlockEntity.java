package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class SunCollectorBlockEntity extends BlockEntity {

    // Ticks of direct sunlight needed to fully charge one quartz sphere
    public static final int CHARGE_TICKS = 6000;

    private ItemStack stored = ItemStack.EMPTY;
    private int chargeProgress = 0;

    public SunCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.SUN_COLLECTOR.get(), pos, state);
    }

    public ItemStack getStored() { return stored; }
    public int getChargeProgress() { return chargeProgress; }

    public boolean isEmpty() { return stored.isEmpty(); }

    public void insertSphere(ItemStack stack, Level level) {
        this.stored = stack.copyWithCount(1);
        stack.shrink(1);
        chargeProgress = 0;
        setChanged();
        syncToClient(level);
    }

    public ItemStack retrieveStored(Level level) {
        ItemStack result = stored;
        stored = ItemStack.EMPTY;
        chargeProgress = 0;
        setChanged();
        syncToClient(level);
        return result;
    }

    private void syncToClient(Level level) {
        if (level != null && !level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public static void serverTick(Level levelRaw, BlockPos pos, BlockState state,
                                  SunCollectorBlockEntity be) {
        if (!(levelRaw instanceof ServerLevel level)) return;
        if (be.stored.isEmpty()) return;
        if (!be.stored.is(WitcheryItems.QUARTZ_SPHERE.get())) return;

        long dayTime = level.getDayTime() % 24000L;
        boolean isDaytime = dayTime >= 0 && dayTime < 13000;
        if (!isDaytime) return;
        if (!level.canSeeSky(pos.above())) return;
        if (level.isRaining()) return;

        be.chargeProgress++;
        if (be.chargeProgress >= CHARGE_TICKS) {
            be.stored = WitcheryItems.CHARGED_QUARTZ_SPHERE.get().getDefaultInstance();
            be.chargeProgress = 0;
            be.setChanged();
            be.syncToClient(level);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        if (!stored.isEmpty()) tag.store("stored", ItemStack.CODEC, stored);
        tag.putInt("ChargeProgress", chargeProgress);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        stored = tag.read("stored", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        chargeProgress = tag.getIntOr("ChargeProgress", 0);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
