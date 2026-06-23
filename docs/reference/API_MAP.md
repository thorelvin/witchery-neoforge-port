# API Map — Old Forge 1.7.10 → NeoForge 1.21.11

Use this as a quick reference. For session-specific gotchas, search `REMAKE_NOTES.md`.

## Registration

| Old | New |
|---|---|
| `GameRegistry.registerBlock` | `DeferredRegister.Blocks` |
| `GameRegistry.registerItem` | `DeferredRegister.Items` |
| `GameRegistry.registerTileEntity` | `DeferredRegister<BlockEntityType<?>>` |
| `GameRegistry.registerEntity` | `DeferredRegister<EntityType<?>>` |
| `GameRegistry.addRecipe` | JSON recipes in `data/<modid>/recipe/` |

## Blocks / block entities

| Old | New |
|---|---|
| `TileEntity` | `BlockEntity` |
| `TileEntity.updateEntity()` | Static server tick registered through `BlockEntityType` |
| `ISidedInventory` | `IItemHandler` capability or `WorldlyContainer` |
| `TileEntitySpecialRenderer` | `BlockEntityRenderer<T>` |
| `Block.func_149711_c(hardness)` | `BlockBehaviour.Properties.of().strength(h, r)` |
| Chest-like one-row container | `new ChestMenu(MenuType.GENERIC_9x1, id, playerInv, container, 1)` |

## Rendering / models

| Old | New |
|---|---|
| `ModelBase` / `ModelRenderer` | `Model` / `ModelPart` via `CubeListBuilder` |
| `ModelRenderer(...).addBox(...)` | `CubeListBuilder.create().texOffs(u,v).addBox(...)` |
| `setRotationPoint(x,y,z)` | `PartPose.offset(x,y,z)` |
| custom rotation fields | `PartPose.offsetAndRotation(...)` |
| `GL11.glRotatef(180, 0, 0, 1)` | `poseStack.mulPose(Axis.ZP.rotationDegrees(180))` |
| explicit `GL11.glScalef(0.0625,...)` | Usually **do not add**; `ModelPart` handles scale |
| `renderToBuffer(buffer, light, overlay)` | `renderToBuffer(poseStack, buffer, light, overlay)` |
| `AddLayers.getSkins()` strings | `Set<PlayerSkin.Model>` |

### Model gotchas

- Always verify actual PNG dimensions before setting `LayerDefinition.create(mesh, width, height)`.
- Original zero-depth boxes can often remain zero-depth; changing to depth 1 can create visible artifacts.
- Old textures were painted for exact UVs. Wrong UVs cause invisible/garbled parts.
- `entityCutoutNoCull` helps for thin flat-face models.

## Player / entity data

| Old | New |
|---|---|
| `IExtendedEntityProperties` | `AttachmentType<T>` / `IAttachmentHolder` |
| `NBTTagCompound` | `CompoundTag` |
| Item NBT | Data components; `DataComponents.CUSTOM_DATA` + `CustomData.update()` where used |
| Identity-bound item stacks | `stacksTo(1)` |

## World / level

| Old | New |
|---|---|
| `World.getBlockId(x,y,z)` | `Level.getBlockState(pos)` |
| `World.setBlock(x,y,z,block)` | `Level.setBlockAndUpdate(pos, state)` |
| `entity.setSecondsOnFire(n)` | `entity.igniteForTicks(n * 20)` |
| Force chunk generation | `serverLevel.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true)` |
| Weather | `serverLevel.setWeatherParameters(clearDuration, rainDuration, isRaining, isThundering)` |

## Dimensions

Custom dimensions are data-driven:
- `data/<modid>/dimension_type/<name>.json`
- `data/<modid>/dimension/<name>.json`
- `data/<modid>/worldgen/biome/<name>.json`
- `ResourceKey<Level>` from `ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(...))`
- `server.getLevel(key)` returns the `ServerLevel` once loaded.

Biome `features` arrays need the expected generation-step sub-array structure.

## Networking

| Old | New |
|---|---|
| `SimpleNetworkWrapper` | `PayloadRegistrar` + `CustomPacketPayload` |
| Sync packet registration | `RegisterPayloadHandlersEvent` |
| Payload codec | `StreamCodec<RegistryFriendlyByteBuf, T>` |
| Send to player | `PacketDistributor.sendToPlayer(...)` |

## Events

| Old / issue | NeoForge 1.21.11 note |
|---|---|
| `LivingDeathEvent` | Cancellable on game bus |
| `LivingDamageEvent.Pre` | Use for damage before armor reduction |
| `PlayerEvent.Clone` | Fires on death respawn and dimension transfer; check `isWasDeath()` |
| `EntityRenderersEvent.AddLayers` | MOD bus |
| Client-only render events | Use `@EventBusSubscriber(value = Dist.CLIENT)` and correct bus |
| `RenderPlayerEvent.Pre/Post` | Use for pre-hiding player model if available; verify exact class in local source |

## Recipes

| Old | New |
|---|---|
| Shaped/shapeless registration | JSON crafting recipes |
| Custom `IRecipe` | Custom `RecipeSerializer<T>` + `RecipeType<T>` |
| Cooking-style machine | `AbstractCookingRecipe` subclass with current constructor signature |
| Burn time | `stack.getBurnTime(recipeType)` |

## Crops

| Old | New |
|---|---|
| Multi-stage crop | Extend `CropBlock` |
| Custom age property | Override `getAgeProperty()`, `getMaxAge()`, and `createBlockStateDefinition()` |
| Seed + ingredient same item | `ItemNameBlockItem` |
| Bonemeal | Use default `CropBlock` logic or override target/perform methods |

## Known model notes

| Model | Note |
|---|---|
| GhostModel | Original Spectre/Banshee/Poltergeist texture is 64×32; mouth zero-depth quad should not become a visible cuboid. |
| Cat familiar | Original `RenderWitchCat` used vanilla `RenderOcelot`/`ModelOcelot`; keep the old ocelot UV/layout, horizontal body rotation, leg anchors, and two root tail segments. |
| Ent | Texture is large; confirm `256×256` before declaring layer texture size. |
| Demon | Verify whether texture is `64×32` or `64×64` before remapping limbs. |
