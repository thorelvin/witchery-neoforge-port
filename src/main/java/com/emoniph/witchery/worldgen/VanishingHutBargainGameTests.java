package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.entity.CrystalBallBlockEntity;
import com.emoniph.witchery.command.HeadlessGameTestPlayer;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.item.TaglockKitItem;
import com.emoniph.witchery.world.VanishingHutData;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Consumer;

/** Dedicated-server transaction, persistence, restraint, and vague-scrying contract. */
public final class VanishingHutBargainGameTests {
    private static final UUID HUT_ID = UUID.fromString("14d2e61d-a952-462c-8b4c-1aa61d86ac91");
    private static final UUID PLAYER_ID = UUID.fromString("736f7bd1-d6ea-4f7d-99df-b5ed80029db4");
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BARGAIN =
            TEST_FUNCTIONS.register("vanishing_hut_bargain_cycle",
                    () -> VanishingHutBargainGameTests::bargainCycle);

    private VanishingHutBargainGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(VanishingHutBargainGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "vanishing_hut_bargain"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 80, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "vanishing_hut_bargain_cycle"),
                new FunctionGameTestInstance(BARGAIN.getKey(), data));
    }

    private static void bargainCycle(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fixture = helper.absolutePos(new BlockPos(4, 2, 4));
        int y = Math.clamp(fixture.getY() + 70, level.getMinY() + 10, level.getMaxY() - 10);
        BlockPos home = new BlockPos(fixture.getX(), y, fixture.getZ());
        BlockPos hutAnchor = VanishingHut.anchorFromResidentHome(home);
        BlockPos trackedPos = new BlockPos(413, 91, -277);
        VanishingHutData huts = VanishingHutData.get(level);
        huts.remove(HUT_ID);

        CovenWitchEntity witch = WitcheryEntities.COVEN_WITCH.get().create(level, EntitySpawnReason.TRIGGERED);
        helper.assertTrue(witch != null, "The server must create the hut resident");
        witch.snapTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 0.0F, 0.0F);
        witch.setHutHome(home);
        level.addFreshEntity(witch);
        huts.upsert(HUT_ID, level, hutAnchor, witch.getUUID());

        ServerPlayer player = HeadlessGameTestPlayer.create(level, PLAYER_ID, "HutCustomer");
        player.snapTo(home.getX() + 1.5D, home.getY(), home.getZ() + 0.5D, 0.0F, 0.0F);
        VanishingHutBargains.Bargain first = VanishingHutBargains.forCycle(HUT_ID, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(first.cost(), first.costCount()));
        player.setShiftKeyDown(true);
        witch.mobInteract(player, InteractionHand.MAIN_HAND);

        helper.assertTrue(huts.get(HUT_ID).orElseThrow().bargainCompleted(),
                "The accepted bargain must persist as completed for this relocation cycle");
        helper.assertTrue(player.getInventory().countItem(first.reward()) == first.rewardCount(),
                "The resident must deliver exactly the selected existing-material reward");
        int rewardAfterFirstUse = player.getInventory().countItem(first.reward());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(first.cost(), first.costCount()));
        witch.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getInventory().countItem(first.reward()) == rewardAfterFirstUse,
                "Repeated interaction in one cycle must not duplicate the rare reward");

        BlockPos restrainedAnchor = hutAnchor.offset(20, 0, 20);
        VanishingHutData.Entry next = huts.relocate(HUT_ID, level, restrainedAnchor, witch.getUUID());
        helper.assertTrue(next.bargainCycle() == 1 && next.bargainAvailable(),
                "A successful relocation must open exactly one fresh cycle");
        int saltY = VanishingHut.groundY(restrainedAnchor) + 1;
        int saltZ = restrainedAnchor.getZ() - 3;
        for (int dx = -1; dx <= 1; dx++) {
            level.setBlock(new BlockPos(restrainedAnchor.getX() + dx, saltY, saltZ),
                    WitcheryBlocks.SALT_LINE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        VanishingHutBargains.Bargain second = VanishingHutBargains.forCycle(HUT_ID, 1);
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(second.cost(), second.costCount()));
        witch.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!huts.get(HUT_ID).orElseThrow().bargainCompleted()
                        && huts.get(HUT_ID).orElseThrow().restraintStage() == 1
                        && player.getMainHandItem().getCount() == second.costCount(),
                "A newly placed physical salt line must immediately forfeit the bargain without consumption");

        ItemStack residentTaglock = TaglockKitItem.filledEntityTaglock(witch);
        huts.relocate(HUT_ID, level, trackedPos, witch.getUUID());
        player.setShiftKeyDown(false);
        witch.discard();
        player.getAbilities().instabuild = true;
        CrystalBallBlockEntity crystal = new CrystalBallBlockEntity(home,
                WitcheryBlocks.CRYSTAL_BALL.get().defaultBlockState());
        helper.assertTrue(crystal.tryScryTaglock(player, level, residentTaglock),
                "The persisted resident Taglock must produce a glimpse even when the witch is unloaded");
        String glimpse = CrystalBallBlockEntity.describeHutSurroundings(
                level, huts.get(HUT_ID).orElseThrow());
        helper.assertTrue(!glimpse.contains("413") && !glimpse.contains("91") && !glimpse.contains("-277"),
                "The hut glimpse must never leak its tracked coordinates");

        huts.remove(HUT_ID);
        HeadlessGameTestPlayer.remove(player);
        helper.succeed();
    }
}
