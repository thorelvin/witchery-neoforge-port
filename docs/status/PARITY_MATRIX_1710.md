# Witchery 1.7.10-0.24.1 Parity Matrix

**Built:** 2026-05-28
**Baseline source:** `C:\claude\witchery\decompiled\witchery-1.7.10-0.24.1`
**Port:** NeoForge 1.21.1

Legend: ✅ Done · 🔶 Partial · 🔲 Placeholder · ❌ Missing · ⚠️ Wrong behavior

---

## Quick Summary

| Category | Source count | Port status |
|---|---|---|
| Items (classes) | ~55 item classes (~120+ variants) | 258 functional + 146 placeholders |
| Blocks | ~98 distinct types | ~99 registered; ~12 families missing |
| Entities | 56 | 26 registered; 18+ missing |
| Brew actions/effects | ~95 brew effects | ~35 items; dynamic action system missing |
| Potion effects | 43 custom classes | ~0 registered as MC effects; brews are items |
| Rituals | ~45 | ~20+ dispatched |
| Infusions (transformation types) | 4 types + creature powers | 4 types registered; creature powers missing |
| Machines (behavioral) | Kettle, Cauldron, Oven, Distillery, Spinning Wheel, Vat, Collector | Partial on all |
| Dimensions | Spirit World, Torment, Mirror World | All 3 present; Spirit/Mirror need QA |

---

## 1. Items

### 1a. Ingredients & Consumables

| Item | Status | Notes |
|---|---|---|
| All 35+ brewing ingredients (bat_wool, rowan_berries, etc.) | ✅ Done | Registered and in creative tab |
| anointing_paste | ✅ Done | Recipe + cauldron route restored |
| taglock_kit | ✅ Done | Fill, creature/player variants, crafting |
| poppets (9 types) | ✅ Done | Voodoo, Vampiric, Protection families |
| glass_goblet / glass_goblet_full | ✅ Done | Blood fill/drink/induction |
| warm_blood / liliths_blood | ✅ Done | Consumable semantics |
| observations_of_an_immortal / torn_page | ✅ Done | Book screen, update recipe, kill drops |
| subdued_spirit | ✅ Done | Capture/release loop |
| parasytic_louse (item) | ✅ Done | Potion bite/hotbar defense |
| clay_jar / clay_jar_soft | ✅ Done | Smelting route restored |
| bone_needle, all standard ingredients | ✅ Done | 35+ ingredient items present |
| treefyd_seeds | ✅ Done | Grass-top raise, model |
| koboldite family | ✅ Done | Dust/nugget/ingot/pickaxe |
| silver_ingot / silver_deposits / silver_sword | ✅ Done | Tagged damage hooks |
| wooden_stake | ✅ Done | Sleeping vampire staking |
| boline, arthana | ✅ Done | Ritual tools |
| attuned_stone / charged | ✅ Done | |
| quartz_sphere / charged_quartz_sphere | ✅ Done | Sun Collector chain |
| sun_grenade | ✅ Done | Throw/return |
| player_compass | ✅ Done | Taglock bind, direction report |
| waystone (blank/bound) | ✅ Done | Scry route, stack cap |
| circle_talisman | ✅ Done | |
| brew_bag | 🔲 Placeholder | Item registered; no container logic |
| leonards_urn | 🔲 Placeholder | Item registered; no drop/range-boost mechanic |
| moon_charm | 🔲 Placeholder | Item registered; no werewolf form-toggle mechanic |
| horn_of_the_hunt | 🔲 Placeholder | Item registered; no Huntsman-challenge mechanic |
| wolf_token | 🔲 Placeholder | Creative-only; no mechanic |
| cane_sword | ❌ Missing | Source: `ItemCaneSword` — combo weapon |
| dup_staff | ❌ Missing | Source: `ItemDuplicationStaff` |
| dupgrenade | ❌ Missing | Source: `ItemSunGrenade(1)` — mirror duplication grenade |
| biome_book / biome_note | ❌ Missing | Source: `ItemBook`, `ItemBiomeNote` — biome scrying |
| cauldron_book | ❌ Missing | Source: `ItemMarkupBook(7)` — in-game cauldron guide |
| vampire_book | ❌ Missing | Source: `ItemMarkupBook(7, [0,9])` — vampire progression book with level callbacks |
| bucket_brew | ❌ Missing | Source: `ItemBrewBucket` — brew bucket container |
| brew_fuel / brew_endless_water | ❌ Missing | Source: `ItemBrewFuel`, `ItemBrewEndlessWater` — fuel/endless brew item forms |
| stew (cooked/raw) | ❌ Missing | Source: `ItemStew` — 12-nutrition witch stew; raw variant |
| polynesia_charm | ❌ Missing | Source: `ItemPolynesiaCharm` — two variants |
| devils_tongue_charm | ❌ Missing | (in placeholder list but no mechanic planned yet) |

