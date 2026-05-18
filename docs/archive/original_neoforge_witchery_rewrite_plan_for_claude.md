# NeoForge Rewrite Plan for a Witchery-Inspired Mod

> Purpose: This document is intended to be fed to Claude/Codex as a planning and implementation guide for rewriting an old Witchery-style Minecraft mod for modern Minecraft using NeoForge.

## Important Legal / Design Boundary

This should be treated as a **clean-room rewrite**, not a direct port.

The original Witchery mod was released for Minecraft 1.7.10 and is not open source. Do not decompile, copy, or reuse original code, assets, textures, sounds, names, or implementation details unless explicit permission exists.

Use only:

- Public documentation
- Public changelogs
- Player-facing mechanics
- Wiki descriptions
- Original design work
- New code and new assets

The goal is to preserve the **design spirit** of Witchery: occult progression, plants, rituals, brews, curses, spirits, dimensions, transformations, covens, and world manipulation.

---

# 1. Target Platform

## Recommended Target

Use a current NeoForge version rather than old Forge.

```md
Primary target: Current Minecraft + NeoForge
Fallback stable target: Latest widely adopted NeoForge LTS-style version
Architecture: data-driven, server-authoritative, multiplayer-safe
```

Exact version should be decided at project creation time based on NeoForge MDK availability and modpack target.

## General Requirements

- Java version required by selected NeoForge MDK
- Gradle-based NeoForge project
- Dedicated server compatibility from day one
- Datagen enabled from the beginning
- Data-driven recipes, rituals, mutations, and brews
- Configurable worldgen and balance values
- No client-only logic controlling gameplay results

---

# 2. Core Rewrite Philosophy

The original mod should be rebuilt as a set of modern systems, not copied system-for-system from 1.7.10.

Modern replacements:

```md
Old NBT-heavy item state       -> Data Components
Old entity extended data       -> Data Attachments / synced entity data
Hardcoded recipes              -> Custom Recipe Types + JSON
Hardcoded rituals              -> Data-driven ritual registry
Manual worldgen hacks          -> Configured features, placed features, structures, biome modifiers
Old packet systems             -> NeoForge custom payloads
Old capabilities where needed  -> Modern attachments/components/service APIs
```

Core design principle:

```md
The world should not merely contain magic.
Magic should gradually rewrite the world.
```

---

# 3. Suggested Package Structure

```text
src/main/java/com/yourname/witchcraft/
  WitchcraftMod.java

  registry/
    ModBlocks.java
    ModItems.java
    ModEntities.java
    ModBlockEntities.java
    ModMenuTypes.java
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
    dimension/

  common/
    block/
    item/
    entity/
    blockentity/
    recipe/
    ritual/
    brew/
    curse/
    altar/
    world/
    dimension/
    village/
    network/
    attachment/

  client/
    render/
    screen/
    model/
    particle/
    keybind/

  data/
    server/
    client/
```

## Recommended System Managers

```md
- AltarPowerSystem
- RitualSystem
- BrewSystem
- MutationSystem
- CurseSystem
- TaglockSystem
- PoppetSystem
- CovenSystem
- FamiliarSystem
- TransformationSystem
- SpiritWorldSystem
- TormentSystem
- MirrorSystem
```

Each major gameplay feature should have a clear API/service boundary so future features do not become tightly coupled.

---

# 4. Development Roadmap

## Phase 1 — Foundation Mod

Goal: Create the base mod and establish all core registries, datagen, resources, and development conventions.

Implement:

```md
- Mod ID and main mod class
- Deferred registers or NeoForge equivalent registry setup
- Creative tab
- Common config and server config
- Datagen pipeline
- Language generation
- Block/item model generation
- Loot table generation
- Recipe generation
- Tags
- Basic materials
- Basic magical plants
- Basic magical trees
```

Suggested first content:

```md
- Rowan tree
- Alder tree
- Hawthorn tree
- Spanish moss
- Mandrake root
- Belladonna
- Water artichoke
- Wormwood
- Clay jar
- Wood ash
- Exhale of the Horned One-inspired item
- Breath of the Goddess-inspired item
```

Milestone:

```md
A player can load the mod, access the creative tab, place basic plants/blocks, and obtain generated loot/recipes.
```

