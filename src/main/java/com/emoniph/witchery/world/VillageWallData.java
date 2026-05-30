package com.emoniph.witchery.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks which villages have already had walls generated so they are not
 * re-processed on world reload. Keyed by packed (minX, minZ) of each
 * village's structure-start bounding box.
 */
public class VillageWallData extends SavedData {

    public static final Codec<VillageWallData> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Codec.LONG.listOf().fieldOf("walls").forGetter(d -> new ArrayList<>(d.processed))
            ).apply(inst, VillageWallData::fromCodec));

    public static final SavedDataType<VillageWallData> TYPE = new SavedDataType<>(
            "witchery_village_walls",
            VillageWallData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Set<Long> processed = new HashSet<>();

    public VillageWallData() {}

    private static VillageWallData fromCodec(List<Long> keys) {
        VillageWallData d = new VillageWallData();
        d.processed.addAll(keys);
        return d;
    }

    public static VillageWallData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isProcessed(BoundingBox bb) {
        return processed.contains(key(bb));
    }

    public void markProcessed(BoundingBox bb) {
        processed.add(key(bb));
        setDirty();
    }

    private static long key(BoundingBox bb) {
        return ((long) (bb.minX() & 0xFFFFFFF) | ((long) (bb.minZ() & 0xFFFFFFF) << 28));
    }
}
