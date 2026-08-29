package com.emoniph.witchery.ritual;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.AltarBlock;
import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.blocks.entity.GrassperBlockEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Dedicated-server coverage for dropped, Grassper, and mixed ritual sacrifice sources. */
public final class RitualOfferingGameTests {
    private static final BlockPos CENTER = new BlockPos(4, 2, 4);
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OFFERING_SOURCES =
            TEST_FUNCTIONS.register("ritual_offering_sources", () -> RitualOfferingGameTests::offeringSources);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INDEPENDENT_ESCROWS =
            TEST_FUNCTIONS.register("ritual_independent_escrows", () -> RitualOfferingGameTests::independentEscrows);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ESCROW_LIFECYCLE =
            TEST_FUNCTIONS.register("ritual_escrow_lifecycle", () -> RitualOfferingGameTests::escrowLifecycle);

    private RitualOfferingGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(RitualOfferingGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "ritual_offerings"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 100, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "ritual_offering_sources"),
                new FunctionGameTestInstance(OFFERING_SOURCES.getKey(), data));
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "ritual_independent_escrows"),
                new FunctionGameTestInstance(INDEPENDENT_ESCROWS.getKey(), data));
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "ritual_escrow_lifecycle"),
                new FunctionGameTestInstance(ESCROW_LIFECYCLE.getKey(), data));
    }

    private static void independentEscrows(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos firstPos = helper.absolutePos(CENTER.west(2));
        BlockPos secondPos = helper.absolutePos(CENTER.east(2));
        ItemEntity first = drop(level, firstPos, new ItemStack(Items.DIAMOND, 4));
        ItemEntity second = drop(level, secondPos, Items.EMERALD.getDefaultInstance());
        RitualTransaction firstTransaction = new RitualTransaction(level, firstPos, 16, 0, "");
        for (int i = 0; i < 4; i++) firstTransaction.offer(first);
        RitualTransaction secondTransaction = new RitualTransaction(level, secondPos, 16, 0, "").offer(second);

        helper.assertTrue(firstTransaction.commit(null) && secondTransaction.commit(null),
                "Independent sacrifice transactions must both commit");
        helper.assertTrue(!first.isAlive() && !second.isAlive(),
                "Both authoritative sources must be removed in the activation tick");
        List<SacrificeBatch> batches = RitualSavedData.get(level).sacrificeBatches.stream()
                .filter(batch -> batch.center.equals(firstPos) || batch.center.equals(secondPos)).toList();
        helper.assertTrue(batches.size() == 2, "Each activation must own a distinct persisted escrow batch");

        helper.runAfterDelay(25, () -> {
            List<SacrificeBatch> remaining = RitualSavedData.get(level).sacrificeBatches.stream()
                    .filter(batch -> batch.center.equals(firstPos) || batch.center.equals(secondPos)).toList();
            helper.assertTrue(remaining.size() == 1 && remaining.getFirst().center.equals(firstPos)
                            && remaining.getFirst().visualStep == 1,
                    "The short batch must finish without waiting for an unrelated long batch");
        });
        helper.runAfterDelay(85, () -> {
            List<SacrificeBatch> finished = RitualSavedData.get(level).sacrificeBatches.stream()
                    .filter(batch -> batch.center.equals(firstPos) || batch.center.equals(secondPos)).toList();
            helper.assertTrue(finished.isEmpty(), "The long batch must retain its own cadence and finish");
            helper.succeed();
        });
    }

    private static void escrowLifecycle(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(CENTER);

        BlockPos preparedPos = center.west(8);
        UUID prepared = SacrificeConsumer.prepareBatch(level, preparedPos,
                List.of(new ItemStack(Items.DIAMOND)), List.of());
        SacrificeConsumer.refund(level, prepared);
        helper.assertTrue(findBatch(level, prepared) == null && countItems(level, preparedPos, Items.DIAMOND) == 0,
                "Cancelling PREPARED must drop the reservation without minting an unremoved source");

        BlockPos paidPos = center.west(5);
        UUID paid = SacrificeConsumer.prepareBatch(level, paidPos,
                List.of(new ItemStack(Items.EMERALD)), List.of());
        SacrificeConsumer.markPaid(level, paid, null, 0);
        SacrificeConsumer.refund(level, paid);
        SacrificeConsumer.refund(level, paid);
        helper.assertTrue(findBatch(level, paid) == null && countItems(level, paidPos, Items.EMERALD) == 1,
                "A paid batch must refund its exact escrow once");

        BlockPos armedPos = center.west(2);
        UUID armed = SacrificeConsumer.prepareBatch(level, armedPos,
                List.of(new ItemStack(Items.LAPIS_LAZULI)), List.of(armedPos));
        SacrificeConsumer.markPaid(level, armed, null, 0);
        SacrificeConsumer.arm(level, armed);
        SacrificeConsumer.refund(level, armed);
        helper.assertTrue(findBatch(level, armed) != null && countItems(level, armedPos, Items.LAPIS_LAZULI) == 0,
                "An armed batch must never refund its startup offering");

        placePoweredAltar(level, center.south(6));

        BlockPos rollbackPos = center.east(2);
        AltarBlockEntity rollbackAltar = RitualExecutor.findNearbyRitualAltar(level, rollbackPos, 16);
        helper.assertTrue(rollbackAltar != null && rollbackAltar.hasValidStructure(),
                "Escrow fixture altar must be valid");
        if (rollbackAltar == null || !rollbackAltar.hasValidStructure()) return;
        ItemEntity rollbackSource = drop(level, rollbackPos, Items.COAL.getDefaultInstance());
        int powerBeforeRollback = rollbackAltar.getPower();
        boolean rolledBack = new RitualTransaction(level, rollbackPos, 16, 1, "")
                        .offer(rollbackSource).commit(null, () -> false);
        helper.assertTrue(!rolledBack && !rollbackSource.isAlive()
                        && countItems(level, rollbackPos, Items.COAL) == 1
                        && rollbackAltar.getPower() == powerBeforeRollback,
                "Failed activation must restore one offering and one altar charge");

        BlockPos successPos = center.east(5);
        AltarBlockEntity successAltar = RitualExecutor.findNearbyRitualAltar(level, successPos, 16);
        helper.assertTrue(successAltar != null && successAltar.hasValidStructure(),
                "Successful transaction must resolve a valid altar");
        if (successAltar == null || !successAltar.hasValidStructure()) return;
        ItemEntity successSource = drop(level, successPos, Items.AMETHYST_SHARD.getDefaultInstance());
        int powerBeforeSuccess = successAltar.getPower();
        helper.assertTrue(new RitualTransaction(level, successPos, 16, 1, "")
                        .offer(successSource).commit(null),
                "Successful paid transaction must arm");
        SacrificeBatch successBatch = RitualSavedData.get(level).sacrificeBatches.stream()
                .filter(batch -> batch.center.equals(successPos)).findFirst().orElse(null);
        helper.assertTrue(successBatch != null && !successSource.isAlive()
                        && successAltar.getPower() == powerBeforeSuccess - RitualExecutor.scaleRitualPowerCost(1),
                "Successful activation must remove its source and charge altar power once");
        if (successBatch != null) SacrificeConsumer.refund(level, successBatch.batchId);
        helper.assertTrue(countItems(level, successPos, Items.AMETHYST_SHARD) == 0
                        && successAltar.getPower() == powerBeforeSuccess - RitualExecutor.scaleRitualPowerCost(1),
                "Cancelling presentation after arming must not refund items or power");

        BlockPos grassperPos = center.east(8);
        GrassperBlockEntity grassper = placeGrassper(level, grassperPos);
        grassper.setItem(new ItemStack(Items.REDSTONE));
        helper.assertTrue(new RitualTransaction(level, grassperPos, 16, 0, "")
                        .offer(RitualOffering.grassper(grassper)).commit(null),
                "Grassper payment must arm");
        level.setBlock(grassperPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

        helper.runAfterDelay(45, () -> {
            helper.assertTrue(countItems(level, successPos, Items.AMETHYST_SHARD) == 0,
                    "Removed dropped sources must not reappear after presentation");
            helper.assertTrue(countItems(level, grassperPos, Items.REDSTONE) == 0,
                    "Breaking a paid Grassper must not duplicate its escrow");
            helper.assertTrue(findBatch(level, armed) == null
                            && RitualSavedData.get(level).sacrificeBatches.stream()
                            .noneMatch(batch -> batch.center.equals(successPos) || batch.center.equals(grassperPos)),
                    "All armed presentations must finish independently");
            helper.succeed();
        });
    }

    private static void offeringSources(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(CENTER);
        GrassperBlockEntity first = placeGrassper(level, center.above().north());
        GrassperBlockEntity second = placeGrassper(level, center.above().south());

        first.setItem(new ItemStack(Items.SLIME_BALL));
        second.setItem(new ItemStack(Items.REDSTONE));
        List<RitualOffering> grassperOnly = matched(helper, level, center);
        helper.assertTrue(grassperOnly.stream().allMatch(offering -> offering.grassper() != null),
                "Grassper-only offerings must remain Grassper-backed");

        first.setItem(ItemStack.EMPTY);
        second.setItem(ItemStack.EMPTY);
        ItemEntity droppedSlime = drop(level, center, Items.SLIME_BALL.getDefaultInstance());
        ItemEntity droppedRedstone = drop(level, center.east(), Items.REDSTONE.getDefaultInstance());
        List<RitualOffering> droppedOnly = matched(helper, level, center);
        helper.assertTrue(droppedOnly.stream().allMatch(offering -> offering.itemEntity() != null),
                "Dropped-only offerings must remain entity-backed");
        droppedSlime.discard();
        droppedRedstone.discard();

        second.setItem(new ItemStack(Items.REDSTONE));
        ItemEntity mixedSlime = drop(level, center, Items.SLIME_BALL.getDefaultInstance());
        List<RitualOffering> mixed = matched(helper, level, center);
        helper.assertTrue(mixed.stream().anyMatch(offering -> offering.itemEntity() != null)
                        && mixed.stream().anyMatch(offering -> offering.grassper() != null),
                "Mixed offerings must select one dropped stack and one Grassper stack");

        RitualTransaction transaction = new RitualTransaction(level, center, 16, 0, "");
        mixed.forEach(transaction::offer);
        helper.assertTrue(transaction.commit(null), "Zero-power mixed transaction must commit");
        helper.assertTrue(!mixedSlime.isAlive() || mixedSlime.getItem().isEmpty(),
                "Dropped offering must be paid immediately into escrow");
        helper.assertTrue(second.getItem().isEmpty(),
                "Grassper offering must be paid immediately into escrow");
        helper.assertTrue(SacrificeConsumer.hasPending(level),
                "Paid batch must retain its delayed presentation steps");
        helper.assertTrue(RitualOfferings.matchOfferings(
                level, center, RitualOfferings.IMPRISONMENT_RECIPE).isEmpty(),
                "Already-paid offerings must not satisfy another activation");

        helper.runAfterDelay(45, () -> {
            helper.assertTrue(!mixedSlime.isAlive() || mixedSlime.getItem().isEmpty(),
                    "Dropped offering must remain consumed after its visual pop");
            helper.assertTrue(second.getItem().isEmpty(),
                    "Grassper offering must remain consumed after its visual pop");
            helper.assertTrue(!SacrificeConsumer.hasPending(level),
                    "Sacrifice queue must drain after both staged pops");
            helper.succeed();
        });
    }

    private static List<RitualOffering> matched(GameTestHelper helper, ServerLevel level, BlockPos center) {
        var result = RitualOfferings.matchOfferings(level, center, RitualOfferings.IMPRISONMENT_RECIPE);
        helper.assertTrue(result.isPresent(), "Imprisonment offerings must match");
        helper.assertTrue(result.get().size() == 2, "Imprisonment must select two distinct sources");
        return result.get();
    }

    private static GrassperBlockEntity placeGrassper(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, WitcheryBlocks.GRASSPER.get().defaultBlockState());
        return (GrassperBlockEntity) level.getBlockEntity(pos);
    }

    private static void placePoweredAltar(ServerLevel level, BlockPos backLeft) {
        for (int z = 0; z < 2; z++) {
            for (int x = 0; x < 3; x++) {
                BlockPos pos = backLeft.offset(x, 0, z);
                level.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(),
                        Block.UPDATE_ALL);
                level.setBlock(pos, WitcheryBlocks.ALTAR.get().defaultBlockState()
                        .setValue(AltarBlock.AXIS, Direction.Axis.X), Block.UPDATE_ALL);
            }
        }
        for (int z = 0; z < 2; z++) {
            for (int x = 0; x < 3; x++) {
                if (level.getBlockEntity(backLeft.offset(x, 0, z)) instanceof AltarBlockEntity altar) {
                    altar.invalidate(level);
                    altar.refundPower(100);
                }
            }
        }
    }

    private static SacrificeBatch findBatch(ServerLevel level, java.util.UUID id) {
        return RitualSavedData.get(level).sacrificeBatches.stream()
                .filter(batch -> batch.batchId.equals(id)).findFirst().orElse(null);
    }

    private static int countItems(ServerLevel level, BlockPos center, Item item) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(1.5D),
                        entity -> entity.isAlive() && entity.getItem().is(item)).stream()
                .mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    private static ItemEntity drop(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D, stack);
        level.addFreshEntity(item);
        return item;
    }
}
