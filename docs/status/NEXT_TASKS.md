# Next Tasks — Witchery NeoForge Port

Current local delta (2026-05-31): Glint Weed source particle parity. A fresh 1.7.10 `BlockGlintWeed` check showed `animateTick` used `ParticleEffect.FLAME` with zero velocity, so the port's stale `END_ROD` detour has been reverted to `ParticleTypes.FLAME`. The existing source-shaped light/spread behavior stays intact: full light, 1-in-6 random spread, a five-plant density cap, dirt/grass/stone/sand/soul-sand spread substrate, and solid support either below or above the target.

Previous local delta (2026-05-31): Somnian Cotton spread parity. Source comparison with `BlockCotton` showed natural spread should roll 1-in-6, run only in the Dream/Spirit dimension, require Flowing Spirit beside the block below the source cotton, and place new cotton only over dirt/grass. The port now uses that gate instead of allowing Spirit World cotton to spread without Flowing Spirit or overworld cotton to spread around Flowing Spirit.

Earlier local delta (2026-05-31): Ritual power config routing. `WitcheryConfig.RITUAL_POWER_MULTIPLIER` now applies through shared ritual altar-power helpers: one-shot/startup ritual costs are scaled once before altar drain, while active staged rites scale their per-tick upkeep debt before whole power is consumed. `ALLOW_BIOME_CHANGING` was already enforced before the Climate Change rite queued or consumed its support items.

Earlier local delta (2026-05-31): Village goto freeze follow-up. Latest logs for the reported plains-village freeze had no exception and never reached the `[Goto] Prepared...` line, so the suspected stall was pre-teleport locate/preload work rather than the wall generator. `/witchery goto` now logs before locate, uses a bounded vanilla-style locate search (`skipReferenced=false`, radius 32), and preloads only the arrival chunk instead of a 5x5 full-chunk area before teleport. Fresh village QA should retest `village_plains`, `village_savanna`, `village_taiga`, and `village_snowy` after a client restart.

Earlier same-day local delta (2026-05-31): Coven Circle support cleanup. Non-desert `witchery:coven_circle` generation keeps the grass surface and standing-stone material palette, but its terrain support is now stone-backed instead of dirt-backed so uneven terrain cannot show a one-block dirt skirt under the stone circle. Fresh plains/savanna QA is still needed.

Earlier same-day delta (2026-05-31): Hand Bow anti-magic power-drain pass. Nullifying bolts fired while the shooter wears a full hunter/silvered/dawn hunter set now carry a powered-draining flag like the old powered anti-magic bolt path. The shot drains the shooter's current infusion-energy pool on launch with the source max-energy reduction rule, and a powered Nullifying Bolt hit drains half of a player target's max infusion energy after the existing potion/effect strip; non-player targets still receive the current port's effect strip only because the old external magic-power hook has no modern local pool yet. The previous client model predicate pass remains live too: uncharged idle uses `crossbow_pistol`, active use uses the three pull-frame models via `witchery:hand_bow_pulling` / `witchery:hand_bow_pull`, and a loaded-but-not-currently-drawing bow uses `charged_crossbow` through `witchery:hand_bow_charged`. Remaining bolt work is in-game projectile/visual QA.

Previous same-day village delta (2026-05-31): village structure/wall pass. `WitcheryVillageElement` now supplies a real `minecraft:building_entrance` jigsaw connection, so procedural apothecaries, bookshops, village witch huts, watch towers, and keeps can attach to vanilla village house pools. Bookshop is live as a 10x8x9 source-inspired shop with the bookshop loot chest, shelves, lectern, sign, and villager shopkeeper, and the house-pool weights now strongly favor bookshops/apothecaries/witch huts over watch towers. Adaptive village walls are live around vanilla villages with stone-brick-family walls, water-safe placement, terrain-robust gatehouses, desert-only sandstone path material, savanna dirt/stone materials, gate/path support fill matching the gatehouse block, graded gate approaches, occupied living-entity wall-column skips, one-ring perimeter chunk force-loading, no artificial wall-generation delay, and `/witchery goto` cooldown/flavor feedback. Coven Circle is also live as `witchery:coven_circle`; this heartbeat tightened its biome material predicate to match old `ComponentCoven`, so sandstone/sand variants are limited to actual desert/badlands instead of every hot biome, restored the hidden mineshaft-loot dispenser plus two vanilla Witch spawners under the altar, checks center/corner water before placement, uses a grass surface with stone-backed support in non-desert biomes so uneven terrain no longer exposes a one-block dirt skirt, and uses the source 90% resident Coven Witch spawn chance. Straw Man worldgen is now live as `witchery:straw_man` from old `ComponentWickerMan`, with a hay effigy, biome plants, hidden Zombie/Zombie/Skeleton spawner, village exclusion, water rejection, and `/witchery goto straw_man`. Next action is fresh-village QA across plains/desert/savanna/taiga/snowy, with special attention to goto-freeze retests, bookshop placement, structure distribution, wall timing, gate filler, and path materials, plus fresh Coven Circle and Straw Man checks in plains/desert/savanna/badlands.

