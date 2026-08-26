# Current Status

Last curated: 2026-08-26

This is the compact living handoff for the Witchery NeoForge remake. Keep it short
enough to read before starting work. Long historical deltas belong in
`docs/archive/CHANGELOG.md` or a focused reference file.

## Project State

- Minecraft target: `1.21.11`
- NeoForge target: `21.11.42`
- Java target: JDK `21`
- Overall shape: playable active parity remake, with many systems implemented and
  the remaining work concentrated in QA, source-feel polish, edge cases, and
  release hardening.
- README completion percentages were retired on 2026-08-26. The feature matrix
  has unevenly scoped rows, and a `Done` implementation can still require
  live-client, multiplayer, balance, or packaging work; use the matrix and QA
  queue directly instead of treating an editorial percentage as a measurement.
- Deterministic verification manifest: 20 exact JVM resource contracts, 34 CI
  GameTests, and one manual forced-restart fixture.
- The standalone Book of Shadows is published at
  `https://thorelvin.github.io/witchery-book-of-shadows/`; this repository's
  root `Book_of_Shadows.html` remains canonical.

## Current Focus

- Keep the in-game Primer, `Book_of_Shadows.html`, and README aligned only for
  real player-facing behavior changes. After changing the canonical HTML, sync
  the public mirror using `docs/reference/BOOK_OF_SHADOWS_PUBLISHING.md`.
- Drive test work from [QA_QUEUE.md](QA_QUEUE.md), not scattered one-off notes.
- Keep [FEATURE_MATRIX.md](FEATURE_MATRIX.md) as feature state, not a rolling
  session log.
- Keep [NEXT_TASKS.md](NEXT_TASKS.md) as a backlog and slice picker, not the
  only handoff.

## High-Value Work Buckets

- Playtest the local AI persona layer in normal survival-like sessions: Coven
  Witches, Leonard, villagers, village guards, hobgoblins, Baba Yaga, bosses,
  demons/flame imps, and reflections should sound distinct while keeping server
  mechanics authoritative. Runtime cards now cover 98 indexed Markdown files and
  1,474 targeted facts behind exact/scoped/rumor/no persona access boundaries.
- In-server QA for the new protection facade: vanilla spawn/build limits plus
  Open Parties and Claims, FTB Chunks, and Common Protection API when installed.
- In-game QA for recently restored or polished systems: Flame Imp contracts,
  Vanishing Witch Hut, NBT structures, the source-corrected Spirit World lifecycle,
  mirrored terrain/spawns/Manifestation visuals, cursed well warnings, the complete
  Ice World area conversion, Witchery crop growth/worldgen, the paired Gulg/Mog
  fight, unusual custom-brew effect combinations, and active-rite presentation.
- State-dependent source-feel QA for exact-ID items and wearables. Foundational
  model geometry, pivots, transforms, textures, and held/armor routes now have a
  source-coordinate audit; remaining checks are animated, equipped, translucent,
  and gameplay-specific states.
- Creative-tab and GUI-item polish for promoted exact IDs: hide utility/internal
  blocks until their player-facing route is useful, keep item renders legible,
  and prefer source-shaped models/textures over improvised geometry.
- Fresh-world certification and traversal for NBT-backed huts, Coven Circle,
  Straw Man, wells, wild Witch Huts, and Nether placements; village mechanics
  already have their five-biome gate.
- Spirit World atmosphere and dream/nightmare behavior QA.
- Public-release hardening: docs clarity, launcher diagnostics, asset/legal
  review, packaging, and multiplayer/server checks.

## Recent High-Signal Changes

- The village ecosystem and cross-biome certification package is complete.
  Witchery village pieces now persist one canonical building claim and an
  origin-marked resident identity; walls persist gate candidates and selected
  gates, retain only force-load tickets they acquired, relocate living blockers,
  and perform one bounded collision rescue when construction finishes. The
  operator-facing `/witchery verify village` command audits building claims,
  biome-family materials, expected residents, wall/gate evidence, and a safe
  arrival. The fixed `witchery-runtime-test` world passed plains, desert,
  savanna, taiga, and snowy audits fresh and after restart; the harness also
  rejects any wall-suffocation death. Were Villagers now preserve complete
  sanitized Villager identity through infection, Wolfman form, reversion, and
  cure, and vampire/werewolf hybrids default to disabled. Creature Taglocks,
  Voodoo/Vampiric Poppets, Curse Creature, and Villager sacrifice selectors are
  covered by dedicated-server fixtures. The manifest is now 20 exact resource
  contracts, 34 CI GameTests, and one manual forced-restart fixture. See
  [VILLAGE_CERTIFICATION.md](VILLAGE_CERTIFICATION.md).

- Persistent ritual lifecycle parity is closed for the original registry's
  continuing workings. All 33 source registrations map to 21 explicit active
  owners with source first-boundary, pulse, and world-effect cadence. Staged
  offerings gate only their own new rite, so an unrelated offering sequence no
  longer freezes established workings. Protection upkeep remains every tick
  while barrier redraw stays on its 20-tick boundary; sustaining altar lookup is
  cached and revalidated instead of rescanning the full range every tick.
  Broken Earth now follows `RitePartEarth`: the first offered focus chooses one
  of eight headings, a 60-position wandering path is built below the circle,
  the first post-boundary update carves path index 5, and subsequent updates run
  every tick with source depth and Coven-scaled width. The real stop/restart
  probe now preserves that path, stage, heading, and exact refund ledger beside
  Forestation and Blight. Focused JVM tests and
  `witchery:persistent_ritual_queue` cover the registration/timing contract,
  per-rite gating, and first carve on a dedicated server.
