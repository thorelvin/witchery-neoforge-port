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

## Remake mapping (NeoForge 1.21.11)

- `SpiritWorldAtmosphere` reproduces the exact fog-color and celestial-angle calculations and
  selects dream, nightmare, or demonic state from loaded Spirit travellers. The strongest shared
  state wins, matching the source dimension-wide presentation.
- `WitcheryClientEvents.onComputeFogColor` changes fog color only. It deliberately does not replace
  the mirrored biome sky color or impose a port-only fog range.
- Client render state pins source celestial time locally to 6000 for dreams and 18000 for nightmare
  states, removes stars and sunrise tint, and uses the source-equivalent no-cloud presentation. It
  does not mutate the server or Overworld clock.
- `dimension/spirit_world.json` combines Overworld noise settings with the Overworld multi-noise
  biome preset. `dimension_type/spirit_world.json` retains skylight and zero ambient-light lift.
- `SpiritWorldPlacement` applies the source 75% chunk roll, chooses the randomized X/Z column, then
  chooses the old Y=0-to-surface-plus-32 seed. The configured patch retains 64 flower attempts.
- Dedicated biome tags reproduce the source cotton exclusions and native-Spirit land categories.
- Nightmare pressure, entry chance modifiers, Flowing Spirit behavior, entity admission, harvest
  restrictions, lifecycle state, and manifestation are implemented in server-side rules.

## Certification evidence (2026-08-28)

Controlled original and port clients captured four matched scenarios:

- `original/original-dream-clear.png` and `port/port-dream-clear.png`
- `original/original-dream-rain.png` and `port/port-dream-rain.png`
- `original/original-nightmare-clear.png` and `port/port-nightmare-clear.png`
- `original/original-demonic-clear.png` and `port/port-demonic-clear.png`

The files live under `C:\claude\witchery\captures\spirit-world\`. The generated contact sheet and
machine-readable measurements are `comparison/atmosphere-contact-sheet.png` and
`comparison/atmosphere-comparison.json`. They certify the source pink, green, and red palettes,
fixed day/night celestial presentation, terrain darkening, zero stars, and no-cloud sky. Modern
Minecraft rain-particle density differs from 1.7.10 and is recorded as a renderer-generation
difference rather than claimed as pixel-identical.

`witchery:spirit_world_fresh_chunks` generates actual paired 9 x 9 fresh regions from the fixed
`witchery-runtime-test` seed. It compares 5,184 biome-holder samples and 1,296 generator
pre-decoration terrain heights, then requires natural Wispy Cotton, ordinary Overworld ecology,
eligible Spirit spawn floors, and the exact weight-1, group-2-to-5 spawn entry.

`witchery:spirit_world_lifecycle` uses two simultaneous Survival players to certify peaceful and
nightmare isolation, shared source atmosphere, Body ownership/persistence, separate inventories and
vitals, reconnect, live Icy Needle and lethal-damage wake, blocked-return fallback, Manifestation
transfer, victim-bound Nightmare pressure, and final cleanup. The separate
`tools/run_spirit_world_lifecycle_certification.ps1` two-process client lane proves exact persisted
Body UUID, escrow, return point, inventory, health, and hunger across a JVM restart before waking
through live Icy Needle item use.

## Portal and entity presentation ledger (2026-08-28)

The presentation follow-up was audited directly against the decompiled 0.24.1 classes before the
port was changed. This table is the source-to-port contract; it deliberately separates exact source
behavior from modern renderer adaptations.

| State | Original 1.7.10 contract | Certified port presentation |
|---|---|---|
| Spirit Portal | `BlockSpiritPortal` uses the animated vanilla `portal` sprite, color multiplier `65382` (`#00ff66`), light `0.75`, a collisionless 4/16-thick plane selected from neighboring portal cells, and two `DRIP_WATER` particles per display tick. It defines no loop, crossing sound, or camera hook. Its interior is 2 x 2 inside a 4 x 4 Snow frame with optional corners. | Axis-backed 4/16 planes use the modern animated Nether Portal sprite, source tint, light level 11, zero occlusion, two oriented dripping-water particles, and no invented sound/camera effect. Portal creation now places the four cells with known-shape updates so partially formed interiors do not tear themselves down. |
| Body | `EntityCorpse` is 1.2 x 0.5, health 20, movement attribute 1.0 but an empty move method, no shadow, and the exact `RenderCorpse` prone translate/90-degree transforms. Dream entry assigns the username as both custom name and owner; the renderer resolves that owner's player skin. It is a bare `RenderLiving` path with no armor, held-item, or equipment layers. The entry code assigns the custom name but does not explicitly force the always-render flag. | The source dimensions, immobility, no-shadow prone pose, owner skin, and owner-name identity are preserved. The modern port keeps that nameplate visible on creation/reload as a deliberate readability adaptation. Equipment is intentionally absent because the source Body renderer never renders it. |
| Manifestation | `WorldProviderDreamWorld` renders the real player through the ordinary player renderer at alpha `0.51`; player skin, armor, held items, and normal lighting/layers therefore remain available. There is no separate model, ambient loop, particle cadence, or camera override. | `ManifestedGhostLayer` re-renders the real player model and its normal layers at exact alpha `0.51`. The controlled case retains the owner skin, full diamond armor, a diamond sword, and Icy Needles; no extra camera or ambient effect was added. |
| Spirit | `EntitySpirit` is 0.25 x 0.25 and silent. `ModelSpirit` is the exact five-cube geometry; `RenderSpirit` uses alpha `0.6`, no shadow, and the source texture. Every client tick emits one tight gold particle across the entity width at Y + 0.1 through Y + 0.3. | The exact model/texture/transforms remain locked by the legacy matched-model captures and numeric tests. The prior one-in-four, broad/high mote spread is corrected to one source-colored dust mote every client tick within the source bounds. Modern dust replaces the removed legacy particle API without changing cadence or footprint. |
| Nightmare | `ModelNightmare`/`RenderNightmare` use the authored tendril model and texture, shadow 0.5, alpha `0.9` normally and `0.6` while defended. The intentionally named sounds are ambient `nightmare_live`, hurt `nightmare_dead`, and death `nightmare_hit`; ambient cadence is twice the vanilla interval. There is no presentation camera hook. | Exact geometry, texture, tendril animation, alphas, shadow, cadence, mappings, and byte-identical OGG assets remain in place. Matched-model evidence is reused because this slice found no renderer defect to change. |
| Demonic Nightmare | Demonic nightmare is world/player state, not a different entity variant. It uses the same `EntityNightmare`, renderer, texture, animation, and sounds; red fog and extra demonic pressure are supplied by the world provider. | The controlled demonic case records `demonic=true` while using the same Nightmare renderer. The distinct presentation remains the already-certified red world atmosphere, not an invented alternate skin or model. |