---

## Phase 2 — Witches’ Oven and Fumes

Goal: Implement the first processing machine and the early ingredient chain.

Implement:

```md
- Witches’ oven block
- Oven block entity
- Fuel handling
- Input/output slots
- Clay jar slot
- Fume byproduct system
- Data-driven oven recipes
- Server-side ticking
- Client GUI/menu
- Progress sync
```

Example recipe idea:

```json
{
  "type": "witchcraft:oven_fuming",
  "input": "minecraft:oak_sapling",
  "jar_required": true,
  "cooking_time": 200,
  "result": {
    "id": "minecraft:charcoal",
    "count": 1
  },
  "byproduct": {
    "id": "witchcraft:breath_of_the_goddess",
    "chance": 0.25
  }
}
```

Milestone:

```md
Player can process saplings/wood/plants into magical fumes and early crafting components.
```

---

## Phase 3 — Garden and Mutation System

Goal: Make magical ecology player-driven rather than purely world-generated.

Implement:

```md
- Mutandis-like item
- Stronger mutation item
- Mutation target registry
- Weighted mutation tables
- Biome-sensitive mutation rules
- Block tag support
- Configurable mutation chances
- Optional progression locks
```

Example data format:

```json
{
  "type": "witchcraft:plant_mutation",
  "input": "#minecraft:flowers",
  "output": "witchcraft:spanish_moss",
  "weight": 5,
  "requires_biome_tag": "#minecraft:is_forest"
}
```

Design rules:

```md
- Do not spawn every magical plant naturally.
- Let players create magical plant lines through mutation.
- Make rare magical trees feel discovered or cultivated.
- Allow modpack makers to add/remove mutation outputs with datapacks.
```

Milestone:

```md
Player can unlock the magical plant ecosystem through mutation instead of mining or random exploration.
```

---

## Phase 4 — Altar Power System

Goal: Build the central energy system used by rituals, brews, curses, and late-game magic.

Implement:

```md
- Altar block
- Altar block entity
- Multiblock or nearby-block detection
- Natural power scanning
- Capacity
- Recharge rate
- Drain/consume API
- Range calculation
- Tooltip or display item
- Optional altar upgrades
- Server-side power cache
```

Possible power sources:

```md
- Leaves
- Logs
- Grass
- Flowers
- Crops
- Mushrooms
- Water
- Magical trees
- Skulls or candles as upgrades
```

Suggested API:

```java
public interface AltarPowerSource {
    int getAvailablePower();
    boolean consumePower(int amount);
    BlockPos getCenter();
    int getRange();
}
```

Milestone:

```md
Altars detect nearby nature, generate/store power, and can pay costs for future systems.
```

---

## Phase 5 — Kettle Brewing

Goal: Implement simple early-mid game brewing before the full custom brew system.

Implement:

```md
- Kettle block
- Kettle block entity
- Water or fluid requirement
- Heat source detection
- Ingredient insertion
- Timed brewing
- Bottle extraction
- Altar power requirement
- Custom recipe type
- Particle/sound feedback
```

Example recipe:

```json
{
  "type": "witchcraft:kettle_brewing",
  "altar_power": 250,
  "heat_required": true,
  "fluid": "minecraft:water",
  "ingredients": [
    "witchcraft:breath_of_the_goddess",
    "minecraft:red_mushroom",
    "minecraft:glowstone_dust"
  ],
  "result": {
    "id": "witchcraft:brew_of_sprouting",
    "count": 3
  }
}
```

Milestone:

```md
Player can brew basic potions/items using a kettle, heat, water, ingredients, and altar power.
```

---

## Phase 6 — Circle Magic / Ritual Engine

Goal: Build the central ritual framework before adding many individual rituals.

Do not hardcode every ritual as a giant if-statement.

Core concepts:

```md
- Chalk/glyph blocks
- Circle shape scanner
- Ritual center block or activation item
- Altar power requirement
- Item inputs
- Entity inputs
- Block/environment requirements
- Coven requirements
- Time-of-day requirements
- Moon phase requirements
- Dimension requirements
- Ritual effect handler
```

Example ritual JSON:

