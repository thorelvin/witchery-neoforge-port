# QA Queue

Last curated: 2026-07-30

This is the active in-game QA queue. It replaces scattered new QA notes in
the archived parity/fixes/ritual QA notes under `docs/archive/status/` and small
one-off status files. Keep entries concrete: setup, action, expected result.

## How To Use

- Add only tests that a player or tester can actually run.
- Put long research or source comparisons in reference docs, then link them.
- Remove or archive entries once they are verified and no longer useful.
- Prefer commands and item IDs where possible.

## Active Focus

### AI NPC Runtime

- Set an invalid local LLM provider or endpoint, talk to a nearby AI NPC, then
  fix the config and retry immediately. The failed provider/config attempt
  should not consume the player/NPC request cooldown.
- During a pending AI reply, move far away, change dimension, disconnect, kill
  the NPC, or unload it. When the async handler resolves, the NPC should not
  stay stuck in pending conversation attention.
- Trigger a card-repair path in a non-plains or non-night scene. Repaired lines
  should be grounded with the live entity biome/time, not fake `Plains`/`night`
  assumptions.
- With local LLM request logging enabled, ask low-risk recipe/lore/trade/small
  talk lines and confirm they skip the separate message-analysis request; ask an
  action-shaped line such as `can you open this?` and confirm it still uses the
  analysis/classifier path.
- With `maxConcurrentRequests` above 1, have multiple players speak to separate
  AI NPCs at once and confirm requests can run in parallel without excess
  provider/client churn or long-session cooldown-map growth.
- Set `provider = "openai_compatible"` against an endpoint that requires bearer
  auth, set `apiKeyEnv` to an environment variable containing the token, and
  confirm NPC chat succeeds without placing secrets in the config file.
- Against an OpenAI-compatible endpoint/model that supports structured outputs,
  set `responseFormat = "json_schema"` and confirm replies still parse into
  `speech`, `intent`, and `memory`; against a looser local-compatible server,
  set `responseFormat = "none"` if it rejects `response_format`.
- Trigger a rejected AI reply, such as a prompt-leak answer, player-echo line,
  repeated NPC line, or busy gate, then run `/witchery ai status` and confirm
  `recentRejections` names the rejection reason clearly.

### Hobgoblin Chat Commands

- Leash a Hobgoblin, give it a pickaxe, then use chat orders for `mine`,
  `worship`, `stop`, `follow me`, and `guard me`.
- Confirm only the leash owner/admin can command it once ownership is claimed;
  other players should get the rope-owner refusal.
- Before ownership is claimed, trigger failure cases such as `mine` with no
  pickaxe, `worship` with no patron statue, or an impossible `come here` path;
  then confirm a different player can still claim it with a successful command.
- Ask command-keyword knowledge questions such as `what is the goblin altar?`
  `how do I get koboldite dust?`, and `can hobgoblins find koboldite?`; they
  should answer as dialogue, not rope denial or command execution.
- While following, confirm it tracks the command holder instead of resuming its
  normal player-avoidance or villager-hunting behavior.
- While guarding, have a hostile mob attack the command holder and confirm the
  Hobgoblin targets that threat, then returns to holding near the player after
  combat.
- Confirm `stop` clears mining, worship, follow, guard, and current combat
  orders without clearing the claimed command owner.

### Hobgoblin Village Ecology

- Run `/test run witchery:hobgoblin_village_ecology`; the headless fixture must
  pass its live two-versus-three pack gate, villager targeting, guard-avoidance
  gate, parent cooldown, baby age, and profession-variant checks.
- Place two free adult Hobgoblins near at least nine claimed village beds. Keep
  villagers and players out of their immediate attention and confirm courtship
  can eventually produce one baby; both parents should then pause breeding for
  6000 ticks. Restart while the child is young and confirm age and profession
  appearance survive and it eventually grows up.
- With one or two free Hobgoblins inside eight blocks, confirm they do not hunt
  villagers and do not specially flee a Village Guard. Add a third and confirm
  the pack may raid villagers but breaks away from a Guard within 12 blocks.
- Attack one free Hobgoblin and confirm nearby Hobgoblins retaliate. Repeat while
  one is following, guarding, mining, worshipping, trading, or in conversation;
  its assigned activity must remain authoritative until direct combat requires
  retaliation, and ambient village raids must not steal it from the order.
- Let a Hobgoblin settle among village homes, leave the area, and return. It
  should remain present and work back toward the village after wandering outside
  its home radius without creating a continuously loaded chunk.

### Flame Imp Contracts

- Sign a blank ownership contract with the player's blood, then use it on a
  wild Flame Imp with at least 25 levels.
