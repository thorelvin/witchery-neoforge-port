# Active Bugs & Fix Plan

Last reconciled: 2026-05-19

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
| Medium | Decide Torment red pulse/fog effect | 5-20 min | Decision made; keep current look |
| Feature | Spirit World vegetation + Nightmare spawning polish | 1-3 hrs | Backlog |

---

## Open bugs

### 1. Ritual queue parity is still partial

Current state:
- Circles now persist after activation again.
- Rite ingredients now consume only on successful activation.
- New casts now stop cleanly once a rite route is actually identified, even if that rite then aborts on `already_active`, coven, obstruction, or taglock/precondition failures, instead of falling through to an unrelated smaller-ring retry.
- Weather Call Storm, Rain of Toads, Summoning, Moving Earth, Raise Column, Crater, Sanctity, and Imprisonment now run as staged active rituals instead of one-shot effects.
- Active rites now stop if the sustaining circle is broken and no longer stack onto an already-busy center.

Remaining gap:
- The original tile-entity ritual queue, upkeep lifecycle, and abort/refund behavior are only partially restored.
- See `docs/status/NEXT_FIXES_PLAN.md` for the ordered parity roadmap.

Files:
- `src/main/java/com/emoniph/witchery/ritual/RitualExecutor.java`
- `src/main/java/com/emoniph/witchery/events/RitualEvents.java`

---

## Fixed / verify

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
- `Blood Rose` stores a fresh player sample again and lets `Taglock Kit` pull it later.
- `Spanish Moss` grows as a fast vine instead of a dead decorative cross.
- `Grassper` works as a one-item stash and renders its stored item again.
- `Glowglobe` emits real light and ambience again.
- `Leaping Lily` grants the expected short movement buffs again.
- `Bramble` and `Void Bramble` now behave as hazards again.

These still need in-game verification for feel and visuals, but they are no longer plain placeholder blocks.

### 14. Altar-adjacent artefact parity was still missing edge cases - Fixed / verify

The altar pass is now closer to the original 1.7.10 behavior again:
- Skull bonuses now stack across the altar structure instead of only taking the single best head.
- Missing natural power contributors like Blood Rose, Critter Snare, and Artichoke now count toward altar power.
- Mystic Branch can be placed on the altar again and grants its enhancement bonus.
- Arthana and other placed altar items now invalidate the altar immediately instead of waiting for the next scan window.
- Retrieving or breaking placed altar items no longer risks silently eating the stored item.

This still needs in-game verification for final feel, but the remaining gap has moved away from basic altar mechanics and toward broader storage/ritual parity.

### 15. Leech Chest and Taglock Kit had lost their original storage flow - Fixed / verify

The first real storage-parity pass is now back in place:
- `Leech Chest` is a 27-slot container again instead of a dead stub block entity.
- Opening it stores up to 3 recent unique players again, matching the old "recent victims" behavior more closely.
- `Taglock Kit` can now pull a stored non-self target from `Leech Chest` again and reports the old failure cases more sensibly: only your own taglock, or players no longer online.
- `Taglock Kit` also splits stacked kits correctly again when filling a single one, instead of binding an entire stack at once.
- Breaking and replacing the `Leech Chest` now preserves that stored player queue on the dropped chest item again instead of wiping it.

### 16. Poppet Shelf only worked as a local chest - Fixed / verify

The first real Poppet Shelf parity pass is now in place:
- `Poppet Shelf` only accepts poppet items again instead of acting like a generic chest.
- Breaking it now drops the stored poppets properly.
- Bound-poppet lookup now scans every loaded shelf on the current server again instead of only looking in a tiny radius around the player.
- Shelves with bound poppets now force-load their chunk again, so poppet protection can still trigger even when the player is far away.

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

Remaining gap:
- The original 1.7.10 rite moved tile entities as part of the terrain lift; the port still skips block-entity columns for safety until that migration logic is restored cleanly.

### 20. Crater was an instant sphere delete instead of a staged excavation rite - Fixed / verify

