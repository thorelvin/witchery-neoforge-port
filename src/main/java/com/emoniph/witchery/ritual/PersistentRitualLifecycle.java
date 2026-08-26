package com.emoniph.witchery.ritual;

import java.util.Map;
import java.util.Set;

/**
 * Source-backed timing contract for every registered rite that can outlive its activation tick.
 * The first boundary is separate from the queue pulse because several source steps wait for a
 * 20-tick activation boundary and then update every tick or every five ticks.
 */
final class PersistentRitualLifecycle {
    private static final Map<ActiveRitualType, Spec> SPECS = Map.ofEntries(
            entry(ActiveRitualType.WEATHER_CALL_STORM, "RiteWeatherCallStorm", 30, 30, 30),
            entry(ActiveRitualType.FERTILITY, "RiteFertility", 20, 5, 5),
            entry(ActiveRitualType.FORESTATION, "RiteForestation", 20, 20, 20),
            entry(ActiveRitualType.ICE_SHELL, "RiteSphereEffect", 20, 5, 5),
            entry(ActiveRitualType.RAIN_OF_TOADS, "RiteRainOfToads", 30, 30, 30),
            entry(ActiveRitualType.BLINDNESS, "RiteBlindness", 20, 5, 5),
            entry(ActiveRitualType.BLIGHT, "RiteBlight", 20, 5, 5),
            entry(ActiveRitualType.HELL_ON_EARTH, "RiteHellOnEarth", 20, 5, 5),
            entry(ActiveRitualType.CALL_CREATURES, "RiteCallCreatures", 60, 60, 60),
            entry(ActiveRitualType.CLIMATE_CHANGE, "RiteClimateChange", 20, 20, 20),
            entry(ActiveRitualType.NATURES_POWER, "RiteNaturesPower", 20, 20, 20),
            entry(ActiveRitualType.TRANSPOSE_ORES, "RiteTransposeOres", 10, 10, 10),
            entry(ActiveRitualType.MOVING_EARTH, "RitePartEarth", 20, 1, 1),
            entry(ActiveRitualType.RAISE_COLUMN, "RiteRaiseColumn", 5, 5, 5),
            entry(ActiveRitualType.VOLCANO, "RiteRaiseVolcano", 15, 15, 15),
            entry(ActiveRitualType.PROTECTION_BARRIER_MONSTERS, "RiteProtectionCircleBarrier", 20, 1, 20),
            entry(ActiveRitualType.PROTECTION_BARRIER_PORTABLE, "RiteProtectionCircleBarrier", 20, 1, 20),
            entry(ActiveRitualType.PROTECTION_BARRIER, "RiteProtectionCircleBarrier", 20, 1, 20),
            entry(ActiveRitualType.INFUSION_RECHARGE, "RiteInfusionRecharge", 20, 20, 20),
            entry(ActiveRitualType.SANCTITY_CIRCLE, "RiteProtectionCircleRepulsive", 20, 1, 1),
            entry(ActiveRitualType.IMPRISONMENT_CIRCLE, "RiteProtectionCircleAttractive", 20, 1, 1)
    );

    /** The 33 source registry rows owned by the lifecycle table above. */
    static final Set<Integer> SOURCE_REGISTRATION_IDS = Set.of(
            4, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 19, 20,
            35, 36, 37, 38, 39, 43, 46, 47, 64, 65, 66, 68,
            70, 71, 72, 73, 74, 75, 76, 77
    );

    private PersistentRitualLifecycle() {
    }

    private static Map.Entry<ActiveRitualType, Spec> entry(ActiveRitualType type, String sourceEffect,
                                                            int firstBoundary, int pulseInterval,
                                                            int worldEffectInterval) {
        return Map.entry(type, new Spec(sourceEffect, firstBoundary, pulseInterval, worldEffectInterval));
    }

    static Spec spec(ActiveRitualType type) {
        Spec spec = SPECS.get(type);
        if (spec == null) {
            throw new IllegalArgumentException("No persistent ritual lifecycle for " + type);
        }
        return spec;
    }

    static Set<ActiveRitualType> types() {
        return SPECS.keySet();
    }

    static void requirePulseInterval(ActiveRitualType type, int pulseInterval) {
        int expected = spec(type).pulseInterval();
        if (pulseInterval != expected) {
            throw new IllegalArgumentException(type + " must use source pulse " + expected
                    + " ticks, got " + pulseInterval);
        }
    }

    static long nextActivationTime(long gameTime, ActiveRitualType type) {
        int interval = spec(type).firstBoundary();
        return gameTime + interval - Math.floorMod(gameTime, interval);
    }

    static boolean shouldApplyWorldEffect(ActiveRitualType type, long gameTime) {
        return Math.floorMod(gameTime, spec(type).worldEffectInterval()) == 0;
    }

    record Spec(String sourceEffect, int firstBoundary, int pulseInterval, int worldEffectInterval) {
    }
}
