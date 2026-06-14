package com.emoniph.witchery.util;

/**
 * Port of {@code GenericEvents.villagerIsInCage}/{@code isCaged}: detects the iron-bar holding cell a
 * level-8 vampire must imprison a villager in for the level 8→9 quest. Kept world-agnostic behind
 * {@link BlockQuery} so the geometry is unit-testable.
 *
 * <p>A cell counts as a cage when, around a column, at least <b>15 of the 16</b> ring positions across
 * the two body layers (y and y+1) are iron bars <em>and</em> all 9 positions of the roof layer (y+2)
 * are non-replaceable (a solid lid). The villager may sit anywhere in a 3×3 footprint, so the test is
 * run at the villager's column and its 8 neighbours.
 */
public final class VampireCage {

    /** The 8 surrounding offsets of a 3×3 ring (centre excluded). */
    private static final int[][] RING_8 = {
            {1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
    };
    /** The full 3×3 (centre included). */
    private static final int[][] CELL_9 = {
            {0, 0}, {1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
    };

    private VampireCage() {}

    /** Supplies block facts at absolute coordinates. */
    public interface BlockQuery {
        boolean isIronBars(int x, int y, int z);
        /** A non-replaceable block (a solid roof tile in source terms). */
        boolean isSolidRoof(int x, int y, int z);
    }

    /** True if the villager column (or any of its 8 neighbours) sits in a cage. */
    public static boolean isCagedAround(int x, int y, int z, BlockQuery q) {
        for (int[] d : CELL_9) {
            if (isCaged(x + d[0], y, z + d[1], q)) {
                return true;
            }
        }
        return false;
    }

    /** True if the single column at (x,y,z) is a sealed iron-bar cage. */
    public static boolean isCaged(int x, int y, int z, BlockQuery q) {
        int bars = countBars(x, y, z, q) + countBars(x, y + 1, z, q);
        if (bars < 15) {
            return false;
        }
        int roof = 0;
        int ry = y + 2;
        for (int[] d : CELL_9) {
            if (q.isSolidRoof(x + d[0], ry, z + d[1])) {
                roof++;
            }
        }
        return roof >= 9;
    }

    private static int countBars(int x, int y, int z, BlockQuery q) {
        int count = 0;
        for (int[] d : RING_8) {
            if (q.isIronBars(x + d[0], y, z + d[1])) {
                count++;
            }
        }
        return count;
    }
}