- Confirm the wild imp attacks players and can despawn at distance; after
  binding, confirm it defends its owner, persists, and wanders within roughly
  16 blocks of the bargain site.
- Give only source shinies. Useful checks include Gold Ingot `+1`, Emerald `+3`,
  Diamond `+8`, Nether Star `+16`, Diamond Pickaxe/Axe `+24`, and Diamond Block
  `+72`; Amethyst, Prismarine Crystals, Heart of the Sea, and Gold Nugget should
  be rejected. Damaged tools should also be rejected.
- Confirm affection can accumulate beyond 20 and rewards become more likely,
  then verify the first four rewards are Hunger Soul, Fear Soul, Anguish Soul,
  and Torment Scroll in that order. Further gifts should come from the source
  seven-item ingredient pool, and another reward should be held for three
  minutes.
- Bind each spell contract to a target, and also test crouch-use self binding.
- Confirm a pleased, unpowered imp spends contracts for Living Flame, Fiery
  Tolerance, Evaporation, Fiery Touch, and Melting Touch.
- Confirm refusal cases are clear: wrong owner, not pleased, powered imp, no
  contract target, or missing contract.
- Give a Demon Heart twice and confirm both are consumed and the 60-minute
  timer refreshes. While powered, confirm the imp becomes wider rather than
  taller, emits flames, heals every second, deals 8 melee damage, and caps one
  incoming hit at 5. Icy Needles should always be consumed and clear power.
- Kill ordinary wild and bound imps and confirm neither drops Exhale of the
  Horned One, Refined Evil, or a random Torment Scroll.

### Vanishing Witch Hut

- Run `/witchery setup vanishing_hut` in plains-like clutter, cleared grass, near
  trees, near water, and on uneven hills.
- Confirm setup clears replaceable vegetation but still rejects water, hard
  obstructions, and bad slopes.
- Step into the hut, leave, look away, and confirm it relocates only when
  unobserved and unoccupied.
- Confirm the same resident witch is carried to the new hut and remains tethered
  inside.
- Lay a three-mark Witches' Salt line across the doorway and confirm relocation
  is held.
- Confirm natural generation remains forest/swamp only.

### NBT Structures

- Confirm designer NBT files remain loadable in a vanilla structure block after
  import and cleanup.
- Hobgoblin Hut: generate in forest/swamp, reject water/ocean placement, avoid
  ugly terrain gashes, and spawn one Hobgoblin resident. There should be no
  chest.
- Warped Coven: generate in Nether warped forest terrain, choose a stable site,
  clear stems/leaves/blocks from enclosed spaces, remove the mirror, and spawn a
  Coven Witch with fitting loot.
- Coven/Stone Circle: in a fresh Overworld, confirm `/witchery goto
  coven_circle` and `/witchery goto stone_circle` locate the same registered
  structure family; verify the ring is flush with terrain, keeps the three
  source cobweb details, hides the dispenser and two Witch spawners under the
  altar, spawns the Coven Witch resident most of the time, rejects river/ocean or
  frozen-water sites, uses sandstone only in desert/badlands, and avoids a raised
  dirt skirt in non-desert terrain.
- Straw Man: in a fresh Overworld, confirm `/witchery goto straw_man` lands on a
  placed 6x8x5 effigy with the hidden Zombie/Zombie/Skeleton spawner set, dry
  footprint, biome-appropriate plant details, no water/frozen-water intersection,
  and no broad raised sand/dirt platform.
- Wells: test regular/small/cursed wells in forest/swamp terrain. Confirm
  surface adaptation, shaft/room clearance, round grass skirt blending, and no
  buried center features.

### Dream And Spirit World

- Run `/witchery verify spiritworld` before fresh-chunk testing. It must pass the
  mirrored-biome, Spirit-only cotton, and native-Spirit spawn wiring audit. During each travel mode, run
  `/witchery verify spirit_lifecycle`; ordinary dream, nightmare, manifested
  ghost, and physical-Hearth states must each report `CONSISTENT`.
- From the Overworld, record F3 coordinates, run `/witchery dream`, and confirm
  Spirit World X/Z match exactly.
- Y should stay in the same column and only move enough to find a safe landing.
- Run `/witchery exit` and confirm return to the stored Overworld coordinate.
- Enter through normal Spirit World gameplay and inspect the sleeping Body at
  the Overworld entry block. It should be centred, broad and prone, show the
  owner's player skin/name, cast no shadow, remain perfectly immobile, and
  survive distance/chunk unload. Right-clicking should do nothing but consume
  the interaction. Normal return must remove it.
