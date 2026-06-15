package com.emoniph.witchery.worldgen;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VillageWallGeneratorTest {
    private static final byte EMPTY = 0;
    private static final byte INTERIOR = 1;
    private static final byte WALL = 2;

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

        assertCorner(base, -1, -1, Direction.WEST, StairsShape.OUTER_LEFT);
        assertCorner(base, -1, 1, Direction.WEST, StairsShape.OUTER_RIGHT);
        assertCorner(base, 1, -1, Direction.EAST, StairsShape.OUTER_RIGHT);
        assertCorner(base, 1, 1, Direction.EAST, StairsShape.OUTER_LEFT);
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
