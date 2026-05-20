Deaths scythe is wrong angle. Not touching hand.

~~Where is mutandis item in tab?~~ ✅ Fixed 2026-05-18

# Witchery Tab — Item Review

Items appear in creative tab order. Add comments in the `Notes` column.

---

## Blocks

| # | Item ID | Notes |
|---|---|---|
| 1 | `witchery:witches_primer` | `Partial - opens real Patchouli book when Patchouli is installed` | No longer a silent stub. It now calls the real Patchouli book-open API through a soft optional hook and shows a clear missing-Patchouli message otherwise. Needs in-game verification with Patchouli present.
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
| 18 | `witchery:leaping_lily` | `Partial - leap buff and custom lily visuals restored` | Walking through it now grants short speed + jump boosts again, and the block/item visuals now use the vanilla lily-pad geometry wrapped with a Witchery-specific dark green lily texture instead of the wrong snowbell/brew placeholder graphics. Needs in-game verification.
| 19 | `witchery:grassper` | `Partial - stash mechanic and 3D plant presentation restored` | Now accepts one offered item again, drops it back out on empty-hand interaction like the original, syncs its stored stack to the client more reliably, and uses a resurrected-style custom grassper model for both placed block and item instead of the wrong cross/wrap/cube path. Needs final in-game verification.
| 20 | `witchery:glowglobe` | `Partial - light and ambience restored` | Now emits strong light and ambient flame particles again. Needs in-game verification.
| 21 | `witchery:kettle` | `Partial - shared placed/item model restored` | The failed NeoForge special-renderer path was removed. The item and placed kettle now share the same resurrected-style static block model/`32x32` texture path, while the block entity renderer only draws liquid/bubbles. The kettle now also uses horizontal facing plus side-support survival rules. Needs in-game verification for final support feel and filled/hanging world-state variants.
| 22 | `witchery:distillery` | `Partial - mechanics restored closer to original (altar power, jars, multi-output)` | The item now uses a dedicated NeoForge 1.21 `special` render path backed by the real distillery model instead of the broken static unwrap path. The GUI now follows the resurrected-style texture and slot/overlay layout path. Needs fresh in-game verification for final silhouette + GUI fit.
| 23 | `witchery:spinning_wheel` | `Partial - mechanics restored closer to original (5 slots, altar power, modifiers)` | The placed spinning wheel now uses the older custom model path again with the original-style `spinningwheel` texture restored, and the item is back on the dedicated special-renderer path using that same texture instead of the wrong static/block-model presentation. The GUI already follows the resurrected-style texture and slot/overlay layout path. Needs fresh in-game verification for final silhouette + facing.
| 24 | `witchery:witches_oven` | `Partial - mechanics restored closer to original (jars, funnels, byproducts)` | Item and placed block now both use the resurrected oven block-model path instead of mixing a resurrected item with the older custom placed renderer. The GUI follows the resurrected-style layout path more closely. Final in-game facing/burning-state verification is still open.
| 25 | `witchery:altar` | `Partial - strict 2x3 mechanics and joined-art variants restored closer to original` | The altar now only validates as a real rectangular 2x3 multiblock again instead of any 6-block flood-filled shape, and the connected blockstate/joined-art pass is back so the red joined top only appears on a valid altar footprint. Skull stacking, chalice bonus, Arthana range, Mystic Branch enhancement, and missing nature-power plants were already restored earlier. Needs in-game verification for final orientation/art feel.
| 26 | `witchery:fume_funnel` | `Partial - resurrected model path, original hardness, and linked-oven interaction restored closer to original` | The funnel now uses the resurrected static block-model path for both item and placed block instead of the mixed old custom renderer path. It also restores the original sturdier hardness and once again opens the linked Witch's Oven when right-clicked, using the old side-or-below neighbor search flow. Final in-game silhouette/facing verification still needs a pass.
| 27 | `witchery:brazier` | `Partial - unburned ingredient drop path restored` | Unlit braziers now drop their stored ingredients again when broken instead of silently eating them through the dead legacy removal hook. The bowl/frame now come from the static block-model path again, while the block entity renderer only adds ash when burning. Needs an in-game pass for final bowl fit and ash state.
| 28 | `witchery:poppet_shelf` | `Partial - mechanics, top rendering, placed facing, comparator output, and low shelf footprint restored closer to original` | Now only accepts poppets again, drops its contents on break, scans server shelves for bound poppets again, force-loads shelves with bound poppets so remote protection can still trigger, renders stored poppets on a larger 3x3 top grid again, once more places/rotates as a real facing-aware shelf instead of a north-locked approximation, now emits inventory-strength comparator output that updates when the stored poppets change, and once again uses the original low half-height shelf footprint instead of behaving like a full cube. The item keeps its dedicated special-render path and the GUI pose was lowered closer to the original. Final in-game spacing verification is still open.
| 29 | `witchery:leech_chest` | `Partial - container, taglock storage, break/replace persistence, blocked-open rules, comparator output, and trapped-chest redstone restored` | Opens as a 27-slot chest again, tracks up to 3 recent players, lets Taglock Kits pull a valid stored target, now preserves that stored queue through block drops like the old mod, once again refuses to open when a solid block or sitting cat blocks the lid, now emits chest-style comparator strength that updates as contents change, and once again outputs trapped-chest-like redstone strength while players are using it. The item keeps the dedicated Witchery leech-chest special renderer, was lowered in GUI, and now rotates front-first again closer to the original chest presentation. Needs in-game verification.
| 30 | `witchery:refilling_chest` | `Partial - refill cadence and vanilla chest render path restored closer to original` | Refill logic is Torment-only again, now follows the old hourly life-tick cadence instead of waiting one hour after emptying, and pulls a small dungeon-style refill closer to 1.7.10. The block now runs on the real vanilla chest path again (`ChestBlock` + `ChestBlockEntity` + vanilla chest lid renderer) while keeping the Witchery name/mechanics, the item uses the same vanilla chest graphic path with corrected front-facing yaw, and break particles now have a valid oak-plank fallback instead of pink/black. Final in-game brightness/ground-fit verification is still open.
| 31 | `witchery:candelabra` | `Partial - original light, support, particles, footprint, and 3D item path restored closer to original` | The item now uses a dedicated NeoForge 1.21 `special` render path backed by the candelabra model instead of the old broken static fallback. The placed block now restores the old full light output, sturdier hardness, support-required placement/break behavior, five-point candle flame/smoke ambience, and original narrow non-full-cube footprint instead of behaving like a full block. Needs in-game verification for final model/light feel.
| 32 | `witchery:chalice` | `Partial - altar power bonus, original hardness, support-required placement/survival, filled-state persistence, footprint, and static 3D item model restored closer to original` | Filled vs empty chalice bonus now affects the altar closer to original, the item follows the resurrected/static `block/chalice` model path again instead of the wrong flat/cross/block-wrap path, and the placed chalice once more requires a solid supporting block below, refuses invalid placement up front like the original, uses the original sturdier hardness, breaks if that support is removed, preserves filled-vs-empty state through pick/drop/re-place via item stack data, and once again uses the original compact goblet-sized footprint instead of behaving like a full cube. Filled chalices also emit the old red dust cue again. Final in-game placed-block verification is still open.
| 33 | `witchery:crystal_ball` | `Partial - shared translucent model path, original compact footprint, support survival, and sturdier hardness restored closer to original` | The block now uses the same resurrected-style translucent static model path as the item instead of the older opaque custom-render fallback, once again uses the original small pedestal-sized shape instead of a full-cube hitbox, requires solid support below, refuses invalid placement up front, breaks if that support is removed, and uses the original sturdier hardness more closely. The block entity keeps only the gameplay logic. Needs in-game verification for final translucency feel.
| 34 | `witchery:dream_catcher` | `Partial - placement, original sturdier hardness, weave-only break/pick flow, footprint, nightmare-weave handling, and 3D model path restored closer to original` | Wall placement now uses the face clicked instead of snapping off to the side, the block once again uses the original sturdier hardness, the thin wall-hung footprint now applies to real collision as well as outline instead of behaving like a hidden full cube, Spirit World nightmare/demonic modifiers now only respect the `Nightmares` weave again, breaking or support-dropping a catcher returns only its stored weave again instead of a normal block item, middle-click now returns the stored weave or the original default Fleet Foot weave when empty, and both item + placed block now use the thin 3D dream-catcher model path instead of the wrong cross/forward-floating renderer. The item now also renders through a front-lit special path so it no longer appears unnaturally dark in GUI. Needs in-game verification.
| 35 | `witchery:weave_fleet_foot` | | Looks ok, mechanics?
| 36 | `witchery:weave_iron_arm` | | Looks ok, mechanics?
| 37 | `witchery:weave_fasting` | | Looks ok, mechanics?
| 38 | `witchery:weave_nightmares` | | Looks ok, mechanics?
| 39 | `witchery:weave_intensity` | | Looks ok, mechanics?
| 40 | `witchery:demonic_contract` | | Looks ok, not tested
| 41 | `witchery:icy_needle` | | Works in dream
| 42 | `witchery:soul_of_torment` | | Not tested
| 43 | `witchery:torment_scroll` | | Looks ok
| 44 | `witchery:alluring_skull` | `Partial - stateful skull behavior, light level, placement rules, floor rotation, blast resistance, footprint, and dedicated skull-head renderer restored closer to original` | Now uses a real Witchery block class instead of a dead simple block, has `on/off` state again, restores the old constant low light output, can be activated with the restored `Necromantic Stone`, swaps to the lit skull texture when active, periodically allures undead mobs like the original, supports both floor and wall placement with matching survival shapes, preserves the original standing-skull-style floor rotation from player facing, restores the old high blast resistance, once again uses its compact skull-sized collision/outline footprint instead of behaving like a hidden full block, and the placed block now renders through a dedicated vanilla-skull-backed head renderer instead of static placeholder block models. Needs in-game verification for final scale/lighting feel.
| 45 | `witchery:demon_heart` | `Partial - low light, low hardness, facing, footprint, particles, heartbeat feel, and animated custom model restored closer to original` | Now uses a real Witchery block class instead of a dead simple block, restores the old weak light output, original fragile hardness, horizontal facing, compact original collision/outline footprint instead of behaving like a hidden full cube, ambient flame/smoke particles, and a client heartbeat pulse closer to original, and the placed block now renders through a dedicated animated `DemonHeartModel` path again instead of the older static approximation. Item/inventory pose still needs in-game verification.
| 46 | `witchery:coffin` | `Partial - placed block and 3D item presentation restored closer to original` | The placed coffin now renders from the head block as a full 2-block coffin again using separate head/bottom textures closer to the original mod, and the item/inventory view now uses the same split coffin geometry through a dedicated special renderer instead of the old flat placeholder sprite. Final in-game pose/scale verification is still open.
| 47 | `witchery:rowan_planks` | ✅ Fixed — added to #minecraft:planks tag | Correct texture, need regular plank recipes.
| 48 | `witchery:rowan_door` | ✅ Fixed — block models now use door_rowan_lower/upper textures; item model uses ingredient.door_rowan | Wrong texture. Looks like oak door.
| 49 | `witchery:rowan_door_key` | `Partial - correct Witchery key texture restored` | No longer borrows the vanilla oak-door placeholder in GUI; now points back at the proper Witchery `ingredient.door_key` item texture. Needs in-game verification.
| 50 | `witchery:rowan_keyring` | `Partial - correct Witchery keyring texture restored` | No longer borrows the vanilla iron-ingot placeholder in GUI; now points back at the proper Witchery `ingredient.door_keyring` item texture. Needs in-game verification.
| 51 | `witchery:ice_door` | ✅ Fixed — BlockSetType.OAK (opens on right-click); item model uses packed_ice texture | Wrong item graphics. Looks like ice door when placed. Dont know how to open, not on right click.
| 52 | `witchery:ice_fence` | ✅ Fixed — added to #minecraft:fences tag | Dont link with other fences
| 53 | `witchery:ice_gate` | | Works, link with fence
| 54 | `witchery:ice_pressure_plate` | | Works on fence. tested
| 55 | `witchery:fetish_scarecrow` | `Partial - facing-aware placed render and 3D special item path restored` | The placed fetish now uses facing-aware render state again, and the item no longer wraps a flat block texture. It now renders through the real `FetishModel` with the scarecrow texture via the NeoForge `special` item path. Needs in-game verification for final pose/scale.
| 56 | `witchery:fetish_trent_effigy` | `Partial - facing-aware placed render and 3D special item path restored` | Shares the restored facing-aware placed render, and the item now uses the real `FetishModel` with the `trent` texture instead of the broken flat wrap path. Needs in-game verification.
| 57 | `witchery:fetish_witchs_ladder` | `Partial - facing-aware placed render and 3D special item path restored` | Shares the restored facing-aware placed render, and the item now uses the real `FetishModel` with the `witchsladder` texture instead of the broken flat wrap path. Needs in-game verification.
| 58 | `witchery:statue_goddess` | `Partial - facing-aware placed render and 3D special item path restored` | The placed statue already reads facing from block state again, and the item now uses the real `StatueGoddessModel` through the NeoForge `special` item path instead of the broken flat/generated wrap. Needs in-game verification for final pose/scale.
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
| 134 | `witchery:coven_witch_spawn_egg` | `Partial - local vanilla-style spawn egg template restored` |
| 135 | `witchery:witch_hunter_spawn_egg` | `Partial - local vanilla-style spawn egg template restored` |

