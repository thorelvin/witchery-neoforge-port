# Next Tasks — Witchery NeoForge Port

Last reconciled: 2026-05-24 (parity scan merged; Sun Collector/Charged Quartz Sphere/Sun Grenade chain implemented; VampiricPoppetItem two-taglock fix; PlayerCompassItem; WitchHunter poppet disruption; villager witchery goal added; garlic grass-seed parity corrected; Garlic Garland facing/click-through/repulsion and vampire break cancel restored)
Full feature status: `docs/status/FEATURE_MATRIX.md`

---

## Current plan

- Treat the former temporary scan notes as merged into the main backlog below; keep any future temporary scans short and promote them here before they grow stale.
- Highest-value parity lane: vampire progression/counterplay, custom-brew action parity, machine automation/exactness, transformation progression, and village/structure/entity gaps.
- Keep placeholder items inert until a family gets a real mechanics/recipe/alias decision, then update `ITEM_REVIEW.md`, `FEATURE_MATRIX.md`, and the Primer together.

---

## Parity scan backlog merged 2026-05-24

The former standalone scan findings have been condensed here so the main plan carries the current repo truth. This section is now the actionable backlog.

### Status corrections to carry forward

| Area | Current truth | Follow-up |
|---|---|---|
| Custom Brew system | Partial. Foulness/Foaming/Poison tier/dispersal slice is real, but old dynamic cauldron action composition is not. | Add composed brew action registry, generic brew bottle/bucket/bag support, moving gas/liquid brew behavior, Endless Water/Combustion, and the missing old action roster. |
| Vampirism | Partial. Current boolean vampire state, blood drain, sunlight, bite, mesmerise, coffin sleep, and clothing hooks are useful slices, not the old 10-level chain. | Add level storage/caps, Glass Goblet -> Elle/Lilith acquisition, Observations/Torn Page loop, cure rite, Sun Collector/Grenade, Blood Crucible/Blood Power, Resist Sun, Zombie Respect, and warm/Lilith blood semantics. |
| Lycanthropy | Partial. Current werewolf render/buffs/mastery/silver hooks are not the old Wolf Altar questline. | Add 10-level Wolf Altar quests, Moon Charm/form selection, Horn of the Hunt/Huntsman step, Wolfsbane/silver tuning, and full-moon Wolftrap sheep curse-spread behavior. |
| Machines | Partial despite several "Done" rows. Blocks, GUIs, and recipe slices exist, but exact automation/source behavior still has gaps. | Prioritize Witches' Oven allowlist/byproducts/funnels, Kettle bottle automation/bonus rolls, hopper QA for restored Spinning Wheel/Distillery/Brazier automation, and Silver Vat stored byproduct behavior. |
| Mirror World and Torment | Implemented/verify. Mirror World portal/room loop and Torment maze are live, but dimension chunks and stale docs can mislead QA. | Use fresh/deleted dimension folders for QA; reconcile Torment chamber scaffolding and Refilling Chest loot/break behavior with repo truth. |

### High-value implementation backlog from the scan

