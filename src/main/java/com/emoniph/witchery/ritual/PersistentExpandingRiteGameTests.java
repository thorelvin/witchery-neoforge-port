package com.emoniph.witchery.ritual;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.command.HeadlessGameTestPlayer;
import com.emoniph.witchery.entity.familiar.FamiliarHelper;
import com.emoniph.witchery.init.WitcheryAttachments;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Dedicated-server behavior coverage for persisted Fertility and Forestation pulses. */
public final class PersistentExpandingRiteGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EXPANDING_RITES =
            TEST_FUNCTIONS.register("persistent_expanding_rites",
                    () -> PersistentExpandingRiteGameTests::expandingRites);

    private PersistentExpandingRiteGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(PersistentExpandingRiteGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "persistent_expanding_rites"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 160, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "persistent_expanding_rites"),
                new FunctionGameTestInstance(EXPANDING_RITES.getKey(), data));
    }

    private static void expandingRites(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fixture = helper.absolutePos(new BlockPos(4, 2, 4));
        int y = Math.clamp(fixture.getY() + 80, level.getMinY() + 20, level.getMaxY() - 30);
        BlockPos fertilityCenter = new BlockPos(fixture.getX(), y, fixture.getZ());
        BlockPos forestCenter = fertilityCenter.east(30);
        ServerPlayer owner = HeadlessGameTestPlayer.create(level,
                UUID.fromString("a0c5be6c-9568-47e5-87ca-14b92343144f"), "ExpandingRiteOwner");
        owner.snapTo(fertilityCenter.getX() + 4.0D, fertilityCenter.getY(),
                fertilityCenter.getZ() + 0.5D, 0.0F, 0.0F);
        WitcheryAttachments.get(owner).setFamiliarType("cat");
        LivingEntity familiar = FamiliarHelper.restoreBoundFamiliar(owner, level,
                owner.getX(), owner.getY(), owner.getZ() + 1.0D);
        helper.assertTrue(familiar != null, "The Fertility fixture must restore an active cat familiar");
        owner.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200));
        owner.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200));
        owner.addEffect(new MobEffectInstance(MobEffects.POISON, 200));

        placeRing(level, fertilityCenter, RitualCircleDetector.CircleSize.SMALL);
        ZombieVillager zombie = EntityType.ZOMBIE_VILLAGER.create(level, EntitySpawnReason.TRIGGERED);
        helper.assertTrue(zombie != null, "The Fertility fixture must create a Zombie Villager");
        if (zombie == null) {
            throw new IllegalStateException("Unable to create Zombie Villager fixture");
        }
        zombie.setNoAi(true);
        zombie.snapTo(fertilityCenter.getX() + 0.5D, fertilityCenter.getY() + 0.5D,
                fertilityCenter.getZ() + 4.5D, 0.0F, 0.0F);
        level.addFreshEntity(zombie);

        ActiveRitual fertility = new ActiveRitual(ActiveRitualType.FERTILITY, fertilityCenter,
                RitualCircleDetector.CircleSize.SMALL, fertilityCenter, 1,
                4, RitualExecutor.FERTILITY_BASE_RADIUS, RitualExecutor.FERTILITY_BASE_RADIUS - 3,
                RitualExecutor.FERTILITY_PULSE_INTERVAL, 0, 0.0F, false, 0,
                level.getGameTime(), ItemStack.EMPTY,
                List.of(new ItemStack(Items.BONE_MEAL), new ItemStack(Items.DIAMOND)),
                owner.getScoreboardName(), ItemStack.EMPTY);
        RitualSavedData data = RitualSavedData.get(level);
        data.activeRituals.add(fertility);
        data.setDirty();
        RitualExecutor.tickActiveRituals(level);
        helper.assertTrue(fertility.stage == 1,
                "One dedicated-server tick must advance one Fertility expansion ring");
        helper.assertTrue(!owner.hasEffect(MobEffects.NAUSEA)
                        && !owner.hasEffect(MobEffects.BLINDNESS)
                        && !owner.hasEffect(MobEffects.POISON),
                "Fertility must clear the source three ailments from players in its passing ring");
        helper.assertTrue(owner.getEffect(MobEffects.REGENERATION) != null
                        && owner.getEffect(MobEffects.REGENERATION).getAmplifier() == 1
                        && owner.hasEffect(MobEffects.HEALTH_BOOST),
                "An active cat familiar must enhance each Fertility ring with Regeneration II and Health Boost");

        helper.runAfterDelay(10, () -> {
            List<Villager> villagers = level.getEntitiesOfClass(Villager.class,
                    new AABB(fertilityCenter).inflate(6.0D));
            helper.assertTrue(fertility.stage >= 1,
                    "Fertility must retain its staged expansion progress");
            helper.assertTrue(villagers.size() == 1 && villagers.getFirst().hasEffect(MobEffects.NAUSEA),
                    "Fertility must immediately restore one nauseated Villager");

            level.removeBlock(RitualCircleDetector.ringPositions(fertilityCenter,
                    RitualCircleDetector.CircleSize.SMALL).getFirst(), false);
            helper.runAfterDelay(8, () -> {
                assertExactRefunds(helper, level, fertilityCenter);
                helper.assertTrue(data.activeRituals.stream().noneMatch(ritual -> ritual.center.equals(fertilityCenter)),
                        "Breaking the sustaining circle must remove Fertility from SavedData");
                runForestationFixture(helper, level, data, forestCenter, owner);
            });
        });
    }

    private static void runForestationFixture(GameTestHelper helper, ServerLevel level,
                                               RitualSavedData data, BlockPos center, ServerPlayer owner) {
        owner.snapTo(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D, 0.0F, 0.0F);
        placeRing(level, center, RitualCircleDetector.CircleSize.MEDIUM);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                level.setBlock(center.offset(dx, -1, dz), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(center.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        ActiveRitual forestation = new ActiveRitual(ActiveRitualType.FORESTATION, center,
                RitualCircleDetector.CircleSize.MEDIUM, center, 1,
                2, RitualExecutor.FORESTATION_HEIGHT, 3, 2, 0, 0.0F, false, 0,
                level.getGameTime(), ItemStack.EMPTY, List.of(new ItemStack(Items.OAK_SAPLING)),
                owner.getScoreboardName(),
                new ItemStack(Items.OAK_SAPLING));
        data.activeRituals.add(forestation);
        data.setDirty();
        RitualExecutor.tickActiveRituals(level);
        helper.assertTrue(forestation.stage == 1,
                "One dedicated-server tick must perform one Forestation attempt");

        helper.runAfterDelay(10, () -> {
            boolean planted = BlockPos.betweenClosedStream(
                            center.offset(-4, 0, -4), center.offset(5, 16, 5))
                    .map(level::getBlockState)
                    .anyMatch(state -> state.is(Blocks.OAK_SAPLING)
                            || state.is(Blocks.OAK_LOG) || state.is(Blocks.OAK_LEAVES));
            helper.assertTrue(planted, "Forestation must plant or grow the persisted Oak Sapling focus");
            helper.assertTrue(data.activeRituals.stream().noneMatch(ritual -> ritual.center.equals(center)),
                    "Forestation must complete after its persisted duration without refunding offerings");
            LivingEntity familiar = FamiliarHelper.findBoundFamiliarEntity(owner);
            if (familiar != null) {
                familiar.discard();
            }
            HeadlessGameTestPlayer.remove(owner);
            helper.succeed();
        });
    }

    private static void placeRing(ServerLevel level, BlockPos center, RitualCircleDetector.CircleSize size) {
        for (BlockPos mark : RitualCircleDetector.ringPositions(center, size)) {
            level.setBlock(mark.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(mark, WitcheryBlocks.CIRCLE_GLYPH.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static void assertExactRefunds(GameTestHelper helper, ServerLevel level, BlockPos center) {
        List<ItemEntity> refunds = level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(2.0D));
        int boneMeal = refunds.stream().filter(item -> item.getItem().is(Items.BONE_MEAL))
                .mapToInt(item -> item.getItem().getCount()).sum();
        int diamonds = refunds.stream().filter(item -> item.getItem().is(Items.DIAMOND))
                .mapToInt(item -> item.getItem().getCount()).sum();
        helper.assertTrue(boneMeal == 1 && diamonds == 1 && refunds.size() == 2,
                "An aborted rite must refund every consumed offering exactly once");
    }
}
