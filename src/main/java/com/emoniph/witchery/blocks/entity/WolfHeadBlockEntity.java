package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.blocks.WolfHeadBlock;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for {@link WolfHeadBlock} / {@code HellhoundHeadBlock}. The original 1.7.10 block
 * stored both wolf/hellhound variant and floor rotation as source tile keys; wall facing comes from
 * the block state in the port.
 */
public class WolfHeadBlockEntity extends BlockEntity {

    private static final String SOURCE_SKULL_TYPE_KEY = "SkullType";
    private static final String SOURCE_ROTATION_KEY = "Rot";
    private static final String LEGACY_PORT_ROTATION_KEY = "Rotation";

    private int skullType = -1;
    private int rotationSegment;

    public WolfHeadBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.WOLF_HEAD.get(), pos, state);
    }

    public int getRotationSegment() {
        return rotationSegment;
    }

    public int getSkullType() {
        return normalizeSkullType(skullType >= 0 ? skullType : skullTypeFromBlockState(getBlockState()));
    }

    public boolean isHellhoundSkull() {
        return getSkullType() == 1;
    }

    public void setSkullType(int skullType) {
        int normalized = normalizeSkullType(skullType);
        if (this.skullType == normalized) {
            return;
        }
        this.skullType = normalized;
        sync();
    }

    public void setRotationSegment(int rotationSegment) {
        int normalized = normalizeRotation(rotationSegment);
        if (this.rotationSegment == normalized) {
            return;
        }
        this.rotationSegment = normalized;
        sync();
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
        tag.putByte(SOURCE_SKULL_TYPE_KEY, (byte) getSkullType());
        tag.putByte(SOURCE_ROTATION_KEY, (byte) rotationSegment);
        tag.putInt(LEGACY_PORT_ROTATION_KEY, rotationSegment);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        int defaultSkullType = skullTypeFromBlockState(getBlockState());
        skullType = normalizeSkullType(Byte.toUnsignedInt(tag.getByteOr(SOURCE_SKULL_TYPE_KEY, (byte) defaultSkullType)));
        int legacyRotation = tag.getIntOr(LEGACY_PORT_ROTATION_KEY, 0);
        rotationSegment = normalizeRotation(Byte.toUnsignedInt(tag.getByteOr(SOURCE_ROTATION_KEY, (byte) legacyRotation)));
    }

    private void sync() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    private static int normalizeSkullType(int skullType) {
        return skullType == 1 ? 1 : 0;
    }

    private static int normalizeRotation(int rotation) {
        return rotation & 15;
    }

    private static int skullTypeFromBlockState(BlockState state) {
        return state.getBlock() instanceof WolfHeadBlock head && head.isHellhound() ? 1 : 0;
    }
}
