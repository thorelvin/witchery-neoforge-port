# Active Bugs & Fix Plan

Last reconciled: 2026-05-18

This file is the active sprint board. When a bug is fixed, move it to **Fixed / Verify** with the commit/task notes instead of deleting it.

---

## Priority board

| Priority | Item | Effort | Status |
|---|---|---:|---|
| Done | Remove GhostModel mouth protrusion | 2-5 min | Fixed (mouth part never present in port) |
| Done | Fix CatFamiliarModel body rotation | 1-5 min | Fixed (body uses PartPose.offset, no rotation) |
| Done | Fix Mandrake spawn category mismatch | 1-5 min | Fixed (MONSTER category + spawn placement registered) |
| Done | Register spawn placements for biome-spawned mobs | 10-20 min | Fixed |
| Done | Demon UV/texture-size audit | 20-45 min | Fixed - model corrected to 128x32, all UVs at y=0 row, wings added |
| Done | Werewolf pre-render body hide | 20-45 min | Fixed - SyncPlayerDataPayload now includes entityId; handleOnClient updates correct tracked entity |
| Medium | Decide Torment red pulse/fog effect | 5-20 min | Decision made; keep current look |
| Feature | Spirit World vegetation + Nightmare spawning polish | 1-3 hrs | Backlog |

---

## Open bugs

### 1. Ritual queue parity is still partial

Current state:
- Circles now persist after activation again.
- Rite ingredients now consume only on successful activation.
- Weather Call Storm and Rain of Toads now run as staged active rituals instead of one-shot effects.

Remaining gap:
- The original tile-entity ritual queue, upkeep lifecycle, and abort/refund behavior are only partially restored.

Files:
- `src/main/java/com/emoniph/witchery/ritual/RitualExecutor.java`
- `src/main/java/com/emoniph/witchery/events/RitualEvents.java`

---

## Fixed / verify

### 2. GhostModel mouth protrusion - Fixed

Current `GhostModel` has no mouth part - it was never introduced in the port. Arm UV sharing (both arms at `texOffs(0,0)`) may still cause slight Poltergeist texture mirroring issues but no protrusion geometry exists. Verify in-game if arm UV ever looks wrong.

---

### 3. Familiar cat body rotation - Fixed

`CatFamiliarModel` body already uses `PartPose.offset(0, 11, 2)` (no rotation). Leg positions are `(+-3, 18, 7)` back and `(+-3, 18, -5)` front.

---

### 4. Demon model transparent holes - Fixed

`demon.png` is 128x32. Model now declares `128, 32`. All part UVs corrected to y=0 row: body `texOffs(64,0)`, arms `texOffs(48,0)`, legs `texOffs(32,0)`. Head detail parts (horns, tusks, snout, lip) restored. Wings added at `texOffs(93,0)`.

---

### 5. Werewolf player body visible under model - Fixed

`WitcheryClientEvents` already implements `RenderPlayerEvent.Pre` (hides model) and `RenderPlayerEvent.Post` (restores it).

---

### 6. Spawn placement warnings - Fixed

Handled by spawn placement registration for the biome-spawned mobs.

---

### 7. Mandrake mob spawn category mismatch - Fixed

`MANDRAKE_MOB` already uses `MobCategory.MONSTER` and is registered in `RegisterSpawnPlacementsEvent`.

---

### 8. Torment red pulse / overlay decision - Closed

The current Nether-style pulse is being kept as the intended hell aesthetic.

---

## Feature-polish backlog, not blocking bugs

### Item review - remaining visual/mechanic issues

Active item review in `docs/status/ITEM_REVIEW.md`. Priority remaining items:
- `altar` - mechanics are back near original 2x3 behavior; block/item visuals still need an art pass
- `distillery`, `spinning_wheel`, `witches_oven` - mechanics were restored closer to original; GUI texture/layout verification is still open
- `crystal_ball` - must be transparent
- `coffin` - shows only one side in inventory
- `alluring_skull`, `demon_heart` - wrong 3D model/UV
- `poppet_shelf` - poppets don't show on top
- Full Tools/Armour/Brew/Spawn egg sections - untested

---

### Corpse entity renderer

`witchery:corpse` uses `NoopRenderer` (invisible). Needs a humanoid sleeping-pose model to be visible to other players. Low priority - entity is hittable and functional server-side.

### Spirit World remaining content

- Wispy Cotton vegetation - Done
- Disturbed Cotton vegetation - Done
- Nightmare spawns only during nightmare state - Done
- Nightmare teleport/respawn pressure - Done
- Dream Weaver blocks affecting dream/nightmare chance - see NEXT_TASKS Sprint 3 #1

### Torment content

- Lord of Torment boss flow (phase 1 -> 2, player return) - Done
- Flame Imp -> Torment Scroll progression - Done
- Maze generator / maze-like structure - deferred
- Torment Stone block and portal exit - deferred
- Torment red pulse - `"effects": "minecraft:the_nether"` in `dimension_type/torment.json` intentionally kept for the hellish red-fog aesthetic
