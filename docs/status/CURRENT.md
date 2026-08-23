# Current Status

Last curated: 2026-08-23

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
- Current estimates from the README: Mechanical remake parity roughly `95-96%`;
  release-ready parity/polish roughly `82-86%`.

## Current Focus

- Keep the in-game Primer, `Book_of_Shadows.html`, and README aligned only for
  real player-facing behavior changes.
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
  Ice World area conversion, and other custom brew/ritual edge cases.
- State-dependent source-feel QA for exact-ID items and wearables. Foundational
  model geometry, pivots, transforms, textures, and held/armor routes now have a
  source-coordinate audit; remaining checks are animated, equipped, translucent,
  and gameplay-specific states.
- Creative-tab and GUI-item polish for promoted exact IDs: hide utility/internal
  blocks until their player-facing route is useful, keep item renders legible,
  and prefer source-shaped models/textures over improvised geometry.
- Structure/worldgen tuning for NBT-backed huts, wells, villages, and Nether
  placements.
- Spirit World atmosphere and dream/nightmare behavior QA.
- Public-release hardening: docs clarity, launcher diagnostics, asset/legal
  review, packaging, and multiplayer/server checks.

## Recent High-Signal Changes

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
  server GameTests passed on 2026-08-18 with custom dimensions loaded, proving a
  bidirectional fake-player round trip with block entities, fluids, redstone,
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
  unloaded paired chunks. NeoForge's headless GameTest server still loads only
  the three vanilla dimensions, so real-client multiplayer/offline behavior and
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
  thresholds. Storage, comparator, collection, and adjacent-output mechanics are
  unchanged; live model and furnace/hopper QA remains.
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
  Patron worship state persists and still feeds prayers into the statue.
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
  `random.click` / `random.mantrap` cues, cursed warning cooldowns, Leonard's Urn focus release and
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
- Village Guards now regain source-style staying power after vampire feeding:
  an idle guard standing in a village periodically heals and has the old
  one-in-four chance to recover one point of its persisted `BloodLevel`.
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
