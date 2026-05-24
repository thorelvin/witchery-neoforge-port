# Porting Notes — Chronological API Gotchas

This is intentionally chronological. Do not load the whole file unless needed; search it for the API/entity/system you are working on.

Current target:
- Minecraft: 1.21.11
- NeoForge: 21.11.42
- Java: 21

---

## Current target
- Minecraft: 1.21.11
- NeoForge: 21.11.42
- Java: 21

## Completed milestones

### 2026-05-24 parity-scan correction

Some early milestones below are historical implementation slices, not current full-parity claims. In particular, M22 Vampirism should be read as the current boolean/blood/sun/bite foundation; the old Creatures of the Night Glass Goblet -> Elle -> Lilith chain, 10 vampire levels, Sun Collector/Grenade progression, Blood Crucible/Blood Power, Resist Sun, Zombie Respect, and old cure/quest-book loops remain backlog. M14/M15/M11 machine notes are likewise historical foundations; Spinning Wheel, Distillery, and Brazier recipe/automation exactness now have first restored passes but still need hopper QA, while Kettle/Oven sided automation, Witches' Oven allowlist/byproducts/funnels, and Silver Vat smelt-byproduct storage remain open parity work. Crystal Ball now has source-style `LastUsedTime` recharge with the old strict `elapsed > 100` gate plus nearby-player subject selection for ordinary predictions, and bound Waystone same-dimension/150-block scrying with the old power-only item-use path and 1200-tick held-use cadence, but the actual old PredictionManager outcome system, remote camera packet/view, and Taglock scrying behavior remain backlog. The merged action list now lives in `docs/status/NEXT_TASKS.md`.

- [x] **M1** — NeoForge project shell (mod loads, logger, registries skeleton)
- [x] **M2** — Registries and creative tab (all blocks/items registered, lang, textures)
- [x] **M2b** — All simple block JSON models and item models (flat icons)
- [x] **M2c** — BESR renderers for 12 complex blocks (Kettle, Oven, Distillery, Candelabra, Brazier, Chalice, Crystal Ball, Dream Catcher, Fume Funnel, Spinning Wheel, Poppet Shelf, Leech Chest)
- [x] **M2d** — Plant transparency (render_type cutout for cross/leaves blocks)
- [x] **M3** — Plants MVP (Belladonna, Mandrake, Artichoke — CropBlock with 5 stages, blockstates, models, loot tables)
- [x] **M4** — Ingredients (belladonna, mandrake_root, artichoke as plantable+ingredient items; wood_ash, rowan_berries, wormwood, exhale_of_horned_one, hint_of_rebirth)
- [x] **M5** — Kettle MVP (BlockEntity with tick, player interactions, item-throw-in, brew extraction)
- [x] **M6** — Custom kettle recipe type (RecipeType + RecipeSerializer + 10 JSON recipe files)
- [x] **M7** — First brew effects: sleeping (blindness+slow), frost (freeze water+slow), love (animal breeding), revealing (night vision+glowing)
- [x] **M17** — Tier 1 Ritual Rites (LARGE 7x7 ring added; ingredient-based dispatch — dropped ItemEntity at center determines rite; 7 new rites: TotalEclipse/coal, BeastialCall/wheat, Fertility/bone_meal, Charging/redstone, TheForest/sapling, Broiling/raw_food, Protection/default-large)
- [x] **M16** — Altar power system (AltarBlock+AltarBlockEntity scan 5-block radius every 100 ticks; power 0-1000 from witch wood/leaves/candelabra/brazier/chalice/statue/skull/demon heart; KettleBlockEntity.tick() replaced isPowered placeholder with findNearbyAltar scan + consumePower; right-click altar shows power level)
- [x] **M15** — Distillery (DistilleryRecipe with 2 ItemStack inputs; fuel-based; DistilleryMenu+Screen; REDSTONE_SOUP/DIAMOND_VAPOUR/BAT_WOOL items; 4 recipes)
- [x] **M14** — Spinning Wheel (SpinningRecipe extends AbstractCookingRecipe; no fuel — 100-tick auto-tick; SpinningWheelMenu + SpinningWheelScreen; FANCIFUL_THREAD item; recipes: cotton→2 thread, rowan_berries→1 thread; crafting: 4 thread→white_wool placeholder)
- [x] **M13** — Cotton/Wormwood/Snowbell crop batch (cotton AGE_3 with somnian+disturbed textures; wormwood+snowbell AGE_4 with own textures; the old Witch Crop / Rowan Seeds shortcut from this milestone was later removed again after decompiled/source comparison showed it was a false port-only path)
- [x] **M12** — All 13 brew effects implemented (Raising: spawns zombies; Wasting: AoE Wither+Slow; Erosion: degrades stone/dirt; Foulness: AoE nausea+poison; Infection: AoE poison spread; Grotesque: Strength IV+Resistance+Slow; FlowingSpirit: Invisibility+SlowFall+NightVision; Foaming: places cobwebs; Poison: Poison III on drinker)
- [x] **M11** — Witches Oven (OvenRecipe extends AbstractCookingRecipe + SimpleCookingSerializer; WitchesOvenBlockEntity with furnace-style tick; WitchesOvenMenu + WitchesOvenScreen; 3 recipes: logs→ash, nether_wart→exhale_of_horned_one, quartz→lime_dye)
- [x] **Broom** — BroomEntity extends Mob (not Entity — MobRenderer needs LivingEntity; Entity-based vehicles need custom EntityRenderer); mobInteract() not interact() for Mob subclasses; empty readAdditionalSaveData/addAdditionalSaveData cause override errors when base class uses HolderLookup.Provider variant — just omit them; flight: setNoGravity(true)+manual deltaMovement in tick() using rider.zza and getLookAngle(); positionRider() places passenger on handle; BroomItem.use() spawns entity + startRiding(); /witchery spawn broom works
- [x] **M29** — Polish pass (K1 sounds: Banshee→PHANTOM, Spectre→VEX, Mandrake→SILVERFISH; K5 config: WitcheryConfig.java with ModConfigSpec registered via modContainer.registerConfig — BoolValue→ConfigValue<Boolean> in NeoForge 1.21.11; 13 crafting recipes: bone_needle/clay_jar_soft/quicklime/frozen_heart/graveyard_dust/attuned_stone/null_catalyst/anointing_paste/wormy_apple/wicker_bundle + 3 shaped (mutandis/null_catalyst_initial/quartz_sphere); 2 new items: anointing_paste/wormy_apple)
- [x] **M27** — Horror + Summoned Entities (Banshee/Spectre/Poltergeist: Monster+FlyingMoveControl+FlyingPathNavigation — FlyingMob base class rejects PathfinderMob goals; Treefyd/Ent: ground Monsters, passive Ent retaliates only; Mandrake: fast tiny mob with poison on hit; Demon: Monster+RangedAttackMob throws SmallFireball(level,entity,vec3.scale); GhostModel shared by 3 flying types with LAYER_BANSHEE/LAYER_SPECTRE/LAYER_POLTERGEIST; RangedBowAttackGoal needs explicit generic type <EntityClass> to avoid inference error; SmallFireball constructor is (Level, LivingEntity, Vec3) not (Level, LivingEntity, double, double, double))
- [x] **M24** — Witch NPCs (CovenWitchEntity: AbstractVillager+Mob.createMobAttributes, 5 random skins, 10 trades for witchery ingredients, WitchModel renderer; WitchHunterEntity: Monster+RangedAttackMob, targets vampire/werewolf players+Witch via NearestAttackableTargetGoal, bow ranged attack+melee, 3 random skins; WitchHunterModel: biped+hat brim+hat top+coat; /witchery spawn command; API notes: ItemCost in trading package not world.item, AbstractVillager needs rewardTradeXp override, SpawnGroupData needs explicit import)
- [x] **M23** — Lycanthropy + 3D werewolf render layer (WerewolfModel: HierarchicalModel from ModelWerewolf 64×64, body hunched PI/9, digitigrade legs, ears/snout/mane/tail; WerewolfLayer: RenderLayer on PlayerRenderer via EntityRenderersEvent.AddLayers; only renders when isWerewolf+fullMoon+isNight; texture=wolfman.png copied from resurrected werewolf.png; API notes: AddLayers.getSkins() returns Set<PlayerSkin.Model> not Set<String> in 1.21.11; renderToBuffer(PoseStack, VertexConsumer, light, overlay) order — PoseStack first) (WerewolfEvents.java: full moon=getMoonPhase()==0; wolf form on full-moon night: Speed III+Strength II+Jump II+Hunger II+NightVision refreshed every 100t; transformationLevel set to 1f on first activation; Curse of the Wolf ritual=LARGE+BONE/BONE_BLOCK; TODO: silver weakness, wolfsbane debuff, howling)
- [x] **M22** — Vampirism (VampireEvents.java: tick drain 1/100t, igniteForTicks(40) sunburn in daylight+sky+no helmet, blood thresholds 0-50 frenzy/51-150 weak/151-349 night vision/350-500 speed+strength; EntityInteract bite=empty hand+non-player LivingEntity→2 damage+bloodValue; Creatures of the Night ritual=MEDIUM+DEMON_HEART→setVampire+250 blood; API note: setSecondsOnFire→igniteForTicks in 1.21.11)
- [x] **M21** — Environment & Transport Rituals (7 new rites; SMALL: Transposition=ender_pearl→respawn point, PriorIncarnation=rotten_flesh→recall items from last death within 16b; MEDIUM: SkysWrath=gunpowder→12 real lightning strikes, Sanctity=gold_ingot→repel mobs 25b + Resistance II, Imprisonment=obsidian→Slowness V+Weakness on mobs in 10b; LARGE: MovingEarth=dirt/gravel/sand→raise 5×5 column 5b, BrokenEarth=flint→dig 3-wide 5-deep trench in look direction; WitcheryPlayerData gains lastDeathPos+lastDeathDimension; PoppetEvents LOWEST-priority handler records death pos when not cancelled)
- [x] **M20** — Familiar Entities (Owl/Cat/Toad custom entities extending TamableAnimal; WitcheryEntities DeferredRegister; OwlModel+ToadModel ported from original 64x32/32x32 UV layouts; CatFamiliarModel now restores the old RenderOcelot-style body/leg/tail layout with Witchery blackcat texture and sit state; three MobRenderer subclasses; RiteBindFamiliar via feather/fish/slimeball in SMALL circle; Owl warns of nearby mobs, Toad collects dropped items, Cat avoids Phantoms/Creepers)
- [x] **M19** — Infusion Base System (InfusionType enum; WitcheryPlayerData + SyncPayload updated; 10 new items: 4 brew bases, mystic unguent, flying ointment, happenstance oil, infused brew base + 2 infused brews; 4 kettle recipes; Rite of Infusion dispatches per brew base; InfusionEvents passive tick effects; all item/block textures audited and filled from original assets)
- [x] **M18** — Taglock Kit + remaining poppets (TaglockKitItem collects UUID+name via right-click; ProtectionPoppetItem with 6 ProtectionTypes; VoodooPoppetItem; VampiricPoppetItem; PoppetEvents handles death prevention across all types + vampiric damage redirect)
- [x] **M10** — Poppet MVP (right-click player to bind UUID; tooltip shows bound player; death prevention consumes poppet + restores 2 hearts + totem particles)
- [x] **M8** — Ritual circle MVP (ChalkItem places CIRCLE blocks; arthana right-click triggers circle scan; two rites: Infusion 3×3 → Regen+Speed, Summoning 5×5 → Strength+NightVision+lightning)
- [x] **M9** — Player data foundation (AttachmentType with CODEC; fields: vampire, werewolf, bloodLevel, transformationLevel, familiarUUID; server→client sync via SyncPlayerDataPayload; persists through death/respawn)