```json
{
  "id": "witchcraft:rite_of_forest",
  "circle": {
    "type": "witchcraft:medium",
    "chalk": "witchcraft:golden_chalk"
  },
  "cost": {
    "altar_power": 3000,
    "experience": 0
  },
  "requirements": {
    "moon_phase": "full",
    "dimension": "minecraft:overworld"
  },
  "inputs": [
    "witchcraft:rowan_sapling",
    "witchcraft:breath_of_the_goddess"
  ],
  "effect": {
    "type": "witchcraft:grow_forest",
    "radius": 18
  }
}
```

Initial ritual effects:

```md
- Grow forest
- Change weather
- Summon coven witch
- Bind familiar
- Teleport to waystone
- Remove curse
- Open spirit portal
- Summon boss
```

Milestone:

```md
At least five different rituals work using the same data-driven engine.
```

---

## Phase 7 — Networking and Sync

Goal: Make all gameplay safe on dedicated servers.

Networking needed for:

```md
- GUI buttons
- Altar power display
- Kettle/oven progress
- Ritual particles
- Ability keybinds
- Vampire/werewolf ability activation
- Spirit World transition effects
- Mirror transition effects
- Player transformation sync
- Curse/taglock targeting UI
```

Rules:

```md
- Server owns truth.
- Client requests actions.
- Server validates all actions.
- Client displays results only after server confirmation.
- Never let client-side state decide ritual success, inventory changes, curses, or teleportation.
```

Milestone:

```md
GUIs, rituals, and player abilities work correctly on a dedicated server.
```

---

## Phase 8 — Taglocks, Poppets, and Curses

Goal: Implement remote sympathetic magic.

Use data components for items and attachments for player/entity state.

Data components:

```md
witchcraft:taglock_target
witchcraft:poppet_owner
witchcraft:poppet_type
witchcraft:bound_waystone
witchcraft:blood_sample
witchcraft:brew_effects
```

Attachments:

```md
witchcraft:active_curses
witchcraft:familiar_bond
witchcraft:coven_data
witchcraft:vampire_data
witchcraft:werewolf_data
witchcraft:spirit_state
```

Implement:

```md
- Taglock kit item
- Taglock item storing target UUID/name
- Poppet binding
- Death-protection poppet
- Damage-redirection poppet
- Curse application ritual
- Curse ticking
- Curse removal ritual
- Configurable PvP protections
```

Milestone:

```md
A player can collect a taglock, bind a poppet, apply a simple curse, and remove it through a ritual.
```

---

## Phase 9 — World Generation

Goal: Add occult structures and village integration without overwhelming vanilla terrain.

Worldgen should reinforce the theme but not replace core progression.

Generate:

```md
- Abandoned witch shacks
- Stone ritual circle ruins
- Straw effigies / scarecrow structures
- Coven witch camps
- Apothecary village buildings
- Bookshop village buildings
- Guard towers or fortified village pieces
- Rare cursed groves
- Rare graveyards
- Rare fairy-ring mushroom circles
```

Use:

```md
- Configured features
- Placed features
- Structures
- Structure sets
- Biome modifiers
- Biome tags
- Spawn placement rules
```

Village systems:

```md
- Apothecary villager
- Bookshop villager
- Sleeping villager hooks
- Vampire feeding interactions
- Witch hunter attacks
- Coven rumors or quests
```

Milestone:

```md
New worlds contain rare occult landmarks and useful village additions.
```

---

## Phase 10 — Spirit World Dimension

Goal: Implement the main dream/survival dimension.

Design:

```md
- Separate dimension
- Similar terrain/topography to Overworld
- Shared multiplayer dream dimension
- Separate inventory or restricted inventory transfer
- Dream/nightmare player state
- Nightmare hunter entity
- Dream-only plants/resources
- Spirit fluid/resource
- Circle magic disabled or limited
- Special escape/return item
- Spirit portal system
- Ghost manifestation into Overworld
```

Player flow:

```md
1. Player uses sleeping brew/item.
2. Player enters Spirit World.
3. Normal inventory is stored or restricted.
4. Player gathers dream-only resources.
5. Player survives nightmare conditions.
6. Player crafts or finds return method.
7. Only specific spirit items can return to Overworld.
```

Technical tasks:

