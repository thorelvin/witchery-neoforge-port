package com.emoniph.witchery.entity;

/** Source constants and deterministic policy for Hobgoblin village ecology. */
final class HobgoblinVillageEcology {
    static final int RAID_PACK_SIZE = 3;
    static final int PACK_RADIUS = 8;
    static final int VILLAGE_SEARCH_RADIUS = 32;
    static final int VILLAGE_REFRESH_MIN_TICKS = 70;
    static final int VILLAGE_REFRESH_RANDOM_TICKS = 50;
    static final int MATING_ROLL_BOUND = 500;
    static final int MATING_DURATION_TICKS = 300;
    static final int PARENT_COOLDOWN_TICKS = 6000;
    static final int BABY_AGE_TICKS = -24000;

    private HobgoblinVillageEcology() {
    }

    static boolean hasRaidPack(int nearbyHobgoblins) {
        return nearbyHobgoblins >= RAID_PACK_SIZE;
    }

    static int breedingCapacity(long homePoiCount) {
        if (homePoiCount <= 0L) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, homePoiCount * 35L / 100L);
    }

    static boolean hasBreedingCapacity(long homePoiCount, int nearbyHobgoblins) {
        return nearbyHobgoblins < breedingCapacity(homePoiCount);
    }

    static int villageRefreshCooldown(int randomOffset) {
        return VILLAGE_REFRESH_MIN_TICKS
                + Math.clamp(randomOffset, 0, VILLAGE_REFRESH_RANDOM_TICKS - 1);
    }

    static int villageRestrictionRadius(int villageRadius) {
        return Math.max(1, (int) (Math.max(1, villageRadius) * 0.6D));
    }
}