- Brew Bag and Leonard's Urn interaction parity is restored. Brew Bag sneak-use
  opens the exact eight-slot source layout; normal held use drives an eight-way,
  bag-coloured radial selected from server-observed camera movement and consumes
  one stored brew on release. Leonard's Urn opens the original capacity-sensitive
  one-to-four-slot diamond layout and leaves casting to its four Mystic Branch
  glyphs. Both menus preserve complete fixed/composed brew stacks, protect the
  held container slot, use the original GUI art, and are covered by focused JVM
  and dedicated-server interaction tests.
- Flying Monkeys now restore their source mutation, tameable companion, and
  courier roles. Filled player or creature Taglocks start a persisted fetch and
  return journey; Bound and Blooded Waystones remain reusable while the monkey
  carries its owner to fixed or living targets, including safe cross-world
  arrival. Cancellation, dismount, unavailable targets, persistence, and arrival
  are covered by focused JVM and dedicated-server tests; live long-distance and
  multiplayer presentation remain QA.
- Custom Brew mechanical parity is closed against the original registry. An
  extracted, source-hashed fixture locks all 158 registrations and their source
  order; every action family has a modern owner. The Witch's Cauldron now keeps
  exact internal fluid volume and restores the original persisted brewing-skill,
  equipment, Brewing Expertise, Owl-familiar, Diamond-penalty, and
  1/2/3/4/5/6/8/10/15/30 bottle-yield ladder. The remaining source station
  recipes are live, including the corrected Egg-last Mutandis route, complex
  Mutandis/Extremis products, graded Endless Water and Combustion, cooking,
  Witch's Hand recycling, and Player Compass creation. Focused JVM contracts,
  the generated Witches' Brews guide, and
  `witchery:custom_brew_environmental_actions` cover the high-risk behavior;
  [CUSTOM_BREW_SOURCE_PARITY.md](../reference/CUSTOM_BREW_SOURCE_PARITY.md)
  holds the complete ownership and verification ledger. Its closeout passed the
  full build and the complete runtime suite available for that slice.
- Circle Magic recipe and offering parity is closed against the original registry.
  An extracted, source-hashed fixture locks all 96 registrations: 93 use exact
  full-recipe definitions and the remaining three use documented native direct
  paths. Source rites no longer enter representative-trigger fallback dispatch;
  required and optional offerings now match deterministically across distinct
  dropped stacks and Grassper-held stacks. Exact JVM contracts and the dedicated
  `witchery:ritual_offering_sources` server GameTest cover rings, source order,
  duplicate/optional requirements, mixed sources, staging, and rollback behavior.
- Fertility and Forestation now run as source-paced persistent rites. Fertility
  expands every five ticks from radius 4 to 50 plus Coven scaling, growing the
  land, restoring Zombie Villagers, clearing the source ailments, and applying
  the cat-familiar Regeneration II and Health Boost bonuses. Forestation attempts
  one selected-species tree per second for 59 base pulses, scales radius and
  attempts with the Coven, and honors a Bound Waystone destination. Both preserve
  stage, owner, focus, effect center, and exact abort refunds across restart, and
  pause while their caster or required chunks are unavailable. Focused JVM tests,
  `witchery:persistent_expanding_rites`, and the real save/reload probe pass.
- Blindness, Curse of Blight, and Hell on Earth now share that persistent,
  refund-safe expanding lifecycle. All wait for the next 20-tick boundary, pulse
  every five ticks, scale by two radius per nearby Coven witch, re-evaluate Cat
  Familiar mastery on each ring, pause for an offline caster or unloaded ring
  chunk, and preserve stage/refunds through restart. Blight's original
  Glistering Melon recipe, Nausea II, villager/cow/animal effects, exact old
  flower/crop set, and sand/dirt decay are restored. The previous Poisonous
  Potato + 3000-power heart seeding behavior is retained as the separately named
  additive **Rite of the Blight Heart**. Hell on Earth restores the source
  200-power upkeep, exact mob weights and center spawn, and configurable
  familiar-enhanced plant fires. Focused JVM coverage and
  `witchery:persistent_expanding_curses` exercise the server behavior; the real
  save/reload probe now certifies an in-progress Blight wave too.
- The Primer's Circle Magic chapter is now a complete generated compendium:
  58 searchable rite families contain all 96 source registrations exactly once.
  Each source-tagged card renders its circles, altar power, coven requirement,
  conditions, and ordered offerings from one canonical resource; optional
  offerings have a gold frame in both held and placed books. The separately
  labeled additive Rite of the Blight Heart brings the Primer to 115 generated
  entries without pretending to be a source registration. A focused parity
  contract locks the generated catalog back to the extracted source fixture.
