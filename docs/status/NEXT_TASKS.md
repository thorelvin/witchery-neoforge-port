# Next Tasks — Witchery NeoForge Port

> Per-session "local delta" history is rotated into `docs/archive/CHANGELOG.md`
> (older entries through 2026-06-02). Keep this file focused on upcoming work; add new
> session deltas briefly at the top, then sweep older ones into the changelog.

Full feature status: `docs/status/FEATURE_MATRIX.md`

Current local delta (2026-06-02): Last placeholders completed — `LEGACY_PLACEHOLDER_ITEMS` is now empty. `wolf_head` and `hellhound_head` were promoted to real mounted-head blocks (ported from 1.7.10 `BlockWolfHead`, skullType 0=wolf / 1=hellhound): new `WolfHeadBlock` + `HellhoundHeadBlock` (thin subclass for its own codec/texture flag), `WolfHeadBlockEntity` (stores floor rotation), and BESR `WolfHeadRenderer` + `WolfHeadModel` (the vanilla-wolf-head geometry — head 6x6x4, two ears, snout, 64x32). Floor/wall placement, floor rotation, VoxelShapes, and inventory-flat-icon all mirror `AlluringSkull`. Wolf reuses the vanilla wolf texture; hellhound uses `textures/entity/hellhound.png` (copied from 1.7.10). Registered: blocks, one shared block-entity type, block items, BE renderer + model layer, blockstates (6 FACING variants), particle block models, self-drop loot tables, `block.witchery.*` lang names, creative-tab entries. The other four IDs were removed rather than faked: `brew_bottle`/`splash_brew_bottle`/`lingering_brew_bottle` are Resurrected generic NBT brew CONTAINERS the port replaces with individual `brew_of_*` items, and `trapped_plant` was an invented ID with no source item. `compileJava processResources` clean; all JSON validated. Fresh QA: place both heads on floor and walls, rotate, break (self-drop), and confirm the wolf/hellhound textures render in-world plus the flat icons in inventory.

Current local delta (2026-06-02): Legacy placeholder roster de-scarified and de-duplicated. The stale "107 placeholders" figure was counting the whole `ITEM_REVIEW.md` review roster (mostly already promoted), not the live registry. The live `LEGACY_PLACEHOLDER_ITEMS` set is now stated accurately: **six** genuinely inert IDs, enumerated one by one in `ITEM_REVIEW.md` and the legacy-placeholder bullet below — `hellhound_head`, `wolf_head` (decorative mounted-head blocks, need head block + BESR), `trapped_plant` (trap block), and `brew_bottle` / `splash_brew_bottle` / `lingering_brew_bottle` (Resurrected empty brew-container IDs, need the cauldron brew-into-bottle flow). Cleaned out the four `brew_of_soul_*` placeholders as duplicates of the real `hunger_demon_soul` / `anguish_demon_soul` / `fear_demon_soul` / `torment_demon_soul` ingredients ("Soul of X Demon"): removed their registry entries, creative-tab group line, lang strings, and orphaned item-model JSONs. `compileJava processResources` clean; lang JSON validated.

Current local delta (2026-06-02): Mystic Branch symbol magic implemented. The branch is no longer a creature-power stand-in; it is now the source air-glyph casting wand (`ItemMysticBranch` + the `infusion.infusions.symbols` system, 1.7.10-0.24.1). New `com.emoniph.witchery.symbol` package: `StrokeSet`, `SymbolEffect`, `SymbolEffectProjectile`, and `EffectRegistry` with the iconic non-knowledge spells at their exact source stroke sequences — Accio, Confundus, Episkey, Expelliarmus, Flipendo, Impedimenta, Incendio, Lumos, Nox, Stupefy. Hold use to channel (`ItemUseAnimation.BLOCK`, 36000-tick duration); the client (`client.SymbolDrawState`, driven from `WitcheryClientEvents.onClientTick`) tracks pitch/yaw flicks into stroke bytes (up=0/down=1/left=2/right=3, 7-degree threshold, max 15), and sends a C2S `SpellPreparedPayload` when a symbol is recognised. Releasing casts the prepared spell on the server: validates infusion presence (`hasValidInfusion`), spends `floor(2^(level-1)*baseCost)` infused energy, and runs the effect — projectile spells launch a new `SpellEffectEntity` (ThrowableProjectile, NoopRenderer + witch-particle trail) that resolves `onCollision`; instant spells (Episkey/Nox) run directly. Polish: action-bar symbol-name readout while drawing, recognition chime, evoker/illusioner cast sounds, impact particles, snare + red feedback on unknown-symbol / not-infused / no-energy, and a usage tooltip. Transient `preparedSpellId`/`preparedSpellLevel` added to `WitcheryPlayerData` (not in CODEC). Altar-top placement is preserved. Deliberate port simplifications vs source (documented in PORTING_NOTES): no Worship-potion level cap (drawn stroke length picks the level directly), no Beast-infusion-gated unforgivable curses (Crucio/Imperio/Avada Kedavra not registered yet), and no persistent spellbook/cooldown (only knowledge-free spells in this slice). `compileJava processResources` clean. Fresh QA: get infused, hold-use the branch, flick a symbol (e.g. Lumos = down,down,right), release, and confirm the cast plus energy spend; verify the action-bar name and fail messages.

Previous local delta (2026-06-02): Structure goto QA fixes. (1) `/witchery goto straw_man` could teleport to a located Straw Man that never placed any blocks: `StrawManPiece.hasDryLandFootprint` aborted the whole effigy whenever `WORLD_SURFACE > OCEAN_FLOOR` (true over any snow layer, grass, or flower) even on dry land that the structure-level `findGenerationPoint` had already approved. That redundant guard is removed; the explicit fluid checks plus the structure water/biome guards still keep it off water. (2) and (3) Coven/Stone Circle and Straw Man both generated one block too high (a raised dirt/grass plinth), because `getFirstOccupiedHeight` returns the air block above terrain here while both pieces anchored the floor at that Y. Both structures now anchor the piece at `groundY - 1`, so the floor/ground layer lands flush and the standing stones rest on the surface. Fresh QA should `/witchery goto straw_man` and confirm an actual effigy appears flush with terrain, and `/witchery goto coven_circle` (or `stone_circle`) and confirm the ring floor is flush rather than on a plinth.

---

## Current plan — 1.7.10 baseline active

Parity baseline is now `1.7.10-0.24.1`. All new work checks decompiled original Witchery first; 1.7.2 and Resurrected are secondary references only. Any existing task note that says "Resurrected-backed" or "1.7.2 source set" should be re-verified against 1.7.10 before that task is actioned.

Five implementation lanes — work one bounded slice at a time per lane:

1. **Custom Brew / Witch's Cauldron** — first priority after parity matrix is built. Source: `WitcheryBrewRegistry.java`, `BlockCauldron.java`, `TileEntityCauldron.java`, `BrewAction*`, `DispersalInstant/Liquid/Gas/Triggered`, 44 potion effect classes. Open: dynamic cauldron action composition, generic brew containers, gas/liquid brew blocks, remaining action roster (tree growth, blight, transpose, undead/demon bane, summon Leonard).

2. **Vampire / Werewolf progression** — Elle/Lilith chain ✅ ported, Blood Crucible block/BE/render/fill/level-10 ultimate selection plus source recipe are live, Glass Goblet drained Villager/Village Guard conversion now spawns the Vampire mob, and source Zombie Respect targeting for level-10 vampires is restored. Moon Charm now covers the first practical manual werewolf transformation-control path. Remaining: broader Blood Power, Resist Sun, exact werewolf Wolf Altar quests and wolf/wolfman level gates, Horn of Hunt/Huntsman step, cure rite, levels 8–10 full gates, and exact guard passive blood-regeneration/village-side-effect polish. Source: `ExtendedPlayer.java`, `Shapeshift.java`, infusion package, `EntityVampire.java`, `EntityWolfman.java`, `EntityVillagerWere.java`.

3. **Village / mobs / bosses / structures** - major recent progress: Village Guard, Wolfman/WereVillager, Vampire, Baba Yaga, Leonard/Lost Souls, Gulg/Mog, village apothecary/bookshop/source-sized witch hut injection, Watch Tower, Keep, first-pass town walls, first-pass Coven Circle, source-shaped Hobgoblin Hut, first-pass Straw Man, source villager-damage protection from Witchery guards/hunters, and source `TownZombieAttackReductionMode` villager protection are live. Next village slice: fresh-village QA for walls/buildings and keep frequency after the house-pool weight increase. Broader remaining structure/entity parity: Coven Circle/Straw Man fresh-world QA, Stone Circle naming, Minedrake, illusion mobs, summoned undead, any optional/non-source Baba Yaga hut decision, and later Witch Forest additive work.

