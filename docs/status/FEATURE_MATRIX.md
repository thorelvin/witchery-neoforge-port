# Feature Matrix - Witchery NeoForge 1.21.1 Port

Legend: Done / Partial / Not started / Deferred

Last reconciled: 2026-05-19 (Sprints 1-10 plus ritual mechanics/doc sync)
Source priority used: latest `PORTING_NOTES.md` over older `FEATURE_MATRIX.md`/`PORTING_PLAN.md` snapshots.

---

## Branch 1: Brewing

| Feature | Status | Notes |
|---|---:|---|
| Kettle block + tick | Done | Power from nearby altar |
| Kettle recipe system | Done | 33 recipes in JSON; all known originals covered |
| Brew items | Done | 33 brews with full effects - see brew list below |
| Witches Oven | Done | 5-slot oven with jars, funnels, byproducts, and legacy-style smelting outputs restored; current GUI layout now tracks the resurrected-style slot/overlay pass more closely |
| Distillery | Done | Altar-powered; jar requirements; up to 4 outputs; core recipes pushed back toward original; resurrected-style GUI texture/layout pass now matches the current screen/menu path more closely |
| Spinning Wheel | Done | 5-slot altar-powered modifier wheel; fibre + modifiers restored; resurrected-style GUI texture/layout pass now matches the current screen/menu path more closely |
| Altar power system | Done | Validated 6-block altar, 14-block scan radius, cumulative skull bonuses, chalice/Arthana/Mystic Branch bonuses, missing natural power sources, and the basic 3D altar block/item form restored closer to original |
| Brazier recipes | Done | 13 recipes: 3 entity summons, 5 pulsing auras, 5 one-shot buffs |
| Coffin | Done | 2-block bed-like; vampire day-sleep; OPEN lid; BESR model |
| Fetishes | Done | 3 host blocks (Scarecrow/Trent Effigy/Witch's Ladder); 5 effects; Rite of Binding |
| Mutandis / Mutandis Extremis | Done | Pool mutation; Extremis: wolf+owl -> Flying Monkey, crop pool, grass/dirt swap |
| Brew of Ice / Depths / Vines | Done | All implemented as BrewItem subclasses |
| Witches' Primer guidebook | Done | Patchouli optional dep; book.json; 8 categories; 21 entries with atmospheric text + images |
| JEI integration | Done | 5 recipe categories: Kettle, Oven, Distillery, Spinning Wheel, Brazier |

### Brew inventory (all Done)
Sleeping, Frost, Love, Revealing, Raising, Wasting, Erosion, Foulness, Infection, Grotesque, Flowing Spirit, Foaming, Poison, Soaring, Ink, Bats, Hitchcock, Cursed Leaping, Frog's Tongue, Webs, Vines, Thorns, Sprouting, Hollow Tears, Ice, The Depths, Solid Stone, Solid Dirt, Solid Sand, Solid Sandstone, Substitution + Grave/Erosion (GenericBrewItem; obtained via ritual)

---

## Branch 2: Circle Magic

| Feature | Status | Notes |
|---|---:|---|
| Chalk item | Done | 4 variants: ritual, golden, otherwhere, infernal |
| Circle detection | Done | SMALL 3x3, MEDIUM 5x5, LARGE 7x7; `witchery:chalk_circles` tag |
| Ritual execution | Partial | Ingredient-dispatched; circles persist; ingredients consume only on success; staged ticker restored for Storm, Rain of Toads, Nature's Power, Transpose Ores, Summoning, Moving Earth, Raise Column, Crater, Protection, Sanctity, and Imprisonment; active rites now stop when the sustaining circle is broken, do not stack on one center, and verify their own required ring size directly; new casts now retry every present ring from outermost to innermost instead of dying on the first detected size; once a rite path is actually matched it now aborts cleanly on precondition failures instead of falling through into unrelated ring-size fallback; staged rites also retain enough sacrifice state to refund the dropped ingredient again on broken-circle and lost-upkeep aborts; those same mid-flight aborts now play the original snare-style failure cue again instead of ending silently; completely unknown offerings on a valid circle now fail with the original snare-style `Unknown rite` feedback instead of silently doing nothing; the curse/transposition inventory-taglock routes now wait until after their final dampening/poppet-protection abort checks before consuming the taglock; Climate Change also now waits until the rite is successfully queued before consuming its dropped biome focus and glowstone; the protection-family upkeep ratios now track the original fractional altar drain more closely, offline/invalid taglock targets no longer burn the taglock for curses and Transposition Pull, and the dispatcher now enforces a growing set of ritual/infernal/otherwhere chalk-family gates instead of treating all chalk variants as interchangeable |
| Small-circle rites | Done | Infusion, blessing, total eclipse, transposition, prior incarnation now infernal-gated again, infernal blindness now uses the original area-rite behavior again, small infernal witch summoning is restored through a representative `diamond_vapour` route with obstruction checks, Banish Demon is restored through representative ritual `attuned_stone` and bound-`waystone` routes, Ice Shell is restored as a staged coven-scaled hollow ice sphere through a representative `frozen_heart` route, Glyphic Transformation is restored through dropped chalk family/stack-size selection, Rite of Necromancy now restores the empty Spectral Stone output through a representative ritual `spectral_dust` route and the item's hold-to-release use flow again, cloak, cleansing, waystone binding now ritual-gated again, the otherwhere bound-waystone transposition route now uses the sacrificed waystone and teleports nearby entities again instead of only the caster, Manifestation now routes through otherwhere chalk again, and small Raise Column |
| Medium-circle rites | Done | Summoning now uses a long-running creature-allure rite again; Rain of Toads now routes to its staged coven rite again; medium Raise Column is staged again; Sanctity and Imprisonment now sustain altar-fed repulsive/attractive circles again; the monster-only and portable Protection barrier variants now exist again through the modern ingredient dispatcher; Forestation is back on the medium ritual ring instead of the wrong large route; familiar binding/calling now live on the medium ritual ring instead of the wrong small route; infernal Blight now uses its original area/world effect again instead of a single-target curse; medium infernal wither summoning is restored through a representative `wither_skeleton_skull` route with obstruction checks; the medium otherwhere taglock transposition route now teleports the bound target into the circle again instead of collapsing into the large pull mechanic; fertility, charging, Creatures of Night, Sky's Wrath, Infernal Power, Silent Step, waystone pull |
| Large-circle rites | Done | Broiling, Moving Earth now follows a staged earth-parting trench closer to old Part Earth, Broken Earth, large Raise Column, staged Crater, the staged volcano terrain rite is restored through a representative `lava_bucket` route with the original lava-pool precheck, Rite of Shifting Seasons is restored through a representative ritual `attuned_stone` route with dropped biome foci, glowstone-based biome picking, coven-scaled radius, weather side effects, and modern biome-grid resend, Protection now conjures a sustained barrier dome again, the animal-calling Beastial/Call Beasts behavior is back on a large otherwhere route instead of a small generic one and now runs as a staged coven-gated creature-allure rite instead of a one-shot teleport, Nature's Power is restored as a staged large ritual `brew_of_sprouting` rite that greens random patches, spreads plants, and scales outward with coven support again, Transpose Ores is restored as a staged large otherwhere `diamond_vapour` rite that lifts iron ore and, at full coven strength, gold ore toward the circle again, large infernal imp summoning is restored through a representative `refined_evil` route with a coven check, large infernal demon summoning is restored through a representative `tormented_twine` route, Rite of Binding now restores the Spectral Stone creature-binding path for up to three nearby same-type spectral mobs and owner-safe release behavior, Curse of the Wolf, Dragon's Fury, Rain, Hellgate, Torment summoning, Fetish binding, 10 taglock curses, Blight |
| Chalk variants | Done | All four done |
| Glyph blocks | Done | CIRCLE, CIRCLE_GLYPH, CIRCLE_OTHERWHERE, CIRCLE_INFERNAL |
| Full rite roster | Partial | The obvious missing core rites are now covered again through the modern dispatcher: several storm/night/day activation traits are restored, infernal-vs-ritual-vs-otherwhere chalk-family gating has started coming back for the single-circle dispatcher, active rites now sustain against their exact ring size instead of a first-match re-detect, new casts now retry all present ring sizes outer-to-inner instead of failing on the first detected ring, Manifestation plus the restored witch/imp/wither/demon summon rites, Banish Demon, small Ice Shell, Glyphic Transformation, the Spectral Stone rite pair, the staged volcano rite, the staged climate-change rite, the staged Nature's Power rite, and the staged Transpose Ores rite are reachable again, and day/night checks use tighter windows closer to original behavior, but full 1.7.10 upkeep/refund, glyph-family, multi-circle activation, and recipe-registry parity is still incomplete |
| Circle talisman | Done | Store+deploy both work; fixed CIRCLE (golden chalk) variant=0 bug (now variant 4) |

---

## Branch 3: Poppet Magic

| Feature | Status | Notes |
|---|---:|---|
| Poppet binding | Done | UUID/name stored via modern item data |
| Taglock Kit | Done | Collects UUID+name from right-clicked players again and can now also pull stored samples from Blood Rose and Leech Chest; split-stack handling restored closer to original |
| Leech Chest | Partial | 27-slot storage, recent-player queue, Taglock Kit extraction, and stored-player block-drop persistence are restored closer to original; broader in-game parity verification is still open |
| Protection poppets | Done | Death, earth, water, fire, hunger, voodoo, poppet-shield |
| Offensive poppets | Done | Voodoo and Vampiric implemented |
| Tool / Armor protection poppets | Done | Tool: repairs held tool; Armor: repairs worn armor |
| Poppet Shelf | Done | 9-slot poppet-only shelf; bound-poppet lookup now scans server shelves again and shelves with bound poppets keep their chunk loaded so protection can still trigger away from the player; top-of-shelf poppet rendering is back on a clearer 3x3 grid |
| Basic sympathetic poppet | Done | Carried in inventory: mirrors damage taken to bound target (5 charges; consumed after 5 hits; creepy skeleton-rattle sound) |
| Curse of Corrupt Poppet | Done | Restored through a representative medium infernal `voodoo_protection_poppet` route; requires active cat familiar, filled taglock target, destroys up to 10 Voodoo Protection Poppets, and is blocked by Poppet Protection again |

---

## Branch 4: Spirit World / Dream Weaving

| Feature | Status | Notes |
|---|---:|---|
| Spirit World dimension | Done | Overworld-like noise/topography |
| Spirit World entry | Done | Brew/Apple of Sleeping -> `SpiritWorldHelper.enter()` |
| Spirit World exit | Done | Icy Needle, death, `/witchery exit` |
| Spirit inventory rules | Done | Inventory saved/restored; Icy Needle + Mutandis transfer |
| Dream/nightmare roll | Done | Player data; nightmare spawns Nightmare entities |
| Spirit entity | Done | Passive flying mob; drops Subdued Spirit |
| Nightmare entity | Done | Spawn on nightmare entry; teleports to player; replacement pressure now follows the victim-bound delayed respawn loop again |
| Wispy/Disturbed Cotton | Partial | Spreading bush behavior, nightmare harvest swap, Spirit World placement, and disturbed->wispy Flowing Spirit cleansing restored; disturbed cotton still remains a placeable block in the port |
| Dream Weaver (Dream Catcher) | Done | 5 weave effects applied on wake; buff arming follows a configurable sleep threshold again; Spirit World nightmare/demonic scans now only respect the `Nightmares` weave again, and wall placement uses the clicked face like the old hanging block |
| Crystal Ball scrying | Done | 12 predictions; 500 altar power; 5s cooldown |
| Rite of Manifestation | Partial | The rite now grants manifestation time again, Spirit Portal can spend that time to manifest the player into the Overworld as a ghost, timed return and Icy Needle recall are back, but ghost rendering/style polish is still open |

---

## Branch 5: Infusions / Transformations

| Feature | Status | Notes |
|---|---:|---|
| Player data system | Done | 16-field CODEC; server->client sync; transient overflow |
| Infusion types | Done | Light/Overworld/Otherwhere/Infernal passives |
| Vampirism | Done | Bite, blood drain/thresholds, sunburn, buffs; coffin day-sleep |
| Lycanthropy | Done | Wolf state, buffs, silver/wolfsbane; Wolf Altar mastery 0-3 |
| Werewolf howling | Done | Sneak+look-up+right-click -> HowlPayload; frightens mobs; summons 2 wolves; cooldown ticks correctly |
| Werewolf 3D model | Partial | Works; body-hide rendering artifact remains |
| Familiar binding | Done | Medium ritual bind now targets existing nearby tamed familiar-capable creatures again, stores and syncs the bound familiar UUID, familiar kind, and custom familiar name, Call Familiar can recreate the right owl/cat/toad when the original bound entity is gone, and legacy UUID-only familiar saves now backfill their familiar kind on the next successful call |
| Familiar powers | Done | Owl warns, cat repels, toad collects; active familiar lookup now follows the bound familiar across loaded levels instead of only the current one |
| Creature powers / symbiosis | Done | SPEED/JUMP/HEAL/ENDERMAN/BAT; Mystic Branch; altar kill trigger |

---

## Entities

| Entity | Status | Notes |
|---|---:|---|
| Coven Witch | Done | AbstractVillager; names, skins, trades, dialogue |
| Witch Hunter | Done | Ranged+melee; silver bolts; targets vampires/werewolves |
| Banshee | Partial | Gameplay works; GhostModel UV needs audit |
| Spectre | Done | Flying ghost-type |
| Poltergeist | Partial | Gameplay works; GhostModel UV needs audit |
| Treefyd | Done | Ground hostile plant creature |
| Ent | Done | Passive until hit; large model |
| Mandrake mob | Done | MONSTER category; spawn placement registered |
| Demon | Partial | Fireball mob; texture/UV audit still open |
| Owl familiar | Done | Tameable; warns of danger; soaring speed bonus |
| Cat familiar | Partial | Gameplay works; body-proportion model needs tweak |
| Toad familiar | Done | Tameable; collects items |
| Broom | Done | Rideable; enchanted variant (Rite of Infusion) |
| Spirit | Done | Passive flying Spirit World mob |
| Nightmare | Partial | Exists; Charm of Disrupted Dreams anti-strip behavior restored; in-game behavior verification pending |
| Flame Imp | Done | Ranged soulfire; Torment patrol |
| Lord of Torment | Done | 500 HP boss; phase 1->2 loop |
| Death entity | Done | 1000 HP boss; stare-aggro; drops outfit |
| Binky | Done | Spawns with Death; tameable via Binky's Skull |
| Hobgoblin | Done | Forest merchant; 5 trades; lead AI; Hobgoblin Hut |
| Flying Monkey | Done | Created via Mutandis Extremis on wolf+owl; waystone carry; dyeable |

---

## Dimensions

| Dimension | Status | Notes |
|---|---:|---|
| Spirit World | Partial | Entry/exit/inventory; health/hunger save-restore; corpse anchor now restores full-body health closer to original; vegetation; atmosphere (pink fog); block-scan nightmare chance; transferable items; victim UUID; periodic respawn+kill cooldown; nightmare death now feeds the old delayed victim-bound replacement loop again; demonic nightmare events now use the old single-roll overhead harassment split again; dynamic fog (dream/nightmare/demonic); disturbed-cotton cleansing and Flowing Spirit living-entity effects restored; Nightmare defense suppression plus Huntsman's Spear weakening, weak out-of-dimension damage, and Charm of Disrupted Dreams armor-strip immunity are back closer to original; wild/void bramble danger behavior restored; snow-frame Spirit Portal activation, manifestation-duration rite, portal-triggered ghost travel, timed return, and Icy Needle recall are back; remaining work is mostly in-game verification and ghost rendering/style polish |
| Torment | Partial | Boss loop complete; spec-correct maze (6 DFS levels Y=10-85, Force walls, TormentStone floor, 1% mycelium, portal at cell 30,30, 3 refilling chests per level); dimension fixed (has_ceiling=false, red sky, no spawns); TormentHelper entry/exit; Hellgate rite updated |
| Mirror World | Not started | Not started - dark blue fog, reflected overworld terrain, hostile player clones |

---

## Worldgen & Structures

| Feature | Status | Notes |
|---|---:|---|
| Rowan tree / witch sapling | Done | TreeGrower; rare (1-in-120); MOTION_BLOCKING_NO_LEAVES heightmap |
| Rowan berries | Done | Loot/drop chain |
| Wild magical plants | Partial | All 10 plants are registered/worldgen-capable; blood rose taglock storage, spanish moss fast growth, grassper item stash, glowglobe light/particles, and leaping lily buffs are now restored closer to original, but in-game verification and a few edge behaviors are still open |
| Rowan Wood family | Done | Planks, door (UUID-locked + key + keyring), stairs, slab registered |
| Ice block family | Done | Ice Door, Fence, Gate, Pressure Plate registered (created by Brew of Ice World - deferred) |
| Witch hut / abandoned shack | Done | 7x7 hut; Coven Witch; loot chest |
| Apothecary village house | Done | 9x8x6 shop; Coven Witch; full interior |
| Hobgoblin Hut | Done | 5x5x5 oak hut; 1-2 hobgoblin residents; koboldite loot chest |
| Village additions | Not started | Bookshop, guards, walls deferred |
| Torment maze | Partial | Generator done (32x32 cells, per-world seed); missing special rooms: Torment Portal, Refilling Chests, mycelium floor |
| Torment boss loop | Done | Full Flame Imp -> Scroll -> ritual -> LoT -> return flow |

---

## Sprint 8-10 features

| Feature | Status | Notes |
|---|---:|---|
| Fetishes (5 types) | Done | Voodoo Protection, Sentinel, Shrieking, Disorientation, Ghost Walking |
| Brazier conjurations named | Done | Anguish/Fortification/Deathly Veil/Drain Growth/Graveyard Mist correctly wired |
| Coffin block | Done | Vampire day-sleep; shift-open lid; BESR model; spawn point |
| Rowan Wood Door | Done | UUID-locked; key given on placement; keyring combines keys |
| Witches' Primer | Done | Patchouli optional; 8 categories; 21 atmospheric entries; 16 generated images; Witch's Hand + Book recipe |
| JEI recipe categories | Done | Kettle, Oven, Distillery, Spinning Wheel, Brazier |
| Spawn eggs | Done | All 20 entities have eggs; Hobgoblin/Flying Monkey/Binky added |

---

## Debug / QoL Commands

| Command | Status | Notes |
|---|---:|---|
| `/witchery werewolf [targets] [bool]` | Done | |
| `/witchery vampire [targets] [bool]` | Done | |
| `/witchery blood <0-500>` | Done | |
| `/witchery infusion <type>` | Done | NONE/LIGHT/OVERWORLD/OTHERWHERE/INFERNAL |
| `/witchery wolfmastery <0-3>` | Done | Set wolf mastery directly |
| `/witchery soaring [ticks]` | Done | Grant soaring brew buff |
| `/witchery familiar none` | Done | Clear bound familiar |
| `/witchery fullmoon` | Done | Jump time to next full moon |
| `/witchery clear` | Done | Wipe all witchery state |
| `/witchery status` | Done | Multi-line player state dump |
| `/witchery spawn <entity>` | Done | 23 entities spawnable |
| `/witchery dream` / `torment` / `exit` | Done | Dimension teleport + return |
| `/witchery give primer` | Done | Give Witches' Primer directly |
| `/witchery altar` | Done | Scan and report altar power within 20 blocks |

---

## Open / deferred

| Item | Priority | Notes |
|---|---:|---|
| Circle talisman | Done | Store+deploy complete; golden chalk bug fixed |
| Full persistent ritual queue parity | Quick | Storm/Toads now stage over time, but the original tile-entity ritual queue/refund system is only partially recreated |
| Werewolf body-hide rendering | Polish | Model works; artifact in playermodel hide |
| Banshee/Poltergeist GhostModel UV | Polish | One audit pass needed |
| Cat familiar model proportions | Polish | Body geometry tweak |
| Demon UV/texture | Polish | Partially fixed; verify in-game |
| Nightmare behavior verification | Polish | Needs in-game test |
| Torment maze | Large | Placeholder terrain only |
| Mirror World dimension | Large | Not started |
| Village additions | Large | Bookshop, guards deferred |
| Custom Brew system | Large | Level 1-5 capacity/power/dispersal system - not started |
| Rite of Manifestation | Partial | Duration rite, Spirit Portal ghost travel, timed return, and Icy Needle recall are back; ghost rendering/style polish is still open |