- With a second player present, damage the Body while its owner is dreaming.
  Confirm it has the owner's entry health, drops nothing, and death immediately
  wakes the owner. Log the owner out and confirm non-creative/environmental
  damage cannot destroy the orphaned Body; a creative player still can.
- Confirm dream, nightmare, and demonic nightmare fog colors match the source:
  pink-magenta, green, and red. Fog distance must continue to follow the normal
  client/render-distance rules rather than becoming shorter in darker dreams.
- Confirm ordinary passive animals can spawn in Spirit World biomes.
- Enter with Icy Needles, ordinary Mutandis, Mutandis Extremis, a Witches' Primer,
  and a marked ordinary item. Only the Needles and ordinary Mutandis should enter;
  the marked item, Primer, and Extremis must remain in the waking snapshot, while
  unused Mutandis remains in the saved dream inventory for the next visit. Gather both cottons,
  Mellifluous Hunger, a Flowing Spirit brew, and both subdued-spirit variants;
  wake and confirm those materials transfer into the restored waking inventory.
- In fresh terrain, confirm the Spirit World preserves recognizable same-coordinate
  Overworld biome geography. A 64-attempt Wispy Cotton patch should be attempted only
  there in about three of four eligible chunks; randomized Y means an attempted patch
  is not guaranteed to place cotton. Cotton must obey bush light rules and grass,
  dirt-family, podzol, or farmland support, while its own spread excludes farmland.
  Native Spirits should appear in groups of 2-5 only above Y 60 on well-lit grass or
  sand and in source-equivalent land biomes, never oceans, rivers, or Mushroom Fields.
  Nightmares must not naturally biome-spawn.
- Enter a peaceful dream and confirm shared daylight. Switch one true dreamer to a
  nightmare and confirm shared night, then clear that nightmare and confirm day.
  A player carried bodily by an Otherwhere Hearth must not affect either state.
- Harvest Wispy Cotton by hand, automation, and explosion during a shared nightmare;
  every route should yield Disturbed Cotton. Confirm Flowing Spirit must be a source
  block to ignite a portal or support cotton spread, and that Ender Pearls and Blaze
  Powder are removed from block-harvest drops in the Spirit World.
- Confirm Flowing Spirit moves with its slower source cadence and roughly four-block
  horizontal reach, extinguishes fire, hydrates farmland, and supports boats without
  extra ambient particles. Break one cell of an active portal and confirm the entire
  2 x 2 interior collapses rather than leaving a partial portal.
- Attempt to bring in an Enderman and a third-party living entity; both should be
  refused while vanilla and Witchery living entities remain. Confirm ordinary vampire
  sunlight does not burn players or vampire mobs during peaceful dream daylight.
- Cast the Rite of Manifestation in the Overworld with all six offerings and 5,000
  altar power. Confirm all offerings are consumed, every player within five blocks
  receives 150 seconds plus 25 seconds per nearby coven witch, and circles refuse all
  casting inside the Spirit World. Walk through a 4 x 4 snow frame with optional
  corners and a 2 x 2 portal interior; passengers and vehicles must be refused.
  Confirm the ghost appears at the same Overworld coordinates using the player's skin
  at about half opacity and normal lighting, arrives with only Icy Needles, can return
  with those Needles plus a bound fetish acquired while manifested, and returns from
  its current coordinates on timeout or Icy Needle use.
- Brew the Otherwhere Hearth ingredients in exact order: Soul of Torment,
  Demon Heart, Brew of Sleeping, Spirit of Otherwhere, Charged Attuned Stone,
  and Drop of Luck. Confirm the Kettle needs 10,000 altar power, yields one
  binding item, binds only while empty and dry, consumes the item, remembers
  its owner through reload, and refuses a second binding or a non-owner.
- In a disposable world with both volumes empty of players, run `/witchery setup
  otherwhere_hearth`. Stand beside its Kettle and run `/witchery verify
  otherwhere_hearth`; it must report `READY`. The generated paired 25 x 25 x 25
  fixture includes contrasting inventories, signs, fluids, redstone, coven,
  pets, a vehicle/passenger pair, a loose item, a hostile, and destination-side
  markers. Confirm both volumes and every entity swap at identical coordinates,
  a second crossing restores them exactly, and the altar loses exactly one
  scaled crossing cost each way.
- On an integrated or dedicated dev server where the custom dimensions are
  loaded, run `/test run witchery:otherwhere_hearth_round_trip`. The registered
  manual GameTest must pass its bidirectional block-entity, fluid, redstone,
  entity, passenger, physical-traveller, journal, and exact altar-charge checks.
  The headless GameTest server cannot satisfy this gate because it creates only
  the vanilla dimensions. This dedicated-server test passed on 2026-08-18.
