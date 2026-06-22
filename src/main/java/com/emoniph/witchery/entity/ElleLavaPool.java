package com.emoniph.witchery.entity;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

final class ElleLavaPool {
    static final int SEARCH_RADIUS_XZ = 15;
    static final int SEARCH_RADIUS_Y = 3;
    static final int POOL_RADIUS = 6;

    private ElleLavaPool() {
    }

    @Nullable
    static BlockPos findNearest(BlockPos origin, Reader reader) {
        for (int radius = 0; radius <= SEARCH_RADIUS_XZ; radius++) {
            for (int dy = -SEARCH_RADIUS_Y; dy <= SEARCH_RADIUS_Y; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                            continue;
                        }
                        BlockPos candidate = origin.offset(dx, dy, dz);
                        if (reader.isLoaded(candidate) && reader.isLava(candidate) && isPool(candidate, reader)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    static boolean isPool(BlockPos center, Reader reader) {
        if (!isColumn(center, reader)
                || !isColumn(center.north(), reader)
                || !isColumn(center.south(), reader)
                || !isColumn(center.east(), reader)
                || !isColumn(center.west(), reader)) {
            return false;
        }
        for (int dx = -POOL_RADIUS; dx <= POOL_RADIUS; dx++) {
            for (int dz = -POOL_RADIUS; dz <= POOL_RADIUS; dz++) {
                if (dx * dx + dz * dz > POOL_RADIUS * POOL_RADIUS) {
                    continue;
                }
                BlockPos pos = center.offset(dx, 0, dz);
                if (!reader.isLoaded(pos) || !reader.isLava(pos) || !reader.isAir(pos.above())) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean isColumn(BlockPos pos, Reader reader) {
        return reader.isLoaded(pos)
                && reader.isLoaded(pos.above(2))
                && reader.isLava(pos)
                && reader.isAir(pos.above())
                && reader.isAir(pos.above(2));
    }

    interface Reader {
        boolean isLoaded(BlockPos pos);

        boolean isLava(BlockPos pos);

        boolean isAir(BlockPos pos);
    }
}