## Notes

### 2026-05-18 — MC 1.21.11 / NeoForge 21.11.42 migration

Full compile-clean migration from 1.21.1 (NeoForge 21.1.229) → 1.21.11 (NeoForge 21.11.42). Every item below caused a real compile error.

**Entity combat**
- `LivingEntity.hurt(DamageSource, float)` is now `final` in Entity. Override point is `hurtServer(ServerLevel, DamageSource, float) → boolean`. Return `super.hurtServer(...)` to chain.
- `Entity.causeFallDamage(float, float, DamageSource)` → first param is now `double`.
- `Entity.doHurtTarget(Entity)` → `doHurtTarget(ServerLevel, Entity)`.

**Goal AI**
- `NearestAttackableTargetGoal` 4th-arg `Predicate<LivingEntity>` replaced by `TargetingConditions.Selector` (two-arg FI): `(e, sl) -> condition`. Method refs that only take one arg must become lambdas.

**Entity spawn**
- `EntityType.create(ServerLevel)` removed. Use `create(Level, EntitySpawnReason)` or the 6-arg overload with `Consumer<E>`.
- `TamableAnimal.setOwnerUUID(UUID)` removed. Use `setOwner(LivingEntity)`.

**Entity render states**
- `AvatarRenderState` has `public int id` but `HumanoidRenderState` does not. For non-player mob renderers that need entity ID (e.g. skin variation), create a custom `RenderState extends HumanoidRenderState` with `entityId` field and override `extractRenderState` to populate it.
- `EntityRenderersEvent.AddLayers.getSkins()` now returns `Set<PlayerModelType>`, not `Set<String>`. `.getSkin(String)` removed; use `.getPlayerRenderer(PlayerModelType)` (returns `AvatarRenderer<AbstractClientPlayer>`).