4. **Machines / storage exactness** — hopper QA for Kettle/Oven/Distillery/Spinning Wheel/Brazier; Silver Vat furnace/hopper QA; any remaining source-behavior edge cases.

5. **Dimensions / world feel** — Spirit World fresh-chunk QA, Mirror World polish, Torment chest feel, exact dimension fog-distance if a concrete source hook surfaces.

Small machine/storage QA fixes run in parallel when clearly bounded. Pull from the QA queue below before broader backlog work unless a new blocker appears.

---

## High-priority user QA/fix queue added 2026-05-25

These are the current top user-reported parity and visual blockers. Treat each row as a bounded candidate task: inspect original 1.7.10 Witchery first, compare the current port, patch the smallest truthful slice, run `.\gradlew.bat compileJava processResources`, update docs, commit, and push after the fifth local commit.

| # | Area | Requested fix / report | Notes |
|---|---|---|---|
| 1 | Anointing Paste | ✅ Classic recipe and use path confirmed. | The item, model, lang, Patchouli entry, loot seed, and `WitchCauldronEvents` right-click conversion were already live. The remaining mismatch was the crafting route: `witchery:anointing_paste` now matches the FTBWiki/local scrape and Resurrected reference by using Water Artichoke, Mandrake, Belladonna, and Snowbell seeds for 4 paste. Deeper dynamic cauldron brewing remains separate backlog, not an Anointing Paste blocker. |
| 2 | Garlic Garland | ✅ Placed Garlic Garland pink/black render fixed. | The block model now uses a block-atlas `witchery:block/garlic_garland` texture copied from the live garland sprite instead of referencing `witchery:item/garlic_garland` from a placed block model. Future polish can still replace the cross-plane stand-in with a true source-style 3D garland model. |
| 3 | Kettle | ✅ Original Witchery texture/model path restored for live Kettle states. | The live `kettle.png` already matched the original 1.7 asset byte-for-byte. This pass removed the Resurrected-only `hanging_kettle` model/texture path and the unused `kettle_item` fallback, so side-supported, ceiling-supported, and item views now all use `witchery:block/kettle` with the original texture. Remaining Kettle work is in-game support/liquid/bottle visual QA, not borrowed-asset cleanup. |
| 4 | Silver Vat | ✅ Empty/filled visual state and 3D item presentation reconciled. | Replaced the old `minecraft:block/template_cauldron_full` stand-in with a custom Silver Vat model. The block entity now drives a `has_deposits` blockstate, so visible silver cubes only render when its stored `Silver Deposits` stack is non-empty; no-update extraction now uses the same sync path, preventing stale filled visuals after the stored stack is cleared. The item model remains a 3D block parent and the stale flat `silver_vat.png` item sprite was removed. Remaining work is furnace/hopper QA and final exact-source renderer polish. |
| 5 | Coffin | Done - GUI item restored to the original-style flat PNG instead of a 3D model. | Uses the local FTB/user-matching `Grid_Coffin.png` as `textures/item/coffin.png`; placed Coffin block rendering remains on the existing two-block BESR path. |
| 6 | Wolftrap / Beartrap | Patched - source renderer path, startup-safe special renderer codecs, hidden-owner opacity/outline cancel, exact source outline footprint, source trap sounds, snap particles, Wolftrap no-drop branch, creative-player trigger behavior, source bite damage, owner-profile persistence, and Wolftrap bait timer foundation restored. | Placed traps now use a block-entity renderer ported from decompiled `RenderBeartrap`/`ModelBeartrap`, with particle-only block models, the old 32x32 `textures/block/beartrap.png`, and the source 1.309-radian closed-jaw rotation instead of static JSON cuboids. Their selectable outline now matches the decompiled source AABB (`0.2..0.8` X/Z and `0.01..0.1` Y in block units) while collision stays empty. Closed owned non-silver Beartraps render translucent to non-owners using the source-default 30% `traps.mantrapOpacityPercent` config; owners and Wolftraps stay opaque. The source `BlockBeartrap.checkForHiddenTrap` client behavior is also restored through the modern block-outline extraction event, so non-owners no longer get the normal selection outline on hidden closed Beartraps. Beartrap and Wolftrap inventory views use the same model through distinct special renderer codecs, matching the old `builtin/entity` item-model intent while avoiding NeoForge startup duplicate-codec rejection. The Resurrected `block.bear_trap.click` and `block.bear_trap.mantrap` sound events/assets are registered and used for manual toggles and trap snaps; snaps also send the old redstone particle burst. The silvered Wolftrap follows source `getDrops` behavior by dropping nothing when broken, while Beartrap still drops itself. Creative players match the decompiled source branch by still closing/sounding the trap while skipping only the snare effect. Trap bites use the modern `out_of_world` damage source, matching the old `DamageSource.outOfWorld` branch instead of generic damage. New placements create a trap block entity and persist the placing player's UUID/name, matching the source `TileEntityBeartrap.owner` foundation. Silvered open Wolftraps now tick the old bait gate every 200 ticks, require an adjacent Wolf Altar, a leashed sheep within 8 blocks, and a full-moon night, and persist `WolftrapStart`/`Wolf`; the final Werewolf summon/capture branch remains blocked on the missing Werewolf entity. Remaining work is in-game visual/audio QA plus the entity-backed Wolftrap summon/capture behavior. |
| 7 | Poppet Shelf | Patched - stale removed-poppet visuals and facing-aware slot centering corrected; needs in-game verification. | `loadAdditional` now clears all 9 client slots before loading the update tag, avoiding lingering render-state stacks when a slot disappears from synced NBT. `removeItemNoUpdate` also marks/syncs changes. The top pass keeps the source 3x3 centers and now renders them inside the shelf's facing transform, with source-like spin only on Fancy/Fabulous graphics. |
| 8 | Dream entry position | Patched - entry now preserves exact source coordinates and removes copied bed blocks around the Spirit World landing point; needs in-game sleep verification. | The previous port scanned the copied Spirit World column from high Y down and landed on a terrain surface. `SpiritWorldHelper.enter()` now captures the player's exact pre-transfer X/Y/Z, loads that copied chunk, clears nearby copied bed blocks, and teleports to those exact coordinates before spawning any nightmare. |
| 9 | Torment refilling chest | Patched - fresh Torment maze generation now creates the custom Refilling Chest block entity for each chest-room block; needs fresh-dimension in-game verification. | The chest Y matches original 1.7/Resurrected (`origY`, with Bedrock under it), but the NeoForge chunk generator only wrote the blockstate. It now also attaches `RefillingChestBlockEntity`, and `/witchery verify torment` checks renderer/storage presence. Delete stale Torment dimension chunks before judging old worlds. |
| 10 | Torment portal | Source-aligned - old Torment portals usually return the player to the Overworld from inside Torment. | Original 1.7 and Resurrected both set inside-Torment portal collisions to END about 95% of the time, with only a small chance to send the player into another random Torment level. Keep this as QA/docs unless user intentionally wants a non-source progression redesign. |
| 11 | Arthana | Patched - source-like altar placed-item footprint, support/drop, facing, lift, overlong scale, and break particle fallback corrected; needs in-game verification. | The placed-item block now stores player-facing state like the old metadata path instead of using a position-hash rotation, uses the old thin 0.6-block footprint, requires a solid support center, and drops the stored stack when broken or when support disappears. The renderer uses the source rotation/lift and no longer applies the oversized scale that made the cleaned Arthana sprite read too long or vanish on the altar. Its blockstate now bakes a particle-only model with the old source-style iron-block fallback particle instead of `minecraft:block/air`, so breaking altar-placed Arthana should no longer spawn pink/black particles. The Arthana sprite still uses original 1.7 solid pixels with alpha-1 padding cleared instead of flattened. Verify shift-click altar visibility, break particles, and held/GUI length in-game. |
| 12 | Spirit World / Nightmare | Patched - Spirit World biome source, passive baseline animals, native spirit spawn category, ambience, source-style fog/sky/cloud rendering, and a server-side data verifier are back on the old-style path; needs fresh-chunk/in-game verification. | The dimension now uses a fixed `witchery:spirit_world` biome instead of the Overworld biome preset, so the biome's grass, tall grass, mushrooms, dead bush, oak, cotton features, colors, and spawners are reachable. Source-era peaceful baseline animals (sheep, pigs, chickens, cows) now spawn through the creature bucket; biome-specific/newer animals are intentionally not added to the fixed biome yet. Native Spirits are registered/spawned as ambient, the duplicate cotton biome modifier is gone, the non-source soul-sand ambience is removed, and dimension `ambient_light` is restored to `0.0`. Client fog and sky render state now share the old dream/nightmare/demonic RGB bases plus fixed celestial-angle falloff, Spirit sky suppresses sunrise and stars, the dark-disc horizon check uses old horizon `0.0`, and vanilla clouds are skipped in Spirit World like Resurrected's empty cloud renderer. `/witchery verify spiritworld` now checks the loaded dimension JSON, fixed biome source, biome colors, animal/Spirit spawn buckets, and vegetation/cotton feature list for QA. `/witchery nightmare` still has the prior forced entry/state patch. Remaining work: fresh-chunk vegetation/spawn QA and exact fog-distance polish only if a concrete old hook surfaces. |
| 13 | Broom movement | Patched - source-like tick order, player-control vehicle hook, rider-facing visual yaw, modern vehicle authority, passenger-turn yaw sync, rider-yaw render-state fallback, item/entity data round-trip, and source bristle tinting restored; needs in-game flight/visual/break verification. | The first fix stopped pure client prediction by letting the server advance ridden brooms too, but once `getControllingPassenger()` was restored to return the first living passenger like `Boat`, Neo/Minecraft began treating player-ridden brooms as ordinary locally controlled vehicles. The vehicle-authority pass removes that double-authority path: ridden brooms now run the source-derived acceleration/move/damp pass only when locally controlled, while server-side player-ridden brooms accept the normal vehicle movement packets instead of doing a second movement pass. Rider-facing visual yaw now syncs entity/body/head yaw after movement, during passenger placement, and when the mounted passenger turns; the renderer also forces the mounted broom render state to the rider's interpolated yaw, so the model should no longer preserve the placed body orientation if entity body-rot sync is stale. Riderless brooms continue to drift server-side. The 2026-05-31 lifecycle slice transfers custom item names to placed brooms and back, stores/persists source `BrushColor` custom data, accepts dyes on placed brooms, and makes damaged brooms drop the matching normal/enchanted broom item instead of being permanently invulnerable. The follow-up bristle layer now applies the source dye table to the bristle strands only, defaulting unset brush color to brown like old `ModelBroom`. |
| 14 | Enchanted Broom | Patched - GUI bristle tail overlay restored and placed enchanted broom entities render full-bright; needs in-game visual verification. | Source comparison showed the old item icon used a second tinted `ingredient.broomOverlay` pass for the tail. The 1.21 item definition now supplies that overlay layer with the default brown tint, while the entity renderer carries an enchanted render state and forces full-bright lighting for enchanted placed brooms. |
| 15 | Mirror Wall | Patched - Mirror Wall again uses the source-backed animated brew-flow cube-column visual; needs in-game translucency/animation verification. | Resurrected's `BlockMirrorWall` points at the `witchery:brew` cube-column model with `brew_flow` sides and `brew_still` caps. The port now uses those same existing textures with translucent rendering and no occlusion instead of the static mirror-glass cube. |
| 16 | Mirror GUI | Patched - Mirror inventory/drop item now uses the original flat 1.7.10 icon; needs in-game GUI verification. | The 1.7.10 `witchery-0.24.1` jar includes `textures/items/mirror.png` as a 16x16 flat item sprite. The port now copies that source icon to `textures/item/mirror.png` and points `models/item/mirror.json` at `minecraft:item/generated`, while the placed Mirror block keeps the existing two-block 3D model. |
| 17 | Silver Vat model | ✅ Custom vat model restored enough to stop the cauldron stand-in. | FTBWiki/local scrape says deposits should appear as tiny cubes only when silver is available; the FTB live page is currently behind an anti-bot gate. The port now uses a custom vat silhouette plus stored-stack-driven silver cube overlay. Further polish can still chase the exact old TESR/source spout/liquid presentation. |
| 18 | Sun Collector | Patched - inserted Quartz Sphere should now render inside the Sun Collector; needs in-game verification. | The source/Resurrected collector exposed progress through `power=0..15` blockstate visuals. This port still stores the sphere in a block entity for the modern Charged Quartz Sphere bridge, but now mirrors that storage into the source `power` state: `0` empty, `1` inserted, `15` charged. `SunCollectorRenderer` submits the synced stored stack as a small in-world item and falls back to the blockstate to draw Quartz Sphere/Charged Quartz Sphere if client BE item sync is late. |
| 19 | Scarecrow | Patched - Scarecrow GUI item now applies a GUI-only face-player yaw correction; needs in-game inventory verification. | The port uses the old scarecrow model through a special item renderer, but inherited the shared machine item base with no GUI yaw correction. `FetishSpecialRenderer` now takes a Scarecrow-only GUI-facing flag and rotates that item 180 degrees only in `ItemDisplayContext.GUI`, leaving placed Fetish rendering and the other fetish variants untouched. |

