package com.emoniph.witchery.ritual;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.entity.GrassperBlockEntity;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Consumer;

/** Dedicated-server coverage for dropped, Grassper, and mixed ritual sacrifice sources. */
public final class RitualOfferingGameTests {
    private static final BlockPos CENTER = new BlockPos(4, 2, 4);
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OFFERING_SOURCES =
            TEST_FUNCTIONS.register("ritual_offering_sources", () -> RitualOfferingGameTests::offeringSources);

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
        helper.assertTrue(SacrificeConsumer.isPending(level, mixedSlime),
                "Dropped offering must be pending during staged consumption");
        helper.assertTrue(SacrificeConsumer.isPending(level, second.getBlockPos()),
                "Grassper offering must be pending during staged consumption");
        helper.assertTrue(RitualOfferings.matchOfferings(
                level, center, RitualOfferings.IMPRISONMENT_RECIPE).isEmpty(),
                "Pending offerings must not satisfy another activation");

        helper.runAfterDelay(45, () -> {
            helper.assertTrue(!mixedSlime.isAlive() || mixedSlime.getItem().isEmpty(),
                    "Dropped offering must be consumed after its staged pop");
            helper.assertTrue(second.getItem().isEmpty(),
                    "Grassper offering must be consumed after its staged pop");
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

    private static ItemEntity drop(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D, stack);
        level.addFreshEntity(item);
        return item;
    }
}
