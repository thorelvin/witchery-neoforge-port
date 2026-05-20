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
    private static final double RANGE = 64.0;
    private static final double HEIGHT = 10.0;
    private static final String KEY_ROTATION = "Rotation";

    private int rotationSegment;

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
        if (!state.getValue(AlluringSkullBlock.ON)) {
            return;
        }
        if (level.getGameTime() % TICK_INTERVAL != 0) {
            return;
        }

        AABB bounds = new AABB(pos).inflate(RANGE, HEIGHT, RANGE);
        for (PathfinderMob mob : level.getEntitiesOfClass(PathfinderMob.class, bounds)) {
            if (!mob.getType().is(EntityTypeTags.UNDEAD) || !mob.isAlive()) {
                continue;
            }
            mob.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Math.max(1.0, mob.getSpeed()));
        }
    }
}
