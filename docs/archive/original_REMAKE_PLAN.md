# Witchery → NeoForge 1.21.11 — Detailed Remaking Plan

> Reference sources: original decompiled jars, WitcheryResurrected 1.12.2, weebly getting-started wiki  
> Last updated: 2026-05-15

---

## Current Status (M1–M10 complete)

The mod loads, renders, and has working gameplay loops for:
- Kettle brewing (10 recipes, 4 brew effects)  
- 3 growable crops with loot tables  
- 14 BESR block renderers  
- Ritual circles (2 rites)  
- Poppet death prevention  
- Player data persistence + sync  

---

## Original Mod Scope (what we're remaking to)

| Category | Original count | Remade so far |
|---|---|---|
| Blocks | 42+ | ~35 registered, most lacking functionality |
| Ingredient items | 70+ | 8 |
| Poppet types | 12 | 1 (death prevention) |
| Armor/clothing sets | 5+ | registered, no function |
| Entities / mobs | 32 | 0 |
| Ritual rites | 87 registered | 2 |
| Kettle recipes | 30+ | 10 |
| Specialized crafting systems | 4 (oven, distillery, spinning, brazier) | 0 functional |
| Infusion types | 4 (light, overworld, otherwhere, infernal) | 0 |
| Dimensions | 2 (dream world, torment) | 0 |
| Village structures | 2 (apothecary, witch hut) | 0 |
| Creature powers / symbiosis | 20+ | 0 |

---

## Five Magic Branches (from original design)

The original Witchery mod is organized around five interlocking branches.  
Everything below maps to one or more of these:

1. **Brewing** — kettle, oven, distillery, ingredients, brews  
2. **Circle Magic** — chalk, ritual circles, rites, altars, glyphs  
3. **Poppet Magic** — 12 poppet types, taglock kit, sympathetic magic  
4. **Dream Weaving** — dream world dimension, dream weave items, nightmare entities  
5. **Infusions** — vampire, werewolf, spirit bonding, creature powers  

---

## Phase A — Complete the Brewing Loop (highest immediate value)

*These unlock most existing brew recipes and make the mod playable as a "witchcraft brewing mod".*

### A1 — Witches' Oven (Medium)
**Purpose:** Smelts special ingredients. Wood → Wood Ash. Odd Pork (from pig flesh) → cooked. Primary source of Wood Ash for brewing.

**What to build:**
- `WitchesOvenBlockEntity` with 1 fuel slot, 1 input slot, 1 output slot  
- Smelting tick logic (burns items like a vanilla furnace)  
- `WitchesOvenMenu` + `WitchesOvenScreen` (3-slot GUI)  
- Recipes JSON: `data/witchery/recipe/oven/wood_to_ash.json` etc.  
- Register `RecipeType` for oven recipes  

**Oven recipe list (original):**
- Any wood log → Wood Ash (×2)
- Raw Pork → Odd Pork Cooked  
- Diamond → Diamond Vapour (ingredient for advanced brews)
- Gypsum (nether quartz) → Quicklime (ingredient)

**Dependencies:** BlockEntity already registered + renderer done.

---

### A2 — Distillery (Medium)
**Purpose:** Combines brews with other ingredients to create enhanced or modified potions. Secondary crafting for advanced brew effects.

**What to build:**
- `DistilleryBlockEntity` — 2 input slots, 1 fuel slot, 1 output slot  
- `DistilleryMenu` + `DistilleryScreen`  
- `DistilleryRecipes` — hardcoded or JSON  

**Key distillery recipes (original):**
- Brew + modifier ingredient → enhanced brew variant  
- Brew of Frost + Diamond Vapour → Brew of Ice (longer effect)  
- Brew of Sleeping + Bat Wool → Brew of Depths  

**Dependencies:** A1 (some distillery inputs come from oven).

---

### A3 — Spinning Wheel (Medium)
**Purpose:** Spins cotton into thread, thread into cloth. Cloth used in poppets, armor, wicker bundles.

**What to build:**
- `SpinningWheelBlockEntity` — 1 input, 1 output  
- `SpinningWheelMenu` + `SpinningWheelScreen`  
- Cotton crop growth logic (already registered as simpleBlock — convert to CropBlock)  
- Recipes: Cotton → Thread → Cloth  

**Key recipes (original):**
- 1 Cotton → 1 Cotton Thread  
- 4 Cotton Thread → 1 Cloth (shapeless)  
- Cloth used in: poppets, witch hat, robes, wicker bundle  

**Dependencies:** Cotton crop (can be done alongside).

---

### A4 — Remaining Kettle Recipes + Ingredients (Easy)
**Purpose:** Make use of the ingredient items already registered.

**What to add:**
- Brew of Sleeping recipe — already has JSON but uses vanilla items. Update to use `witchery:belladonna` + other ingredients.
- Wire remaining 6 brews with effects:
  - **Brew of Raising** — resurrects a nearby dead mob as undead servant  
  - **Brew of Wasting** — wither effect + slowness on target area  
  - **Brew of Erosion** — degrades blocks in area (like acid rain)  
  - **Brew of Foulness** — nausea + poison on nearby mobs  
  - **Brew of Infection** — spreads poison to other mobs near target  
  - **Brew of the Grotesque** — massive strength + slowness, mutates player appearance temporarily  
  - **Brew of Flowing Spirit** — creates flowing spirit fluid  
  - **Brew of Foaming** — covers area in foam (slowing terrain)  

**New ingredient items to register:**
- `bat_wool` — dropped by bats  
- `odd_pork` / `odd_pork_cooked` — from pigs via oven  
- `quicklime` — from gypsum (nether quartz) via oven  
- `diamond_vapour` — from diamonds via oven  
- `oil_of_vitriol` — crafted ingredient  
- `foul_fume` — from distillery  
- `demon_heart_item` — rare drop from demons  
- `wormwood` — already registered, needs plant (see B3)  

---

## Phase B — Complete the Garden (plants + worldgen)

### B1 — Cotton Crop (Easy)
- Convert `COTTON` from `registerSimpleBlock` to `CottonBlock extends CropBlock`  
- 4 stages (disturbed_cotton texture exists for mature stage)  
- Drops: Cotton Thread × 1-3 at maturity  
- Loot table: `data/witchery/loot_table/blocks/cotton.json`

