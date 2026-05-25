# QA Parity Plan

Last updated: 2026-05-25

This checklist is the quality gate for the NeoForge port. The goal is not just "feature present", but "behaves, sounds, and looks like original Witchery unless a modern runtime constraint forces a deliberate deviation."

`docs/status/NEXT_FIXES_PLAN.md` stays the ordered implementation roadmap. This file is the parity and verification harness used while working through that roadmap.

Current phase note: the obvious missing-core-rite sweep is nearly exhausted, and the highest-value QA work now shifts toward dispatcher accuracy, refund/upkeep fidelity, Spirit World feel verification, machine/storage GUI cleanup, and then the sound/UI/block-render audits that are easier to trust once rite coverage is no longer the main gap. Use Witchery Resurrected layouts/assets when they preserve the original result more faithfully on NeoForge, but keep original 1.7.10 mechanics and feel as the final judge.

Current unattended execution note:
- Use the current 5-point unattended run order in `docs/status/NEXT_FIXES_PLAN.md`.
- Finish one focused subsystem at a time.
- When a visual issue can be solved by correcting a bad item/model/texture chain, do that before introducing a new custom renderer.
- When a subsystem needs a custom renderer, prefer matching original orientation, brightness, transparency, and inventory pose before adding extra effects.
- Behavior-preserving refactors are explicitly allowed when they remove a real parity blocker, reduce ritual/storage renderer duplication, or make the next parity fix safer to land. Avoid broad cleanup that does not improve correctness, traceability, or verification.

## Reference hierarchy

Use `docs/reference/DECOMPILED_SOURCES.md` as the canonical guide for decompiled source roles and priority. In short:

1. Original Witchery mechanics and edge cases: `C:\claude\witchery\decompiled\witchery-1.7.2-0.19.0`.
2. Witchery Resurrected visual parity, models, textures, GUI clues, and renderer clues: `C:\claude\witchery\decompiled\WitcheryResurrected-1.12.2-0.5.2.4-forge-all`.
3. Witchery Companion bugfixes, compatibility notes, mixins, and community corrections: `C:\claude\witchery\decompiled\witcherycompanion-0.36.1-beta`.
4. The active NeoForge port being changed and verified: `C:\claude\witchery\witchery-port`.
5. NeoForge docs and modern examples are API-shape references only; they do not override Witchery behavior without a documented reason.

## External reference set

Use these as implementation examples when the modern runtime differs from 1.7.10:

- Neo/Forge sound docs:
  - [Sounds](https://docs.minecraftforge.net/en/1.21.x/gameeffects/sounds/)
  - [Sound Providers](https://docs.minecraftforge.net/en/1.21.x/datagen/client/sounds/)
- Working NeoForge example repos:
  - [umfhero/WoodMek-NeoForge-1.21.1](https://github.com/umfhero/WoodMek-NeoForge-1.21.1) *(1.21.1 example — API may differ)*
  - [jeremiahwinsley/pylons](https://github.com/jeremiahwinsley/pylons)

These examples are especially useful for modern screen rendering, sound/event registration, menu syncing, data generation, and block entity rendering patterns.

## QA lanes

### 1. Mechanics parity

For each subsystem:

- Compare the original 1.7.10 class or recipe registry against the current port implementation.
- Preserve original rules first: activation conditions, ranges, timing, upkeep, inventory slot meaning, AI triggers, dimension rules, and failure cases.
- Only modernize structure, not outcomes. Safer helper methods, data-driven registries, and better separation are encouraged if behavior stays the same.

Priority mechanics checklist:

1. Ritual engine: activation traits, queued stages, upkeep, refunds, aborts, altar drain, and multi-pulse effects.
2. Machines: Distillery, Witch's Oven, Spinning Wheel, Kettle, Altar item interactions, and funnel/jar/byproduct edge cases.
3. Poppets and storage: Poppet Shelf, Leech Chest, Refilling Chest, Taglock flow, chunk-loading expectations.
4. Spirit World: Dream Catcher rules, Nightmare/Demonic Nightmare cadence, corpse rules, Flowing Spirit, plant interactions.
5. Blocks and plants: cotton, blood rose, brambles including Void Bramble ritual nulling, grassper, glowglobe, critter snare, wicker bundle, crystal ball, coffin, brazier, fetish blocks.
6. Entities: broom flight, familiar powers, mob AI, renderer-linked gameplay expectations.

Current entity verification note:
- Broom flight should be retested specifically for the old snap-back symptom: mount via item or `/witchery spawn broom`, fly forward and upward for more than several seconds, and confirm the broom no longer returns toward its placement point while preserving pitch lift and Soaring/Owl speed bonuses.
- Witch Hunter aggro should be retested after the target-parity pass: spawn a hunter near a Coven Witch, undead mob, Demon/Blaze-style demonic target, a future entity type tagged `witchery:vampires`, and a player carrying a Voodoo or Vampiric Poppet, then confirm it selects valid supernatural targets without attacking ordinary players.
- Silver damage should be retested against transformed players with both a Silver Ingot and Silver Sword in the attacker's main hand; once vampire mobs land, also verify entity types tagged `witchery:vampires` receive the same bonus.
- Werewolf player rendering should be retested in third-person view after `/witchery werewolf @s true`: confirm the wolfman model appears without the vanilla human body or skin visible underneath, confirm walking/idling drives the wolfman animation, then `/witchery clear` and confirm the normal player skin renders again. With the current default config, the command/cursed state can show wolf form during daytime; exact old full-moon/level-gated transformation rules remain pending. Also confirm transformed/wolfsbane and Primer text no longer show mojibake punctuation/format codes, and confirm ordinary beds plus coffins refuse sleep with "You cannot sleep in wolf form!" while non-werewolf bed taglock sampling and vampire coffin sleep still work.
- Formatted player feedback should be retested anywhere it was historically color-coded: absorb a creature power near an altar, activate/empty a Mystic Branch creature power, defeat the Lord of Torment, destroy a Spirit World sleeping body, and trigger Poppet Protection shield consumption. Confirm the messages remain colored correctly and do not display raw `§`, `Â§`, or `\u00a7`-style control text.
- Stake counterplay should be retested by putting a vampire player to sleep in a coffin, then right-clicking them with a Stake to confirm one Stake is consumed and the sleeping vampire is killed; also confirm non-sleeping vampire players are not staked.
- Chalice filling should be retested in creative and survival: confirm the Witchery creative tab exposes both empty and filled Chalice stacks with the 2D `ingredient.chalice` / `ingredient.chalice_full` sprites, place an empty Chalice on a solid block, right-click it with Redstone Soup, confirm one soup is consumed, the placed chalice visibly fills inside the cup with red liquid/red dust particles, breaking in survival or pick-blocking preserves the filled state and switches the inventory sprite to `ingredient.chalice_full`, and a nearby altar counts the filled Chalice as the stronger +2x bonus instead of the empty +1x bonus.
- Glass Goblet should be retested in survival: craft it from three Glass Bottles plus Glass, confirm an empty goblet uses the blocking pose and a full goblet uses the drinking pose, set a test player as vampire with at least 125 blood, use the goblet to create a named Full Glass Goblet, then have a non-vampire drink it and confirm they become a vampire with the current starting blood plus smoke/level feedback. Also set a test player as werewolf and confirm a vampire-filled goblet is not consumed and shows the no-hybrids refusal. For the old chicken-blood bridge, build the skull/string/redstone/torch Creatures of the Night setup, keep an empty Glass Goblet in the hotbar, kill a Chicken with a Boline near the skull, and confirm the goblet becomes a Full Glass Goblet tagged with Chicken blood; drinking that chicken-blood goblet should empty it without starting vampirism. Using the Chicken-blood goblet on the Skeleton Skull should validate the rite only in the overworld at night with sky access, show pending Elle feedback on success, show the wrong-rite message on failure, and consume neither the skull nor the goblet until Elle exists. For the old villager-conversion gate, use a non-Chicken Full Glass Goblet on a Villager and verify the source-style failure messages for not mesmerised, still having persisted blood, and missing nearby Coffin; once the Villager is mesmerised, drained to 0 blood, and within four blocks of a Coffin, it should show pending vampire-rebirth feedback without consuming the goblet. Elle/Lilith spawning, exact old level gates, and actual villager-to-vampire entity conversion are intentionally still pending.
- Observations of an Immortal should be retested at a crafting table, in hand, and through combat drops: Nether Wart around a Nether Star, Book, and Garlic should craft the book, the tooltip should show 6/16 revealed pages, right-clicking should open a readable vanilla book screen with only those revealed pages, and combining one book with one Torn Page should return the same book advanced by one source-style `Level` step up to 9/9, showing and opening all 16 revealed pages at completion. While carrying an incomplete book in the main inventory or offhand, player kills should have the restored source-family Torn Page chances for Endermen/Zombified Piglins, villagers, and undead, plus guaranteed drops from vanilla bosses and the currently ported source non-boss-false targets Death, Lord of Torment, and Reflection; complete books should stop qualifying for those drops. Once Bookshop structures are wired, generate/check the Bookshop chest table for two Books, three Torn Page pools, and one Observations book; exact old page-position sync/custom screen polish, unported Baba Yaga/Leonard/Gulg/Mog boss mappings, and actual structure placement are still pending.
- Warm Blood and Lilith's Blood should be retested in survival/creative: Warm Blood should refill vampire blood to the current 500 cap and poison non-vampires for 120 ticks; Lilith's Blood should start vampirism for non-vampires with the current practical starting blood and refill vampires to the current cap. Exact old level-up gates remain pending until the 10-level vampire model exists.
- Vampire villager feeding should be retested in survival/creative: empty-hand bite an awake villager and confirm the reduced two-thirds blood drain, then mesmerise or catch a sleeping villager and confirm the full drain, persisted depletion across multiple bites, the dry message when empty, and low-blood harm below the source 250-blood threshold. After draining the villager completely, confirm the Full Glass Goblet conversion gate above can see the 0-blood state. Exact old level-2/level-8 quest counters and actual villager conversion remain pending.
- Brew of Wasting should be retested in survival/creative: place players, mobs, leaves, saplings, flowers, crops, vines, grass, and chalk circles within four blocks, drink the brew, and confirm falloff behavior: players above 10 hunger lose 10 food and receive Hunger + Weakness, non-player living targets receive Wither + Weakness, fragile plant/leaf blocks vanish without drops, and chalk circles remain intact. Source-style thrown brew transport is still pending.
- Brew of Erosion should be retested in survival/creative: right-click mixed breakable blocks and Obsidian to confirm the small sphere is removed without normal drops, fluids/fire/unbreakables/block entities are skipped, melted Obsidian returns as item drops, and one Glass Bottle is returned; right-click a living target with held gear/armor and confirm 8 magic acid damage plus 100 durability corrosion on equipped slots. Drinking should run the same block/entity behavior centered on the player until source-style thrown brew transport is restored.
- Brew of Raising should be retested in survival/creative: right-click the top face of dirt, stone, grass, netherrack, soul sand, sand, cobblestone, and gravel and confirm one brew plus one returned Glass Bottle raises one guaranteed undead, with occasional extra nearby undead using the old 40%/10% extra-spawn odds and the source zombie/skeleton/zombified-piglin weights. Confirm the spawn columns clear without block drops, non-top-face clicks do not consume the brew, and drinking runs the centered practical fallback. Raised undead now carry the old `WITCEnslaverName` metadata plus a UUID tag, but full infernal ownership AI remains pending.
- Brew of Infection should be retested in survival/creative: right-click Stone, Cobblestone, and Stone Bricks and confirm one brew plus one returned Glass Bottle converts them to the matching infested blocks; right-click a Villager and confirm it becomes a Zombie Villager while preserving baby/name/profession data where applicable; right-click another living target and confirm the old small magic hit plus brief Slowness IX instead of the previous poison/weakness aura. Drinking the brew should run the same living-target effect in a small practical radius until source-style thrown-brew transport is restored.
- Brew of Love should be retested in survival/creative: place adult animals and adult Villagers within four blocks, drink the brew, and confirm adult animals enter love mode while Villagers are paired up to 10 pairs, one partner is moved to the other, both parents enter breeding cooldown, and baby Villagers spawn with heart feedback. Baby Villagers should not be selected as parents. Also raise or tag at least two adult Zombies with the drinker's `WITCEnslaverName` or `witchery_enslaver_uuid`, drink the brew near them, and confirm up to 10 pairs move together, emit heart particles, and produce baby Zombies carrying the same owner tags. Source-style projectile transport and deeper infernal minion AI remain pending.
- Fume Funnel and Filtered Fume Funnel should be retested in survival: craft the source Fume Funnel, craft Fume Filter from Glass/Iron/Charged Attuned Stone, right-click a placed unfiltered Fume Funnel with the Fume Filter to upgrade it in-world, craft the alternate Filtered Fume Funnel recipe, place one funnel above an oven and confirm it speeds cooking without improving byproduct odds, then place normal and filtered side funnels to confirm the side-only byproduct chance path and visible filtered model parts.
- Bolt crafting should be retested at a crafting table: String + Sticks + Feathers should yield 9 Wooden Bolts; 3 Wooden Bolts + 1 Silver Deposits should yield 3 Silver Bolts; 6 Wooden Bolts + 1 Silver Ingot should still yield 6 Silver Bolts through the local compatibility bridge; Wooden Bolts + Bone + Ghast Tear should yield 12 Bone Bolts; Null Catalyst + 2 Belladonna + 3 Bone Bolts should yield 3 Nullifying Bolts; and 3 Wooden Bolts + String + Feather should yield 1 Splitting Bolt. Projectile behavior is intentionally still pending.
- Silver Deposits recipe routes should be retested after collecting deposits from a Silver Vat: 9 deposits should craft 1 Silver Ingot as the current no-external-dust fallback; 8 deposits around a Golden Sword should craft a Silver Sword; the source deposit patterns should also craft Silvered Hunter gear and Wolftrap alongside the existing Silver Ingot bridge recipes.
- Poppet Shelf Compass should be retested in survival: craft it from Gold Ingots, Diamonds, a Clock, and a Null Catalyst; place Poppet Shelves at several distances in the same dimension; right-click the compass and confirm the nearest loaded shelf direction/distance updates across the old 0-5 distance bands, and watch the carried/inventory icon move through the matching six shelfcompass frames as loaded shelves get nearer.
- Player Compass should be retested in survival: fill a Taglock Kit from a player, craft it with a vanilla Compass, verify the output tooltip names that player, then right-click in the same dimension, another dimension, and while the target is offline to confirm the locate/offline/realm feedback. Empty Taglock Kits and creature taglocks should not match the recipe.
- Crystal Ball predictions should be retested in survival by taking repeated powered readings and confirming they draw from the old weighted roster families: undead, arrow, ent, fall, ore/treasure, love, crone, friend/rescue, wet, Nether, and coal. Stored prediction fulfillment and forced future events are intentionally still pending.
- Crystal Ball Taglock scrying should be retested in survival: fill a Taglock Kit from a player and a loaded living mob, use each filled kit on a powered Crystal Ball, and confirm the ball consumes 500 altar power, starts the held-use feedback, refreshes target particles every 20 ticks, refuses empty kits, and stops with feedback for missing or cross-dimension targets. True remote camera view is intentionally still pending.
- Taglock-to-poppet binding should be retested in survival: fill a Taglock Kit from a player and a villager, craft one filled kit with an ordinary or Voodoo Poppet and verify the bound name/tooltips survive, then craft two filled kits with a Vampiric Poppet and confirm the first kit becomes the protected source and the second becomes the victim. Use villager-bound ordinary/Voodoo/Vampiric poppets to confirm living mob targets resolve while loaded.
- Witch Hunter black-magic pressure should be retested separately: complete a successful taglock curse or offensive poppet use, wait past the 2-minute mark, and confirm delayed hunter spawns can appear near the caster and keep that caster as their target even if they are otherwise an ordinary player.

### 2. Sound parity

Audit every gameplay loop that should feel distinct in original Witchery:

- Ritual start, sustain, fail, and completion cues.
- Machine idle/working/output cues.
- Block interaction cues: Crystal Ball, Coffin, Brazier, Poppet Shelf, Dream Catcher, altar items, doors/locks.
- Entity cues: broom, familiars, mandrake, nightmares, boss loops, poppet triggers.

For each sound issue:

- Check whether the original used a Witchery sound asset, a vanilla event, or a cadence built from repeated vanilla sounds.
- Confirm the modern port has the right `SoundEvent`/`sounds.json` mapping and correct call site timing.
- Verify sound source, volume, pitch variance, and one-shot vs looped behavior.
- Keep placeholder vanilla sounds only when the original had no custom identity or when the custom asset is still missing.

### 3. Graphics and UI parity

Audit three layers separately:

1. World/block rendering:
   - Block entity renderers
   - translucency/cutout choice
   - placed-item overlays
   - facing/orientation
   - blockstate/model selection
2. Item/inventory rendering:
   - item JSON parents
   - missing textures
   - block item visuals
   - spawn eggs
   - broken wraps or wrong UV assumptions

Recent progress:
- Several broken machine/storage icons now use NeoForge 1.21 `special` item renderers backed by the actual Witchery models instead of invalid static block-model fallbacks.
- Chest-like item renders now point at local `textures/entity/chest/...` resources instead of broken root-level ids.
- Spawn eggs were moved back onto the vanilla `template_spawn_egg` parent after the local template path regressed into pink/black fallbacks.
3. GUI parity:
   - background textures
   - slot alignment
   - progress bars
   - title/inventory label placement
   - hidden or overdrawn layers

Open graphics focus areas right now:

- machine GUI alignment and art verification
- crystal ball translucency
- coffin item presentation
- alluring skull / demon heart item/block presentation
- remaining pink/black block item models

## Per-batch workflow

Every implementation batch should follow this loop:

1. Pick one subsystem from `NEXT_FIXES_PLAN.md`.
2. Find the original 1.7.10 source of truth.
3. Compare against the current port and write down the exact mismatch.
4. Check a modern NeoForge example or official docs only for API shape when needed.
5. Implement the smallest behavior-preserving fix that moves parity forward.
   If a small refactor makes that fix safer or clearer, do it in the same patch and keep the behavioral goal explicit.
6. Run `./gradlew.bat compileJava processResources`.
7. Update `BUGS.md`, `FEATURE_MATRIX.md`, `ITEM_REVIEW.md`, and/or `NEXT_FIXES_PLAN.md` when the repo truth has changed.
8. Keep unresolved visual-only or feel-only issues documented instead of hand-waving them away.

## Regression checklist

After any meaningful batch, verify at least the affected lane:

### Mechanics smoke checks

- ritual activates only under original conditions
- failed activation does not consume the wrong items
- staged rites stop cleanly on broken circles or missing upkeep
- staged rites stop cleanly if their sustaining ring is rebuilt into the wrong chalk family
- staged rites refund both their primary trigger item and any consumed support sacrifices on documented abort paths
- machine slots and outputs match original expectations
- storage blocks preserve or expose the same data the old mod did

### Sound smoke checks

- action produces a sound at the right phase
- repeated processes do not spam every tick
- ambient/idle sounds stop when the underlying state stops
- volume and source category feel sensible in-world

### Graphics smoke checks

- no missing textures
- no broken GUI UV sampling
- translucent blocks look translucent instead of solid/cutout artifacts
- block entity items/overlays render in the expected place
- block item model is recognisable in inventory and hand

## Success criteria

The port is "QA-stable" only when:

- the original mechanic can be explained and traced to matching port behavior,
- the sound cue is present or deliberately documented as deferred,
- the visual representation is recognisable and not misleading,
- and the remaining gap is explicitly tracked in the status docs instead of being rediscovered later.