- `/test run witchery:otherwhere_hearth_destination_refusal` passed on a
  custom-dimension dedicated server on 2026-08-18, proving an occupied target
  creates no journal, changes no source blocks, and spends no altar power.
- Separately confirm the Otherwhere Hearth refuses wrong dimensions, incomplete
  rings/coven/altar, insufficient altar capacity or power, an altar outside the
  volume, a destination player, a dream-walking player, an unteleportable
  entity, an unbreakable block, world-border/build-height overflow, source or
  destination claim denial, and another bound Kettle on either side. No failed
  preflight may consume altar power or change either volume.
- Interrupt a prepared Otherwhere Hearth crossing during a disposable-world
  server restart and verify its persisted journal converges to one complete,
  charged swap. Repeat with a travelling player offline; the structures should
  settle, the journal should wait, and that player should cross on login before
  the journal clears. The forced-kill plus
  `/test run witchery:otherwhere_hearth_recovery_resume` sequence passed on
  2026-08-18 with both entity manifests, an offline owner, and no second altar
  charge. Repeat with two real clients before release. A physically carried
  Spirit World player keeps inventory and dies normally rather than waking like
  a dream traveller.

### Torment Dimension

- On a fresh disposable world, run `/witchery verify torment`. It must certify
  six deterministic 31 x 31 perfect mazes, exact Force/Torment Stone geometry,
  all routes and portal axes, 18 live Refilling Chests, layout fingerprints, and
  atmosphere resources. Restart that same world and run it again; no fingerprint,
  route, block, or block-entity result may change. This fresh/reload gate passed
  on a dedicated server on 2026-08-18.
- On an integrated or dedicated dev server with custom dimensions loaded, run
  `/test run witchery:torment_structure_and_refill`, `/test run
  witchery:torment_portal_round_trip`, and `/test run
  witchery:torment_multiplayer_boss_reconnect`. These passed on 2026-08-18 and
  cover initial chest fill, source entry height, dedicated return-anchor
  persistence, more-Torment routing, safe return, one shared two-player boss
  session, phase-2 attacks/damage cap/drops, disconnect persistence, and release
  on reconnect.
- With a real client, confirm the dimension has a pure red fixed-night sky and
  red fog with no sun, stars, sunrise, or clouds; Force walls remain invisible;
  Torment Stone visibly uses the Mycelium-top source art; and the portal is a
  thin red-tinted plane with flame and ambient/teleport feedback.
- Fight the Lord through both phases and judge the source model, horn placement,
  flying movement, punch animation, flame aura, Ignianima and Soulfire volleys,
  Iron Golem/Ghast combat cues, Torment voice volume/cadence, and the transition
  and return effects. Repeat with two real clients, including one reconnect, as
  the final multiplayer feel check.

### Mirror World

- On a fresh disposable world, enter two different inhabited mirrors with two
  players and run `/witchery verify mirrorworld`. It must report distinct
  source-owned 9 x 9 x 9 rooms, exact two-way links, one guardian per uncleared
  room, and consistent cleared state. Restart and run the verifier again. This
  fresh/restart certificate passed on a dedicated server on 2026-08-18.
- Run `/test run witchery:mirror_world_lifecycle` on an integrated or dedicated
  server with custom dimensions loaded. It covers two-player room isolation,
  persisted routes, owner identity and equipment, one heart per true Reflection,
  succession while another Reflection remains alive, safe cleared re-entry,
  killed and expired passive Duplication Grenade decoys, reconnect return, and
  exact source clearing. The expanded fixture passed on a fresh dedicated world
  on 2026-08-18, including the linked-item carrier route through the Nether.
- With a real client, confirm the Reflection uses the entering player's skin,
  armor, and held items without dropping copies; the inner mirror permits return
  before guardian defeat; and defeating the guardian prevents its return. Judge
  deep-blue fixed-dark atmosphere, Mirror Wall flow, portal feedback, movement,
  and source Reflection audio. Mirror Face apparitions must remain visibly and
  mechanically distinct from both guardians and passive grenade decoys.
- With one player inside, have a second player break the inhabited source Mirror,
  carry its linked item outside Mirror World, then place it elsewhere. Confirm
  the inner mirror follows the carrier first and reconnects the same room after
  placement.
- Verify an ordinary opposite-facing pair carries a mob and dropped item within
  31 blocks, a cleared vertical pair works outside Mirror World, and an unpaired
  Mirror World mirror spends exactly 3,000 altar power for its local move.
