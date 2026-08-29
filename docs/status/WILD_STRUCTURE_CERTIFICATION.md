# Wild Structure Certification

Last certified: 2026-08-27

This ledger records the deterministic fresh-world, traversal, relocation, and
restart gate for the six non-village Wild structures. It distinguishes direct
1.7.10 ports from deliberate modern replacements and additive structures.

## Scope And Provenance

| Structure | Source relationship | Current shape and ownership |
|---|---|---|
| Coven Circle / Stone Circle | Directly based on 1.7.10 `ComponentCoven` | Exact 11 x 4 x 11 source footprint, center basin, hidden dispenser, two Witch spawners, three cobweb details, and source 90% Coven Witch roll. Modern code adds dry-site checks, biome adaptation, supports, and safe arrival. |
| Straw Man | Directly based on 1.7.10 `ComponentWickerMan` | Exact 6 x 8 x 5 source footprint and hay effigy with the hidden spawner marker. Modern code adds dry-site checks, local surface adaptation, and safe arrival. |
| Hobgoblin Hut | Deliberate designer-authored replacement for 1.7.10 `ComponentGoblinHut` | Cleaned 14 x 14 x 14 NBT hut with one Hobgoblin and no storage. It does not claim exact source shell geometry. |
| Wild Witch Hut | Deliberate reuse of the source-shaped 1.7.10 `ComponentVillageWitchHut` port | Compact 4 x 6 x 5 adaptive hut with Witchery loot and the existing Coven Witch / Crazy Cat Lady resident roll. The original natural `ComponentShack` was 7 x 10 x 7, so this is not claimed as an exact natural-shack shell. |
| Warped Coven | Modern additive structure | Designer-authored 17 x 17 x 17 NBT structure for warped forests. No matching 1.7.10 Wild structure was found. |
| Vanishing Hut | Modern additive structure | Designer-authored 7 x 10 x 9 moving hut with Witchery loot, one identity-bound Coven Witch, and persisted relocation ownership. No matching 1.7.10 Wild structure was found. |

Current implementation evidence lives in `src/main/java/com/emoniph/witchery/worldgen/`,
the six `src/main/resources/data/witchery/worldgen/structure/*.json` files, their
structure sets and biome tags, and the packaged structure templates.

## Registry And Placement Inventory

All six structures use empty spawn overrides and a locate range of 80 chunks.

| Structure | Biomes | Generation step / adaptation | Random spread |
|---|---|---|---|
| Coven Circle | `#minecraft:is_overworld` | `surface_structures` / `beard_thin` | spacing 32, separation 8, salt 502938471; excludes villages within 4 chunks |
| Straw Man | `#minecraft:is_overworld` | `surface_structures` / `none` | spacing 32, separation 8, salt 744019231; excludes villages within 4 chunks |
| Hobgoblin Hut | `#witchery:has_hobgoblin_hut` (forest, swamp, mangrove swamp) | `surface_structures` / `none` | spacing 32, separation 8, salt 847291035 |
| Wild Witch Hut | `#minecraft:is_forest` | `surface_structures` / `none` | spacing 36, separation 10, salt 1630924712 |
| Warped Coven | `#witchery:has_warped_coven` (warped forest) | `top_layer_modification` / `none` | spacing 48, separation 16, salt 604183902 |
| Vanishing Hut | `#witchery:has_vanishing_hut` (forest, swamp, mangrove swamp) | `surface_structures` / `none` | spacing 44, separation 20, salt 681240973 |

`WildStructureRegistryTest` locks this registry contract, including biome tags,
template paths, structure-set parameters, and source-backed marker details.

## Deterministic Fresh-World Gate

The harness generated one fixed-seed world, located every structure through
normal world generation, audited the actual piece bounding box, selected a
collision-safe exterior landing, and proved a walkable route to an interior
target. `blocks` counts non-air blocks inside the audited piece box.

| Structure | Certified anchor | Box | blocks | Required content | Landing -> target | Route |
|---|---:|---:|---:|---|---|---:|
| Coven Circle | `88,68,376` | 11 x 4 x 11 | 264 | Witch spawner, abandoned-mineshaft loot dispenser; sampled Coven Witch | `88,68,370` -> `88,69,374` | 4 |
| Straw Man | `263,69,40` | 6 x 8 x 5 | 82 | Skeleton spawner marker | `263,70,37` -> `263,70,39` | 2 |
| Hobgoblin Hut | `-232,118,-2312` | 14 x 14 x 14 | 539 | one Hobgoblin, no storage or loot | `-233,121,-2308` -> `-233,121,-2310` | 2 |
| Wild Witch Hut | `-455,63,824` | 4 x 6 x 5 | 88 | one Coven Witch in this sample, Witchery hut loot | `-456,63,821` -> `-456,64,824` | 15 |
| Warped Coven | `-3528,37,-376` | 17 x 17 x 17 | 735 | one Coven Witch, Warped Coven loot | `-3529,39,-379` -> `-3529,39,-377` | 2 |
| Vanishing Hut | `-1064,74,1000` | 7 x 10 x 9 | 303 | exact expected Coven Witch, Vanishing Hut loot | `-1064,76,997` -> `-1064,76,1001` | 4 |

All six audits reported zero fluid intersections and no failures. Arrival support
uses collision geometry, so valid slabs and stairs count as footing without
requiring a full cube.

## Relocation And Restart

The natural Vanishing Hut moved from `-1064,74,1000` to `-1030,80,1039`. The
relocated 7 x 10 x 9 hut retained its expected resident UUID and loot, passed a
four-step route from `-1030,82,1036` to `-1030,82,1040`, and remained at the new
anchor after restart. The other five structures were recertified at their fresh
anchors after the same server restart. Every restart audit passed.

The Vanishing Hut's natural placement and relocated placement intentionally use
different biome rules: world generation remains forest/swamp scoped, while a
valid unobserved relocation may move within its bounded search area.

## Live Client Evidence

Controlled captures are retained locally under
`C:\claude\witchery\captures\wild-structures\port`:

- `coven-circle-overview.png`, `coven-circle-after-walk.png`
- `straw-man-overview.png`, `straw-man-after-walk.png`
- `hobgoblin-hut-overview.png`, `hobgoblin-hut-after-walk.png`
- `witch-hut-overview.png`, `witch-hut-interior.png`
- `warped-coven-overview.png`, `warped-coven-interior.png`
- `vanishing-hut-relocated-overview.png`, `vanishing-hut-relocated-after-walk.png`

The client pass confirmed terrain contact and recognisable silhouettes, the open
Hobgoblin doorway and furnished resident room, the compact Witch Hut interior,
survival movement through the vine-obscured Warped Coven entrance, and the same
Vanishing Hut witch beside the oxidized copper chest after relocation. The Witch
Hut's safe route correctly goes around to its south door rather than straight
through the terrain-facing wall.

## Commands And Evidence

```powershell
python tools/runtime_test_harness.py wild-structure-certification --startup-timeout 300
python tools/runtime_test_harness.py validate
```

The machine-readable result is
`build/runtime-test/wild-structure-certification/summary.json`. The focused JVM
contracts are `WildStructureRegistryTest`, `NbtStructureTemplateTest`, and the
Vanishing Hut test family.

## Conclusion

The current six-structure Wild roster is certified for registry wiring,
fixed-seed fresh generation, structure content, safe arrival, internal
traversal, restart, and Vanishing Hut relocation identity. Remaining Wild work
is ordinary long-session distribution and incidental visual feel, not another
mechanical certification package. Wells remain a separate structure family.
