# Witchery NeoForge Port

> *The old craft, transcribed for a new age of Minecraft.*

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-5b8731)
![NeoForge](https://img.shields.io/badge/NeoForge-21.11.42-f16436)
![Java](https://img.shields.io/badge/Java-21-e8772e)
![Status](https://img.shields.io/badge/status-active%20parity%20port-6b4a8a)
![License](https://img.shields.io/badge/release-unofficial%20fan%20port-a23b3b)

An unofficial modern NeoForge port and continuation of the classic **Witchery** mod for Minecraft `1.21.11`.

The aim is to restore the *feel* and mechanics of original Witchery first — the altar's hum, the cauldron's patience, the weight of a rite — then lean on community fan-mod inspiration and other references where they help close visual, item, or registry parity gaps.

## Contents

- [Current State](#current-state)
- [Feature Highlights](#feature-highlights)
- [Not Done Yet](#not-done-yet)
- [Status Docs](#status-docs)
- [Build](#build)
- [Debug / QA Helpers](#debug--qa-helpers)
- [Mirror / Torment QA Notes](#mirror--torment-qa-notes)
- [Contributing Workflow](#contributing-workflow)
- [Credits](#credits)
- [Project Notes & Legal](#project-notes--legal)

## Current State

The port is playable and well beyond prototype stage, but it remains an active parity port rather than a polished release. Many systems are implemented, and many more are in the "works, needs in-game verification and edge-case parity" phase. Recent work has focused heavily on village generation stability, exact ID promotion, the native guidebook, and closing visible parity gaps from the old mod.

High-level estimate:

- Mechanical porting: roughly `80-85%`
- Release-ready parity/polish: roughly `60-70%`
- Best source of truth: [docs/status/FEATURE_MATRIX.md](docs/status/FEATURE_MATRIX.md)

The long tail is mostly old Witchery edge cases, in-game QA, final visuals, custom brew parity, and family-by-family promotion of remaining legacy placeholder items. The cauldron is lit; the brew is still thickening.

## Feature Highlights

Substantial restored or in-progress systems include:

- Altar power, kettle brewing, Witch's Oven, Distillery, Spinning Wheel, Brazier recipes, and Witch's Cauldron workings.
- Circle Magic with a broad restored rite roster, staged active rites, altar power costs, ring/chalk-family checks, curse routes, and many transaction/refund fixes.
- Poppets, taglocks, familiars, creature traps, belts, vampire clothing, and many source-backed item recipe paths.
- Vampirism and lycanthropy foundations, including vampire clothing set bonuses for feeding, fire survival, and mesmerise behavior.
- Spirit World gameplay loop, corpse/manifestation flow, native spirits, nightmare pressure, Spirit Portal behavior, and Spirit World vegetation.
- Torment dimension boss flow and legacy-style maze restoration, with server-side `/witchery verify torment` QA support.
- Mirror World work with mirror portal blocks, room generation, reflection guardian flow, and fresh-world/dimension-folder QA caveats.
- Many mobs and renderers restored or polished toward old Witchery visuals — ravens, spirits, nightmares, flying monkeys, imps, Ents, Mandrakes, Banshees, Poltergeists, and others.
- The **Witches' Primer** as a native Modonomicon guidebook that grows as the witch does: every player is handed the Primer on first join, and crafting a topic book unlocks that chapter and reveals the next book's recipe — a guided path from Herbology through to the transformations. Station recipes (oven, distillery, kettle, cauldron) are shown in-book with live items and hover tooltips.
- Vanilla villages now receive Witchery village buildings and adaptive town walls: apothecaries, bookshops, witch huts, watch towers, keeps, wall guards, gatehouses, and biome-aware village materials are live and under active QA.
- A large exact-ID cleanup pass has promoted many source and community fan-mod aliases out of placeholder status, including guidebooks, hunter gear, brews, contracts, waystones, special wearables, utility tools, and several old food/ingredient items.
- Practical Ice World, Stockade, Shaded Glass, Pit Trap, Infinity Egg, Sun Collector, Silver Vat, Blood Crucible, and other block-family slices are restored or partially restored, with current QA notes in the status docs.
- The in-game guidebook is powered by **Modonomicon**; JEI integration is declared optional. Dependency wiring may lag current Minecraft/NeoForge availability.

## Not Done Yet

Every grimoire has blank pages. Important remaining work includes:

- Some legacy placeholder items are still intentionally inert TODO markers, even though their IDs/models are visible and tracked.
- Some systems marked "Done" in status docs are implementation slices rather than full old-Witchery parity, especially transformations, machines, custom brews, Torment/Mirror World fresh-dimension checks, and armor/equipment feel.
- Custom Brew parity is still an evolving area: Foulness/Foaming/Poison tier scaling exists, but original dynamic cauldron action composition, generic brew containers, and moving brew dispersals remain open.
- The latest parity-scan backlog is merged into [docs/status/NEXT_TASKS.md](docs/status/NEXT_TASKS.md), with status corrections reflected in [docs/status/FEATURE_MATRIX.md](docs/status/FEATURE_MATRIX.md).
- Mirror World needs careful QA around room generation, reflection behavior, stale generated chunks, and player-skin/equipment behavior.
- Village additions are live, but still need cross-biome visual QA, traversal checks, and continued tuning for walls, gatehouses, guard movement, and structure placement.
- Ritual execution is powerful but still carries old-queue/refund/edge-case parity risk.
- Public-release cleanup remains: licensing clarity, asset audit/replacement direction, packaging, balance passes, and broad multiplayer/server QA.

## Status Docs

Consult these before choosing what to work on next — they hold the project's true memory:

- [docs/status/FEATURE_MATRIX.md](docs/status/FEATURE_MATRIX.md) — feature-level parity matrix.
- [docs/status/NEXT_TASKS.md](docs/status/NEXT_TASKS.md) — current plan, quick verify items, and next-session priorities.
- [docs/status/BUGS.md](docs/status/BUGS.md) — active/fixed bug board and parity notes.
- [docs/status/ITEM_REVIEW.md](docs/status/ITEM_REVIEW.md) — item roster, placeholder tracking, and promotion decisions.
- [docs/reference/PORTING_NOTES.md](docs/reference/PORTING_NOTES.md) — chronological implementation notes and reference findings.

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

The mod includes a growing `/witchery` debug command suite (permission level 2). Useful current helpers include:

- `/witchery setup stations` — builds a stocked Rowan QA hut with core machines, altar, traders, chest supplies, kettle setup, Glowglobes, and test layout.
- `/witchery setup rite` — builds a ritual QA pad with white, golden, otherwhere, and infernal circle sets.
- `/witchery verify torment` — audits the restored Torment maze footprint and atmosphere server-side.
- `/witchery goto <structure>` — teleports to Witchery structures and vanilla village variants, with safer ticked village locate/prep behavior for village targets.
- `/witchery walls` and `/witchery walls reset` — generate or reset adaptive village walls around the nearest loaded village start.
- `/witchery spawn <entity>` — spawns registered Witchery entities for model/behavior QA.
- `/witchery spawn wickerman` — places a summon-valid bloodied Wicker Man structure for Horned Huntsman testing.
- `/witchery ritual hitchcock [count]` — releases a raven flock without consuming a brew.
- `/witchery guide unlock [all|<book>]` and `/witchery guide reset` — unlock or re-lock Witches' Primer chapters for guidebook QA.
- `/witchery dream`, `/witchery torment`, `/witchery mirror`, and `/witchery exit` — dimension QA shortcuts.
- `/witchery vampire`, `/witchery werewolf`, `/witchery blood`, `/witchery infusion`, `/witchery familiar`, and `/witchery status` — player-state QA helpers.

## Mirror / Torment QA Notes

Custom dimension chunk generators do not rewrite chunks that already exist on disk. After changing Mirror World or Torment generation code, test with a fresh world or delete the relevant dimension folder under the run save before verifying layout changes.

For Mirror World specifically, stale generated chunks can make new room logic appear broken. Break and replace the source mirror block if you also need to clear its saved room assignment.

## Contributing Workflow

Recommended loop — measure twice, cast once:

1. Check [docs/status/NEXT_TASKS.md](docs/status/NEXT_TASKS.md) and [docs/status/FEATURE_MATRIX.md](docs/status/FEATURE_MATRIX.md).
2. Compare the port against original/decompiled behavior before choosing a bounded task.
3. Implement one coherent parity slice or bug fix.
4. Run `.\gradlew.bat compileJava processResources`.
5. Update status docs to match repository truth.
6. Commit meaningful progress with a clear message.

Keep unrelated local art experiments, temp folders, or generated scratch files out of parity commits unless the task explicitly includes them.

## Credits

All credit for Witchery belongs to its creator, **Emoniph**. This port exists only because that original work was so good — the altars, the rites, the brews, the whole brooding world of the old craft are Emoniph's design, and every system here traces back to that vision. Enormous gratitude and respect to the original author. We are tending someone else's garden, not claiming it as our own.

## Project Notes & Legal

- This is an **unofficial fan port** inspired by the original Witchery mod by **Emoniph**. It is not affiliated with or endorsed by Emoniph, and it is not an official Witchery release.
- **All rights to original Witchery remain with Emoniph and/or the original rights holder.** Original art, textures, sounds, and other assets belong entirely to Emoniph; where any appear in this repository they are development placeholders only, to be replaced with original artwork before any public release.
- **The code is an independent reimplementation, rewritten against the modern NeoForge `1.21.11` API rather than copied.** None of the original mod's source is shipped or copied verbatim — it targets a long-obsolete Minecraft version and will not run on modern Minecraft. Original gameplay behavior, decompiled jars, and community ports are studied as references to keep the port faithful, in the same spirit as other community continuation projects.
- The long-term art direction is to move toward original replacement art that keeps the same gameplay identity and mood, rather than treating old assets as the final public-release target.
- If Emoniph or a legitimate rights holder requests removal or changes, the project will comply promptly.
