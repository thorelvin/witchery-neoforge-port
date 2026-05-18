# Active Bugs & Fix Plan

Last updated: 2026-05-15 (post-test session)

---

## 1. Torment ambient red pulse

**Symptom:** Torment dimension has a constant red pulsing overlay — same visual style as werewolf's black overlay.

**Cause:** `"effects": "minecraft:the_nether"` applies vanilla Nether's ambient red fog and overlay effect.

**Decision needed:** Keep as deliberate hell aesthetic, or change to a custom/neutral sky effect (`"minecraft:overworld"` or a new custom effect JSON).

**Fix (if changing):** In `data/witchery/dimension_type/torment.json`, change `"effects"` to `"minecraft:overworld"` and add a `"fog_color"` override in the biome JSON to keep the red tone without the animated overlay.

---

## 2. Banshee "witch nose" — GhostModel mouth protrusion

**Symptom:** Banshee renders with a small box sticking out of its face (looks like a witch nose / graphical artifact).

**Cause:** The original `ModelSpectre` had `addBox(0, 0, 0, 4, 5, 0)` (zero depth = flat invisible quad). We replaced depth=0 with depth=1 to avoid degenerate geometry, but depth=1 creates a visible protrusion at `PartPose.offset(-2, -4, -5)`.

**Fix:** Remove the `mouth` child part entirely from `GhostModel.createBodyLayer()`. The original flat quad was effectively invisible anyway and the face shape reads fine without it.

**File:** `src/main/java/com/emoniph/witchery/client/model/GhostModel.java`

---

## 3. Poltergeist holes / bad rendering

**Symptom:** Poltergeist has visible holes/transparency artifacts in the model.

**Likely cause:** Same mouth protrusion issue as Banshee (shared GhostModel). May also have arm UV overlap where `texOffs(0, 0)` for both arms causes the right/left sides to share the same UV region without mirroring.

**Fix:** Remove mouth (same as Bug 2). Check if arm UV needs offset — left arm could use `texOffs(14, 0)` to avoid overlap.

**File:** `src/main/java/com/emoniph/witchery/client/model/GhostModel.java`

---

## 4. Familiar cat body — "turtle" shape, wrong proportions

**Symptom:** Cat familiar body looks like a turtle — wrong proportions, weird spacing between body and legs.

**Cause:** `CatFamiliarModel` body uses `PartPose.offsetAndRotation(0, 11, 2, Mth.HALF_PI, 0, 0)`. Rotating the body 90° on X axis makes it stand vertically like a pillar instead of lying horizontal. The original ModelOcelot had **no rotation** on the body — it was purely `offset(0, 11, 2)`.

**Fix:** Change body PartPose to `PartPose.offset(0, 11, 2)` (remove the `Mth.HALF_PI` rotation). Also verify leg positions match the original (back legs at `(±3, 18, 7)`, front legs at `(±3, 18, -5)`).

**File:** `src/main/java/com/emoniph/witchery/client/model/CatFamiliarModel.java`

---

## 5. Demon — transparent holes in arms and legs

**Symptom:** Demon has large transparent holes visible in arms, legs, and possibly other body parts.

**Cause:** `demon.png` is likely **64×32** pixels but `DemonEntityModel` declares `LayerDefinition.create(mesh, 64, 64)`. Leg UV at `texOffs(0, 36)` falls completely outside a 32-pixel-tall texture, sampling transparent/empty pixels. The arm UV `texOffs(40, 16)` may also be partially out of bounds.

**Fix:**
1. Check actual `demon.png` dimensions (open file in image viewer).
2. If 64×32: change declaration to `LayerDefinition.create(mesh, 64, 32)` and remap all UV offsets to fit within 32px height. Move legs to available space.
3. If 64×64: the texture is correct — investigate whether the `.mirror()` on `left_arm` causes UV bleeding.

**Files:** `src/main/java/com/emoniph/witchery/client/model/DemonEntityModel.java`, `src/main/resources/assets/witchery/textures/entity/demon.png`

---

## 6. Player body still visible under werewolf form

**Symptom:** Player skin remains visible underneath the wolfman model overlay.

**Cause:** `WerewolfLayer.render()` calls `getParentModel().setAllVisible(false)` AFTER the player model has already rendered this frame. The hide only takes effect on the next frame — one-frame lag, but also fails entirely if any layer rendering is skipped.