---

## Spawn Eggs — Hostile

| # | Item ID | Notes |
|---|---|---|
| 136 | `witchery:banshee_spawn_egg` | `Partial - local vanilla-style spawn egg template restored` |
| 137 | `witchery:spectre_spawn_egg` | `Partial - local vanilla-style spawn egg template restored` |
| 138 | `witchery:poltergeist_spawn_egg` | `Partial - local vanilla-style spawn egg template restored` |
| 139 | `witchery:treefyd_spawn_egg` | `Partial - local vanilla-style spawn egg template restored` |
| 140 | `witchery:ent_spawn_egg` | `Partial - local vanilla-style spawn egg template restored` |
| 141 | `witchery:mandrake_mob_spawn_egg` | `Partial - local vanilla-style spawn egg template restored` |
| 142 | `witchery:demon_spawn_egg` | `Partial - local vanilla-style spawn egg template restored` |
| 143 | `witchery:nightmare_spawn_egg` | `Partial - local vanilla-style spawn egg template restored` |
| 144 | `witchery:spirit_spawn_egg` | `Partial - local vanilla-style spawn egg template restored` |
| 145 | `witchery:flame_imp_spawn_egg` | `Partial - local vanilla-style spawn egg template restored` |
| 146 | `witchery:lord_of_torment_spawn_egg` | `Partial - local vanilla-style spawn egg template restored` |
| 147 | `witchery:death_spawn_egg` | `Partial - local vanilla-style spawn egg template restored` |

---

## Spawn Eggs — Familiars & Other

| # | Item ID | Notes |
|---|---|---|
| 148 | `witchery:owl_spawn_egg` | `Partial - local vanilla-style spawn egg template restored` |
| 149 | `witchery:familiar_cat_spawn_egg` | `Partial - local vanilla-style spawn egg template restored` |
| 150 | `witchery:toad_spawn_egg` | `Partial - local vanilla-style spawn egg template restored` |
| 151 | `witchery:hobgoblin_spawn_egg` | `Partial - local vanilla-style spawn egg template restored` |
| 152 | `witchery:flying_monkey_spawn_egg` | `Partial - local vanilla-style spawn egg template restored` |
| 153 | `witchery:binky_spawn_egg` | `Partial - local vanilla-style spawn egg template restored` |