Last reconciled: 2026-05-25 (parity scan merged; Sun Collector/Charged Quartz Sphere/Sun Grenade chain implemented plus source recipe/shape/hardness and Quartz Sphere return/tagged vampire hits; Silver Sword held-damage plus tagged-vampire silver/hunter hooks restored; Wooden Stake recipe and sleeping-vampire staking restored; Wooden/Silver/Bone/Nullifying/Splitting Bolt source crafting restored; Glass Goblet recipe, first practical vampire blood fill/drink induction, and chicken-blood ritual capture restored; Observations of an Immortal + Torn Page promoted with source recipe, custom update recipe, page-progress tooltip, vanilla readable book screen, source-family player-kill Torn Page drops including the source `isNonBoss=false` branch for current bosses/specials, and Bookshop loot seed; Warm Blood and Lilith's Blood consumable semantics restored in practical form; source-style persisted villager blood draining restored for vampire bites; Fume Filter + Filtered Fume Funnel source recipes/chance restored; Silver Vat stored Silver Deposits byproduct plus source deposit recipe routes/local silver-ingot bridge restored; Kettle bottle automation and source bonus rolls restored; Witches' Oven source sided inventory and sapling fume table restored; Refilling Chest source refill/loot decision reconciled; Poppet Shelf special item pose corrected to the source low half-block baseline; Mirror front/back model UVs corrected for placed and item views; Leech Chest/Refilling Chest GUI item heights normalized to the old neutral item baseline; source-style taglock-to-poppet binding recipe and mob-bound Voodoo/Vampiric resolution restored; VampiricPoppetItem two-taglock fix; Poppet Shelf Compass source recipe, practical loaded-shelf locate, and old six-frame distance icon model restored; Player Compass filled-player-taglock binding route restored; Crystal Ball filled-Taglock-Kit scrying and weighted old prediction reading roster restored in practical form; PlayerCompassItem; WitchHunter poppet disruption; villager witchery goal added; garlic grass-seed parity corrected; Garlic Garland facing/click-through/repulsion and vampire break cancel restored)
Previous local delta (2026-05-29l): Goblin boss chain implemented. **`GoblinGulgEntity`** (melee boss, 400 HP, armor 8): damage resistance scales with distance to nearest `GoblinMogEntity` (immune ≤3 blocks, 20% at ≤6, 50% at ≤9, 80% at ≤16, cap 15 beyond); melee attack power inversely scales (stronger near Mog); invulnerability phase (invulTimer synced, heals 20 HP/10 ticks); teleports toward target every 200 ticks when stuck; passive regen 2 HP/20 ticks; purple boss bar; drops koboldite nuggets + enchanted iron armor + 50% Gulg's Girdle. **`GoblinMogEntity`** (ranged boss, 400 HP): same partner-distance immunity mechanic checking for nearest `GoblinGulgEntity`; `RangedBowAttackGoal` fires arrows faster when target is airborne; passive regen 1 HP/20 ticks; randomly self-equips bow; teleports every 300 ticks; drops koboldite nuggets + enchanted iron armor + 50% Mog's Quiver. Both use `HobgoblinModel` (same goblin proportions) with original 1.7.10 textures (gulg.png, mog.png). **`gulgs_girdle`** and **`mogs_quiver`** promoted from placeholder to registered items. Spawn commands, spawn eggs, creative tab, lang added. BUILD SUCCESSFUL.
Previous delta (2026-05-29k): `VampireEntity` ported (source parity: EntityVampire 1.7.10). Stats: 40 HP, 0.4 speed, 5.0 attack, 20 follow range. UNDEAD type (registered in `minecraft:undead` entity tag). AI: `FloatGoal`, `RestrictSunGoal`, `FleeSunGoal` (flees daylight), melee, door-opening, wander, target non-vampire players + villagers. Daytime: burns in sunlight every 20 ticks, clears target, teleports to coffinPos if >16 blocks away. Nighttime: drains blood from villagers (10% chance per hit → heal 4 HP, add 4 to damageDone); when damageDone ≥ 20 returns to coffin and fills nearest `BloodCrucibleBlockEntity` within 6 blocks. Spawn equipment: vampire_boots always; male (coat/chain-coat + trousers/skirted) or female (corset/chain-coat-ladies + skirted/trousers), chosen randomly in `finalizeSpawn`. No drops (source parity). `VampireRenderer` reuses `VillageGuardModel` (identical UV layout: texOffs(28,46) head) with original `vampire.png` texture (copied from 1.7.10 JAR). `HumanoidArmorLayer` renders vampire clothing automatically. Spawn command, spawn egg, creative tab, lang added. Fixed `WolfmanModel.LAYER_LOCATION` conflict with `WerewolfModel` (both were `witchery:wolfman/main` → wolfman entity renamed to `witchery:wolfman_entity/main`). BUILD SUCCESSFUL.
Previous delta (2026-05-29j): `BabaYagaEntity` ported — 500 HP boss witch with purple boss bar. Source parity from EntityBabaYaga 1.7.10: throws witchery brew approximations (1/3: Slowness/Blindness/Wither via splash potion) or vanilla potions (2/3: distance-based Slowness/Poison via ThrownSplashPotion); caps incoming damage at 15; reduces magic/BYPASSES_ARMOR damage to 15%; reflects 25% physical damage back to non-wooden-weapon player attackers; spawns CaveSpider when hit by undead or untamed pets; teleports toward target (~2%/tick, >100 tick cooldown); applies Blindness V to falling/sprinting targets (5%/tick); ownership system (neutral to owner, despawns after 600 ticks, drops witch ingredients every 100 ticks when near owner). Drops: witch ingredients (2-4 stacks) + enchanted book + Baba's Hat (guaranteed). `BabaYagaModel` uses villager proportions (8×10×8 head, 4×8×4 arms, 4×10×4 legs) with 3-tier stacked witch hat (brim at texOffs(0,98), tiers at 0,76 / 0,87 / 0,95), 128×128 texture matching existing babayaga.png. Spawn command, spawn egg, creative tab, and lang added. BUILD SUCCESSFUL.
Previous delta (2026-05-29i): Silver system implemented. **`BoltEntity extends AbstractArrow`**: crossbow bolt projectile carrying `boltType` (0=wooden, 4=silver); silver type detected by `SilverEvents.isSilverSource()`; fires from `HandBowItem`. **`HandBowItem`**: full bow-mechanic USE/releaseUsing — charged shot support started here and later expanded to Nullifying/Bone/Splitting bolt behavior on 2026-05-31. **`SilverEvents`** (new centralized handler): `onSilverDamage` adds +5 bonus damage to vampires/werewolves from silver sources (melee: item tag `witchery:silver_weapons`; projectile: `BoltEntity.isSilver()`); `onSilveredArmorHit` deals 1.0 back-damage to werewolves attacking wearers of silvered hunter armor (source parity: ItemHunterClothes.getProperties). **`WitcheryItemTags.SILVER_WEAPONS`**: tag key + JSON (silver_sword, silver_bolt). **`WolfmanEntity`**: silver detection delegated to `SilverEvents.isSilverSource()` — now correctly catches bolt hits as well as melee. `WerewolfEvents` duplicate silver handler removed. BUILD SUCCESSFUL.
Previous delta (2026-05-29h): `WolfmanEntity` and `WereVillagerEntity` ported and registered. `WolfmanEntity` (Monster, 40 HP, 0.4 speed, 5.0 attack, 80 follow range): targets players, villagers, witch hunters; silver weapons deal 1.5× damage (cap 15), all other damage capped at 1.0 per hit; every 100 ticks on non-full-moon nights converts back to `WereVillagerEntity` (carrying formerProfession + infectious flag); drops silver deposits/rotten flesh/bone; uses zombie sounds as placeholder (wolf variant system changed in 1.21, no simple static WOLF_AMBIENT etc.). `WereVillagerEntity` (PathfinderMob, 20 HP): wanders peacefully, panics from threats, transforms to `WolfmanEntity` on full-moon night every 100 ticks. `WolfmanModel` ported from ModelWolfman 1.7.10 (64×64, double-jointed hind legs, tail, ears+snout, attack animation); `WolfmanRenderer` with custom `WolfmanRenderState` carrying attackTime. `WereVillagerRenderer` reuses WitchHunterModel with generated `were_villager.png` (sickly/cursed-looking biped). `WerewolfEvents.spawnWereVillagerPlaceholder` replaced with real `WereVillagerEntity` spawn. Spawn commands, spawn eggs, creative tab, and lang added. Parity matrix updated. BUILD SUCCESSFUL.
Previous delta (2026-05-29g): Village Guard AI + flavor text polished. `HurtByTargetGoal(this, VillageGuardEntity.class)` added so nearby guards mutually alert each other when one is attacked. Quote system added: on target acquired the guard shouts a threat-specific war cry (vampire / werewolf / witch / demon / generic monster pools), on sword draw ("Too close for arrows — blades out!"), and on taking a hit (1-in-3 chance). All shouts broadcast as action-bar messages (gold, 20-block radius, 10 s cooldown). `EgassyEntity` converted to same system: 15 fart quotes and 4 injury reactions now use translatable lang keys and display on the action bar in yellow. Fixed lang JSON missing comma that caused all new keys to silently fail (raw keys were showing instead of text). BUILD SUCCESSFUL.
Previous delta (2026-05-29f): `VillageGuardEntity` design locked. Helmet rendering fixed: armor head model now uses 8×8×8 (matching `ZombieVillagerModel.createBaseArmorMesh`) rather than 8×10×8. The 8-unit height stops at y=−2, leaving the face area (y=−2 to y=0) uncovered — exactly how vanilla handles villager-proportion heads. Grey face-wrapping artefact eliminated. Equipment updated to leather-by-default with 1-in-5 iron upgrade per piece and 1-in-20 chainmail chest chance. Two texture variants corrected: guard2 was the Resurrected `plain.png` designed for standard biped UV; replaced with a dark-armour variant generated from guard1 at the correct texOffs(28,46) UV layout. Combat (bow at range, sword close) confirmed working. BUILD SUCCESSFUL.
Previous delta (2026-05-29e): `VillageGuardEntity` ported and registered. 25 HP melee Monster with iron sword and 4 armour points. Attacks: vanilla undead (`EntityTypeTags.UNDEAD`), raiders/illagers (`EntityTypeTags.RAIDERS`), Witchery vampires (entity tag), `CovenWitchEntity`, vanilla `Witch`, `DemonEntity`, and vampire/werewolf players. Neutral to normal players. Two appearance variants (type 0 = silver iron guard, type 1 = blue-grey captain); textures generated (`village_guard1.png`, `village_guard2.png`, 64×32). Sounds: villager hurt/death. Registered in entities, client model layer, renderer, spawn command `/witchery spawn village_guard`, spawn egg (`village_guard_spawn_egg`) added to creative tab. BUILD SUCCESSFUL.
Previous delta (2026-05-29d): `BloodCrucibleBlock` visual correctness pass. Three bugs fixed: (1) **Front wall Z offset** — `PartPose.offset(-4f, 19f, -5f)` corrected to `(-4f, 19f, -4f)`; the old value placed the front wall 1/16 block outside the AABB (world Z 0.1875–0.25 instead of 0.25–0.3125), leaving a visible gap at the front corners of the bowl. (2) **Blood UV texture region** — regenerated `blood_crucible.png` (32×64) so rows 44–63 are solidly bright crimson; the old texture had dark/transitional material at rows 44–50 where the blood top-face samples `texOffs(0,44)`, making blood render near-black. (3) **Blood tint colour** — `BLOOD_COLOR` changed from `colorFromFloat(1.0F, 0.55F, 0.05F, 0.05F)` (dark red multiplier → near-black result) to `colorFromFloat(0.88F, 1.0F, 1.0F, 1.0F)` (white tint with slight transparency — lets the crimson texture show through as-is). BUILD SUCCESSFUL.
Previous delta (2026-05-29c): `GlintWeedBlock` has since been rechecked against 1.7.10 `BlockGlintWeed`: source `animateTick` uses `ParticleEffect.FLAME` with zero velocity, full light `0.9375F`, 1-in-6 density-capped spread, dirt/grass/stone/sand/soul-sand spread substrate, and solid support either below or above the target. The newer port restores the flame particle; the earlier `END_ROD` correction note was stale. `EmberMossBlock` and `SpanishMossBlock` confirmed complete: ember moss fires walkers and spreads, spanish moss grows at double vine speed with high flammability.
Previous delta (2026-05-29b): `LeonardEntity` movement fixed — removed `setNoGravity(true)` which was breaking ground pathfinding (WaterAvoidingRandomStrollGoal and MeleeAttackGoal both stalled because the path planner could not find floor waypoints for a no-gravity Monster). Leonard now walks on the ground like Banshee/Spectre. Visual shade feel preserved: `LeonardRenderer.scale()` applies a whole-body sine hover bob (±0.06 blocks, 0.05 rad/tick) instead of the broken per-part `body.y` offset that was leaving head/arms behind. `LeonardModel.setupAnim` per-part oscillation removed.
Previous delta (2026-05-29a): `LeonardEntity` (Shade of Leonard, 500 HP boss) and `LostSoulEntity` (immunity-phase mob, 3 types) ported and registered. Leonard is neutral until attacked; spawns 6 Lost Souls (2 GREEN/RED/BLACK) at 300 HP threshold; immune to all damage while Lost Souls are alive within 20 blocks; applies Wither II on melee hit (Mortal Coil approximation). GREEN souls take all damage, RED only fire/explosion, BLACK only `BYPASSES_ARMOR` magic damage. Leonard drops Leonard's Urn (promoted from placeholder). Model: GhostModel-layout shadowy humanoid (`LeonardModel`); dedicated `leonard.png` (64×32, dark purple-black shade with green eyes, generated); Lost Souls use orb model (`LostSoulModel`, `lost_soul.png`) with green/red/black ARGB tinting per type. Cauldron summon ritual: Witches' Hat + Refined Evil + Mandrake Root + Tear of the Goddess + Nether Wart + 10 000 altar power → spawns Leonard above cauldron. Both spawnable via `/witchery spawn leonard` and `/witchery spawn lost_soul`. Primer entry added. `ExperimentsScreen` auto-confirmed via `ScreenEvent.Opening` to stop the vanilla experimental-features gate on every world creation. `HobgoblinHutPiece.postProcess` deadlock fixed: `getCurrentDifficultyAt` called on raw `ServerLevel` during worldgen now replaced with `new DifficultyInstance(level.getDifficulty(), 0L, 0L, 0.0F)`. BUILD SUCCESSFUL.
Previous delta (2026-05-28d): Elle + Lilith entities ported and registered. `ElleEntity` (PathfinderMob, 20 HP) wanders and transforms into `LilithEntity` after 20 ticks near lava; spawns from the chicken-blood goblet ritual via `GlassGobletItem`. `LilithEntity` is a 200 HP boss (Monster + RangedAttackMob): caps incoming damage at 12/hit, blocks lethal hits and transitions to friendly mode instead of dying; fires `LargeFireball` at range; friendly Lilith offers three quests — full goblet → vampire induction, garlic → cure vampire, brown mushroom at level 6 → advance to level 7. Auto-level hooks added for levels 4, 5, 9 (auto on blood-power fill). `ImmortalsObservationsBook` updates vampire level cap when read by a vampire. Textures from 1.7.10 source (lilith.png, lilithfol1.png→elle.png). `LilithModel` (64×64, full boss geometry with horns/wings/skirt) + both renderers registered. Blood Crucible inventory icon fix: `assets/witchery/items/blood_crucible.json` was missing from the new-format items/ directory — added, resolves pink/black GUI render. Previous delta: Debug Inspector item added.
Full feature status: `docs/status/FEATURE_MATRIX.md`
Latest baseline change (2026-05-28): switched from 1.7.2-0.19.0 to canonical 1.7.10-0.24.1. Any task note citing "Resurrected" or "1.7.2 source" as baseline is subject to re-verification.

