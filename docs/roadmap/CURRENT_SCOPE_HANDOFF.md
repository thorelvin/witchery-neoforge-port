# Current Scope Handoff - Witchery Port

> Docs cleanup note (2026-06-21): this file remains the additive-scope roadmap
> handoff. For the day-to-day current project state, use
> `docs/status/CURRENT.md`; for docs routing, use `docs/DOCS_INDEX.md`.

**Target:** Minecraft 1.21.11 / NeoForge 21.11
**Purpose:** This file is the clean implementation handoff. Read this first, then use the supporting roadmap files in the order and scope described below.

**2026-06-20 additive snapshot:** `vanishing_witch_hut` (§3.3 / §6) slice 1 is implemented — a block hut that relocates when unobserved (inside→exit→unwatched trigger), usually within the Overworld and ~25% crossing to the Spirit World at mirror coords, leaving ash/debris. It is a block structure (remove + rebuild), not a walking creature, per the structure doc. Built block-by-block around a hidden anchor BlockEntity; the resident reuses `CovenWitchEntity` (tethered to the hut, and carried to the new site on each move so it stays the same witch). The **salt-line counterplay is implemented**: a line of Witches' Salt (`SaltLineBlock`, item `witchery:witches_salt` — reuses the redstone-wire connection rendering, tinted white; smelt kelp in the Witches' Oven or buy from a Coven Witch) across the doorway holds the hut. Natural forest/swamp worldgen is in (`VanishingHutStructure` + `has_vanishing_hut` biome tag, random_spread). Remaining: the other "hold it" routes (red thread / iron nail / offering) plus an anger-on-restraint chance. QA: `/witchery setup|goto vanishing_hut`, `/witchery hut move|watch`.

**2026-06-14 porting snapshot:** today's landed scope is still faithful-port work, not additive expansion: placed Modonomicon live pages/navigation, Waystone/Blooded Waystone/Taglock Crystal Ball scrying, dropped bound/Blooded Waystone transposition, Player Compass needle/model parity, Apothecary cauldron-jobsite trade repair, triggered cursed-block brew dispersal, vanilla-shaped custom brew liquid polish, Gas Mask/lifetime gas parity, Disease brew/block spread parity, Purified Milk disease cleanup, and Dog Tongue ritualized custom-brew cauldron release. Next faithful-port follow-ups from this cluster are the wider dynamic brew action roster, exact ritual pulse/backfire/target-modifier parity, old spell-projectile routes, and final scrying/apothecary reload QA.

---

## 1. Source Priority

Treat the roadmap files with this priority:

1. `witchery_neoforge_12111_additive_gameplay_spec_v4_integration_review(1).md`
   - Canonical source for feature direction, scope, integration rules, naming baseline, and implementation order.

2. `witchery_structure_generation_redo_v2_cursed_huts.md`
   - Supporting detail for worldgen, structure states, structure clues, cursed huts, and Witch Forest atmosphere.
   - Use it to enrich implementation, not to override the main spec.

3. `witchery_mob_rework_coven_hobgoblins.md`
   - Supporting detail for coven witch behavior, trust, hobgoblin jobs, structure-aware mob behavior, and anti-frustration rules.
   - Use it to enrich implementation, not to create separate parallel entity taxonomies.

4. `FUTURE_FEATURES.md`
   - Backlog only.
   - Do not implement anything from this file during current scope unless explicitly requested.

If two files conflict, the additive gameplay spec wins.

---

## 2. Non-Negotiable Rules

Implement this as an additive expansion to the existing Witchery port.

Always prefer extending existing Witchery systems:

- item/block/entity registration
- ritual and circle logic
- altar power
- poppets
- taglocks
- brewing, oven, distillery, spinning
- familiar ownership and commands
- witch/coven NPC logic
- worldgen and structure registration
- networking and saved data
- data generation
- config system

Do not build:

- a second ritual engine
- a second curse framework
- a second familiar ownership model
- a second taglock identity model
- a separate folklore progression track
- a large deity UI for Local Powers

Every feature should answer:

> Which existing Witchery system does this extend?

If the answer is "none," redesign it.

---

## 3. Canonical Naming and Normalization

These decisions exist to stop naming drift across docs.

### 3.1 Witch taxonomy

Use three separate concepts:

1. **Coven witch specialty**
   - Canonical reusable specialty profiles:
   - `hedge_witch`
   - `grave_witch`
   - `pale_witch`
   - `storm_witch`
   - `hearth_witch`

2. **Spawn context / role context**
   - `hut witch`, `circle witch`, `village witch`, and `mountain witch` are context labels, not separate registry types.
   - Use them to drive spawn tables, task pools, dialogue, and structure links.

3. **Special hag NPC**
   - Canonical implementation name: `forest_hag`
   - "forest crone" can remain flavor text, dialogue wording, or an in-world title.
   - Do not create both `forest_hag` and `forest_crone` as separate primary entities unless explicitly requested later.

### 3.2 Local Powers

Current MVP powers:

- `watching_wood`
- `old_hearth`
- `well_below`

Rules:

- Keep Local Powers as shrine/favor/taboo modifiers.
- Do not build a major deity UI.
- Do not implement a broad deity progression system from `FUTURE_FEATURES.md`.

Naming resolution:

- If a forest bargain power beyond `watching_wood` is needed later, prefer `forest_elder`.
- Do not introduce `forest_grandfather` as a separate power.

### 3.3 Structures

Canonical structure names:

- `witch_forest`
- `hanging_witch_puppet`
- `vanishing_witch_hut`
- `cursed_hut`
- `cursed_well`

