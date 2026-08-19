package com.emoniph.witchery.item;

import com.emoniph.witchery.data.WitcheryPlayerData.WolfForm;

/** Source-backed lycanthropy level gates, quest targets, moon timing, and form statistics. */
public final class WerewolfProgression {
    public static final int MAX_LEVEL = 10;
    public static final int CONTROL_LEVEL = 2;
    public static final int WOLFMAN_LEVEL = 5;
    public static final long HOWL_COOLDOWN_MILLIS = 60_000L;
    public static final long BONE_FIND_COOLDOWN_MILLIS = 60_000L;

    private static final int[] QUEST_TARGETS = {0, 0, 0, 0, 0, 10, 16, 6, 30, 1, 0};

    private static final StatBoost[] WOLF = {
            boost(0.0F, 0.0, 0.0, 0, 0.0F, 0.0F, 0, 4.0F),
            boost(0.5F, 0.2, 0.2, 0, 1.0F, 0.0F, 2, 4.0F),
            boost(0.5F, 0.2, 0.2, 0, 1.0F, 0.0F, 2, 3.0F),
            boost(0.75F, 0.2, 0.3, 0, 2.0F, 0.0F, 2, 3.0F),
            boost(0.75F, 0.2, 0.4, 0, 2.0F, 0.0F, 3, 3.0F),
            boost(0.75F, 0.2, 0.5, 0, 2.0F, 0.0F, 3, 2.0F),
            boost(1.0F, 0.2, 0.6, 0, 2.0F, 1.0F, 3, 2.0F),
            boost(1.25F, 0.3, 0.7, 4, 2.0F, 1.0F, 4, 2.0F),
            boost(1.5F, 0.3, 0.8, 8, 3.0F, 2.0F, 4, 2.0F),
            boost(1.75F, 0.3, 0.9, 12, 3.0F, 3.0F, 5, 2.0F),
            boost(1.75F, 0.3, 1.0, 12, 3.0F, 3.0F, 5, 2.0F)
    };

    private static final StatBoost[] WOLFMAN = {
            boost(0.0F, 0.0, 0.0, 0, 0.0F, 0.0F, 0, 4.0F),
            boost(0.0F, 0.0, 0.0, 0, 0.0F, 0.0F, 0, 4.0F),
            boost(0.0F, 0.0, 0.0, 0, 0.0F, 0.0F, 0, 3.0F),
            boost(0.0F, 0.0, 0.0, 0, 0.0F, 0.0F, 0, 3.0F),
            boost(0.0F, 0.0, 0.0, 0, 0.0F, 0.0F, 0, 3.0F),
            boost(0.2F, 0.2, 0.2, 20, 4.0F, 3.0F, 3, 2.0F),
            boost(0.2F, 0.3, 0.2, 20, 4.0F, 3.0F, 4, 2.0F),
            boost(0.4F, 0.4, 0.4, 20, 5.0F, 4.0F, 5, 2.0F),
            boost(0.4F, 0.5, 0.4, 30, 6.0F, 4.0F, 6, 2.0F),
            boost(0.5F, 0.6, 0.6, 40, 7.0F, 5.0F, 7, 2.0F),
            boost(0.5F, 0.6, 0.6, 40, 7.0F, 5.0F, 7, 2.0F)
    };

    private WerewolfProgression() {
    }

    public static int questTarget(int level) {
        return level >= 0 && level < QUEST_TARGETS.length ? QUEST_TARGETS[level] : 0;
    }

    public static StatBoost stats(WolfForm form, int level) {
        int clamped = Math.clamp(level, 0, MAX_LEVEL);
        return switch (form) {
            case WOLF -> WOLF[clamped];
            case WOLFMAN -> WOLFMAN[clamped];
            case NONE -> StatBoost.NONE;
        };
    }

    public static WolfForm moonCharmNextForm(WolfForm current, boolean sneaking, int level) {
        return switch (current) {
            case NONE -> sneaking && level >= WOLFMAN_LEVEL ? WolfForm.WOLFMAN : WolfForm.WOLF;
            case WOLF -> sneaking && level >= WOLFMAN_LEVEL ? WolfForm.WOLFMAN : WolfForm.NONE;
            case WOLFMAN -> sneaking ? WolfForm.NONE : WolfForm.WOLF;
        };
    }

    public static boolean isFullMoon(long dayTime) {
        return Math.floorDiv(dayTime, 24000L) % 8L == 0L;
    }

    public static boolean isFullMoonNight(long dayTime) {
        long timeOfDay = Math.floorMod(dayTime, 24000L);
        return isFullMoon(dayTime) && timeOfDay > 13000L && timeOfDay < 23000L;
    }

    /** Source amplifier mapping: Wolfsbane I/II/III/IV suppresses mastery 1/3/6/9. */
    public static int wolfsbaneStrength(int amplifier) {
        return 1 + Math.max(0, amplifier * 3 - 1);
    }

    public static boolean canPowerHowl(long nowMillis, long lastHowlMillis, boolean creative) {
        return creative || lastHowlMillis == Long.MIN_VALUE
                || lastHowlMillis + HOWL_COOLDOWN_MILLIS < nowMillis;
    }

    public static int remainingHowlCooldownTicks(long nowMillis, long lastHowlMillis) {
        if (lastHowlMillis == Long.MIN_VALUE) return 0;
        long remainingMillis = Math.clamp(
                lastHowlMillis + HOWL_COOLDOWN_MILLIS - nowMillis,
                0L, HOWL_COOLDOWN_MILLIS);
        return (int) ((remainingMillis + 49L) / 50L);
    }

    private static StatBoost boost(float speed, double jump, double leap, int health,
                                   float damage, float resistance, int fall, float damageCap) {
        return new StatBoost(speed, jump, leap, health, damage, resistance, fall, damageCap);
    }

    public record StatBoost(float speed, double jump, double leap, int health,
                            float damage, float resistance, int fall, float damageCap) {
        private static final StatBoost NONE = new StatBoost(0.0F, 0.0, 0.0, 0,
                0.0F, 0.0F, 0, 0.0F);
    }
}
