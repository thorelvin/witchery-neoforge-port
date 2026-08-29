package com.emoniph.witchery.entity;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.command.HeadlessGameTestPlayer;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.TaglockKitItem;
import com.emoniph.witchery.item.WaystoneItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Consumer;

/** Dedicated-server coverage for taglock retrieval, persistence, and waystone transport. */
public final class FlyingMonkeyCourierGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> COURIER_PARITY =
            TEST_FUNCTIONS.register("flying_monkey_courier_parity",
                    () -> FlyingMonkeyCourierGameTests::courierParity);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CROSS_DIMENSION =
            TEST_FUNCTIONS.register("flying_monkey_cross_dimension",
                    () -> FlyingMonkeyCourierGameTests::crossDimension);

    private FlyingMonkeyCourierGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(FlyingMonkeyCourierGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "flying_monkey_courier"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 4000, 0,
                true, Rotation.NONE, false, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "flying_monkey_courier_parity"),
                new FunctionGameTestInstance(COURIER_PARITY.getKey(), data));
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "flying_monkey_cross_dimension"),
                new FunctionGameTestInstance(CROSS_DIMENSION.getKey(), data));
    }

    private static void courierParity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos home = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos animalStart = home.offset(6, 0, 0);
        BlockPos playerStart = home.offset(6, 0, 3);
        BlockPos waystoneDestination = home.offset(10, 0, 0);
        BlockPos bloodedDestination = home.offset(-3, 0, 0);
        buildFloor(level, home.offset(-4, -1, -4), home.offset(14, -1, 4));

        ServerPlayer owner = HeadlessGameTestPlayer.create(
                level, UUID.fromString("c419066d-1fc8-4e2d-8474-840721f08034"), "MonkeyCourierOwner");
        ServerPlayer playerTarget = HeadlessGameTestPlayer.create(
                level, UUID.fromString("d09c9cc2-130e-45c1-932b-e9885045b4d7"), "MonkeyCourierTarget");
        FlyingMonkeyEntity monkey = WitcheryEntities.FLYING_MONKEY.get().create(level, EntitySpawnReason.TRIGGERED);
        Pig target = EntityType.PIG.create(level, EntitySpawnReason.TRIGGERED);
        long[] nextWaystoneOrderAt = {Long.MIN_VALUE};
        if (monkey == null || target == null) {
            HeadlessGameTestPlayer.remove(owner);
            HeadlessGameTestPlayer.remove(playerTarget);
            throw new IllegalStateException("Unable to create Flying Monkey courier fixture");
        }

        owner.snapTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 0.0F, 0.0F);
        playerTarget.snapTo(playerStart.getX() + 0.5D, playerStart.getY(), playerStart.getZ() + 0.5D,
                0.0F, 0.0F);
        owner.setGameMode(GameType.SURVIVAL);
        playerTarget.setGameMode(GameType.SURVIVAL);
        monkey.snapTo(home.getX() + 0.5D, home.getY() + 1.0D, home.getZ() + 0.5D, 0.0F, 0.0F);
        target.snapTo(animalStart.getX() + 0.5D, animalStart.getY(), animalStart.getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(monkey);
        level.addFreshEntity(target);
        monkey.tame(owner);
        monkey.setOrderedToSit(false);

        ItemStack taglock = TaglockKitItem.filledEntityTaglock(target);
        owner.setItemInHand(InteractionHand.MAIN_HAND, taglock);
        InteractionResult commandResult = monkey.mobInteract(owner, InteractionHand.MAIN_HAND);
        helper.assertTrue(commandResult.consumesAction(), "A filled creature taglock must issue a courier order");
        helper.assertTrue(taglock.isEmpty(), "An accepted taglock courier order must consume one taglock");
        helper.assertTrue(monkey.getCourierState() == FlyingMonkeyEntity.CourierState.FETCHING,
                "An accepted taglock must put the monkey into FETCHING state");
        helper.assertTrue(monkey.getCourierTargetUuid().filter(target.getUUID()::equals).isPresent(),
                "The courier order must retain the exact bound creature UUID");
        assertCourierPersistence(helper, level, monkey, target.getUUID());

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(monkey.getCourierState() == FlyingMonkeyEntity.CourierState.IDLE,
                            "The monkey has not completed its taglock round trip: state="
                                    + monkey.getCourierState() + ", monkey=" + monkey.position()
                                    + ", target=" + target.position() + ", velocity=" + monkey.getDeltaMovement()
                                    + ", passenger=" + target.isPassenger() + ", effectiveAi=" + monkey.isEffectiveAi()
                                    + ", noAi=" + monkey.isNoAi() + ", sitting=" + monkey.isOrderedToSit()
                                    + ", horizontalCollision=" + monkey.horizontalCollision);
                    helper.assertTrue(!target.isPassenger(), "The returned creature must be dismounted");
                    helper.assertTrue(target.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D) <= 16.0D,
                            "The returned creature must be placed safely near the command origin");
                })
                .thenExecute(() -> {
                    ItemStack waystone = new ItemStack(WitcheryItems.BOUND_WAYSTONE.get());
                    WaystoneItem.bindTo(waystone, waystoneDestination, level.dimension());
                    owner.teleportTo(monkey.getX(), monkey.getY(), monkey.getZ());
                    owner.setItemInHand(InteractionHand.MAIN_HAND, waystone);
                    InteractionResult result = monkey.mobInteract(owner, InteractionHand.MAIN_HAND);
                    helper.assertTrue(result.consumesAction(), "A bound waystone must issue an owner journey");
                    helper.assertTrue(waystone.getCount() == 1,
                            "Bound waystone transport must retain the source's reusable waystone");
                    helper.assertTrue(monkey.getCourierState() == FlyingMonkeyEntity.CourierState.PREPARING_WAYSTONE,
                            "A bound waystone must put the monkey into destination preparation");
                    helper.assertTrue(owner.getVehicle() == monkey,
                            "The owner must hang from the monkey during same-dimension waystone travel");
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(monkey.getCourierState() == FlyingMonkeyEntity.CourierState.IDLE,
                            "The monkey has not completed its waystone journey yet");
                    helper.assertTrue(!monkey.hasDestinationPreparationTicket(),
                            "Same-dimension completion must release the destination ticket");
                    helper.assertTrue(!owner.isPassenger(), "The owner must dismount at the destination");
                    helper.assertTrue(owner.distanceToSqr(
                                    waystoneDestination.getX() + 0.5D,
                                    waystoneDestination.getY(),
                                    waystoneDestination.getZ() + 0.5D) <= 16.0D,
                            "The owner must arrive safely beside the bound location");
                })
                .thenExecute(() -> {
                    ItemStack playerTaglock = TaglockKitItem.filledEntityTaglock(playerTarget);
                    owner.teleportTo(monkey.getX(), monkey.getY(), monkey.getZ());
                    owner.setItemInHand(InteractionHand.MAIN_HAND, playerTaglock);
                    monkey.mobInteract(owner, InteractionHand.MAIN_HAND);
                    helper.assertTrue(playerTaglock.isEmpty(),
                            "An accepted player Taglock order must consume one taglock");
                    helper.assertTrue(monkey.getCourierTargetUuid().filter(playerTarget.getUUID()::equals).isPresent(),
                            "A player Taglock must retain the exact bound player UUID");
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(monkey.getCourierState() == FlyingMonkeyEntity.CourierState.IDLE,
                            "The monkey has not completed its player Taglock round trip yet");
                    helper.assertTrue(!playerTarget.isPassenger(), "The returned player must be dismounted");
                    helper.assertTrue(playerTarget.distanceToSqr(
                                    waystoneDestination.getX() + 0.5D,
                                    waystoneDestination.getY(),
                                    waystoneDestination.getZ() + 0.5D) <= 25.0D,
                            "The returned player must arrive near the second command origin");
                })
                .thenExecute(() -> nextWaystoneOrderAt[0] = level.getGameTime() + 100L)
                .thenWaitUntil(() -> helper.assertTrue(level.getGameTime() >= nextWaystoneOrderAt[0],
                        "The fixture is waiting for the waystone preparation cooldown"))
                .thenExecute(() -> {
                    ItemStack bloodedWaystone = new ItemStack(WitcheryItems.BLOODED_WAYSTONE.get());
                    WaystoneItem.bindToEntity(bloodedWaystone, target);
                    target.snapTo(bloodedDestination.getX() + 0.5D, bloodedDestination.getY(),
                            bloodedDestination.getZ() + 0.5D, 0.0F, 0.0F);
                    owner.teleportTo(monkey.getX(), monkey.getY(), monkey.getZ());
                    owner.setItemInHand(InteractionHand.MAIN_HAND, bloodedWaystone);
                    monkey.mobInteract(owner, InteractionHand.MAIN_HAND);
                    helper.assertTrue(bloodedWaystone.getCount() == 1,
                            "Blooded Waystone travel must retain the reusable waystone");
                    helper.assertTrue(monkey.getCourierTargetUuid().filter(target.getUUID()::equals).isPresent(),
                            "A Blooded Waystone must retain its exact living destination UUID");
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(monkey.getCourierState() == FlyingMonkeyEntity.CourierState.IDLE,
                            "The monkey has not completed its Blooded Waystone journey yet");
                    helper.assertTrue(owner.distanceToSqr(
                                    bloodedDestination.getX() + 0.5D,
                                    bloodedDestination.getY(),
                                    bloodedDestination.getZ() + 0.5D) <= 16.0D,
                            "The owner must arrive near the Blooded Waystone's moving target");
                })
                .thenExecute(() -> {
                    monkey.discard();
                    target.discard();
                    HeadlessGameTestPlayer.remove(owner);
                    HeadlessGameTestPlayer.remove(playerTarget);
                })
                .thenSucceed();
    }

    private static void crossDimension(GameTestHelper helper) {
        ServerLevel source = helper.getLevel();
        ServerLevel destination = source.getServer().getLevel(Level.NETHER);
        if (destination == null) {
            throw new IllegalStateException("Nether level unavailable to Flying Monkey fixture");
        }

        BlockPos home = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos destinationPos = new BlockPos(8, 70, 8);
        buildFloor(source, home.offset(-2, -1, -2), home.offset(2, -1, 2));
        buildFloor(destination, destinationPos.offset(-2, -1, -2), destinationPos.offset(2, -1, 2));
        clearSpace(destination, destinationPos.offset(-2, 0, -2), destinationPos.offset(2, 3, 2));

        ServerPlayer owner = HeadlessGameTestPlayer.create(
                source, UUID.fromString("098b0e3a-9ce4-45b1-9238-36b6e7c55bcc"), "MonkeyWorldTraveler");
        FlyingMonkeyEntity monkey = WitcheryEntities.FLYING_MONKEY.get().create(source, EntitySpawnReason.TRIGGERED);
        Pig remoteTarget = EntityType.PIG.create(destination, EntitySpawnReason.TRIGGERED);
        if (monkey == null || remoteTarget == null) {
            HeadlessGameTestPlayer.remove(owner);
            throw new IllegalStateException("Unable to create cross-dimension courier fixture");
        }

        owner.snapTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 0.0F, 0.0F);
        owner.setGameMode(GameType.SURVIVAL);
        monkey.snapTo(home.getX() + 0.5D, home.getY() + 1.0D, home.getZ() + 0.5D, 0.0F, 0.0F);
        remoteTarget.snapTo(destinationPos.getX() + 0.5D, destinationPos.getY(),
                destinationPos.getZ() + 0.5D, 0.0F, 0.0F);
        source.addFreshEntity(monkey);
        destination.addFreshEntity(remoteTarget);
        monkey.tame(owner);

        ItemStack remoteTaglock = TaglockKitItem.filledEntityTaglock(remoteTarget);
        owner.setItemInHand(InteractionHand.MAIN_HAND, remoteTaglock);
        monkey.mobInteract(owner, InteractionHand.MAIN_HAND);
        helper.assertTrue(monkey.getCourierState() == FlyingMonkeyEntity.CourierState.IDLE,
                "A cross-dimension Taglock fetch must be rejected cleanly");
        helper.assertTrue(remoteTaglock.getCount() == 1,
                "A rejected cross-dimension Taglock fetch must not consume the taglock");

        ItemStack waystone = new ItemStack(WitcheryItems.BOUND_WAYSTONE.get());
        WaystoneItem.bindTo(waystone, destinationPos, Level.NETHER);
        owner.setItemInHand(InteractionHand.MAIN_HAND, waystone);
        monkey.mobInteract(owner, InteractionHand.MAIN_HAND);
        helper.assertTrue(monkey.getCourierState() == FlyingMonkeyEntity.CourierState.PREPARING_WAYSTONE,
                "A valid cross-dimension Bound Waystone must start destination preparation");
        helper.assertTrue(owner.getVehicle() == monkey,
                "The owner must mount before a cross-dimension waystone journey");
        UUID monkeyUuid = monkey.getUUID();

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(owner.level() == destination,
                            "The owner has not crossed to the waystone dimension yet");
                    helper.assertTrue(!owner.isPassenger(),
                            "The owner must be safely dismounted after crossing dimensions");
                    helper.assertTrue(owner.distanceToSqr(
                                    destinationPos.getX() + 0.5D,
                                    destinationPos.getY(),
                                    destinationPos.getZ() + 0.5D) <= 16.0D,
                            "The owner must arrive beside the cross-dimension waystone");
                    Entity movedMonkey = destination.getEntity(monkeyUuid);
                    helper.assertTrue(movedMonkey instanceof FlyingMonkeyEntity moved
                                    && moved.getCourierState() == FlyingMonkeyEntity.CourierState.IDLE,
                            "The monkey must cross dimensions and clear its completed order");
                    helper.assertTrue(movedMonkey instanceof FlyingMonkeyEntity moved
                                    && !moved.hasDestinationPreparationTicket(),
                            "Cross-dimension completion must release the destination ticket");
                })
                .thenExecute(() -> {
                    Entity movedMonkey = destination.getEntity(monkeyUuid);
                    if (movedMonkey != null) {
                        movedMonkey.discard();
                    }
                    remoteTarget.discard();
                    HeadlessGameTestPlayer.remove(owner);
                })
                .thenSucceed();
    }

    private static void assertCourierPersistence(GameTestHelper helper, ServerLevel level,
                                                 FlyingMonkeyEntity monkey, UUID targetUuid) {
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, level.registryAccess());
        monkey.saveWithoutId(output);
        CompoundTag snapshot = output.buildResult();
        FlyingMonkeyEntity restored = WitcheryEntities.FLYING_MONKEY.get().create(level, EntitySpawnReason.LOAD);
        if (restored == null) {
            throw new IllegalStateException("Unable to create restored Flying Monkey fixture");
        }
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), snapshot));
        helper.assertTrue(restored.getCourierState() == FlyingMonkeyEntity.CourierState.FETCHING,
                "Save/load must retain the active courier state");
        helper.assertTrue(restored.getCourierTargetUuid().filter(targetUuid::equals).isPresent(),
                "Save/load must retain the courier target UUID");
        helper.assertTrue(restored.getCourierHome().distanceTo(monkey.getCourierHome()) < 0.001D,
                "Save/load must retain the exact return origin");
        restored.discard();
    }

    private static void buildFloor(ServerLevel level, BlockPos from, BlockPos to) {
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
    }

    private static void clearSpace(ServerLevel level, BlockPos from, BlockPos to) {
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }
}
