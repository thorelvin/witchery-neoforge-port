# Active Bugs & Fix Plan

Last reconciled: 2026-05-21

This file is the active sprint board. When a bug is fixed, move it to **Fixed / Verify** with the commit/task notes instead of deleting it.

---

## Priority board

| Priority | Item | Effort | Status |
|---|---|---:|---|
| Done | Remove GhostModel mouth protrusion | 2-5 min | Fixed (mouth part never present in port) |
| Done | Fix CatFamiliarModel body rotation | 1-5 min | Fixed (body uses PartPose.offset, no rotation) |
| Done | Fix Mandrake spawn category mismatch | 1-5 min | Fixed (MONSTER category + spawn placement registered) |
| Done | Register spawn placements for biome-spawned mobs | 10-20 min | Fixed |
| Done | Demon UV/texture-size audit | 20-45 min | Fixed - model corrected to 128x32, all UVs at y=0 row, wings added |
| Done | Werewolf pre-render body hide | 20-45 min | Fixed - SyncPlayerDataPayload now includes entityId; handleOnClient updates correct tracked entity |
| Done | Verify Torment visibility / darkness | 10-20 min | Fixed - Force no longer blocks light and Torment now has low ambient skylight |
| Done | Decide Torment red pulse/fog effect | 5-20 min | Decision made; keep red biome sky/fog without Nether pulse |
| Done | Restore Torment maze block/layout parity | 20-45 min | Fixed - legacy Bedrock/Force `GenerateMaze.display` layout restored |
| Feature | Spirit World vegetation + Nightmare spawning polish | 1-3 hrs | Backlog |

---

## Open bugs

### 1. Ritual queue parity is still partial

Current state:
- Circles now persist after activation again.
- Rite ingredients now consume only on successful activation.
- New casts now stop cleanly once a rite route is actually identified, even if that rite then aborts on `already_active`, coven, obstruction, or taglock/precondition failures, instead of falling through to an unrelated smaller-ring retry.
- Staged rites now retain the dropped ingredient long enough to refund it on the clearest modern abort paths again: broken sustaining circles, lost altar-upkeep on the protection-family rites, and internal Climate Change plan-loss aborts.
- Active rites that collapse under broken circles or lost upkeep now play the original snare-style abort cue again instead of failing silently mid-flight.
- Active rites now also drop cleanly if the sustaining ring is rebuilt into the wrong chalk family instead of continuing on any same-size chalk loop.
- Completely unknown offerings on a valid circle now play the original snare-style failure cue and report `Unknown rite.` again instead of failing silently.
- Inventory taglocks on curse/transposition routes now survive the final dampening/poppet-protection abort checks instead of being consumed too early.
- Climate Change no longer burns its dropped biome focus or glowstone when the rite fails at the final `already_active` queue gate.
- Climate Change staged aborts now refund their consumed biome-focus items and glowstone support sacrifices too, instead of only refunding the main dropped trigger item.
- Broiling, Broken Earth, Moving Earth, Raise Column, Volcano, and Flying Ointment's broom infusion now require the original chalk family again instead of activating from any same-size circle.
- Flying Ointment's broom infusion now also routes from the original medium ritual ring only when the inner small ritual ring is present again, instead of firing from a lone large ritual circle.
- The four player infusion rites now also require their original small-plus-medium paired-circle layouts again: Light and Overworld infusions expect ritual chalk on both rings, Otherwhere infusion expects otherwhere chalk on both, and Infernal infusion expects infernal chalk on both instead of all four firing from any lone small circle.
- Those same four player infusions now also restore their original altar startup costs again instead of infusing players for free: Light costs 2000 altar power, while Overworld, Otherwhere, and Infernal each cost 4000.
- `Hell on Earth` now also expects its original mixed three-ring stack again — large infernal outer ring, medium otherwhere middle ring, and small infernal inner ring — instead of activating from any generic large-circle `nether_star` drop.
- Fertility now routes from the original small ritual circle again instead of the wrong medium ring.
- That small-circle fertility route now also restores its original support gate and startup cost again: it once more expects `Hint of Rebirth`, `Diamond Vapour`, `quicklime`, `gypsum`, and `Mutandis`, and once more consumes 3000 altar power before the growth pulse begins.
- The old portable fertility sibling is back in practical form too: a `bone_meal` trigger with `Hint of Rebirth`, `Diamond Vapour`, `quicklime`, `gypsum`, `Mutandis Extremis`, and a nearby `Charged Attuned Stone` now takes the portable branch instead of collapsing into the normal altar-powered rite or the charged-stone banish path.
- Sanctity, Imprisonment, and the monster-only barrier variant now route from small ritual circles again, while the broader Protection dome now queues from its original medium ritual ring instead of the wrong large route.
- `bindspectral` now routes from the original medium ritual circle again instead of the wrong large ring.
- Fetish binding now routes from the original medium ritual circle again instead of the wrong large ring.
- `Nature's Power` now routes from the original medium ritual circle again instead of the wrong large ring, and its staged active-rite state now remembers that same medium ring instead of collapsing under a stray large-ring sustain check.
- Sky's Wrath now routes from the original small ritual circle again instead of the wrong medium ring.
- The non-original redstone Charging buff fallback is gone again, and the real paired-circle `chargestone` path now exists again: a plain Attuned Stone on the medium ritual ring with its inner small ritual companion plus the original glowstone-dust, redstone, wood-ash, and quicklime support offerings plus 2000 altar power now spits out a Charged Attuned Stone instead of falling through to the portable-barrier placeholder.
- The original infusion charge state is back too: successful infusion rites now seed `200/200` current/max energy again, that charge data persists in player saves, syncs to the client, and shows up in `/witchery status` instead of leaving infused players with no meter at all.
- The old `infusionrecharge` half is back too: a `Potion of Regeneration` on the medium ritual ring with its inner small ritual companion and 4000 altar power now raises the original indefinite charging field again, pulsing 10 infusion energy per second into infused players standing inside the circle while draining 40 altar power per second.
- The old infused-brew pair is back too: an `Infused Brew Base` on the medium ritual ring with its inner small ritual companion now once again branches into the original 3000-power living-sacrifice infusions, creating Brew of Soaring from a broom, feather, Arthana, and an owl, or Brew of the Grave from a bone, Dense Web, Necromantic Stone, and a zombie.
- Blank Circle Talismans now bind from any present circle stack again instead of being incorrectly trapped behind the large-circle dispatcher, and the stored talisman data no longer defaults missing rings back to ritual glyphs.
- `summonwither` now routes from the original large infernal ring only when the inner medium infernal ring is present again, instead of collapsing onto a single medium infernal circle.
- The small ritual bound-waystone copy path is back closer to original too: a dropped bound waystone on ritual chalk now copies its location onto one extra offered unbound waystone, consumes the original Ender Dew plus diamond support offerings again, and spits out two bound waystones instead of falsely collapsing into the representative Banish Demon branch.
- Weather Call Storm, Rain of Toads, Summoning, Moving Earth, Raise Column, Crater, Sanctity, and Imprisonment now run as staged active rituals instead of one-shot effects.
- Active rites now stop if the sustaining circle is broken and no longer stack onto an already-busy center.

Remaining gap:
- The original tile-entity ritual queue, upkeep lifecycle, and abort/refund behavior are only partially restored.
- The Charging family is functionally back now, but it still runs through the port's simplified active-ritual queue and refund/upkeep handling rather than the full original tile-entity rite registry.
- See `docs/status/NEXT_FIXES_PLAN.md` for the ordered parity roadmap.

Files:
- `src/main/java/com/emoniph/witchery/ritual/RitualExecutor.java`
- `src/main/java/com/emoniph/witchery/events/RitualEvents.java`

---

## Fixed / verify

### 0. Chalk circles were still placing as cube blocks - Fixed / verify

The four chalk circle marks now use a dedicated thin `ChalkCircleBlock` again instead of generic full-shape `Block` registrations, so their outline/support behavior is back to reading like drawn floor sigils instead of square blocks. The ritual chalk asset chain is corrected too: `circle_glyph` no longer uses a `cube_all` model and now matches the flat cutout/carpet-style path already used by the other chalk families.

### 1a. Merchant trade GUIs flashed shut immediately - Fixed / verify

`CovenWitchEntity` and `HobgoblinEntity` now follow the vanilla merchant-open contract again by setting the active trading player before opening their menu. That keeps the trade menu from failing `MerchantMenu.stillValid()` on the next tick, which was causing the one-frame flash-close behavior. The interaction pass also now lets vanilla item interactions win before trade opening where appropriate.

### 1a.i. Hobgoblin renderer was still a scaled zombie fallback - Fixed / verify

