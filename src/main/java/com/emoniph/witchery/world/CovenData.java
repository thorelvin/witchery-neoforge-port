package com.emoniph.witchery.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persisted per-player coven roster. Source ({@code EntityCovenWitch}) stores a player's recruited
 * witches as a {@code WITCCoven} list on the player's NBT and counts that list for the 6-witch cap.
 * The port keeps recruited witches as live world entities (so they can fight as allies), so the roster
 * here is a parallel record of which witch UUIDs belong to which player — a stable, persisted basis for
 * the cap that survives the witches being unloaded, mirroring the source player-NBT roster.
 */
public class CovenData extends SavedData {

    /** Source {@code MAX_COVEN_SIZE}. */
    public static final int MAX_COVEN = 6;

    private final Map<UUID, List<UUID>> covens = new HashMap<>();

    private record Entry(UUID owner, List<UUID> witches) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("owner").forGetter(Entry::owner),
                UUIDUtil.CODEC.listOf().fieldOf("witches").forGetter(Entry::witches)
        ).apply(i, Entry::new));
    }

    public static final Codec<CovenData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf().optionalFieldOf("covens", List.of()).forGetter(d ->
                    d.covens.entrySet().stream()
                            .map(e -> new Entry(e.getKey(), List.copyOf(e.getValue()))).toList())
    ).apply(i, CovenData::fromCodec));

    public static final SavedDataType<CovenData> TYPE = new SavedDataType<>(
            "witchery_covens", CovenData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    public CovenData() {}

    private static CovenData fromCodec(List<Entry> entries) {
        CovenData data = new CovenData();
        for (Entry e : entries) {
            data.covens.put(e.owner(), new ArrayList<>(e.witches()));
        }
        return data;
    }

    public static CovenData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Number of witches recruited by {@code owner}. */
    public int size(UUID owner) {
        List<UUID> list = covens.get(owner);
        return list == null ? 0 : list.size();
    }

    public boolean isFull(UUID owner) {
        return size(owner) >= MAX_COVEN;
    }

    public boolean contains(UUID owner, UUID witch) {
        List<UUID> list = covens.get(owner);
        return list != null && list.contains(witch);
    }

    /** Adds a witch to the owner's roster; returns false if it was already present or the coven is full. */
    public boolean add(UUID owner, UUID witch) {
        List<UUID> list = covens.computeIfAbsent(owner, k -> new ArrayList<>());
        if (list.contains(witch) || list.size() >= MAX_COVEN) {
            return false;
        }
        list.add(witch);
        setDirty();
        return true;
    }

    public void remove(UUID owner, UUID witch) {
        List<UUID> list = covens.get(owner);
        if (list != null && list.remove(witch)) {
            if (list.isEmpty()) {
                covens.remove(owner);
            }
            setDirty();
        }
    }

    /** Empties an owner's whole roster (QA / reset). */
    public void clear(UUID owner) {
        if (covens.remove(owner) != null) {
            setDirty();
        }
    }
}