- The source-facing `witchery:cauldron_book` now opens a dedicated native
  Modonomicon edition of **Witches' Brews**, separate from the progressive
  Primer. Four searchable categories and 14 entries cover cauldron setup,
  ordered brewing and bottling, live capacity/modifier/dispersal reagents,
  implemented old cauldron craftings, ritual targets/circles/covens, and the
  current effect/action registry. A registered resource contract maps all 196
  original markup keys exactly once and requires every live composed-brew
  ingredient to appear once in the generated reference. Unsupported old
  transmutations are named honestly instead of being shown as working recipes.
- The deterministic runtime harness and CI gates are complete. An explicit
  manifest locks 20 resource-contract classes, 34 exact CI GameTests, and one
  manual forced-restart fixture. Clean normal-difficulty dedicated servers now
  run every named test with all Witchery dimensions loaded, require unambiguous
  per-test success, preserve logs/JSON summaries, and verify Kettle, Witches'
  Oven, Distillery, Spinning Wheel, Brazier, Silver Vat, and command-storage
  state plus active Forestation, Blight, and Broken Earth workings across a real
  stop/reload. A separate fresh-structure lane certifies five village biome
  families and then reuses the same coordinates after restart.
  CI uploads build
  or runtime diagnostics on failure. Building the gate also exposed and fixed
  the Otherwhere Hearth traveller-state race on Spirit entry and the Lord of
  Torment's failure to collect nearby non-attacking players. Runtime worlds now
  explicitly set `pause-when-empty-seconds=0`, preventing long suites from
  freezing at Minecraft's default one-minute empty-server pause.
- Machine presentation parity is closed for the current machine roster. Witches'
  Oven, Distillery, and Spinning Wheel screens use the exact 1.7.10 atlases,
  slot coordinates, progress lanes, and source-specific one-pixel behavior.
  Kettle, Oven, Distillery, Brazier, and Fume Funnel ambient effects now follow
  their source state gates; accepted or overflowing Kettle ingredients use the
  original splash feedback. Brazier items retain their special renderer, and
  Silver Vat world spouts appear only toward adjacent block entities while item
  views show all four. Controlled original/port captures cover four GUIs, eight
  item forms, Oven facings/states, and the machine blocks; focused JVM tests,
  the full build, all 26 GameTests, and save/reload are green.
- Observations of an Immortal now uses its source-shaped standalone reader instead
  of the temporary Modonomicon route: the byte-identical 192 x 192 book sheet is
  exposed through a legal lowercase resource path, all 16 literal source markup
  pages and both sketches render, level 0-9 reveals exactly 6-16 pages, and the
  source page stack persists on close. The server validates the held book before
  storing navigation state or advancing the vampire level cap. A focused resource
  contract covers page names, unlock counts, markup, custom data, and image hashes;
  three live-client captures cover the title, ritual sketch, and cage sketch.
- Vampire, Village Guard, and village-consequence parity now share one persisted
  0-500 villager-blood contract. Sleeping villagers recover blood and eventually
  heal; conscious guards witness feeding, defend attacked villagers and exactly
  +10 village friends, turn on players at -15 reputation, and impose the old
  five-point reputation loss when slain. Guards retain the source 40 health,
  village-return cadence, monster exclusions, friendly-fire immunity, equipment,
  idle healing/blood recovery, paralysis gate, and tall fireproof type-1 variant.
  Hostile Vampire mobs now acquire a village at night, drain villager blood without
  ordinary melee damage, heal only when blood is taken, and return home after their
  source quota. Reversible Bell conversion preserves current blood in both
  directions. Focused JVM tests and
  `witchery:vampire_guard_village_consequences` pass on a dedicated server.
- Witchery crop parity now has a shared source-soil contract for Belladonna,
  Mandrake, Mindrake, Snowbell, Wormwood, Wolfsbane, and Garlic; source water
  support remains mandatory for Water Artichoke. Mindrake/Wolfsbane growth and
  bonemeal behavior, the mature Mindrake second-bulb chance, and mature-base
  Wormwood upward extension are restored. A generic configured-feature validator
  also catches invalid registered block states, including the corrected wild
  Wormwood age. Focused JVM tests and `witchery:witch_crop_parity` pass.
- Goblin Gulg and Goblin Mog now implement their source paired-boss contracts:
  400 health, partner-distance damage bands, a 15-damage cap, Mog's airborne-target
  arrow speed, Gulg's launch profile, source equipment/drops, persistence,
  fire/water/web resilience, full-bright rendering, and guarded atomic combat
  teleports. Focused JVM tests and `witchery:goblin_boss_parity` pass; live combat,
  reward, and presentation feel remain QA.
- The Hobgoblin Patron closes the boss acquisition path. Its exact Stone and
  Koboldite figure recipe feeds the existing source-exact 4000-power small-circle
  binding rite, which now stores owner name and UUID. A bound statue recruits
  Hobgoblins within eight blocks every five seconds, restores 30 infusion energy
  at five worshippers, grants Worship I/II at ten/fifteen, and keeps the original
  config-scaled paired automatic summon. The owner can spend one Nether Star and
  five active worshippers to summon Mog and Gulg together; duplicate bosses,
  unsafe ground, low worship, or wrong ownership consume nothing. Owner state,
  staged trades, source player-skin/stone-overlay renderer, special item route,
  save/reload, and the `witchery:patron_statue_lifecycle` GameTest pass.
