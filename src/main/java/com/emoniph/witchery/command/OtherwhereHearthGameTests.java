package com.emoniph.witchery.command;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.blocks.entity.KettleBlockEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.ritual.AltarPowerSourceLookup;
import com.emoniph.witchery.ritual.OtherwhereHearthTransposition;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Integrated-server GameTest for the destructive two-world Hearth transaction. */
public final class OtherwhereHearthGameTests {
    private static final BlockPos KETTLE_POS = new BlockPos(256, 80, 256);
    private static final UUID OWNER_ID = UUID.fromString("04d818d4-304d-4d49-a0ba-58d839b340f1");
    private static final UUID BLOCKER_ID = UUID.fromString("9ae38615-2822-40c8-859c-7e2d100514c7");
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROUND_TRIP =
            TEST_FUNCTIONS.register("otherwhere_hearth_round_trip",
                    () -> OtherwhereHearthGameTests::roundTrip);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RECOVERY_RESUME =
            TEST_FUNCTIONS.register("otherwhere_hearth_recovery_resume",
                    () -> OtherwhereHearthGameTests::recoveryResume);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DESTINATION_REFUSAL =
            TEST_FUNCTIONS.register("otherwhere_hearth_destination_refusal",
                    () -> OtherwhereHearthGameTests::destinationRefusal);