Additional 2026-05-25 door texture report: Alder/Hawthorn placed-door assets now point at dedicated lower/upper door textures instead of planks. Original 1.7 Alder Door notes explicitly say its placed texture looked like a vanilla Wooden Door; this pass restored the missing Alder-specific behavior around open-state redstone output and redstone-input immunity. Hawthorn Door is not present in the 1.7.2 source set, but the Resurrected decompiled class extends `BloodTrap`; the port now has a first practical lower-half BE/taglock-trap slice while remaining on the existing Resurrected-backed texture path pending in-game texture QA.

---

## Parity scan backlog merged 2026-05-24

The former standalone scan findings have been condensed here so the main plan carries the current repo truth. This section is now the actionable backlog.

### Status corrections to carry forward

| Area | Current truth | Follow-up |
|---|---|---|
| Custom Brew system | Partial. Foulness/Foaming/Poison tier/dispersal slice is real; Brew of Infection now has the old direct hit semantics for infesting stone-family blocks, zombifying Villagers, and slowing/damaging other living targets; Brew of Love restores the old animal, Villager, and player-enslaved zombie pairing/birth branches in practical drink form; Brew of Wasting restores source-style hunger/weakness/wither falloff plus fragile plant/leaf decay; Brew of Erosion restores source-style acid/entity corrosion plus no-drop terrain erosion/Obsidian return; Brew of Raising restores source-style top-block grave raising with weighted undead and old enslaver metadata; Brew of Bats restores source-style bat burst, short Slowness falloff, no-drop bat tags, and attack-target clearing; Brew of Ink restores source-style compact Blindness falloff plus attack-target clearing; Brew of Hitchcock restores source-style 3-4 temporary target-raven hits; Brew of Cursed Leaping restores source-style Jump Boost IV plus vertical shove falloff; Brew of Frog's Tongue restores source-style thrower pull; Brew of Frost restores source-style water freeze, ice shield, entity ice cage hits, and first practical single-click Ice World block-family conversions; Brew of Webs restores the old compact cobweb cross; Brew of Thorns restores source-style cactus column hits; Brew of Vines restores source-style horizontal side-hit vine strips; Brew of Sprouting restores source-style face-directed branch growth; Brew of Substitution restores source-style dropped-block replacement in practical item form; Brew of Hollow Tears places the current Hollow Tears stand-in block; Solidifying brews now spread through connected Hollow Tears; Brew of the Depths now runs the old five-minute Water Breathing plus out-of-water Wither timer in practical player-data form; Brew of Endless Water fills cauldrons/places water as a reusable utility item under both the source ID and the Resurrected Endless Water Brew alias; the Resurrected direct-use brew alias IDs now share their canonical direct-use classes through the Sleeping/Flowing Spirit/Wasting/Infection/Grotesque/Revealing/Frost/Frog's Tongue/Cursed Leaping/Hitchcock/Bats/Hollow Tears/Substitution/Depths/Solidifying families, and the current Sprouting/Love/Ink/Raising/Grotesque/Depths ritual or kettle consumers accept aliases where they directly inspect item IDs; Brew of Combustion is now a stackable glowing fuel item with source/Resurrected burn tiers; and Brew of Revealing now clears nearby invisibility instead of applying non-source Night Vision/Glowing. Old dynamic cauldron action composition, full radius Ice World action composition, and the source projectile transport are not complete. | Add composed brew action registry, generic brew bottle/bucket/bag support, moving gas/liquid brew behavior beyond the Hollow Tears stand-in, source-style brew projectile behavior, remaining generic brew-container aliases, infernal ownership behavior for raised/bred undead, and the missing old action roster. |
| Vampirism | Partial. Current slice covers: boolean vampire state, blood drain/thresholds, sunlight, bite, mesmerise (blood-power cost), coffin sleep, clothing hooks, garlic/sun counterplay, Silver Sword/tagged-vampire hooks, Glass Goblet chicken-blood ritual -> Elle spawn -> lava transform -> Lilith boss, Glass Goblet drained Villager/Village Guard -> Vampire mob conversion plus source own-blood level-9 handoff, Lilith friendly-mode quests (vampire induction / cure / L6->L7), auto-level for levels 1-5 + 9, Blood Crucible block/BE/render/fill/recipe/level-10 ultimate selection, ImmortalsObservationsBook level-cap unlock, Observations/Torn Page survival access + custom update recipe + readable screen + source-family kill drops including the current Witchery boss roster, practical consumables, persisted villager/guard blood draining, and source Zombie Respect targeting for level-10 vampires. | Remaining: broader Blood Power, Resist Sun, cure rite, levels 8-10 full gates, exact guard passive blood-regeneration/village-side-effect polish, and exact Observations screen polish. |
| Lycanthropy | Partial. Current werewolf render/buffs/mastery/silver hooks now include active transformation state, Moon Charm manual transform/restore control, automatic moon-shift suppression while carried, the direct Horn of the Hunt Huntsman summon item, Creative Bat/Wolf Token mastery QA, animated wolfman rendering that suppresses the vanilla player body through render-state invisibility, source-style wolf-form sleep refusal, and the Wolftrap full-moon bait gate/timer foundation. They are still not the old 10-level Wolf Altar questline. | Add 10-level Wolf Altar quests, exact wolf/wolfman form selection and level gates, Wolfsbane/silver tuning, a real Werewolf entity, and the entity-backed Wolftrap sheep curse-spread/capture behavior. |
| Machines | Partial despite several "Done" rows. Blocks, GUIs, and recipe slices exist, and the Witches' Oven now has source fume funnel/filter/filtered funnel recipes plus the side/top chance split, source top/bottom/side inventory rules, and the vanilla/Witchery sapling fume table; Silver Vat now stores/drops source-style Silver Deposits from adjacent gold-output growth; Kettle bottle/result automation and source bonus rolls are restored. Exact automation/source behavior still has gaps. | Prioritize hopper QA for restored Spinning Wheel/Distillery/Brazier/Kettle/Oven funnel/Silver Vat automation, remaining modded/edge oven byproduct checks, and any remaining Kettle fluid-container/jar edge decisions. |
| Mirror World and Torment | Implemented/verify. Mirror World portal/room loop and Torment maze are live, and Refilling Chest now matches the source choice of Torment-only hourly dungeon/simple-dungeon refills with the old unbreakable/no-drop profile; it also reads the original 1.7 `WITCLifeTicks` key, Resurrected `WitcheryLifeTicks`, and the port's older cooldown key. Fresh Torment generation now creates the custom Refilling Chest block entities needed for vanilla chest rendering/storage. Dimension chunks can still mislead QA. | Use fresh/deleted dimension folders for QA; reconcile any remaining stale-chunk/player-flow issues with repo truth. |