- The remaining automatic vampire level perks now follow the 1.7.10 contract:
  human-form attack damage rises to ×2/×3/×4 at levels 1/4/7, level-3
  sneaking strikes launch their victim, and level-6 sneaking fist/tool-mismatch
  hits crush Stone, Netherrack, or Cobblestone for ten exhaustion while keeping
  ordinary drops. Vampires also refill air underwater, clear Poison every forty
  ticks, and take the source two armor-bypassing damage when Fire Resistance
  masks active flame. Focused JVM tests and the dedicated-server
  `witchery:vampire_passive_perks` fixture pass.
- Vampire Blood Power parity is implemented against the 1.7.10 contract. The
  source `NONE / DRINK / MESMERIZE / SPEED / BAT / ULTIMATE` selection ladder,
  level gates, costs, partial sunlight spending, ten-tick cooldown, source HUD
  icons/drops, slot-1 controls, vision toggle, logarithmic Speed stacking,
  compact flying Bat form and upkeep, and charged Storm/Swarm/Farm ultimates are
  live. Mortal players now persist and synchronize the old 0-500 human-blood
  pool, recover two points every 40 ticks, and can be drained by another player.
  Focused JVM tests and `witchery:vampire_power_activation` cover the contract;
  multiplayer HUD, camera, and audio feel remain live QA.
- Infernal ownership parity is complete for raised and bred undead. Binding now
  applies the permanent but curable Enslaved effect without making arbitrary
  mobs persistent; owner immunity survives a cure, while same-world loaded
  revenge requires the active effect. Brew of Raising owns persistent no-drop
  undead, and Brew of Love converts both owned adult zombie parents to zombie
  villagers while preserving their state and leaving the baby unowned. Three
  dedicated-server GameTests cover the lifecycle, raising, and breeding routes.
- Custom Brew projectile and ritual-target parity is restored. Leonard's Urn
  focuses cast only through Mystic Branch glyphs, using the source 4.0 velocity,
  zero gravity, and 200-tick lifetime. A filled Taglock Kit
  can now be the final cauldron ingredient for a direct 1,000-power ritual:
  later ingredients and bottling are locked, Tongue of Dog releases without a
  splash requirement, and player or loaded-mob targets use the existing coven,
  range, Leonard, Hunter armor, Area Marker, Voodoo Poppet, and Brick rules. The
  target payload persists across saves. Leonard also restores the source +1
  misfortune risk and asymmetric ritual backfire bounds.
- Legacy model-coordinate parity is now an explicit tested contract rather than
  a collection of visual approximations. `LegacyModelTransforms` preserves the
  source `ModelRenderer.render`, `renderWithRotation`, and `postRender` matrix
  orders; the source model inventory, cube graphs, pivots, hierarchies, mirror
  timing, renderer anchors, dimensions, held-item transforms, armor layers, and
  shadows were audited across entities, special blocks, equipment, and items.
  Controlled original/port front-and-rear captures cover the high-risk entity
  roster plus Village Guard, Vampire, Spectral Familiar, Garlic Garland, Mirror,
  and Witch's Cauldron. The cauldron body is now the exact transformed 17-cube
  source model with source liquid footprint/heights; its 12-piece grounded
  corner-reaching splayed feet remain the approved custom exception. The full
  JVM suite, post-fix client capture, and dedicated-server boot/shutdown passed.
  See `docs/reference/LEGACY_MODEL_COORDINATES.md`.

- Hobgoblin village ecology now follows the audited `EntityGoblin` and
  `EntityAIGoblinMate` rules. Registered village homes replace old door counts;
  settled Hobgoblins refresh their village center every 70-119 ticks, remain
  persistent, and return when they stray beyond the source-scaled home radius.
  Adults use the 1-in-500, 300-tick courtship and 35% housing cap, producing a
  vanilla-aged baby with a valid profession variant and putting both parents on
  the source 6000-tick cooldown. Only packs of three within eight blocks raid
  villagers or avoid Village Guards, while retaliation alerts nearby kin.
  Existing conversation, trade, leash mining, worship, follow, and player-guard
  orders override ambient ecology. Focused JVM tests and
  `witchery:hobgoblin_village_ecology` pass on the headless GameTest server.
- Lycanthropy is now source-complete as a level 1-10 survival path. The Wolf
  Altar enforces the exact three offerings, Horned Huntsman handoff, and five
  task gates; Moon Charm controls the separately synced/rendered wolf and
  wolfman forms at levels 2 and 5; full-moon nights, exact stat tables, source
  equipment shedding, digging, feeding, pounce, howl, taming, armor-rend, and
  infection rules are live. Wolftrap capture provides the natural initial bite.
  Progress, distinct howl chunks, and power-howl timing persist across reloads.
  `/witchery verify lycanthropy`, focused JVM tests, and the registered
  `witchery:lycanthropy_progression` GameTest cover the contract; live-client
  form/camera/audio and multiplayer bite combinations remain queued QA.
