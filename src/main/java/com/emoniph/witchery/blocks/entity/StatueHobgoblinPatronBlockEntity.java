package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class StatueHobgoblinPatronBlockEntity extends BlockEntity {
    public static final String ITEM_OWNER_KEY = "WITCBoundPlayer";
    private static final String OWNER_KEY = "Owner";
    private static final String STORED_PRAYERS_KEY = "StoredPrayers";
    private static final String LIFETIME_PRAYERS_KEY = "LifetimePrayers";
    private static final int MAX_STORED_PRAYERS = 64;

    private String owner = "";
    private int storedPrayers;
    private int lifetimePrayers;

    public StatueHobgoblinPatronBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.STATUE_HOBGOBLIN_PATRON.get(), pos, state);
    }

    public void setOwner(String owner) {
        this.owner = owner == null ? "" : owner;
        setChanged();
    }

    public int storedPrayers() {
        return storedPrayers;
    }

    public int lifetimePrayers() {
        return lifetimePrayers;
    }

    public boolean hasPrayers() {
        return storedPrayers > 0;
    }

    public int addPrayer(int amount) {
        if (amount <= 0 || storedPrayers >= MAX_STORED_PRAYERS) {
            return 0;
        }
        int accepted = Math.min(amount, MAX_STORED_PRAYERS - storedPrayers);
        storedPrayers += accepted;
        lifetimePrayers += accepted;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return accepted;
    }

    public int drainPrayers() {
        int drained = storedPrayers;
        if (drained > 0) {
            storedPrayers = 0;
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
        return drained;
    }

    public ItemStack createDropStack(Block block) {
        ItemStack stack = new ItemStack(block);
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putString(ITEM_OWNER_KEY, owner));
        return stack;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putString(OWNER_KEY, owner);
        tag.putInt(STORED_PRAYERS_KEY, storedPrayers);
        tag.putInt(LIFETIME_PRAYERS_KEY, lifetimePrayers);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        owner = tag.getStringOr(OWNER_KEY, "");
        storedPrayers = Math.max(0, Math.min(MAX_STORED_PRAYERS, tag.getIntOr(STORED_PRAYERS_KEY, 0)));
        lifetimePrayers = Math.max(0, tag.getIntOr(LIFETIME_PRAYERS_KEY, storedPrayers));
    }
}
