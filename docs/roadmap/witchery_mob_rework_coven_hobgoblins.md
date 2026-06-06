# Witchery Mob Rework — Coven Witches, Hobgoblins, and Structure-Linked Mobs

**Target:** Minecraft 1.21.11 / NeoForge 21.11  
**Project context:** This document is for adding new mob behavior and integration layers to an already ported Witchery mod. It should extend existing Witchery mobs and systems, not redefine Witchery’s core gameplay.

**Read this first:** start with [CURRENT_SCOPE_HANDOFF.md](./CURRENT_SCOPE_HANDOFF.md). This document is supporting detail for mobs, trust, jobs, and reactions. It should not override the handoff on canonical naming, scope, or implementation order.

---

## 1. Design Goal

The mob rework should make existing Witchery mobs feel more alive, social, territorial, and ritual-aware.

The goal is not to add many unrelated RPG enemies.

The goal is:

> Mobs should belong to places, rituals, bargains, structures, and consequences.

A good Witchery mob should answer:

- What does this mob want?
- What structure or biome does it belong to?
- Which Witchery system does it interact with?
- What happens if the player uses it well?
- What happens if the player mistreats it?

---

## 2. Main Principle

Every mob feature should extend at least one existing Witchery system.

Valid integration targets:

- Coven witches
- Circle magic
- Altar power
- Witch huts
- Witch Forest
- Poppets
- Taglocks
- Brews / kettle / cauldron
- Oven / distillery / spinning
- Familiars
- Curses
- Transformations
- Spirit/dream/nightmare mechanics
- Hobgoblin villages/workers
- Hobgoblin patron statue
- Rite discovery
- Structure generation

Avoid creating parallel mob systems that do not interact with Witchery.

---

# 3. Coven Witch Rework

## 3.1 Role to preserve

Coven witches should remain tied to advanced Witchery play, especially:

- circle magic
- ritual power
- coven participation
- advanced rite progression
- strange witch NPC interaction

The rework should make coven witches feel like distinct people with local ties and ritual value.

---

## 3.2 New coven witch model

Each coven witch can have:

- specialty
- trust state
- preferred biome or structure
- favorite offering
- disliked action
- ritual modifier
- small personal task
- rumor/clue pool

This makes them useful without turning them into generic quest NPCs.

---

## 3.3 Coven witch specialties

### Hedge Witch

**Role:** early friendly witch / herbalist.

Found near:

- villages
- apothecaries
- gardens
- normal witch huts

Likes:

- herbs
- flowers
- bottles
- bread
- simple plant ingredients

Dislikes:

- reckless curses near villages
- killing animals near her hut

Ritual bonus:

- improves brew stability
- reduces ingredient waste
- helps plant/growth rites
- improves basic poppet preparation

---

### Grave Witch

**Role:** death, poppet, ghost, curse-removal specialist.

Found near:

- graveyard groves
- burned huts
- old cellars
- spirit bridges
- grave-related structures

Likes:

- bones
- grave fern
- black candles
- soul jars
- old nails

Dislikes:

- bright hearth wards
- disturbing graves without offering

Ritual bonus:

- strengthens poppets
- improves ghost/death rites
- improves curse removal
- helps locate curse anchors near graves

---

### Forest Hag

**Role:** Witch Forest specialist and hag-style bargain NPC.
In-world dialogue or rumors may still refer to this figure as a forest crone.

Found near:

- Witch Forest
- vanishing huts
- cursed huts
- old bonfire clearings
- hanging puppet clusters

Likes:

- red thread
- mushrooms
- old blood
- hanging puppet parts
- cursed silk
- broken poppets

Dislikes:

- rude interaction
- broken promises
- careless destruction of witch markers

Ritual bonus:

- strengthens Witch Forest rites
- improves curse anchor rituals
- improves familiar binding in forests
- improves blood/red-thread rites

Special behavior:

- may give impossible tasks
- may vanish or relocate after trade
- may know hut/curse-anchor rumors

---

### Pale Witch

**Role:** Creaking Heart / Pale Garden specialist.

Found near:

- Pale Gardens
- Pale Huts
- Grove Hearts
- heartbound curse sites
- Witch Forest + Pale Garden border areas

Likes:

- resin
- eyeblossom ink
- pale wood
- hidden hearts
- intact Creaking Hearts

Dislikes:

- fire
- axes
- destroying hearts
- over-cleansing grove magic

Ritual bonus:

- helps bind Creaking familiar
- stabilizes Grove Heart
- improves heartbound curses
- improves Watching Wood rites

---

### Storm Witch