`HobgoblinRenderer` no longer bakes the vanilla zombie layer and scales it down as an approximation. The port now has a dedicated `HobgoblinModel` built from Witchery: Resurrected's `ModelGoblin` proportions/UV layout instead, so the Hobgoblin textures land on the intended short goblin body again instead of stretching into hollow green slabs. The client launch path also now bakes that layer in the exact 1.21 `HumanoidModel` head/hat structure, which removes the black-screen `Can't find part hat` startup failure the first pass introduced.

### 1b. Kettle side supports were too strict for fence rigs - Fixed / verify

`KettleBlock` now accepts fences, walls, iron bars, and sturdy full blocks as valid side supports again instead of requiring only the old narrow support subset. The kettle no longer exposes a full-cube support shape to neighbors, so support posts can hold it up without visually linking into the kettle body. `/witchery setup stations` now clears a 20x20 pad in front of the player only at and above door/player height, then rebuilds a full Rowan QA hut five blocks ahead with an 11x11 footprint, a 3-deep stone-brick foundation, lit windows and a Rowan door, a linked altar, a cobblestone-supported kettle over a campfire with a nearby water cauldron, a distillery, a witch's oven with a fume funnel above it, a spinning wheel, a stocked double chest, indoor Coven Witch + Hobgoblin trader spawns, and ceiling Glowglobes for light. The hut shell now sits one block lower while the altar, machines, chest, and traders stay on the original player-height test plane, the altar rig is offset off the back wall so the Rowan wall no longer disappears behind it, and the hut window panes now recompute their connected state from the finished frame so they visually meet the Rowan walls instead of only later pane neighbors. The oven, distillery, and spinning wheel are preloaded with one ready recipe each for faster machine QA.

### 1c. Base alchemy ingredient chain had hidden and missing steps - Fixed / verify

`odour_of_purity`, `oil_of_vitriol`, and the related early-game intermediates were already registered with real item assets and real Witchery recipes, including the `fanciful_thread` spinning recipe that consumes `odour_of_purity`. The actual break was lower in the chain: `clay_jar_soft` had its crafting recipe, but the smelting step into the usable `clay_jar` item was missing, which made parts of the oven/distillery progression look absent in survival.

Current state:
- `clay_jar_soft -> clay_jar` now has the missing furnace smelting recipe again.
- The Witchery creative tab now exposes the omitted base-chain items for quicker QA: `breath_of_goddess`, `whiff_of_magic`, `foul_fume`, `quicklime`, `gypsum`, `odour_of_purity`, `oil_of_vitriol`, `reek_of_misfortune`, `golden_thread`, both clay jars, and the current `alder_sapling` / `hawthorn_sapling` ingredient placeholders.
- `alder_sapling` and `hawthorn_sapling` still need a broader survival/worldgen pass later; this fix makes the base item chain visible and the jar progression usable again without claiming those placeholder ingredients are fully world-integrated yet.

### 2. GhostModel mouth protrusion - Fixed

Current `GhostModel` has no mouth part - it was never introduced in the port. Arm UV sharing (both arms at `texOffs(0,0)`) may still cause slight Poltergeist texture mirroring issues but no protrusion geometry exists. Verify in-game if arm UV ever looks wrong.

---

### 3. Familiar cat body rotation - Fixed

`CatFamiliarModel` body already uses `PartPose.offset(0, 11, 2)` (no rotation). Leg positions are `(+-3, 18, 7)` back and `(+-3, 18, -5)` front.

---

### 4. Demon model transparent holes - Fixed

`demon.png` is 128x32. Model now declares `128, 32`. All part UVs corrected to y=0 row: body `texOffs(64,0)`, arms `texOffs(48,0)`, legs `texOffs(32,0)`. Head detail parts (horns, tusks, snout, lip) restored. Wings added at `texOffs(93,0)`.

---

### 5. Werewolf player body visible under model - Fixed

`WitcheryClientEvents` already implements `RenderPlayerEvent.Pre` (hides model) and `RenderPlayerEvent.Post` (restores it).

---

### 6. Spawn placement warnings - Fixed

Handled by spawn placement registration for the biome-spawned mobs.

### 8. Spawn eggs were still relying on a flat fallback icon chain - Fixed / verify

Witchery spawn eggs now use a local copy of the vanilla two-layer spawn-egg template again instead of the temporary flat fallback sprite chain. This avoids the missing `minecraft:item/template_spawn_egg` issue in this runtime while restoring the normal tinted egg presentation path much closer to original expectations.

---

### 7. Mandrake mob spawn category mismatch - Fixed

`MANDRAKE_MOB` already uses `MobCategory.MONSTER` and is registered in `RegisterSpawnPlacementsEvent`.

### 9. Mandrake harvest always double-rewarded the player - Fixed / verify

`MandrakeBlock` now restores the old branch between "normal crop drop" and "spawn a hostile mandrake" instead of always doing both. The mature crop now checks difficulty and the original day/night weighted chance before summoning the mob.

### 10. Broom forced-hover controls drifted downward too aggressively - Fixed / verify

`BroomEntity` now uses a movement model closer to the 1.7.10 broom: forward input drives acceleration, rider pitch controls lift, owl-familiar strafe support is restored, the mounted broom no longer applies a constant hover-drop, and riding now cancels fall damage again. `BroomItem` also remounts only empty nearby brooms.

### 11. Dream Catcher sleep threshold was hardcoded - Fixed / verify

`DreamCatcherBlockEntity` now uses a config-backed sleep threshold again, defaulting to the old 100% behavior. The wakeup fallback event now only fires Dream Catchers that were already armed during the night instead of bypassing the threshold.

### 12. Somnian/Wispy Cotton was still a crop-style placeholder - Fixed / verify

`CottonBlock` now behaves like a spreading Spirit World bush again instead of a staged farmland crop. `WISPY_COTTON` now uses that same plant behavior for worldgen, nightmare harvesting drops disturbed cotton again, Spirit World plant features no longer inject into the Overworld biome modifier, and Flowing Spirit now cleanses disturbed cotton item entities back into wispy cotton.

### 13. Special plants were still mostly decorative placeholders - Fixed / verify

The special-plant pass has now restored the missing gameplay hooks that mattered most for parity:
- `Blood Rose` stores a fresh player sample again and lets `Taglock Kit` pull it later, while once more refusing to mint a taglock from your own stored sample or an offline player.
- `Ember Moss` burns living entities again, gives the old low fire-moss light/ambience, spreads onto the legacy substrate set, and only drops itself when sheared.
- `Glint Weed` emits the original full-strength light and flame ambience again, and now uses the old capped spreading behavior over legacy substrates instead of staying as a passive decorative block.
- `Spanish Moss` grows as a fast vine instead of a dead decorative cross, no longer acts as a climbable ladder, and now restores the old high fire spread/flammability profile.
- `Grassper` works as a one-item stash, renders its stored item again, and once more keeps the old facing-aware placement/orientation on its asymmetric plant model instead of ignoring block rotation entirely.
- `Glowglobe` emits real light and ambience again, and now matches the old ambient-only block more closely by dropping nothing and returning no pick-block clone stack.
- `Leaping Lily` grants the expected short movement buffs again.
- `Critter Snare` is stateful again: it catches bats, silverfish, small slimes, and small magma cubes, swaps to the matching caught texture, chirps while occupied, and releases the creature on sneak-use.
- `Bramble` and `Void Bramble` now behave as hazards again; Wild Bramble also has its original high hardness/blast resistance back, and Void Bramble now stores its placer again so creative players or the owner can left-click away an upward column despite the restored unbreakable/high-blast-resistance profile.
- `Void Bramble` also blocks ritual activation within the old 32-block null-source radius again, returning the existing black-magic dampening feedback instead of consuming the dropped rite ingredient.

Remaining gap:
- `Critter Snare` does not yet preserve the old special Polynesia Charm bat stock-inventory flag because `Polynesia Charm` is still a simple placeholder item in the port.

These still need in-game verification for feel and visuals, but they are no longer plain placeholder blocks.

### 14. Altar-adjacent artefact parity was still missing edge cases - Fixed / verify

The altar pass is now closer to the original 1.7.10 behavior again:
- Skull bonuses now stack across the altar structure instead of only taking the single best head.
- Missing natural power contributors like Blood Rose, Critter Snare, and Artichoke now count toward altar power.
- Mystic Branch can be placed on the altar again and grants its enhancement bonus.
- Arthana and other placed altar items now invalidate the altar immediately instead of waiting for the next scan window.
- Retrieving or breaking placed altar items no longer risks silently eating the stored item.

The altar is now stricter and closer to original again:
- The multiblock no longer treats arbitrary 6-block flood-filled shapes as valid. It now requires a real rectangular 2x3 altar footprint again.
- The connected/joined blockstate art pass is back too, so a valid altar now selects the original-style joined pieces with the red carpeted top segments instead of staying on the single standalone model.
- The joined corner models now omit the same hidden internal support faces as Resurrected instead of rendering those faces with altar texture coordinates, and block updates refresh the whole possible 2x3 neighborhood so newly completed or broken structures do not keep stale joined states as easily.

This still needs in-game verification for final orientation/art feel, but the remaining gap has moved away from basic altar mechanics and toward broader storage/ritual parity.

### 14a. Wicker Bundle was still a flat/simple block - Fixed / verify

The first Wicker Bundle parity pass is now in place:
- The placed block is a rotated pillar again instead of a directionless simple cube.
- Fire spread and flammability are back at the old `20/20` profile.
- The inventory item now renders from the block model instead of the temporary flat generated side sprite.

Current status:
- Bloodied Wicker Bundle is registered as its own rotated-pillar block/item with Resurrected bloodied textures, self drop, Demonic Blood recipe, and the same old `20/20` fire profile.
- Demonic Blood now also restores the old right-click behavior on plain Wicker Bundle: one item converts up to five plain wicker blocks in the surrounding 3x3x3 cube into bloodied wicker while preserving pillar axis.
- Flint-and-steel on a valid old Witchery bloodied wickerman now checks the full documented 20-bundle shape, can be triggered from any bloodied bundle in the structure instead of only a narrow subset of click positions, replaces the matching structure blocks with fire, and summons a persistent Horned Huntsman boss.
- `/witchery spawn wickerman` now builds that same summon-valid bloodied wickerman pattern five blocks in front of the executing player, rotates the figure based on the player's facing instead of only the coarse axis, and uses mixed pillar axes so the debug statue reads like a hay man instead of a plain vertical stack.
- Horned Huntsman now has first-pass old-Witchery parity for 400 HP, boss bar, fire immunity, invulnerability windup, regen, ranged arrows, melee knock-up, wolf summons, stuck teleporting, capped incoming damage, drops, spawn egg, and renderer.
- Horned Huntsman melee now uses the original random `7 + rand(15)` damage roll again instead of the flat modern `14` attack attribute hit, while preserving the old knock-up and golem-throw cue.
- Horned Huntsman arrows now restore the original ranged damage formula, `distanceFactor * 8 + gaussian(0.25) + difficulty * 0.11`, instead of keeping the weaker vanilla mob-arrow helper damage.
- Horned Huntsman footsteps now use the old iron-golem step cue again instead of the default monster step sound.

Remaining verification:
- Check in game that the Wickerman base/orientation offsets match old Witchery from each clicked side.
- Check Horned Huntsman model pose/scale/texture wrap against Resurrected and tune if needed.

### 15. Leech Chest and Taglock Kit had lost their original storage flow - Fixed / verify

The first real storage-parity pass is now back in place:
- `Leech Chest` is a 27-slot container again instead of a dead stub block entity.
- Opening it stores up to 3 recent unique players again, matching the old "recent victims" behavior more closely.
- `Taglock Kit` can now pull a stored non-self target from `Leech Chest` again and reports the old failure cases more sensibly: only your own taglock, or players no longer online.
- `Taglock Kit` also splits stacked kits correctly again when filling a single one, instead of binding an entire stack at once.
- Breaking and replacing the `Leech Chest` now preserves that stored player queue on the dropped chest item again instead of wiping it.
- `Leech Chest` once again refuses to open when the lid is blocked by a solid block or a sitting cat, matching the old chest-like access rules more closely.
- `Leech Chest` once again exposes real chest-style comparator output and now refreshes that signal correctly as its contents change.
- `Leech Chest` once again behaves like a trapped chest for redstone, outputting open-count signal strength while players are using it and notifying adjacent blocks when that state changes.
- `Leech Chest` once again uses the original compact chest-sized outline/collision footprint instead of behaving like a hidden full cube.

### 16. Poppet Shelf only worked as a local chest - Fixed / verify

The first real Poppet Shelf parity pass is now in place:
- `Poppet Shelf` only accepts poppet items again instead of acting like a generic chest.
- Breaking it now drops the stored poppets properly.
- Bound-poppet lookup now scans every loaded shelf on the current server again instead of only looking in a tiny radius around the player.
- Shelves with bound poppets now force-load their chunk again, so poppet protection can still trigger even when the player is far away.
- `Poppet Shelf` once again exposes real container-strength comparator output, and that redstone signal now refreshes when the stored poppets change.
- `Poppet Shelf` once again uses the original low half-height shelf footprint for outline and collision instead of behaving like a full cube.

### 17. Dream Catcher nightmare handling was too loose - Fixed / verify

The Dream Catcher parity pass is now tighter again:
- Spirit World nightmare and demonic-nightmare chance modifiers now only activate when a nearby Dream Catcher actually has the `Nightmares` weave, matching the old mod more closely instead of treating any catcher as special.
- Wall placement now uses the face the player actually clicked, so the block no longer snaps to a side based on look direction.

Remaining gap:
- The render/item-texture side is still open, and the block-removal cleanup path in this mapping is still worth a later audit.

### 18. Medium-circle Summoning was doing the wrong mechanic entirely - Fixed / verify

The Summoning rite is back much closer to old Witchery now:
- Instead of acting like a one-shot self-buff, it now queues a long-running active ritual again.
- It requires a coven of 3 like the original rite.
- Every 60 ticks it allures up to 2 valid creatures from rotating search quadrants around the circle, restoring the old gradual creature-calling behavior more closely.

Remaining gap:
- The port still uses the simplified modern ingredient-dispatch system instead of the original multi-sacrifice ritual registry, so the activation recipe shape is still not full 1.7.10 parity.

### 19. Raise Column was a one-tick stone stamp instead of a staged earth-lift rite - Fixed / verify

The Raise Column rite is back much closer to old Witchery now:
- Instead of generating a fake stone cylinder in one tick, it now queues as an active ritual and lifts real terrain upward over time.
- It pulses every 5 ticks again, matching the old rite cadence.
- Small, medium, and large circles now use their original escalating base radii again.
- Nearby coven witches now expand the lifted radius again, instead of being ignored.
- The three size variants no longer fire from bare dropped stone alone: they now also once more expect the old `brew_of_sprouting` + cactus + size-specific powder support sets (`gunpowder` on small, `redstone` on medium, `glowstone dust` on large) before the staged lift will queue.

Remaining gap:
- The original 1.7.10 rite moved tile entities as part of the terrain lift, and the old small-circle optional bound-waystone extra is still deferred; the port still skips block-entity columns for safety until that migration logic is restored cleanly.

### 20. Crater was an instant sphere delete instead of a staged excavation rite - Fixed / verify

The Crater rite is back much closer to old Witchery now:
- Instead of deleting a full sphere in one tick, it now queues as an active large-circle ritual again.
- It pulses every 10 ticks and deepens over several stages, matching the old crater-opening cadence more closely.
- The carve now follows a layered downward crater shape instead of flattening everything inside a sphere immediately.

Remaining gap:
- The original rite had more exact legacy terrain-drop rules, so the port still uses a conservative modern approximation for which removed blocks drop items.

### 21. Sanctity and Imprisonment were one-shot effects and on the wrong ring size - Fixed / verify

The Sanctity and Imprisonment rites are back much closer to old Witchery now:
- Instead of immediately teleporting mobs and ending, they now queue as sustained active circles again.
- Both now require a nearby working altar to keep running and consume altar power over time.
- Sanctity now behaves like a repulsive ward, while Imprisonment now keeps nearby creatures penned toward the circle instead of acting as a one-tick trap.
- Both now also queue from the original small ritual circle again instead of the wrong medium ring.

Remaining gap:
- The original mod split the full protection family across several different sacrifice shapes. The modern dispatcher is closer now on small-vs-medium ring size, and the portable barrier now keys off `Charged Attuned Stone` again instead of plain `Attuned Stone`, but the family still uses representative trigger items rather than the full original multi-sacrifice matcher.

### 22. Dream Catcher and Brazier still used dead legacy break hooks - Fixed / verify

The destroy-path cleanup is now back in place:
- Breaking or support-dropping a `Dream Catcher` now returns its stored weave again instead of risking a silent loss because the old removal hook no longer fired in this runtime.
- Breaking an unlit `Brazier` now drops its stored ingredients again instead of silently eating them for the same reason.

Remaining gap:
- This restores the practical break/drop mechanic, but the remaining work on both blocks has shifted back to visuals and broader in-game verification instead of dead removal hooks.

### 23. Moving Earth was still the wrong ritual entirely - Fixed / verify

The Moving Earth rite is back much closer to old Witchery now:
- Instead of lifting a square chunk of land upward in one tick, it now queues as a staged active ritual again.
- It follows a wandering earth-parting path over time, matching the old Part Earth behavior much more closely than the old placeholder did.
- Coven support now affects the carved width again instead of being ignored.

Remaining gap:
- The modern port still chooses this rite through simplified ingredient dispatch instead of the original Brew of Erosion sacrifice registry, so the ritual's activation mapping is still not full 1.7.10 parity even though the ongoing terrain behavior is much closer.

### 24. Refilling Chest used the wrong refill timer, loot source, and render base - Fixed / verify

The Refilling Chest is back much closer to old Witchery now:
- Instead of starting a fresh one-hour cooldown only after the chest is emptied, it now uses the original lifetime tick cadence again and refills on the hourly boundary while empty.
- It now saves that lifetime counter directly, matching the old `WITCLifeTicks` behavior more closely and avoiding the port's extra `Filled`/cooldown bookkeeping.
- The refill source now pulls from a small dungeon-style chest table again instead of the custom Torment-only loot table, and only inserts a small 2-5 stack refill like the original.
- The placed block now uses the real vanilla single-chest path (`ChestBlock`, `ChestBlockEntity`, and vanilla chest lid renderer) instead of the older custom Witchery refilling-chest renderer, while keeping the Witchery container name and Torment-only refill mechanic.
- The item still renders through the vanilla chest graphic path, and break particles now have a valid oak-plank fallback instead of the old pink/black missing-texture path.

Remaining gap:
- This restores the practical refill mechanic and core render base, but the remaining work on the block is now visual polish and in-game verification rather than core storage behavior.

### 25. Protection-family ring sizes were still shifted upward - Fixed / verify

The protection barrier family is now back much closer to old Witchery:
- The broader `Rite of Protection` barrier no longer resolves as a modern potion buff on the caster.
- It now queues as a sustained active rite again, once more asks for its startup glowstone-dust support offering plus 1000 altar power, redraws an invisible barrier dome around the circle on a timed pulse, and now does so from the original medium ritual ring instead of the wrong large route.
- The barrier itself now has real timed block/entity behavior again instead of being a dead placeholder block.
- The smaller monster-only barrier variant now routes from the original small ritual ring instead of medium and once again asks for redstone plus its startup altar cost before sustaining.
- The timed portable barrier variant is still reachable on its original medium circle, now expects `Charged Attuned Stone` again instead of collapsing onto plain `Attuned Stone`, and once more asks for its obsidian support offering.
- Sanctity and Imprisonment now also once more ask for redstone plus their original 500 altar-power startup cost instead of activating as effectively free wards once an altar merely exists nearby.

Remaining gap:
- This restores the practical barrier mechanics, startup altar costs, and most of the ring-size split, and the old charged-attuned-stone distinction is now back for the portable route, but the activation mapping is still modernized rather than matching the original multi-sacrifice recipes exactly and the optional bound-waystone relocation behavior is still deferred.

### 26. Flowing Spirit world-block effects were still mostly missing - Fixed / verify

The Spirit World fluid block is back much closer to old Witchery now:
- `Flowing Spirit` still cleanses disturbed cotton back into wispy cotton, but now also applies the original living-entity split again.
- Ordinary living creatures touching it now receive short `Regeneration II`.
- Undead, demonic creatures, and `Nightmare` now receive `Weakness II` instead of the beneficial effect.

Remaining gap:
- The broader Spirit Portal flow is still only partially restored in the modern port, so this fixes the fluid's creature interaction without claiming full Spirit World portal parity yet.

### 27. Nightmare death was bypassing the old delayed respawn loop - Fixed / verify

The Spirit World nightmare pressure is back much closer to old Witchery now:
- Killing a `Nightmare` no longer competes with a second instant-respawn event path in parallel with the existing cooldown loop.
- Replacement pressure now comes back through the player-bound delayed respawn scan again instead of a separate 60% death proc.
- Kill cooldown bookkeeping now only credits the nightmare's actual bound victim, matching the old personal-hunter behavior more closely.

Remaining gap:
- The broader Spirit Portal / corpse-manifestation side of Spirit World parity is still incomplete, so this fixes nightmare lifecycle pressure without claiming full dream-state parity yet.

### 28. Demonic nightmare events and sleeping-corpse durability were too simplified - Fixed / verify

The next Spirit World parity pass is now in place:
- Demonic nightmare pressure now follows the old single-roll event split again instead of stacking several fireball and demon procs in the same 100-tick pulse.
- The two fireball variants now fall from high overhead again, much closer to the original Dream World harassment pattern.
- The sleeping `Corpse` now uses a full 20-health body again and spawns with the player's current health instead of a flat fragile placeholder value.
- Offline corpses no longer accept ordinary damage, matching the old "owner must still exist unless a creative player breaks it" rule more closely.

Remaining gap:
- Spirit Portal / ghost-manifestation parity is still deferred, so this restores practical corpse and demonic-nightmare behavior without claiming the full manifestation system.

### 29. Protection-family ritual upkeep was still using coarse rounded drain - Fixed / verify

The ritual lifecycle is now closer to old Witchery on the protection family:
- The large barrier dome now drains altar power at the old relative `1.4` upkeep rate again instead of the port's rounded `2`.
- The medium monster-only barrier now uses the old `1.2` upkeep rate again.
- Sanctity and Imprisonment now use the old `0.8` upkeep rate again instead of draining a full point every pulse.
- Sanctity, Imprisonment, the monster-only barrier, and the broader dome now also restore their missing startup altar costs, so these wards are no longer effectively free to begin before the upkeep loop takes over.
- The port now tracks fractional upkeep debt across pulses, so these rites preserve the old relative costs without forcing the altar system back to floating-point storage.

Remaining gap:
- This restores the protection-family upkeep ratios, but the broader active-ritual queue and refund lifecycle is still only partially recreated compared with the original tile-entity ritual engine.

### 30. Several curse rites were missing their original weather/time gates - Fixed / verify

The next ritual precondition pass is now back in place:
- `Curse Creature`, `Curse of Insanity`, `Curse of Sinking`, `Curse of Overheating`, and `Curse of Waking Nightmare` now require an actual thunderstorm again before they will cast, matching the old `ONLY_IN_STROM` recipe traits.
- `Curse Creature` now also once more expects its original `Taglock Kit`, `Exhale of the Horned One`, `fermented spider eye`, `gunpowder`, and `Brew of Grotesque` support offerings plus its original 2000 altar-power startup cost, instead of firing for free once the storm check passes.
- `Curse of Hell on Earth` now requires the Overworld at night again instead of firing in any dimension at any time.
- These checks now happen before taglock consumption, so failed preconditions no longer waste the curse item flow.

Remaining gap:
- This restores some of the missing activation traits, but the ingredient-dispatch ritual system still does not model the full original recipe registry and its trait matrix yet.

### 31. Total Eclipse and broom infusion were missing original day/night gates - Fixed / verify

The next ritual trait pass is now back in place:
- `Rite of Total Eclipse` now requires daytime again before it will fire, matching the old `ONLY_AT_DAY` recipe trait instead of allowing free midnight shifts at any time.
- The broom infusion rite triggered by `Flying Ointment` now requires nighttime again, once more expects the original medium-plus-small ritual ring pair, and now also once more consumes its original 3000 altar-power startup cost instead of enchanting brooms for free.
- These checks now fail early with the same snare/message style as the other restored ritual preconditions.

Remaining gap:
- More original recipe-trait coverage is still missing outside these rites, especially where the modern ingredient dispatcher has collapsed multiple legacy ritual recipes together.

### 32a. Mystic Branch infusion rite was still missing from the medium ritual dispatcher - Fixed / verify

The old `infusiontree` path is back in practical form:
- `Ent Twig` on the medium ritual circle now once again routes into the original night-only paired ritual-circle infusion instead of doing nothing.
- The port now also once more expects `Mystic Unguent` and 3000 altar power before the rite completes.
- Success now drops a fresh `Mystic Branch` item at the circle center instead of leaving the old item obtainable only through non-ritual shortcuts.

### 32b. Crystal Ball infusion rite was still missing from the medium ritual dispatcher - Fixed / verify

The old `infusionfuture` path is back in practical form:
- `Quartz Sphere` on the medium ritual circle now once again routes into the original night-only Crystal Ball infusion instead of doing nothing.
- The port now also once more expects a gold ingot, `Happenstance Oil`, and 2000 altar power before the rite completes.
- Success now drops a fresh `Crystal Ball` item at the circle center instead of leaving the old rite absent from the dispatcher.

### 32. Curse of Insanity was unreachable because a non-original medium placeholder route claimed the same ingredient first - Fixed / verify

The next ritual-dispatch pass is now back in place:
- `FERMENTED_SPIDER_EYE` no longer gets intercepted by the old non-original medium placeholder branch before the curse route runs.
- The port no longer claims a fake `Silent Step` medium rite on `SPIDER_EYE` either; that placeholder path is gone because it does not exist in the original Witchery 1.7.10 rite registry or language data.
- `Curse of Insanity` can therefore keep its infernal medium-circle route clear without the modern dispatcher inventing an extra Witchery rite that never existed.

Remaining gap:
- This fixes one concrete ingredient collision, but the broader risk from the simplified single-item dispatcher is still present until more of the original multi-sacrifice ritual mapping is restored or separated more safely.

### 33. Large-circle Protection was incorrectly the fallback for unrelated ingredients - Fixed / verify

The large-circle dispatcher is now stricter and closer to original Witchery:
- `Rite of Protection` no longer triggers as the generic fallback for any unrecognised large-circle ingredient.
- The simplified dispatcher now only maps large Protection from `obsidian` or `glowstone dust`, which is much closer to the original `obsidian + glowstone dust` sacrifice pair than the old permissive fallback.

Remaining gap:
- This removes a real false-positive activation path, but the large-circle dispatcher is still a modern single-item approximation rather than the original multi-sacrifice registry.

### 34. Medium-circle Summoning was incorrectly the fallback for unrelated ingredients - Fixed / verify

The medium-circle dispatcher is now stricter and closer to original Witchery:
- `Rite of Summoning` no longer triggers as the generic fallback for arbitrary unrecognised medium-circle ingredients.
- The simplified dispatcher now maps Summoning from `milk bucket` as a deliberate stand-in for the original multi-sacrifice animal-calling recipe, instead of treating any unknown item as a valid summon trigger.

Remaining gap:
- This removes a major false-positive activation path, but Summoning is still routed through a single representative item instead of the full original sacrifice set.

### 35. Small-circle Blessing was incorrectly the fallback for unrelated ingredients - Fixed / verify

The small-circle dispatcher is now stricter and closer to original Witchery:
- `Blessing` no longer fires for arbitrary unrecognised small-circle ingredients.
- The simple blessing effect is now limited to an actually empty small-circle activation instead of acting as a catch-all for wrong items, which is much closer to the original explicit recipe-matching model.

Remaining gap:
- The port still keeps a lightweight empty-circle blessing shortcut, so this removes the false-positive item path without claiming full original ritual-registry matching yet.

### 36. Day/night ritual gates used overly loose manual time windows - Fixed / verify

The ritual precondition layer is now tighter again:
- `requireDay` and `requireNight` now use narrower explicit time windows that line up more closely with the original `isDaytime`-style matching instead of the port's looser approximation.
- This brings rites like `Total Eclipse`, broom infusion, and the Overworld-night curse checks closer to original behavior around dawn and dusk.

Remaining gap:
- This fixes the shared time gate helper, but more ritual-trait and dispatcher parity work is still open elsewhere in the ritual engine.

### 37. Rain of Toads routing and taglock consumption were still wrong - Fixed / verify

The next ritual lifecycle pass is now closer to original Witchery:
- `Rain of Toads` now routes from the medium-circle dispatcher into the staged coven weather rite again instead of incorrectly firing a non-original taglock curse path.
- Taglock-based curse rites and the remaining medium otherwhere taglock-transposition route now validate their filled taglock target before succeeding, so invalid or offline targets no longer waste the sacrifice.
- The non-original large filled-taglock `Transposition Pull` fallback is gone again; the original teleportation family is back to the real small bound-waystone path plus the medium otherwhere `RiteTeleportEntity` path instead of keeping that false-positive large-circle branch alive.

Remaining gap:
- The ritual dispatcher is still a representative single-item shortcut for many rites, so more sacrifice-mapping cleanup is still needed even though this removes one of the larger false-role activations and one of the more obvious refund errors.

### 37a. Weather Call Storm still ignored its original support offering and startup power - Fixed / verify

The small ritual storm path is now closer to original Witchery:
- `Weather Call Storm` no longer starts from a bare snowball or water bucket alone.
- It now also expects the old `wood_ash` support offering again and once more consumes its original 1000 altar power before the staged storm queue begins.

Remaining gap:
- The old optional bound-waystone variant and the larger storm-family sibling are still deferred, so this restores the base rite's missing support/power gate without claiming the full storm family is recipe-perfect yet.

### 38. Chalk variant parity was still mostly ignored by the ritual dispatcher - Fixed / verify

The next ritual activation pass is now closer to original Witchery:
- `Curse of Blindness` now routes through the small infernal-circle path again instead of firing from the medium generic dispatcher.
- The infernal curse cluster now only activates from infernal chalk where the original single-circle recipes were infernal: `Rain of Toads`, `Curse Creature`, `Blight`, `Sinking`, `Overheating`, and `Insanity`.
- The ritual protection family now again expects ritual chalk instead of accepting any chalk variant for `Protection`, `Sanctity`, `Imprisonment`, and the medium barrier variants.
- `Total Eclipse`, `Weather Call Storm`, and infernal `Cook Food` now also require the original chalk family instead of activating from any ring variant.

Remaining gap:
- This restores the first real glyph-family gate, but the modern dispatcher still does not model the original full multi-circle and multi-sacrifice registry, especially for otherwhere rites and the mixed-circle large rituals like `Hell on Earth`.

### 39. Active ritual sustain and otherwhere glyph-family checks were still too loose - Fixed / verify

The next ritual integrity pass is now closer to original Witchery:
- Active rites now verify that their own required ring size still exists instead of re-running the generic first-match detector, so staged rites no longer abort just because a smaller ring at the same center is found first.
- Large-circle `Curse of Waking Nightmare` and `Blight` now require infernal chalk again, and Fetish binding now requires ritual chalk again, instead of all three accepting the wrong family.
- Small-circle teleport-to-bound-waystone now requires otherwhere chalk again instead of activating from any small ring.
- New casts now retry every present ring from large to small instead of collapsing through the first detected ring size only, so mixed-ring activations no longer dead-end just because a smaller inner circle is present.

Remaining gap:
- This fixes sustain-time validation, one more slice of glyph-family parity, and the first major mixed-ring activation failure, but the port still uses an ordered size retry plus representative trigger items instead of the original full multi-circle, multi-sacrifice ritual registry.

### 40. Small-circle waystone/manifest routes were still off - Fixed / verify

The next small-circle parity pass is now closer to original Witchery:
- `Bind Waystone` now requires ritual chalk again instead of accepting any small ring, and it once more consumes its original Ender Dew plus glowstone dust supports before spitting out a freshly bound waystone item at the ritual center itself instead of mutating an inventory stack or binding to the caster's current position.
- `Bind Waystone Copy` is now reachable again from the dropped bound-waystone ritual route, and it once more consumes one extra offered unbound waystone plus its original Ender Dew and diamond supports to produce two bound waystones instead of firing the false Banish Demon branch.
- `Manifestation` is now actually reachable again through the simplified dispatcher, mapped to `spectral_dust` on an otherwhere small circle instead of remaining implemented-but-dead.

Remaining gap:
- These routes are now reachable and family-gated, but the dispatcher is still using representative single items rather than the original full multi-sacrifice recipe sets, so broader small-circle recipe-shape parity remains open even though the waystone-copy support offerings are back.

### 41. Forestation and animal-calling were routed through the wrong ring sizes - Fixed / verify

The next ritual routing pass is now closer to original Witchery:
- `Forestation` now routes from the medium ritual circle again instead of incorrectly living on the large-circle dispatcher.
- The animal-calling `Beastial Call` behavior no longer fires from an arbitrary small ring; it now routes through a large otherwhere circle instead, which is much closer to the original `Call Beasts` rite.
- That large otherwhere animal-calling route no longer resolves as a one-shot mass teleport either; it now queues the staged `Call Creatures`-style coven rite again and allures creatures in 60-tick pulses much closer to 1.7.10.

Remaining gap:
- Both routes are still using representative single-item triggers inside the simplified dispatcher instead of the full original sacrifice sets, but their ring size and chalk family are now much closer to original parity.

### 42. Familiar and reincarnation rites were still on the wrong circles - Fixed / verify

The next ritual routing pass is now closer to original Witchery:
- `Prior Incarnation` now requires an infernal small circle again instead of activating from any small ring.
- Familiar binding and familiar calling no longer live on the small-circle dispatcher; they now route through the medium ritual circle instead, which is much closer to the original `bindfamiliar` and `callfamiliar` rites.
- That shared medium `whiff_of_magic` path now also restores the original familiar support gates and altar costs again: binding once more asks for Tear of the Goddess, Odour of Purity, a diamond, Demonic Blood, and 8000 altar power, while calling once more asks for Breath of the Goddess, Hint of Rebirth, and 1000 altar power.
- The original small ritual `summonfamiliar` path is back too through a representative `drop_of_luck` trigger, and it once again summons a bound Witch's Cat into the circle instead of leaving the small ritual familiar branch missing entirely.

Remaining gap:
- These rites are still triggered through simplified representative ingredients rather than the full original sacrifice sets, but their ring size, chalk family, support offerings, and altar-power costs now line up much more closely with 1.7.10.

### 43. Blindness and Blight were using the wrong mechanic entirely - Fixed / verify

The next infernal-rite correction is now closer to original Witchery:
- Small infernal `curseblindness` now runs the area `Rite of Blindness` again instead of incorrectly behaving like a taglock curse on one player.
- Medium infernal `curseblight` now runs the area/world `Rite of Blight` again instead of incorrectly behaving like a single-target curse.
- Both rites now also restore their original support-sacrifice gates: Blindness once again expects `Charged Attuned Stone`, `Redstone Soup`, `Reek of Misfortune`, `Exhale of the Horned One`, `Brew of Ink`, `poisonous potato`, `spider eye`, and `diamond`, while Blight once again expects `Charged Attuned Stone`, `Redstone Soup`, `Reek of Misfortune`, `fermented spider eye`, `glistering melon slice`, `rotten flesh`, and `diamond`.
- The false large-circle blight activation path was removed, so the world-blight effect no longer appears as a non-original extra large infernal rite.

Remaining gap:
- These infernal rites are now much closer mechanically, but they still use simplified representative trigger items on top of those restored support offerings instead of the full original sacrifice registry.

### 44. Small infernal witch summoning was missing entirely - Fixed / verify

The next missing-rite pass is now closer to original Witchery:
- `summonwitch` is reachable again as a small infernal rite through `diamond_vapour`, instead of being absent from the modern dispatcher.
- The restored route now spawns a hostile witch at the circle center, uses a proper summoning-space obstruction check instead of blindly spawning inside blocked space, and once more expects `Diamond Vapour`, `Exhale of the Horned One`, `magma cream`, `Arthana`, and `fermented spider eye` plus its original 2000 altar-power startup cost.

Remaining gap:
- The dispatcher is still using one representative original ingredient to select the rite instead of the full original sacrifice registry, but the shared summoning-space obstruction check now covers the restored witch/imp/demon/wither/death/Lord of Torment family instead of only the smallest infernal path.

### 45. Large infernal imp summoning was missing entirely - Fixed / verify

The next missing-rite pass is now closer to original Witchery:
- `summonimp` is reachable again as a large infernal rite through `refined_evil`, instead of being absent from the modern dispatcher.
- The restored route now brings back the original coven dependency for imp summoning, reuses the restored summoning-space obstruction check instead of blindly spawning into blocked circles, and once more expects `Refined Evil`, the port's `Demonic Blood` item as the old `Infernal Blood` equivalent, an `ender pearl`, an `Attuned Stone`, and its original 5000 altar-power startup cost.

Remaining gap:
- This is still routed through one representative ingredient instead of the full original summon registry, but the old `Infernal Blood` support slot is now materially covered through the port's `Demonic Blood` item rather than left missing.

### 46. Large infernal demon summoning was missing entirely - Fixed / verify

The next missing-rite pass is now closer to original Witchery:
- `summondemon` is reachable again as a large infernal rite through `tormented_twine`, instead of being absent from the modern dispatcher.
- The restored route now summons the Witchery demon entity at the circle center, reuses the restored summoning-space obstruction check instead of blindly spawning into blocked circles, and once more expects `Refined Evil`, `blaze powder`, an `ender pearl`, a villager standing in the circle, and its original 3000 altar-power startup cost.
- The old expensive no-villager demon sibling is now modeled too: the same route will also accept `Refined Evil`, a `blaze rod`, an `ender pearl`, `Attuned Stone`, and `Charged Attuned Stone` as the alternate support set instead of wrongly insisting on the villager path only.

Remaining gap:
- This is still routed through one representative ingredient instead of the full original summon registry, but both original support-set branches now exist behind that modern trigger.

### 47. Paired infernal wither summoning was still collapsed onto the wrong single ring - Fixed / verify

The next missing-rite pass is now closer to original Witchery:
- `summonwither` is still reachable through the representative `wither_skeleton_skull` route, but it now does so from the original large infernal circle and again expects the inner medium infernal companion ring instead of collapsing onto a lone medium infernal circle.
- The restored route now spawns the Wither at the circle center, reuses the shared summoning-space obstruction check instead of blindly spawning inside blocked circles, and once more expects `Diamond Vapour`, an `ender pearl`, a villager standing in the circle, and its original 4000 altar-power startup cost.
- The old expensive no-villager wither sibling is now modeled too: the same route will also accept a `diamond`, `ender pearl`, `Attuned Stone`, and `Charged Attuned Stone` as the alternate support set instead of wrongly requiring the villager branch only.

Remaining gap:
- This still uses one representative trigger instead of the original full multi-recipe summon registry, but both original support-set branches now exist behind that modern trigger.

### 48. Small Ice Shell rite was missing entirely - Fixed / verify

The next missing-rite pass is now closer to original Witchery:
- `iceshell` is reachable again as a small staged rite through a representative `frozen_heart` trigger, instead of being absent from the modern dispatcher.
- The restored route again requires a coven of at least 2, waits through the old startup beat, and grows a hollow ice sphere in pulses with coven-based radius scaling instead of acting like a missing or one-shot placeholder.

Remaining gap:
- This still uses one representative trigger instead of the full original sacrifice set, and the original sphere effect replaced a slightly broader set of soft blocks than the current port helper does.

### 49. Glyphic Transformation was missing entirely - Fixed / verify

The next missing-rite pass is now closer to original Witchery:
- `glyphictransform` is reachable again by dropping ritual, otherwhere, or infernal chalk onto an existing circle setup, instead of being absent from the port entirely.
- The restored route now counts nearby stacks of the triggering chalk family to choose small/medium/large ring size again and rewrites only the matching ring marks, which is much closer to the original “1 = small, 2 = medium, 3 = large” ritual behavior.

Remaining gap:
- The port now uses the nearest triggering chalk item as the deterministic family selector instead of the old item-iteration-order quirk, and it still rewrites the ring through the modern simplified ritual entrypoint rather than the original separate rite registry.

### 50. Rite of Necromancy was missing entirely - Fixed / verify

The next missing-rite pass is now closer to original Witchery:
- `spectralstone` is reachable again as a small night-only ritual through a representative ritual `spectral_dust` route, instead of being absent from the modern dispatcher.
- The restored route now produces a real Spectral Stone item again rather than leaving the feature family dead, the ported item carries explicit binding data + tooltip state instead of staying a plain anonymous item, and the old hold-to-release summon behavior is back so a charged stone can actually be used again.
- It now also once more expects its old `Necro Stone`, `Congealed Spirit`, `Condensed Fear`, and `Boline` support set around the representative `spectral_dust` trigger, and once more consumes its original 6000 altar-power startup cost before the stone condenses.

Remaining gap:
- This still uses one representative trigger instead of the original multi-sacrifice recipe, and the modern item no longer uses the old metadata/icon split even though the release mechanic itself is back.

### 51. Rite of Binding for Spectral Stone was missing entirely - Fixed / verify

The next missing-rite pass is now closer to original Witchery:
- `bindspectral` is reachable again as a medium ritual-circle route from an empty Spectral Stone, instead of being absent from the port entirely.
- The restored route now looks for nearby spectral creatures, chooses the nearest creature family, binds up to three of that same type into the stone, removes those entities from the world, stores the result in modern item data so the item is no longer mechanically blank, and the released spirits now remember their summoner well enough to avoid immediately targeting them.
- The route now also uses the original medium ring again instead of the wrong large-circle dispatcher slot.
- It now also once more expects its old `Spectral Dust` plus `Boline` support offerings and once more consumes its original 5000 altar-power startup cost before the spirits are actually bound.

Remaining gap:
- The port now uses deterministic nearest-type selection instead of the old entity-iteration-order quirk, and the released spirits still use a lighter modern owner-safe filter instead of the full old summoned-undead enslaver system.

### 52. Rite of Earth's Wrath was missing entirely - Fixed / verify

The next missing-rite pass is now closer to original Witchery:
- `volcano` is reachable again as a staged large-circle terrain rite through a representative `lava_bucket` route, instead of being absent from the port entirely.
- The restored route now checks for a real lava pool beneath the circle before activation, raises the sand/sandstone cone in pulses instead of resolving as a one-shot terrain edit, and opens the center vent at the end of the eruption sequence.

Remaining gap:
- This still uses one representative trigger instead of the original multi-sacrifice set, and the modern terrain helper is a little cleaner and more deterministic than the original block-protection aware sand placement loop.

### 53. Rite of Shifting Seasons was missing entirely - Fixed / verify

The next missing-rite pass is now closer to original Witchery:
- `climatechange` is reachable again as a staged large-circle biome rite through a representative ritual `attuned_stone` route, instead of being absent from the port entirely.
- The restored route now scans nearby dropped biome foci plus glowstone dust, stages over 5 pulses, requires a coven of 4, and scales the affected radius by coven size again.
- The biome rewrite now uses the modern 1.21 biome grid with an explicit chunk resend, and desert/frozen outcomes restore the old weather side effects.

Remaining gap:
- This now uses deterministic nearest-focus selection and representative modern biome mappings instead of the original last-item-wins item-iteration quirk and old biome-dictionary contents.

### 54. Spirit Portal activation was still deferred - Fixed / verify

The next Spirit World parity pass is now closer to original Witchery:
- `spirit_portal` is no longer a dead decorative block. It now validates the old 2x2 interior inside a 4x4 snow-block frame again and can be ignited by placing Flowing Spirit into the frame while inside the Spirit World.
- Flowing Spirit now restores the original "portal source" behavior closely enough that the portal interior replaces the fluid source blocks when the frame is valid, and portal blocks break back out if the snow frame is disturbed.

Remaining gap:
- This restores the original portal activation/lifecycle, but the actual Rite of Manifestation ghost-travel behavior that uses the portal is still deferred as a separate player-state feature.

### 55. Rite of Manifestation ghost travel was still deferred - Fixed / verify

The next Spirit World parity pass is now closer to original Witchery:
- The small otherwhere `Manifestation` route no longer throws the caster into the Spirit World. It now restores the real old job: granting a Spirit World walker 150 seconds of manifestation time plus 25 seconds per nearby coven witch.
- Entering a Spirit Portal in the Spirit World now spends that stored time to manifest the same player into the Overworld as a ghost-like traveler while keeping the Spirit World session active in the background.
- The old timed return loop is back: manifestation time now drops by 5 every 100 ticks outside the Spirit World, warns again at 60/30/15 seconds, and returns the player to their Spirit World anchor when the timer expires.
- Icy Needle now matches the old ghost-state behavior again by sending a manifested player back to the Spirit World instead of waking them outright, and manifested death now safely snaps the player back to the Spirit World instead of leaving the Spirit World state corrupted.

Remaining gap:
- The practical mechanics are back, but ghost rendering/style parity is still open because the modern port still lacks the old dedicated ghost visual/player-style layer.

### 56. Nightmare combat was still too strong and missed spear weakening - Fixed / verify

The next Spirit World combat pass is now closer to original Witchery:
- `Nightmare` no longer hits with full Spirit World damage while manifested in the Overworld. Outside the Spirit World it now drops back to the old weak `0.5` damage profile instead of using the full monster attribute.
- Taking damage from a wielder using `Huntsman's Spear` now shortens the Nightmare defense window again, restoring the old `40/30` tick weakened defense timing instead of always using the full `80/40` timing.
- The incorrect modern self-heal side effect on Nightmare hits outside the Spirit World is gone.

