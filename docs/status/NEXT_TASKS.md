# Next Tasks — Witchery NeoForge Port

Last reconciled: 2026-05-23 (Vampire clothing player mesmerise slice)
Full feature status: `docs/status/FEATURE_MATRIX.md`

---

## Current plan

- Verify the promoted Vampire clothing lethal-fire save, drink boost, villager/player mesmerise, set-bonus tooltips, and exact alias armor pieces in-game against sunlight/fire/lava damage, feeding, and player-target use.
- Next highest-value vampire equipment work is armor model polish, full old ability-selection/timing/paralysis parity, Village Guard mesmerise parity, or old level-gate refinement.
- Keep the remaining 221 placeholder items inert until each family gets an explicit promotion or alias decision.

---

## Priority order for next session

### 0. Item Review — remaining Blocks (ongoing)

Items from `docs/status/ITEM_REVIEW.md` still needing work:

| Item | Issue | Effort |
|---|---|---|
| `altar` | Should be 2×3 multiblock; item graphic wrong (just a square); top should be red | Large |
| `distillery` | Retest machine GUI after explicit source-region blit fix | Low |
| `spinning_wheel` | Retest machine GUI after explicit source-region blit fix | Low |
| `witches_oven` | Retest machine GUI after explicit source-region blit fix | Low |
| `crystal_ball` | ✅ Now purple stained-glass orb on polished-blackstone stand | Small |
| `coffin` | ✅ Item now 3D box using coffin_head.png UV regions; verify in-game angles | Medium |
| `alluring_skull` | Dedicated skull-head renderer path is restored; final scale/lighting verification only | Low |
| `demon_heart` | Retest custom model no-cull face fill in-world | Low |
| `chalice` | Final GUI pose / placed-block verification | Low |
| `dream_catcher` | Clicked-face wall placement is restored; needs in-game verification only | Low |
| `kettle` | Side-post and ceiling-hung model/support paths restored; verify filled/hanging world-state visuals in-game | Low |
| `poppet_shelf` | ✅ Items now visible; verify in-game positioning | Low |
| `mandrake_root` | Retest Mandrake summon branch, Banshee custom scream, Earmuffs hush, and final audio feel | Small |
| `grassper` | Stash/item display path restored; verify held-item cup pose in-game | Low |
| `glowglobe` | Translucent low-alpha render path restored; verify visibility in-game | Low |
| `rowan_door` | Vanilla hinge/facing placement restored; verify lock/unlock feel in-game | Low |
| `leaping_lily` | Custom lily visuals restored; needs in-game verification only | Low |
| Legacy placeholder items | 221 source/wiki/decompiled item IDs remain registered and visible as inert creative-tab placeholders after `Biting Belt`, `Bark Belt`, `Beartrap`, `Wolftrap`, `Blood-stained Wool`, `Woven Cruor`, the primary Vampire clothing set, and the exact Resurrected Vampire clothing aliases were promoted: the remaining original 117 missing visible IDs plus 104 exact 1.12 Resurrected registry aliases that need remap/TODO decisions. All 221 remaining placeholders point at non-shared source/wiki-backed item models or clear alias models; none still use the shared placeholder model. Implement mechanics/recipes or alias-remaps one family at a time | Varies |
| Items 62–153 | Tools & Items, Armour, Brews, Infusion, Spawn Eggs — untested | Varies |

---

### 1. Spirit World — remaining spec gaps (medium, ~2 hr)

Items from spec not yet implemented:

| Gap | Spec ref | Status | Notes |
|---|---|---|---|
| **Health/hunger save-restore** | §8 | ✅ Done | Saved in `SpiritWorldState` sub-codec; restored on exit |
| **Demonic Nightmare events** | §13 | ✅ Done | Every 100t: 50% SmallFireball, 65% LargeFireball, 75% demon spawn (if <10 hostiles, <2 demons) |
| **Corpse entity** | §9 | ✅ Done | `witchery:corpse` (MISC mob, sleeping pose, visible humanoid sleeping-body renderer) spawns on SW entry, removed on exit, killing it forces player back; UUID tracked in `SpiritWorldState` |
| **Flowing Spirit entity effects** | §21 | Partial | Regen/weakness, disturbed-cotton cleansing, Nightmare defense suppression, and `Charm of Disrupted Dreams` armor-strip protection are back; remaining work is in-game feel verification rather than missing item coverage |
| **Spirit Portal activation** | §22 | ✅ Done | Snow-frame portal now opens again from a Flowing Spirit source in the Spirit World and is used by the restored manifestation loop |
| **Rite of Manifestation ghost** | §23 | Partial | The rite now grants 2.5 min + 25s per coven witch of manifestation time again, Spirit Portal can spend that time to manifest the player into the Overworld, timed return and Icy Needle recall are back, and manifested players now have a first-pass translucent ghost layer; remaining work is in-game feel/style polish |

---

### 2. Mirror World dimension (large, ~4–6 hr)