- In Mirror World, say `mirror mirror send me home` and `mirror mirror i give up`.
  Confirm their five-minute and one-hour cooldowns and their entry/bed/spawn
  destinations. These remain manual chat-path checks.

### Cursed Blocks And Wells

- Use triggered splash brews on doors, buttons, levers, pressure plates, and
  ladders.
- Confirm the block keeps its original block state and releases the stored brew
  only under the intended trigger condition.
- For warning brews, say a test line near the cauldron before bottling and
  confirm the text appears in purple actionbar text when triggered.
- Re-trigger warning doors/buttons/levers and confirm repeated warnings respect
  the cooldown instead of spamming every activation.
- Cursed well: confirm top ladder warning uses `You feel uneasy.` without poison;
  lower ladder applies the actual curse once.

### Custom Brews And Rituals

- Multi-offering rites should not show their visible result until all offerings
  disappear one by one.
- Leonard cauldron summon: dissolve Nether Wart, Tear of the Goddess, Diamond
  Vapour, Diamond, and Nether Star, then use a Witches' Hat. No Gunpowder.
- Retest Dog Tongue live cauldron release for supported custom-brew delivery
  shapes.
- Retest gas/liquid brews for tint persistence, lifetime, source/flow behavior,
  deterministic contact refresh, and Gas Mask protection.
- Compose Ice World with level-five capacity, Ender Eye, and Gunpowder. A base
  splash should use a source radius of 3 and convert the old scaled filled sphere
  (effective radius 5); repeat with Wood Ash/Cocoa Beans/Wispy Cotton Extent and
  confirm effective radii 6/8/9. Test wooden doors, stairs, slabs, fences, gates,
  pressure plates, all stockade wood variants, terrain, logs, planks, and leaves.
  Facing, half, hinge, open, powered, waterlogged, connection, and stockade `up`
  state should survive where the target supports it.
- Repeat Ice World at a claim boundary and beside a filled chest, Bedrock, world
  border, build-height edge, and unloaded chunk. Allowed blocks should convert;
  protected, block-entity, unbreakable, out-of-bounds, and unloaded positions
  should remain untouched. Also release it with Tongue of Dog and from moving
  brew liquid to verify ritual and reduced-radius adjacent pulse delivery.
- Apply ordinary curable effects plus Witchery source-incurable effects, then
  test Reek Cure Buffs, Purified Milk Cure Debuffs, Skeleton Skull Steal Buffs,
  and Creeper Head Spread Affliction. Ordinary curable effects should clear,
  steal, or spread by source strength gates; Moonshine, Stout Belly, Gas Mask,
  Sun Allergy, Paralysed, Queasy, and Leonard hexes should not count as normal
  curable effects. Diseased should still follow Purified Milk's special strength
  2+ cure and strength 3+ disease-block cleanup.
- Apply Paralysed amplifier 5+ to a Villager and a Village Guard with a vampire
  player level 8+ standing 1-16 blocks away. They should not receive the
  movement-speed lock; instead they should drop combat targeting and step toward
  the vampire roughly once per second. Check that level-7 vampires and targets
  closer than 1 block or farther than 16 blocks do not trigger the lure.
- Drink Infused Brew of the Grave, sneak-use a Necromantic Stone on an unowned
  undead mob, and confirm it becomes an owned thrall with witch/green feedback.
  Reuse within exactly 200 ticks should fizzle; after 201+ ticks it should bind
  a second undead. Then normal-use the Necromantic Stone on a living target and
  on a block top to verify nearby owned undead attack/move instead of idling.
- Release bound Spectral Stones for Spectre, Banshee, and Poltergeist. Confirm
  each spawned entity remembers the releaser as summoner, does not target that
  owner, and keeps the expected source-style combat role.
- Release a bound Spectre and use Brew of Revealing nearby. The Spectre should
  start obscured, then become revealed when the brew resolves.
- Hit Spectre, Banshee, and Poltergeist with a composed Stone/Dissipate brew.
  They should take the source spectral-undead damage; ordinary vanilla raised
  undead should not qualify solely because they are owned thralls.
- Use Graveyard Dust on owned spectral undead. It should add 2 max health per
  dust up to the source 50-health cap, heal to the new max, refuse non-owners,
  and avoid consuming dust when already capped or invalid.
- Kill ordinary Spectre, Banshee, and Poltergeist entities and confirm their
  Spectral Dust drop is a shared rare one-item roll: about 1-in-4 normally,
  1-in-3 with Looting I, and capped at 1-in-2 for Looting II+. Temporary
  spectral undead from timed summons should drop no Spectral Dust.