### High-value implementation backlog from the scan

| Family | Next bounded tasks |
|---|---|
| Vampire counterplay and quest chain | ✅ Garlic/garland, Sun Collector/Grenade chain, Silver Sword/Wooden Stake, Glass Goblet ritual and induction loop, chicken-blood ritual → Elle spawn → lava transform → Lilith boss + Lilith friendly-mode quests (vampire induction / cure / L6→L7), Glass Goblet drained Villager/Village Guard → Vampire mob conversion, auto-level 1–5 + 9 plus source own-blood level-9 handoff, Blood Crucible block/BE/render/fill/source recipe/level-10 ultimate selection, ImmortalsObservationsBook level-cap unlock, Torn Page drops, Warm/Lilith's Blood consumables, persisted villager/guard blood draining, source Zombie Respect targeting, and Bookshop loot/structure placement for Observations/Torn Page access. Next: broader Blood Power charges, cure rite, levels 8–10 gates, Resist Sun, and exact guard passive blood-regeneration/village-side-effect polish. |
| Transformation counterplay | Tie Hunter Dawn armor, Weaken Vampires brews, remaining bolt projectile/crossbow behavior, Silvered Hunter armor, Wolfsbane, Moon Charm, and Wolftrap into explicit vampire/werewolf QA rows. |
| Poppets and taglocks | ✅ Taglock+poppet recipes done, including source-style filled Taglock Kit + Poppet data-copy crafting for player and creature taglocks. ✅ Empty Taglock Kit detection fixed. ✅ Two-taglock Vampiric Poppet + PoppetEvents redirect bug fixed, now resolving living mob victims across loaded server levels. ✅ Voodoo throw/needle/lava/drowning done, now resolving mob-bound targets across loaded server levels. ✅ WitchHunter poppet disruption done. Poppet-family item models now share a flat sprite-card template instead of generated side-strip extrusion, and the shelf top pass lays stored poppets flat on the surface. Poppet Shelf Compass now has the source shaped recipe, a first practical loaded-shelf locate readout using the active shelf registry, and the old 0-5 distance-band icon/model predicate path on the client. Remaining: in-game poppet visual QA, shelf-compass visual/distance QA, and in-game mob-taglock poppet QA. |
| Scrying and waystones | Crystal Ball now has nearby-player subject selection for ordinary predictions, source-style `LastUsedTime` recharge with the old strict `elapsed > 100` gate for the normal prediction path, the old weighted 17-reading prediction roster in immediate-message form, a first bound-Waystone scry hook using the old same-dimension/150-block focus constraint, and filled Taglock Kits/Blooded Waystones can now use the Crystal Ball to consume 500 altar power and start the old 1200-tick held-use scry lifecycle with 20-tick same-dimension loaded-target feedback. Waystone/taglock scrying follows the old item route by consuming altar power without applying the normal ball recharge gate, Waystone helpers now only treat stacks as bound when all coordinate and dimension data is present, newly bound and Blooded Waystones now cap at 1 while blank Waystones keep their 16-stack common-item behavior, bind/copy rites now output the distinct source `witchery:bound_waystone` ID while still accepting old saved bound blank-waystone stacks, both source Blooded Waystone support sets now output the distinct source `witchery:blooded_waystone` ID with taglock-style player/creature data for Transposition to the live target, and dropped blank Waystones now auto-bind through small Otherwhere circles again. Player Compass now binds either by right-clicking a player or by crafting a vanilla Compass with a filled player Taglock Kit, rejects empty/creature taglocks, and reports online/offline plus same-dimension distance feedback. Remaining: stored PredictionManager prediction fulfillment/outcomes, real remote camera packet/view, exact Player Compass dynamic model/needle behavior, and the old dropped bound/Blooded Waystone transposition item path. |
| Infusion, coven, and familiars | Keep infusion charge/rite foundation, but add Witches Hand active ability families, Mystic Branch symbol magic, infernal enthrall/sacrifice creature powers, coven recruitment/quest roster, Seer Stone own-coven support, and missing familiar edge behaviors. |
| Hobgoblins and patrons | Split merchant/hut foundation from old labor/village behavior; the source `EntityGoblin` out-of-world damage immunity is restored for `HobgoblinEntity`. Add real mining/gathering/breeding/raid behavior, Koboldite Pickaxe miner bonuses, bound Patron worship polish, exact Gulg/Mog boss QA, and remaining reward render/model polish. |
| Villages, structures, and entities | Expand "Village additions" beyond the now-live Bookshop, Guard Tower/Town Keep, Town Wall, Village Guard, first-pass Coven Circle, first-pass Straw Man, source Villager damage-protection rule, and source `TownZombieAttackReductionMode` zombie/villager rule into mayor/cot/vampire feeding support, Coven Circle/Straw Man fresh-world QA, and Stone Circle naming. Add missing boss/NPC rows for Baba Yaga, Leonard/Lost Souls, Gulg/Mog, vampire/werewolf/were-villager mobs, Minedrake, summoned undead, and illusion mobs. |
| Machine exactness | Spinning Wheel: Hay Bale golden thread route, 8-string web route, and legacy sided automation are restored; remaining work is hopper QA. Distillery: legacy sided automation is restored; remaining work is hopper QA. Brazier: legacy ingredient-only automation is restored; remaining work is hopper QA. Oven/Funnel: ore rejection, food/wood Foul Fume, source log-to-Charcoal cooking, source Fume Funnel/Fume Filter/Filtered Fume Funnel recipes, top speed-only behavior, side byproduct chance, filtered 0.30 chance, source sided inventory, and the source vanilla/Witchery sapling fume table are restored; top exposes input+jars, bottom exposes jars+fuel and extracts only empty buckets from fuel, side faces expose byproduct+cooked+jars+fuel and extract only outputs, and lava buckets now leave buckets in the fuel slot. Remaining work is in-game funnel/hopper QA plus any modded-sapling/Forestry/BOP integration decisions. Kettle: all slots are exposed for sided automation, bottles insert into slot 7, automated result extraction requires/consumes stored bottles, result exhaustion drains/resets the kettle, and manual extraction uses source-style Witch Hat/Baba's Hat/robe/Necromancer Robes/toad bonus-roll chances; remaining work is hopper QA plus any fluid-container/jar and altar-source exactness decisions. Silver Vat: stored Silver Deposits stack, horizontal sided-inventory gold-output detection, 1-in-5 byproduct roll, right-click/break drop, comparator output, low no-occlusion footprint, BE-driven visible deposit overlay, custom 3D block/item model, source deposit recipes for Silver Sword/Silver Bolts/Silvered Hunter gear/Wolftrap, and a local 9-deposits-to-Silver-Ingot bridge are restored; remaining work is furnace/hopper QA and any later cross-mod external silver-dust/tag integration. |
| Torment/chest polish | Refilling Chest decision closed: 1.7 used `dungeonChest` with the old misspelled `WITCLifeTicks` save key, Resurrected used vanilla `simple_dungeon`, and the current port intentionally keeps the Torment-only hourly small dungeon refill plus unbreakable/no-drop anti-cheese profile. The block entity now reads both source lifetime keys and the older port cooldown key, and fresh Torment chunks explicitly attach that block entity to each generated maze chest. Remaining work is fresh-dimension QA for chest visibility/refill feel. |

