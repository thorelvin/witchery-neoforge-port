package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class GrassperBlockEntity extends BlockEntity {

    private ItemStack stack = ItemStack.EMPTY;

    public GrassperBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.GRASSPER.get(), pos, state);
    }

    public ItemStack getItem() {
        return stack;
    }

    public void setItem(ItemStack stack) {
        this.stack = stack.copy();
        sync();
    }

    public ItemStack takeItem() {
        ItemStack result = stack;
        stack = ItemStack.EMPTY;
        sync();
        return result;
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
        if (!stack.isEmpty()) {
            tag.store("Item", ItemStack.CODEC, stack);
            NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
            items.set(0, stack);
            ContainerHelper.saveAllItems(tag, items);
        }
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        stack = tag.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (stack.isEmpty()) {
            NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, items);
            stack = items.getFirst();
        }
    }

    private void sync() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
    }
}