---

## Current plan — 1.7.10 baseline active

Parity baseline is now `1.7.10-0.24.1`. All new work checks decompiled original Witchery first; 1.7.2 and Resurrected are secondary references only. Any existing task note that says "Resurrected-backed" or "1.7.2 source set" should be re-verified against 1.7.10 before that task is actioned.

Five implementation lanes — work one bounded slice at a time per lane:

1. **Custom Brew / Witch's Cauldron** — first priority after parity matrix is built. Source: `WitcheryBrewRegistry.java`, `BlockCauldron.java`, `TileEntityCauldron.java`, `BrewAction*`, `DispersalInstant/Liquid/Gas/Triggered`, 44 potion effect classes. Open: dynamic cauldron action composition, generic brew containers, gas/liquid brew blocks, remaining action roster (tree growth, blight, transpose, undead/demon bane, summon Leonard).

2. **Vampire / Werewolf progression** — Elle/Lilith chain ✅ ported. Remaining: Blood Crucible/Blood Power, Resist Sun, Zombie Respect, exact werewolf Wolf Altar quests, Moon Charm form, Horn of Hunt/Huntsman step, actual vampire villager/entity conversion, cure rite, levels 8–10 full gates. Source: `ExtendedPlayer.java`, `Shapeshift.java`, infusion package, `EntityVampire.java`, `EntityWolfman.java`, `EntityVillagerWere.java`.