```md
- Dimension JSON
- Chunk generator/noise settings
- Inventory snapshot storage
- Spirit-state attachment
- Teleport manager
- Death/exit handling
- Cross-dimensional item whitelist
- Nightmare spawning
- Dream/nightmare sync
```

Milestone:

```md
Player can enter the Spirit World, survive, gather special resources, and return safely.
```

---

## Phase 11 — Torment Dimension

Goal: Build the boss/punishment maze dimension.

Design:

```md
- Procedural maze dimension
- Indestructible torment blocks
- Hidden loot rooms
- Boss arena
- Torment portal exit
- Death returns player to Overworld
- Later magic can send enemies to Torment
```

Boss flow:

```md
1. Player summons Torment boss in Overworld.
2. Boss fight begins normally.
3. At half health, boss and player are pulled into Torment.
4. Player must finish boss or escape maze.
5. Portal/death returns player.
```

Technical tasks:

```md
- Maze generator
- Dimension definition
- Boss teleport logic
- Exit portal block
- Death handling
- Loot room placement
- Structure protection rules
```

Milestone:

```md
Player can fight a boss that transitions into a maze dimension and escape afterward.
```

---

## Phase 12 — Mirror World / Mirror Pockets

Goal: Build mirror interiors, reflection fights, and disguise/teleport mechanics.

Design:

```md
- Magic mirror block
- Mirror pocket dimension or generated room system
- Reflection entity
- First-entry reflection boss fight
- Claimed mirror state
- Mirror-to-mirror short-range teleport
- Optional mirror networks
- Taglock-based disguise mechanic
- Reflection decoy item/entity
- Escape chant or failsafe command
```

Data model:

```md
MirrorBlockEntity:
- owner UUID
- claimed state
- pocket ID
- linked mirror position
- contained reflection state

MirrorPocket:
- pocket ID
- dimension/position
- owner
- room template
- allowed exits
```

Milestone:

```md
Player can enter a mirror, defeat a reflection, claim the mirror, and use it for travel or disguise mechanics.
```

---

## Phase 13 — Familiars and Covens

Goal: Build companion and NPC magic systems.

Familiars:

```md
- Cat
- Owl
- Toad
- Raven/crow
- Wolf-like option
- Familiar binding ritual
- Familiar bonus effects
- Familiar death/protection rules
```

Covens:

```md
- Coven witch NPC
- Recruitment conditions
- Coven capacity
- Coven-based ritual power
- Coven member teleport/summon
- Coven reputation or trust
```

Milestone:

```md
Player can bind a familiar and recruit coven witches that empower rituals.
```

---

## Phase 14 — Transformations: Vampire and Werewolf

Goal: Add late-game transformation progression trees.

Do this late because transformations touch many systems.

Vampire data:

```md
- Level
- Blood amount
- Sire/source
- Sunlight weakness
- Feeding cooldown
- Ability unlocks
- Coffin/home position
- Village/NPC reactions
```

Werewolf data:

```md
- Level
- Wolf form state
- Rage
- Moon phase behavior
- Silver weakness
- Ability unlocks
- Hunter aggression
```

Needed systems:

```md
- Attribute modifiers
- Custom damage rules
- Rendering/model swaps
- Ability keybinds
- Feeding interactions
- Weaknesses
- Level progression
- Configurable PvP/server rules
- Sync to client
```

Milestone:

```md
Player can become a vampire or werewolf, level up, gain abilities, and suffer meaningful weaknesses.
```

---

## Phase 15 — Guidebook and Integration

Goal: Make the mod playable without external wiki dependency.

Implement:

```md
- Patchouli or custom guidebook
- In-game progression chapters
- Ritual diagrams
- Kettle recipe hints
- Mutation hints
- Spirit World survival guide
- Boss summoning clues
- JEI/EMI recipe integration
- Jade/The One Probe integration if desired
```

Milestone:

```md
Players can understand progression using in-game documentation.
```

---

# 5. Suggested Release Milestones

## Alpha 1 — The Witch’s Garden

```md
- Magical trees
- Magical plants
- Mutandis-like mutation
- Witches’ oven
- Fumes
- Clay jars
- Early recipes
- Basic guidebook
```

## Alpha 2 — Altar and Kettle

