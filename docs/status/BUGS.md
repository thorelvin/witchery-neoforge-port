# Active Bugs

Last reconciled: 2026-05-25. Bugs #2–59 are closed — see `docs/archive/BUGS_CLOSED.md` for the full history.

**Closed 2026-05-25:**
- ~~Coffin bottom exterior texture wrapping wrong~~ — spurious `inner_floor` parts (not in original source) removed from `CoffinModel.createHeadLayer()` and `createBottomLayer()`.
- ~~Mirror hitbox on wrong side / air gap between mirror and wall~~ — all four `VoxelShape` constants in `MirrorBlock.java` were inverted (slab was on the player side instead of the wall side). Fixed: NORTH=z=0-4, SOUTH=z=12-16, WEST=x=0-4, EAST=x=12-16.
- ~~Mirror World command entry at y=16 (ceiling)~~ — `findSafeEntry` y=200→-60 scan found ceiling first in 16-tall dimension; replaced with direct snap to `ROOM_FLOOR_Y`.
- ~~Mirror glass faces wall (N/S) — pending retest~~ — blockstate rotations (NORTH:y=180, SOUTH:y=0, WEST:y=270, EAST:y=90) are theoretically correct; likely a perception artifact of the now-fixed inverted VoxelShapes. Retest required.

---

## Open

### 1. Ritual queue parity is still partial

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
| Spirit World horizon/day-angle atmosphere and fresh-chunk vegetation/spawn QA | 1-2 hrs |
