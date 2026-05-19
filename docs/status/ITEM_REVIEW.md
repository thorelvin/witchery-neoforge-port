Deaths scythe is wrong angle. Not touching hand.

~~Where is mutandis item in tab?~~ ✅ Fixed 2026-05-18

# Witchery Tab — Item Review

Items appear in creative tab order. Add comments in the `Notes` column.

---

## Blocks

| # | Item ID | Notes |
|---|---|---|
| 1 | `witchery:witches_primer` | | Stubbed, wont open
| 2 | `witchery:rowan_log` | ✅ Fixed — RotatedPillarBlock + axis blockstate variants | Can only be placed vertically, should work as regular logs all directions
| 3 | `witchery:rowan_wood` | | Ok
| 4 | `witchery:rowan_leaves` | ✅ Fixed — UntintedParticleLeavesBlock, multipart blockstate, logs tag added for decay | Texture wrongly mappet, and not transparent
| 5 | `witchery:rowan_sapling` | | Works, bone meal too
| 6 | `witchery:belladonna` | | Ok
| 7 | `witchery:mandrake_root` | `Partial - original spawn-vs-drop branch restored` | Mature harvest now checks difficulty and original weighted summon chance instead of always spawning a mandrake. Needs in-game verification.
| 8 | `witchery:artichoke` | | Ok
| 9 | `witchery:cotton` | `Partial - restored closer to old Somnian Cotton behavior` | Now behaves like a spreading bush instead of a staged crop, nightmare harvest can yield disturbed cotton again, and the plant render/loot path is aligned. Needs in-game verification and a final decision on whether the extra placeable disturbed-cotton block should stay.
| 10 | `witchery:witch_seeds` | | What is this? Plants on farmland. Real item in mod?
| 11 | `witchery:snowbell` | | Ok, bone meal grow
| 12 | `witchery:blood_rose` | `Partial - blood sample storage restored` | Walking through the rose now stores the last player's UUID/name, switches to the full rose variant, and lets Taglock Kits pull that sample later. Needs in-game verification.
| 13 | `witchery:ember_moss` | | Looks ok
| 14 | `witchery:glint_weed` | ✅ Fixed — lightLevel(s->7) added | Ok, should have light
| 15 | `witchery:spanish_moss` | `Partial - growth behavior restored` | Now behaves like a faster-growing vine again. Needs in-game verification for spread feel.
| 16 | `witchery:bramble` | `Partial - wild bramble hazard restored` | Now hurts entities and spreads into nearby open tiles when broken without shears. Needs in-game verification against original placement/spread feel.
| 17 | `witchery:void_bramble` | `Partial - void teleport trap restored` | Now teleports living entities to a distant safe point again. Ownership/column-break behavior from the old tile-entity version is still not restored.
| 18 | `witchery:leaping_lily` | `Partial - leap buff restored` | Walking through it now grants short speed + jump boosts again. Still uses placeholder visuals and needs in-game verification.
| 19 | `witchery:grassper` | `Partial - stash mechanic restored` | Holds one given item again and now renders that stored item above the plant. Visual verification still needed.
| 20 | `witchery:glowglobe` | `Partial - light and ambience restored` | Now emits strong light and ambient flame particles again. Needs in-game verification.
| 21 | `witchery:kettle` | | Seems to work, missing item texture. Missing 3d string. Pink black. Check block mechanics in old.
| 22 | `witchery:distillery` | `Partial - mechanics restored closer to original (altar power, jars, multi-output)` | GUI art/layout and item texture still need verification.
| 23 | `witchery:spinning_wheel` | `Partial - mechanics restored closer to original (5 slots, altar power, modifiers)` | GUI art/layout and item texture still need verification.
| 24 | `witchery:witches_oven` | `Partial - mechanics restored closer to original (jars, funnels, byproducts)` | Facing/item texture and GUI verification still open.
| 25 | `witchery:altar` | `Partial - validated shared 2x3 altar behavior and upgrade logic restored closer to original` | Skull stacking, chalice bonus, Arthana range, Mystic Branch enhancement, and missing nature-power plants were all brought closer to 1.7.10. The block and item now use a shaped 3D altar form again instead of a flat sprite/full cube, but the original connected joined-altar art still needs a deeper render pass.
| 26 | `witchery:fume_funnel` | | Wrong design and texture, check mechanics. Missing item texture. Pink black
| 27 | `witchery:brazier` | `Partial - unburned ingredient drop path restored` | Unlit braziers now drop their stored ingredients again when broken instead of silently eating them through the dead legacy removal hook. Item texture/model work is still open, and ash/result verification still needs an in-game pass.
| 28 | `witchery:poppet_shelf` | `Partial - mechanics restored closer to original; top-shelf rendering restored` | Now only accepts poppets again, drops its contents on break, scans server shelves for bound poppets again, force-loads shelves with bound poppets so remote protection can still trigger, and renders stored poppets visibly on top again. Item texture/model polish is still open and needs in-game spacing verification.
| 29 | `witchery:leech_chest` | `Partial - container, taglock storage, and break/replace persistence restored` | Opens as a 27-slot chest again, tracks up to 3 recent players, lets Taglock Kits pull a valid stored target, and now preserves that stored queue through block drops like the old mod. Item texture/model still need work.
| 30 | `witchery:refilling_chest` | `Partial - refill cadence and loot restored closer to original` | Refill logic is Torment-only again, now follows the old hourly life-tick cadence instead of waiting one hour after emptying, and pulls a small dungeon-style refill closer to 1.7.10. Texture wrap, GUI title/text polish, and item texture/model still need work.
| 31 | `witchery:candelabra` | | Light? Missing item texture. Pink black
| 32 | `witchery:chalice` | `Partial - altar power bonus wired again` | Filled vs empty chalice bonus now affects the altar closer to original. Item/model art still needs work.
| 33 | `witchery:crystal_ball` | | Must be transparent. Says flavor text. Missing item texture. Pink black
| 34 | `witchery:dream_catcher` | `Partial - placement, nightmare-weave handling, and weave break-drop restored closer to original` | Wall placement now uses the face clicked instead of snapping off to the side, Spirit World nightmare/demonic modifiers now only respect the `Nightmares` weave again, and breaking or support-dropping a catcher now returns its stored weave again. Item texture/model work is still open.
| 35 | `witchery:weave_fleet_foot` | | Looks ok, mechanics?
| 36 | `witchery:weave_iron_arm` | | Looks ok, mechanics?
| 37 | `witchery:weave_fasting` | | Looks ok, mechanics?
| 38 | `witchery:weave_nightmares` | | Looks ok, mechanics?
| 39 | `witchery:weave_intensity` | | Looks ok, mechanics?
| 40 | `witchery:demonic_contract` | | Looks ok, not tested
| 41 | `witchery:icy_needle` | | Works in dream
| 42 | `witchery:soul_of_torment` | | Not tested
| 43 | `witchery:torment_scroll` | | Looks ok
| 44 | `witchery:alluring_skull` | | Wrong 3d model, wrong wrap
| 45 | `witchery:demon_heart` | | Wrong 3d model, wrong wrap
| 46 | `witchery:coffin` | | Is two blocks, but shows only one side. Lid works. Sets respawn point. 
| 47 | `witchery:rowan_planks` | ✅ Fixed — added to #minecraft:planks tag | Correct texture, need regular plank recipes.
| 48 | `witchery:rowan_door` | ✅ Fixed — block models now use door_rowan_lower/upper textures; item model uses ingredient.door_rowan | Wrong texture. Looks like oak door.
| 49 | `witchery:rowan_door_key` | | Not tested
| 50 | `witchery:rowan_keyring` | | Not tested
| 51 | `witchery:ice_door` | ✅ Fixed — BlockSetType.OAK (opens on right-click); item model uses packed_ice texture | Wrong item graphics. Looks like ice door when placed. Dont know how to open, not on right click.
| 52 | `witchery:ice_fence` | ✅ Fixed — added to #minecraft:fences tag | Dont link with other fences
| 53 | `witchery:ice_gate` | | Works, link with fence
| 54 | `witchery:ice_pressure_plate` | | Works on fence. tested
| 55 | `witchery:fetish_scarecrow` | ✅ Fixed — FACING property added; FetishRenderer uses facing-aware render state | Facing to right when placed. Missing item texture. Pink black
| 56 | `witchery:fetish_trent_effigy` | ✅ Fixed — same FACING fix | Facing to right when placed. Missing item texture. Pink black
| 57 | `witchery:fetish_witchs_ladder` | ✅ Fixed — same FACING fix | Facing to right when placed. Missing item texture. Pink black. All three looks same
| 58 | `witchery:statue_goddess` | ✅ Fixed — FACING property; reads facing from block state in renderer | Facing to right when placed. Missing item texture. Pink black.
| 59 | `witchery:wicker_bundle` | | Burns? Mechanics?
| 60 | `witchery:critter_snare` | | Looks ok, not tested
| 61 | `witchery:witch_door` | ✅ Fixed — converted to DoorBlock(OAK), full door blockstates/models created, DoubleHighBlockItem; uses rowan textures as placeholder | Not a door, shaped as cube. Texture wrapped wrong. Wont open.

