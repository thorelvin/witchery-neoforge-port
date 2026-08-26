package com.emoniph.witchery.config;

import com.emoniph.witchery.worldgen.VillageWallGenerator;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Witchery common config. Values apply on both client and server.
 * Loaded from config/witchery-common.toml.
 */
public class WitcheryConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue DEBUG;
    public static final ModConfigSpec.IntValue TOWN_ZOMBIE_ATTACK_REDUCTION_MODE;
    public static final ModConfigSpec.BooleanValue ALLOW_DEATHS_HOOD_FREEZE;
    public static final ModConfigSpec.IntValue HOBGOBLIN_GOD_SPAWN_CHANCE_PERCENT;

    // ── Vampire ───────────────────────────────────────────────────────────────
    public static final ModConfigSpec.IntValue VAMPIRE_BLOOD_DRAIN_INTERVAL;
    public static final ModConfigSpec.IntValue VAMPIRE_SUNBURN_CHECK_INTERVAL;
    public static final ModConfigSpec.IntValue VAMPIRE_SUNBURN_DAMAGE;

    // ── Werewolf ──────────────────────────────────────────────────────────────
    public static final ModConfigSpec.BooleanValue ALLOW_PLAYER_TO_PLAYER_WOLF_INFECTION;
    public static final ModConfigSpec.BooleanValue ALLOW_VAMPIRE_WOLF_HYBRIDS;

    public static final ModConfigSpec.IntValue MANTRAP_OPACITY_PERCENT;

    // ── Spirit World ──────────────────────────────────────────────────────────
    public static final ModConfigSpec.IntValue NIGHTMARE_CHANCE_PERCENT;
    public static final ModConfigSpec.IntValue PLAYERS_SLEEPING_PERCENT_FOR_BUFF;

    // ── Altar / Rituals ───────────────────────────────────────────────────────
    public static final ModConfigSpec.DoubleValue RITUAL_POWER_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue ALLOW_BIOME_CHANGING;
    public static final ModConfigSpec.BooleanValue ALLOW_HELL_ON_EARTH_FIRES;
    public static final ModConfigSpec.BooleanValue ALLOW_DECURSE_TELEPORT;
    public static final ModConfigSpec.BooleanValue ALLOW_DECURSE_DIRECTED;
    public static final ModConfigSpec.IntValue DECURSE_TELEPORT_RADIUS;
    public static final ModConfigSpec.IntValue DECURSE_DIRECTED_RADIUS;

    // ── World gen ─────────────────────────────────────────────────────────────
    public static final ModConfigSpec.BooleanValue ENABLE_ROWAN_TREES;
    public static final ModConfigSpec.BooleanValue ENABLE_WILD_CROPS;
    public static final ModConfigSpec.BooleanValue ENABLE_BRAMBLE_PATCHES;
    public static final ModConfigSpec.IntValue VILLAGE_WALL_SPAWN_PERCENT;

    // Protection / anti-grief
    public static final ModConfigSpec.BooleanValue PROTECT_NPC_COMMANDS;
    public static final ModConfigSpec.BooleanValue PROTECT_TERRAIN_MAGIC;
    public static final ModConfigSpec.ConfigValue<String> TERRAIN_MAGIC_PROTECTED_AREA_MODE;
    public static final ModConfigSpec.ConfigValue<String> TERRAIN_MAGIC_DESTRUCTIVE_CORE_MODE;

    // Local-only NPC LLM experiment
    public static final ModConfigSpec.BooleanValue LLM_ENABLED;
    public static final ModConfigSpec.BooleanValue LLM_PREWARM_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> LLM_PROVIDER;
    public static final ModConfigSpec.ConfigValue<String> LLM_ENDPOINT;
    public static final ModConfigSpec.ConfigValue<String> LLM_MODEL;
    public static final ModConfigSpec.IntValue LLM_TIMEOUT_MILLIS;
    public static final ModConfigSpec.IntValue LLM_PREWARM_TIMEOUT_MILLIS;
    public static final ModConfigSpec.IntValue LLM_MAX_CONCURRENT_REQUESTS;
    public static final ModConfigSpec.IntValue LLM_MAX_TOKENS;
    public static final ModConfigSpec.IntValue LLM_KEEP_ALIVE_MINUTES;
    public static final ModConfigSpec.IntValue LLM_CHAT_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue LLM_COOLDOWN_TICKS;
    public static final ModConfigSpec.BooleanValue LLM_CANCEL_HANDLED_NPC_CHAT;
    public static final ModConfigSpec.BooleanValue LLM_ALLOW_LAN_ENDPOINT;
    public static final ModConfigSpec.ConfigValue<String> LLM_API_KEY_ENV;
    public static final ModConfigSpec.ConfigValue<String> LLM_EXTRA_HEADERS;
    public static final ModConfigSpec.ConfigValue<String> LLM_RESPONSE_FORMAT;

    static {
        DEBUG = BUILDER
                .comment("Enable extra Witchery diagnostic logging for QA. Default: false")
                .define("debug", false);

        BUILDER.push("general");
        TOWN_ZOMBIE_ATTACK_REDUCTION_MODE = BUILDER
                .comment("Source TownZombieAttackReductionMode: 0 disables village zombie protection, 1 reduces zombie damage to villagers, 2 also clears zombie villager targets. Default: 1")
                .defineInRange("TownZombieAttackReductionMode", 1, 0, 2);
        ALLOW_DEATHS_HOOD_FREEZE = BUILDER
                .comment("Source AllowDeathsHoodToFreezeVictims: if true, Death's Hood slows creatures the wearer stares at. Default: true")
                .define("AllowDeathsHoodToFreezeVictims", true);
        HOBGOBLIN_GOD_SPAWN_CHANCE_PERCENT = BUILDER
                .comment("Source HobgoblinGodSpawnChance: relative percent used by a 15-worshipper Patron statue every five seconds. Default: 10")
                .defineInRange("HobgoblinGodSpawnChance", 10, 0, 100);
        BUILDER.pop();

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
        ALLOW_PLAYER_TO_PLAYER_WOLF_INFECTION = BUILDER
                .comment("Source AllowPlayerToPlayerWolfInfection. Default: true")
                .define("AllowPlayerToPlayerWolfInfection", true);
        ALLOW_VAMPIRE_WOLF_HYBRIDS = BUILDER
                .comment("Source AllowVampireWerewolfHybrids. Default: true")
                .define("AllowVampireWerewolfHybrids", true);
        BUILDER.pop();

        BUILDER.push("traps");
        MANTRAP_OPACITY_PERCENT = BUILDER
                .comment("Opacity percent for closed owned Beartraps when viewed by non-owners. Original default: 30")
                .defineInRange("mantrapOpacityPercent", 30, 10, 100);
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
        ALLOW_BIOME_CHANGING = BUILDER
                .comment("Allow the Rite of Shifting Seasons to rewrite biome data. Default: true")
                .define("allowBiomeChanging", true);
        ALLOW_HELL_ON_EARTH_FIRES = BUILDER
                .comment("Source AllowHellOnEarthFires: let a cat-enhanced Hell on Earth ignite affected plants. Default: true")
                .define("AllowHellOnEarthFires", true);
        ALLOW_DECURSE_TELEPORT = BUILDER
                .comment("Allow Statues of Occluded Summons to dampen taglock transposition pulls near the target. Source default: true")
                .define("allowDecurseTeleport", true);
        ALLOW_DECURSE_DIRECTED = BUILDER
                .comment("Allow Statues of Broken Curses to dampen directed curse rites near the target. Source default: true")
                .define("allowDecurseDirected", true);
        DECURSE_TELEPORT_RADIUS = BUILDER
                .comment("Horizontal protection radius for Statues of Occluded Summons. Source default: 32")
                .defineInRange("decurseTeleportRadius", 32, 0, 128);
        DECURSE_DIRECTED_RADIUS = BUILDER
                .comment("Horizontal protection radius for Statues of Broken Curses. Source default: 32")
                .defineInRange("decurseDirectedRadius", 32, 0, 128);
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
        VILLAGE_WALL_SPAWN_PERCENT = BUILDER
                .comment("Percent chance (0-100) that a village receives adaptive Witchery walls. Deterministic per village. 0 = never, 100 = every village. Default: 40")
                .defineInRange("villageWallSpawnPercent", VillageWallGenerator.DEFAULT_WALL_SPAWN_PERCENT, 0, 100);
        BUILDER.pop();

        BUILDER.push("protection");
        PROTECT_NPC_COMMANDS = BUILDER
                .comment("If true, hobgoblins and recruited witches only obey commands from their owner/authorized player. Default: true")
                .define("protectNpcCommands", true);
        PROTECT_TERRAIN_MAGIC = BUILDER
                .comment("If true, Witchery terrain-changing magic checks player build permissions before modifying blocks. Default: true")
                .define("protectTerrainMagic", true);
        TERRAIN_MAGIC_PROTECTED_AREA_MODE = BUILDER
                .comment("How broad terrain magic handles protected blocks: skip or fail. Default: skip")
                .define("terrainMagicProtectedAreaMode", "skip");
        TERRAIN_MAGIC_DESTRUCTIVE_CORE_MODE = BUILDER
                .comment("How destructive core effects handle protected blocks: fail or skip. Default: fail")
                .define("terrainMagicDestructiveCoreMode", "fail");
        BUILDER.pop();

        BUILDER.push("llm");
        LLM_ENABLED = BUILDER
                .comment("Enable experimental local NPC LLM dialogue. Default: false. Set true only when this install intentionally provides a local model sidecar or compatible endpoint.")
                .define("enabled", false);
        LLM_PREWARM_ENABLED = BUILDER
                .comment("Send one invisible startup request so the local model is loaded before the first player chat. Default: false. Enable only for installs with a known working model sidecar.")
                .define("prewarmEnabled", false);
        LLM_PROVIDER = BUILDER
                .comment("Local LLM provider: ollama or openai_compatible. Default: ollama")
                .define("provider", "ollama");
        LLM_ENDPOINT = BUILDER
                .comment("Local provider endpoint. Ollama default: http://127.0.0.1:11434")
                .define("endpoint", "http://127.0.0.1:11434");
        LLM_MODEL = BUILDER
                .comment("Local model name. Tested branch target: witchery-eva-qwen2.5-1.5b-q4km-4k")
                .define("model", "witchery-eva-qwen2.5-1.5b-q4km-4k");
        LLM_TIMEOUT_MILLIS = BUILDER
                .comment("Maximum time to wait for an NPC LLM response before falling back. Default: 3000")
                .defineInRange("timeoutMillis", 3000, 250, 30000);
        LLM_PREWARM_TIMEOUT_MILLIS = BUILDER
                .comment("Maximum time to allow the invisible startup warmup request. Cold local models can need longer than normal chat. Default: 30000")
                .defineInRange("prewarmTimeoutMillis", 30000, 1000, 120000);
        LLM_MAX_CONCURRENT_REQUESTS = BUILDER
                .comment("Maximum concurrent NPC LLM requests. Default: 1")
                .defineInRange("maxConcurrentRequests", 1, 1, 4);
        LLM_MAX_TOKENS = BUILDER
                .comment("Maximum generated tokens for one NPC line. Default: 80")
                .defineInRange("maxTokens", 80, 16, 256);
        LLM_KEEP_ALIVE_MINUTES = BUILDER
                .comment("Ollama keep_alive duration after NPC requests, in minutes. Keeps prewarmed models resident. 0 uses provider default. Default: 30")
                .defineInRange("keepAliveMinutes", 30, 0, 240);
        LLM_CHAT_COOLDOWN_TICKS = BUILDER
                .comment("Cooldown per NPC/player chat response trigger in ticks. Default: 20")
                .defineInRange("chatCooldownTicks", 20, 20, 12000);
        LLM_COOLDOWN_TICKS = BUILDER
                .comment("Cooldown per non-chat NPC/player dialogue trigger in ticks, including greetings, gaze, and ambient reactions. Default: 100")
                .defineInRange("cooldownTicks", 100, 20, 12000);
        LLM_CANCEL_HANDLED_NPC_CHAT = BUILDER
                .comment("Cancel normal player chat broadcast after an explicit/private nearby NPC chat or NPC command is accepted. Look-at ambient routing still broadcasts. Default: true")
                .define("cancelHandledNpcChat", true);
        LLM_ALLOW_LAN_ENDPOINT = BUILDER
                .comment("Allow non-loopback LLM endpoints for a LAN sidecar machine. Default: false")
                .define("allowLanEndpoint", false);
        LLM_API_KEY_ENV = BUILDER
                .comment("Environment variable containing a bearer API key for openai_compatible endpoints. Blank disables auth header. Default: blank")
                .define("apiKeyEnv", "");
        LLM_EXTRA_HEADERS = BUILDER
                .comment("Extra openai_compatible HTTP headers as 'Header: value' lines separated by semicolons or newlines. Authorization and Content-Type are ignored here.")
                .define("extraHeaders", "");
        LLM_RESPONSE_FORMAT = BUILDER
                .comment("openai_compatible response format: json_schema, json_object, or none. json_schema uses strict NPC JSON schema when the endpoint supports it. Set json_object or none for looser local servers. Default: json_schema")
                .define("responseFormat", "json_schema");
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