Remaining gap:
- In-game verification is still open for the overall Nightmare fight feel and ghost-state presentation, but the missing anti-strip charm item is no longer a blocker.

### 57. Curse of Corrupt Poppet was missing entirely - Fixed / verify

The next missing-feature pass is now closer to original Witchery:
- The `corruptvoodooprotection` rite is reachable again through a representative medium infernal `voodoo_protection_poppet` route instead of being absent from the port.
- It now requires an active cat familiar again as the curse-mastery gate before it will fire.
- It now also restores the original missing support gate and altar startup cost more closely: the representative Voodoo Protection Poppet trigger now also expects Exhale of the Horned One, blaze powder, and Spectral Dust nearby, and the rite once again consumes 7000 altar power before it starts unravelling wards.
- It now validates a filled taglock target, consumes that taglock only after target resolution, and destroys up to 10 bound `Voodoo Protection Poppet` items on the target again.
- `Poppet Protection` now blocks this rite again and throws the backlash lightning onto the caster instead of letting the anti-voodoo poppets be destroyed.

Remaining gap:
- The modern dispatcher still collapses the original five-sacrifice recipe into a representative infernal trigger item plus inventory taglock path, so the exact multi-sacrifice activation shape is still not full 1.7.10 recipe-registry parity.

### 58. Banish Demon rites were missing entirely - Fixed / verify