**Role:** weather, wind, storm, broom, and mountain ritual specialist.

Found near:

- cliffs
- mountain sabbath sites
- storm circles
- exposed highlands
- old bonfire hills

Likes:

- feathers
- wind jars
- lightning-charged items
- breeze-related items
- storm-brew ingredients

Dislikes:

- sleeping through storms
- extinguishing ritual fires during storms

Ritual bonus:

- improves weather rites
- improves wind/broom upgrades
- improves lightning/storm ritual power
- improves mountain sabbath rituals

---

### Hearth Witch

**Role:** protection, cleansing, home, sleep, and ward specialist.

Found near:

- villages
- apothecaries
- occupied huts
- safe hearth structures
- old homes

Likes:

- bread
- milk
- candles
- stew
- warm food
- white chalk

Dislikes:

- careless curses near homes
- harming village families
- using sleep curses near her domain

Ritual bonus:

- improves wards
- improves cleansing
- improves sleep protection
- improves anti-haunting rites
- weakens local curse effects

---

## 3.4 Coven trust system

Keep trust simple and readable.

Each coven witch may have one trust state:

| State | Meaning |
|---|---|
| Stranger | Basic trades only |
| Wary | Gives rumor or minor task |
| Trusted | Joins rites |
| Bound Coven | Participates in stronger rites |
| Offended | Refuses help or worsens prices |
| Hostile | Attacks, vanishes, or warns other witches |

### Trust increases when player:

- completes task
- gives correct offering
- cleanses nearby curse
- protects hut/grove
- trades respectfully
- repairs a ritual site
- uses correct ritual location

### Trust decreases when player:

- attacks familiar
- breaks hut objects
- steals from cellar
- fails bargain
- uses wrong offering
- harms local shrine
- destroys anchors without understanding
- attacks coven-aligned mobs

---

## 3.5 Coven witches should react to structures

Coven witches should not spawn without context.

| Structure | Coven Witch Behavior |
|---|---|
| Witch Hut | Trades, warnings, small tasks |
| Cursed Hut | May be victim, culprit, or trapped by curse |
| Witch Forest Clearing | Appears at night for rites |
| Old Bonfire | Gathers with others on special nights |
| Graveyard Grove | Grave Witch tasks and death rites |
| Cursed Well | Offering warnings or bargain clues |
| Pale Hut | Pale Witch protects hidden heart |
| Stone Circle | Helps repair or empower circle |
| Village Apothecary | Hidden/social witchcraft gateway |

Example interaction:

> Player enters a cursed hut. The witch says: “Do not look in the mirror unless you brought chalk.”

This connects NPC, structure, curse, and counterplay.

---

## 3.6 Coven witches and rituals

Coven witches should modify rituals by specialty, not only add raw power.

| Witch Type | Ritual Modifier |
|---|---|
| Hedge Witch | safer brews, growth, plants |
| Grave Witch | death, poppets, ghosts, curse removal |
| Forest Hag | curses, familiar binding, Witch Forest rites |
| Pale Witch | Creaking, heartbound curses, Grove Heart |
| Storm Witch | weather, broom, wind, lightning |
| Hearth Witch | wards, cleansing, sleep protection |

This makes coven composition meaningful.

---

# 4. Hobgoblin Rework

## 4.1 Role to preserve

Hobgoblins should remain useful, greedy, strange, and slightly dangerous.

They should preserve their identity as:

- laborers
- miners
- gatherers
- traders
- hut/camp dwellers
- patron worshipers
- troublemakers

The rework should make them more visible and flavorful without making them too safe or too automated.

---

## 4.2 Hobgoblin personality traits

Each hobgoblin can have one simple trait.

| Trait | Behavior |
|---|---|
| Greedy | Demands better payment, may find better loot |
| Cowardly | Flees danger faster |
| Nosy | Opens containers, finds hidden things |
| Hardworking | Works faster |
| Lazy | Works slowly unless bribed |
| Superstitious | Reacts strongly to curses and wards |
| Devout | Worships patron statue more often |
| Thieving | May steal small items if unpaid |
| Brave | Joins group attacks more easily |

No heavy UI required. Behavior and small tooltips/nameplate hints can reveal traits.

---

## 4.3 Existing-style jobs to preserve

### Miner

Assigned using a post/tool style system.

Behavior:

- mines nearby marked stone or ore area
- should respect anti-grief config
- needs food/payment
- better tools improve output
- may refuse dangerous cursed areas

### Gatherer

Assigned using post/chest style system.

Behavior:

- collects dropped items nearby
- useful for farms, rituals, and Witch Forest cleanup
- may bring junk if trait is Nosy or Thieving
- may avoid cursed structures unless paid

---

## 4.4 New hobgoblin jobs

### Mushroom Picker

Collects:

- mushrooms
- Witch Forest herbs
- roots
- small plant ingredients

Best use:

- Witch Forest bases
- brew ingredient gathering

Risk:

- may bring cursed mushrooms or strange junk.

---

### Web Cutter

Collects:

- cobweb
- cursed silk
- thread-like materials

Best use:

- web groves
- poppet/thread crafting
- mosquito bottle crafting

Risk:

- may anger spiders or bring back cursed silk.

---

### Bone Picker

Collects:

- bones
- skull fragments
- old nails
- graveyard scraps

Best use:

- grave groves
- burned huts
- curse investigations

Risk:

- may attract ghosts.

---

### Candle Lighter

Maintains:

- candles
- old bonfires
- hearth lights
- safe-pocket lights

Best use:

- Witch Forest bases
- ritual clearings
- cursed hut cleansing

Risk:

- may light something that should stay dark.

---

### Offering Runner

Carries offerings to:

- wells
- shrines
- patron statues
- hearths
- grove markers

Best use:

- local power systems
- patron worship
- ritual upkeep

Risk:

- wrong offering may anger the wrong power if not configured carefully.

---

### Puppet Finder

Searches for:

- hanging puppets
- buried poppets
- curse anchors
- red thread markers

Best use:

- curse investigations
- Witch Forest exploration

Risk:

- may trigger puppet backlash if careless.

---

## 4.5 Hobgoblin settlements

Hobgoblin camps and villages should become more visible and interactive.

Possible settlement features:

- crude huts
- dirty campfire
- stolen fence pieces
- mushroom patch
- junk pile
- little shrine
- stolen bell
- odd chest/junk storage
- sleeping piles
- warning markings
- patron statue area

### Settlement states

| State | Meaning |
|---|---|
| Tiny Camp | 1–2 hobgoblins, little trade |
| Working Camp | miners/gatherers available |
| Shrine Camp | patron worship active |
| Greedy Camp | better loot, higher prices |
| Angry Camp | attacks villagers/players |
| Protected Camp | witch/hag nearby |
| Cursed Camp | needs cleansing or bargain |

---

## 4.6 Hobgoblins and villages

Preserve their uneasy relationship with villagers.

Behavior options:

- hobgoblins dislike villagers
- groups may harass villages
- witches can calm or redirect them
- village apothecary may complain about theft
- player can resolve, exploit, or worsen the conflict

Example village rumor:

> “Little thieves took the bell again. They left red thread on the road.”

---

## 4.7 Hobgoblin patron statue rework

The patron statue is a strong classic Witchery hook and should be expanded.

### Patron Statue behavior

A player can create/bind a patron statue.

Nearby hobgoblins may worship it if:

- they are fed or paid
- they have huts nearby
- statue is decorated
- no rival patron is nearby
- player has not mistreated hobgoblins

### Worship output

Avoid free infinite power.

Possible benefits:

- small altar power trickle
- minor infusion support
- better hobgoblin trades
- faster hobgoblin work
- occasional offering item
- stronger hobgoblin loyalty
- local power favor hook

### Patron moods

| Mood | Cause | Effect |
|---|---|---|
| Pleased | paid workers, offerings | better work and shrine power |
| Bored | no offerings | weaker benefit |
| Greedy | too many trades | demands items |
| Angry | unpaid workers, killed hobgoblins | theft, sabotage, curse risk |
| Afraid | nearby Creaking/curse | worship pauses |
| Devoted | long-term care | stronger stable boost |

This creates a distinctive hobgoblin niche:

> goblin labor + crude worship economy + funny danger.

---

## 4.8 Hobgoblins and Witch Forest

Hobgoblins should appear mostly near the edges of Witch Forest, not dominate the deep woods.

Behavior:

- avoid deep cursed clearings unless paid
- fear hanging puppets
- refuse to enter Cursed Huts unless highly trusted
- may point at hidden anchors if Superstitious
- may steal puppet parts
- trade cursed silk, red thread, bones, mushrooms
- can be hired to clear web groves

Flavor moment:

> A hobgoblin gatherer returns from the woods with a hanging puppet and immediately regrets it.

---

# 5. Other Mob Reworks

## 5.1 Treefyd / plant guardian mobs

If Treefyd-style mobs are present in the port, use them as grove and forest defenders.

### Role

- living plant ward
- grove protector
- old circle guardian
- Witch Forest threat
- plant mutation result

