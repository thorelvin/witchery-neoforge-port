# Witchery Rewrite: Future / Unfinished Feature Brief for Claude

**Purpose:** This document is a design brief for a modern, legally safer, Witchery-inspired rewrite. It focuses on features that appear to have been planned, unfinished, hinted at, or left unexplored around Emoniph's later work, especially **Intangible**, rather than claiming there was a confirmed public Witchery 0.25 roadmap.

**Use with Claude:** Feed this file as project context before asking Claude to design systems, write registries, or generate implementation tickets.

---

## 0. Important boundaries

### Do
- Use public descriptions, changelogs, wikis, videos, and community memory as **design inspiration**.
- Rebuild mechanics with fresh code, fresh data structures, fresh textures, fresh names where appropriate, and modern Minecraft architecture.
- Mark uncertain information clearly as `confirmed`, `community-reported`, `speculative`, or `myth`.
- Favor configurable systems over one-off hardcoded mechanics.

### Do not
- Do not copy original Witchery or Intangible code.
- Do not decompile the original mods for implementation details.
- Do not copy assets, textures, models, sounds, names, or text wholesale where there is licensing risk.
- Do not present rumors as confirmed Emoniph plans.

---

## 1. Confidence legend

| Label | Meaning |
|---|---|
| `confirmed` | Public project page, changelog, or Emoniph-authored description supports it. |
| `strong inference` | Not directly a roadmap item, but strongly implied by official descriptions or unfinished systems. |
| `community-reported` | Mentioned by players/community; useful, but not official proof. |
| `speculative` | Good design direction inspired by the evidence, but not confirmed. |
| `myth` | Treat as folklore unless verified independently. |

---

## 2. Core finding

There does not appear to be a reliable public list of “Witchery 0.25 planned features.”

The strongest evidence of unfinished Emoniph work is instead in **Intangible**, the mod Emoniph made after Witchery. Intangible’s public description says the **deity powers** part of the mod was still being worked on, while the progression-critical part of the deity system was complete.

Design interpretation:

> The most credible “lost future direction” is a late-game soul/deity/spirit system that could have complemented Witchery’s rituals, demons, transformations, and dimensions.

---

# 3. Future Feature Backlog

## Feature 1: Deity Powers System

**Confidence:** `confirmed` for unfinished deity powers in Intangible; `speculative` as a Witchery rewrite feature.

### Design fantasy
Players create, bind, empower, bargain with, or oppose insubstantial godlike entities. These deities are not just bosses; they are persistent world forces that can grant boons, demand offerings, curse regions, empower followers, and influence server politics.

### Player-facing loop
1. Discover spirit essence or soul fragments.
2. Build a shrine, idol, mirror, altar, or vessel.
3. Feed the entity with offerings, memories, sacrifices, vows, or rituals.
4. The entity gains domains such as forest, night, blood, storm, death, dreams, beasts, or decay.
5. Players receive powers, curses, quests, or faction effects.
6. Rival deities compete for influence.

### Suggested mechanics
- **Deity domains:** configurable tags like `blood`, `moon`, `storm`, `death`, `forest`, `dream`, `mirror`, `beast`.
- **Favor score:** per-player and per-deity.
- **World influence score:** per chunk, biome, structure, or claimed shrine area.
- **Offerings:** item sacrifices, mob sacrifices, vows, moon-phase rituals, rare brews.
- **Boons:** potion-like effects, ritual discounts, spell unlocks, familiar upgrades, transformation perks.
- **Wrath:** bad luck, curse effects, hostile spirits, ritual backlash, nightmares.
- **Schisms:** players can steal or split deity essence to form rival cults.

### Implementation notes for Claude
Create a data-driven deity registry:

```json
{
  "id": "rewrite:ashen_moon",
  "display_name": "The Ashen Moon",
  "domains": ["moon", "death", "dream"],
  "preferred_offerings": ["rewrite:grave_dust", "minecraft:phantom_membrane"],
  "taboo_actions": ["burn_sacred_grove"],
  "boons": ["rewrite:night_vision_boon", "rewrite:spirit_walk"],
  "wrath_effects": ["rewrite:nightmare_mark"],
  "influence_rules": {
    "moon_phase_multiplier": true,
    "biome_tags": ["is_forest", "is_spooky"]
  }
}
```

### Minimal viable version
- One deity.
- One shrine block.
- One favor value per player.
- Three offerings.
- Two boons.
- One wrath event.

### Stretch version
- Deity rivalry.
- Server-wide omens.
- Player cult/faction system.
- Regional world modifiers.

---

## Feature 2: Soul Extraction, Storage, and Essence Types

