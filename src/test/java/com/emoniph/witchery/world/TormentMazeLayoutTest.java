package com.emoniph.witchery.world;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TormentMazeLayoutTest {
    @Test
    void everyCertifiedSeedBuildsSixPerfectNavigableMazes() {
        for (long seed = -32; seed < 32; seed++) {
            TormentMazeLayout layout = TormentMazeLayout.forSeed(seed);
            for (int level = 0; level < TormentHelper.NUM_LEVELS; level++) {
                int mazeLevel = level;
                TormentMazeLayout.MazeLevel maze = layout.level(level);
                assertEquals(TormentHelper.MAZE_SIZE * TormentHelper.MAZE_SIZE - 1,
                        maze.cellEdgeCount(), "seed=" + seed + " level=" + level);
                assertTrue(maze.allCellsConnected(), "seed=" + seed + " level=" + level);
                assertTrue(maze.hasEntryToPortalRoute(), "seed=" + seed + " level=" + level);
                assertEquals(3, maze.chestPositions().size(), "seed=" + seed + " level=" + level);
                assertTrue(maze.chestPositions().stream().allMatch(pos ->
                                pos.getY() == TormentHelper.getMazeY(mazeLevel)
                                        && maze.kindAt(pos.getX(), pos.getZ())
                                        == TormentMazeLayout.PlanarKind.PASSAGE),
                        "seed=" + seed + " level=" + level);
            }
        }
    }

    @Test
    void fingerprintsAreStableAndSeparateLevels() {
        TormentMazeLayout first = TormentMazeLayout.forSeed(8675309L);
        TormentMazeLayout second = TormentMazeLayout.forSeed(8675309L);
        Set<String> fingerprints = new HashSet<>();
        for (int level = 0; level < TormentHelper.NUM_LEVELS; level++) {
            assertEquals(first.level(level).fingerprint(), second.level(level).fingerprint());
            fingerprints.add(first.level(level).fingerprint());
        }
        assertEquals(TormentHelper.NUM_LEVELS, fingerprints.size());
        assertNotEquals(first.level(0).fingerprint(), TormentMazeLayout.forSeed(8675310L).level(0).fingerprint());
    }

    @Test
    void sourceCoordinatesUseSeparateEntryAndPassageHeights() {
        for (int level = 0; level < TormentHelper.NUM_LEVELS; level++) {
            assertEquals(TormentHelper.getMazeY(level) + 2, TormentHelper.getEntryPos(level).getY());
            assertEquals(TormentHelper.getMazeY(level) + 1, TormentHelper.getBossPos(level).getY());
            assertEquals(TormentHelper.getMazeY(level) + 1, TormentHelper.getPortalLowerPos(level).getY());
        }
    }
}
