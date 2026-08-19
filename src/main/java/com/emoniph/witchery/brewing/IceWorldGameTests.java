package com.emoniph.witchery.brewing;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.StockadeBlock;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryItems;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
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

/** Dedicated-server coverage for the composed Ice World block-impact action. */
public final class IceWorldGameTests {
    private static final BlockPos CENTER = new BlockPos(8, 3, 8);
    private static final UUID PLAYER_ID = UUID.fromString("0c6a3352-c31b-4878-9927-894cdd1d11e4");
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AREA_CONVERSION =
            TEST_FUNCTIONS.register("ice_world_area_conversion", () -> IceWorldGameTests::areaConversion);

    private IceWorldGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(IceWorldGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "ice_world"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 100, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "ice_world_area_conversion"),
                new FunctionGameTestInstance(AREA_CONVERSION.getKey(), data));
    }

    private static void areaConversion(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(CENTER);
        ServerPlayer player = createTestPlayer(level, center);
        try {
            Fixture fixture = buildFixture(level, center);
            List<net.minecraft.world.item.Item> brew = List.of(
                    WitcheryItems.PENTACLE.get(), Items.ENDER_EYE, Items.GUNPOWDER);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(center), Direction.UP, center, false);

            Brews.applyToArea(level, Vec3.atCenterOf(center), brew, Brews.Dispersal.SPLASH,
                    null, player, hit);

            helper.assertTrue(level.getBlockState(center).is(Blocks.SNOW_BLOCK),
                    "Dirt at the impact must become Snow");
            helper.assertTrue(level.getBlockState(fixture.cobblestone()).is(Blocks.PACKED_ICE),
                    "Cobblestone must become Packed Ice");
            helper.assertTrue(level.getBlockState(fixture.stone()).is(WitcheryBlocks.PERPETUAL_ICE.get()),
                    "Stone must become Perpetual Ice");
            assertFixtureStates(helper, level, fixture);
            helper.assertTrue(level.getBlockState(fixture.chest()).is(Blocks.CHEST),
                    "Block entities must not be transformed");
            helper.assertTrue(level.getBlockEntity(fixture.chest()) instanceof ChestBlockEntity chest
                            && chest.getItem(0).is(Items.DIAMOND),
                    "Skipped block entities must retain their contents");
            helper.assertTrue(level.getBlockState(fixture.bedrock()).is(Blocks.BEDROCK),
                    "Unbreakable blocks must remain untouched");
            helper.assertTrue(level.getBlockState(fixture.outside()).is(Blocks.DIRT),
                    "The strict source sphere boundary must remain outside the conversion");
            helper.succeed();
        } finally {
            removeTestPlayer(player);
            clearFixture(level, center);
        }
    }

    private static Fixture buildFixture(ServerLevel level, BlockPos center) {
        BlockPos cobblestone = center.east();
        BlockPos stone = center.west();
        BlockPos stairs = center.north();
        BlockPos slab = center.south();
        BlockPos gate = center.offset(1, 0, 1);
        BlockPos stockade = center.offset(-1, 0, -1);
        BlockPos doorLower = center.offset(3, 0, 0);
        BlockPos doorUpper = doorLower.above();
        BlockPos chest = center.offset(-2, 0, 0);
        BlockPos bedrock = center.offset(0, 0, 2);
        BlockPos outside = center.offset(5, 0, 0);

        for (BlockPos support : List.of(stairs, slab, gate, stockade, doorLower, chest, bedrock, outside)) {
            setFixtureBlock(level, support.below(), Blocks.STONE.defaultBlockState());
        }
        setFixtureBlock(level, center, Blocks.DIRT.defaultBlockState());
        setFixtureBlock(level, cobblestone, Blocks.COBBLESTONE.defaultBlockState());
        setFixtureBlock(level, stone, Blocks.STONE.defaultBlockState());
        setFixtureBlock(level, stairs, Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.WEST)
                .setValue(StairBlock.HALF, Half.TOP));
        setFixtureBlock(level, slab, Blocks.OAK_SLAB.defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.TOP));
        setFixtureBlock(level, gate, Blocks.OAK_FENCE_GATE.defaultBlockState()
                .setValue(FenceGateBlock.FACING, Direction.SOUTH)
                .setValue(FenceGateBlock.OPEN, true));
        setFixtureBlock(level, stockade, WitcheryBlocks.STOCKADE.get().defaultBlockState()
                .setValue(StockadeBlock.UP, true));

        BlockState lowerDoor = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.WEST)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(DoorBlock.HINGE, DoorHingeSide.RIGHT)
                .setValue(DoorBlock.OPEN, true);
        setFixtureBlock(level, doorLower, lowerDoor);
        setFixtureBlock(level, doorUpper, lowerDoor.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(chest) instanceof ChestBlockEntity chestBlockEntity) {
            chestBlockEntity.setItem(0, new ItemStack(Items.DIAMOND));
        }
        setFixtureBlock(level, bedrock, Blocks.BEDROCK.defaultBlockState());
        setFixtureBlock(level, outside, Blocks.DIRT.defaultBlockState());
        return new Fixture(cobblestone, stone, stairs, slab, gate, stockade,
                doorLower, doorUpper, chest, bedrock, outside);
    }

    private static void assertFixtureStates(GameTestHelper helper, ServerLevel level, Fixture fixture) {
        BlockState stairs = level.getBlockState(fixture.stairs());
        helper.assertTrue(stairs.is(WitcheryBlocks.ICE_STAIRS.get())
                        && stairs.getValue(StairBlock.FACING) == Direction.WEST
                        && stairs.getValue(StairBlock.HALF) == Half.TOP,
                "Stairs must preserve facing and half");
        BlockState slab = level.getBlockState(fixture.slab());
        helper.assertTrue(slab.is(WitcheryBlocks.ICE_SLAB.get())
                        && slab.getValue(SlabBlock.TYPE) == SlabType.TOP,
                "Slabs must preserve their half");
        BlockState gate = level.getBlockState(fixture.gate());
        helper.assertTrue(gate.is(WitcheryBlocks.ICE_GATE.get())
                        && gate.getValue(FenceGateBlock.FACING) == Direction.SOUTH
                        && gate.getValue(FenceGateBlock.OPEN),
                "Fence gates must preserve facing and open state");
        helper.assertTrue(level.getBlockState(fixture.stockade()).is(WitcheryBlocks.ICE_STOCKADE.get()),
                "Wooden Stockades must become Ice Stockades");

        BlockState lowerDoor = level.getBlockState(fixture.doorLower());
        BlockState upperDoor = level.getBlockState(fixture.doorUpper());
        helper.assertTrue(lowerDoor.is(WitcheryBlocks.ICE_DOOR.get())
                        && upperDoor.is(WitcheryBlocks.ICE_DOOR.get()),
                "Both wooden door halves must convert in the door-first pass");
        helper.assertTrue(lowerDoor.getValue(DoorBlock.FACING) == Direction.WEST
                        && lowerDoor.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT
                        && lowerDoor.getValue(DoorBlock.OPEN),
                "Ice Doors must preserve facing, hinge, and open state");
    }

    private static void setFixtureBlock(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    private static ServerPlayer createTestPlayer(ServerLevel level, BlockPos center) {
        ServerPlayer player = new FakePlayer(level, new GameProfile(PLAYER_ID, "ice-world-test"));
        if (player.connection.getConnection().channel() == null) {
            new EmbeddedChannel(player.connection.getConnection());
        }
        player.snapTo(center.getX() + 10.5D, center.getY(), center.getZ() + 0.5D,
                0.0F, 0.0F);
        level.addNewPlayer(player);
        return player;
    }

    private static void removeTestPlayer(ServerPlayer player) {
        if (player != null && player.level() instanceof ServerLevel level && !player.isRemoved()) {
            level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void clearFixture(ServerLevel level, BlockPos center) {
        for (int dx = -6; dx <= 6; dx++) {
            for (int dy = -2; dy <= 3; dy++) {
                for (int dz = -6; dz <= 6; dz++) {
                    level.setBlock(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
    }

    private record Fixture(BlockPos cobblestone, BlockPos stone, BlockPos stairs, BlockPos slab,
                           BlockPos gate, BlockPos stockade, BlockPos doorLower, BlockPos doorUpper,
                           BlockPos chest, BlockPos bedrock, BlockPos outside) {
    }
}
