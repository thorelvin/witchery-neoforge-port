package com.emoniph.witchery.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns Mirror World room allocation records. Each room has exactly one source mirror, allowing
 * guardian completion and reload verification to resolve links without proximity guesses.
 */
public class MirrorRoomData extends SavedData {

    public record Room(int index, String sourceDimension, long sourcePos, String guardianUuid) {
        public static final Codec<Room> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.fieldOf("Index").forGetter(Room::index),
                Codec.STRING.fieldOf("SourceDimension").forGetter(Room::sourceDimension),
                Codec.LONG.fieldOf("SourcePos").forGetter(Room::sourcePos),
                Codec.STRING.optionalFieldOf("Guardian", "").forGetter(Room::guardianUuid)
        ).apply(inst, Room::new));

        public int chunkX() {
            return MirrorWorldLayout.roomForIndex(index).chunkX();
        }

        public int sectionY() {
            return MirrorWorldLayout.roomForIndex(index).sectionY();
        }

        public int chunkZ() {
            return MirrorWorldLayout.roomForIndex(index).chunkZ();
        }

        public BlockPos sourceBlockPos() {
            return BlockPos.of(sourcePos);
        }

        public boolean belongsTo(String dimension, BlockPos pos) {
            return sourceDimension.equals(dimension) && sourcePos == pos.asLong();
        }

        public Optional<UUID> guardian() {
            if (guardianUuid.isBlank()) return Optional.empty();
            try {
                return Optional.of(UUID.fromString(guardianUuid));
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }

        private Room withGuardian(@Nullable UUID guardian) {
            return new Room(index, sourceDimension, sourcePos,
                    guardian == null ? "" : guardian.toString());
        }

        private Room withSource(String dimension, BlockPos pos) {
            return new Room(index, dimension, pos.asLong(), guardianUuid);
        }
    }

    public static final Codec<MirrorRoomData> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Codec.INT.fieldOf("NextRoom").forGetter(d -> d.nextRoom),
                    Room.CODEC.listOf().fieldOf("Rooms").forGetter(MirrorRoomData::savedRooms)
            ).apply(inst, MirrorRoomData::fromCodec));

    public static final SavedDataType<MirrorRoomData> TYPE = new SavedDataType<>(
            "witchery_mirror_rooms",
            MirrorRoomData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private int nextRoom;
    private final List<Room> rooms = new ArrayList<>();

    public MirrorRoomData() {
        this.nextRoom = 0;
    }

    private static MirrorRoomData fromCodec(int nextRoom, List<Room> savedRooms) {
        MirrorRoomData data = new MirrorRoomData();
        for (Room room : savedRooms) {
            if (room.index() >= 0
                    && data.rooms.stream().noneMatch(existing -> existing.index() == room.index())
                    && data.rooms.stream().noneMatch(existing -> existing.belongsTo(
                            room.sourceDimension(), room.sourceBlockPos()))) {
                data.rooms.add(room);
            }
        }
        data.rooms.sort(Comparator.comparingInt(Room::index));
        int highestClaimed = data.rooms.stream().mapToInt(Room::index).max().orElse(-1) + 1;
        data.nextRoom = Math.max(Math.max(0, nextRoom), highestClaimed);
        return data;
    }

    /** Returns the stable room already owned by this source mirror, or allocates a new one. */
    public Room getOrAllocate(ResourceKey<Level> sourceDimension, BlockPos sourcePos) {
        String dimension = sourceDimension.identifier().toString();
        Optional<Room> existing = roomForSource(dimension, sourcePos);
        if (existing.isPresent()) {
            return existing.get();
        }

        while (roomAtIndex(nextRoom).isPresent()) {
            nextRoom++;
        }
        Room room = new Room(nextRoom++, dimension, sourcePos.asLong(), "");
        rooms.add(room);
        setDirty();
        return room;
    }

    public Optional<Room> roomForSource(String dimension, BlockPos sourcePos) {
        return rooms.stream().filter(room -> room.belongsTo(dimension, sourcePos)).findFirst();
    }

    public Optional<Room> roomAt(int chunkX, int sectionY, int chunkZ) {
        int index = MirrorWorldLayout.indexForRoom(chunkX, sectionY, chunkZ);
        return index < 0 ? Optional.empty() : roomAtIndex(index);
    }

    public Optional<Room> roomAtIndex(int index) {
        return rooms.stream().filter(room -> room.index() == index).findFirst();
    }

    public List<Room> rooms() {
        return List.copyOf(rooms);
    }

    public int nextRoomIndex() {
        return nextRoom;
    }

    public void setGuardian(int roomIndex, @Nullable UUID guardian) {
        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            if (room.index() == roomIndex) {
                Room updated = room.withGuardian(guardian);
                if (!updated.equals(room)) {
                    rooms.set(i, updated);
                    setDirty();
                }
                return;
            }
        }
    }

    public boolean rebindRoom(int roomIndex, ResourceKey<Level> sourceDimension, BlockPos sourcePos) {
        String dimension = sourceDimension.identifier().toString();
        if (rooms.stream().anyMatch(room -> room.index() != roomIndex
                && room.belongsTo(dimension, sourcePos))) {
            return false;
        }
        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            if (room.index() == roomIndex) {
                rooms.set(i, room.withSource(dimension, sourcePos));
                setDirty();
                return true;
            }
        }
        return false;
    }

    private List<Room> savedRooms() {
        return List.copyOf(rooms);
    }

    public static MirrorRoomData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public static MirrorRoomData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Mirror room data requires the Overworld");
        }
        return get(overworld);
    }
}
