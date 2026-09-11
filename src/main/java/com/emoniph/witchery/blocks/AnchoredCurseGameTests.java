package com.emoniph.witchery.blocks;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.blocks.entity.PoppetShelfBlockEntity;
import com.emoniph.witchery.command.HeadlessGameTestPlayer;
import com.emoniph.witchery.data.AnchoredCurseSavedData;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.PoppetItem;
import com.emoniph.witchery.item.RedThreadBinding;
import com.emoniph.witchery.item.TaglockKitItem;
import com.emoniph.witchery.ritual.RitualCircleDetector;
import com.emoniph.witchery.ritual.RitualExecutor;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Consumer;

/** Dedicated-server lifecycle coverage for the narrow Red Thread curse anchor. */
public final class AnchoredCurseGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LIFECYCLE =
            TEST_FUNCTIONS.register("red_thread_anchored_sinking",
                    () -> AnchoredCurseGameTests::lifecycle);

    private AnchoredCurseGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(AnchoredCurseGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "red_thread_anchored_sinking"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 220, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "red_thread_anchored_sinking"),
                new FunctionGameTestInstance(LIFECYCLE.getKey(), data));
    }

    private static void lifecycle(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fixture = helper.absolutePos(new BlockPos(4, 2, 4));
        int y = Math.clamp(fixture.getY() + 80, level.getMinY() + 20, level.getMaxY() - 20);
        BlockPos center = new BlockPos(fixture.getX(), y, fixture.getZ());
        BlockPos shelfPos = center.north(10);
        ServerPlayer caster = HeadlessGameTestPlayer.create(
                level, UUID.randomUUID(), "RedThreadCaster");
        ServerPlayer target = HeadlessGameTestPlayer.create(
                level, UUID.randomUUID(), "RedThreadTarget");
        caster.setGameMode(GameType.SURVIVAL);
        target.setGameMode(GameType.SURVIVAL);
        caster.snapTo(center.getX() + 0.5D, center.getY() + 1.0D,
                center.getZ() + 0.5D, 0.0F, 0.0F);
        target.snapTo(center.getX() + 3.5D, center.getY() + 1.0D,
                center.getZ() + 0.5D, 0.0F, 0.0F);

        prepareGround(level, center);
        placeInfernalRing(level, center);
        AltarBlockEntity altar = placePoweredAltar(level, center.east(10));
        helper.assertTrue(altar != null && altar.hasValidStructure() && altar.getPower() >= 2_000,
                "The Red Thread fixture needs a valid altar holding the full Sinking cost");

        level.setBlock(shelfPos, WitcheryBlocks.POPPET_SHELF.get().defaultBlockState(), Block.UPDATE_ALL);
        PoppetShelfBlockEntity shelf = (PoppetShelfBlockEntity) level.getBlockEntity(shelfPos);
        helper.assertTrue(shelf != null, "The anchor fixture must place a Poppet Shelf block entity");
        ItemStack poppet = new ItemStack(WitcheryItems.POPPET.get());
        PoppetItem.bind(poppet, target);
        shelf.setItem(0, poppet);

        ItemStack thread = new ItemStack(WitcheryItems.RED_THREAD.get());
        caster.setItemInHand(InteractionHand.MAIN_HAND, thread);
        InteractionResult prepared = WitcheryBlocks.POPPET_SHELF.get().useItemOn(
                thread, level.getBlockState(shelfPos), level, shelfPos, caster, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(shelfPos), Direction.UP, shelfPos, false));
        helper.assertTrue(prepared == InteractionResult.SUCCESS && thread.isEmpty()
                        && RedThreadBinding.isPrepared(shelf.getItem(0)),
                "Using Red Thread must consume one thread and prepare the sole player-bound plain Poppet");

        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 600, 1));
        drop(level, center, TaglockKitItem.filledPlayerTaglock(target.getUUID(), target.getName().getString()));
        drop(level, center, new ItemStack(WitcheryItems.EXHALE_OF_HORNED_ONE.get()));
        drop(level, center, new ItemStack(Items.INK_SAC));
        drop(level, center, new ItemStack(Items.NETHER_WART));
        drop(level, center, new ItemStack(WitcheryItems.BREW_OF_THE_GROTESQUE.get()));
        level.setWeatherParameters(0, 6_000, true, true);

        helper.runAfterDelay(105, () -> {
            var circles = RitualCircleDetector.detectPresent(level, center);
            helper.assertTrue(level.isThundering(), "The Sinking fixture must establish a thunderstorm");
            helper.assertTrue(circles.stream().anyMatch(result ->
                                    result.size() == RitualCircleDetector.CircleSize.MEDIUM)
                            && RitualCircleDetector.detectVariant(
                            level, center, RitualCircleDetector.CircleSize.MEDIUM) == 3,
                    "The Sinking fixture must expose one complete Medium Infernal ring");
            AnchoredCurseSavedData.Preparation preflight = AnchoredCurseSavedData.get(level.getServer())
                    .prepareSinking(caster, target);
            helper.assertTrue(preflight.allowed() && preflight.anchored(),
                    "The real rite preflight must discover exactly one valid prepared anchor, got "
                            + preflight.issue());

            RitualExecutor.ActivationResult activation = RitualExecutor.execute(
                    caster, level, center, circles, ItemStack.EMPTY);
            AnchoredCurseSavedData saved = AnchoredCurseSavedData.get(level.getServer());
            helper.assertTrue(activation == RitualExecutor.ActivationResult.SUCCESS_NO_CONSUME,
                    "The complete Infernal Sinking rite must activate through the normal recipe path, got "
                            + activation + " with altar power " + altar.getPower());
            helper.assertTrue(saved.clueFor(target.getUUID()).isPresent()
                            && RedThreadBinding.isActive(shelf.getItem(0)),
                    "A successful source curse must persist exactly one active shelf binding");
            helper.assertTrue(target.getEffect(MobEffects.SLOWNESS) != null
                            && target.getEffect(MobEffects.SLOWNESS).getAmplifier() >= 3
                            && target.hasEffect(MobEffects.MINING_FATIGUE),
                    "The anchored rite must still apply the original Curse of Sinking effects");

            helper.runAfterDelay(25, () -> {
                target.removeEffect(MobEffects.SLOWNESS);
                target.removeEffect(MobEffects.MINING_FATIGUE);
                helper.runAfterDelay(AnchoredCurseSavedData.TICK_INTERVAL + 1, () -> {
                    helper.assertTrue(target.hasEffect(MobEffects.SLOWNESS)
                                    && target.hasEffect(MobEffects.MINING_FATIGUE),
                            "A loaded intact anchor must renew the original finite curse effects");

                    level.destroyBlock(shelfPos, false, caster);
                    MobEffectInstance restored = target.getEffect(MobEffects.SLOWNESS);
                    helper.assertTrue(saved.clueFor(target.getUUID()).isEmpty()
                                    && restored != null && restored.getAmplifier() == 1
                                    && !target.hasEffect(MobEffects.MINING_FATIGUE),
                            "Breaking the shelf must end the record and restore the exact prior Sinking effects");

                    BlockPos replacementPos = shelfPos.east();
                    level.setBlock(replacementPos, WitcheryBlocks.POPPET_SHELF.get().defaultBlockState(),
                            Block.UPDATE_ALL);
                    PoppetShelfBlockEntity replacement =
                            (PoppetShelfBlockEntity) level.getBlockEntity(replacementPos);
                    helper.assertTrue(replacement != null,
                            "The ward fixture must place a replacement Poppet Shelf block entity");
                    ItemStack replacementPoppet = new ItemStack(WitcheryItems.POPPET.get());
                    PoppetItem.bind(replacementPoppet, target);
                    helper.assertTrue(RedThreadBinding.prepare(replacementPoppet, caster),
                            "The ward fixture must prepare a replacement binding");
                    replacement.setItem(0, replacementPoppet);
                    helper.assertTrue(saved.prepareSinking(caster, target).issue()
                                    == AnchoredCurseSavedData.PreparationIssue.WARDED,
                            "A broken anchor must briefly ward this caster-target pair against rebinding");

                    HeadlessGameTestPlayer.remove(caster);
                    HeadlessGameTestPlayer.remove(target);
                    helper.succeed();
                });
            });
        });
    }

    private static void prepareGround(ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-16, -1, -16), center.offset(16, 3, 16))) {
            level.setBlock(pos, pos.getY() == center.getY() - 1
                    ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(center, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static void placeInfernalRing(ServerLevel level, BlockPos center) {
        for (BlockPos mark : RitualCircleDetector.ringPositions(
                center, RitualCircleDetector.CircleSize.MEDIUM)) {
            level.setBlock(mark.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(mark, WitcheryBlocks.CIRCLE_INFERNAL.get().defaultBlockState(), Block.UPDATE_ALL);
        }
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
        AltarBlockEntity result = null;
        for (int z = 0; z < 2; z++) {
            for (int x = 0; x < 3; x++) {
                if (level.getBlockEntity(backLeft.offset(x, 0, z)) instanceof AltarBlockEntity altar) {
                    altar.invalidate(level);
                    altar.refundPower(100_000);
                    if (result == null) result = altar;
                }
            }
        }
        return result;
    }

    private static void drop(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 1.5D,
                pos.getZ() + 0.5D, stack);
        item.setDeltaMovement(Vec3.ZERO);
        item.setNoGravity(true);
        level.addFreshEntity(item);
    }
}
