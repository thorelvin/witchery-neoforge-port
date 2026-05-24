# Session Changelog

Older completed-session entries extracted from `docs/status/NEXT_TASKS.md` on 2026-05-24.
Current session history lives in NEXT_TASKS.md. Newest entries at the top.

---

### Observations book access (2026-05-24)

- `immortals_observations_book` and `torn_page` are now real item registrations instead of legacy placeholders.
- Restored the Resurrected shaped recipe for Observations of an Immortal from Nether Wart, Nether Star, Book, and Garlic.
- Added a source-derived Bookshop chest loot table seed that provides Books, Torn Pages, and the Observations book for the future Bookshop structure path.
- The custom Torn Page update recipe, readable book UI/page state, and Bookshop structure placement remain follow-up work.

---

### Glass Goblet induction bridge (2026-05-24)

- `glass_goblet`, `full_glass_goblet`, and `glass_goblet_full` are now real items instead of legacy placeholders.
- Restored the Resurrected Glass Goblet recipe from three Glass Bottles and one Glass.
- Vampire players can spend 125 blood with an empty goblet to create a named Full Glass Goblet.
- Non-vampire players can drink that filled goblet to become vampires with the current port's practical starting blood.
- The old chicken-over-skull Elle/Lilith ritual, level-9 master gate, villager conversion, and Blood Power semantics remain follow-up work.

---

### Special Bolt crafting (2026-05-24)

- `bone_bolt`, `nullifying_bolt`, and `splitting_bolt` are now real items instead of legacy placeholders.
- Restored the Resurrected 12-output Bone Bolt recipe from Wooden Bolts, Bone, and Ghast Tear.
- Restored the Nullifying Bolt recipe from Null Catalyst, Belladonna, and Bone Bolts, using the port's live `belladonna` item.
- Restored the Splitting Bolt recipe from Wooden Bolts, String, and Feather.
- Hand Bow/Crossbow Pistol loading and all special projectile behavior remain follow-up work.

---

### Wooden/Silver Bolt crafting (2026-05-24)

- `wooden_bolt` is now a real item instead of a legacy placeholder.
- Restored the old/Resurrected 9-output Wooden Bolt recipe from String, Sticks, and Feathers.
- Restored the Resurrected 6-output Silver Bolt recipe from Wooden Bolts and Silver Ingot.
- Hand Bow/Crossbow Pistol loading and Silver Bolt projectile behavior remain follow-up work.

---

### Wooden Stake counterplay (2026-05-24)

- `stake` is now a real item instead of a legacy placeholder.
- Restored the Resurrected recipe shape: one Stick surrounded by Garlic.
- Right-clicking a sleeping vampire player with a Stake consumes one Stake and applies lethal staking damage.
- Bark Belt wooden-damage bypass now checks the real Stake item.

---

### Silver counterplay tag pass (2026-05-24)

- Silver damage now recognizes held `silver_sword` as well as `silver_ingot`.
- Silver damage now reaches non-player entity types tagged `witchery:vampires`, matching the same vampire-family tag used by Garlic Garland and Sun Grenades.
- Witch Hunters now consider entity types tagged `witchery:vampires` valid supernatural targets.

---

### Demonic Contract promotion (2026-05-24)

- **5 Flame Imp contract subtypes** promoted from LEGACY_PLACEHOLDER_ITEMS to real `DemonicContractItem` registrations: `contract_blaze` (Living Flame), `contract_resist_fire` (Fiery Tolerance), `contract_evaporate` (Evaporation), `contract_fiery_touch` (Fiery Touch), `contract_smelting` (Melting Touch).
- **2 legacy alias names** removed from LEGACY_PLACEHOLDER_ITEMS without new registrations: `contract_ownership` (maps to existing `demonic_contract`) and `contract_torment` (maps to existing `torment_scroll`).
- **5 shapeless recipe JSONs** created: `contract_blaze.json` (demonic_contract + blaze_rod + hint_of_rebirth), `contract_resist_fire.json` (demonic_contract + blaze_powder), `contract_evaporate.json` (demonic_contract + blaze_rod + magma_cream), `contract_fiery_touch.json` (demonic_contract + blaze_rod + ember_moss), `contract_smelting.json` (demonic_contract + lava_bucket).
- **Lang names** updated: `(Placeholder)` suffix removed. `contract_ownership`/`contract_torment` lang keys removed.
- Placeholder count: 214 → 207.

---

### Armor promotion + mechanics slice (2026-05-24)

