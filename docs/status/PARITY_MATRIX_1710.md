# Witchery 1.7.10-0.24.1 Parity Matrix

**Built:** 2026-05-28
**Baseline source:** `C:\claude\witchery\decompiled\witchery-1.7.10-0.24.1`
**Port:** NeoForge 1.21.1

Legend: ✅ Done · 🔶 Partial · 🔲 Placeholder · ❌ Missing · ⚠️ Wrong behavior

---

## Quick Summary

| Category | Source count | Port status |
|---|---|---|
| Items (classes) | ~55 item classes (~120+ variants) | 265 functional + 121 placeholders |
| Blocks | ~98 distinct types | ~99 registered; ~12 families missing |
| Entities | 56 | 26 registered; 18+ missing |
| Brew actions/effects | ~95 brew effects | ~35 items; dynamic action system missing |
| Potion effects | 43 custom classes | ~0 registered as MC effects; brews are items |
| Rituals | ~45 | ~20+ dispatched |
| Infusions (transformation types) | 4 types + creature powers | 4 types registered; Mystic Branch creature powers restored in practical active form; passive/symbol UI parity missing |
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
| observations_of_an_immortal / torn_page | ✅ Done | Book screen, update recipe, source-family kill drops including the current Witchery boss roster |
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
| moon_charm | Partial | Source: `ItemMoonCharm` now has rare single-stack durability, gold-nugget repair, 60-tick bow-style use, manual werewolf transform/restore toggle, wolfsbane refusal, inventory prevention of forced moon shifting, and Wolf Altar 3-nugget output. Remaining: exact wolf/wolfman split and old level-gate quest semantics. |
| horn_of_the_hunt / hunts_horn | 🔶 Partial | Source `ItemHornOfTheHunt` / Resurrected alias now use 40-tick bow-style hold, one-use durability, nearby safe Horned Huntsman spawn, 150-tick quarter-health entrance, horn/Wither-spawn feedback, and source-only explosive entrance. Remaining: in-game boss entrance/terrain QA and exact old horn sound asset. |
| wolf_token | 🔲 Placeholder | Creative-only; no mechanic |
| cane_sword | Partial | Source: `ItemCaneSword` combo weapon now has sheathed/drawn variants, sneak-use transform, recipe, vampire blood-reservoir capture from villagers/players, and sheathed extraction into blood power. Remaining: exact old sound assets and player-data-backed reserve parity. |
| dup_staff | ❌ Missing | Source: `ItemDuplicationStaff` |
| dupgrenade | ❌ Missing | Source: `ItemSunGrenade(1)` — mirror duplication grenade |
| biome_book / biome_note | ❌ Missing | Source: `ItemBook`, `ItemBiomeNote` — biome scrying |
| cauldron_book | ❌ Missing | Source: `ItemMarkupBook(7)` — in-game cauldron guide |
| vampire_book | ❌ Missing | Source: `ItemMarkupBook(7, [0,9])` — vampire progression book with level callbacks |
| bucket_brew | ❌ Missing | Source: `ItemBrewBucket` — brew bucket container |
| brew_fuel / brew_endless_water | Partial | `ItemBrewEndlessWater` is restored as `witchery:brew_of_endless_water`; source `ItemBrewFuel` is represented by the restored `witchery:combustion_brew` fuel item |
| stew (cooked/raw) | ✅ Done | `witchery:meaty_stew` / `witchery:raw_meaty_stew` now use source `ItemStew` food values, bowl return, original sprites, raw stew crafting, and smelting |
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
| Mystic Branch | 🟡 Partial | Item registered; altar placement/enhancement and practical creature-power activation restored; symbol/glyph drawing system MISSING |
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
| ice_pressure_plate | ✅ Done | Source `BlockPerpetualIcePressurePlate(Material.ice)` restored as an Icy Slippers-only trigger plate with 2.0 hardness / 5.0 resistance and explicit Perpetual Ice Fence support |
| perpetual_ice (base block) | ✅ Done | Source `BlockPerpetualIce` base behavior restored: vanilla ice break feel, never random-melts, hardness 2.0/resistance 5.0/light opacity 3 |
| ice_stairs | ✅ Done | Source `BlockPerpertualIceStairs` restored as a real stair block: ice friction, hardness 2.0/resistance 5.0/light opacity 3, translucent ice model, item, tags, and loot |
| ice_slab (single/double) | ✅ Done | Source `BlockPerpetualIceSlab` restored as one modern slab block with bottom/top/double states and double-slab loot returning two singles |
| ice_fence_gate | ✅ Done | Live as modern `witchery:ice_gate`; old registry alias was `witchery:icefencegate` |
| stockade_ice | ✅ Done | Source `BlockStockade(true)` restored as `witchery:ice_stockade`: low fence-like collision, 25.0 hardness / 20.0 resistance, light opacity 1, cactus contact damage, and connections to itself, Iron Bars, and Ice Gate |
| snow_pressure_plate | ✅ Done | Source `BlockPerpetualIcePressurePlate(Material.snow)` restored as an all-entity snow pressure plate with 0.2 hardness, snow sound/model, loot, and source two-snow-block recipe |
| cursed_snow_pressure_plate | Partial | Hidden source `BlockPressurePlateBase` wrapper now registers as `witchery:cursed_snow_pressure_plate`, reuses the snow pressure-plate models/behavior, has no item or creative-tab entry, and drops the normal snow pressure plate; remaining parity is the cursed block entity and triggered-brew conversion payload |
| snow_stairs | ✅ Done | Source `BlockSnowStairs` restored with snow-block recipe, 0.2 hardness, snow sound/model, item, tags, and loot |
| snow_slab (single/double) | ✅ Done | Source `BlockSnowSlab` restored as one modern slab block with snow-layer recipe, bottom/top/double states, and double-slab loot returning two singles |

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
| circle / circle_glyph / circle_otherwhere / circle_infernal | ✅ Done | Thin glyph block path restored; ritual/otherwhere/infernal glyphs now carry the source-style 12-way visual metadata/state and same-chalk use rerolls the visible symbol |
| blood_crucible | 🔶 Partial | Block/BE/render, vampire fill path, source recipe, and level-10 ultimate selection are live; broader Blood Power/late vampire gates remain under Vampirism |
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
| stockade (wood) | 🔶 Partial | `witchery:stockade` restores a first-pass source `BlockStockade(false)` slice with low stockade collision, 25/20 toughness, cactus contact damage, self/Iron Bars/Ice Gate connections, oak-log models, self loot, and the old logs + `Exhale of the Horned One` shaped recipe for 9. Remaining: per-wood metadata variants and freezing/brew conversion into Ice Stockades. |
| stockade_ice | ✅ Done | Live under Perpetual Ice & Snow Family as `witchery:ice_stockade`; old source produced it through stockade freezing/brew conversion rather than direct crafting |
| blooded_wool | ⚠️ Wrong | Port has `blood_stained_wool`; verify ID matches original |
| shaded_glass | ❌ Missing | See 2c above |
| pit_dirt / pit_grass | 🔶 Partial | `witchery:pit_dirt` and `witchery:pit_grass` are now real no-collision camouflage blocks with vanilla dirt/grass visuals, source hardness/sound feel, self loot, creative-tab wiring, and old display names. Remaining: Tormented Twine Witch's Cauldron recipes and in-game trap/Torment QA. |
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
| Brew of Endless Water | Done | Source-style glowing 100-use utility item fills cauldrons or places water source blocks |
| Brew of Combustion | Partial | `witchery:combustion_brew` is now a real glowing stackable furnace/Witches' Oven fuel with the Resurrected `Level` burn tiers (2400/5000/10000/50000 ticks) and a base coal kettle recipe; full dynamic composed-brew creation remains under the custom cauldron backlog |

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
| Lycanthropy (player infusion) | 🔶 Partial | Wolfman render, wolf form, mastery foundation, Moon Charm control, and Horn of the Hunt summon item; missing 10-level Wolf Altar quest chain |
| **Creature Powers (14 types)** | 🟡 Partial | Source: CreaturePowerBat/Blaze/Creeper/Enderman/Ghast/Heal/Jump/PigMan/Skeleton/Speed/Spider/Squid/Zombie + base. Practical altar-kill acquisition and Mystic Branch activations now exist for the old sacrifice families; passive effects, exact charge UI/selection flow, and symbol drawing remain missing |
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
| Blood Crucible | 🔶 Partial | Source block, BE, renderer, recipe, filling, and level-10 ultimate selection are live; broader Blood Power/late vampire progression remains open |
| Poppet Shelf | 🔶 Partial | Slot clearing, facing-aware rendering; in-game spacing QA needed |
| Leech Chest | 🔶 Partial | Storage and player queue; broader parity verify open |
| Refilling Chest | ✅ Done | Source refill logic, unbreakable, Torment-only |

