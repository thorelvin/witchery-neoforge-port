# Remake Guide — Witchery 1.7.10 → NeoForge 1.21.11

## Project Goal

Rebuild the gameplay of **Witchery for Minecraft 1.7.10** as a modern **NeoForge** mod.
This is a feature-by-feature modern remake. Reuse decompiled logic where it works; rewrite where the API has changed or the original can be improved.
Old code is used as a **behavior reference only**, not as code to paste.

---

## Working Rules

1. One subsystem at a time — never remake the whole mod in one step.
2. Do not delete assets, data files, lang files, or textures unless explicitly told.
3. Reuse decompiled source logic freely — adapt to NeoForge 1.21.11 APIs as needed. Rewrite only when the original approach is poor or the API has fundamentally changed.
4. After every coding milestone, confirm `gradlew compileJava` passes.
5. Keep REMAKE_NOTES.md updated with API gotchas after each milestone.
6. Keep FEATURE_MATRIX.md status updated.
7. When unsure about a NeoForge API, check decompiled NeoForge source or local Gradle cache — do not guess.

---

## How to add things

**New brew item:** `BrewOf<Name>Item extends BrewItem`, override `applyEffect()`. One file, ~15 lines.

**New rite:** Add ingredient check in `RitualExecutor.dispatch<Size>()` + private method.

**New protection poppet:** Add case in `PoppetEvents.damageMatchesProtection()` and the `ProtectionType` enum.

**New BESR block model:** One line in `WitcheryBlockItemRenderer.registeredItems()` + one entry in `WitcheryClient.LAYER_DEFS`.

**New oven/distillery recipe:** One JSON in `data/witchery/recipe/oven/` or `data/witchery/recipe/distillery/`.

**New entity renderer:** Entry in `WitcheryClient.registerRenderers()` + layer in `LAYER_DEFS` if custom model.

**New dimension:** JSON files in `data/witchery/dimension_type/`, `data/witchery/dimension/`, `data/witchery/worldgen/biome/`. ResourceKey in `WitcheryDimensions`.

---

## Behavior preservation rules

1. Identify what the feature does from the player's perspective.
2. Ignore old implementation details unless still useful.
3. Rebuild using modern NeoForge systems.
4. Prefer data-driven JSON for recipes, loot, tags, worldgen, configurable content.

---

## Remaking strategy — current progress

See FEATURE_MATRIX.md for full status.

| Layer | Status |
|---|---|
| Project shell, registries, creative tab | ✅ |
| Plants, ingredients, crops | ✅ |
| Kettle + recipe type + 13 brews | ✅ |
| Witches Oven / Distillery / Spinning Wheel | ✅ |
| Altar power system | ✅ |
| Ritual circle detection + 20+ rites | ✅ |
| Player data system | ✅ |
| 11 poppet types + shelf | ✅ |
| Vampirism + lycanthropy | ✅ |
| Familiar entities (owl/cat/toad) | ✅ |
| NPC entities (Coven Witch, Witch Hunter) | ✅ |
| Horror entities (Banshee, Spectre, Poltergeist, Treefyd, Ent, Mandrake, Demon) | ✅ |
| Broom (rideable) | ✅ |
| Dream World + Torment dimensions | ✅ |
| Worldgen (rowan tree) | ✅ |
| Dream Weaver items | 🔲 |
| More ritual rites (target 40+) | 🔲 |
| Witch hut + apothecary structures | 🔲 |
| Creature powers / symbiosis | 🔲 |
| Polish: particles, sounds, config | 🔲 |

---

## Key API renames at a glance

See **OLD_TO_NEW_API_MAP.md** for the full table and **REMAKE_NOTES.md** for session gotchas.

| Old | New |
|---|---|
| `TileEntity` | `BlockEntity` |
| `IExtendedEntityProperties` | `AttachmentType<T>` |
| `SimpleNetworkWrapper` | `CustomPacketPayload` + `PayloadRegistrar` |
| `entity.setSecondsOnFire(n)` | `entity.igniteForTicks(n * 20)` |
| `ModelBase` / `ModelRenderer` | `Model` / `ModelPart` via `CubeListBuilder` |
| Old dimension provider | JSON in `data/modid/dimension/` |
---

