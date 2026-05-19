package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class PlacedItemBlockEntity extends BlockEntity {

    private ItemStack stack = ItemStack.EMPTY;

    public PlacedItemBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.PLACED_ITEM.get(), pos, state);
    }

    public ItemStack getStack() { return stack; }

    public void setStack(ItemStack stack) {
        this.stack = stack.copy();
        setChanged();
        notifySupportingAltar();
    }

    public ItemStack takeStack() {
        ItemStack stored = stack;
        stack = ItemStack.EMPTY;
        setChanged();
        notifySupportingAltar();
        return stored;
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) {
        super.saveAdditional(tag);
        if (!stack.isEmpty())
            tag.store("Item", net.minecraft.world.item.ItemStack.CODEC, stack);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) {
        super.loadAdditional(tag);
        stack = tag.read("Item", net.minecraft.world.item.ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    private void notifySupportingAltar() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            if (level.getBlockEntity(worldPosition.below()) instanceof AltarBlockEntity altar) {
                altar.invalidate(level);
            }
        }
    }
}