---

## 8. Dimensions & World

| System | Status | Notes |
|---|---|---|
| Spirit World (dimension) | 🔶 Partial | Biome, fog/sky, vegetation features connected; fresh-chunk QA pending |
| Spirit World vegetation (cotton/grass/mushroom) | 🔶 Partial | Features registered; fresh-chunk spawn verification needed |
| Torment maze | 🔶 Partial | 6-level maze, Refilling Chests, Force walls; pit_dirt/pit_grass block items are live, but fresh Torment trap QA and their old cauldron recipe route remain open |
| Mirror World | 🔶 Partial | Portal loop, room generator, Reflection guardian; room QA with fresh world needed |
| Rowan tree worldgen | ✅ Done | Placed in biomes |
| Village additions (Bookshop, Guard Tower, Town Wall) | Partial | Village Guard, source-shaped Apothecary/Bookshop/village Witch Hut injection, Watch Tower, Keep, and first-pass adaptive town walls are live. The Apothecary now follows old `ComponentVillageApothecary` 9x9x6 shell/roof/window/front-door layout with sparse source furnishings, biome materials, and a librarian-skinned `Apothecary` Villager carrying the 1.7.10 apothecary Emerald trade list. The Bookshop follows old `ComponentVillageBookShop` shape/roof with a path-height entrance baseline, source local chest position, connected side panes, and the old rear-wall four-frame book display row filled from the current Book/Torn Page/Observations loot family. Apothecary/Bookshop/Witch Hut/Watch Tower/Keep blocks adapt to desert/badlands, savanna, taiga/snowy, and plains village palettes with connected generated panes where those buildings use panes; desert/badlands versions use sandstone-family materials instead of cobble/stone/oak. Watch Towers now use outward-facing pyramidal roof stairs, a continuous supported center ladder from entry to upper deck, the old three-block upper handoff ladder, a lowered mid-platform chest, and guard spawns on solid deck cells around the shaft. Vanilla village house pools now weight Witchery buildings at apothecary 16, witch hut 12, bookshop 16, watch tower 8, and keep 32, and `WitcheryVillageElement` advertises building-specific front-door jigsaws while capping each Witchery building type to one placement per owning village start; Keep is now confirmed to appear, and its bridge ladders use the same source meta-2 support-facing state as the tower ladders so they should survive generation, with fresh traversal QA still needed alongside `[VillageBuildings:Debug]` offer/claim/placement logs. The wall queue retries/refreshes early starts with loaded-only automatic structure-reference lookups, can queue from non-start village chunks when referenced start chunks are already resident, waits for naturally loaded padded-footprint chunks on automatic discovery, and uses nearest-first readiness-gated temporary forced-chunk jobs only for `/witchery walls`; explicit jobs request one chunk ticket at a time, cap very large forced footprints at 144 chunks, and release/skip a forced chunk that makes no readiness progress for 10 seconds. Automatic pending checks and active wall jobs are bounded/round-robin so several discovered villages can coexist without hidden sync start-chunk loads. Village gotos use a ticked safe locate queue that probes one candidate chunk per server tick, pass the found `StructureStart` directly into pre-teleport arrival prep, full-load the destination footprint one chunk per tick for fresh villages, hold prepared chunks with temporary tickets until walls and landing complete, and generate walls/gates before crossing. Goto/manual walls respect the processed-village set unless `/witchery walls reset` is used first, preventing duplicate wall stacks. Gate path grading and height sampling stop within 10 inward blocks so gates do not level paths through houses, and wall/gate/path corridors clear touched connected tree logs/leaves to avoid sliced taiga trees. Cross-biome fresh-village wall/building QA remains open, especially apothecary resident/trade verification, bookshop frame verification, structure distribution, keep traversal, duplicate-cap verification, taiga tree cleanup, biome material checks, gate support fill, immediate wall generation, zero-gate automatic runs, and new-world/goto/manual-wall freeze retests. |
| Coven Circle / Stone Circle worldgen | Partial | `witchery:coven_circle` is registered with a source-inspired 11x4x11 `ComponentCoven` ring, the three source cobweb detail blocks, hidden mineshaft-loot dispenser, two vanilla Witch spawners under the altar, source 90% Coven Witch resident spawn chance, padded-footprint water rejection, village exclusion, desert/badlands-only sandstone variant, grass surface for non-desert stone circles, stone-backed support, and lowest-surface padded terrain anchoring so uneven terrain should not expose a one-block dirt skirt. `/witchery goto stone_circle` remains the old-name command alias. Fresh-world QA remains open. |
| Straw Man / Wicker Man worldgen | Partial | `witchery:straw_man` is registered from source `ComponentWickerMan`: 6x8x5 hay effigy, terrain-blended cross footprint, dandelion/dead-bush biome plants, hidden Zombie/Zombie/Skeleton spawner, padded-footprint dry-land water/ocean/river rejection at selection and placement time, village exclusion, and `/witchery goto straw_man`. Badlands reuse local surface blocks instead of a raised pale sand platform, and the selector now anchors to the lowest padded surface to avoid a raised dirt/grass plinth. Fresh-world QA remains open. |
| Goblin structures | Partial | Hobgoblin Hut is live and now uses the decompiled 7x6x7 `ComponentGoblinHut` schematic with one generated resident hobgoblin. Broader old goblin village/labor behavior remains open. Source: `WorldGenGoblinHut` |
| Baba Yaga hut | Deferred / not source-backed | Local 1.7.10 source scan found `EntityBabaYaga`, predictions, model/renderer, and no `WorldGenBabaYagaHut` or `ComponentBabaYagaHut`. Do not treat as canonical 1.7.10 parity unless another source is found. |
| Witch Hut (Witchery-specific) | Partial | The village witch hut path now matches old `ComponentVillageWitchHut` at 4x6x5, keeps the raised road-facing entrance, uses connected side panes, adapts visible shell/door/stair/base materials to the owning village biome, and places the front stair facing back toward the door instead of away; it is injected into vanilla village house pools. The separate non-village `WorldGenWitchHut` abandoned-shack/world structure still needs source verification and, if distinct, a dedicated structure path. |

