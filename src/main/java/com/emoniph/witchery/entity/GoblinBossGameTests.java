package com.emoniph.witchery.entity;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Consumer;

/** Dedicated-server contract for the paired Goblin Mog and Gulg bosses. */
public final class GoblinBossGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BOSS_PARITY =
            TEST_FUNCTIONS.register("goblin_boss_parity", () -> GoblinBossGameTests::bossParity);

    private GoblinBossGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(GoblinBossGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "goblin_bosses"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 160, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "goblin_boss_parity"),
                new FunctionGameTestInstance(BOSS_PARITY.getKey(), data));
    }

    private static void bossParity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fixtureBase = helper.absolutePos(new BlockPos(2, 0, 2));
        int fixtureY = Math.clamp(fixtureBase.getY() + 100,
                level.getMinY() + 20, level.getMaxY() - 20);
        BlockPos origin = new BlockPos(fixtureBase.getX(), fixtureY, fixtureBase.getZ());
        GoblinMogEntity mog = WitcheryEntities.GOBLIN_MOG.get().create(level, EntitySpawnReason.TRIGGERED);
        GoblinGulgEntity gulg = WitcheryEntities.GOBLIN_GULG.get().create(level, EntitySpawnReason.TRIGGERED);
        Pig target = EntityType.PIG.create(level, EntitySpawnReason.TRIGGERED);
        Ravager launchTarget = EntityType.RAVAGER.create(level, EntitySpawnReason.TRIGGERED);
        if (mog == null || gulg == null || target == null || launchTarget == null) {
            throw new IllegalStateException("Unable to create Goblin boss fixture entities");
        }

        mog.snapTo(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D, 0.0F, 0.0F);
        gulg.snapTo(origin.getX() + 40.5D, origin.getY(), origin.getZ() + 0.5D, 0.0F, 0.0F);
        target.snapTo(origin.getX() + 10.5D, origin.getY(), origin.getZ() + 0.5D, 0.0F, 0.0F);
        launchTarget.snapTo(origin.getX() + 42.5D, origin.getY(), origin.getZ() + 0.5D, 0.0F, 0.0F);
        DifficultyInstance difficulty = level.getCurrentDifficultyAt(origin);
        mog.finalizeSpawn(level, difficulty, EntitySpawnReason.TRIGGERED, null);
        gulg.finalizeSpawn(level, difficulty, EntitySpawnReason.TRIGGERED, null);
        level.addFreshEntity(mog);
        level.addFreshEntity(gulg);
        level.addFreshEntity(target);
        level.addFreshEntity(launchTarget);

        try {
            assertSharedInvariants(helper, mog, gulg);
            assertMogContract(helper, level, mog, gulg, target);
            assertGulgLaunch(helper, level, gulg, launchTarget);
            assertTeleportContract(helper, level, gulg, origin.offset(48, 0, 6));
            helper.succeed();
        } finally {
            level.getEntitiesOfClass(AbstractArrow.class, new AABB(origin).inflate(96.0D))
                    .forEach(AbstractArrow::discard);
            mog.discard();
            gulg.discard();
            target.discard();
            launchTarget.discard();
        }
    }

    private static void assertSharedInvariants(GameTestHelper helper, GoblinMogEntity mog,
                                               GoblinGulgEntity gulg) {
        helper.assertTrue(mog.fireImmune() && gulg.fireImmune(), "Both bosses must be fire immune");
        helper.assertTrue(mog.canBreatheUnderwater() && gulg.canBreatheUnderwater(),
                "Both bosses must breathe underwater");
        helper.assertTrue(!mog.removeWhenFarAway(256.0D) && !gulg.removeWhenFarAway(256.0D),
                "Neither boss may naturally despawn");
        helper.assertTrue(mog.isPersistenceRequired() && gulg.isPersistenceRequired(),
                "Spawn initialization must mark both bosses persistent");
        helper.assertTrue(close(mog.getAttribute(Attributes.MAX_HEALTH).getBaseValue(), 400.0D)
                        && close(gulg.getAttribute(Attributes.MAX_HEALTH).getBaseValue(), 400.0D)
                        && close(mog.getAttribute(Attributes.FOLLOW_RANGE).getBaseValue(), 50.0D)
                        && close(gulg.getAttribute(Attributes.FOLLOW_RANGE).getBaseValue(), 50.0D)
                        && close(mog.getAttribute(Attributes.ARMOR).getBaseValue(), 5.0D)
                        && close(gulg.getAttribute(Attributes.ARMOR).getBaseValue(), 8.0D)
                        && close(mog.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getBaseValue(), 0.0D)
                        && close(gulg.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getBaseValue(), 1.0D),
                "Boss attributes must match the source contract");
        mog.setTarget(gulg);
        gulg.setTarget(mog);
        helper.assertTrue(mog.getTarget() == null && gulg.getTarget() == null,
                "Goblin bosses must reject goblin-family revenge targets");
    }

    private static void assertMogContract(GameTestHelper helper, ServerLevel level,
                                          GoblinMogEntity mog, GoblinGulgEntity gulg, Pig target) {
        helper.assertTrue(mog.getMainHandItem().is(Items.BOW), "Mog must spawn holding a bow");
        mog.setInvulTimer(73);
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        mog.saveWithoutId(output);
        CompoundTag snapshot = output.buildResult();
        GoblinMogEntity restored = WitcheryEntities.GOBLIN_MOG.get().create(level, EntitySpawnReason.LOAD);
        if (restored == null) throw new IllegalStateException("Unable to restore Mog fixture");
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), snapshot));
        helper.assertTrue(restored.getInvulTimer() == 73 && restored.getMainHandItem().is(Items.BOW),
                "Mog save/load must retain invulnerability and bow equipment");

        mog.setInvulTimer(0);
        gulg.setPos(mog.getX() + 40.0D, mog.getY(), mog.getZ());
        target.setOnGround(true);
        mog.performRangedAttack(target, 1.0F);
        AbstractArrow grounded = newestArrow(level, mog);
        helper.assertTrue(grounded != null, "Grounded-target Mog attack must spawn an arrow");
        helper.assertTrue(grounded.getOwner() == mog, "Grounded-target Mog arrow must retain its owner");
        double groundedSpeed = grounded.getDeltaMovement().length();
        helper.assertTrue(withinRelative(groundedSpeed, 2.4D, 0.2D),
                "Grounded-target Mog arrow speed must remain near 2.4 after inaccuracy, got "
                        + groundedSpeed);
        if (grounded != null) grounded.discard();

        target.setOnGround(false);
        mog.performRangedAttack(target, 1.0F);
        AbstractArrow airborne = newestArrow(level, mog);
        helper.assertTrue(airborne != null, "Airborne-target Mog attack must spawn an arrow");
        double airborneSpeed = airborne.getDeltaMovement().length();
        helper.assertTrue(withinRelative(airborneSpeed, 4.0D, 0.25D)
                        && airborneSpeed > groundedSpeed * 1.25D,
                "Airborne-target Mog arrow must be materially faster and remain near 4.0 after inaccuracy; got "
                        + airborneSpeed + " versus grounded " + groundedSpeed);
        if (airborne != null) airborne.discard();
    }

    private static void assertGulgLaunch(GameTestHelper helper, ServerLevel level,
                                         GoblinGulgEntity gulg, Ravager target) {
        target.setDeltaMovement(Vec3.ZERO);
        target.invulnerableTime = 0;
        boolean hit = gulg.doHurtTarget(level, target);
        helper.assertTrue(hit && close(target.getDeltaMovement().y, 0.5D),
                "A Gulg hit without nearby Mog must add the source base 0.5 launch");
    }

    private static void assertTeleportContract(GameTestHelper helper, ServerLevel level,
                                               GoblinGulgEntity gulg, BlockPos floor) {
        level.setBlock(floor, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        gulg.setPos(floor.getX() - 5.0D, floor.getY() + 1.0D, floor.getZ());
        boolean landed = GoblinBossTeleportHelper.tryTeleportTo(
                gulg, floor.getX() + 0.5D, floor.getY() + 6.0D, floor.getZ() + 0.5D);
        helper.assertTrue(landed && close(gulg.getY(), floor.getY() + 1.0D),
                "Boss teleport must search down to solid ground; landed=" + landed
                        + ", actualY=" + gulg.getY() + ", expectedY=" + (floor.getY() + 1.0D)
                        + ", chunkLoaded=" + level.hasChunkAt(floor));

        BlockPos wetFloor = floor.east(3);
        level.setBlock(wetFloor, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(wetFloor.above(), Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        Vec3 beforeLiquid = gulg.position();
        boolean liquid = GoblinBossTeleportHelper.tryTeleportTo(
                gulg, wetFloor.getX() + 0.5D, wetFloor.getY() + 1.0D, wetFloor.getZ() + 0.5D);
        helper.assertTrue(!liquid && gulg.position().distanceToSqr(beforeLiquid) < 0.0001D,
                "Liquid teleport failure must restore the original position");

        BlockPos blockedFloor = floor.east(6);
        level.setBlock(blockedFloor, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(blockedFloor.above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        Vec3 beforeCollision = gulg.position();
        boolean collision = GoblinBossTeleportHelper.tryTeleportTo(
                gulg, blockedFloor.getX() + 0.5D, blockedFloor.getY() + 1.0D, blockedFloor.getZ() + 0.5D);
        helper.assertTrue(!collision && gulg.position().distanceToSqr(beforeCollision) < 0.0001D,
                "Collision teleport failure must restore the original position");

        Vec3 beforeHeightFailure = gulg.position();
        boolean outsideBuildHeight = GoblinBossTeleportHelper.tryTeleportTo(
                gulg, floor.getX() + 0.5D, level.getMaxY() + 1.0D, floor.getZ() + 0.5D);
        helper.assertTrue(!outsideBuildHeight
                        && gulg.position().distanceToSqr(beforeHeightFailure) < 0.0001D,
                "Out-of-height teleport failure must leave the boss in place");

        Vec3 beforeUnsupportedFailure = gulg.position();
        boolean unsupported = GoblinBossTeleportHelper.tryTeleportTo(
                gulg, floor.getX() + 0.5D, level.getMinY(), floor.getZ() + 0.5D);
        helper.assertTrue(!unsupported
                        && gulg.position().distanceToSqr(beforeUnsupportedFailure) < 0.0001D,
                "A destination without searchable support must leave the boss in place");
    }

    private static AbstractArrow newestArrow(ServerLevel level, GoblinMogEntity mog) {
        List<AbstractArrow> arrows = level.getEntitiesOfClass(
                AbstractArrow.class, mog.getBoundingBox().inflate(24.0D));
        return arrows.isEmpty() ? null : arrows.get(arrows.size() - 1);
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) < 0.01D;
    }

    private static boolean withinRelative(double actual, double expected, double tolerance) {
        return Math.abs(actual - expected) <= expected * tolerance;
    }
}