The Crater rite is back much closer to old Witchery now:
- Instead of deleting a full sphere in one tick, it now queues as an active large-circle ritual again.
- It pulses every 10 ticks and deepens over several stages, matching the old crater-opening cadence more closely.
- The carve now follows a layered downward crater shape instead of flattening everything inside a sphere immediately.

Remaining gap:
- The original rite had more exact legacy terrain-drop rules, so the port still uses a conservative modern approximation for which removed blocks drop items.

### 21. Sanctity and Imprisonment were one-shot effects instead of sustained protection circles - Fixed / verify

The Sanctity and Imprisonment rites are back much closer to old Witchery now:
- Instead of immediately teleporting mobs and ending, they now queue as sustained active circles again.
- Both now require a nearby working altar to keep running and consume altar power over time.
- Sanctity now behaves like a repulsive ward, while Imprisonment now keeps nearby creatures penned toward the circle instead of acting as a one-tick trap.

Remaining gap:
- The original mod split these protection-family rites across different circle sizes and also had separate barrier variants, so the remaining parity work is now mostly about full ritual mapping and barrier-specific behavior rather than the basic sustained-circle mechanic.

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

### 24. Refilling Chest used the wrong refill timer and loot source - Fixed / verify

The Refilling Chest is back much closer to old Witchery now:
- Instead of starting a fresh one-hour cooldown only after the chest is emptied, it now uses the original lifetime tick cadence again and refills on the hourly boundary while empty.
- It now saves that lifetime counter directly, matching the old `WITCLifeTicks` behavior more closely and avoiding the port's extra `Filled`/cooldown bookkeeping.
- The refill source now pulls from a small dungeon-style chest table again instead of the custom Torment-only loot table, and only inserts a small 2-5 stack refill like the original.

Remaining gap:
- This restores the practical refill mechanic, but the remaining work on the block is now visual polish and in-game verification rather than core storage behavior.

### 25. Large-circle Protection was still a self-buff instead of a real barrier dome - Fixed / verify

The protection barrier family is now back much closer to old Witchery:
- The default large-circle `Rite of Protection` no longer resolves as a modern potion buff on the caster.
- It now queues as a sustained active rite again, requires altar power to stay alive, and redraws an invisible barrier dome around the circle on a timed pulse.
- The barrier itself now has real timed block/entity behavior again instead of being a dead placeholder block.
- The smaller monster-only barrier variant and the timed portable barrier variant are both reachable again through the modern medium-circle ingredient dispatcher.

Remaining gap:
- This restores the practical barrier mechanics, but the activation mapping is still modernized rather than matching the original multi-sacrifice recipes exactly, and the old charged-attuned-stone distinction is still collapsed into the port's single `attuned_stone` item.

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
- The port now tracks fractional upkeep debt across pulses, so these rites preserve the old relative costs without forcing the altar system back to floating-point storage.

Remaining gap:
- This restores the protection-family upkeep ratios, but the broader active-ritual queue and refund lifecycle is still only partially recreated compared with the original tile-entity ritual engine.

### 30. Several curse rites were missing their original weather/time gates - Fixed / verify

The next ritual precondition pass is now back in place:
- `Curse Creature`, `Curse of Insanity`, `Curse of Sinking`, `Curse of Overheating`, and `Curse of Waking Nightmare` now require an actual thunderstorm again before they will cast, matching the old `ONLY_IN_STROM` recipe traits.
- `Curse of Hell on Earth` now requires the Overworld at night again instead of firing in any dimension at any time.
- These checks now happen before taglock consumption, so failed preconditions no longer waste the curse item flow.

Remaining gap:
- This restores some of the missing activation traits, but the ingredient-dispatch ritual system still does not model the full original recipe registry and its trait matrix yet.

### 31. Total Eclipse and broom infusion were missing original day/night gates - Fixed / verify

The next ritual trait pass is now back in place:
- `Rite of Total Eclipse` now requires daytime again before it will fire, matching the old `ONLY_AT_DAY` recipe trait instead of allowing free midnight shifts at any time.
- The broom infusion rite triggered by `Flying Ointment` now requires nighttime again, matching the original enchanted-broom recipe instead of allowing daytime enchanting.
- These checks now fail early with the same snare/message style as the other restored ritual preconditions.