**Item tooltip**
- `Item.appendHoverText` signature changed completely:
  - Old: `(ItemStack, Item.TooltipContext, List<Component>, TooltipFlag)`
  - New: `(ItemStack, Item.TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)`
  - Change `List<Component> tooltip` param to `Consumer<Component> tooltip`.
  - Change `tooltip.add(x)` to `tooltip.accept(x)`.
  - Add `net.minecraft.world.item.component.TooltipDisplay display` as 3rd param.
  - Import `net.minecraft.world.item.Item` if using `Item.TooltipContext`.
  - `super.appendHoverText(...)` calls must include the `_display` arg.

**Item rendering (placed block entity)**
- `ItemModelResolver.updateForTopItem` signature:
  - Old: `(ItemStackRenderState, ItemStack, ItemDisplayContext, boolean, Entity, Entity, int)`
  - New: `(ItemStackRenderState, ItemStack, ItemDisplayContext, Level, ItemOwner, int)`
  - `ItemOwner` is an interface with `level()`, `position()`, `getVisualRotationYInDegrees()`. Implement inline with an anonymous class for BESRs.

**Tools**
- `PickaxeItem` class removed. Use `ToolMaterial.IRON.applyToolProperties(new Item.Properties(), BlockTags.INCORRECT_FOR_IRON_TOOL, miningSpeed, attackDamage, attackSpeed)`.
- `ToolMaterial.applySwordProperties(Properties, attackDamage, attackSpeed)` for swords.

**NBT / CompoundTag**
- `CompoundTag.getList(String, byte)` → `getListOrEmpty(String)` (or `getList(String)` which returns `Optional<ListTag>`).
- `CompoundTag.getCompound(String)` → returns `Optional<CompoundTag>`. Use `getCompoundOrEmpty(String)` for non-null.
- `CompoundTag.getString(String)` → returns `Optional<String>`. Use `getStringOr(String, "")`.
- `CompoundTag.getInt(String)` → returns `Optional<Integer>`. Use `getIntOr(String, 0)`.
- `CompoundTag.getByte(String)` → returns `Optional<Byte>`. Use `.orElse((byte)0) & 0xFF`.
- All of the above apply to **both** string-literal keys and constant-variable keys. The regex-based fix script only patches string literals — find constant usages manually (e.g. `tag.getString(TAG_NAME)` → `tag.getStringOr(TAG_NAME, "")`).
- `TagParser.parseTag(String)` → `TagParser.parseCompoundFully(String)` (throws `CommandSyntaxException`).
- `ListTag.getString(int)` → `ListTag.getStringOr(int, "")`.
- `CompoundTag.getLong(String)` → returns `Optional<Long>`. Use `.orElse(0L)` or store as string. The `putLong/getLong` roundtrip requires `cd.getLong("key").orElse(0L)` on read.

**ValueInput / ValueOutput (block entity NBT)**
- `ValueOutput.putUUID` and `ValueInput.getUUID/hasUUID` **do not exist** in this API. Store UUIDs as strings: `tag.putString("key", uuid.toString())` / `UUID.fromString(tag.getStringOr("key", ""))`.
- `ValueOutput.put(String, CompoundTag/ListTag)` does not exist. Store lists as comma-separated strings, or use `childrenList(key)` / `childrenListOrEmpty(key)`.
- **No `getList` on ValueInput** — ValueInput is not CompoundTag.

**Block API**
- `ScheduledTickAccess` package: `net.minecraft.world.ticks.ScheduledTickAccess` → **`net.minecraft.world.level.ScheduledTickAccess`** (both import and FQ usage in method signatures).
- `ServerLevel.blockUpdated(BlockPos, Block)` removed. Use `Level.sendBlockUpdated(BlockPos, BlockState, BlockState, int)`.
- `ChunkAccess.setBlockState(BlockPos, BlockState, boolean)` → second boolean replaced by `int` flags. Use `0` instead of `false`.
- `BlockBehaviour.entityInside` extra params added: `(BlockState, Level, BlockPos, Entity, InsideBlockEffectApplier, boolean)`. `InsideBlockEffectApplier` is in `net.minecraft.world.entity` (not `world.level.block`).

**Level utilities**
- `Level.isDay()` / `isNight()` removed. Use `getDayTime() % 24000 < 12000` (day) or `>= 12000` (night).
- `Level.getMoonPhase()` removed. Use `(getDayTime() / 24000L) % 8L == 0` for full moon.
- `ServerLevel.getMoonBrightness()` → `getMoonBrightness(BlockPos)`. Or use the moon-phase calculation above.
- `LevelHeightAccessor.getMinBuildHeight()` → `getMinY()`. `getMaxBuildHeight()` → `getMaxY()`. Both old names are absent from bytecode entirely.

**Recipe system**
- `RecipeManager.getAllRecipesFor(RecipeType)` removed. Use `level.recipeAccess().recipeMap().byType(type)` returns `Collection<RecipeHolder<T>>`.
- `SingleItemRecipe.result()` is `protected`. Use `assemble(SingleRecipeInput, registryAccess)` to get the output `ItemStack`.
- `AbstractCookingRecipe` no longer takes `RecipeType` in its constructor.

**Spawn/sleep**
- `ServerPlayer.getRespawnPosition()` / `getRespawnDimension()` removed. Use `sp.getRespawnConfig()` which returns `ServerPlayer.RespawnConfig` (nullable). Pos is `respawnConfig.respawnData().pos()`, dimension is `.dimension()`.
- `ServerPlayer.RespawnPosAngle.of(Vec3, BlockPos)` → now requires a third `float yaw` arg.
- `Player.BedSleepingProblem.NOT_POSSIBLE_NOW` removed. `BedSleepingProblem` is now a record with only `TOO_FAR_AWAY`, `OBSTRUCTED`, `OTHER_PROBLEM`, `NOT_SAFE`.
- `BedSleepingProblem.getMessage()` → `.message()`.

**Permissions / commands**
- `CommandSourceStack.hasPermissions(int)` removed. Use `src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))` for op-level 2. Enum values: `ALL(0)`, `MODERATORS(1)`, `GAMEMASTERS(2)`, `ADMINS(3)`, `OWNERS(4)`.

**Networking**
- `PacketDistributor.sendToServer(payload)` removed from the common class. Client-to-server is now `net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload)`.

**Fuel values**
- `ItemStack.getBurnTime(RecipeType, FuelValues)` removed. Use `level.fuelValues().isFuel(stack)` or `level.fuelValues().burnDuration(stack)`.

**Mod loading**
- `FMLEnvironment.dist` (field) removed. Use `FMLEnvironment.getDist()` (method).

