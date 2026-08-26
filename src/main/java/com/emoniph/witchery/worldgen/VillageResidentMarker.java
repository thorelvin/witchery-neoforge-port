package com.emoniph.witchery.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Durable provenance for residents created by Witchery village pieces. */
public final class VillageResidentMarker {
    private static final String BUILDING_TAG = "witchery_village_building";
    private static final String ORIGIN_TAG = "witchery_village_building_origin";

    private VillageResidentMarker() {
    }

    public static void mark(LivingEntity resident, String building, BoundingBox buildingBox) {
        resident.getPersistentData().putString(BUILDING_TAG, building);
        resident.getPersistentData().putLong(ORIGIN_TAG,
                new BlockPos(buildingBox.minX(), buildingBox.minY(), buildingBox.minZ()).asLong());
    }

    public static boolean matches(LivingEntity resident, String building, BlockPos origin) {
        return building.equals(resident.getPersistentData().getStringOr(BUILDING_TAG, ""))
                && resident.getPersistentData().getLongOr(ORIGIN_TAG, Long.MIN_VALUE) == origin.asLong();
    }
}
