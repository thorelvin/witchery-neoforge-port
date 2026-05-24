# CLAUDE.md - Witchery NeoForge Port

You are helping build a NeoForge 1.21.11 fan port / continuation of the original Witchery mod.
This file is always loaded by Claude Code. Keep it short; read deeper docs only when relevant.

## Project stance

- This is an unofficial fan port / rebuild inspired by Witchery by Emoniph.
- All rights to original Witchery remain with Emoniph / the original rights holder.
- The project owner allows original Witchery behavior and decompiled reference material to be used as development reference where useful.
- Original textures/assets may exist as development placeholders; track and replace/review them before public release.
- Keep attribution. If a rights holder requests removal or changes, comply promptly.

## Target

- Minecraft: 1.21.11
- NeoForge: 21.11.42
- Java: 21
- Style: feature-faithful gameplay, modern NeoForge internals.

## Core rule

Implement one focused subsystem or bug fix at a time. Prefer working vertical slices over broad rewrites.

Before creating new code, inspect existing patterns for:
- registries, recipe types, block entities, menus, screens
- entity/model/renderer/layer registrations
- attachments/player data and networking payloads
- dimension/worldgen JSONs and config helpers

Reuse project patterns aggressively. Do not duplicate systems.

## Docs map

Read only the doc needed for the task:

- `docs/status/FEATURE_MATRIX.md` - current feature status.
- `docs/status/BUGS.md` - active bug list and priority order.
- `docs/status/NEXT_TASKS.md` - recommended next implementation slices.
- `docs/reference/PORTING_GUIDE.md` - quick implementation recipes and workflow.
- `docs/reference/API_MAP.md` - 1.7.10 Forge to NeoForge 1.21.11 API mapping reference.
- `docs/reference/PORTING_NOTES.md` - chronological API gotchas; search before guessing.
- `docs/reference/OBSCURE_MECHANICS.md` - obscure original behavior notes.
- `docs/roadmap/CLAUDE_HANDOFF_CURRENT_SCOPE.md` - current additive gameplay scope, naming decisions, and source priority for Witch Forest / curse / structure / mob expansion work.
- `docs/roadmap/PORTING_BACKLOG.md` - port backlog after current bugs.
- `docs/roadmap/FUTURE_FEATURES.md` - post-port expansion ideas; do not implement unless asked.
- `docs/legal/LEGAL_NOTES.md` - legal/release policy.

If docs disagree, use this priority:
1. current source code
2. `docs/status/BUGS.md`
3. `docs/status/FEATURE_MATRIX.md`
4. `docs/roadmap/CLAUDE_HANDOFF_CURRENT_SCOPE.md` when additive roadmap docs overlap
5. newest sections of `docs/reference/PORTING_NOTES.md`
6. `docs/reference/PORTING_GUIDE.md`
7. `docs/roadmap/PORTING_BACKLOG.md`
8. `docs/roadmap/FUTURE_FEATURES.md`

## Current progress summary

Mostly working:
- registries, creative tab, blocks/items, crops/plants baseline
- kettle, oven, distillery, spinning wheel, altar power
- 20+ rituals, taglocks, poppets, shelf
- player attachment/sync, infusions, vampirism, lycanthropy
- owl/cat/toad familiars, coven witch, witch hunter
- horror mobs, broom, Spirit World entry/exit, Torment placeholder dimension
- rowan tree worldgen

Still important:
- fix active rendering/spawn bugs
- Mutandis / Mutandis Extremis behavior
- brazier recipes, chalk variants, glyph blocks
- more rites toward full roster
- Dream Weaver items, Crystal Ball, Spirit World plants
- witch hut / apothecary structures, wild plant generation
- Torment maze + Lord of Torment + Flame Imp + Death entity
- creature powers / symbiosis
- polish: particles, sounds, config, guidebook

## Development workflow

For code tasks:
1. Read relevant source files first.
2. Read only relevant docs from the docs map.
3. Make the smallest coherent change.
4. Prefer editing over rewriting.
5. Run the narrowest useful build/test command when practical.
6. Update `FEATURE_MATRIX.md`, `BUGS.md`, or `PORTING_NOTES.md` when status changes or API gotchas are discovered.
7. Summarize briefly.

