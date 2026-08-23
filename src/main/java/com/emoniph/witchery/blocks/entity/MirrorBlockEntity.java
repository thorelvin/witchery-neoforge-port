package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
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
    private boolean bottomOccupied;
    private boolean topOccupied;

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

    public boolean isBottomOccupied() {
        return bottomOccupied;
    }

    public boolean isTopOccupied() {
        return topOccupied;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
        setChanged();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, MirrorBlockEntity blockEntity) {
        if (level.getGameTime() % 10L != 1L) {
            return;
        }
        Direction facing = state.getValue(com.emoniph.witchery.blocks.MirrorBlock.FACING);
        blockEntity.bottomOccupied = hasLivingEntity(level, sourceDetectionBounds(pos, facing));
        blockEntity.topOccupied = hasLivingEntity(level, sourceDetectionBounds(pos.above(), facing));
    }

    static AABB sourceDetectionBounds(BlockPos pos, Direction facing) {
        return switch (facing) {
            case NORTH -> new AABB(pos.getX() - 1, pos.getY(), pos.getZ() - 4,
                    pos.getX() + 2, pos.getY() + 1, pos.getZ() + 1);
            case SOUTH -> new AABB(pos.getX() - 1, pos.getY(), pos.getZ(),
                    pos.getX() + 2, pos.getY() + 1, pos.getZ() + 5);
            case WEST -> new AABB(pos.getX() - 4, pos.getY(), pos.getZ() - 1,
                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 2);
            case EAST -> new AABB(pos.getX(), pos.getY(), pos.getZ() - 1,
                    pos.getX() + 5, pos.getY() + 1, pos.getZ() + 2);
            default -> throw new IllegalArgumentException("Mirrors require a horizontal facing");
        };
    }

    private static boolean hasLivingEntity(Level level, AABB bounds) {
        return !level.getEntitiesOfClass(LivingEntity.class, bounds).isEmpty();
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