| Family | Next bounded tasks |
|---|---|
| Vampire counterplay and quest chain | ✅ Garlic/garland grass-seed, recipe, block, facing/click-through shape, vampire repulsion, and break-cancel punishment done. ✅ Sun Collector block (6000-tick sunlight charge), Charged Quartz Sphere item, Sun Grenade throwable done. Next: Glass Goblet blood capture, Observations/Torn Page/Bookshop loot, Lilith/Elle chain, Blood Crucible block/BE, and Blood Power charges. |
| Transformation counterplay | Tie Wooden Stake, garlic, Sun Grenade, Hunter Dawn armor, Weaken Vampires brews, Silver Sword/Bolt, Silvered Hunter armor, Wolfsbane, Moon Charm, and Wolftrap into explicit vampire/werewolf QA rows. |
| Poppets and taglocks | ✅ Taglock+poppet recipes done. ✅ Empty Taglock Kit detection fixed. ✅ Two-taglock Vampiric Poppet + PoppetEvents redirect bug fixed. ✅ Voodoo throw/needle/lava/drowning done. ✅ WitchHunter poppet disruption done. Remaining: verify Poppet Shelf Compass (already registered), verify mob-taglock poppet binding works end-to-end in-game. |
| Scrying and waystones | Crystal Ball now has nearby-player subject selection for ordinary predictions, source-style `LastUsedTime` recharge with the old strict `elapsed > 100` gate for the normal prediction path, a first bound-Waystone scry hook using the old same-dimension/150-block focus constraint, and the old 1200-tick held-use lifecycle with 20-tick refresh feedback. Waystone scrying follows the old item route by consuming altar power without applying the normal ball recharge gate, Waystone helpers now only treat stacks as bound when all coordinate and dimension data is present, and newly bound Waystones now cap at 1 and stamp the old uncommon rarity while blank Waystones keep their 16-stack common-item behavior. Remaining: old PredictionManager outcomes, real remote camera packet/view, Taglock scrying, Player Compass binding/tracking, online/same-dimension target rules, and a canonical blank/bound/blooded Waystone identity decision. |
| Infusion, coven, and familiars | Keep infusion charge/rite foundation, but add Witches Hand active ability families, Mystic Branch symbol magic, infernal enthrall/sacrifice creature powers, coven recruitment/quest roster, Seer Stone own-coven support, and missing familiar edge behaviors. |
| Hobgoblins and patrons | Split merchant/hut foundation from old labor/village behavior; add real mining/gathering/breeding/raid behavior, Koboldite Pickaxe miner bonuses, bound Patron worship, Gulg/Mog bosses, and Gulg/Mog reward gear mechanics. |
| Villages, structures, and entities | Expand "Village additions" into Bookshop, Guard Tower/Town Keep, Town Wall, Village Guard, mayor/cot/vampire feeding support, Stone/Coven Circle, and Straw Man. Add missing boss/NPC rows for Baba Yaga, Leonard/Lost Souls, Gulg/Mog, vampire/werewolf/were-villager mobs, Minedrake, summoned undead, and illusion mobs. |
| Machine exactness | Spinning Wheel: Hay Bale golden thread route, 8-string web route, and legacy sided automation are restored; remaining work is hopper QA. Distillery: legacy sided automation is restored; remaining work is hopper QA. Brazier: legacy ingredient-only automation is restored; remaining work is hopper QA. Oven/Funnel: reject ore smelting, restore food/wood Foul Fume, top funnel speed-only, filtered funnel block. Kettle: bottle automation, bottle/jar rules, and altar-source decisions. Silver Vat: real stored Silver Deposits/Dust byproduct BE. |
| Torment/chest polish | Decide whether Refilling Chest intentionally uses vanilla simple-dungeon loot or the custom `witchery:chests/torment` table, then align docs/code. Decide whether unbreakable/no-drop is intentional anti-cheese polish or should return to old high-resistance chest behavior. |

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
| Legacy placeholder items | 195 source/wiki/decompiled item IDs remain registered and visible as inert creative-tab placeholders after the belts/traps/vampire clothing/contracts/wearables/diviners/garlic/dawn-gear/custom-brew promotions. All remaining placeholders point at non-shared source/wiki-backed item models or clear alias models; none still use the shared placeholder model. Implement mechanics/recipes or alias-remaps one family at a time | Varies |
| Items 62–153 | Tools & Items, Armour, Brews, Infusion, Spawn Eggs — untested | Varies |

---

### 1. Spirit World — ✅ Complete (2026-05-23)

All spec gaps are closed:
- Vegetation worldgen connected: grass, tall grass, mushrooms, dead bush, oak trees, wispy cotton, and disturbed cotton are now placed in the Spirit World biome via the VEGETAL_DECORATION step (the placed features existed but the biome features arrays were empty)
- Ghost layer upgraded to `LightTexture.FULL_BRIGHT` so manifested players glow in darkness
- Circle Magic is now blocked in the Spirit World; only Rite of Manifestation (spectral_dust on otherwhere circle) is permitted — matches original Witchery spec
- All other §1-§23 spec items were already done (see FEATURE_MATRIX.md)

---

### 2. Mirror World dimension - implemented, needs fresh-world QA/reconciliation

Original: `hasNoSky=true`, permanent dark, dark blue fog `(0, 0.03, 0.1)`, no rain/lightning.
Current repo truth has the portal-room loop landed: Mirror block, Mirror Wall, room chunk generator, room SavedData, entry/exit teleport, Reflection guardian, and owner-skin renderer.
- **QA:** test in a fresh world or delete `run/client/saves/<world>/dimensions/witchery/mirror_world/` before judging room generation.
- **Follow-up:** survival/ritual ingress wording, Duplication Grenade mirror charging, Reflection behavior polish, and stale-chunk/player-skin edge cases.

---

### 3. Custom Brew system - partial

The Foulness/Foaming/Poison tier slice is implemented across liquid/splash/gas variants with `witchery:brew_power`, thrown brew sync, and scalable effects. That is useful progress, but not the old full custom brew system.

Open parity:
- Dynamic cauldron action composition and serializers/effects.
- Generic brew bottle, splash, lingering, bucket/fluid, Brew Bag, Endless Water, and Combustion containers.
- Moving gas/liquid brew block or a documented modern replacement.
- Many old action effects such as tree growth, blight, transpose, undead/insect/demon bane, drain magic, steal buffs, and summon Leonard.

---

### 4a. Witchery against villagers — goal