Remaining gap:
- More original recipe-trait coverage is still missing outside these rites, especially where the modern ingredient dispatcher has collapsed multiple legacy ritual recipes together.

### 32. Curse of Insanity was unreachable because Silent Step claimed the same ingredient first - Fixed / verify

The next ritual-dispatch pass is now back in place:
- `FERMENTED_SPIDER_EYE` no longer gets intercepted by `Silent Step` before the curse branch runs.
- `Silent Step` now only claims `SPIDER_EYE`, so `Curse of Insanity` can actually be triggered again through the modern medium-circle dispatcher instead of being dead code.

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
- Taglock-based curse rites and `Transposition Pull` now validate that a filled taglock resolves to a real online target before consuming it, so invalid or offline targets no longer waste the taglock.
- `Transposition Pull` now respects Witch Hunter black-magic dampening and Voodoo Protection poppets again, and the original `witchery.rite.blackmagicdampening` feedback text is restored.

Remaining gap:
- The ritual dispatcher is still a representative single-item shortcut for many rites, so more sacrifice-mapping cleanup is still needed even though this removes one of the larger false-role activations and one of the more obvious refund errors.

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
- Large-circle `Curse of Waking Nightmare` and `Blight` now require infernal chalk again, and large-circle Fetish binding now requires ritual chalk again, instead of all three accepting the wrong family.
- Small-circle teleport-to-bound-waystone now requires otherwhere chalk again instead of activating from any small ring.
- New casts now retry every present ring from large to small instead of collapsing through the first detected ring size only, so mixed-ring activations no longer dead-end just because a smaller inner circle is present.

Remaining gap:
- This fixes sustain-time validation, one more slice of glyph-family parity, and the first major mixed-ring activation failure, but the port still uses an ordered size retry plus representative trigger items instead of the original full multi-circle, multi-sacrifice ritual registry.

### 40. Small-circle waystone/manifest routes were still off - Fixed / verify

The next small-circle parity pass is now closer to original Witchery:
- `Bind Waystone` now requires ritual chalk again instead of accepting any small ring.
- `Manifestation` is now actually reachable again through the simplified dispatcher, mapped to `spectral_dust` on an otherwhere small circle instead of remaining implemented-but-dead.

Remaining gap:
- These routes are now reachable and family-gated, but the dispatcher is still using representative single items rather than the original full multi-sacrifice recipe sets.

### 41. Forestation and animal-calling were routed through the wrong ring sizes - Fixed / verify

The next ritual routing pass is now closer to original Witchery:
- `Forestation` now routes from the medium ritual circle again instead of incorrectly living on the large-circle dispatcher.
- The animal-calling `Beastial Call` behavior no longer fires from an arbitrary small ring; it now routes through a large otherwhere circle instead, which is much closer to the original `Call Beasts` rite.

Remaining gap:
- Both routes are still using representative single-item triggers inside the simplified dispatcher instead of the full original sacrifice sets, but their ring size and chalk family are now much closer to original parity.

### 42. Familiar and reincarnation rites were still on the wrong circles - Fixed / verify

The next ritual routing pass is now closer to original Witchery:
- `Prior Incarnation` now requires an infernal small circle again instead of activating from any small ring.
- Familiar binding and familiar calling no longer live on the small-circle dispatcher; they now route through the medium ritual circle instead, which is much closer to the original `bindfamiliar` and `callfamiliar` rites.

Remaining gap:
- These rites are still triggered through simplified representative ingredients rather than the full original sacrifice sets, but their ring size and chalk family now line up much more closely with 1.7.10.

### 43. Blindness and Blight were using the wrong mechanic entirely - Fixed / verify

The next infernal-rite correction is now closer to original Witchery:
- Small infernal `curseblindness` now runs the area `Rite of Blindness` again instead of incorrectly behaving like a taglock curse on one player.
- Medium infernal `curseblight` now runs the area/world `Rite of Blight` again instead of incorrectly behaving like a single-target curse.
- The false large-circle blight activation path was removed, so the world-blight effect no longer appears as a non-original extra large infernal rite.