- **Hunter armor** promoted to wearable LEATHER-tier armor (HELMET/CHESTPLATE/LEGGINGS/BOOTS). Recipes (all Impregnated Leather) added. Fixes `isWearingWitchHunterArmor` in RitualExecutor.
- **Witch outfit** promoted: `witches_hat`, `witches_robe`, `witches_shoes` are now wearable LEATHER-tier. Recipes added.
- **Deaths outfit** promoted: `deaths_hood`, `deaths_robe`, `deaths_shoes` are now wearable DIAMOND-tier. Deaths Hood tick effect: raytraces entities up to 16 blocks, applies Slowness III for 3 seconds.
- **Diviner items** implemented: `DivinerItem` class (hold-to-use, scans downward). `water_diviner` and `lava_diviner` promoted. Recipes added.
- **Wearable outfit promotion** (5 items): `babas_hat`, `necromancer_robes`, `ice_slippers`, `seeping_shoes`, `ruby_slippers`.
- **Ruby Slippers mechanic**: player types "there's no place like home" in chat → teleports to respawn position. 30-minute cooldown.
- Placeholder count: 221 → 214.

---

### Legacy placeholder item registry sweep (2026-05-22)

- Added `WitcheryItems.LEGACY_PLACEHOLDER_ITEMS` with 123 inert source/wiki item IDs.
- Follow-up exact-registry pass added 114 additional 1.12 Resurrected item IDs as inert placeholders. `javap` audit reports zero exact item-init registry strings missing.
- Decompiled asset pass restored distinct item models/textures for 107 more placeholders.
- Wiki/existing-icon pass restored 46 more confident placeholder icons/models.
- Exact wiki-icon pass restored 27 more placeholder icons/models.
- Final shared-placeholder model pass wired the last 18 placeholder item definitions away from `legacy_placeholder`. All 243 placeholders then had non-shared item models; 195 remain after promotions.
- Koboldite art correction restored user-approved live-backup icons.
- Glowglobe render: switched to translucent render type.
- Kettle ceiling-hung pass added `hanging` blockstate and compact hanging model.
- Grassper held-item polish: off-center petal-cup pose with gentle spin.
- Rowan Door: removed forced right-hinge override; vanilla hinge placement restored.
- Death scythe: pivot corrected from swapped `x=0, z=-0.8` to source `x=-0.8, z=0`.
- Demon wing: restored zero-depth source planes, removed non-source idle z-flap.
- Treefyd: restored old 1.7 model/texture, zero-width/depth petal, tongue, and crossed leaf planes.
- Poltergeist: all-part `mirror=true` UV setup.
- Banshee: removed non-source chin plate; flat zero-depth mouth plane; screaming state sync.
- Mandrake: all-part mirror UVs, zero-depth leaf crown, arm-only twitch.
- Nightmare: 15-tick melee attack timer sync; triangle-wave arm lift; mirror UV tendril sway.
- Ent: mirror UVs, conditional face plate, inner leaf fill, root-leg walking, attack arm lift, axe vulnerability, knock-up throw.
- Spirit: translucent white alpha, zero shadow radius, source translate/half-scale.
- Flying Monkey: dedicated ModelMonkey-style body, wing/arm/leg/tail animation, feather tint.
- Flame Imp: right-arm punch/body twist; powered 1.5x vertical scale.
- Parasytic Louse: potion-bite + hotbar-defense parity slices.
- Biting Belt: promoted with recipe, leg-slot wear, potion-defense fallback.
- Bark Belt: promoted with recipe, grass/mycelium recharge, non-wooden damage shield.
- Beartrap: promoted as real block, arming delay, snap behavior, Primer coverage.
- Wolftrap: promoted as silvered Beartrap-family block, werewolf-only snap.
- Wicker Man: Primer layout coverage (exact seven-layer 20-block shape).
- Vampire clothing: full family promoted with wearable armor, recipes, set-bonus hooks (drink-boost, mesmerise, lethal-fire), exact alias family, tooltips, Blood-stained Wool acquisition path.

---

### Garlic and garlic garland promotion (2026-05-24)

- `garlic` and `garlic_garland` promoted from placeholder to real block items. `GarlicCropBlock` (6 age stages), `GarlicGarlandBlock` with facing-aware click-through placement, source-style vampire repulsion, and non-creative vampire break fire/cancel.
- Sun Grenade follow-up: throwable grenades now hit non-player entity types tagged `witchery:vampires`, return a Quartz Sphere when sunlight releases, and use a one Charged Quartz Sphere -> one Sun Grenade recipe to preserve the source-style sphere loop.
- Sun Collector follow-up: restored the Resurrected source recipe shape (gold ingots, repeater, daylight detector, iron ingots), source compact footprint, and original `3.5F` hardness.
- `wolfsbane_seeds` promoted: `WolfsbaneCropBlock` (8 age stages), drops `wolfsbane_flower` at max age. Added to grass-seed drop pool.
- Poppet and Taglock Kit crafting recipes added: `poppet.json`, `taglock_kit.json`, and all 8 poppet upgrade recipes.
- Placeholder count: 207 → 204.

