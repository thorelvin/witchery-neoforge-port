# Witchery NeoForge 1.21.11 — Additive Gameplay Feature Spec

**Purpose:** Add new gameplay features to an already remade Witchery mod, originally from Minecraft 1.7.10 and now targeting **NeoForge / Minecraft 1.21.11**.

This is **not a remake** and **not a standalone mod design**. It is an expansion roadmap for the existing Witchery remake. New features should plug into existing Witchery systems wherever possible: rituals, altar power, poppets, taglocks, brews, familiars, witches, covens, worldgen, and curse logic.

**Important scope correction:** classic Witchery already provides the core gameplay. This document must not redefine that core loop. Treat everything here as additive content: new biomes, encounters, curses, familiars, ritual modifiers, structures, ingredients, and late-game integrations that strengthen the remade mod without replacing its existing systems.

**Design promise:** every feature should feel like genuine Witchery: physical, eerie, useful, counterable, and story-generating.

**Read this first:** start with [CURRENT_SCOPE_HANDOFF.md](./CURRENT_SCOPE_HANDOFF.md). This file remains the canonical feature-direction spec, but the handoff file resolves cross-document naming, scope, and source-priority decisions.

---

## 1. One-Sentence Direction

Extend the remade Witchery mod with a stronger **physical occult loop**: players discover the Witch Forest, gather blood and red thread, bind curses to hidden anchors, send mosquitoes to steal blood, trade with vanishing huts, empower rituals through shelves and place, and eventually bind heart-based forest familiars and grow a Witch Grove.

The red thread is literal and mechanical:

> **Blood identifies. Red thread binds. Shelves prepare. Anchors hide. Curses haunt. Huts vanish. Hearts remember only what they must.**

Folklore should be felt through atmosphere and mechanics, not presented as a folklore encyclopedia. Witchery remains the headline.

---

## 2. Existing-Remake Integration Rules

Before implementing anything, inspect the current remake and reuse existing systems.

### Reuse or extend existing systems

- item/block/entity registration
- ritual and circle magic framework
- altar power logic
- poppet and taglock mechanics
- blood mechanics, if already present
- kettle, brewing, oven, and distillery logic
- familiar ownership and command logic
- witch/coven/NPC systems
- data generation
- networking and persistent player data
- config system
- worldgen and structure registration

### Avoid duplicate systems

Do not create a parallel curse, ritual, taglock, or familiar framework unless the existing remake cannot support the new behavior. If a classic Witchery feature already exists, extend it.

### Version note: Minecraft / NeoForge target

The target is **Minecraft 1.21.11 on NeoForge 21.11**. Treat modern vanilla content from the 1.21 line as available for design planning, including Pale Garden / Creaking-era concepts where present in the target environment.

For this 1.21.11 expansion, Creaking, Creaking Heart, Pale Garden, resin, and eyeblossom concepts should be treated as **first-class integration points** rather than backport placeholders. When implementing, prefer extending or wrapping vanilla behavior over duplicating it. Only create Witchery-owned variants when the existing remake needs custom curse/familiar/grove behavior that vanilla blocks or entities cannot safely support.


### Integration walkover: use old Witchery as the spine

These new features should not sit beside Witchery as a second mod. Each one should enter through an existing Witchery-style interaction: altar power, circle rites, poppets, taglocks, brews, familiar summoning, witch huts, covens, or worldgen.

| New feature family | Best old-Witchery integration point | Implementation direction |
|---|---|---|
| Witch Forest | Altar power, worldgen, witch huts, herbs, circle ritual conditions | Make the biome a high-value place for existing Witchery activity. It should boost or modify rites; it should not create a separate progression track. |
| Hanging Witch Puppets | Poppets, taglocks, curse anchors | Treat puppets as environmental poppet fragments or minor anchors. A rare puppet can hold a real curse link. |
| Blood Vials / Witch Needle | Taglock Kits, poppets, curses | Blood should be an advanced taglock tier. If the remake already has taglock storage, Blood Vials should reuse the same target identity model. |
| Red Thread | Poppets, ritual circles, shelves, curse anchors | Red Thread should visibly connect Witchery objects: poppet → anchor → shelf → blood vial. Use it to make existing systems feel more physical. |
| Mosquito in a Bottle | Taglock collection, poppets, curse rites | The mosquito is a mobile taglock collector. Its output should feed existing poppet/cursing systems instead of inventing separate blood magic. |
| Anchored curses | Existing curse logic, poppet protection, cleansing rites | Add optional physical anchors to existing curses. Direct curses still exist; anchored curses are stronger and more story-rich. |
| Herobrine curse | Curse framework, nightmare logic, mirror cleansing, poppets | Treat as a forbidden curse effect, not a new boss. It should use existing curse ticking and cleansing hooks. |
| Spontaneous Sleep curse | Brew of Sleeping, nightmare/sleep logic, bed traces | The curse should feel like a twisted sleep brew/rite. Reuse any existing sleep/nightmare hooks where possible. |
| Vanishing Block Witch Hut | Witch hut worldgen, witch NPC/coven logic, trades | Make it a special hut behavior layered onto existing witch structures/NPCs. Do not create a separate village system. |
| Forest Hag | Witch/coven NPCs, trades, rites | The Hag should feel like a rare Witchery witch variant with strange trades and ritual consequences. |
| Cursed Well | Circle rites, waystone/location logic, local power offerings | Use wells as offering/clue/teleport-adjacent ritual anchors, not as generic loot boxes. |
| Ancient Witch Bonfire | Circle magic, coven gathering, ritual discovery | This should be a ritual event site that teaches or modifies existing rites. |
| Local Powers | Altar modifiers, shrine anchors, offerings | Local Powers should be invisible pressure behind rituals, not a separate deity UI. Favor should mostly modify rites, trades, and rare events. |
| Heartbound Familiar | Familiar summoning, bound entities, heart blocks | Bind the heart through a rite; the body is a manifestation. Reuse familiar ownership/commands where possible. |
| Witch Grove | Altar power, Rite of the Forest, Treefyd/plant systems, familiars | The Grove should be a late-game Witchery base upgrade, not a new dimension or settlement system. |
| Barkbound teaser | Existing transformations/infusions, bark/nature mechanics | Keep it small at first: forest affinity, bark/heart interactions, weakness to axe/fire. Full path later. |