**Fix:** Use `RenderPlayerEvent.Pre` (NeoForge game bus, `Dist.CLIENT`) to call `setAllVisible(false)` BEFORE the player renders. This avoids the delay entirely.

```java
// In WitcheryClientEvents (already exists):
@SubscribeEvent
public static void onRenderPlayerPre(/* RenderPlayerEvent.Pre or equivalent */ event) {
    if (WitcheryAttachments.get(event.getEntity()).isWerewolf()) {
        event.getRenderer().getModel().setAllVisible(false);
    }
}
// Also add Post handler to restore visibility
```

Verify the correct NeoForge 21.1.229 event class — may be `RenderPlayerEvent.Pre` or `RenderLivingEntityEvent.Pre<AbstractClientPlayer, PlayerModel<...>>`.

**File:** `src/main/java/com/emoniph/witchery/events/WitcheryClientEvents.java`

---

## 7. Dream World content — missing features

**Symptom:** Dream World generates correctly but lacks gameplay content.

**Missing (from original Witchery):**
- **Nightmare mob** — horse-like entity that spawns in Dream World and attacks sleeping players' dream selves. Needs entity class + model + renderer + AI goals.
- **Wispy cotton** — cotton plant variant that spawns as world vegetation in Dream World. Can be harvested. Used in Dream Weaver crafting.
- **Icy Needle exit mechanic** — original: throw an `icy_needle` item into a pool of water while in Dream World to wake up (teleport back). Our current exit is `/witchery exit` only.

**Fix plan:**
1. Register wispy cotton as a biome feature in `data/witchery/worldgen/biome/dream_world.json` (add feature entries in the features array).
2. Create `NightmareEntity` extending `Monster` — horse-like model or use vanilla horse with custom renderer. Add to dream_world biome spawners.
3. Wire `icy_needle` item: `PlayerInteractEvent.RightClickBlock` — if player is in Dream World and target block is water, call `exitDimension()`.

**Files:** `data/witchery/worldgen/biome/dream_world.json`, new `NightmareEntity.java`, `events/WitcheryPlayerEvents.java` or new event handler.

---

## 8. RegisterSpawnPlacementsEvent — missing registrations

**Symptom:** Each world load logs a NeoForge warning for 5 entities not registered with spawn placements:
`witchery:banshee`, `witchery:poltergeist`, `witchery:spectre`, `witchery:mandrake_mob`, `witchery:demon`

**Cause:** These entities are in biome spawn lists but haven't registered spawn placement restrictions.

**Fix:** Add a `@SubscribeEvent` on the MOD bus:
```java
event.register(WitcheryEntities.BANSHEE.get(),
    SpawnPlacementTypes.NO_RESTRICTIONS,
    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
    Monster::checkMonsterSpawnRules);
// repeat for spectre, poltergeist, mandrake_mob, demon
```

**File:** `init/WitcheryEntities.java` or a new `@EventBusSubscriber(Bus.MOD)` method.

---

## 9. Mandrake mob — wrong biome spawn category

**Symptom:** NeoForge warns: `mandrake_mob registered as CREATURE but added under MONSTER in witchery:torment biome`.

**Cause:** `WitcheryEntities.MANDRAKE_MOB` uses `MobCategory.CREATURE` but `torment.json` lists it under `"monster"`.

**Fix (pick one):**
- Change entity registration to `MobCategory.MONSTER` (more appropriate for a hostile cave creature).
- Or move it to `"creature"` in `torment.json`.

**Files:** `init/WitcheryEntities.java` or `data/witchery/worldgen/biome/torment.json`

---

## Priority order

| Priority | Bug | Effort |
|---|---|---|
| 🔴 Quick | Remove banshee/poltergeist mouth (2+8) | 2 min |
| 🔴 Quick | Cat body rotation fix (4) | 1 min |
| 🔴 Quick | Mandrake mob category (9) | 1 min |
| 🔴 Quick | Spawn placements (8) | 10 min |
| 🟡 Medium | Demon UV/texture audit (5) | 20 min |
| 🟡 Medium | Werewolf Pre-render fix (6) | 20 min |
| 🟡 Medium | Torment effects decision (1) | 5 min |
| 🟢 Feature | Dream World content (7) | 2–4 hrs |
