package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.brewing.CursedBlockData;
import com.emoniph.witchery.brewing.CursedBlockWarningData;
import com.emoniph.witchery.command.HeadlessGameTestPlayer;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Consumer;

/** Real server-tick contact coverage for the warning-only and consuming cursed-well ladders. */
public final class WitchWellGameTests {
    private static final UUID PLAYER_ID = UUID.fromString("c6f30703-c35e-4fb8-bf54-40d7a7640461");
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CURSED_LADDERS =
            TEST_FUNCTIONS.register("witch_well_cursed_ladder_triggers",
                    () -> WitchWellGameTests::cursedLadderTriggers);

    private WitchWellGameTests() {}

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(WitchWellGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "witch_well_cursed_ladders"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 80, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID,
                        "witch_well_cursed_ladder_triggers"),
                new FunctionGameTestInstance(CURSED_LADDERS.getKey(), data));
    }

    private static void cursedLadderTriggers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos bottom = helper.absolutePos(new BlockPos(3, 3, 3));
        BlockPos middle = bottom.above();
        BlockPos top = middle.above();
        for (BlockPos pos : new BlockPos[]{bottom, middle, top}) {
            level.setBlock(pos.south(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos, Blocks.LADDER.defaultBlockState()
                    .setValue(LadderBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        }
        WitchWellPiece.seedCaveEntryTriggers(level, top, bottom);

        CursedBlockWarningData.Warning warning = CursedBlockWarningData.get(level).get(top);
        helper.assertTrue(warning != null
                        && WitchWellPiece.CAVE_ENTRY_WARNING.equals(warning.message())
                        && warning.cooldownTicks() == WitchWellPiece.CAVE_ENTRY_WARNING_COOLDOWN_TICKS,
                "The top ladder must keep the exact warning and five-second cooldown");
        helper.assertTrue(CursedBlockData.get(level).get(top) == null,
                "The warning-only top ladder must not carry poison");
        helper.assertTrue(CursedBlockData.get(level).get(bottom) != null,
                "The bottom ladder must begin with one consuming curse charge");

        ServerPlayer player = HeadlessGameTestPlayer.create(level, PLAYER_ID, "WellDelver");
        player.setGameMode(GameType.SURVIVAL);
        player.setNoGravity(true);
        player.snapTo(top.getX() + 0.5D, top.getY(), top.getZ() + 0.5D, 0.0F, 0.0F);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(!player.hasEffect(MobEffects.POISON),
                    "Touching the warning ladder must not apply the lower poison curse");
            helper.assertTrue(CursedBlockData.get(level).get(bottom) != null,
                    "The warning ladder must not consume the lower ladder charge");

            player.snapTo(bottom.getX() + 2.5D, bottom.getY(), bottom.getZ() + 0.5D, 0.0F, 0.0F);
            helper.runAfterDelay(2, () -> {
                player.snapTo(bottom.getX() + 0.5D, bottom.getY(), bottom.getZ() + 0.5D, 0.0F, 0.0F);
                helper.runAfterDelay(3, () -> {
                    helper.assertTrue(level.getBlockState(bottom).is(Blocks.LADDER),
                            "The lower fixture ladder must survive its support updates");
                    helper.assertTrue(CursedBlockData.get(level).get(bottom) == null,
                            "Entering the lower ladder must consume its single curse charge");
                    helper.assertTrue(player.hasEffect(MobEffects.POISON),
                            "Entering the lower ladder must apply the authored poison payload");
                    helper.assertTrue(CursedBlockWarningData.get(level).get(top) != null,
                            "The atmospheric warning must remain after the real curse fires");
                    CursedBlockWarningData.get(level).remove(top);
                    HeadlessGameTestPlayer.remove(player);
                    helper.succeed();
                });
            });
        });
    }
}
