# Next Tasks — Witchery NeoForge Port

Last reconciled: 2026-05-17 (post Torment + Spirit World spec pass)
Full feature status: `docs/status/FEATURE_MATRIX.md`

---

## Priority order for next session

### 0. Item Review — remaining Blocks (ongoing)

Items from `docs/status/ITEM_REVIEW.md` still needing work:

| Item | Issue | Effort |
|---|---|---|
| `altar` | Should be 2×3 multiblock; item graphic wrong (just a square); top should be red | Large |
| `distillery` | Wrong/faulty GUI | Medium |
| `spinning_wheel` | GUI wrong | Medium |
| `witches_oven` | GUI wrong; faces left when placed | Medium |
| `crystal_ball` | Must be transparent (currently solid pink-black or wrong) | Small |
| `coffin` | Shows only one side in inventory | Medium |
| `alluring_skull` | Dedicated animated skull-head render still approximated through static block models | Low |
| `demon_heart` | Wrong 3D model / UV wrap | Medium |
| `dream_catcher` | Places on wrong surface (left side instead of face clicked) | Small |
| `kettle` | Missing 3D rope/string model detail | Small |
| `poppet_shelf` | Poppets don't visually appear on top of shelf | Medium |
| `mandrake_root` | Mandrake mob doesn't spawn from block? | Small |
| `grassper` | Wrong texture wrapping / should hold items | Medium |
| `glowglobe` | "Invisible" — may be alpha issue | Small |
| `rowan_door` | Facing issue when placed? Not tested | Small |
| `leaping_lily` | Needs real texture (currently snowbell placeholder) | Small |
| Items 62–153 | Tools & Items, Armour, Brews, Infusion, Spawn Eggs — untested | Varies |

---

### 1. Spirit World — remaining spec gaps (medium, ~2 hr)

Items from spec not yet implemented:

| Gap | Spec ref | Status | Notes |
|---|---|---|---|
| **Health/hunger save-restore** | §8 | ✅ Done | Saved in `SpiritWorldState` sub-codec; restored on exit |
| **Demonic Nightmare events** | §13 | ✅ Done | Every 100t: 50% SmallFireball, 65% LargeFireball, 75% demon spawn (if <10 hostiles, <2 demons) |
| **Corpse entity** | §9 | ✅ Done | `witchery:corpse` (MISC mob, sleeping pose, NoopRenderer placeholder) spawns on SW entry, removed on exit, killing it forces player back; UUID tracked in `SpiritWorldState` |
| **Flowing Spirit entity effects** | §21 | Partial | Regen/weakness, disturbed-cotton cleansing, and Nightmare defense suppression are back; the old anti-armor charm interaction tied to Nightmare attacks is still missing because `Charm of Disrupted Dreams` is not restored as an item yet |
| **Spirit Portal activation** | §22 | ✅ Done | Snow-frame portal now opens again from a Flowing Spirit source in the Spirit World and is used by the restored manifestation loop |
| **Rite of Manifestation ghost** | §23 | Partial | The rite now grants 2.5 min + 25s per coven witch of manifestation time again, Spirit Portal can spend that time to manifest the player into the Overworld, timed return and Icy Needle recall are back, but ghost rendering/style polish is still open |

---

### 2. Mirror World dimension (large, ~4–6 hr)

Original: `hasNoSky=true`, permanent dark, dark blue fog `(0, 0.03, 0.1)`, no rain/lightning.
Reflected overworld terrain with hostile player-clone inhabitants.
- **Dimension type:** `effects: the_end`, `fixed_time: 18000`, `has_skylight: false`, `ambient_light: 0.1`
- **Biome:** fog ~`RGB(0, 8, 26)`, sky very dark
- **Entry:** Mirror block ritual

---

### 3. Custom Brew system (very large, separate sprint)

5-tier capacity/dispersal; unlocks high-level brew effects.
**Approach:** capacity + dispersal tiers first (liquid/splash/gas), skip modifier/power scaling v1.

---

## Quick polish / verify items

- **Demon model** — `/witchery spawn demon` and verify horns/wings look correct after model fix
- **Nightmare behavior** — `/witchery dream`, wait for nightmare roll; verify: single nightmare spawns, targets only victim, green fog in nightmare, re-spawns after kill with 30s delay, drops Mellifluous Hunger
- **Torment maze** — delete `run/client/saves/<world>/dimensions/witchery/torment/` folder, re-enter; verify: 6 levels, Force walls, TormentStone floor, Torment Portal at far corner, Refilling Chests

---

## Completed this session ✅

### Item Review — Blocks pass (2026-05-18)

**Creative tab / registration**
- `mutandis` and `mutandis_extremis` added to creative tab (were registered but missing from tab)
- `witch_door` converted from plain `Block` to proper `DoorBlock(BlockSetType.OAK)` + `DoubleHighBlockItem`