```md
- Altar power
- Kettle brewing
- Basic brews
- Heat/fluid handling
- First altar-powered recipes
```

## Alpha 3 — Circle Magic

```md
- Chalk circles
- Ritual engine
- 10–15 rituals
- Familiar binding
- Simple curses
- Basic coven witch
```

## Alpha 4 — The World Watches

```md
- Abandoned shacks
- Ritual ruins
- Coven camps
- Village apothecaries
- Village bookshops
- Occult worldgen config
```

## Alpha 5 — Dreams and Nightmares

```md
- Spirit World
- Dream/nightmare states
- Nightmare entity
- Dream-only resources
- Spirit portals
- Manifestation mechanics
```

## Alpha 6 — Torment and Mirrors

```md
- Torment maze dimension
- Torment boss
- Mirror pockets
- Reflection boss
- Mirror teleporting
```

## Beta 1 — Blood and Moon

```md
- Vampire progression
- Werewolf progression
- Hunters
- Weaknesses
- Abilities
- Multiplayer balancing
```

---

# 6. Data-Driven Systems

Make these reloadable or datapack-friendly from the beginning:

```md
- Oven fume recipes
- Kettle recipes
- Cauldron recipes
- Distillery recipes
- Spinning wheel recipes
- Ritual definitions
- Mutation tables
- Familiar variants
- Curse definitions
- Brew effects
- Structure weights
- Dimension transfer rules
- Vampire/werewolf progression values
- Altar power values
```

Avoid hardcoding content unless it is truly structural.

---

# 7. Implementation Prompts for Claude

## Prompt 1 — Project Skeleton

```text
Create a modern NeoForge mod skeleton for a clean-room Witchery-inspired mod.
Use package com.yourname.witchcraft.
Add registry classes for blocks, items, block entities, entities, menus, data components, attachments, recipe serializers, creative tabs, effects, sounds, particles, dimensions, and rituals.
Set up datagen providers for blockstates, item models, loot tables, recipes, tags, and language files.
Do not implement gameplay yet. Focus on clean architecture.
```

## Prompt 2 — Magical Plants and Trees

```text
Implement magical plants and three magical tree types: rowan, alder, and hawthorn.
Include saplings, leaves, logs, planks, stripped logs, wood, signs/buttons/doors if reasonable, loot tables, blockstates, item models, and tags.
Use datagen wherever possible.
Make the tree system easy to extend with more magical woods later.
```

## Prompt 3 — Oven and Fume Recipes

```text
Implement a Witches' Oven block entity with input slots, fuel, output, clay jar slot, progress tracking, and data-driven fume byproduct recipes.
Add a custom recipe type called oven_fuming.
The server should own all cooking logic.
The client GUI should display progress and burn time.
```

## Prompt 4 — Mutation System

```text
Implement a Mutandis-style plant mutation system.
Create a data-driven mutation recipe format where input block tags can transform into weighted output blocks.
Support biome requirements, random weights, and config values.
Add an item that applies mutation when used on valid blocks.
```

## Prompt 5 — Altar Power

```text
Implement an altar block entity that scans nearby natural blocks, calculates max power and recharge rate, stores power, and exposes an AltarPowerSource API.
The altar should support server-side ticking, client sync for display, and configurable scanning radius.
```

## Prompt 6 — Ritual Engine

```text
Implement a data-driven circle ritual engine.
It should scan chalk/glyph blocks around an activation point, validate circle shape, check item inputs, altar power, dimension, moon phase, time, and coven requirements, then dispatch a ritual effect handler.
Implement at least three effect handlers: change_weather, grow_forest, and teleport_to_waystone.
```

## Prompt 7 — Taglocks, Poppets, and Curses

```text
Implement taglock items using data components to store target UUID/name.
Implement poppet items using data components to store owner UUID and poppet type.
Implement active curses using entity/player attachments.
Add one simple curse and one simple protective poppet as proof of concept.
```

## Prompt 8 — Occult Worldgen

```text
Implement occult world generation using modern NeoForge worldgen systems.
Add abandoned witch shacks, small stone ritual ruins, and rare coven camps.
Use biome tags and configurable spawn weights.
Keep structures rare and avoid disrupting vanilla terrain.
```

## Prompt 9 — Spirit World

