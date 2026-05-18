# CLAUDE.md — Witchery Fan Rewrite / NeoForge Project Instructions

You are helping build an unofficial fan rewrite / continuation of the original Witchery mod for modern Minecraft using NeoForge.

This file is the persistent instruction source for Claude Code. Follow it before making code changes.

---

## 1. Project stance

This is an unofficial fan project inspired by the original Witchery mod by Emoniph.

All rights to the original Witchery mod, including its name, code, assets, textures, sounds, models, concepts, mechanics, and related materials, belong to Emoniph and/or the original rights holder.

This project is a fan continuation / preservation / rewrite effort because the original mod is no longer maintained and the original author appears inactive or unreachable.

We do not claim ownership of the original Witchery mod or its intellectual property.

If Emoniph or any legitimate rights holder requests removal, alteration, or takedown of this project or any part of it, we will comply promptly.

---

## 2. Development goal

Build a modern NeoForge rewrite that preserves the player-facing spirit of Witchery while using current Minecraft and NeoForge architecture.

The goal is not a line-by-line 1.7.10 port. The goal is a working, maintainable, modern implementation.

Prioritize:

1. Working gameplay systems.
2. Modern NeoForge architecture.
3. Data-driven design.
4. Server-authoritative logic.
5. Reusable internal APIs.
6. Token-efficient, focused output.

---

## 3. Code reuse policy

The project owner allows reuse when it saves time or improves quality.

Claude may reuse:

- Code already present in this repository.
- Code written by the project owner.
- Existing helper classes and framework patterns in this project.
- Original Witchery behavior as a functional gameplay reference.
- Public descriptions, changelogs, wikis, player guides, and documented behavior.
- Official NeoForge examples and documented patterns.
- Compatible open-source code when the license is checked and noted.

Claude should still prefer modern NeoForge architecture over copying old 1.7.10 patterns directly when the old approach is obsolete.

When reusing external code, add a note in `THIRD_PARTY_NOTICES.md` or `docs/ATTRIBUTION.md` when appropriate.

Do not remove attribution to the original Witchery author.

---

## 4. Token-saving rules

Be concise. Do not waste tokens repeating the whole plan.

When responding:

- Do not summarize this file unless asked.
- Do not paste unchanged files.
- Output only files that changed.
- Prefer diffs or full changed files, depending on the task request.
- Keep explanations short.
- Do not explain basic NeoForge concepts unless asked.
- Do not generate broad theory when a code change is requested.
- If the task is too large, split it into the next smallest useful milestone.
- Ask at most one clarification question. If the likely answer is obvious, make a reasonable assumption and continue.
- Always check existing code before creating a parallel system.

Before creating a new class/system, look for existing:

- Registries
- Recipe frameworks
- Block entity base classes
- Menu/screen patterns
- Network payload helpers
- Data component helpers
- Attachment helpers
- Saved data helpers
- Config helpers
- Datagen providers

Reuse project patterns aggressively.

---

## 5. Output format for coding tasks

Use this format unless the user asks otherwise:

```md
## Files changed

- `path/to/File.java` — short reason
- `path/to/file.json` — short reason

## Code

[Only include changed files or diffs. Do not paste unchanged files.]

## Notes

- Short implementation notes only.
- Mention any TODOs or follow-up tasks.
```

If using Claude Code with direct file editing, make the file changes directly, then summarize only:

```md
## Changed

- `...`

## Build/test

- Command run: `./gradlew build`
- Result: pass/fail

## Next

- One or two next steps.
```

---

## 6. Build/test rules

When practical, run the relevant build/test command after code changes.

Prefer:

```bash
./gradlew build
```

For smaller checks, use the narrowest useful Gradle task available in the project.

If the build fails:

1. Fix obvious compile errors.
2. Rerun the relevant command.
3. If still failing, report the exact failure and the likely fix.

Do not claim success unless a build/test was actually run or the user explicitly asked not to run it.

---

## 7. Architecture rules

Use modern NeoForge patterns.

General rules:

- Server owns gameplay truth.
- Client displays synced state and visual effects.
- Keep client-only code out of common/server code.
- Use data generation where possible.
- Use JSON/datapack-driven definitions for systems that modpacks may customize.
- Avoid hardcoded magic numbers where configs or data files make sense.
- Keep systems modular and testable.

Prefer:

- Data components for item-bound custom data.
- Attachments or saved data for entity/player/level state.
- Custom recipe types for machines and brewing.
- Custom registries or reload listeners for rituals, mutations, curses, and brew effects.
- NeoForge biome modifiers/worldgen systems for world generation.
- Custom payload networking for client/server sync.

---

## 8. Package layout

Use a structure similar to this unless the existing project already has a clear convention:

```text
src/main/java/<package>/
  WitcheryReborn.java

  registry/
    ModBlocks.java
    ModItems.java
    ModBlockEntities.java
    ModEntities.java
    ModMenus.java
    ModCreativeTabs.java
    ModDataComponents.java
    ModAttachments.java
    ModEffects.java
    ModParticles.java
    ModSounds.java
    ModRecipes.java
    ModRituals.java
    ModWorldgen.java
    ModDimensions.java

  api/
    altar/
    ritual/
    brew/
    curse/
    mutation/
    familiar/
    transformation/

  common/
    block/
    blockentity/
    item/
    entity/
    recipe/
    ritual/
    brew/
    curse/
    altar/
    mutation/
    world/
    dimension/
    network/
    data/

  client/
    render/
    screen/
    model/
    particle/
    keybind/

  data/
    client/
    server/
```

Do not reorganize the entire project unless the user specifically asks.

---

## 9. Preferred implementation order

Implement systems in this order unless the user asks for a specific system:

1. NeoForge project skeleton and registries
2. Core materials, creative tab, datagen
3. Magical plants and trees
4. Witch oven and fumes
5. Altar power
6. Kettle brewing
7. Mutandis mutation
8. Circle ritual engine
9. Taglocks, poppets, and curses
10. Worldgen structures
11. Coven and familiar systems
12. Spirit World
13. Torment
14. Mirror World
15. Vampire progression
16. Werewolf progression
17. Bosses, polish, guidebook, compatibility

The first playable alpha should focus on:

- Magical trees/plants
- Mutandis
- Witch oven
- Fumes
- Altar
- Kettle
- Basic rituals

---

## 10. Core systems design

### 10.1 Magical ecology

Magical plants should mostly be obtained by mutation, rituals, or special dimensions rather than simple worldgen.

Implement:

- Rowan
- Alder
- Hawthorn
- Spanish moss
- Mandrake
- Belladonna
- Snowbell
- Water artichoke
- Wormwood
- Wispy/disturbed dream plants later

Mutation should be data-driven.

Example concept:

```json
{
  "type": "witchery_reborn:plant_mutation",
  "input": "#minecraft:small_flowers",
  "output": "witchery_reborn:spanish_moss",
  "weight": 5,
  "conditions": {
    "biome_tag": "#minecraft:is_forest"
  }
}
```

### 10.2 Witch oven and fumes

The oven should be an early progression machine.

Implement:

- Input slot
- Fuel slot
- Jar slot
- Output slot(s)
- Fume chance/output
- Data-driven oven recipes
- Proper server-side ticking
- Synced burn/cook progress

### 10.3 Altar power

The altar is the backbone of rituals and advanced brewing.

Implement:

- Altar block
- Altar block entity
- Nearby nature scanning
- Power capacity
- Recharge rate
- Power drain API
- Client sync for display

Expose a small API:

```java
public interface AltarPowerSource {
    int getAvailablePower();
    boolean consumePower(int amount);
    int getCapacity();
    BlockPos getCenter();
    int getRange();
}
```

### 10.4 Kettle brewing

Kettle recipes should be data-driven.

Support:

- Water/fluid requirement
- Heat source
- Ingredient sequence or shapeless ingredient list
- Altar power cost
- Output item/count
- Optional color/particle data

### 10.5 Circle rituals

Build a reusable ritual engine, not one-off hardcoded rituals.

Ritual definitions should support:

- Chalk circle pattern
- Circle size
- Required chalk type
- Altar power
- Time of day
- Moon phase
- Dimension
- Weather
- Coven size
- Item inputs
- Entity inputs
- Sacrifice conditions
- Effect handler

Example concept:

```json
{
  "id": "witchery_reborn:rite_of_forest",
  "circle": {
    "size": "medium",
    "chalk": "witchery_reborn:golden_chalk"
  },
  "cost": {
    "altar_power": 3000
  },
  "requirements": {
    "moon_phase": "full",
    "dimension": "minecraft:overworld"
  },
  "inputs": [
    "witchery_reborn:rowan_sapling",
    "witchery_reborn:breath_of_the_goddess"
  ],
  "effect": {
    "type": "witchery_reborn:grow_forest",
    "radius": 18
  }
}
```

### 10.6 Taglocks, poppets, curses

Use item data components for item-bound identity data.

Use entity/player attachments or saved data for active long-term effects.

Implement:

- Taglock item with target UUID/name
- Poppet item with owner UUID and poppet type
- Curse manager
- Curse removal ritual
- Death/damage hooks
- Remote targeting rules

### 10.7 World generation

Worldgen should add occult places, not flood the world with resources.

Implement:

- Abandoned witch shacks
- Coven camps
- Ritual circle ruins
- Village apothecary
- Village bookshop
- Rare cursed groves
- Rare graveyard/stone circle structures

