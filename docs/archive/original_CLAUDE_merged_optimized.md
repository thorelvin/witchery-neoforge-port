# CLAUDE.md — Witchery NeoForge Port

You are helping build a modern NeoForge 1.21.1 fan port / continuation of the original Witchery mod.

This file is always loaded by Claude Code. Keep it short. Do not import large docs here with `@...`; read the relevant docs only when needed.

## Project stance

- This is an unofficial fan port / rebuild inspired by Witchery by Emoniph.
- All rights to the original Witchery mod remain with Emoniph / the original rights holder.
- The project owner allows original Witchery behavior and decompiled reference material to be used for development reference where useful.
- Original textures/assets may exist as development placeholders, but must be tracked and replaced/reviewed before public release.
- Do not remove attribution. If a rights holder requests removal or changes, comply promptly.

## Target

- Minecraft: 1.21.1
- NeoForge: 21.1.229
- Java: 21
- Style: feature-faithful where useful, modern NeoForge internals where practical.

## Core rule

Implement one focused subsystem or bug fix at a time. Prefer working vertical slices over broad rewrites.

Before creating anything new, inspect existing code and docs for an existing pattern:
- registries
- recipe types
- block entity/menu/screen patterns
- entity/model/renderer patterns
- attachments/player data
- networking payloads
- dimension/worldgen JSONs
- config helpers

Reuse project patterns aggressively.

## Docs map

Read only the doc relevant to the task:

- `docs/FEATURE_MATRIX.md` — current feature status.
- `docs/bugs.md` — active bug list and priority order.
- `docs/PORTING_GUIDE.md` — quick how-to patterns and 1.7.10 → NeoForge API map.
- `docs/PORTING_NOTES.md` — session log and API gotchas; search this before guessing APIs.
- `docs/PORTING_PLAN.md` — large historical roadmap and deep behavior notes; do not treat old status sections as current.
- `docs/FUTURE_FEATURES.md` — post-port expansion ideas; do not implement during faithful port unless asked.
- `docs/LEGAL_NOTES.md` — project legal/release policy.

If docs disagree, use this priority:
1. current source code
2. `docs/bugs.md`
3. `docs/FEATURE_MATRIX.md`
4. newest sections of `docs/PORTING_NOTES.md`
5. `docs/PORTING_GUIDE.md`
6. `docs/PORTING_PLAN.md`
7. `docs/FUTURE_FEATURES.md`

## Current known progress

Already mostly working:
- registries, creative tab, blocks/items
- crops/plants baseline
- kettle, oven, distillery, spinning wheel
- altar power
- 20+ rituals
- taglocks, poppets, shelf
- player attachment/sync
- vampirism and lycanthropy
- familiars
- coven witch, witch hunter
- horror mobs
- broom
- Dream World and Torment dimensions
- rowan tree worldgen

Still important:
- fix active rendering/spawn bugs
- Mutandis / Mutandis Extremis behavior
- brazier recipes
- chalk variants and glyph blocks
- more rites toward full roster
- Dream Weaver items and Crystal Ball logic
- witch hut / apothecary structures
- wild plant generation
- Lord of Torment, Flame Imp, Death entity
- creature powers / symbiosis
- polish: particles, sounds, config, guidebook

## Development workflow

For code tasks:
1. Read the relevant source files first.
2. Read only the relevant docs from the docs map.
3. Make the smallest coherent change.
4. Prefer editing over rewriting.
5. Run the narrowest useful build/test command when practical.
6. Update `docs/FEATURE_MATRIX.md`, `docs/bugs.md`, or `docs/PORTING_NOTES.md` when the task changes status or discovers an API gotcha.
7. Summarize briefly.

Do not:
- paste unchanged files
- duplicate systems
- rename packages/mod IDs unless asked
- introduce dependencies without explaining why
- implement future-feature systems during port work unless asked
- perform giant rewrites unless explicitly requested

## Build/test

Prefer:
```bash
./gradlew compileJava
```

Use `./gradlew build` when resources/datagen/pack output might be affected.

Do not claim success unless the command was actually run. If not run, say so.

## Coding standards

- Server owns gameplay truth.
- Client code only renders/displays synced state.
- Use data-driven JSON where it helps modpack support: recipes, worldgen, dimensions, ritual definitions, mutation tables, curse/brew definitions.
- Use NeoForge data components for item-bound data.
- Use attachments/saved data for entity/player/level state.
- Use custom payloads for networking.
- Keep client-only classes under client-side packages/events.
- Prefer small reusable helpers over repeated code.

## Known high-value patterns

- New brew item: extend existing `BrewItem`, override effect method, add recipe/lang/model if needed.
- New rite: reuse `RitualExecutor` dispatch style unless converting to a data-driven registry as a separate milestone.
- New protection poppet: add enum/case in existing poppet event matching.
- New entity: register `EntityType`, attributes, spawn egg if useful, model/renderer/layer, spawn placement if biome-spawned.
- New dimension: add dimension_type, dimension, biome JSON, `ResourceKey<Level>`, teleport/return handling.
- New machine recipe: add one JSON under the existing custom recipe path.

## Model/rendering caution

When porting models, verify actual PNG dimensions and original UV layout before changing `LayerDefinition.create(...)`. Many rendering bugs are UV/texture-size mismatches.

## Dimensions/worldgen caution

Dimension biome `features` arrays must contain the correct vanilla generation-step sub-array structure. Do not use bare empty arrays if NeoForge/Minecraft expects 10 generation-step arrays.

## Response format

When editing files directly:
```md
## Changed
- `path` — reason

## Build/test
- Command:
- Result:

## Next
- one or two useful next steps
```

When only planning:
```md
## Recommendation
...

## Merge action
...

## Next Claude task
...
```

Keep explanations short.
