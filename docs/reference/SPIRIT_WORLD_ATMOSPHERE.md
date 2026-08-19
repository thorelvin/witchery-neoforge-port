# Spirit / Dream World Atmosphere - Source Reference (1.7.10-0.24.1)

Canonical behavior of the old Witchery **Spirit World** (Dream World), transcribed from the
decompiled source. The primary references are
`dimension/WorldProviderDreamWorld.java`, `worldgen/WitcheryWorldGenerator.java`,
`blocks/BlockCotton.java`, `blocks/BlockFlowingSpirit.java`, and the relevant entities.

> Decompiled `1.7.10-0.24.1` is the parity authority. Wikis are supplementary.

## Sky, fog, and time

`WorldProviderDreamWorld` defines three fog palettes, scaled by its fixed celestial angle:

- Normal dream: RGB `(0.8, 0.2, 0.6)`, a pink-magenta haze.
- Nightmare: RGB `(0.0, 1.0, 0.0)`, green.
- Demonic nightmare: RGB `(1.0, 0.0, 0.0)`, red.
- Brightness is `clamp(cos(angle * 2 PI) * 2 + 0.5, 0, 1)`. Red and green are multiplied by
  `factor * 0.94 + 0.06`; blue uses `factor * 0.91 + 0.09`.

The provider does **not** set a custom fog distance. It returns horizon `0.0`, star brightness
`0.0`, cloud height `0.0`, surface-world rendering `true`, and no void particles. Celestial angle
is fixed at `1.0` for a normal dream and `0.5` for nightmare or demonic nightmare. Weather still
runs through the vanilla provider after nightmare state is synchronized; the source does not
explicitly suppress rain. Respawning there is disabled. Sea level is 64 and the nominal spawn is
`(100, 50, 0)`.

## Terrain and flora

- The source asks the Overworld provider for its chunk generator, preserving Overworld-shaped
  terrain and biome geography.
- Dream-world decoration adds **only Wispy Cotton**. Flowing Spirit has no natural world generator.
- In an eligible biome, 75% of chunks attempt one `WorldGenFlowers` patch. The patch makes 64
  placement attempts from a random X/Z column and a random Y in `[0, surface + 32)`.
- The old biome exclusions cover ocean, river, mountain, beach, and swamp categories, with jungle
  explicitly allowed. Cotton uses ordinary bush light and soil survival rules. Its own spread
  additionally requires a neighboring Flowing Spirit source below it and only spreads onto
  grass/dirt-family soil.

## Entities and dimension rules

- Native Spirits are silent, passive tameable animals. They use Bone as their food predicate,
  emit gold particles, do not drown, and naturally despawn. Their source spawn registration covers
  land biome categories but excludes oceans, rivers, and Mushroom Island.
- Nightmares are not biome spawns. Periodic nightmare pressure rolls them around their named victim;
  demonic dreams can also call fireballs and Demons. Nightmares ignore fire, falling, cobwebs, and
  drowning.
- Source entity admission rejects Endermen and living classes outside vanilla Minecraft and
  Witchery while in the Spirit World. Ordinary creature sunlight checks also return false there,
  so vampires do not burn merely because a peaceful dream uses its daylight angle.
- Circle magic is blocked. Ender Pearl and Blaze Powder block-harvest drops are removed.

## Travel and manifestation

- Dream entry leaves an `EntityCorpse` body in the waking world and maintains separate waking and
  dream inventory, health, and hunger state.
- Entry supplies only Icy Needles and ordinary Mutandis from waking inventory. Normal wake-up restores
  waking state and transfers the source Spirit materials acquired in the dream.
- Environmental modifiers near the sleeping body affect nightmare chance: Dream Catchers, Flowing
  Spirit sources, and Wispy Cotton reduce it; Demon Hearts and fire increase it with source caps.
- The Rite of Manifestation is prepared in the Overworld. A prepared dreamer crosses a complete
  2 x 2 Spirit Portal on foot and appears at matching Overworld coordinates with source 51% alpha.
  Return transfers only Icy Needles and bound fetishes acquired while manifested.
- Waking dismounts the traveller, extinguishes fire, and uses the nearest safe standing position in
  the stored return X/Z column when the exact Y is obstructed.

## Remake mapping (NeoForge 1.21.1)

- `SpiritWorldAtmosphere` reproduces the exact fog-color and celestial-angle calculations.
- `WitcheryClientEvents.onComputeFogColor` changes fog color only. It deliberately does not replace
  the mirrored biome sky color or impose a port-only fog range.
- Client render state fixes the source sun angle, removes stars and sunrise tint, and uses the
  source-equivalent no-cloud presentation.
- `dimension/spirit_world.json` combines Overworld noise settings with the Overworld multi-noise
  biome preset. `dimension_type/spirit_world.json` retains skylight and zero ambient-light lift.
- `SpiritWorldPlacement` applies the source 75% chunk roll, chooses the randomized X/Z column, then
  chooses the old Y=0-to-surface-plus-32 seed. The configured patch retains 64 flower attempts.
- Dedicated biome tags reproduce the source cotton exclusions and native-Spirit land categories.
- Nightmare pressure, entry chance modifiers, Flowing Spirit behavior, entity admission, harvest
  restrictions, lifecycle state, and manifestation are implemented in server-side rules.

## Live QA still required

Unit/resource tests and `/witchery verify spiritworld` cover the deterministic contracts. A real
client should still verify fog color, fixed daylight/night, weather feel, no-cloud presentation,
fresh-chunk cotton density, native-Spirit density, portal teardown, and manifested-skin appearance.