Original: `hasNoSky=true`, permanent dark, dark blue fog `(0, 0.03, 0.1)`, no rain/lightning.
Reflected overworld terrain with hostile player-clone inhabitants.
- **Dimension type:** `effects: the_end`, `fixed_time: 18000`, `has_skylight: false`, `ambient_light: 0.1`
- **Biome:** fog ~`RGB(0, 8, 26)`, sky very dark
- **Entry:** Mirror block ritual
- **Current foundation:** dimension key/data now exists, `/witchery mirror` can enter it for QA, landing uses the player's current X/Z with a safe surface scan, the client now forces the dark-blue fog tone, and server tick enforcement now keeps the realm clear of rain and thunder
- **Next real steps:** mirrored terrain rules, ingress block/ritual, and hostile player-clone inhabitants

---

### 3. Custom Brew system (very large, separate sprint)

5-tier capacity/dispersal; unlocks high-level brew effects.
**Approach:** capacity + dispersal tiers first (liquid/splash/gas), skip modifier/power scaling v1.

---

### 4a. Debug command: add `spectral` to `/witchery familiar` suggestions — Done

`"spectral"` added to `FAMILIAR_TYPES` suggestions. `setFamiliar` handler now accepts `owl`, `cat`, `toad`, and `spectral` — stores the type via `setFamiliarType`, then calls `FamiliarHelper.restoreBoundFamiliar` to spawn the entity near the player. `"none"` still clears. Unknown types get an error message listing valid options.

---

### 4b. Small code quality items (deferred — after parity complete)

These are individually small but accumulate maintenance debt. **Do not action until all major parity work (Mirror World, Custom Brew, Spirit World gaps) is stable** — they carry non-zero risk of altering observable behavior mid-sprint.

- **`ConcentratedBatBallRecipe.assemble`** calls `matches(input, null)`. Current `matches` ignores the level so it works, but passing `null` into a `Level`-typed API is brittle. Extract a private pattern-check method called from both `matches` and `assemble`.
- **`TaglockKitItem.isPlayerTaglock`** returns `true` on unfilled stacks because it only checks whether there is no entity type tag. Should be: `return isFilled(stack) && getBoundEntityTypeId(stack).isEmpty()`.
- **`MutatingSprigItem`** repeats the same "spawn effects + damage held item + return consume" block for every successful transform. Extract a `finishUse(ctx, pos)` helper. Also, `tryCreateWormwood` checks for Wispy Cotton but calls `clearHorizontalMoss` — rename to `clearHorizontalRing` to match its actual behavior.
- **Registry classes** contain many hard-coded `"witchery"` string literals. An `id(String path)` helper would reduce copy/paste mistakes across blocks, entities, dimensions, and worldgen.

---

### 4c. RitualExecutor decomposition (deferred — after parity complete)

**Do not start until Mirror World, Custom Brew, and all open ritual-parity bugs are closed.** Trigger-order changes in this file can silently break which rite fires; doing it mid-parity-sprint is high risk.

`RitualExecutor` currently handles dispatch, precondition checking, item/power transactions, active ritual queueing, terrain effects, curse logic, climate-change biome rewriting, and utility scans — too many jobs in one file. There are also duplicated imports and a dead `isRawFood` helper.

`RitualTransaction` is a good first extraction; the next step is a `RitualDefinition` or per-rite handler registry so each rite owns its own validation, cost, and effect logic. Also strengthen `RitualTransaction` to validate → prepare → commit → apply order with a rollback path before this decomposition begins.

Priority for decomposition once unblocked: curse logic → active-rite queueing → terrain/climate effects → utility scans.

Files:
- `src/main/java/com/emoniph/witchery/ritual/RitualExecutor.java`

---

### 5. Witches' Primer / compendium completeness pass (medium, staged)

High-value player-discovery gaps to close in the in-game guide:

- **Blocked-player items first** - first-pass Primer coverage for `Boline`, the four infusion source items (`Ghost of the Light`, `Soul of the World`, `Spirit of Otherwhere`, `Infernal Animus`), `Anointing Paste`, and now `Mutandis` / `Mutandis Extremis` is in; the next guidebook value is completeness/polish rather than blocked progression basics.
- **Missing rites in Chapter 11** - first-pass Primer coverage is now in for the whole current missing-rite batch, including `Rite of Moving Earth`; the main remaining guidebook work is now completeness/polish rather than missing rite basics.
- **Treefyd seed bridge** - closed. `Treefyd Seeds` are now restored as a real crafted item, can once more be planted on grass to raise an owned Treefyd, and now have a dedicated Primer page instead of being left implied.
- **Tool/familiar identity gaps** - `Mystic Branch`, `Necromantic Stone`, `Treefyd Seeds`, and `Spectral Familiar` now all have truthful Primer coverage for their current port roles; next guidebook value is more in equipment/completeness than in blocked progression identity pages.
- **Equipment/completeness follow-up** - `Beartrap` / `Wolftrap` usage coverage, the explicit `Wicker Man` pattern/layout, first-pass Vampire clothing recipe/armor/acquisition coverage, the exact Resurrected Vampire clothing alias family, first drink-boost, mesmerise, and lethal-fire clothing set-bonus hooks, and source-style set-bonus tooltip lines are now in; remaining high-value equipment polish is armor model/level-gate polish plus full old ability-selection/paralysis parity.
- **Legacy placeholder follow-up** - `ITEM_REVIEW.md` now has a grouped TODO roster for the 221 remaining inert placeholder items, including 104 exact 1.12 Resurrected alias IDs that no longer silently disappear. Highest-value follow-up is to pick one family at a time: wearables/charms, bolts/contracts, hobgoblin rewards, custom-brew derivatives, block-like items, or exact-alias remaps.

