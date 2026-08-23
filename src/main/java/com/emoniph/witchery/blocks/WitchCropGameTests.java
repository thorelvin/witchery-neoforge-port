package com.emoniph.witchery.blocks;

import com.emoniph.witchery.WitcheryMod;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Consumer;

/** Dedicated-server coverage for source crop survival and Wormwood extension. */
public final class WitchCropGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CROP_PARITY =
            TEST_FUNCTIONS.register("witch_crop_parity", () -> WitchCropGameTests::cropParity);

    private WitchCropGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(WitchCropGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "witch_crops"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 100, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "witch_crop_parity"),
                new FunctionGameTestInstance(CROP_PARITY.getKey(), data));
    }

    private static void cropParity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.setDayTime(1000L);
        BlockPos origin = helper.absolutePos(new BlockPos(2, 300, 2));
        List<CropBlock> ordinaryCrops = List.of(
                WitcheryBlocks.BELLADONNA.get(),
                WitcheryBlocks.MANDRAKE.get(),
                WitcheryBlocks.MINDRAKE.get(),
                WitcheryBlocks.SNOWBELL.get(),
                WitcheryBlocks.WORMWOOD_PLANT.get(),
                WitcheryBlocks.WOLFSBANE_CROP.get(),
                WitcheryBlocks.GARLIC_CROP.get());

        int offset = 0;
        for (CropBlock crop : ordinaryCrops) {
            BlockPos soilPos = origin.offset(offset++, 0, 0);
            assertSurvives(helper, level, soilPos, Blocks.GRASS_BLOCK.defaultBlockState(), crop);
            assertSurvives(helper, level, soilPos, Blocks.DIRT.defaultBlockState(), crop);
            assertSurvives(helper, level, soilPos, Blocks.FARMLAND.defaultBlockState(), crop);
        }

        CropBlock snowbell = WitcheryBlocks.SNOWBELL.get();
        assertSurvives(helper, level, origin.offset(0, 0, 2), Blocks.PODZOL.defaultBlockState(), snowbell);
        assertSurvives(helper, level, origin.offset(1, 0, 2), Blocks.SNOW_BLOCK.defaultBlockState(), snowbell);

        BlockPos water = origin.offset(2, 0, 2);
        level.setBlock(water.north().above(), Blocks.GLOWSTONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(water, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        BlockPos artichokePos = water.above();
        BlockState artichoke = WitcheryBlocks.ARTICHOKE.get().getStateForAge(4);
        helper.assertTrue(artichoke.canSurvive(level, artichokePos),
                "Artichoke must survive above source water");
        level.setBlock(artichokePos, artichoke, Block.UPDATE_ALL);

        BlockPos wormwoodSoil = origin.offset(3, 0, 2);
        BlockPos wormwoodPos = wormwoodSoil.above();
        WormwoodBlock wormwood = WitcheryBlocks.WORMWOOD_PLANT.get();
        BlockState matureWormwood = wormwood.getStateForAge(wormwood.getMaxAge());
        level.setBlock(wormwoodPos.north(), Blocks.GLOWSTONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(wormwoodSoil, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(wormwoodPos, matureWormwood, Block.UPDATE_ALL);
        wormwood.randomTick(matureWormwood, level, wormwoodPos, RandomSource.create(42L));
        helper.assertTrue(level.getBlockState(wormwoodPos.above()).is(wormwood),
                "Mature base Wormwood must extend into air");
        level.setBlock(wormwoodPos.above(), matureWormwood, Block.UPDATE_ALL);
        wormwood.randomTick(matureWormwood, level, wormwoodPos.above(), RandomSource.create(43L));
        helper.assertTrue(level.getBlockState(wormwoodPos.above(2)).isAir(),
                "A Wormwood segment above Wormwood must not extend the tower");

        BlockPos removalSoil = origin.offset(4, 0, 2);
        BlockPos removalCrop = removalSoil.above();
        level.setBlock(removalCrop.north(), Blocks.GLOWSTONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(removalSoil, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(removalCrop, WitcheryBlocks.BELLADONNA.get().getStateForAge(4), Block.UPDATE_ALL);
        level.setBlockAndUpdate(removalSoil, Blocks.STONE.defaultBlockState());
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(level.getBlockState(removalCrop).isAir(),
                    "A witch crop must break after its supporting soil becomes invalid");
            helper.succeed();
        });
    }

    private static void assertSurvives(GameTestHelper helper, ServerLevel level, BlockPos soilPos,
                                       BlockState soil, CropBlock crop) {
        BlockPos cropPos = soilPos.above();
        level.setBlock(cropPos.north(), Blocks.GLOWSTONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(soilPos, soil, Block.UPDATE_ALL);
        BlockState cropState = crop.getStateForAge(crop.getMaxAge());
        helper.assertTrue(cropState.canSurvive(level, cropPos),
                BuiltInRegistries.BLOCK.getKey(crop) + " must survive above "
                        + BuiltInRegistries.BLOCK.getKey(soil.getBlock()));
        level.setBlock(cropPos, cropState, Block.UPDATE_ALL);
        level.setBlock(cropPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }
}
