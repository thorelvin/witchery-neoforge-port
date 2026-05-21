# Feature Matrix - Witchery NeoForge 1.21.1 Port

Legend: Done / Partial / Not started / Deferred

Last reconciled: 2026-05-21 (trader menu pass + setup stations QA hut pass + Torment portal/verifier follow-up + fetish visual parity pass + rowan spacing + Mutandis bush pool correction + base alchemy item pass + kettle/oven polish + hobgoblin resurrected model pass + hobgoblin launch-structure fix + startup warning cleanup + ritual refund lifecycle pass + ritual chalk-family sustain pass + ritual family-gate pass + chalk block shape/model fix + infusion charge-state groundwork + infusion recharge rite)
Source priority used: latest `PORTING_NOTES.md` over older `FEATURE_MATRIX.md`/`PORTING_PLAN.md` snapshots.

---

## Branch 1: Brewing

| Feature | Status | Notes |
|---|---:|---|
| Kettle block + tick | Done | Power from nearby altar; fence/wall/iron-bar side support now works again for the original between-post mounting style, and the kettle no longer exposes a full-cube support face so fence-post rigs stop visually connecting into it |
| Kettle recipe system | Done | 33 recipes in JSON; all known originals covered |
| Brew items | Done | 33 brews with full effects - see brew list below |
| Witches Oven | Done | 5-slot oven with jars, funnels, byproducts, and legacy-style smelting outputs restored; the missing `clay_jar_soft -> clay_jar` furnace step is back so the early jar chain is usable again in survival; current GUI layout now tracks the resurrected-style slot/overlay pass more closely, the screen blits the intended panel/overlay subregions directly instead of compressing the full sheet, and the animated cook arrow is back on the correct main-arrow lane instead of drawing offset below it |
| Distillery | Done | Altar-powered; jar requirements; up to 4 outputs; core recipes pushed back toward original; the clay-jar progression is usable again and omitted base intermediates like `odour_of_purity` / `oil_of_vitriol` are now exposed in the Witchery tab for faster QA; resurrected-style GUI texture/layout pass now matches the current screen/menu path more closely, and the screen now blits the intended panel/overlay subregions directly instead of compressing the full sheet |
| Spinning Wheel | Done | 5-slot altar-powered modifier wheel; fibre + modifiers restored; resurrected-style GUI texture/layout pass now matches the current screen/menu path more closely, and the screen now blits the intended panel/overlay subregions directly instead of compressing the full sheet |
| Altar power system | Done | Validated 6-block altar, 14-block scan radius, cumulative skull bonuses, chalice/Arthana/Mystic Branch bonuses, missing natural power sources, and the basic 3D altar block/item form restored closer to original |
| Brazier recipes | Done | 13 recipes: 3 entity summons, 5 pulsing auras, 5 one-shot buffs |
| Coffin | Done | 2-block bed-like; vampire day-sleep; OPEN lid; BESR model |
| Fetishes | Done | 3 host blocks (Scarecrow/Trent Effigy/Witch's Ladder); 5 effects; Rite of Binding; original sturdier hardness and very high blast resistance restored closer to 1.7.10; scarecrow/trent once again use dedicated visuals, fetish placement now faces the placer instead of rendering backward after placement, and Witch's Ladder is back on its flat model path with explicit cutout rendering so the original ladder sprite no longer draws like an opaque placeholder block |
| Mutandis / Mutandis Extremis | Done | Pool mutation; normal/Extremis plant pool now includes the vanilla `bush` shrub, upper-half double-plant clicks still normalize safely, and Extremis keeps the crop pool plus grass/dirt swap; wolf+owl -> Flying Monkey |
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
| Circle detection | Done | SMALL 3x3, MEDIUM 5x5, LARGE 7x7; `witchery:chalk_circles` tag; all four chalk marks now use a flat thin floor-glyph block path again instead of rendering/outing like full cubes |
| Ritual execution | Partial | Ingredient-dispatched; circles persist; ingredients consume only on success; staged ticker restored for Storm, Rain of Toads, Nature's Power, Transpose Ores, Summoning, Moving Earth, Raise Column, Crater, Protection, Sanctity, and Imprisonment; active rites now stop when the sustaining circle is broken, do not stack on one center, verify their own required ring size directly, and keep the chalk family they were cast on instead of sustaining on any same-size replacement loop; new casts now retry every present ring from outermost to innermost instead of dying on the first detected size; once a rite path is actually matched it now aborts cleanly on precondition failures instead of falling through into unrelated ring-size fallback; staged rites also retain enough sacrifice state to refund the dropped ingredient again on broken-circle and lost-upkeep aborts, and Climate Change now retains and refunds its consumed biome-focus plus glowstone support sacrifices too if the queued rite later collapses; those same mid-flight aborts now play the original snare-style failure cue again instead of ending silently; completely unknown offerings on a valid circle now fail with the original snare-style `Unknown rite` feedback instead of silently doing nothing; the curse routes plus the remaining medium otherwhere taglock-transposition route now wait until after their final dampening/poppet-protection abort checks before consuming the taglock, and the obsolete large filled-taglock pull false-positive is gone; Climate Change also now waits until the rite is successfully queued before consuming its dropped biome focus and glowstone; Broiling, Broken Earth, Moving Earth, Raise Column, Volcano, and Flying Ointment's broom infusion now enforce their original infernal-vs-ritual chalk family again instead of activating from any same-size loop; the first paired-ring activation requirements are also back for Flying Ointment, Wither summoning, the four player infusion rites, and Hell on Earth's mixed-ring curse; fertility plus most of the protection-family rites now also route from the closer original small/medium ring sizes again, once again ask for their missing startup altar costs/support offerings, and the protection-family upkeep ratios now track the original fractional altar drain more closely; the dispatcher also now enforces a growing set of ritual/infernal/otherwhere chalk-family gates instead of treating all chalk variants as interchangeable |
| Small-circle rites | Done | Infusion, blessing, total eclipse, fertility, sanctity, imprisonment, and the monster-only Protection barrier now route through small circles again; Sanctity and Imprisonment once more ask for redstone plus their original 500 altar-power startup cost, and the monster-only barrier once more asks for redstone plus its startup altar cost before the restored upkeep loop takes over. Weather Call Storm now also once more expects `wood_ash` beside its snow/water trigger and once more consumes 1000 altar power before the staged storm queue begins. Small Raise Column now also once more expects `brew_of_sprouting`, cactus, and gunpowder around the representative stone trigger before the staged lift begins. The four player infusions once more expect their original matching outer medium companion rings as well (ritual for Light/Overworld, otherwhere for Otherwhere, infernal for Infernal) instead of firing from lone small circles, and they now also restore their original startup altar costs again: 2000 for Light, 4000 for Overworld, 4000 for Otherwhere, and 4000 for Infernal; transposition, prior incarnation now infernal-gated again, infernal blindness now uses the original area-rite behavior again, Sky's Wrath now also routes from the original small ritual circle again instead of the wrong medium ring, small infernal witch summoning is restored through a representative `diamond_vapour` route with obstruction checks, small ritual familiar summoning is restored through representative `drop_of_luck` again and now spawns a bound Witch's Cat into the circle while also once more expecting an `egg`, `gold ingot`, and `Arthana` plus its original 2000 altar-power startup cost, Banish Demon is restored closer to the original split family again with `Charged Attuned Stone` driving the portable variant while the plain `Attuned Stone` fallback now also restores the original 2000 altar-power cost for the non-portable path, Ice Shell is restored as a staged coven-scaled hollow ice sphere through a representative `frozen_heart` route, Glyphic Transformation is restored through dropped chalk family/stack-size selection, Rite of Necromancy now restores the empty Spectral Stone output through a representative ritual `spectral_dust` route, the item's hold-to-release use flow again, its old `Necro Stone` / `Congealed Spirit` / `Condensed Fear` / `Boline` support set, and its original 6000 altar-power startup cost; cloak, cleansing, and waystone binding/copying are now ritual-gated again with base binding once more expecting Ender Dew plus glowstone dust supports before summoning a fresh bound waystone item at the ritual center and copy binding once more expecting the offered unbound waystone plus Ender Dew and diamond supports, the otherwhere bound-waystone transposition route now uses the sacrificed waystone and teleports nearby entities again instead of only the caster, Manifestation now routes through otherwhere chalk again, and small Raise Column |
| Medium-circle rites | Done | Summoning now uses a long-running creature-allure rite again; Rain of Toads now routes to its staged coven rite again; medium Raise Column is staged again and now once more expects `brew_of_sprouting`, cactus, and redstone around the representative stone trigger before the lift will queue. The broader Protection dome now runs from the medium ritual ring again, once more asks for its glowstone-dust support offering plus 1000 altar power to start, and the portable barrier variant remains reachable on its original medium circle while now expecting `Charged Attuned Stone` again instead of plain `Attuned Stone` and once more asking for its obsidian support offering. Forestation is back on the medium ritual ring instead of the wrong large route; familiar binding/calling now live on the medium ritual ring again while small ritual familiar summoning is restored separately on `drop_of_luck`, so the familiar trio no longer collapses onto the wrong circle sizes, and shared `whiff_of_magic` now chooses bind-vs-call based on whether a real bindable familiar is present; Flying Ointment's broom infusion now routes from the original medium ritual ring again, also expects the inner small ritual companion ring, and once more consumes its original 3000 altar-power startup cost before the broom is enchanted; the Charging family is functionally back again: `chargestone` now routes from the original paired ritual-circle stack by charging a plain Attuned Stone into a Charged Attuned Stone with its original glowstone-dust, redstone, wood-ash, and quicklime support offerings plus 2000 altar power, infused players once more carry synced `200/200` current/max charge state from the base infusion rites, and `infusionrecharge` is restored as the original indefinite paired-circle `Potion of Regeneration` upkeep rite that feeds 10 charge per second at 40 altar power per second; Nature's Power now also routes from its original medium ritual ring again instead of the wrong large route, now once more expects its mixed Witchery-plus-vanilla sapling offering set plus a `Charged Attuned Stone` alongside `brew_of_sprouting`, and its staged active-rite state now sustains on that same medium ring instead of collapsing under an accidental large-ring check; infernal Blight now uses its original area/world effect again instead of a single-target curse; the medium otherwhere taglock transposition route now teleports the bound target into the circle again, and the old non-original large pull fallback is removed instead of shadowing it; `bindspectral` now restores the Spectral Stone creature-binding path from the original medium ritual ring again while also once more expecting `Spectral Dust` plus a `Boline` and its original 5000 altar-power startup cost before the spirits are consumed, Fetish binding now also routes from the original medium ritual ring again, and both keep the nearby same-type/owner-safe spirit-binding behavior restored in the port; charging, Creatures of Night, Infernal Power, Silent Step, waystone pull |
| Large-circle rites | Done | Broiling, Moving Earth now follows a staged earth-parting trench closer to old Part Earth, Broken Earth, large Raise Column now once more expects `brew_of_sprouting`, cactus, and glowstone dust around the representative stone trigger before the staged lift will queue, staged Crater, the staged volcano terrain rite is restored through a representative `lava_bucket` route with the original lava-pool precheck, Rite of Shifting Seasons is restored through a representative ritual `attuned_stone` route with dropped biome foci, glowstone-based biome picking, coven-scaled radius, weather side effects, and modern biome-grid resend, the animal-calling Beastial/Call Beasts behavior is back on a large otherwhere route instead of a small generic one and now runs as a staged coven-gated creature-allure rite instead of a one-shot teleport, Transpose Ores is restored as a staged large otherwhere rite that lifts iron ore and, at full coven strength, gold ore toward the circle again, now once more expects its ender-pearl, iron-ingot, blaze-powder, and `Charged Attuned Stone` support set around the representative `diamond_vapour` trigger, large infernal imp summoning is restored through a representative `refined_evil` route with a coven check, large infernal wither summoning is restored through a representative `wither_skeleton_skull` route with obstruction checks and once again expects the inner medium infernal companion ring, `Hell on Earth` now also expects its original infernal/otherwhere/infernal mixed-ring stack again instead of a generic large-ring `nether_star` path, large infernal demon summoning is restored through a representative `tormented_twine` route, Curse of the Wolf, Dragon's Fury, Rain, Hellgate, Torment summoning, 10 taglock curses, Blight |
| Chalk variants | Done | All four done |
| Glyph blocks | Done | CIRCLE, CIRCLE_GLYPH, CIRCLE_OTHERWHERE, CIRCLE_INFERNAL |
| Full rite roster | Partial | The obvious missing core rites are now covered again through the modern dispatcher: several storm/night/day activation traits are restored, infernal-vs-ritual-vs-otherwhere chalk-family gating has started coming back for the single-circle dispatcher, active rites now sustain against their exact ring size and remembered chalk family instead of a loose first-match re-detect, more direct rite routes like Broiling, Broken Earth, Moving Earth, Raise Column, Volcano, Flying Ointment, fertility, most of the protection family, the full old Charging family, and the missing small `summonfamiliar` path now also require closer original ring/family behavior again, the first paired-ring activation requirements are back for Flying Ointment, `summonwither`, the four player infusion rites, and `Hell on Earth`, new casts now retry all present ring sizes outer-to-inner instead of failing on the first detected ring, Manifestation plus the restored witch/imp/wither/demon/familiar summon rites, Banish Demon, small Ice Shell, Glyphic Transformation, the Spectral Stone rite pair, the staged volcano rite, the staged climate-change rite, the staged Nature's Power rite, and the staged Transpose Ores rite are reachable again, day/night checks use tighter windows closer to original behavior, and the old infusion-energy meter now exists again through synced current/max player charge state, but full 1.7.10 upkeep/refund, glyph-family, multi-circle activation, and recipe-registry parity is still incomplete because the broader ritual queue/dispatcher architecture remains simplified |
| Circle talisman | Done | Store+deploy both work; fixed CIRCLE (golden chalk) variant=0 bug (now variant 4); blank talisman binding no longer depends on the wrong large-circle dispatcher and now stores absent ring slots as 0 instead of defaulting them back to ritual glyphs |

---

## Branch 3: Poppet Magic

| Feature | Status | Notes |
|---|---:|---|
| Poppet binding | Done | UUID/name stored via modern item data |
| Taglock Kit | Done | Collects UUID+name from right-clicked players again and can now also pull stored samples from Blood Rose and Leech Chest; Blood Rose extraction once again refuses your own stored sample and offline players instead of minting a false taglock; split-stack handling restored closer to original |
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
| Player data system | Done | 16-field CODEC; server->client sync; transient overflow; nested infusion charge state now persists current/max infusion energy without blowing the DFU field cap |
| Infusion types | Done | Light/Overworld/Otherwhere/Infernal passives |
| Vampirism | Done | Bite, blood drain/thresholds, sunburn, buffs; coffin day-sleep |
| Lycanthropy | Done | Wolf state, buffs, silver/wolfsbane; Wolf Altar mastery 0-3 |
| Werewolf howling | Done | Sneak+look-up+right-click -> HowlPayload; frightens mobs; summons 2 wolves; cooldown ticks correctly |
| Werewolf 3D model | Partial | Works; body-hide rendering artifact remains |
| Familiar binding | Done | Small ritual familiar summoning is back through representative `drop_of_luck`, medium ritual bind now targets existing nearby tamed familiar-capable creatures again, shared `whiff_of_magic` now chooses bind when a real candidate is present and otherwise falls through to call, stores and syncs the bound familiar UUID, familiar kind, custom familiar name, sit state, and name-visibility flag, Call Familiar can recreate the right owl/cat/toad when the original bound entity is gone, and legacy UUID-only familiar saves now backfill their familiar kind on the next successful call |
| Familiar powers | Done | Owl warns, cat repels, toad collects; active familiar lookup now follows the bound familiar across loaded levels instead of only the current one |
| Creature powers / symbiosis | Done | SPEED/JUMP/HEAL/ENDERMAN/BAT; Mystic Branch; altar kill trigger |

---

## Entities

| Entity | Status | Notes |
|---|---:|---|
| Coven Witch | Done | AbstractVillager; names, skins, trades, dialogue, and stable merchant opening via the vanilla trading-player flow |
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
| Hobgoblin | Done | Forest merchant; 5 trades; lead AI; Hobgoblin Hut; trade GUI now stays open through the same trading-player flow vanilla merchants use, and the entity now renders on a dedicated Resurrected-style `ModelGoblin` body instead of a scaled zombie fallback |
| Flying Monkey | Done | Created via Mutandis Extremis on wolf+owl; waystone carry; dyeable |

---

## Dimensions

| Dimension | Status | Notes |
|---|---:|---|
| Spirit World | Partial | Entry/exit/inventory; health/hunger save-restore; corpse anchor now restores full-body health closer to original; vegetation; atmosphere (pink fog); block-scan nightmare chance; transferable items; victim UUID; periodic respawn+kill cooldown; nightmare death now feeds the old delayed victim-bound replacement loop again; demonic nightmare events now use the old single-roll overhead harassment split again; dynamic fog (dream/nightmare/demonic); disturbed-cotton cleansing and Flowing Spirit living-entity effects restored; Nightmare defense suppression plus Huntsman's Spear weakening, weak out-of-dimension damage, and Charm of Disrupted Dreams armor-strip immunity are back closer to original; wild/void bramble danger behavior restored; snow-frame Spirit Portal activation, manifestation-duration rite, portal-triggered ghost travel, timed return, and Icy Needle recall are back; remaining work is mostly in-game verification and ghost rendering/style polish |
| Torment | Partial | Boss loop now reuses one shared random maze level again, sends players to the legacy maze start, and respawns the phase-2 Lord on the old fixed lane at half health. Legacy `GenerateMaze.display` layout is restored too (6 DFS levels Y=10-85, origin x=-23/z=6, invisible Force walls/ceilings, vanilla Bedrock floor/portal frame, rare mycelium patches, old portal chamber, and 3 refilling chest rooms per level); the stray standalone `torment_chambers` worldgen path is removed so fresh/deleted Torment chunks only use that maze. Dimension settings are fixed too (`has_ceiling=false`, skylight, fixed day, ambient_light=0.15, red sky, no spawns); Force restores old light opacity 0 behavior; the Torment exit now renders as a thin old-style portal plane with ambient sound/flames again; `/witchery verify torment` now audits the full legacy footprint server-side before the remaining manual fresh-world QA pass; Hellgate rite updated |
| Mirror World | Not started | Not started - dark blue fog, reflected overworld terrain, hostile player clones |

---

## Worldgen & Structures

| Feature | Status | Notes |
|---|---:|---|
| Rowan tree / witch sapling | Done | TreeGrower; rare (1-in-120); `WORLD_SURFACE_WG` placement now avoids dropping Rowan trunks into existing forest canopies, and a custom `tree_clearance` placement filter now keeps rowan generation away from nearby logs/leaves so it gets a more open, special footprint |
| Rowan berries | Done | Loot/drop chain |
| Wild magical plants | Partial | All 10 plants are registered/worldgen-capable; blood rose taglock storage plus the old self-sample/offline-player drain safeguards, ember moss fire/spread/shears behavior, spanish moss fast growth plus non-ladder/high-flammability behavior, bramble hazard/spread plus original toughness, void bramble teleport plus owner-column removal/unbreakable/ritual-nulling profile, grassper item stash plus comparator output plus facing-aware model orientation, glint weed full light/flame ambience/spread, glowglobe light/particles plus no-drop/no-pick ambient behavior, leaping lily buffs, and critter snare catch/release behavior are now restored closer to original, but in-game verification and a few edge behaviors are still open |
| Rowan Wood family | Done | Planks craft from rowan logs and are tagged as planks; rowan log, wood, planks, sapling, and lower-half door drops now mirror vanilla self-drop behavior; Rowan stairs and slab are restored with rowan plank models, recipes, tags, loot, and old `5/20` flammability; rowan door has original `5.0F` hardness plus UUID-locked key/keyring flow |
| Ice block family | Done | Ice Door, Fence, Gate, Pressure Plate registered (created by Brew of Ice World - deferred) |
| Wicker Bundle / Wickerman | Partial | Plain and Bloodied Wicker Bundles now use rotated-pillar placement, original 20/20 flammability, block-backed inventory models, self drops, and the Resurrected bloodied textures. Demonic Blood can right-click plain wicker to convert up to five nearby blocks to bloodied wicker like old Witchery, and flint-and-steel on a valid old Witchery bloodied wickerman now checks the full documented 20-bundle shape, can ignite from any bloodied bundle in the structure, burns the matching blocks, and summons a persistent Horned Huntsman boss with first-pass AI, old random melee damage, original ranged damage scaling, old iron-golem step/throw cues, drops, boss bar, spawn egg, and renderer. `/witchery spawn wickerman` now places a summon-valid bloodied wickerman five blocks in front of the player, oriented from the player's facing and using a more hay-man-like mixed pillar-axis pose for quick QA/setup. Needs in-game verification for the exact Huntsman model pose/scale, arrow knockback, wolf behavior, and wickerman structure offsets. |
| Witch hut / abandoned shack | Done | 7x7 hut; Coven Witch; loot chest |
| Apothecary village house | Done | 9x8x6 shop; Coven Witch; full interior |
| Hobgoblin Hut | Done | 5x5x5 oak hut; 1-2 hobgoblin residents; koboldite loot chest |
| Village additions | Not started | Bookshop, guards, walls deferred |
| Torment maze | Partial | Legacy 31x31 DFS generator restored with old origin, invisible Force wall/ceiling blocks, Bedrock floor, rare mycelium, portal chamber, and 3 refilling chest rooms per level. The Torment exit now uses a thin axis-based portal plane instead of a placeholder full cube, and the obsolete random `torment_chambers` structure-set path is removed so fresh/deleted Torment chunks now verify the maze generator alone; `/witchery verify torment` now checks the full footprint/chambers/chests/atmosphere data server-side without missing the right chest room, but final in-game verification is still open |
| Torment boss loop | Done | Full Flame Imp -> Scroll -> ritual -> LoT -> shared random Torment level -> fixed phase-2 maze spawn -> return flow |

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
| `/witchery status` | Done | Multi-line player state dump, now including infusion current/max energy |
| `/witchery spawn <entity>` | Done | 23 entities spawnable |
| `/witchery dream` / `torment` / `exit` | Done | Dimension teleport + return |
| `/witchery verify torment` | Done | Server-side Torment audit for fresh/deleted chunks, full legacy footprint/layout parity, and atmosphere data |
| `/witchery give primer` | Done | Give Witches' Primer directly |
| `/witchery altar` | Done | Scan and report altar power within 20 blocks |
| `/witchery setup stations` | Done | Clears a 20x20 pad in front of the player without digging below door/player height, then rebuilds a player-facing 11x11 Rowan QA hut 5 blocks ahead with a 3-deep stone-brick foundation, a one-block-lower shell, player-height altar/machines/chest/traders, a cobblestone-supported kettle over a campfire plus water cauldron, distillery, witch's oven with fume funnel, spinning wheel, stocked double chest, indoor Coven Witch + Hobgoblin traders, ceiling Glowglobes, and a horizontal Rowan log header above the door; the altar sits off the back wall now, the window panes recompute against the finished frame so they visually connect to the Rowan walls, and oven/distillery/spinning wheel preload one ready recipe each |

---

## Open / deferred

| Item | Priority | Notes |
|---|---:|---|
| Circle talisman | Done | Store+deploy complete; golden chalk bug fixed; blank binding no longer large-circle locked |
| Full persistent ritual queue parity | Quick | Storm/Toads now stage over time, Climate Change now preserves/refunds its consumed support sacrifices on queued-abort paths, and active rites now keep their original chalk family during sustain, but the original tile-entity ritual queue/refund system is still only partially recreated |
| Werewolf body-hide rendering | Polish | Model works; artifact in playermodel hide |
| Banshee/Poltergeist GhostModel UV | Polish | One audit pass needed |
| Cat familiar model proportions | Polish | Body geometry tweak |
| Demon UV/texture | Polish | Partially fixed; verify in-game |
| Nightmare behavior verification | Polish | Needs in-game test |
| Torment maze | Partial | Legacy block/layout parity restored, the exit portal now renders as a thin old-style plane again, the stray random chamber structure-set path is removed, and `/witchery verify torment` now covers the repeatable full-footprint server-side audit; still needs fresh-dimension in-game verification |
| Mirror World dimension | Large | Not started |
| Village additions | Large | Bookshop, guards deferred |
| Custom Brew system | Large | Level 1-5 capacity/power/dispersal system - not started |
| Rite of Manifestation | Partial | Duration rite, Spirit Portal ghost travel, timed return, and Icy Needle recall are back; ghost rendering/style polish is still open |