---

## Long-term art direction

- **Original replacement art pass (deferred, post-parity)** - once mechanical parity is in a trustworthy state, plan a broad replacement pass for textures, sounds, and other audiovisual assets so the port is not just reusing/riffing on old Witchery content. The target should be "new art with the same gameplay identity and mood", not a straight rip of the original assets. Treat original Witchery as tone/mechanics reference, not as the final art source, and prefer building a distinct visual/audio identity before any public release push.

---

## Raven feature expansion (deferred, post core-parity)

The new raven should not stop at "real bird instead of parrot". It can become one of the port's signature systems once the main parity backlog is under control.

- **Raven familiar track** - allow a raven to bind to a player more like a true witch companion than a passive mob. Core behaviors to target: follow at medium range, perch near the player when idle, recall/teleport back if lost, and respect sit/stay-style commands without becoming a full wolf clone.
- **Murder of crows events** - add a low-frequency ambient system where a random murder of ravens can gather under the right mood conditions: storms, dusk, full moon, cursed land, nightmares, recent death, or active infernal rites. These flocks should feel like omens first and combat second.
- **Brew of Hitchcock escalation** - keep the current raven flock as the baseline, but later let the brew optionally interact with the murder system so a cast in the right conditions can chain into a larger local frenzy instead of always spawning the same simple flock burst.
- **Omen mechanics** - ravens are a good fit for soft warnings. They could circle above nearby curses, corpse locations, hidden hostile mobs, dream/nightmare disturbances, or ritual sites that are about to fail. This gives them a unique identity beyond "owl but darker".
- **Scouting and messenger utility** - a bound raven could be sent toward a nearby point of interest and return with a rough result rather than exact minimap-like data. Good candidates: nearest bound waystone, nearest witch structure, last death location, active nightmare corpse, or the last place a player slept.
- **Ingredient and object interactions** - ravens can make the world feel more alive if they steal, drop, or deliver small items. Future hooks could include pecking up seeds/shiny items, carrying a single lightweight delivery item, or occasionally bringing back minor curios when sent out.
- **Ritual integration** - ravens fit Witchery best when they cross into magic systems. Future ideas: perch on circles during certain rites, react to infernal/otherwhere chalk families differently, help reveal valid ritual centers, or serve as an optional omen/sacrifice/witness mechanic in new Rewoven-specific content.
- **Dream and Spirit World hooks** - ravens could become one of the cleaner bridges between dimensions and states. Examples: black-feather apparitions in the Spirit World, ravens warning of demonic nightmare risk, or a manifested raven familiar that can still find the player while ghosted.
- **Social and world flavor** - coven witches and hobgoblins can both gain flavor from raven presence. Witches might occasionally keep or summon ravens, while hobgoblins could fear them, throw food at them, or react to them as bad luck.
- **Rowan and ward interactions** - ravens should not ignore the rest of the mod's rules. Rowan protections, sanctity barriers, dream catchers, and other warding systems should influence where raven omens gather and whether they can cross into protected areas.

### Suggested rollout order

1. **Bound raven familiar** - follow, perch, recall, persistence, and basic command handling.
2. **Improved Hitchcock flock** - reuse the existing raven entity for a better flock event with more variation and less "spawn 12 birds and stop".
3. **Ambient murder/omen system** - rare flock gathering tied to weather, moon, curse, and nightmare signals.
4. **Utility interactions** - scouting, returning to the player, simple messenger/item-carry hooks.
5. **Deep system hooks** - ritual, Spirit World, curse, and ward integration.

### Design guardrails

- Ravens should feel **uncanny, useful, and slightly unpredictable**, not like a reskinned vanilla pet.
- Prefer **information, omen, and atmosphere** over raw combat power.
- Keep their strongest mechanics tied to **witchcraft context**: weather, rites, curses, death, dreams, waystones, and familiars.
- If the project leans into original replacement art later, the raven is a strong candidate for becoming part of **Witchery: Rewoven's own identity**, including custom sounds, animation, and eventual unique ritual content.

---

## Quick polish / verify items

- **Trader menus** - spawn/use both `Coven Witch` and `Hobgoblin`; verify the trade screen stays open instead of flashing shut, and make sure lead/name-tag interactions still win when intended
- **Setup hut** - run `/witchery setup stations`; verify the command clears the intended 20x20 pad in front of the player without digging below door/player height, the 11x11 Rowan hut shell/foundation still sits one block lower while the altar/machines/chest/traders stay on the original player-height test plane, the 3-deep stone-brick foundation still hides uneven ground, the Rowan door/windows plus the new horizontal Rowan log header generate correctly, the window panes now visually meet the Rowan walls, the ceiling Glowglobes light the hut cleanly, the altar sits off the back wall and still links as a real 2x3 multiblock, the cobblestone-supported kettle survives over the campfire, the water cauldron lands beside the kettle, the distillery/oven/spinning wheel face into the room, the fume funnel links cleanly above the oven, the oven cook arrow still sits on the correct upper lane, the stocked double chest contains the intended QA supplies, and the indoor Coven Witch + Hobgoblin both spawn and keep their trade GUIs open
- **Base alchemy chain** - smelt `clay_jar_soft` into `clay_jar`, then retest the oven/distillery loop; confirm `odour_of_purity`, `oil_of_vitriol`, and the other early intermediates now show in the Witchery creative tab for fast QA