- Mirror World's fresh-save source contract is implemented. Its featureless
  biome carries the original 256-block repeating 9 x 9 x 9 lattice, including
  fifteen vertical allocation tiers before each source-spiral step. Source
  mirrors in any dimension own persisted links; the linked dropped item can be
  carried and replaced; player route stacks survive reconnects; ordinary mirror
  bridges carry living entities and items; and both source escape chants work.
  Reflections copy player state but not loot, every true Reflection yields one
  Demon Heart, and a chamber clears only after all Reflections inside it die.
  Duplication Grenades now create a separate passive `mirror_decoy`. Automated
  layout/resource gates and the normal-server lifecycle fixture cover these
  rules. The expanded fixture passed fresh on 2026-08-18, including guardian
  succession and a linked item carried into the Nether. See
  `docs/reference/MIRROR_WORLD_SOURCE_PARITY.md`; backwards save migration is
  intentionally deferred, and real-client rendering/atmosphere QA remains.
- Torment Fresh-Dimension Certification is complete. Generation now derives six
  deterministic source-shaped 31 x 31 perfect DFS mazes from one cached layout,
  using invisible Force walls/ceilings and source-correct Torment Stone rather
  than Bedrock for floors, foundations, and portal frames. Every level has the
  exact chamber, route, portal axis, and three Refilling Chests; all 18 chests
  fill initially and use the empty-only 72,000-tick refill rule. `/witchery
  verify torment` checks every expected block and block entity, layout
  fingerprints, maze connectivity, routes, and atmosphere resources. A fresh
  dedicated world and its restart both passed on 2026-08-18. Dedicated-server
  GameTests also passed structure/refill, exact portal round trip with a
  dedicated persisted return anchor, and a two-player Lord encounter with one
  disconnected participant queued and safely released on reconnect. The Lord's
  source flight, attacks, damage cap, drops, XP, model state, texture, lighting,
  particles, and audio are restored in code. Only live-client red sky/fog,
  portal tint, animation, particle, and audio feel remain manual QA.
- Spirit World completion QA now has dedicated, low-frequency tooling. `/witchery
  setup otherwhere_hearth` builds a destructive paired-world fixture with the
  approved volume, rings, bound Kettle, funded altar, coven, stateful blocks,
  fluids, redstone, pets, hostiles, items, and a ridden vehicle. `/witchery verify
  otherwhere_hearth` shares the activation block/protection preflight and reports
  ownership, bounds, route, journal phase, rings, coven, altar charge/capacity,
  players, entities, and the first world blocker without spending power.
  `/witchery verify spirit_lifecycle` audits mutually exclusive dream,
  manifestation, and physical-Hearth player state. JVM coverage now includes
  journal phase/backward-compatibility invariants, lifecycle-state invariants,
  and exact dream/nightmare/demonic atmosphere rules. The registered dedicated-
  server GameTests pass with custom dimensions loaded, proving a bidirectional
  real-player transfer path with block entities, fluids, redstone,
  entities on both sides, passenger links, physical-traveller flags, cleared
  journals, and one measured altar charge per crossing. A forced process kill
  after the roughly 88 KB prepared journal reached disk also passed restart replay: both
  structures and all non-player entities converged, the journal remained while
  the owner was offline, the returning owner arrived in Spirit, the journal
  cleared, and no second altar charge was taken. The destination-player refusal
  GameTest likewise passed without mutation or power spend. Those runs fixed
  dedicated-server client-handler classloading, the 1.21.11 snapshot API call,
  passenger journalability, canonical cross-dimension entity recovery,
  deterministic fixture altar funding, and entity-section loading for otherwise
  unloaded paired chunks. The CI runtime server now loads every Witchery
  dimension and covers the ordinary two-way transfer plus destination refusal;
  deliberate process-kill recovery, real-client multiplayer feel, and
  claim-adapter execution remain manual gates.
- The additive Otherwhere Hearth endgame working is code-complete, with live
  dedicated round-trip, restart-recovery, offline-owner, and destination-refusal
  GameTests passing; real-client multiplayer and claim QA remain open. A
  10,000-power six-ingredient Kettle batch permanently binds
  one empty Kettle to its owner. A large Otherwhere ring, medium Ritual ring,
  six coven members, and an in-volume altar with a base 10,000 available power
  then swap the complete same-coordinate 25 x 25 x 25 Overworld/Spirit World
  volumes, including block entities and every movable entity on either side.
  Activation refuses occupied destinations, dream travellers, unmovable
  entities, world/claim boundaries, unbreakable blocks, and other bound hearths.
  A persisted pre-mutation journal stores both structures, the traveller
  manifest, and complete non-player entity snapshots, allowing interrupted
  crossings, missing-entity recovery, and offline-player transfers to resume
  safely.
- Corpse 1.7.10 parity is code-complete pending live QA. Spirit World entry now
  leaves the source `1.2 x 0.5` persistent Body at the centre of the entry block,
  carrying the owner's name, UUID, health, and skin. It cannot fall, drift, or
  be pushed; accepts the source no-op interaction; uses the old `Owner` save key
  beside modern UUID data; has no ordinary loot; and renders as a motionless
  prone player with the source `0.9 / 0.25` offset, rotations, and zero shadow.
  Destroying it while the owner is online still forces that dreamer awake, and
  normal return removes the Body.
