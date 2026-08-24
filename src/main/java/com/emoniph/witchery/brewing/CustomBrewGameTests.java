package com.emoniph.witchery.brewing;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.BrewFluidHoldBlock;
import com.emoniph.witchery.entity.familiar.ToadFamiliarEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Dedicated-server proof for the restored environmental custom-brew actions. */
public final class CustomBrewGameTests {
    private static final BlockPos CENTER = new BlockPos(8, 3, 8);
    private static final UUID PLAYER_ID = UUID.fromString("d4d99f0e-3720-4cc0-a094-4bdce0f21169");
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ENVIRONMENTAL_ACTIONS =
            TEST_FUNCTIONS.register("custom_brew_environmental_actions", () -> CustomBrewGameTests::environmentalActions);

    private CustomBrewGameTests() {}

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(CustomBrewGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "custom_brew"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 260, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "custom_brew_environmental_actions"),
                new FunctionGameTestInstance(ENVIRONMENTAL_ACTIONS.getKey(), data));
    }

    private static void environmentalActions(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(CENTER);
        BlockPos water = center.offset(-2, 1, 0);
        BlockPos lava = center.offset(2, 1, 0);
        BlockPos webSupport = center.offset(0, 1, 2);
        ServerPlayer player = createTestPlayer(level, center.offset(0, 1, -3));
        ToadFamiliarEntity toad = null;
        try {
            level.setBlock(water, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(lava, Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(webSupport, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);

            splashBlock(level, water, List.of(WitcheryItems.MANDRAKE_ROOT.get(), Items.SAND), player);
            splashBlock(level, lava, List.of(WitcheryItems.MANDRAKE_ROOT.get(), Items.NETHER_WART,
                    Items.COBBLESTONE), player);
            splashBlock(level, webSupport, List.of(Items.NETHER_WART, WitcheryItems.DENSE_WEB.get()), player);

            helper.assertTrue(level.getBlockState(water).is(WitcheryBlocks.BREW_FLUID_HOLD.get())
                            && level.getBlockState(water).getValue(BrewFluidHoldBlock.FLUID)
                            == BrewFluidHoldBlock.HeldFluid.WATER,
                    "Tide Hold must replace water with the invisible timed holder");
            helper.assertTrue(level.getBlockState(lava).is(WitcheryBlocks.BREW_FLUID_HOLD.get())
                            && level.getBlockState(lava).getValue(BrewFluidHoldBlock.FLUID)
                            == BrewFluidHoldBlock.HeldFluid.LAVA,
                    "Lava Hold must replace lava with the invisible timed holder");
            helper.assertTrue(level.getBlockState(webSupport.above()).is(Blocks.COBWEB),
                    "Webs must place a cobweb on the struck face");

            toad = WitcheryEntities.TOAD.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            helper.assertTrue(toad != null, "Poison Toad lifecycle fixture must spawn");
            toad.snapTo(center.getX() + 0.5D, center.getY() + 1.0D, center.getZ() + 0.5D, 0.0F, 0.0F);
            toad.setPoisonousLifetime(3);
            level.addFreshEntity(toad);
            ToadFamiliarEntity spawnedToad = toad;

            helper.runAfterDelay(205, () -> {
                try {
                    helper.assertTrue(level.getBlockState(water).is(Blocks.WATER),
                            "Tide Hold must restore the original water source after 200 ticks");
                    helper.assertTrue(level.getBlockState(lava).is(Blocks.LAVA),
                            "Lava Hold must restore the original lava source after 200 ticks");
                    helper.assertTrue(spawnedToad.isRemoved(),
                            "a poisonous summoned toad must discard itself when its source lifetime expires");
                    helper.succeed();
                } finally {
                    removeTestPlayer(player);
                    if (!spawnedToad.isRemoved()) spawnedToad.discard();
                }
            });
        } catch (RuntimeException | AssertionError exception) {
            removeTestPlayer(player);
            if (toad != null && !toad.isRemoved()) toad.discard();
            throw exception;
        }
    }

    private static void splashBlock(ServerLevel level, BlockPos pos, List<Item> ingredients, Entity source) {
        Vec3 hit = Vec3.atCenterOf(pos);
        Brews.applyToArea(level, hit, ingredients, Brews.Dispersal.SPLASH, null, source,
                new BlockHitResult(hit, net.minecraft.core.Direction.UP, pos, false));
    }

    private static ServerPlayer createTestPlayer(ServerLevel level, BlockPos pos) {
        ServerPlayer player = new FakePlayer(level, new GameProfile(PLAYER_ID, "custom-brew-test"));
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