### Controlled evidence

The package evidence lives under `C:\claude\witchery\captures\spirit-presentation\`:

- `original/` contains the successful original-client Portal, Body, and Manifestation run plus
  `original-core-20260828.log`. The archived 1.7 skin endpoint no longer resolves, so the original
  Body image falls back to Steve; source code establishes the owner-skin contract, while the port
  capture proves the live modern owner skin and name.
- `original/original-spirit.png`, `original/original-nightmare.png`, and
  `original/original-demonic-nightmare.png` reuse the already-certified original model captures.
  `matched-model/` holds the corresponding original/port close pairs.
- `port/port-portal.png` through `port/port-demonic-nightmare.png` are real framebuffer captures
  made by the property-gated integrated-client harness. `port/port-20260828-232655.log` records the
  exact portal tint, Body state, manifested equipment, and ordinary/demonic flags for the valid run.

`tools/original_capture_agent/plans/spirit-presentation-core.plan` reproduces the source core run.
`tools/run_spirit_presentation_capture.ps1` reproduces all six port cases in a verified disposable
save copy. The focused JVM presentation/resource tests, all 22 runtime resource contracts,
`witchery:spirit_world_lifecycle`, `witchery:spirit_world_fresh_chunks`, and the full Gradle build
all passed on 2026-08-28. The lifecycle run used two simultaneous server players and finished in
3.734 seconds; the fresh-chunk run finished in 12.703 seconds.

## Remaining manual QA

Source research, controlled Portal/Body/Manifestation/entity captures, numeric/resource contracts,
and the two-player server lifecycle are closed. One ordinary two-client pass still needs to judge
crossing feedback and portal sound range, remote Body interaction/wake/reconnect presentation,
simultaneous peaceful/nightmare feel, and live Nightmare audio cadence. Rain-density feel remains
the separate modern-renderer caveat. Until that pass is recorded, this presentation package is
automated/controlled-client certified rather than fully closed.
