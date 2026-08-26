# Village Certification

Last certified: 2026-08-26

This is the repeatable mechanical gate for Witchery's village ecosystem. It is
not a substitute for live-client visual, traversal, guard-movement, or
distribution judgment.

## Run It

```powershell
python tools/runtime_test_harness.py village-certification --startup-timeout 600
```

The harness creates a normal-difficulty dedicated server with structures and
NPCs enabled, seed `witchery-runtime-test`, and a fixed five-biome matrix. For
each qualifying village it runs `/witchery verify village`, saves and stops the
server cleanly, restarts the same world, and recertifies the same coordinates.
It fails if any wall-generation log reports `suffocated in a wall`.

The verifier requires:

- one persistent claim for every canonical Witchery building that actually
  generated, with no duplicate type/origin claim;
- real blocks from the enclosing village's plains, desert, savanna, taiga, or
  snowy material family;
- the expected origin-marked residents: Apothecary 1, Bookshop 1, Witch Hut 1,
  Watch Tower 3, and Keep 12;
- persisted wall records with at least one road candidate and one selected gate;
- a loaded collision-safe landing point.

## Certified Matrix

| Family | Village center | Claims | Materials | Residents | Gates / candidates | Landing |
|---|---:|---:|---:|---:|---:|---:|
| Plains | `149,64,594` | `1/1` | `1/1` | `1/1` | `3/139` | `148,63,594` |
| Desert | `4579,64,4192` | `2/2` | `2/2` | `2/2` | `3/198` | `4578,82,4192` |
| Savanna | `-748,64,2556` | `1/1` | `1/1` | `3/3` | `3/67` | `-748,80,2553` |
| Taiga | `3916,64,196` | `1/1` | `1/1` | `1/1` | `3/46` | `3916,72,196` |
| Snowy | `271,64,371` | `3/3` | `3/3` | `5/5` | `3/124` | `271,65,371` |

All five rows passed fresh and after restart. The nearest first taiga candidate
at `-789,64,-811` legitimately contained no Witchery building because house-pool
selection is weighted; the harness recorded that outcome, removed its temporary
force loads, and selected the next unique taiga village instead of weakening the
assertion.

## Persistence And Safety Contract

`WitcheryStructureData` stores successful village-building claims only after
piece placement. Spawned residents carry durable building type and origin
provenance. `VillageWallData` stores a wall key plus candidate/gate counts, and
manual or queued wall jobs release only forced chunks whose tickets they
actually acquired. Wall, gatehouse, and graded-path blockers are moved only to
solid, dry, collision-free nearby ground; a final bounded village-envelope scan
rescues any living entity still intersecting generated terrain.

No migration for older remake saves is provided yet. The project currently
targets new worlds and current-format saves by explicit decision.

## Evidence

- Machine-readable result: `build/runtime-test/village-certification/summary.json`
- Per-biome captures: `build/runtime-test/village-certification/captures/`
- Fresh/restart logs: `build/runtime-test/village-certification/minecraft-*.log`
- Structure contracts: `VillageStructureContractTest`
- Wall persistence contract: `VillageWallDataTest`
- Villager lifecycle fixture: `witchery:were_villager_state_cycle`
- Magic/sacrifice fixture: `witchery:villager_magic_and_sacrifices`