### Behavior

- protect Witch Groves
- appear near old circles or cursed forest sites
- react to Barkbound players
- hostile to axe-heavy players if cursed
- can be grown intentionally as living wards

### Integrations

- Rite of the Forest
- Witch Grove
- Curse of Moving Trees
- Witch Forest
- plant mutations
- altar/nature rites

---

## 5.2 Ent / old forest guardian mobs

Use sparingly.

### Role

Rare forest guardian, not common enemy.

Behavior:

- angered by tree destruction
- calmed by correct offering or rite
- guards ancient bonfires or Grove Hearts
- may test player before granting forest rite clue

Rewards should prefer:

- bargain
- blessing
- rare seed
- grove permission
- rite clue

Do not make it primarily a kill-for-loot mob.

---

## 5.3 Nightmare / sleep mobs

Because a full dream dimension is deferred, keep nightmare mobs local and event-based.

### Role

Sleep-curse manifestation.

Spawn/appearance contexts:

- Curse of Sleeplessness
- Curse of Spontaneous Sleep
- cursed beds
- cursed huts
- failed sleep rites
- mirror or candle events

Behavior:

- appear briefly
- do not become normal cave mobs
- leave clues like ash, thread, frost, broken feather, or bone dust
- become hostile only if disturbed or bound

Player feeling:

> Something followed me out of the dream.

---

## 5.4 Demon / infernal mobs

Do not expand too early.

### Role

High-risk ritual consequences.

Behavior direction:

- tied to existing summoning rites
- bargains are remembered
- contracts matter
- failed summoning can mark structures
- no random Witch Forest demon spam

Integrations:

- circle magic
- contracts
- poppets
- curse anchors
- advanced local power/Usurper content later

---

## 5.5 Vampire / werewolf mobs and players

Do not make vampire/werewolf content random hostile spam.

### Vampire integration

Vampires should:

- value blood mosquito bottles
- dislike hearth wards
- dislike sunlight rites
- use cursed huts as hideouts
- interact with Red Hunger-style blood power later

### Werewolf integration

Werewolves should:

- react to moonlit Witch Forest clearings
- track bloodmarked targets
- dislike silverleaf and chalk wards
- interact with howling events
- become stronger around some forest/moon ritual sites

---

## 5.6 Spectral mobs / ghosts

Ghosts should be clues more than enemies.

### Spawn/appearance contexts

- graveyard groves
- cursed wells
- burned huts
- spirit bridges
- cursed hut cellars
- old bonfires

### Behavior

- point toward anchors
- replay small scenes
- whisper incomplete warnings
- appear in mirrors
- become hostile only if disturbed, bound, or ignored too long

### Integrations

- grave fern
- soul jars
- curse anchors
- Spirit Bridge
- graveyard grove
- mirror cleansing
- Herobrine/haunting systems

---

## 5.7 Spiders and cursed spiders

Use them strongly in Witch Forest but not everywhere.

### Role

Web grove guardians and puppet-risk response.

Behavior:

- spawn in web groves
- react to broken hanging puppets
- guard cursed silk
- appear near abandoned altars
- may be summoned by careless puppet breaking

Integrations:

- cursed silk
- red thread
- poppets
- mosquito bottle crafting
- Witch Forest ambience

---

# 6. Mob-to-Structure Links

Every mob should have a reason to appear.

| Mob | Best Structure Links |
|---|---|
| Coven Witch | hut, circle, apothecary, Witch Forest clearing |
| Forest Hag | vanishing hut, cursed hut, Witch Forest |
| Hobgoblin | hobgoblin camp, mine post, shrine |
| Creaking Familiar | Pale Hut, Grove Heart, Witch Grove |
| Treefyd | Witch Grove, old circle, cursed forest |
| Ghost | grave grove, bridge, well, burned hut |
| Nightmare Mob | cursed bed, sleep curse, cursed hut |
| Spider/Cursed Spider | web grove, hanging puppets |
| Black Cat | hut, apothecary, curse warning |
| Raven/Owl | grave grove, Witch Forest, mountain fog site |
| Ent/Forest Guardian | ancient bonfire, Grove Heart, deep forest |

---

# 7. Mob Behavior Around Player Actions

## 7.1 Player breaks hanging puppet

Possible reactions:

- nearby spiders aggro
- forest hag notices
- raven flies away
- hobgoblin becomes afraid
- ghost whispers
- curse anchor reveals particles
- player receives a temporary mark

---

## 7.2 Player completes hag trade

Possible reactions:

- hut may vanish
- coven witch trust increases
- nearby cats follow briefly
- local power favor changes
- hobgoblin thieves may appear later
- rumor chain updates

---

## 7.3 Player cleanses cursed hut

Possible reactions:

- ghost despawns peacefully
- coven witch offers better trade
- hut becomes normal or abandoned
- curse anchor becomes loot
- local power favor changes
- structure fog disappears

---

## 7.4 Player mistreats hobgoblins

Possible reactions:

- patron statue turns angry
- hobgoblins steal items
- miners refuse work
- camp becomes hostile
- coven witches dislike player
- small curse or sabotage event may occur

---

## 7.5 Player repairs old ritual circle

Possible reactions:

- circle witch appears
- coven trust increases
- rite discovery unlocks
- ravens gather
- altar power bonus activates

---

# 8. Player Gratification Goals

The mob rework should create moments like:

- A witch warns the player about a curse before they understand it.
- A hobgoblin worker finds something useful but suspicious.
- A black cat hisses at an invisible curse anchor.
- A raven flies from a grave grove toward a hidden hut.
- A ghost points silently at the floorboards.
- A forest hag trades something valuable, then the hut vanishes.
- A hobgoblin patron statue becomes pleased and the whole camp works better.
- A Creaking familiar emerges from trees to guard the Grove Heart.

The player should feel:

- clever for noticing clues
- rewarded for preparation
- amused by hobgoblin behavior
- uneasy around witches
- attached to familiars
- powerful when rituals and mobs work together

---

# 9. Current Scope Note

Read [CURRENT_SCOPE_HANDOFF.md](./CURRENT_SCOPE_HANDOFF.md) before using this file for implementation.

This document now serves as supporting detail for:

- coven trust states and witch specialties
- hobgoblin jobs, camp behavior, and patron logic
- structure-aware mob reactions
- anti-frustration expectations for mob behavior

Current implementation order and canonical naming are centralized in the handoff file and the additive gameplay spec.

---

# 10. Anti-Frustration Rules

## Coven witches

- Do not make trust decay too punishing.
- Do not lock too many basic features behind rare NPCs.
- Do not make rude/failed interactions permanently ruin progression without recovery.
- Provide ways to regain trust.

## Hobgoblins

- Prevent grief by config.
- Make stealing/sabotage minor and recoverable.
- Do not let workers create infinite resource exploits.
- Make payment/food requirements clear.
- Let players dismiss or relocate workers.

## Ghosts/nightmares

- Use as clues first, combat second.
- Avoid endless spawns.
- Avoid jumpscare spam.
- Always provide counterplay.

## Creaking/forest mobs

- Keep strong effects local to groves, hearts, structures, or rituals.
- Do not let AI destroy player bases uncontrollably.
- Make heart/anchor logic understandable.

---

# 11. Registry Name Suggestions

These are suggestions only.
Use the handoff file for canonical naming. Context labels such as hut witch or village witch are not separate ids.

## Coven Witch Specialties

- `hedge_witch`
- `grave_witch`
- `forest_hag`
- `pale_witch`
- `storm_witch`
- `hearth_witch`

## Coven Trust States

- `stranger`
- `wary`
- `trusted`
- `bound_coven`
- `offended`
- `hostile`

## Hobgoblin Traits

- `greedy`
- `cowardly`
- `nosy`
- `hardworking`
- `lazy`
- `superstitious`
- `devout`
- `thieving`
- `brave`

## Hobgoblin Jobs

- `miner`
- `gatherer`
- `mushroom_picker`
- `web_cutter`
- `bone_picker`
- `candle_lighter`
- `offering_runner`
- `puppet_finder`

## Hobgoblin Camp States

- `tiny_camp`
- `working_camp`
- `shrine_camp`
- `greedy_camp`
- `angry_camp`
- `protected_camp`
- `cursed_camp`

## Mob Event Hooks

- `on_puppet_broken`
- `on_hag_trade_complete`
- `on_cursed_hut_cleansed`
- `on_hobgoblin_mistreated`
- `on_ritual_circle_repaired`
- `on_anchor_found`

---

# 12. Final Direction

The mob rework should not add “more enemies.”

It should make Witchery’s world feel inhabited by beings with wants, grudges, bargains, and jobs.

Best summary:

> Coven witches become people with specialties and grudges.  
> Hobgoblins become greedy little workers, worshipers, and troublemakers.  
> Ghosts and nightmares become clues, not mob spam.  
> Forest mobs belong to groves, circles, and curses.  
> Every mob connects to rituals, structures, trades, anchors, or offerings.