```text
Implement a Spirit World dimension.
It should use terrain similar to the Overworld, isolate or restrict inventory transfer, track dream/nightmare state per player, spawn a Nightmare entity during nightmare state, and allow return through a specific item or portal.
Design it to be multiplayer-safe.
```

## Prompt 10 — Torment Dimension

```text
Implement a Torment dimension as a procedural maze with indestructible torment blocks, hidden loot rooms, a boss arena, and an exit portal.
Add boss logic where a boss fight starts in the Overworld and transitions to Torment at half health.
```

## Prompt 11 — Mirror Pockets

```text
Implement a magic mirror block with a pocket interior system.
On first entry, spawn a Reflection entity.
After defeating the reflection, the player claims the mirror.
Add mirror-to-mirror linking and an emergency escape command or chant.
```

## Prompt 12 — Transformations

```text
Implement foundation data structures for vampire and werewolf transformations using player attachments.
Do not build the full progression yet.
Create synced state, attribute hooks, ability unlock placeholders, and config values for weaknesses.
```

---

# 8. Technical Risk List

## Risk: Dimensions Too Early

Dimensions depend on:

```md
- Teleportation
- Inventory storage
- Player attachments
- Networking
- Entity spawning
- Death handling
- Worldgen
```

Do not build Spirit World, Torment, or Mirror World before the core systems are stable.

## Risk: Hardcoded Rituals

Hardcoding every ritual will make the mod painful to expand.

Solution:

```md
Build a ritual framework first, then add content through JSON and handlers.
```

## Risk: Client/Server Desync

Many occult systems are stateful and dangerous to sync incorrectly.

Solution:

```md
Server controls all gameplay results.
Client only displays and requests.
```

## Risk: Scope Explosion

A Witchery-style mod is enormous.

Solution:

```md
Ship in slices:
Garden -> Oven -> Altar -> Kettle -> Rituals -> Worldgen -> Spirit World -> Torment/Mirrors -> Transformations
```

## Risk: Asset and Copyright Problems

Do not use original assets or copied naming wholesale.

Solution:

```md
Create new textures, sounds, names, and code.
Use Witchery only as public gameplay inspiration.
```

---

# 9. Recommended First Sprint

First sprint goal: playable occult crafting loop.

```md
1. Create NeoForge project skeleton
2. Add registries and datagen
3. Add magical trees/plants
4. Add clay jars and basic materials
5. Add Witches' Oven
6. Add oven fuming recipes
7. Add Mutandis-style mutation item
8. Add simple guidebook entries
```

First sprint success condition:

```md
In survival mode, a player can discover/craft magical plants, process them in an oven, obtain magical fumes, and unlock the next crafting tier.
```

---

# 10. Recommended Second Sprint

Second sprint goal: altar-powered magic.

```md
1. Add altar block entity
2. Add natural power scanning
3. Add altar power API
4. Add kettle block entity
5. Add data-driven kettle recipes
6. Add basic brews
7. Add altar power display
8. Add config values for altar balance
```

Second sprint success condition:

```md
A player can build an altar, generate altar power from the surrounding environment, and use that power to brew useful magical items.
```

---

# 11. Recommended Third Sprint

Third sprint goal: first ritual framework.

```md
1. Add chalk/glyph blocks
2. Add circle scanner
3. Add ritual recipe/data format
4. Add ritual activation item
5. Add altar power validation
6. Add item input consumption
7. Add ritual particles/sounds
8. Add three ritual effects
```

Third sprint success condition:

```md
A player can draw a valid circle, provide ingredients, activate a ritual, consume altar power, and produce a world effect.
```

---

# 12. Final Implementation Priority

Use this order unless there is a strong reason to deviate:

```md
1. Project skeleton
2. Registries + datagen
3. Plants and trees
4. Oven and fumes
5. Mutation system
6. Altar power
7. Kettle brewing
8. Circle ritual engine
9. Taglocks, poppets, curses
10. Occult worldgen
11. Familiars and covens
12. Spirit World
13. Torment
14. Mirror pockets
15. Vampire progression
16. Werewolf progression
17. Bosses and hunters
18. Guidebook and integrations
19. Balance/config/modpack support
20. Public beta polish
```