**Confidence:** `confirmed` as Intangible theme; `strong inference` for expansion potential.

### Design fantasy
Every living being contains a soul pattern. Witches can extract, bottle, split, imprint, consume, trade, or bind these patterns.

### Player-facing loop
1. Use a ritual blade, bone, charm, or vessel to collect soul residue.
2. Refine residue into typed essence.
3. Store essence in jars, gems, masks, mirrors, or soulbones.
4. Use essence for spells, possession, familiars, curses, constructs, and deity feeding.

### Essence categories
- **Life essence:** animals, villagers, natural mobs.
- **Undeath essence:** undead mobs, ghosts, revenants.
- **Beast essence:** wolves, cats, bears, bats, were-creatures.
- **Infernal essence:** demons, nether mobs.
- **Fey essence:** forest spirits, pixies, magical plants.
- **Human essence:** villagers/players, high ethical and balance risk.
- **Echo essence:** memory, name, reflection, taglock-like identity fragments.

### Suggested systems
- Essence type should be stored as NBT/data components.
- Essence quality should depend on moon phase, kill method, biome, victim state, and ritual setup.
- Essence should decay unless stabilized.
- Soul theft should create consequences.

### Implementation notes for Claude
Represent soul data as a component:

```json
{
  "soul_type": "beast",
  "source_entity": "minecraft:wolf",
  "purity": 0.82,
  "stability": 0.45,
  "memories": ["loyalty", "hunt"],
  "owner_uuid": null
}
```

### Minimal viable version
- Three essence types: life, beast, undead.
- One extraction item.
- One storage jar.
- One recipe using each essence.

---

## Feature 3: Possession / Spirit Walking

**Confidence:** `confirmed` as Intangible theme; `speculative` for rewrite expansion.

### Design fantasy
The player can leave their body and act as a spirit, scout invisible paths, possess bodies, or operate constructs.

### Player-facing loop
1. Prepare a safe body anchor.
2. Perform a trance ritual.
3. Player enters spirit form.
4. Spirit form has limited interaction but can see hidden things.
5. With the right soul key, the spirit can possess a valid body.
6. Returning late or losing the anchor causes consequences.

### Possession targets
- **Small animals:** scouting and stealth.
- **Hostile mobs:** combat risk/reward.
- **Iron golems or constructs:** mining/combat utility.
- **Soulless shells:** prepared artificial bodies.
- **Dream bodies:** temporary forms in a dream dimension.

### Risks
- Body can be attacked while vacant.
- Silver/salt/wards can trap spirits.
- Possession can fail if target has strong will.
- Possession can anger deities or spirits.
- Death while disembodied may create a ghost state.

### Implementation notes for Claude
Build this as a state machine:

```text
NORMAL_BODY
  -> TRANCE_ENTERING
  -> SPIRIT_FORM
  -> POSSESSION_ATTEMPT
  -> POSSESSING_ENTITY
  -> RETURNING
  -> NORMAL_BODY
```

### Minimal viable version
- Spirit form as a temporary spectator-like mode with strict timer.
- Anchor block required.
- Possess only one custom “soulless shell” entity first.
- Add animals and mobs later.

---

## Feature 4: Soulless Shells / Artificial Bodies

**Confidence:** `confirmed` as Intangible theme; `strong inference` for compatibility with possession/deity systems.

### Design fantasy
A witch can build an empty body and animate it with a soul, spirit, demon, deity fragment, or player projection.

### Shell types
- **Clay shell:** cheap, weak, disposable.
- **Flesh shell:** biological, potion-compatible, morally darker.
- **Bone shell:** undead-adjacent, durable, vulnerable to holy/sun effects.
- **Wooden shell:** nature magic, compatible with familiars and tree spirits.
- **Iron shell:** golem-like, strong, expensive.
- **Mirror shell:** reflection/identity-based, fragile but magical.

### Gameplay uses
- Possession vessel.
- Servant/minion.
- Ritual assistant.
- Familiar body upgrade.
- Deity avatar.
- Emergency respawn vessel.

### Implementation notes for Claude
Separate:
- `ShellBlock` or `ShellEntity`
- `SoulBindingComponent`
- `BehaviorCore`
- `OwnerLink`
- `DeityInfluence`

### Minimal viable version
- One shell entity.
- One ritual to bind life essence.
- Follow/protect owner behavior.
- Optional possession.

---

## Feature 5: Spirit-Powered Devices

**Confidence:** `confirmed` as Intangible theme; `speculative` feature implementation.

### Design fantasy
Instead of machines powered by RF/FE, magical devices run on bound spirits, memories, vows, soul pressure, or deity favor.