### 1b. Armor & Clothing

| Family | Status | Notes |
|---|---|---|
| Witch's hat/robe/shoes | ✅ Done | |
| Baba's hat / Necromancer's Robes | ✅ Done | |
| Biting Belt / Bark Belt | ✅ Done | Potion-defense / bark-charge mechanics |
| Icy Slippers / Ruby Slippers / Seeping Shoes | 🔲 Placeholder | Items registered; effects not hooked |
| Earmuffs | ✅ Done | Sound mute, Mandrake/Banshee protection |
| Hunter set (plain) | ✅ Done | |
| Hunter set (silvered) | ✅ Done | Silver damage hooks |
| Hunter set (garlicked) | 🔲 Placeholder | Items registered; no garlic-effect on wear |
| Death's Hood/Robe/Shoes | ✅ Done | Fire cancel, water-walk, low-light NV |
| Death's Hand | ✅ Done | Spectral touch + scythe deploy |
| Koboldite Helm / Mog's Quiver / Gulg's Gurdle | 🔲 Placeholder | Items registered; no goblin-tier mechanics |
| Vampire clothing sets (12 variants) | 🔶 Partial | Items/recipes present; exact level-gate parity and full ability-selection/paralysis incomplete |
| Witch Hunter Dawn set | 🔶 Partial | Items registered; armor values in; combat bonuses not fully verified |

### 1c. Weapons & Tools

| Item | Status | Notes |
|---|---|---|
| Silver Sword | ✅ Done | Tagged vampire/werewolf damage |
| Huntsman's Spear | ✅ Done | Blocking retaliation, weapon-id parity |
| Hand Bow (Crossbow Pistol) | 🔶 Partial | Dedicated item now uses source-style stored bolt loading: crouch-hold loads a preferred bolt, crouch-cycles through Wooden/Nullifying/Bone/Splitting/Silver while returning the old loaded bolt, firing uses the stored bolt, and launch behavior covers live Wooden/Silver/Nullifying/Bone/Splitting `BoltEntity` shots. Old Infinity/Power/Punch/Flame behavior is restored for loaded bolts, with nullifying bolts skipping Power/Punch/Flame. Full hunter/silvered/dawn hunter armor now powers Nullifying Bolts, draining the shooter's infusion energy on launch and half of a player target's max infusion energy on hit. Client range-dispatch predicates now show idle, pull, and loaded crossbow states. In-game projectile/visual QA remains |
| Witch Hand | 🔲 Placeholder | Item registered; no active power |
| Mystic Branch | 🔲 Placeholder | Item registered; symbol/glyph drawing system MISSING |
| Koboldite Pickaxe | ✅ Done | Mining bonuses placeholder |

---

## 2. Blocks

### 2a. Wood/Tree Families

| Block | Status | Notes |
|---|---|---|
| Rowan family (log/planks/stairs/slab/door/leaves/sapling) | ✅ Done | |
| Alder family | ✅ Done | Door BloodTrap slice done |
| Hawthorn family | ✅ Done | |
| Witchery slab variants (old metadata slabs) | ❌ Missing | Source `BlockWitchWoodSlab` — old single/double slab; modern MC uses slab blockstate natively, may not be needed |

### 2b. Perpetual Ice & Snow Family

