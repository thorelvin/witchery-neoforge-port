package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.data.WitcheryPlayerData;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Consumer;

/** Integrated-server fixture for automatic vampire level perks and physiology. */
public final class VampirePassiveGameTests {
    private static final UUID PLAYER_ID = UUID.fromString("ec0ee37d-e7eb-4d52-8573-fcd69032dbda");
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PASSIVES =
            TEST_FUNCTIONS.register("vampire_passive_perks", () -> VampirePassiveGameTests::passives);

    private VampirePassiveGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(VampirePassiveGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "vampire_passives"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 120, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "vampire_passive_perks"),
                new FunctionGameTestInstance(PASSIVES.getKey(), data));
    }

    private static void passives(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 2, 1));
        ServerPlayer player = createTestPlayer(level, origin);
        Pig fireSubject = EntityType.PIG.create(level, EntitySpawnReason.TRIGGERED);
        if (fireSubject == null) {
            removeTestPlayer(player);
            throw new IllegalStateException("Unable to create vampire fire test subject");
        }
        fireSubject.snapTo(origin.getX() + 2.5D, origin.getY(), origin.getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(fireSubject);
        try {
            WitcheryPlayerData data = WitcheryAttachments.get(player);
            data.setVampire(true);

            double baseDamage = player.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
            data.setVampireLevel(1);
            VampireEvents.applyVampireState(player, data);
            helper.assertTrue(close(player.getAttributeValue(Attributes.ATTACK_DAMAGE), baseDamage * 2.0D),
                    "Level 1 human vampire damage must use the source x2 total modifier");

            data.setVampireLevel(7);
            VampireEvents.applyVampireState(player, data);
            helper.assertTrue(close(player.getAttributeValue(Attributes.ATTACK_DAMAGE), baseDamage * 4.0D),
                    "Level 7 human vampire damage must use the source x4 total modifier");

            data.setVampireBatForm(true);
            VampireEvents.applyVampireState(player, data);
            helper.assertTrue(close(player.getAttributeValue(Attributes.ATTACK_DAMAGE), 0.0D),
                    "Bat form must replace the human damage boost with zero attack power");
            data.setVampireBatForm(false);

            player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
            VampireEvents.tickVampirePhysiology(player);
            helper.assertTrue(!player.hasEffect(MobEffects.POISON),
                    "Vampire physiology must remove poison");

            DamageSource vampireFire = VampireEvents.vampireFireDamageSource(level);
            helper.assertTrue(!vampireFire.is(DamageTypeTags.IS_FIRE)
                            && vampireFire.is(DamageTypeTags.BYPASSES_ARMOR),
                    "Vampire fire damage must bypass armor without being blocked by Fire Resistance");
            fireSubject.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
            fireSubject.setRemainingFireTicks(100);
            fireSubject.invulnerableTime = 0;
            float healthBeforeFire = fireSubject.getHealth();
            boolean fireDamageAccepted = fireSubject.hurtServer(level, vampireFire, 2.0F);
            helper.assertTrue(fireDamageAccepted && close(fireSubject.getHealth(), healthBeforeFire - 2.0F),
                    "Source vampire fire damage must deal two damage through Fire Resistance");

            player.getFoodData().setFoodLevel(20);
            BlockPos stonePos = origin.offset(1, 0, 0);
            level.setBlockAndUpdate(stonePos, Blocks.STONE.defaultBlockState());
            boolean crushed = VampireEvents.tryVampireCrush(player, stonePos, level.getBlockState(stonePos));
            helper.assertTrue(crushed && level.getBlockState(stonePos).isAir(),
                    "Level 6 crushing must remove the selected stone immediately");
            AABB drops = new AABB(stonePos).inflate(1.0D);
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, drops).stream()
                            .anyMatch(item -> item.getItem().is(Items.COBBLESTONE)),
                    "Fist-crushed Stone must still drop Cobblestone like the source helper");

            data.setVampire(false);
            VampireEvents.applyVampireState(player, data);
            helper.assertTrue(close(player.getAttributeValue(Attributes.ATTACK_DAMAGE), baseDamage),
                    "Curing vampirism must remove its transient attack modifier");
            helper.succeed();
        } finally {
            fireSubject.discard();
            removeTestPlayer(player);
        }
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) < 0.001D;
    }

    private static ServerPlayer createTestPlayer(ServerLevel level, BlockPos pos) {
        ServerPlayer player = new FakePlayer(level, new GameProfile(PLAYER_ID, "vampire-passives"));
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
}
