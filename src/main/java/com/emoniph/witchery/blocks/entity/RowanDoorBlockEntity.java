package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Stores the UUID that identifies this door instance — placed on the LOWER half only. */
public class RowanDoorBlockEntity extends BlockEntity {

    @Nullable private UUID doorUUID;

    public RowanDoorBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.ROWAN_DOOR.get(), pos, state);
    }

    public Optional<UUID> getDoorUUID() { return Optional.ofNullable(doorUUID); }

    public void setDoorUUID(UUID uuid) {
        this.doorUUID = uuid;
        setChanged();
    }

    public boolean isLocked() { return doorUUID != null; }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) {
        super.saveAdditional(tag);
        if (doorUUID != null) {
            tag.putLong("DoorUUIDMost",  doorUUID.getMostSignificantBits());
            tag.putLong("DoorUUIDLeast", doorUUID.getLeastSignificantBits());
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) {
        super.loadAdditional(tag);
        if (tag.getInt("DoorUUIDMost").isPresent()) {
            doorUUID = new UUID(tag.getLongOr("DoorUUIDMost", 0L), tag.getLongOr("DoorUUIDLeast", 0L));
        }
    }
}
