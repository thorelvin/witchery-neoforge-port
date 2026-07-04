# QA Queue

Last curated: 2026-07-04

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

### Hobgoblin Chat Commands

- Leash a Hobgoblin, give it a pickaxe, then use chat orders for `mine`,
  `worship`, `stop`, `follow me`, and `guard me`.
- Confirm only the leash owner/admin can command it once ownership is claimed;
  other players should get the rope-owner refusal.
- Before ownership is claimed, trigger failure cases such as `mine` with no
  pickaxe, `worship` with no patron statue, or an impossible `come here` path;
  then confirm a different player can still claim it with a successful command.
- Ask command-keyword knowledge questions such as `what is the goblin altar?`
  and `how do I get koboldite dust?`; they should answer as dialogue, not rope
  denial or command execution.
- While following, confirm it tracks the command holder instead of resuming its
  normal player-avoidance or villager-hunting behavior.
- While guarding, have a hostile mob attack the command holder and confirm the
  Hobgoblin targets that threat, then returns to holding near the player after
  combat.
- Confirm `stop` clears mining, worship, follow, guard, and current combat
  orders without clearing the claimed command owner.

### Flame Imp Contracts

- Sign a blank ownership contract with the player's blood, then use it on a
  wild Flame Imp with at least 25 levels.
- Give shiny gifts until the imp is pleased.
- Bind each spell contract to a target, and also test crouch-use self binding.
- Confirm a pleased, unpowered imp spends contracts for Living Flame, Fiery
  Tolerance, Evaporation, Fiery Touch, and Melting Touch.
- Confirm refusal cases are clear: wrong owner, not pleased, powered imp, no
  contract target, or missing contract.

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
- Wells: test regular/small/cursed wells in forest/swamp terrain. Confirm
  surface adaptation, shaft/room clearance, round grass skirt blending, and no
  buried center features.

### Dream And Spirit World

- From the Overworld, record F3 coordinates, run `/witchery dream`, and confirm
  Spirit World X/Z match exactly.
- Y should stay in the same column and only move enough to find a safe landing.
- Run `/witchery exit` and confirm return to the stored Overworld coordinate.
- Confirm dream, nightmare, and demonic nightmare colors/fog match the current
  design; normal dreams should read visibly foggy, with nightmares denser.
- Confirm ordinary passive animals can spawn in Spirit World biomes.

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
- Block the first release space over a ritual cauldron and confirm gas/liquid
  Dog Tongue release chooses a nearby safe space instead of failing immediately.

### Visuals And Creative Tab

- Open the Witchery creative tab and confirm internal utility entries stay
  hidden: Pit Dirt, Pit Grass, duplicate Koboldite Pentacle, Witches' Shoes, and
  the unfinished Hobgoblin Patron statue should not appear as normal player
  picks.
- Place and inspect Rowan, Alder, and Hawthorn leaves. Blocks and inventory
  items should read as green foliage from one combined texture, with Rowan
  berries red, Alder catkins yellow, and Hawthorn blossoms pale instead of
  grey/brown or layered tint artifacts.
- Check Goddess Statue and Crystal Ball GUI items: the statue should fit inside
  the slot, and the Crystal Ball should sit one pixel lower than the prior pass.
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