**Sound events**
- `SoundEvents.THUNDER_HIT` removed. Equivalent is `SoundEvents.LIGHTNING_BOLT_THUNDER`.

**ChunkGenerator**
- `ChunkGenerator.applyCarvers` no longer has a `GenerationStep` 7th parameter. Signature is now `(WorldGenRegion, long, RandomState, BiomeManager, StructureManager, ChunkAccess)`.
- `fillFromNoise(Blender, RandomState, StructureManager, ChunkAccess) → CompletableFuture<ChunkAccess>` is now abstract and must be implemented (return `CompletableFuture.completedFuture(chunk)` for flat/maze generators).
- `NoiseColumn` moved to `net.minecraft.world.level.NoiseColumn` (was `world.level.chunk.NoiseColumn`).

**SavedData (level-persistent data)**
- `SavedData.Factory<T>` is **gone**. The new pattern uses `SavedDataType<T extends SavedData>`:
  ```java
  public static final Codec<MyData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
      Codec.INT.fieldOf("key").forGetter(d -> d.field)
  ).apply(inst, MyData::fromCodec));
  public static final SavedDataType<MyData> TYPE = new SavedDataType<>(
      "save_id", MyData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
  // Retrieve: overworld.getDataStorage().computeIfAbsent(TYPE)
  ```
- `SavedData` only has `setDirty()` / `isDirty()`. No `save()`/`load()` — serialization is driven by the Codec in `SavedDataType`.

**Entity position / rotation**
- `Entity.moveTo(double, double, double, float, float)` does **not** exist in bytecode. The correct methods are:
  - `snapTo(double, double, double)` — position only
  - `snapTo(double, double, double, float, float)` — position + yaw + pitch  ← **use this**
  - `snapTo(BlockPos, float, float)`, `snapTo(Vec3)`, `snapTo(Vec3, float, float)`
- Do NOT call `moveTo(double,double,double,float,float)` or `moveTo(double,double,double)` — neither exists in the compiled class.

**Entity renderers (simple)**
- All `MobRenderer` subclasses must implement `createRenderState()`. If using `LivingEntityRenderState`, override: `public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }`.

### 2026-05-18 — Item review fixes (LeavesBlock, BESR facing, block registration)

**LeavesBlock is abstract in NeoForge 21.11.42**
- The decompiled source shows `LeavesBlock(BlockBehaviour.Properties)` as concrete, but NeoForge patches it to be abstract with two abstract methods:
  - `codec()` — must return `MapCodec<? extends LeavesBlock>`
  - `spawnFallingLeavesParticle(Level, BlockPos, RandomSource)` — particle spawn hook
- Also the constructor takes `(float leafParticleChance, BlockBehaviour.Properties)`, not just `Properties`.
- Use the concrete subclasses: `UntintedParticleLeavesBlock(float chance, ParticleOptions particle, Properties)` or `TintedParticleLeavesBlock`. For modded leaves, create a class extending `UntintedParticleLeavesBlock`:
  ```java
  public class RowanLeavesBlock extends UntintedParticleLeavesBlock {
      public RowanLeavesBlock(BlockBehaviour.Properties p) {
          super(0.02f, ParticleTypes.PALE_OAK_LEAVES, p);
      }
  }
  ```
- Do NOT try to override `codec()` with a tighter generic type — it will fail with incompatible return type.

**DirectionProperty removed — use EnumProperty<Direction>**
- `net.minecraft.world.level.block.state.properties.DirectionProperty` no longer exists.
- Replace with `EnumProperty<Direction>` from the same package.
- `BlockStateProperties.HORIZONTAL_FACING` is typed as `EnumProperty<Direction>`.

**BESR custom render state with facing**
- To pass block-entity data to a BESR's `submit()`, create a custom render state:
  ```java
  public static class State extends BlockEntityRenderState {
      public Direction facing = Direction.NORTH;
  }
  ```
- Override `createRenderState()` to return the custom type.
- Override `extractRenderState` (5-arg: `T be, S state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay`). Call the default with `BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, overlay)` — use the interface-default syntax, not `super.`, because the interface method is a default method, not a superclass method.
- For blocks where facing is stored in block state (not BE): read via `be.getBlockState().getValue(MyBlock.FACING)` inside `extractRenderState`.

**BESR Y-rotation for HORIZONTAL_FACING**
- `Direction.toYRot()` returns the yaw angle matching Minecraft's block-facing convention (SOUTH=0, WEST=90, NORTH=180, EAST=270).
- Use `Axis.YP.rotationDegrees(-facing.toYRot())` to make a model face the direction of the `FACING` property.

**Leaf block decay requires #minecraft:logs tag**
- `LeavesBlock` computes decay distance by checking `BlockTags.LOGS` on adjacent blocks. If your modded log block isn't in `#minecraft:logs`, leaves will always decay (distance=7). Add your logs to `data/minecraft/tags/block/logs.json`.

**Fence connection requires #minecraft:fences tag**
- Non-vanilla `FenceBlock` instances need to be added to `data/minecraft/tags/block/fences.json` so the connection logic recognises them.

**Plank tags for vanilla recipes**
- Modded planks are not automatically usable in vanilla recipes (crafting table, sticks, etc.). Add them to `data/minecraft/tags/block/planks.json` and `data/minecraft/tags/item/planks.json`.

**Block and item registration (CRITICAL)**
- `BlockBehaviour.Properties` requires `.setId(ResourceKey<Block>)` to be called before the Block constructor runs. In 1.21.11, `Block.<init>` calls `properties.effectiveDrops()` which asserts the ID is non-null.
  - `BLOCKS.register(name, () -> new MyBlock(BlockBehaviour.Properties.of()...))` **breaks** — ID isn't set in time.
  - Use `BLOCKS.registerBlock(name, props -> new MyBlock(props.mapColor(...).strength(...)), BlockBehaviour.Properties.of())` instead. `props` has the ID pre-set by NeoForge; chain builder methods on it.
  - For blocks with extra constructor args: `BLOCKS.registerBlock(name, props -> new DoorBlock(BlockSetType.OAK, props.strength(...)), BlockBehaviour.Properties.of())`.
  - For custom Properties helpers (e.g. cropProps()): `BLOCKS.registerBlock(name, MyBlock::new, () -> cropProps())` (Supplier form).
  - `BLOCKS.registerSimpleBlock` already handles this correctly and does not need changes.
