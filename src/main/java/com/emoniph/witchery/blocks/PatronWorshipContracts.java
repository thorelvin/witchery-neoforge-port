package com.emoniph.witchery.blocks;

/** Source thresholds and probability policy for the Hobgoblin Patron statue. */
public final class PatronWorshipContracts {
    public static final int PULSE_TICKS = 5 * 20;
    public static final int MANUAL_SUMMON_WORSHIPPERS = 5;
    public static final int INFUSION_WORSHIPPERS = 5;
    public static final int WORSHIP_I_WORSHIPPERS = 10;
    public static final int WORSHIP_II_WORSHIPPERS = 15;
    public static final int INFUSION_PER_PULSE = 30;
    public static final int WORSHIP_EFFECT_TICKS = 60 * 20;

    private PatronWorshipContracts() {
    }

    public static boolean canManuallySummon(int worshippers) {
        return worshippers >= MANUAL_SUMMON_WORSHIPPERS;
    }

    public static boolean canRechargeInfusion(int worshippers) {
        return worshippers >= INFUSION_WORSHIPPERS;
    }

    public static int worshipAmplifier(int worshippers) {
        if (worshippers >= WORSHIP_II_WORSHIPPERS) {
            return 1;
        }
        return worshippers >= WORSHIP_I_WORSHIPPERS ? 0 : -1;
    }

    public static double automaticSummonChance(int configuredPercent) {
        return Math.clamp(configuredPercent, 0, 100) * 0.0001D;
    }
}