## Priority order for next session

### 0a. ✅ 1.7.10 parity matrix — Done (2026-05-28)

`docs/status/PARITY_MATRIX_1710.md` created. Built from the 1.7.10 decompiled source registries:
- `WitcheryItems` — all registered items
- `WitcheryBlocks` — all registered blocks
- `WitcheryBrewRegistry` — all brew recipes and actions (46 entity types, 44 potion effects, 14 action base classes confirmed in decompile)
- Entity registration
- Worldgen/structures
- Ritual roster

For each feature, mark: `Done` | `Partial` | `Placeholder` | `Missing` | `Wrong behavior`.
Note where the current port was built against 1.7.2/Resurrected and may need re-evaluation against 1.7.10.

After the matrix exists, pick the first Custom Brew / Witch's Cauldron gap and implement it.

---

### 0. Item Review — remaining Blocks (ongoing)

Items from `docs/status/ITEM_REVIEW.md` still needing work:

| Item | Issue | Effort |
|---|---|---|
| `altar` | Original 1.7 cube texture/model rollback is in; verify 2x3 connected top/side orientation plus item/drop presentation | Low |
| `distillery` | Retest machine GUI after explicit source-region blit fix | Low |
| `spinning_wheel` | Retest machine GUI after explicit source-region blit fix | Low |
| `witches_oven` | Retest machine GUI after explicit source-region blit fix, and confirm placed ovens now face the player after the custom-renderer yaw fix | Low |
| `crystal_ball` | ✅ Now purple stained-glass orb on polished-blackstone stand | Small |
| `coffin` | ✅ Item now uses the original-style flat Coffin icon in GUI/inventory; placed block still needs normal in-game sleep/open pose QA | Low |
| `alluring_skull` | Dedicated skull-head renderer path and source unbreakable hardness are restored; verify Necromantic Stone toggle/removal plus scale/lighting | Low |
| `demon_heart` | Retest custom model no-cull face fill in-world | Low |
| `chalice` | Retest creative tab empty/filled entries, 2D empty/filled inventory sprites, Redstone Soup right-click filling, in-cup liquid height, survival break drop/re-place persistence, altar bonus, and centered world pose after the renderer rotation correction | Low |
| `dream_catcher` | Clicked-face wall placement and centered GUI item renderer are restored; needs in-game verification only | Low |
| `kettle` | Original texture/model path restored for all live blockstates; verify side/ceiling support, filled liquid, and bottle visuals in-game | Low |
| `poppet_shelf` | ✅ Client slot clearing patched and source 3x3 top centers now rotate with shelf facing; verify creative-slot height and shelf-top spacing in-game | Low |
| `mandrake_root` | Retest Mandrake summon branch, Banshee custom scream, Earmuffs hush, and final audio feel | Small |
| `grassper` | Source transform order/scale restored for the held item; verify held-item cup pose and Fancy/Fabulous spin in-game | Low |
| `glowglobe` | Translucent low-alpha render path restored; verify visibility in-game | Low |
| `rowan_door` | Vanilla hinge/facing placement restored; verify lock/unlock feel in-game | Low |
| `leaping_lily` | Old lily-pad texture/display model restored with Witchery tinting; needs in-game verification only | Low |
| Legacy placeholder items | 10 source/wiki/decompiled item IDs remain registered and visible as inert creative-tab placeholders after the belts/traps/vampire clothing/contracts/wearables/diviners/garlic/dawn-gear/custom-brew/Brew of Endless Water and its Endless Water Brew alias/direct brew alias/Infused Brew exact-ID/Brew of Combustion/Meaty Stew/Cane Sword/Moon Charm/Horn of the Hunt/Creative Bat-Wolf Token/Witch Hand and Witches Hand alias/Devil's Tongue Charm/Twisting Band/stake/bolt/glass-goblet/observations-book/blood-consumable/fume-filter/silver-deposits/alder-hawthorn-wood/Hollow Tears/Perpetual Ice block/stair/slab/stockade, Snow pressure/stair/slab, Subdued Village Spirit, source bolt alias, Bound Waystone, Blooded Waystone, Odd Porkchop alias, Icy Slippers exact-ID, Witches Robes exact-ID, Gulg's Gurdle exact-ID, Hunter armor exact-ID, Witchcraft guidebook exact-ID, Flame Imp contract exact-ID, direct Resurrected brew alias promotions, duplicate Resurrected alias cleanup for items that already have canonical port IDs, Resurrected Fanciful Thinking/Pentacle exact-ID promotion, Medallion of Blood and Beast promotion, Charged Crossbow model-only cleanup, Silver Dust non-item cleanup, Heart of Gold ingredient promotion, deployed Hand of Death exact-ID promotion, Demon Soul ingredient alias promotion, advanced Witchcraft book promotion, legacy source Witchcraft book alias promotion, Torment contract alias promotion, blank Demonic Contract source-alias promotion, and custom-brew flower reagent promotion. All remaining placeholders point at non-shared source/wiki-backed item models or clear alias models; none still use the shared placeholder model. Implement mechanics/recipes or alias-remaps one family at a time | Varies |
| Items 62–153 | Tools & Items, Armour, Brews, Infusion, Spawn Eggs — untested | Varies |

---

### 1. Spirit World - patched, needs fresh-world QA (2026-05-25)

Current truth:
- Dimension biome source corrected: the Spirit World now uses the fixed `witchery:spirit_world` biome, so its existing vegetation, cotton features, colors, and native spawns are no longer hidden behind the vanilla Overworld biome preset.
- Vegetation worldgen connected: grass, tall grass, mushrooms, dead bush, oak trees, wispy cotton, and disturbed cotton are placed in the Spirit World biome via the VEGETAL_DECORATION step.
- Native Spirits now use the ambient spawn bucket again, matching the old ambient-creature registration more closely.
- Source-era passive animals now use the creature spawn bucket: sheep, pigs, chickens, and cows. This closes the animal-spawn decision without adding modern/biome-specific animals to the fixed Spirit biome.
- Non-source soul-sand ambient sound loops are removed and dimension `ambient_light` is back to `0.0`.
- Client render state now applies the source dream/nightmare/demonic sky color/fixed celestial angle, zero-star sky, old horizon `0.0` dark-disc rule, and no-cloud rendering; fog uses the same source color helper.
- Ghost layer upgraded to `LightTexture.FULL_BRIGHT` so manifested players glow in darkness
- Circle Magic is now blocked in the Spirit World; only Rite of Manifestation (spectral_dust on otherwhere circle) is permitted — matches original Witchery spec
- Remaining: fresh-chunk/in-game vegetation and spawn QA using `/witchery verify spiritworld`; exact old fog-distance polish only if a concrete old hook beyond color/sky state surfaces.

---

### 2. Mirror World dimension - implemented, needs fresh-world QA/reconciliation

Original: `hasNoSky=true`, permanent dark, dark blue fog `(0, 0.03, 0.1)`, no rain/lightning.
Current repo truth has the portal-room loop landed: Mirror block, source 3D placed mirror visuals, original flat Mirror GUI icon, source-backed animated brew-flow Mirror Wall visuals, Mirror/Mirror Wall item definitions, room chunk generator, room SavedData, entry/exit teleport, Reflection guardian, and owner-skin renderer. Mirror model front/back UVs now face the block-state front instead of showing the back side to the placer. Mirror Wall now follows Resurrected's `witchery:brew` cube-column presentation with animated `brew_flow` sides and `brew_still` caps, using a translucent no-occlusion block path so it reads as running silver water again.
- **QA:** test in a fresh world or delete `run/client/saves/<world>/dimensions/witchery/mirror_world/` before judging room generation; also place/break/drop Mirror and Mirror Wall to verify Mirror item flat icon/placed orientation, animated Mirror Wall translucency/flow, and inventory/dropped-stack models no longer fall back to magenta/black.
- **Follow-up:** survival/ritual ingress wording, exact Duplication Grenade mirror charging/binding, Reflection behavior polish, and stale-chunk/player-skin edge cases.