| Block | Status | Notes |
|---|---|---|
| ice_door | ✅ Done | |
| ice_fence | ✅ Done | |
| ice_gate | ✅ Done | |
| ice_pressure_plate | ✅ Done | |
| perpetual_ice (base block) | ❌ Missing | Source: `BlockPerpetualIce` — never melts |
| ice_stairs | ❌ Missing | |
| ice_slab (single/double) | ❌ Missing | |
| ice_fence_gate | ❌ Missing | |
| snow_pressure_plate | ❌ Missing | Source: `BlockPerpetualIcePressurePlate` (snow material) |
| snow_stairs | ❌ Missing | |
| snow_slab (single/double) | ❌ Missing | |

### 2c. Ritual & Magic Structure Blocks

| Block | Status | Notes |
|---|---|---|
| altar | ✅ Done | Power system, multiblock |
| witch_cauldron / kettle | ✅ Done | |
| chalice | ✅ Done | |
| candelabra | ✅ Done | |
| brazier | ✅ Done | |
| spirit_portal / torment_portal | ✅ Done | |
| poppet_shelf | ✅ Done | |
| crystal_ball | ✅ Done | |
| dream_catcher | ✅ Done | |
| circle / circle_glyph / circle_otherwhere / circle_infernal | ✅ Done | |
| blood_crucible | ❌ Missing | Source: `BlockBloodCrucible` — vampire level-up progression block |
| shaded_glass (off/on) | ❌ Missing | Source: `BlockShadedGlass` — light-blocking glass with switch state |
| decurse_teleport | ❌ Missing | Source: `BlockAreaMarker` (TileEntityAreaTeleportPullProtect) — area curse protection |
| decurse_directed | ❌ Missing | Source: `BlockAreaMarker` (TileEntityAreaCurseProtect) |

### 2d. Machine & Processing Blocks

| Block | Status | Notes |
|---|---|---|
| witches_oven (idle/burning states) | ✅ Done | |
| fume_funnel / filtered_fume_funnel | ✅ Done | |
| distillery | ✅ Done | |
| spinning_wheel | ✅ Done | |
| sun_collector | ✅ Done | |
| daylight_collector | ✅ Done | (same as sun_collector in port) |

### 2e. Storage & Containers

| Block | Status | Notes |
|---|---|---|
| poppet_shelf | ✅ Done | |
| leech_chest | ✅ Done | |
| refilling_chest | ✅ Done | |
| coffin (block) | ✅ Done | |

### 2f. Plants & Decorative

| Block | Status | Notes |
|---|---|---|
| All 8 crop types (belladonna/mandrake/etc.) | ✅ Done | |
| garlic_garland | ✅ Done | Repulsion, facing |
| wispy_cotton / disturbed_cotton | ✅ Done | |
| blood_rose / ember_moss / glint_weed / spanish_moss | ✅ Done | |
| bramble / void_bramble | ✅ Done | |
| leaping_lily | ✅ Done | |
| grassper | ✅ Done | |
| glowglobe | ✅ Done | |
| critter_snare | ✅ Done | |
| plant_mine | ✅ Done | |
| beartrap / wolftrap | ✅ Done | Full source renderer, trap logic |
| wolf_altar | ✅ Done | |
| wicker_bundle / bloodied_wicker_bundle | ✅ Done | |
| critter_snare | ✅ Done | |
| hollow_tears | ✅ Done | Stand-in block |
| flowing_spirit | ✅ Done | |
| alluringskull | ✅ Done | |
| demon_heart | ✅ Done | |
| mirror / mirror_wall | ✅ Done | |
| silver_vat | ✅ Done | |
| statue_goddess | ✅ Done | |
| fetish family (scarecrow/trent/ladder) | ✅ Done | |
| stockade (wood) | ❌ Missing | Source: `BlockStockade(false)` — punishment/decoration block |
| stockade_ice | ❌ Missing | Source: `BlockStockade(true)` |
| blooded_wool | ⚠️ Wrong | Port has `blood_stained_wool`; verify ID matches original |
| shaded_glass | ❌ Missing | See 2c above |
| pit_dirt / pit_grass | ❌ Missing | Source: `BlockPitDirt`, `BlockPitGrass` — Pit biome variants for Torment |
| infinity_egg | ❌ Missing | Source: `BlockInfinityEgg` — dragon egg variant |
| disease (block) | ❌ Missing | Source: `BlockDisease` — flowing disease block |
| brew / brew_gas / brew_liquid (as placed blocks) | 🔶 Partial | Hollow Tears stand-in only; full moving brew gas/liquid blocks not ported |

