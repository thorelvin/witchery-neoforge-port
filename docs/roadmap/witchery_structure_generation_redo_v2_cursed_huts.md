# Witchery Structure Generation Redo

**Target:** Minecraft 1.21.11 / NeoForge 21.11  
**Project context:** This document is for adding new structure/world-generation gameplay to an already ported Witchery mod. It should extend existing Witchery systems, not redefine Witchery’s core gameplay.

**Read this first:** start with [CURRENT_SCOPE_HANDOFF.md](./CURRENT_SCOPE_HANDOFF.md). This document is supporting detail for structure and worldgen implementation. It should not override the current handoff on scope, naming, or implementation order.

---

## 1. Design Goal

The structure generation pass should make Witchery feel more alive, eerie, and discoverable in modern Minecraft.

The goal is not to add random loot houses. The goal is to make the world feel like witchcraft happened before the player arrived.

A good Witchery structure should make the player ask:

> What happened here?  
> Who did it?  
> Is something still active?  
> Can I use this in a ritual?

---

## 2. Main Principle

Every new structure should connect to at least one existing Witchery system.

Valid integration targets:

- Circle magic
- Altar power
- Coven witches
- Witch huts
- Brews / kettle / cauldron
- Oven / distillery / spinning
- Poppets
- Taglocks
- Familiars
- Curses
- Transformations
- Rite discovery
- Witchery guidebook progression
- Witchery plants / ingredients

Avoid adding structures that are only decorative unless they are part of ambience generation.

---

## 3. Structure Generation Philosophy

### 3.1 Structures should create clues, not just loot

Loot is secondary. The structure should first communicate a story.

Examples:

- A broken poppet and red thread under a tree.
- A cellar with old blood bottles and a note warning not to sleep.
- A hanging puppet facing toward a hidden curse anchor.
- A bonfire clearing with ash, chalk, and no mobs nearby.
- A hut that vanishes after the player trades and looks away.

### 3.2 Not everything should be important

The player should not know immediately whether a detail is:

- decoration
- warning
- clue
- trap
- actual curse anchor
- ritual component
- structure chain marker

This uncertainty is part of the Witchery feeling.

### 3.3 Structures should be reusable

Many structures should support generated states instead of needing completely separate structures.

Example: a witch hut can generate as abandoned, occupied, sealed, cursed, vanishing, burned, pale, or cellar-linked.

---

## 4. Improved Existing Witchery Structures

## 4.1 Witch Huts

### Existing role

Witch huts are already part of Witchery’s identity and should remain recognizable.

### Additive improvement

Turn witch huts into procedural mini-stories.

Possible hut states:

| State | Description | Gameplay |
|---|---|---|
| Occupied Hut | Witch or hag trader lives there | Trades, rumors, small tasks |
| Abandoned Hut | No resident, but traces remain | Loot, clues, hidden cellar chance |
| Sealed Hut | Cannot be entered normally | Requires key, phrase, rite, or trade |
| Vanishing Hut | Relocates after one trade if unseen | Memorable folklore moment |
| Cellar Hut | Hidden underground room | Jars, blood, roots, poppets, notes |
| Cursed Hut | Contains curse anchor or trap | Investigation + cleansing |
| Burned Hut | Ruined version with ash and remains | Black salt, ghost clue, old rite page |
| Pale Hut | Pale Garden / Creaking variant | Resin, eyeblossoms, hidden heart |

### Vanishing Block Witch Hut

Approved feature direction:

A normal block hut relocates after the player completes one trade, exits, and looks away.

Rules:

- It is a block structure, not a walking creature.
- It cannot move while a player is inside.
- It cannot move while directly watched.
- It leaves traces: ash, moss, candle wax, smoke, loose planks, old footprints.
- The witch may say: **“We may not meet again.”**
- Holding it in place with red thread, iron nail, salt line, or paid offering may work once.
- Attempting to hold it in place may anger the witch with a small chance.

Implementation note:

Do not animate the hut walking. Remove and rebuild the structure at a valid nearby location after line-of-sight is broken.

---

## 4.1.1 Cursed Huts

Cursed Huts are rare Witch Hut variants that feel actively wrong before the player understands why.

They should be generated as normal block structures, but with a local curse aura, altered visibility, and at least one hidden curse anchor or cleansing objective.

### Purpose

Cursed Huts should create an immediate player question:

> Why does this place feel unsafe, and what is causing it?

They are not just loot structures. They are investigation structures.

### Generation

Cursed Huts can appear:

- rarely in Witch Forest
- rarely in swamps
- rarely near graveyard groves
- very rarely in dark forests
- as a possible state of existing Witchery huts
- as a possible second-stage location after a hag rumor or structure chain

Suggested rarity:

- Witch Forest: rare but findable
- Outside Witch Forest: very rare
- Near old bonfire clearings or grave groves: slightly higher chance

### Local Curse Aura

When a player enters the hut’s local area, apply atmosphere and mild pressure.

Possible local effects:

| Effect | Description |
|---|---|
| Witch Fog | Local fog thickens around the hut |
| Short Sight | Render/visibility distance feels reduced near the structure |
| Dim Light | Area feels darker even during day |
| Muffled Sound | Distant sounds become quieter; hut sounds become louder |
| False Footsteps | Player hears steps outside or behind walls |
| Cold Breath | White particles or frost-like visual around windows |
| Candle Flicker | Candles/torches flicker or briefly dim |
| Door Unease | Doors creak or open/close slightly |
| Window Watcher | Rare silhouette appears behind a window, then vanishes |

### Sight Range Rule

The short-sight/fog effect should be local and temporary.

Implementation direction:

- Use a structure-bound aura volume or invisible controller block/entity.
- Apply a client-side fog/vision effect only within a defined radius.
- Radius suggestion: 24–48 blocks.
- Strongest inside or near the hut.
- Fades quickly when leaving.
- Should not permanently alter world fog.
- Should not stack heavily with other fog effects.
- Avoid making navigation impossible; this is tension, not blindness.

### Curse Anchor

Every true Cursed Hut should contain one hidden anchor.

Possible anchors:

- cursed mirror
- red-thread poppet
- skull under floorboards
- old blood bottle
- hanging puppet in attic
- buried charm below hearth
- cracked witch shelf item
- cursed bed
- sealed jar in cellar
- pale effigy near roots

The hut aura ends or weakens when the anchor is cleansed/destroyed.

### Cursed Hut States

Cursed Huts can have sub-states:

| State | Description |
|---|---|
| Quiet Curse | Subtle fog, whispers, hidden anchor |
| Sleeping Curse | Bed/nightmare focused, stronger at night |
| Mirror Curse | Reflections, window silhouettes, Herobrine hook |
| Harvest Curse | Spoiled food, dead plants, animal unease nearby |
| Blood Curse | Blood bottles, mosquito jar, red thread, poppet clue |
| Pale Curse | Resin, eyeblossoms, Creaking sounds, hidden heart |
| Burned Curse | Hut already burned but curse survived in ash/cellar |

### Gameplay Hooks

Cursed Huts can connect to:

- Curse of Herobrine
- Curse of Spontaneous Sleep
- Curse of Sleeplessness
- Curse of the Wrong Door
- Curse of Spoiled Harvest
- Curse of Rooted Steps
- Blood Mosquito discovery
- Red thread/poppet crafting
- Mirror cleansing
- Hag bargain tasks
- Witch Forest structure chains
- Creaking Heart / Heartbound curse content

### Player Interaction

Players should be able to:

- investigate clues
- find hidden cellar/attic/floorboard
- identify the curse type
- cleanse the anchor
- steal/use the anchor for their own rites
- trade with a witch if occupied
- flee and return prepared

### Counterplay

Possible counterplay:

- white chalk door sigil reduces hut aura
- mirror cleansing reveals anchor direction
- hag candle burns blue/green near anchor
- black cat familiar warns before curse triggers
- allay familiar may find hidden anchor
- black salt weakens aura temporarily
- breaking anchor ends local fog/sight effect

### Rewards

Rewards should feel like story loot.

Possible loot:

- old blood vial
- broken poppet
- red thread
- witch needle
- hag candle
- cracked mirror shard
- torn curse page
- dead mosquito bottle
- black salt
- curse-specific ingredient
- clue to another structure

### Anti-Frustration Rules

- Do not trap players permanently.
- Do not apply lethal effects just for approaching.
- Do not reduce sight so much that the player cannot leave.
- Do not spawn endless mobs.
- Always provide at least one clue toward the anchor.
- Strong effects should have cooldowns or require nighttime.

---

## 4.2 Witch Circles / Stone Circles

### Existing role

A ritual landmark.

### Additive improvement

Make circles feel like damaged ritual machines.

Circle states:

| State | Description | Gameplay |
|---|---|---|
| Intact Circle | Complete and usable | Ritual bonus |
| Broken Circle | Missing stones/chalk | Repair to activate |
| Overgrown Circle | Hidden by moss/roots/leaves | Discovery reward |
| Bloodied Circle | Darker rite remnants | Curse/poppet bonus, risk |
| Cursed Circle | Strong but unstable | Cleansing recommended |
| Mountain Circle | High-altitude sabbath site | Weather/coven bonus |
| Pale Circle | Tied to Creaking/Pale Garden | Heartbound ritual bonus |

Integration:

- Existing circle magic should recognize these locations.
- Altar power may get a small location bonus.
- Coven witch spawn/task chance may increase nearby.
- Rite discovery pages may appear in ruined circles.

---

## 4.3 Coven Witch Generation

### Existing role

Coven witches are already important for advanced Witchery play.

### Additive improvement

Make coven witches belong to their surroundings.

These are context labels for spawning and task generation, not separate registry/entity types.

Spawn contexts:

| Coven Witch Type | Location | Role |
|---|---|---|
| Hut Witch | Witch hut | Trades, gossip, small rites |
| Circle Witch | Stone circle | Teaches or modifies rites |
| Village Witch | Village/apothecary | Hidden social witchcraft |
| Forest Hag | Witch Forest | Hag bargains and strange tasks |
| Mountain Witch | Mountain sabbath site | Weather/coven/familiar rites |
| Grave Witch | Graveyard grove | Death, poppets, ghosts |
| Pale Witch | Pale Garden / Pale Hut | Creaking Hearts and Grove magic |

Task generation should be based on nearby structures.

Examples:

- Near grave grove: retrieve grave fern or cleanse a restless anchor.
- Near Witch Forest: cut down a cursed hanging puppet.
- Near village: investigate spoiled harvest.
- Near Pale Garden: inspect a Creaking Heart.
- Near circle: repair a missing ritual piece.

---

## 4.4 Village Apothecaries

### Existing role

Village Witchery gateway.

### Additive improvement

Make village apothecaries feel socially hidden and morally ambiguous.

Possible variants:

| Variant | Gameplay |
|---|---|
| Public Herbalist | Safe herbs and basic ingredients |
| Backroom Apothecary | Hidden red thread, needles, poppet parts |
| Cursed Apothecary | Villager problem; help or exploit |
| Watched Apothecary | Riskier trades; witch-hunter suspicion |
| Well-Linked Apothecary | Connected to cursed well and local offerings |

Rumor examples:

- “The old well has been knocking again.”
- “Don’t sleep in the north house.”
- “That forest hut was here yesterday.”
- “Someone tied red thread to the barn door.”
- “The crows keep watching the wheat field.”

Rumors should guide players toward structure chains without map markers.

---

## 4.5 Straw Men / Scarecrows

### Existing role

Eerie field object.

### Additive improvement

Make straw men into field curse markers.

Variants:

| Variant | Use |
|---|---|
| Plain Straw Man | Atmosphere |
| Red Thread Straw Man | Weak curse anchor |
| Crow-Crowned Straw Man | Bird omen marker |
| Burned Straw Man | Cleansed/failed ritual clue |
| Watching Straw Man | Rotates/faces player when unseen |
| Harvest Curse Straw Man | Spoiled harvest anchor |

Integrations:

- Curse of Spoiled Harvest
- Old Barn ritual sites
- Field curses
- Witch Forest boundary markers
- Poppet ingredient source

---

## 5. New Biome: Witch Forest

The Witch Forest should be the main new worldgen addition.

It is a rare, thick, cliffy, sun-blocking forest biome with webs, fog, old structures, and hidden ritual sites.

### Generation traits

- Rare biome.
- Thick canopy blocks sunlight.
- Uneven terrain with cliffs, ravines, roots, cave mouths, and overhangs.
- Dense ground clutter: mushrooms, dead leaves, roots, moss, cobwebs, bones, black flowers, fallen logs, candle stumps.
- Ambient sounds: crows, owls, whispers, creaking trees, twig snaps, false footsteps.

### Structure ecosystem

Witch Forest should generate:

- Vanishing Block Witch Huts
- Cursed Huts with local fog/short-sight aura
- Old bonfire clearings
- Broken fence ruins
- Hanging witch puppets
- Web groves
- Cursed wells
- Hag camps
- Graveyard groves
- Cellar entrances
- Abandoned altars
- Carved-face trees
- Bone markers
- Ash circles
- Red thread trails

### Ritual effects

Witch Forest should affect ritual power, not become a required place for everything.

Bonuses:

- Stronger curse rites
- Stronger familiar binding
- Better blood rites
- Better forest spirit bargains
- Stronger dream/sleep curses
- Mild Creaking/Watching Wood synergy

Risks:

- Wrong offerings may attract spiders, hag spirits, forest ghosts, or hostile familiars.
- Breaking hanging puppets carelessly may mark the player or alert a witch.
- Night rituals may trigger ambient haunt events.

---

## 6. Hanging Witch Puppets

Hanging Witch Puppets are a signature Witch Forest micro-feature.

### Description

Small twig/thread/bone/cloth effigies hanging from trees. They should feel eerie, handmade, and ambiguous.

### Visual variants

- Twig dolls
- Red thread knots
- Bone charms
- Moss bundles
- Feather bundles
- Teeth charms
- Cloth strips
- Tiny poppets

### Placement

Mostly found near:

- old bonfire clearings
- paths
- ravines
- huts
- cursed wells
- grave groves
- web groves
- hidden curse anchors

### Gameplay functions

Most puppets are atmospheric. Some are meaningful.

Possible roles:

| Puppet Type | Behavior |
|---|---|
| Decorative Puppet | Atmosphere only |
| Warning Puppet | Faces dangerous area |
| Clue Puppet | Points toward hidden hut/anchor |
| Ward Puppet | Weak protection or boundary |
| Curse Puppet | Actual curse anchor |
| Bait Puppet | Breaking it spawns/alerts something |

### Interaction

Players can:

- break
- collect
- burn
- cut with shears
- cleanse with chalk/salt

Careless breaking may:

- trigger whispers
- spawn spiders
- alert a witch
- apply temporary mark
- reveal a hidden path
- start a curse investigation

---

## 7. Micro-Generation Around Structures

Structures should scatter small details nearby to make them feel embedded in the world.

### Around Witch Huts

- broken bottles
- mushrooms
- hanging puppets
- old chalk marks
- dead campfire
- red thread on fence
- black cat spawn chance
- buried poppet
- suspicious tree
- faint path to well, cellar, or circle

### Around Stone Circles

- missing chalk
- skull
- candle stump
- herb patch
- crow/raven spawn chance
- broken altar stone
- nearby coven witch chance

### Around Cursed Wells