---

### 3. Custom Brew system - partial

The Foulness/Foaming/Poison tier slice is implemented across liquid/splash/gas variants with `witchery:brew_power`, thrown brew sync, and scalable effects. That is useful progress, but not the old full custom brew system.

Open parity:
- Dynamic cauldron action composition and serializers/effects.
- Generic brew bottle, splash, lingering, bucket/fluid, Brew Bag, and full dynamic composed-brew containers.
- Moving gas/liquid brew block or a documented modern replacement.
- Many old action effects such as tree growth, blight, transpose, undead/insect/demon bane, drain magic, steal buffs, and summon Leonard.

---

### 4a. Witchery against villagers — goal

**Goal:** make villagers meaningful targets across all Witchery systems — taglocks, poppets, vampire feeding, ritual sacrifice, and werewolf infection.

This goal is not about one file; it is a cross-cutting vertical slice. Track each sub-task below and tick it off when done.

#### Sub-tasks

| Sub-task | Status | Notes |
|---|---|---|
| Taglock Kit on villagers | Needs verify | `interactLivingEntity` uses the LivingEntity path for mobs and filled Taglock Kits can now craft-bind those creature signatures onto poppets. Test: right-click villager with empty kit, then craft with a poppet and verify the output shows the villager name. |
| Voodoo Poppet bound to villager | Needs verify | Voodoo Poppets can now receive mob taglocks through the source-style binding recipe and resolve the bound UUID across loaded server levels. Test: bind poppet to villager, use throw/needle/lava, verify villager takes effect. |
| VampiricPoppetItem — villager as victim | Needs verify | Two-taglock crafting and direct binding now preserve/resolve living mob victims for redirect. Test: bind source to self, bind victim to villager, take damage, verify villager receives redirect. |
| Vampiric blood feeding from villagers/guards | Partial - source-style persisted drain plus Glass Goblet conversion restored | Empty-hand vampire bites on villagers now use a persisted 500-blood store. Awake villagers yield the old two-thirds drain, sleeping/mesmerised villagers yield the full drain, clothing drink boost applies, and low-blood villagers take extra harm. Village Guards now carry source-style persisted `BloodLevel`, can be mesmerised, use the source two-thirds guard drain rule, and can be fully drained for the same conversion gate. A vampire-blood Full Glass Goblet used on a mesmerised, fully drained Villager or Village Guard near a Coffin now replaces that target with the current Vampire mob, empties the goblet, plays source-style red-dust drink feedback, and can advance a level-9 owner to level 10 when the goblet carries their own blood. Remaining: exact 10-level quest counters and guard passive blood-regeneration/village-side-effect polish. |
| Ritual sacrifice — villager offering | Needs verify | Hell on Earth, Wither summoning, and Demon summoning already specify `VillagerEntity` sacrifice. Test each rite with a caged villager on-site. |
| Curse rite targeting villager via taglock | Needs verify | `Curse Creature` resolves taglock UUID via `ServerLevel.getEntity`. Test: fill taglock from villager, place it in medium infernal circle, verify villager receives Wither/Slowness/Weakness. |
| Blight villager zombification | Needs verify | Blight rite's expand loop zombifies villagers. Test: cast medium infernal Blight near a village, verify villagers turn to zombie villagers as the annulus grows. |
| Werewolf infection of villagers | Partial - source Wolfman bite branch restored | Infectious `WolfmanEntity` damage now follows source `Shapeshift.processWolfInfection`: if the bite would leave a Villager below 25% health, the Villager is immediately replaced by the existing `WereVillagerEntity` and the damage is suppressed. The earlier full-moon-night werewolf-player kill shortcut still exists as a first-pass bridge, but exact source parity remains the level-10 transformed player bite branch, old profession/trade transfer, and a deliberate decision on the source `AllowVampireWerewolfHybrids` / current no-hybrid policy. |
| Player Compass — scry to named villager | Deferred | Player Compass only binds to players currently. Villager support would need a non-player name resolution path. Defer unless requested. |

#### Why this matters

Vanilla villages are a rich social target that the original Witchery treated as a living ecosystem: vampires fed from villagers, witches cursed them, werewolves infected them, and ritual casters sacrificed them. Making the current port respect that ecosystem makes the open-world feel of Witchery land properly — players can run a coven near a village and use it both as a resource and a risk.

#### Suggested implementation order

1. Verify the three "needs verify" rows first (no code, just QA) — they may already work.
2. Retest vampiric villager feeding now that persisted source-style drain is live.
3. Retest werewolf villager infection in-game: first use an infectious Wolfman bite to push a Villager below 25% health and confirm immediate Were Villager conversion, then separately retest the existing full-moon werewolf-player kill bridge until the level-10 source player-bite branch is restored.
4. Were-Villager mob is a separate larger entity task; add it to the entities backlog if requested.

---

### 4b. Debug command: add `spectral` to `/witchery familiar` suggestions — Done

`"spectral"` added to `FAMILIAR_TYPES` suggestions. `setFamiliar` handler now accepts `owl`, `cat`, `toad`, and `spectral` — stores the type via `setFamiliarType`, then calls `FamiliarHelper.restoreBoundFamiliar` to spawn the entity near the player. `"none"` still clears. Unknown types get an error message listing valid options.

---

### 4c. Small code quality items (deferred — after parity complete)

These are individually small but accumulate maintenance debt. **Do not action until all major parity work (Mirror World, Custom Brew, Spirit World gaps) is stable** — they carry non-zero risk of altering observable behavior mid-sprint.

- **Registry classes** contain many hard-coded `"witchery"` string literals. An `id(String path)` helper would reduce copy/paste mistakes across blocks, entities, dimensions, and worldgen.

Closed by earlier code before this reconciliation: `ConcentratedBatBallRecipe` already uses a shared private `matchesPattern(input)` path for both `matches` and `assemble`; `TaglockKitItem.isPlayerTaglock` already requires `isFilled(stack)` before treating a stack as a player taglock; `MutatingSprigItem` already has `finishUse(ctx, pos)` and the `clearHorizontalRing` naming.

---

### 4d. RitualExecutor decomposition (deferred — after parity complete)

**Do not start until Mirror World, Custom Brew, and all open ritual-parity bugs are closed.** Trigger-order changes in this file can silently break which rite fires; doing it mid-parity-sprint is high risk.

`RitualExecutor` currently handles dispatch, precondition checking, item/power transactions, active ritual queueing, terrain effects, curse logic, climate-change biome rewriting, and utility scans — too many jobs in one file. There are also duplicated imports and a dead `isRawFood` helper.

`RitualTransaction` is a good first extraction; the next step is a `RitualDefinition` or per-rite handler registry so each rite owns its own validation, cost, and effect logic. Also strengthen `RitualTransaction` to validate → prepare → commit → apply order with a rollback path before this decomposition begins.

Priority for decomposition once unblocked: curse logic → active-rite queueing → terrain/climate effects → utility scans.

Files:
- `src/main/java/com/emoniph/witchery/ritual/RitualExecutor.java`

---

### 4e. Entity backlog — 1.7.10 missing entities

The 1.7.10 decompiled source has 46+ entity classes. This tracking table lists the source-confirmed entity parity backlog; struck-through rows are already ported and registered in the current repo:

| Entity class | Description | Priority |
|---|---|---|
| ~~`EntityLilith`~~ | ~~Lilith vampire patron~~ | ✅ Done — `LilithEntity` + `ElleEntity` ported |
| ~~`EntityLeonard`~~ | ~~Shade of Leonard boss~~ | ✅ Done — `LeonardEntity` ported |
| ~~`EntityLostSoul`~~ | ~~Leonard immunity phase mob~~ | ✅ Done — `LostSoulEntity` ported |
| ~~`EntityBabaYaga`~~ | ~~Baba Yaga boss~~ | ✅ Done — `BabaYagaEntity` ported; boss QA/reward polish remains in entity QA, not missing registration |
| ~~`EntityVillageGuard`~~ | ~~Village Guard~~ | ✅ Done — `VillageGuardEntity` ported, design locked |
| ~~`EntityVampire`~~ | ~~Vampire mob (player vampirism is separate)~~ | ✅ Done — `VampireEntity` ported; conversion/cure/quest parity remains in vampire progression |
| ~~`EntityWolfman`~~ | ~~Standalone werewolf entity~~ | ✅ Done — `WolfmanEntity` ported; Wolftrap wiring remains a separate item |
| ~~`EntityVillagerWere`~~ | ~~Were-villager~~ | ✅ Done — `WereVillagerEntity` ported |
| `EntityGoblin` | Base goblin | Partial — `HobgoblinEntity` exists with trading/hut support and source out-of-world damage immunity; old labor/village chain remains |
| ~~`EntityGoblinGulg`~~ | ~~Gulg goblin boss~~ | ✅ Done — `GoblinGulgEntity` ported |
| ~~`EntityGoblinMog`~~ | ~~Mog goblin boss~~ | ✅ Done — `GoblinMogEntity` ported |
| `EntityHellhound` | Hell hound | Medium — infernal lane |
| `EntityMindrake` | Mindrake mob | Low–medium |
| `EntityIllusionCreeper` / `Spider` / `Zombie` | Illusion mob variants | Low |
| `EntitySummonedUndead` | Summoned undead via brew | Low — custom brew dep |
| `EntityMirrorFace` | Mirror World guardian variant | Low — mirror dep |
| `EntityDarkMark` | Dark Mark projectile/effect | Low |
| `EntityEye` | Eye mob | Low |
| `EntitySpellEffect` | Spell visual projectile | Low |
| `EntityWitchCat` (wild) | Wild witch cat variant | Low |

Implementation order: ~~Lilith~~ ✅ → ~~Leonard/Lost Souls~~ ✅ → ~~Village Guard~~ ✅ → ~~Wolfman/WereVillager~~ ✅ → Goblin/Gulg/Mog → remainder.
Each entity needs: registration, attributes, AI goals, renderer, loot table, and spawn placement.

---

### 5. Witches' Primer / compendium completeness pass (medium, staged)

High-value player-discovery gaps to close in the in-game guide:

- **Blocked-player items first** - first-pass Primer coverage for `Boline`, the four infusion source items (`Ghost of the Light`, `Soul of the World`, `Spirit of Otherwhere`, `Infernal Animus`), `Anointing Paste`, and now `Mutandis` / `Mutandis Extremis` is in; the next guidebook value is completeness/polish rather than blocked progression basics.
- **Missing rites in Chapter 11** - first-pass Primer coverage is now in for the whole current missing-rite batch, including `Rite of Moving Earth`; the main remaining guidebook work is now completeness/polish rather than missing rite basics.
- **Treefyd seed bridge** - closed. `Treefyd Seeds` are now restored as a real crafted item, have a 1.21 item definition pointing at the original seed sprite, can once more be used on the top face of grass to immediately raise an owned Treefyd as the old source did, and now have a dedicated Primer page instead of being left implied.
- **Tool/familiar identity gaps** - `Mystic Branch`, `Necromantic Stone`, `Treefyd Seeds`, and `Spectral Familiar` now all have truthful Primer coverage for their current port roles; next guidebook value is more in equipment/completeness than in blocked progression identity pages.
- **Equipment/completeness follow-up** - `Beartrap` / `Wolftrap` usage coverage, the explicit `Wicker Man` pattern/layout, first-pass Vampire clothing recipe/armor/acquisition coverage, the exact Resurrected Vampire clothing alias family, first drink-boost, mesmerise, and lethal-fire clothing set-bonus hooks, and source-style set-bonus tooltip lines are now in; remaining high-value equipment polish is armor model/level-gate polish plus full old ability-selection/paralysis parity.
- **Legacy placeholder follow-up — COMPLETE.** `LEGACY_PLACEHOLDER_ITEMS` is now **empty**; there are no inert placeholders left. The old "107" figure was always stale (it counted the whole `ITEM_REVIEW.md` roster, most already promoted). Resolution of the final set:
    1. `wolf_head` + `hellhound_head` → **real mounted-head blocks** (`WolfHeadBlock` / `HellhoundHeadBlock` + `WolfHeadBlockEntity` + BESR `WolfHeadRenderer`/`WolfHeadModel`), ported from 1.7.10 `BlockWolfHead` (skullType 0=wolf / 1=hellhound). Floor/wall placement with floor rotation, mirroring `AlluringSkull`. Wolf reuses the vanilla wolf texture; hellhound uses copied `textures/entity/hellhound.png`. Self-drop loot, flat inventory icons, creative-tab entries.
    2. `brew_bottle` / `splash_brew_bottle` / `lingering_brew_bottle` → **removed.** These are Resurrected's generic NBT custom-brew CONTAINERS; the port deliberately uses individual `brew_of_*` items, so they have no role in this architecture.
    3. `trapped_plant` → **removed.** Invented ID with no source item ("trapped plants" is only a generic Boline-tooltip phrase; the real trap plants are the already-promoted `pit_dirt`/`pit_grass`/`plant_mine` + Poppy/Dandelion/Shrub derivatives).
    4. The four `brew_of_soul_*` were earlier removed as duplicates of the `*_demon_soul` ingredients.

---

## Long-term art direction

- **Original replacement art pass (deferred, post-parity)** - once mechanical parity is in a trustworthy state, plan a broad replacement pass for textures, sounds, and other audiovisual assets so the port is not just reusing/riffing on old Witchery content. The target should be "new art with the same gameplay identity and mood", not a straight rip of the original assets. Treat original Witchery as tone/mechanics reference, not as the final art source, and prefer building a distinct visual/audio identity before any public release push.

---

## Raven feature expansion (deferred, post core-parity)

The new raven should not stop at "real bird instead of parrot". It can become one of the port's signature systems once the main parity backlog is under control.

- **Raven familiar track** - allow a raven to bind to a player more like a true witch companion than a passive mob. Core behaviors to target: follow at medium range, perch near the player when idle, recall/teleport back if lost, and respect sit/stay-style commands without becoming a full wolf clone.
- **Murder of crows events** - add a low-frequency ambient system where a random murder of ravens can gather under the right mood conditions: storms, dusk, full moon, cursed land, nightmares, recent death, or active infernal rites. These flocks should feel like omens first and combat second.
- **Brew of Hitchcock escalation** - keep the restored short target-raven flock as the baseline, but later let the brew optionally interact with the murder system so a cast in the right conditions can chain into a larger local frenzy instead of always using the same small source-style hit.
- **Omen mechanics** - ravens are a good fit for soft warnings. They could circle above nearby curses, corpse locations, hidden hostile mobs, dream/nightmare disturbances, or ritual sites that are about to fail. This gives them a unique identity beyond "owl but darker".
- **Scouting and messenger utility** - a bound raven could be sent toward a nearby point of interest and return with a rough result rather than exact minimap-like data. Good candidates: nearest bound waystone, nearest witch structure, last death location, active nightmare corpse, or the last place a player slept.
- **Ingredient and object interactions** - ravens can make the world feel more alive if they steal, drop, or deliver small items. Future hooks could include pecking up seeds/shiny items, carrying a single lightweight delivery item, or occasionally bringing back minor curios when sent out.
- **Ritual integration** - ravens fit Witchery best when they cross into magic systems. Future ideas: perch on circles during certain rites, react to infernal/otherwhere chalk families differently, help reveal valid ritual centers, or serve as an optional omen/sacrifice/witness mechanic in new Rewoven-specific content.
- **Dream and Spirit World hooks** - ravens could become one of the cleaner bridges between dimensions and states. Examples: black-feather apparitions in the Spirit World, ravens warning of demonic nightmare risk, or a manifested raven familiar that can still find the player while ghosted.
- **Social and world flavor** - coven witches and hobgoblins can both gain flavor from raven presence. Witches might occasionally keep or summon ravens, while hobgoblins could fear them, throw food at them, or react to them as bad luck.
- **Rowan and ward interactions** - ravens should not ignore the rest of the mod's rules. Rowan protections, sanctity barriers, dream catchers, and other warding systems should influence where raven omens gather and whether they can cross into protected areas.

### Suggested rollout order

1. **Bound raven familiar** - follow, perch, recall, persistence, and basic command handling.
2. **Hitchcock escalation hooks** - build on the restored targeted flock with optional weather, moon, curse, or rite context rather than replacing the source-style baseline.
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

