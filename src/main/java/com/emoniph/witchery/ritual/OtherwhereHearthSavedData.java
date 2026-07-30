package com.emoniph.witchery.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Crash journal for the additive Otherwhere Hearth crossing.
 *
 * <p>The journal lives in the Overworld data store and contains both complete block-volume
 * snapshots before any mutation occurs. A prepared transaction is therefore an instruction to
 * apply one known final state, not to repeat a relative "swap whatever is there now" operation.
 */
public final class OtherwhereHearthSavedData extends SavedData {
    private PendingSwap pending;

    public static final Codec<OtherwhereHearthSavedData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    PendingSwap.CODEC.optionalFieldOf("pending")
                            .forGetter(data -> Optional.ofNullable(data.pending))
            ).apply(instance, OtherwhereHearthSavedData::fromCodec));

    public static final SavedDataType<OtherwhereHearthSavedData> TYPE = new SavedDataType<>(
            "witchery_otherwhere_hearth",
            OtherwhereHearthSavedData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    public OtherwhereHearthSavedData() {
    }

    private static OtherwhereHearthSavedData fromCodec(Optional<PendingSwap> pending) {
        OtherwhereHearthSavedData data = new OtherwhereHearthSavedData();
        data.pending = pending.orElse(null);
        return data;
    }

    public static OtherwhereHearthSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    PendingSwap pending() {
        return pending;
    }

    boolean hasPending() {
        return pending != null;
    }

    void setPending(PendingSwap pending) {
        this.pending = pending;
        setDirty();
    }

    void clearPending() {
        this.pending = null;
        setDirty();
    }

    enum Phase {
        PREPARED,
        WAITING_FOR_PLAYERS
    }

    record PendingSwap(
            UUID transactionId,
            UUID ownerId,
            String ownerName,
            BlockPos kettlePos,
            BlockPos origin,
            BlockPos altarPos,
            boolean sourceSpirit,
            int powerCost,
            boolean powerPaid,
            Phase phase,
            CompoundTag overworldSnapshot,
            CompoundTag spiritSnapshot,
            List<EntityTransfer> entities
    ) {
        static final Codec<PendingSwap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("transaction_id").forGetter(PendingSwap::transactionId),
                UUIDUtil.CODEC.fieldOf("owner_id").forGetter(PendingSwap::ownerId),
                Codec.STRING.optionalFieldOf("owner_name", "").forGetter(PendingSwap::ownerName),
                BlockPos.CODEC.fieldOf("kettle_pos").forGetter(PendingSwap::kettlePos),
                BlockPos.CODEC.fieldOf("origin").forGetter(PendingSwap::origin),
                BlockPos.CODEC.fieldOf("altar_pos").forGetter(PendingSwap::altarPos),
                Codec.BOOL.fieldOf("source_spirit").forGetter(PendingSwap::sourceSpirit),
                Codec.INT.fieldOf("power_cost").forGetter(PendingSwap::powerCost),
                Codec.BOOL.optionalFieldOf("power_paid", false).forGetter(PendingSwap::powerPaid),
                Codec.STRING.xmap(Phase::valueOf, Phase::name)
                        .optionalFieldOf("phase", Phase.PREPARED).forGetter(PendingSwap::phase),
                CompoundTag.CODEC.fieldOf("overworld_snapshot").forGetter(PendingSwap::overworldSnapshot),
                CompoundTag.CODEC.fieldOf("spirit_snapshot").forGetter(PendingSwap::spiritSnapshot),
                EntityTransfer.CODEC.listOf().optionalFieldOf("entities", List.of()).forGetter(PendingSwap::entities)
        ).apply(instance, PendingSwap::new));

        PendingSwap {
            ownerName = ownerName == null ? "" : ownerName;
            overworldSnapshot = overworldSnapshot.copy();
            spiritSnapshot = spiritSnapshot.copy();
            entities = List.copyOf(entities);
        }

        PendingSwap withPowerPaid() {
            return new PendingSwap(transactionId, ownerId, ownerName, kettlePos, origin, altarPos,
                    sourceSpirit, powerCost, true, phase, overworldSnapshot, spiritSnapshot, entities);
        }

        PendingSwap waitingForPlayers() {
            return new PendingSwap(transactionId, ownerId, ownerName, kettlePos, origin, altarPos,
                    sourceSpirit, powerCost, powerPaid, Phase.WAITING_FOR_PLAYERS,
                    overworldSnapshot, spiritSnapshot, entities);
        }
    }

    record EntityTransfer(
            UUID entityId,
            boolean player,
            boolean toSpirit,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            Optional<UUID> vehicleId,
            CompoundTag entitySnapshot
    ) {
        static final Codec<EntityTransfer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("entity_id").forGetter(EntityTransfer::entityId),
                Codec.BOOL.fieldOf("player").forGetter(EntityTransfer::player),
                Codec.BOOL.fieldOf("to_spirit").forGetter(EntityTransfer::toSpirit),
                Codec.DOUBLE.fieldOf("x").forGetter(EntityTransfer::x),
                Codec.DOUBLE.fieldOf("y").forGetter(EntityTransfer::y),
                Codec.DOUBLE.fieldOf("z").forGetter(EntityTransfer::z),
                Codec.FLOAT.fieldOf("yaw").forGetter(EntityTransfer::yaw),
                Codec.FLOAT.fieldOf("pitch").forGetter(EntityTransfer::pitch),
                UUIDUtil.CODEC.optionalFieldOf("vehicle_id").forGetter(EntityTransfer::vehicleId),
                CompoundTag.CODEC.optionalFieldOf("entity_snapshot", new CompoundTag())
                        .forGetter(EntityTransfer::entitySnapshot)
        ).apply(instance, EntityTransfer::new));

        EntityTransfer {
            entitySnapshot = entitySnapshot.copy();
        }
    }
}