### Integration Rule

For every implementation task, answer this before coding:

> Which existing Witchery system does this feature extend?

Valid answers should usually be one of:

- circle rite
- altar power
- poppet
- taglock
- brew/kettle
- oven/distillery/spinning recipe
- familiar
- witch/coven NPC
- worldgen structure
- curse effect
- transformation/infusion
- guidebook/discovery entry

If the answer is “none,” redesign the feature until it plugs into Witchery.

### Avoid parallel systems

Do not add a second ritual engine, second curse registry, second familiar ownership model, or second taglock identity format unless the remake has no usable equivalent. The cleanest result is that existing Witchery players can use familiar old tools to discover the new content.

---

## 3. Expansion Design Principles

### 3.1 Witchery first, folklore as flavor

The expansion should feel like lost Witchery content, not a new theme mod. It should build on the remade mod’s existing Witchery gameplay and add more reasons to use it. Prioritize:

- rituals
- poppets
- blood and taglocks
- curses with counterplay
- familiars
- witch huts
- hags and bargains
- strange ingredients
- shelves and altar rooms
- forests, groves, wells, and crossroads

Folklore informs the mood. The remade Witchery mod already defines the core gameplay.

### 3.2 No mana bar

Do not alter the remake with a generic mana system. New content should continue to reward preparation:

- altar base power
- moon phase
- time of day
- weather
- biome
- shelves
- chalk type
- offerings
- blood freshness
- candles, skulls, and ritual objects
- curse anchors
- shrine favor
- Grove Heart / Living Hut Heart / Bound Heart

### 3.3 Curses are physical, readable, and satisfying

Every major curse should have:

1. **Source** — who or what cast it
2. **Anchor** — hidden block, poppet, skull, mirror, effigy, heart, or puppet
3. **Symptom** — what the victim experiences
4. **Clue** — how the victim can investigate
5. **Escalation** — how it worsens over time
6. **Counterplay** — cleansing, reflection, warding, breaking, or enduring
7. **Payoff** — visible relief/reward when cleansed or reflected

Curses should be scary, but never cheap. The victim should eventually think: “I can solve this.”

### 3.4 Narrow memory only

Do not implement broad “every object remembers everything.” Use memory only where gameplay needs it:

- blood stores target and freshness
- beds store limited sleep/taglock traces
- curse anchors store curse and victim data
- Bound Hearts store familiar/grove/curse links
- shelves hold visible ritual modifiers
- red thread links objects together

### 3.5 Maximum player gratification

Every new feature should deliver at least one strong player feeling:

- **Discovery:** “What is that?”
- **Mischief:** “I can use this on someone.”
- **Power:** “My preparation mattered.”
- **Fear:** “Something is watching me.”
- **Relief:** “I found the anchor and broke it.”
- **Ownership:** “This grove/hut/familiar is mine now.”
- **Story:** “This weird thing happened, and I remember it.”

Avoid systems that are only passive stat boosts.

---

## 4. Additive Feature Flow

This is not the main Witchery progression. It is the suggested order in which players may discover the new expansion content while still using classic Witchery systems.

### Add-on Step 1 — Find the Witch Forest

The player discovers a rare dark biome full of ritual remains, web groves, broken fences, old bonfires, carved trees, hanging puppets, and hidden structures.

**Gratification:** discovery, danger, loot, atmosphere.

### Add-on Step 2 — Learn Blood and Binding

The player obtains a Witch Needle, Blood Vials, Red Thread, and early binding ingredients.

**Gratification:** the player gains personal, targeted magic rather than generic spell power.

### Add-on Step 3 — Use the Mosquito in a Bottle

The player releases a mosquito that hunts the nearest valid non-owner player, steals blood, and returns.

**Gratification:** funny, creepy, immediately understandable, multiplayer memorable.

### Add-on Step 4 — Cast Anchored Curses

Blood, poppets, red thread, shelves, and anchors allow stronger curses. Victims can investigate clues and cleanse the anchor.

**Gratification:** caster gets mischief and power; victim gets mystery and counterplay.

### Add-on Step 5 — Find the Vanishing Witch Hut

A normal block hut appears in the Witch Forest. After one trade, the witch says, “We may not meet again.” The player exits, looks away, and the hut is gone.

**Gratification:** surprise, wonder, and a memorable story moment.

### Add-on Step 6 — Build Ritual Power

Shelves, chalk, moon, weather, biome, offerings, and place improve existing Witchery rites.

**Gratification:** the player’s witch room and environment matter.

### Add-on Step 7 — Meet Local Powers

Players feed shrine anchors, earn favor, trigger wrath, and receive rare miracles from place-bound powers.

**Gratification:** the world feels alive without becoming a god-game UI.

### Add-on Step 8 — Bind the Heart Familiar

The player binds a forest-heart entity through its heart block. The body can die and reform if the heart survives.

**Gratification:** a truly Witchery-style familiar: powerful, eerie, physical, vulnerable.

### Add-on Step 9 — Grow a Witch Grove

Late progression lets the player create a forest base around a Grove Heart with wards, rituals, familiars, and heartbound curses.

**Gratification:** ownership, protection, ritual identity, long-term progression.

---

## 5. Feature Family: Witch Forest

**Feature name:** Witch Forest  
**Registry suggestion:** `witch_forest`

A rare, dark, cliffy forest biome that becomes the expansion’s atmospheric anchor.

### Visual identity

- dense canopy blocking most sunlight
- uneven cliffy terrain with ravines and overhangs
- roots, mossy stones, fallen logs, cave mouths
- web pockets between trees and cliffs
- mushrooms, dead leaves, bones, candle stumps
- carved-face trees
- hanging bottles, cloth strips, bones, and charms
- occasional fog silhouettes

### Ambient behavior

- distant crows and owls
- creaking branches
- twig snaps
- faint whispers
- false footsteps
- rare distant laughter
- very rare mountain/fog apparition in high terrain

### World features

