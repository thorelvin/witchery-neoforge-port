package com.emoniph.witchery.command;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.WolfAltarBlock;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.data.WitcheryPlayerData.WolfQuestState;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.WerewolfProgression;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Consumer;

/** Integrated server fixture for the complete Wolf Altar level-one-to-ten path. */
public final class LycanthropyGameTests {
    private static final BlockPos ALTAR_RELATIVE_POS = new BlockPos(1, 2, 1);
    private static final UUID PLAYER_ID = UUID.fromString("bba86f7c-31c8-426b-9c86-700015518d75");
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PROGRESSION =
            TEST_FUNCTIONS.register("lycanthropy_progression", () -> LycanthropyGameTests::progression);

    private LycanthropyGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(LycanthropyGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "lycanthropy"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 200, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "lycanthropy_progression"),
                new FunctionGameTestInstance(PROGRESSION.getKey(), data));
    }

    private static void progression(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos altarPos = helper.absolutePos(ALTAR_RELATIVE_POS);
        level.setBlock(altarPos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(altarPos, WitcheryBlocks.WOLF_ALTAR.get().defaultBlockState(), 3);
        ServerPlayer player = createTestPlayer(level, altarPos);

        try {
            WitcheryPlayerData data = WitcheryAttachments.get(player);
            data.setWerewolfLevel(1);

            ItemStack shortOffering = new ItemStack(Items.GOLD_INGOT, 2);
            interact(level, altarPos, player, shortOffering);
            helper.assertTrue(data.getWerewolfLevel() == 1 && shortOffering.getCount() == 2,
                    "An incomplete offering must neither advance nor consume items");

            ItemStack gold = new ItemStack(Items.GOLD_INGOT, 3);
            interact(level, altarPos, player, gold);
            helper.assertTrue(data.getWerewolfLevel() == 2 && gold.isEmpty(),
                    "Three gold ingots must grant level two and a Moon Charm");

            ItemStack mutton = new ItemStack(Items.MUTTON, 30);
            interact(level, altarPos, player, mutton);
            helper.assertTrue(data.getWerewolfLevel() == 3 && mutton.isEmpty(),
                    "Thirty raw mutton must grant level three");

            ItemStack tongues = new ItemStack(WitcheryItems.TONGUE_OF_DOG.get(), 10);
            interact(level, altarPos, player, tongues);
            helper.assertTrue(data.getWerewolfLevel() == 4 && tongues.isEmpty(),
                    "Ten Tongues of Dog must grant level four");

            interact(level, altarPos, player, ItemStack.EMPTY);
            helper.assertTrue(data.getWolfQuestState() == WolfQuestState.STARTED,
                    "Level four must issue the Horn of the Hunt quest");
            data.setWolfQuestState(WolfQuestState.COMPLETE);
            interact(level, altarPos, player, ItemStack.EMPTY);
            helper.assertTrue(data.getWerewolfLevel() == 5,
                    "Returning after the Huntsman dies must grant level five");

            for (int currentLevel = 5; currentLevel <= 9; currentLevel++) {
                interact(level, altarPos, player, ItemStack.EMPTY);
                helper.assertTrue(data.getWolfQuestState() == WolfQuestState.STARTED,
                        "Level " + currentLevel + " must start its task at the altar");
                int target = WerewolfProgression.questTarget(currentLevel);
                for (int i = 0; i < target; i++) data.incrementWolfQuestCounter();
                interact(level, altarPos, player, ItemStack.EMPTY);
                helper.assertTrue(data.getWerewolfLevel() == currentLevel + 1,
                        "Completing the level " + currentLevel + " task must advance exactly once");
            }

            helper.assertTrue(data.getWerewolfLevel() == WerewolfProgression.MAX_LEVEL,
                    "The complete altar path must finish at level ten");
            helper.succeed();
        } finally {
            removeTestPlayer(player);
            level.setBlock(altarPos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(altarPos.below(), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static void interact(ServerLevel level, BlockPos altarPos, ServerPlayer player,
                                 ItemStack stack) {
        WolfAltarBlock altar = WitcheryBlocks.WOLF_ALTAR.get();
        altar.useItemOn(stack, level.getBlockState(altarPos), level, altarPos, player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(altarPos), Direction.UP, altarPos, false));
    }

    private static ServerPlayer createTestPlayer(ServerLevel level, BlockPos pos) {
        ServerPlayer player = new FakePlayer(level, new GameProfile(PLAYER_ID, "lycanthropy-progression"));
        if (player.connection.getConnection().channel() == null) {
            new EmbeddedChannel(player.connection.getConnection());
        }
        player.snapTo(pos.getX() + 2.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        level.addNewPlayer(player);
        return player;
    }

    private static void removeTestPlayer(ServerPlayer player) {
        if (player != null && player.level() instanceof ServerLevel level && !player.isRemoved()) {
            level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
    }
}
