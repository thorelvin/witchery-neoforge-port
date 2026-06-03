package com.emoniph.witchery.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player active crystal-ball prediction (the prophecy that has not yet come true). Stored on the
 * overworld so it survives logout/restart, keyed by player UUID. One prediction per player, matching
 * source ({@code PredictionManager}, MAX_CONCURRENT = 1).
 */
public class PredictionData extends SavedData {

    public record Active(int id, long time) {}

    private final Map<UUID, Active> map = new HashMap<>();

    private record Entry(UUID uuid, int id, long time) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("uuid").forGetter(Entry::uuid),
                Codec.INT.fieldOf("id").forGetter(Entry::id),
                Codec.LONG.fieldOf("time").forGetter(Entry::time)
        ).apply(i, Entry::new));
    }

    public static final Codec<PredictionData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf().optionalFieldOf("predictions", List.of()).forGetter(d ->
                    d.map.entrySet().stream()
                            .map(e -> new Entry(e.getKey(), e.getValue().id(), e.getValue().time())).toList())
    ).apply(i, PredictionData::fromCodec));

    public static final SavedDataType<PredictionData> TYPE = new SavedDataType<>(
            "witchery_predictions", PredictionData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    public PredictionData() {}

    private static PredictionData fromCodec(List<Entry> entries) {
        PredictionData data = new PredictionData();
        for (Entry e : entries) {
            data.map.put(e.uuid(), new Active(e.id(), e.time()));
        }
        return data;
    }

    public static PredictionData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Active get(UUID uuid) {
        return map.get(uuid);
    }

    public void set(UUID uuid, Active active) {
        map.put(uuid, active);
        setDirty();
    }

    public void remove(UUID uuid) {
        if (map.remove(uuid) != null) {
            setDirty();
        }
    }
}