3. **Village / mobs / bosses / structures** - major recent progress: Village Guard, Wolfman/WereVillager, Vampire, Baba Yaga, Leonard/Lost Souls, Gulg/Mog, village apothecary/bookshop/witch hut injection, Watch Tower, Keep, first-pass town walls, first-pass Coven Circle, and first-pass Straw Man are live. Next village slice: fresh-village QA for walls/buildings. Broader remaining structure/entity parity: Coven Circle/Straw Man fresh-world QA, Stone Circle naming, Minedrake, illusion mobs, summoned undead, any optional/non-source Baba Yaga hut decision, and later Witch Forest additive work.

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
| 13 | Broom movement | Patched - source-like tick order, player-control vehicle hook, rider-facing visual yaw, modern vehicle authority, passenger-turn yaw sync, and rider-yaw render-state fallback restored; needs in-game flight/visual verification. | The first fix stopped pure client prediction by letting the server advance ridden brooms too, but once `getControllingPassenger()` was restored to return the first living passenger like `Boat`, Neo/Minecraft began treating player-ridden brooms as ordinary locally controlled vehicles. The vehicle-authority pass removes that double-authority path: ridden brooms now run the source-derived acceleration/move/damp pass only when locally controlled, while server-side player-ridden brooms accept the normal vehicle movement packets instead of doing a second movement pass. Rider-facing visual yaw now syncs entity/body/head yaw after movement, during passenger placement, and when the mounted passenger turns; the renderer also forces the mounted broom render state to the rider's interpolated yaw, so the model should no longer preserve the placed body orientation if entity body-rot sync is stale. Riderless brooms continue to drift server-side. |
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
| Custom Brew system | Partial. Foulness/Foaming/Poison tier/dispersal slice is real; Brew of Infection now has the old direct hit semantics for infesting stone-family blocks, zombifying Villagers, and slowing/damaging other living targets; Brew of Love restores the old animal, Villager, and player-enslaved zombie pairing/birth branches in practical drink form; Brew of Wasting restores source-style hunger/weakness/wither falloff plus fragile plant/leaf decay; Brew of Erosion restores source-style acid/entity corrosion plus no-drop terrain erosion/Obsidian return; Brew of Raising restores source-style top-block grave raising with weighted undead and old enslaver metadata; Brew of Bats restores source-style bat burst, short Slowness falloff, no-drop bat tags, and attack-target clearing; Brew of Ink restores source-style compact Blindness falloff plus attack-target clearing; Brew of Hitchcock restores source-style 3-4 temporary target-raven hits; Brew of Cursed Leaping restores source-style Jump Boost IV plus vertical shove falloff; Brew of Frog's Tongue restores source-style thrower pull; Brew of Frost restores source-style water freeze, ice shield, and entity ice cage hits; Brew of Webs restores the old compact cobweb cross; Brew of Thorns restores source-style cactus column hits; Brew of Vines restores source-style horizontal side-hit vine strips; Brew of Sprouting restores source-style face-directed branch growth; Brew of Substitution restores source-style dropped-block replacement in practical item form; Brew of Hollow Tears places the current Hollow Tears stand-in block; Solidifying brews now spread through connected Hollow Tears; Brew of the Depths now runs the old five-minute Water Breathing plus out-of-water Wither timer in practical player-data form; and Brew of Revealing now clears nearby invisibility instead of applying non-source Night Vision/Glowing. Old dynamic cauldron action composition and the source projectile transport are not complete. | Add composed brew action registry, generic brew bottle/bucket/bag support, moving gas/liquid brew behavior beyond the Hollow Tears stand-in, Endless Water/Combustion, source-style brew projectile behavior, infernal ownership behavior for raised/bred undead, and the missing old action roster. |
| Vampirism | Partial. Current slice covers: boolean vampire state, blood drain/thresholds, sunlight, bite, mesmerise (blood-power cost), coffin sleep, clothing hooks, garlic/sun counterplay, Silver Sword/tagged-vampire hooks, Glass Goblet chicken-blood ritual → Elle spawn → lava transform → Lilith boss, Lilith friendly-mode quests (vampire induction / cure / L6→L7), auto-level for levels 1–5 + 9, ImmortalsObservationsBook level-cap unlock, Observations/Torn Page survival access + custom update recipe + readable screen + source-family kill drops, practical consumables, persisted villager blood draining. | Remaining: Blood Crucible/Blood Power, Resist Sun, Zombie Respect, actual vampire villager/entity conversion, cure rite, levels 8–10 full gates, exact Observations screen polish, Baba Yaga/Leonard/Gulg/Mog boss drop mappings. |
| Lycanthropy | Partial. Current werewolf render/buffs/mastery/silver hooks now include animated wolfman rendering that suppresses the vanilla player body through render-state invisibility, the source-style wolf-form sleep refusal, and the Wolftrap full-moon bait gate/timer foundation. They are still not the old Wolf Altar questline. | Add 10-level Wolf Altar quests, Moon Charm/form selection, Horn of the Hunt/Huntsman step, Wolfsbane/silver tuning, a real Werewolf entity, and the entity-backed Wolftrap sheep curse-spread/capture behavior. |
| Machines | Partial despite several "Done" rows. Blocks, GUIs, and recipe slices exist, and the Witches' Oven now has source fume funnel/filter/filtered funnel recipes plus the side/top chance split, source top/bottom/side inventory rules, and the vanilla/Witchery sapling fume table; Silver Vat now stores/drops source-style Silver Deposits from adjacent gold-output growth; Kettle bottle/result automation and source bonus rolls are restored. Exact automation/source behavior still has gaps. | Prioritize hopper QA for restored Spinning Wheel/Distillery/Brazier/Kettle/Oven funnel/Silver Vat automation, remaining modded/edge oven byproduct checks, and any remaining Kettle fluid-container/jar edge decisions. |
| Mirror World and Torment | Implemented/verify. Mirror World portal/room loop and Torment maze are live, and Refilling Chest now matches the source choice of Torment-only hourly dungeon/simple-dungeon refills with the old unbreakable/no-drop profile; it also reads the original 1.7 `WITCLifeTicks` key, Resurrected `WitcheryLifeTicks`, and the port's older cooldown key. Fresh Torment generation now creates the custom Refilling Chest block entities needed for vanilla chest rendering/storage. Dimension chunks can still mislead QA. | Use fresh/deleted dimension folders for QA; reconcile any remaining stale-chunk/player-flow issues with repo truth. |