---

### Witch Hunter black-magic pressure (2026-05-22)

- Successful taglock curses, `Curse of Corrupt Poppet`, `Hell on Earth`, Voodoo Poppet use, and Vampiric Poppet redirection now call `WitchHunterEntity.blackMagicPerformed`.
- 10% notice chance, 2-minute delay, 100-tick cadence, 1% spawn roll, up to two hunters spawned near caster. Session-state pressure (not yet persistent NBT).

---

### Witch Hunter target parity (2026-05-22)

- `WitchHunterEntity` restores broader old anti-supernatural target set: undead mobs, vanilla demonic mobs, `DemonEntity`, vanilla witches, vampires/werewolves, and players carrying Voodoo/Vampiric Poppets.

---

### Broom ridden movement sync (2026-05-22)

- `BroomEntity` avoids ridden-flight client/server tug-of-war. Controlling client predicts old Witchery forward/pitch flight model; server accepts controlled-vehicle movement packet. Client control path reads synced Soaring/Owl state.

---

### Crystal ball, coffin item, leech chest (2026-05-22)

- Crystal Ball: replaced yellow placeholder with purple stained-glass orb on polished-blackstone stand.
- Coffin item: replaced flat sprite with 3D `block/block` box using `coffin_head.png` UV regions.
- Leech Chest: open animation, `blockEvent` sync, auto-close after 200 ticks, lid animation, `LeechChestModel.setLidAngle`.

---

### Renderer fixes (2026-05-22)

- `WitchesOvenRenderer` — `state.yaw` changed from `facing.getOpposite().toYRot()` to `facing.toYRot()`.
- `PoppetShelfRenderer` — item y changed from `0.60f` to `-0.15f` (ZP-180 flip means negative local y = above shelf).

---

### Item Review — Blocks pass (2026-05-18)

- `mutandis` and `mutandis_extremis` added to creative tab.
- `witch_door` converted to proper `DoorBlock(BlockSetType.OAK)` + `DoubleHighBlockItem`.
- Rowan tree family: `rowan_log` → `RotatedPillarBlock`; `rowan_leaves` → `UntintedParticleLeavesBlock`; added to `#minecraft:planks` and `#minecraft:logs` tags.
- Ice blocks: `ice_door` BlockSetType fixed to OAK; `ice_fence` added to `#minecraft:fences`.
- `glint_weed` restored to light level 15 with flame ambience/spread.
- `leaping_lily` model fixed (no longer reuses glint_weed texture).
- Directional BESR blocks: `fetish_scarecrow`, `fetish_trent_effigy`, `fetish_witchs_ladder`, `statue_goddess` all gain `FACING` property + `getStateForPlacement`.
- Witch door: 8 new block models + full blockstate with all facing/hinge/half/open variants.

---

### Torment dimension (full spec implementation)

- Dimension type fixed: `has_ceiling: false`, fixed_time 6000, ambient_light 0.15, red sky, no mob spawns.
- `TormentChunkGenerator` rewritten: 6 DFS maze levels (Y=10-85), old `GenerateMaze.display` footprint at origin x=-23/z=6, 31×31 recursive-backtracking perfect maze, per-world seed.
- Maze blocks: invisible Force walls/ceiling (6 tall), Bedrock floor/portal frame, 1% mycelium patches, old portal chamber near x=8/z=68, 3 Refilling Chest rooms per level.
- `TormentPortalBlock`, `TormentHelper`, `RefillingChestBlockEntity + RefillingChestBlock` all implemented.
- Hellgate rite updated to use `TormentHelper.sendToTorment`.
- Force block: invisible, solid, unbreakable, non-suffocating/view-blocking, old light opacity 0.

---

### Spirit World (spec §1-19 partial)

- Block-scan nightmare chance (§7): Dream Weaver/Flowing Spirit/Cotton/Demon Heart/Fire modifiers.
- Demonic Nightmare state (§13 partial): calculated on entry, tracked as transient.
- Transferable items on exit (§8): Wispy Cotton, Disturbed Cotton, Mellifluous Hunger, Brew of Flowing Spirit, Subdued Spirit, Icy Needle.
- Nightmare victim UUID (§14): `setVictim()` assigns UUID; despawns if victim gone/dead/>32 blocks.
- Defense timer spec-correct (§16): 80 ticks in Spirit World / 40 outside.
- Flowing Spirit disables defense (§16).
- Kill cooldown (§12): 30s delay before nightmare respawn.
- Nightmare periodic respawn (§12): 100-tick check.
- Dynamic fog (§5): Dream=pink, Nightmare=green, Demonic=red.
- Nightmare loot formula (§18): `qty = random.nextInt(max(10-looting,5))==0 ? 2 : 1`.
