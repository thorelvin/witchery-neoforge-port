package com.emoniph.witchery.item;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.brewing.Brews;
import com.emoniph.witchery.events.EnslavedMobEvents;
import com.emoniph.witchery.init.WitcheryMobEffects;
import com.emoniph.witchery.util.Enslaved;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Consumer;

/** Dedicated-server proof for source infernal ownership on raised and bred undead. */
public final class InfernalOwnershipGameTests {
    private static final BlockPos CENTER = new BlockPos(8, 3, 8);
    private static final UUID OWNER_ID = UUID.fromString("9a3fd9e3-5a8c-4d32-86c8-f5b5dc41c421");
    private static final UUID RIVAL_ID = UUID.fromString("84d8ef35-46cc-4816-bb2f-9fef56643257");
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OWNERSHIP_LIFECYCLE =
            TEST_FUNCTIONS.register("infernal_ownership_lifecycle", () -> InfernalOwnershipGameTests::ownershipLifecycle);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RAISED_UNDEAD =
            TEST_FUNCTIONS.register("infernal_raised_undead", () -> InfernalOwnershipGameTests::raisedUndead);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BRED_UNDEAD =
            TEST_FUNCTIONS.register("infernal_bred_undead", () -> InfernalOwnershipGameTests::bredUndead);