### High-value implementation backlog from the scan

| Family | Next bounded tasks |
|---|---|
| Vampire counterplay and quest chain | ✅ Garlic/garland, Sun Collector/Grenade chain, Silver Sword/Wooden Stake, Glass Goblet ritual and induction loop, chicken-blood ritual → Elle spawn → lava transform → Lilith boss + Lilith friendly-mode quests (vampire induction / cure / L6→L7), auto-level 1–5 + 9, ImmortalsObservationsBook level-cap unlock, Torn Page drops, Warm/Lilith's Blood consumables, persisted villager blood draining, and Bookshop loot/structure placement for Observations/Torn Page access. Next: Blood Crucible block/BE, Blood Power charges, Zombie Respect, actual vampire entity/villager conversion, cure rite, and levels 8–10 gates. |
| Transformation counterplay | Tie Hunter Dawn armor, Weaken Vampires brews, remaining bolt projectile/crossbow behavior, Silvered Hunter armor, Wolfsbane, Moon Charm, and Wolftrap into explicit vampire/werewolf QA rows. |
| Poppets and taglocks | ✅ Taglock+poppet recipes done, including source-style filled Taglock Kit + Poppet data-copy crafting for player and creature taglocks. ✅ Empty Taglock Kit detection fixed. ✅ Two-taglock Vampiric Poppet + PoppetEvents redirect bug fixed, now resolving living mob victims across loaded server levels. ✅ Voodoo throw/needle/lava/drowning done, now resolving mob-bound targets across loaded server levels. ✅ WitchHunter poppet disruption done. Poppet-family item models now share a flat sprite-card template instead of generated side-strip extrusion, and the shelf top pass lays stored poppets flat on the surface. Poppet Shelf Compass now has the source shaped recipe, a first practical loaded-shelf locate readout using the active shelf registry, and the old 0-5 distance-band icon/model predicate path on the client. Remaining: in-game poppet visual QA, shelf-compass visual/distance QA, and in-game mob-taglock poppet QA. |
| Scrying and waystones | Crystal Ball now has nearby-player subject selection for ordinary predictions, source-style `LastUsedTime` recharge with the old strict `elapsed > 100` gate for the normal prediction path, the old weighted 17-reading prediction roster in immediate-message form, a first bound-Waystone scry hook using the old same-dimension/150-block focus constraint, and filled Taglock Kits can now use the Crystal Ball to consume 500 altar power and start the old 1200-tick held-use scry lifecycle with 20-tick same-dimension loaded-target feedback. Waystone/taglock scrying follows the old item route by consuming altar power without applying the normal ball recharge gate, Waystone helpers now only treat stacks as bound when all coordinate and dimension data is present, and newly bound Waystones now cap at 1 and stamp the old uncommon rarity while blank Waystones keep their 16-stack common-item behavior. Player Compass now binds either by right-clicking a player or by crafting a vanilla Compass with a filled player Taglock Kit, rejects empty/creature taglocks, and reports online/offline plus same-dimension distance feedback. Remaining: stored PredictionManager prediction fulfillment/outcomes, real remote camera packet/view, exact Player Compass dynamic model/needle behavior, and a canonical blank/bound/blooded Waystone identity decision. |
| Infusion, coven, and familiars | Keep infusion charge/rite foundation, but add Witches Hand active ability families, Mystic Branch symbol magic, infernal enthrall/sacrifice creature powers, coven recruitment/quest roster, Seer Stone own-coven support, and missing familiar edge behaviors. |
| Hobgoblins and patrons | Split merchant/hut foundation from old labor/village behavior; add real mining/gathering/breeding/raid behavior, Koboldite Pickaxe miner bonuses, bound Patron worship, Gulg/Mog bosses, and Gulg/Mog reward gear mechanics. |
| Villages, structures, and entities | Expand "Village additions" beyond the now-live Bookshop, Guard Tower/Town Keep, Town Wall, Village Guard, first-pass Coven Circle, and first-pass Straw Man into mayor/cot/vampire feeding support, Coven Circle/Straw Man fresh-world QA, and Stone Circle naming. Add missing boss/NPC rows for Baba Yaga, Leonard/Lost Souls, Gulg/Mog, vampire/werewolf/were-villager mobs, Minedrake, summoned undead, and illusion mobs. |
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
| `alluring_skull` | Dedicated skull-head renderer path is restored; final scale/lighting verification only | Low |
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
| Legacy placeholder items | 142 source/wiki/decompiled item IDs remain registered and visible as inert creative-tab placeholders after the belts/traps/vampire clothing/contracts/wearables/diviners/garlic/dawn-gear/custom-brew/stake/bolt/glass-goblet/observations-book/blood-consumable/fume-filter/silver-deposits/alder-hawthorn-wood/Hollow Tears promotions plus duplicate Resurrected alias cleanup for items that already have canonical port IDs. `contract_ownership` and `contract_torment` are back in the registry after a creative-tab crash exposed the stale list/registry mismatch. All remaining placeholders point at non-shared source/wiki-backed item models or clear alias models; none still use the shared placeholder model. Implement mechanics/recipes or alias-remaps one family at a time | Varies |
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
| Taglock Kit on villagers | Needs verify | `interactLivingEntity` uses the LivingEntity path for mobs and filled Taglock Kits can now craft-bind those creature signatures onto poppets. Test: right-click villager with empty kit, then craft with a poppet and verify the output shows the villager name. |
| Voodoo Poppet bound to villager | Needs verify | Voodoo Poppets can now receive mob taglocks through the source-style binding recipe and resolve the bound UUID across loaded server levels. Test: bind poppet to villager, use throw/needle/lava, verify villager takes effect. |
| VampiricPoppetItem — villager as victim | Needs verify | Two-taglock crafting and direct binding now preserve/resolve living mob victims for redirect. Test: bind source to self, bind victim to villager, take damage, verify villager receives redirect. |
| Vampiric blood feeding from villagers | Partial - source-style persisted drain plus conversion validation restored | Empty-hand vampire bites on villagers now use a persisted 500-blood store. Awake villagers yield the old two-thirds drain, sleeping/mesmerised villagers yield the full drain, clothing drink boost applies, and low-blood villagers take extra harm. A Full Glass Goblet used on a mesmerised, fully drained villager near a Coffin now reaches the source conversion gate and gives non-consuming pending feedback. Remaining: exact 10-level quest counters, village-guard parity, and actual vampire villager/entity conversion. |
| Ritual sacrifice — villager offering | Needs verify | Hell on Earth, Wither summoning, and Demon summoning already specify `VillagerEntity` sacrifice. Test each rite with a caged villager on-site. |
| Curse rite targeting villager via taglock | Needs verify | `Curse Creature` resolves taglock UUID via `ServerLevel.getEntity`. Test: fill taglock from villager, place it in medium infernal circle, verify villager receives Wither/Slowness/Weakness. |
| Blight villager zombification | Needs verify | Blight rite's expand loop zombifies villagers. Test: cast medium infernal Blight near a village, verify villagers turn to zombie villagers as the annulus grows. |
| Werewolf infection of villagers | Patched - placeholder | Full-moon-night villager deaths caused by a werewolf player now roll 10% to cancel the death and spawn a marked zombie-villager placeholder with the original villager data, baby/name state, persistence, and the werewolf as its target. Decompiled Resurrected uses a true `EntityWereVillager`/`EntityWerewolf` conversion path for the final behavior; that entity/renderer remains a separate larger task. |
| Player Compass — scry to named villager | Deferred | Player Compass only binds to players currently. Villager support would need a non-player name resolution path. Defer unless requested. |