Use data-driven worldgen and biome modifiers where practical.

### 10.8 Spirit World

Implement after core systems are stable.

Design:

- Separate dimension
- Similar terrain/topography to Overworld
- Separate or restricted inventory
- Dream/nightmare state
- Nightmare hunter entity
- Spirit-only resources
- Exit mechanics
- Manifestation/ghost mechanics later

### 10.9 Torment

Design:

- Maze dimension
- Boss progression
- Indestructible or highly resistant torment blocks
- Hidden loot
- Exit portal
- Death/escape handling

### 10.10 Mirror World

Design:

- Mirror pocket dimension or generated mirror rooms
- Reflection boss
- Mirror ownership
- Mirror-to-mirror links
- Escape failsafe
- Taglock disguise mechanics

---

## 11. Data-driven files to prefer

Make these datapack/config-driven where practical:

- Oven recipes
- Kettle recipes
- Cauldron recipes
- Distillery recipes
- Spinning wheel recipes
- Mutation tables
- Ritual definitions
- Brew effects
- Curse definitions
- Familiar variants
- Structure weights
- Dimension transfer rules
- Transformation progression rules

---

## 12. Documentation files to maintain

Keep these files updated as the project evolves:

```text
docs/IMPLEMENTATION_STATUS.md
docs/NEXT_TASKS.md
docs/ATTRIBUTION.md
THIRD_PARTY_NOTICES.md
```

If these files do not exist, create them when useful.

### `docs/IMPLEMENTATION_STATUS.md`

Track what is done, what is partially done, and what is broken.

Use this format:

```md
# Implementation Status

## Done

- [ ] Project skeleton
- [ ] Registries
- [ ] Magical trees
- [ ] Witch oven
- [ ] Altar power
- [ ] Kettle brewing
- [ ] Mutandis
- [ ] Ritual engine
- [ ] Taglocks
- [ ] Poppets
- [ ] Curses
- [ ] Worldgen
- [ ] Spirit World
- [ ] Torment
- [ ] Mirror World
- [ ] Vampires
- [ ] Werewolves

## Current focus

None yet.

## Known issues

None yet.

## Design decisions

None yet.
```

### `docs/NEXT_TASKS.md`

Keep a short prioritized list.

Do not turn this into a huge planning essay.

---

## 13. Attribution text

Use this where needed:

```md
## Original Witchery inspiration

This system is inspired by or based on the original Witchery mod by Emoniph.

Original author: Emoniph  
Original project: Witchery for Minecraft 1.7.10  
Status: Unofficial fan continuation / rewrite  
Removal policy: Will remove or alter on rights-holder request.
```

Suggested README disclaimer:

```md
# Witchery Reborn / Fan Rewrite

This is an unofficial fan project inspired by the original Witchery mod by Emoniph.

All rights to the original Witchery mod, including its name, concepts, code, assets, textures, sounds, models, and related materials, belong to Emoniph and/or the original rights holder.

This project is created as a fan continuation / preservation effort because the original mod is no longer maintained and the original author appears to be inactive or unreachable.

We do not claim ownership of the original Witchery mod or its intellectual property.

If Emoniph or any legitimate rights holder requests removal, alteration, or takedown of this project or any part of it, we will comply promptly.
```

---

## 14. Good task prompt pattern

When the user gives a task, interpret it as one focused implementation step.

Good format:

```text
Task: Implement the first version of the altar power system.

Read:
- CLAUDE.md
- docs/IMPLEMENTATION_STATUS.md
- docs/NEXT_TASKS.md

Goal:
Create an Altar block and block entity that stores power, scans nearby natural blocks, and exposes a small API other systems can use later.

Constraints:
- NeoForge
- Server-authoritative
- Data-driven where useful
- Do not implement rituals yet
- Do not paste unchanged files

Output:
- Changed files only
- Build/test result
- Short TODO list
```

Bad format:

```text
Port Witchery.
```

If the user gives a broad task like that, choose the next smallest milestone and implement it.

---

## 15. Claude Code behavior

When using Claude Code directly in the repository:

1. Inspect relevant files first.
2. Make the smallest coherent change.
3. Prefer editing over rewriting.
4. Run a build/test if practical.
5. Update status/task docs if relevant.
6. Summarize briefly.

Do not perform giant rewrites unless explicitly asked.

Do not create duplicate systems when one already exists.

Do not rename packages or mod IDs unless asked.

Do not introduce new dependencies without explaining why.

---

## 16. Current priority

Unless the user specifies otherwise, work toward this first playable alpha:

```text
The Witch's Garden Alpha

- Magical trees
- Mutandis
- Witch oven
- Fumes
- Altar
- Kettle
- Basic rituals
- Minimal guide/documentation
```

Build toward playable vertical slices, not isolated unfinished systems.