### 2g. Cursed Redstone Variants (entire family missing)

| Block | Status | Notes |
|---|---|---|
| cursed_lever | ❌ Missing | Source: `BlockLeverBase` variant |
| cursed_wooden_door | ❌ Missing | |
| cursed_wooden_pressure_plate | ❌ Missing | |
| cursed_stone_pressure_plate | ❌ Missing | |
| cursed_snow_pressure_plate | ❌ Missing | |
| cursed_button_wood | ❌ Missing | |
| cursed_button_stone | ❌ Missing | |

*Note: These are probably low priority — they provide redstone output on curse interaction, not core gameplay.*

---

## 3. Entities

| Entity | Port status | Notes |
|---|---|---|
| coven_witch | ✅ Done | 5-skin variants, trades, greet fix |
| witch_hunter | ✅ Done | Source stats, poppet disruption |
| banshee | ✅ Done | Scream, Earmuffs hook |
| spectre | ✅ Done | Source combat, armor-bypass |
| poltergeist | ✅ Done | Needs model-UV verify |
| treefyd | ✅ Done | Old UV model, seeds item |
| ent | ✅ Done | Source stats, log break hook |
| mandrake_mob | ✅ Done | Source loot, scream |
| demon | ✅ Done | Source combat; wing-pose verify needed |
| nightmare | ✅ Done | Source behavior + tendrils model |
| spirit | ✅ Done | Translucent wisp, ambient spawn |
| corpse | ✅ Done | |
| reflection | ✅ Done | Mirror World guardian |
| death | ✅ Done | Source stats, stare, portal particle; scythe pivot verify |
| binky | ✅ Done | |
| flame_imp | ✅ Done | Contracts, punch anim; needs verify |
| lord_of_torment | ✅ Done | |
| horned_huntsman | ✅ Done | Spear, enchanted book drops |
| broom | ✅ Done | Vehicle authority, rider yaw |
| owl | ✅ Done | |
| raven | ✅ Done | Placeholder behavior |
| familiar_cat | ✅ Done | |
| spectral_familiar | ✅ Done | Source combat, spectral dust drop |
| toad | ✅ Done | |
| parasytic_louse (entity) | ✅ Done | Potion bite |
| hobgoblin | ✅ Done | Trades, leash labor |
| flying_monkey | ✅ Done | Feather drops; model verify needed |
| egassy | ✅ Done | Custom NPC, wart layer, quotes |
| soulfire (projectile) | ✅ Done | |
| thrown_brew (projectile) | ✅ Done | |
| sun_grenade (projectile) | ✅ Done | |
| ~~**EntityLilith**~~ | ✅ Done | `LilithEntity` + `ElleEntity` are registered/ported; remaining late vampire gates belong to the progression lane, not missing entity parity |
| ~~**EntityLeonard**~~ | ✅ Done | `LeonardEntity` registered/ported with custom boss behavior; exact brew summon wiring remains in the dynamic cauldron lane |
| ~~**EntityLostSoul**~~ | ✅ Done | `LostSoulEntity` registered/ported as Leonard's immunity-phase mob |
| ~~**EntityBabaYaga**~~ | ✅ Done | `BabaYagaEntity` registered/ported as 500 HP boss with renderer, spawn egg, and spawn command; local 1.7.10 scan found no source hut generator |
| ~~**EntityVillageGuard**~~ | ✅ Done | `VillageGuardEntity` — 25 HP, iron sword, 4 armour; targets undead/raiders/vampires/witches/demonic mobs and vampire/werewolf players; neutral to normal players; 2 variants |
| ~~**EntityVampire**~~ | ✅ Done | `VampireEntity` registered/ported; villager conversion/cure/level gates remain in vampire progression |
| ~~**EntityWolfman**~~ | ✅ Done | `WolfmanEntity` — 40 HP, silver weakness, moon reversion to WereVillager; WolfmanModel (64×64) |
| ~~**EntityVillagerWere**~~ | ✅ Done | `WereVillagerEntity` — peaceful mob, full-moon transforms to WolfmanEntity |
| **EntityGoblin** | ❌ Missing | Base trading/labor goblin |
| ~~**EntityGoblinGulg**~~ | ✅ Done | `GoblinGulgEntity` registered/ported; reward/patron in-game QA remains |
| ~~**EntityGoblinMog**~~ | ✅ Done | `GoblinMogEntity` registered/ported; reward/patron in-game QA remains |
| **EntityHellhound** | ❌ Missing | Infernal mob |
| **EntityMindrake** | ❌ Missing | Mindrake mob (crop → mob) |
| **EntityIllusionCreeper/Spider/Zombie** | ❌ Missing | Illusion mob variants (3 entities) |
| **EntitySummonedUndead** | ❌ Missing | Brew of Raising / ritual undead |
| **EntityMirrorFace** | ❌ Missing | Mirror World alternate guardian |
| **EntityDarkMark** | ❌ Missing | Dark Mark effect entity |
| **EntityEye** | ❌ Missing | Eye mob |
| **EntityAttackBat** | ❌ Missing | Attack bat entity |
| **EntityWitchCat** (wild) | ❌ Missing | Wild witch cat (different from familiar_cat) |
| **EntityBolt** | 🔶 Partial | `BoltEntity` is registered and live for Wooden/Silver/Nullifying/Bone/Splitting Hand Bow shots; nullifying strips active effects, powered nullifying shots from a full hunter/silvered/dawn hunter set carry a synced/saved drain flag and drain infusion energy from player targets, bone has holy bonus, splitting fires three weaker bolts, and Hand Bow stored-bolt loading/cycling plus Infinity/Power/Punch/Flame handling now feeds those types. In-game projectile/visual QA remains |
| **EntitySpellEffect** | ❌ Missing | Spell visual effect entity |

