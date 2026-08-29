package com.emoniph.witchery.blocks;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.command.HeadlessGameTestPlayer;
import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.data.BlightHeartSavedData;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryItems;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Consumer;

/** Dedicated-server coverage for the owned, bounded, reversible Blight Heart lifecycle. */
public final class BlightHeartLifecycleGameTests {
    private static final UUID OWNER_ID = UUID.fromString("470a5bf7-36f6-419e-b0ef-a0b33a443705");
    private static final UUID COUNTER_ID = UUID.fromString("f61b7d0d-6608-4fc1-a96e-c905b1378d05");
    private static final UUID RESTART_OWNER_ID = UUID.fromString("4c10bc50-45ae-4eb4-905c-f2d16b069f8f");
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LIFECYCLE =
            TEST_FUNCTIONS.register("blight_heart_lifecycle",
                    () -> BlightHeartLifecycleGameTests::lifecycle);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RESTART_RESUME =
            TEST_FUNCTIONS.register("blight_heart_restart_resume",
                    () -> BlightHeartLifecycleGameTests::restartResume);

    private BlightHeartLifecycleGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(BlightHeartLifecycleGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "blight_heart_lifecycle"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 100, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "blight_heart_lifecycle"),
                new FunctionGameTestInstance(LIFECYCLE.getKey(), data));
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "blight_heart_restart_resume"),
                new FunctionGameTestInstance(RESTART_RESUME.getKey(), data));
    }

    private static void lifecycle(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fixture = helper.absolutePos(new BlockPos(4, 2, 4));
        int y = Math.clamp(fixture.getY() + 80, level.getMinY() + 20, level.getMaxY() - 20);
        BlockPos anchor = new BlockPos(fixture.getX(), y, fixture.getZ());
        ServerPlayer owner = HeadlessGameTestPlayer.create(level, OWNER_ID, "BlightHeartOwner");
        ServerPlayer counter = HeadlessGameTestPlayer.create(level, COUNTER_ID, "BlightHeartCounter");
        owner.snapTo(anchor.getX() + 0.5D, anchor.getY() + 1.0D,
                anchor.getZ() + 0.5D, 0.0F, 0.0F);
        counter.snapTo(anchor.getX() + 2.5D, anchor.getY() + 1.0D,
                anchor.getZ() + 0.5D, 0.0F, 0.0F);

        boolean previousGate = WitcheryConfig.ENABLE_BLIGHT_HEART.get();
        WitcheryConfig.ENABLE_BLIGHT_HEART.set(true);
        try {
            prepareFixture(level, anchor);
            helper.assertTrue(BlightHelper.placeHeart(level, anchor, false, owner),
                    "An enabled, protected-safe Heart must register and place for its owner");

            BlightHeartSavedData saved = BlightHeartSavedData.get(level.getServer());
            BlightHeartSavedData.HeartRecord heart = saved.heartAt(level, anchor);
            helper.assertTrue(heart != null && OWNER_ID.equals(heart.ownerId())
                            && heart.state() == BlightHeartSavedData.HeartState.ACTIVE,
                    "The Heart journal must persist the real owner and active lifecycle state");
            helper.assertTrue(BlightHelper.canPlaceHeartFor(level, owner, anchor.east(40))
                            == BlightHeartSavedData.PlacementResult.OWNER_LIMIT,
                    "One owner may not seed a second Heart while the first journal exists");
            helper.assertTrue(BlightHelper.canPlaceHeartFor(level, counter, anchor.east(8))
                            == BlightHeartSavedData.PlacementResult.NEARBY_HEART,
                    "A second owner may not overlap another Heart's bounded corruption area");

            BlockPos soil = anchor.east();
            BlockPos stone = anchor.west();
            BlockPos log = anchor.north();
            helper.assertTrue(BlightHelper.convertTarget(level, heart, owner, soil, true)
                            && level.getBlockState(soil).is(WitcheryBlocks.BLIGHTED_SOIL.get()),
                    "Owned conversion must journal and blight grass");
            helper.assertTrue(BlightHelper.convertTarget(level, heart, owner, stone, true)
                            && level.getBlockState(stone).is(WitcheryBlocks.BLIGHTED_STONE.get()),
                    "Owned conversion must journal and blight cobblestone");
            helper.assertTrue(BlightHelper.convertTarget(level, heart, owner, log, true)
                            && level.getBlockState(log).is(WitcheryBlocks.BLIGHTED_LOG.get()),
                    "Owned conversion must journal and blight spruce log");

            BlockPos protectedTarget = anchor.south();
            level.setBlock(protectedTarget.south(), Blocks.CALCITE.defaultBlockState(),
                    Block.UPDATE_ALL);
            helper.assertTrue(!BlightHelper.convertTarget(level, heart, owner, protectedTarget, true),
                    "A tagged Blight barrier must reject conversion");
            helper.assertTrue(level.getBlockState(protectedTarget).is(Blocks.STONE),
                    "Rejected protected terrain must retain its original block");
            helper.assertTrue(!heart.mutations().containsKey(protectedTarget),
                    "Rejected protected terrain must not enter the restoration ledger");

            counter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WitcheryItems.SEER_STONE.get()));
            InteractionResult trace = WitcheryItems.SEER_STONE.get().useOn(new UseOnContext(
                    counter, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(soil), Direction.UP, soil, false)));
            helper.assertTrue(trace == InteractionResult.CONSUME,
                    "A Seer Stone used on tracked blight must reveal the indexed anchor trail");

            int ledgerBeforePause = heart.mutations().size();
            HeadlessGameTestPlayer.remove(owner);
            BlightHelper.spreadFromHeart(level, anchor, RandomSource.create(0xB11A7L));
            helper.assertTrue(heart.mutations().size() == ledgerBeforePause,
                    "Spread must pause when the real owner is offline");

            helper.assertTrue(BlightHelper.cleanseBlock(
                            level, anchor, level.getBlockState(anchor), counter),
                    "A non-owner with terrain permission must be able to cleanse the physical anchor");
            helper.assertTrue(level.getBlockState(anchor).is(Blocks.MOSS_BLOCK),
                    "Cleansing the Heart must immediately restore the exact buried anchor block");
            for (int i = 0; i < 8 && saved.heartOwnedBy(OWNER_ID) != null; i++) {
                saved.tickLevel(level);
            }

            helper.assertTrue(saved.heartOwnedBy(OWNER_ID) == null,
                    "Bounded cleanup must retire the owner journal after all loaded mutations restore");
            helper.assertTrue(level.getBlockState(soil).is(Blocks.GRASS_BLOCK),
                    "Cleanup must restore grass rather than generic dirt");
            helper.assertTrue(level.getBlockState(stone).is(Blocks.COBBLESTONE),
                    "Cleanup must restore cobblestone rather than generic stone");
            helper.assertTrue(level.getBlockState(log).is(Blocks.SPRUCE_LOG),
                    "Cleanup must restore the original spruce log family");
            helper.succeed();
        } finally {
            WitcheryConfig.ENABLE_BLIGHT_HEART.set(previousGate);
            if (!owner.isRemoved()) {
                HeadlessGameTestPlayer.remove(owner);
            }
            if (!counter.isRemoved()) {
                HeadlessGameTestPlayer.remove(counter);
            }
        }
    }

    private static void prepareFixture(ServerLevel level, BlockPos anchor) {
        for (BlockPos pos : BlockPos.betweenClosed(anchor.offset(-4, -2, -4), anchor.offset(4, 3, 4))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(anchor, Blocks.MOSS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(anchor.east(), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(anchor.west(), Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(anchor.north(), Blocks.SPRUCE_LOG.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(anchor.south(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static void restartResume(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlightHeartSavedData saved = BlightHeartSavedData.get(level.getServer());
        BlightHeartSavedData.HeartRecord persisted = saved.heartOwnedBy(RESTART_OWNER_ID);
        boolean previousGate = WitcheryConfig.ENABLE_BLIGHT_HEART.get();
        WitcheryConfig.ENABLE_BLIGHT_HEART.set(true);
        ServerPlayer owner = null;
        try {
            if (persisted == null) {
                BlockPos fixture = helper.absolutePos(new BlockPos(4, 2, 4));
                int y = Math.clamp(fixture.getY() + 80, level.getMinY() + 20, level.getMaxY() - 20);
                BlockPos anchor = new BlockPos(fixture.getX(), y, fixture.getZ());
                for (BlockPos pos : BlockPos.betweenClosed(anchor.offset(-2, -2, -2),
                        anchor.offset(2, 2, 2))) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
                level.setBlock(anchor, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(anchor.east(), Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
                owner = HeadlessGameTestPlayer.create(level, RESTART_OWNER_ID, "BlightRestartOwner");
                owner.snapTo(anchor.getX() + 0.5D, anchor.getY() + 1.0D,
                        anchor.getZ() + 0.5D, 0.0F, 0.0F);
                helper.assertTrue(BlightHelper.placeHeart(level, anchor, false, owner),
                        "Restart stage one must place an owned Heart");
                persisted = saved.heartAt(level, anchor);
                helper.assertTrue(BlightHelper.convertTarget(level, persisted, owner, anchor.east(), true),
                        "Restart stage one must persist a partially progressed mutation ledger");
                helper.assertTrue(saved.claimSpreadWindow(persisted, level.getGameTime()),
                        "Restart stage one must persist the spread cadence cursor");
                helper.assertTrue(persisted.mutations().size() == 2
                                && persisted.remainingBudget() == BlightHeartSavedData.MAX_SPREAD_MUTATIONS - 1,
                        "Restart stage one must leave one bounded mutation plus the anchor for disk save");
            } else {
                BlockPos anchor = persisted.anchor();
                helper.assertTrue(level.isLoaded(anchor)
                                && level.getBlockState(anchor).is(WitcheryBlocks.BLIGHT_HEART.get()),
                        "Restart stage two must reload the physical anchor in its original dimension");
                helper.assertTrue(persisted.state() == BlightHeartSavedData.HeartState.ACTIVE
                                && persisted.mutations().size() == 2
                                && persisted.remainingBudget() == BlightHeartSavedData.MAX_SPREAD_MUTATIONS - 1
                                && persisted.nextSpreadTick() > 0L,
                        "Restart stage two must retain owner, lifecycle, budget, cadence, and partial ledger");
                owner = HeadlessGameTestPlayer.create(level, RESTART_OWNER_ID, "BlightRestartOwner");
                owner.snapTo(anchor.getX() + 0.5D, anchor.getY() + 1.0D,
                        anchor.getZ() + 0.5D, 0.0F, 0.0F);
                helper.assertTrue(BlightHelper.cleanseBlock(
                                level, anchor, level.getBlockState(anchor), owner),
                        "Restart stage two must resume through the normal cleansing lifecycle");
                for (int i = 0; i < 8 && saved.heartOwnedBy(RESTART_OWNER_ID) != null; i++) {
                    saved.tickLevel(level);
                }
                helper.assertTrue(saved.heartOwnedBy(RESTART_OWNER_ID) == null
                                && level.getBlockState(anchor).is(Blocks.DIRT)
                                && level.getBlockState(anchor.east()).is(Blocks.COBBLESTONE),
                        "Restart-resumed cleanup must restore both exact original block families");
            }
            helper.succeed();
        } finally {
            WitcheryConfig.ENABLE_BLIGHT_HEART.set(previousGate);
            if (owner != null && !owner.isRemoved()) {
                HeadlessGameTestPlayer.remove(owner);
            }
        }
    }
}
