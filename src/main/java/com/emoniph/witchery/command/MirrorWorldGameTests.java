package com.emoniph.witchery.command;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.MirrorBlock;
import com.emoniph.witchery.blocks.entity.MirrorBlockEntity;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.ReflectionEntity;
import com.emoniph.witchery.entity.MirrorDecoyEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.world.MirrorHelper;
import com.emoniph.witchery.world.MirrorRoomData;
import com.emoniph.witchery.world.MirrorWorldCertification;
import com.emoniph.witchery.world.MirrorWorldLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Integrated Mirror World isolation, guardian, route, and decoy lifecycle fixture. */
public final class MirrorWorldGameTests {
    private static final BlockPos FIRST_MIRROR = new BlockPos(384, 80, 384);
    private static final BlockPos SECOND_MIRROR = new BlockPos(400, 80, 384);
    private static final UUID FIRST_PLAYER_ID = UUID.fromString("e6af078b-ff31-46ac-ae41-346a32364312");
    private static final UUID SECOND_PLAYER_ID = UUID.fromString("39df7398-d237-47dc-9f5b-a772f11dd051");
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LIFECYCLE =
            TEST_FUNCTIONS.register("mirror_world_lifecycle", () -> MirrorWorldGameTests::lifecycle);

    private MirrorWorldGameTests() {}

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(MirrorWorldGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "mirror_world"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 300, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "mirror_world_lifecycle"),
                new FunctionGameTestInstance(LIFECYCLE.getKey(), data));
    }

    private static void lifecycle(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel mirrorWorld = overworld.getServer().getLevel(WitcheryDimensions.MIRROR_WORLD);
        helper.assertTrue(mirrorWorld != null, "Mirror World dimension must be loaded");
        if (mirrorWorld == null) {
            return;
        }

        prepareSourceMirror(overworld, FIRST_MIRROR);
        prepareSourceMirror(overworld, SECOND_MIRROR);
        loadExistingRoom(overworld, mirrorWorld, FIRST_MIRROR);
        loadExistingRoom(overworld, mirrorWorld, SECOND_MIRROR);
        helper.runAfterDelay(2, () -> beginLifecycle(helper, overworld, mirrorWorld));
    }

    private static void beginLifecycle(GameTestHelper helper, ServerLevel overworld,
                                       ServerLevel mirrorWorld) {
        cleanExistingRoomEntities(overworld, mirrorWorld, FIRST_MIRROR);
        cleanExistingRoomEntities(overworld, mirrorWorld, SECOND_MIRROR);

        ServerPlayer first = createTestPlayer(overworld, FIRST_PLAYER_ID, "mirror-first");
        ServerPlayer second = createTestPlayer(overworld, SECOND_PLAYER_ID, "mirror-second");
        first.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
        first.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        first.snapTo(FIRST_MIRROR.getX() + 1.5D, FIRST_MIRROR.getY(),
                FIRST_MIRROR.getZ() + 0.5D, 0.0F, 0.0F);
        second.snapTo(SECOND_MIRROR.getX() + 1.5D, SECOND_MIRROR.getY(),
                SECOND_MIRROR.getZ() + 0.5D, 0.0F, 0.0F);

        boolean scheduled = false;
        try {
            helper.assertTrue(MirrorHelper.enterMirrorWorld(first, overworld, FIRST_MIRROR),
                    "First mirror crossing must succeed");
            helper.assertTrue(MirrorHelper.enterMirrorWorld(second, overworld, SECOND_MIRROR),
                    "Second mirror crossing must succeed");
            setFixtureChunksForced(overworld, mirrorWorld, true);
            helper.runAfterDelay(3, () -> verifyIsolation(
                    helper, overworld, mirrorWorld, first, second));
            scheduled = true;
        } finally {
            if (!scheduled) {
                removeTestPlayer(first);
                removeTestPlayer(second);
                setFixtureChunksForced(overworld, mirrorWorld, false);
            }
        }
    }

    private static void verifyIsolation(GameTestHelper helper, ServerLevel overworld,
                                        ServerLevel mirrorWorld, ServerPlayer first,
                                        ServerPlayer second) {
        boolean scheduled = false;
        try {
            MirrorBlockEntity firstSource = mirror(overworld, FIRST_MIRROR, helper);
            MirrorBlockEntity secondSource = mirror(overworld, SECOND_MIRROR, helper);
            int firstIndex = roomIndex(firstSource);
            int secondIndex = roomIndex(secondSource);
            helper.assertTrue(firstIndex >= 0 && secondIndex >= 0 && firstIndex != secondIndex,
                    "Two source mirrors must own distinct room indexes");
            helper.assertTrue(WitcheryAttachments.get(first).getMirrorRouteDepth() == 1
                            && WitcheryAttachments.get(second).getMirrorRouteDepth() == 1,
                    "Each player must carry one independent persisted route");

            ReflectionEntity firstGuardian = guardian(mirrorWorld, firstIndex, helper);
            ReflectionEntity secondGuardian = guardian(mirrorWorld, secondIndex, helper);
            helper.assertTrue(FIRST_PLAYER_ID.equals(firstGuardian.getOwnerUUID())
                            && SECOND_PLAYER_ID.equals(secondGuardian.getOwnerUUID()),
                    "Each guardian must retain its own entering player identity");
            helper.assertTrue(firstGuardian.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.DIAMOND_SWORD)
                            && firstGuardian.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE),
                    "Guardian equipment slots must mirror owner equipment for rendering");

            MirrorWorldCertification.Report initial = MirrorWorldCertification.audit(mirrorWorld);
            helper.assertTrue(initial.passed(), describe(initial));

            clearItems(mirrorWorld, firstIndex);
            ReflectionEntity remainingReflection = new ReflectionEntity(
                    com.emoniph.witchery.init.WitcheryEntities.REFLECTION.get(), mirrorWorld);
            BlockPos firstCenter = roomCenter(firstIndex);
            remainingReflection.snapTo(firstCenter.getX() + 1.5D, firstCenter.getY(),
                    firstCenter.getZ() + 0.5D, 0.0F, 0.0F);
            remainingReflection.setOwner(first);
            remainingReflection.setFreeSpawn(true);
            mirrorWorld.addFreshEntity(remainingReflection);

            firstGuardian.setHealth(1.0F);
            firstGuardian.invulnerableTime = 0;
            helper.assertTrue(firstGuardian.hurtServer(mirrorWorld,
                            first.damageSources().playerAttack(first), 10.0F),
                    "First guardian must accept its killing blow");
            helper.assertFalse(firstSource.isDemonKilled(),
                    "A room must remain inhabited while any Reflection survives inside it");
            helper.assertTrue(remainingReflection.isRoomGuardianFor(firstIndex),
                    "The surviving Reflection must inherit the room guardian lifecycle");

            remainingReflection.setHealth(1.0F);
            remainingReflection.invulnerableTime = 0;
            helper.assertTrue(remainingReflection.hurtServer(mirrorWorld,
                            first.damageSources().playerAttack(first), 10.0F),
                    "The inherited room guardian must accept its killing blow");
            helper.assertTrue(firstSource.isDemonKilled(),
                    "Defeating every Reflection in room one must clear its exact source mirror");
            helper.assertFalse(secondSource.isDemonKilled(),
                    "Defeating room one must not clear room two");
            helper.assertTrue(secondGuardian.isAlive(),
                    "The isolated second guardian must remain alive");
            helper.runAfterDelay(1, () -> verifyAfterFirstKill(helper, overworld, mirrorWorld,
                    first, second, firstIndex, secondIndex, secondGuardian));
            scheduled = true;
        } finally {
            if (!scheduled) {
                removeTestPlayer(first);
                removeTestPlayer(second);
                setFixtureChunksForced(overworld, mirrorWorld, false);
            }
        }
    }

    private static void verifyAfterFirstKill(GameTestHelper helper, ServerLevel overworld,
                                             ServerLevel mirrorWorld, ServerPlayer first,
                                             ServerPlayer second, int firstIndex, int secondIndex,
                                             ReflectionEntity secondGuardian) {
        boolean scheduled = false;
        try {
            MirrorBlockEntity secondSource = mirror(overworld, SECOND_MIRROR, helper);
            assertGuardianLoot(helper, mirrorWorld, firstIndex);
            BlockPos firstInner = innerMirror(firstIndex);
            helper.assertTrue(MirrorHelper.exitMirrorWorld(first, mirrorWorld, firstInner),
                    "First player must return beside the linked source mirror");
            helper.assertTrue(first.level() == overworld
                            && WitcheryAttachments.get(first).getMirrorRouteDepth() == 0,
                    "Successful exit must consume the first route");
            helper.assertTrue(MirrorHelper.enterMirrorWorld(first, overworld, FIRST_MIRROR),
                    "Cleared source mirror must remain usable");
            helper.assertTrue(guardians(mirrorWorld, firstIndex).isEmpty(),
                    "Cleared-room re-entry must not respawn a guardian");
            helper.assertTrue(MirrorHelper.exitMirrorWorld(first, mirrorWorld, firstInner),
                    "Cleared-room return must remain safe");

            ServerLevel carrierLevel = overworld.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
            helper.assertTrue(carrierLevel != null, "The carrier test requires the Nether");
            if (carrierLevel == null) return;
            prepareCarrierPlatform(carrierLevel, first.blockPosition());
            first.teleportTo(carrierLevel, first.getX(), first.getY(), first.getZ(),
                    Set.of(), first.getYRot(), first.getXRot(), false);
            helper.assertTrue(first.level() == carrierLevel && carrierLevel.players().contains(first),
                    "The linked mirror carrier must be registered outside the source dimension");

            ItemStack carriedMirror = new ItemStack(WitcheryItems.MIRROR.get());
            carriedMirror.set(DataComponents.BLOCK_ENTITY_DATA,
                    TypedEntityData.of(WitcheryBlockEntities.MIRROR.get(),
                            secondSource.saveCustomOnly(overworld.registryAccess())));
            helper.assertTrue(com.emoniph.witchery.item.MirrorItem.roomIndex(carriedMirror) == secondIndex,
                    "The carried mirror item must preserve its exact room link");
            helper.assertTrue(first.getInventory().add(carriedMirror),
                    "The outside carrier must accept the linked mirror item");
            helper.assertTrue(hasLinkedMirror(first, secondIndex),
                    "The inventory copy must retain the linked mirror room component");
            MirrorHelper.disconnectSourceMirror(overworld, secondSource);
            MirrorBlockEntity secondInner = mirror(mirrorWorld, innerMirror(secondIndex), helper);
            helper.assertFalse(secondInner.isConnected(),
                    "Disconnecting the source must mark the linked inner mirror disconnected");
            BlockPos secondInnerPos = innerMirror(secondIndex);
            helper.assertTrue(MirrorWorldLayout.indexForRoom(
                            Math.floorDiv(secondInnerPos.getX(), 16),
                            MirrorWorldLayout.sectionY(secondInnerPos.getY()),
                            Math.floorDiv(secondInnerPos.getZ(), 16)) == secondIndex,
                    "The inner mirror position must resolve its carried-link room index");
            helper.assertTrue(MirrorHelper.exitMirrorWorld(second, mirrorWorld, innerMirror(secondIndex)),
                    "A disconnected inner mirror must follow its linked item outside Mirror World");
            helper.runAfterDelay(1, () -> verifyCarrierLanding(helper, overworld, mirrorWorld,
                    first, second, carrierLevel, secondIndex, secondGuardian, secondSource));
            scheduled = true;
        } finally {
            if (!scheduled) {
                removeTestPlayer(first);
                removeTestPlayer(second);
                setFixtureChunksForced(overworld, mirrorWorld, false);
            }
        }
    }

    private static void verifyCarrierLanding(GameTestHelper helper, ServerLevel overworld,
                                             ServerLevel mirrorWorld, ServerPlayer first,
                                             ServerPlayer second, ServerLevel carrierLevel,
                                             int secondIndex,
                                             ReflectionEntity secondGuardian,
                                             MirrorBlockEntity secondSource) {
        ServerPlayer reconnect = null;
        boolean scheduled = false;
        try {
            helper.assertTrue(second.level() == carrierLevel,
                    "The linked item carrier's dimension must become the disconnected room's exit target");
            removeLinkedMirror(first, secondIndex);
            MirrorHelper.reconnectSourceMirror(overworld, SECOND_MIRROR, secondSource);
            second.teleportTo(overworld, SECOND_MIRROR.getX() + 1.5D, SECOND_MIRROR.getY(),
                    SECOND_MIRROR.getZ() + 0.5D, Set.of(), 0.0F, 0.0F, false);
            helper.assertTrue(MirrorHelper.enterMirrorWorld(second, overworld, SECOND_MIRROR),
                    "Replacing the linked source state must reconnect the same room");

            clearItems(mirrorWorld, secondIndex);
            MirrorDecoyEntity killedDecoy = new MirrorDecoyEntity(
                    com.emoniph.witchery.init.WitcheryEntities.MIRROR_DECOY.get(), mirrorWorld);
            BlockPos secondCenter = roomCenter(secondIndex);
            killedDecoy.snapTo(secondCenter.getX() + 1.5D, secondCenter.getY(),
                    secondCenter.getZ() + 0.5D, 0.0F, 0.0F);
            killedDecoy.setOwner(second);
            mirrorWorld.addFreshEntity(killedDecoy);
            killedDecoy.setHealth(1.0F);
            killedDecoy.invulnerableTime = 0;
            killedDecoy.hurtServer(mirrorWorld, second.damageSources().playerAttack(second), 10.0F);
            helper.assertFalse(secondSource.isDemonKilled(),
                    "A killed Duplication Grenade decoy must not clear a room");
            helper.assertTrue(countItem(mirrorWorld, secondIndex, WitcheryItems.DEMON_HEART.get()) == 0,
                    "A killed decoy must not drop guardian loot");

            MirrorDecoyEntity expiredDecoy = new MirrorDecoyEntity(
                    com.emoniph.witchery.init.WitcheryEntities.MIRROR_DECOY.get(), mirrorWorld);
            expiredDecoy.snapTo(secondCenter.getX() + 2.5D, secondCenter.getY(),
                    secondCenter.getZ() + 0.5D, 0.0F, 0.0F);
            expiredDecoy.setOwner(second);
            expiredDecoy.setLifetimeTicks(1);
            mirrorWorld.addFreshEntity(expiredDecoy);
            expiredDecoy.tick();
            helper.assertTrue(expiredDecoy.isRemoved() && !secondSource.isDemonKilled(),
                    "An expired decoy must disappear without touching guardian state");

            WitcheryPlayerData disconnectedData = new WitcheryPlayerData();
            disconnectedData.copyFrom(WitcheryAttachments.get(second), true);
            removeTestPlayer(second);
            second = null;
            reconnect = createTestPlayer(mirrorWorld, SECOND_PLAYER_ID, "mirror-second");
            WitcheryAttachments.get(reconnect).copyFrom(disconnectedData, true);
            reconnect.snapTo(secondCenter.getX() + 0.5D, secondCenter.getY(),
                    secondCenter.getZ() + 0.5D, 0.0F, 0.0F);
            helper.assertTrue(WitcheryAttachments.get(reconnect).getMirrorRouteDepth() == 1,
                    "A reconnect must retain its Mirror World route");
            helper.assertTrue(MirrorHelper.exitMirrorWorld(reconnect, mirrorWorld, innerMirror(secondIndex)),
                    "A reconnected player must resolve the persisted source route");
            helper.assertTrue(reconnect.level() == overworld,
                    "A reconnect exit must resolve the source dimension immediately");
            ServerPlayer settledReconnect = reconnect;
            helper.runAfterDelay(1, () -> finishAfterReconnect(helper, overworld, mirrorWorld,
                    first, settledReconnect, secondGuardian));
            scheduled = true;
        } finally {
            if (!scheduled) {
                removeTestPlayer(first);
                removeTestPlayer(second);
                removeTestPlayer(reconnect);
                setFixtureChunksForced(overworld, mirrorWorld, false);
            }
        }
    }

    private static void finishAfterReconnect(GameTestHelper helper, ServerLevel overworld,
                                             ServerLevel mirrorWorld, ServerPlayer first,
                                             ServerPlayer reconnect,
                                             ReflectionEntity secondGuardian) {
        try {
            BlockPos landing = MirrorHelper.findLandingBesideMirror(overworld, SECOND_MIRROR);
            helper.assertTrue(landing != null && landing.distManhattan(SECOND_MIRROR) <= 2,
                    "Reconnect exit must resolve a safe landing beside the correct source mirror");
            helper.assertTrue(WitcheryAttachments.get(reconnect).getMirrorRouteDepth() == 0,
                    "A reconnect exit must consume its persisted route");

            secondGuardian.setHealth(1.0F);
            secondGuardian.invulnerableTime = 0;
            secondGuardian.hurtServer(mirrorWorld, mirrorWorld.damageSources().generic(), 10.0F);
            MirrorBlockEntity secondSource = mirror(overworld, SECOND_MIRROR, helper);
            helper.assertTrue(secondSource.isDemonKilled(),
                    "The real second guardian must still clear its own mirror");

            MirrorWorldCertification.Report completed = MirrorWorldCertification.audit(mirrorWorld);
            helper.assertTrue(completed.passed(), describe(completed));
            helper.succeed();
        } finally {
            removeTestPlayer(first);
            removeTestPlayer(reconnect);
            setFixtureChunksForced(overworld, mirrorWorld, false);
        }
    }

    private static void prepareSourceMirror(ServerLevel level, BlockPos foot) {
        for (int x = foot.getX() - 2; x <= foot.getX() + 2; x++) {
            for (int z = foot.getZ() - 2; z <= foot.getZ() + 2; z++) {
                level.setBlock(new BlockPos(x, foot.getY() - 1, z), Blocks.BEDROCK.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, foot.getY(), z), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, foot.getY() + 1, z), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        BlockState mirror = WitcheryBlocks.MIRROR.get().defaultBlockState()
                .setValue(MirrorBlock.FACING, Direction.EAST)
                .setValue(MirrorBlock.PART, BedPart.FOOT)
                .setValue(MirrorBlock.UNBREAKABLE, false);
        level.setBlockAndUpdate(foot, mirror);
        level.setBlockAndUpdate(foot.above(), mirror.setValue(MirrorBlock.PART, BedPart.HEAD));
    }

    private static void prepareCarrierPlatform(ServerLevel level, BlockPos feet) {
        for (int x = feet.getX() - 2; x <= feet.getX() + 2; x++) {
            for (int z = feet.getZ() - 2; z <= feet.getZ() + 2; z++) {
                level.setBlock(new BlockPos(x, feet.getY() - 1, z), Blocks.BEDROCK.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, feet.getY(), z), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, feet.getY() + 1, z), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static void cleanExistingRoomEntities(ServerLevel overworld, ServerLevel mirrorWorld,
                                                  BlockPos sourcePos) {
        MirrorRoomData.get(overworld).roomForSource(
                        overworld.dimension().identifier().toString(), sourcePos)
                .ifPresent(room -> {
                    mirrorWorld.getChunk(room.chunkX(), room.chunkZ());
                    mirrorWorld.getEntitiesOfClass(
                                    Entity.class, MirrorHelper.roomBounds(room.chunkX(), room.sectionY(), room.chunkZ()))
                            .forEach(Entity::discard);
                    MirrorRoomData.get(overworld).setGuardian(room.index(), null);
                });
    }

    private static void loadExistingRoom(ServerLevel overworld, ServerLevel mirrorWorld,
                                         BlockPos sourcePos) {
        MirrorRoomData.get(overworld).roomForSource(
                        overworld.dimension().identifier().toString(), sourcePos)
                .ifPresent(room -> mirrorWorld.getChunk(room.chunkX(), room.chunkZ()));
    }

    private static void setFixtureChunksForced(ServerLevel overworld, ServerLevel mirrorWorld,
                                               boolean forced) {
        for (BlockPos source : List.of(FIRST_MIRROR, SECOND_MIRROR)) {
            MirrorRoomData.get(overworld).roomForSource(
                            overworld.dimension().identifier().toString(), source)
                    .ifPresent(room -> mirrorWorld.setChunkForced(
                            room.chunkX(), room.chunkZ(), forced));
        }
    }

    private static MirrorBlockEntity mirror(ServerLevel level, BlockPos pos, GameTestHelper helper) {
        MirrorBlockEntity mirror = MirrorHelper.getMirrorBlockEntity(level, pos);
        helper.assertTrue(mirror != null, "Expected Mirror block entity at " + pos);
        if (mirror == null) throw new IllegalStateException("Missing test mirror");
        return mirror;
    }

    private static int roomIndex(MirrorBlockEntity mirror) {
        if (!mirror.hasRoom()) return -1;
        int[] room = mirror.getRoom();
        return MirrorWorldLayout.indexForRoom(room[0], room[1], room[2]);
    }

    private static ReflectionEntity guardian(ServerLevel level, int roomIndex, GameTestHelper helper) {
        List<ReflectionEntity> guardians = guardians(level, roomIndex);
        helper.assertTrue(guardians.size() == 1,
                "Expected one guardian for room " + roomIndex + ", found " + guardians.size());
        if (guardians.isEmpty()) throw new IllegalStateException("Missing test guardian");
        return guardians.getFirst();
    }

    private static List<ReflectionEntity> guardians(ServerLevel level, int roomIndex) {
        MirrorWorldLayout.RoomCoordinates room = MirrorWorldLayout.roomForIndex(roomIndex);
        return level.getEntitiesOfClass(ReflectionEntity.class,
                MirrorHelper.roomBounds(room.chunkX(), room.sectionY(), room.chunkZ()),
                reflection -> reflection.isAlive() && reflection.isRoomGuardianFor(roomIndex));
    }

    private static BlockPos roomCenter(int roomIndex) {
        MirrorWorldLayout.RoomCoordinates room = MirrorWorldLayout.roomForIndex(roomIndex);
        return MirrorWorldLayout.roomCenter(room.chunkX(), room.sectionY(), room.chunkZ());
    }

    private static BlockPos innerMirror(int roomIndex) {
        MirrorWorldLayout.RoomCoordinates room = MirrorWorldLayout.roomForIndex(roomIndex);
        return MirrorWorldLayout.innerMirrorFoot(room.chunkX(), room.sectionY(), room.chunkZ());
    }

    private static void assertGuardianLoot(GameTestHelper helper, ServerLevel level, int roomIndex) {
        helper.assertTrue(countItem(level, roomIndex, WitcheryItems.DEMON_HEART.get()) == 2,
                "Each of the two defeated Reflections must drop exactly one Demon Heart");
        helper.assertTrue(countItem(level, roomIndex, Items.DIAMOND_SWORD) == 0
                        && countItem(level, roomIndex, Items.IRON_CHESTPLATE) == 0,
                "Copied owner equipment must never become loot");
    }

    private static int countItem(ServerLevel level, int roomIndex, net.minecraft.world.item.Item item) {
        MirrorWorldLayout.RoomCoordinates room = MirrorWorldLayout.roomForIndex(roomIndex);
        return level.getEntitiesOfClass(ItemEntity.class,
                        MirrorHelper.roomBounds(room.chunkX(), room.sectionY(), room.chunkZ()))
                .stream().mapToInt(entity -> entity.getItem().is(item) ? entity.getItem().getCount() : 0).sum();
    }

    private static void removeLinkedMirror(ServerPlayer player, int roomIndex) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (com.emoniph.witchery.item.MirrorItem.roomIndex(
                    player.getInventory().getItem(slot)) == roomIndex) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
                return;
            }
        }
    }

    private static boolean hasLinkedMirror(ServerPlayer player, int roomIndex) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (com.emoniph.witchery.item.MirrorItem.roomIndex(
                    player.getInventory().getItem(slot)) == roomIndex) {
                return true;
            }
        }
        return false;
    }

    private static void clearItems(ServerLevel level, int roomIndex) {
        MirrorWorldLayout.RoomCoordinates room = MirrorWorldLayout.roomForIndex(roomIndex);
        level.getEntitiesOfClass(ItemEntity.class,
                        MirrorHelper.roomBounds(room.chunkX(), room.sectionY(), room.chunkZ()))
                .forEach(Entity::discard);
    }

    private static ServerPlayer createTestPlayer(ServerLevel level, UUID id, String name) {
        return HeadlessGameTestPlayer.create(level, id, name);
    }

    private static void removeTestPlayer(ServerPlayer player) {
        HeadlessGameTestPlayer.remove(player);
    }

    private static String describe(MirrorWorldCertification.Report report) {
        return "Mirror certificate has " + report.mismatchCount() + " mismatch(es): "
                + String.join(" | ", report.failures());
    }
}