- **Treefyd model** - `/witchery spawn treefyd` and verify the restored old 1.7 zero-plane petals, tongue, and crossed leaves now render visibly with the original 64x32 texture
- **Poltergeist model** - `/witchery spawn poltergeist` and verify the restored old all-part mirror UV flags close the reported wrong-mapping/holes issue
- **Banshee model** - `/witchery spawn banshee`, provoke/approach it, and verify the flat source mouth appears only while screaming and no chin/face patch protrudes at rest
- **Mandrake model** - `/witchery spawn mandrake` and verify the restored old mirror UVs and zero-depth leaf crown close the reported texture holes
- **Ent model** - `/witchery spawn ent`, provoke it, and verify the restored old mirror UVs, face plate, inner leaf fill, root-leg walk, and attack arm lift close the reported wrong-mapping/holes issue

- **Flying Monkey model** - `/witchery spawn flying_monkey` and verify the source monkey body, wings, ears, tail, landed/flying animation, and feather tint close the reported wrong-model/texture-holes issue
- **Flame Imp model** - `/witchery spawn flame_imp`, provoke it, and verify the source right-arm punch/body twist now plays during melee; power one up and verify the old taller powered render scale
- **Parasytic Louse potion bite/hotbar defense** - craft `parasytic_louse` with a non-water vanilla potion, place it, let it bite a target, and verify the carried potion effects apply once and then clear; verify pickup preserves unused potion payloads. Then keep a potion-fed louse in the hotbar and verify hostile effects bite the attacker while beneficial effects bite the player, clear the louse payload, and cost 1 magic damage
- **Biting Belt potion defense** - craft `biting_belt`, load it with up to two non-water vanilla potions, wear it in the legs slot, take entity damage with no usable hotbar louse, and verify it consumes one stored effect, routes harmful effects to the attacker or beneficial effects to the player, skips already-active/instant-heal effects, and costs 1 magic damage
- **Bark Belt charge defense** - craft `bark_belt`, wear it in the legs slot, stand on grass or mycelium long enough to gain bark pieces, then verify non-wooden entity damage is canceled by spending charges and popping stick drops while wooden sword/stake/Horned Huntsman damage still bypasses it
- **Beartrap trap block** - craft `beartrap`, place it, wait one second for arming, verify living entities snap it shut for 4 damage plus strong slowness, verify creative players do not trigger it, and verify right-click toggles it open/closed with a fresh arming delay after reopening
- **Wolftrap silvered trap block** - craft `wolftrap` from `beartrap`, silver ingots, Null Catalysts, and Wolfsbane, place it, verify ordinary living entities ignore it, verify a werewolf player snaps it shut for 4 damage plus strong slowness, and verify right-click still toggles open/closed with the arming delay
- **Spirit render** - enter the Spirit World or spawn a spirit and verify it now appears as the old small translucent wisp with no visible shadow instead of an opaque yellow ball
- **Demon model** — `/witchery spawn demon` and verify the source-static zero-depth wing pose now matches the Imp-like correct wing presentation
- **Death model** - `/witchery spawn death` and verify the scythe now sits against the hand after the original runtime pivot coordinate correction
- **Nightmare behavior/visuals** — `/witchery dream`, wait for nightmare roll; verify: single nightmare spawns, targets only victim, arms lift while attacking, tendrils sway without texture holes, green fog in nightmare, re-spawns after kill with 30s delay, drops Mellifluous Hunger
- **Torment maze** — delete `run/client/saves/<world>/dimensions/witchery/torment/` folder, re-enter; verify: 6 levels, invisible Force walls/ceilings with collision, Bedrock floor/frame, old portal chamber near x=8/z=68, and 3 refilling chest rooms per level

---

## Completed this session ✅

### Legacy placeholder item registry sweep (2026-05-22)

