package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.blocks.SunCollectorBlock;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
        setPresentationPower(level, 1);
    }

    public ItemStack retrieveStored(Level level) {
        ItemStack result = stored;
        stored = ItemStack.EMPTY;
        chargeProgress = 0;
        setChanged();
        setPresentationPower(level, 0);
        return result;
    }

    private void setPresentationPower(Level level, int power) {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(SunCollectorBlock.POWER)) {
            syncToClient(level);
            return;
        }
        int clamped = Math.max(0, Math.min(15, power));
        if (state.getValue(SunCollectorBlock.POWER) != clamped) {
            level.setBlock(worldPosition, state.setValue(SunCollectorBlock.POWER, clamped), 3);
        }
    }

    private void syncToClient(Level level) {
        if (level != null && !level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public static void serverTick(Level levelRaw, BlockPos pos, BlockState state,
                                  SunCollectorBlockEntity be) {
        if (!(levelRaw instanceof ServerLevel level)) return;
        if (be.stored.isEmpty()) {
            be.setPresentationPower(level, 0);
            return;
        }
        if (be.stored.is(WitcheryItems.CHARGED_QUARTZ_SPHERE.get())) {
            be.setPresentationPower(level, 15);
            return;
        }
        if (!be.stored.is(WitcheryItems.QUARTZ_SPHERE.get())) {
            be.setPresentationPower(level, 1);
            return;
        }

        long dayTime = level.getDayTime() % 24000L;
        boolean isDaytime = dayTime >= 0 && dayTime < 13000;
        if (!isDaytime || !level.canSeeSky(pos.above()) || level.isRaining()) {
            be.setPresentationPower(level, 1);
            return;
        }

        be.chargeProgress++;
        be.setPresentationPower(level, 1 + Math.min(13, be.chargeProgress * 13 / CHARGE_TICKS));
        if (be.chargeProgress % 600 == 0) {
            level.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5D, pos.getY() + 0.75D, pos.getZ() + 0.5D,
                    6, 0.18D, 0.25D, 0.18D, 0.02D);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS, 0.25F, 1.6F);
        }
        if (be.chargeProgress >= CHARGE_TICKS) {
            be.stored = WitcheryItems.CHARGED_QUARTZ_SPHERE.get().getDefaultInstance();
            be.chargeProgress = 0;
            be.setChanged();
            be.setPresentationPower(level, 15);
            level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.9F, 1.35F);
            level.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5D, pos.getY() + 0.85D, pos.getZ() + 0.5D,
                    36, 0.35D, 0.45D, 0.35D, 0.06D);
            level.sendParticles(ParticleTypes.WITCH,
                    pos.getX() + 0.5D, pos.getY() + 0.65D, pos.getZ() + 0.5D,
                    12, 0.25D, 0.25D, 0.25D, 0.02D);
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
