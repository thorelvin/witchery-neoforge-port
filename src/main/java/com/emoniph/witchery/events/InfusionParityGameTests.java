package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.command.HeadlessGameTestPlayer;
import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
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
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Dedicated-server contract for source-parity infusion tick behavior. */
public final class InfusionParityGameTests {
    private static final int STARTING_ENERGY = 200;
    private static final List<InfusionType> TYPES = List.of(
            InfusionType.LIGHT, InfusionType.OVERWORLD,
            InfusionType.OTHERWHERE, InfusionType.INFERNAL);
    private static final List<UUID> PLAYER_IDS = List.of(
            UUID.fromString("d0cf68d0-61f5-4ef2-891c-d08dc4eefb0e"),
            UUID.fromString("2837cf45-b012-4dde-b5c8-17a1002df001"),
            UUID.fromString("12fea57c-9ec0-426e-9e57-cc3852cf99bb"),
            UUID.fromString("8ad3bd98-72e9-4577-890c-4f0bd91438a1"));
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PASSIVE_PARITY =
            TEST_FUNCTIONS.register("infusion_passive_parity",
                    () -> InfusionParityGameTests::passiveParity);

    private InfusionParityGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(InfusionParityGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "infusion_parity"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 40, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "infusion_passive_parity"),
                new FunctionGameTestInstance(PASSIVE_PARITY.getKey(), data));
    }

    private static void passiveParity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 2, 1));
        List<ServerPlayer> players = new ArrayList<>();
        List<BlockPos> floors = new ArrayList<>();
        try {
            for (int index = 0; index < TYPES.size(); index++) {
                InfusionType type = TYPES.get(index);
                BlockPos standing = origin.offset(index * 2, 0, 0);
                BlockPos floor = standing.below();
                level.setBlock(floor, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
                floors.add(floor);

                ServerPlayer player = HeadlessGameTestPlayer.create(
                        level, PLAYER_IDS.get(index), "infusion-" + type.name().toLowerCase());
                player.snapTo(standing.getX() + 0.5D, standing.getY(), standing.getZ() + 0.5D,
                        0.0F, 0.0F);
                players.add(player);

                WitcheryPlayerData data = WitcheryAttachments.get(player);
                data.setInfusionType(type);
                data.setInfusionEnergy(STARTING_ENERGY, STARTING_ENERGY);
                InfusionEvents.onInfusionGranted(player, type);
            }

            helper.assertTrue(players.get(3).hasEffect(MobEffects.DARKNESS),
                    "Infernal infusion must retain its one-time grant portent");

            for (ServerPlayer player : players) {
                player.tickCount = 100;
                InfusionEvents.onPlayerTick(new PlayerTickEvent.Post(player));
            }
            assertNoEffect(helper, players.get(0), MobEffects.NIGHT_VISION, "Light");
            assertNoEffect(helper, players.get(1), MobEffects.REGENERATION, "Overworld");
            assertNoEffect(helper, players.get(2), MobEffects.SLOW_FALLING, "Otherwhere");
            assertNoEffect(helper, players.get(3), MobEffects.FIRE_RESISTANCE, "Infernal");
            for (ServerPlayer player : players) {
                WitcheryPlayerData data = WitcheryAttachments.get(player);
                helper.assertTrue(data.getInfusionEnergy() == STARTING_ENERGY,
                        "Ambient infusion ticks must not spend energy for " + data.getInfusionType());
            }
            helper.succeed();
        } finally {
            cleanUp(level, players, floors);
        }
    }

    private static void assertNoEffect(GameTestHelper helper, ServerPlayer player,
                                       Holder<MobEffect> effect, String infusion) {
        helper.assertFalse(player.hasEffect(effect),
                infusion + " infusion must not grant a recurring identity potion effect");
    }

    private static void cleanUp(ServerLevel level, List<ServerPlayer> players, List<BlockPos> floors) {
        players.forEach(HeadlessGameTestPlayer::remove);
        floors.forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
    }
}
