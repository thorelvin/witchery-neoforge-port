# QA Queue

Last curated: 2026-06-21

This is the active in-game QA queue. It replaces scattered new QA notes in
the archived parity/fixes/ritual QA notes under `docs/archive/status/` and small
one-off status files. Keep entries concrete: setup, action, expected result.

## How To Use

- Add only tests that a player or tester can actually run.
- Put long research or source comparisons in reference docs, then link them.
- Remove or archive entries once they are verified and no longer useful.
- Prefer commands and item IDs where possible.

## Active Focus

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
  design.
- Confirm ordinary passive animals can spawn in Spirit World biomes.

### Cursed Blocks And Wells

- Use triggered splash brews on doors, buttons, levers, pressure plates, and
  ladders.
- Confirm the block keeps its original block state and releases the stored brew
  only under the intended trigger condition.
- For warning brews, say a test line near the cauldron before bottling and
  confirm the text appears in purple actionbar text when triggered.
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
  and Gas Mask protection.

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
