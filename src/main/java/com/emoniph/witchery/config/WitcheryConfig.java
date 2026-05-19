package com.emoniph.witchery.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Witchery common config. Values apply on both client and server.
 * Loaded from config/witchery-common.toml.
 */
public class WitcheryConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // ── Vampire ───────────────────────────────────────────────────────────────
    public static final ModConfigSpec.IntValue VAMPIRE_BLOOD_DRAIN_INTERVAL;
    public static final ModConfigSpec.IntValue VAMPIRE_SUNBURN_CHECK_INTERVAL;
    public static final ModConfigSpec.IntValue VAMPIRE_SUNBURN_DAMAGE;

    // ── Werewolf ──────────────────────────────────────────────────────────────
    public static final ModConfigSpec.BooleanValue WEREWOLF_FULL_MOON_ONLY;
    public static final ModConfigSpec.IntValue WEREWOLF_HOWL_COOLDOWN_SECONDS;

    // ── Spirit World ──────────────────────────────────────────────────────────
    public static final ModConfigSpec.IntValue NIGHTMARE_CHANCE_PERCENT;
    public static final ModConfigSpec.IntValue PLAYERS_SLEEPING_PERCENT_FOR_BUFF;

    // ── Altar / Rituals ───────────────────────────────────────────────────────
    public static final ModConfigSpec.DoubleValue RITUAL_POWER_MULTIPLIER;

    // ── World gen ─────────────────────────────────────────────────────────────
    public static final ModConfigSpec.BooleanValue ENABLE_ROWAN_TREES;
    public static final ModConfigSpec.BooleanValue ENABLE_WILD_CROPS;
    public static final ModConfigSpec.BooleanValue ENABLE_BRAMBLE_PATCHES;

    static {
        BUILDER.push("vampire");
        VAMPIRE_BLOOD_DRAIN_INTERVAL = BUILDER
                .comment("Ticks between blood drain ticks (lower = faster drain). Default: 100 (5s)")
                .defineInRange("bloodDrainInterval", 100, 20, 6000);
        VAMPIRE_SUNBURN_CHECK_INTERVAL = BUILDER
                .comment("Ticks between sunburn damage ticks in direct sunlight. Default: 20 (1s)")
                .defineInRange("sunburnCheckInterval", 20, 5, 200);
        VAMPIRE_SUNBURN_DAMAGE = BUILDER
                .comment("Damage dealt per sunburn tick. Default: 1 (half a heart)")
                .defineInRange("sunburnDamage", 1, 1, 20);
        BUILDER.pop();

        BUILDER.push("werewolf");
        WEREWOLF_FULL_MOON_ONLY = BUILDER
                .comment("If true, wolf form buffs only apply on full moon nights. Default: false (always active)")
                .define("fullMoonOnly", false);
        WEREWOLF_HOWL_COOLDOWN_SECONDS = BUILDER
                .comment("Cooldown in seconds between wolf howls. Default: 60")
                .defineInRange("howlCooldownSeconds", 60, 5, 600);
        BUILDER.pop();

        BUILDER.push("spirit_world");
        NIGHTMARE_CHANCE_PERCENT = BUILDER
                .comment("Percent chance of entering a nightmare instead of a dream (0-100). Default: 70")
                .defineInRange("nightmareChancePercent", 70, 0, 100);
        PLAYERS_SLEEPING_PERCENT_FOR_BUFF = BUILDER
                .comment("Percent of players that must be asleep before Dream Catchers arm their sunrise buff. Default: 100")
                .defineInRange("playersSleepingPercentForBuff", 100, 1, 100);
        BUILDER.pop();

        BUILDER.push("rituals");
        RITUAL_POWER_MULTIPLIER = BUILDER
                .comment("Multiplier applied to altar power costs. 0.5 = cheaper, 2.0 = more expensive.")
                .defineInRange("powerMultiplier", 1.0, 0.1, 10.0);
        BUILDER.pop();

        BUILDER.push("worldgen");
        ENABLE_ROWAN_TREES = BUILDER
                .comment("Enable rowan (witch) tree worldgen in forests.")
                .define("enableRowanTrees", true);
        ENABLE_WILD_CROPS = BUILDER
                .comment("Enable wild belladonna and mandrake worldgen.")
                .define("enableWildCrops", true);
        ENABLE_BRAMBLE_PATCHES = BUILDER
                .comment("Enable bramble patch worldgen in forests.")
                .define("enableBramblePatches", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