- Block the first release space over a ritual cauldron and confirm gas/liquid
  Dog Tongue release chooses a nearby safe space instead of failing immediately.

### Visuals And Creative Tab

- Grow several mature Mandrakes and harvest them in daylight, at night, and on
  Peaceful. Day should awaken roughly 9 in 10, night roughly 1 in 10, and
  Peaceful never; awakenings give no crop drop and raise the creature in a
  20-particle poof burst. Spawn one directly with
  `/witchery spawn mandrake_mob` and verify its `0.6 x 0.9` source-sized body,
  `0.5` shadow, mirror-mapped face/body, flat leaf crown, arm twitch, and subtle
  whole-body side wobble while walking. It should pursue within about 32 blocks,
  use Ghast scream/hurt/death sounds, deal no direct bite damage, apply 15
  seconds of Nausea on contact without Earmuffs, apply none with Earmuffs, award
  no XP, and drop one Mandrake Root plus one seed with a 25% second seed.
- Place a Silver Vat alone, then put block-entity machines against each side one
  at a time. It should use the exact pale source texture, keep its blue bath,
  show all four spouts when clear, and hide only the spout touching each machine.
  Feed new Gold Ingots into adjacent sided output inventories and verify the
  one-in-five deposit rolls render one source piece for counts 1-15, two at 16,
  and up to eight at 64; right-click collection should clear every piece.
  Check the creative, held, framed, and dropped item forms show all four spouts,
  the blue bath, and no deposits. Also verify hopper/furnace output detection,
  comparator strength, break drops, and the nine-deposit Silver Ingot recipe.
- Place `witchery:decurseteleport` and `witchery:decursedirected` facing all four
  horizontal directions. Occluded Summons should be the small source-textured
  stone mandrake; Broken Curses should be the source-textured wolf with its low
  pedestal. Confirm neither has the old static stone-marker cubes, both sit on
  the floor inside the source half-block outline, and breaking particles use
  the matching statue texture. Check each item in the creative GUI, both hands,
  an item frame, and as a dropped stack for readable scale/centering. With two
  players, also confirm the renderer stays visible while owner/killer protection
  and sneaking-owner stacked removal still behave as documented.
- Drink Brew of the Grotesque and confirm the 60-second repulsion aura also
  replaces the player's head with the source Demon-textured horned/tusked head.
  Have a second player enter tracking range after activation and verify they see
  it immediately, then verify it disappears for both clients on expiry. Apply a
  composed Infernal Blood Grotesque effect to a non-player living entity and
  confirm the same head follows yaw/pitch for the three-minute effect. Finally,
  test normal invisibility: the fixed player style should hide, while the
  composed effect should retain the source faint 15% render only for a viewer
  allowed to see the invisible entity.
- Place a normal Mirror that has not had its Reflection guardian cleared. Use it
  empty-handed, then say `mirror mirror on the wall` nearby. Each route should
  call up one translucent floating Mirror Face just outside the reflective side,
  turn its head toward a nearby player, use the old reflection-speech cue, ignore
  hits/collision, and disappear just after ten seconds. Repeated prompts while it
  is present should not stack faces; cleared mirrors and mirrors inside Mirror
  World should not show one.
- Open the Witchery creative tab and confirm internal utility entries stay
  hidden: Pit Dirt, Pit Grass, duplicate Koboldite Pentacle, Witches' Shoes, and
  the unfinished Hobgoblin Patron statue should not appear as normal player
  picks.
- Spawn a Wolfman with `/witchery spawn wolfman`; confirm idle audio uses the
  old low talk with rare howls, and hurt/death sounds are no longer zombie
  placeholders.
- As a transformed werewolf player, trigger the howl keybind and confirm it uses
  the old Wolfman howl clip rather than vanilla wolf shake. Interact with the
  Wolf Altar and confirm the feedback voice is private/source Wolfman Lord, then
  run the Wolftrap lure/capture loop and confirm lure talk plus Lord capture
  voice cues.
- Run the registered `witchery:lycanthropy_progression` GameTest on a fresh
  dedicated-server world, then run `/witchery verify lycanthropy` on a normal
  player at levels 0, 4, 6, and 10. Confirm the fixture reaches level 10 and the
  verifier reports no registry, task, form, or persistence mismatch.
- Prove the natural entry path without commands: leave an open Wolftrap beside
  a Wolf Altar, leash a sheep within eight blocks, wait for a full-moon night,
  let the lured Wolfman enter the trap, then take one hit. Confirm infection
  starts at level 1 and survives logout/restart.