    private InfernalOwnershipGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(InfernalOwnershipGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "infernal_ownership"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 200, 0,
                true, Rotation.NONE, true, 1, 1, false);
        registerTest(event, "infernal_ownership_lifecycle", OWNERSHIP_LIFECYCLE, data);
        registerTest(event, "infernal_raised_undead", RAISED_UNDEAD, data);
        registerTest(event, "infernal_bred_undead", BRED_UNDEAD, data);
    }

    private static void registerTest(RegisterGameTestsEvent event, String id,
                                     DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> function,
                                     TestData<Holder<TestEnvironmentDefinition>> data) {
        event.registerTest(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, id),
                new FunctionGameTestInstance(function.getKey(), data));
    }

    private static void ownershipLifecycle(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(CENTER);
        ServerPlayer owner = createTestPlayer(level, center, OWNER_ID, "infernal-owner");
        ServerPlayer rival = createTestPlayer(level, center.offset(2, 0, 0), RIVAL_ID, "infernal-rival");
        Zombie thrall = spawnZombie(level, center.offset(64, 0, 0));
        IronGolem attacker = spawnIronGolem(level, center.offset(4, 0, 0));
        helper.runAfterDelay(1, () -> {
            try {
                helper.assertTrue(!thrall.isPersistenceRequired(),
                        "A naturally bindable mob must begin without forced persistence");
                helper.assertTrue(Enslaved.enslave(thrall, owner), "The first owner binding must succeed");
                assertOwner(helper, thrall, owner);
                helper.assertTrue(!thrall.isPersistenceRequired(),
                        "Source infernal binding must not make an arbitrary mob persistent");

                thrall.setTarget(owner);
                helper.assertTrue(thrall.getTarget() == null,
                        "An enslaved mob must never retain its enslaver as attack target");

                helper.assertTrue(thrall.canAttack(attacker),
                        "The lifecycle fixture attacker must be a valid zombie target");
                var damage = level.damageSources().mobAttack(attacker);
                EnslavedMobEvents.onEnslaverHurt(
                        new LivingDamageEvent.Post(owner, new DamageContainer(damage, 0.0F)));
                helper.assertTrue(thrall.getTarget() == null,
                        "A blocked hit that removes no health must not alert thralls");
                float healthBeforeDamage = owner.getHealth();
                owner.setHealth(healthBeforeDamage - 2.0F);
                EnslavedMobEvents.onEnslaverHurt(
                        new LivingDamageEvent.Post(owner, new DamageContainer(damage, 2.0F)));
                helper.assertTrue(owner.getHealth() < healthBeforeDamage,
                        "The lifecycle fixture must model the post-damage health state");
                helper.assertTrue(thrall.getTarget() == attacker,
                        "A loaded pathfinding thrall 64 blocks away must answer its enslaver's hurt event");

                thrall.setTarget(null);
                thrall.removeEffect(WitcheryMobEffects.ENSLAVED);
                thrall.setTarget(owner);
                helper.assertTrue(thrall.getTarget() == null,
                        "The ownership tag must still prevent owner targeting after the curable effect is removed");
                helper.assertTrue(!Enslaved.enslave(thrall, owner),
                        "Rebinding by the same owner must remain a source no-op");
                helper.assertTrue(!Enslaved.hasActiveBond(thrall),
                        "A same-owner no-op must not recreate a cured Enslaved effect");
                helper.assertTrue(EnslavedMobEvents.alertThralls(level, owner, attacker) == 0,
                        "A cured ownership effect must suppress revenge AI");

                helper.assertTrue(Enslaved.enslave(thrall, rival),
                        "A different owner must be able to replace the old binding");
                assertOwner(helper, thrall, rival);
                helper.assertTrue(!thrall.isPersistenceRequired(),
                        "Rebinding must not change the mob's despawn rules");
                helper.succeed();
            } finally {
                discard(thrall);
                discard(attacker);
                removeTestPlayer(owner);
                removeTestPlayer(rival);
            }
        });
    }

    private static void raisedUndead(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos floor = helper.absolutePos(CENTER);
        ServerPlayer owner = createTestPlayer(level, floor.offset(3, 0, 0), OWNER_ID, "raising-owner");
        Mob raised = null;
        try {
            level.setBlock(floor, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            raised = BrewOfRaisingItem.raiseUndead(level, floor, owner);
            helper.assertTrue(raised != null && raised.isAlive(), "Brew of Raising must create one live undead");
            assertOwner(helper, raised, owner);
            helper.assertTrue(raised.isPersistenceRequired(), "Raised undead must be persistent");
            helper.assertTrue(raised.getPersistentData().getBooleanOr(Brews.SUMMONED_NO_DROPS_TAG, false),
                    "Raised undead must retain the source no-drops marker");
            helper.succeed();
        } finally {
            discard(raised);
            removeTestPlayer(owner);
        }
    }

    private static void bredUndead(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(CENTER);
        ServerPlayer owner = createTestPlayer(level, center.offset(4, 0, 0), OWNER_ID, "breeding-owner");
        Zombie first = spawnZombie(level, center);
        Zombie second = spawnZombie(level, center.offset(1, 0, 0));
        Zombie attackTarget = spawnZombie(level, center.offset(3, 0, 0));
        BrewOfLoveItem.ZombieBirthResult birth = null;
        try {
            Enslaved.enslave(first, owner);
            Enslaved.enslave(second, owner);
            first.setPersistenceRequired();
            first.getPersistentData().putBoolean(Brews.SUMMONED_NO_DROPS_TAG, true);
            first.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            first.setHealth(7.0F);
            first.setTarget(attackTarget);

            birth = BrewOfLoveItem.giveZombieBirth(level, first, second);
            ZombieVillager firstParent = birth.firstParent();
            ZombieVillager secondParent = birth.secondParent();
            Zombie baby = birth.baby();
            helper.assertTrue(firstParent != null && secondParent != null,
                    "Both adult parents must become zombie villagers");
            helper.assertTrue(first.isRemoved() && second.isRemoved(),
                    "Modern conversion must replace both original zombie entities");
            assertOwner(helper, firstParent, owner);
            assertOwner(helper, secondParent, owner);
            helper.assertTrue(firstParent.isPersistenceRequired(),
                    "A raised parent's persistence must survive zombie-villager conversion");
            helper.assertTrue(!secondParent.isPersistenceRequired(),
                    "An ordinary bound parent's nonpersistent state must survive conversion");
            helper.assertTrue(firstParent.getPersistentData().getBooleanOr(Brews.SUMMONED_NO_DROPS_TAG, false),
                    "A raised parent's no-drops marker must survive conversion");
            helper.assertTrue(firstParent.getMainHandItem().is(Items.IRON_SWORD),
                    "Parent equipment must survive zombie-villager conversion");
            helper.assertTrue(Math.abs(firstParent.getHealth() - 7.0F) < 0.001F,
                    "Parent health must survive zombie-villager conversion");
            helper.assertTrue(firstParent.getTarget() == attackTarget,
                    "Parent attack target must survive zombie-villager conversion");

            helper.assertTrue(baby != null && baby.isBaby(), "The birth must create a baby zombie");
            helper.assertTrue(!Enslaved.isEnslaved(baby) && !baby.hasEffect(WitcheryMobEffects.ENSLAVED),
                    "The source baby zombie must not inherit either ownership tag or effect");
            helper.assertTrue(!baby.isPersistenceRequired(),
                    "The unowned baby must keep ordinary despawn rules");
            helper.assertTrue(baby.getMainHandItem().isEmpty(),
                    "The source baby is not finalized with random spawn equipment");
            helper.succeed();
        } finally {
            if (birth != null) {
                discard(birth.firstParent());
                discard(birth.secondParent());
                discard(birth.baby());
            }
            discard(first);
            discard(second);
            discard(attackTarget);
            removeTestPlayer(owner);
        }
    }

    private static Zombie spawnZombie(ServerLevel level, BlockPos pos) {
        level.getChunkAt(pos);
        Zombie zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.TRIGGERED);
        if (zombie == null) {
            throw new IllegalStateException("Unable to create test zombie");
        }
        zombie.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(zombie);
        return zombie;
    }

    private static IronGolem spawnIronGolem(ServerLevel level, BlockPos pos) {
        level.getChunkAt(pos);
        IronGolem golem = EntityType.IRON_GOLEM.create(level, EntitySpawnReason.TRIGGERED);
        if (golem == null) {
            throw new IllegalStateException("Unable to create test iron golem");
        }
        golem.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(golem);
        return golem;
    }

    private static void assertOwner(GameTestHelper helper, Mob mob, ServerPlayer owner) {
        helper.assertTrue(Enslaved.isEnslavedBy(mob, owner), "The mob must recognize its current owner");
        helper.assertTrue(owner.getName().getString().equals(Enslaved.enslaverName(mob)),
                "The source owner-name tag must match");
        helper.assertTrue(owner.getUUID().toString().equals(Enslaved.enslaverUuid(mob)),
                "The port UUID tag must match");
        helper.assertTrue(mob.hasEffect(WitcheryMobEffects.ENSLAVED),
                "The permanent source Enslaved effect must be active");
    }

    private static ServerPlayer createTestPlayer(ServerLevel level, BlockPos pos, UUID id, String name) {
        ServerPlayer player = new FakePlayer(level, new GameProfile(id, name));
        player.setInvulnerable(false);
        player.getAbilities().invulnerable = false;
        player.setHealth(player.getMaxHealth());
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

    private static void discard(Entity entity) {
        if (entity != null && !entity.isRemoved()) {
            entity.discard();
        }
    }
}
