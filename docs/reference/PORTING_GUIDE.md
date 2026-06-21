# Porting Guide — Witchery 1.7.10 → NeoForge 1.21.11

This is the fast implementation manual. For docs routing, start with
`docs/DOCS_INDEX.md`. For exact API gotchas, search
`docs/reference/PORTING_NOTES.md` and `docs/reference/API_MAP.md`.

## Goal

Build a working, feature-faithful Witchery fan port on modern NeoForge 1.21.11.

- Preserve player-facing behavior where useful.
- Use modern NeoForge architecture where old APIs are obsolete.
- Reuse project patterns.
- Keep port work separate from post-port expansions.

## Working rules

1. One subsystem at a time.
2. Read current source before adding new code.
3. Use `docs/status/CURRENT.md` for the current handoff; old roadmap snapshots may be stale.
4. Use `BUGS.md` for active bugs and `QA_QUEUE.md` for active in-game verification.
5. After each coding milestone, run `./gradlew compileJava` when practical.
6. Add durable API lessons to `PORTING_NOTES.md`; put short session notes in `CURRENT.md`.
7. Update `FEATURE_MATRIX.md` only when feature-level status changes.
8. Do not delete assets/data/lang/textures unless explicitly told.
9. If unsure about a NeoForge API, inspect local Gradle/decompiled NeoForge source; do not guess.

## How to add common things

### New brew item
- Extend existing `BrewItem`.
- Override/apply the effect method.
- Add model, lang, recipe if needed.
- Add to creative tab/registry if not handled by existing pattern.

### New kettle recipe
- Add JSON under existing kettle recipe path.
- Use existing ingredients/items.
- Check altar power requirements.

### New oven/distillery/spinning recipe
- Add one JSON under the matching recipe folder.
- Only add Java if the recipe needs a new behavior type.

### New rite
- Add ingredient check in the correct `RitualExecutor.dispatch<Size>()`.
- Implement a private method or handler.
- Prefer existing conventions for particles, altar power, item consumption, and player messaging.
- Do not convert all rituals to data-driven in the same task unless explicitly asked.

### New protection poppet
- Add or reuse a `ProtectionType`.
- Add matching condition in poppet event handling.
- Ensure stack size is 1 if it stores player identity.

### New entity
- Register `EntityType`.
- Add attributes in MOD-bus attribute event.
- Add model/renderer/layer if custom.
- Add spawn egg only if useful for testing.
- Register spawn placement if biome-spawned.
- Add command support if current spawn command pattern supports it.

### New BESR/block item renderer
- Add one registration entry to the existing BEWLR/factory map.
- Add layer definition if custom model.
- Ensure block uses `.noOcclusion()` if it has cutouts/gaps.

### New dimension
- Add:
  - `data/witchery/dimension_type/<name>.json`
  - `data/witchery/dimension/<name>.json`
  - `data/witchery/worldgen/biome/<name>.json`
  - `ResourceKey<Level>` in dimension helper/init class
  - entry/exit teleport logic
- Biome `features` must use the expected generation-step sub-array structure.

### New structure/worldgen feature
- Prefer a small working structure first.
- Add biome modifier/placed feature/structure JSONs as appropriate.
- Keep structure spawn rates configurable later.

## Behavior preservation checklist

For every feature:
1. What does the player see/do?
2. What items/blocks/entities are involved?
3. What is consumed?
4. What is produced?
5. What state is saved?
6. Does it work on a dedicated server?
7. Which route in `docs/DOCS_INDEX.md` should be updated?

## Current major incomplete areas

See `docs/status/CURRENT.md`, `docs/status/NEXT_TASKS.md`, and
`docs/status/FEATURE_MATRIX.md` for details. High-value missing/partial areas:
- active render/spawn bug fixes
- Mutandis mutation behavior
- brazier recipes
- chalk variants and glyphs
- more rites
- Dream Weaver items
- Crystal Ball logic
- Spirit World plants/content
- witch hut/apothecary structures
- Torment boss/maze
- Mirror World
- creature powers

## Testing rules

- Code-only changes: run `./gradlew compileJava`.
- Resource/datapack changes: prefer `./gradlew build` or launch-data validation if available.
- Rendering/model changes: compile + in-game spawn/placement test.
- Dimension/worldgen changes: compile + create/enter a fresh test world when possible.

## Status update rule

When a task finishes:
- Check `docs/DOCS_INDEX.md` and update only the docs that truly changed.
- Move bugs from **Open** to **Fixed / Verify** in `BUGS.md`.
- Add active in-game retests to `QA_QUEUE.md`.
- Change feature rows in `FEATURE_MATRIX.md` only when the feature state changed.
- Add durable API discoveries to `PORTING_NOTES.md`; keep ordinary handoff notes in `CURRENT.md`.
- Add next small tasks to `NEXT_TASKS.md` if obvious.