The next missing-feature pass is now closer to original Witchery:
- The small `banishdemon` and `banishdemonportable` family is reachable again instead of being absent from the port.
- The restored rite now banishes the port's infernal creature equivalents within the original 9-block radius: `Demon`, `Flame Imp`, `Death`, and `Lord of Torment`.
- The portable half now keys off `Charged Attuned Stone` again like the original family, while the non-portable fallback still uses a representative plain `Attuned Stone` route but now also restores the original 2000 altar-power cost instead of acting like a free portable cast.

Remaining gap:
- The exact portable-vs-regular multi-sacrifice activation shape is still collapsed into representative trigger items, so this restores the split charged-vs-altar feel without claiming full 1.7.10 recipe-registry parity.

### 59. Transpose Ores was missing entirely - Fixed / verify

The next missing-feature pass is now closer to original Witchery:
- The old large otherwhere `teleportironore` behavior is reachable again instead of being absent from the port.
- The restored rite now pulses every 10 ticks, scans descending underground layers in an 8-block radius, removes matching ore blocks, and throws them upward as collectible ore-block items near the circle again.
- Full-coven behavior is back in practical form too: ordinary casts transpose iron ore, while covens of 6 also pull gold ore like the original.
- It no longer fires from `diamond_vapour` alone: the port now also expects the old ender-pearl, iron-ingot, blaze-powder, and `Charged Attuned Stone` support set around the circle before the staged rite will queue.

