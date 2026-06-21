# Current Status

Last curated: 2026-06-21

This is the compact living handoff for the Witchery NeoForge port. Keep it short
enough to read before starting work. Long historical deltas belong in
`docs/archive/CHANGELOG.md` or a focused reference file.

## Project State

- Minecraft target: `1.21.11`
- NeoForge target: `21.11.42`
- Java target: JDK `21`
- Overall shape: playable active parity port, with many systems implemented and
  the remaining work concentrated in QA, source-feel polish, edge cases, and
  release hardening.
- Current estimates from the README: mechanical porting roughly `95-96%`;
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

- In-game QA for recently restored or polished systems: Flame Imp contracts,
  Vanishing Witch Hut, NBT structures, dream entry/exit, cursed well warnings,
  and custom brew/ritual edge cases.
- Source-feel polish on exact-ID items and wearables that are registered but
  still need model, texture, transform, or gameplay verification.
- Structure/worldgen tuning for NBT-backed huts, wells, villages, and Nether
  placements.
- Spirit World atmosphere and dream/nightmare behavior QA.
- Public-release hardening: docs clarity, launcher diagnostics, asset/legal
  review, packaging, and multiplayer/server checks.

## Recent High-Signal Changes

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

## Docs Cleanup Note

Use [../DOCS_INDEX.md](../DOCS_INDEX.md) as the routing contract before adding
new notes. Active docs should stay short; long session history belongs in
`docs/archive/CHANGELOG.md`.
