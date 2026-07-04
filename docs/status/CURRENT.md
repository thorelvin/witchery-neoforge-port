# Current Status

Last curated: 2026-07-04

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
  Witches, Leonard, villagers, hobgoblins, Baba Yaga, demons/flame imps, and
  reflections should sound distinct while keeping server mechanics authoritative.
- In-server QA for the new protection facade: vanilla spawn/build limits plus
  Open Parties and Claims, FTB Chunks, and Common Protection API when installed.
- In-game QA for recently restored or polished systems: Flame Imp contracts,
  Vanishing Witch Hut, NBT structures, dream entry/exit, cursed well warnings,
  and custom brew/ritual edge cases.
- Source-feel polish on exact-ID items and wearables that are registered but
  still need model, texture, transform, or gameplay verification.
- Creative-tab and GUI-item polish for promoted exact IDs: hide utility/internal
  blocks until their player-facing route is useful, keep item renders legible,
  and prefer source-shaped models/textures over improvised geometry.
- Structure/worldgen tuning for NBT-backed huts, wells, villages, and Nether
  placements.
- Spirit World atmosphere and dream/nightmare behavior QA.
- Public-release hardening: docs clarity, launcher diagnostics, asset/legal
  review, packaging, and multiplayer/server checks.

## Recent High-Signal Changes

- OpenAI-compatible NPC providers now support bearer-token auth via an
  environment variable, optional extra metadata headers, configurable
  `responseFormat` (`json_object`, `json_schema`, or `none`), and strict NPC
  JSON-schema parsing when schema mode is enabled. README and the LLM NPC
  experiment note document the new knobs. The shared NPC response parser also
  avoids regex salvage for object-shaped invalid JSON and unescapes quoted
  fallback speech fragments cleanly.
- Local AI NPC dialogue has moved from a Coven Witch-only experiment into a
  general persona framework. The current playtest path uses local Ollama/EVA by
  default, prewarms the model, loads compact markdown knowledge cards, keeps
  per-entity/player memory, uses LLM checks for routing/social/command intent
  where useful, and still treats code/fallbacks as the source of authority.
  Command-keyword knowledge questions now route to dialogue before command
  permission denial, and card repair keeps hobgoblin role/voice instead of
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
  stockade stack presentation while preserving contact damage, moved Death's
  Hand back to a source-style same-item scythe toggle, and nudged the Goddess
  statue / Crystal Ball GUI transforms.
- Current parity polish tightened moving brew fluids, braziers, Spirit World
  fog, Death's Hand held-side rendering plus hit/toggle cues, Mystic Branch
  cast/fail feedback, Crystal Ball scry validation before altar-power charge,
  brew bucket gas/liquid tooltip feedback, Dream Weaver placement/wake cues,
  trap state sync, cursed warning cooldowns, Leonard's Urn focus release and
  capacity upgrades, Witch Hand Otherwhere cooldown/feedback, Duplication
  Grenade mirror charging, Book of Biomes climate page stats,
  Wolftrap lured-wolf steering/capture feedback, Parasytic Louse payload cues,
  Sun Collector/Sun Grenade feedback, and compass scrying cues. Manual QA still
  needs eyes on the Death Hand pose and in-world atmosphere feel.
- A first Witch's Triangle scanner slice is now live: a rowan-framed wooden
  triangle that uses a two-arm hold pose, lights its centre with purple portal
  art while scanning, remembers unique scanned block/entity ids on the item, and
  counts simple research points for those discoveries.

## Docs Cleanup Note

Use [../DOCS_INDEX.md](../DOCS_INDEX.md) as the routing contract before adding
new notes. Active docs should stay short; long session history belongs in
`docs/archive/CHANGELOG.md`.
