# QA Parity Plan

Last updated: 2026-05-19

This checklist is the quality gate for the NeoForge port. The goal is not just "feature present", but "behaves, sounds, and looks like original Witchery unless a modern runtime constraint forces a deliberate deviation."

`docs/status/NEXT_FIXES_PLAN.md` stays the ordered implementation roadmap. This file is the parity and verification harness used while working through that roadmap.

Current phase note: the obvious missing-core-rite sweep is nearly exhausted, and the highest-value QA work now shifts toward dispatcher accuracy, refund/upkeep fidelity, Spirit World feel verification, machine/storage GUI cleanup, and then the sound/UI/block-render audits that are easier to trust once rite coverage is no longer the main gap. Use Witchery Resurrected layouts/assets when they preserve the original result more faithfully on NeoForge, but keep original 1.7.10 mechanics and feel as the final judge.

Current overnight execution note:
- Use the 5-point unattended run order in `docs/status/NEXT_FIXES_PLAN.md`.
- Finish one focused subsystem at a time.
- When a visual issue can be solved by correcting a bad item/model/texture chain, do that before introducing a new custom renderer.
- When a subsystem needs a custom renderer, prefer matching original orientation, brightness, transparency, and inventory pose before adding extra effects.

## Reference hierarchy

1. Original Witchery 1.7.10 code and assets in `C:\claude\witchery\decompiled\witchery-original`.
2. The active NeoForge port in `C:\claude\witchery\witchery-port`.
3. Official modern modding references for the current runtime:
   - Neo/Forge sound registration and `sounds.json` workflow.
   - Working NeoForge 1.21.x renderer/menu/datapack patterns.
4. Other working modern mods are examples for API usage and structure only, never the source of truth for Witchery behavior.

## External reference set

Use these as implementation examples when the modern runtime differs from 1.7.10:

- Neo/Forge sound docs:
  - [Sounds](https://docs.minecraftforge.net/en/1.21.x/gameeffects/sounds/)
  - [Sound Providers](https://docs.minecraftforge.net/en/1.21.x/datagen/client/sounds/)
- Working NeoForge example repos:
  - [umfhero/WoodMek-NeoForge-1.21.1](https://github.com/umfhero/WoodMek-NeoForge-1.21.1)
  - [jeremiahwinsley/pylons](https://github.com/jeremiahwinsley/pylons)

These examples are especially useful for modern screen rendering, sound/event registration, menu syncing, data generation, and block entity rendering patterns.

## QA lanes

### 1. Mechanics parity

For each subsystem:

- Compare the original 1.7.10 class or recipe registry against the current port implementation.
- Preserve original rules first: activation conditions, ranges, timing, upkeep, inventory slot meaning, AI triggers, dimension rules, and failure cases.
- Only modernize structure, not outcomes. Safer helper methods, data-driven registries, and better separation are encouraged if behavior stays the same.

Priority mechanics checklist:

1. Ritual engine: activation traits, queued stages, upkeep, refunds, aborts, altar drain, and multi-pulse effects.
2. Machines: Distillery, Witch's Oven, Spinning Wheel, Kettle, Altar item interactions, and funnel/jar/byproduct edge cases.
3. Poppets and storage: Poppet Shelf, Leech Chest, Refilling Chest, Taglock flow, chunk-loading expectations.
4. Spirit World: Dream Catcher rules, Nightmare/Demonic Nightmare cadence, corpse rules, Flowing Spirit, plant interactions.
5. Blocks and plants: cotton, blood rose, brambles, grassper, glowglobe, crystal ball, coffin, brazier, fetish blocks.
6. Entities: broom flight, familiar powers, mob AI, renderer-linked gameplay expectations.

### 2. Sound parity

Audit every gameplay loop that should feel distinct in original Witchery:

- Ritual start, sustain, fail, and completion cues.
- Machine idle/working/output cues.
- Block interaction cues: Crystal Ball, Coffin, Brazier, Poppet Shelf, Dream Catcher, altar items, doors/locks.
- Entity cues: broom, familiars, mandrake, nightmares, boss loops, poppet triggers.

For each sound issue:

- Check whether the original used a Witchery sound asset, a vanilla event, or a cadence built from repeated vanilla sounds.
- Confirm the modern port has the right `SoundEvent`/`sounds.json` mapping and correct call site timing.
- Verify sound source, volume, pitch variance, and one-shot vs looped behavior.
- Keep placeholder vanilla sounds only when the original had no custom identity or when the custom asset is still missing.

### 3. Graphics and UI parity

Audit three layers separately:

1. World/block rendering:
   - Block entity renderers
   - translucency/cutout choice
   - placed-item overlays
   - facing/orientation
   - blockstate/model selection
2. Item/inventory rendering:
   - item JSON parents
   - missing textures
   - block item visuals
   - spawn eggs
   - broken wraps or wrong UV assumptions

Recent progress:
- Several broken machine/storage icons now use NeoForge 1.21 `special` item renderers backed by the actual Witchery models instead of invalid static block-model fallbacks.
- Chest-like item renders now point at local `textures/entity/chest/...` resources instead of broken root-level ids.
- Spawn eggs were moved back onto the vanilla `template_spawn_egg` parent after the local template path regressed into pink/black fallbacks.
3. GUI parity:
   - background textures
   - slot alignment
   - progress bars
   - title/inventory label placement
   - hidden or overdrawn layers

Open graphics focus areas right now:

- machine GUI alignment and art verification
- crystal ball translucency
- coffin item presentation
- alluring skull / demon heart item/block presentation
- remaining pink/black block item models

## Per-batch workflow

Every implementation batch should follow this loop:

1. Pick one subsystem from `NEXT_FIXES_PLAN.md`.
2. Find the original 1.7.10 source of truth.
3. Compare against the current port and write down the exact mismatch.
4. Check a modern NeoForge example or official docs only for API shape when needed.
5. Implement the smallest behavior-preserving fix that moves parity forward.
6. Run `./gradlew.bat compileJava processResources`.
7. Update `BUGS.md`, `FEATURE_MATRIX.md`, `ITEM_REVIEW.md`, and/or `NEXT_FIXES_PLAN.md` when the repo truth has changed.
8. Keep unresolved visual-only or feel-only issues documented instead of hand-waving them away.

## Regression checklist

After any meaningful batch, verify at least the affected lane:

### Mechanics smoke checks

- ritual activates only under original conditions
- failed activation does not consume the wrong items
- staged rites stop cleanly on broken circles or missing upkeep
- machine slots and outputs match original expectations
- storage blocks preserve or expose the same data the old mod did

### Sound smoke checks

- action produces a sound at the right phase
- repeated processes do not spam every tick
- ambient/idle sounds stop when the underlying state stops
- volume and source category feel sensible in-world

### Graphics smoke checks

- no missing textures
- no broken GUI UV sampling
- translucent blocks look translucent instead of solid/cutout artifacts
- block entity items/overlays render in the expected place
- block item model is recognisable in inventory and hand

## Success criteria

The port is "QA-stable" only when:

- the original mechanic can be explained and traced to matching port behavior,
- the sound cue is present or deliberately documented as deferred,
- the visual representation is recognisable and not misleading,
- and the remaining gap is explicitly tracked in the status docs instead of being rediscovered later.