- Nightmare 1.7.10 parity is code-complete pending live QA. The entity now uses
  the source 0.6 x 1.8 fireproof body, 25 XP, full door/target/village movement
  stack, 16-block victim expiry burst, persistent source victim-name key,
  screaming and defended sync, fall/web immunity, 15-damage cap, source defense
  timings, pre-hit armor stripping, out-of-dimension half-heart attacks, and
  exact Mellifluous Hunger plus Looting math instead of invented teleporting and
  Condensed Fear. The renderer uses the authentic texture, 0.5 shadow, 0.9
  translucency, and 0.6 defended veil. Waking Nightmare is again a persisted
  curse that rarely calls one victim-bound hunter outside the Spirit World and
  clears on Spirit World entry or either source cure route.
- Flame Imp 1.7.10 parity is code-complete pending live QA. The entity is again
  tameable rather than a generic monster, with wild player hostility, owner
  defense, a 16-block home range, source attributes/healing/despawn rules, and
  the original affection decay and rare contract-abandonment path. The exact
  21-item shiny table and uncapped gift odds restore the Hunger, Fear, Anguish,
  and Torment Scroll sequence plus later ingredient gifts. Demon Heart power
  lasts 60 minutes, doubles melee damage from 4 to 8, changes the incoming cap
  from 15 to 5, heals each second, widens the model/body without making it
  taller, and ends with Icy Needles. Source names, particles, sounds, texture,
  wall-clock persistence, and empty ordinary death loot are restored.
- Demon 1.7.10 parity is code-complete pending live QA. The bow-only ranged goal
  was replaced with the original close-melee/generic-fireball split and village
  movement stack; source XP, attributes, door navigation, spawn Fire Resistance,
  Peaceful survival, summoned persistence, nightmare cleanup, walking debris,
  attack pose, gait/wobble, full-bright light, `0.5` shadow, and exact texture are
  restored. Bargains now use Blaze Rod rather than Blaze Powder, restore the
  Ghast Tear/Ender Pearl offers, and explode after Blaze Rod or Magma Cream
  payment. Player kills roll `0..(1 + Looting)` Magma Cream; Demon Hearts require
  the source Arthana chance or the guaranteed heart bargain.
- Awakened Mandrake code parity is complete against 1.7.10. Mature plants use
  the source 90% daytime / 10% nighttime awakening split on the server,
  Peaceful always yields the crop, and a successful awakening now restores the
  20-particle root burst, exact `0.6 x 0.9` body, inherited 32-block player pursuit,
  ordinary wandering, `0.5` render shadow, and whole-body walking wobble. The
  exact source texture remains byte-identical. Original Mandrakes intentionally
  use vanilla Ghast scream/hurt/death sounds; no custom Mandrake sound asset
  existed. Live harvest/contact/render feel remains in the QA queue.
- Silver Vat source-renderer parity replaces the temporary baked approximation.
  Placed and item forms now use the exact 64x32 `silvervat` sheet and remade
  `ModelSilverVat` geometry, including all four item spouts, world spouts hidden
  beside neighboring block entities, the old blue brew surface, and one to eight
  visible source deposit pieces based on the stored stack's eight-item
  thresholds. Adjacent output growth, exact one-in-five deposit odds, hopper
  extraction/refusal, and stored inventory reload are dedicated-server
  certified; only live model, collection, and break-drop feel remain here.
- Grotesque visual parity now covers both source routes. The fixed Brew of the
  Grotesque keeps its 1200-tick player repulsion timer, while composed Infernal
  Blood applies the three-minute `witchery:grotesque` effect to living targets;
  either route now renders the original mirrored 128x32 Demon-textured head
  with horns, tusks, snout, lip, source scale/offset, head tracking, and
  source-shaped invisibility handling. Fixed player state syncs on activation,
  expiry, and first tracking so other clients see the transformation without
  per-tick network traffic.
- `EntityMirrorFace` is restored in its actual source role, correcting the old
  backlog description that called it another Mirror World guardian. Ordinary
  uncleared mirrors now show the invulnerable, no-collision translucent
  head apparition for the source ten seconds when used empty-handed or prompted
  by nearby mirror speech, with the original texture, head-only model, source
  placement offsets, reflection-speech cue, and persisted `WITCTicksAlive` age.
- Spectre, Banshee, and Poltergeist now share a source-shaped
  `SummonedUndeadEntity` base without changing their registry IDs. The restored
  common behavior covers summoner name/UUID persistence, Spectre obscured state,
  optional suicide timers, source 15-damage incoming caps, undead tag membership,
  source-targeted Dissipate/Revealing hooks, Spectral Stone finalization/owner
  stamping, Graveyard Dust strengthening for owned spectral undead, and the old
  shared rare Spectral Dust drop for non-temporary spectral undead.
- Decurse statues were promoted from missing parity to playable exact IDs:
  `witchery:decurseteleport` / Statue of Occluded Summons and
  `witchery:decursedirected` / Statue of Broken Curses now appear in the
  Witchery creative tab, persist source-style owner/killer marker data, allow
  creative or sneaking-owner stacked removal, and dampen taglock entity
  transposition or directed curse rites in the configured 32-block horizontal
  radius. Their placed and item forms now use exact 1.7.10 geometry, transforms,
  and 64x32 textures: the former is the small mandrake effigy and the latter is
  the wolf statue with its source pedestal. Live visual/multiplayer QA remains.