### Device examples
- **Soul bell:** detects spirits, curses, or hidden entities.
- **Whispering jar:** stores messages, rumors, or coordinates.
- **Spirit mill:** converts essence into magical power.
- **Bound lantern:** reveals invisible spirits or illusions.
- **Omen engine:** predicts moon events, storms, curses, or deity anger.
- **Warding nail:** protects doors/chests from spirits and curses.
- **Dream loom:** crafts dream items or nightmares.
- **Possession cage:** holds a spirit body or captured essence.

### Suggested energy model
Avoid conventional FE at first. Use a distinct system:

```text
spirit_pressure = essence_quality * vessel_integrity * local_influence
```

Devices consume:
- Stored essence,
- Deity favor,
- Moonlight charge,
- Nearby altar power,
- Bound spirit durability.

### Minimal viable version
- One spirit jar.
- One lantern that consumes jar charge to reveal hidden entities.
- One bell that detects curses/spirits.

---

## Feature 6: Armor and Tools Modified by Soul Powers

**Confidence:** `confirmed` as Intangible theme; `speculative` design.

### Design fantasy
Instead of normal enchantments, gear gains personality-like traits from bound essence.

### Examples
- **Wolf-bound boots:** speed near pack allies.
- **Bat-bound cloak:** slow fall and night vision.
- **Spider-bound gloves:** climb walls.
- **Golem-bound chestpiece:** knockback resistance.
- **Wraith-bound hood:** brief invisibility after taking damage.
- **Fey-bound charm:** flower/forest bonuses.
- **Infernal-bound blade:** stronger in Nether or under blood moon.

### Design rule
Soul modifications should feel alive and conditional, not like flat enchantment upgrades.

### Implementation notes for Claude
Create an `InfusionModifier` registry:

```json
{
  "id": "rewrite:wolf_step",
  "slot": "feet",
  "essence": "beast",
  "source_entities": ["minecraft:wolf"],
  "effects": [
    {
      "type": "movement_speed",
      "condition": "near_allied_wolves",
      "amount": 0.15
    }
  ],
  "drawback": {
    "type": "fear",
    "condition": "near_silver"
  }
}
```

### Minimal viable version
- Three gear infusions.
- One drawback per infusion.
- One altar/crafting station.

---

## Feature 7: World-Influencing Insubstantial Beings

**Confidence:** `confirmed` as Intangible description theme; `speculative` implementation details.

### Design fantasy
Some entities are not mobs but invisible world-layer forces. They haunt areas, bless groves, corrupt villages, create storms, manipulate dreams, or alter spawn rules.

### Entity types
- **Grove spirit:** accelerates plant growth, punishes tree cutting.
- **House spirit:** protects base, moves items, extinguishes fires.
- **Road spirit:** alters travel speed and encounters.
- **Grave spirit:** raises undead, reveals death sites.
- **Mirror spirit:** swaps identities, creates reflections.
- **Storm spirit:** affects weather and lightning.
- **Dream spirit:** creates nightmares and visions.

### Technical model
Do not implement as normal always-ticking mobs everywhere. Use lightweight world attachments:

```text
WorldSpiritInstance
- id
- type
- anchor position or region
- radius
- strength
- temperament
- owner/faction
- active conditions
- cooldowns
```

### Minimal viable version
- Grove spirit bound to a shrine.
- Area radius effect.
- Offering improves strength.
- Neglect causes negative effect.

---

## Feature 8: Modern Witchery Ritual Expansion

**Confidence:** `speculative`, inspired by Witchery's existing ritual-heavy design.

### Design fantasy
Rituals should become a generalized grammar rather than a fixed list of hardcoded recipes.

### Ritual grammar
A ritual is defined by:

```text
circle pattern + focus item + power source + offerings + timing + location + target
```

### Ritual variables
- Moon phase
- Biome
- Weather
- Altitude/depth
- Dimension
- Nearby blocks
- Nearby mobs
- Player coven size
- Deity favor
- Blood/soul essence type
- Known secret symbol

### Example modern rituals
- **Bind Grove Spirit:** creates a local nature spirit.
- **Call the Hollow Guest:** summons a temporary spirit merchant.
- **Mirror of Borrowed Faces:** creates identity disguise item.
- **Wake the Empty Body:** animates a shell.
- **Vow of the Red Moon:** binds player to blood/death deity.
- **Pact of Many Eyes:** reveals nearby players/entities at a cost.
- **Sever Name:** removes a curse but damages reputation/favor.
- **Open Dream Path:** travel through dream-space to another anchor.

### Implementation notes for Claude
Use JSON/datapack ritual definitions if possible:

