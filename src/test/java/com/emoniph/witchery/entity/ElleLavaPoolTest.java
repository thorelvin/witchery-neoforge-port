package com.emoniph.witchery.entity;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElleLavaPoolTest {
    @Test
    void sourceShapedLavaDiskCountsAsPool() {
        TestReader reader = new TestReader();
        BlockPos center = new BlockPos(0, 64, 0);
        reader.addPool(center);

        assertTrue(ElleLavaPool.isPool(center, reader));
    }

    @Test
    void blockedAirAboveColumnRejectsPool() {
        TestReader reader = new TestReader();
        BlockPos center = new BlockPos(0, 64, 0);
        reader.addPool(center);
        reader.blockAir(center.above());

        assertFalse(ElleLavaPool.isPool(center, reader));
    }

    @Test
    void missingLavaInsideDiskRejectsPool() {
        TestReader reader = new TestReader();
        BlockPos center = new BlockPos(0, 64, 0);
        reader.addPool(center);
        reader.removeLava(center.offset(ElleLavaPool.POOL_RADIUS, 0, 0));

        assertFalse(ElleLavaPool.isPool(center, reader));
    }

    @Test
    void unloadedCellInsideDiskRejectsPool() {
        TestReader reader = new TestReader();
        BlockPos center = new BlockPos(0, 64, 0);
        reader.addPool(center);
        reader.unload(center.offset(ElleLavaPool.POOL_RADIUS, 0, 0));

        assertFalse(ElleLavaPool.isPool(center, reader));
    }

    @Test
    void searchFindsNearestValidPool() {
        TestReader reader = new TestReader();
        BlockPos nearest = new BlockPos(8, 64, 0);
        BlockPos farther = new BlockPos(-12, 64, 0);
        reader.addPool(nearest);
        reader.addPool(farther);

        assertEquals(nearest, ElleLavaPool.findNearest(new BlockPos(0, 64, 0), reader));
    }

    private static final class TestReader implements ElleLavaPool.Reader {
        private final Set<BlockPos> lava = new HashSet<>();
        private final Set<BlockPos> blockedAir = new HashSet<>();
        private final Set<BlockPos> unloaded = new HashSet<>();

        void addPool(BlockPos center) {
            for (int dx = -ElleLavaPool.POOL_RADIUS; dx <= ElleLavaPool.POOL_RADIUS; dx++) {
                for (int dz = -ElleLavaPool.POOL_RADIUS; dz <= ElleLavaPool.POOL_RADIUS; dz++) {
                    if (dx * dx + dz * dz <= ElleLavaPool.POOL_RADIUS * ElleLavaPool.POOL_RADIUS) {
                        lava.add(center.offset(dx, 0, dz));
                    }
                }
            }
        }

        void removeLava(BlockPos pos) {
            lava.remove(pos);
        }

        void blockAir(BlockPos pos) {
            blockedAir.add(pos);
        }

        void unload(BlockPos pos) {
            unloaded.add(pos);
        }

        @Override
        public boolean isLoaded(BlockPos pos) {
            return !unloaded.contains(pos);
        }

        @Override
        public boolean isLava(BlockPos pos) {
            return lava.contains(pos);
        }

        @Override
        public boolean isAir(BlockPos pos) {
            return !blockedAir.contains(pos) && !lava.contains(pos);
        }
    }
}
