# QA Parity Plan

Last updated: 2026-05-31

This checklist is the quality gate for the NeoForge port. The goal is not just "feature present", but "behaves, sounds, and looks like original Witchery unless a modern runtime constraint forces a deliberate deviation."

`docs/status/NEXT_FIXES_PLAN.md` stays the ordered implementation roadmap. This file is the parity and verification harness used while working through that roadmap.

Current phase note: the obvious missing-core-rite sweep is nearly exhausted, and the highest-value QA work now shifts toward dispatcher accuracy, refund/upkeep fidelity, Spirit World feel verification, machine/storage GUI cleanup, and then the sound/UI/block-render audits that are easier to trust once rite coverage is no longer the main gap. Use Witchery Resurrected layouts/assets when they preserve the original result more faithfully on NeoForge, but keep original 1.7.10 mechanics and feel as the final judge.

Current unattended execution note:
- Use the current 5-point unattended run order in `docs/status/NEXT_FIXES_PLAN.md`.
- Finish one focused subsystem at a time.
- When a visual issue can be solved by correcting a bad item/model/texture chain, do that before introducing a new custom renderer.
- When a subsystem needs a custom renderer, prefer matching original orientation, brightness, transparency, and inventory pose before adding extra effects.
- Behavior-preserving refactors are explicitly allowed when they remove a real parity blocker, reduce ritual/storage renderer duplication, or make the next parity fix safer to land. Avoid broad cleanup that does not improve correctness, traceability, or verification.

Debug logging note (2026-05-31): `config/witchery-common.toml` defaults `debug = true` during the village wall/goto freeze chase. Look for `[VillageWalls:Debug]` lines around queue state, loaded-only reference lookups, safe-locate probes, goto full-chunk prep, forced ticket requests, automatic footprint readiness, and wall-generator entry. If logs become too noisy after the freeze path is settled, set `debug = false` and restart.

## Reference hierarchy

Use `docs/reference/DECOMPILED_SOURCES.md` as the canonical guide for decompiled source roles and priority. In short:

1. Original Witchery parity baseline: `C:\claude\witchery\decompiled\witchery-1.7.10-0.24.1`.
2. Historical original Witchery context only: `C:\claude\witchery\decompiled\witchery-1.7.2-0.19.0`.
3. Witchery Resurrected visual parity, models, textures, GUI clues, and renderer clues: `C:\claude\witchery\decompiled\WitcheryResurrected-1.12.2-0.5.2.4-forge-all`.
4. Witchery Companion bugfixes, compatibility notes, mixins, and community corrections: `C:\claude\witchery\decompiled\witcherycompanion-0.36.1-beta`.
5. The active NeoForge port being changed and verified: `C:\claude\witchery\witchery-port`.
6. NeoForge docs and modern examples are API-shape references only; they do not override Witchery behavior without a documented reason.

Baseline correction note: local comparison shows `1.7.10-0.24.1` adds 670 files over `1.7.2-0.19.0`, including 346 Java files and 323 assets. The added/expanded areas include the custom brew and Witch's Cauldron system, vampire/werewolf content, Blood Crucible, Silver Vat, Coffin, Beartrap/Wolftrap, Garlic Garland, Mirror/Mirror Wall, Daylight Collector, Shaded Glass, Perpetual Ice, Stockade, Wolf Head/Altar, Mindrake/Garlic/Wolfsbane crops, village/goblin structures, and many new mobs/renderers/assets. Future parity checks should compare the port against 1.7.10 first.

Blood Crucible QA note (2026-05-31): the basic crucible path is live and should be tested in-game before marking Done. Verify the restored stone-brick-stairs/stone-bricks/stone-brick-slab recipe, source-like stone sound/hardness/resistance, vampire/NPC fill path to 20 blood, level-10 vampire use with Artichoke/Bat Wool/written book selecting Storm/Swarm/Farm and draining blood, and creative-mode fullness bypass. Broader Blood Power and late-level gates remain separate parity work.

Stockade QA note (2026-05-31): `witchery:stockade` and `witchery:ice_stockade` now both use the source-shaped Stockade collision/damage/connection slice. Verify both block items appear in the Witchery creative tab without placeholder suffixes, place with low fence-like collision, connect to themselves, vanilla Iron Bars, and `witchery:ice_gate`, and apply cactus-style contact damage to living entities. Verify `witchery:stockade` crafts as nine blocks from logs around `Exhale of the Horned One`; the remaining source gap is per-wood stockade variants plus brew-freezing conversion to Ice Stockade.

