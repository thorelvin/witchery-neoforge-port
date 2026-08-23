package com.emoniph.witchery.blocks;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.entity.BrazierBlockEntity;
import com.emoniph.witchery.blocks.entity.DistilleryBlockEntity;
import com.emoniph.witchery.blocks.entity.KettleBlockEntity;
import com.emoniph.witchery.blocks.entity.SilverVatBlockEntity;
import com.emoniph.witchery.blocks.entity.SpinningWheelBlockEntity;
import com.emoniph.witchery.blocks.entity.WitchesOvenBlockEntity;
import com.emoniph.witchery.crafting.KettleBrewingRecipe;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryRecipeTypes;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Arrays;
import java.util.function.Consumer;

/** Dedicated-server certification for the source-sided machine inventory contracts. */
public final class MachineAutomationGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> KETTLE =
            TEST_FUNCTIONS.register("kettle_hopper_automation", () -> MachineAutomationGameTests::kettle);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OVEN =
            TEST_FUNCTIONS.register("witches_oven_hopper_automation", () -> MachineAutomationGameTests::oven);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DISTILLERY =
            TEST_FUNCTIONS.register("distillery_hopper_automation", () -> MachineAutomationGameTests::distillery);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SPINNING_WHEEL =
            TEST_FUNCTIONS.register("spinning_wheel_hopper_automation", () -> MachineAutomationGameTests::spinningWheel);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BRAZIER =
            TEST_FUNCTIONS.register("brazier_hopper_automation", () -> MachineAutomationGameTests::brazier);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SILVER_VAT =
            TEST_FUNCTIONS.register("silver_vat_output_automation", () -> MachineAutomationGameTests::silverVat);

    private static final BlockPos MACHINE_POS = new BlockPos(2, 2, 2);

    private MachineAutomationGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(MachineAutomationGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "machine_automation"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 300, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "kettle_hopper_automation"),
                new FunctionGameTestInstance(KETTLE.getKey(), data));
        event.registerTest(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "witches_oven_hopper_automation"),
                new FunctionGameTestInstance(OVEN.getKey(), data));
        event.registerTest(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "distillery_hopper_automation"),
                new FunctionGameTestInstance(DISTILLERY.getKey(), data));
        event.registerTest(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "spinning_wheel_hopper_automation"),
                new FunctionGameTestInstance(SPINNING_WHEEL.getKey(), data));
        event.registerTest(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "brazier_hopper_automation"),
                new FunctionGameTestInstance(BRAZIER.getKey(), data));
        event.registerTest(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "silver_vat_output_automation"),
                new FunctionGameTestInstance(SILVER_VAT.getKey(), data));
    }

    private static void kettle(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(MACHINE_POS);
        level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.below(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        BlockState state = WitcheryBlocks.KETTLE.get().defaultBlockState()
                .setValue(KettleBlock.HANGING, true);
        KettleBlockEntity kettle = placeBlockEntity(helper, level, pos, state, KettleBlockEntity.class);
        kettle.fillWater();

        assertSlots(helper, kettle.getSlotsForFace(Direction.NORTH), 0, 1, 2, 3, 4, 5, 6, 7);
        helper.assertTrue(kettle.canPlaceItemThroughFace(0, new ItemStack(Items.STICK), Direction.WEST),
                "A filled Kettle must accept non-bottle ingredients from every face");
        helper.assertFalse(kettle.canPlaceItemThroughFace(KettleBlockEntity.RESULT_SLOT,
                        new ItemStack(Items.STICK), Direction.WEST),
                "Kettle result slot must reject automation insertion");
        helper.assertTrue(kettle.canPlaceItemThroughFace(KettleBlockEntity.BOTTLE_SLOT,
                        new ItemStack(Items.GLASS_BOTTLE), Direction.WEST),
                "Kettle bottle slot must accept glass bottles");

        HopperBlockEntity input = placeHopper(helper, level, pos.west(), Direction.EAST,
                new ItemStack(Items.STICK));
        helper.runAfterDelay(12, () -> verifyKettleIngredient(helper, level, pos, kettle, input));
    }

    private static void verifyKettleIngredient(GameTestHelper helper, ServerLevel level, BlockPos pos,
                                               KettleBlockEntity kettle, HopperBlockEntity input) {
        helper.assertTrue(kettle.getItem(0).is(Items.STICK) && input.isEmpty(),
                "A side hopper must insert a Kettle ingredient into slot 0");
        kettle.setItem(0, ItemStack.EMPTY);

        KettleBrewingRecipe recipe = null;
        for (var holder : level.recipeAccess().recipeMap().byType(WitcheryRecipeTypes.KETTLE_BREWING.get())) {
            if (holder.value().isBrewableBy(null)) {
                recipe = holder.value();
                break;
            }
        }
        helper.assertTrue(recipe != null, "At least one non-familiar Kettle recipe must be loaded");
        ItemStack storedResult = recipe.createStoredResult();
        int resultCount = storedResult.getCount();
        input.setItem(0, new ItemStack(Items.GLASS_BOTTLE, resultCount));

        KettleBrewingRecipe selectedRecipe = recipe;
        helper.runAfterDelay(resultCount * 8 + 12, () -> {
            helper.assertTrue(kettle.bottleCount() == resultCount && input.isEmpty(),
                    "A side hopper must route glass bottles to Kettle slot 7");
            kettle.setItem(KettleBlockEntity.RESULT_SLOT, selectedRecipe.createStoredResult());
            HopperBlockEntity output = placeHopper(helper, level, pos.below(), Direction.DOWN, ItemStack.EMPTY);
            helper.runAfterDelay(resultCount * 8 + 12,
                    () -> finishKettle(helper, kettle, output, storedResult, resultCount));
        });
    }

    private static void finishKettle(GameTestHelper helper, KettleBlockEntity kettle,
                                     HopperBlockEntity output, ItemStack expected, int expectedCount) {
        helper.assertTrue(matchingCount(output, expected) == expectedCount,
                "A lower hopper must extract every stored Kettle result");
        helper.assertTrue(kettle.getItem(KettleBlockEntity.RESULT_SLOT).isEmpty()
                        && kettle.getItem(KettleBlockEntity.BOTTLE_SLOT).isEmpty(),
                "Automated Kettle extraction must consume one bottle per result");
        helper.assertTrue(kettle.getWaterLevel() == 0,
                "Extracting the final automated Kettle result must drain and reset it");
        helper.succeed();
    }

    private static void oven(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(MACHINE_POS);
        level.setBlock(pos.below(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        WitchesOvenBlockEntity oven = placeBlockEntity(helper, level, pos,
                WitcheryBlocks.WITCHES_OVEN.get().defaultBlockState(), WitchesOvenBlockEntity.class);

        assertSlots(helper, oven.getSlotsForFace(Direction.UP),
                WitchesOvenBlockEntity.SLOT_INPUT, WitchesOvenBlockEntity.SLOT_JARS);
        assertSlots(helper, oven.getSlotsForFace(Direction.DOWN),
                WitchesOvenBlockEntity.SLOT_JARS, WitchesOvenBlockEntity.SLOT_FUEL);
        assertSlots(helper, oven.getSlotsForFace(Direction.NORTH),
                WitchesOvenBlockEntity.SLOT_BYPRODUCT, WitchesOvenBlockEntity.SLOT_COOKED,
                WitchesOvenBlockEntity.SLOT_JARS, WitchesOvenBlockEntity.SLOT_FUEL);
        helper.assertTrue(oven.canTakeItemThroughFace(WitchesOvenBlockEntity.SLOT_COOKED,
                        new ItemStack(Items.CHARCOAL), Direction.NORTH),
                "Oven cooked output must extract from horizontal faces");
        helper.assertFalse(oven.canTakeItemThroughFace(WitchesOvenBlockEntity.SLOT_COOKED,
                        new ItemStack(Items.CHARCOAL), Direction.DOWN),
                "Oven cooked output must not extract downward");

        HopperBlockEntity top = placeHopper(helper, level, pos.above(), Direction.DOWN,
                new ItemStack(WitcheryItems.CLAY_JAR.get(), 2));
        helper.runAfterDelay(20, () -> verifyOvenJars(helper, level, pos, oven, top));
    }

    private static void verifyOvenJars(GameTestHelper helper, ServerLevel level, BlockPos pos,
                                       WitchesOvenBlockEntity oven, HopperBlockEntity top) {
        helper.assertTrue(oven.getItem(WitchesOvenBlockEntity.SLOT_JARS).getCount() == 2 && top.isEmpty(),
                "A top hopper must route clay jars to the Oven jar slot");
        top.setItem(0, new ItemStack(Items.OAK_LOG));
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(oven.getItem(WitchesOvenBlockEntity.SLOT_INPUT).is(Items.OAK_LOG),
                    "A top hopper must route non-jars to the Oven input");
            oven.setItem(WitchesOvenBlockEntity.SLOT_INPUT, ItemStack.EMPTY);
            HopperBlockEntity side = placeHopper(helper, level, pos.west(), Direction.EAST,
                    new ItemStack(Items.COAL));
            helper.runAfterDelay(12, () -> verifyOvenFuel(helper, level, pos, oven, side));
        });
    }

    private static void verifyOvenFuel(GameTestHelper helper, ServerLevel level, BlockPos pos,
                                       WitchesOvenBlockEntity oven, HopperBlockEntity side) {
        helper.assertTrue(oven.getItem(WitchesOvenBlockEntity.SLOT_FUEL).is(Items.COAL) && side.isEmpty(),
                "A side hopper must route valid fuel to the Oven fuel slot");
        oven.setItem(WitchesOvenBlockEntity.SLOT_FUEL, new ItemStack(Items.BUCKET));
        HopperBlockEntity output = placeHopper(helper, level, pos.below(), Direction.DOWN, ItemStack.EMPTY);
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(hopperCount(output, Items.BUCKET) == 1
                            && oven.getItem(WitchesOvenBlockEntity.SLOT_FUEL).isEmpty(),
                    "A lower hopper must extract only the Oven fuel-slot bucket remainder");
            helper.succeed();
        });
    }

    private static void distillery(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(MACHINE_POS);
        level.setBlock(pos.below(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        DistilleryBlockEntity distillery = placeBlockEntity(helper, level, pos,
                WitcheryBlocks.DISTILLERY.get().defaultBlockState(), DistilleryBlockEntity.class);

        assertSlots(helper, distillery.getSlotsForFace(Direction.UP), 0, 1, 2);
        assertSlots(helper, distillery.getSlotsForFace(Direction.DOWN), 0, 1, 2);
        assertSlots(helper, distillery.getSlotsForFace(Direction.NORTH), 0, 1, 2, 3, 4, 5, 6);
        helper.assertTrue(distillery.canPlaceItemThroughFace(DistilleryBlockEntity.SLOT_OUTPUT_1,
                        new ItemStack(Items.COBBLESTONE), Direction.NORTH),
                "The source Distillery quirk must allow horizontal insertion into output slot 3");
        helper.assertFalse(distillery.canPlaceItemThroughFace(DistilleryBlockEntity.SLOT_OUTPUT_2,
                        new ItemStack(Items.COBBLESTONE), Direction.NORTH),
                "Distillery output slots 4-6 must reject insertion");
        helper.assertTrue(distillery.canTakeItemThroughFace(DistilleryBlockEntity.SLOT_OUTPUT_1,
                        new ItemStack(Items.COBBLESTONE), Direction.NORTH),
                "Distillery outputs must extract horizontally");
        helper.assertFalse(distillery.canTakeItemThroughFace(DistilleryBlockEntity.SLOT_OUTPUT_1,
                        new ItemStack(Items.COBBLESTONE), Direction.DOWN),
                "Distillery outputs must not extract vertically");

        HopperBlockEntity top = placeHopper(helper, level, pos.above(), Direction.DOWN,
                new ItemStack(WitcheryItems.CLAY_JAR.get()));
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(distillery.getItem(DistilleryBlockEntity.SLOT_JARS)
                            .is(WitcheryItems.CLAY_JAR.get()),
                    "A top hopper must route clay jars to the Distillery jar slot");
            distillery.setItem(DistilleryBlockEntity.SLOT_INPUT_1, new ItemStack(Items.STONE));
            distillery.setItem(DistilleryBlockEntity.SLOT_INPUT_2, new ItemStack(Items.DIRT));
            HopperBlockEntity side = placeHopper(helper, level, pos.west(), Direction.EAST,
                    new ItemStack(Items.COBBLESTONE));
            helper.runAfterDelay(12,
                    () -> verifyDistilleryOutputQuirk(helper, level, pos, distillery, side));
        });
    }

    private static void verifyDistilleryOutputQuirk(GameTestHelper helper, ServerLevel level, BlockPos pos,
                                                    DistilleryBlockEntity distillery, HopperBlockEntity side) {
        helper.assertTrue(distillery.getItem(DistilleryBlockEntity.SLOT_OUTPUT_1).is(Items.COBBLESTONE)
                        && side.isEmpty(),
                "A horizontal hopper must reproduce the source first-output insertion quirk");
        HopperBlockEntity below = placeHopper(helper, level, pos.below(), Direction.DOWN, ItemStack.EMPTY);
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(below.isEmpty()
                            && distillery.getItem(DistilleryBlockEntity.SLOT_OUTPUT_1).is(Items.COBBLESTONE),
                    "A lower hopper must not bypass the Distillery's horizontal-only output rule");
            helper.succeed();
        });
    }

    private static void spinningWheel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(MACHINE_POS);
        level.setBlock(pos.below(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        SpinningWheelBlockEntity wheel = placeBlockEntity(helper, level, pos,
                WitcheryBlocks.SPINNING_WHEEL.get().defaultBlockState(), SpinningWheelBlockEntity.class);

        assertSlots(helper, wheel.getSlotsForFace(Direction.UP), SpinningWheelBlockEntity.SLOT_FIBRE);
        assertSlots(helper, wheel.getSlotsForFace(Direction.DOWN),
                SpinningWheelBlockEntity.SLOT_MODIFIER_3, SpinningWheelBlockEntity.SLOT_MODIFIER_1,
                SpinningWheelBlockEntity.SLOT_MODIFIER_2);
        assertSlots(helper, wheel.getSlotsForFace(Direction.NORTH),
                SpinningWheelBlockEntity.SLOT_MODIFIER_2, SpinningWheelBlockEntity.SLOT_OUTPUT,
                SpinningWheelBlockEntity.SLOT_MODIFIER_3, SpinningWheelBlockEntity.SLOT_MODIFIER_1);
        helper.assertTrue(wheel.canTakeItemThroughFace(SpinningWheelBlockEntity.SLOT_OUTPUT,
                        new ItemStack(Items.WHITE_WOOL), Direction.NORTH),
                "Spinning Wheel output must extract horizontally");
        helper.assertFalse(wheel.canTakeItemThroughFace(SpinningWheelBlockEntity.SLOT_OUTPUT,
                        new ItemStack(Items.WHITE_WOOL), Direction.DOWN),
                "Spinning Wheel output must not extract downward");

        HopperBlockEntity top = placeHopper(helper, level, pos.above(), Direction.DOWN,
                new ItemStack(Items.STRING));
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(wheel.getItem(SpinningWheelBlockEntity.SLOT_FIBRE).is(Items.STRING)
                            && top.isEmpty(),
                    "A top hopper must insert Spinning Wheel fibre");
            HopperBlockEntity side = placeHopper(helper, level, pos.west(), Direction.EAST,
                    new ItemStack(Items.IRON_INGOT));
            helper.runAfterDelay(12, () -> verifySpinningModifier(helper, level, pos, wheel, side));
        });
    }

    private static void verifySpinningModifier(GameTestHelper helper, ServerLevel level, BlockPos pos,
                                               SpinningWheelBlockEntity wheel, HopperBlockEntity side) {
        helper.assertTrue(wheel.getItem(SpinningWheelBlockEntity.SLOT_MODIFIER_2).is(Items.IRON_INGOT)
                        && side.isEmpty(),
                "A side hopper must use the first exposed Spinning Wheel modifier slot");
        wheel.setItem(SpinningWheelBlockEntity.SLOT_OUTPUT, new ItemStack(Items.WHITE_WOOL));
        HopperBlockEntity below = placeHopper(helper, level, pos.below(), Direction.DOWN, ItemStack.EMPTY);
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(below.isEmpty()
                            && wheel.getItem(SpinningWheelBlockEntity.SLOT_OUTPUT).is(Items.WHITE_WOOL),
                    "A lower hopper must not bypass the Spinning Wheel's side-only output rule");
            helper.succeed();
        });
    }

    private static void brazier(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(MACHINE_POS);
        level.setBlock(pos.below(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        BrazierBlockEntity brazier = placeBlockEntity(helper, level, pos,
                WitcheryBlocks.BRAZIER.get().defaultBlockState(), BrazierBlockEntity.class);

        assertSlots(helper, brazier.getSlotsForFace(Direction.UP), 0, 1, 2);
        assertSlots(helper, brazier.getSlotsForFace(Direction.DOWN), 0, 1, 2);
        assertSlots(helper, brazier.getSlotsForFace(Direction.NORTH), 0, 1, 2);
        helper.assertFalse(brazier.canTakeItemThroughFace(BrazierBlockEntity.SLOT_INGREDIENT_1,
                        new ItemStack(Items.STICK), Direction.DOWN),
                "Brazier ingredients must never be automation-extractable");

        HopperBlockEntity top = placeHopper(helper, level, pos.above(), Direction.DOWN,
                new ItemStack(Items.STICK, 3));
        HopperBlockEntity below = placeHopper(helper, level, pos.below(), Direction.DOWN, ItemStack.EMPTY);
        helper.runAfterDelay(32, () -> {
            helper.assertTrue(top.isEmpty(),
                    "A hopper must be able to feed three single-count Brazier ingredients");
            helper.assertTrue(brazier.getItem(0).is(Items.STICK) && brazier.getItem(0).getCount() == 1
                            && brazier.getItem(1).is(Items.STICK) && brazier.getItem(1).getCount() == 1
                            && brazier.getItem(2).is(Items.STICK) && brazier.getItem(2).getCount() == 1,
                    "Brazier automation must distribute one item into each ingredient slot");
            helper.assertTrue(below.isEmpty(),
                    "A lower hopper must not remove Brazier ingredients");
            helper.succeed();
        });
    }

    private static void silverVat(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(MACHINE_POS);
        level.setBlock(pos.below(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        SilverVatBlockEntity vat = placeBlockEntity(helper, level, pos,
                WitcheryBlocks.SILVER_VAT.get().defaultBlockState(), SilverVatBlockEntity.class);
        BlockPos furnacePos = pos.east();
        Container furnace = placeContainer(helper, level, furnacePos, Blocks.FURNACE.defaultBlockState());

        long seed = 0x51_17_3AL;
        int trials = 40;
        RandomSource expectedRandom = RandomSource.create(seed);
        int expectedDeposits = 0;
        for (int i = 0; i < trials; i++) {
            if (expectedRandom.nextInt(5) == 0) {
                expectedDeposits++;
            }
        }
        helper.assertTrue(expectedDeposits > 0, "Silver Vat deterministic seed must produce deposits");

        level.random.setSeed(seed);
        for (int i = 0; i < trials; i++) {
            furnace.setItem(2, ItemStack.EMPTY);
            vat.checkAdjacentGoldOutputs(level, pos);
            furnace.setItem(2, new ItemStack(Items.GOLD_INGOT));
            vat.checkAdjacentGoldOutputs(level, pos);
        }

        helper.assertTrue(vat.getStored().is(WitcheryItems.SILVER_DEPOSITS.get())
                        && vat.getStored().getCount() == expectedDeposits,
                "Silver Vat must create exactly one deposit on each successful 1-in-5 output-growth roll; expected="
                        + expectedDeposits + ", actual=" + vat.getStored().getCount());
        helper.assertTrue(furnace.getItem(2).is(Items.GOLD_INGOT) && furnace.getItem(2).getCount() == 1,
                "Silver Vat observation must not consume the adjacent gold output");

        int generatedDeposits = expectedDeposits;
        HopperBlockEntity below = placeHopper(helper, level, pos.below(), Direction.DOWN, ItemStack.EMPTY);
        helper.runAfterDelay(generatedDeposits * 8 + 12,
                () -> verifySilverVatExtraction(helper, level, pos, vat, below, generatedDeposits));
    }

    private static void verifySilverVatExtraction(GameTestHelper helper, ServerLevel level, BlockPos pos,
                                                  SilverVatBlockEntity vat, HopperBlockEntity below,
                                                  int expectedDeposits) {
        helper.assertTrue(vat.getStored().isEmpty()
                        && hopperCount(below, WitcheryItems.SILVER_DEPOSITS.get()) == expectedDeposits,
                "A lower hopper must extract all generated Silver Deposits");
        HopperBlockEntity above = placeHopper(helper, level, pos.above(), Direction.DOWN,
                new ItemStack(WitcheryItems.SILVER_DEPOSITS.get()));
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(above.getItem(0).is(WitcheryItems.SILVER_DEPOSITS.get())
                            && vat.getStored().isEmpty(),
                    "Silver Vat must reject hopper insertion like the source IInventory");
            helper.succeed();
        });
    }

    private static HopperBlockEntity placeHopper(GameTestHelper helper, ServerLevel level,
                                                  BlockPos pos, Direction facing, ItemStack stack) {
        BlockState state = Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, facing);
        HopperBlockEntity hopper = placeBlockEntity(helper, level, pos, state, HopperBlockEntity.class);
        if (!stack.isEmpty()) {
            hopper.setItem(0, stack.copy());
        }
        return hopper;
    }

    private static Container placeContainer(GameTestHelper helper, ServerLevel level,
                                            BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_ALL);
        BlockEntity entity = level.getBlockEntity(pos);
        helper.assertTrue(entity instanceof Container,
                "Expected a container block entity at " + pos + ", found " + entity);
        return (Container) entity;
    }

    private static <T extends BlockEntity> T placeBlockEntity(GameTestHelper helper, ServerLevel level,
                                                               BlockPos pos, BlockState state, Class<T> type) {
        level.setBlock(pos, state, Block.UPDATE_ALL);
        BlockEntity entity = level.getBlockEntity(pos);
        helper.assertTrue(type.isInstance(entity),
                "Expected " + type.getSimpleName() + " at " + pos + ", found " + entity);
        return type.cast(entity);
    }

    private static void assertSlots(GameTestHelper helper, int[] actual, int... expected) {
        helper.assertTrue(Arrays.equals(actual, expected),
                "Wrong sided slots: expected=" + Arrays.toString(expected)
                        + ", actual=" + Arrays.toString(actual));
    }

    private static int matchingCount(Container container, ItemStack expected) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (ItemStack.isSameItemSameComponents(stack, expected)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int hopperCount(HopperBlockEntity hopper, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
            ItemStack stack = hopper.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
