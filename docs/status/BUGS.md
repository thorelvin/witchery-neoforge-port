# Active Bugs

Last reconciled: 2026-05-31. Latest village structure/wall bugs are closed; see `docs/archive/BUGS_CLOSED.md` for the older history.

**Closed 2026-05-31:**
- ~~Witchery village buildings never appear in vanilla villages~~ - the procedural `WitcheryVillageElement` had no connectable jigsaw block, so village house pools could list Witchery buildings without ever attaching them to streets. Fixed: the element now exposes a north-facing `minecraft:building_entrance` connection for the generated piece, allowing apothecaries, bookshops, witch huts, watch towers, and keeps to be selected by village pools.
- ~~Village wall generation freezes when `/witchery goto` is chained~~ - wall setup was force-loading every chunk in the village bounding box plus padding in one server tick. Fixed: wall jobs now use temporary forced-chunk tickets instead of synchronous chunk generation, and `/witchery goto` has a per-player cooldown.
- ~~Village walls/gates still fail on water and hill edges~~ - the RC wall pass now avoids placing walls under water, keeps walls at water level when crossing water, centers gatehouses on the wall line, grades approaches to hilly gates, prefers biome-appropriate road/path materials, and keeps corners from overwriting gates.
- ~~Village wall gates can attach sideways to a nearby path~~ - gate detection accepted any road ray that reached a wall cell, even if the road clipped the side of a wall segment/corner. Fixed: gate candidates now require the road to hit a three-wide flat wall face head-on, with interior behind the approach and empty/outside space beyond the wall.
- ~~Straw Man can generate below water~~ - the first pass only checked center/corner water and allowed all overworld biomes, so water inside the 6x5 footprint or river/ocean terrain could slip through; a later report showed the structure-selection heightmap guard could still be fooled by actual water at placement time. Fixed: generation rejects any wet footprint column and ocean/river biomes before adding the piece, and `StrawManPiece.postProcess` now aborts before writing blocks if the live 6x5 footprint has water by fluid or heightmap state.
- ~~Automatic village walls can be missed around `/witchery goto` targets~~ - the queue could rely on a chunk-load event from the wrong village chunk or mark an early empty start as processed, while `/witchery goto` only teleported and hoped the queue fired. Fixed: queued starts refresh/retry until ready, non-start village chunks can queue via structure references, and village goto now queues wall pre-generation before teleport.
- ~~Village wall pre-generation can freeze the integrated server~~ - the 11:13 plains log showed wall generation finishing but only after one tick synchronously force-loaded 56 perimeter chunks, producing a 4.59s server catch-up warning and a later client shutdown watchdog. Fixed: `/witchery goto village_*` and `/witchery walls` now queue temporary forced-chunk wall jobs, wait for needed wall chunks to load over normal ticks, generate walls, release tickets, and only then complete pending village teleports.
- ~~Automatic village walls can generate with zero gate candidates~~ - the 11:31 logs showed several automatic villages reaching `0 gate candidate(s)` because the queued job waited only for perimeter chunks while gate detection scans existing village roads inside the wall footprint. Fixed: wall jobs now ticket the full padded village wall footprint before `VillageWallGenerator.generate`, so automatic and goto-generated walls see road chunks before selecting gates.

**Closed 2026-05-30:**
- ~~`/witchery goto` teleports player far underground~~ — `Level.getHeight(MOTION_BLOCKING)` returns `0` (not `-64`) for cold/unloaded chunks; old threshold `<= -59` missed it. Fixed: threshold now `< 10`.
- ~~Village walls have no gate opening~~ — `placeGatePillar` cleared terrain but never placed a gate block; gate detection only scanned 1 cell deep ±4 height. Fixed: detection now scans WALL→INTERIOR up to depth 3 ±8 height and also checks GRAVEL; middle cells now place `OAK_FENCE_GATE`. `GAP_FILL` reduced 7→4 to reduce stub wall protrusions.

