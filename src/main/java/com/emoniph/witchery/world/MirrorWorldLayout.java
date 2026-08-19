package com.emoniph.witchery.world;

import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;

/** Shared geometry and source-order room-coordinate rules for the Mirror World. */
public final class MirrorWorldLayout {
    public static final int WORLD_MIN_Y = 0;
    public static final int WORLD_HEIGHT = 256;
    public static final int ROOM_SECTION_HEIGHT = 16;
    public static final int ALLOCATED_VERTICAL_ROOMS = 15;
    public static final int ROOM_FLOOR_LOCAL_Y = 6;
    public static final int ROOM_CEILING_LOCAL_Y = 15;
    public static final int INTERIOR_MIN = 4;
    public static final int INTERIOR_MAX = 12;
    public static final int SOURCE_SPIRAL_LIMIT = 256;

    private MirrorWorldLayout() {}

    /**
     * The source scans all fifteen Y cells before advancing through its X/Z spiral. Duplicate
     * coordinates produced at turns are naturally skipped because the first mirror already occupies
     * the cell; de-duplicating here gives the same fresh-world allocation order deterministically.
     */
    public static RoomCoordinates roomForIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Mirror room index must be non-negative");
        }
        int horizontalOrdinal = index / ALLOCATED_VERTICAL_ROOMS;
        int sectionY = index % ALLOCATED_VERTICAL_ROOMS;
        HorizontalCoordinates horizontal = horizontalForOrdinal(horizontalOrdinal);
        return new RoomCoordinates(horizontal.chunkX(), sectionY, horizontal.chunkZ());
    }

    public static int indexForRoom(int chunkX, int sectionY, int chunkZ) {
        if (sectionY < 0 || sectionY >= ALLOCATED_VERTICAL_ROOMS) {
            return -1;
        }
        int ordinal = ordinalForHorizontal(chunkX, chunkZ);
        return ordinal < 0 ? -1 : ordinal * ALLOCATED_VERTICAL_ROOMS + sectionY;
    }

    public static BlockPos roomCenter(int chunkX, int sectionY, int chunkZ) {
        return new BlockPos(chunkX * 16 + 8, sectionY * 16 + 9, chunkZ * 16 + 8);
    }

    /** Source inner mirror top is local (4, 8, 8), with its lower half at Y 7. */
    public static BlockPos innerMirrorTop(int chunkX, int sectionY, int chunkZ) {
        return new BlockPos(chunkX * 16 + 4, sectionY * 16 + 8, chunkZ * 16 + 8);
    }

    public static BlockPos innerMirrorFoot(int chunkX, int sectionY, int chunkZ) {
        return innerMirrorTop(chunkX, sectionY, chunkZ).below();
    }

    public static int roomFloorY(int sectionY) {
        return sectionY * ROOM_SECTION_HEIGHT + ROOM_FLOOR_LOCAL_Y;
    }

    public static int roomCeilingY(int sectionY) {
        return sectionY * ROOM_SECTION_HEIGHT + ROOM_CEILING_LOCAL_Y;
    }

    public static int sectionY(int blockY) {
        return Math.floorDiv(blockY, ROOM_SECTION_HEIGHT);
    }

    public static boolean isInterior(int localX, int y, int localZ) {
        int localY = Math.floorMod(y, ROOM_SECTION_HEIGHT);
        return localX >= INTERIOR_MIN && localX <= INTERIOR_MAX
                && localZ >= INTERIOR_MIN && localZ <= INTERIOR_MAX
                && localY >= ROOM_FLOOR_LOCAL_Y && localY < ROOM_CEILING_LOCAL_Y;
    }

    /** The source generator stops at Y 254, leaving its final world layer untouched air. */
    public static boolean isGeneratedAir(int localX, int y, int localZ) {
        return y == WORLD_HEIGHT - 1 || isInterior(localX, y, localZ);
    }

    private static HorizontalCoordinates horizontalForOrdinal(int targetOrdinal) {
        int cellX = 0;
        int cellZ = 0;
        int sign = 1;
        int ordinal = -1;
        Set<Long> seen = new HashSet<>();

        for (int ring = 0; ring < SOURCE_SPIRAL_LIMIT; ring++) {
            for (int spin = 0; spin <= ring; spin++) {
                for (int axis = 0; axis < 2; axis++) {
                    if (ring > 0) {
                        if (axis == 0) cellZ += sign;
                        else cellX += sign;
                    }
                    if (seen.add(pack(cellX, cellZ)) && ++ordinal == targetOrdinal) {
                        return new HorizontalCoordinates(cellX, cellZ);
                    }
                }
            }
            sign *= -1;
        }
        throw new IllegalArgumentException("Mirror room index exceeds the source spiral capacity");
    }

    private static int ordinalForHorizontal(int targetX, int targetZ) {
        int cellX = 0;
        int cellZ = 0;
        int sign = 1;
        int ordinal = -1;
        Set<Long> seen = new HashSet<>();

        for (int ring = 0; ring < SOURCE_SPIRAL_LIMIT; ring++) {
            for (int spin = 0; spin <= ring; spin++) {
                for (int axis = 0; axis < 2; axis++) {
                    if (ring > 0) {
                        if (axis == 0) cellZ += sign;
                        else cellX += sign;
                    }
                    if (seen.add(pack(cellX, cellZ))) {
                        ordinal++;
                        if (cellX == targetX && cellZ == targetZ) {
                            return ordinal;
                        }
                    }
                }
            }
            sign *= -1;
        }
        return -1;
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    public record RoomCoordinates(int chunkX, int sectionY, int chunkZ) {}

    private record HorizontalCoordinates(int chunkX, int chunkZ) {}
}
