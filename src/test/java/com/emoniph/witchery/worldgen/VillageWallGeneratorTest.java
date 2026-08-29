package com.emoniph.witchery.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    void straightSourceWallHasThreeWideCurtainAndFiveWideCrown() {
        byte[][] grid = emptyGrid(5, 5);
        grid[1][2] = WALL;
        grid[2][2] = WALL;
        grid[3][2] = WALL;

        List<VillageWallGenerator.SourceWallBlock> blocks =
                VillageWallGenerator.sourceWallTemplate(grid, 2, 2, 5, 5);

        assertEquals(76, blocks.size());
        assertEquals(76, uniqueOffsets(blocks).size());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy < 8; dy++) {
                    assertPart(blocks, dx, dy, dz, VillageWallGenerator.SourceWallPart.MASONRY);
                }
            }
        }
        assertPart(blocks, 0, 8, -2, VillageWallGenerator.SourceWallPart.MASONRY);
        assertPart(blocks, 0, 8, 2, VillageWallGenerator.SourceWallPart.MASONRY);
        assertStair(blocks, 0, 9, -2, Direction.EAST, Half.BOTTOM);
        assertStair(blocks, 0, 9, 2, Direction.EAST, Half.BOTTOM);
        assertFalse(hasBlock(blocks, 0, 8, 0), "the patrol deck must be open at standing height");
    }

    @Test
    void sourceWallCornerUsesTwoCourseMasonryL() {
        byte[][] grid = emptyGrid(5, 5);
        grid[2][2] = WALL;
        grid[1][2] = WALL;
        grid[2][3] = WALL;

        List<VillageWallGenerator.SourceWallBlock> blocks =
                VillageWallGenerator.sourceWallTemplate(grid, 2, 2, 5, 5);

        for (int y : new int[]{8, 9}) {
            assertPart(blocks, 2, y, -2, VillageWallGenerator.SourceWallPart.MASONRY);
            assertPart(blocks, 2, y, -1, VillageWallGenerator.SourceWallPart.MASONRY);
            assertPart(blocks, 1, y, -2, VillageWallGenerator.SourceWallPart.MASONRY);
        }
    }

    @Test
    void sourceGateIsThreeWideThreeDeepWithLayeredArchAndWalk() {
        List<VillageWallGenerator.SourceWallBlock> blocks =
                VillageWallGenerator.sourceGateTemplate(1, 0, 0, 1);

        assertEquals(93, blocks.size());
        assertEquals(93, uniqueOffsets(blocks).size());
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y <= 4; y++) {
                    assertFalse(hasBlock(blocks, x, y, z), "the gate tunnel must remain clear");
                }
            }
        }
        for (int x : new int[]{-2, 2}) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y <= 5; y++) {
                    assertPart(blocks, x, y, z, VillageWallGenerator.SourceWallPart.MASONRY);
                }
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -1; z <= 1; z++) {
                assertPart(blocks, x, 6, z, VillageWallGenerator.SourceWallPart.MASONRY);
                assertPart(blocks, x, 7, z, VillageWallGenerator.SourceWallPart.MASONRY);
            }
        }
        for (int x = -1; x <= 1; x++) {
            assertPart(blocks, x, 5, 0, VillageWallGenerator.SourceWallPart.ARCH_WALL);
        }
        for (int z : new int[]{-1, 1}) {
            assertStair(blocks, -1, 5, z, Direction.WEST, Half.TOP);
            assertStair(blocks, 1, 5, z, Direction.EAST, Half.TOP);
        }
        for (int x = -2; x <= 2; x++) {
            for (int z : new int[]{-2, 2}) {
                assertPart(blocks, x, 8, z, VillageWallGenerator.SourceWallPart.MASONRY);
                assertStair(blocks, x, 9, z, Direction.EAST, Half.BOTTOM);
            }
        }
    }

    @Test
    void sourceGateTemplateRotatesWithoutChangingItsBlockCount() {
        List<VillageWallGenerator.SourceWallBlock> blocks =
                VillageWallGenerator.sourceGateTemplate(0, 1, -1, 0);

        assertEquals(93, blocks.size());
        assertEquals(93, uniqueOffsets(blocks).size());
        for (int z = -1; z <= 1; z++) {
            assertPart(blocks, 0, 5, z, VillageWallGenerator.SourceWallPart.ARCH_WALL);
        }
        for (int x : new int[]{-1, 1}) {
            assertStair(blocks, x, 5, -1, Direction.NORTH, Half.TOP);
            assertStair(blocks, x, 5, 1, Direction.SOUTH, Half.TOP);
        }
    }

    @Test
    void sourceWallMaterialsRemainBiomeAdaptive() {
        assertPalette(VillageBuildingMaterials.VillageType.PLAINS,
                Blocks.STONE_BRICKS.defaultBlockState(), Blocks.COBBLESTONE_WALL.defaultBlockState(),
                Blocks.STONE_BRICK_STAIRS.defaultBlockState());
        assertPalette(VillageBuildingMaterials.VillageType.DESERT,
                Blocks.SMOOTH_SANDSTONE.defaultBlockState(), Blocks.SANDSTONE_WALL.defaultBlockState(),
                Blocks.SANDSTONE_STAIRS.defaultBlockState());
        assertPalette(VillageBuildingMaterials.VillageType.SAVANNA,
                Blocks.ORANGE_TERRACOTTA.defaultBlockState(), Blocks.RED_SANDSTONE_WALL.defaultBlockState(),
                Blocks.RED_SANDSTONE_STAIRS.defaultBlockState());
        assertPalette(VillageBuildingMaterials.VillageType.TAIGA,
                Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState(),
                Blocks.STONE_BRICK_STAIRS.defaultBlockState());
        assertPalette(VillageBuildingMaterials.VillageType.SNOWY,
                Blocks.STONE_BRICKS.defaultBlockState(), Blocks.COBBLESTONE_WALL.defaultBlockState(),
                Blocks.STONE_BRICK_STAIRS.defaultBlockState());
    }

    @Test
    void wallRelocationRejectsDistantRoofsButAllowsNearbyTerrainSteps() {
        assertTrue(VillageWallGenerator.isRelocationHeightAllowed(72, 68));
        assertTrue(VillageWallGenerator.isRelocationHeightAllowed(72, 76));
        assertFalse(VillageWallGenerator.isRelocationHeightAllowed(72, 67));
        assertFalse(VillageWallGenerator.isRelocationHeightAllowed(72, 77));
        assertFalse(VillageWallGenerator.isRelocationHeightAllowed(72, 88));
    }

    @Test
    void inwardGateGradeReachesTheDetectedVillageRoadBeyondTheOldCap() {
        assertEquals(31, VillageWallGenerator.gateRoadDepth(50, 80, 50, 49));
        assertEquals(29, VillageWallGenerator.inwardGateGradeLength(31));
    }

    @Test
    void inwardGateGradeNeedsNoExtraCellsWhenRoadMeetsTheFlatApron() {
        assertEquals(0, VillageWallGenerator.inwardGateGradeLength(0));
        assertEquals(0, VillageWallGenerator.inwardGateGradeLength(1));
        assertEquals(0, VillageWallGenerator.inwardGateGradeLength(2));
        assertEquals(1, VillageWallGenerator.inwardGateGradeLength(3));
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

    private static Set<BlockPos> uniqueOffsets(
            List<VillageWallGenerator.SourceWallBlock> blocks) {
        Set<BlockPos> offsets = new HashSet<>();
        for (VillageWallGenerator.SourceWallBlock block : blocks) offsets.add(block.offset());
        return offsets;
    }

    private static boolean hasBlock(List<VillageWallGenerator.SourceWallBlock> blocks,
                                    int x, int y, int z) {
        return blocks.stream().anyMatch(block -> block.offset().equals(new BlockPos(x, y, z)));
    }

    private static void assertPart(List<VillageWallGenerator.SourceWallBlock> blocks,
                                   int x, int y, int z, VillageWallGenerator.SourceWallPart part) {
        assertTrue(blocks.stream().anyMatch(block -> block.offset().equals(new BlockPos(x, y, z))
                        && block.part() == part),
                () -> "missing " + part + " at " + x + "," + y + "," + z);
    }

    private static void assertStair(List<VillageWallGenerator.SourceWallBlock> blocks,
                                    int x, int y, int z, Direction facing, Half half) {
        assertTrue(blocks.stream().anyMatch(block -> block.offset().equals(new BlockPos(x, y, z))
                        && block.part() == VillageWallGenerator.SourceWallPart.STAIR
                        && block.facing() == facing && block.half() == half),
                () -> "missing " + half + " stair facing " + facing + " at " + x + "," + y + "," + z);
    }

    private static void assertPalette(VillageBuildingMaterials.VillageType type,
                                      BlockState masonry, BlockState arch, BlockState stair) {
        assertEquals(masonry, VillageWallGenerator.wallMaterial(type));
        assertEquals(arch, VillageWallGenerator.archMaterial(type));
        assertEquals(stair, VillageWallGenerator.stairMaterial(type));
    }
}