Perpetual ice/snow QA note (2026-05-31): `witchery:cursed_snow_pressure_plate` is a hidden source-parity shell, not a player item. Use `/setblock` to place it, then verify it uses the normal Snow Pressure Plate model/name, triggers for ordinary entities, is absent from the Witchery creative tab, and drops `witchery:snow_pressure_plate` when broken. Full cursed brew payload conversion from normal snow plates is still pending.

Pit Trap QA note (2026-05-31): `witchery:pit_dirt` and `witchery:pit_grass` should look exactly like ordinary dirt and grass blocks, have no collision, drop themselves, and appear in the Witchery creative tab as "Dirt" and "Grass". Do not expect direct crafting yet; the original route is through Tormented Twine Witch's Cauldron recipes, which remain tied to the broader dynamic cauldron backlog.

Village structure QA note (2026-05-31): village house-pool weights are now apothecary `16`, witch hut `12`, bookshop `16`, watch tower `8`, and keep `8` in plains/desert/savanna/snowy/taiga pools. Verify fresh villages show Witchery shops/huts more often than the earlier watchtower-heavy pass, verify keeps appear without requiring many dozens of villages, verify each village contains no more than one of each Witchery building type while nearby separate villages can still get their own copies, verify walls/gatehouses/gate paths remove touched natural logs/leaves without stripping village wood structures, verify the village witch hut uses the old compact `4x6x5` footprint, sits one block above the path, attaches with its door/stair facing the road, uses biome-matched door/wall/stair/base blocks with connected side panes, has no cobblestone visible step/foundation in non-stone palettes, and has the outside stair climbing back into the door. Verify the bookshop uses the old compact `ComponentVillageBookShop` footprint and stair roof, sits at path height instead of on a one-block raised platform, uses connected panes, and adapts materials by village biome. Verify watch towers have a continuous ladder from entry to upper deck, an open top exit, and guards standing on solid deck cells around the ladder shaft. Verify apothecaries, watch towers, and keeps also use the shared biome material palette, especially sandstone-family blocks in desert/badlands villages instead of cobble/stone/oak. Verify the Hobgoblin Hut resembles the old `7x6x7` source schematic with one resident hobgoblin standing on the slab floor rather than clipping into it. For `/witchery goto village_*`, verify the pre-teleport prep logs one final ready count, keeps the prepared footprint resident, does not mark wall generation complete after `0 gate candidate(s)`, and teleports only after nonzero candidates generate real gates, landing on a village road/safe surface rather than underground.