---

## Tools & Items

| # | Item ID | Notes |
|---|---|---|
| 62 | `witchery:broom` | `Partial - control model rewritten closer to 1.7.10` | Forward acceleration, pitch lift, no forced hover-drop, and mounted fall cancellation are restored. Needs in-game verification.
| 63 | `witchery:enchanted_broom` | `Partial - shares the updated broom flight path` | Needs in-game verification.
| 64 | `witchery:arthana` | `Partial - altar placement/range bonus restored` | Now places cleanly on altar, updates bonuses immediately, and restores the old altar range boost. Visual work still needed.
| 65 | `witchery:boline` | |
| 66 | `witchery:chalk` | |
| 67 | `witchery:mystic_branch` | `Partial - altar placement/enhancement bonus restored` | Can now be placed on altar again and correctly grants the enhancement bonus in the altar scan. In-hand creature-power use still works.
| 68 | `witchery:taglock_kit` | `Partial - Blood Rose + Leech Chest extraction restored` | Now splits stacked kits correctly when filling one, and can pull stored samples from Blood Rose and Leech Chest again. Bed/original edge-case audit is still open.
| 69 | `witchery:poppet` | |
| 70 | `witchery:death_protection_poppet` | |
| 71 | `witchery:earth_protection_poppet` | |
| 72 | `witchery:water_protection_poppet` | |
| 73 | `witchery:fire_protection_poppet` | |
| 74 | `witchery:hunger_protection_poppet` | |
| 75 | `witchery:voodoo_protection_poppet` | |
| 76 | `witchery:poppet_protection_poppet` | |
| 77 | `witchery:voodoo_poppet` | |
| 78 | `witchery:vampiric_poppet` | |
| 79 | `witchery:brew_bag` | |
| 80 | `witchery:diviner` | |
| 81 | `witchery:circle_talisman` | |
| 82 | `witchery:witch_hand` | |
| 83 | `witchery:fanciful_thread` | |
| 84 | `witchery:redstone_soup` | |
| 85 | `witchery:diamond_vapour` | |
| 86 | `witchery:bat_wool` | |
| 87 | `witchery:wood_ash` | |
| 88 | `witchery:rowan_berries` | |
| 89 | `witchery:wormwood` | |
| 90 | `witchery:exhale_of_horned_one` | |
| 91 | `witchery:hint_of_rebirth` | |
| 92 | `witchery:mutator` | |
| 93 | `witchery:spectral_stone` | `Partial - rite family, binding data, and release-use restored` | Small Rite of Necromancy now creates it again, large Rite of Binding now stores up to 3 nearby same-type spectral mobs in item data, tooltip state is visible again, and holding use now releases the stored spirits again. Still needs in-game verification for summon feel and owner behavior. |
| 94 | `witchery:poppet_shelf_compass` | |
| 95 | `witchery:polynesia_charm` | |
| 95a | `witchery:charm_of_disrupted_dreams` | `Done - missing item and Nightmare anti-strip protection restored` | Original string + fanciful-thread recipe is back, the item is registered with assets/lang, and Nightmare now skips random armor stripping when the player carries one. Needs in-game verification for tab order and feel. |
| 96 | `witchery:parasytic_louse` | |
| 97 | `witchery:bucket_spirit` | |
| 98 | `witchery:hand_bow` | |
| 99 | `witchery:huntsman_spear` | |