Do not:
- paste unchanged files
- rename packages/mod IDs unless asked
- introduce dependencies without explaining why
- implement future-feature systems during port work unless asked
- perform giant rewrites unless explicitly requested

## Local Ollama helper

A local MCP server named `ollama-code-helper` may be available.

Use it for narrow token-saving tasks, not final design judgment.

Good uses:
- `list_ollama_models` to see which local models are available before choosing a light or fallback model.
- `ast_grep_search` to find structural Java patterns before asking for reasoning.
- `extract_mechanics` to pull gameplay behavior from old 1.7.10 Witchery source.
- `port_forge_file` or `port_model_file` to create conservative first drafts.
- `review_port_against_source` to compare a generated draft against the old source and catch lost behavior.
- `review_feature_spec` to map a future-feature spec onto existing Witchery systems and find the safest first slice.
- `draft_feature_worklist` to turn a future-feature spec into an ordered implementation checklist Claude can execute.
- `summarize_resource_folder` to inventory JSON/model/worldgen/recipe patterns before adding new features.
- `explain_build_errors` to group Gradle/Java failures by likely root cause.

Rules:
- Claude must inspect actual source and diffs before accepting local-model output.
- Keep local-model outputs compact.
- Save useful summaries to `docs/local_analysis/` when that workflow is being used.
- Do not let the local model make architecture calls or broad redesign decisions.
- For future-feature work, a good sequence is `summarize_resource_folder` -> `review_feature_spec` -> `draft_feature_worklist` before Claude starts coding.

## Build/test

Prefer:
```bash
./gradlew compileJava
```

Use `./gradlew build` when resources, datagen, or pack output may be affected.
Do not claim success unless the command was actually run. If not run, say so.

## Coding standards

- Server owns gameplay truth; client only renders/displays synced state.
- Use data-driven JSON where useful: recipes, worldgen, dimensions, rituals, mutations, curses, brews.
- Use data components for item-bound data.
- Use attachments/saved data for entity/player/level state.
- Use custom payloads for networking.
- Keep client-only classes under client packages/events.
- Prefer small reusable helpers over repeated code.

## High-value implementation patterns

- New brew item: extend existing `BrewItem`, override effect method, add recipe/lang/model if needed.
- New rite: reuse `RitualExecutor` dispatch style unless converting to a data-driven registry as a separate milestone.
- New poppet: add enum/case in existing poppet event matching.
- New entity: register type, attributes, renderer/layer, spawn egg if useful, and spawn placement if biome-spawned.
- New dimension: add `dimension_type`, `dimension`, biome JSON, `ResourceKey<Level>`, teleport/return handling.
- New machine recipe: add one JSON under the existing custom recipe path.

## Rendering/worldgen cautions

- Verify PNG dimensions and original UV layout before changing `LayerDefinition.create(...)`.
- `ModelPart` divides by 16 internally; do not add old `scale(0.0625)`.
- Dimension biome `features` arrays must contain the correct generation-step sub-array structure.

## Art pipeline

`tools/img_to_asset.py` converts high-res AI-generated concept sheets into
32x32 and 16x16 Minecraft item textures (RGBA PNG, 1-px transparent border).

Key flags:
- `--grid 3x2` or `--grid auto` — split concept sheet into cells
- `--names name1,name2,...` — cell names left-to-right, top-to-bottom
- `--bg rembg` — AI background removal (best for parchment/textured BGs)
- `--save-manifest` — write a `_manifest.json` after the run for future batches
- `--skip` — don't reprocess already-exported files

Outputs go to `{out}/32x32/name.png` and `{out}/16x16/name.png` by default.
A `_preview_*.png` contact sheet is generated automatically for review.

Typical call:
```
python tools/img_to_asset.py sheet.png --grid 4x3 --bg rembg \
  --names "item_a,item_b,..." -o src/main/resources/.../textures/item \
  --save-manifest
```

Batch re-run:
```
python tools/img_to_asset.py --manifest docs/art/concepts/_manifest.json --skip
```

**`--grid auto` note:** works well for high-contrast items; falls back gracefully
for warm-toned/low-contrast items but may need an explicit grid in those cases.

## Response format

When editing files directly:
```md
## Changed
- `path` - reason

## Build/test
- Command:
- Result:

## Next
- one or two useful next steps
```

Keep explanations short.
