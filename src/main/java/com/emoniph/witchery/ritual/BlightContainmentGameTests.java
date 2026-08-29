package com.emoniph.witchery.ritual;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.AltarBlock;
import com.emoniph.witchery.blocks.BlightHelper;
import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.command.HeadlessGameTestPlayer;
import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryItems;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Dedicated-server coverage for the default-off Blight Heart containment boundary. */
public final class BlightContainmentGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CONTAINMENT =
            TEST_FUNCTIONS.register("blight_heart_containment",
                    () -> BlightContainmentGameTests::containment);

    private BlightContainmentGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(BlightContainmentGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "blight_heart_containment"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 100, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "blight_heart_containment"),
                new FunctionGameTestInstance(CONTAINMENT.getKey(), data));
    }

    private static void containment(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fixture = helper.absolutePos(new BlockPos(4, 2, 4));
        int y = Math.clamp(fixture.getY() + 80, level.getMinY() + 20, level.getMaxY() - 20);
        BlockPos center = new BlockPos(fixture.getX(), y, fixture.getZ());
        ServerPlayer owner = HeadlessGameTestPlayer.create(level,
                UUID.fromString("b11953ca-c956-4dd7-af75-6a70f720ed32"), "BlightContainmentOwner");
        owner.snapTo(center.getX() + 0.5D, center.getY() + 1.0D,
                center.getZ() + 0.5D, 0.0F, 0.0F);

        helper.assertTrue(!WitcheryConfig.ENABLE_BLIGHT_HEART.get(),
                "The Blight Heart containment fixture requires the release-default closed gate");

        AltarBlockEntity altar = placePoweredAltar(level, center.south(8));
        helper.assertTrue(altar != null && altar.hasValidStructure()
                        && altar.getEffectiveMaxPower() >= 3000,
                "The fixture must provide enough valid altar capacity to isolate the disabled gate");
        if (altar == null) {
            HeadlessGameTestPlayer.remove(owner);
            throw new IllegalStateException("Unable to create Blight containment altar");
        }
        altar.refundPower(3000);
        int powerBefore = altar.getPower();

        List<ItemEntity> sources = new ArrayList<>();
        sources.add(drop(level, center.north(), new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get())));
        sources.add(drop(level, center.north().east(), new ItemStack(WitcheryItems.REDSTONE_SOUP.get())));
        sources.add(drop(level, center.east(), new ItemStack(WitcheryItems.REEK_OF_MISFORTUNE.get())));
        sources.add(drop(level, center.south().east(), new ItemStack(Items.FERMENTED_SPIDER_EYE)));
        sources.add(drop(level, center.south(), new ItemStack(Items.POISONOUS_POTATO)));
        sources.add(drop(level, center.south().west(), new ItemStack(Items.ROTTEN_FLESH)));
        sources.add(drop(level, center.west(), new ItemStack(Items.DIAMOND)));
        List<RitualOffering> offerings = sources.stream().map(RitualOffering::dropped).toList();

        boolean activated = RitualExecutor.startBlightHeart(owner, level, level, center, offerings);
        helper.assertTrue(!activated, "A disabled Blight Heart rite must refuse activation");
        helper.assertTrue(altar.getPower() == powerBefore,
                "A disabled Blight Heart rite must spend no altar power");
        helper.assertTrue(sources.stream().allMatch(source -> source.isAlive() && source.getItem().getCount() == 1),
                "A disabled Blight Heart rite must consume no offering");
        helper.assertTrue(RitualSavedData.get(level).sacrificeBatches.stream()
                        .noneMatch(batch -> batch.center.equals(center)),
                "A disabled Blight Heart rite must not create a ritual escrow");

        ItemStack brew = new ItemStack(WitcheryItems.BREW_OF_BLIGHT.get());
        owner.setItemInHand(InteractionHand.MAIN_HAND, brew);
        InteractionResult brewUse = WitcheryItems.BREW_OF_BLIGHT.get()
                .use(level, owner, InteractionHand.MAIN_HAND);
        helper.assertTrue(brewUse == InteractionResult.FAIL && brew.getCount() == 1,
                "A disabled Brew of Blight must refuse before drinking or consuming the bottle");
        ItemStack forcedFinish = new ItemStack(WitcheryItems.BREW_OF_BLIGHT.get());
        WitcheryItems.BREW_OF_BLIGHT.get().finishUsingItem(forcedFinish, level, owner);
        helper.assertTrue(forcedFinish.getCount() == 1,
                "The disabled finish-use path must also preserve the Brew of Blight bottle");
        BlockPos refusedSeed = center.west(4);
        level.setBlock(refusedSeed, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(!BlightHelper.placeHeart(level, refusedSeed, true)
                        && level.getBlockState(refusedSeed).is(Blocks.DIRT),
                "The shared Heart placement boundary must reject disabled seeding");

        BlockPos heartPos = center.east(28);
        level.setBlock(heartPos, WitcheryBlocks.BLIGHT_HEART.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(heartPos.east(), WitcheryBlocks.BLIGHTED_STONE.get().defaultBlockState(), Block.UPDATE_ALL);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                BlockPos pos = heartPos.offset(dx, 0, dz);
                if (!BlightHelper.isBlight(level.getBlockState(pos))) {
                    level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        int blightBefore = countBlight(level, heartPos, 4);
        RandomSource spreadRandom = RandomSource.create(0xB11947L);
        for (int i = 0; i < 64; i++) {
            BlightHelper.spreadFromHeart(level, heartPos, spreadRandom);
            BlightHelper.trySpreadNear(level, heartPos.east(), spreadRandom, true);
        }
        helper.assertTrue(countBlight(level, heartPos, 4) == blightBefore,
                "A loaded disabled Heart and adjacent taint must not spread");

        BlockPos orphanPos = center.south(28);
        level.setBlock(orphanPos, WitcheryBlocks.BLIGHTED_STONE.get().defaultBlockState(), Block.UPDATE_ALL);
        RandomSource cleanupRandom = RandomSource.create(0xC1EA4L);
        for (int i = 0; i < 64 && level.getBlockState(orphanPos).is(WitcheryBlocks.BLIGHTED_STONE.get()); i++) {
            BlightHelper.decayIfOrphaned(level, orphanPos, level.getBlockState(orphanPos), cleanupRandom);
        }
        helper.assertTrue(level.getBlockState(orphanPos).is(Blocks.STONE),
                "Disabled orphaned taint must perform bounded local cleanup");
        helper.assertTrue(level.getBlockState(heartPos).is(WitcheryBlocks.BLIGHT_HEART.get()),
                "The disabled gate must keep existing Blight Heart IDs loadable in-world");

        sources.forEach(ItemEntity::discard);
        HeadlessGameTestPlayer.remove(owner);
        helper.succeed();
    }

    private static AltarBlockEntity placePoweredAltar(ServerLevel level, BlockPos backLeft) {
        for (int z = -4; z <= 4; z++) {
            for (int x = -4; x <= 6; x++) {
                level.setBlock(backLeft.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(),
                        Block.UPDATE_ALL);
            }
        }
        for (int z = 0; z < 2; z++) {
            for (int x = 0; x < 3; x++) {
                BlockPos pos = backLeft.offset(x, 0, z);
                level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(pos, WitcheryBlocks.ALTAR.get().defaultBlockState()
                        .setValue(AltarBlock.AXIS, Direction.Axis.X), Block.UPDATE_ALL);
                level.setBlock(pos.above(), Blocks.PLAYER_HEAD.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        for (int z = 0; z < 2; z++) {
            for (int x = 0; x < 3; x++) {
                if (level.getBlockEntity(backLeft.offset(x, 0, z)) instanceof AltarBlockEntity altar) {
                    altar.invalidate(level);
                }
            }
        }
        return RitualExecutor.findNearbyRitualAltar(level, backLeft, 16);
    }

    private static ItemEntity drop(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D, stack);
        level.addFreshEntity(item);
        return item;
    }

    private static int countBlight(ServerLevel level, BlockPos center, int radius) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius),
                center.offset(radius, 1, radius))) {
            if (BlightHelper.isBlight(level.getBlockState(pos))) {
                count++;
            }
        }
        return count;
    }
}
