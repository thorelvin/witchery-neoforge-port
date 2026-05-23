# Witchery NeoForge Port

An unofficial modern NeoForge port and continuation of the classic Witchery mod for Minecraft `1.21.11`.

This project is focused on restoring the feel and mechanics of original Witchery first, then using Witchery: Resurrected and other references where they help close visual, item, or registry parity gaps.

## Current State

The port is playable and well beyond prototype stage, but it is still an active parity port rather than a polished release. Many systems are implemented, and many more are in the "works, needs in-game verification and edge-case parity" phase.

High-level estimate:

- Mechanical porting: roughly `70-75%`
- Release-ready parity/polish: roughly `55-65%`
- Best source of truth: [docs/status/FEATURE_MATRIX.md](docs/status/FEATURE_MATRIX.md)

The long tail is mostly old Witchery edge cases, in-game QA, final visuals, and family-by-family promotion of legacy placeholder items.

## Feature Highlights

Substantial restored or in-progress systems include:

- Altar power, kettle brewing, Witch's Oven, Distillery, Spinning Wheel, Brazier recipes, and early Witch's Cauldron work.
- Circle Magic with a broad restored rite roster, staged active rites, altar power costs, ring/chalk-family checks, curse routes, and many transaction/refund fixes.
- Poppets, taglocks, familiars, creature traps, belts, vampire clothing, and many source-backed item recipe paths.
- Vampirism and lycanthropy foundations, including recent vampire clothing set bonuses for feeding, fire survival, and mesmerise behavior.
- Spirit World gameplay loop, corpse/manifestation flow, native spirits, nightmare pressure, Spirit Portal behavior, and Spirit World vegetation.
- Torment dimension boss flow and legacy-style maze restoration, with server-side `/witchery verify torment` QA support.
- Mirror World work with mirror portal blocks, room generation, reflection guardian flow, and fresh-world/dimension-folder QA caveats.
- Many mobs and renderers restored or polished toward old Witchery visuals, including ravens, spirits, nightmares, flying monkeys, imps, Ents, Mandrakes, Banshees, Poltergeists, and others.
- Witches' Primer guidebook coverage for major restored progression paths.
- JEI and Patchouli are declared optional; dependency wiring may lag current Minecraft/NeoForge availability.

## Not Done Yet

Important remaining work includes:

- `221` legacy placeholder items are still intentionally inert TODO markers, even though their IDs/models are now visible and tracked.
- Some systems marked "Done" in status docs still need in-game verification, especially visual models, trap behavior, Torment/Mirror World fresh-dimension checks, and armor/equipment feel.
- Custom Brew parity is still an evolving area: dispersal tiers and scaling exist in the active work, but deeper original modifier behavior should remain under review.
- Mirror World needs careful QA around room generation, reflection behavior, stale generated chunks, and player-skin/equipment behavior.
- Village additions such as bookshops, guards, and walls remain deferred.
- Ritual execution is powerful but still carries old-queue/refund/edge-case parity risk.
- Public-release cleanup remains: licensing clarity, asset audit/replacement direction, packaging, balance passes, and broad multiplayer/server QA.

## Status Docs

Use these files when deciding what to work on next:

- [docs/status/FEATURE_MATRIX.md](docs/status/FEATURE_MATRIX.md) - feature-level parity matrix.
- [docs/status/NEXT_TASKS.md](docs/status/NEXT_TASKS.md) - current plan, quick verify items, and next-session priorities.
- [docs/status/BUGS.md](docs/status/BUGS.md) - active/fixed bug board and parity notes.
- [docs/status/ITEM_REVIEW.md](docs/status/ITEM_REVIEW.md) - item roster, placeholder tracking, and promotion decisions.
- [docs/reference/PORTING_NOTES.md](docs/reference/PORTING_NOTES.md) - chronological implementation notes and reference findings.

## Build

Requirements:

- Java `21`
- Minecraft `1.21.11`
- NeoForge `21.11.42`

Compile and process resources:

```powershell
.\gradlew.bat compileJava processResources
```

On Unix-like shells:

```bash
./gradlew compileJava processResources
```

Useful dev runs:

```powershell
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat runData
```

## Debug / QA Helpers

The mod includes a growing `/witchery` debug command suite. Useful current helpers include:

- `/witchery setup stations` - builds a stocked Rowan QA hut with core machines, altar, traders, chest supplies, kettle setup, Glowglobes, and test layout.
- `/witchery verify torment` - audits the restored Torment maze footprint and atmosphere server-side.
- `/witchery spawn <entity>` - spawns registered Witchery entities for model/behavior QA.
- `/witchery spawn wickerman` - places a summon-valid bloodied Wicker Man structure for Horned Huntsman testing.
- `/witchery ritual hitchcock [count]` - releases a raven flock without consuming a brew.
- `/witchery dream`, `/witchery torment`, `/witchery mirror`, and `/witchery exit` - dimension QA shortcuts.
- `/witchery vampire`, `/witchery werewolf`, `/witchery blood`, `/witchery infusion`, `/witchery familiar`, and `/witchery status` - player-state QA helpers.

## Mirror / Torment QA Notes

Custom dimension chunk generators do not rewrite chunks that already exist on disk. After changing Mirror World or Torment generation code, test with a fresh world or delete the relevant dimension folder under the run save before verifying layout changes.

For Mirror World specifically, stale generated chunks can make new room logic appear broken. Break and replace the source mirror block if you also need to clear its saved room assignment.

## Contributing Workflow

Recommended loop:

1. Check [docs/status/NEXT_TASKS.md](docs/status/NEXT_TASKS.md) and [docs/status/FEATURE_MATRIX.md](docs/status/FEATURE_MATRIX.md).
2. Compare the port against original/decompiled behavior before choosing a bounded task.
3. Implement one coherent parity slice or bug fix.
4. Run `.\gradlew.bat compileJava processResources`.
5. Update status docs to match repository truth.
6. Commit meaningful progress with a clear message.

Keep unrelated local art experiments, temp folders, or generated scratch files out of parity commits unless the task explicitly includes them.

## Project Notes

- This is a fan port inspired by the original Witchery mod by Emoniph.
- Original Witchery behavior and decompiled reference material are used as porting references where needed.
- The long-term art direction is to move toward original replacement art with the same gameplay identity and mood, rather than treating old assets as the final public-release target.
- This repository is not an official Witchery release.
