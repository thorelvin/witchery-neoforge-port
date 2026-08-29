# Witch Well Certification

Certified: 2026-08-28

This ledger is the source of truth for the regular, small, and cursed-cave Witch
Well structure family. It records provenance, the cleaned designer-template
contract, modern terrain adaptation, fixed-seed natural generation, traversal,
curse behavior, restart persistence, and controlled client captures.

## Provenance Boundary

Original Witchery `1.7.10-0.24.1` contains no Witch Well class, registry entry,
structure generator, or matching asset. A filename audit of the canonical
decompiled source tree returned no well implementation. These structures are an
additive feature based on three supplied Structure Block exports:

- `well.nbt`
- `small_well.nbt`
- `well_cave.nbt`

The word `source` below therefore means the supplied designer-authored NBT, not
original Witchery. Exact authored geometry and contents are preserved after
captured terrain, air, leaves, structure blocks, and editor residue are removed.
Modern Java owns biome eligibility, terrain fitting, support, clutter clearing,
loot binding, curse triggers, indexing, safe arrival, and traversal. No
original-client comparison exists for an additive structure.

## Template Ledger

| Variant | Packaged template | Size | Captured entries | Authored blocks | Block entities | Fluid-bearing cells | Bytes | Source SHA-256 |
|---|---|---:|---:|---:|---:|---:|---:|---|
| Regular | `data/witchery/structure/witch_well/well.nbt` | 15 x 15 x 14 | 3,150 | 138 | 3 | 1 | 9,360 | `E35EBA38E8E9E56E80F31E985351A67BEF3D0BB2877C7337AA0A12059144FA89` |
| Small | `data/witchery/structure/witch_well/small_well.nbt` | 10 x 8 x 10 | 800 | 42 | 0 | 1 | 2,967 | `B3202B2A6B087AED224EAB317A2A45DCBF5F7FDAA46D76F39198F329788B4584` |
| Cave | `data/witchery/structure/witch_well/well_cave.nbt` | 30 x 20 x 30 | 18,000 | 496 | 19 | 7 | 54,549 | `B802780182A0F5541FF61748959A190EAD20C641535E00DC0C7A15A06ED89C28` |

`WitchWellSourceContract` pins each cleaned block palette as well as these totals.
The seven cave fluid-bearing cells are three Water blocks, one waterlogged
trapdoor, and three Flowing Spirit blocks. The small template retains one
waterlogged authored state beside its Cauldron.

## Generation Contract

- One `witchery:witch_well` structure family generates in the
  `witchery:has_witch_well` forest/swamp biome tag.
- The random-spread set uses spacing 40, separation 12, and salt `912347651`.
- A ten-way preferred roll chooses cave at 10%, small at 50%, and regular at
  40%. If a cave or small site fails its terrain contract, generation retries the
  regular variant; a failed regular site remains empty.
- Regular samples its full 15 x 14 footprint and accepts at most three blocks of
  height variance.
- Small anchors from the six surviving designer dirt-ring samples, accepts at
  most one block of variance, and adapts its compact skirt after placement.
- Cave samples an oval surface footprint, accepts at most four blocks of
  variance, repairs support below the surface, and clears only its authored
  shaft, connector, transition, and lower-room masks.
- Surface and support material follow the local terrain family, including grass,
  podzol, mycelium, moss, mud, clay, gravel, sand, red sand, and dirt variants.
- Authored cave fluids are restored after terrain backfill. Cross-chunk two-block
  Coffins are restored as a pair so one clipped placement cannot delete the
  other half.

## Fixed-Seed Certificate

Harness: `python tools/runtime_test_harness.py witch-well-certification`

Seed name: `witchery-runtime-test`  
Numeric seed: `-119995921`