    private OtherwhereHearthGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(OtherwhereHearthGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "otherwhere_hearth"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                400,
                0,
                true,
                Rotation.NONE,
                true,
                1,
                1,
                false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "otherwhere_hearth_round_trip"),
                new FunctionGameTestInstance(ROUND_TRIP.getKey(), data));
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "otherwhere_hearth_recovery_resume"),
                new FunctionGameTestInstance(RECOVERY_RESUME.getKey(), data));
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "otherwhere_hearth_destination_refusal"),
                new FunctionGameTestInstance(DESTINATION_REFUSAL.getKey(), data));
    }

    private static void roundTrip(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel spirit = overworld.getServer().getLevel(WitcheryDimensions.SPIRIT_WORLD);
        helper.assertTrue(overworld.dimension().equals(Level.OVERWORLD),
                "Hearth GameTest must run in the Overworld");
        helper.assertTrue(spirit != null, "Spirit World must be loaded");
        if (spirit == null) {
            return;
        }

        ServerPlayer owner = createTestPlayer(overworld, OWNER_ID, "hearth-test");
        BlockPos kettlePos = KETTLE_POS;
        OtherwhereHearthTransposition.Volume volume =
                OtherwhereHearthTransposition.volumeFor(kettlePos);
        owner.snapTo(kettlePos.getX() + 1.5D, kettlePos.getY(), kettlePos.getZ() + 0.5D,
                0.0F, 0.0F);

        boolean activationScheduled = false;
        try {
            OtherwhereHearthQaFixture.buildAt(owner, overworld, spirit, kettlePos,
                    net.minecraft.core.Direction.SOUTH);
            // The paired fixture creates entities in a dimension that is not currently ticking.
            // Let both entity indexes publish those additions before exercising the player-paced rite.
            helper.runAfterDelay(2, () -> startRoundTrip(
                    helper, owner, overworld, spirit, kettlePos, volume));
            activationScheduled = true;
        } finally {
            if (!activationScheduled) {
                cleanUp(owner, overworld, spirit, volume);
            }
        }
    }

    private static void startRoundTrip(GameTestHelper helper, ServerPlayer owner,
                                       ServerLevel overworld, ServerLevel spirit,
                                       BlockPos kettlePos,
                                       OtherwhereHearthTransposition.Volume volume) {
        boolean returnScheduled = false;
        try {
            KettleBlockEntity sourceKettle = kettle(overworld, kettlePos, helper);

            OtherwhereHearthTransposition.Inspection preflight =
                    OtherwhereHearthTransposition.inspect(owner, overworld, kettlePos);
            if (preflight.altarPos() == null) {
                helper.fail("Fixture preflight must locate its travelling altar");
                return;
            }
            BlockPos altarPos = preflight.altarPos();
            int initialPower = altar(overworld, altarPos, helper).getPower();
            int cost = preflight.powerCost();
            helper.assertTrue(preflight.ready(), "Fixture preflight must be ready: " + preflight.blockers());
            helper.assertTrue(initialPower >= cost * 2,
                    "Fixture altar must fund a complete round trip");
            helper.assertTrue(overworld.players().contains(owner)
                            && volume.entityBounds().contains(owner.position()),
                    "Owner must be visible in the source player list and travelling volume; indexed="
                            + (overworld.getEntity(owner.getUUID()) == owner)
                            + ", pos=" + owner.position());

            helper.assertTrue(OtherwhereHearthTransposition.activate(
                    owner, overworld, kettlePos, sourceKettle), "First crossing must complete");
            helper.assertTrue(owner.level() == spirit,
                    "Owner must arrive bodily in the Spirit World; sourcePlayers="
                            + overworld.players().contains(owner) + ", destinationPlayers="
                            + spirit.players().contains(owner) + ", removed=" + owner.isRemoved());
            helper.assertTrue(WitcheryAttachments.get(owner).isOtherwhereHearthTraveler(),
                    "Physical Spirit traveller flag must be set");
            helper.assertFalse(OtherwhereHearthTransposition.hasPendingTransaction(overworld.getServer()),
                    "Completed crossing must clear its journal");

            assertSourceFixture(helper, spirit, overworld, kettlePos, altarPos,
                    initialPower - cost);

            helper.runAfterDelay(20, () -> finishReturn(
                    helper, owner, overworld, spirit, kettlePos, altarPos,
                    cost, volume));
            returnScheduled = true;
        } finally {
            if (!returnScheduled) {
                cleanUp(owner, overworld, spirit, volume);
            }
        }
    }

    /** Finishes the fixed fixture after the round-trip test process was killed with a journal. */
    private static void recoveryResume(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel spirit = overworld.getServer().getLevel(WitcheryDimensions.SPIRIT_WORLD);
        helper.assertTrue(spirit != null, "Spirit World must be loaded");
        if (spirit == null) {
            return;
        }

        OtherwhereHearthTransposition.Volume volume =
                OtherwhereHearthTransposition.volumeFor(KETTLE_POS);
        helper.assertTrue(OtherwhereHearthTransposition.hasPendingTransaction(overworld.getServer()),
                "A crash journal must exist before recovery-resume QA");
        OtherwhereHearthTransposition.PendingEntityCounts pendingCounts =
                OtherwhereHearthTransposition.pendingEntityCounts(overworld.getServer());
        helper.assertTrue(pendingCounts.toSpirit() >= 10,
                "Crash journal must contain the populated Overworld entity manifest: " + pendingCounts);
        helper.assertTrue(pendingCounts.toOverworld() >= 1,
                "Crash journal must contain the Spirit-side entity manifest: " + pendingCounts);
        helper.assertTrue(pendingCounts.players() == 1,
                "Crash journal must contain exactly the owner player: " + pendingCounts);

        // Force PREPARED to converge to WAITING_FOR_PLAYERS if the startup tick has not done so.
        OtherwhereHearthTransposition.recover(overworld.getServer());
        helper.assertTrue(OtherwhereHearthTransposition.hasPendingTransaction(overworld.getServer()),
                "The offline traveller must keep the recovered journal open");
        kettle(spirit, KETTLE_POS, helper);
        AltarBlockEntity recoveredAltar = AltarPowerSourceLookup.findClosest(spirit, KETTLE_POS);
        helper.assertTrue(recoveredAltar != null, "Recovered travelling altar must exist");
        if (recoveredAltar == null) {
            return;
        }
        BlockPos altarPos = recoveredAltar.getBlockPos();
        int powerBeforePlayerResume = recoveredAltar.getPower();

        ServerPlayer owner = createTestPlayer(overworld, OWNER_ID, "hearth-test");
        owner.snapTo(KETTLE_POS.getX() + 1.5D, KETTLE_POS.getY(), KETTLE_POS.getZ() + 0.5D,
                0.0F, 0.0F);
        try {
            OtherwhereHearthTransposition.recover(overworld.getServer());
            helper.assertFalse(OtherwhereHearthTransposition.hasPendingTransaction(overworld.getServer()),
                    "Returning offline traveller must clear the recovered journal");
            helper.assertTrue(owner.level() == spirit,
                    "Recovered offline traveller must arrive in the Spirit World");
            helper.assertTrue(WitcheryAttachments.get(owner).isOtherwhereHearthTraveler(),
                    "Recovered traveller must receive physical Spirit state");
            assertSourceFixture(helper, spirit, overworld, KETTLE_POS, altarPos,
                    powerBeforePlayerResume);
            helper.succeed();
        } finally {
            cleanUp(owner, overworld, spirit, volume);
        }
    }

    private static void destinationRefusal(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel spirit = overworld.getServer().getLevel(WitcheryDimensions.SPIRIT_WORLD);
        helper.assertTrue(spirit != null, "Spirit World must be loaded");
        if (spirit == null) {
            return;
        }
        helper.assertFalse(OtherwhereHearthTransposition.hasPendingTransaction(overworld.getServer()),
                "Destination refusal QA requires a clear journal");

        OtherwhereHearthTransposition.Volume volume =
                OtherwhereHearthTransposition.volumeFor(KETTLE_POS);
        ServerPlayer owner = createTestPlayer(overworld, OWNER_ID, "hearth-test");
        owner.snapTo(KETTLE_POS.getX() + 1.5D, KETTLE_POS.getY(), KETTLE_POS.getZ() + 0.5D,
                0.0F, 0.0F);
        ServerPlayer blocker = null;
        try {
            OtherwhereHearthQaFixture.buildAt(owner, overworld, spirit, KETTLE_POS,
                    net.minecraft.core.Direction.SOUTH);
            OtherwhereHearthTransposition.Inspection ready =
                    OtherwhereHearthTransposition.inspect(owner, overworld, KETTLE_POS);
            helper.assertTrue(ready.ready(), "Refusal fixture must begin ready: " + ready.blockers());
            if (ready.altarPos() == null) {
                helper.fail("Refusal fixture must locate its travelling altar");
                return;
            }
            int powerBefore = altar(overworld, ready.altarPos(), helper).getPower();

            blocker = createTestPlayer(spirit, BLOCKER_ID, "hearth-blocker");
            blocker.snapTo(KETTLE_POS.getX() + 2.5D, KETTLE_POS.getY(), KETTLE_POS.getZ() + 0.5D,
                    0.0F, 0.0F);
            OtherwhereHearthTransposition.Inspection blocked =
                    OtherwhereHearthTransposition.inspect(owner, overworld, KETTLE_POS);
            helper.assertTrue(blocked.blockers().stream()
                            .anyMatch(reason -> reason.contains("Destination contains player")),
                    "Dry-run must report the destination player");
            helper.assertFalse(OtherwhereHearthTransposition.activate(
                    owner, overworld, KETTLE_POS, kettle(overworld, KETTLE_POS, helper)),
                    "Occupied destination must refuse activation");
            helper.assertFalse(OtherwhereHearthTransposition.hasPendingTransaction(overworld.getServer()),
                    "Refused activation must not create a journal");
            helper.assertTrue(altar(overworld, ready.altarPos(), helper).getPower() == powerBefore,
                    "Refused activation must not spend altar power");
            helper.assertTrue(overworld.getBlockEntity(KETTLE_POS) instanceof KettleBlockEntity,
                    "Refused activation must leave the source structure in place");
            helper.succeed();
        } finally {
            removeTestPlayer(blocker);
            cleanUp(owner, overworld, spirit, volume);
        }
    }

    private static void finishReturn(GameTestHelper helper, ServerPlayer owner,
                                     ServerLevel overworld, ServerLevel spirit,
                                     BlockPos kettlePos, BlockPos altarPos,
                                     int cost,
                                     OtherwhereHearthTransposition.Volume volume) {
        try {
            KettleBlockEntity spiritKettle = kettle(spirit, kettlePos, helper);
            int powerBeforeReturn = altar(spirit, altarPos, helper).getPower();
            helper.assertTrue(OtherwhereHearthTransposition.activate(
                    owner, spirit, kettlePos, spiritKettle), "Return crossing must complete");
            helper.assertTrue(owner.level() == overworld,
                    "Owner must return bodily to the Overworld");
            helper.assertFalse(WitcheryAttachments.get(owner).isOtherwhereHearthTraveler(),
                    "Physical Spirit traveller flag must clear on return");
            helper.assertFalse(OtherwhereHearthTransposition.hasPendingTransaction(overworld.getServer()),
                    "Round trip must finish without a journal");

            assertSourceFixture(helper, overworld, spirit, kettlePos, altarPos,
                    powerBeforeReturn - cost);
            helper.succeed();
        } finally {
            cleanUp(owner, overworld, spirit, volume);
        }
    }

    private static void cleanUp(ServerPlayer owner, ServerLevel overworld, ServerLevel spirit,
                                OtherwhereHearthTransposition.Volume volume) {
        OtherwhereHearthQaFixture.removeNonPlayers(overworld, volume);
        OtherwhereHearthQaFixture.removeNonPlayers(spirit, volume);
        OtherwhereHearthQaFixture.clearVolume(overworld, volume);
        OtherwhereHearthQaFixture.clearVolume(spirit, volume);
        removeTestPlayer(owner);
    }

    private static ServerPlayer createTestPlayer(ServerLevel level, UUID id, String name) {
        return HeadlessGameTestPlayer.create(level, id, name);
    }

    private static void removeTestPlayer(ServerPlayer player) {
        HeadlessGameTestPlayer.remove(player);
    }

    private static void assertSourceFixture(GameTestHelper helper, ServerLevel sourceSide,
                                            ServerLevel destinationSide, BlockPos kettlePos,
                                            BlockPos altarPos, int expectedPower) {
        KettleBlockEntity kettle = kettle(sourceSide, kettlePos, helper);
        helper.assertTrue(kettle.isOtherwhereHearthBound(), "Bound Kettle must survive snapshot placement");
        helper.assertTrue(sourceSide.getBlockState(kettlePos.offset(8, 0, 7)).is(Blocks.WATER),
                "Water source must survive the crossing");
        helper.assertTrue(sourceSide.getBlockState(kettlePos.offset(9, 0, 3)).is(Blocks.REDSTONE_LAMP),
                "Redstone fixture must survive the crossing");
        helper.assertTrue(container(sourceSide, kettlePos.offset(9, 0, -7), helper)
                        .countItem(Items.DIAMOND) == 3,
                "Source chest contents must survive the crossing");
        helper.assertTrue(container(destinationSide, kettlePos.offset(-9, 0, -7), helper)
                        .countItem(Items.AMETHYST_SHARD) == 11,
                "Destination chest contents must swap to the opposite world");
        helper.assertTrue(destinationSide.getBlockState(kettlePos.offset(-8, 0, 7)).is(Blocks.LAVA),
                "Destination lava source must swap to the opposite world");
        helper.assertTrue(altar(sourceSide, altarPos, helper).getPower() == expectedPower,
                "Each crossing must charge the travelling altar exactly once");

        OtherwhereHearthTransposition.loadVolumeForInspection(sourceSide, kettlePos);
        OtherwhereHearthTransposition.loadVolumeForInspection(destinationSide, kettlePos);
        Entity vehicle = namedEntity(sourceSide, kettlePos, "Round Trip Vehicle");
        Entity villager = namedEntity(sourceSide, kettlePos, "Round Trip Passenger");
        helper.assertTrue(vehicle instanceof AbstractMinecart, "Named vehicle must survive the crossing");
        helper.assertTrue(villager != null && villager.getVehicle() == vehicle,
                "Passenger-to-vehicle relationship must be restored");
        helper.assertTrue(namedEntity(sourceSide, kettlePos, "Round Trip Dog") != null,
                "Tamed dog must survive the crossing");
        helper.assertTrue(namedEntity(sourceSide, kettlePos, "Round Trip Cat") != null,
                "Tamed cat must survive the crossing");
        helper.assertTrue(namedEntity(sourceSide, kettlePos, "Round Trip Item") != null,
                "Loose item entity must survive the crossing");
        Entity destinationMarker = namedEntity(destinationSide, kettlePos, "Spirit-side Marker");
        helper.assertTrue(destinationMarker != null,
                "Destination entity must swap to the opposite world; destination="
                        + describeEntities(destinationSide, kettlePos) + ", source="
                        + describeEntities(sourceSide, kettlePos));
    }

    private static KettleBlockEntity kettle(ServerLevel level, BlockPos pos, GameTestHelper helper) {
        if (level.getBlockEntity(pos) instanceof KettleBlockEntity kettle) {
            return kettle;
        }
        helper.fail("Missing Kettle block entity in " + level.dimension().identifier());
        throw new IllegalStateException("unreachable");
    }

    private static AltarBlockEntity altar(ServerLevel level, BlockPos pos, GameTestHelper helper) {
        if (level.getBlockEntity(pos) instanceof AltarBlockEntity altar) {
            return altar;
        }
        helper.fail("Missing Altar block entity in " + level.dimension().identifier());
        throw new IllegalStateException("unreachable");
    }

    private static RandomizableContainerBlockEntity container(
            ServerLevel level, BlockPos pos, GameTestHelper helper) {
        if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity container) {
            return container;
        }
        helper.fail("Missing container at " + pos + " in " + level.dimension().identifier());
        throw new IllegalStateException("unreachable");
    }

    private static Entity namedEntity(ServerLevel level, BlockPos center, String name) {
        OtherwhereHearthTransposition.Volume volume =
                OtherwhereHearthTransposition.volumeFor(center);
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, volume.entityBounds(),
                entity -> entity.hasCustomName() && name.equals(entity.getCustomName().getString()));
        return entities.isEmpty() ? null : entities.getFirst();
    }

    private static List<String> describeEntities(ServerLevel level, BlockPos center) {
        OtherwhereHearthTransposition.Volume volume =
                OtherwhereHearthTransposition.volumeFor(center);
        return level.getEntitiesOfClass(Entity.class, volume.entityBounds()).stream()
                .map(entity -> entity.getType().toShortString() + ":"
                        + (entity.hasCustomName() ? entity.getCustomName().getString() : "unnamed")
                        + ":removed=" + entity.isRemoved())
                .sorted()
                .toList();
    }
}
