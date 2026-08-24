package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.HobgoblinEntity;
import com.emoniph.witchery.entity.VampireEntity;
import com.emoniph.witchery.entity.VillageGuardEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryMobEffects;
import com.emoniph.witchery.util.VillagerBlood;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Consumer;

/** Dedicated-server fixture for vampire, guard, and village consequence parity. */
public final class VampireVillageGameTests {
    private static final UUID VAMPIRE_ID = UUID.fromString("b8ae2b62-a713-46c6-a745-d5171e694fc0");
    private static final UUID FRIEND_ID = UUID.fromString("cbbcdada-068c-47ac-b28c-8973be15c5ee");
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CONSEQUENCES =
            TEST_FUNCTIONS.register("vampire_guard_village_consequences",
                    () -> VampireVillageGameTests::villageConsequences);

    private VampireVillageGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(VampireVillageGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "vampire_village"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 180, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID,
                        "vampire_guard_village_consequences"),
                new FunctionGameTestInstance(CONSEQUENCES.getKey(), data));
    }

    private static void villageConsequences(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        placeFloor(level, origin);
        placeVillageBed(level, origin.offset(0, 0, 5));

        Villager villager = EntityType.VILLAGER.create(level, EntitySpawnReason.TRIGGERED);
        VillageGuardEntity guard = WitcheryEntities.VILLAGE_GUARD.get().create(level, EntitySpawnReason.TRIGGERED);
        VillageGuardEntity secondGuard = WitcheryEntities.VILLAGE_GUARD.get().create(level, EntitySpawnReason.TRIGGERED);
        VampireEntity npcVampire = WitcheryEntities.VAMPIRE.get().create(level, EntitySpawnReason.TRIGGERED);
        HobgoblinEntity hobgoblin = WitcheryEntities.HOBGOBLIN.get().create(level, EntitySpawnReason.TRIGGERED);
        Creeper creeper = EntityType.CREEPER.create(level, EntitySpawnReason.TRIGGERED);
        if (villager == null || guard == null || secondGuard == null || npcVampire == null
                || hobgoblin == null || creeper == null) {
            throw new IllegalStateException("Unable to create vampire-village fixture entities");
        }

        villager.snapTo(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D, 0.0F, 0.0F);
        guard.snapTo(origin.getX() + 2.5D, origin.getY(), origin.getZ() + 0.5D, 0.0F, 0.0F);
        secondGuard.snapTo(origin.getX() + 3.5D, origin.getY(), origin.getZ() + 0.5D, 0.0F, 0.0F);
        npcVampire.snapTo(origin.getX() + 6.5D, origin.getY(), origin.getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(villager);
        level.addFreshEntity(guard);
        level.addFreshEntity(secondGuard);
        level.addFreshEntity(npcVampire);

        ServerPlayer vampire = createTestPlayer(level, origin.offset(5, 0, 2), VAMPIRE_ID, "village-vampire");
        ServerPlayer friend = createTestPlayer(level, origin.offset(1, 0, 2), FRIEND_ID, "village-friend");
        WitcheryPlayerData vampireData = WitcheryAttachments.get(vampire);
        vampireData.setVampire(true);
        vampireData.setVampireLevel(1);

        helper.runAfterDelay(30, () -> {
            try {
                helper.assertTrue(guard.getTarget() != vampire,
                        "A guard must not attack a player merely for being a vampire");
                helper.assertTrue(!guard.canDefendVillageAgainst(creeper),
                        "Source guards must refuse Creepers");
                helper.assertTrue(guard.canDefendVillageAgainst(hobgoblin),
                        "Source guards must treat Hobgoblins as village threats");

                VillageSocialEvents.alertVillageGuards(villager, vampire);
                helper.assertTrue(guard.getTarget() == vampire,
                        "An attack on a villager must alert nearby guards");
                guard.setTarget(null);

                VampireEvents.alertBloodDrinkingWitnesses(vampire, villager);
                helper.assertTrue(guard.getTarget() == vampire,
                        "A conscious guard with line of sight must witness vampire feeding");
                guard.setTarget(null);
                guard.addEffect(new MobEffectInstance(WitcheryMobEffects.PARALYSED, 200, 0));
                VampireEvents.alertBloodDrinkingWitnesses(vampire, villager);
                helper.assertTrue(guard.getTarget() == null,
                        "A paralysed guard must not witness vampire feeding");
                guard.removeEffect(WitcheryMobEffects.PARALYSED);

                float guardHealth = guard.getHealth();
                boolean friendlyHit = guard.hurtServer(level,
                        guard.damageSources().mobAttack(secondGuard), 4.0F);
                helper.assertTrue(!friendlyHit && close(guard.getHealth(), guardHealth),
                        "Village Guards must be immune to other Village Guards");

                guard.setGuardType(1);
                helper.assertTrue(guard.fireImmune()
                                && close(guard.getBbWidth(), 0.72F)
                                && close(guard.getBbHeight(), 2.34F),
                        "Guard type 1 must retain its source dimensions and fire immunity");

                VillagerBlood.set(villager, 137);
                guard.captureVillagerOrigin(villager);
                helper.assertTrue(guard.getBlood() == 137,
                        "Bell promotion must preserve the villager's current blood");
                guard.setBlood(88);
                guard.restoreVillagerOrigin(villager);
                helper.assertTrue(VillagerBlood.get(villager) == 88,
                        "Bell dismissal must preserve the guard's current blood");

                VillagerBlood.set(villager, VillagerBlood.MAX);
                float villagerHealth = villager.getHealth();
                npcVampire.setHealth(20.0F);
                int taken = npcVampire.drainVillagerOnBite(level, villager);
                helper.assertTrue(taken == 20 && VillagerBlood.get(villager) == 480,
                        "An awake villager must yield the source-adjusted 20 blood to an NPC vampire");
                helper.assertTrue(close(villager.getHealth(), villagerHealth)
                                && close(npcVampire.getHealth(), 24.0F),
                        "NPC vampire feeding must heal the vampire without ordinary villager melee damage");

                int reputationBefore = villager.getPlayerReputation(vampire);
                guard.hurtServer(level, vampire.damageSources().playerAttack(vampire), Float.MAX_VALUE);
                int reputationAfter = villager.getPlayerReputation(vampire);
                helper.assertTrue(reputationAfter == reputationBefore - 5,
                        "Killing a village-bound guard must cost exactly five village reputation");
                helper.assertTrue(!secondGuard.isHostileToVillage(vampire),
                        "One guard death must not cross the source hostile-reputation threshold");
                villager.getGossips().add(vampire.getUUID(), GossipType.MINOR_NEGATIVE, 10);
                helper.assertTrue(secondGuard.isHostileToVillage(vampire),
                        "Guards must recognize the source -15 hostile village reputation threshold");

                villager.getGossips().add(friend.getUUID(), GossipType.MINOR_POSITIVE, 10);
                helper.assertTrue(secondGuard.protectsVillageFriend(friend),
                        "Guards must recognize a player with exactly +10 village reputation");
                secondGuard.setTarget(null);
                int friendDefenders = VillageSocialEvents.alertGuardsDefendingVillageFriend(friend, vampire);
                helper.assertTrue(secondGuard.getTarget() == vampire,
                        "Guards must defend a +10 village friend from another player; target="
                                + (secondGuard.getTarget() == null ? "null" : secondGuard.getTarget().getName().getString())
                                + ", defenders=" + friendDefenders
                                + ", friendAlive=" + friend.isAlive()
                                + ", friendDistance=" + secondGuard.distanceTo(friend)
                                + ", attackerAlive=" + vampire.isAlive()
                                + ", attackerCreative=" + vampire.isCreative()
                                + ", attackerSpectator=" + vampire.isSpectator());
                helper.succeed();
            } finally {
                villager.discard();
                guard.discard();
                secondGuard.discard();
                npcVampire.discard();
                hobgoblin.discard();
                removeTestPlayer(vampire);
                removeTestPlayer(friend);
            }
        });
    }

    private static void placeVillageBed(ServerLevel level, BlockPos footPos) {
        BlockPos headPos = footPos.relative(Direction.EAST);
        level.setBlockAndUpdate(footPos, Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.EAST)
                .setValue(BedBlock.PART, BedPart.FOOT));
        level.setBlockAndUpdate(headPos, Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.EAST)
                .setValue(BedBlock.PART, BedPart.HEAD));
    }

    private static void placeFloor(ServerLevel level, BlockPos origin) {
        for (int x = -2; x <= 10; x++) {
            for (int z = -2; z <= 8; z++) {
                level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            }
        }
    }

    private static ServerPlayer createTestPlayer(ServerLevel level, BlockPos pos, UUID id, String name) {
        ServerPlayer player = new FakePlayer(level, new GameProfile(id, name));
        player.setInvulnerable(false);
        player.getAbilities().invulnerable = false;
        if (player.connection.getConnection().channel() == null) {
            new EmbeddedChannel(player.connection.getConnection());
        }
        player.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        level.addNewPlayer(player);
        player.setInvulnerable(false);
        player.getAbilities().invulnerable = false;
        return player;
    }

    private static void removeTestPlayer(ServerPlayer player) {
        if (player != null && player.level() instanceof ServerLevel level && !player.isRemoved()) {
            level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
    }

    private static boolean close(float actual, float expected) {
        return Math.abs(actual - expected) < 0.001F;
    }
}