- AI persona/knowledge routing was expanded from a small NPC-card layer into a
  broad runtime card library. `NpcKnowledgeBase` now filters cards through the
  machine-readable persona access matrix, downgrades scoped/rumor facts in prompt
  context, reports dropped-card diagnostics, and normalizes villager profession
  aliases so weaponsmith/armorer/toolsmith route through the shared smith lane
  while farmer/fisherman/shepherd/butcher route through the farm lane. Prompt and
  knowledge-base regression tests now assert that every active persona routes to
  its own card lane. Villager raw-polish guards also reject wrong-profession
  identity leaks, so a weaponsmith cannot drift into "I am the apothecary" even
  in relaxed raw mode.
- Creative-tab/icon parity pass tightened several source-facing item families:
  Circle Talismans expose the original visible encoded stacks and choose icons
  from stored circle data, Spectral Stones expose blank/Spectre/Banshee/Poltergeist
  variants from binding data, Leonard's Urn shows capacity 1-4, Plant Mines render
  as poppy/dandelion/dead-bush disguises without creative duplicate clutter,
  Torment Scroll and Soul of Torment use source-style art, Brew Bag tinting returns
  to leather colour, wolf/hellhound head sprites were refreshed, source-named
  stale Belladonna/Mandrake/Water Artichoke seed ingredient entries are visible
  beside the real plantable seeds, debug-only QA items are kept out of the
  normal player-facing tab, spawn eggs live in their own Witchery Spawn Eggs
  tab, and the two
  source decurse statues are visible as exact IDs with their original
  mandrake/wolf render forms. The
  screenshot comparison lives in `docs/status/CREATIVE_TAB_PARITY_AUDIT.md`.
- Source-facing name parity now prefers old English display names while keeping
  stable modern IDs: Minedrake, Blood Poppy, Wild Bramble, Demonic Blood, and
  Gulg's Gurdle are restored in lang/docs/cards; the latest display-name pass
  also restores source labels such as Belladonna Flower, Wool of Bat, Diamond
  Vapor, Witches Hand, Icy Slippers, Attuned Stone (Charged), the ladies vampire
  coats, and Mirror Surface while keeping stable modern IDs. Exact
  `witchery:gulgs_grudle` is live and drops from Gulg while
  `witchery:gulgs_girdle` remains compatible.
- OpenAI-compatible NPC providers now support bearer-token auth via an
  environment variable, optional extra metadata headers, configurable
  `responseFormat` (`json_object`, `json_schema`, or `none`), and strict NPC
  JSON-schema parsing when schema mode is enabled. README and the LLM NPC
  experiment note document the new knobs, plus the release-pack guidance to
  disable AI/prewarm unless a model sidecar is intentionally managed. The shared
  NPC response parser also avoids regex salvage for object-shaped invalid JSON
  and unescapes quoted fallback speech fragments cleanly. Prompt construction
  now renders durable memory and current message routing as separate sections
  instead of mixing routing instructions into the memory block. `/witchery ai status`
  and `/witchery ai knowledge` now expose knowledge-card diagnostics: loaded section
  and card counts, targeted-note count, last retrieval reason, and last sources.
  AI status also keeps a short rolling trail of recent rejection labels, so
  validation misses, repeated lines, busy gates, and intent denials are visible
  without log-diving. Provider request URI path appending now rebuilds from URI
  components, avoiding query/fragment leakage and duplicate suffixes.
- Local AI NPC dialogue has moved from a Coven Witch-only experiment into a
  general persona framework. The current playtest path uses local Ollama/EVA by
  default, prewarms the model, loads compact markdown knowledge cards, keeps
  per-entity/player memory, uses LLM checks for routing/social/command intent
  where useful, and still treats code/fallbacks as the source of authority.
  Command-keyword knowledge questions now route to dialogue before command
  permission denial, including modal lore questions such as whether hobgoblins
  can find koboldite, and card repair keeps hobgoblin role/voice instead of
  falling back to coven-witch wording. Repair prompts now use the entity's real
  biome/time context, async/direct command conversation cleanup has explicit
  completion/abort paths, and provider/config failures no longer spend the
  request cooldown. Low-risk normal chat now skips the separate model-backed
  message-analysis call and uses deterministic routing context before the
  persona response. Provider HTTP clients are reused by timeout bucket, request
  executor parallelism now follows the configured concurrency limit, and AI
  cooldown maps prune expired entries during long sessions.
- The protection facade is live for NPC command ownership and terrain-changing
  magic. It checks vanilla permissions and optional reflective adapters for
  Open Parties and Claims, FTB Chunks, and Common Protection API, with config
  modes for broad terrain effects (`skip`) and destructive core effects
  (`fail`). Adapter exceptions now fail closed for protected actions, and the
  Common Protection API bridge tolerates the known `UNKNOWN` profile signature
  drift.
- Curse of Insanity now uses source-shaped hallucinations instead of the earlier
  nausea stand-in: victim-only fake Creeper/Spider/Zombie entities spawn near
  the cursed player, render only for that player, make private fake sounds, deal
  no damage, drop nothing, and decay away. Needs in-game fear/frequency QA.