- Added `WitcheryItems.LEGACY_PLACEHOLDER_ITEMS` with 123 inert source/wiki item IDs that were not registered yet, exposed them in the Witchery creative tab, gave them a shared placeholder item model, and added explicit `(Placeholder)` language keys. `ITEM_REVIEW.md` now tracks the roster as TODO groups rather than hidden missing items. `spectral_familiar_spawn_egg` is also now visible in the creative tab.
- Follow-up exact-registry pass added 114 additional 1.12 Resurrected item IDs from `WitcheryGeneralItems`, `WitcheryBrewItems`, `WitcheryBookItems`, `WitcheryBoltItems`, `WitcheryContractItems`, `WitcheryEquipmentItems`, `WitcheryFumeItems`, and `WitcheryIngredientItems` as inert placeholders with shared item models and `(Placeholder)` translations. A `javap` audit now reports zero exact item-init registry strings missing from the port.
- Decompiled asset pass restored distinct item models/textures for 107 more placeholders, including the Resurrected brew aliases, books, contracts, bolts, fume ingredients, several tools, and companion override models for the placeholder crossbow/spear visuals. Current count: 154 distinct placeholder icons/models, 91 still on the shared placeholder icon.
- Wiki/existing-icon pass restored 46 more confident placeholder icons/models, including glass goblets, Gulg's Gurdle, Mog's Quiver, Horn of the Hunt, Player Compass, Blooded Waystone, vampire garments, odd-porkchop aliases, and generic brew bottle silhouettes. Current count: 200 distinct placeholder icons/models, 45 still on the shared placeholder icon.
- Exact wiki-icon pass restored 27 more placeholder icons/models, including traps, heads, ice/snow block-family items, Sun Collector, Staff of Duplication, Twisting Band, silvered hunter gear, and Witch Hunter Dawn gear. Current count: 227 distinct placeholder icons/models, 18 still on the shared placeholder icon.
- Final shared-placeholder model pass wired the last 18 placeholder item definitions away from `legacy_placeholder`. The 12 custom-brew plant target items plus `Biome Note` now use local FTB wiki scrape PNGs, while `Leonard's Urn`, `Lilith's Blood`, `Meaty Stew`, `Trapped Plant`, and `Warm Blood` use clear source-backed alias models until their real mechanics/art are implemented. That pass gave all then-remaining 243 placeholders non-shared item models; after the later `biting_belt`, `bark_belt`, `beartrap`, `wolftrap`, `blood_stained_wool`, `woven_cruor`, primary Vampire clothing, and exact Resurrected Vampire clothing alias promotions, 221 inert placeholder items remain and zero still use the shared placeholder icon.
- Koboldite art correction restored the user-approved live-backup icons for `Koboldite Pickaxe`, `Koboldite Dust`, `Koboldite Nugget`, and `Koboldite Ingot` from `docs/art/item-backups/2026-05-22-live-before-old-assets-restore`; keep these instead of the older restored koboldite sprites.
- Glowglobe render polish switched the placed block model from cutout to translucent so the original low-alpha `glow_globe` sprite can render as a soft spirit light again instead of behaving like a masked plant cross. The remaining task is in-game visibility verification.
- Kettle ceiling-hung pass added a `hanging` blockstate, restored the compact decompiled `hanging_kettle` model/texture path when a solid block supports the kettle from above, and kept the existing side-post support behavior for between-post setups. Remaining work is in-game verification of the two visual/support variants.
- Grassper held-item polish moved the stored-item render out of the plant model transform and back toward the original off-center petal-cup pose with a gentle spin. The one-item stash, comparator output, and state sync were already restored; remaining work is in-game visual verification.
- Rowan Door placement polish removed the forced right-hinge override so vanilla clicked-side and neighbor-aware hinge placement can run again while keeping the UUID key/keyring locking flow intact. Remaining work is in-game lock/unlock and facing verification.
- Death scythe attachment polish corrected the ported runtime pivot from the swapped `x=0, z=-0.8` values back to the original 1.7 `x=-0.8, z=0` pose, so the scythe should sit sideways at Death's hand instead of drifting forward. Remaining work is in-game angle/contact verification.
- Demon wing source-pose polish restored the wings to zero-depth source planes and removed the non-source idle z-flap so the demon now keeps the same static decompiled wing angle family as the correctly presented Imp. Remaining work is in-game wing-angle verification.
- Treefyd old visual parity polish kept the user-directed old version rather than the Resurrected variant: the active texture hash matches the original 1.7 64x32 asset, and the model now restores the old zero-width/depth petal, tongue, and crossed leaf planes plus original UV offsets. Remaining work is in-game visibility verification.
- Poltergeist mirror-UV parity polish kept the original 1.7 `poltergeist.png` asset and restored the old model's all-part `mirror=true` UV setup instead of mirroring only the left-side limbs. Remaining work is in-game wrong-mapping/holes verification.
- Banshee screaming mouth parity polish kept the original 1.7 `banshee.png` asset, removed the non-source GhostModel chin plate, restored the old flat zero-depth mouth plane, and synced Banshee screaming state so the mouth renders only during screams. Remaining work is in-game face/mouth verification.
- Mandrake model/UV parity polish kept the original 1.7 `mandrake.png` asset, restored all-part mirror UVs, put the leaf crown back to the old zero-depth plane, and removed the non-source head/leg walking in favor of the original arm-only twitch. Remaining work is in-game texture-hole verification.
- Nightmare attack-pose/tendril parity polish kept the original 1.7 `nightmare.png` asset, synced the 15-tick melee attack timer to the client render state, restored the old triangle-wave arm lift while attacking, and put the trailing tendrils back on the original mirror-UV, entity-id sway, and anchor-position setup. Remaining work is in-game attack-pose/tendril verification.
- Ent old model/mapping parity polish kept the original 1.7 `ent.png` asset, restored the source model's missing conditional face plate and inner leaf fill boxes, put all parts back on mirror UVs, restored animated root-leg walking and synced melee attack arm lifts, and brought back the old axe vulnerability plus knock-up throw behavior. Remaining work is in-game wrong-mapping/holes verification.
- Spirit translucent render polish kept the original 1.7 `spirit.png` asset and model geometry, restored the old renderer's translucent white alpha, zero shadow radius, and source translate/half-scale transform so the Spirit World spirit should render as a small wisp instead of an opaque yellow blob. Remaining work is in-game visibility verification.
- Flying Monkey source model/render parity polish replaced the scaled zombie renderer with a dedicated `ModelMonkey`-style 64x32 model, restored the wing/arm/leg/tail landed-vs-flight animation family, source shadow radius, and decompiled feather tint math. Remaining work is in-game wrong-model/texture-hole verification.
- Flame Imp attack-pose/powered render parity polish synced vanilla melee swing progress into the renderer, restored the old source right-arm punch/body twist, restored powered 1.5x vertical render scale, and returned the renderer to the source 0.5 shadow radius. Remaining work is in-game attack-pose/powered-height verification.
- Parasytic Louse potion-bite parity slice restored the old louse-plus-drinkable-potion crafting path in first practical form, carries `PotionContents` from item to placed louse and back when picked up, applies the stored potion effects once on a successful bite, then clears the payload. The renderer also returns to the source 0.3 shadow radius and 180-degree death flip.
- Parasytic Louse hotbar defense parity slice restored the old practical hotbar trigger: when a player takes damage, the first potion-fed louse in the 9-slot hotbar bites once, sends harmful effects to the living attacker or beneficial effects to the player, clears back to a plain louse, and costs the player 1 magic damage with a recursion guard. Remaining follow-up is in-game louse QA.
- Biting Belt potion-defense parity slice promoted `biting_belt` out of the inert placeholder roster, restored the source shaped recipe, added a belt-plus-potion recipe that stores up to two removable potion bites, made it wearable in the legs slot, and restored the old fallback defensive bite after hotbar lice fail. Remaining follow-up is in-game recipe/equip/defense QA plus visual-model polish.
- Bark Belt charge-defense parity slice promoted `bark_belt` out of the inert placeholder roster, restored its source shaped recipe from Biting Belt / Flowing Spirit brew / emerald / Ent Twigs / Creeper Heart, made it wearable in the legs slot, restored grass/mycelium recharge, and restored its charge-spending non-wooden entity-damage shield. Remaining follow-up is in-game recharge/damage QA plus HUD/model polish.
- Beartrap trap-block parity slice promoted `beartrap` out of the inert placeholder roster, registered it as a real low-profile placeable block/item, restored the source shaped recipe, added loot/model/blockstate resources, and restored the first practical old trap loop: one-second arming delay, living-entity snap shut, 4 damage, strong slowness, creative-player bypass, and manual open/close toggling. Remaining follow-up is in-game trap QA, dedicated open/closed visuals/sounds, and owner/hidden behavior.
- Wolftrap silvered trap parity slice promoted `wolftrap` out of the inert placeholder roster, registered it as a real low-profile placeable block/item using the shared Beartrap behavior, restored a modern practical version of the source upgrade recipe from Beartrap plus silver ingots, Null Catalysts, and live Wolfsbane flowers, and made the silvered variant ignore ordinary living entities while snapping shut on werewolf players. Remaining follow-up is in-game trap QA, dedicated open/closed visuals/sounds, owner/hidden behavior, and the full moon/sheep/Wolf Altar bait-summon tile-entity path.
- Creature traps Primer coverage added a new Witches' Primer entry for `Beartrap` and `Wolftrap`, with their restored crafting recipes, current arming/snap behavior, silvered werewolf-only behavior, and an honest note that the old full-moon sheep/Wolf Altar bait-summon path remains future tile-entity work.
- Wicker Man Primer layout coverage added a new Witches' Primer entry for Wicker Bundles, Bloodied Wicker Bundles, the exact seven-layer 20-block bloodied silhouette checked by the live matcher, and flint-and-steel Huntsman ignition. Remaining follow-up is in-game verification of the exact structure offsets/model pose rather than the pattern being hidden.
- Vampire clothing wearable recipe slice promoted `woven_cruor` and the primary `vampire_*` clothing family (`top_hat`, `dress_coat`, `dress_jacket`, `trousers`, `skirted_trousers`, `oxford_boots`, `helmet`, `chain_coat`, `chain_coat_ladies`) from inert placeholders into real wearable armor items with source/wiki recipes and Primer coverage. The remaining follow-up at that point was the old vampire-only Bone Needle -> Blood-stained Wool path, exact Resurrected alias decisions, in-game armor QA, and the old light/medium set bonuses.
- Blood-stained Wool vampire acquisition slice promoted `blood_stained_wool` from the inert placeholder roster into a real wool-like block item, wired Bone Needle use on placed White Wool for vampire players with the restored 125-blood cost, and updated the Primer path so Woven Cruor is now survival-connected. Because old vampire level progression is not represented in the port yet, this first practical pass gates by vampire state plus blood cost; remaining follow-up is exact level-gate parity, exact Resurrected alias decisions, in-game armor QA, and old light/medium set bonuses.
- Vampire clothing lethal-fire set bonus slice added a promoted-piece counter and restored the first practical source-backed fire-protection hook for vampire players: lethal fire damage can be reduced to leave the vampire at 1 health, with a 25% basic save from 3 light pieces or 2 medium pieces and a 75% extended save from all 4 light pieces. The Primer now describes the live thresholds; remaining follow-up after the later drink-boost and mesmerise slices is full old ability-selection/paralysis parity and in-game armor/fire QA.
- Vampire exact clothing alias promotion slice moved `vampire_veil_hat`, `vampire_coat`, `vampire_corset`, `vampire_chestplate`, `vampire_skirt`, `vampire_leggings`, `vampire_shoes`, `vampire_heels`, and `vampire_boots` out of the inert placeholder roster into real wearable armor items, wired them into the creative tab and set-bonus counter, restored the source recipes that exist in Resurrected, and updated translations away from `(Placeholder)`.
- Vampire clothing tooltip parity slice replaced the promoted vampire armor registrations with a dedicated item class that shows source-style set-bonus tooltip lines: light garments list faster drinking, fire resistance, mesmerise boost, and extended fire resistance thresholds, while medium garments list their 2-piece fire-resistance threshold.
- Vampire clothing drink-boost slice wired the existing two-light-piece set-bonus check into the current simplified vampire bite path, making bites draw 50% more blood when the boost is active, and updated the Primer/status docs so the remaining Vampire clothing behavior gap is now the mesmerise bonus plus deeper old ability timing.
- Vampire clothing mesmerise slice added a first practical sneak-right-click villager mesmerise for vampires: it spends 50 blood, applies a short paralysis-style slowness/weakness trance for 5 seconds, and uses the three-light-piece clothing set bonus to extend that duration to 8 seconds like the source's +3s bonus.
- Vampire clothing player mesmerise slice widened that practical mesmerise hook to players too, matching another source-valid target family while keeping ordinary player biting deferred. Full old selected-power UI, custom paralysis effect, and Village Guard behavior remain follow-up.

