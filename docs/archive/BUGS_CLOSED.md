# Closed Bug Archive

Bugs #2–58 were tracked in `docs/status/BUGS.md` and closed by 2026-05-24. Bug #1 (Ritual queue parity) remains open — see `docs/status/BUGS.md`.

For commit-level fix details: `git log --oneline` and search for the relevant feature name.

---

## Closed bugs by category

| # | Category | Description | Closed |
|---|---|---|---|
| 2 | Rendering | PoppetShelfRenderer items rendered below shelf floor (y offset wrong after 180° flip) | 2026-05-22 |
| 3 | Rendering | WitchesOvenRenderer faced 180° backward (`facing.getOpposite()` instead of `facing`) | 2026-05-22 |
| 4 | Rendering | Cat familiar body/tail misaligned (wrong model hierarchy) | 2026-05-22 |
| 5 | Rendering | Nightmare tendrils used wrong anchor/sway (non-source) | 2026-05-22 |
| 6 | Rendering | Ent had wrong UV mapping and missing face plate / inner leaf fill | 2026-05-22 |
| 7 | Rendering | Mandrake had wrong UV mapping and non-source walking animation | 2026-05-22 |
| 8 | Rendering | Banshee had non-source chin plate always visible (should only show during scream) | 2026-05-22 |
| 9 | Rendering | Poltergeist used partial mirror UVs instead of all-part mirror | 2026-05-22 |
| 10 | Rendering | Treefyd used wrong model/texture variant (Resurrected instead of 1.7) | 2026-05-22 |
| 11 | Rendering | Flying Monkey used scaled zombie renderer instead of dedicated monkey model | 2026-05-22 |
| 12 | Rendering | Flame Imp attack arm/body twist did not play; powered imp did not scale taller | 2026-05-22 |
| 13 | Rendering | Spirit appeared as opaque yellow ball instead of translucent wisp | 2026-05-22 |
| 14 | Rendering | Death scythe pivot had x/z swapped — scythe drifted forward instead of sitting at hand | 2026-05-22 |
| 15 | Rendering | Demon wings used non-source idle z-flap instead of source static pose | 2026-05-22 |
| 16 | Rendering | Glowglobe rendered opaque (cutout) instead of translucent | 2026-05-22 |
| 17 | Items | `TaglockKitItem.isPlayerTaglock` returned true on unfilled stacks | 2026-05-22 |
| 18 | Items | Crystal Ball showed yellow placeholder cube instead of purple stained-glass orb on stand | 2026-05-22 |
| 19 | Items | Coffin item used flat generated sprite instead of 3D block model | 2026-05-22 |
| 20 | Items | `sun_collector` item model used `item/generated` parent instead of block parent | 2026-05-24 |
| 21 | Items | Circle Talisman golden chalk variant stored wrong index (0 instead of 4) | 2026-05-23 |
| 22 | Items | Circle Talisman blank binding depended on wrong large-circle dispatcher | 2026-05-23 |
| 23 | Blocks | `ice_door` used IRON block-set type (no right-click open) | 2026-05-18 |
| 24 | Blocks | `rowan_log` placed as non-rotated block (no axis variants) | 2026-05-18 |
| 25 | Blocks | `rowan_leaves` lacked proper transparency and decay | 2026-05-18 |
| 26 | Blocks | `witch_door` was plain `Block` instead of `DoorBlock` | 2026-05-18 |
| 27 | Blocks | Fetish blocks faced wrong direction on placement | 2026-05-18 |
| 28 | Blocks | `glint_weed` emitted no light (original is full bright 15) | 2026-05-18 |
| 29 | Blocks | `leaping_lily` reused wrong texture (`glint_weed` instead of snowbell) | 2026-05-18 |
| 30 | Blocks | Leech Chest open animation did not play | 2026-05-22 |
| 31 | Poppets | `VampiricPoppetItem` two-taglock semantics missing; redirect targeted source player instead of victim | 2026-05-24 |
| 32 | Poppets | `PoppetEvents.onVampiricRedirect` looked up source UUID instead of victim UUID for redirect target | 2026-05-24 |
| 33 | Rituals | Active rites stacked on same center (no dedup check) | 2026-05-22 |
| 34 | Rituals | Rings sustained on any same-size replacement loop instead of the original chalk family | 2026-05-22 |
| 35 | Rituals | Mid-flight abort did not refund staged rite dropped ingredients | 2026-05-22 |
| 36 | Rituals | Climate Change consumed biome focus before successfully queueing | 2026-05-22 |
| 37 | Rituals | Curse routes consumed offered taglock before precondition/protection checks | 2026-05-22 |
| 38 | Rituals | Summon family burned altar power before obstruction/entity creation checks | 2026-05-22 |
| 39 | Rituals | Fake public routes (TNT Crater, ender pearl self-transposition, etc.) activated | 2026-05-22 |
| 40 | Rituals | Paired-ring rites (Flying Ointment, player infusions, Wither, Hell on Earth) fired from lone rings | 2026-05-22 |
| 41 | Rituals | Chalk family gates missing — ritual/infernal/otherwhere treated as interchangeable | 2026-05-22 |
| 42 | Rituals | Blight/Blindness/Hell on Earth collapsed to one-shot pulses instead of staged annulus | 2026-05-22 |
| 43 | Rituals | Medium curse routes used fake trigger items instead of offered filled Taglock Kit | 2026-05-22 |
| 44 | Rituals | Large Wither summoning did not require inner medium infernal companion ring | 2026-05-22 |
| 45 | Rituals | Unknown offerings on valid circles gave no feedback instead of "Unknown rite" snare cue | 2026-05-22 |
| 46 | Rituals | Sanctity/Imprisonment missing 500 altar-power startup cost | 2026-05-22 |
| 47 | Rituals | Forestation routed through wrong large circle (should be medium) | 2026-05-22 |
| 48 | Rituals | Nature's Power routed through wrong large circle (should be medium) | 2026-05-22 |
| 49 | Rituals | New casts failed on first detected ring size instead of retrying outer-to-inner | 2026-05-22 |
| 50 | Machines | Spinning Wheel recipe system accepted unintended inputs | 2026-05-22 |
| 51 | Machines | Witches' Oven accepted ore smelting (should be food/witchery only) | 2026-05-22 |
| 52 | Machines | Distillery showed false Frost shortcuts from wrong source comparison | 2026-05-22 |
| 53 | Machines | Kettle had false duplicate Frost item and fake direct brew routes | 2026-05-22 |
| 54 | Entities | `WitchHunterEntity` targeting did not include vampires/werewolves/poppet carriers | 2026-05-22 |
| 55 | Entities | `BroomEntity` ridden flight rubber-banded back to spawn point (dual client/server simulation) | 2026-05-22 |
| 56 | Spirit World | Spirit World vegetation worldgen features present but biome features arrays empty | 2026-05-23 |
| 57 | Spirit World | Manifested ghost did not use full-bright lighting | 2026-05-23 |
| 58 | Spirit World | Circle Magic not blocked in Spirit World (only Rite of Manifestation should be permitted) | 2026-05-23 |