---

## Armour

| # | Item ID | Notes |
|---|---|---|
| 100 | `witchery:witches_hat` | |
| 101 | `witchery:witches_robe` | |
| 102 | `witchery:witches_shoes` | |
| 103 | `witchery:hunter_helmet` | |
| 104 | `witchery:hunter_chestplate` | |
| 105 | `witchery:hunter_leggings` | |
| 106 | `witchery:hunter_boots` | |
| 107 | `witchery:deaths_hood` | |
| 108 | `witchery:deaths_robe` | |
| 109 | `witchery:deaths_shoes` | |
| 110 | `witchery:deaths_hand` | |

---

## Brews

| # | Item ID | Notes |
|---|---|---|
| 111 | `witchery:brew_of_raising` | |
| 112 | `witchery:brew_of_sleeping` | |
| 113 | `witchery:brew_of_flowing_spirit` | |
| 114 | `witchery:brew_of_wasting` | |
| 115 | `witchery:brew_of_erosion` | |
| 116 | `witchery:brew_of_foulness` | |
| 117 | `witchery:brew_of_infection` | |
| 118 | `witchery:brew_of_love` | |
| 119 | `witchery:brew_of_the_grotesque` | |
| 120 | `witchery:brew_of_revealing` | |
| 121 | `witchery:brew_of_poison` | |
| 122 | `witchery:brew_of_frost` | |
| 123 | `witchery:brew_of_foaming` | |