Remaining gap:
- These infernal rites are now much closer mechanically, but they still use simplified representative trigger items instead of the full original sacrifice lists.

### 44. Small infernal witch summoning was missing entirely - Fixed / verify

The next missing-rite pass is now closer to original Witchery:
- `summonwitch` is reachable again as a small infernal rite through `diamond_vapour`, instead of being absent from the modern dispatcher.
- The restored route now spawns a hostile witch at the circle center and uses a proper summoning-space obstruction check instead of blindly spawning inside blocked space.

Remaining gap:
- The dispatcher is still using one representative original ingredient instead of the full original sacrifice set, and only this one summon-creature rite has the restored obstruction check so far.

### 45. Large infernal imp summoning was missing entirely - Fixed / verify

The next missing-rite pass is now closer to original Witchery:
- `summonimp` is reachable again as a large infernal rite through `refined_evil`, instead of being absent from the modern dispatcher.
- The restored route now brings back the original coven dependency for imp summoning and reuses the restored summoning-space obstruction check instead of blindly spawning into blocked circles.

Remaining gap:
- This is still routed through one representative ingredient instead of the full original sacrifice set, and the shared summon-creature family is not fully rebuilt yet beyond the restored witch/imp paths.

### 46. Large infernal demon summoning was missing entirely - Fixed / verify

The next missing-rite pass is now closer to original Witchery:
- `summondemon` is reachable again as a large infernal rite through `tormented_twine`, instead of being absent from the modern dispatcher.
- The restored route now summons the Witchery demon entity at the circle center and reuses the restored summoning-space obstruction check instead of blindly spawning into blocked circles.

Remaining gap:
- This is still routed through one representative ingredient instead of the full original sacrifice set, and the larger summon-creature family still needs fuller parity work beyond the restored witch/imp/demon paths.

### 47. Medium infernal wither summoning was missing entirely - Fixed / verify

The next missing-rite pass is now closer to original Witchery:
- `summonwither` is reachable again as a medium infernal rite through a representative `wither_skeleton_skull` route, instead of being absent from the modern dispatcher.
- The restored route now spawns the Wither at the circle center and reuses the shared summoning-space obstruction check instead of blindly spawning inside blocked circles.

Remaining gap:
- This still uses one representative trigger instead of the original multi-sacrifice summon recipes, and the larger binding/summoning family still needs fuller parity work beyond the restored witch/imp/wither/demon paths.

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

Remaining gap:
- This still uses one representative trigger instead of the original multi-sacrifice recipe, and the modern item no longer uses the old metadata/icon split even though the release mechanic itself is back.

### 51. Rite of Binding for Spectral Stone was missing entirely - Fixed / verify

The next missing-rite pass is now closer to original Witchery:
- `bindspectral` is reachable again as a large ritual-circle route from an empty Spectral Stone, instead of being absent from the port entirely.
- The restored route now looks for nearby spectral creatures, chooses the nearest creature family, binds up to three of that same type into the stone, removes those entities from the world, stores the result in modern item data so the item is no longer mechanically blank, and the released spirits now remember their summoner well enough to avoid immediately targeting them.

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
- It now validates a filled taglock target, consumes that taglock only after target resolution, and destroys up to 10 bound `Voodoo Protection Poppet` items on the target again.
- `Poppet Protection` now blocks this rite again and throws the backlash lightning onto the caster instead of letting the anti-voodoo poppets be destroyed.

Remaining gap:
- The modern dispatcher still collapses the original five-sacrifice recipe into a representative infernal trigger item, so the exact multi-sacrifice activation shape is still not full 1.7.10 recipe-registry parity.

### 58. Banish Demon rites were missing entirely - Fixed / verify