Rules:

- Vanishing huts are block structures, not walking creatures.
- Cursed huts are investigation structures, not just loot huts.
- Structure states should usually be variants of existing Witchery structures, not whole disconnected systems.

### 3.4 Familiars and heart content

Canonical current late-game feature:

- `heartbound_familiar`

Rules:

- Reuse existing familiar ownership/command conventions.
- Treat heart/grove mechanics as extensions of Witchery ritual and altar space, not a separate companion system.

---

## 4. What Each Supporting File Is Allowed To Contribute

Use the files like this:

### `witchery_neoforge_12111_additive_gameplay_spec_v4_integration_review(1).md`

Use for:

- overall design direction
- integration rules
- major feature approval
- scope boundaries
- slice order
- multiplayer/safety expectations

### `witchery_structure_generation_redo_v2_cursed_huts.md`

Use for:

- Witch Forest worldgen mood
- hut states
- cursed hut aura behavior
- structure chains
- puppet placement and clue behavior
- structure-linked ritual value

Do not use it to:

- rename canonical powers
- create extra witch type registries
- override the main implementation order

### `witchery_mob_rework_coven_hobgoblins.md`

Use for:

- coven trust states
- witch preferences and ritual modifiers
- hobgoblin jobs and camp states
- mob reactions to player actions
- anti-frustration constraints

Do not use it to:

- override the canonical hag name
- create a second witch classification system
- front-load advanced mob content ahead of foundational curse/worldgen work

### `FUTURE_FEATURES.md`

Use only as inspiration for post-scope backlog.

Do not pull current implementation work from it, especially:

- Deity Powers System
- Soul Extraction and Essence Storage
- Spirit Walking / Possession
- Soulless Shells / Artificial Bodies
- Spirit-Powered Devices
- Soul-Modified Gear
- World-Influencing Insubstantial Beings
- Modern Ritual Grammar rewrite
- Mirror / Identity Expansion
- Secret Knowledge / Symbol Magic
- Emoniph NPC

---

## 5. Recommended Current Implementation Order

Use this as the unified order for current work:

1. **Port integration audit**
   - Inspect existing registry helpers, ritual APIs, altar logic, taglock/poppet storage, familiar ownership, worldgen hooks, saved data, networking, config, and data-gen.

2. **Blood and binding foundation**
   - Blood Vial as advanced taglock
   - Witch Needle
   - Red Thread support

3. **Mosquito in a Bottle**
   - Mobile blood collection feeding existing taglock/poppet/curse systems

4. **Anchored curse foundation**
   - Optional anchor data layer on top of existing curse logic
   - Start with a small, safe curse set

5. **Witch Forest atmosphere**
   - biome
   - hanging puppets
   - bonfire clearings
   - clue-rich ambient worldgen

6. **Vanishing Witch Hut + Forest Hag**
   - memorable encounter
   - structure/NPC integration
   - no walking hut behavior

7. **Cursed Hut variant**
   - local aura
   - hidden anchor
   - clue/counterplay loop

8. **Ritual shelves and place-based ritual bonuses**
   - reinforce physical occult play

9. **Coven polish**
   - simple trust state
   - structure-aware tasks
   - specialty-based ritual modifiers

10. **Hobgoblin polish**
    - improve existing worker identity
    - add a few structure-aware jobs
    - keep rewards readable and non-exploitable

11. **Heartbound Familiar**
    - only after curse/worldgen/familiar hooks are stable

12. **Local Powers MVP**
    - only the small invisible-pressure version
    - start with `watching_wood`, `old_hearth`, and `well_below`

Do not jump to broad future systems before the earlier slices feel real and integrated.

---

## 6. Current Scope Boundaries

Approved now:

- Witch Forest
- hanging witch puppets
- blood vial / needle / red thread
- mosquito blood collection
- anchored curses
- vanishing hut
- forest hag
- cursed huts
- ritual shelves / place-based ritual value
- coven trust and structure-aware witches
- hobgoblin polish tied to place, work, and shrines
- heartbound familiar
- small Local Powers MVP

Keep carefully scoped:

- Spontaneous Sleep
- Moving Trees
- Herobrine curse
- Creaking / Pale Garden hooks
- Barkbound teaser

Not approved for current build-out unless explicitly requested:

- broad deity progression
- large-scale spirit simulation
- ritual engine rewrite
- mirror network systems
- possession systems
- soul economy

---

## 7. Execution Checklist

Before coding a feature, verify all of the following:

1. It extends an existing Witchery system.
2. It creates a visible player story moment, not just a stat buff.
3. It has readable counterplay if hostile.
4. It uses existing saved-data, config, networking, and registry patterns where possible.
5. It avoids grief-heavy behavior by default.
6. It can be introduced in a small PR-sized slice.
7. It does not silently create a second taxonomy, registry family, or progression track.

If a feature needs new terminology, first check whether the main additive gameplay spec already names it.

---

## 8. Short Implementation Instruction

Implement this as a Witchery expansion, not as a second mod hiding inside Witchery.

Use the additive gameplay spec as the authority. Use the structure and mob docs as supporting detail only. Ignore `FUTURE_FEATURES.md` for current work unless explicitly told otherwise.

If you hit a naming conflict:

- keep `forest_hag` as the canonical hag implementation
- keep `forest_crone` as flavor only unless later promoted
- keep `watching_wood`, `old_hearth`, and `well_below` as the only Local Powers MVP
- do not create `forest_grandfather`

The target feeling is:

> The old Witchery world is still there, but it has started hiding new things in the forest.
