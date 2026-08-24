<p align="center">
  <img src="docs/witchery_rewoven.png" alt="Witchery: Rewoven" width="100%">
</p>

# Witchery NeoForge Remake

> *The old craft, transcribed for a new age of Minecraft.*

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-5b8731)
![NeoForge](https://img.shields.io/badge/NeoForge-21.11.42-f16436)
![Java](https://img.shields.io/badge/Java-21-e8772e)
![Status](https://img.shields.io/badge/status-active%20parity%20remake-6b4a8a)
![License](https://img.shields.io/badge/release-unofficial%20fan%20remake-a23b3b)

An unofficial modern NeoForge remake and continuation of the classic **Witchery** mod for Minecraft `1.21.11`.

The aim is to restore the *feel* and mechanics of original Witchery first — the altar's hum, the cauldron's patience, the weight of a rite — then lean on community fan-mod inspiration and other references where they help close visual, item, or registry parity gaps.

## Contents

- [Current State](#current-state)
- [Feature Highlights](#feature-highlights)
- [Local AI NPCs](#local-ai-npcs)
- [Not Done Yet](#not-done-yet)
- [Status Docs](#status-docs)
- [Build](#build)
- [Debug / QA Helpers](#debug--qa-helpers)
- [Mirror / Torment QA Notes](#mirror--torment-qa-notes)
- [Contributing Workflow](#contributing-workflow)
- [Credits](#credits)
- [Project Notes & Legal](#project-notes--legal)

## Current State

The remake is playable and well beyond prototype stage, but it remains an active parity project rather than a polished public release. The baseline is original Witchery `1.7.10-0.24.1`; community continuations are secondary references when they help bridge modern Minecraft constraints.

Current audited baseline as of **2026-08-24**:

- **Mechanical parity is broad.** Altars, machines, custom brews, Circle Magic, poppets, infusions, covens, familiars, transformations, major bosses, villages, and all three Witchery dimensions have substantial playable implementations.
- **Recent brew parity is certified.** Source-shaped projectile routing, filled-Taglock ritual targeting, Ice World conversion, raised-undead ownership, the curable Enslaved effect, owner-safe revenge behavior, and Brew of Love's owned-zombie breeding path are implemented and covered by dedicated tests.
- **Lycanthropy and the vampire level-perk/Blood Power ladders are mechanically complete.** Lycanthropy has its full level 1-10 Wolf Altar progression, forms, statistics, abilities, infection route, persistence, verifier, and dedicated progression fixture. Vampirism now has its automatic source perks, selectable Blood Power strip, exact power costs and cooldowns, mortal-player blood, Bat flight form, all three charged ultimates, source HUD art, and dedicated passive/activation fixtures.
- **Dimension structure and lifecycle work is deep.** Mirror World and Torment have fresh-world and restart certification. Spirit World terrain, dream/nightmare states, Corpse behavior, manifestation, and the endgame Otherwhere Hearth are implemented, with remaining work concentrated in live presentation and multiplayer QA.
- **Runtime verification is deterministic.** CI validates an explicit manifest, runs 13 exact resource contracts, executes 23 named GameTests on a clean dedicated NeoForge server with all Witchery dimensions loaded, and proves the Kettle, Witches' Oven, Distillery, Spinning Wheel, Brazier, Silver Vat, and command storage survive a real stop/restart cycle.
- **Creature parity has advanced substantially.** Demons, Flame Imps, Nightmares, Awakened Mandrakes, spectral undead, the Shade of Leonard, Death, the Lord of Torment, and the paired Goblin bosses Gulg and Mog use source-backed behavior and presentation paths. Gulg and Mog now carry their original 400-health partnership, distance-scaled damage reduction, 15-damage cap, launch/arrow specialties, persistence, environmental immunities, equipment, drops, and safe combat teleports. Hobgoblins now have persistent labor, worship, breeding, village, raid, follow, and guard behavior.
- **Witchery crops follow the old growing rules again.** The herb crops recognize the source soil set, Snowbell keeps its podzol and snow exceptions, Water Artichoke still requires water, Mindrake and Wolfsbane use their original growth/bonemeal tuning, and mature Wormwood grows its young upper segment. Configured-feature block states are validated during tests so invalid crop ages cannot silently break world generation.
- **Visual parity has an explicit contract.** The legacy model-coordinate audit covers source cubes, pivots, hierarchy, mirror timing, transform order, renderer anchors, held items, armor layers, shadows, and entity dimensions. The Witch's Cauldron keeps one documented custom exception: grounded splayed feet that touch all four block corners.
- **The old placeholder roster is empty.** Remaining exact-ID work is source-feel polish, live rendering checks, and deeper behavior rather than inert creative-tab stand-ins.

See [Legacy Model Coordinate Parity](docs/reference/LEGACY_MODEL_COORDINATES.md) for the transform rules and evidence ledger, and [Current Status](docs/status/CURRENT.md) for the compact living handoff.

High-level estimate:

- Mechanical remake parity: roughly `95-96%`
- Release-ready parity/polish: roughly `82-86%`
- Best source of truth: [docs/DOCS_INDEX.md](docs/DOCS_INDEX.md) and [docs/status/CURRENT.md](docs/status/CURRENT.md)

The long tail is mostly old Witchery edge cases, state-dependent in-game visual/audio QA, deeper custom-brew serializer behavior, and polish on promoted exact IDs. Foundational source-model geometry and anchors are now audited; machine sided automation is dedicated-server certified, and the remaining visual work is judging animated, equipped, translucent, and dimension-specific states in play. The live legacy placeholder roster is empty; the remaining work is making those promoted systems feel finished. The cauldron is lit; the brew is still thickening.

## Feature Highlights

Substantial restored or in-progress systems include:

- Altar power, kettle brewing, Witch's Oven, Distillery, Spinning Wheel, Brazier recipes, and Witch's Cauldron workings.
- Custom brews now have a real composed-brew spine: capacity ingredients, strength/duration/inversion modifiers, drink/splash/gas/liquid containers, many restored source action ingredients, finite vanilla-like liquid flow with shallow/sloped render states, deterministic gas/liquid contact pulses, brew buckets, synced cauldron/bucket/liquid/particle colour, source-shaped thrown bottle travel, tinted thrown bottles, glass impact sound, source Gas Mask protection for harmful gas contact, lifetime-scaled moving gas caps, Rotten Flesh's source Disease brew route with disease-block spread, Purified Milk's source Diseased cleanup branch, Dog Tongue live cauldron ritual release with compact fallback placement for gas/liquid releases, filled-Taglock direct player/mob ritual targeting with source coven/range/Leonard and protection rules, persistent Brick/Nether Brick target exclusions, triggered curses for dropped items plus buttons, levers, pressure plates, ladders, and doors, the original Grotesque demon-head overlay, and the full Ender Eye Ice World action with source capacity, power, radius, protection, and block-family conversion rules. Raised undead now keep source persistence/no-drop ownership, the curable Enslaved effect, owner immunity, and same-world loaded revenge behavior; Brew of Love converts owned adult zombie parents without stripping state and leaves their baby unowned. Mystic Branch casts from Leonard's Urn use the old fast, weightless spell-projectile route; only deeper serializer and obscure action edges remain active in this lane.
- Circle Magic with a broad restored rite roster, staged active rites, altar power costs, ring/chalk-family checks, curse routes, source decurse statues for target-side curse/transposition dampening with original mandrake/wolf placed-and-item rendering, and many transaction/refund fixes. Rite dispatch is **data-driven**: a single declarative `RitualDefinition` registry (75 rites — including a **Rite to remove vampirism**, a coven-of-six working led by a cat-familiar witch that drains a bound vampire's dark gift through a filled taglock) keyed on trigger item, ring size, chalk variant, companion rings, offering requirements, ordered disambiguation, and veto conditions — replacing the old hand-ordered `if`/`else if` ladders. The old `RitualExecutor` monolith was split into focused files (`RitualDefinition`, `RitualOfferings`, `RitualOfferingSets`, `RitualSavedData`, `RitualTransaction`/`RitualOffering`), behaviour-preserving and covered by headless JVM tests. A new **Spectral Coven** path lets a solo witch stand in for a living coven: finding 5 of the 12 in-world story books unlocks the *Rite of the Gathered Lives*, which raises five translucent, footless witches that orbit the altar and count toward any coven-gated working before fading.
- The **Mystic Branch** — the witch's wand and spell focus — forged through a night-time rite from an Ent Twig and Mystic Unguent. Glyphs traced in the air spend infused energy and demon-soul knowledge learned from slain demons (each soul teaches its own sign), while the branch also channels creature powers stolen from the world's beasts — swiftness, impossible leaps, healing, transposition, and bat-light levitation among them. An infusion type and learned-glyph set gate what a witch can cast; glyph and creature-power coverage is actively expanding, with `/witchery learn` and `/witchery infuse` available for testing.
- Crystal Ball, Waystone, and Player Compass parity is now practical: Bound Waystones, Blooded Waystones, and filled Taglock Kits can drive source-style held scrying, dropped bound/Blooded Waystones transpose nearby living entities and loose items from Otherwhere circles, and Player Compasses use source-style 32-frame needle art while preserving direct player/taglock binding.
- Poppets, taglocks, familiars, creature traps, belts, vampire clothing, and many source-backed item recipe paths. Familiars span owls, cats, toads, and spectral companions, with the newest being the **Creaking familiar**: the **Rite of the Creakening** binds a wild vanilla Creaking through a parallel familiar implementation (bind/follow/defend/sit/dismiss are working; the freeze-when-watched behaviour, resin cadence, and overall feel remain in-game QA).
- A living **Coven**: a witch with a bound familiar can recruit wandering Coven Witches into her own coven (up to six). Each recruit first sets a trial — fetch an ingredient, or slay a pet she conjures (a buffed spider or zombie) and return its remains — before swearing in. Sworn witches **fight for their leader**, hurling harmful brews at whatever she strikes or at any foe that strikes her, while never turning on her or their sister witches. The roster is persisted, so it holds across logout and the witches unloading, and the **Seer Stone** and `/witchery status` report your coven size and any trial a nearby witch has set you.
- Optional local **AI NPC personas** can give the world a spoken layer: Coven Witches, Leonard, villagers, village guards, hobgoblins, Baba Yaga, bosses, demons, flame imps, and reflection-style voices use compact markdown knowledge cards, local memory, route/intent checks, and deterministic safety fallbacks. The runtime card library now indexes 98 Markdown files with 1,474 targeted facts, filtered by a persona/card access matrix (`exact`, `scoped`, `rumor`, or `no`) so a witch, guard, villager, hobgoblin, imp, boss, or reflection receives facts in the right voice and knowledge lane. Villagers, guards, and witches also have lightweight world reactions for nearby vampires, werewolves, golems, workstations, cats, pet dogs, food stores, and supper gossip. The local model can flavor dialogue and help classify social/command intent; it cannot grant items, bypass trades, rewrite quests, or override combat logic.
- **Vampirism**, including vampire clothing set bonuses for feeding, fire survival, and mesmerise behavior. The vampire level 1-10 climb is gated through its source quest chain: blood-feeding, the villager and **caged-villager** blood quests, the sun-endurance and **three-village pilgrimage** stages, source-shaped Elle-to-Lilith lava transformation, Lilith's empty-goblet blood reward, Poppy mastery boon, cure, and item blessing handoffs. Blood Power uses the original selectable `Drink / Mesmerise / Speed / Bat / Ultimate` ladder: slot 1 and the mouse wheel select a power, use activates it, and exact source icons and blood-drop HUD art show the current state. Mesmerise costs 50 and can toggle vampire vision; Speed costs 10 and stacks with mastery; Bat Form costs 50 plus one power every 40 ticks and grants a compact flying body; level-10 Storm, Swarm, and Farm spend Blood Crucible charges only when they succeed. Mortal players carry a synchronized 0-500 human-blood pool that slowly recovers and can be drained in multiplayer. Automatic source perks are live too: human-form attack damage is multiplied by 2/3/4 at levels 1/4/7, a level-3 sneaking strike launches its victim, and a level-6 sneaking vampire can crush Stone, Netherrack, or Cobblestone bare-handed at a heavy exhaustion cost. Vampires keep their breath in water, reject poison, and still take the source two damage when Fire Resistance masks active flame. The daylight rules are source-faithful: a fledgling caught in the sun loses all blood power and burns (a starved vampire dies outright), while a level-5+ vampire **resists the sun**, bleeding blood power to endure daylight under heavy debuffs.
- **Lycanthropy** is a source-backed 1-10 progression climbed at the **Wolf Altar**: exact material gates (3 Gold Ingots, 30 Raw Mutton, 10 Tongues of Dog), the **Horn of the Hunt** boss quest, then airborne kills, 16 distinct night howls, wild-wolf taming, Zombified Piglin kills, and finally taking a life. The Moon Charm controls the separately rendered four-legged **wolf** and upright **wolfman** shapes at their original level gates; exact stat tables, digging, feeding, temporary pack calls, paralysis, armor rending, fall protection, silver counterplay, and the level-10 infectious bite are live. A baited **Wolftrap** beside a Wolf Altar naturally produces the infectious Wolfman needed to begin the path, and the progression state and howl timing persist across reloads.
- Player **infusions** channel active powers through the **Witch's Hand**: Light raises light-barrier cages and shield walls, Otherwhere phase-steps and recall-drags through the Ender, Overworld cushions falls and shapes earth-metal (fling armoured foes, magnetise and smelt ore, hurl loose rock, raise pillars, ground-slam shockwaves), and the Infernal witch **enslaves** mobs to a thrall army — bound minions never turn on their master and rally to avenge them, can be **sacrificed** for their creature power, and the witch's stolen beast powers ward the world's blows (extinguishing fire, breathing under a drowning, soaking a lightning strike for charge).
- Spirit World gameplay loop, corpse/manifestation flow, native spirits, nightmare pressure, Spirit Portal behavior, and Spirit World vegetation.
- The Spirit World sleeping **Body** now follows the original Corpse entity closely: it is a persistent, immovable `1.2 x 0.5` owner-skinned anchor placed at the centre of the entry block, rendered prone with no shadow, and leaves no loot. Destroying it while its owner is online forcibly wakes that player; returning normally removes it.
- The additive endgame **Otherwhere Hearth** combines Kettle brewing and circle magic to move a home bodily between the Overworld and Spirit World. Its one-use 10,000-power binding batch permanently names an empty Kettle to its owner; later crossings require a large Otherwhere ring, medium Ritual ring, six coven members, and a complete high-strength altar holding a base 10,000 power. Each crossing swaps the same-coordinate `25 x 25 x 25` volumes, from 8 blocks below through 16 above the Kettle, including block entities and every movable entity in both regions. Destination players, dream travellers, immovable entities, protected or out-of-bounds terrain, unbreakable blocks, and other bound hearths block activation. Both structures, the traveller manifest, and complete non-player entity snapshots are journalled before mutation so an interrupted crossing resumes after restart instead of losing the home or its inhabitants.
- The **Torment Dimension** restores all six source-shaped 31 x 31 DFS mazes with invisible Force walls, indestructible Mycelium-faced Torment Stone, exact portal rooms, and three live Refilling Chests per level. Chests fill initially and refresh only when empty after 72,000 ticks. The Lord's half-health transition carries the whole fight into one shared level and session; victory returns linked players safely, including disconnected participants when they next join. `/witchery verify torment` certifies every expected block, chest, route, fingerprint, portal, and atmosphere resource, and its fresh-world plus restart runs passed on 2026-08-18.
- The **Mirror World** uses the source 256-block room lattice, stable source-owned 9 x 9 x 9 chambers, exact two-way links, and persisted per-player return routes. The inner unbreakable mirror always provides a safe return. Every true Reflection yields one Demon Heart without copied gear, while a chamber clears only after all Reflections inside it die. Duplication Grenades create a separate passive `mirror_decoy`, and the ten-second translucent Mirror Face is another non-guardian path. See [Mirror World source parity](docs/reference/MIRROR_WORLD_SOURCE_PARITY.md) for the audited contract and remaining peripheral mirror-interaction gaps.
- Spectres, Banshees, and Poltergeists share the restored summoned-undead rules: persisted summoners, temporary lifetimes, a 15-damage incoming cap, Spectre obscuring and Revealing, Graveyard Dust strengthening, owner-safe targeting, and the old rare shared Spectral Dust drop for non-temporary spirits.
- Demons now match the 1.7.10 combat, movement, summon-persistence, bargain, loot, and render paths: close throws hand off to generic fireballs at range, summoned Demons persist, source currencies and the delayed escape explosion are back, Demon Hearts require an Arthana chance or the guaranteed bargain, and the exact full-bright model uses its original gait, attack pose, body wobble, texture, and shadow.
- Flame Imps now match the 1.7.10 tameable contract creature rather than a generic hostile mob: wild imps attack players, signed ownership bargains cost 25 levels, bound imps defend their owner and remain near the bargain site, and exact shiny affection drives the fixed Hunger/Fear/Anguish soul and Torment Scroll gift sequence. Demon Heart power lasts 60 minutes, doubles melee damage from 4 to 8, lowers the incoming-damage cap from 15 to 5, widens the imp without making it taller, and can be ended with Icy Needles. Ordinary kills have no Imp loot.
- Nightmares now match their 1.7.10 hunter and curse roles: victim names persist under the source key, the full door/village/target movement stack replaces invented teleporting, targets expire beyond 16 blocks with the old burst, armor is stripped before the attack unless a disrupted-dream charm is carried, Flowing Spirit suppresses the synchronized defense veil, and only Spirit World deaths yield Mellifluous Hunger with real Looting odds. The persistent Curse of Waking Nightmare again calls one weakened Nightmare at a time outside the Spirit World and is removed on Spirit World entry, by its cure rite, or at a Goddess Statue.
- Many mobs and renderers are restored or polished toward old Witchery visuals — ravens, spirits, nightmares, flying monkeys, imps, Ents, Mandrakes, Banshees, Poltergeists, Hellhounds, Minedrakes, spectral coven witches, and others. Awakened Mandrakes now use the exact source body, texture, shadow, walking wobble, 90% day / 10% night harvest odds, root-burst feedback, contact Nausea, loot, and intentional Ghast-family voice. **Flame Imp contracts** use source-style signed bargains: sign an ownership contract with your own blood to tame an imp for 25 levels, impress it with shiny gifts, then bind spell contracts to a target for Living Flame, Fiery Tolerance, Evaporation, Fiery Touch, or Melting Touch. The **Shade of Leonard** is a full parity boss: a fireproof 600 HP demon summoned at the Witch's Cauldron by dissolving Nether Wart, Tear of the Goddess, Diamond Vapor, Diamond, and a Nether Star in order, then using a Witches' Hat (no Gunpowder), neutral until struck, immune to all but a player's own melee, who hurls Mystic Branch spells, lays the lethal Mortal Coil on the whole party, shields himself behind waves of Lost Souls below half health, and swells to a giant near death — rendered as the source horned goat-shade with its authentic art and sounds.
- The **Witches' Primer** as a native Modonomicon guidebook that grows as the witch does: every player is handed the Primer on first join, and crafting a topic book unlocks that chapter and reveals the next book's recipe — a guided path from Getting Started through Collecting Fumes, Herbology, Circle Magic, and the transformations. Station and rite recipes use baked source-style book art, chapters unlock in their proper order with later pages revealed as you obtain the matching station output, and a placeable Modonomicon block now opens like a Hexerei-style book with live Primer pages, entry lists, gating, page buttons, back-to-entries navigation, and Primer-matching item rendering directly on the model. The current Primer also gives concrete setup help for the altar's six-block 2x3/3x2 footprint and documents brazier operation, altar-wait feedback, Wood Ash collection, and the full brazier working roster.
- Vanilla villages now receive Witchery village buildings and, on roughly 40% of villages, adaptive town walls (the rest stay open): apothecaries, bookshops, witch huts, watch towers, keeps, wall guards, gatehouses, and biome-aware village materials are live and under active QA. Apothecary residents now keep their cauldron jobsite anchor, custom look/name, and full levelled trade list across reload/profession churn. Village Guards bind to nearby homes, patrol back toward their village, defend attacked villagers and exactly respected village friends, and turn on players whose reputation falls to the old hostile threshold. They do not attack a player merely for being a vampire or werewolf, but a conscious guard that sees a vampire feed immediately intervenes. Sneak-use a Bell on an adult villager to appoint them as a Village Guard, or on a player-appointed guard to dismiss them back to village life; profession, type, level, XP, name, offers, persistent data, and current blood are restored where possible.
- A **Vanishing Witch Hut** brings folklore to the worldgen: an uncommon designer-authored NBT hut that generates in forests and swamps and will not be watched. Step inside, leave, and look away and it vanishes in a gust of ash and debris, rebuilding itself elsewhere — usually a nearby spot, but sometimes slipping across into the dream-lit Spirit World at matching coordinates. It never moves while someone is inside or watching, leaves loose planks and cobwebs on restored natural ground where it stood, and carries its **same resident witch** with it (she keeps to her hearth and won't wander out). The old remedy holds it fast: a three-mark line of **Witches' Salt** laid across the doorway threshold (the salt trace reuses the redstone-wire connection rendering, tinted white; obtain salt by smelting kelp in the Witches' Oven or buying it from a Coven Witch). QA via `/witchery setup vanishing_hut`, `/witchery goto vanishing_hut`, `/witchery hut move`, and `/witchery hut watch`.
- Designer-authored NBT structures are now part of the worldgen path. The Vanishing Witch Hut, Hobgoblin Hut, wells, and Warped Coven keep standard structure-block-readable NBT assets while Java handles terrain adaptation, cleanup, residents, loot, and special behavior. The Hobgoblin Hut uses a cleaned NBT template in forests and swamps, rejects water/steep footprints, clears only local clutter, has no chest, and spawns a resident Hobgoblin. The Warped Coven is a Nether warped-forest structure with terrain/clearance checks, late vegetation cleanup, a Coven Witch resident, and a focused infernal loot chest.
- Hobgoblins now have a practical old-goblin work loop: authorized chat commands can send leashed, pickaxe-equipped hobgoblins to mine nearby natural blocks, worship a nearby Patron statue, stay, stop, follow, come closer, go away, or guard. Work and worship state persists, mining wears the pickaxe, normal drops are produced, and Koboldite Dust can be deposited nearby or dropped into the world. Their village ecology is live too: settled Hobgoblins keep near village homes, adults breed only under the source 35% housing cap, babies grow with vanilla age persistence, groups of three raid villagers and avoid Village Guards, and attacked Hobgoblins call nearby kin into the fight.
- Goblin Gulg and Goblin Mog are once again a paired boss fight rather than two generic high-health mobs. Each has 400 health and becomes untouchable while the other is within 3 blocks; damage opens gradually at 6, 9, and 16 blocks and every hit is capped at 15. Mog fires much faster at airborne prey, Gulg launches victims, and both retain source fire/water/web resilience, persistence, enchanted chainmail rewards, Koboldite drops, full-bright presentation, and guarded teleports that cannot strand them inside terrain.
- A large exact-ID cleanup pass has promoted or removed the former legacy placeholder roster. Source and community fan-mod aliases now cover guidebooks, hunter gear, brew containers, contracts, waystones, special wearables, utility tools, mounted trophy heads, and many old food/ingredient items; deeper behavior and visual QA continue family by family.
- Ice World and its complete construction family are restored: an Ender Eye supplies the level-five, 2000-power custom action, splash/ritual/liquid delivery uses the old scaled filled sphere, and protected terrain, block entities, unbreakable blocks, and unloaded or out-of-bounds positions are skipped. Stockades use the pointy-top/stacked-lower visual split, retain cactus-style contact damage, and carry the old Oak/Spruce/Birch/Jungle/Rowan/Alder/Hawthorn/Acacia/Dark Oak variants through one modern block item. Shaded Glass, Pit Trap, Infinity Egg, Sun Collector, Silver Vat, Blood Crucible, and other block-family slices are also restored or partially restored.
- The in-game guidebook is powered by **Modonomicon** as a required dependency. JEI integration is declared optional and can be wired in as current Minecraft/NeoForge availability allows.

## Local AI NPCs

Local AI dialogue is experimental but integrated enough for playtesting. It is **disabled by default** in `config/witchery-common.toml` under `[llm]`; enable it only when the installation intentionally provides a local model sidecar or compatible endpoint. The Windows launcher checks before installing Ollama or downloading model files. The tested playtest model is `witchery-eva-qwen2.5-1.5b-q4km-4k`, served locally through Ollama at `http://127.0.0.1:11434`.

The model is used as a voice and classifier layer, not as game authority. Persona cards under `src/main/resources/data/witchery/ai/cards/` provide compact facts about recipes, brews, rites, mobs, social behavior, and entity-specific lore. The mod still validates output, falls back when needed, and keeps all real mechanics in server code.

Knowledge routing is deliberately persona-bound. Coven Witches can answer broad Witchery and vanilla mechanics. Villagers stay close to village life, professions, trade, maps, food, and rumor. Guards know public safety and threats. Hobgoblins know work, tools, mining, and Koboldite. Flame Imps know fire, contracts, and Nether paths. Bosses and demons speak from their domain rather than becoming friendly tutorial menus. Reflection voices may know uncanny map-like concepts, but must not invent coordinates or hidden authority.

Useful AI config keys:

```toml
[llm]
enabled = false
prewarmEnabled = false
provider = "ollama"
endpoint = "http://127.0.0.1:11434"
model = "witchery-eva-qwen2.5-1.5b-q4km-4k"
timeoutMillis = 3000
maxConcurrentRequests = 1
maxTokens = 80
chatCooldownTicks = 20
cooldownTicks = 100
cancelHandledNpcChat = true
allowLanEndpoint = false
apiKeyEnv = ""
extraHeaders = ""
responseFormat = "json_object"
```

Set `enabled = true` to opt into AI dialogue. `prewarmEnabled = true` sends an
invisible startup request so the first conversation does not pay the model's cold
start cost. Servers without a managed model sidecar should keep both values false.
`cancelHandledNpcChat = true` keeps explicit/private NPC-addressed messages out
of normal multiplayer chat; ambient look-at routing still broadcasts. Keep
`chatCooldownTicks` lower than `cooldownTicks` if ordinary chat should recover
faster than greetings, gaze, and ambient reactions. Keep
`allowLanEndpoint = false` unless the model endpoint is on a trusted LAN machine. For
`provider = "openai_compatible"`, `apiKeyEnv` can name a
bearer-token environment variable, `extraHeaders` can pass non-auth metadata
headers, and `responseFormat = "json_schema"` opts into strict structured output
when the endpoint supports it.

## Not Done Yet

Every grimoire has blank pages. Important remaining work includes:

- Some systems marked "Done" in status docs are implementation slices rather than full old-Witchery parity, especially machines, custom brews, and armor/equipment feel. Lycanthropy's full progression, vampirism's selectable Blood Power ladder, and the foundational legacy model-coordinate pass are complete; state-dependent client presentation still needs ordinary play QA. Mirror World and Torment structural/lifecycle checks are complete, while their final client visual/audio feel remains QA.
- The legacy placeholder map is empty, but promotion is not the same as finished parity. Several exact IDs, aliases, wearables, blocks, and brew paths still need source-feel polish, in-game QA, or deeper behavior.
- Custom Brew parity is still an evolving area: the composed-brew core, finite moving liquid, brew buckets, colour sync, ordinary and old spell-mode projectile travel, source Gas Mask gas protection, lifetime-scaled moving gas, deterministic moving-fluid contact, Disease brew/block spread, Purified Milk disease cleanup, Dog Tongue ritualized cauldron release, Bound Waystone location targeting, filled-Taglock direct body targeting, persistent Brick/Nether Brick exclusions, triggered item/block curses, the practical source action roster, and raised/bred undead ownership behavior exist. Remaining work is concentrated in obscure serializer/action edges.
- Vampirism's Blood Power and village-consequence contracts are implemented; remaining work is Observations screen polish and live multiplayer/camera/audio verification of the bat proxy, target-blood HUD, feeding witnesses, and power strip.
- Machine sided automation is source-audited and dedicated-server certified across the Kettle, Witches' Oven, Distillery, Spinning Wheel, Brazier, and Silver Vat. Remaining machine work is live GUI/model/particle feel, Fume Funnel placement and cooking presentation, and unrelated source edge cases.
- The Otherwhere Hearth is code-complete and restart-safe, but its destructive full-volume swap still needs ordinary live multiplayer, claim-adapter, and restart playtesting before release.
- The latest active handoff lives in [docs/status/CURRENT.md](docs/status/CURRENT.md), with slice candidates in [docs/status/NEXT_TASKS.md](docs/status/NEXT_TASKS.md) and feature state in [docs/status/FEATURE_MATRIX.md](docs/status/FEATURE_MATRIX.md).
- Mirror World needs real-client QA for player skin/equipment rendering, atmosphere, movement, particles, and audio. Fresh generation, source allocation order, room isolation, carried links, guardian succession, reconnect routes, local bridge rules, escape chants, and grenade-decoy separation are automated. Backwards migration of the earlier remake's room saves is intentionally deferred.
- Village additions and the first NBT-backed wild structures are live, but still need cross-biome visual QA, traversal checks, and continued tuning for walls, gatehouses, guard movement, and structure placement.
- Ritual execution is powerful but still carries old-queue/refund/edge-case parity risk.
- Public-release cleanup remains: licensing clarity, asset audit/replacement direction, packaging, balance passes, and broad multiplayer/server QA.
- The local AI layer is playtest-ready, not release-final: it needs more real-session testing, more cards, and final decisions about whether it ships as default, optional, or dev-only.
- Protection adapters for Open Parties and Claims, FTB Chunks, and Common Protection API are reflective/no-hard-dependency integrations. They need in-server QA with those mods installed.

## Status Docs

The exact 1.7.10-to-modern transform contract, source model inventory, entity
dimensions, and controlled capture results live in
[docs/reference/LEGACY_MODEL_COORDINATES.md](docs/reference/LEGACY_MODEL_COORDINATES.md).

Consult [docs/DOCS_INDEX.md](docs/DOCS_INDEX.md) before choosing what to work on next. It explains which docs are active, frozen, or reference-only.

- [docs/status/CURRENT.md](docs/status/CURRENT.md) — compact living handoff.
- [docs/status/QA_QUEUE.md](docs/status/QA_QUEUE.md) — active in-game QA queue.
- [Book_of_Shadows.html](Book_of_Shadows.html) — broad player-facing guide, kept in step with the in-game Primer for current behavior.
- [docs/status/FEATURE_MATRIX.md](docs/status/FEATURE_MATRIX.md) — feature-level parity matrix.
- [docs/status/NEXT_TASKS.md](docs/status/NEXT_TASKS.md) — backlog and slice candidates.
- [docs/status/BUGS.md](docs/status/BUGS.md) — active/fixed bug board and parity notes.
- [docs/status/ITEM_REVIEW.md](docs/status/ITEM_REVIEW.md) — item roster, exact-ID promotions, and follow-up decisions.
- [docs/status/CREATIVE_TAB_PARITY_AUDIT.md](docs/status/CREATIVE_TAB_PARITY_AUDIT.md) — original creative-tab screenshot comparison and icon/variant parity notes.
- [docs/experiments/LLM_NPCS.md](docs/experiments/LLM_NPCS.md) — local AI NPC setup, scope, config, and model notes.
- [docs/experiments/LLM_NPC_PERSONA_AND_KNOWLEDGE_EXPANSION_PLAN_v3.md](docs/experiments/LLM_NPC_PERSONA_AND_KNOWLEDGE_EXPANSION_PLAN_v3.md) — imported v3 persona/card expansion design notes.
- [docs/reference/REMAKE_NOTES.md](docs/reference/REMAKE_NOTES.md) — historical implementation notes and reference findings.
- [docs/testing/RUNTIME_TEST_HARNESS.md](docs/testing/RUNTIME_TEST_HARNESS.md) - exact local/CI runtime gates, artifacts, and manual lanes.

## Build

Requirements:

- Java `21` JDK, not a JRE. Use JDK 21 specifically; do not use JDK 25 or 26 for this repo.
- Minecraft `1.21.11`
- NeoForge `21.11.42`
- Modonomicon `1.121.0` at runtime; Gradle resolves it automatically for development.
- JEI is optional.

Compile and process resources:

```powershell
.\gradlew.bat compileJava processResources
```

On Unix-like shells:

```bash
./gradlew compileJava processResources
```

Run the JVM test and packaging gate:

```powershell
.\gradlew.bat test build
```

```bash
./gradlew test build
```

Run the complete deterministic runtime gate (Python standard library only):

```powershell
python tools/runtime_test_harness.py all
```

```bash
python3 tools/runtime_test_harness.py all
```

This validates the exact resource-contract and GameTest manifest, runs all 16
CI GameTests by name on a clean dedicated server with Witchery's custom
dimensions loaded, and verifies Kettle/block-entity plus command-storage state
after a real server stop and reload. Logs and JSON summaries are written under
`build/runtime-test/`. See the
[runtime test harness guide](docs/testing/RUNTIME_TEST_HARNESS.md) for focused
runs, CI behavior, failure artifacts, and manual-only lanes.

The development JAR is written under `build/libs/`. This repository is still a
pre-release development project; do not treat a successful local build as a
finished public release.

Useful dev runs:

```powershell
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat runData
```

Windows launcher:

```powershell
.\RUN_MINECRAFT.bat
.\RUN_MINECRAFT.bat --diagnose
.\RUN_MINECRAFT.bat --stacktrace
```

`RUN_MINECRAFT.bat` also runs the local AI preflight. If `[llm].enabled = true`, it checks Ollama/model availability and prompts before installing or downloading anything. `RUN_MINECRAFT.bat --diagnose` checks the local Java/Gradle/client setup without launching Minecraft and writes support details to `run/logs/launcher-latest.log`. If the client crashes after startup, also check `run/logs/latest.log`, `run/logs/debug.log`, and `run/crash-reports`.

If setup fails with `compiler is null` or `jdk.compiler module` missing, Windows is launching Gradle with a JRE. Install a Java 21 JDK, not a JRE and not JDK 25/26, then set `JAVA_HOME` to the JDK folder, for example `C:\Program Files\Eclipse Adoptium\jdk-21...`.

If setup fails with `Missing Gradle wrapper jar: gradle\wrapper\gradle-wrapper.jar`, the repository copy is incomplete or security software removed the wrapper jar. Re-download or `git clone` the full repository and make sure `gradle/wrapper/gradle-wrapper.jar` exists before running the launcher.

### Dev tools

Dev runs (`runClient` / `RUN_MINECRAFT.bat`) bundle [WorldEdit](https://www.curseforge.com/minecraft/mc-mods/worldedit) for in-game world-building (the `//wand`, `//set`, `//copy`/`//paste` command suite). It is wired in as a dev-only `localRuntime` dependency (CurseForge build for MC `1.21.11` / NeoForge) and is **not** bundled into the published Witchery mod — it loads only in the development client/server.

## Debug / QA Helpers

The mod includes a growing `/witchery` debug command suite (permission level 2). Useful current helpers include:

Structure goto targets currently include `hobgoblin_hut`, `vanishing_hut`, `coven_circle`, `stone_circle`, `straw_man`, `witch_well`, `small_well`, `cursed_well`, and Nether-only `warped_coven`. `/witchery goto nether` searches for a safe Nether foothold and stores the player's previous position; `/witchery exit nether` returns to that stored point.

- `/witchery setup stations` — builds a stocked Rowan QA hut with core machines, altar, traders, chest supplies, kettle setup, Glowglobes, and test layout.
- `/witchery setup rite` — builds a ritual QA pad with white, golden, otherwhere, and infernal circle sets.
- `/witchery setup otherwhere_hearth` — destructively builds paired same-coordinate disposable fixtures in the Overworld and Spirit World, including a bound Hearth, correct rings, six coven members, a charged high-capacity altar, contrasting containers/signs/fluids/redstone, pets, a hostile, a loose item, and a ridden minecart.
- `/witchery verify torment` — exhaustively certifies all six Torment mazes: deterministic fingerprints, exact Force/Torment Stone geometry, routes, portal axes, all 18 Refilling Chest block entities, and dimension atmosphere resources.
- `/witchery goto <structure>` — teleports to Witchery structures and vanilla village variants, with safer ticked village locate/prep behavior for village targets.
- `/witchery walls` and `/witchery walls reset` — generate or reset adaptive village walls around the nearest loaded village start.
- `/witchery spawn <entity>` — spawns registered Witchery entities for model/behavior QA.
- `/witchery spawn wickerman` — places a summon-valid bloodied Wicker Man structure for Horned Huntsman testing.
- `/witchery ritual hitchcock [count]` — releases a raven flock without consuming a brew.
- `/witchery rite list` and `/witchery rite <name>` — list the data-driven rite definitions and force-fire one at the player (bypassing the dispatch gating; held item used as the trigger) to smoke-test the dispatch table. The rite's own internal power/offering checks still apply, so run it near a powered altar.
- `/witchery coven recruit`, `/witchery coven fill [count]`, and `/witchery coven clear` — swear Coven Witches to your coven (skipping the familiar gate and recruit quest), or empty the roster, for living-coven QA.
- `/witchery coven unlock` and `/witchery coven summon` — unlock the Rite of the Gathered Lives knowledge, or raise the spectral coven directly, for coven-rite QA.
- `/witchery learn` and `/witchery infuse` — teach Mystic Branch glyphs and set a player infusion for symbol-spell QA.
- `/witchery guide unlock [all|<book>]` and `/witchery guide reset` — unlock or re-lock Witches' Primer chapters for guidebook QA.
- `/witchery dream`, `/witchery torment`, `/witchery mirror`, and `/witchery exit` — dimension QA shortcuts. Dream entry uses the normal Spirit World flow: it stores the Overworld return point, enters at the matching X/Z with a same-column Y safety check, and wakes through `/witchery exit`.
- `/witchery nightmare`, `/witchery nightmare false`, and `/witchery nightmare demonic` — switch the Spirit World between its three atmospheres for QA: a normal **dream** (pink fog/daylight), a **nightmare** (green fog/night), and a **demonic** nightmare (red fog/night). Run from the Overworld to enter directly, or while already in the Spirit World to switch in place; nightmare/demonic also force a victim-bound Nightmare (demonic adds fireballs/demons). `/witchery status` reports the current mode.
- `/witchery verify spiritworld` — audits the loaded Spirit World dimension type, mirrored Overworld biome source, Spirit-only Wispy Cotton placement, and native Spirit spawn wiring. `/witchery verify spirit_lifecycle` separately checks the current player's dream, manifestation, Corpse return, or physical-Hearth state for contradictions.
- `/witchery verify mirrorworld` — audits every allocated Mirror room's exact 9 x 9 x 9 geometry, unique source ownership, two-way links, guardian UUID/count, and cleared state. `/test run witchery:mirror_world_lifecycle` exercises the two-player guardian, decoy, reconnect, loot, and safe-return lifecycle on a server with custom dimensions loaded.
- `/witchery verify otherwhere_hearth` — performs the real Hearth preflight without moving blocks or spending power, reporting bounds, route, journal phase, ownership, rings, coven count, altar position/capacity/charge, destination players, dream travellers, unmovable entities, and the first block/protection failure.
- `/test run witchery:otherwhere_hearth_round_trip` — runs the destructive bidirectional Hearth GameTest. The runtime harness executes it and `witchery:otherwhere_hearth_destination_refusal` automatically on a clean dedicated server with custom dimensions loaded; the deliberately interrupted `witchery:otherwhere_hearth_recovery_resume` stage remains manual.
- `/test run witchery:infernal_ownership_lifecycle`, `/test run witchery:infernal_raised_undead`, and `/test run witchery:infernal_bred_undead` — verify binding/cure/rebinding rules, owner immunity and revenge, raised-undead persistence/no-drop state, parent conversion state, and the ordinary unowned baby path.
- `/test run witchery:ice_world_area_conversion`, `/test run witchery:lycanthropy_progression`, `/test run witchery:vampire_power_activation`, `/test run witchery:vampire_passive_perks`, and `/test run witchery:hobgoblin_village_ecology` — exercise the complete Ice World area action, both vampire power/passive contracts, the level 1-10 werewolf ladder, and source-shaped hobgoblin village behavior.
- `/witchery vampire`, `/witchery werewolf`, `/witchery blood`, `/witchery infusion`, `/witchery familiar <owl|cat|toad|spectral|creaking|none>`, and `/witchery status` — player-state QA helpers. `/witchery verify lycanthropy` audits the complete altar/form contract and current player state.
- `/witchery ai status`, `/witchery ai chat <message>`, `/witchery ai memory`, `/witchery ai knowledge <query>`, `/witchery ai probe [prompt]`, `/witchery ai reload`, and `/witchery ai forget [nearest|all|<npc-uuid>]` — local NPC dialogue QA helpers.

## Mirror / Torment QA Notes

Custom dimension chunk generators do not rewrite chunks that already exist on disk. After changing Mirror World or Torment generation code, test with a fresh world or delete the relevant dimension folder under the run save before verifying layout changes.

Torment's fresh-world and restart/reload certification passed on 2026-08-18. The CI dedicated-server GameTests `witchery:torment_structure_and_refill`, `witchery:torment_portal_round_trip`, and `witchery:torment_multiplayer_boss_reconnect` cover generation, chest fill, dedicated return anchors, deeper routing, shared boss sessions, disconnect persistence, drops, and reconnect release. A live client is still required to judge the red sky/fog, portal tint, Lord animation, particles, and audio mix.

Mirror World's source audit was refreshed on 2026-08-18. `witchery:mirror_world_lifecycle` passed on a fresh dedicated world with two source mirrors and players, vertically distinct source-order rooms, isolated guardian ownership, copied equipment, one heart per true Reflection, all-Reflection clearing, a linked item carried into the Nether, reconnection, cleared re-entry, killed/expired passive decoys, reconnect return, and final audit. A live client is still required to judge owner skin/equipment rendering, the dark-blue atmosphere, Mirror Wall flow, portal feedback, and Reflection movement/audio.

## Contributing Workflow

Recommended loop — measure twice, cast once:

1. Check [docs/DOCS_INDEX.md](docs/DOCS_INDEX.md) and [docs/status/CURRENT.md](docs/status/CURRENT.md).
2. Compare the remake against original/decompiled behavior before choosing a bounded task.
3. Implement one coherent parity slice or bug fix.
4. Run `.\gradlew.bat test build` and `python tools/runtime_test_harness.py all`; use focused exact-ID GameTests while iterating.
5. Run `.\gradlew.bat runData` when a generated Primer provider changes, and inspect the generated diff.
6. Update only the docs routed by [docs/DOCS_INDEX.md](docs/DOCS_INDEX.md).
7. Commit meaningful progress with a clear message.

Keep unrelated local art experiments, temp folders, or generated scratch files out of parity commits unless the task explicitly includes them.

## Credits

All credit for Witchery belongs to its creator, **Emoniph**. This remake exists only because that original work was so good — the altars, the rites, the brews, the whole brooding world of the old craft are Emoniph's design, and every system here traces back to that vision. Enormous gratitude and respect to the original author. We are tending someone else's garden, not claiming it as our own.

## Project Notes & Legal

- This is an **unofficial fan remake** inspired by the original Witchery mod by **Emoniph**. It is not affiliated with or endorsed by Emoniph, and it is not an official Witchery release.
- **All rights to original Witchery remain with Emoniph and/or the original rights holder.** Original art, textures, sounds, and other assets belong entirely to Emoniph; where any appear in this repository they are development placeholders only, to be replaced with original artwork before any public release.
- **The code is an independent reimplementation, rewritten against the modern NeoForge `1.21.11` API rather than copied.** None of the original mod's source is shipped or copied verbatim — it targets a long-obsolete Minecraft version and will not run on modern Minecraft. Original gameplay behavior, decompiled jars, and community remakes are studied as references to keep the remake faithful, in the same spirit as other community continuation projects.
- The long-term art direction is to move toward original replacement art that keeps the same gameplay identity and mood, rather than treating old assets as the final public-release target.
- If Emoniph or a legitimate rights holder requests removal or changes, the project will comply promptly.
