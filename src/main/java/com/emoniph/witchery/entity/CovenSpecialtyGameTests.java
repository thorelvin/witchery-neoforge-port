package com.emoniph.witchery.entity;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.command.HeadlessGameTestPlayer;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Dedicated-server ownership and scene contract for restrained Coven specialties. */
public final class CovenSpecialtyGameTests {
    private static final UUID OWNER_ID = UUID.fromString("aed08542-a9ca-46be-aad3-1d3be9a4425c");
    private static final UUID OTHER_ID = UUID.fromString("a69fdf2f-4e75-43ee-a952-c41528c5a9c9");
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CONTRIBUTIONS =
            TEST_FUNCTIONS.register("coven_specialty_contributions",
                    () -> CovenSpecialtyGameTests::contributions);

    private CovenSpecialtyGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(CovenSpecialtyGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "coven_specialties"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 80, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "coven_specialty_contributions"),
                new FunctionGameTestInstance(CONTRIBUTIONS.getKey(), data));
    }

    private static void contributions(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fixture = helper.absolutePos(new BlockPos(4, 2, 4));
        int y = Math.clamp(fixture.getY() + 70, level.getMinY() + 10, level.getMaxY() - 10);
        BlockPos center = new BlockPos(fixture.getX(), y, fixture.getZ());
        prepareGround(level, center);
        ServerPlayer owner = HeadlessGameTestPlayer.create(level, OWNER_ID, "ConcordOwner");
        ServerPlayer other = HeadlessGameTestPlayer.create(level, OTHER_ID, "ConcordOther");
        owner.snapTo(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D, 0.0F, 0.0F);
        other.snapTo(center.getX() + 1.5D, center.getY(), center.getZ() + 0.5D, 0.0F, 0.0F);

        List<CovenWitchEntity> owned = new ArrayList<>();
        CovenSpecialty[] specialties = {
                CovenSpecialty.HEDGE_WITCH,
                CovenSpecialty.GRAVE_WITCH,
                CovenSpecialty.PALE_WITCH,
                CovenSpecialty.STORM_WITCH,
                CovenSpecialty.HEARTH_WITCH,
                CovenSpecialty.HEDGE_WITCH
        };
        for (int index = 0; index < specialties.length; index++) {
            CovenWitchEntity witch = spawn(level, center.offset(2 + index, 0, 2), OWNER_ID, specialties[index]);
            owned.add(witch);
        }
        CovenWitchEntity otherOwned = spawn(level, center.offset(-3, 0, 2), OTHER_ID,
                CovenSpecialty.STORM_WITCH);
        CovenWitchEntity distant = spawn(level, center.offset(30, 0, 0), OWNER_ID,
                CovenSpecialty.GRAVE_WITCH);

        AABB bounds = new AABB(center).inflate(CovenSpecialtyContributions.PARTICIPATION_RADIUS);
        CovenContributionCounter.SpecialtyRoster roster =
                CovenContributionCounter.livingSwornRoster(level, bounds, OWNER_ID);
        helper.assertTrue(roster.livingCount() == CovenWitchEntity.MAX_COVEN,
                "Exactly six nearby owner-sworn living witches must participate");
        helper.assertTrue(roster.specialties().size() == CovenSpecialty.values().length,
                "Duplicate Hedge Witches must collapse while all five craft identities remain present");
        helper.assertTrue(!roster.participants().contains(otherOwned) && !roster.participants().contains(distant),
                "Other-owned and distant witches must not contribute");

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        owned.get(1).saveWithoutId(output);
        CompoundTag snapshot = output.buildResult();
        CovenWitchEntity restored = WitcheryEntities.COVEN_WITCH.get().create(level, EntitySpawnReason.LOAD);
        helper.assertTrue(restored != null, "The dedicated server must create a Coven Witch for reload");
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), snapshot));
        helper.assertTrue(restored.getSpecialty() == CovenSpecialty.GRAVE_WITCH
                        && restored.isRecruitedBy(OWNER_ID),
                "Specialty and coven owner must survive entity save/load together");

        CovenSpecialtyContributions.revealContribution(
                owner, level, roster, CovenSpecialty.GRAVE_WITCH);
        var concord = level.getServer().getAdvancements().get(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "guide/coven_concord"));
        helper.assertTrue(concord != null && owner.getAdvancements().getOrStartProgress(concord).isDone(),
                "Six physically present sworn witches must complete the hidden Coven Concord gate");

        owned.getFirst().kill(level);
        roster = CovenContributionCounter.livingSwornRoster(level, bounds, OWNER_ID);
        helper.assertTrue(roster.livingCount() == 5 && roster.has(CovenSpecialty.HEDGE_WITCH),
                "A dead participant must stop counting while the duplicate specialty remains non-stacking");

        helper.assertTrue(CovenSpecialtyContributions.gravePower(1_000, roster) == 950,
                "One living Grave Witch must grant the single five-percent contribution");

        for (CovenWitchEntity witch : owned) {
            witch.discard();
        }
        otherOwned.discard();
        distant.discard();
        HeadlessGameTestPlayer.remove(owner);
        HeadlessGameTestPlayer.remove(other);
        helper.succeed();
    }

    private static CovenWitchEntity spawn(ServerLevel level, BlockPos pos, UUID owner,
                                          CovenSpecialty specialty) {
        CovenWitchEntity witch = WitcheryEntities.COVEN_WITCH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (witch == null) {
            throw new IllegalStateException("Unable to create Coven Witch fixture");
        }
        witch.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        witch.setCovenOwner(owner);
        witch.setSpecialty(specialty);
        level.addFreshEntity(witch);
        return witch;
    }

    private static void prepareGround(ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-18, -1, -18), center.offset(34, 3, 18))) {
            level.setBlock(pos, pos.getY() == center.getY() - 1
                    ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
