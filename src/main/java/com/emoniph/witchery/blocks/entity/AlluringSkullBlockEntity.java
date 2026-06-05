package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.blocks.AlluringSkullBlock;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class AlluringSkullBlockEntity extends BlockEntity {

    private static final int TICK_INTERVAL = 100;
    private static final int QUADRANT_COUNT = 8;
    private static final double RANGE = 64.0;
    private static final double VERTICAL_RANGE = 10.0;
    private static final double PATH_SPEED = 1.0;
    private static final String KEY_ROTATION = "Rotation";

    private int rotationSegment;
    private int quadrant;
    private long ticks;

    public AlluringSkullBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.ALLURING_SKULL.get(), pos, state);
    }

    public int getRotationSegment() {
        return rotationSegment;
    }

    public void setRotationSegment(int rotationSegment) {
        if (this.rotationSegment == rotationSegment) {
            return;
        }
        this.rotationSegment = rotationSegment;
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
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
        tag.putInt(KEY_ROTATION, rotationSegment);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        rotationSegment = tag.getIntOr(KEY_ROTATION, 0);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlluringSkullBlockEntity blockEntity) {
        blockEntity.tickCounter();
        if (!state.getValue(AlluringSkullBlock.ON)) {
            return;
        }
        if (blockEntity.ticks % TICK_INTERVAL != 0) {
            return;
        }

        blockEntity.quadrant = (blockEntity.quadrant + 1) % QUADRANT_COUNT;
        AABB bounds = quadrantBounds(pos, blockEntity.quadrant);
        for (PathfinderMob mob : level.getEntitiesOfClass(PathfinderMob.class, bounds)) {
            if (!mob.getType().is(EntityTypeTags.UNDEAD) || !mob.isAlive()) {
                continue;
            }
            mob.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), PATH_SPEED);
        }
    }

    private void tickCounter() {
        if (ticks == Long.MAX_VALUE) {
            ticks = 1L;
        } else {
            ticks++;
        }
    }

    private static AABB quadrantBounds(BlockPos pos, int quadrant) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        return switch (quadrant) {
            case 0 -> new AABB(x, y - VERTICAL_RANGE, z - RANGE, x + RANGE, y, z);
            case 1 -> new AABB(x - RANGE, y - VERTICAL_RANGE, z - RANGE, x, y, z);
            case 2 -> new AABB(x, y - VERTICAL_RANGE, z, x + RANGE, y, z + RANGE);
            case 3 -> new AABB(x - RANGE, y - VERTICAL_RANGE, z, x, y, z + RANGE);
            case 4 -> new AABB(x - RANGE, y + 1.0, z - RANGE, x, y + VERTICAL_RANGE, z);
            case 5 -> new AABB(x, y + 1.0, z, x + RANGE, y + VERTICAL_RANGE, z + RANGE);
            case 6 -> new AABB(x - RANGE, y + 1.0, z, x, y + VERTICAL_RANGE, z + RANGE);
            default -> new AABB(x, y + 1.0, z - RANGE, x + RANGE, y + VERTICAL_RANGE, z);
        };
    }
}
