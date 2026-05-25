package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.blocks.BeartrapBlock;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BeartrapBlockEntity extends BlockEntity {
    private static final String KEY_OWNER_UUID = "OwnerUUID";
    private static final String KEY_OWNER_NAME = "OwnerName";
    private static final String KEY_WOLFTRAP_START = "WolftrapStart";
    private static final String KEY_WOLF_UUID = "Wolf";
    private static final int WOLFTRAP_CHECK_INTERVAL = 200;
    private static final double BAIT_RANGE = 8.0D;
    private static final double BAIT_RANGE_SQUARED = BAIT_RANGE * BAIT_RANGE;

    private String ownerUuid = "";
    private String ownerName = "";
    private long wolftrapStartTime;
    @Nullable
    private UUID spawnedWolfUuid;

    public BeartrapBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.BEARTRAP.get(), pos, state);
    }

    public void setOwner(Player player) {
        ownerUuid = player.getStringUUID();
        ownerName = player.getScoreboardName();
        setChanged();
    }

    public boolean isOwner(Player player) {
        return player.getStringUUID().equals(ownerUuid);
    }

    public boolean hasOwner() {
        return !ownerUuid.isEmpty();
    }

    @Nullable
    public UUID ownerUuid() {
        if (ownerUuid.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(ownerUuid);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public String ownerName() {
        return ownerName;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BeartrapBlockEntity trap) {
        if (!(level instanceof ServerLevel serverLevel)
                || !(state.getBlock() instanceof BeartrapBlock beartrapBlock)
                || !beartrapBlock.isSilvered()) {
            return;
        }
        if (state.getValue(BeartrapBlock.CLOSED)
                || trap.spawnedWolfUuid != null
                || serverLevel.getGameTime() % WOLFTRAP_CHECK_INTERVAL != 0L) {
            return;
        }

        if (trap.hasWolftrapBait(serverLevel, pos) && isFullMoonNight(serverLevel)) {
            long time = serverLevel.getGameTime();
            if (trap.wolftrapStartTime > 0L && time > trap.wolftrapStartTime) {
                trap.trySummonBaitWerewolf(serverLevel, pos);
            } else {
                trap.setWolftrapStartTime(time);
            }
        } else {
            trap.setWolftrapStartTime(0L);
        }
    }

    private boolean hasWolftrapBait(ServerLevel level, BlockPos pos) {
        if (!hasAdjacentWolfAltar(level, pos)) {
            return false;
        }
        AABB area = new AABB(pos).inflate(BAIT_RANGE);
        return !level.getEntitiesOfClass(Animal.class, area, animal -> animal.getType() == EntityType.SHEEP
                && animal.isLeashed()
                && animal.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= BAIT_RANGE_SQUARED).isEmpty();
    }

    private static boolean hasAdjacentWolfAltar(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(direction)).is(WitcheryBlocks.WOLF_ALTAR.get())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFullMoonNight(ServerLevel level) {
        long dayTime = level.getDayTime();
        long timeOfDay = dayTime % 24000L;
        return (dayTime / 24000L) % 8L == 0L && timeOfDay > 13000L && timeOfDay < 23000L;
    }

    private void trySummonBaitWerewolf(ServerLevel level, BlockPos pos) {
        // Source summons WitcheryEntities.WEREWOLF here; keep the trigger state live until that entity is ported.
    }

    private void setWolftrapStartTime(long startTime) {
        if (wolftrapStartTime != startTime) {
            wolftrapStartTime = startTime;
            setChanged();
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
        tag.putString(KEY_OWNER_UUID, ownerUuid);
        tag.putString(KEY_OWNER_NAME, ownerName);
        tag.putLong(KEY_WOLFTRAP_START, wolftrapStartTime);
        if (spawnedWolfUuid != null) {
            tag.putString(KEY_WOLF_UUID, spawnedWolfUuid.toString());
        }
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        ownerUuid = tag.getStringOr(KEY_OWNER_UUID, "");
        ownerName = tag.getStringOr(KEY_OWNER_NAME, "");
        wolftrapStartTime = tag.getLongOr(KEY_WOLFTRAP_START, 0L);
        spawnedWolfUuid = readUuid(tag.getStringOr(KEY_WOLF_UUID, ""));
    }

    @Nullable
    private static UUID readUuid(String value) {
        if (value.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