- Climb the live altar path with exact offerings and tasks. Check that short or
  wrong offerings are never consumed; each counter quest begins only after an
  altar visit; the Horn is issued once; and every completed task requires a
  return to the altar before the next level begins.
- Compare both player forms in first and third person and from a second client.
  WOLF should be quadruped with the low 0.6 x 0.8 body and WOLFMAN upright;
  Moon Charm normal/sneak controls, armor/held-item shedding, one-block step,
  full-moon-night forcing, carried-charm freezing, and strength-gated active
  Wolfsbane should all agree on both clients.
- Exercise every perk at its first level: soft-block claw digging and timed bone
  finds at 3, non-undead kill feeding at 4, airborne hostile kills at 5, 16
  distinct night-howl chunks at 6, wolfman paralysis at 7, six wild-adult wolf
  tamings plus temporary wolf packs at 8, Zombified Piglin kills and armor rend
  at 9, and near-fatal player/villager infection at 10. Restart during the howl
  cooldown and during the level-6 quest to confirm both persist.
- Test bite protection/config combinations on two clients: a full silvered
  hunter set blocks player infection; ordinary armor does not; existing
  werewolves are immune; `AllowPlayerToPlayerWolfInfection` disables player
  spread; and `AllowVampireWerewolfHybrids` controls both curse directions.
- Spawn a Flame Imp with `/witchery spawn flame_imp`; confirm idle/cast audio
  uses the old imp laugh at low volume, with slower/lower powered pitch, and
  hurt/death use the old imp hit/death sounds. Spawn a Demon with
  `/witchery spawn demon`; confirm ambient remains Blaze-like, but hurt/death
  read as Wither-like, fireballs use the Ghast launch cue, and footsteps use
  the Iron Golem step cue. At range it should launch large fireballs rather than
  stall with bow AI; inside three blocks it should use the `7-21` throw and raise
  only its right arm. Confirm the full-bright source skin, static wings,
  side-to-side walking wobble, and block debris under moving feet.
- Summon a Demon through the Infernal rite, leave the area, and return; it should
  persist. Test direct bargains and Devil's Tongue discounts: currencies may be
  Blaze Rod, Magma Cream, Diamond, Emerald, or Gold Ingot, while optional
  diamond offers return two Ghast Tears or two Ender Pearls. Paying with Blaze
  Rod or Magma Cream should play the Wither launch cue and produce the delayed
  3-power explosion about 2.5 seconds later. Name Tags and spawn eggs should
  retain their normal interactions, and Spirit World Demons should not trade.
- Kill Demons as a player with no Looting, then increasing Looting. Expect
  `0..(1 + Looting)` Magma Cream. No Demon Heart should drop without an Arthana;
  with an Arthana, expect 33% at Looting 0-1, 66% at 2-3, 99% at 4-5, and 100%
  at 6+. Every generated bargain list should expose a Demon Heart among its
  first three offers.
- Spawn Baba Yaga with `/witchery spawn baba_yaga`; confirm ambient uses the
  old Baba living voice, hurt remains Witch-like, and death uses the old Baba
  death sound.
- Spawn Leonard with `/witchery spawn leonard`; confirm ambient, attention,
  hurt, and death sounds still use the old source Leonard voice clips after the
  move back to `mob.leonard.*` event names.
- Spawn Owl and Toad familiars with `/witchery spawn owl` and
  `/witchery spawn toad`; confirm Owl idle/death/hurt use the old hoot/hurt
  sounds, Toad idle/death/hurt use the old croak/hurt sounds, and both idle
  less often than vanilla pets.
- Spawn Hobgoblin/Gulg/Mog with `/witchery spawn hobgoblin`,
  `/witchery spawn goblin_gulg`, and `/witchery spawn goblin_mog`; confirm
  Hobgoblin idle/haggle/hurt/death/trade-yes use the old goblin clips, and
  Gulg/Mog use their source idle voices, zombie-horse hurt, Wither death, and
  Iron Golem footstep cues.
- Spawn Nightmare, Spectre, Treefyd, Flying Monkey, and Reflection with
  `/witchery spawn nightmare`, `/witchery spawn spectre`,
  `/witchery spawn treefyd`, `/witchery spawn flying_monkey`, and
  `/witchery spawn reflection`; confirm their old source voices replace the
  Vex/wood/silent/vanilla placeholders. Summon Lilith with
  `/summon witchery:lilith` and confirm hostile idle/hurt/death plus friendly
  reward interactions use the old Lilith clips instead of dragon/illusioner
  placeholders.
