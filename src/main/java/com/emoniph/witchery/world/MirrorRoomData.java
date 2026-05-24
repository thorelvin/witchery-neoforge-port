package com.emoniph.witchery.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Tracks the next available Mirror World room index.
 * Rooms are allocated on a 64-wide grid: room N is at chunk coords (N%64, N/64).
 */
public class MirrorRoomData extends SavedData {

    public static final Codec<MirrorRoomData> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Codec.INT.fieldOf("NextRoom").forGetter(d -> d.nextRoom)
            ).apply(inst, MirrorRoomData::fromCodec));

    public static final SavedDataType<MirrorRoomData> TYPE = new SavedDataType<>(
            "witchery_mirror_rooms",
            MirrorRoomData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private int nextRoom;

    public MirrorRoomData() {
        this.nextRoom = 0;
    }

    private static MirrorRoomData fromCodec(int nextRoom) {
        MirrorRoomData data = new MirrorRoomData();
        data.nextRoom = nextRoom;
        return data;
    }

    /** Allocate a room and return [chunkX, chunkZ]. */
    public int[] allocateRoom() {
        int index = nextRoom++;
        setDirty();
        return new int[]{ index % 64, index / 64 };
    }

    public static MirrorRoomData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }
}