```json
{
  "id": "rewrite:bind_grove_spirit",
  "circle": "otherwhere_tier_2",
  "focus": "rewrite:living_branch",
  "offerings": [
    "minecraft:honeycomb",
    "rewrite:life_essence",
    "minecraft:oak_sapling"
  ],
  "conditions": {
    "biome_tag": "is_forest",
    "moon_phase": ["full", "waxing_gibbous"],
    "weather": "clear"
  },
  "cost": {
    "altar_power": 3500,
    "deity_favor": 5
  },
  "result": {
    "type": "spawn_world_spirit",
    "spirit": "rewrite:grove_spirit",
    "radius": 32
  }
}
```

---

## Feature 9: Mirror / Identity / Reflection Expansion

**Confidence:** `confirmed` as an existing late Witchery feature; `speculative` as future expansion.

### Design fantasy
Mirrors are not only teleporters or portals. They are identity machines: reflections can lie, steal names, imitate players, copy bodies, or split souls.

### Expansion ideas
- Reflection demons become long-term rivals that learn from the player.
- Mirrors can store names, faces, signatures, memories, or curses.
- Identity masks can fool villagers, mobs, wards, or players.
- Mirror realm contains impossible rooms linked to every placed magic mirror.
- Broken mirrors create reflection shards with partial powers.
- A mirror duplicate can act independently but risks replacing the player.

### Integration
- Uses `echo essence`.
- Interacts with deity domain `mirror`.
- Supports disguise, teleport, nightmares, doppelgangers, and curse redirection.

### Minimal viable version
- Mirror anchor block.
- Reflection shard item.
- Temporary disguise effect.
- One hostile reflection entity.

---

## Feature 10: Secret Knowledge / Symbol Magic

**Confidence:** `confirmed` as Witchery had symbol/spell-like secret mechanics; `speculative` as future system expansion.

### Design fantasy
Some magic should not appear in JEI. Players discover it through books, dreams, witch huts, demon bargains, ruins, or community experimentation.

### Mechanics
- Symbol sequences drawn in air, on blocks, in books, or around circles.
- Secret phrases/chants.
- Forbidden glyphs requiring deity permission or soul cost.
- Wrong sequence causes mild backlash.
- Secret knowledge is stored per player.

### Design goals
- Encourages discovery.
- Supports hidden late-game spells.
- Preserves Witchery’s mystery.
- Lets servers add custom secret magic.

### Implementation notes for Claude
Store discovered symbols in player data:

```json
{
  "known_symbols": [
    "rewrite:glyph_blood",
    "rewrite:glyph_mirror",
    "rewrite:glyph_moon"
  ],
  "known_sequences": [
    "rewrite:open_dream_path",
    "rewrite:call_reflection"
  ]
}
```

### Minimal viable version
- Three glyphs.
- One symbol station.
- Two secret recipes.
- One hidden spell.

---

## Feature 11: Possible Modern Port / Rewrite Direction

**Confidence:** `community-reported` that Intangible had a 1.9 port in progress; `speculative` for broader Witchery plan.

### Design fantasy
If Emoniph had continued, a likely path would have been:
1. Finish Intangible's deity powers.
2. Port Intangible to newer Minecraft.
3. Possibly return to Witchery or merge soul/deity concepts with Witchery-style ritual magic.

### Rewrite interpretation
Build the rewrite as if Witchery’s ritual/nature/witchcraft systems and Intangible’s soul/deity systems were meant to meet.

### Concrete combined pillars
- **Witchcraft:** herbs, brews, poppets, curses, familiars.
- **Rituals:** circles, covens, altars, dimensions.
- **Transformations:** vampire, werewolf, spirit walker, maybe fey-touched.
- **Soulcraft:** essence extraction, possession, shells, soul gear.
- **Deities:** shrines, favor, world influence, cults.
- **Mirrors/Dreams:** identity, nightmares, reflection realm.

---

# 4. Recommended Implementation Order

## Phase 1: Foundation
- Data registries for essences, rituals, curses, boons, and domains.
- Altar/ritual power system.
- Basic herb/brew/item infrastructure.
- Documentation generation from data.

## Phase 2: Soulcraft
- Soul extraction.
- Soul jars.
- Essence types.
- Soul-based recipes.
- Simple shell entity.

## Phase 3: Spirit Form and Possession
- Trance/anchor block.
- Spirit state.
- Possession of shell.
- Possession timer and return rules.

## Phase 4: Deity Alpha
- One deity.
- Shrine block.
- Favor system.
- Offerings.
- Boon/wrath event.
- Basic deity UI or book entry.