- Enter a forced Spirit World nightmare and verify the victim-bound Nightmare
  never teleports, bursts away when separated by more than 16 blocks or when
  nightmare state ends, opens/breaks doors, strips armor before a strike unless
  either disrupted-dream charm is in the main inventory, and raises its
  translucent defended veil after a hit. Flowing Spirit should prevent the veil;
  the Spear of the Huntsman should shorten it. Kill it with and without Looting
  and confirm Mellifluous Hunger is its only Spirit World loot. Then cast Curse
  of Waking Nightmare on an Overworld player, confirm at most one weaker
  victim-bound hunter nearby, and clear the curse by Spirit World entry, cure
  rite, and Goddess Statue in separate runs.
- Spawn a Banshee with `/witchery spawn banshee`; confirm it has no idle
  phantom ambient, its scream uses the old lower-pitched Banshee clip, and
  hurt/death use the source Spectre hit sound.
- Spawn a Lord of Torment with `/witchery spawn lord_of_torment` and a Dark
  Mark with `witchery:dark_mark_spawn_egg`; confirm the Lord uses the old
  Torment laugh/hit/death clips with a roughly 10-second idle cadence, while
  Dark Mark uses the distant Torment laugh and has no hurt/death sound.
- Place a Beartrap and Wolftrap, open/close them by hand, then spring each with
  an appropriate target. Toggling should use the old `random.click` cue, and
  snapping shut should use the old `random.mantrap` cue rather than the
  compatibility `block.bear_trap.*` event names.
- As a vampire, bite a Villager, Village Guard, animal, player, and caged
  villager quest target; fill/drink a Glass Goblet, take Lilith's empty-goblet
  blood reward, consume Vampire Blood, let an Attack Bat feed, and use a Bone
  Needle as a level-4+ vampire on White Wool. Confirm blood-transfer and wool
  staining actions use `random.drink`, full goblet consumption burps, and the
  red dust feedback lands at the victim/reward/block position.
- Drain a Village Guard inside a village, then leave it idle with no combat
  target. Its health should slowly recover and its persisted blood should tick
  upward only occasionally; a guard outside a village or actively fighting
  should not recover blood from this passive path.
- Place and inspect Rowan, Alder, and Hawthorn leaves. Blocks and inventory
  items should read as green foliage from one combined texture, with Rowan
  berries red, Alder catkins yellow, and Hawthorn blossoms pale instead of
  grey/brown or layered tint artifacts.
- Check Goddess Statue and Crystal Ball GUI items: the statue should fit inside
  the slot, and the Crystal Ball should sit one pixel lower than the prior pass.
- Use a Goddess Statue while carrying a removable curse, including persisted
  Waking Nightmare. A natural Spirit World nightmare is not itself a curse. A
  successful cure should produce player-centered instant-spell particles with a
  low fizz cue; using it with nothing to cure should still give snare feedback.
- Stack normal and ice stockades two high. The lower stockade should switch to
  the stacked/fence-like body, the top should remain pointy, and both should
  damage players and mobs on contact.
- Wear the full Death set, use Hand of Death to deploy/retract the scythe, and
  confirm the held model protrudes from the arm, the scythe faces forward, and
  right-click toggles without losing the base item.

### Villages And Walls

- Generate multiple villages and confirm walls appear on roughly 40% of villages
  by default.
- Check square inner/outer wall corners, rampart stair angles, gatehouse access,
  and entity blocker relocation.
- Confirm unwalled villages remain valid and `/witchery walls` can still force a
  wall build for QA.

### Books And Player Docs

- Confirm the Primer's chapter unlock path still teaches stations in a workable
  order.
- Open Circle Magic rite recipe cards in the held Primer and placed Primer.
  Confirm ring diagrams, two-row offering layouts, coven faces, and captions fit
  for Beastial Call, Manifestation, Nature's Power, and a small two-item rite
  such as Sanctity.
- Draw a few ritual/otherwhere/infernal chalk marks and reroll one existing
  same-family mark; each successful draw should use the old chalk-scratch sound
  rather than a sand placement cue.
- Confirm altar setup instructions are concrete and readable in the in-game
  book.
- Confirm `Book_of_Shadows.html` and README match player-facing recipes,
  commands, and setup requirements.

### Witch's Triangle Scanner

- Craft `witchery:wooden_scanner` from Rowan Planks and Ender Dew, or take it
  from the tools section of the Witchery creative tab.
- Hold right-click while aiming at a block and at a living creature. Confirm the
  item is held in the two-arm bow-style pose, its centre switches to purple
  portal art while in use, and portal/witch particles appear at the target.
- Confirm a first scan of each unique block/entity id adds research points to
  the item tooltip, while repeated scans report the target as already understood
  and do not add points.
