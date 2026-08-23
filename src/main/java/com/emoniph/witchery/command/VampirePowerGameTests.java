package com.emoniph.witchery.command;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.AttackBatEntity;
import com.emoniph.witchery.events.VampireEvents;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Consumer;

/** Integrated-server activation fixture for Speed, Bat, and the Swarm ultimate. */
public final class VampirePowerGameTests {
    private static final UUID PLAYER_ID = UUID.fromString("e66b3211-34e2-4d35-864d-2f4553886aea");
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ACTIVATION =
            TEST_FUNCTIONS.register("vampire_power_activation", () -> VampirePowerGameTests::activation);

    private VampirePowerGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(VampirePowerGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "vampire_power"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 120, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "vampire_power_activation"),
                new FunctionGameTestInstance(ACTIVATION.getKey(), data));
    }

    private static void activation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 2, 1));
        ServerPlayer player = createTestPlayer(level, origin);
        try {
            WitcheryPlayerData data = WitcheryAttachments.get(player);
            data.setVampire(true);
            data.setVampireLevel(7);
            data.setBloodPower(500);

            VampireEvents.handleVampirePowerRequest(player, WitcheryPlayerData.VampirePower.SPEED, true);
            helper.assertTrue(player.hasEffect(MobEffects.SPEED) && player.hasEffect(MobEffects.JUMP_BOOST),
                    "Speed power must apply both source potion effects");
            helper.assertTrue(data.getBloodPower() == 490 && data.getVampirePowerCooldown() == 10,
                    "Speed power must spend ten Blood Power and start the global cooldown");

            data.setVampirePowerCooldown(0);
            VampireEvents.handleVampirePowerRequest(player, WitcheryPlayerData.VampirePower.BAT, true);
            helper.assertTrue(data.isVampireBatForm() && data.getBloodPower() == 440,
                    "Bat power must spend fifty Blood Power and persist the form");
            helper.assertTrue(player.getAbilities().mayfly && player.getAbilities().flying,
                    "Bat form must grant and engage survival flight");
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.STEP_HEIGHT) - 0.5D) < 0.001D,
                    "Bat form must use the source half-block step height");

            data.setVampirePowerCooldown(0);
            VampireEvents.handleVampirePowerRequest(player, WitcheryPlayerData.VampirePower.BAT, true);
            helper.assertTrue(!data.isVampireBatForm()
                            && !player.getAbilities().mayfly && !player.getAbilities().flying,
                    "Leaving Bat form must be free and revoke survival flight");

            data.setVampireLevel(10);
            data.setVampireUltimate(WitcheryPlayerData.VampireUltimate.SWARM, 5);
            data.setVampirePowerCooldown(0);
            AABB bats = player.getBoundingBox().inflate(8.0D);
            int before = level.getEntitiesOfClass(AttackBatEntity.class, bats).size();
            VampireEvents.handleVampirePowerRequest(player, WitcheryPlayerData.VampirePower.ULTIMATE, true);
            int after = level.getEntitiesOfClass(AttackBatEntity.class, bats).size();
            helper.assertTrue(after - before == 15 && data.getVampireUltimateCharges() == 4,
                    "Swarm must spawn fifteen owned attack bats and spend one crucible charge");
            helper.succeed();
        } finally {
            removeTestPlayer(player);
        }
    }

    private static ServerPlayer createTestPlayer(ServerLevel level, BlockPos pos) {
        ServerPlayer player = new FakePlayer(level, new GameProfile(PLAYER_ID, "vampire-power"));
        if (player.connection.getConnection().channel() == null) {
            new EmbeddedChannel(player.connection.getConnection());
        }
        player.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        level.addNewPlayer(player);
        return player;
    }

    private static void removeTestPlayer(ServerPlayer player) {
        if (player != null && player.level() instanceof ServerLevel level && !player.isRemoved()) {
            level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
    }
}
