package com.emoniph.witchery.ritual;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.command.HeadlessGameTestPlayer;
import com.emoniph.witchery.init.WitcheryBlocks;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Dedicated-server coverage for per-rite arming and source-timed Broken Earth progression. */
public final class PersistentRitualQueueGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> QUEUE_LIFECYCLE =
            TEST_FUNCTIONS.register("persistent_ritual_queue",
                    () -> PersistentRitualQueueGameTests::queueLifecycle);

    private PersistentRitualQueueGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(PersistentRitualQueueGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "persistent_ritual_queue"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 120, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "persistent_ritual_queue"),
                new FunctionGameTestInstance(QUEUE_LIFECYCLE.getKey(), data));
    }

    private static void queueLifecycle(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fixture = helper.absolutePos(new BlockPos(4, 2, 4));
        int y = Math.clamp(fixture.getY() + 80, level.getMinY() + 20, level.getMaxY() - 30);
        BlockPos establishedCenter = new BlockPos(fixture.getX(), y, fixture.getZ());
        BlockPos earthCenter = establishedCenter.east(24);
        RitualSavedData data = RitualSavedData.get(level);
        ServerPlayer owner = HeadlessGameTestPlayer.create(level,
                UUID.fromString("85c49ff9-ae11-47ac-a06e-1ff448c1a4e7"), "RitualQueueOwner");
        owner.snapTo(earthCenter.getX() + 0.5D, earthCenter.getY() + 1.0D,
                earthCenter.getZ() + 0.5D, 0.0F, 0.0F);

        placeRing(level, establishedCenter, RitualCircleDetector.CircleSize.SMALL);
        ActiveRitual established = new ActiveRitual(ActiveRitualType.FERTILITY, establishedCenter,
                RitualCircleDetector.CircleSize.SMALL, establishedCenter, 1,
                4, 8, 5, 5, 0, 0.0F, false, 0,
                level.getGameTime(), ItemStack.EMPTY, List.of(), "", ItemStack.EMPTY);
        data.activeRituals.add(established);

        ItemEntity pending = new ItemEntity(level, establishedCenter.getX() + 10.5D,
                establishedCenter.getY() + 1.0D, establishedCenter.getZ() + 0.5D,
                new ItemStack(Items.BONE));
        level.addFreshEntity(pending);
        RitualOffering.dropped(pending).consumeOne();
        helper.assertTrue(SacrificeConsumer.hasPending(level),
                "The fixture must retain one staged sacrifice");
        RitualExecutor.tickActiveRituals(level);
        helper.assertTrue(established.stage == 1,
                "An unrelated staged sacrifice must not freeze an established rite");

        SacrificeConsumer.flush(level);
        data.activeRituals.remove(established);
        placeRing(level, earthCenter, RitualCircleDetector.CircleSize.SMALL);
        ActiveRitual movingEarth = new ActiveRitual(ActiveRitualType.MOVING_EARTH, earthCenter,
                RitualCircleDetector.CircleSize.SMALL, earthCenter, 1,
                1, 10, 60, 1, 0, 0.0F, false, 2,
                level.getGameTime(), ItemStack.EMPTY, List.of(new ItemStack(Items.POTION)),
                owner.getScoreboardName(), ItemStack.EMPTY);
        RitualExecutor.buildMovingEarthPath(level, movingEarth);
        helper.assertTrue(movingEarth.path.size() == 60,
                "Broken Earth must build the source 60-position path");
        BlockPos firstCarve = movingEarth.path.get(5);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int depth = 0; depth < 12; depth++) {
                    level.setBlock(firstCarve.offset(dx, -depth, dz),
                            Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }

        data.activeRituals.add(movingEarth);
        data.setDirty();
        RitualExecutor.tickActiveRituals(level);
        helper.assertTrue(movingEarth.stage == 1,
                "Broken Earth must advance on its first post-boundary update");
        helper.assertTrue(level.getBlockState(firstCarve).isAir(),
                "Broken Earth must begin by carving source path index 5");
        HeadlessGameTestPlayer.remove(owner);
        helper.succeed();
    }

    private static void placeRing(ServerLevel level, BlockPos center,
                                  RitualCircleDetector.CircleSize size) {
        for (BlockPos mark : RitualCircleDetector.ringPositions(center, size)) {
            level.setBlock(mark.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(mark, WitcheryBlocks.CIRCLE_GLYPH.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