- Hobgoblin labor has a deeper source-shaped pass: leashed hobgoblins with
  pickaxes can be ordered by chat to mine, actively path to nearby natural
  stone/earth/ore blocks, break them with feedback, wear their pickaxe, drop
  normal resources, and roll Koboldite Dust into nearby chests or the world.
  Patron worship state persists and now feeds the source 5/10/15 owner reward
  thresholds and paired Goblin God lifecycle.
  Follow and guard chat orders now have real movement behavior; guard watches
  for threats attacking the command holder instead of just acknowledging text.
  Failed hobgoblin command attempts no longer claim a new command owner.
- Flame Imp contracts now use the source-style bargain loop: signed ownership
  contract, 25-level taming cost, shiny-gift mood, target/self spell contracts,
  and the five source abilities.
- Designer-authored NBT structures are part of worldgen while keeping standard
  structure-block-readable NBT assets.
- Vanishing Witch Hut has natural forest/swamp worldgen, relocation when
  unobserved, same-resident carrying, and salt-line counterplay.
- Dream command flow should behave like normal dream entry: matching X/Z,
  same-column Y safety, stored Overworld return point, and expanded passive
  animal spawning.
- Triggered cursed-block mechanics now support warning messages and cursed
  well ladder behavior.
- Recent QA polish cleaned up the Witchery creative tab, restored single-texture
  Rowan/Alder/Hawthorn leaf art with visible berry/flower colors, added vertical
  stockade stack presentation plus source wood variants while preserving contact damage, moved Death's
  Hand back to a source-style same-item scythe toggle, and nudged the Goddess
  statue / Crystal Ball GUI transforms. Goddess Statue cures now use
  player-centered instant-spell particles with a low fizz cue instead of the
  prior block-centered bonemeal effect.
- Current parity polish tightened moving brew fluids, braziers, Spirit World
  fog, Death's Hand held-side rendering plus hit/toggle cues, Mystic Branch
  cast/fail feedback, Crystal Ball scry validation before altar-power charge,
  brew bucket gas/liquid tooltip feedback, Dream Weaver placement/wake cues,
  trap state sync, Beartrap/Wolftrap toggles/snaps through the old
  `random.click` / `random.mantrap` cues, cursed warning cooldowns, Leonard's Urn focus storage and
  capacity upgrades, Witches Hand Otherwhere cooldown/feedback, Duplication
  Grenade mirror charging, Book of Biomes climate page stats,
  Wolftrap lured-wolf steering/capture feedback plus source Wolfman lure/capture
  voices, player werewolf howls and Wolf Altar checks through the old Wolfman
  clips, Wolfman, Flame Imp, Baba Yaga,
  Owl, Toad, Hobgoblin, Gulg, Mog, Nightmare, Spectre, Banshee, Treefyd,
  Flying Monkey, Reflection, Lilith, Leonard, Dark Mark, and Lord of Torment
  source sounds, the original `random.*` utility sound event surface, chalk
  drawing through the old `random.chalk` cue, Demon source vanilla sound mix,
  Parasytic Louse payload cues,
  Sun Collector/Sun Grenade feedback, and compass scrying cues. The Hand of
  Death's forward, palm-down held orientation and outward third-person alignment
  were approved during the model pass; manual visual work now concentrates on
  animated equipment states and in-world atmosphere feel.
- Vampire blood-transfer feedback now follows the source sound surface too:
  player bites, hostile vampire villager drains, Glass Goblet fills/conversions,
  Lilith's empty-goblet reward, Vampire Blood, Attack Bat feeding, and Bone
  Needle white-wool staining use `random.drink`; drinking full goblets uses
  burp feedback.
- Custom Brew curability now respects the old `PotionBase.setIncurable()` gate
  for current Witchery effects across Spread Affliction, Cure Buffs, Cure
  Debuffs, and Steal Buffs. Moonshine, Stout Belly, Gas Mask, Sun Allergy,
  Paralysed, Queasy, Diseased, and the Shade of Leonard hex effects no longer
  count as normal curable effects for those composed-brew actions; Purified Milk
  still keeps the source Diseased special-cleanup branch.
- High-strength Paralysed now restores the old vampire lure branch for villagers
  and Village Guards: amplifier 5+ leaves them unfrozen, clears combat targeting
  while active, and paths them once per second toward the nearest level-8+
  vampire player within the source 1-16 block window.
- Infused Brew of the Grave now has its missing active effect: drinking the
  Grave brew arms a source-length 120-minute state, and the Necromantic Stone
  now handles the old undead command surface. Normal use orders nearby owned
  undead toward a target or top-face destination; sneak-use on an undead while
  Grave is active binds it through the shared enslaved-minion tags with the
  old strict `> 200` tick reuse gate.
- A first Witch's Triangle scanner slice is now live: a rowan-framed wooden
  triangle that uses a two-arm hold pose, lights its centre with purple portal
  art while scanning, remembers unique scanned block/entity ids on the item, and
  counts simple research points for those discoveries.

## Docs Cleanup Note

Use [../DOCS_INDEX.md](../DOCS_INDEX.md) as the routing contract before adding
new notes. Active docs should stay short; long session history belongs in
`docs/archive/CHANGELOG.md`.