**Closed 2026-05-25:**
- ~~Coffin bottom exterior texture wrapping wrong~~ — spurious `inner_floor` parts (not in original source) removed from `CoffinModel.createHeadLayer()` and `createBottomLayer()`.
- ~~Mirror hitbox on wrong side / air gap between mirror and wall~~ — all four `VoxelShape` constants in `MirrorBlock.java` were inverted (slab was on the player side instead of the wall side). Fixed: NORTH=z=0-4, SOUTH=z=12-16, WEST=x=0-4, EAST=x=12-16.
- ~~Mirror World command entry at y=16 (ceiling)~~ — `findSafeEntry` y=200→-60 scan found ceiling first in 16-tall dimension; replaced with direct snap to `ROOM_FLOOR_Y`.
- ~~Mirror glass faces wall (N/S) — pending retest~~ — blockstate rotations (NORTH:y=180, SOUTH:y=0, WEST:y=270, EAST:y=90) are theoretically correct; likely a perception artifact of the now-fixed inverted VoxelShapes. Retest required.

---

## Open

### 1. Village goto plains/savanna hang needs follow-up retest

The 2026-05-31 `/witchery goto village_savanna` hang stopped immediately after the success chat at `8560 ~ -3072` and before `[VillageWalls] Generating walls...`, pointing to post-teleport chunk/view loading rather than confirmed wall placement. A later `/witchery goto village_plains` hang left no exception in `latest.log` and did not reach the `[Goto] Prepared...` log, so the remaining risk moved earlier: pre-teleport structure lookup or synchronous arrival chunk generation. Follow-up fixes: `/witchery goto` now logs before locate, uses a vanilla-style bounded locate search (`skipReferenced=false`, radius 32), preloads only the arrival chunk instead of a 5x5 full-chunk area, keeps the 30 second cooldown, resets motion/fall distance after teleport, removes the artificial wall-generation delay, skips wall columns occupied by living entities, keeps savanna on dirt/stone materials instead of desert sandstone/sand, makes gate/path support fill match the gatehouse block, and replaces synchronous chunk force-loading with a temporary forced-chunk wall job that completes before village goto teleports the player. The latest 11:31/11:33 logs exposed the next wall-job issue: perimeter-only jobs could generate automatic walls with `0 gate candidate(s)` or enter `generate()` without the road footprint ready, so jobs now wait for the padded wall footprint before generating. Retest on fresh plains/savanna/taiga/snowy villages after restarting the client.

### 2. Ritual queue parity is still partial

Current gaps:

- **Session-only active state.** `ACTIVE_RITUALS` and `CLIMATE_CHANGE_PLANS` are static maps keyed by dimension — session memory, not saved world state. Long-running rites can vanish on server crash after consuming resources. A clean-shutdown refund path exists (`RitualExecutor.cancelAllWithRefund` + `RitualEvents.onServerStopping`) but crash loss is still possible. Fix: migrate to level `SavedData`, a dimension attachment, or a ritual-anchor block entity.
- **`RitualTransaction` lacks a true rollback path.** It currently commits altar power first, then offerings, with no undo. A post-commit failure can silently lose resources. Target pattern: `validate → prepareEffect → commit → applyEffect`.
- **Representative-trigger gap.** All rite routes use one representative dropped ingredient instead of the full original multi-sacrifice recipe registry. Functionally correct but not full 1.7.10 parity.

Files:
- `src/main/java/com/emoniph/witchery/ritual/RitualExecutor.java`
- `src/main/java/com/emoniph/witchery/events/RitualEvents.java`

See `docs/status/NEXT_FIXES_PLAN.md` for the ordered parity roadmap.

---

## Backlog

| Item | Effort |
|---|---|
| Fresh-village wall/building QA across plains, desert, savanna, taiga, and snowy villages | 1-2 hrs |
| Spirit World horizon/day-angle atmosphere and fresh-chunk vegetation/spawn QA | 1-2 hrs |