---

### Witch Hunter black-magic pressure (2026-05-22)

- Successful taglock curses, `Curse of Corrupt Poppet`, `Hell on Earth`, Voodoo Poppet use, and Vampiric Poppet redirection now call the restored `WitchHunterEntity.blackMagicPerformed` hook. The hook follows the original 10% notice chance, 2-minute delay, 100-tick check cadence, and 1% spawn roll in first practical form, spawning up to two hunters near the caster and remembering that caster as their target. `./gradlew.bat compileJava processResources` passes. Remaining fidelity gap: the pressure timer is session-state rather than the old persistent infusion NBT key, and the custom `theycome` sound is still approximated.

---

### Witch Hunter target parity (2026-05-22)

- `WitchHunterEntity` now keeps Coven Witch targeting and restores the broader old anti-supernatural target set: undead mobs, vanilla demonic mobs, the port's `DemonEntity`, vanilla witches, vampires/werewolves, and players carrying Voodoo/Vampiric Poppets. `./gradlew.bat compileJava processResources` passes. The old delayed black-magic hunter-spawn pressure remains a separate future parity slice.

---

### Broom ridden movement sync (2026-05-22)

- `BroomEntity` now avoids the ridden-flight client/server tug-of-war that could pull the player back toward the broom's spawn point. The controlling client predicts the old Witchery forward/pitch flight model, while the server accepts the normal controlled-vehicle movement packet instead of running a second independent ridden simulation. The client control path also reads synced Soaring/Owl state for broom-speed bonuses. `./gradlew.bat compileJava processResources` passes.