- mossy stones
- old bucket
- dropped item
- whisper sound
- water particles
- hidden offering table nearby

### Around Graveyard Groves

- bent trees
- grave fern
- bones
- cracked stone
- raven/crow chance
- soul jar fragment
- buried poppet

### Around Web Groves

- dense webs
- cursed silk
- bones
- trapped chest chance
- spider/cave spider spawn chance
- red thread and poppet scraps

---

## 8. Structure States

Most structures should support states.

This creates replay value without needing too many separate structures.

### General state types

| State | Meaning |
|---|---|
| Abandoned | Loot and clues, no resident |
| Occupied | NPC/trader/witch present |
| Sealed | Requires item, phrase, or rite |
| Cursed | Contains active curse, anchor, local fog, or short-sight aura |
| Burned | Remains after cleansing/failure |
| Overgrown | Hidden and nature-covered |
| Pale | Creaking/Heart/eyeblossom variant |
| Vanishing | Relocates when conditions are met |
| Ritual-Active | Can empower rites |
| Looted/Spent | Reduced rewards but still atmospheric |

Implementation suggestion:

Use structure processors, jigsaw variants, or post-placement decorators to apply state features.

---

## 9. Structure Chains

Structure chains create the best player gratification.

A structure should sometimes point toward another structure or feature.

### Example chain: Hut → Well → Circle

1. Player trades with hag.
2. Hag mentions the old well.
3. Well gives clue or demands offering.
4. Clue points to damaged circle.
5. Repairing circle unlocks or strengthens a rite.

### Example chain: Puppet → Bonfire → Curse Anchor

1. Hanging puppet faces one direction.
2. Player follows to old bonfire clearing.
3. Under ash is a hidden curse anchor.
4. Cleansing anchor removes local effect or unlocks loot.

### Example chain: Apothecary → Barn → Field Curse

1. Village apothecary mentions spoiled milk.
2. Old barn contains red thread.
3. Field scarecrow is anchor.
4. Player can cleanse or exploit the curse.

### Example chain: Witch Forest → Vanishing Hut → Grove Heart

1. Player finds Vanishing Block Witch Hut.
2. After trade, hut disappears.
3. Smoke trail points deeper into Witch Forest.
4. Second encounter reveals Grove Heart or Creaking-related rite.

---

## 10. Structure-Based Ritual Power

Structures should modify ritual power in readable ways.

| Structure | Suggested Bonus |
|---|---|
| Witch Hut | Brewing, poppets, hag trades |
| Witch Circle | Circle magic efficiency |
| Witch Forest Clearing | Curses, familiar binding, fear rites |
| Cursed Well | Offerings, curse transfer, local power favor |
| Graveyard Grove | Ghost, death, poppet, protection rites |
| Old Barn | Harvest, animal, misfortune rites |
| Mountain Sabbath Site | Coven, weather, spirit rites |
| Pale Hut | Creaking, heartbound curses |
| Web Grove | Binding, red thread, mosquito/poppet rites |

Important: These bonuses should be useful, not mandatory.

---

## 11. Loot Philosophy

Avoid generic chest loot.

Use story loot that implies an event.

Preferred loot examples:

- red thread
- broken poppet
- half-used poppet
- bottle of old blood
- witch needle
- old nail
- hag candle
- chalk fragment
- torn rite page
- curse clue note
- cracked mirror shard
- dead mosquito bottle
- cursed silk
- bone charm
- rare seed
- black salt
- ash
- grave fern
- map scrap
- suspicious key

Example cellar chest:

- red thread
- broken poppet
- bottle of old blood
- note: “Do not sleep in the east room.”
- black salt
- cracked mirror shard

This feels more Witchery than diamonds or random enchanted gear.

---

## 12. Structure Events

Small events make structures memorable.

Use sparingly.

Examples:

- Candle lights when player enters cellar.
- Door closes behind player in sealed hut.
- Puppet turns to face player.
- Crows scatter when curse anchor is nearby.
- A distant hut chimney appears after trade.
- Trees creak when player breaks a hanging puppet.
- Well whispers after player drops wrong item.
- Bonfire reignites at midnight.
- Mirror briefly shows a figure behind player.
- Creaking sound plays from inside hut wall.

Events should be local, atmospheric, and not overused.

---

## 13. Integration With Approved Additive Features

### Blood Mosquito

Structure integrations:

- Dead mosquito bottle in hut/cellar loot.
- Web groves provide cursed silk for mosquito/poppet crafting.
- Blood vials found rarely in witch cellars.
- Mosquito release is stronger in Witch Forest only if later approved.

### Anchored Curses

Structure integrations:

- Hanging puppet as curse anchor.
- Straw man as field curse anchor.
- Hidden skull/mirror/poppet in hut cellar.
- Old well as curse-transfer anchor.
- Bonfire ash hiding curse anchor.

### Herobrine Curse

Structure integrations:

- Hidden skull or mirror in Witch Forest hut.
- Cursed Hut mirror-state can be a natural Herobrine curse source.
- Apparitions appear near old bonfires, windows, treelines, or foggy clearings.
- Local hut fog/short-sight makes distant sightings more effective.
- White chalk door sigil and mirror cleansing provide counterplay.

### Spontaneous Sleep Curse

Structure integrations:

- Curse anchor hidden in bed, hut, cellar, or sleep-related poppet.
- Wake-up destination should prefer nearby safe shelter, hut interior, barn, or strange clearing.
- Never teleport into lava, void, suffocation, or lethal fall.

### Creaking Familiar / Heartbound Magic

Structure integrations:

- Pale Hut with hidden heart.
- Grove Heart in advanced Witch Grove.
- Creaking answers hut call from nearby trees.
- Heartbound curse anchors can appear in Pale Garden or Witch Forest variants.

### Local Powers

Structure integrations:

- Cursed wells feed **The Well Below**.
- Witch Forest groves should stay inside the current Local Powers MVP; prefer **The Watching Wood** unless later scope explicitly expands.
- Hearth/hut structures feed **The Old Hearth**.
- Pale Huts and Creaking Hearts feed **The Watching Wood**.
- Grave groves feed spirit/death powers.

---

## 14. Current Scope Note

Read [CURRENT_SCOPE_HANDOFF.md](./CURRENT_SCOPE_HANDOFF.md) before using this file for implementation.

This document now serves as supporting detail for:

- Witch Forest atmosphere and worldgen
- hut states and cursed hut behavior
- structure clues, chains, and ritual value

Current implementation order, naming resolution, and agent-facing constraints are centralized in the handoff file and the additive gameplay spec.

---

## 15. Registry Name Suggestions

These are suggestions only.

### Biomes

- `witch_forest`

### Structures

- `old_witch_hut`
- `vanishing_witch_hut`
- `sealed_witch_hut`
- `cursed_witch_hut`
- `witch_cellar`
- `cursed_well`
- `old_bonfire_clearing`
- `broken_fence_ruin`
- `web_grove`
- `graveyard_grove`
- `mountain_sabbath_site`
- `pale_witch_hut`
- `old_barn_ritual_site`
- `abandoned_altar`

### Features

- `hanging_witch_puppet`
- `carved_face_tree`
- `bone_marker`
- `ash_circle`
- `red_thread_trail`
- `candle_stump`
- `cursed_silk_web`
- `suspicious_tree`

### Structure states

- `abandoned`
- `occupied`
- `sealed`
- `cursed`
- `burned`
- `overgrown`
- `pale`
- `vanishing`
- `fogbound`
- `short_sight`
- `ritual_active`

---

## 16. Final Direction

The structure generation redo should make the world feel haunted by Witchery.

Players should not simply find structures. They should find evidence.

A great generated Witchery place should feel like:

> Someone made a bargain here.  
> Something went wrong.  
> The world remembers just enough to be useful — and dangerous.