## Phase 5: World Spirits
- Region-bound spirit instances.
- Grove spirit.
- Spirit detection device.
- Offerings and decay.

## Phase 6: Mirror/Dream Expansion
- Mirror identity data.
- Reflection enemy.
- Dream path travel.
- Disguise/echo essence.

## Phase 7: Server-Scale Systems
- Deity rivalry.
- Cult/faction support.
- Configurable deity packs.
- Admin tools.
- Modpack integration.

---

# 5. High-Value Claude Tasks

Use these prompts with Claude.

## Prompt A: System architecture

```text
You are designing a modern Minecraft mod inspired by Witchery and Intangible, but without copying original code or assets.

Design the architecture for:
- soul essence extraction and storage
- spirit form / possession
- deity favor
- shrine offerings
- world spirits
- data-driven rituals

Return:
1. Java/Kotlin package structure
2. registries/data objects
3. core interfaces
4. save-data strategy
5. networking requirements
6. test plan
```

## Prompt B: Deity system

```text
Design a data-driven deity system for a Minecraft magic mod.

Requirements:
- deities have domains, offerings, boons, wrath effects, favor, and regional influence
- favor is per-player
- influence is per-region/chunk
- deities can be defined by JSON/datapacks
- avoid hardcoded deity logic where possible

Output:
- JSON schema
- Java/Kotlin interfaces/classes
- sample deity definitions
- event hooks
- balancing rules
```

## Prompt C: Soul essence

```text
Design the soul essence system.

Requirements:
- living entities can produce typed essence
- essence has type, source entity, purity, stability, and optional memories
- essence can be stored in items/blocks
- essence powers rituals, possession, gear infusions, and deity offerings
- include ethical/config settings for villagers/players

Output implementation plan and data model.
```

## Prompt D: Possession MVP

```text
Create an MVP plan for spirit walking and possession.

Requirements:
- player enters spirit form from an anchor block
- physical body remains vulnerable
- spirit has limited interaction
- spirit can possess only a custom soulless shell entity in v1
- timeout returns player to body
- death/failure states are handled safely

Output:
- state machine
- entity/component data
- network sync
- edge cases
- pseudocode
```

## Prompt E: World spirits

```text
Design lightweight world spirits that affect regions without being normal ticking mobs.

Requirements:
- spirits have type, anchor, radius, strength, temperament, owner/faction, active conditions, and cooldowns
- spirits can grant buffs, curses, crop growth, spawn changes, weather nudges, or warnings
- players can feed or anger spirits

Output:
- saved data format
- tick scheduling strategy
- performance considerations
- example grove spirit
```

---

# 6. Things to Treat Carefully

## Doomcat-like content
**Confidence:** `myth`.

Do not treat Doomcat as confirmed. It can inspire a hidden folklore mechanic, but the rewrite should label it internally as an original secret transformation, not as an actual recovered Witchery feature.

## Original names
Names like Witchery, Intangible, Lilith, specific item names, and exact text may have legal or community expectation issues. Prefer original names unless compatibility or tribute is intentional and safe.

## Player sacrifice / human essence
Powerful but sensitive. Add server config:
- allow player soul capture: true/false
- allow villager soul capture: true/false
- require consent for player soul mechanics: true/false
- PvP-only soul capture: true/false
- soul capture loss severity

## Griefing risk
Deity/world-spirit systems can become grief tools. Add:
- claims/permissions integration
- config caps
- admin commands
- decay over time
- visible warnings
- reversible curses

---

# 7. Source Notes

These are starting points, not exhaustive proof.

- Witchery CurseForge page / latest official file:  
  https://www.curseforge.com/minecraft/mc-mods/witchery  
  https://www.curseforge.com/minecraft/mc-mods/witchery/files/2234410

- Intangible CurseForge page / WIP deity powers note:  
  https://www.curseforge.com/minecraft/mc-mods/intangible

- Reddit discussion about Intangible and possible 1.9 port/community memory:  
  https://www.reddit.com/r/feedthebeast/comments/4t9t87/what_happened_to_the_mod_intangible/

- Reddit retrospective on Intangible’s soul/deity concepts:  
  https://www.reddit.com/r/feedthebeast/comments/8oypat/i_miss_intangible_an_ode_to_forgotten_mods/

- Emoniph YouTube channel, public videos on Witchery/Intangible:  
  https://www.youtube.com/c/emoniph

---

# 8. One-Sentence Design Thesis

A modern rewrite should not only recreate Witchery’s visible features; it should complete the implied missing arc: **witchcraft evolves into soulcraft, soulcraft evolves into spirit possession, and spirit possession evolves into deity-scale world magic.**