Remaining gap:
- The exact multi-sacrifice activation shape is still partially collapsed because `diamond_vapour` remains the direct dropped trigger instead of one equal member of the full sacrifice registry, so this restores the missing support-offering gate without claiming full 1.7.10 recipe-registry parity.

### 60. Nature's Power was missing entirely - Fixed / verify

The next missing-feature pass is now closer to original Witchery:
- The old medium ritual `naturespower` behavior is reachable again instead of being absent from the port.
- The restored rite now runs as a long staged growth effect, picks random nearby ground anchors, greens the soil, spreads plants, bonemeals growth, scales its working radius and lifetime with nearby coven witches again, and now sustains against the same medium ritual ring it activates from instead of mistakenly requiring a large ring after queueing.
- It no longer fires from `brew_of_sprouting` alone: the port now also expects the original mixed sapling offering set plus a `Charged Attuned Stone` alongside the brew before the staged rite will queue.

Remaining gap:
- The exact old plant palette and fake-player bonemeal edge behavior are still approximated through modern block growth helpers, and the Witchery-sapling half currently uses the port's existing `rowan_sapling` / `alder_sapling` / `hawthorn_sapling` item forms rather than full old metadata-bearing block saplings, so this restores the rite's world-shaping job and its missing support-offering gate without claiming exact 1.7.10 block-for-block growth parity.

