package com.emoniph.witchery.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tracks which villages have already had walls generated so they are not
 * re-processed on world reload. Keyed by packed (minX, minZ) of each
 * village's structure-start bounding box.
 */
public class VillageWallData extends SavedData {

    public record WallRecord(long key, int gateCandidates, int gates) {
        static final Codec<WallRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("key").forGetter(WallRecord::key),
                Codec.INT.fieldOf("gate_candidates").forGetter(WallRecord::gateCandidates),
                Codec.INT.fieldOf("gates").forGetter(WallRecord::gates)
        ).apply(instance, WallRecord::new));
    }

    public static final Codec<VillageWallData> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    WallRecord.CODEC.listOf().fieldOf("walls").forGetter(d -> d.records.values().stream()
                            .sorted(Comparator.comparingLong(WallRecord::key))
                            .toList())
            ).apply(inst, VillageWallData::fromCodec));

    public static final SavedDataType<VillageWallData> TYPE = new SavedDataType<>(
            "witchery_village_walls",
            VillageWallData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<Long, WallRecord> records = new HashMap<>();

    public VillageWallData() {}

    private static VillageWallData fromCodec(List<WallRecord> records) {
        VillageWallData d = new VillageWallData();
        for (WallRecord record : records) {
            d.records.put(record.key(), record);
        }
        return d;
    }

    public static VillageWallData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isProcessed(BoundingBox bb) {
        return records.containsKey(key(bb));
    }

    public Optional<WallRecord> record(BoundingBox bb) {
        return Optional.ofNullable(records.get(key(bb)));
    }

    public void markProcessed(BoundingBox bb, int gateCandidates, int gates) {
        records.put(key(bb), new WallRecord(key(bb), gateCandidates, gates));
        setDirty();
    }

    public void reset() {
        records.clear();
        setDirty();
    }

    private static long key(BoundingBox bb) {
        return ((long) (bb.minX() & 0xFFFFFFF) | ((long) (bb.minZ() & 0xFFFFFFF) << 28));
    }
}