**Goal:** make villagers meaningful targets across all Witchery systems — taglocks, poppets, vampire feeding, ritual sacrifice, and werewolf infection.

This goal is not about one file; it is a cross-cutting vertical slice. Track each sub-task below and tick it off when done.

#### Sub-tasks

| Sub-task | Status | Notes |
|---|---|---|
| Taglock Kit on villagers | Needs verify | `interactLivingEntity` in TaglockKitItem uses `LivingEntity` path for mobs; should already bind villager UUID + entity type. Test: right-click villager with empty kit; verify kit shows villager name/UUID. |
| Voodoo Poppet bound to villager | Needs verify | `VoodooPoppetItem.resolveBoundEntity` calls `ServerLevel.getEntity(UUID)` for mob-bound poppets. Test: bind poppet to villager, use throw/needle/lava, verify villager takes effect. |
| VampiricPoppetItem — villager as victim | Needs verify | `interactLivingEntity` accepts any LivingEntity as victim target; villagers are LivingEntity. Test: bind source to self, bind victim to villager, take damage, verify villager receives redirect. |
| Vampiric blood feeding from villagers | Not started | Vampires currently bite only players. Add sneak-right-click on Villager: spend 25 blood to mesmerise 5 s then drain 25–50 blood at mesmerise end. Gate by vampire state and ≥ 25 blood cost. Uses existing mesmerise lang key. |
| Ritual sacrifice — villager offering | Needs verify | Hell on Earth, Wither summoning, and Demon summoning already specify `VillagerEntity` sacrifice. Test each rite with a caged villager on-site. |
| Curse rite targeting villager via taglock | Needs verify | `Curse Creature` resolves taglock UUID via `ServerLevel.getEntity`. Test: fill taglock from villager, place it in medium infernal circle, verify villager receives Wither/Slowness/Weakness. |
| Blight villager zombification | Needs verify | Blight rite's expand loop zombifies villagers. Test: cast medium infernal Blight near a village, verify villagers turn to zombie villagers as the annulus grows. |
| Werewolf infection of villagers | Not started | When a werewolf player kills a villager on a full moon, small chance (10%) to convert them into a Were-Villager hostile mob (zombie villager skin + wolf head overlay as a placeholder). Blocked on Were-Villager entity/renderer — track as separate entity task if scope grows. |
| Player Compass — scry to named villager | Deferred | Player Compass only binds to players currently. Villager support would need a non-player name resolution path. Defer unless requested. |

#### Why this matters

Vanilla villages are a rich social target that the original Witchery treated as a living ecosystem: vampires fed from villagers, witches cursed them, werewolves infected them, and ritual casters sacrificed them. Making the current port respect that ecosystem makes the open-world feel of Witchery land properly — players can run a coven near a village and use it both as a resource and a risk.

#### Suggested implementation order

1. Verify the three "needs verify" rows first (no code, just QA) — they may already work.
2. Implement vampiric blood feeding from villagers (small, contained change to vampire bite handler).
3. Implement werewolf villager infection (full moon kill → zombie villager placeholder) as a small `LivingDeathEvent` hook.
4. Were-Villager mob is a separate larger entity task; add it to the entities backlog if requested.

---

### 4b. Debug command: add `spectral` to `/witchery familiar` suggestions — Done

`"spectral"` added to `FAMILIAR_TYPES` suggestions. `setFamiliar` handler now accepts `owl`, `cat`, `toad`, and `spectral` — stores the type via `setFamiliarType`, then calls `FamiliarHelper.restoreBoundFamiliar` to spawn the entity near the player. `"none"` still clears. Unknown types get an error message listing valid options.

---

### 4c. Small code quality items (deferred — after parity complete)

These are individually small but accumulate maintenance debt. **Do not action until all major parity work (Mirror World, Custom Brew, Spirit World gaps) is stable** — they carry non-zero risk of altering observable behavior mid-sprint.

- **`ConcentratedBatBallRecipe.assemble`** calls `matches(input, null)`. Current `matches` ignores the level so it works, but passing `null` into a `Level`-typed API is brittle. Extract a private pattern-check method called from both `matches` and `assemble`.
- **`TaglockKitItem.isPlayerTaglock`** returns `true` on unfilled stacks because it only checks whether there is no entity type tag. Should be: `return isFilled(stack) && getBoundEntityTypeId(stack).isEmpty()`.
- **`MutatingSprigItem`** repeats the same "spawn effects + damage held item + return consume" block for every successful transform. Extract a `finishUse(ctx, pos)` helper. Also, `tryCreateWormwood` checks for Wispy Cotton but calls `clearHorizontalMoss` — rename to `clearHorizontalRing` to match its actual behavior.
- **Registry classes** contain many hard-coded `"witchery"` string literals. An `id(String path)` helper would reduce copy/paste mistakes across blocks, entities, dimensions, and worldgen.

---

