package com.emoniph.witchery.ritual;

import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Mirrors old Witchery's PowerSources lookup for altar power users:
 * a power source is available when the target point is inside the altar's own range.
 */
public final class AltarPowerSourceLookup {
    public static final int BASE_ALTAR_RANGE = 16;
    public static final int MAX_ALTAR_RANGE = 32;

    private AltarPowerSourceLookup() {
    }

    public static AltarBlockEntity findClosest(Level level, BlockPos center) {
        AltarBlockEntity closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (BlockPos checkPos : BlockPos.betweenClosed(
                center.offset(-MAX_ALTAR_RANGE, -MAX_ALTAR_RANGE, -MAX_ALTAR_RANGE),
                center.offset(MAX_ALTAR_RANGE, MAX_ALTAR_RANGE, MAX_ALTAR_RANGE))) {
            if (!(level.getBlockEntity(checkPos) instanceof AltarBlockEntity altar) || !altar.hasValidStructure()) {
                continue;
            }

            double distance = distanceSqr(center, altar.getBlockPos());
            int range = altar.getRange();
            if (distance <= (double) range * range && distance < closestDistance) {
                closest = altar;
                closestDistance = distance;
            }
        }

        return closest;
    }

    public static double distanceSqr(BlockPos from, BlockPos to) {
        int dx = from.getX() - to.getX();
        int dy = from.getY() - to.getY();
        int dz = from.getZ() - to.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