| Variant | Natural anchor | Box | Authored | Adaptive surface | Authored fluids | Loot | Safe route |
|---|---|---:|---:|---|---:|---|---:|
| Regular | `-3176,94,4728` | 15 x 15 x 14 | 138/138 | Dirt 2, Grass 201 | 1/1 | `witchery:chests/witch_well` | 5 blocks |
| Small | `-520,69,1304` | 10 x 8 x 10 | 42/42 | Grass 55 | 1/1 | None | 3 blocks |
| Cave | `-568,65,-2504` | 30 x 20 x 30 | 496/496 | Dirt 2, Grass 227 | 7/7 | `witchery:chests/witch_well` | 23 blocks |

The cave's 401 total fluid cells include naturally generated aquifer water; the
certificate separately identifies and requires all seven authored cells. Its
segmented path consists of an eight-block exterior approach and a fifteen-block
shaft/lower-room route. Non-colliding circle glyphs and decoration count as
passable, matching their actual collision shapes.

The harness creates a fresh deterministic world, probes up to 64 deterministic
candidates per requested variant, requires all three to generate naturally,
flushes and stops the server, restarts the same save, and recertifies the same
variant identities, anchors, blocks, block entities, fluids, loot, triggers,
landings, and routes. Fresh and restart stages both pass. The machine-readable
result is `build/runtime-test/witch-well-certification/summary.json`.
The complete `python tools/runtime_test_harness.py all --startup-timeout 300`
release gate also passed on 2026-08-28, including all earlier resource,
GameTest, save/reload, village, and Wild structure stages.

## Cursed-Cave Contract

The cave entrance ladder spans `-568,59,-2504` through `-568,61,-2504` in the
fixed-seed world.

- The top ladder displays `You feel uneasy.` with a 100-tick per-player warning
  cooldown and applies no poison or curse payload.
- The lower ladder carries the real composed curse made from Nether Wart,
  Spider Eye, and Zombie Head.
- The trigger records survive restart and are audited independently.

## Controlled Captures

The property-gated real-client capture harness is dormant in ordinary play. Run:

```powershell
.\tools\run_witch_well_capture.ps1 -WaitSeconds 300 -Width 1920 -Height 1080
```

The wrapper copies the certified world into a disposable client save, accepts
only that copy's custom-dimension compatibility warning, and removes the copy
after capture; the certified server save is never opened for client writes. It
writes seven 1920 x 1080 views under
`build/runtime-test/witch-well-certification/captures/visual/`:

- `regular-exterior.png`
- `regular-center.png`
- `small-exterior.png`
- `small-center.png`
- `cave-exterior.png`
- `cave-shaft.png`
- `cave-lower.png`

The reviewed set shows both surface structures in their naturally adapted forest
terrain, the cave's visible surface ruin, its water-and-ladder shaft, and its
authored lower chamber. Foliage is intentionally not removed for the evidence;
the captures prove natural integration rather than a staged void placement.

## Verification Surface

- `/witchery verify well [any|regular|small|cave]` audits the nearest loaded
  natural start and emits one parseable PASS/FAIL line.
- `witchery:witch_well_cursed_ladder_triggers` locks the warning-only top ladder
  and consuming lower curse on real server ticks.
- `WitchWellSourceContract` plus `NbtStructureTemplateTest` lock all three exact
  NBT signatures, cleaned palettes, anchors, and cleanup masks.
- `python tools/runtime_test_harness.py validate` rejects manifest drift.
- `python tools/runtime_test_harness.py witch-well-certification` is the complete
  fresh-world plus restart release certificate for this family.

## Closed Defects

- The small template no longer contains copied terrain, leaves, air, or editor
  residue.
- Variant terrain variance is measured against the right footprint instead of a
  single shared threshold.
- Cave backfill no longer replaces authored Flowing Spirit or water cells.
- Cross-chunk Coffin placement no longer destroys its neighboring half.
- Arrival routing honors empty collision shapes on circle glyphs and decorative
  blocks.
- Candidate search budgets are per variant, preventing earlier probes from
  starving the small-well search.

Remaining work is ordinary long-session distribution and visual feel in other
seeds and biome edges. Additive Well Below progression, bargains, Witch Forest,
and Baba Yaga content remain separate design packages.