---

## 4. Brew System

This is the largest structural gap. The port has 35 brew items but does NOT have the original dynamic cauldron action composition system.

### 4a. Dynamic Cauldron System

| Component | Status | Notes |
|---|---|---|
| WitcheryBrewRegistry (ingredient → action mapping) | ❌ Missing | Source: ingredient stacking builds a brew from action list |
| BrewAction base + dispatch | ❌ Missing | Source: 14 action base classes |
| BrewActionDispersal (instant/liquid/gas/triggered) | 🔶 Partial | Splash/gas variants exist as separate item classes only |
| BrewActionEffect (direct hit effects) | ❌ Missing | |
| BrewActionModifier / ImpactModifier | ❌ Missing | Mandrake root / golden apple / void ore strength system |
| BrewActionRitual* (cauldron ritual summons) | ❌ Missing | Summon Leonard via cauldron |
| TileEntityCauldron ingredient-throwing loop | ❌ Missing | Dynamic brew building from thrown items |
| FluidBrew / BlockBrew (moving brew fluid) | ❌ Missing | Solid block stand-in only |
| BlockBrewGas (gas brew cloud) | ❌ Missing | |
| EntityBrew (thrown brew projectile physics) | 🔶 Partial | thrown_brew entity exists; full brew physics incomplete |
| Brew bottle / splash / lingering / bucket variants | 🔶 Partial | Item-based only; no container conversion |
| Brew Bag container logic | ❌ Missing | |
| Brew of Endless Water | ❌ Missing | |
| Brew of Combustion | ❌ Missing | |

### 4b. Brew Effects (as items vs dynamic actions)

All currently implemented as fixed items only, not as composable cauldron actions:

| Brew | Item status | Dynamic action |
|---|---|---|
| Brew of Raising | ✅ Item | ❌ No action |
| Brew of Sleeping | ✅ Item | ❌ No action |
| Brew of Flowing Spirit | ✅ Item | ❌ No action |
| Brew of Wasting | ✅ Item | ❌ No action |
| Brew of Erosion | ✅ Item | ❌ No action |
| Brew of Foulness / Foaming / Poison | ✅ Item + splash + gas | ❌ No action |
| Brew of Infection | ✅ Item | ❌ No action |
| Brew of Love | ✅ Item | ❌ No action |
| Brew of the Grotesque | ✅ Item | ❌ No action |
| Brew of Revealing | ✅ Item | ❌ No action |
| Brew of Vines / Webs / Thorns / Ice / Sprouting | ✅ Item | ❌ No action |
| Brew of Ink / Hitchcock / Bats / Frogs Tongue | ✅ Item | ❌ No action |
| Brew of Cursed Leaping / Depths / Soaring | ✅ Item | ❌ No action |
| Brew of Hollow Tears / Substitution / Grave | ✅ Item | ❌ No action |
| Solidifying brews (stone/dirt/sand/etc.) | ✅ Item | ❌ No action |
| Infused brew variants (soaring/grave) | ✅ Item | ❌ No action |
| **~60 brew effects from source not yet as items** | ❌ Missing | Missing brews include: Snow Trail, Swimming, Ender Inhibition, Moonshine, Harm Undead/Insects/Werewolves/Demons, Weaken Vampires, Drain Magic, Steal Buffs, Seasons, Tree-growing family, Blight (brew form), Transpose/Transpose Ore, Volatility, Levelling, Raise Land, Tidehold, Lilify, Fertilization/Harvesting/Tilling/Planting/Pruning, Bodega (bird summon), Air Hike, Reincarnate, Resizing, Fortune, Keep Inventory, Keep Effects on Death, Hellgate, Blast, Ice World, Ice Shell (brew form), Extinguish, Dissipate, Pulverisation, Frog Tongue (brew form), Poison Toad |

### 4c. Potion Effects (MC effect registry)

| Status | Notes |
|---|---|
| ❌ Not registered as MC MobEffects | All 43 custom Witchery potion classes from source (PotionAbsorbMagic, PotionMortalCoil, PotionGrotesque, etc.) are not registered as persistent MobEffects in the port. Effects are applied directly on brew use. |
| Priority effects to add | MortalCoil (Leonard boss dep), Grotesque (Brew of Grotesque dep), Paralysis (vampire/brew dep), Enslaved (Love brew dep), SunAllergy (vampire dep), Sinking/Overheating (curse brews) |

---

## 5. Rituals

Port implements ~20+ rituals dispatched through RitualExecutor. Source has ~45.

### Implemented (✅)

Infusion (Light/Overworld/Otherwhere/Infernal), Total Eclipse, Fertility, Portable Fertility, Breath of Goddess Cure, Blindness, Summon Witch, Summon Familiar, Bind Waystone, Bind Waystone Copy, Transposition to Waystone, Banish Demon, Necromantic Stone, Glyph Transformation, Bind Circle to Talisman, Hell on Earth, Blight, Wither summoning, Demon summoning, Weather Call Storm, Ice Shell, Rain of Toads, Climate Change, Nature's Power, Transpose Ores, Moving Earth, Raise Column, Volcano, Protection Barrier (monsters/portable/sanctity), Imprisonment Circle, Infusion Recharge, Manifestation (Spirit World entry), Curse of the Wolf.

### Likely Missing (~25 rituals)

| Ritual | Notes |
|---|---|
| Rite of Prior Incarnation | Reincarnation / death recovery |
| Rite of Infestation | |
| Rite of Transposition | Mob/block swap |
| Rite of Summon Corpse | |
| Rite of Summon Death | Death entity summon |
| Rite of Creatures of the Night | Vampire induction key rite |
| Rite of Infernal Power | |
| Rite of Charging | |
| Rite of Calling | |
| Rite of Enchanting | |
| Rite of Curse Protection | |
| Rite of Circle Protection | |
| Rite of Sphere Protection | |
| Rite of Expanding Effect | |
| Rite of Set NBT | |
| Rite of Cook Item | |
| Rite of Glorification | Worship mechanic |
| Rite of Reveal Invisible | |
| Rite of Remove Curse | |
| Rite of Reverse | |
| Rite of Felling | Tree-felling area effect |
| Rite of Raising (ritual form) | |
| Rite of Lycanthropic Compulsion | |
| Rite of Summoning Leonard | Custom brew dep |
| Rite of Spectral Binding | Spirit capture |