Chalk glyph QA note (2026-05-31): draw at least 12 marks each with Ritual Chalk, Otherwhere Chalk, and Infernal Chalk and confirm the visible symbol varies across the old family textures. Use the same chalk on an existing same-family mark and confirm it rerolls the symbol instead of deleting the mark. Then complete small/medium/large circles with mixed visual symbols and confirm ritual circle detection still treats the visual `glyph=0..11` state as cosmetic only.

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
- Village wall/building QA should be run on fresh or newly generated plains, desert, savanna, taiga, and snowy villages. Confirm Witchery apothecaries, bookshops, source-sized village witch huts, watch towers, and keeps can attach to vanilla streets with the current `16/12/16/8/8` weights; confirm keeps appear more readily than the earlier weight-2 pass; confirm each village has at most one apothecary, one bookshop, one witch hut, one watch tower, and one keep, without suppressing those buildings in nearby separate villages; confirm taiga/forest walls, gatehouses, and graded gate paths clear any touched connected tree logs/leaves so they do not leave half-cut trunks or floating crowns, while planks/fences/building wood remain intact; confirm every Witchery village building uses plains/desert/savanna/taiga/snowy materials matching nearby village houses, with desert/badlands apothecaries, bookshops, witch huts, watch towers, and keeps all using sandstone-family blocks rather than cobble/stone/oak; confirm watch towers have a continuous ladder from entry to upper deck, an open top exit, and guards standing on solid deck cells around the ladder shaft; confirm the witch hut's jigsaw attaches at its real local `x=1` front door, its front stair climbs toward the door instead of away, and its visible base/stair use the local palette rather than cobblestone; confirm bookshops are path-height with the restored compact roof and no floating roof pieces; confirm generated glass panes connect into surrounding walls instead of rendering as isolated posts; confirm adaptive walls generate once per village before the player discovers the village on foot; for `/witchery goto village_*`, confirm safe locate holds the player at the old location while `Queued village arrival prep` / `Preparing village arrival` advances, temporary prep tickets keep all prepared chunks resident until walls and landing finish, fresh villages generate walls/gates before teleport, and the teleport chat appears only after prep completes; confirm walls leave 2-3 road-aligned gate openings whose paths hit a three-wide flat wall face rather than sideways wall segments, do not log repeated `0 gate candidate(s)` after a full prepared footprint for ordinary road villages, do not stack duplicate wall layers on already processed villages, do not force-load automatic wall chunk tickets or hidden automatic start-chunk loads during fresh world entry, do not place under water, grade hilly gate approaches without burying paths or rewriting more than 10 inward blocks into the village, use desert sandstone path material only for desert/badlands, keep savanna on dirt/stone materials, hide gate/path support fill with matching gatehouse blocks, spawn guards on wall tops, and no longer freeze the client when several automatic villages queue at once or when `/witchery goto village_*` / `/witchery walls` is used after the ticked safe village locate / pre-teleport one-chunk-per-tick prep with temporary chunk hold / loaded-only automatic lookup / bounded multi-village queue / no forced wall tickets for goto wall jobs / manual wall one-at-a-time tickets / 30 second cooldown / stuck forced-chunk skip changes. Biome-specific village goto should show safe-locate progress and fail cleanly rather than freezing if no matching village exists within its safe radius.
- Coven Circle QA should be run in fresh plains, desert, savanna, and badlands terrain. Confirm `witchery:coven_circle` and `/witchery goto stone_circle` both locate the same structure, reject center/corner water, keep the 11x4x11 old `ComponentCoven` footprint, place the kettle/standing-stone ring and three source cobweb detail blocks, give the visible Coven Witch the source 90% resident spawn chance, hide the mineshaft-loot dispenser and two vanilla Witch spawners under the altar, use a grass surface plus cobble/stone-brick materials in savanna and other non-desert biomes without an exposed one-block dirt support skirt, and switch to sand/sandstone only in actual desert/badlands.
- Straw Man QA should be run in fresh plains, desert, savanna, and badlands terrain with `/witchery goto straw_man` and `/locate structure witchery:straw_man`. Confirm the old 6x8x5 `ComponentWickerMan` hay effigy footprint, desert-only sand base, terrain-blended non-desert/badlands base without a raised pale sand platform, dandelion/dead-bush biome plants, hidden Zombie/Zombie/Skeleton spawner at the center base, full-footprint dry-land rejection for water/ocean/river terrain at selection and placement time, and village exclusion.
- Somnian Cotton QA should be run in the Spirit World with cotton beside Flowing Spirit at the block below it. Confirm natural spread eventually occurs over dirt/grass support, does not occur without the Flowing Spirit adjacency, does not occur outside the Spirit World, and nightmare-state harvesting returns Disturbed Cotton.
- Sleeping Apple and Brew of Sleeping should be retested against the Spirit World entry roll: use each item in the Overworld while not already spirit-walking or manifested, confirm entry still refuses recursive/wrong-dimension use, confirm Apple starts from the old guaranteed nightmare chance, and confirm Brew starts from the old `0.998` chance that nearby Nightmare-weave Dream Catcher setups can still modify.
- Broom flight should be retested specifically for the old snap-back symptom and the rider-facing visual yaw after the 2026-05-25 server-authority/render-state follow-ups: mount via item or `/witchery spawn broom`, fly forward and upward for more than several seconds, turn the rider in place and while moving, and confirm the broom no longer returns toward its placement point while preserving pitch lift, Soaring/Owl speed bonuses, and player-facing model rotation. Also verify the 2026-05-31 lifecycle/tint slices: named broom items keep their name when placed and picked back up, dyeing a placed broom stores/persists `BrushColor`, the bristle strands visibly recolor while the handle stays normally textured, unset color defaults to brown, and sneak-pickup plus damage-drop return the matching normal/enchanted broom.
- Witch Hunter aggro should be retested after the target-parity pass: spawn a hunter near a Coven Witch, undead mob, Demon/Blaze-style demonic target, a future entity type tagged `witchery:vampires`, and a player carrying a Voodoo or Vampiric Poppet, then confirm it selects valid supernatural targets without attacking ordinary players.
- Silver damage should be retested against transformed players with both a Silver Ingot and Silver Sword in the attacker's main hand; once vampire mobs land, also verify entity types tagged `witchery:vampires` receive the same bonus.
- Werewolf player rendering should be retested in third-person view after `/witchery werewolf @s true`: confirm the wolfman model appears without the vanilla human body or skin visible underneath, confirm walking/idling drives the wolfman animation, then `/witchery clear` and confirm the normal player skin renders again. With the current default config, the command/cursed state can show wolf form during daytime; exact old full-moon/level-gated transformation rules remain pending. Also confirm transformed/wolfsbane and Primer text no longer show mojibake punctuation/format codes, and confirm ordinary beds plus coffins refuse sleep with "You cannot sleep in wolf form!" while non-werewolf bed taglock sampling and vampire coffin sleep still work.
- Formatted player feedback should be retested anywhere it was historically color-coded: absorb a creature power near an altar, activate/empty a Mystic Branch creature power, defeat the Lord of Torment, destroy a Spirit World sleeping body, and trigger Poppet Protection shield consumption. Confirm the messages remain colored correctly and do not display raw `§`, `Â§`, or `\u00a7`-style control text.
- Stake counterplay should be retested by putting a vampire player to sleep in a coffin, then right-clicking them with a Stake to confirm one Stake is consumed and the sleeping vampire is killed; also confirm non-sleeping vampire players are not staked.
- Chalice filling should be retested in creative and survival: confirm the Witchery creative tab exposes both empty and filled Chalice stacks with the 2D `ingredient.chalice` / `ingredient.chalice_full` sprites, place an empty Chalice on a solid block, right-click it with Redstone Soup, confirm one soup is consumed, the placed chalice visibly fills inside the cup with red liquid/red dust particles, breaking in survival or pick-blocking preserves the filled state and switches the inventory sprite to `ingredient.chalice_full`, and a nearby altar counts the filled Chalice as the stronger +2x bonus instead of the empty +1x bonus.
- Glass Goblet should be retested in survival: craft it from three Glass Bottles plus Glass, confirm an empty goblet uses the blocking pose and a full goblet uses the drinking pose, set a test player as vampire with at least 125 blood, use the goblet to create a named Full Glass Goblet, then have a non-vampire drink it and confirm they become a vampire with the current starting blood plus smoke/level feedback. Also set a test player as werewolf and confirm a vampire-filled goblet is not consumed and shows the no-hybrids refusal. For the old chicken-blood bridge, build the skull/string/redstone/torch Creatures of the Night setup, keep an empty Glass Goblet in the hotbar, kill a Chicken with a Boline near the skull, and confirm the goblet becomes a Full Glass Goblet tagged with Chicken blood; drinking that chicken-blood goblet should empty it without starting vampirism. Using the Chicken-blood goblet on the Skeleton Skull should validate the rite only in the overworld at night with sky access, show pending Elle feedback on success, show the wrong-rite message on failure, and consume neither the skull nor the goblet until Elle exists. For the old villager-conversion gate, use a non-Chicken Full Glass Goblet on a Villager and verify the source-style failure messages for not mesmerised, still having persisted blood, and missing nearby Coffin; once the Villager is mesmerised, drained to 0 blood, and within four blocks of a Coffin, it should show pending vampire-rebirth feedback without consuming the goblet. Elle/Lilith spawning, exact old level gates, and actual villager-to-vampire entity conversion are intentionally still pending.
- Observations of an Immortal should be retested at a crafting table, in hand, through combat drops, and inside fresh Bookshop structures: Nether Wart around a Nether Star, Book, and Garlic should craft the book, the tooltip should show 6/16 revealed pages, right-clicking should open a readable vanilla book screen with only those revealed pages, and combining one book with one Torn Page should return the same book advanced by one source-style `Level` step up to 9/9, showing and opening all 16 revealed pages at completion. While carrying an incomplete book in the main inventory or offhand, player kills should have the restored source-family Torn Page chances for Endermen/Zombified Piglins, villagers, and undead, plus guaranteed drops from vanilla bosses and the currently ported source non-boss-false targets Death, Lord of Torment, and Reflection; complete books should stop qualifying for those drops. Generate/check the Bookshop chest table for two Books, three Torn Page pools, and one Observations book; exact old page-position sync/custom screen polish and unported Baba Yaga/Leonard/Gulg/Mog boss mappings are still pending.
- Warm Blood and Lilith's Blood should be retested in survival/creative: Warm Blood should refill vampire blood to the current 500 cap and poison non-vampires for 120 ticks; Lilith's Blood should start vampirism for non-vampires with the current practical starting blood and refill vampires to the current cap. Exact old level-up gates remain pending until the 10-level vampire model exists.
- Vampire villager feeding should be retested in survival/creative: empty-hand bite an awake villager and confirm the reduced two-thirds blood drain, then mesmerise or catch a sleeping villager and confirm the full drain, persisted depletion across multiple bites, the dry message when empty, and low-blood harm below the source 250-blood threshold. After draining the villager completely, confirm the Full Glass Goblet conversion gate above can see the 0-blood state. Exact old level-2/level-8 quest counters and actual villager conversion remain pending.
- Brew of Wasting should be retested in survival/creative: place players, mobs, leaves, saplings, flowers, crops, vines, grass, and chalk circles within four blocks, drink the brew, and confirm falloff behavior: players above 10 hunger lose 10 food and receive Hunger + Weakness, non-player living targets receive Wither + Weakness, fragile plant/leaf blocks vanish without drops, and chalk circles remain intact. Source-style thrown brew transport is still pending.
- Brew of Erosion should be retested in survival/creative: right-click mixed breakable blocks and Obsidian to confirm the small sphere is removed without normal drops, fluids/fire/unbreakables/block entities are skipped, melted Obsidian returns as item drops, and one Glass Bottle is returned; right-click a living target with held gear/armor and confirm 8 magic acid damage plus 100 durability corrosion on equipped slots. Drinking should run the same block/entity behavior centered on the player until source-style thrown brew transport is restored.
- Brew of Raising should be retested in survival/creative: right-click the top face of dirt, stone, grass, netherrack, soul sand, sand, cobblestone, and gravel and confirm one brew plus one returned Glass Bottle raises one guaranteed undead, with occasional extra nearby undead using the old 40%/10% extra-spawn odds and the source zombie/skeleton/zombified-piglin weights. Confirm the spawn columns clear without block drops, non-top-face clicks do not consume the brew, and drinking runs the centered practical fallback. Raised undead now carry the old `WITCEnslaverName` metadata plus a UUID tag, but full infernal ownership AI remains pending.
- Brew of Frost (`brew_of_ice`) should be retested in survival/creative: right-click a block adjacent to connected water and confirm one brew plus one returned Glass Bottle freezes nearby connected water in the old 3-block flood range. Right-click a top face and a horizontal face away from water and confirm it builds the old three ice columns oriented by the caster. Right-click ordinary living targets and confirm the old ice cage forms around them; then test Demon/Blaze/Ender Dragon/Horned Huntsman/Ent/Wither/Iron Golem style resistant targets and confirm the source fire exception instead. Drinking remains a centered fallback until source-style thrown brew transport is restored.
- Brew of Bats should be retested in survival/creative: right-click blocks and living targets and confirm one brew plus one returned Glass Bottle creates 10 bats at the impact point tagged `WITCNoDrops`, plays the Enderman-style burst, applies short Slowness VI with distance falloff inside roughly three blocks, and clears hostile mobs' current attack target. Drinking should run the same burst centered on the player until source-style thrown brew transport is restored.
- Brew of Hitchcock should be retested in survival/creative: right-click a living target and confirm one brew plus one returned Glass Bottle spawns 3-4 ravens around the target at roughly head height, each raven targets that victim, plays the Enderman/portal-style burst, and despawns after the source 400-tick lifetime. Block use should not run the targeted effect, and drinking plus `/witchery ritual hitchcock [count]` should keep the broader scatter flock fallback until source-style thrown brew transport is restored.
- Brew of Ink should be retested in survival/creative: right-click a block face and a living target and confirm one brew plus one returned Glass Bottle applies only Blindness, not Darkness, with the old distance falloff in a 4-block radius, or 5 blocks with active toad brew mastery. The directly hit living target should receive the full burst, hostile mobs in range should drop their attack targets, and drinking should run the same centered fallback until source-style thrown brew transport is restored.
- Brew of Cursed Leaping should be retested in survival/creative: right-click a block face and a living target and confirm one brew plus one returned Glass Bottle applies Jump Boost IV for 10 seconds and launches nearby living entities upward in the old 5-block distance-falloff burst. Confirm the directly hit living target receives the full 1.6 upward shove, nearby targets receive slightly less shove with distance, the previous Levitation effect is absent, and drinking runs the same centered fallback until source-style thrown brew transport is restored.
- Brew of Frog's Tongue should be retested in survival/creative: right-click a block face and a living target and confirm one brew plus one returned Glass Bottle pulls nearby living entities within four blocks toward the thrower with the old non-normalized motion formula and a short Jump Boost II effect. Confirm the thrower is not pulled, Ender Dragon and Horned Huntsman targets are skipped, the previous 12-block normalized drink-only pull is gone, and drinking runs the same centered fallback until source-style thrown brew transport is restored.
- Brew of Webs should be retested in survival/creative: right-click a block face and a living target and confirm one brew plus one returned Glass Bottle places the source compact cobweb pattern at the impact point: center, north/south/east/west, one above, and one below. Confirm solid blocks, fluids, and block entities are not overwritten. Drinking should run the same pattern centered on the player until source-style thrown brew transport is restored.
- Brew of Vines should be retested in survival/creative: right-click only horizontal block faces and confirm one brew plus one returned Glass Bottle grows the old vine strip down from the hit block and up from the block above, using the correct side attachment for north/south/east/west faces. Confirm top/bottom face clicks and living-target clicks do not consume the brew, existing solids/fluids/block entities are not overwritten, active toad brew mastery can follow the source extra wall step, and drinking only runs the nearby-wall fallback until source-style thrown brew transport is restored.
- Brew of Thorns should be retested in survival/creative: right-click the top of valid solid ground or an existing Cactus and confirm one brew plus one returned Glass Bottle converts the base to Sand when needed and grows a 3-high cactus column, or 4-high with active toad brew mastery. Right-click a living target and confirm the brew tries the old four adjacent short columns, 1 high normally or 2 high with mastery, and does not consume when no column can be placed. Drinking remains a centered practical fallback until source-style thrown brew transport is restored.
- Brew of Sprouting should be retested in survival/creative: right-click top, bottom, and horizontal block faces and confirm one brew plus one returned Glass Bottle grows a branch along the clicked face, 15 blocks normally or 20 with active toad brew mastery, with occasional side leaves. Test oak/spruce/birch/jungle and Rowan/Alder/Hawthorn logs, planks, leaves, and saplings to confirm matching wood families are preserved; test ordinary blocks to confirm random vanilla/Witchery branch families. Entity hits should not consume the brew, upward hits should move nearby living entities to a clear branch tip, and drinking should remain only a player-facing fallback until source-style thrown brew transport is restored.
- Brew of Substitution should be retested in survival/creative: drop block items near the impact, right-click a reference block, and confirm one brew plus one returned Glass Bottle replaces matching nearby reference blocks with the dropped block-item material in a 4-block radius, or 6 with active toad brew mastery. Confirm dropped item stacks shrink/disappear, displaced reference block stacks appear at the impact point, unbreakable/block-entity references do not consume the brew, entity hits do not consume the brew, and drinking remains only the old port shuffle fallback until source-style thrown brew transport is restored.
- Brew of Hollow Tears should be retested in survival/creative: right-click replaceable space next to blocks and living targets, then confirm one brew plus one returned Glass Bottle places `witchery:hollow_tears`, does not displace solid blocks, uses the old Tears texture path, weakens ordinary living entities standing in it, and gives undead/demonic targets Regeneration instead. Hollow Tears Bucket should also be retested: it now places the same stand-in block and returns an empty bucket, while full moving-fluid behavior remains pending.
- Solidifying brews should be retested in survival/creative: place connected Hollow Tears and use Solid Stone, Dirt, Sand, Sandstone, and Erosion on the source block or an adjacent face. Confirm one brew plus one returned Glass Bottle spreads through connected Hollow Tears within the old 64-block coordinate range, converts only Hollow Tears to the selected block, and that Solid Erosion removes Hollow Tears plus one breakable block below without normal drops. Non-Hollow-Tears block and entity hits should not consume the brew.
- Brew of the Depths should be retested in survival/creative: drink the brew and confirm the old five-minute timer starts, Water Breathing is restored if it falls off while the timer remains active, leaving water applies Wither II in short pulses, returning to water removes Wither, and expiry removes the maintained Water Breathing cleanup path. Confirm the previous Dolphin's Grace and Night Vision drift is gone.
- Brew of Revealing should be retested in survival/creative: make nearby players and mobs invisible through potion effects and explicit invisibility commands where possible, drink the brew, and confirm living targets within five blocks have Invisibility removed and visible flags cleared. Confirm the previous self Night Vision/Glowing behavior is gone; source-style thrown-brew transport and the old obscured-summoned-undead hook remain broader custom-brew/entity follow-up work.
- Brew of Infection should be retested in survival/creative: right-click Stone, Cobblestone, and Stone Bricks and confirm one brew plus one returned Glass Bottle converts them to the matching infested blocks; right-click a Villager and confirm it becomes a Zombie Villager while preserving baby/name/profession data where applicable; right-click another living target and confirm the old small magic hit plus brief Slowness IX instead of the previous poison/weakness aura. Drinking the brew should run the same living-target effect in a small practical radius until source-style thrown-brew transport is restored.
- Brew of Love should be retested in survival/creative: place adult animals and adult Villagers within four blocks, drink the brew, and confirm adult animals enter love mode while Villagers are paired up to 10 pairs, one partner is moved to the other, both parents enter breeding cooldown, and baby Villagers spawn with heart feedback. Baby Villagers should not be selected as parents. Also raise or tag at least two adult Zombies with the drinker's `WITCEnslaverName` or `witchery_enslaver_uuid`, drink the brew near them, and confirm up to 10 pairs move together, emit heart particles, and produce baby Zombies carrying the same owner tags. Source-style projectile transport and deeper infernal minion AI remain pending.
- Fume Funnel and Filtered Fume Funnel should be retested in survival: craft the source Fume Funnel, craft Fume Filter from Glass/Iron/Charged Attuned Stone, right-click a placed unfiltered Fume Funnel with the Fume Filter to upgrade it in-world, craft the alternate Filtered Fume Funnel recipe, place one funnel above an oven and confirm it speeds cooking without improving byproduct odds, then place normal and filtered side funnels to confirm the side-only byproduct chance path and visible filtered model parts.
- Bolt crafting and Hand Bow behavior should be retested in survival/creative: String + Sticks + Feathers should yield 9 Wooden Bolts; 3 Wooden Bolts + 1 Silver Deposits should yield 3 Silver Bolts; 6 Wooden Bolts + 1 Silver Ingot should still yield 6 Silver Bolts through the local compatibility bridge; Wooden Bolts + Bone + Ghast Tear should yield 12 Bone Bolts; Null Catalyst + 2 Belladonna + 3 Bone Bolts should yield 3 Nullifying Bolts; and 3 Wooden Bolts + String + Feather should yield 1 Splitting Bolt. Then crouch-hold the Hand Bow to load, confirm the loaded bolt persists on the bow until fired, crouch-cycle through Wooden -> Nullifying -> Bone -> Splitting -> Silver with replaced bolts returned to inventory/drop, and fire each loaded type: Silver should still trigger silver damage hooks, Nullifying should remove active potion effects from the hit target, Bone should hit undead/demonic targets harder than ordinary targets, and Splitting should launch three weaker wooden-bolt projectiles from one loaded Splitting Bolt. With a full hunter/silvered/dawn hunter set and an infused player, a fired Nullifying Bolt should drain the shooter's infusion energy on launch and drain half of an infused player target's max energy on hit; without a full hunter set, it should still keep the current port's effect-strip behavior but should not drain infusion energy. Enchanted Hand Bow QA should confirm Infinity occasionally keeps the loaded bolt and makes fired projectiles non-pickup, Power increases non-nullifying shot damage, Punch knocks targets back, Flame ignites non-nullifying shots, and Nullifying ignores Power/Punch/Flame. Visual QA should confirm uncharged idle shows the pistol crossbow, active use shows the three pull frames, and loaded idle shows the charged crossbow model.
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