- **Fresh village walls + injected buildings** - create or locate fresh plains, desert, savanna, taiga, and snowy villages; verify Witchery apothecaries, bookshops, source-sized witch huts, watch towers, and keeps can attach to village roads with the new weights (`16/12/16/8/32`), and verify Keeps now appear and log `[VillageBuildings:Debug] Claimed village_keep` / `Placing village_keep` when selected; verify every Witchery village building uses the matching biome palette, especially sandstone-family blocks for desert/badlands apothecaries, bookshops, witch huts, watch towers, and keeps; verify adaptive walls generate once without artificial delay, do not force-load chunk tickets from automatic world-entry discovery, do not stack duplicate wall layers on already processed villages, do not sink under water, keep paths/gates open on hills with inward grading reaching up to 15 terrain-only blocks and stopping before building blocks, produce nonzero road-aligned gate candidates on ordinary road villages, use desert sandstone only for desert/badlands and dirt/stone for savanna wall/path materials, use terrain-style filler under generated gate paths instead of stone brick, avoid enclosing gates with corners, and do not freeze when `/witchery goto village_*` or `/witchery walls` is used after the bounded locate / pre-teleport one-chunk-per-tick prep / nearest-first one-chunk forced wall jobs / stuck-chunk skip / 30 second cooldown changes.
- **Trader menus** - spawn/use both `Coven Witch` and `Hobgoblin`; verify the trade screen stays open instead of flashing shut, and make sure lead/name-tag interactions still win when intended
- **Setup hut** - run `/witchery setup stations`; verify the command clears the intended 20x20 pad in front of the player without digging below door/player height, the 11x11 Rowan hut shell/foundation still sits one block lower while the altar/machines/chest/traders stay on the original player-height test plane, the 3-deep stone-brick foundation still hides uneven ground, the Rowan door/windows plus the new horizontal Rowan log header generate correctly, the window panes now visually meet the Rowan walls, the ceiling Glowglobes light the hut cleanly, the altar sits off the back wall and still links as a real 2x3 multiblock, the cobblestone-supported kettle survives over the campfire, the water cauldron lands beside the kettle, the distillery/oven/spinning wheel face into the room, the fume funnel links cleanly above the oven, the oven cook arrow still sits on the correct upper lane, the stocked double chest contains the intended QA supplies, and the indoor Coven Witch + Hobgoblin both spawn and keep their trade GUIs open
- **Base alchemy chain** - smelt `clay_jar_soft` into `clay_jar`, then retest the oven/distillery loop; confirm `odour_of_purity`, `oil_of_vitriol`, and the other early intermediates now show in the Witchery creative tab for fast QA

- **Treefyd model and seed item** - `/witchery spawn treefyd` and verify the restored old 1.7 zero-plane petals, tongue, and crossed leaves now render visibly with the original 64x32 texture; also verify the `Treefyd Seeds` inventory icon is no longer magenta/black and that grass-top use immediately raises an owned Treefyd
- **Poltergeist model** - `/witchery spawn poltergeist` and verify the restored old all-part mirror UV flags close the reported wrong-mapping/holes issue
- **Banshee model** - `/witchery spawn banshee`, provoke/approach it, and verify the flat source mouth appears only while screaming and no chin/face patch protrudes at rest
- **Mandrake model** - `/witchery spawn mandrake` and verify the restored old mirror UVs and zero-depth leaf crown close the reported texture holes
- **Ent model** - `/witchery spawn ent`, provoke it, and verify the restored old mirror UVs, face plate, inner leaf fill, root-leg walk, and attack arm lift close the reported wrong-mapping/holes issue

- **Flying Monkey model** - `/witchery spawn flying_monkey` and verify the source monkey body, wings, ears, tail, landed/flying animation, and feather tint close the reported wrong-model/texture-holes issue
- **Flame Imp model** - `/witchery spawn flame_imp`, provoke it, and verify the source right-arm punch/body twist now plays during melee; power one up and verify the old taller powered render scale
- **Parasytic Louse potion bite/hotbar defense** - craft `parasytic_louse` with a non-water vanilla potion, place it, let it bite a target, and verify the carried potion effects apply once and then clear; verify pickup preserves unused potion payloads. Then keep a potion-fed louse in the hotbar and verify hostile effects bite the attacker while beneficial effects bite the player, clear the louse payload, and cost 1 magic damage
- **Biting Belt potion defense** - craft `biting_belt`, load it with up to two non-water vanilla potions, wear it in the legs slot, take entity damage with no usable hotbar louse, and verify it consumes one stored effect, routes harmful effects to the attacker or beneficial effects to the player, skips already-active/instant-heal effects, and costs 1 magic damage
- **Bark Belt charge defense** - craft `bark_belt`, wear it in the legs slot, stand on grass or mycelium long enough to gain bark pieces, then verify non-wooden entity damage is canceled by spending charges and popping stick drops while wooden sword/stake/Horned Huntsman damage still bypasses it
- **Beartrap trap block** - craft `beartrap`, place it, wait one second for arming, verify living entities snap it shut for 4 source-style out-of-world damage plus strong slowness, verify creative players still close/sound the trap but skip the slowness snare, and verify right-click toggles it open/closed with a fresh arming delay after reopening
- **Wolftrap silvered trap block** - craft `wolftrap` from `beartrap`, Silver Deposits or silver ingots, Null Catalysts, and Wolfsbane, place it, verify ordinary living entities ignore it, verify a werewolf player snaps it shut for 4 source-style out-of-world damage plus strong slowness, verify right-click still toggles open/closed with the arming delay, and on a full-moon night verify an open Wolftrap beside a Wolf Altar with a leashed sheep within 8 blocks starts the persisted `WolftrapStart` bait timer without crashing while the Werewolf entity branch remains unported
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
- **Crystal Ball scrying**: waystone scry hook (same-dimension/150-block focus constraint), filled Taglock Kit scry hook for loaded same-dimension living targets, nearby-player prediction subjects, weighted old 17-reading prediction roster, `LastUsedTime` recharge with strict `elapsed > 100` gate, old 1200-tick held-use lifecycle, and 20-tick refresh feedback.
- **Bound Waystone**: distinct source `witchery:bound_waystone` item registration, bind/copy rite outputs, compatibility acceptance for old saved bound blank-waystone stacks, stack cap (1 when bound, 16 when blank), complete-binding requirement before treating as bound, old uncommon rarity on bind.
- **Circle Talisman**: floor deployment restored, reset behavior, ring visualization in tooltip, old uncommon rarity on stored circles.
- **Item presentations**: Arthana uncommon rarity, Seer Stone enchanted glow, Spectral Stone glint, rare magic item family.
- **Broom**: enchanted broom spawn flag restored on enchantment rite.
- **Icy Needle**: original use cost restored.
- **Sleeping entry guard and chance split**: sleep-to-Spirit-World safety check restored; Sleeping Apple and Brew of Sleeping now seed entry with their old `1.0` vs `0.998` nightmare chances again.
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

- **SunCollectorBlock + SunCollectorBlockEntity** promoted from legacy placeholder: BaseEntityBlock with server-ticking charge loop. Right-click with Quartz Sphere inserts it; empty right-click retrieves; drops stored item on break. Charges during daytime (sky access, no rain, dayTime < 13000) at 1 tick/game-tick; after 6000 ticks in sunlight outputs Charged Quartz Sphere. The block now also mirrors source `power=0..15` presentation state for empty/inserted/charged display sync.
- **charged_quartz_sphere** new item (foil enchant glow). Item model/assets added.
- **Sun Grenade recipe** updated again after source/companion comparison: 1 Charged Quartz Sphere -> 1 Sun Grenade, and thrown grenades now return a Quartz Sphere when the sunlight releases. Full chain: Quartz Sphere -> Sun Collector (6000 daylight ticks) -> Charged Quartz Sphere -> Sun Grenade -> Quartz Sphere return.
- **Sun Collector recipe** restored to the Resurrected source shape: gold ingots, repeater, daylight detector, and iron ingots. The block now also uses the source compact footprint and original `3.5F` hardness.
- **VampiricPoppetItem** fully rewritten with two-taglock semantics (source = witch protected, victim = damage absorber). `PoppetEvents.onVampiricRedirect` was broken — `matchesBound` found poppets where TAG_UUID == damaged player, then looked up that same UUID and bailed because target==player. Fixed: now finds poppets where TAG_UUID == damaged player (source match), then redirects to TAG_VICTIM_UUID.
- **PlayerCompassItem** promoted: right-click player → attuned; right-click air → shows 8-point compass direction + block distance. Offline/other-dimension messages. Shapeless recipe: compass + taglock_kit.
- **WitchHunter poppet disruption** added to PoppetEvents: 30% chance on WitchHunterEntity hit to destroy a bound ProtectionPoppetItem from inventory or shelf.
- Placeholder count: 195 → 192.
- BUILD SUCCESSFUL.

---

## Deferred (do not implement unless asked)

- Village additions beyond the current live village slice: Coven Circle/Straw Man fresh-world QA, Stone Circle naming, any optional/non-source Baba Yaga hut decision, and broader Witch Forest structures
- Werewolf player model full replacement
- Flowing Spirit as proper fluid (currently plain block)
- Manifestation ghost rendering / style polish
- `Egassy` - future non-parity joke-villain idea; a farting antagonist for a later optional content/fun pass, explicitly lower priority than original Witchery mechanics and parity work