---

### Crystal ball, coffin item, leech chest (2026-05-22)

- `crystal_ball.json` block/item model — replaced yellow placeholder texture with `minecraft:block/purple_stained_glass` (translucent orb) + `polished_blackstone` (stand). `render_type: translucent` already present, `noOcclusion()` already set.
- `models/item/coffin.json` — replaced flat `item/generated` sprite with a 3D `block/block` box using `coffin_head.png` texture. UV regions calculated from CoffinModel texOffs offsets (lid top, side wall, end wall). Shows all 3 faces in GUI.
- **Leech chest open animation**: `startOpen(player)` now called in `LeechChestBlock.useWithoutItem`; `updateOpenCount` sends `level.blockEvent(id=1, param=openCount)` to sync open state to client; `triggerEvent` sets `clientOpenCount`; server ticker auto-closes after 200 ticks (10 s) of inactivity; client ticker animates `lidAngle`; `LeechChestModel.setLidAngle` rotates all 4 lid quadrants by `angle * HALF_PI` (yRot). Inside texture becomes visible once lid opens.

---

### Renderer fixes (2026-05-22)

- `WitchesOvenRenderer` — `state.yaw` changed from `facing.getOpposite().toYRot()` to `facing.toYRot()`; oven no longer faces 180° backward
- `PoppetShelfRenderer` — item y changed from `0.60f` to `-0.15f`; after ZP-180 flip positive local y = downward, so items were rendering below the floor; negative y places them above the shelf surface

---

### Item Review — Blocks pass (2026-05-18)

**Creative tab / registration**
- `mutandis` and `mutandis_extremis` added to creative tab (were registered but missing from tab)
- `witch_door` converted from plain `Block` to proper `DoorBlock(BlockSetType.OAK)` + `DoubleHighBlockItem`

