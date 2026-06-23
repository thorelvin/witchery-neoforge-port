# Spirit / Dream World Atmosphere — Source Reference (1.7.10-0.24.1)

Canonical look-and-feel of the old Witchery **Spirit World** (a.k.a. Dream World), transcribed from
the decompiled source for parity work. Primary source: `com/emoniph/witchery/dimension/WorldProviderDreamWorld.java`
(line refs below), plus `worldgen/WitcheryWorldGenerator.java` and the relevant entities.

> Decompiled `1.7.10-0.24.1` is the canonical parity reference (wikis are supplementary).

## Sky, fog, horizon, colours

From `WorldProviderDreamWorld`:

- **Fog colour** (`getFogColor` / `func_76562_b`, L154–186) — three palettes by nightmare level, scaled
  by a day/brightness factor:
  - **Normal dream:** RGB **(0.8, 0.2, 0.6)** — a bright **pink / magenta-purple** haze.
  - **Nightmare (level 1):** **(0.0, 1.0, 0.0)** — green.
  - **Demonic nightmare (level 2):** **(1.0, 0.0, 0.0)** — red.
  - Brightness factor `var3 = clamp(cos(angle·2π)·2 + 0.5, 0, 1)`; then
    `R,G ×= var3·0.94 + 0.06` and `B ×= var3·0.91 + 0.09`. The slightly higher **blue floor (0.09)**
    keeps a cold tint even in darkness.
- **Horizon = 0.0** (`getHorizon`, L136) — horizon at the very bottom; the fog band fills almost the
  whole vertical view → an open, bottomless, skyline-less feel.
- **No stars** (`getStarBrightness` = 0, L103); **no clouds** (`getCloudHeight` = 0, L119);
  **surface world = true** (renders a sky, L146); **no void particles** (L141).
- **Time / light** (`calculateCelestialAngle`, L115): normal = **1.0 (≡ noon, bright)**;
  nightmare = **0.5 (midnight, dark)**. A plain dream is **washed-out perpetual daylight in pink fog**;
  a nightmare is **perpetual night** (dark green); demonic is dark red.
- No rain/snow (weather/`canRespawnHere` false, L107); sea level 64 (`func_76557_i`, L132);
  spawn (100, 50, 0) (`func_76554_h`, L128).

## Terrain & flora

- Terrain uses the **overworld's chunk generator** (`func_76555_c`, L81) — land shape and biomes mirror
  the overworld, just under the dream sky.
- Decoration (`generateDreamworld`, `WitcheryWorldGenerator` L56): sprinkles only **Wispy Cotton**
  (white wispy plant, ~1 cluster/chunk, skipped 1/4 of chunks).
- **Flowing Spirit** pools (`BlockFlowingSpirit`, a water-material `BlockFluidClassic`) dot the world as
  pale spirit pools — also used as a nightmare-chance reducer on entry.

## Sounds

- **Native spirits are silent** (`EntitySpirit` living/hurt/death sounds all `null`) — eerie, voiceless.
- **Nightmares** roar: `witchery:mob.nightmare.nightmare_live / _dead / _hit`.
- **Banshees / Spectres:** `witchery:mob.banshee.banshee_scream` on attack; `spectre_hit` otherwise.
- **No dimension music/record** and **no ambient wind loop** — the soundscape is just sparse,
  unsettling mob sounds.

## Atmosphere via gameplay (FX)

- **Nightmare pressure** (`updatePlayerEffects`, L788+): in a nightmare, nightmares spawn near the
  player every ~100 ticks; **demonic** rains small/large **fireballs from ~15 blocks up** (chance-based)
  and spawns **Demons** with `SMOKE` particles + `MOB_WITHER_DEATH` sound.
- **Entry** leaves the body as an `EntityCorpse` in the overworld; the player arrives with only
  **Icy Needle + Mutandis** and a gentle upward push (`PacketPushTarget 0, 0.1, 0`).
- Environmental nightmare modifiers on entry (`sendPlayerToSpiritWorld`, L319+): dream catchers,
  flowing-spirit pools (−0.1 each, max 3) and wispy cotton (−0.1 each, max 2) reduce nightmare chance;
  demon hearts (+0.35 each) and fire (+0.1 each) raise it. Demonic requires catcher + pool + heart + RNG.

