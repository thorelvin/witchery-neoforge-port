package com.emoniph.witchery.world;

import com.emoniph.witchery.WitcheryMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Persisted index of Witchery structures after their pieces actually place.
 * Goto commands can consult this instead of re-running expensive locate scans.
 */
public class WitcheryStructureData extends SavedData {
    public static final String WITCH_WELL = "witchery:witch_well";
    public static final String VILLAGE_BUILDING = "witchery:village_building";

    public record Entry(String dimension, String structure, String variant, BlockPos pos, long gameTime) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("dimension").forGetter(Entry::dimension),
                Codec.STRING.fieldOf("structure").forGetter(Entry::structure),
                Codec.STRING.optionalFieldOf("variant", "").forGetter(Entry::variant),
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                Codec.LONG.optionalFieldOf("game_time", 0L).forGetter(Entry::gameTime)
        ).apply(i, Entry::new));
    }

    private record Key(String dimension, String structure, String variant, BlockPos pos) {
        static Key of(Entry entry) {
            return new Key(entry.dimension(), entry.structure(), entry.variant(), entry.pos().immutable());
        }
    }

    public static final Codec<WitcheryStructureData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf().optionalFieldOf("structures", List.of())
                    .forGetter(d -> List.copyOf(d.entries.values()))
    ).apply(i, WitcheryStructureData::fromCodec));

    public static final SavedDataType<WitcheryStructureData> TYPE = new SavedDataType<>(
            "witchery_structures",
            WitcheryStructureData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<Key, Entry> entries = new HashMap<>();

    public WitcheryStructureData() {}

    private static WitcheryStructureData fromCodec(List<Entry> entries) {
        WitcheryStructureData data = new WitcheryStructureData();
        for (Entry entry : entries) {
            data.entries.put(Key.of(entry), entry);
        }
        return data;
    }

    public static WitcheryStructureData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void record(ServerLevel level, String structure, String variant, BlockPos pos) {
        Entry entry = new Entry(dimensionId(level), structure, variant == null ? "" : variant,
                pos.immutable(), level.getGameTime());
        recordEntry(entry);
    }

    void recordForTest(String dimension, String structure, String variant, BlockPos pos, long gameTime) {
        recordEntry(new Entry(dimension, structure, variant == null ? "" : variant, pos.immutable(), gameTime));
    }

    private void recordEntry(Entry entry) {
        Key key = Key.of(entry);
        if (entries.containsKey(key)) {
            return;
        }

        entries.put(key, entry);
        setDirty();
        WitcheryMod.LOGGER.info("[Structures] Recorded {}{} in {} at {}.",
                entry.structure(), entry.variant().isEmpty() ? "" : " (" + entry.variant() + ")",
                entry.dimension(), entry.pos());
    }

    public Optional<Entry> nearest(ServerLevel level, String structure, String variant, BlockPos from) {
        return nearest(dimensionId(level), structure, variant, from);
    }

    Optional<Entry> nearest(String dimension, String structure, String variant, BlockPos from) {
        String wantedVariant = variant == null ? "" : variant;
        Entry best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Entry entry : entries.values()) {
            if (!dimension.equals(entry.dimension())) continue;
            if (!structure.equals(entry.structure())) continue;
            if (!wantedVariant.isEmpty() && !wantedVariant.equals(entry.variant())) continue;

            double distance = entry.pos().distSqr(from);
            if (distance < bestDistance) {
                best = entry;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    public List<Entry> entriesWithin(ServerLevel level, String structure, BoundingBox box, int padding) {
        String dimension = dimensionId(level);
        return entries.values().stream()
                .filter(entry -> dimension.equals(entry.dimension()))
                .filter(entry -> structure.equals(entry.structure()))
                .filter(entry -> entry.pos().getX() >= box.minX() - padding
                        && entry.pos().getX() <= box.maxX() + padding
                        && entry.pos().getZ() >= box.minZ() - padding
                        && entry.pos().getZ() <= box.maxZ() + padding)
                .sorted(java.util.Comparator.comparing(Entry::variant).thenComparing(Entry::gameTime))
                .toList();
    }

    public int size() {
        return entries.size();
    }

    private static String dimensionId(ServerLevel level) {
        return level.dimension().identifier().toString();
    }
}
