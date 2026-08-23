package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.blocks.CoffinBlock;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.HolderLookup;

/** Simple block entity backing the Coffin block — needed for BESR rendering. */
public class CoffinBlockEntity extends BlockEntity {
    private static final String SOURCE_OPENED_KEY = "Opened";

    private boolean sourceOpened;
    private float lidAngle;
    private float previousLidAngle;

    public CoffinBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.COFFIN.get(), pos, state);
    }

    public boolean isSourceOpened() {
        return sourceOpened;
    }

    public float getLidAngle(float partialTick) {
        return Mth.lerp(partialTick, previousLidAngle, lidAngle);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, CoffinBlockEntity blockEntity) {
        blockEntity.previousLidAngle = blockEntity.lidAngle;
        boolean open = state.hasProperty(CoffinBlock.OPEN)
                ? state.getValue(CoffinBlock.OPEN)
                : blockEntity.sourceOpened;
        blockEntity.lidAngle = sourceNextLidAngle(blockEntity.lidAngle, open);
    }

    static float sourceNextLidAngle(float current, boolean open) {
        return open ? Math.min(1.0F, current + 0.1F) : Math.max(0.0F, current - 0.1F);
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider reg) {
        return saveWithoutMetadata(reg);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putBoolean(SOURCE_OPENED_KEY, currentOpenState());
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        sourceOpened = tag.getBooleanOr(SOURCE_OPENED_KEY, false);
    }

    private boolean currentOpenState() {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof CoffinBlock && state.hasProperty(CoffinBlock.OPEN)) {
            return state.getValue(CoffinBlock.OPEN);
        }
        return sourceOpened;
    }
}
