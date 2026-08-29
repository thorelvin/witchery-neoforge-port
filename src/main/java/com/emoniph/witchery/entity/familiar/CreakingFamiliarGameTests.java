package com.emoniph.witchery.entity.familiar;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.command.HeadlessGameTestPlayer;
import com.emoniph.witchery.data.CreakingFamiliarData;
import com.emoniph.witchery.init.WitcheryAttachments;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Consumer;

/** Dedicated-server contract for the Creaking familiar's two-mode specialization. */
public final class CreakingFamiliarGameTests {
    private static final UUID OWNER_ID = UUID.fromString("0373a130-32ad-4a9e-a79d-fc6e82153b4e");
    private static final UUID INTRUDER_ID = UUID.fromString("e179a733-37e4-44b8-a8e9-9df719764793");
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MODES =
            TEST_FUNCTIONS.register("creaking_familiar_modes",
                    () -> CreakingFamiliarGameTests::modes);

    private CreakingFamiliarGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(CreakingFamiliarGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "creaking_familiar"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 120, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "creaking_familiar_modes"),
                new FunctionGameTestInstance(MODES.getKey(), data));
    }

    private static void modes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fixture = helper.absolutePos(new BlockPos(4, 2, 4));
        int y = Math.clamp(fixture.getY() + 60, level.getMinY() + 10, level.getMaxY() - 10);
        BlockPos anchor = new BlockPos(fixture.getX(), y, fixture.getZ());
        ServerPlayer owner = HeadlessGameTestPlayer.create(level, OWNER_ID, "CreakingOwner");
        ServerPlayer intruder = null;
        Creaking creaking = null;
        try {
            prepareFixture(level, anchor);
            owner.snapTo(anchor.getX() + 2.5D, anchor.getY(), anchor.getZ() + 0.5D, 0.0F, 0.0F);

            creaking = EntityType.CREAKING.create(level, EntitySpawnReason.TRIGGERED);
            helper.assertTrue(creaking != null, "The dedicated server must create a vanilla Creaking");
            creaking.snapTo(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D, 0.0F, 0.0F);
            level.addFreshEntity(creaking);
            helper.assertTrue(CreakingFamiliarHelper.bind(owner, level, creaking) == creaking,
                    "The rite helper must bind an unclaimed Creaking in FOLLOW mode");
            helper.assertTrue(CreakingFamiliarHelper.getData(creaking).mode()
                            == CreakingFamiliarData.Mode.FOLLOW,
                    "Fresh bindings must begin in FOLLOW mode");

            creaking.tickCount = CreakingFamiliarHelper.ROOTED_WARD_INTERVAL_TICKS;
            CreakingFamiliarHelper.serverTick(level, creaking);
            helper.assertTrue(resinCount(level, anchor) == 0,
                    "FOLLOW mode must never produce Resin Clumps");
            helper.assertTrue(CreakingFamiliarHelper.getData(creaking).lastAlertGameTime()
                            == CreakingFamiliarData.NO_SCHEDULED_TICK,
                    "FOLLOW mode must never run intrusion alerts");

            CreakingFamiliarHelper.toggleMode(owner, creaking);
            CreakingFamiliarData rooted = CreakingFamiliarHelper.getData(creaking);
            helper.assertTrue(rooted.mode() == CreakingFamiliarData.Mode.ROOTED_GUARD
                            && rooted.guardPosition().filter(anchor::equals).isPresent()
                            && WitcheryAttachments.get(owner).isFamiliarSitting(),
                    "Rooting must persist mode, dimension, anchor, and the player compatibility summary");

            owner.snapTo(anchor.getX() + 40.5D, anchor.getY(), anchor.getZ() + 0.5D, 0.0F, 0.0F);
            Vec3 rootedPosition = creaking.position();
            creaking.tickCount += CreakingFamiliarHelper.ROOTED_WARD_INTERVAL_TICKS;
            CreakingFamiliarHelper.serverTick(level, creaking);
            helper.assertTrue(creaking.position().distanceToSqr(rootedPosition) < 0.01D,
                    "ROOTED_GUARD must not follow or teleport to a distant owner");

            intruder = HeadlessGameTestPlayer.create(level, INTRUDER_ID, "CreakingIntruder");
            intruder.snapTo(anchor.getX() + 3.5D, anchor.getY(), anchor.getZ() + 0.5D, 90.0F, 0.0F);
            creaking.tickCount += CreakingFamiliarHelper.ROOTED_WARD_INTERVAL_TICKS;
            CreakingFamiliarHelper.serverTick(level, creaking);
            CreakingFamiliarData alerted = CreakingFamiliarHelper.getData(creaking);
            helper.assertTrue(alerted.lastAlertCount() == 1
                            && alerted.lastAlertGameTime() == level.getGameTime(),
                    "A rooted guard must record one bounded, owner-correct intrusion alert");

            long firstAlert = alerted.lastAlertGameTime();
            creaking.tickCount += CreakingFamiliarHelper.ROOTED_WARD_INTERVAL_TICKS;
            CreakingFamiliarHelper.serverTick(level, creaking);
            helper.assertTrue(CreakingFamiliarHelper.getData(creaking).lastAlertGameTime() == firstAlert,
                    "An unchanged intrusion count must remain rate-limited");
            HeadlessGameTestPlayer.remove(intruder);
            intruder = null;

            ItemEntity waitingResin = new ItemEntity(level, anchor.getX() + 0.5D, anchor.getY() + 0.25D,
                    anchor.getZ() + 0.5D, new ItemStack(Items.RESIN_CLUMP));
            level.addFreshEntity(waitingResin);
            setResinDueNow(level, creaking);
            creaking.tickCount += CreakingFamiliarHelper.ROOTED_WARD_INTERVAL_TICKS;
            CreakingFamiliarHelper.serverTick(level, creaking);
            helper.assertTrue(resinCount(level, anchor) == 1,
                    "A rooted guard must not accumulate resin while an uncollected clump is nearby");
            waitingResin.discard();

            setResinDueNow(level, creaking);
            creaking.tickCount += CreakingFamiliarHelper.ROOTED_WARD_INTERVAL_TICKS;
            CreakingFamiliarHelper.serverTick(level, creaking);
            helper.assertTrue(resinCount(level, anchor) == 1
                            && CreakingFamiliarHelper.getData(creaking).nextResinGameTime()
                            > level.getGameTime(),
                    "An unwatched rooted guard may produce one Resin Clump and must reschedule slowly");

            TagValueOutput output = TagValueOutput.createWithContext(
                    ProblemReporter.DISCARDING, level.registryAccess());
            creaking.saveWithoutId(output);
            CompoundTag persisted = output.buildResult();
            Creaking reloaded = EntityType.CREAKING.create(level, EntitySpawnReason.LOAD);
            helper.assertTrue(reloaded != null, "The dedicated server must recreate a saved Creaking");
            reloaded.load(TagValueInput.create(
                    ProblemReporter.DISCARDING, level.registryAccess(), persisted));
            CreakingFamiliarData reloadedData = CreakingFamiliarHelper.getData(reloaded);
            helper.assertTrue(reloadedData.mode() == CreakingFamiliarData.Mode.ROOTED_GUARD
                            && reloadedData.guardPosition().filter(anchor::equals).isPresent()
                            && reloadedData.nextResinGameTime()
                            == CreakingFamiliarHelper.getData(creaking).nextResinGameTime(),
                    "Entity save/reload must retain rooted mode, anchor, and resin cadence");

            CreakingFamiliarHelper.toggleMode(owner, creaking);
            helper.assertTrue(CreakingFamiliarHelper.getData(creaking).mode()
                            == CreakingFamiliarData.Mode.FOLLOW
                            && CreakingFamiliarHelper.getData(creaking).guardPosition().isEmpty(),
                    "Using the rooted familiar again must return it to FOLLOW and clear the anchor");
            creaking.tickCount += CreakingFamiliarHelper.WATCH_SCAN_INTERVAL_TICKS;
            CreakingFamiliarHelper.serverTick(level, creaking);
            helper.assertTrue(creaking.distanceToSqr(owner) < 4.0D,
                    "FOLLOW mode must retain long-distance recovery to its owner");
            helper.succeed();
        } finally {
            if (creaking != null && !creaking.isRemoved()) {
                creaking.discard();
            }
            if (intruder != null && !intruder.isRemoved()) {
                HeadlessGameTestPlayer.remove(intruder);
            }
            HeadlessGameTestPlayer.remove(owner);
            level.getEntitiesOfClass(ItemEntity.class, new AABB(anchor).inflate(8.0D),
                    item -> item.getItem().is(Items.RESIN_CLUMP)).forEach(ItemEntity::discard);
        }
    }

    private static void setResinDueNow(ServerLevel level, Creaking creaking) {
        CreakingFamiliarData data = CreakingFamiliarHelper.getData(creaking)
                .withWatched(false)
                .withNextResinGameTime(level.getGameTime());
        CreakingFamiliarHelper.setData(creaking, data);
    }

    private static int resinCount(ServerLevel level, BlockPos anchor) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(anchor).inflate(4.0D),
                item -> item.isAlive() && item.getItem().is(Items.RESIN_CLUMP)).size();
    }

    private static void prepareFixture(ServerLevel level, BlockPos anchor) {
        for (BlockPos pos : BlockPos.betweenClosed(anchor.offset(-5, -2, -5), anchor.offset(5, 4, 5))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        for (BlockPos pos : BlockPos.betweenClosed(anchor.offset(-5, -1, -5), anchor.offset(5, -1, 5))) {
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