*Note: Some of these may be partially implemented in RitualExecutor under different names. Verify against source before adding.*

---

## 6. Infusions & Creature Powers

| System | Status | Notes |
|---|---|---|
| InfusionLight | ✅ Done | Registered + basic mechanics |
| InfusionOverworld | ✅ Done | Registered + basic mechanics |
| InfusionOtherwhere | ✅ Done | Registered + basic mechanics |
| InfusionInfernal | ✅ Done | Registered + basic mechanics |
| Vampirism (player infusion) | 🔶 Partial | Boolean state, bite, sunlight, mesmerise; missing 10-level progression |
| Lycanthropy (player infusion) | 🔶 Partial | Wolfman render, wolf form, mastery foundation; missing 10-level Wolf Altar quest chain |
| **Creature Powers (14 types)** | ❌ Missing | Source: CreaturePowerBat/Blaze/Creeper/Enderman/Ghast/Heal/Jump/PigMan/Skeleton/Speed/Spider/Squid/Zombie + base; activated via Mystic Branch symbol drawing — entire system absent |
| Spirit infusions (7 types) | 🔶 Partial | Infused brew base items present; GhostWalker/Screamer/Sentinel/Twister/Twister effects not implemented |
| Symbol/glyph drawing (Mystic Branch) | ❌ Missing | Source: `SymbolEffect`, `StrokeSet`, `EffectRegistry` — gesture/stroke recognition system |
| InfusedBrewSoaring / Grave | 🔶 Partial | Items exist; exact source infusion application not verified |

---

## 7. Machine Behavioral Completeness

| Machine | Implemented | Behavioral gaps |
|---|---|---|
| Witch's Cauldron | 🔶 Partial | Brew-item recipes work; dynamic action composition missing (see brew system) |
| Kettle | 🔶 Partial | Bottle automation, bonus rolls restored; hopper QA needed |
| Witches' Oven | 🔶 Partial | Sided inventory, fume table, recipes restored; in-game hopper/funnel QA needed |
| Distillery | 🔶 Partial | Sided automation restored; hopper QA needed |
| Spinning Wheel | 🔶 Partial | Hay bale/web routes restored; hopper QA needed |
| Brazier | 🔶 Partial | Ingredient automation restored; hopper QA needed |
| Silver Vat | 🔶 Partial | 3D model, deposit byproduct, recipes; furnace/hopper QA needed |
| Sun Collector | 🔶 Partial | Charge loop, power state, item render; in-game verification needed |
| Blood Crucible | ❌ Missing | Source: `BlockBloodCrucible` — vampire level progression block; entire system absent |
| Poppet Shelf | 🔶 Partial | Slot clearing, facing-aware rendering; in-game spacing QA needed |
| Leech Chest | 🔶 Partial | Storage and player queue; broader parity verify open |
| Refilling Chest | ✅ Done | Source refill logic, unbreakable, Torment-only |

---

## 8. Dimensions & World