- old bonfire clearings
- broken fence ruins
- bone posts
- web groves
- vanishing huts
- hag camps
- cursed wells
- grave groves
- cellar doors
- abandoned altars
- hanging witch puppets

### Loot and rewards

The biome must be worth exploring. Include meaningful loot:

- ash
- red thread
- old nails
- Witchlight Candles
- broken poppets
- bone charms
- cursed silk
- rare seeds
- old ritual notes
- blood/binding ingredients
- low chance of unique hag trade item

### Ritual value

The Witch Forest should provide ritual power bonuses for:

- curses
- familiar binding
- blood rites
- forest rites
- dream/nightmare effects, without requiring a full dream dimension
- local power offerings

### Safe pockets

Players can create temporary safety areas using:

- hearth lights
- white chalk sigils
- bells
- salt posts, if implemented later

Safe pockets reduce minor hauntings and ambience intensity locally.

### Player gratification review

- Strong discovery biome.
- Strong loot motivation.
- Strong atmosphere.
- Strong setup for hags, curses, and rituals.
- Avoid making it only cosmetic; every structure should provide loot, clue, risk, or progression.

---

## 6. Feature Family: Hanging Witch Puppets

**Feature name:** Hanging Witch Puppet  
**Registry suggestion:** `hanging_witch_puppet`

Small twig/thread effigies hanging from Witch Forest trees.

### Purpose

- atmosphere
- warning marker
- ritual clue
- cheap poppet ingredient
- fear ingredient
- spirit bait
- possible curse anchor

### Behavior

Some puppets subtly face or rotate toward:

- hidden hut
- curse anchor
- grave grove
- old bonfire clearing
- cursed well
- hag camp

### Interaction

Players can:

- break them
- collect them
- burn them
- cut them with shears
- cleanse them

Careless breaking can:

- alert a witch
- spawn spiders
- trigger whispers
- briefly mark the player

Rare red-thread-and-hair puppets can be actual curse anchors.

### Player gratification review

- Good visual horror.
- Good clue system.
- Good player choice: collect, ignore, cleanse, or risk breaking.
- Strong genuine Witchery fit because it overlaps with poppets, curses, and ritual anchors.

---

## 7. Feature Family: Blood, Red Thread, and Needles

**Feature names:** Witch Needle, Blood Vial, Bottle of Stolen Blood, Red Thread

### Witch Needle

Used to:

- extract blood
- stitch poppets
- repair or modify dolls
- mark victims in rituals
- prepare mosquito bottles or blood vials

### Blood Vial data

Store:

- victim UUID
- victim display name
- freshness timestamp
- amount/quality
- collection source: needle, mosquito, ritual, other

### Blood freshness

Blood should decay:

- **Fresh:** strongest curse/tracking modifier
- **Stale:** weaker but usable
- **Ruined:** ingredient only or low-grade rites

Later preservation can use resin, cold storage, or ritual treatment.

### Red Thread

Red Thread is the mechanical through-line:

- ties poppets to victims
- links blood vials to curse anchors
- marks shelves as ritual participants
- helps bind heart familiars
- can hold a vanishing hut once, with risk
- connects ritual objects visibly in the world

### Player gratification review

- Strong tactile witchcraft.
- Blood feels personal and valuable.
- Red thread gives the expansion a clear visual identity.
- Works naturally with existing Witchery poppets/taglocks.

---

## 8. Feature Family: Mosquito in a Bottle

**Feature name:** Mosquito in a Bottle  
**Registry suggestion:** `mosquito_bottle`

A corked witch jar containing a tiny blood-hunting familiar/tool.

### Feature behavior

On right-click:

1. bottle releases mosquito entity
2. mosquito searches for nearest valid non-owner player
3. mosquito flies toward target
4. target hears faint buzzing or receives a subtle message
5. mosquito bites target
6. target receives minor **Itching** and possible **Bloodmarked**
7. mosquito returns
8. bottle becomes **Bottle of Stolen Blood**

### Valid target rules

- nearest non-owner player
- within configurable range
- alive
- not creative/spectator
- respects team/coven exclusions if existing remake supports them
- blocked or reduced by counterplay

### Counterplay

- campfire/hearth smoke repels it
- cats, frogs, and toads can kill it
- black salt/garlic/wards reduce chance, if implemented
- player can hit it
- cooldown prevents spam

### Failure behavior

If killed or unable to return:

- bottle becomes **Dead Mosquito Bottle** or empty bottle variant
- mosquito must be revived/replaced
- owner gets no blood

### Deferred upgrades

Do not implement initially:

- swarm bottle
- silent mosquito
- vampiric mosquito
- pale mosquito
- ominous mosquito
- blood-spirit path

### Player gratification review

This is one of the highest-gratification features. It is simple, creepy, funny, and immediately useful. It creates a story for both players:

- caster: “I stole your blood.”
- victim: “Something bit me. Who did that?”

Keep it lightweight, readable, and counterable.

---

## 9. Feature Family: Anchored Curse Framework

**Registry type suggestion:** `witchery:curse_type`

### Curse tiers

- **Direct curse:** timed, weaker, easier to cleanse
- **Anchored curse:** stronger, longer, investigable
- **Heartbound curse:** strongest forest/grove variant, tied to Bound Heart or Grove Heart

### Curse definition concept

```json
{
  "id": "curse_herobrine",
  "requires_anchor": false,
  "anchor_boost": true,
  "valid_anchors": ["skull", "mirror", "poppet", "effigy", "bound_heart"],
  "symptoms": [],
  "clues": [],
  "cleansing_methods": [],
  "escalation": {}
}
```

### Anchor rules

An anchor stores:

- curse type
- target UUID/name
- caster UUID/name, if appropriate
- start time
- escalation level
- clue state
- optional blood/taglock reference

### Player gratification review

Anchors create the best Witchery experience because they make curses feel like mysteries, not just debuffs. Caster gets power; victim gets a hunt.

---

## 10. First Curse Set

### 10.1 Curse of Herobrine

A forbidden haunting curse.

**Effects:**

- distant Herobrine apparition at night
- reduced sight or darkness pulses
- brief Weakness
- fake block-breaking sounds
- fake door knocks
- local whispers
- paranoia ambience

