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

    private String owner = "";

    public StatueHobgoblinPatronBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.STATUE_HOBGOBLIN_PATRON.get(), pos, state);
    }

    public void setOwner(String owner) {
        this.owner = owner == null ? "" : owner;
        setChanged();
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
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        owner = tag.getStringOr(OWNER_KEY, "");
    }
}