- Same applies to items: `Item.Properties` requires `.setId(ResourceKey<Item>)` before `Item.<init>`.
  - `ITEMS.register(name, () -> new MyItem(new Item.Properties()...))` **breaks**.
  - Options:
    1. `ITEMS.registerItem(name, MyItem::new)` — factory gets empty Properties with ID pre-set.
    2. `ITEMS.registerItem(name, MyItem::new, new Item.Properties().stacksTo(1))` — factory gets base Properties (with chain) with ID pre-set.
    3. Helper: `ITEMS.register(name, id -> new MyItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).chain()))` — pass `id` directly from the `Function<Identifier, Item>` form.
  - `ITEMS.registerSimpleItem` and `ITEMS.registerSimpleBlockItem` already handle this correctly.
- **Item model format**: In 1.21.11, `ModelManager` looks for item models at `assets/<mod>/items/<name>.json` (new format), not the old `models/item/<name>.json` path by default. The old path may still serve as a fallback for some model types. Expect "No model loaded for default item model ID" WARNs for every item without a new-format JSON — non-fatal, items render as missing-texture model.

**Biome JSON**
- `"carvers": {}` (empty object) → `"carvers": []` (empty array). The carvers field is now a flat list in 1.21.11.
- `"temperature_modifier": "none"` field removed entirely in 1.21.11 biomes.