**Apparition rules:**

- appears at medium/far distance
- watches silently
- never directly attacks
- vanishes when approached
- vanishes when stared at too long
- appears more often if curse escalates

**Anchors:**

- skull
- mirror
- poppet
- pale/forest effigy
- heartbound anchor later

**Counterplay:**

- break or cleanse anchor
- mirror cleansing
- white chalk door sigil
- protective familiar near sleep

**Gratification:** extremely high. Iconic, scary, readable, and perfect for distant horror. Keep it eerie, not goofy.

### 10.2 Curse of the Moving Trees

A forest curse where trees shift when the victim looks away.

**Effects:**

- nearby trees may shift 1–4 blocks when unobserved
- forest paths feel wrong
- navigation becomes unreliable
- maps/compasses may feel less trustworthy in cursed zone

**Safety:**

- do not destroy player builds unless config allows
- prefer fake/temporary tree entities or generated curse-zone trees where possible
- avoid griefing claims/protected areas

**Clues:**

- fallen leaves where tree stood
- root trails
- puppets facing anchor
- Witchlight Candle burns green nearby

**Gratification:** high if subtle. The player should doubt themselves before realizing the forest is moving.

### 10.3 Curse of the Wrong Door

A threshold curse.

**Effects:**

- doors sometimes lead to wrong nearby exit
- brief disorientation after crossing
- knocking from behind
- eerie door sounds at night

**Counterplay:**

- white chalk door sigil
- mirror cleansing
- break threshold anchor

**Gratification:** strong if rare. Do not overuse, or it becomes annoying.

### 10.4 Curse of Rooted Steps

**Effects:**

- standing still too long causes roots/slowness
- stronger in forests and Witch Forest
- can be tied to forest-heart magic

**Gratification:** good because it changes behavior without hard disabling the player.

### 10.5 Curse of Sleeplessness

**Effects:**

- poor sleep
- nightmares
- phantom sounds
- may wake player early

Do not build a full dream dimension yet. Keep this as overworld/nightmare pressure.

### 10.6 Curse of Spoiled Harvest

**Effects:**

- crops and animals behave badly near target
- minor yield penalties or visual rot
- animals may avoid target

Use config limits to avoid grief.

### 10.7 Curse of the Watching Wood

**Effects:**

- Creaking-like sounds in forests
- sense of being watched
- possible stalk behavior if linked to blood/taglock and heart familiar

### 10.8 Curse of Spontaneous Sleep

A mischievous but unsettling displacement curse. The target is overcome by sudden magical sleep, blacks out briefly, then wakes up somewhere nearby.

**Effects:**

- target abruptly falls asleep or collapses into a short sleep state
- after a brief fade/blackout, target wakes 10–20 blocks away
- preferred wake locations are nearby safe indoor spaces, porches, barns, huts, beds, or sheltered corners
- if no safe indoor/sheltered location exists, choose a safe nearby surface within range
- can trigger a few times per Minecraft day, with strong cooldown and anti-spam limits
- target may wake with brief disorientation, reduced movement, or “Where am I?” ambience

**Rules:**

- never place the player in lava, void, suffocation, deep water, or obvious death traps
- avoid moving players during boss fights, high fall risk, PvP combat, or protected/server-disabled situations
- do not steal items or alter inventory
- do not move the player across dimensions
- should feel like uncanny witch mischief, not hard crowd-control abuse

**Anchors:**

- bed trace
- poppet
- red-thread bundle
- sleep charm
- hut/house threshold anchor
- mirror or Witchlight Candle near the victim’s bed

**Clues:**

- victim hears a lullaby, creaking floorboards, distant whisper, or soft knock before sleep
- red thread, ash, candle wax, or a feather appears near the old position
- Witchlight Candle burns blue near the anchor
- protective familiars become restless before the sleep event

**Counterplay:**

- white chalk door sigil near bed/house entrance
- poppet protection
- mirror cleansing
- break or cleanse sleep anchor
- sleep near black cat, owl, or Creaking familiar

**Gratification:** very high if rare and safe. The caster gets a hilarious “witch prank” curse; the victim gets a memorable mystery moment without losing progress.

---

## 11. Counter-Curse Tools

Prioritize approved protections.

### White Chalk Door Sigil

- blocks weak spirits
- reduces Herobrine night entry events
- helps against Wrong Door
- placed over/near door or window

### Mirror Cleansing

- points toward curse anchor
- weakens direct curses
- may reflect weak curses
- can reveal sender/anchor direction briefly

### Poppet Protection

- absorbs curse, bite, misfortune, or damage
- consumes durability or breaks
- can be strengthened with blood and red thread

### Mirror Curse Reflection

- reflects weak curses back to sender or anchor
- strong curses may only reveal direction

### Player gratification review

Counterplay must feel satisfying. When a player protects a doorway, reflects a curse, or finds an anchor, give visible feedback: smoke, sound, particles, broken thread, extinguished candle, or a curse-cleansed message.

---

## 12. Ritual Power and Witch Shelves

### Ritual power inputs

Use approved inputs as modifiers:

- altar base power
- shelves nearby
- chalk type
- moon phase
- time of day
- weather
- biome
- correct place
- offerings
- local power favor
- special structures: Grove Heart, Living Hut Heart, Bound Heart

### Formula concept

```text
ritual_power = (base_power + shelf_bonus + altar_bonus + offering_bonus + favor_bonus)
             * moon_modifier
             * biome_modifier
             * weather_modifier
             * time_modifier
```

### Important user note

Biome, weather, place, and time are primarily **for ritual power**, not constant hard locks. Use hard locks only for special rites.

### Witch Shelves

Shelves near rituals should visibly matter. They may hold:

- blood vial
- skull
- red thread
- candle
- poppet
- bone charm
- Witchlight Candle
- rare plant
- old ritual note

Shelf contents can affect:

- ritual strength
- range
- duration
- curse clue quality
- local power attention
- familiar behavior

### Chalk

Multiple chalk types are approved. Full “chalk grammar” is deferred.

Initial chalk types:

- white — protection/cleansing
- red — blood/binding
- black — curse/death
- green — forest/growth
- pale — watcher/heart magic
- ominous — stronger but riskier
- copper — deferred unless copper system is active

### Player gratification review

Shelves are high-value because they let the player *see* their witchcraft. A good altar room should look powerful before the player clicks anything.

---

## 13. Feature Family: Vanishing Block Witch Hut

**Feature name:** Vanishing Block Witch Hut  
**Registry suggestion:** `vanishing_witch_hut`

A signature encounter.

### Feature behavior

- normal block structure
- rare in Witch Forest and related dark biomes
- contains Forest Hag / Witch NPC
- after one trade, hag may say: **“We may not meet again.”**
- player exits
- if player looks away, sleeps, leaves line of sight, or leaves area, the hut relocates
- when player turns back, only traces remain

### Important tone

- not a chicken-legged hut
- not an animated creature
- not a boss
- an impossible place made of blocks

### Traces left behind

- moss square
- ash pile
- candle wax
- loose plank
- faint smoke
- dead leaves
- strange empty clearing

No chicken-leg tracks.

### Anti-cheese

- hut cannot move while player is inside
- hut cannot move while directly watched
- some huts are unbreakable until addressed/unlocked

### Holding the hut

Approved methods:

- red thread on door
- salt line if implemented
- iron nail if implemented
- paid offering
- Bone Key later

Holding the hut has a **10% chance to anger the witch**.

### Living Hut Heart

Not all huts have one. Rare advanced huts may contain a hidden heart block that ties into:

- vanishing behavior
- Creaking hut call
- shrine logic
- Witch Grove hints

### Player gratification review

Very high. This is the “tell your friend” encounter. The key is timing: let the player trade, read the line, step out, look away, then feel the shock when the hut is gone.

---

## 14. Feature Family: Forest Hag

**Feature name:** Forest Hag  
**Registry suggestion:** `forest_hag`

A rare NPC who feels like a Witchery witch, not a villager reskin.

### Roles

- trader
- tester
- curse-giver
- bargain-maker
- ritual hint source
- local power intermediary

### Behavior

- manners matter
- repeated interaction can improve or worsen trades
- rude/violent behavior can trigger curse risk
- trades may require strange payment
- some rewards create debt or taboo

### Approved systems

- hag NPC
- test of manners
- impossible tasks
- bargain system
- forest crone trader
- three-hag progression
- hut must be addressed/unlocked
- child-horror softened into dolls, poppets, lost spirits, and unsettling lore only

### Deferred

- stove trial
- mortar travel
- swept-away tracks

### Player gratification review

The Hag should give players meaningful choices: accept a risky bargain, refuse, trick her, pay properly, or try to hold her hut. Do not make her only a shop.

---

## 15. Cursed Well and The Well Below

**Feature name:** Cursed Well  
**Local Power:** The Well Below

Cursed Wells connect most directly to **The Well Below**.

### Theme

- wells
- water
- drowned secrets
- old doors
- bargains
- hidden passages
- whispered clues

### Offerings

- emeralds or coins
- bones
- red thread
- blood vial
- bread
- water bottles
- rare roots

### Outcomes

- favor gain
- curse clue
- hidden anchor direction
- strange loot
- bad bargain
- temporary debuff
- distant whisper

### Player gratification review

A well should feel like a gamble: “What happens if I throw this in?” Keep the results surprising but not random nonsense. The best rewards are clues, rare ingredients, and weird bargains.

---

## 16. Ancient Witch Bonfire Event

**Feature name:** Ancient Witch Bonfire  
**Registry suggestion:** `ancient_witch_bonfire`

A rare ritual clearing event.

### Activation

At midnight, full moon, or special nights:

- ghostly flame appears
- shelves/puppets/candles react
- ritual power increases
- hag or coven silhouettes may appear
- distant fog apparitions and noises occur
- rare rite fragments may become obtainable

### Uses

- ritual discovery
- local power offering
- curse strengthening
- curse cleansing
- familiar binding clue
- Witch Forest progression

### Player gratification review

This should feel like stumbling into an old sabbath site. Reward the player for returning at the right time.

---

## 17. Local Powers and Alignment

**Registry suggestion:** `local_power`

Local Powers are place-bound forces, not menu gods.

### Relationship paths

- **Devotee:** reliable miracles, strict taboos
- **Binder:** contractual bargains, flexible but costly
- **Usurper:** steals power, strong but dangerous

### Feature mechanics

- shrine anchors
- favor
- wrath
- offerings
- taboos
- rare miracles
- avatar/familiar interaction

### Approved powers

#### The Watching Wood

- hidden hearts
- forest watchers
- night
- Creaking / Pale Garden magic
- groves

#### The Old Hearth

- home protection
- warmth
- sleep
- food
- house safety

#### The Well Below

- wells
- drowned secrets
- old doors
- bargains

#### The Noon Lady

- open fields
- heat
- exposure
- crop/field curses

#### The Winter Mother

- snow
- spinning/thread
- judgment
- winter nights

#### The Forest Elder

English replacement for forest-spirit bargain ideas.

- animals
- hunting
- lost paths
- tree punishment
- forest bargains

#### The Red Hunger

- blood
- hunger
- night feeding
- vampire-adjacent rites

#### The Deep Listener

- sculk-like depth
- death
- echoes
- vibration
- warnings

### Offerings and alignment

Milk and bread offerings should be placed at shrines and affect:

- favor
- alignment
- minor haunting calm
- ritual modifiers
- shrine trade outcomes

### Player gratification review

Local Powers work best when they react visibly but rarely. Favor should feel earned through behavior, offerings, and places — not a UI grind.

---

## 18. Creaking-Style Familiar and Bound Heart

**Feature names:** Rite of the Watchful Bond, Bound Heart, Heartbound Familiar

Use vanilla Creaking/Pale Garden naming only if the dependency exists. Otherwise use Witchery-owned equivalents.

### Feature principle

The ritual binds the **heart**, not the body.

### Ritual result

- heart block becomes Bound Heart
- body becomes familiar manifestation
- if body dies, it can reform from the heart
- if heart is destroyed, familiar is lost, weakened, or enraged