**Rowan tree family**
- `rowan_log` changed to `RotatedPillarBlock` — now places in all 3 axes; added `rowan_log_horizontal.json` model and axis=x/y/z blockstate variants
- `rowan_leaves` changed to `RowanLeavesBlock extends UntintedParticleLeavesBlock` — proper transparency, leaf decay, falling particle (PALE_OAK_LEAVES placeholder)
- `rowan_planks` and `rowan_log/wood` added to `#minecraft:planks` and `#minecraft:logs` tags so vanilla plank recipes work
- `rowan_door` block models fixed from oak textures → `door_rowan_lower/upper` textures; item model fixed from oak_door → `ingredient.door_rowan`

**Ice blocks**
- `ice_door` BlockSetType changed from `IRON` → `OAK` (now opens on right-click); item model changed from oak_door → packed_ice texture
- `ice_fence` added to `#minecraft:fences` tag (fence connection logic)

**Plant tweaks**
- `glint_weed` now emits original full light level 15 and has its legacy flame ambience/spread behavior restored
- `leaping_lily` model no longer reuses glint_weed texture (now uses snowbell_stage_4 as placeholder)

**Directional BESR blocks** — all four now face the direction placed:
- `fetish_scarecrow`, `fetish_trent_effigy`, `fetish_witchs_ladder`: `FetishBlock` gains `FACING` property + `getStateForPlacement`; `FetishBlockEntity` stores facing in NBT; `FetishRenderer` uses custom `State` with `extractRenderState` to pass facing to `submit`
- `statue_goddess`: same pattern; reads facing from block state in `extractRenderState`

**Witch door models** — 8 new `witch_door_*.json` block models created from rowan door template; full blockstate with all facing/hinge/half/open variants

---

### Torment dimension (full spec implementation)
- Dimension type: `has_ceiling: false`, `has_skylight: true`, `fixed_time: 6000`, `ambient_light: 0.15`, red sky (RGB 200,80,80 fog 160,50,50), no mob spawns
- **TormentChunkGenerator** rewritten: air everywhere by default; 6 DFS maze levels (Y=10,25,40,55,70,85); old `GenerateMaze.display` footprint restored at origin x=-23/z=6; 31×31 recursive-backtracking perfect maze per level; per-world seed via `WorldGenRegion.getSeed()`
- **Maze blocks:** invisible Force walls/ceiling (6 tall), vanilla Bedrock floor/portal frame, 1% mycelium on passage floors, old portal chamber near x=8/z=68, and 3 old-style Refilling Chest rooms per level with chests placed at floor level
- **TormentPortalBlock** — `entityInside`: outside→enter Torment (random level), inside→95% return/5% more; 100-tick cooldown
- **TormentHelper** — `sendToTorment(player, level)`: entry at (8, entryY, 8), 3×3 clearing, save return pos; `returnFromTorment`: safe-block search, restore return pos
- **RefillingChestBlockEntity + RefillingChestBlock** — 27-slot chest; fills from `witchery:chests/torment` loot table on first placement; refills every 72000t (3600s) when empty; loot includes spectral dust, demon heart, infernal animus, torment scroll, gold, etc.
- **Hellgate rite** updated to use `TormentHelper.sendToTorment` (was scanning for safe Y at 0,0)
- **Force** block: invisible, solid, unbreakable (`strength(-1,3_600_000)`), non-suffocating/non-view-blocking, and old light opacity 0 behavior; **TormentStone** remains registered but no longer drives the legacy maze floor

### Spirit World (spec §1-19 partial)
- **Block-scan nightmare chance** (§7): Dream Weaver −50%, Flowing Spirit −10%×3, Wispy Cotton −10%×2, Demon Heart +35%×2, Fire +10%×3; modifiers only apply if Dream Weaver found
- **Demonic Nightmare state** (§13 partial): calculated on entry, tracked as transient `demonicNightmare`; entry message distinct
- **Transferable items on exit** (§8): Wispy Cotton, Disturbed Cotton, Mellifluous Hunger, Brew of Flowing Spirit, Subdued Spirit + Icy Needle transfer from Spirit inventory to Overworld on exit
- **Nightmare victim UUID** (§14): `setVictim()` assigns UUID; `NearestAttackableTargetGoal` uses victim predicate; despawns if victim gone/dead/>32 blocks
- **Defense timer spec-correct** (§16): 80 ticks in Spirit World / 40 outside (was always 40)
- **Flowing Spirit disables defense** (§16): `standingInFlowingSpirit()` check
- **Kill cooldown** (§12): `lastNightmareKillGameTime` recorded; 30s (600t) delay before respawn
- **Nightmare periodic respawn** (§12): `tickNightmareRespawn()` called every 100t for nightmare players; checks victim has no nearby nightmare, respects kill cooldown
- **Dynamic fog** (§5): `ViewportEvent.ComputeFogColor` in `WitcheryClientEvents`; Dream=pink, Nightmare=green, Demonic=red
- **Nightmare loot formula** (§18): `qty = random.nextInt(max(10-looting,5))==0 ? 2 : 1`

---

## Deferred (do not implement unless asked)

- Village additions (bookshop, guards, walls)
- Werewolf player model full replacement
- Flowing Spirit as proper fluid (currently plain block)
- Manifestation ghost rendering / style polish
- `Egassy` - future non-parity joke-villain idea; a farting antagonist for a later optional content/fun pass, explicitly lower priority than original Witchery mechanics and parity work