**Rowan tree family**
- `rowan_log` changed to `RotatedPillarBlock` — now places in all 3 axes; added `rowan_log_horizontal.json` model and axis=x/y/z blockstate variants
- `rowan_leaves` changed to `RowanLeavesBlock extends UntintedParticleLeavesBlock` — proper transparency, leaf decay, falling particle (PALE_OAK_LEAVES placeholder)
- `rowan_planks` and `rowan_log/wood` added to `#minecraft:planks` and `#minecraft:logs` tags so vanilla plank recipes work
- `rowan_door` block models fixed from oak textures → `door_rowan_lower/upper` textures; item model fixed from oak_door → `ingredient.door_rowan`

**Ice blocks**
- `ice_door` BlockSetType changed from `IRON` → `OAK` (now opens on right-click); item model changed from oak_door → packed_ice texture
- `ice_fence` added to `#minecraft:fences` tag (fence connection logic)

**Plant tweaks**
- `glint_weed` now emits original full light level 15 and has its legacy flame ambience/spread behavior restored
- `leaping_lily` model no longer reuses glint_weed texture (now uses snowbell_stage_4 as placeholder)

**Directional BESR blocks** — all four now face the direction placed:
- `fetish_scarecrow`, `fetish_trent_effigy`, `fetish_witchs_ladder`: `FetishBlock` gains `FACING` property + `getStateForPlacement`; `FetishBlockEntity` stores facing in NBT; `FetishRenderer` uses custom `State` with `extractRenderState` to pass facing to `submit`
- `statue_goddess`: same pattern; reads facing from block state in `extractRenderState`

**Witch door models** — 8 new `witch_door_*.json` block models created from rowan door template; full blockstate with all facing/hinge/half/open variants

---

### Torment dimension (full spec implementation)
- Dimension type: `has_ceiling: false`, `has_skylight: true`, `fixed_time: 6000`, `ambient_light: 0.15`, red sky (RGB 200,80,80 fog 160,50,50), no mob spawns
- **TormentChunkGenerator** rewritten: air everywhere by default; 6 DFS maze levels (Y=10,25,40,55,70,85); 31×31 recursive-backtracking perfect maze per level; per-world seed via `WorldGenRegion.getSeed()`
- **Maze blocks:** Force walls/ceiling (6 tall), TormentStone floor, 1% mycelium, Torment Portal at cell (30,30), 3 Refilling Chests per level at cells (15,15), (5,15), (25,15)
- **TormentPortalBlock** — `entityInside`: outside→enter Torment (random level), inside→95% return/5% more; 100-tick cooldown
- **TormentHelper** — `sendToTorment(player, level)`: entry at (8, entryY, 8), 3×3 clearing, save return pos; `returnFromTorment`: safe-block search, restore return pos
- **RefillingChestBlockEntity + RefillingChestBlock** — 27-slot chest; fills from `witchery:chests/torment` loot table on first placement; refills every 72000t (3600s) when empty; loot includes spectral dust, demon heart, infernal animus, torment scroll, gold, etc.
- **Hellgate rite** updated to use `TormentHelper.sendToTorment` (was scanning for safe Y at 0,0)
- **Force** block: solid, unbreakable (`strength(-1,3_600_000)`), non-suffocating/non-view-blocking, and old light opacity 0 behavior; **TormentStone**: same unbreakable profile

### Spirit World (spec §1-19 partial)
- **Block-scan nightmare chance** (§7): Dream Weaver −50%, Flowing Spirit −10%×3, Wispy Cotton −10%×2, Demon Heart +35%×2, Fire +10%×3; modifiers only apply if Dream Weaver found
- **Demonic Nightmare state** (§13 partial): calculated on entry, tracked as transient `demonicNightmare`; entry message distinct
- **Transferable items on exit** (§8): Wispy Cotton, Disturbed Cotton, Mellifluous Hunger, Brew of Flowing Spirit, Subdued Spirit + Icy Needle transfer from Spirit inventory to Overworld on exit
- **Nightmare victim UUID** (§14): `setVictim()` assigns UUID; `NearestAttackableTargetGoal` uses victim predicate; despawns if victim gone/dead/>32 blocks
- **Defense timer spec-correct** (§16): 80 ticks in Spirit World / 40 outside (was always 40)
- **Flowing Spirit disables defense** (§16): `standingInFlowingSpirit()` check
- **Kill cooldown** (§12): `lastNightmareKillGameTime` recorded; 30s (600t) delay before respawn
- **Nightmare periodic respawn** (§12): `tickNightmareRespawn()` called every 100t for nightmare players; checks victim has no nearby nightmare, respects kill cooldown
- **Dynamic fog** (§5): `ViewportEvent.ComputeFogColor` in `WitcheryClientEvents`; Dream=pink, Nightmare=green, Demonic=red
- **Nightmare loot formula** (§18): `qty = random.nextInt(max(10-looting,5))==0 ? 2 : 1`

---

## Deferred (do not implement unless asked)

- Village additions (bookshop, guards, walls)
- Werewolf player model full replacement
- Flowing Spirit as proper fluid (currently plain block)
- Manifestation ghost rendering / style polish
