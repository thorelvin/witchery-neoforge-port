package com.emoniph.witchery.world;

import com.emoniph.witchery.WitcheryMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Live registry of Vanishing Witch Huts, keyed by a stable hut id so a single hut can be tracked
 * as it relocates (unlike {@link WitcheryStructureData}, which is an append-only landmark index
 * with no update/remove). The same record also owns the hut's relocation-cycle bargain and
 * restraint state, preventing restart rerolls or a second hut lifecycle registry.
 */
public class VanishingHutData extends SavedData {

    public record Entry(UUID hutId, String dimension, BlockPos pos, Optional<UUID> residentUuid,
                        int bargainCycle, boolean bargainCompleted, int restraintStage,
                        long saltHeldSince) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("hut_id").forGetter(Entry::hutId),
                Codec.STRING.fieldOf("dimension").forGetter(Entry::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                UUIDUtil.CODEC.optionalFieldOf("resident_uuid").forGetter(Entry::residentUuid),
                Codec.INT.optionalFieldOf("bargain_cycle", 0).forGetter(Entry::bargainCycle),
                Codec.BOOL.optionalFieldOf("bargain_completed", false).forGetter(Entry::bargainCompleted),
                Codec.INT.optionalFieldOf("restraint_stage", 0).forGetter(Entry::restraintStage),
                Codec.LONG.optionalFieldOf("salt_held_since", -1L).forGetter(Entry::saltHeldSince)
        ).apply(i, Entry::new));

        public Entry relocated(String dimension, BlockPos pos, @Nullable UUID residentUuid) {
            return new Entry(hutId, dimension, pos.immutable(), Optional.ofNullable(residentUuid),
                    bargainCycle + 1, false, 0, -1L);
        }

        public Entry at(String dimension, BlockPos pos, @Nullable UUID residentUuid,
                        boolean preserveResidentWhenMissing) {
            Optional<UUID> resident = residentUuid == null && preserveResidentWhenMissing
                    ? residentUuid() : Optional.ofNullable(residentUuid);
            return new Entry(hutId, dimension, pos.immutable(), resident, bargainCycle,
                    bargainCompleted, restraintStage, saltHeldSince);
        }

        public Entry withCompletedBargain() {
            return bargainCompleted ? this : new Entry(hutId, dimension, pos, residentUuid,
                    bargainCycle, true, restraintStage, saltHeldSince);
        }

        public Entry observeSalt(long gameTime) {
            int stage = Math.max(1, restraintStage);
            long since = saltHeldSince < 0L ? gameTime : saltHeldSince;
            return stage == restraintStage && since == saltHeldSince ? this
                    : new Entry(hutId, dimension, pos, residentUuid, bargainCycle, bargainCompleted,
                    stage, since);
        }

        public Entry withRestraintStage(int stage) {
            int next = Math.max(restraintStage, stage);
            return next == restraintStage ? this : new Entry(hutId, dimension, pos, residentUuid,
                    bargainCycle, bargainCompleted, next, saltHeldSince);
        }

        public Entry clearContinuousSalt() {
            return saltHeldSince < 0L ? this : new Entry(hutId, dimension, pos, residentUuid,
                    bargainCycle, bargainCompleted, restraintStage, -1L);
        }

        public boolean bargainAvailable() {
            return !bargainCompleted && restraintStage == 0;
        }
    }

    public static final Codec<VanishingHutData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf().optionalFieldOf("huts", List.of())
                    .forGetter(d -> List.copyOf(d.entries.values()))
    ).apply(i, VanishingHutData::fromCodec));

    public static final SavedDataType<VanishingHutData> TYPE = new SavedDataType<>(
            "witchery_vanishing_huts",
            VanishingHutData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, Entry> entries = new HashMap<>();

    public VanishingHutData() {}

    private static VanishingHutData fromCodec(List<Entry> entries) {
        VanishingHutData data = new VanishingHutData();
        for (Entry entry : entries) {
            data.entries.put(entry.hutId(), entry);
        }
        return data;
    }

    public static VanishingHutData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Insert or move a hut to its current dimension + position. */
    public void upsert(UUID hutId, ServerLevel level, BlockPos pos) {
        upsertInternal(hutId, level, pos, null, true);
    }

    /** Insert or update a hut and its current resident without advancing its relocation cycle. */
    public void upsert(UUID hutId, ServerLevel level, BlockPos pos, @Nullable UUID residentUuid) {
        upsertInternal(hutId, level, pos, residentUuid, false);
    }

    private void upsertInternal(UUID hutId, ServerLevel level, BlockPos pos,
                                @Nullable UUID residentUuid, boolean preserveResidentWhenMissing) {
        Entry existing = entries.get(hutId);
        Entry updated = existing == null
                ? new Entry(hutId, dimensionId(level), pos.immutable(), Optional.ofNullable(residentUuid),
                0, false, 0, -1L)
                : existing.at(dimensionId(level), pos, residentUuid, preserveResidentWhenMissing);
        entries.put(hutId, updated);
        setDirty();
        WitcheryMod.LOGGER.info("[VanishingHut] Tracked hut {} at {} in {}.", hutId, pos, dimensionId(level));
    }

    /** Commit a successful relocation: advance the bargain cycle and clear prior restraint. */
    public Entry relocate(UUID hutId, ServerLevel level, BlockPos pos, @Nullable UUID residentUuid) {
        Entry existing = entries.get(hutId);
        Entry updated = existing == null
                ? new Entry(hutId, dimensionId(level), pos.immutable(), Optional.ofNullable(residentUuid),
                0, false, 0, -1L)
                : existing.relocated(dimensionId(level), pos, residentUuid);
        entries.put(hutId, updated);
        setDirty();
        return updated;
    }

    public Optional<Entry> completeBargain(UUID hutId) {
        return update(hutId, Entry::withCompletedBargain);
    }

    public Optional<Entry> observeSalt(UUID hutId, long gameTime) {
        return update(hutId, entry -> entry.observeSalt(gameTime));
    }

    public Optional<Entry> setRestraintStage(UUID hutId, int stage) {
        return update(hutId, entry -> entry.withRestraintStage(stage));
    }

    public Optional<Entry> clearContinuousSalt(UUID hutId) {
        return update(hutId, Entry::clearContinuousSalt);
    }

    private Optional<Entry> update(UUID hutId, java.util.function.UnaryOperator<Entry> operation) {
        Entry before = entries.get(hutId);
        if (before == null) {
            return Optional.empty();
        }
        Entry after = operation.apply(before);
        if (after != before) {
            entries.put(hutId, after);
            setDirty();
        }
        return Optional.of(after);
    }

    public void remove(UUID hutId) {
        if (entries.remove(hutId) != null) {
            setDirty();
        }
    }

    public Optional<Entry> get(UUID hutId) {
        return Optional.ofNullable(entries.get(hutId));
    }

    public Optional<Entry> byResident(UUID residentUuid) {
        return entries.values().stream()
                .filter(entry -> entry.residentUuid().filter(residentUuid::equals).isPresent())
                .findFirst();
    }

    /** Nearest tracked hut in the given dimension to {@code from}. */
    public Optional<Entry> nearest(ServerLevel level, BlockPos from) {
        String dimension = dimensionId(level);
        Entry best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entry entry : entries.values()) {
            if (!dimension.equals(entry.dimension())) continue;
            double distance = entry.pos().distSqr(from);
            if (distance < bestDistance) {
                best = entry;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    public int size() {
        return entries.size();
    }

    private static String dimensionId(ServerLevel level) {
        return level.dimension().identifier().toString();
    }
}