# Full API Map — Old Forge 1.7.10 → NeoForge 1.21.11

## Registration

| Old | New |
|---|---|
| `GameRegistry.registerBlock` | `DeferredRegister.Blocks` |
| `GameRegistry.registerItem` | `DeferredRegister.Items` |
| `GameRegistry.registerTileEntity` | `DeferredRegister<BlockEntityType<?>>` |
| `GameRegistry.registerEntity` | `DeferredRegister<EntityType<?>>` |
| `GameRegistry.addRecipe` | JSON recipe files in `data/modid/recipe/` |

---

## Block / Block Entity

| Old | New |
|---|---|
| `Block.func_149711_c(hardness)` | `BlockBehaviour.Properties.of().strength(h, r)` |
| `TileEntity` | `BlockEntity` |
| `TileEntity.updateEntity()` | Static `serverTick()` registered via `BlockEntityType` |
| `ISidedInventory` | `IItemHandler` capability or `WorldlyContainer` |
| `TileEntitySpecialRenderer` | `BlockEntityRenderer<T>` |
| `RenderShape.ENTITYBLOCK_ANIMATED` | Same — `BaseEntityBlock.getRenderShape()` |
| Chest-like container (9 slots) | `new ChestMenu(MenuType.GENERIC_9x1, id, playerInv, container, 1)` — `oneRow()` overloads don't accept a container in 1.21.11 |

---

## Rendering / Models

| Old | New |
|---|---|
| `ModelBase` / `ModelRenderer` | `Model` / `ModelPart` via `CubeListBuilder` |
| `ModelRenderer(this, texU, texV).addBox(...)` | `CubeListBuilder.create().texOffs(u,v).addBox(...)` |
| `setRotationPoint(rx, ry, rz)` | `PartPose.offset(rx, ry, rz)` |
| `setRotation(model, rx, ry, rz)` | `PartPose.offsetAndRotation(...)` |
| `GL11.glRotatef(180, 0, 0, 1)` | `poseStack.mulPose(Axis.ZP.rotationDegrees(180))` |
| `GL11.glScalef(0.0625, ...)` | **Do NOT add** — `ModelPart` divides by 16 internally |
| `RenderType.entityCutout` | Same; use `entityCutoutNoCull` for thin flat-face models |
| `renderToBuffer(buffer, light, overlay)` | `renderToBuffer(poseStack, buffer, light, overlay)` — **PoseStack first** in 1.21.11 |
| `AddLayers.getSkins()` | Returns `Set<PlayerSkin.Model>` not `Set<String>` |
| `EntityRenderDispatcher.getRenderer(entity)` | Returns raw `EntityRenderer<Entity>` — cast via `Object` then `instanceof` |

**BESR transform (standard Witchery):** `translate(0.5,0.5,0.5)` → `rotateZ(180)` → `translate(0,-1,0)` → `rotateY(-90)`  
**LeechChest transform:** `translate(0,1,1)` → `scale(1,-1,-1)` → `translate(0.5,0.5,0.5)` → `rotateY(-90)` → `translate(-0.5,-0.5,-0.5)`  
All BESR blocks need `.noOcclusion()`.

---

## Entity Models — remaking from original ModelXxx

Always check the decompiled `ModelXxx.java` for correct UV offsets, texture size, and part positions before writing a modern model.
Textures were painted for the original UV layout; wrong UV = garbled or invisible model.

| Gotcha | Detail |
|---|---|
| GhostModel UV | head=texOffs(0,16), body=texOffs(16,0), arms=texOffs(0,0), robeUpper=texOffs(38,9), robeLower=texOffs(32,20). 64×32. |
| CatFamiliarModel | `blackcat.png` was used by `RenderWitchCat extends RenderOcelot` — it's an ocelot UV skin, not a custom cat skin. |
| UV overflow | `texOffs(0,30)` with a 4×9×4 box in a 64×32 texture overflows to y=43. Move legs to free UV space. |
| Texture size | Always check actual PNG dimensions. `ent.png` is 256×256 — declare that in `LayerDefinition.create(mesh, 256, 256)`. |

