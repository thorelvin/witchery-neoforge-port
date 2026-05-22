Deaths scythe is wrong angle. Not touching hand.

~~Where is mutandis item in tab?~~ ✅ Fixed 2026-05-18

# Witchery Tab — Item Review

Items appear in creative tab order. Add comments in the `Notes` column.

---

## Blocks

| # | Item ID | Notes |
|---|---|---|
| 1 | `witchery:witches_primer` | `Partial - opens real Patchouli book when Patchouli is installed` | No longer a silent stub. It now calls the real Patchouli book-open API through a soft optional hook and shows a clear missing-Patchouli message otherwise. Needs in-game verification with Patchouli present.
| 2 | `witchery:rowan_log` | ✅ Fixed — RotatedPillarBlock + axis blockstate variants + self-drop loot | Can only be placed vertically, should work as regular logs all directions; loot now drops itself like vanilla logs.
| 3 | `witchery:rowan_wood` | `Done - self-drop loot restored` | Ok; now drops itself when broken.
| 4 | `witchery:rowan_leaves` | ✅ Fixed — UntintedParticleLeavesBlock, multipart blockstate, logs tag added for decay | Texture wrongly mappet, and not transparent
| 5 | `witchery:rowan_sapling` | `Done - self-drop loot restored` | Works, bone meal too; now drops itself when broken.
| 6 | `witchery:belladonna` | `Partial - restored as the harvested Belladonna flower item instead of the planting stock` | The crop no longer self-plants through the harvested flower. Dedicated `Belladonna Seeds` now handle planting again, and mature harvest now follows the old seed-plus-flower split more closely. Needs in-game verification.
| 6a | `witchery:belladonna_seeds` | `Partial - dedicated Belladonna seed item restored` | Belladonna now once again plants from a seed item instead of the harvested flower, and short grass/ferns once again have a weighted chance to drop it like the old seed-progression bridge. Wild placement rules intentionally stay permissive for now so naturally generated Belladonna does not pop off grass or dirt. |
| 7 | `witchery:mandrake_root` | `Partial - harvested root item and original spawn-vs-drop branch restored` | Mandrake no longer self-plants through the harvested root. Dedicated `Mandrake Seeds` now handle planting again, while mature harvest still keeps the restored original-style summon-vs-drop branch and now yields seeds plus root closer to source whenever the mob does not awaken. Needs in-game verification.
| 7a | `witchery:mandrake_seeds` | `Partial - dedicated Mandrake seed item restored` | Mandrake now once again plants from a seed item instead of the harvested root, and short grass/ferns once again have a weighted chance to drop it like the old seed-progression bridge. Wild placement rules intentionally stay permissive for now so naturally generated Mandrake does not pop off grass or dirt. |
| 8 | `witchery:artichoke` | `Done` | Water Artichoke no longer self-plants through the harvested ingredient: the `artichoke` item stands as the harvested globe used in recipes again, while planting uses a dedicated seed item and the crop once more survives only above still water. |
| 8a | `witchery:artichoke_seeds` | `Done` | Planting now uses a real `Water Artichoke Seeds` item again, mature harvest yields seeds plus the globe ingredient closer to original, short grass/ferns once again have a weighted chance to drop the seed like the old herb-progression bridge, and the seed now right-click plants onto still water instead of using the port's old land-grown shortcut. |
| 9 | `witchery:cotton` | `Partial - restored closer to old Somnian Cotton behavior` | Now behaves like a spreading bush instead of a staged crop, nightmare harvest can yield disturbed cotton again, the plant render/loot path is aligned, and carried disturbed cotton no longer places back into the world like an extra plant block. Needs in-game verification.
| 10 | `witchery:witch_seeds` | `Removed - false port-only Rowan seed shortcut` | The old farmland `Rowan Seeds` / `Rowan Crop` path has been removed again after decompiled/source comparison. Rowan Berries now stay on the real tree-side progression instead of a non-original crop.
| 11 | `witchery:snowbell` | `Partial - restored as the Snowbell plant-side harvest instead of the planting item` | The crop no longer self-plants through this item. Dedicated `Snowbell Seeds` now handle planting again, while the mature crop yields seeds, snowballs, and occasional Icy Needles closer to the old source. The Anointing Paste cold-reagent audit is now closed too: that recipe once again uses `Icy Needle`, not the whole Snowbell plant. |
| 11a | `witchery:snowbell_seeds` | `Partial - dedicated Snowbell seed item restored` | Snowbell now once again plants from a dedicated seed item, short grass/ferns once again have a weighted chance to drop it like the old herb-progression bridge, and mature harvest restores the old seeds + snowballs + occasional Icy Needle split more closely. Wild placement rules intentionally stay permissive for now so naturally generated Snowbell does not pop off cold-biome terrain. |
| 12 | `witchery:blood_rose` | `Partial - blood sample storage, drain safeguards, and sprig creation route restored` | Walking through the rose now stores the last player's UUID/name, switches to the full rose variant, and lets Taglock Kits pull that sample later. Blood Rose extraction once again refuses to hand you your own stored sample and also refuses offline targets instead of creating a false taglock, and the classic Leech Chest-over-water + four-poppy Mutating Sprig creation route is back. Needs in-game verification.
| 13 | `witchery:ember_moss` | `Partial - original fire moss behavior restored` | Now uses a real block class again with low footprint, original low light, fire-on-touch feedback, rare flame ambience, legacy-style spread over dirt/grass/stone/sand/soul sand, no normal drops, and shears-only self drop. Needs in-game spread/shears verification.
| 14 | `witchery:glint_weed` | `Partial - original light, ambience, and spread restored` | Now uses full light level 15 again, matching the old `0.9375F` brightness, emits the old flame ambience, and spreads with the legacy density cap over dirt/grass/stone/sand/soul sand. Needs in-game spread feel verification.
| 15 | `witchery:spanish_moss` | `Partial - original growth, non-ladder, and fire behavior restored` | Now behaves like a faster-growing vine again, no longer counts as a ladder/climbable block, and uses the old high fire spread/flammability values. Needs in-game verification for spread and burn feel.
| 16 | `witchery:bramble` | `Partial - wild bramble hazard, toughness, and sprig creation route restored` | Now hurts entities, spreads into nearby open tiles when broken without shears, uses the original high hardness/blast resistance profile again, and once more grows from the classic cactus-on-sand mutation with four surrounding water sources, four Spanish Moss, and diagonal Grasspers holding two Bone Meal plus two Blaze Powder. Needs in-game verification against original placement/spread feel.
| 17 | `witchery:void_bramble` | `Partial - void teleport, owner break, unbreakable profile, ritual nulling, and sprig creation route restored` | Now teleports living entities to a distant safe point again, has the old unbreakable/high-blast-resistance profile back, no longer requires support below like a normal plant, stores the placer so creative players or the owner can left-click to remove an upward bramble column with item drops, blocks ritual activation within the old 32-block null-source radius, and once more grows from the classic sugar-cane mutation with four surrounding water sources, four Spanish Moss, and four Ender Pearl-loaded Grasspers. Needs in-game verification.
| 18 | `witchery:leaping_lily` | `Partial - leap buff and custom lily visuals restored` | Walking through it now grants short speed + jump boosts again, and the block/item visuals now use the vanilla lily-pad geometry wrapped with a Witchery-specific dark green lily texture instead of the wrong snowbell/brew placeholder graphics. Needs in-game verification.
| 19 | `witchery:grassper` | `Partial - stash mechanic, comparator output, and facing-aware 3D plant presentation restored` | Now accepts one offered item again, drops it back out on empty-hand interaction like the original, syncs its stored stack to the client more reliably, once again emits a full comparator signal while its one-slot stash is occupied, uses a resurrected-style custom grassper model for both placed block and item instead of the wrong cross/wrap/cube path, and the placed model now respects the old facing-aware block orientation again instead of staying in one fixed pose. Needs final in-game verification.
| 20 | `witchery:glowglobe` | `Partial - light, ambience, and non-harvestable behavior restored` | Now emits strong light and ambient flame particles again, and once again behaves like the original ambient block by dropping nothing and returning no middle-click clone stack. Needs in-game verification.
| 21 | `witchery:kettle` | `Partial - classic texture/model path restored closer to original` | The item and placed kettle now once again use the older Witchery kettle texture sheet instead of the more generic `kettle_item` fallback, while the block entity renderer still only handles the liquid/bubble overlay. The kettle also keeps its restored horizontal facing plus side-support survival rules, and it no longer exposes a full-cube support face to neighbors so fence-post hanging rigs stop visually connecting into the kettle body. Needs in-game verification for final support feel and filled/hanging world-state variants.
| 22 | `witchery:distillery` | `Partial - mechanics restored closer to original (altar power, jars, multi-output, comparator output, active light/particles)` | The item now uses a dedicated NeoForge 1.21 `special` render path backed by the real distillery model instead of the broken static unwrap path. The GUI still follows the resurrected-style texture and slot/overlay layout path, but the screen now explicitly blits the intended `176x166` panel region plus the exact overlay slices instead of compressing the full `256x256` sheet into the container, which removes the black barcode-style stripe artifact from the right-margin art. The block once again emits chest-style comparator strength from its inventory like the original, and it now regains the old low active glow plus top-side spell-like ambience while actually distilling instead of staying inert. Needs fresh in-game verification for final silhouette + GUI fit.
| 23 | `witchery:spinning_wheel` | `Partial - mechanics restored closer to original (5 slots, altar power, modifiers, comparator output, hardness)` | The placed spinning wheel now uses the older custom model path again with the original-style `spinningwheel` texture restored, the item is back on the dedicated special-renderer path using that same texture instead of the wrong static/block-model presentation, the block once again emits inventory-strength comparator output like the original, and the original sturdier `3.5F` hardness is back. The GUI still follows the resurrected-style texture and slot/overlay layout path, and the screen now explicitly blits the intended `176x166` panel region plus the exact overlay slices instead of compressing the full `256x256` sheet into the container, which removes the black barcode-style stripe artifact from the right-margin art. Needs fresh in-game verification for final silhouette + facing.
| 24 | `witchery:witches_oven` | `Partial - mechanics restored closer to original (jars, funnels, byproducts, comparator output, burning light/state/ambience)` | The placed oven now renders through the older Witchery custom model again, the item also uses a matching special-render path, and both are back on the classic `witches_oven` texture sheet instead of the later resurrected baked-model geometry. The GUI follows the resurrected-style layout path more closely, and the screen now explicitly blits the intended `176x166` panel region plus the exact fire/arrow overlay slices instead of compressing the full `256x256` sheet into the container, which removes the black barcode-style stripe artifact from the right-margin art. The animated cook arrow is also back on the correct upper-arrow lane instead of drawing offset below the background guide arrow. The block once again emits inventory-strength comparator output like the original furnace-style container, still uses a real `lit` blockstate for burning light/logic, and the front-face smoke/flame ambience now returns while the oven is actually burning. Final in-game facing/burning-state verification is still open.
| 25 | `witchery:altar` | `Partial - strict 2x3 mechanics and resurrected-style joined-art path restored closer to original` | The altar now only validates as a real rectangular 2x3 multiblock again instead of any 6-block flood-filled shape, and the connected blockstate/joined-art pass now follows the resurrected single-texture altar asset path instead of the older placeholder top/joined overlays. The joined corner models also hide the same internal support faces as Resurrected again instead of drawing stray altar-texture panels, and neighbor refresh now covers the full possible 2x3 footprint so partial builds clear stale joined states more reliably. Skull stacking, chalice bonus, Arthana range, Mystic Branch enhancement, and missing nature-power plants were already restored earlier. Needs fresh in-game verification for final orientation/art feel after the hidden-face/refresh pass.
| 26 | `witchery:fume_funnel` | `Partial - classic custom model path, original hardness, and linked-oven interaction restored closer to original` | The placed funnel now renders through the older Witchery custom model again, the item also uses the matching special-render path, and both are back on the classic `fumefunnel` texture sheet instead of the later resurrected baked-model geometry. It also restores the original sturdier hardness and once again opens the linked Witch's Oven when right-clicked, using the old side-or-below neighbor search flow. Final in-game silhouette/facing verification still needs a pass.
| 27 | `witchery:brazier` | `Partial - unburned ingredient drop path, comparator output, original hardness, and bowl footprint restored` | Unlit braziers now drop their stored ingredients again when broken instead of silently eating them through the dead legacy removal hook. The bowl/frame now come from the static block-model path again, the placed block once more uses the original narrow bowl footprint for outline/collision instead of behaving like a hidden full cube, the block once again emits inventory-strength comparator output like the original ingredient container, the original sturdier `3.5F` hardness is back, and the block entity renderer only adds ash when burning. Needs an in-game pass for final bowl fit and ash state.
| 28 | `witchery:poppet_shelf` | `Partial - mechanics, top rendering, placed facing, comparator output, and low shelf footprint restored closer to original` | Now only accepts poppets again, drops its contents on break, scans server shelves for bound poppets again, force-loads shelves with bound poppets so remote protection can still trigger, renders stored poppets on a larger 3x3 top grid again, once more places/rotates as a real facing-aware shelf instead of a north-locked approximation, now emits inventory-strength comparator output that updates when the stored poppets change, and once again uses the original low half-height shelf footprint instead of behaving like a full cube. The item keeps its dedicated special-render path and the GUI pose was lowered closer to the original. Final in-game spacing verification is still open.
| 29 | `witchery:leech_chest` | `Partial - container, taglock storage, break/replace persistence, blocked-open rules, comparator output, trapped-chest redstone, and chest footprint restored` | Opens as a 27-slot chest again, tracks up to 3 recent players, lets Taglock Kits pull a valid stored target, now preserves that stored queue through block drops like the old mod, once again refuses to open when a solid block or sitting cat blocks the lid, now emits chest-style comparator strength that updates as contents change, once again outputs trapped-chest-like redstone strength while players are using it, and now uses the original compact chest-sized outline/collision footprint instead of behaving like a hidden full cube. The item keeps the dedicated Witchery leech-chest special renderer, was lowered in GUI, and now rotates front-first again closer to the original chest presentation. Needs in-game verification.
| 30 | `witchery:refilling_chest` | `Partial - refill cadence and vanilla chest render path restored closer to original` | Refill logic is Torment-only again, now follows the old hourly life-tick cadence instead of waiting one hour after emptying, and pulls a small dungeon-style refill closer to 1.7.10. The block now runs on the real vanilla chest path again (`ChestBlock` + `ChestBlockEntity` + vanilla chest lid renderer) while keeping the Witchery name/mechanics, the item uses the same vanilla chest graphic path with corrected front-facing yaw, and break particles now have a valid oak-plank fallback instead of pink/black. Final in-game brightness/ground-fit verification is still open.
| 31 | `witchery:candelabra` | `Partial - original light, support, particles, footprint, and 3D item path restored closer to original` | The item now uses a dedicated NeoForge 1.21 `special` render path backed by the candelabra model instead of the old broken static fallback. The placed block now restores the old full light output, sturdier hardness, support-required placement/break behavior, five-point candle flame/smoke ambience, and original narrow non-full-cube footprint instead of behaving like a full block. Needs in-game verification for final model/light feel.
| 32 | `witchery:chalice` | `Partial - altar power bonus, original hardness, support-required placement/survival, filled-state persistence, footprint, and water-filled presentation restored closer to original` | Filled vs empty chalice bonus now affects the altar closer to original, the item follows the resurrected/static `block/chalice` model path again instead of the wrong flat/cross/block-wrap path, and the item model now also has a smaller dedicated GUI transform so it no longer overfills the creative/inventory slot. The placed chalice once more requires a solid supporting block below, refuses invalid placement up front like the original, uses the original sturdier hardness, breaks if that support is removed, preserves filled-vs-empty state through pick/drop/re-place via item stack data, once again uses the original compact goblet-sized footprint instead of behaving like a full cube, and now visibly renders water inside the cup when filled instead of always looking empty. Filled chalices also emit the old red dust cue again. Final in-game placed-block and item-pose verification is still open.
| 33 | `witchery:crystal_ball` | `Partial - shared translucent model path, original compact footprint, support survival, sturdier hardness, and use feedback restored closer to original` | The block now uses the same resurrected-style translucent static model path as the item instead of the older opaque custom-render fallback, once again uses the original small pedestal-sized shape instead of a full-cube hitbox, requires solid support below, refuses invalid placement up front, breaks if that support is removed, uses the original sturdier hardness more closely, and once again plays the old snare-style failure cue plus levelup/explosion success feedback when used. The block entity keeps only the gameplay logic. Needs in-game verification for final translucency feel.
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
| 44 | `witchery:alluring_skull` | `Partial - stateful skull behavior, light level, placement rules, floor rotation, blast resistance, footprint, and dedicated skull-head renderer restored closer to original` | Now uses a real Witchery block class instead of a dead simple block, has `on/off` state again, restores the old constant low light output, can be activated with the restored `Necromantic Stone`, swaps to the lit skull texture when active, periodically allures undead mobs like the original, supports both floor and wall placement with matching survival shapes, preserves the original standing-skull-style floor rotation from player facing, now follows vanilla nearest-looking standing-vs-wall placement so edge hits stop randomly flipping the mount/facing while placing several in a row, restores the old high blast resistance, once again uses its compact skull-sized collision/outline footprint instead of behaving like a hidden full block, and the placed block now renders through a dedicated vanilla-skull-backed head renderer instead of static placeholder block models. The renderer now uses model-only skull textures with a filled underside so wall-mounted skulls no longer show a transparent bottom, and skull yaw is applied through the pose stack instead of a shared mutable head pose so nearby standing skulls stop turning together. Needs in-game verification for final scale/lighting feel.
| 45 | `witchery:demon_heart` | `Partial - low light, low hardness, facing, footprint, particles, heartbeat feel, and animated custom model restored closer to original` | Now uses a real Witchery block class instead of a dead simple block, restores the old weak light output, original fragile hardness, horizontal facing, compact original collision/outline footprint instead of behaving like a hidden full cube, ambient flame/smoke particles, and a client heartbeat pulse closer to original. The heart-shaped animated custom model is back again instead of the patchwork baked-shell fallback, it now renders on a no-cull cutout layer, the renderer uses a padded model-only Demon Heart texture so modern sampling no longer punches transparent holes through the old UV islands as aggressively, and the remaining placement-side shell faces now have their model-texture UV islands filled directly instead of relying on the original sparse alpha cutouts. Final in-game verification of the remaining Demon Heart face fill and pulse feel is still open.
| 46 | `witchery:coffin` | `Partial - placed block presentation, low footprint, and 3D item presentation restored closer to original` | The placed coffin now renders from the head block as a full 2-block coffin again using separate head/bottom textures closer to the original mod, now also uses its intended lower coffin-height collision instead of acting like a hidden full cube, and the item/inventory view uses the same split coffin geometry through a dedicated special renderer instead of the old flat placeholder sprite. Final in-game pose/scale verification is still open.
| 47 | `witchery:rowan_planks` | `Done - planks recipe/tag/loot plus Rowan stair/slab family restored` | Rowan logs craft into four planks, planks are tagged as Minecraft planks, self-drop when broken, and the original Rowan stairs/slab family is now registered, craftable, tagged, lootable, flammable at the old `5/20` profile, and backed by rowan plank block/item models.
| 47a | `witchery:rowan_stairs` | `Done - original Rowan stairs restored` | Restores the old rowan stairs block as a craftable stair with rowan plank textures, item model, loot table, Minecraft stair tags, and original-style `5.0F/20.0F` strength plus `5/20` fire profile.
| 47b | `witchery:rowan_slab` | `Done - original Rowan slab restored` | Restores the old rowan slab as a craftable slab with rowan plank textures, item model, double-slab drops, Minecraft slab tags, and original `2.0F` hardness / `5.0F` resistance plus `5/20` fire profile.
| 48 | `witchery:rowan_door` | `Partial - door model, item texture, lock flow, hardness, and lower-half loot restored` | Block models now use the Witchery rowan lower/upper door textures, the item model uses `ingredient.door_rowan`, placement binds a matching key, key/keyring access is enforced, the block now restores the original `5.0F` rowan-door hardness instead of the generic wood-door value, and breaking the lower half drops the door item. Needs in-game verification for final lock/unlock feel.
| 49 | `witchery:rowan_door_key` | `Partial - correct Witchery key texture restored` | No longer borrows the vanilla oak-door placeholder in GUI; now points back at the proper Witchery `ingredient.door_key` item texture. Needs in-game verification.
| 50 | `witchery:rowan_keyring` | `Partial - correct Witchery keyring texture restored` | No longer borrows the vanilla iron-ingot placeholder in GUI; now points back at the proper Witchery `ingredient.door_keyring` item texture. Needs in-game verification.
| 51 | `witchery:ice_door` | ✅ Fixed — BlockSetType.OAK (opens on right-click); item model uses packed_ice texture | Wrong item graphics. Looks like ice door when placed. Dont know how to open, not on right click.
| 52 | `witchery:ice_fence` | ✅ Fixed — added to #minecraft:fences tag | Dont link with other fences
| 53 | `witchery:ice_gate` | | Works, link with fence
| 54 | `witchery:ice_pressure_plate` | | Works on fence. tested
| 55 | `witchery:fetish_scarecrow` | `Partial - placement facing, original toughness, and 3D scarecrow render path restored` | The placed scarecrow now faces the placer instead of yawing left or showing its back on placement, the block once more uses original-style sturdier `3.5F` hardness plus very high blast resistance instead of the old placeholder `1.0F` strength, and the item still renders through the dedicated `FetishModel` special-item path. Needs in-game verification for final pose/scale.
| 56 | `witchery:fetish_trent_effigy` | `Partial - placement facing, original toughness, and dedicated Trent render path restored` | Trent Effigy now shares the fixed face-the-placer placement facing, keeps the restored sturdier fetish hardness/blast resistance profile, and once again renders with a dedicated Trent model/texture instead of borrowing the scarecrow model. Needs in-game verification for final pose/scale.
| 57 | `witchery:fetish_witchs_ladder` | `Partial - placement facing, original toughness, flat ladder block/item models, and cutout render path restored` | Witch's Ladder now shares the fixed face-the-placer placement facing, keeps the restored sturdier fetish hardness/blast resistance profile, renders in-world through its own flat ladder model instead of the scarecrow renderer, explicitly uses the cutout block render path its transparent sprite needs, and uses the matching ladder item model again in GUI/inventory. Needs in-game verification.
| 58 | `witchery:statue_goddess` | `Partial - facing-aware placed render, original toughness, tall footprint, owner recall break, curse/nightmare cleansing feedback, and 3D special item path restored` | The placed statue already reads facing from block state again, the block now also restores the original lower `2.5F` hardness plus high blast resistance instead of the later placeholder strength, and once again uses the original tall narrow 2-block-style outline/collision footprint instead of behaving like a hidden full cube. It also remembers its placer again, so creative players or the sneaking owner can instantly take a stacked statue back down like the old block. Right-click now restores the old fizz/snare feedback loop and removes the Witchery curse/nightmare signatures the current port actually tracks: misfortune, insanity, sinking, overheating, and waking nightmare. The item uses the real `StatueGoddessModel` through the NeoForge `special` item path instead of the broken flat/generated wrap. Remaining work is final in-game pose/scale verification plus any broader curse-state parity beyond the modern port's tracked afflictions.
| 59 | `witchery:wicker_bundle` / `witchery:bloodied_wicker_bundle` | `Partial - rotated pillars, bloodied variant, flammability, item models, Demonic Blood use, Wickerman summon, and debug spawn helper restored` | Plain and Bloodied Wicker Bundles now place as real rotated pillars, restore the old `20/20` fire spread/flammability profile, render in inventory from block models, drop themselves, and can craft five bloodied bundles from wicker plus Demonic Blood. Demonic Blood also restores the old right-click behavior: one item converts up to five nearby plain wicker blocks in a 3x3x3 around the clicked block into bloodied wicker while preserving axis. Flint-and-steel on a valid old Witchery bloodied wickerman now validates the full documented 20-bundle structure, can be triggered from any bloodied bundle in that structure, ignites the matching wickerman blocks, and summons the Horned Huntsman boss, whose melee damage now uses the old random `7 + rand(15)` profile instead of a flat modern attribute hit, whose arrows now use the old distance/difficulty-scaled damage formula, and whose footsteps/throw cues now use the old iron-golem sounds again. `/witchery spawn wickerman` now builds a ready-to-light bloodied wickerman five blocks in front of the player, rotated from player facing with a more deliberate mixed-axis straw-man pose, for setup and QA. Needs in-game verification for exact structure offsets and Huntsman pose/scale.
| 60 | `witchery:critter_snare` | `Partial - catch/release state, caught textures, and sprig creation route restored` | Now catches bats, silverfish, small slimes, and small magma cubes like old Witchery, stores the caught type in block state, uses the matching caught texture, plays occasional critter sounds, drops/picks its caught state, releases the creature on sneak-use, and once more grows from the classic cobweb-over-water + four dropped Alder Saplings + trapped Zombie Mutating Sprig route. The old Polynesia Charm bat stock-inventory flag is deferred until that item has real mechanics.
| 61 | `witchery:witch_door` | ✅ Fixed — converted to DoorBlock(OAK), full door blockstates/models created, DoubleHighBlockItem; uses rowan textures as placeholder | Not a door, shaped as cube. Texture wrapped wrong. Wont open.