### 61. Small otherwhere waystone transposition was doing the wrong job - Fixed / verify

The next mechanical parity pass is now closer to original Witchery:
- The small otherwhere bound-waystone rite no longer ignores the offered waystone and teleports only the caster from inventory.
- It now reads the sacrificed bound waystone itself again and teleports nearby entities within the circle's original short radius, which is much closer to the old `RiteTeleportToWaystone` behavior.
- That nearby-entity sweep now also measures from the actual center of the circle with the old radius-3 feel again, instead of using a slightly tighter corner-based approximation.
- Cross-dimension travel now applies to non-player entities as well, instead of only the player path being handled.

Remaining gap:
- The still-loaded sacrificial item entity is deliberately skipped by the port's modern world-scan to avoid dragging the offered waystone itself around before the post-cast consume step, so the runtime shape is slightly modernized even though the practical rite behavior is back closer to 1.7.10.

### 62. Medium otherwhere taglock transposition was missing / wrong - Fixed / verify

The next mechanical parity pass is now closer to original Witchery:
- The medium otherwhere taglock rite no longer collapses into the large-circle pull mechanic or depends on an inventory search for a separate taglock.
- It now reads the offered filled taglock directly again and teleports the bound target into the circle center, which is much closer to the old `RiteTeleportEntity` behavior.
- Spirit World crossovers are blocked again, and Witch Hunter dampening plus Voodoo Protection poppets still get a chance to stop the cast before the target is moved.

