package com.emoniph.witchery.entity;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.init.WitcheryEntities;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Consumer;

/** Dedicated-server coverage for live Hobgoblin pack routing and offspring state. */
public final class HobgoblinEcologyGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ECOLOGY =
            TEST_FUNCTIONS.register("hobgoblin_village_ecology", () -> HobgoblinEcologyGameTests::ecology);

    private HobgoblinEcologyGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(HobgoblinEcologyGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "hobgoblin_ecology"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 160, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "hobgoblin_village_ecology"),
                new FunctionGameTestInstance(ECOLOGY.getKey(), data));
    }

    private static void ecology(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        buildFloor(helper, level);
        buildHomes(helper, level);
        HobgoblinEntity first = hobgoblin(helper, level, new BlockPos(2, 2, 3));
        HobgoblinEntity second = hobgoblin(helper, level, new BlockPos(3, 2, 3));
        VillageGuardEntity guard = guard(helper, level, new BlockPos(3, 2, 6));
        Villager villager = villager(helper, level, new BlockPos(8, 2, 3));

        helper.assertTrue(!first.hasRaidPack() && !first.canRaidVillagers(),
                "Two Hobgoblins must not form a villager-raiding pack");
        helper.assertTrue(!first.shouldAvoidVillageGuards(),
                "A sub-threshold pair must not flee a Village Guard");
        helper.assertTrue(first.hasHobgoblinBreedingCapacity(level),
                "Nine registered homes must leave breeding capacity for two adults");

        HobgoblinEntity third = hobgoblin(helper, level, new BlockPos(4, 2, 3));
        helper.assertTrue(first.hasRaidPack() && first.canRaidVillagers(),
                "Three nearby Hobgoblins must unlock villager raids");
        helper.assertTrue(first.shouldAvoidVillageGuards(),
                "The same source pack threshold must unlock Village Guard avoidance");
        helper.assertTrue(!first.hasHobgoblinBreedingCapacity(level),
                "Nine homes must cap the local population at three Hobgoblins");
        guard.discard();

        helper.runAfterDelay(40, () -> {
            boolean acquiredVillager = first.getTarget() instanceof Villager
                    || second.getTarget() instanceof Villager
                    || third.getTarget() instanceof Villager;
            helper.assertTrue(acquiredVillager,
                    "A live three-Hobgoblin pack must acquire the nearby villager");
            first.setTarget(null);
            second.setTarget(null);
            third.setTarget(null);
            first.setNoAi(true);
            second.setNoAi(true);
            third.setNoAi(true);
            villager.discard();
        });

        // The source-shaped village refresh cadence is randomized across 70-119 ticks.
        // Waiting beyond the maximum also removes POI-registration timing from the fixture.
        helper.runAfterDelay(130, () -> {
            try {
                long nearbyHomes = level.getPoiManager().getCountInRange(
                        holder -> holder.is(PoiTypes.HOME), first.blockPosition(),
                        HobgoblinVillageEcology.VILLAGE_SEARCH_RADIUS, PoiManager.Occupancy.ANY);
                helper.assertTrue(first.hasVillageHome() && first.isPersistenceRequired(),
                        "Nearby HOME POIs must establish persistent village membership; alive="
                                + first.isAlive() + ", pos=" + first.blockPosition()
                                + ", homes=" + nearbyHomes + ", membership=" + first.hasVillageHome()
                                + ", persistent=" + first.isPersistenceRequired());

                first.setTarget(null);
                second.setTarget(null);
                third.setTarget(null);
                first.completeMating(level, second);

                AABB nursery = new AABB(helper.absolutePos(new BlockPos(0, 1, 0)))
                        .inflate(12.0D, 4.0D, 8.0D);
                List<HobgoblinEntity> babies = level.getEntitiesOfClass(
                        HobgoblinEntity.class, nursery, HobgoblinEntity::isBaby);
                helper.assertTrue(babies.size() == 1,
                        "Completing mating must add exactly one baby Hobgoblin");
                HobgoblinEntity baby = babies.getFirst();
                helper.assertTrue(first.getAge() == 6000 && second.getAge() == 6000,
                        "Both parents must receive the source 6000-tick cooldown");
                helper.assertTrue(baby.getAge() < 0 && baby.isBaby(),
                        "The child must use vanilla's persistent baby age state");
                helper.assertTrue(baby.getProfessionVariant() >= 0 && baby.getProfessionVariant() < 4,
                        "The child must inherit a valid random Hobgoblin profession variant");
                helper.succeed();
            } finally {
                cleanup(level, helper.absolutePos(new BlockPos(5, 2, 4)));
            }
        });
    }

    private static void buildFloor(GameTestHelper helper, ServerLevel level) {
        for (int x = 0; x <= 12; x++) {
            for (int z = 0; z <= 14; z++) {
                level.setBlock(helper.absolutePos(new BlockPos(x, 1, z)),
                        Blocks.STONE.defaultBlockState(), 3);
            }
        }
    }

    private static void buildHomes(GameTestHelper helper, ServerLevel level) {
        BlockState foot = Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.EAST)
                .setValue(BedBlock.PART, BedPart.FOOT);
        BlockState head = foot.setValue(BedBlock.PART, BedPart.HEAD);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                BlockPos footPos = helper.absolutePos(new BlockPos(1 + column * 3, 2, 8 + row * 2));
                level.setBlock(footPos, foot, 2);
                level.setBlock(footPos.east(), head, 2);
            }
        }
    }

    private static HobgoblinEntity hobgoblin(GameTestHelper helper, ServerLevel level, BlockPos relativePos) {
        HobgoblinEntity entity = WitcheryEntities.HOBGOBLIN.get().create(level, EntitySpawnReason.TRIGGERED);
        helper.assertTrue(entity != null, "Hobgoblin entity type must create on a dedicated server");
        if (entity == null) {
            throw new IllegalStateException("Hobgoblin entity type returned null");
        }
        BlockPos pos = helper.absolutePos(relativePos);
        entity.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(entity);
        return entity;
    }

    private static VillageGuardEntity guard(GameTestHelper helper, ServerLevel level, BlockPos relativePos) {
        VillageGuardEntity entity = WitcheryEntities.VILLAGE_GUARD.get().create(level, EntitySpawnReason.TRIGGERED);
        helper.assertTrue(entity != null, "Village Guard entity type must create on a dedicated server");
        if (entity == null) {
            throw new IllegalStateException("Village Guard entity type returned null");
        }
        BlockPos pos = helper.absolutePos(relativePos);
        entity.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        entity.setNoAi(true);
        level.addFreshEntity(entity);
        return entity;
    }

    private static Villager villager(GameTestHelper helper, ServerLevel level, BlockPos relativePos) {
        Villager entity = EntityType.VILLAGER.create(level, EntitySpawnReason.TRIGGERED);
        helper.assertTrue(entity != null, "Vanilla villager entity type must create");
        if (entity == null) {
            throw new IllegalStateException("Villager entity type returned null");
        }
        BlockPos pos = helper.absolutePos(relativePos);
        entity.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        entity.setNoAi(true);
        level.addFreshEntity(entity);
        return entity;
    }

    private static void cleanup(ServerLevel level, BlockPos center) {
        for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(center).inflate(20.0D))) {
            if (!(entity instanceof net.minecraft.server.level.ServerPlayer)) {
                entity.discard();
            }
        }
    }
}