---

## Tools & Items

| # | Item ID | Notes |
|---|---|---|
| 62 | `witchery:broom` | `Partial - control model rewritten closer to 1.7.10` | Forward acceleration, pitch lift, no forced hover-drop, and mounted fall cancellation are restored. Needs in-game verification.
| 63 | `witchery:enchanted_broom` | `Partial - shares the updated broom flight path` | Needs in-game verification.
| 64 | `witchery:arthana` | `Partial - altar placement/range bonus restored` | Now places cleanly on altar, updates bonuses immediately, and restores the old altar range boost. Visual work still needed.
| 65 | `witchery:boline` | |
| 66 | `witchery:chalk` | `Partial - placed glyph block path corrected` | Ritual chalk now places a thin floor glyph again instead of a cube block: `circle_glyph` uses the flat cutout/carpet model path and the chalk-circle blocks now share a dedicated low-profile block shape. Needs in-game verification for final outline/selection feel.
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
| 84 | `witchery:redstone_soup` | `Done - source-backed kettle base only` | The false direct distillery shortcut is gone again; Redstone Soup now stays on its original kettle progression as the base for infusions and related alchemy chains. |
| 85 | `witchery:diamond_vapour` | |
| 86 | `witchery:bat_wool` | |
| 86a | `witchery:concentrated_bat_ball` | `Done - classic critter-snare crafting bridge restored` | The real intermediate item is back: four bat-filled `Critter Snare`s with four `Slime Ball`s now craft into `Concentrated Bat Ball` again, and `Brew of Bats` now uses that chain instead of the old bat-spawn-egg placeholder. |
| 87 | `witchery:wood_ash` | |
| 88 | `witchery:rowan_berries` | |
| 89 | `witchery:wormwood` | `Partial - classic mutation route plus separate harvest item restored closer to original` | The old advanced mutation bridge is back: fully grown wheat with four adjacent Wispy Cotton and four diagonal water corners now once again mutates into young Wormwood when touched by a Mutating Sprig. Wormwood is now also split back away from its seed, so the crop yields a real herb item while planting uses a dedicated seed again. Needs in-game verification for growth/drop feel.
| 89a | `witchery:wormwood_seeds` | `Partial - dedicated seed item restored` | Wormwood no longer self-plants through the harvested herb item: planting now uses a separate `Wormwood Seeds` item again, the crop once more insists on farmland, and mature harvest now yields seeds plus the Wormwood ingredient closer to original. Needs in-game verification.
| 90 | `witchery:exhale_of_horned_one` | |
| 91 | `witchery:hint_of_rebirth` | |
| 92 | `witchery:mutator` | `Partial - restored as the real Mutating Sprig with the cauldron recipe, core block-conversion tool role, and first seven classic mutation routes` | `Mutator` is now truthfully surfaced as `Mutating Sprig`, brewed from a heated Witch's Cauldron with `Mutandis Extremis`, `Nether Wart`, and `Ent Twig`, has the old 16-durability tool role again, restores the controlled Grass Block <-> Mycelium plus underwater Dirt <-> Clay transformations, performs the classic empty-chest-over-water + four-grass `Grassper` mutation, mutates an empty `Trapped Chest` with four adjacent vines and water beneath them into a `Leech Chest`, mutates a cobweb over water with four dropped `Alder Sapling` items and a trapped Zombie into a `Critter Snare`, mutates a `Leech Chest` over water with four surrounding `Poppy` plants into four `Blood Roses`, mutates a cactus with four Spanish Moss plus diagonal Bone Meal/Blaze Powder Grasspers into `Wild Bramble`, mutates sugar cane with four Spanish Moss plus four Ender Pearl Grasspers into `Void Bramble`, and now also mutates fully grown wheat with four adjacent Wispy Cotton plus four diagonal water corners into `Wormwood`. The wider advanced mutation-structure family beyond that still remains follow-up work. |
| 93 | `witchery:spectral_stone` | `Partial - rite family, binding data, and release-use restored` | Small Rite of Necromancy now creates it again, medium `bindspectral` now stores up to 3 nearby same-type spectral mobs in item data, tooltip state is visible again, and holding use now releases the stored spirits again. Still needs in-game verification for summon feel and owner behavior. |
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
| 116 | `witchery:brew_of_foulness` | `Deferred - custom brew-side placeholder only` | The false direct kettle recipe is gone again after source comparison; this item remains registered for the later Custom Brew sprint instead of claiming source-backed survival acquisition now. |
| 117 | `witchery:brew_of_infection` | |
| 118 | `witchery:brew_of_love` | |
| 119 | `witchery:brew_of_the_grotesque` | |
| 120 | `witchery:brew_of_revealing` | |
| 121 | `witchery:brew_of_poison` | `Deferred - custom brew-side placeholder only` | The false direct kettle recipe is gone again after source comparison; the scraped wiki and broader Custom Brew notes point to this as a cauldron/custom-brew effect family rather than a base 1.7.2 kettle brew. |
| 122 | `witchery:brew_of_ice` | Displays as `Brew of Frost` like original Witchery |
| 123 | `witchery:brew_of_foaming` | `Deferred - custom brew-side placeholder only` | The false direct kettle recipe is gone again after source comparison; this item remains registered for the later Custom Brew sprint instead of claiming source-backed survival acquisition now. |

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