#### Why this matters

Vanilla villages are a rich social target that the original Witchery treated as a living ecosystem: vampires fed from villagers, witches cursed them, werewolves infected them, and ritual casters sacrificed them. Making the current port respect that ecosystem makes the open-world feel of Witchery land properly — players can run a coven near a village and use it both as a resource and a risk.

#### Suggested implementation order

1. Verify the three "needs verify" rows first (no code, just QA) — they may already work.
2. Retest vampiric villager feeding now that persisted source-style drain is live.
3. Retest werewolf villager infection in-game on a full-moon night now that the small `LivingDeathEvent` hook is live.
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
| `EntityGoblin` | Base goblin | Medium — goblin/village chain |
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
- **Legacy placeholder follow-up** - `ITEM_REVIEW.md` now has a grouped TODO roster for the 142 remaining inert placeholder items, including exact 1.12 Resurrected alias IDs that no longer silently disappear. Duplicate aliases that already map cleanly to promoted canonical port items have been removed, Alder/Hawthorn wood blocks are now real block items, and Hollow Tears is now a real stand-in block, so the highest-value follow-up is to pick one unresolved family at a time: charms, remaining bolt aliases, hobgoblin rewards, custom-brew containers, remaining block-like items, or exact-alias remaps.

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

- **Fresh village walls + injected buildings** - create or locate fresh plains, desert, savanna, taiga, and snowy villages; verify Witchery apothecaries, bookshops, witch huts, watch towers, and keeps can attach to village roads with the rebalanced weights; verify adaptive walls generate once without artificial delay, do not sink under water, keep paths/gates open on hills, use desert sandstone only for desert/badlands and dirt/stone for savanna, hide gate/path support fill with matching gatehouse blocks, avoid enclosing gates with corners, and do not freeze when `/witchery goto village_*` is used after the bounded locate / arrival-only preload / 30 second cooldown changes.
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
- **Bound Waystone**: stack cap (1 when bound, 16 when blank), complete-binding requirement before treating as bound, old uncommon rarity on bind.
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