### Approved modes

- **Guard Heart** — protects heart block
- **Watch Circle** — guards ritual circles and detects intruders
- **Stalk Taglock** — follows bloodmarked/taglocked targets from distance
- **Hollow Form** — disguises as log/tree/statue
- **Grove Patrol** — patrols Witch Grove

### Explicitly deferred

- direct Hunt mode

### Behavior

- emerges from nearby trees
- watches from distance
- protects ritual circles
- stalks bloodmarked/taglocked targets
- can answer special hut calls
- interacts with witness plant/effects

### Player gratification review

This is the strongest familiar concept because it is not just a pet. The player gets power, atmosphere, and vulnerability: hide the heart well or lose the familiar.

---

## 19. Witch Grove and Barkbound Teaser

### Witch Grove

A player-created magical forest base.

Components:

- Grove Heart
- hidden hearts
- effigies
- bone fences
- roots
- familiars
- ritual clearings
- shrine anchors

### Grove Heart

Advanced heart-block variant used for:

- grove anchor
- familiar anchor
- ward source
- local power shrine source
- heartbound curse source

### Grove behavior

- rituals are stronger inside
- familiars patrol better
- wards can detect or slow intruders
- curses and cleansing become more dramatic
- spiritual attention increases

### Barkbound teaser

Approved direction:

- hidden hearts
- roots
- tree movement
- forest travel
- Creaking familiar synergy
- axe/fire/open-desert weakness

Do not build the full transformation tree until Witch Grove and Bound Heart are stable.

### Player gratification review

The Grove gives long-term ownership. It should feel like the player has grown a witch base, not built a normal house.

---

## 20. Approved Simple Familiars

Do not implement full temper/jealousy/offering system yet.

Use simpler behavior first:

- **Black Cat:** detects spirits, eats mosquitoes, warns before curses
- **Toad/Frog:** brewing bonus, poison resistance, eats mosquitoes, protects wells/swamps
- **Raven/Crow:** death omens, taglock scouting, graveyard clues
- **Owl:** night vision, forest spirit detection
- **Fox:** finds buried poppets and hidden curse anchors
- **Allay:** carries ritual ingredients, retrieves items, helps find curse anchors
- **Copper Golem:** workshop helper
- **Heartbound Familiar:** forest guardian, stalker, heart protector, grove patrol

### Deferred

- full Pact Familiar framework
- temper system
- jealousy
- maintenance offerings
- taboos
- snake/goat familiars

### Player gratification review

Familiars should provide visible utility, not just passive buffs. The best familiar moments are when they save, warn, reveal, retrieve, or interrupt something.

---

## 21. Modern Minecraft Integration

Implement only approved/high-fit integrations first.

### Ominous Bottles

Use as ritual corruptors:

- stronger ritual
- more risk
- better reward
- possible backlash

### Trial Keys / Vaults

Use to unlock rare rite fragments or forbidden items. Do not build full Trial Chamber witch trials yet.

### Breeze Wind Magic

Use for:

- Wind Jars
- broom upgrades
- knockback rites
- fall protection

### Deferred modern integrations

- full Trial Chamber witch trial system
- Heavy Core gravity system
- copper oxidation ritual system
- sculk soul magic
- archaeology rite discovery
- sniffer plant discovery
- Happy Ghast travel

---

## 22. Plants and Ingredients

Approved ingredients:

- **Moonroot:** moon, werewolf, transformation, night rituals
- **Grave Fern:** necromancy, ghosts, graves, soul jars
- **Blood Nettle:** blood magic, mosquito, vampire rites
- **Dreamcap Mushroom:** sleep, nightmares, limited dream effects
- **Silverleaf:** cleansing, protection, anti-werewolf
- **Ash Lily:** fire protection, infernal rites, burnt offerings
- **Hollow Reed:** spirit flutes, whispers, ghost communication
- **Sunbane:** vampire sunlight protection or anti-vampire brews
- **Eyeblossom Ink / Witness Ink:** hidden glyphs, watcher magic, night-visible writing
- **Resin Clumps / Heart Resin:** sealing, preserving, binding, curse anchors, heart rites
- **Cursed Silk:** web grove ingredient for poppets, thread, and traps

Deferred:

- Black Flowers unless needed by Witch Forest generation or fear rites

### Player gratification review

Each plant should have a clear use. Avoid adding plants that are only generic crafting filler.

---

## 23. Deferred / Do Not Build Yet

### Broad systems

- full dream dimension
- broad object memory
- full chalk grammar
- full familiar temper/offering/taboo system
- possession system
- complex spirit workers
- full trial chamber witch trials
- broad sculk soul magic
- copper oxidation ritual system
- archaeology rite discovery
- sniffer plant discovery
- Happy Ghast travel

### Mosquito upgrades

- swarm
- silent
- vampiric
- pale
- ominous
- blood-spirit path

### Creaking / Pale Garden systems

- direct Hunt mode
- full forest-path looping
- broad resin memory
- bone fence + Creaking ward, unless needed by Grove later

### Folklore-heavy systems

- full winter event cycle
- sky rider/wild hunt event
- house helper spirit as separate entity
- river wraith haunting
- full field spirit system
- blood-revenant transformation
- broad salt/iron universal protection

---

## 24. Recommended Implementation Order

### Slice 0 — Remake Integration Audit

Inspect existing Witchery remake:

- mod id and package layout
- registry helpers
- ritual APIs
- taglock/poppet classes
- altar power logic
- familiar ownership
- config/data-gen setup
- worldgen and structures
- networking and saved data

Deliverable: short implementation notes showing which existing systems are extended.

### Slice 1 — Witch Forest Atmosphere

Implement:

- Witch Forest biome
- canopy, fog, cliffs, webs, clutter
- bonfire clearings
- broken fences
- hanging witch puppets
- basic loot
- ambient sounds

Goal: player finds the biome and instantly understands the mood.

### Slice 2 — Blood and Binding

Implement:

- Witch Needle
- Blood Vial
- Blood freshness
- Red Thread
- blood-as-taglock API
- basic poppet protection extension

