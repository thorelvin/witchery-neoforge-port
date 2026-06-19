package com.emoniph.witchery.worldgen;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillageWallGeneratorTest {
    private static final byte EMPTY = 0;
    private static final byte INTERIOR = 1;
    private static final byte WALL = 2;

    @Test
    void wallSpawnRollIsDeterministicPerVillage() {
        long seed = 0xC0FFEEL;
        for (int minX = -2000; minX <= 2000; minX += 137) {
            for (int minZ = -2000; minZ <= 2000; minZ += 211) {
                boolean first = VillageWallGenerator.shouldGenerateWalls(seed, minX, minZ, 40);
                boolean again = VillageWallGenerator.shouldGenerateWalls(seed, minX, minZ, 40);
                assertEquals(first, again, "roll must be stable for the same village");
            }
        }
    }

    @Test
    void wallSpawnRollHonoursBounds() {
        assertFalse(VillageWallGenerator.shouldGenerateWalls(1L, 16, 32, 0), "0% never spawns");
        assertTrue(VillageWallGenerator.shouldGenerateWalls(1L, 16, 32, 100), "100% always spawns");
    }

    @Test
    void wallSpawnRollApproximatesRequestedRate() {
        long seed = 1234567L;
        int total = 0;
        int walled = 0;
        // Distinct villages on a coarse grid (spacing avoids adjacent-coordinate correlation).
        for (int minX = -8000; minX <= 8000; minX += 80) {
            for (int minZ = -8000; minZ <= 8000; minZ += 80) {
                total++;
                if (VillageWallGenerator.shouldGenerateWalls(seed, minX, minZ, 40)) {
                    walled++;
                }
            }
        }
        double rate = (double) walled / total;
        assertTrue(rate > 0.35 && rate < 0.45,
                "expected ~40% walled, got " + rate + " over " + total + " villages");
    }

    @Test
    void squaresOuterCornerOnEmptySide() {
        byte[][] grid = emptyGrid(4, 4);
        grid[1][2] = WALL;
        grid[2][1] = WALL;
        grid[2][2] = INTERIOR;

        VillageWallGenerator.squareWallCornerGaps(grid, 4, 4);

        assertEquals(WALL, grid[1][1]);
        assertEquals(INTERIOR, grid[2][2]);
    }

    @Test
    void squaresInnerCornerNotchOnEmptySide() {
        byte[][] grid = emptyGrid(4, 4);
        grid[1][2] = WALL;
        grid[2][1] = WALL;
        grid[1][1] = INTERIOR;

        VillageWallGenerator.squareWallCornerGaps(grid, 4, 4);

        assertEquals(WALL, grid[2][2]);
        assertEquals(INTERIOR, grid[1][1]);
    }

    @Test
    void cornerRampartStairsUseOuterCornerShapes() {
        BlockState base = Blocks.STONE_BRICK_STAIRS.defaultBlockState();

        assertCorner(base, -1, -1, Direction.EAST, StairsShape.OUTER_LEFT);
        assertCorner(base, -1, 1, Direction.EAST, StairsShape.OUTER_RIGHT);
        assertCorner(base, 1, -1, Direction.WEST, StairsShape.OUTER_RIGHT);
        assertCorner(base, 1, 1, Direction.WEST, StairsShape.OUTER_LEFT);
    }

    private static byte[][] emptyGrid(int w, int d) {
        byte[][] grid = new byte[w][d];
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                grid[x][z] = EMPTY;
            }
        }
        return grid;
    }

    private static void assertCorner(BlockState base, int dx, int dz,
                                     Direction facing, StairsShape shape) {
        BlockState state = VillageWallGenerator.cornerStairFor(base, dx, dz);
        assertEquals(facing, state.getValue(StairBlock.FACING));
        assertEquals(Half.BOTTOM, state.getValue(StairBlock.HALF));
        assertEquals(shape, state.getValue(StairBlock.SHAPE));
    }
}