---

## Infusion Items

| # | Item ID | Notes |
|---|---|---|
| 124 | `witchery:ghost_of_the_light` | |
| 125 | `witchery:soul_of_the_world` | |
| 126 | `witchery:spirit_of_otherwhere` | |
| 127 | `witchery:infernal_animus` | |
| 128 | `witchery:mystic_unguent` | |
| 129 | `witchery:flying_ointment` | |
| 130 | `witchery:happenstance_oil` | |
| 131 | `witchery:infused_brew_base` | |
| 132 | `witchery:infused_brew_of_soaring` | |
| 133 | `witchery:infused_brew_of_the_grave` | |

---

## Spawn Eggs — NPCs

| # | Item ID | Notes |
|---|---|---|
| 134 | `witchery:coven_witch_spawn_egg` | |
| 135 | `witchery:witch_hunter_spawn_egg` | |

---

## Spawn Eggs — Hostile

| # | Item ID | Notes |
|---|---|---|
| 136 | `witchery:banshee_spawn_egg` | |
| 137 | `witchery:spectre_spawn_egg` | |
| 138 | `witchery:poltergeist_spawn_egg` | |
| 139 | `witchery:treefyd_spawn_egg` | |
| 140 | `witchery:ent_spawn_egg` | |
| 141 | `witchery:mandrake_mob_spawn_egg` | |
| 142 | `witchery:demon_spawn_egg` | |
| 143 | `witchery:nightmare_spawn_egg` | |
| 144 | `witchery:spirit_spawn_egg` | |
| 145 | `witchery:flame_imp_spawn_egg` | |
| 146 | `witchery:lord_of_torment_spawn_egg` | |
| 147 | `witchery:death_spawn_egg` | |

---

## Spawn Eggs — Familiars & Other

| # | Item ID | Notes |
|---|---|---|
| 148 | `witchery:owl_spawn_egg` | |
| 149 | `witchery:familiar_cat_spawn_egg` | |
| 150 | `witchery:toad_spawn_egg` | |
| 151 | `witchery:hobgoblin_spawn_egg` | |
| 152 | `witchery:flying_monkey_spawn_egg` | |
| 153 | `witchery:binky_spawn_egg` | |
