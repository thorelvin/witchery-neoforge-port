package com.emoniph.witchery.spiritworld;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.command.HeadlessGameTestPlayer;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.CorpseEntity;
import com.emoniph.witchery.events.SpiritWorldEvents;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.entity.NightmareEntity;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Dedicated-server certification for dream entry, waking, manifestation, and player isolation. */
public final class SpiritWorldGameTests {
    private static final BlockPos DREAM_POS = new BlockPos(544, 90, 544);
    private static final BlockPos NIGHTMARE_POS = new BlockPos(560, 90, 544);
    private static final BlockPos FRESH_SEARCH_ANCHOR = new BlockPos(6144, 64, -6144);
    private static final int FRESH_CHUNK_RADIUS = 4;
    private static final UUID DREAM_PLAYER_ID = UUID.fromString("04bf9665-a65d-41ea-a47b-494e320bf06a");
    private static final UUID NIGHTMARE_PLAYER_ID = UUID.fromString("848520a6-1735-4681-b3af-0f9c4c3dc48a");
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LIFECYCLE =
            TEST_FUNCTIONS.register("spirit_world_lifecycle", () -> SpiritWorldGameTests::lifecycle);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FRESH_CHUNKS =
            TEST_FUNCTIONS.register("spirit_world_fresh_chunks", () -> SpiritWorldGameTests::freshChunks);