Remaining gap:
- The exact old multi-sacrifice recipe and full multi-circle matcher still sit behind the port's simplified dispatcher, so this restores the rite's real job without claiming byte-for-byte activation parity yet.

### 63. Mixed-ring ritual activation still dead-ended on the first detected circle - Fixed / verify

The next ritual-dispatch pass is now closer to original Witchery:
- New casts no longer stop at the first complete ring detected at a center.
- The activation layer now gathers every complete ring present and retries from large to small, which restores the first real slice of mixed-ring behavior and stops inner circles from masking valid outer rites.

Remaining gap:
- This still uses ordered size retry plus representative trigger items instead of the original full multi-circle, multi-sacrifice ritual registry, so dispatcher fidelity is improved but not complete.

### 64. Machine GUI overlays and storage renders were still misleading - Fixed / verify

The next graphics-parity pass is now closer to original Witchery:
- Distillery, Witch's Oven, and Spinning Wheel now use resurrected-style GUI layout alignment again through the current menu/screen path, instead of the old mixed original/resurrected overlay positions.
- Refilling Chest now uses the real vanilla chest block/lid render path like original Witchery again, keeps the Witchery refill mechanic/name on top of that base, and has a valid oak-plank particle texture so mining it no longer falls back pink/black.
- Poppet Shelf top-item rendering is back on a tighter visible 3x3 grid instead of the looser oversized placement that was easy to miss or clip visually.

Remaining gap:
- These fixes still need in-game verification against the latest client build, and the remaining art mismatches are now narrower item/block polish issues tracked in `ITEM_REVIEW.md`.

### 65. Familiar rites still spawned pets instead of binding/calling like the original - Fixed / verify

The next familiar-mechanics pass is now closer to original Witchery:
- `Bind Familiar` no longer creates a brand-new owl/cat/toad out of the ritual offering. It now scans the medium ritual circle for nearby tamed familiar-capable creatures owned by players inside the ring and binds those instead, which is much closer to the old `RiteBindFamiliar` behavior.
- Vanilla cats are now converted into the port's `Cat Familiar` entity when they are bound, which restores the old "bind a real pet and gain familiar powers from it" flow better than the previous spawn-a-new-pet shortcut.
- The missing small-circle `summonfamiliar` route is back too through representative `drop_of_luck`, and it now spawns a real bound Witch's Cat at the circle again instead of leaving the original familiar-summon branch absent.
- That small-circle familiar summon now also restores its original support gate and startup cost again: it once more expects an `egg`, `gold ingot`, and `Arthana`, and once more consumes 2000 altar power before the familiar appears.
- `Call Familiar` no longer only works when the bound familiar entity happens to already be loaded in the same dimension as the rite. It now searches all loaded server levels for the stored familiar UUID and teleports the real bound entity into the circle again.
- Familiar-binding dispatch no longer reports unconditional success for the feather/fish/slime placeholder routes. If there is no valid creature to bind, the medium-circle cast now fails/refunds instead of silently consuming the offering and minting a free familiar.
- The familiar dispatcher routes are now keyed from representative Witchery ingredients (`tear_of_goddess` / `odour_of_purity` / `whiff_of_magic` for binding, `breath_of_goddess` / `hint_of_rebirth` / `whiff_of_magic` for calling) instead of unrelated vanilla placeholders, which is much closer to the original ritual registry feel.
- The shared original `whiff_of_magic` overlap is now handled more honestly too: the medium ritual circle will use it for `Bind Familiar` when a real bindable familiar candidate is present, otherwise it falls through to `Call Familiar`, instead of forcing `whiff_of_magic` onto Call only.
- The familiar-powered side systems now use the same cross-level bound-familiar lookup too, so brew-mastery, curse-mastery, and owl broom-mastery checks no longer fail just because the familiar entity is loaded in a different server level than the player.
- Bound familiar data now also stores the familiar kind (`owl` / `cat` / `toad`) instead of only the live entity UUID, so `Call Familiar` can recreate the correct familiar type at the circle when the old familiar entity has been dismissed or died.
- Legacy UUID-only familiar saves now repair themselves on the next successful `Call Familiar`: if the live familiar still exists, the rite backfills the missing familiar kind automatically instead of waiting for a fresh rebind.

Remaining gap:
- The modern port now preserves bound familiar UUID, familiar kind, custom familiar name, sit state, and familiar-name visibility, but it still does not carry over the original deeper familiar instance/template state beyond that core identity. Recreated familiars therefore regain the right owl/cat/toad type, familiar name, resting/follow posture, and visible-vs-hidden nameplate preference without yet restoring every old-Witchery per-familiar history or styling detail.

### 66. Witches' Primer was still a silent stub - Fixed / verify

The guidebook flow is now closer to how a modern Witchery port should behave:
- `The Witches' Primer` no longer right-clicks into a silent no-op.
- When Patchouli is installed, the item now opens the real `witchery:witches_primer` Patchouli book again through a soft optional reflection hook, so Witchery still compiles cleanly without a hard Patchouli dependency.
- When Patchouli is not installed or the API call fails, the player now gets a clear missing-Patchouli message instead of the item pretending to work.

Remaining gap:
- This restores the practical book-open behavior, but it still needs an in-game verification pass with Patchouli actually present on the client/server pack.

### 67. Alluring Skull was still too fragile - Fixed / verify

The next active-block parity pass is now closer to original Witchery:
- `Alluring Skull` already had its stateful on/off flow, low light output, floor/wall placement, floor rotation, undead allure tick, and skull-head renderer back.
- It now also restores the old very high blast resistance instead of behaving like an ordinary fragile decorative block under explosions.

Remaining gap:
- Final in-game verification is still open for scale/lighting feel, but the core block behavior is now closer to the original 1.7.10 skull.

### 68. Necromantic Stone item was missing its item-definition asset - Fixed / verify

The asset chain is now complete again:
- `necro_stone` already had a valid item model and texture.
- The missing `assets/witchery/items/necro_stone.json` definition has now been added, matching the working `attuned_stone` asset chain.

Remaining gap:
- Final in-game verification is still open, but the pink-black fallback should now be gone in inventory, hand, and other item render contexts.

---

### 8. Torment red pulse / overlay decision - Closed

The current red biome sky/fog is being kept as the intended hell aesthetic. The dimension type stays on `minecraft:overworld` effects so the view remains red/pink without adding the darker Nether pulse overlay.

---

### 9. Torment was too dark - Fixed / verify

Old Witchery's `BlockForce` used light opacity `0`, rendered as a non-opaque cube, and the Torment provider forced a constant red sky. The port had Force registered as a normal opaque block, `has_skylight: false`, midnight fixed time, and no ambient floor, which made maze levels much darker than the original/reference screenshot.