## Net impression

An overworld-shaped land drowned in flat, bottomless **pink-purple fog** with no stars/clouds/sun-disc
to anchor it, dotted with white wispy cotton and pale spirit pools, eerily quiet except for distant
nightmare roars and banshee screams — turning to a dark **green** (or hellish **red**) night when a
nightmare takes hold.

## modern-remake mapping (NeoForge 1.21.1)

Source method → modern equivalent for the remake:

- `getFogColor` palettes → `DimensionSpecialEffects.getBrightnessDependentFogColor` (+ a custom
  `DimensionSpecialEffects` subtype) and/or biome `fog_color`; nightmare/demonic variants need a
  client-readable nightmare-level signal.
- `getHorizon` 0 / no stars / no clouds → `DimensionSpecialEffects` (`cloudLevel`,
  `hasGround`/`skyType`, `isFoggyAt`, custom sky renderer) and dimension_type flags.
- perpetual day vs nightmare-night (`calculateCelestialAngle`) → dimension_type `fixed_time` or a
  client celestial-angle override; nightmare needs a darker fixed time.
- Wispy cotton / flowing-spirit decoration → biome placed features.
- Silent spirits / nightmare roars / banshee screams → entity `SoundEvents`.

## Remake implementation status (2026-06-19)

Already faithful in the remake:

- `SpiritWorldSpecialEffects` reproduces the exact fog-colour logic (pink/green/red palettes, the
  `cos·2+0.5` brightness factor, the 0.06/0.09 floors).
- `WitcheryClientEvents.onExtractLevelRenderState` sets the spirit sky colour, **no clouds**
  (`NO_CLOUDS`), **no stars** (`starBrightness=0`), no sunrise/sunset tint, and the fixed sun/moon/star
  angle (dream 1.0, nightmare 0.5). `onComputeFogColor` tints the fog to match.
- `dimension_type/spirit_world.json` + `worldgen/biome/spirit_world.json` (`fog_color`/`sky_color`
  `0xCC3399` = the source pink) drive the static base; the biome also places richer flora than source
  (spirit grass/trees/mushrooms/dead bush + wispy & disturbed cotton).

Fixed 2026-06-19:

- **Nightmare/dream colour was inverted on the client.** `SyncPlayerDataPayload` synced `inSpiritWorld`
  but not the dream/demonic flags, and `isNightmare()` is `!sw.isDream` with `isDream` defaulting to
  `false` — so the client always read "nightmare" and rendered the **green** sky/fog for a normal
  **pink** dream. The payload now also carries `spiritWorldDream` + `demonicNightmare`, applied
  client-side, so dreams are pink, nightmares green, demonic red. The fog gate was also aligned to the
  sky's dimension-only check so they can't desync.
- **Dream entry coordinates now match the source-style mirrored-column flow.** Normal entry and the
  `/witchery dream` QA command both store the Overworld return point, load the matching Spirit World
  chunk, keep the player's X/Z, preserve Y when it is safe, and otherwise only nudge Y within that same
  column to the nearest two-block-tall landing space. `/witchery exit` uses the full Spirit World wake-up
  helper for those entries.
- **Fog range is now explicitly shortened.** The source `getFogColor` supplied the three pink/green/red
  palettes, but modern rendering also needs a fog-distance override; the remake now gives dreams a 52-block
  fog end, nightmares 40, and demonic nightmares 32 while retaining the source colours.
- **Dream wildlife is now a requested modern expansion.** Source Witchery reused Overworld generation and
  allowed peaceful spawns; the remake's fixed Spirit World biome now opens that up beyond sheep/pig/chicken/cow
  to the broader vanilla passive animal roster across creature, ambient, water, cave-water, and axolotl spawn
  categories.

Still divergent from source (deliberate / low priority): the remake colours the whole **sky** spirit-pink
(source only tinted the fog and left a dimmed vanilla sky); the dimension uses one custom biome rather
than borrowing overworld biomes.