    private SpiritWorldGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(SpiritWorldGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "spirit_world"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 300, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "spirit_world_lifecycle"),
                new FunctionGameTestInstance(LIFECYCLE.getKey(), data));
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "spirit_world_fresh_chunks"),
                new FunctionGameTestInstance(FRESH_CHUNKS.getKey(), data));
    }

    private static void freshChunks(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel spirit = overworld.getServer().getLevel(WitcheryDimensions.SPIRIT_WORLD);
        helper.assertTrue(overworld.dimension().equals(Level.OVERWORLD),
                "Fresh-chunk certification must run in the Overworld");
        helper.assertTrue(spirit != null, "Spirit World dimension must be loaded");
        if (spirit == null) {
            return;
        }

        Pair<BlockPos, Holder<Biome>> plains = overworld.findClosestBiome3d(
                biome -> biome.is(Biomes.PLAINS), FRESH_SEARCH_ANCHOR, 6400, 32, 64);
        helper.assertTrue(plains != null,
                "Fixed-seed certificate requires plains near its untouched search anchor");
        if (plains == null) {
            return;
        }
        int centerChunkX = plains.getFirst().getX() >> 4;
        int centerChunkZ = plains.getFirst().getZ() >> 4;
        helper.assertTrue(spirit.getBiome(plains.getFirst()).equals(plains.getSecond()),
                "The fixed-seed Spirit World must mirror the selected Overworld plains biome");

        int biomeSamples = 0;
        int terrainSamples = 0;
        int cottonBlocks = 0;
        int ordinaryEcologyBlocks = 0;
        int sourceFloorBlocks = 0;
        int spiritSpawnBiomes = 0;

        for (int chunkX = centerChunkX - FRESH_CHUNK_RADIUS;
             chunkX <= centerChunkX + FRESH_CHUNK_RADIUS; chunkX++) {
            for (int chunkZ = centerChunkZ - FRESH_CHUNK_RADIUS;
                 chunkZ <= centerChunkZ + FRESH_CHUNK_RADIUS; chunkZ++) {
                overworld.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
                spirit.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);

                for (int localX = 2; localX < 16; localX += 4) {
                    for (int localZ = 2; localZ < 16; localZ += 4) {
                        int x = (chunkX << 4) + localX;
                        int z = (chunkZ << 4) + localZ;
                        for (int y : new int[]{32, 64, 96, 128}) {
                            Holder<?> overworldBiome = overworld.getBiome(new BlockPos(x, y, z));
                            Holder<?> spiritBiome = spirit.getBiome(new BlockPos(x, y, z));
                            helper.assertTrue(overworldBiome.equals(spiritBiome),
                                    "Spirit biome geography diverged from Overworld at " + x + "," + y + "," + z
                                            + ": overworld=" + overworldBiome + ", spirit=" + spiritBiome);
                            biomeSamples++;
                        }

                        int overworldFloor = overworld.getChunkSource().getGenerator().getFirstOccupiedHeight(
                                x, z, Heightmap.Types.OCEAN_FLOOR_WG, overworld,
                                overworld.getChunkSource().randomState());
                        int spiritFloor = spirit.getChunkSource().getGenerator().getFirstOccupiedHeight(
                                x, z, Heightmap.Types.OCEAN_FLOOR_WG, spirit,
                                spirit.getChunkSource().randomState());
                        helper.assertTrue(overworldFloor == spiritFloor,
                                "Spirit terrain height diverged from Overworld at " + x + "," + z
                                        + ": overworld=" + overworldFloor + ", spirit=" + spiritFloor);
                        terrainSamples++;

                        var spiritSpawn = spirit.getBiome(
                                        new BlockPos(x, Math.max(spiritFloor, 64), z)).value()
                                .getMobSettings().getMobs(MobCategory.AMBIENT).unwrap().stream()
                                .filter(spawn -> spawn.value().type() == WitcheryEntities.SPIRIT.get())
                                .findFirst().orElse(null);
                        if (spiritSpawn != null) {
                            MobSpawnSettings.SpawnerData spawnData = spiritSpawn.value();
                            helper.assertTrue(spiritSpawn.weight() == 1
                                            && spawnData.minCount() == 2 && spawnData.maxCount() == 5,
                                    "Native Spirit spawn settings must retain source weight 1 and group 2-5");
                            spiritSpawnBiomes++;
                        }
                    }
                }

                for (int localX = 0; localX < 16; localX++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        int x = (chunkX << 4) + localX;
                        int z = (chunkZ << 4) + localZ;
                        int top = spirit.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                        int bottom = Math.max(spirit.getMinY(), top - 24);
                        for (int y = bottom; y <= top; y++) {
                            BlockState state = spirit.getBlockState(new BlockPos(x, y, z));
                            if (state.is(WitcheryBlocks.WISPY_COTTON.get())) {
                                cottonBlocks++;
                            } else if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)
                                    || state.is(BlockTags.FLOWERS) || state.is(Blocks.SHORT_GRASS)
                                    || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN)
                                    || state.is(Blocks.LARGE_FERN)) {
                                ordinaryEcologyBlocks++;
                            }
                            if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.SAND)) {
                                sourceFloorBlocks++;
                            }
                        }
                    }
                }
            }
        }

        WitcheryMod.LOGGER.info(
                "[SpiritWorldFreshChunks] centerChunk={},{} chunks={} biomeSamples={} terrainSamples={} cotton={} ecology={} sourceFloors={} spiritSpawnSamples={}",
                centerChunkX, centerChunkZ,
                (FRESH_CHUNK_RADIUS * 2 + 1) * (FRESH_CHUNK_RADIUS * 2 + 1),
                biomeSamples, terrainSamples, cottonBlocks, ordinaryEcologyBlocks,
                sourceFloorBlocks, spiritSpawnBiomes);
        helper.assertTrue(biomeSamples == 5184 && terrainSamples == 1296,
                "Fresh-chunk certificate must complete every fixed geography sample");
        helper.assertTrue(cottonBlocks > 0,
                "Fixed-seed fresh Spirit chunks must naturally generate Wispy Cotton; ecology="
                        + ordinaryEcologyBlocks + ", sourceFloors=" + sourceFloorBlocks
                        + ", spiritSpawnSamples=" + spiritSpawnBiomes);
        helper.assertTrue(ordinaryEcologyBlocks > 0 && sourceFloorBlocks > 0,
                "Fresh Spirit chunks must retain ordinary Overworld ecology and source spawn floors");
        helper.assertTrue(spiritSpawnBiomes > 0,
                "At least one eligible generated biome must expose the native Spirit spawn entry");
        helper.succeed();
    }

    private static void lifecycle(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel spirit = overworld.getServer().getLevel(WitcheryDimensions.SPIRIT_WORLD);
        helper.assertTrue(overworld.dimension().equals(Level.OVERWORLD),
                "Spirit lifecycle GameTest must run in the Overworld");
        helper.assertTrue(spirit != null, "Spirit World dimension must be loaded");
        if (spirit == null) {
            return;
        }

        prepareColumn(overworld, DREAM_POS);
        prepareColumn(overworld, NIGHTMARE_POS);
        prepareColumn(spirit, DREAM_POS);
        prepareColumn(spirit, NIGHTMARE_POS);
        clearBodies(overworld);
        setReturnChunksForced(overworld, true);

        ServerPlayer dreamer = HeadlessGameTestPlayer.create(overworld, DREAM_PLAYER_ID, "dream-lifecycle");
        ServerPlayer nightmare = HeadlessGameTestPlayer.create(overworld, NIGHTMARE_PLAYER_ID, "nightmare-lifecycle");
        makeDamageable(dreamer);
        makeDamageable(nightmare);
        dreamer.snapTo(DREAM_POS.getX() + 0.25D, DREAM_POS.getY(), DREAM_POS.getZ() + 0.75D,
                0.0F, 0.0F);
        nightmare.snapTo(NIGHTMARE_POS.getX() + 0.5D, NIGHTMARE_POS.getY(), NIGHTMARE_POS.getZ() + 0.5D,
                0.0F, 0.0F);
        configureDreamer(dreamer);
        nightmare.getInventory().setItem(0, new ItemStack(WitcheryItems.ICY_NEEDLE.get(), 2));
        nightmare.getInventory().setItem(4, new ItemStack(Items.EMERALD, 3));

        boolean scheduled = false;
        try {
            SpiritWorldHelper.enter(dreamer, 0.0D);
            SpiritWorldHelper.enter(nightmare, 1.0D);
            helper.runAfterDelay(3, () -> verifyLifecycle(helper, overworld, spirit, dreamer, nightmare));
            scheduled = true;
        } finally {
            if (!scheduled) {
                cleanUp(overworld, dreamer, nightmare);
            }
        }
    }

    private static void verifyLifecycle(GameTestHelper helper, ServerLevel overworld, ServerLevel spirit,
                                        ServerPlayer dreamer, ServerPlayer nightmare) {
        ServerPlayer reconnectedNightmare = null;
        boolean deathCheckScheduled = false;
        try {
            WitcheryPlayerData dreamData = WitcheryAttachments.get(dreamer);
            WitcheryPlayerData nightmareData = WitcheryAttachments.get(nightmare);
            helper.assertTrue(dreamer.level() == spirit && nightmare.level() == spirit,
                    "Both dream travellers must enter the loaded Spirit World");
            helper.assertTrue(dreamData.isInSpiritWorld() && dreamData.isSpiritWorldDream(),
                    "Forced peaceful entry must retain local dream state");
            helper.assertTrue(nightmareData.isInSpiritWorld() && !nightmareData.isSpiritWorldDream(),
                    "Forced nightmare entry must retain local nightmare state");
            SpiritWorldHelper.tickNightmareRespawn(nightmare, spirit, nightmareData);
            ServerPlayer pressuredNightmare = nightmare;
            helper.assertTrue(!spirit.getEntitiesOfClass(NightmareEntity.class,
                            nightmare.getBoundingBox().inflate(18.0D),
                            entity -> entity.matchesVictim(pressuredNightmare))
                            .isEmpty(),
                    "A nightmare traveller must receive a victim-bound Nightmare pressure entity");
            helper.assertTrue(spirit.getEntitiesOfClass(NightmareEntity.class,
                            dreamer.getBoundingBox().inflate(18.0D), entity -> entity.matchesVictim(dreamer))
                            .isEmpty(),
                    "Nightmare pressure must remain isolated from a simultaneous peaceful dreamer");
            helper.assertTrue(SpiritWorldEvents.hasNightmareAtmosphere(spirit),
                    "One nightmare traveller must drive the shared source atmosphere");
            assertConsistent(helper, dreamer);
            assertConsistent(helper, nightmare);

            helper.assertTrue(close(dreamer.getX(), DREAM_POS.getX() + 0.25D)
                            && close(dreamer.getZ(), DREAM_POS.getZ() + 0.75D),
                    "Dream entry must retain exact same-coordinate X/Z");
            helper.assertTrue(countItem(dreamer, WitcheryItems.ICY_NEEDLE.get()) == 2
                            && countItem(dreamer, WitcheryItems.MUTANDIS.get()) == 3,
                    "Only Icy Needles and ordinary Mutandis must follow the first dream entry");
            helper.assertTrue(countItem(dreamer, Items.DIAMOND) == 0
                            && countItem(dreamer, WitcheryItems.MUTANDIS_EXTREMIS.get()) == 0
                            && countItem(dreamer, WitcheryItems.WITCHES_PRIMER.get()) == 0,
                    "Waking items, the Primer, and Mutandis Extremis must remain in the mortal snapshot");

            CorpseEntity dreamBody = body(overworld, dreamData, helper);
            helper.assertTrue(DREAM_PLAYER_ID.equals(dreamBody.getOwnerUUID().orElse(null))
                            && close(dreamBody.getX(), DREAM_POS.getX() + 0.5D)
                            && close(dreamBody.getZ(), DREAM_POS.getZ() + 0.5D),
                    "The sleeping Body must be owner-bound and centered on the entry block");
            helper.assertTrue(!dreamBody.isPushable() && dreamBody.isPersistenceRequired()
                            && close(dreamBody.getHealth(), 13.0D),
                    "The sleeping Body must remain immovable, persistent, and use waking health");

            WitcheryPlayerData persistedNightmare = new WitcheryPlayerData();
            persistedNightmare.copyFrom(nightmareData, true);
            List<ItemStack> nightmareInventory = copyInventory(nightmare);
            HeadlessGameTestPlayer.remove(nightmare);
            nightmare = null;
            reconnectedNightmare = HeadlessGameTestPlayer.create(
                    spirit, NIGHTMARE_PLAYER_ID, "nightmare-lifecycle");
            makeDamageable(reconnectedNightmare);
            WitcheryAttachments.get(reconnectedNightmare).copyFrom(persistedNightmare, true);
            restoreInventory(reconnectedNightmare, nightmareInventory);
            reconnectedNightmare.snapTo(NIGHTMARE_POS.getX() + 0.5D, NIGHTMARE_POS.getY(),
                    NIGHTMARE_POS.getZ() + 0.5D, 0.0F, 0.0F);
            helper.assertTrue(WitcheryAttachments.get(reconnectedNightmare).isNightmare()
                            && SpiritWorldEvents.hasNightmareAtmosphere(spirit),
                    "Reconnect must retain nightmare state and its shared atmosphere contribution");
            assertConsistent(helper, reconnectedNightmare);
            useNeedle(reconnectedNightmare, helper);
            helper.assertTrue(reconnectedNightmare.level() == overworld
                            && !WitcheryAttachments.get(reconnectedNightmare).isInSpiritWorld(),
                    "A reconnected nightmare traveller must wake through a real Icy Needle use");
            helper.assertTrue(!SpiritWorldEvents.hasNightmareAtmosphere(spirit)
                            && WitcheryAttachments.get(dreamer).isSpiritWorldDream(),
                    "Waking the nightmare traveller must leave the other player's dream untouched");

            dreamer.getInventory().add(new ItemStack(WitcheryItems.WISPY_COTTON.get(), 2));
            dreamer.getInventory().add(new ItemStack(Items.COBBLESTONE, 5));
            dreamer.setHealth(9.0F);
            dreamer.getFoodData().setFoodLevel(6);
            dreamer.getFoodData().setSaturation(1.0F);
            blockReturnColumn(overworld, DREAM_POS);
            useNeedle(dreamer, helper);
            helper.assertTrue(dreamer.level() == overworld
                            && dreamer.blockPosition().equals(DREAM_POS.above(2)),
                    "Blocked waking columns must resolve upward without changing X/Z");
            helper.assertTrue(countItem(dreamer, Items.DIAMOND) == 4
                            && countItem(dreamer, WitcheryItems.WISPY_COTTON.get()) == 2
                            && countItem(dreamer, Items.COBBLESTONE) == 0,
                    "Waking inventory must restore mortal items and only transfer source Spirit materials");
            helper.assertTrue(close(dreamer.getHealth(), 13.0D)
                            && dreamer.getFoodData().getFoodLevel() == 7
                            && close(dreamer.getFoodData().getSaturationLevel(), 2.0D),
                    "Waking must restore the original mortal vitals");
            helper.assertTrue(overworld.getEntity(dreamBody.getUUID()) == null,
                    "A normal Icy Needle wake must remove the sleeping Body");

            prepareColumn(overworld, DREAM_POS);
            dreamer.snapTo(DREAM_POS.getX() + 0.25D, DREAM_POS.getY(), DREAM_POS.getZ() + 0.75D,
                    0.0F, 0.0F);
            SpiritWorldHelper.enter(dreamer, 0.0D);
            dreamData = WitcheryAttachments.get(dreamer);
            helper.assertTrue(dreamer.level() == spirit && close(dreamer.getHealth(), 10.0D),
                    "A later dream must restore the separately saved, minimum-safe Spirit health");
            helper.assertTrue(countItem(dreamer, WitcheryItems.MUTANDIS.get()) == 3
                            && countItem(dreamer, Items.COBBLESTONE) == 5,
                    "Unused dream inventory must survive the next visit");

            dreamData.setManifestationTime(150);
            SpiritWorldHelper.manifestPlayerInOverworldAsGhost(dreamer);
            helper.assertTrue(dreamer.level() == overworld && dreamData.isManifestedGhost()
                            && dreamData.isInSpiritWorld(),
                    "Manifestation must preserve the underlying dream while moving the ghost outward");
            helper.assertTrue(countItem(dreamer, WitcheryItems.ICY_NEEDLE.get()) > 0
                            && countItem(dreamer, Items.COBBLESTONE) == 0,
                    "Only Icy Needles may leave the Spirit inventory during manifestation");
            assertConsistent(helper, dreamer);

            dreamer.getInventory().add(boundFetish());
            useNeedle(dreamer, helper);
            helper.assertTrue(dreamer.level() == spirit && !dreamData.isManifestedGhost()
                            && dreamData.isInSpiritWorld(),
                    "Icy Needle recall must return a manifested ghost to the same underlying dream");
            helper.assertTrue(hasBoundFetish(dreamer) && countItem(dreamer, Items.COBBLESTONE) == 5,
                    "A bound fetish acquired while manifested must join the restored Spirit inventory");
            assertConsistent(helper, dreamer);

            ServerPlayer settledNightmare = reconnectedNightmare;
            helper.runAfterDelay(65, () -> verifyDeathWake(
                    helper, overworld, spirit, dreamer, settledNightmare));
            deathCheckScheduled = true;
        } finally {
            if (!deathCheckScheduled) {
                cleanUp(overworld, dreamer, nightmare, reconnectedNightmare);
            }
        }
    }

    private static void verifyDeathWake(GameTestHelper helper, ServerLevel overworld, ServerLevel spirit,
                                        ServerPlayer dreamer, ServerPlayer reconnectedNightmare) {
        try {
            WitcheryPlayerData dreamData = WitcheryAttachments.get(dreamer);
            dreamer.setHealth(1.0F);
            dreamer.invulnerableTime = 0;
            dreamer.hasChangedDimension();
            dreamer.connection.markClientLoaded();
            boolean accepted = dreamer.hurtServer(spirit, spirit.damageSources().genericKill(), 20.0F);
            helper.assertTrue(dreamer.level() == overworld && dreamer.isAlive()
                            && !dreamData.isInSpiritWorld(),
                    "Lethal Spirit damage must be canceled into a normal wake rather than player death; "
                            + "accepted=" + accepted + ", dimension=" + dreamer.level().dimension().identifier()
                            + ", alive=" + dreamer.isAlive()
                            + ", health=" + dreamer.getHealth() + ", dreaming=" + dreamData.isInSpiritWorld()
                            + ", escrow=" + dreamData.getOverworldInventorySnbt().length());
            helper.assertTrue(dreamData.getCorpseUUID().isEmpty()
                            && dreamData.getOverworldInventorySnbt().isEmpty(),
                    "Death wake must clear the Body link and mortal inventory escrow");
            helper.succeed();
        } finally {
            cleanUp(overworld, dreamer, reconnectedNightmare);
        }
    }

    private static void configureDreamer(ServerPlayer player) {
        player.setHealth(13.0F);
        player.getFoodData().setFoodLevel(7);
        player.getFoodData().setSaturation(2.0F);
        player.getInventory().setItem(0, new ItemStack(WitcheryItems.ICY_NEEDLE.get(), 2));
        player.getInventory().setItem(1, new ItemStack(WitcheryItems.MUTANDIS.get(), 3));
        player.getInventory().setItem(2, new ItemStack(WitcheryItems.MUTANDIS_EXTREMIS.get(), 2));
        player.getInventory().setItem(3, new ItemStack(Items.DIAMOND, 4));
        player.getInventory().setItem(4, new ItemStack(WitcheryItems.WITCHES_PRIMER.get()));
    }

    private static void makeDamageable(ServerPlayer player) {
        player.setGameMode(GameType.SURVIVAL);
        player.setInvulnerable(false);
        player.getAbilities().invulnerable = false;
    }

    private static void prepareColumn(ServerLevel level, BlockPos feet) {
        level.getChunk(feet.getX() >> 4, feet.getZ() >> 4);
        for (int x = feet.getX() - 2; x <= feet.getX() + 2; x++) {
            for (int z = feet.getZ() - 2; z <= feet.getZ() + 2; z++) {
                level.setBlock(new BlockPos(x, feet.getY() - 1, z), Blocks.STONE.defaultBlockState(), 3);
                for (int y = feet.getY(); y <= feet.getY() + 4; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void blockReturnColumn(ServerLevel level, BlockPos feet) {
        level.setBlock(feet, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(feet.above(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(feet.above(2), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(feet.above(3), Blocks.AIR.defaultBlockState(), 3);
    }

    private static CorpseEntity body(ServerLevel overworld, WitcheryPlayerData data,
                                     GameTestHelper helper) {
        CorpseEntity body = data.getCorpseUUID()
                .map(overworld::getEntity)
                .filter(CorpseEntity.class::isInstance)
                .map(CorpseEntity.class::cast)
                .orElse(null);
        helper.assertTrue(body != null, "Dream entry must create a loaded sleeping Body");
        if (body == null) {
            throw new IllegalStateException("Missing Spirit lifecycle Body");
        }
        return body;
    }

    private static void useNeedle(ServerPlayer player, GameTestHelper helper) {
        int needleSlot = findItem(player, WitcheryItems.ICY_NEEDLE.get());
        helper.assertTrue(needleSlot >= 0, "Lifecycle stage requires an Icy Needle");
        if (needleSlot < 0) {
            return;
        }
        player.getInventory().setSelectedSlot(needleSlot);
        WitcheryItems.ICY_NEEDLE.get().use(player.level(), player, InteractionHand.MAIN_HAND);
    }

    private static ItemStack boundFetish() {
        ItemStack stack = new ItemStack(WitcheryItems.FETISH_SCARECROW.get());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("EffectTypeID", 1));
        return stack;
    }

    private static boolean hasBoundFetish(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (com.emoniph.witchery.blocks.entity.FetishBlockEntity.isBoundFetish(
                    player.getInventory().getItem(slot))) {
                return true;
            }
        }
        return false;
    }

    private static int findItem(ServerPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(item)) {
                return slot;
            }
        }
        return -1;
    }

    private static int countItem(ServerPlayer player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static List<ItemStack> copyInventory(ServerPlayer player) {
        List<ItemStack> stacks = new ArrayList<>(player.getInventory().getContainerSize());
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            stacks.add(player.getInventory().getItem(slot).copy());
        }
        return stacks;
    }

    private static void restoreInventory(ServerPlayer player, List<ItemStack> stacks) {
        player.getInventory().clearContent();
        for (int slot = 0; slot < stacks.size(); slot++) {
            player.getInventory().setItem(slot, stacks.get(slot).copy());
        }
    }

    private static void assertConsistent(GameTestHelper helper, ServerPlayer player) {
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        List<String> violations = SpiritLifecycleAudit.violations(new SpiritLifecycleAudit.Snapshot(
                player.level().dimension().equals(WitcheryDimensions.SPIRIT_WORLD),
                data.isInSpiritWorld(), data.isOtherwhereHearthTraveler(), data.isManifestedGhost(),
                data.getReturnPos().isPresent(), !data.getReturnDimension().isBlank(),
                !data.getOverworldInventorySnbt().isBlank(), data.getManifestationTime()));
        helper.assertTrue(violations.isEmpty(),
                "Spirit lifecycle state must remain consistent: " + String.join(" | ", violations));
    }

    private static void cleanUp(ServerLevel overworld, ServerPlayer... players) {
        ServerLevel spirit = overworld.getServer().getLevel(WitcheryDimensions.SPIRIT_WORLD);
        if (spirit != null) {
            for (ServerPlayer player : players) {
                if (player != null) {
                    spirit.getEntitiesOfClass(NightmareEntity.class,
                                    player.getBoundingBox().inflate(24.0D), nightmare -> nightmare.matchesVictim(player))
                            .forEach(NightmareEntity::discard);
                }
            }
        }
        for (ServerPlayer player : players) {
            if (player == null) {
                continue;
            }
            WitcheryPlayerData data = WitcheryAttachments.get(player);
            if (data.isManifestedGhost()) {
                SpiritWorldHelper.returnGhostPlayerToSpiritWorld(player);
            }
            if (data.isInSpiritWorld()) {
                SpiritWorldHelper.exit(player);
            }
            HeadlessGameTestPlayer.remove(player);
        }
        clearBodies(overworld);
        setReturnChunksForced(overworld, false);
    }

    private static void clearBodies(ServerLevel overworld) {
        for (BlockPos center : List.of(DREAM_POS, NIGHTMARE_POS)) {
            overworld.getEntitiesOfClass(CorpseEntity.class,
                            new net.minecraft.world.phys.AABB(center).inflate(4.0D))
                    .forEach(CorpseEntity::discard);
        }
    }

    private static void setReturnChunksForced(ServerLevel overworld, boolean forced) {
        for (BlockPos center : List.of(DREAM_POS, NIGHTMARE_POS)) {
            overworld.setChunkForced(center.getX() >> 4, center.getZ() >> 4, forced);
        }
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) <= 0.01D;
    }
}
