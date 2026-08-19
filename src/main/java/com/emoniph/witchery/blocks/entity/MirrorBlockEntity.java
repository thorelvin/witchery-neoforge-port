package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Stores per-mirror data:
 *  - room chunk coords (assigned Mirror World room)
 *  - demonKilled flag (room cleared)
 *  - linkedPos / linkedDim (for inner mirrors, points back to the outer overworld mirror)
 */
public class MirrorBlockEntity extends BlockEntity {

    private int roomChunkX = Integer.MIN_VALUE;
    private int roomSectionY = Integer.MIN_VALUE;
    private int roomChunkZ = Integer.MIN_VALUE;
    private boolean demonKilled = false;

    @Nullable private BlockPos linkedPos = null;
    @Nullable private String linkedDim   = null;
    private boolean connected = true;

    public MirrorBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.MIRROR.get(), pos, state);
    }

    public boolean hasRoom() {
        return roomChunkX != Integer.MIN_VALUE && roomSectionY != Integer.MIN_VALUE
                && roomChunkZ != Integer.MIN_VALUE;
    }

    public void setRoom(int cx, int sectionY, int cz) {
        roomChunkX = cx;
        roomSectionY = sectionY;
        roomChunkZ = cz;
        setChanged();
    }

    public int[] getRoom() {
        return new int[]{ roomChunkX, roomSectionY, roomChunkZ };
    }

    public boolean isDemonKilled() { return demonKilled; }

    public void setDemonKilled(boolean killed) {
        demonKilled = killed;
        setChanged();
    }

    @Nullable public BlockPos getLinkedPos() { return linkedPos; }

    public void setLinkedPos(@Nullable BlockPos pos) {
        linkedPos = pos;
        setChanged();
    }

    @Nullable public String getLinkedDim() { return linkedDim; }

    public void setLinkedDim(@Nullable String dim) {
        linkedDim = dim;
        setChanged();
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
        setChanged();
    }

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        if (hasRoom()) {
            tag.putInt("RoomCX", roomChunkX);
            tag.putInt("RoomY", roomSectionY);
            tag.putInt("RoomCZ", roomChunkZ);
        }
        tag.putBoolean("DemonKilled", demonKilled);
        tag.putBoolean("Connected", connected);
        if (linkedPos != null) {
            tag.putLong("LinkedPos", linkedPos.asLong());
        }
        if (linkedDim != null) {
            tag.putString("LinkedDim", linkedDim);
        }
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        int cx = tag.getIntOr("RoomCX", Integer.MIN_VALUE);
        if (cx != Integer.MIN_VALUE) {
            roomChunkX = cx;
            roomSectionY = tag.getIntOr("RoomY", Integer.MIN_VALUE);
            roomChunkZ = tag.getIntOr("RoomCZ", Integer.MIN_VALUE);
        }
        demonKilled = tag.getBooleanOr("DemonKilled", false);
        connected = tag.getBooleanOr("Connected", true);
        long linkedPosLong = tag.getLongOr("LinkedPos", Long.MIN_VALUE);
        if (linkedPosLong != Long.MIN_VALUE) {
            linkedPos = BlockPos.of(linkedPosLong);
        }
        String dim = tag.getStringOr("LinkedDim", "");
        linkedDim = dim.isEmpty() ? null : dim;
    }
}