---

## Player / Entity Data

| Old | New |
|---|---|
| `IExtendedEntityProperties` | `IAttachmentHolder` + `AttachmentType<T>` |
| `NBTTagCompound` | `CompoundTag` |
| Poppet item NBT | `DataComponents.CUSTOM_DATA` + `CustomData.update()`. Keys: `bound_uuid`, `bound_name`. |
| Item UUID binding | `stacksTo(1)` required if identity matters |

---

## World / Level

| Old | New |
|---|---|
| `World.getBlockId(x,y,z)` | `Level.getBlockState(pos)` |
| `World.setBlock(x,y,z,block)` | `Level.setBlockAndUpdate(pos, state)` |
| `entity.setSecondsOnFire(n)` | `entity.igniteForTicks(n * 20)` — renamed in 1.21.11 |
| Force chunk generation | `serverLevel.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true)` — synchronous on server thread |
| Set weather | `serverLevel.setWeatherParameters(clearDuration, rainDuration, isRaining, isThundering)` |

---

## Dimensions

Custom dimensions are fully data-driven in 1.21.11:
- `data/<modid>/dimension_type/<name>.json` — physical properties (ambient light, ceiling, skylight, effects)
- `data/<modid>/dimension/<name>.json` — references dimension type + chunk generator
- `data/<modid>/worldgen/biome/<name>.json` — custom biome (features array must be 10 sub-arrays, not bare `[]`)
- `ResourceKey<Level>` from `ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(...))` 
- `server.getLevel(key)` returns the ServerLevel once loaded

---

## Networking

| Old | New |
|---|---|
| `SimpleNetworkWrapper` | `PayloadRegistrar` + `CustomPacketPayload` |
| Sync packet | `RegisterPayloadHandlersEvent` (not `RegisterPayloadsEvent`); `event.registrar("1").playToClient(TYPE, CODEC, handler)` |
| Codec type | `StreamCodec<RegistryFriendlyByteBuf, T>` (not plain `ByteBuf`) |

---

## Events

| Old | New |
|---|---|
| `LivingDeathEvent` | Cancellable on NeoForge game bus (`@EventBusSubscriber` without bus=MOD) |
| `LivingDamageEvent.Pre` | Use `.Pre` to intercept before armor reduction |
| `PlayerEvent.Clone` | Fires on both death respawn AND dimension transfer; `isWasDeath()` distinguishes them |
| `EntityRenderersEvent.AddLayers` | MOD bus; `getSkins()` returns `Set<PlayerSkin.Model>` |
| Client-only game events | Use `@EventBusSubscriber(value = Dist.CLIENT)` (game bus, not MOD bus) |

---

## Recipes

| Old | New |
|---|---|
| `GameRegistry.addRecipe(shaped)` | `data/modid/recipe/name.json` with `minecraft:crafting_shaped` |
| Custom `IRecipe` | Custom `RecipeSerializer<T>` + `RecipeType<T>` |
| `AbstractCookingRecipe` subclass | Constructor signature: `(group, category, ingredient, result, exp, cookTime)` — pass `WitcheryRecipeTypes.OVEN.get()` in `super()` |
| Burn time | `stack.getBurnTime(recipeType)` via NeoForge `IItemStackExtension` |

---

## Crops

| Old | New |
|---|---|
| Multi-stage crop | Extend `CropBlock`; override `getAgeProperty()` returning `BlockStateProperties.AGE_4`; override `createBlockStateDefinition()` to register it; override `getMaxAge()`, `getBaseSeedId()` |
| Seed + ingredient same item | `ItemNameBlockItem` — one item serves as both seed and ingredient |
| Bonemeal interaction | Override `isValidBonemealTarget()` and `performBonemeal()` or use default CropBlock logic |