Goal: blood becomes a practical Witchery resource.

### Slice 3 — Mosquito in a Bottle

Implement:

- Mosquito Bottle
- mosquito entity
- nearest-player targeting
- bite/return
- Bottle of Stolen Blood
- Itching / Bloodmarked
- counterplay
- cooldown/config

Goal: first iconic new tool.

### Slice 4 — Anchored Curses

Implement:

- curse registry/type extension
- anchor data
- symptom tick behavior
- clues
- cleansing hooks
- config safety

First curses:

- Herobrine
- Moving Trees
- Wrong Door
- Rooted Steps
- Sleeplessness
- Spontaneous Sleep

Goal: curses become stories.

### Slice 5 — Vanishing Hut + Hag

Implement:

- Vanishing Block Witch Hut
- Forest Hag
- one-trade relocation
- “We may not meet again”
- traces
- unbreakable-until-unlocked
- 10% anger chance if held

Goal: signature encounter.

### Slice 6 — Ritual Power + Shelves

Implement:

- Witch Shelves
- multiple chalk types
- altar power modifiers
- biome/time/weather/moon modifiers
- offerings
- Witchlight Candle scan behavior

Goal: preparation matters.

### Slice 7 — Heartbound Familiar

Implement:

- Rite of the Watchful Bond
- Bound Heart
- heartbound familiar manifestation
- Guard Heart / Watch Circle / Stalk Taglock / Hollow Form / Grove Patrol
- tree emergence
- heart destruction consequence

Goal: signature modern familiar.

### Slice 8 — Local Powers MVP

Implement first:

- The Watching Wood
- The Old Hearth
- The Well Below

Add:

- shrine anchors
- offerings
- favor
- wrath
- rare simple miracles

Goal: explain wells, offerings, grove magic, and alignment.

### Slice 9 — Witch Grove + Barkbound Teaser

Implement:

- Grove Heart
- Witch Grove range/boundary
- grove ritual bonus
- grove wards
- first Barkbound teaser effects

Goal: long-term progression.

---

## 25. Registry Name Suggestions

| Registry key | Display name |
|---|---|
| `witch_forest` | Witch Forest |
| `hanging_witch_puppet` | Hanging Witch Puppet |
| `old_bonfire_clearing` | Old Bonfire Clearing |
| `ancient_witch_bonfire` | Ancient Witch Bonfire |
| `vanishing_witch_hut` | Vanishing Block Witch Hut |
| `forest_hag` | Forest Hag |
| `witch_needle` | Witch Needle |
| `blood_vial` | Blood Vial |
| `stolen_blood_bottle` | Bottle of Stolen Blood |
| `red_thread` | Red Thread |
| `mosquito_bottle` | Mosquito in a Bottle |
| `dead_mosquito_bottle` | Dead Mosquito Bottle |
| `bloodmarked` | Bloodmarked |
| `itching` | Itching |
| `curse_herobrine` | Curse of Herobrine |
| `curse_moving_trees` | Curse of the Moving Trees |
| `curse_wrong_door` | Curse of the Wrong Door |
| `curse_rooted_steps` | Curse of Rooted Steps |
| `curse_sleeplessness` | Curse of Sleeplessness |
| `curse_spoiled_harvest` | Curse of Spoiled Harvest |
| `curse_watching_wood` | Curse of the Watching Wood |
| `curse_spontaneous_sleep` | Curse of Spontaneous Sleep |
| `witchlight_candle` | Witchlight Candle |
| `white_chalk_door_sigil` | White Chalk Door Sigil |
| `mirror_cleansing` | Mirror Cleansing |
| `witch_shelf` | Witch Shelf |
| `bound_heart` | Bound Heart |
| `rite_watchful_bond` | Rite of the Watchful Bond |
| `grove_heart` | Grove Heart |
| `witch_grove` | Witch Grove |
| `watching_wood` | The Watching Wood |
| `old_hearth` | The Old Hearth |
| `well_below` | The Well Below |
| `forest_elder` | The Forest Elder |
| `red_hunger` | The Red Hunger |
| `deep_listener` | The Deep Listener |

---

## 26. Multiplayer Safety Guardrails

For curses and mosquito behavior:

- cooldowns
- range limits
- config toggles
- anti-grief checks
- owner/team/coven exclusions where supported
- warning cues
- cleansing methods
- debug logs for curse source
- claim-protection compatibility if possible

Avoid:

- unavoidable blindness
- permanent build grief
- infinite mosquito spam
- curse effects with no clue
- constant jump scares
- non-consensual permanent progression loss

---

## 27. Player Interaction Gratification Review

This pass intentionally focuses on player satisfaction. The design now has clear interaction loops.

### 27.1 Discovery gratification

**Strong features:** Witch Forest, hanging puppets, vanishing hut, ancient bonfire, cursed well.

Why it works:

- visible landmarks invite curiosity
- players can interact without reading a manual first
- exploration produces loot, clues, and danger

### 27.2 Mischief gratification

**Strong features:** mosquito bottle, stolen blood, anchored curses, Wrong Door, Herobrine.

Why it works:

- the caster gets a memorable tool
- the victim receives a readable mystery
- counterplay keeps it from feeling unfair

### 27.3 Preparation gratification

**Strong features:** shelves, red thread, blood vials, altar power, chalk types, offerings.

Why it works:

- the player’s base visually reflects power
- preparation affects outcome
- Witchery keeps its slow ritual identity

### 27.4 Fear gratification

**Strong features:** Herobrine apparition, moving trees, Witch Forest ambience, puppets, hags.

Why it works:

- fear is mostly distant, subtle, and atmospheric
- scares escalate over time
- players can investigate and respond

### 27.5 Relief gratification

**Strong features:** anchor cleansing, mirror cleansing, chalk door sigils, poppet protection.

Why it works:

- the player gets a clear “I solved it” moment
- curse removal should have visible/audio payoff

### 27.6 Ownership gratification

**Strong features:** Bound Heart, heart familiar, Witch Grove, Grove Heart, shrine favor.

Why it works:

- the player builds something personal and strange
- power has a location and vulnerability
- the grove becomes identity, not just decoration