**Recipe JSON**
- Ingredient format completely changed in 1.21.11:
  - Item: `{"item": "namespace:id"}` → `"namespace:id"` (plain string)
  - Tag: `{"tag": "namespace:tag"}` → `"#namespace:tag"` (# prefix string)
  - In shaped `key` entries and shapeless/custom `ingredients` arrays
  - Smelting `ingredient` field: same change
- Result format: `{"item": "x", "count": N}` → `{"id": "x", "count": N}` (`item` → `id`)
- Custom recipe types (witchery:kettle_brewing, witchery:distillery, etc.) that return `PlacementInfo.NOT_PLACEABLE` will log `"can't be placed due to empty ingredients"` — this is expected and harmless.

**Structure worldgen**
- `BlockEntity.level` is null when accessed inside `StructurePiece.postProcess()` during worldgen. Call `blockEntity.setLevel(worldGenLevel.getLevel())` before using any methods that internally invoke `this.level.sendBlockUpdated(...)` (e.g. `SignBlockEntity.setText()`).
- `StructurePiece.getMirror()` returns null if `setOrientation(Direction)` was never called on the piece — `placeBlock()` will crash calling `BlockState.mirror(null)`. Always call `setOrientation(Direction.NORTH)` (or any valid direction) in custom `StructurePiece` constructors.
- `postProcess` receives a `WorldGenLevel`, NOT a `ServerLevel`. Never cast `level` to `ServerLevel` directly — it will `ClassCastException`. Use `level.getLevel()` to obtain the `ServerLevel` when needed (e.g. for `getCurrentDifficultyAt`, `addFreshEntityWithPassengers`).

**Loot tables**
- `minecraft:looting_enchant` function renamed to `minecraft:enchanted_count_increase` in 1.21.11. New format requires `"enchantment": "minecraft:looting"` field alongside the existing `count` field.

**Misc**
- `Player.sendSystemMessage(Component)` removed. Use `displayClientMessage(Component, false)`.
- `Level.canSeeSky(BlockPos)` still exists (via `BlockAndTintGetter`).
- `Inventory.items` (field) is now private. Iterate via `getContainerSize()` + `getItem(i)`.

### 2026-05-16 (M25b — Nightmare + Spirit entities)

**New entity files:**
- NightmareEntity.java: 100 HP, 0.35 speed, 4 dmg, MeleeAttackGoal; defenseTimer=40 ticks invincibility after each hit (damage capped 15/hit); doHurtTarget strips random armor slot (HEAD/CHEST/LEGS/FEET) from player; drops Mellifluous Hunger only in Spirit World (dropCustomDeathLoot override)
- SpiritEntity.java: extends Monster (needed for PathfinderMob/AvoidEntityGoal); 20 HP, flying (FlyingMoveControl + FlyingPathNavigation), passive — AvoidEntityGoal<Player> 8f radius; drops 1 Subdued Spirit always

**New model files:**
- NightmareModel.java: HierarchicalModel 64x64; head/body/left_arm/right_arm with 3 fingers each; 7 leg-tendrils (leg1-leg7) animated with sine waves keyed on entity ID modulus; attack animation swings arms via triangleWave()
- SpiritModel.java: HierarchicalModel 32x32; orb of 5 sub-boxes (shape1-5) as children of body pivot; gently bobs via sin(ageInTicks*0.05); rendered at 0.5x scale via SpiritRenderer.scale() override

**New renderer files:** NightmareRenderer (shadow 0.6f), SpiritRenderer (shadow 0.25f, scale 0.5x)

**Spawn eggs:** NIGHTMARE_SPAWN_EGG (0x1A0033/0x9900CC), SPIRIT_SPAWN_EGG (0xAADDFF/0xFFFFFF)
**Commands:** /witchery spawn nightmare, /witchery spawn spirit
**Spirit World biome:** Spirit added as creature spawner (weight 10, 1-3)
**API note:** AvoidEntityGoal requires explicit generic type <Player> and PathfinderMob — use extends Monster not Mob for passive flying entities that need goal AI


### 2026-05-16 (M25 — Spirit World entry/exit mechanics)

**New files:**
- `spiritworld/SpiritWorldHelper.java` — core entry/exit logic: saves inventory (keeps Icy Needle + Mutandis + Mutandis Extremis), 30/70 dream/nightmare roll, XZ-matched spawn in Spirit World, restores inventory on exit
- `item/AppleOfSleepingItem.java` — food item, on eat server-side calls SpiritWorldHelper.enter()
- `item/IcyNeedleItem.java` — right-click in Spirit World calls SpiritWorldHelper.exit()

**Modified:**
- `BrewOfSleepingItem` — calls SpiritWorldHelper.enter() instead of sleep effects
- `WitcheryPlayerData` — 3 new codec-persisted fields: inSpiritWorld, spiritWorldIsDream, savedInventorySnbt; all carried in copyFrom()
- `SyncPlayerDataPayload` — inSpiritWorld synced to client
- `WitcheryPlayerEvents` — LivingDeathEvent in Spirit World: cancel death, restore inventory, exit to Overworld at 1 HP
- `WitcheryCommands.findSafeEntry` — Spirit World uses overworld scan (Y 200→−60) not End-style
- `WitcheryItems` — registered APPLE_OF_SLEEPING (AppleOfSleepingItem), ICY_NEEDLE now IcyNeedleItem

**Resources:** apple_of_sleeping model + texture (ingredient.sleeping_apple.png), recipe (apple + belladonna), lang key

**NeoForge API note:** `TagParser.parseTag(String)` (net.minecraft.nbt) is available and returns Tag; cast to ListTag for inventory restore. `player.getInventory().save(ListTag)` + `.load(ListTag)` is the correct inventory serialization pattern.

**Up next (M25b):** NightmareEntity + SpiritEntity — these are the first Spirit World mobs. Nightmare: 100 HP, spawns during nightmare state (spiritWorldIsDream == false), teleports to player, strips armor. Spirit: 20 HP, passive/flees, drops Subdued Spirit. Both need entity class + model + renderer + registration.


### 2026-05-16 (Wiki corrections — M27 entity stats + dimension data)

**Entity HP corrections (FTB Wiki):**
- Demon: 60 → 100 HP (was placeholder; wiki: 100 HP, fires Fire Charges, fire-immune)
- Treefyd: 30 → 50 HP (wiki: 50 HP, blinds on attack, hostile to non-whitelist)
- Ent: 80 → 200 HP (wiki: 200 HP, flings player, spawns when Alder/Hawthorn/Rowan chopped)
- Mandrake: 10 → 20 HP (wiki: 20 HP, scream causes Nausea, spawns via ritual)
- Coven Witch: 20 → 30 HP (wiki: 30 HP, neutral, throws vanilla potions, spawns in Overworld)
- Banshee: already 20 HP ✓ (was 40 in a previous placeholder — corrected last session)

**Ritual-only mobs (do NOT naturally spawn):**
- Banshee: Summon Banshee ritual + Brazier (drops Spectral Dust)
- Spectre: Summon Spectre ritual + Brazier
- Demon: Rite of Summoning (drops Demon Heart, Gunpowder, Magma Cream)
- Mandrake mob: spawned from Mandrake plant pull
- Treefyd: conjured by witch player ability

**Spirit World dimension (confirmed by wiki):**
- Mirror of Overworld topography — using minecraft:overworld noise ✓
- Renamed dream_world → spirit_world; orphaned dream_world JSON files removed
- Entry: Apple of Sleeping, Brew of Sleeping, Spirit Portal (needs Rite of Manifestation)
- Exit: die, or throw Icy Needle
- Inventory stripped on entry (Icy Needle + Mutandis always transfer; Baubles transfer if mod installed)
- Circle Magic does not work in Spirit World
- Sources: Wispy Cotton, Disturbed Cotton, Mellifluous Hunger (from Nightmares)
- Wispy Cotton and Brew of Flowing Spirit can pass back to Overworld; other items cannot
- Dream state: Dream (perpetual day) or Nightmare (perpetual night, more common); Dream Weaver blocks affect chance

**Spirit World mobs (not yet implemented — deferred):**
- Nightmare: 100 HP, spawns during nightmares, teleports to player, respawns, strips armor, drops Mellifluous Hunger
- Spirit: 20 HP, passive/flees, flying, drops 1 Subdued Spirit

**Torment dimension (confirmed by wiki / old source):**
- Horizontally aligned mazes. Old 1.7.10 `GenerateMaze.display` uses invisible `Force` for walls/ceilings and vanilla Bedrock for floors/portal frames; Resurrected has a TormentStone naming/art path, but the baseline port should preserve old mechanics/layout unless intentionally deviating.
- No vanilla mobs (blaze/magma_cube removed from torment biome spawner)
- Remaining spawners: witchery:demon, witchery:mandrake_mob
- Entry: summon Lord of Torment via Torment Scroll (final gift from tamed Flame Imp), fight to 50% HP (250 HP), gets teleported in with player
- Exit: die → respawn in Overworld, or complete maze → Torment Portal
- Legacy maze generator restored in the port; existing generated chunks must be deleted before visual/layout retest.

**Lord of Torment (not yet implemented):**
- 500 HP boss, fires Slowness fireballs, at 50% HP teleports self+player to Torment
- Drops: Soul of Torment Demon, Demon Hearts, Miniature Yellow Hearts, enchanted books
- Eating Soul of Torment Demon grants ability to send others to Torment

**Flame Imp (not yet implemented):**
- 50 HP, 4 damage, hostile; summoned via Rite of Summoning
- Tameable with Demonic Contract (crafted, signed in blood) + 25 XP levels
- Bound imp stays in area, attacks those hostile to witch
- Demon Heart → temporarily doubles size and damage (30 min cooldown; Icy Needle cancels)
- Happy tamed Flame Imp eventually gives Torment Scroll (Lord of Torment summon item)


### 2026-05-15 (M20 — Familiar Entities)
- TamableAnimal is at net.minecraft.world.entity.TamableAnimal (NOT world.entity.passive) in 1.21.11
- Mob.createMobAttributes() replaces Animal.createAnimalAttributes() which was removed in 1.21.11
- Inventory.add(ItemStack) returns boolean in 1.21.11 (not ItemStack remainder)
- AvoidEntityGoal<T> requires explicit type parameter when Java can't infer: new AvoidEntityGoal<Phantom>(...)
- EntityAttributeCreationEvent handler goes in @EventBusSubscriber(bus=MOD) class alongside DeferredRegister
- Entity textures go in textures/entity/ (new folder); model LayerLocations in WitcheryClient.LAYER_DEFS list
- riteOfBindFamiliar: spawns entity at circle center, calls tame(player), stores UUID in WitcheryPlayerData, syncs payload
- Owl warning: scans 20-block radius every 100 ticks via tick() override, sends chat message if monsters found
- Toad collection: checks 6-block radius every 200 ticks, calls Inventory.add() and discards ItemEntity on success

### 2026-05-15 (M19 — Infusion Base System)
- InfusionType enum stored as String in WitcheryPlayerData to avoid codec complexity; fromString() parses at call sites
- SyncPlayerDataPayload updated to carry infusionType — add one writeUtf/readUtf pair to both encode/decode and the from() factory
- Rite of Infusion now dispatches before coal/food rites in dispatchSmall — brew base item determines infusion type
- RiteOfBlessing (Regen II + Speed I) replaces old riteOfInfusion fallback for empty small circle
- InfusionEvents uses PlayerTickEvent.Post every 100 ticks; re-applies effects for 140 ticks so they stay active without requiring always-on duration
- Infernal Animus recipe consumes a Voodoo Poppet as ingredient (unbound poppets work since recipe matching ignores CustomData)
- PacketDistributor.sendToPlayer() syncs infusion immediately after Rite of Infusion
- All 15 missing item-model block textures copied from original decompiled assets (camelCase→snake_case rename); 12 poppet textures + taglock textures also added from original

### 2026-05-15 (M18 — Taglock Kit + remaining poppets)
- TaglockKitItem.getBoundName() avoids collision with Item.getName(ItemStack) — never use "getName" as a static method name in Item subclasses
- ProtectionPoppetItem uses ProtectionType enum (EARTH/WATER/FIRE/HUNGER/VOODOO/POPPET_SHIELD) dispatched in PoppetEvents.damageMatchesProtection()
- Death poppets use LivingDeathEvent (cancellable) on the forge bus; POPPET_SHIELD returns false (protects other poppets, not death itself)
- Vampiric poppet uses LivingDamageEvent.Pre (not LivingDamageEvent) to intercept damage before armor reduction; redirects 50% as magic damage to bound player
- DamageTypes.* for specific types (FALL, DROWN, STARVE, MAGIC); DamageTypeTags.IS_FIRE for fire tag; DamageTypeTags.BYPASSES_ARMOR added to VOODOO coverage
- VoodooPoppetItem right-click applies Poison II + Weakness II to bound player; consumes on use
- All poppets stackTo(1) — each is uniquely bound to one player UUID
- Item models point to specific textures (poppet.protect_earth.png etc.) registered alongside poppet base texture



### 2026-05-15 (M11 — Witches Oven)
- OvenRecipe extends AbstractCookingRecipe; constructor takes (group, category, ingredient, result, exp, cookTime) matching SimpleCookingSerializer.Factory interface. No recipeBookCategory() override needed.
- Burn time: use stack.getBurnTime(recipeType) (NeoForge IItemStackExtension), not CommonHooks.getBurnTime()
- AbstractCookingRecipe stores RecipeType in this.type field; getType() returns it. Pass WitcheryRecipeTypes.OVEN.get() in super() call.
- Screen registered via RegisterMenuScreensEvent on the MOD bus (in WitcheryClient @EventBusSubscriber MOD)
- Menu type registered via WitcheryMenuTypes.MENU_TYPES.register(modEventBus) in WitcheryMod constructor
- openMenu(be, pos) pattern: ServerPlayer.openMenu(MenuProvider, Consumer<FriendlyByteBuf>) or the overload ServerPlayer.openMenu(menuProvider, blockPos)
- Official Witchery site scraped: full rite names, complete item list, dream catcher types, additional brews confirmed — PORTING_PLAN.md updated

### 2026-05-15 (Crop block fix)
- CropBlock.createBlockStateDefinition uses the STATIC field CropBlock.AGE (AGE_7), not this.getAgeProperty(). Overriding only getAgeProperty() is not enough — must also override createBlockStateDefinition to register the correct property (AGE_4). The Java constant name (BELLADONNA_AGE etc.) is irrelevant; the property's actual name is "age" (from BlockStateProperties.AGE_4 = IntegerProperty.create("age", 0, 4)). Blockstate JSON variants must use "age=0" through "age=4".

### 2026-05-15 (Polishing pass)
- WitcheryBlockItemRenderer refactored: if-else chain + 14 fields → IdentityHashMap<Item, Supplier<BlockEntity>> factory map + computeIfAbsent cache. Adding a BESR block item now requires one line here only.
- WitcheryClient.registerClientExtensions now derives item list from WitcheryBlockItemRenderer.registeredItems() — no more duplicate list to maintain.
- WitcheryClient.registerLayerDefinitions collapsed from 14 calls to a List<Map.Entry<...>> + forEach.
- KettleBlockEntity.color made private, getColor() added; KettleRenderer updated.
- ChalkItem.hurtAndBreak guarded against null player (ctx.getPlayer() can be null in automation).
- Removed orphaned lang key "block.witchery.demon_heart_block".

### 2026-05-15 (M10)
- PoppetItem uses DataComponents.CUSTOM_DATA + CustomData.update() for UUID binding (not legacy NBT tag)
- LivingDeathEvent is cancellable on the NeoForge event bus (@EventBusSubscriber, not mod bus)
- Poppet stacks to 1 (each is uniquely bound); death restores to 4f HP (2 hearts) before re-die loop
- Binding: interactLivingEntity() intercepts right-click on a Player entity

### 2026-05-15 (M8+M9)
- M9: AttachmentType registered via DeferredRegister<AttachmentType<?>> + NeoForgeRegistries.Keys.ATTACHMENT_TYPES
- M9: SyncPlayerDataPayload uses StreamCodec<RegistryFriendlyByteBuf, T> (not ByteBuf) for playToClient
- M9: RegisterPayloadHandlersEvent (not RegisterPayloadsEvent) + event.registrar("1").playToClient(...)
- M9: WitcheryPlayerEvents uses @EventBusSubscriber (forge bus, not mod bus) for player lifecycle events
- M8: ChalkItem.hurtAndBreak takes EquipmentSlot not InteractionHand in 1.21.11
- M8: LightningBolt spawned via new LightningBolt(EntityType.LIGHTNING_BOLT, level) + addFreshEntity() + setVisualOnly(true)
- M8: CIRCLE block uses carpet model (16×1×16 thin flat block) for chalk mark appearance
- M8: Ritual trigger: right-click any floor block with arthana; scans ring one Y level above clicked block

### 2026-05-15
- M3+M4 done: BelladonnaBlock/MandrakeBlock/ArtichokeBlock extend CropBlock with AGE_4 property (5 stages)
- BlockStateProperties.AGE_4 used; getAgeProperty() + getMaxAge() + getBaseSeedId() overridden
- Crop items (belladonna, mandrake_root, artichoke) are ItemNameBlockItem — same item is both seed and ingredient
- Loot tables: always drop 1 seed, mature (age=4) drops 0-2 more
- WITCH_CROP and COTTON remain as simpleBlock placeholders (growth logic deferred)
- Fetish + Statue of Goddess converted to full BESR (FetishBlock/StatueGoddessBlock + BlockEntity + Renderer)
- Refilling chest inventory icon fixed via BEWLR reusing LeechChest renderer
- Dream catcher GUI rotation fixed: [0,180,0] → [0,90,0]

### 2026-05-14
- Decompiled all three jars (witchery-1.7.2-0.19.0, companion, WitcheryResurrected 1.12.2)
- Renamed all camelCase texture files to snake_case (required by 1.21.11 resource location rules)
- BESR pattern established: ModelPart divides by /16 internally — do NOT add explicit scale(0.0625)
- Standard BESR transform: translate(0.5,0.5,0.5) → rotateZ(180) → translate(0,-1,0) → rotateY(-90)
- noOcclusion() required on all BESR blocks so block below is visible through legs/frame gaps
- Blocks placed before noOcclusion was set need to be broken and replaced to refresh light
- Zero-dimension boxes (W=0/H=0/D=0) port correctly to CubeListBuilder; negative UV offsets clamp to 0 and give correct texture sampling (e.g. chalice cup sides)
- LeechChest uses different transform: translate(0,1,1) + scale(1,-1,-1) instead of standard rotateZ pattern

### 2026-05-15 (M25/M28 — Dimensions)
- Dimensions are fully data-driven: `data/witchery/dimension_type/`, `data/witchery/dimension/`, `data/witchery/worldgen/biome/`
- Biome `features` array must be 10 sub-arrays `[[], [], ...]`, not bare `[]`
- `server.getLevel(ResourceKey<Level>)` returns null if dimension not loaded yet — safe to call after the first teleport
- Force chunk generation before scanning for a safe Y: `level.getChunk(0, 0, ChunkStatus.FULL, true)`
- Dream World uses End-style noise (`"settings": "minecraft:end"`) — central island always at ~(0, 64, 0)
- Torment now uses a custom legacy maze generator, not Nether-style noise; regenerate existing Torment chunks before retesting maze block/layout changes.
- `WitcheryPlayerData` has `returnPos`/`returnDimension` — set before teleport, cleared on exit; copied via `PlayerEvent.Clone`

### 2026-05-15 (Entity models)
- Always compare against decompiled `ModelXxx.java` before writing a modern model — textures were painted for the original UV
- **GhostModel** (Banshee/Spectre/Poltergeist): original `ModelSpectre` UV: head=texOffs(0,16), body=texOffs(16,0), arms=texOffs(0,0), robeUpper=texOffs(38,9), robeLower=texOffs(32,20). 64×32 texture.
- **CatFamiliarModel**: `blackcat.png` was used by `RenderWitchCat extends RenderOcelot`, so it is an ocelot UV texture. The port renderer now uses the dedicated CatFamiliarModel instead of modern vanilla CatModel/all_black.png, with old ocelot anchors: head(0,0), body(20,0) at the horizontal ModelOcelot pose, back legs(8,13), front legs(40,0), and two root tail segments at tail(0,15)/(4,15).
- **UV overflow**: `texOffs(0,30)` with a 4×9×4 box in 64×32 texture needs y=43 — beyond texture height. Move to free UV area.
- **EntModel**: actual `ent.png` is 256×256 — must declare `LayerDefinition.create(mesh, 256, 256)` to avoid UV mismatch.
- `EntityRenderDispatcher.getRenderer(Entity)` returns raw type — cast via `Object` then `instanceof PlayerRenderer`

### 2026-05-15 (Poppet shelf + coven witch)
- Poppet shelf switched from custom 4-slot menu/screen to vanilla `ChestMenu(MenuType.GENERIC_9x1, id, playerInv, this, 1)` — original had 9 slots and used `player.displayGUIChest()`
- `ChestMenu.oneRow(id, playerInv)` in 1.21.11 takes only 2 args (no container) — use the full constructor instead
- Coven Witch: `AbstractVillager`; override `showProgressBar()→false`, `getVillagerXp()→0`, `canRestock()→true` for no-prerequisites trading
- Coven Witch random names: generate from first/surname arrays in `finalizeSpawn()`; set via `setCustomName(Component.literal(...))`
- `Component.translatable(key).getString()` on server returns raw key — send `Component.translatable(key)` directly and let client translate

### 2026-05-15 (WerewolfLayer)
- `getParentModel().setAllVisible(false)` hides player body; called each frame with one-frame delay before effect takes hold
- Werewolf buffs now always apply when `isWerewolf()=true` — removed full-moon gate to match the always-on visual
- `WitcheryClientEvents` (game bus, `Dist.CLIENT`) resets `setAllVisible(true)` on respawn/dimension change for non-werewolf players

### 2026-05-23 — PlayerSkin API and humanoid NPC renderer (NeoForge 21.11.42)

**PlayerSkin package moved**
- In 21.11.42 `PlayerSkin` is at `net.minecraft.world.entity.player.PlayerSkin`, NOT `net.minecraft.client.resources.PlayerSkin` (the old 21.1.x location). Imports from the client package will fail to resolve.

**PlayerSkin texture accessor changed**
- Old (21.1.x): `PlayerSkin.texture()` → `ResourceLocation`
- New (21.11.42): `PlayerSkin.body()` → `ClientAsset$Texture`; then `.texturePath()` → `Identifier`
- Do NOT call `.texture()` — it does not exist in the compiled 21.11.42 jar.

**SkinManager lookup changed**
- Old: `SkinManager.lookupInsecure(GameProfile)` — does not exist in 21.11.42
- New: `SkinManager.createLookup(GameProfile, boolean)` → `Supplier<PlayerSkin>`; call `.get()` then `.body().texturePath()`
- The boolean controls whether to allow insecure (non-HTTPS) lookups; pass `false` for the cached/default behavior.

**Full skin resolution pattern for an NPC that mirrors a player's skin:**
```java
private static Identifier resolveSkin(ReflectionEntity entity) {
    UUID ownerUUID = entity.getOwnerUUID();
    if (ownerUUID == null) return DefaultPlayerSkin.getDefaultTexture();
    Minecraft mc = Minecraft.getInstance();
    if (mc.level != null) {
        Player player = mc.level.getPlayerByUUID(ownerUUID);
        if (player instanceof AbstractClientPlayer acp) {
            return acp.getSkin().body().texturePath();
        }
    }
    String name = entity.getOwnerName();
    GameProfile profile = new GameProfile(ownerUUID, name.isEmpty() ? null : name);
    return mc.getSkinManager().createLookup(profile, false).get().body().texturePath();
}
```

**HumanoidMobRenderer for humanoid NPCs**
- Use `HumanoidMobRenderer<T extends Mob, S extends HumanoidRenderState, M extends HumanoidModel<S>>` as the base class for any NPC that should render with standard humanoid animation and accept a player skin texture.
- Use `MobRenderer` + `LivingEntityRenderState` only for non-humanoid mobs; it will not correctly animate arms/legs for player-skin UV layouts.

**ModelLayers.PLAYER for player-skin-compatible models**
- `ctx.bakeLayer(ModelLayers.PLAYER)` produces a `ModelPart` built to the exact 64×64 player skin UV layout.
- Using any other model (e.g. `WitchHunterModel`, `ZombieModel`) with a player skin texture will produce UV mismatches and visible head/body artifacts even if the mesh geometry looks roughly correct.

**Verifying compiled API with javap**
- When unsure which API variant is live in the current NeoForge jar, run:
  `javap -cp ./build/moddev/artifacts/neoforge-21.11.42-merged.jar net.minecraft.client.resources.SkinManager`
- Use `neoforge-21.11.42-merged.jar`, not `neoforge-21.1.229.jar` — method signatures differ between these two jars.

**Stale dimension chunks are not regenerated**
- If a custom chunk generator changes its structure (e.g. Mirror World room layout), previously generated and saved chunks will NOT be regenerated on the next launch. Old saved chunk data is loaded from disk as-is.
- To force regeneration: delete `run/saves/<world>/dimensions/<modid>/<dimension>/` before launching, then trigger any block/BE state that was tied to the old chunk (e.g. break and replace the mirror block to clear its room assignment).
- This applies to all custom dimensions (Mirror World, Torment, Spirit World). Always delete the dimension folder and reset relevant block entities when testing chunk-structure changes.