---

## 9. Priority Action List (from this matrix)

### Immediate (blocking other systems)

1. **Dynamic cauldron brew action system** — gates Brew Bag, Leonard summon, ~60 missing brews
2. **Late vampire progression gates** — broader Blood Power, level storage, conversion/cure, levels 8-10
3. **Blood Crucible QA/polish** — block/BE/recipe/ultimate path live; needs in-game fill/use verification and particle/sound exactness
4. **Wolftrap entity-backed summon/capture** — `WolfmanEntity` exists; trap wiring still needs source behavior

### High value (large surface area)

5. **Creature Powers + Mystic Branch symbol system** — practical active powers now work; passive hooks, UI polish, and symbol drawing remain
6. **10-level vampire progression** (broader Blood Power → level storage → ability gates)
7. **10-level werewolf progression** (Wolf Altar quest chain → Moon Charm)
8. **Leonard summon integration** — `LeonardEntity`/`LostSoulEntity` exist; custom brew summon action still missing
9. **MobEffect registrations** (PotionMortalCoil, PotionGrotesque, PotionParalysis, etc.)
10. **~25 missing rituals** — especially Creatures of the Night (vampire induction gate)

### Medium (fill-in work)

11. Perpetual ice/snow follow-up: cursed snow pressure plate brew payload/conversion hooks and brew/rite placement hooks
12. Stockade follow-up: per-wood wooden stockade variants plus future freezing conversion from wood to ice
13. Shaded glass (2 block states)
14. Blood Crucible in-game fill/use QA and particle/sound exactness
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
24. Pit Dirt/Pit Grass cauldron recipe plus in-game trap QA
25. Model/UV verification pass (demon wings, death scythe, poltergeist)