### Verdict

The design now has strong gratification coverage. The best first release slice should prioritize:

1. Witch Forest discovery
2. blood/red thread tools
3. Mosquito in a Bottle
4. anchored Herobrine or Moving Trees curse
5. Vanishing Hut encounter

Those five together will immediately feel like Witchery, while also feeling new.

---

## 28. Genuine Witchery Feeling Checklist

Before implementing any feature, ask:

1. Does it use a physical object, ingredient, place, or ritual?
2. Can the player prepare for it?
3. Can another player investigate or counter it?
4. Does it create a memorable story?
5. Does it avoid generic spellcasting?
6. Does it fit inside a Witchery altar room, hut, grove, shelf, poppet system, or curse system?
7. Does it produce visible feedback when it succeeds or fails?
8. Would it still be fun after the surprise is gone?

If yes, it belongs.

---


## 29. Integration Review — Does This Strengthen Old Witchery?

### Overall verdict

The feature set integrates well if it is implemented as **additive Witchery content**, not as a separate folklore mod. The strongest through-line is:

> Witch Forest exploration gives physical clues and ingredients → blood/red thread improve taglocks and poppets → mosquito creates stolen blood → stolen blood powers anchored curses → anchors can be hidden in puppets, shelves, huts, wells, or hearts → ritual shelves and altar power make preparation matter → Creaking/heart familiar and Witch Grove become late-game physical ownership.

That loop supports classic Witchery instead of replacing it.

### Highest-fit integrations

1. **Mosquito in a Bottle → Taglock Kit / poppet / curse pipeline**
   - Best integration in the whole spec.
   - It gives a new way to obtain target material, then lets old Witchery systems do the work.
   - Recommended first iconic feature after blood vial support.

2. **Anchored curses → existing curse effects + physical counterplay**
   - Very Witchery.
   - Keep normal curses as-is; anchored curses are upgraded versions with clues, hidden objects, and stronger payoff.
   - Poppet protection and mirror cleansing should plug into the same resolution path.

3. **Witch Forest → altar power + rite conditions + structure loot**
   - Good if the biome is useful, not just decorative.
   - Add altar/rite bonuses for Witch Forest blocks, clearings, bonfires, puppets, and grove structures.

4. **Vanishing Block Witch Hut → witch hut/coven/hag interaction**
   - Excellent “Witchery story moment.”
   - Keep the hut made of blocks. The magic is the relocation rule, not animation.

5. **Heartbound Familiar → familiar summoning + heart anchor**
   - Strong late-game addition.
   - It should feel like a darker, physical cousin of classic familiars.

6. **Witch Grove → altar room evolution**
   - Works best as an extension of altar power, rite modifiers, and familiar territory.
   - It should not become a base-building tech tree.

### Features to keep carefully scoped

- **Local Powers:** keep as shrine/favor modifiers. Do not build a big god UI.
- **Barkbound:** teaser only until heart/familiar/grove systems are stable.
- **Spontaneous Sleep:** high-gratification but needs server safety checks and cooldowns.
- **Moving Trees:** use fake/temporary curse-zone trees where possible to avoid griefing.
- **Herobrine:** keep as apparition and sound/vision curse, not a combat mob.

### Features that need explicit old-system hooks before coding

- Witchlight Candle: define whether it extends altar scanning, curse detection, or both.
- Mirror Cleansing: define whether it is a ritual, item use, block interaction, or poppet-like protection.
- White Chalk Door Sigil: define whether it is a chalk glyph, block decal, or small ward block.
- Cursed Well: define whether it is a structure, shrine anchor, ritual block, or local power interface.
- Hag trades: define which existing Witchery recipes/progression they unlock or shortcut.

### Recommended integration-first MVP

Build in this order:

1. **Blood Vial as advanced taglock** — must reuse existing target identity/taglock logic.
2. **Witch Needle** — manual blood collection and poppet interaction.
3. **Mosquito in a Bottle** — mobile blood collector that produces Blood Vials/Bottle of Stolen Blood.
4. **Anchor data layer for curses** — optional extension to existing curse effects.
5. **One anchored curse: Herobrine** — high player gratification, low mechanical risk.
6. **One protective counter: White Chalk Door Sigil or Mirror Cleansing** — makes curses fair.
7. **Witch Forest biome with hanging puppets** — atmosphere and ingredient source.
8. **Vanishing Block Witch Hut + Forest Hag** — memorable progression encounter.
9. **Ritual shelf modifiers** — makes existing altar/ritual spaces more satisfying.
10. **Heartbound Familiar** — late MVP / first major content milestone.

### Genuine Witchery feel check

The file now has the right direction, but implementation should enforce these constraints:

- every new curse must be castable or strengthened by existing Witchery-style rites;
- every blood feature must feed taglocks, poppets, or curses;
- every structure must contain loot, clues, trades, or ritual value;
- every new familiar must plug into existing ownership/command conventions;
- every protective item must give visible feedback;
- every major feature should create a story moment, not just a stat buff.

### Player interaction gratification confirmation

The new features have strong interaction payoff because they give both sides something to do:

- the witch prepares, hides anchors, steals blood, arranges shelves, and sends familiars;
- the victim hears clues, notices symptoms, follows traces, protects doors, finds anchors, and cleanses curses;
- explorers find puppets, bonfires, huts, wells, and fog shapes;
- builders turn altar rooms into visible witch spaces;
- late-game players grow a grove and hide hearts.

This is the right kind of gratification for Witchery: slow, physical, mischievous, eerie, and personal.

---

## 30. Final Implementation Instruction

Read [CURRENT_SCOPE_HANDOFF.md](./CURRENT_SCOPE_HANDOFF.md) first, then implement this as an **additive expansion** to the existing Witchery NeoForge 1.21.11 remake.

Prefer small PR-sized slices. Use the remake’s current code style, registry patterns, data-gen conventions, configs, storage, and networking. If a new feature conflicts with an existing Witchery mechanic, adapt the new feature to the old mechanic rather than replacing the old mechanic.

The target feeling is:

> The player should not feel like they installed a spell mod. They should feel like the old Witchery world started hiding new things in the forest.