| System | Status | Notes |
|---|---|---|
| Spirit World (dimension) | 🔶 Partial | Biome, fog/sky, vegetation features connected; fresh-chunk QA pending |
| Spirit World vegetation (cotton/grass/mushroom) | 🔶 Partial | Features registered; fresh-chunk spawn verification needed |
| Torment maze | 🔶 Partial | 6-level maze, Refilling Chests, Force walls; pit_dirt/pit_grass blocks missing |
| Mirror World | 🔶 Partial | Portal loop, room generator, Reflection guardian; room QA with fresh world needed |
| Rowan tree worldgen | ✅ Done | Placed in biomes |
| Village additions (Bookshop, Guard Tower, Town Wall) | Partial | Village Guard, apothecary/bookshop/witch-hut village injection, Watch Tower, Keep, and first-pass adaptive town walls are live. The wall queue now retries/refreshes early starts, can queue from non-start village chunks via structure references, and `/witchery goto village_*` / `/witchery walls` use temporary forced-chunk wall jobs instead of synchronous perimeter force-loading. Cross-biome fresh-village wall/building QA remains open, especially structure distribution, savanna materials, gate support fill, immediate wall generation, and goto/manual-wall freeze retests. |
| Coven Circle / Stone Circle worldgen | Partial | `witchery:coven_circle` is registered with a source-inspired 11x4x11 `ComponentCoven` ring, hidden mineshaft-loot dispenser, two vanilla Witch spawners under the altar, source 90% Coven Witch resident spawn chance, center/corner water rejection, village exclusion, desert/badlands-only sandstone variant, grass surface for non-desert stone circles, and stone-backed support so uneven terrain does not expose a one-block dirt skirt. Stone Circle naming/details and fresh-world QA remain open. |
| Straw Man / Wicker Man worldgen | Partial | `witchery:straw_man` is registered from source `ComponentWickerMan`: 6x8x5 hay effigy, grass/sand footprint, dandelion/dead-bush biome plants, hidden Zombie/Zombie/Skeleton spawner, full-footprint dry-land water/ocean/river rejection at selection and placement time, village exclusion, and `/witchery goto straw_man`. Fresh-world QA remains open. |
| Goblin structures | ❌ Missing | Source: `WorldGenGoblinHut` |
| Baba Yaga hut | Deferred / not source-backed | Local 1.7.10 source scan found `EntityBabaYaga`, predictions, model/renderer, and no `WorldGenBabaYagaHut` or `ComponentBabaYagaHut`. Do not treat as canonical 1.7.10 parity unless another source is found. |
| Witch Hut (Witchery-specific) | ❌ Missing | Source: `WorldGenWitchHut` |

---

## 9. Priority Action List (from this matrix)

### Immediate (blocking other systems)

1. **Blood Crucible block** — gates the entire vampire level progression system
2. **Dynamic cauldron brew action system** — gates Brew Bag, Leonard summon, ~60 missing brews
3. **Late vampire progression gates** — Blood Crucible/Blood Power, level storage, conversion/cure, levels 8-10
4. **Wolftrap entity-backed summon/capture** — `WolfmanEntity` exists; trap wiring still needs source behavior

### High value (large surface area)

5. **Creature Powers + Mystic Branch symbol system** — 14 creature power types, none working
6. **10-level vampire progression** (Blood Crucible → level storage → ability gates)
7. **10-level werewolf progression** (Wolf Altar quest chain → Moon Charm)
8. **Leonard summon integration** — `LeonardEntity`/`LostSoulEntity` exist; custom brew summon action still missing
9. **MobEffect registrations** (PotionMortalCoil, PotionGrotesque, PotionParalysis, etc.)
10. **~25 missing rituals** — especially Creatures of the Night (vampire induction gate)

### Medium (fill-in work)

11. Perpetual ice/snow block family (11 blocks)
12. Stockade blocks (2 blocks)
13. Shaded glass (2 block states)
14. Blood Crucible item + recipe
15. Baba Yaga boss in-game QA / non-source hut decision
16. Fresh village structure QA for guards, walls, watch towers, keeps, bookshops, apothecaries, and witch huts
17. Goblin entity family (`EntityGoblin` base remains; Gulg/Mog bosses are ported)
18. Illusion entity family (3 entities)
19. EntitySummonedUndead (Brew of Raising dep)
20. Cursed redstone family (7 blocks — low gameplay impact)

### Low (QA / verification pass)

21. In-game hopper QA for all 6 machines
22. Spirit World fresh-chunk vegetation/spawn QA
23. Mirror World room QA
24. Torment pit_dirt/pit_grass block addition
25. Model/UV verification pass (demon wings, death scythe, poltergeist)