### B2 — Witch Crop / Rowan Berries (Easy)
- Convert `WITCH_CROP` from simpleBlock to `WitchCropBlock extends CropBlock`  
- 7 stages (uses wheat textures as placeholder)  
- Drops: Rowan Berries at maturity  
- Rowan Berries: key ingredient for many rituals and the altar  

### B3 — Wormwood + Snowbell Plants (Easy)
- Register `WORMWOOD` and `SNOWBELL` crop blocks (textures exist)  
- Wormwood drops `wormwood` item (already registered)  
- Snowbell drops `snowbell` item (need to register)  
- Both: 5 stages, CropBlock pattern  

### B4 — World Generation (Hard)
- **Witch Sapling → Witch Tree** — grow a rowan tree (witch wood log + leaves)  
  - Use `TreeFeature` JSON or custom tree grower  
- **Village Apothecary hut** — structure that spawns in villages  
  - Small hut with kettle, cauldron, chest with starter ingredients  
- **Village Witch Hut** — witch NPC hut  
- **Bramble patches** — natural bramble spawning in forests  
- **Wild belladonna, mandrake** — sparse crop generation in specific biomes  

**Deferred:** full worldgen can wait until core gameplay is stable.

---

## Phase C — Circle Magic (complete the system)

### C1 — Chalk Types (Easy)
Original has 4 chalk types, each draws different glyph patterns:
- `CHALK_RITUAL` — standard ritual circle (already implemented)
- `CHALK_OTHERWHERE` — otherwhere transport circles (teleport rituals)
- `CHALK_INFERNAL` — infernal summoning circles (demon rituals)
- `CHALK_GOLDEN` — golden glyph (altar power glyphs)

**What to build:** 3 more `ChalkItem` variants with different `CIRCLE` block targets; `GLYPH_OTHERWHERE` and `GLYPH_INFERNAL` blocks.

### C2 — Altar Power System (Hard)
**Purpose:** The altar provides "power" to ritual circles and powered kettle recipes.  
- `AltarBlockEntity` — tracks power level (0–1000)  
- Power radius scan: counts specific blocks nearby (witch wood, witch leaves, candles, chalices, braziers, skulls, etc.)  
- Power regenerates over time  
- `KettleBlockEntity.tick()` already has placeholder for altar power check  

**Power block contributions (original):**
| Block | Power |
|---|---|
| Witch Wood Log | +2 |
| Witch Leaves | +0.5 |
| Candelabra (lit) | +5 |
| Brazier (lit) | +10 |
| Chalice (filled) | +15 |
| Statue of Goddess | +20 |
| Alluring Skull | +5 |
| Demon Heart | +25 |
| Grass, Dirt, etc. | small amounts |

### C3 — Full Ritual Rites (Hard, ongoing)
Official rite names from the original mod website, grouped by priority:

**Tier 1 — Core progression (unlock familiar + infusion systems):**
- Rite of Binding — binds circle talismans, waystones, familiars, or spectral creatures to the caster  
- Rite of Beastial Call — animals teleport from wide area INTO the circle; useful for farms  
- Rite of Protection — converts altar power into a force field barrier (7×7 circle, 0 power)  
- Rite of Fertility — bonemeal entire area; also cures zombie villagers in range  
- Rite of the Forest — creates a forest of chosen tree type outside the circle; supports vanilla + witchery trees  
- Rite of Infusion — infuses items, players, or brews (see Phase E); also makes Infused Brew Base  
- Rite of Summoning — summons: Demon, Flame Imp, Wither, Coven Witch, or Spectral Familiar  