### 4d. RitualExecutor decomposition (deferred — after parity complete)

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
- **Legacy placeholder follow-up** - `ITEM_REVIEW.md` now has a grouped TODO roster for the 195 remaining inert placeholder items, including exact 1.12 Resurrected alias IDs that no longer silently disappear. Highest-value follow-up is to pick one family at a time: charms, bolts, hobgoblin rewards, custom-brew containers, block-like items, or exact-alias remaps.

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

See `docs/archive/CHANGELOG.md` for older session history.

### Garlic grass-seed parity (2026-05-24)

- **Grass/fern seed table** now matches the Resurrected Witchery reference weights for Witchery crops: Artichoke 3, Belladonna 4, Mandrake 5, Snowbell 2, Wolfsbane 1, and Garlic 1. Garlic is no longer only worldgen/crop propagated, and the stale garland-recipe TODO was corrected because the live recipe already uses the old 5 garlic + 3 string shape.

### Codex parity batch (2026-05-24)

A focused source-behavior restore pass across 32 commits:
- **Machines automation**: Spinning Wheel, Distillery, and Brazier now all support sided hopper/pipe automation per original source slot rules.
- **Crystal Ball scrying**: waystone scry hook (same-dimension/150-block focus constraint), nearby-player prediction subjects, `LastUsedTime` recharge with strict `elapsed > 100` gate, old 1200-tick held-use lifecycle, and 20-tick refresh feedback.
- **Bound Waystone**: stack cap (1 when bound, 16 when blank), complete-binding requirement before treating as bound, old uncommon rarity on bind.
- **Circle Talisman**: floor deployment restored, reset behavior, ring visualization in tooltip, old uncommon rarity on stored circles.
- **Item presentations**: Arthana uncommon rarity, Seer Stone enchanted glow, Spectral Stone glint, rare magic item family.
- **Broom**: enchanted broom spawn flag restored on enchantment rite.
- **Icy Needle**: original use cost restored.
- **Sleeping entry guard**: sleep-to-Spirit-World safety check restored.
- **Binky**: summoned name restored on skull right-click.
- **Dream Weave**: wall placement using clicked face restored.
- **Rowan Door Key**: UUID lookup fix.
- **Witch Hunter**: source stats restored (30 HP, 0.25 speed, 9-damage cap, periodic Slowness self-clear, villager hurt/death sounds).
- **Ent**: source movement speed (0.4) restored; non-source blanket damage cap removed.
- **Demon**: source combat profile restored.
- **Spectre**: source combat restored (extra 2 armor, 32-block follow range, armor-bypassing magic melee ≥15% target max health).
- BUILD SUCCESSFUL (all 32 commits).

---

### Sun Collector chain + VampiricPoppet fixes (2026-05-24)

- **SunCollectorBlock + SunCollectorBlockEntity** promoted from legacy placeholder: BaseEntityBlock with server-ticking charge loop. Right-click with Quartz Sphere inserts it; empty right-click retrieves; drops stored item on break. Charges during daytime (sky access, no rain, dayTime < 13000) at 1 tick/game-tick; after 6000 ticks in sunlight outputs Charged Quartz Sphere.
- **charged_quartz_sphere** new item (foil enchant glow). Item model/assets added.
- **Sun Grenade recipe** updated: 4 gold nuggets + 1 Charged Quartz Sphere → 2 grenades. Full chain: Quartz Sphere → Sun Collector (6000 daylight ticks) → Charged Quartz Sphere → Sun Grenade.
- **Sun Collector recipe** added: Quartz Sphere center + glass + quartz surround.
- **VampiricPoppetItem** fully rewritten with two-taglock semantics (source = witch protected, victim = damage absorber). `PoppetEvents.onVampiricRedirect` was broken — `matchesBound` found poppets where TAG_UUID == damaged player, then looked up that same UUID and bailed because target==player. Fixed: now finds poppets where TAG_UUID == damaged player (source match), then redirects to TAG_VICTIM_UUID.
- **PlayerCompassItem** promoted: right-click player → attuned; right-click air → shows 8-point compass direction + block distance. Offline/other-dimension messages. Shapeless recipe: compass + taglock_kit.
- **WitchHunter poppet disruption** added to PoppetEvents: 30% chance on WitchHunterEntity hit to destroy a bound ProtectionPoppetItem from inventory or shelf.
- Placeholder count: 195 → 192.
- BUILD SUCCESSFUL.

---

## Deferred (do not implement unless asked)

- Village additions (bookshop, guards, walls)
- Werewolf player model full replacement
- Flowing Spirit as proper fluid (currently plain block)
- Manifestation ghost rendering / style polish
- `Egassy` - future non-parity joke-villain idea; a farting antagonist for a later optional content/fun pass, explicitly lower priority than original Witchery mechanics and parity work