The next missing-feature pass is now closer to original Witchery:
- The small `banishdemon` and `banishdemonportable` family is reachable again instead of being absent from the port.
- The restored rite now banishes the port's infernal creature equivalents within the original 9-block radius: `Demon`, `Flame Imp`, `Death`, and `Lord of Torment`.
- It is exposed through representative small ritual `attuned_stone` and bound-`waystone` routes because the port still lacks the original charged-attuned-stone split and full sacrifice matcher.

Remaining gap:
- The exact portable-vs-regular multi-sacrifice activation shape is still collapsed into representative trigger items, so this restores the effect family without claiming full 1.7.10 recipe-registry parity.

### 59. Transpose Ores was missing entirely - Fixed / verify

The next missing-feature pass is now closer to original Witchery:
- The old large otherwhere `teleportironore` behavior is reachable again instead of being absent from the port.
- The restored rite now pulses every 10 ticks, scans descending underground layers in an 8-block radius, removes matching ore blocks, and throws them upward as collectible ore-block items near the circle again.
- Full-coven behavior is back in practical form too: ordinary casts transpose iron ore, while covens of 6 also pull gold ore like the original.
- It is exposed through a representative large otherwhere `diamond_vapour` route because the port still lacks the original charged-attuned-stone split and full sacrifice matcher.

Remaining gap:
- The exact multi-sacrifice activation shape is still collapsed into a representative trigger item, so this restores the effect family without claiming full 1.7.10 recipe-registry parity.

### 60. Nature's Power was missing entirely - Fixed / verify

The next missing-feature pass is now closer to original Witchery:
- The old large ritual `naturespower` behavior is reachable again instead of being absent from the port.
- The restored rite now runs as a long staged growth effect, picks random nearby ground anchors, greens the soil, spreads plants, bonemeals growth, and scales its working radius and lifetime with nearby coven witches again.
- It is exposed through a representative large ritual `brew_of_sprouting` route because the port still lacks the original charged-attuned-stone split and full sacrifice matcher.

Remaining gap:
- The exact old plant palette and fake-player bonemeal edge behavior are still approximated through modern block growth helpers, so this restores the rite's world-shaping job without claiming exact 1.7.10 block-for-block growth parity.

### 61. Small otherwhere waystone transposition was doing the wrong job - Fixed / verify

The next mechanical parity pass is now closer to original Witchery:
- The small otherwhere bound-waystone rite no longer ignores the offered waystone and teleports only the caster from inventory.
- It now reads the sacrificed bound waystone itself again and teleports nearby entities within the circle's original short radius, which is much closer to the old `RiteTeleportToWaystone` behavior.
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
- Refilling Chest now uses a stable local chest texture path for both block and item rendering instead of depending on a brittle vanilla chest texture lookup.
- Poppet Shelf top-item rendering is back on a tighter visible 3x3 grid instead of the looser oversized placement that was easy to miss or clip visually.

Remaining gap:
- These fixes still need in-game verification against the latest client build, and the remaining art mismatches are now narrower item/block polish issues tracked in `ITEM_REVIEW.md`.

---

### 8. Torment red pulse / overlay decision - Closed

The current Nether-style pulse is being kept as the intended hell aesthetic.

---

## Feature-polish backlog, not blocking bugs

### Item review - remaining visual/mechanic issues

Active item review in `docs/status/ITEM_REVIEW.md`. Priority remaining items:
- `altar` - mechanics are back near original 2x3 behavior; block and item are 3D again, but the original connected/joined altar art still needs a deeper render pass
- `distillery`, `spinning_wheel`, `witches_oven` - mechanics were restored closer to original; GUI texture/layout verification is still open
- `crystal_ball` - must be transparent
- `coffin` - shows only one side in inventory
- `alluring_skull`, `demon_heart` - wrong 3D model/UV
- `poppet_shelf` - server/storage mechanics are back and top-shelf poppet rendering is restored; item texture/model polish is still open
- Full Tools/Armour/Brew/Spawn egg sections - untested

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
- Maze generator / maze-like structure - deferred
- Torment Stone block and portal exit - deferred
- Torment red pulse - `"effects": "minecraft:the_nether"` in `dimension_type/torment.json` intentionally kept for the hellish red-fog aesthetic