**Tier 2 — Environment & terrain:**
- Rite of Shifting Seasons — alters biome in area; uses Book of Biomes to pick target biome  
- Rite of Sky's Wrath — calls down lightning strikes; cast inside circle = strikes inside, outside = outside  
- Rite of Broken Earth — creates a crevice in the ground; direction set by where a Brew of Erosion is placed  
- Rite of Moving Earth — raises blocks UP (cast in the circle's diameter; blocks go 10 blocks down then rise)  
- Rite of Earth's Wrath — brings lava up from underground to form a volcano directly inside the circle  
- Rite of Total Eclipse — moves the sun to night position (makes it night temporarily)  
- Rite of Icy Expansion — creates a breathable spherical dome under water (useful for underwater bases)  
- Rite of Broiling — mass food-cooking ritual; burns food slightly (7×7 circle, 0 altar power)  

**Tier 3 — Transport & utility:**
- Rite of Transposition — teleports the witch to the Bound Waystone location; also transposes entities or ores  
- Rite of Manifestation — sends a sleeping player into the overworld as a dream self for 2.5 minutes  
- Rite of Sanctity — prevents hostile mobs from ENTERING the circle (Rite of Imprisonment is the inverse)  
- Rite of Imprisonment — prevents hostile mobs from LEAVING the circle (trap mobs inside)  
- Rite of Charging — charges Attuned Stones with altar power; also charges players with power  
- Rite of Remove Curse — removes curses from players; also can send back demons  
- Rite of Prior Incarnation — recalls items lost from the player's most recent death; must be cast within 16 blocks  
- Rite of Necromancy — creates Necromantic Stone (control regular undead) and Spectral Stone (control spectres)  

**Tier 4 — Curses (dark magic):**
All curses persist through death unless removed by Rite of Remove Curse or Voodoo Protection Poppet.

- **Curse of Blindness** — all players in a LARGE area become blind; countered by Voodoo Protection Poppet  
- **Curse of Blight** — large area: flora/fauna dies, earth decays to sand, nearby villagers zombified  
- **Curse of Misfortune** — random debuffs inflicted on victim continuously; countered by Voodoo Protection Poppet  
- **Curse of Insanity** — hallucinatory monsters appear to victim that slowly die (psychic attack)  
- **Curse of Sinking** — victim is heavier in water, sinks faster and rises slower; countered by Voodoo Protection Poppet  
- **Curse of Overheating** — random spontaneous combustion in hot biomes; countered by Voodoo Protection Poppet  
- **Curse of Waking Nightmare** — pulls Nightmares from the Spirit World into the overworld near the victim; Nightmares outside Spirit World are weaker  
- **Curse of Corrupt Poppet** — destroys up to 10 of target's poppets; **requires a cat familiar** to cast; boosted by cat familiar  
- **Curse of Raining Toads** — toads rain down at target location and explode in a poisonous mist; countered by Voodoo Protection Poppet  
- **Curse of the Wolf** — permanently mutates victim into a lycanthrope; persists after death; countered only by Rite of Remove Curse  
- **Curse of Hell on Earth** — opens a doorway to Hell, releases Flame Imps; boosted by cat familiar  
- **Creatures of the Night** — permanently mutates victim into a **vampire** (this is a curse, not a rite); persists after death  

**Tier 5 — Conjuration (necromancy / spirit work):**
- Summon Spectre — bring a spectre entity to the circle  
- Summon Banshee — bring a banshee to the circle  
- Graveyard Mist — summon fog and spawn undead near graves  
- Deathly Veil — hide the caster from undead detection  
- Rite of Necromancy — resurrect a dead mob as servant  
- Rite of Prior Incarnation — summon an entity from a past event  
- Rite of Glyphic Transformation — transform an entity using a glyph  
- Rite of Imprisonment — trap an entity in a circle  
- Rite of Banishing — banish a demon or summoned entity  
- Rite of Nature's Power — supercharge nearby plants and ores  

### C3b — Fetish Spells (Medium)
Fetishes are **spells** applied to physical blocks (Scarecrow, Trent Effigy, or Witch's Ladder). Each fetish type has a trigger radius that fires when players walk near the bound block.

| Fetish | Effect on nearby players |
|---|---|
| **Disorientation** | Disorients/confuses players who come near (Nausea, random spin) |
| **Ghost Walking** | Allows dreaming (dream-state) players to walk through the fetish's area |
| **Sentinel** | Sends a notification to the bound player when others approach |
| **Shrieking** | Shrieks loudly at approaching players (sound + fear) |
| **Voodoo Protection** | Protects nearby players from voodoo effects |

**Implementation:** Each Scarecrow/Trent Effigy/Witch's Ladder block entity stores which fetish spell is applied. On player proximity (entity tick or scheduled scan), apply the fetish effect to players in range. The spell is applied via the ritual circle system.

### C4 — Circle Talisman System (Medium)
- Bind a stored ritual circle pattern to the talisman item  
- Deploy stored pattern by right-clicking with talisman  
- Requires `RiteBindCircleToTalisman` from C3  

---

## Phase D — Poppet Magic (complete the system)

We have 1 of 12 poppet types. Here is the full list with mechanics:

| Poppet | Effect | How used |
|---|---|---|
| **Death Protection** ✅ | Prevents one death, consumes poppet | In inventory of bound player |
| **Earth Protection** | Prevents fall damage once | In inventory |
| **Water Protection** | Prevents drowning once | In inventory |
| **Fire Protection** | Prevents fire damage once | In inventory |
| **Hunger Protection** | Prevents starvation | In inventory |
| **Tool Protection** | Prevents tool breakage | In inventory |
| **Armor Protection** | Prevents armor damage | In inventory |
| **Anti-Voodoo** | Prevents voodoo targeting | In inventory |
| **Voodoo** | Used offensively against bound player | In poppet shelf |
| **Vampiric** | Drains health from bound player to user | In poppet shelf |
| **Poppet Protection** | Protects poppets from destruction | In shelf |
| **Unbound** | Base poppet, needs taglock to bind | — |

### D1 — Taglock Kit (Medium)
- Right-click a player to collect their taglock (stores UUID + name)  
- Right-click a sleeping player for higher-quality taglock  
- Used to bind poppets and create filled taglocks for rituals  
- `TaglockKitItem` with states: empty → filled  

### D2 — Remaining Poppet Types (Medium per type)
All protection poppets follow the same pattern: listen to `LivingDamageEvent`, check if damage source matches, if so cancel damage and shrink the poppet stack. Poppet must be in player's inventory OR on a nearby Poppet Shelf.

| Poppet | Trigger | Effect |
|---|---|---|
| **Earth Protection** | Fall damage (lethal) | Cancel death, consume poppet |
| **Water Protection** | Drowning damage (lethal) | Cancel death, consume poppet |
| **Fire Protection** | Fire damage (lethal) | Cancel death, consume poppet |
| **Hunger Protection** | Starvation damage (lethal) | Cancel death, consume poppet |
| **Tool Protection** | Tool would break (durability 0) | Restore tool to full, consume poppet |
| **Armor Protection** | Armor piece would break (durability 0) | Restore armor to full, consume poppet |
| **Anti-Voodoo / Voodoo Protection** | Voodoo poppet effect, vampiric poppet, any curse | Absorbs the effect, consume poppet |
| **Poppet Protection** | Curse of Corrupt Poppet would destroy a Voodoo Protection Poppet | Absorbs curse, consume this poppet instead |
| **Voodoo** *(offensive)* | Held by attacker, bound to victim | Damage, burn, or other effects inflicted on victim remotely |
| **Vampiric** *(offensive)* | In inventory or shelf, bound to victim | Your incoming damage redirects to the bound player |

For Tool/Armor poppets: hook `LivingEquipmentChangeEvent` or tick-check durability. For Voodoo/Vampiric: active tick-based drain.

### D3 — Poppet Shelf Functionality (Medium)
- `PoppetShelfBlockEntity` stores up to 4 poppets  
- On each damage event: scan all shelves within 8 blocks of the player for matching poppets  
- Poppets in shelves have the same protection radius as poppets in inventory  
- Voodoo/Vampiric poppets work from shelves too (offensive use at range)  

---

## Phase E — Infusions (player transformation)

### E1 — Infusion Base System (Medium)
The Rite of Infusion uses a special kettle brew as the infusion "vessel". Each infusion type uses a different brew base made in the kettle:

| Infusion | Brew Base Item | Kettle Recipe (key ingredients) | Player Effect |
|---|---|---|---|
| **Infusion of Light** | Ghost of the Light | Redstone Soup + Invisibility Potion + (others) | Control light; day immunity effects |
| **Infusion of Overworld** | Soul of the World | Redstone Soup + Potion of Regeneration + (others) | Nature-based abilities; plant growth aura |
| **Infusion of Otherwhere** | Spirit of Otherwhere | Redstone Soup + Potion of Swiftness + (others) | The End powers; phase/teleport effects |
| **Infernal Infusion** | Infernal Animus | Redstone Soup + Potion of Harming II + Demon Heart + Voodoo Poppet | Enthrall creatures; sacrifice for power; darkness |

**Also brewed for infusion:** 
- **Mystic Unguent** — for infusing the Mystic Branch: Redstone Soup + Strength Potion (8:00) + Diamond + Rowan Sapling + Creeper Heart
- **Flying Ointment** — for infusing the Enchanted Broom; poisonous if drunk directly
- **Happenstance Oil** — ingredient for Crystal Ball and Seer Stone: Redstone Soup + Night Vision Potion (8:00) + Eye of Ender
- **Infused Brew Base** — base for all Infused Brews (Soaring, Grave, etc.); two recipe variants

**Infused Brews (made from Infused Brew Base via Rite of Infusion):**
- **Infused Brew of Soaring** — better broom control: faster, easier turning, quicker deceleration  
- **Infused Brew of the Grave** — while active, Necromantic Stone commands regular undead  

### E2 — Vampirism (Hard, uses M9 data)
- **Infection:** bite from a vampire NPC or voodoo poppet  
- **Blood mechanics:** blood level 0–500 (stored in WitcheryPlayerData)  
  - Drains over time; must bite mobs/players to refill  
  - 0 blood = weakness, frenzy  
  - High blood = strength, speed  
- **Abilities:** bat form, fog form, charm, mesmerise  
- **Weaknesses:** sunburn (fire damage in daylight), garlic, holy water  
- **Progression:** 5 vampire levels, each unlocking abilities  
- **Cure:** complex ritual  

### E3 — Lycanthropy (Hard, uses M9 data)
- **Infection:** bite from a werewolf NPC  
- **Transformation:** triggers at full moon (or by howling)  
- **Wolf form:** massively increased speed/damage/hunger  
- **Weaknesses:** silver, wolfsbane (wormwood in original)  
- **Pack mechanics:** nearby werewolves share buffs  
- **Cure:** complex ritual  

### E4 — Creature Powers / Symbiosis (Medium)
- Acquired by killing specific mobs during special infusion ritual  
- Gives passive ability matching the creature:  
  - Spider → wall-climbing  
  - Bat → echolocation (entity-sensing)  
  - Ghast → tear projectile  
  - Blaze → fire breath  
  - Enderman → limited teleport  
  - Etc. (20 total)  

---

## Phase F — Entities / Mobs

### F1 — Familiars (Hard)
Three bindable familiar types:
- **Owl** — flies overhead, can scout, warns of dangers  
- **Cat** — follows player, repels phantoms/creepers  
- **Toad** — hops around, passive ingredient collection  

Implementation: custom entity type + goals + `RiteBindFamiliar` activation.

### F2 — Witch NPCs (Hard)
- **Coven Witch** — friendly witch NPC that lives in witch huts; trades ingredients  
- **Witch Hunter** — hostile NPC that tracks and attacks witches  
- **Baba Yaga** — rare neutral boss that trades powerful items  

### F3 — Horror Entities (Hard)
- **Banshee** — scream attack, wails near dying players  
- **Spectre** — semi-transparent, passes through walls  
- **Nightmare** — horse-like, appears in Dream World  
- **Poltergeist** — throws items at players  

### F4 — Summoned/Transformed Entities (Hard)
- **Treefyd** — golem made of witch wood, summoned by ritual  
- **Ent** — tree spirit; passive until attacked  
- **Mandrake mob** — baby mandrake creature when mandrake is harvested at night  
- **Demon** — summoned through infernal ritual circle  
- **Lord of Torment** — boss entity in Torment dimension  
- **Imp** — small fire demon, servant of the Lord of Torment  

### F5 — Utility Entities (Medium)
- **Broom** — rideable entity, enchantable for speed/altitude  
- **Brew projectile** — thrown brew bottle entity  
- **Louse** — parasytic louse entity (attaches to mobs)  

---

## Phase F2 — Additional Brews (from official site)

The official site lists these brews not yet in our recipe files:
- **Brew of Hollow Tears** — stores spirit essence  
- **Brew of Bats** — summons bat swarm as distraction  
- **Brew of Substitution** — swap position with another entity  
- **Brew of Bodega** — creates a temporary food supply  
- **Redstone Soup** — gives haste and speed temporarily  
- **Infused Brew Base** — foundation for enhanced brews (distillery output)  
- **Infused Brew of Soaring** — extended levitation / flight  
- **Infused Brew of the Grave** — partial undead features temporarily  
- **Brew of Cursed Leaping** — extreme jump boost with fall damage  
- **Brew of Frog's Tongue** — extended reach / pull items to you  
- **Solidifying Brews** — turn liquids (water/lava) to solid  

---

## Phase G — Dream Weaving

### G1 — Dream World Dimension (Hard)
- Custom dimension JSON + `ChunkGenerator`  
- Enters via ritual circle + dream weave item while sleeping  
- Exits by waking up (death or morning)  
- Twisted landscape, fog, nightmare entities spawn  
- Maze-like generated structures  

### G2 — Dream Weaver Items (Medium)
These are **decorative wall-hanging items placed near the player's bed**. They are not worn — they're placed as blocks on walls. Effects apply when the player wakes up from sleeping. Items must be within a few blocks of where the player sleeps.

Correct item names (from wiki):
- **Dream Weaver of Fleet Foot** — Speed boost on wake (not "Move")
- **Dream Weaver of Iron Arm** — Strength boost on wake
- **Dream Weaver of Fasting** — Boosts saturation on wake (not "Eat")
- **Dream Weaver of Intensity** — Intensifies ALL other weavers; adds bonus to each
- **Dream Weaver of Nightmares** — Increases likelihood of entering the Spirit World during a nightmare

**Implementation:** `DreamWeaverBlock` (wall block), `PlayerSleepInBedEvent` listener scans nearby dream weavers and applies effects on wake.

### G3 — Rite of Manifestation (Hard)
The "Dream World entry" rite actually sends **sleeping players into the overworld as their dream self** (spectral form) for 2.5 minutes — it's not a separate dimension portal. The Spirit/Nightmare World dimension is separate from this.

**How it works:**
- Player must be sleeping in a bed
- Rite of Manifestation is cast at the sleeper's location
- Sleeping player's "dream self" appears in the overworld at the same position
- Dream self lasts ~2.5 minutes; returning to bed ends the rite
- Dream self can be attacked by Nightmares that spawn nearby  

### G4 — Scrying / Crystal Ball (Medium)
- Right-click Crystal Ball to scry a bound player's location  
- Shows compass direction + distance  
- Requires taglock of target player  

### G5 — Magic Mirror (Hard)
- Reflects players/projectiles into a parallel space  
- Fairest of them All reflection mechanic (cosmetic double)  

---

## Phase H — Torment Dimension

### H1 — Torment Dimension (Hard)
- Entered via infernal ritual circle  
- Hellish landscape (fire, basalt, tortured terrain)  
- Contains the Lord of Torment (boss)  
- Imps and demons spawn here  
- Killing Lord of Torment → major dark power reward  

---

## Phase I — Village Structures & Worldgen

### I1 — Witch Hut (Medium)
- Village structure: small hut with cauldron, mushrooms, chest  
- Coven Witch spawns inside  
- Chest loot: starter witch ingredients  

### I2 — Apothecary Hut (Medium)
- Village structure: herbalist shop  
- Sells ingredients for emeralds  
- Has bookshelves with Witchery books  

### I3 — Wild Plant Spawning (Easy)
- Belladonna: sparse in plains/forests  
- Mandrake: sparse in dark oak forests  
- Blood Rose: in swamps and flower forests  
- Ember Moss: in nether or near lava  
- Glint Weed: underwater/rivers  
- Spanish Moss: in jungles hanging from trees  

### I4 — Witch Tree (Medium)
- Custom `TreeGrower` for witch sapling → rowan tree  
- Uses `WITCH_LOG` + `WITCH_LEAVES` with custom shape  
- May occasionally grow with Rowan Berries attached  

---

## Phase J — GUIs / Menus (all medium difficulty)

Each functional block needs a proper screen. NeoForge pattern: `AbstractContainerMenu` + `Screen`.

| Block | Slots | Priority |
|---|---|---|
| Witches Oven | fuel, input, output | A1 (this phase) |
| Distillery | 2× input, fuel, output | A2 |
| Spinning Wheel | input, output | A3 |
| Poppet Shelf | 4× poppet display | D3 |
| Leech Chest | full 27-slot chest | Medium |
| Kettle advanced | view ingredients/water/color | Low (current interaction works) |
| Brazier | recipe display | Low |

---

## Phase K — Polish & Systems

### K1 — Sounds (Easy)
- Wire existing extracted sound files to events  
- Kettle bubbling, oven crackling, ritual activation, poppet save  

### K2 — Predictions (Medium)
8 diviner prediction types (using the diviner item):
- Arrow, Fall, Fight, Love, NetherTrip, Rescue, Treasure, Wet  
- Each predicts a future event and displays as a tooltip message  

### K3 — Books & Documentation Items (Easy)
5 in-game books from original:
- Book of Oven Lore, Book of Distilling, Book of Circle Magic, Book of Infusions, Book of Herbology  
- Register as `WrittenBookItem` with pre-written pages  

### K4 — Waystone System (Medium)
- Bind a waystone to a location  
- `RiteTeleportToWaystone` uses it for circle-based fast travel  

### K5 — Config System (Easy)
- `ModConfigSpec` (NeoForge toml config)  
- Ritual power costs, potion durations, vampire/werewolf strengths  
- Dimension IDs, world gen toggles  

---

## Recommended Milestone Order

| Milestone | Phase | Approx effort | Unlocks |
|---|---|---|---|
| M11 | A1 — Witches Oven GUI + smelting | 3–4h | Wood Ash, advanced ingredients |
| M12 | A4 — Remaining brew effects (6) | 3h | Full brewing loop playable |
| M13 | B1–B3 — Cotton/Witch Crop/Wormwood | 2h | Spinning wheel inputs, more ingredients |
| M14 | A3 — Spinning Wheel GUI | 3h | Cloth → poppets, armor |
| M15 | A2 — Distillery GUI | 3h | Enhanced brews |
| M16 | C2 — Altar power system | 4h | Powered kettle recipes work |
| M17 | C3 Tier 1 — Core rituals (7 rites) | 4h | Familiar binding, protection circles |
| M18 | D1+D2 — Taglock + 5 more poppet types | 4h | Core poppet magic playable |
| M19 | E1 — Infusion base system | 3h | Gateway to transformations |
| M20 | F1 — Familiar entities | 5h | Owl/cat/toad companions |
| M21 | C3 Tier 2–3 — Environment rituals | 4h | Teleport, weather, terrain |
| M22 | E2 — Vampirism | 6h | Full vampire gameplay |
| M23 | E3 — Lycanthropy | 6h | Full werewolf gameplay |
| M24 | F2 — Witch NPCs | 5h | Coven witch, witch hunter |
| M25 | G1 — Dream World dimension | 8h | Dream weaving |
| M26 | I1–I4 — Worldgen + structures | 6h | World feels alive |
| M27 | F3–F4 — Horror + summoned entities | 8h | Full monster roster |
| M28 | H1 — Torment dimension | 6h | Endgame content |
| M29 | K1–K5 — Polish pass | 4h | Release quality |

---

## Token-Efficient Architecture Notes

These patterns minimize future token cost per feature:

**Adding a brew effect:** Create `BrewOf{Name}Item extends BrewItem`, override `applyEffect()`. One file, ~15 lines.

**Adding a rite:** Add new `CircleSize` enum value, add offset array in `RitualCircleDetector`, add case in `RitualExecutor.execute()`. No new files needed for simple rites.

**Adding a protection poppet:** Add case in `PoppetEvents.onPlayerDeath()`, check `DamageSource` type. ~10 lines per poppet.

**Adding an oven/distillery recipe:** One JSON file in `data/witchery/recipe/oven/`.

**Adding a BESR block:** One line in `WitcheryBlockItemRenderer.registeredItems()`, one entry in `WitcheryClient.LAYER_DEFS`. No other files change.

---

## Future Milestone — M30: Creaking Integration

> **Version prerequisite:** The vanilla `Creaking` entity and `CreakingHeart` block were added in MC 1.21.4.
> This milestone requires upgrading to **NeoForge for MC 1.21.4+** before implementation.
> All design below is finalised; implementation is gated on the version bump.

### Concept

The Creaking is a pale, statue-like horror that only moves when unobserved. Witchery's theme of
secret knowledge, the unseen world, and pacts with entities fits perfectly. The integration makes
the Creaking a *witchcraft resource* and a *ritual guardian* rather than a pure combat mob.

---

### Task 1 — Creaking Heart as a ritual ingredient

The `CreakingHeart` block can be broken by the player (drops itself with silk-touch).
Add it as a high-tier ingredient in the kettle and as a ritual circle component.

**New kettle recipes (add JSON):**
| Recipe | Inputs | Output | Notes |
|---|---|---|---|
| Brew of Stillness | Creaking Heart fragment + Icy Needle + Condensed Fear | `brew_of_stillness` | Freezes mobs that look at the target |
| Essence of the Watching | Creaking Heart + Tear of Goddess + Attuned Stone | `essence_of_watching` | Ingredient for the Ward ritual |

**New ingredient item:** `creaking_heart_fragment` — obtained by crafting `CreakingHeart` with a Boline.
Register in `WitcheryItems`, add model, lang entry, kettle recipes.

---

### Task 2 — Brew of Stillness

A brew that coats the drinker's surroundings: mobs within 12 blocks that have line-of-sight to the
player become Frozen for as long as they face the player (similar to the vanilla Creaking mechanic).
Effect duration: 2 minutes.

**Implementation:**
- `BrewOfStillnessItem extends BrewItem`
- `PlayerTickEvent`: each tick while effect is active, raytrace all nearby LivingEntities;
  if the entity's look vector points toward the player within 30° → apply `FreezeTicksTag` via
  `entity.setTicksFrozen(40)` (reset each tick they are watching).
- On effect end, unfreeze everything in range.

---

### Task 3 — Ward of the Watching (ritual rite)

A ritual that binds a naturally-spawned Creaking to the player's ritual circle location.
The bound Creaking becomes a **guardian**: it attacks any hostile mob that approaches within
24 blocks of the circle centre, as long as the player isn't looking at it directly.

**Circle:** 5×5 golden chalk with a Creaking Heart at the centre (world-placed, not broken).
**Rite:** `RiteOfWatching` — detects the Creaking Heart block inside the circle.

**Behaviour post-binding:**
- The bound Creaking doesn't despawn and keeps its vanilla freeze-when-watched mechanic.
- It ignores the binding player but attacks all other hostile mobs.
- Destroying the Creaking Heart block breaks the ward and causes the Creaking to despawn.
- NBT tag `witchery:bound_circle` stores the circle origin BlockPos on the Creaking entity.

**Files:**
- `ritual/rites/RiteOfWatching.java`
- Add entry to `RitualExecutor.execute()` switch
- New circle size constant (5×5 = offset radius 2) in `RitualCircleDetector`

---

### Task 4 — Creaking familiar (optional stretch goal)

A tamed Creaking that follows the player silently. Unlike vanilla behaviour it *does* move when
the owning player watches it, but freezes for all other observers.

**Taming:** complete the Ward of the Watching rite, then right-click the bound Creaking while
holding a `Taglock Kit` filled with your own blood (taglock on self). The Creaking becomes tamed.

**Behaviour:**
- Follows owner like a vanilla wolf (sits on command).
- `isWatchedByNonOwner(entity)` helper: returns true if any non-owner player within 16 blocks has
  line-of-sight → freeze the familiar.
- On owner death: familiar despawns with a wail (`SoundEvents.CREAKING_DEATH`).

---

### Lang keys to add

```json
"witchery.item.creaking_heart_fragment":           "Creaking Heart Fragment",
"witchery.item.brew_of_stillness":                 "Brew of Stillness",
"witchery.item.essence_of_watching":               "Essence of the Watching",
"witchery.rite.watching":                          "Rite of the Watching",
"witchery.rite.watching.success":                  "The Creaking heeds your call.",
"witchery.familiar.creaking.bound":                "The pale guardian is bound.",
"witchery.familiar.creaking.broken":               "The ward is broken. The guardian is gone."
```

---

### Estimated effort

| Task | Hours |
|---|---|
| Ingredient item + recipes | 1h |
| Brew of Stillness | 2h |
| Ward ritual rite | 3h |
| Creaking familiar | 3h |
| **Total** | **~9h** |

**Adding an entity:** `EntityType` registration + `MobEntity` subclass + goals. Standard NeoForge pattern.

---

# Witchery — Obscure & Poorly-Documented Mechanics

> Compiled from: decompiled jars (witchery-1.7.2-0.19.0, WitcheryResurrected-1.12.2, witchery-companion),
> FTB wiki scrape (C:\claude\witchery\FTW Witchery Web\), official site scrape (C:\claude\witchery\Witchery Web\).
> Each entry states its confidence level: **Confirmed** (in source), **Community** (reported, no source), **Rumor** (unverified).

---

## 1. Avada Kedavra Kill Curse

**Status:** Confirmed (decompiled source — Witchery Companion mod)  
**Source:** `AvadaKedavraSymbolEffectMixin.java` in witchery-companion decompiled jar

### What it does
A symbol-based spell (infusion symbol effect) that fires a projectile. On collision it deals damage to the target — and under specific conditions **instantly kills** the target (calls `EntityUtil.instantDeath()`).

### How players trigger it
- Obtained via an Infusion symbol (drawn glyph), not via a simple item
- Damage amount is configurable via Witchery Companion config
- With a **negative damage value** in the config, it becomes a true one-hit kill
- Reduced damage against: bosses, demons, Flame Imps, Golems; full instant-death for standard mobs and Creative-mode players

### Damage breakdown (from mixin)
| Target | Effect |
|--------|--------|
| Standard mobs | Configurable damage / instant kill |
| Bosses | Reduced damage, no instant kill |
| Demons / Imps | Special reduced multiplier |
| Coven Witch | Specific damage value |
| Golems | Different calculation |
| Creative player | `instantDeath()` always called |

### Notes for implementation
- Implement as a `SymbolEffect` (infusion ability) activated via the Infusion of Otherwhere
- Create a targeting projectile entity; on `onHit()` call `entity.hurt(...)` with configurable damage
- Gate behind a high-level infusion + attuned stone charge

---

## 2. Lilith Vampire Leash Trick

**Status:** Rumor — No code evidence found  
**Source:** Searched `EntityLilithMixin.java` and all vampire-related classes; found only enchantment toggles for Lilith, no leash mechanic.

### What the rumor claims
Lilith (the vampire boss) can be "leashed" in the overworld by a creative player, turning her into a source of infinite vampire bites — allowing mass vampirism spreading without requiring the Curse of Creatures of the Night ritual.

### What we actually found
- Lilith code only contains: enchantment enable/disable toggle (Witchery Companion tweak)
- No `EntityLiving.isLeashable()` override
- No "overworld spawn" codepath for Lilith without the Lord of Torment kill chain

### Implementation note
Do not implement. If adding Lilith, mark `isLeashable()` as false and confine her to the Torment dimension or summoned-only spawn.

---

## 3. Doomcat Rumor

**Status:** Rumor — Community legend, no code evidence  
**Source:** Searched `EntityCatFamiliarMixin.java`, all familiar code; found only standard loot-table fixes.

### What the rumor claims
A "Doomcat" is a maxed-out cat familiar with a specific name that becomes invincible and deals massive damage. Some reports claim it can kill the Ender Dragon in seconds.

### What we actually found
- Cat familiar code is standard familiar behavior
- No special-name detection, no power scaling based on familiar age or kills
- Witchery Companion only adds: loot table override, owner-disconnect fix

### Implementation note
When implementing cat familiars, no special power scaling is needed. The "Doomcat" appears to be a community myth about outlier behavior from stacked armor/potion effects.

---

## 4. Magic Mirror, Reflection Demon, and Skin/Name Masquerading

**Status:** Confirmed (decompiled WitcheryResurrected source)  
**Source:** `BlockMirror.java`, `TileEntityMirror.java`, `EntityReflection.java`, `EntityFairest.java`

### What it does
The **Magic Mirror** is a 2-block-tall structure. When a player walks through it:
1. An `EntityReflection` is spawned — a demon copy of the player
2. The reflection copies the player's skin (UUID-based texture) and name
3. The reflection acts as a hostile entity using symbol effects: Ignianima, Expelliarmus, Flipendo, Impedimenta, Confundus
4. **"Fairest of them All"** (`EntityFairest`) is a named reflection variant that masquerades as the player in chat and nametag — essentially a player-shaped impostor

### How it triggers
- Place the 2-tall Magic Mirror block
- Walk through it (requires 3000F power from nearby altar for some modes)
- The reflection spawns on the other side
- `mirrorGoHome()` and `mirrorGiveUp()` are cooldown-based escape methods

### Skin/Name masquerading details
- Reflection stores player UUID in NBT
- Client renders the reflection using the player's actual skin
- Name tag shows the original player's name
- Chat messages from the reflection appear as the player (original Witchery behaviour — likely a grief tool on multiplayer servers)

### Notes for implementation
- **Mirror block:** `BlockMirror extends BaseEntityBlock`, 2-tall multiblock
- **EntityReflection:** Custom mob, copies player texture UUID on spawn
- **EntityFairest:** Named subvariant; add nametag override + chat listener intercept
- Gate behind high altar power (3000+); rare and late-game
- **Milestone:** Phase G5 in REMAKE_PLAN.md

---

## 5. Witch's Cauldron — Custom Brew Charges, Item Charges, Dispenser Automation

**Status:** Partially Confirmed  
**Source:** `BlockWitchCauldron.java` (WitcheryResurrected); dispenser interaction not found in examined code

### Brew charges
- Brews stack to 3 by default (already implemented in `BrewItem.stacksTo(3)`)
- Some brews in the original had configurable "charges" — drinking consumes 1 charge, not the whole stack
- This is the vanilla behaviour for Witchery brews and is already implemented correctly

### Item charges (advanced)
- Certain ritual items (e.g., Attuned Stone) can be **charged** with altar power via `Rite of Charging`
- The charged item stores power as NBT; active abilities drain charge per use
- `ItemAttunedStoneCharged` in the original holds a power int in NBT

### Dispenser automation
- No confirmed code for automatic cauldron feeding via dispensers in the original 1.7.10 mod
- WitcheryResurrected adds some automation hooks but details not extracted
- **Community claim:** Dispensers can drop items into a lit kettle from above to automate ingredient insertion

### Notes for implementation
- Kettle `entityInside()` already handles items falling in (M5 implementation)
- This naturally enables dispenser automation already — items dispensed into the kettle space will land and get picked up by `entityInside()`
- No extra code needed for basic dispenser automation

---

## 6. Altar Power Cost Multiplier / Ritual Circle Cost Reduction

**Status:** Confirmed (decompiled WitcheryResurrected source)  
**Source:** `TileEntityAltar.java` — fields `powerScale`, `rechargeScale`, `rangeScale`

### How it works
The altar has three hidden scalars that modify its behavior:

| Field | Effect | Default |
|-------|--------|---------|
| `powerScale` | Multiplies maximum power storage | 1.0 |
| `rechargeScale` | Multiplies power recharge rate per tick | 1.0 |
| `rangeScale` | Extends the block-scanning radius | 1.0 |

Power formula:
```
maxPowerStored = basePower * powerScale
rechargePerTick = basePowerPerUpdate * rechargeScale
```

### How players modify scalars (original)
- Building larger ritual circles near the altar increases `powerScale`
- Using a Chalice filled with a specific brew sets `rechargeScale`
- Placing a Statue of the Goddess boosts `powerScale` by +20 (already in our implementation)

### Circle size → cost reduction
- Larger chalk circles reduce the power cost of rites (scaled by circle radius)
- A 7×7 circle might cost 500 power while a 3×3 costs the same rite at 300 power
- **Not yet implemented in our remake** — our `RitualExecutor` has fixed effects

### Notes for implementation
- Add `powerMultiplier` field to `AltarBlockEntity` that is recalculated based on nearby ritual circles
- `RitualExecutor` should query `altar.getPower()` and apply `powerMultiplier` to cost
- Add `rechargeScale` based on `Chalice` blocks filled with specific brews

---

## 7. Minedrake Mutation via Mutandis

**Status:** Confirmed mechanic exists; specific Minedrake output not extracted  
**Source:** `ItemMutandis.java` (WitcheryResurrected), `EntityMandrake.java`

### What Mutandis does
- **Mutandis:** Right-click on crops/saplings to randomly mutate them to different plants
  - Can convert: Nether Wart → Belladonna/Mandrake/Snowbell/Artichoke/Wormwood
  - Can convert: Vanilla flowers → Witchery plants
  - Random outcome — multiple possible results per source plant
- **Mutandis Extremis:** More powerful variant; can mutate Minedrake itself

### Minedrake-specific mutation chain
From community reports (partially confirmed in class structure):
- **Mandrake (mature, harvested at night)** → sometimes spawns Minedrake *mob* instead of dropping root
- **Minedrake plant** (underground block, not harvestable directly) → mutated via Mutandis Extremis
- Minedrake mob drops `Minedrake bulb` — when planted, grows Minedrake plant
- Minedrake plant drops can be used in specific kettle recipes

### Notes for implementation
- `ItemMutandis` works as a right-click item on specific blocks
- Implement as a weighted random table: `source_block → [possible_outputs]`
- Gate Minedrake access behind night-time mandrake harvesting (random drop)
- Minedrake mob (small mandrake creature) entity needed for full chain

---

## 8. Tormentum — Sending Players to the Torment Dimension

**Status:** Confirmed (decompiled source)  
**Source:** `BlockTormentPortal.java`, `TormentumSymbolEffect.java`, `ItemTormentContract.java`

### What it does
`Tormentum` is a **symbol effect** (infusion ability or demonic contract power) that banishes a targeted player or mob to the Torment dimension against their will.

### How it triggers
1. **Via symbol effect:** Cast during an Infernal Infusion ritual — player fires a Tormentum projectile; on hit, teleports the target to Torment
2. **Via Torment Portal block:** The `BlockTormentPortal` has a **5% chance per tick** to pull nearby players in (passive hazard if placed)
3. **Via Lord of Torment kill chain:** Defeating the Lord of Torment completes a cycle that may send the caster briefly to Torment

### Escaping Torment
- Find another Torment Portal block to return
- Wait for the portal's timer to expire (time-limited visit)
- `ItemTormentContract` system — pre-negotiate escape terms with a demon before entering

### Torment Portal passive hazard (5% chance)
```java
// Line 38 of BlockTormentPortal (original)
if (random.nextFloat() < 0.05f) { increaseTorment(); }
```
This makes placing Torment Portals near other players a griefing tool.

### Notes for implementation
- Torment dimension: separate dimension JSON + `ChunkGenerator` (Phase H1 in REMAKE_PLAN)
- `TormentumSymbolEffect`: projectile entity; on hit, call `player.changeDimension(tormentLevel, ...)`
- Torment Portal passive hazard: `BlockTormentPortal.entityInside()` with 5% check
- Gate: requires Infernal Infusion + charged Attuned Stone

---

## 9. Waystone Chalk-Circle Teleport — Exact Behavior

**Status:** Confirmed (decompiled source)  
**Source:** `ItemChalk.java`, `RiteEffectTeleportToWaystone.java`, `BindWaystoneRitual.java`

### Step-by-step behavior

**Step 1 — Create a Waystone item:**
- Craft `Waystone` (blank location token)
- Bind it using `Rite of Binding` at destination:
  - Draw a ritual circle at the destination
  - Place Waystone in center
  - Activate with Arthana
  - Cost: **4000F altar power**
  - Result: `Bound Waystone` (stores dimension + BlockPos in NBT)

**Step 2 — Teleport:**
- Draw a teleport circle at origin (same or different location)
- Place `Bound Waystone` in center
- Activate ritual with Arthana
- The `RiteEffectTeleportToWaystone` reads the stored position from the Waystone NBT
- All entities within the circle's radius teleport to the bound location
- **Cross-dimension support:** Can teleport between Overworld, Nether, Torment, Dream World

### Bound Waystone NBT structure
```
{dimension: "witchery:dream_world", x: 120, y: 64, z: -340}
```

### Player-bound Waystone variant
- `RiteBindCircleToTalisman` can create a **player-bound Waystone** that always teleports to the bound player's current location (dynamic target)
- Requires having a taglock of the target player

### Notes for implementation
- `WAYSTONE` item already registered (no function yet)
- `Rite of Binding`: checks for Waystone in center slot of circle, writes NBT, consumes 4000 altar power
- `Rite of Transposition`: reads Waystone NBT, calls `player.teleportTo(level, x, y, z, yaw, pitch)`
- Cross-dimension: use `ServerPlayer.teleport(ServerLevel, ...)` in NeoForge 1.21.11

---

## 10. Modpack-Only Vampire Items

**Status:** Not Confirmed — No modpack-specific items found in source  
**Source:** Searched `ItemVampireClothes.java`, all vampire-related classes in all three jars

### What was found
- Vanilla Witchery has: Vampire Clothing set (hat, robes, boots), Wooden Stake, Glass Goblet, Bottle of Warm Blood, Bottle of Lilith's Blood
- `ItemVampireClothes.java` exists in WitcheryResurrected with no modpack-specific branching
- No B Team-specific item IDs or conditional loading found

### Community claim
Players on Attack of the B-Team modpack reported unique vampire items (silver-tipped cane sword, sunlight grenades with special crafting). These appear to be **Hunter items** (Witch Hunter armor set) that exist in vanilla Witchery but are rare.

### Actual special items (vanilla, not modpack-only)
- **Cane Sword** — Witch Hunter sword that functions as a cane; confirmed in vanilla
- **Sunlight Grenade** — Throws sunlight in a radius; damages vampires; confirmed in vanilla
- **Silver Sword** — Anti-vampire weapon; confirmed in vanilla
- **Wooden Stake** — Instant-kill on sleeping vampires; confirmed in vanilla

### Notes for implementation
These are all vanilla Witchery items already in the item texture directory. Register them as items under Phase F (Entities) since their usefulness depends on vampire entities existing.

---

## Summary Table

| Mechanic | Confidence | Source | Priority for Remake |
|---|---|---|---|
| Avada Kedavra kill curse | ✅ Confirmed | AvadaKedavraSymbolEffectMixin.java | Phase E (infusion abilities) |
| Lilith vampire leash | ❌ Rumor | No code found | Do not implement |
| Doomcat familiar | ❌ Rumor | No code found | Do not implement |
| Magic Mirror / Fairest reflection | ✅ Confirmed | BlockMirror.java, EntityFairest.java | Phase G5 |
| Cauldron dispenser automation | ✅ Already works | entityInside() covers this | Done (M5) |
| Altar power multiplier (powerScale) | ✅ Confirmed | TileEntityAltar.java | M16 extension |
| Circle size → cost reduction | ✅ Confirmed | TileEntityAltar.java | C2 extension |
| Minedrake mutation chain | 🔶 Partial | ItemMutandis.java + class structure | Phase B (plants) |
| Tormentum / Torment dimension | ✅ Confirmed | BlockTormentPortal.java | Phase H |
| Waystone teleport ritual | ✅ Confirmed | RiteEffectTeleportToWaystone.java | Tier 3 rites (C3) |
| Modpack-only vampire items | ❌ Not confirmed | ItemVampireClothes.java | N/A — use vanilla list |
