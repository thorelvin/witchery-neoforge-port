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

/**
 * Live registry of Vanishing Witch Huts, keyed by a stable hut id so a single hut can be tracked
 * as it relocates (unlike {@link WitcheryStructureData}, which is an append-only landmark index
 * with no update/remove). Stores the hut's current dimension + anchor position; goto reads it.
 */
public class VanishingHutData extends SavedData {

    public record Entry(UUID hutId, String dimension, BlockPos pos) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("hut_id").forGetter(Entry::hutId),
                Codec.STRING.fieldOf("dimension").forGetter(Entry::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos)
        ).apply(i, Entry::new));
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
        entries.put(hutId, new Entry(hutId, dimensionId(level), pos.immutable()));
        setDirty();
        WitcheryMod.LOGGER.info("[VanishingHut] Tracked hut {} at {} in {}.", hutId, pos, dimensionId(level));
    }

    public void remove(UUID hutId) {
        if (entries.remove(hutId) != null) {
            setDirty();
        }
    }

    public Optional<Entry> get(UUID hutId) {
        return Optional.ofNullable(entries.get(hutId));
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