Current state:
- `ForceBlock` now restores the old light behavior by propagating skylight and returning light block `0`, while remaining solid/unbreakable for maze collision.
- Torment now uses `has_skylight: true`, `fixed_time: 6000`, and `ambient_light: 0.15` so it remains ominous but navigable.
- `/witchery verify torment` now loads the full legacy Torment footprint and audits the Force ceiling/light behavior, Bedrock floor/frame, legacy portal chamber, exact per-level refilling chest positions/count, zero `Torment Stone` contamination, and the red sky/fog data settings before manual in-game inspection.
- Final in-game verification is still needed across all six stacked maze levels, especially lower floors where Bedrock floors and Force ceilings stack through the old maze footprint.

### 10. Torment maze blocks/layout used wrong port simplification - Fixed / verify

Old Witchery generated Torment by calling `GenerateMaze.display(originX=-23, y, originZ=6, Force, Bedrock)`. The port had simplified that into a 0..62 footprint with `TormentStone`/nether-brick-looking floors, portal at the far corner, and chests floating at `y + 1`.

Current state:
- `TormentChunkGenerator` now follows the legacy 31x31 DFS display footprint at x=-23/z=6 for all six stacked levels.
- Floor and portal frame blocks use vanilla Bedrock again; walls and ceilings use invisible, solid, light-transparent Force blocks like old `BlockForce`.
- Portal chamber and three old-style refilling chest rooms are restored per level, with chests placed at floor level.
- `TormentPortalBlock` now renders as the old thin red portal plane again, keeps the fixed west/east-framed axis used by the legacy chamber, and restores the original ambient portal sound plus flame particles so the exit is readable in-world instead of looking like a placeholder cube.
- The obsolete random `torment_chambers` structure-set path is gone, so fresh/deleted Torment chunks now rely on the restored legacy maze generator alone instead of mixing in the later standalone chamber structure.
- `TormentHelper`, `/witchery torment`, `/witchery tormentlevel`, and the Lord of Torment half-health transfer now all enter the real maze start again, and the phase-2 Lord now respawns on the old fixed maze lane at half health instead of using arbitrary safe-spot scans.
- `/witchery verify torment` gives a repeatable server-side audit for fresh/deleted Torment chunks before the remaining visual/manual QA pass, and its footprint scan now covers the full right-side chest room instead of undercounting those chests.
- Existing saves still need the Torment dimension folder deleted, or a fresh world, before this visual/layout fix can be verified because old chunks will not regenerate automatically.

### 11. Rowan worldgen still tucked itself into existing canopies, Mutandis was missing vanilla bush, and Witch's Ladder rendered wrong in-world - Fixed / verify

The previous rowan worldgen improvement stopped trunks from dropping onto leaf-covered forest floors, but it still allowed rowan to generate too close to other trees. Mutandis also still lacked the new vanilla `bush` shrub in its mutation pool, and Witch's Ladder's block model was missing an explicit cutout render type, which could make the original sprite look wrong or opaque in-world.

Current state:
- `rowan_tree.json` now keeps the `WORLD_SURFACE_WG` surface sample and adds a custom `tree_clearance` placement filter.
- `TreeClearancePlacement` rejects rowan generation when nearby logs or leaves occupy the checked clearance box, giving rowan a more open footprint instead of letting it tuck straight into other tree canopies.
- `MutandisItem` now includes vanilla `BUSH` in the shared plant mutation pool that Extremis inherits from, restoring the intended shrub outcome.
- Mutandis mutation placement still normalizes upper-half double-plant clicks back to the lower half and safely clears orphaned upper halves when swapping away from double plants.
- `fetish_witchs_ladder.json` now declares `minecraft:cutout`, matching the other transparent cross-style Witchery blocks so the original Witch's Ladder sprite no longer renders like an opaque block.
- Final in-game verification is still needed to tune whether the current rowan clearance radius feels right across dense forests, confirm vanilla `bush` shows up in both normal Mutandis and Extremis, and retest Witch's Ladder in-world after the cutout render fix.

---

## Feature-polish backlog, not blocking bugs

### Item review - remaining visual/mechanic issues

Active item review in `docs/status/ITEM_REVIEW.md`. Priority remaining items:
- `altar` - strict 2x3 validation is back, and the joined-art path now uses the resurrected-style single-texture altar assets instead of the placeholder split top/joined overlays; remaining work is now in-game orientation/polish verification rather than missing connected states
- `distillery`, `spinning_wheel`, `witches_oven` - mechanics were restored closer to original, including the old comparator-output behavior; `distillery` now regains its low active glow plus spell-like work ambience, `witches_oven` now regains its burning light/state presentation plus front smoke/flame ambience, `spinning_wheel` also has the original sturdier hardness back, and the machine screens now explicitly blit the intended `176x166` Resurrected-style panel region plus exact overlay slices instead of compressing the full `256x256` sheet into the container, which should remove the black barcode-style stripe artifact from the right-margin art; final GUI/in-game verification is still open
- `grassper` - one-item stash behavior and the custom plant render path are back, and the block now again emits comparator output while occupied like the old one-slot inventory.
- `crystal_ball` - shared translucent model path, original compact pedestal footprint, support-required placement/survival, sturdier hardness, and original-style snare/levelup/explosion use feedback are back; remaining work is final in-game translucency verification
- `coffin` - placed two-block render is back on split head/bottom textures, the placed block now uses its intended lower coffin-height collision instead of a hidden full cube, and item/inventory presentation now uses the same split 3D geometry through a dedicated special renderer; remaining work is final in-game pose/scale polish only
- Witchery spawn eggs no longer point at the missing `minecraft:item/template_spawn_egg` path used by older versions. They now render again through a stable local fallback egg texture/model chain on 1.21.11; exact per-egg colour art parity is still a later polish pass.
- `alluring_skull` - stateful block behavior, low light output, floor/wall mounting, floor rotation, undead allure behavior, blast resistance, compact collision footprint, and the dedicated skull-head render path are back closer to original; standing-vs-wall placement now follows nearest-looking direction so edge hits stop flipping the skull unexpectedly while placing several in a row, the renderer now uses model-only skull textures with a filled underside so wall-mounted skulls no longer show a transparent bottom, and skull yaw is applied through the pose stack instead of a shared mutable head pose so nearby standing skulls stop turning together; remaining work is final in-game verification and any scale polish
- `demon_heart` - stateful block behavior, weak light output, original low hardness, compact collision footprint, ambient particles, and the heart-shaped animated custom render are back; the baked-shell fallback was reverted because it looked wrong, the custom model now renders through a no-cull cutout layer, the renderer uses a padded model-only texture so modern sampling does not punch the old UV islands transparent as aggressively, and the remaining placement-side shell faces now have their model-texture UV islands filled directly instead of relying on the original sparse alpha cutouts; remaining work is in-game verification plus any final item-pose polish
- `fetish_scarecrow`, `fetish_trent_effigy`, `fetish_witchs_ladder`, `statue_goddess` - fetish placement now faces the placer instead of yawing left or rendering backward after placement, scarecrow/trent once more render through their dedicated placed/item model paths, Witch's Ladder is back on a flat ladder block/item model instead of borrowing scarecrow visuals, the original sturdier hardness plus high blast-resistance profiles are now restored, and `statue_goddess` once again uses the original tall narrow outline/collision footprint instead of a hidden full cube, remembers its placer so creative players or the sneaking owner can pop the statue stack back off like the old block, and now restores the original fizz/snare cure feedback for the curse/nightmare signatures the port currently tracks; remaining work is broader in-game verification plus any deeper legacy curse-flag parity beyond those modern tracked afflictions
- `chalice` - altar power bonus, original hardness, support-required placement/survival, filled-state persistence through pick/drop/re-place, compact original footprint, and static 3D presentation are back; the item model now also uses a smaller dedicated GUI transform so it no longer overfills the creative/inventory slot; remaining work is final in-game placed-block and item-pose verification
- `poppet_shelf` - server/storage mechanics are back, top-shelf poppet rendering is restored, and the placed shelf now rotates with block facing again; the remaining gap is final in-game item spacing/presentation verification
- `distillery`, `spinning_wheel`, `fume_funnel`, `brazier`, `candelabra` - item rendering now runs through dedicated special-model renderers; `candelabra` has its original light/support/particle behavior and narrow non-full-cube footprint back, `brazier` once again uses its original narrow bowl footprint for collision/outline, restores comparator output, and now uses the original sturdier hardness too, `fume_funnel` has its original linked-oven interaction and hardness back, and the remaining work is final in-game silhouette verification
- `leech_chest`, `refilling_chest` - chest item/block rendering is much closer to stable now, but final in-game verification is still open
- Spawn eggs now use the normal per-egg item-definition -> per-egg model -> vanilla `template_spawn_egg` chain again instead of trying to jump straight to the vanilla template from the item definition; needs an in-game retest before this can leave the backlog completely
- Full Tools/Armour/Brew sections - untested

---

### Corpse entity renderer

`witchery:corpse` uses `NoopRenderer` (invisible). Needs a humanoid sleeping-pose model to be visible to other players. Low priority - entity is hittable and functional server-side.

### Spirit World remaining content

- Wispy Cotton vegetation and disturbed-cotton cleansing loop - Done
- Disturbed Cotton vegetation - Done
- Nightmare spawns only during nightmare state - Done
- Nightmare teleport/respawn pressure - Done
- Spirit Portal snow-frame activation - Done
- Dream Weaver blocks affecting dream/nightmare chance - see NEXT_TASKS Sprint 3 #1

### Torment content

- Lord of Torment boss flow (phase 1 -> 2, player return) - Done
- Flame Imp -> Torment Scroll progression - Done
- Maze generator / maze-like structure - legacy block/layout pass restored; needs fresh-dimension in-game verification
- Torment Stone block and portal exit - portal exit active; Torment Stone remains registered but no longer used as the old maze floor
- Torment red sky/fog - biome red sky/fog with `minecraft:overworld` dimension effects is intentionally kept for the hellish aesthetic without the extra Nether pulse darkening
