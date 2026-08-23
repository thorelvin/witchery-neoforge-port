package com.emoniph.witchery.command;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.LordOfTormentEntity;
import com.emoniph.witchery.entity.SoulfireProjectile;
import com.emoniph.witchery.entity.SpellEffectEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.world.TormentCertification;
import com.emoniph.witchery.world.TormentHelper;
import com.emoniph.witchery.world.TormentReleaseData;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
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

/** Custom-dimension certification tests intended for a normal dedicated-server fresh save. */
public final class TormentGameTests {
    private static final BlockPos RETURN_POS = new BlockPos(384, 80, 384);
    private static final UUID STRUCTURE_PLAYER_ID = UUID.fromString("514cc0bb-fdaf-4290-a413-5b61c0657e21");
    private static final UUID FIRST_PLAYER_ID = UUID.fromString("770c8872-f0af-4a06-93ed-73e36279ca87");
    private static final UUID SECOND_PLAYER_ID = UUID.fromString("8597e766-ddbd-4523-8449-962023bc0dab");
    private static final UUID PORTAL_PLAYER_ID = UUID.fromString("e708d27d-4e73-4f69-80d6-b83b76f96cee");

    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STRUCTURE_AND_REFILL =
            TEST_FUNCTIONS.register("torment_structure_and_refill", () -> TormentGameTests::structureAndRefill);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PORTAL_ROUND_TRIP =
            TEST_FUNCTIONS.register("torment_portal_round_trip", () -> TormentGameTests::portalRoundTrip);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MULTIPLAYER_BOSS =
            TEST_FUNCTIONS.register("torment_multiplayer_boss_reconnect", () -> TormentGameTests::multiplayerBossReconnect);

    private TormentGameTests() {}

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(TormentGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "torment"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                600,
                0,
                true,
                Rotation.NONE,
                true,
                1,
                1,
                false);
        event.registerTest(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "torment_structure_and_refill"),
                new FunctionGameTestInstance(STRUCTURE_AND_REFILL.getKey(), data));
        event.registerTest(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "torment_portal_round_trip"),
                new FunctionGameTestInstance(PORTAL_ROUND_TRIP.getKey(), data));
        event.registerTest(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "torment_multiplayer_boss_reconnect"),
                new FunctionGameTestInstance(MULTIPLAYER_BOSS.getKey(), data));
    }

    private static void structureAndRefill(GameTestHelper helper) {
        ServerLevel torment = helper.getLevel().getServer().getLevel(WitcheryDimensions.TORMENT);
        helper.assertTrue(torment != null, "Torment dimension must be loaded");
        if (torment == null) return;

        ServerPlayer observer = createTestPlayer(torment, STRUCTURE_PLAYER_ID, "torment-structure");
        observer.snapTo(TormentHelper.ENTRY_X + 0.5D, TormentHelper.getEntryPos(0).getY(),
                TormentHelper.ENTRY_Z + 0.5D, 0.0F, 0.0F);
        TormentCertification.Report initial = TormentCertification.audit(torment);
        helper.assertTrue(initial.passed(), describe(initial));

        helper.runAfterDelay(40, () -> {
            try {
                TormentCertification.Report refilled = TormentCertification.audit(torment);
                helper.assertTrue(refilled.passed(), describe(refilled));
                helper.assertTrue(refilled.chestCount() == TormentHelper.NUM_LEVELS * 3,
                        "Every level must retain three Refilling Chests");
                helper.assertTrue(refilled.filledChestCount() == refilled.chestCount(),
                        "Every fresh Refilling Chest must initialize after its first ticking cycle: "
                                + refilled.filledChestCount() + "/" + refilled.chestCount());
                helper.succeed();
            } finally {
                removeTestPlayer(observer);
            }
        });
    }

    private static void portalRoundTrip(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel torment = overworld.getServer().getLevel(WitcheryDimensions.TORMENT);
        helper.assertTrue(torment != null, "Torment dimension must be loaded");
        if (torment == null) return;

        prepareReturnPlatform(overworld);
        ServerPlayer player = createTestPlayer(overworld, PORTAL_PLAYER_ID, "torment-portal");
        player.snapTo(RETURN_POS.getX() + 0.5D, RETURN_POS.getY(), RETURN_POS.getZ() + 0.5D,
                0.0F, 0.0F);
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        BlockPos unrelatedSpiritAnchor = new BlockPos(12, 91, -44);
        data.setReturnPos(unrelatedSpiritAnchor, Level.OVERWORLD);
        try {
            TormentHelper.sendToTorment(player, 5);
            helper.assertTrue(player.level() == torment, "Portal entry must move the player to Torment");
            helper.assertTrue(player.blockPosition().getY() == TormentHelper.getEntryPos(5).getY(),
                    "Portal entry must use the source +2 arrival height; expected="
                            + TormentHelper.getEntryPos(5) + " actual=" + player.position());
            helper.assertTrue(data.getTormentReturnPos().filter(RETURN_POS::equals).isPresent(),
                    "Torment must persist its dedicated return anchor");

            TormentHelper.sendToTorment(player, 2);
            helper.assertTrue(data.getTormentReturnPos().filter(RETURN_POS::equals).isPresent(),
                    "More-Torment routing must preserve the original return anchor");
            TormentHelper.returnFromTorment(player);
            helper.assertTrue(player.level() == overworld, "Exit must return to the original dimension");
            helper.assertTrue(player.blockPosition().equals(RETURN_POS),
                    "Exit must return to the safe source position");
            helper.assertTrue(data.getReturnPos().filter(unrelatedSpiritAnchor::equals).isPresent(),
                    "Torment travel must not overwrite unrelated Spirit World state");
            helper.succeed();
        } finally {
            removeTestPlayer(player);
            clearReturnPlatform(overworld);
        }
    }

    private static void multiplayerBossReconnect(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel torment = overworld.getServer().getLevel(WitcheryDimensions.TORMENT);
        helper.assertTrue(torment != null, "Torment dimension must be loaded");
        if (torment == null) return;

        removeTormentTestEntities(torment);
        TormentReleaseData releases = TormentReleaseData.get(overworld.getServer());
        releases.consume(FIRST_PLAYER_ID, null);
        releases.consume(SECOND_PLAYER_ID, null);

        prepareReturnPlatform(overworld);
        ServerPlayer first = createTestPlayer(overworld, FIRST_PLAYER_ID, "torment-first");
        ServerPlayer second = createTestPlayer(overworld, SECOND_PLAYER_ID, "torment-second");
        first.snapTo(RETURN_POS.getX() + 0.5D, RETURN_POS.getY(), RETURN_POS.getZ() + 0.5D, 0.0F, 0.0F);
        second.snapTo(RETURN_POS.getX() + 2.5D, RETURN_POS.getY(), RETURN_POS.getZ() + 0.5D, 0.0F, 0.0F);
        LordOfTormentEntity phase1 = WitcheryEntities.LORD_OF_TORMENT.get()
                .create(overworld, EntitySpawnReason.MOB_SUMMONED);
        helper.assertTrue(phase1 != null, "Phase-one Lord must spawn");
        if (phase1 == null) return;
        phase1.snapTo(RETURN_POS.getX() + 1.5D, RETURN_POS.getY(), RETURN_POS.getZ() + 2.5D,
                0.0F, 0.0F);
        phase1.setPersistenceRequired();
        overworld.addFreshEntity(phase1);

        helper.runAfterDelay(1, () -> runMultiplayerBossReconnect(
                helper, overworld, torment, first, second, phase1));
    }

    private static void runMultiplayerBossReconnect(GameTestHelper helper, ServerLevel overworld,
                                                     ServerLevel torment, ServerPlayer first,
                                                     ServerPlayer second, LordOfTormentEntity phase1) {
        boolean continuationScheduled = false;
        try {
            phase1.setHealth(251.0F);
            helper.assertTrue(phase1.hurtServer(overworld,
                            first.damageSources().playerAttack(first), 5.0F),
                    "Threshold hit must damage the Lord");
            helper.assertTrue(first.level() == torment && second.level() == torment,
                    "Both nearby players must enter the same Torment encounter; first="
                            + first.level().dimension().identifier() + " " + first.position()
                            + " second=" + second.level().dimension().identifier() + " " + second.position()
                            + " phase1Removed=" + phase1.isRemoved());
            UUID session = WitcheryAttachments.get(first).getTormentSession().orElse(null);
            helper.assertTrue(session != null && WitcheryAttachments.get(second).getTormentSession()
                            .filter(session::equals).isPresent(),
                    "Both players must share one persisted encounter session");

            helper.runAfterDelay(1, () -> finishMultiplayerBossReconnect(
                    helper, overworld, torment, first, second, phase1, session));
            continuationScheduled = true;
        } finally {
            if (!continuationScheduled) {
                cleanupMultiplayerBossReconnect(overworld, torment, first, second, null, phase1);
            }
        }
    }

    private static void finishMultiplayerBossReconnect(GameTestHelper helper, ServerLevel overworld,
                                                        ServerLevel torment, ServerPlayer first,
                                                        ServerPlayer second, LordOfTormentEntity phase1,
                                                        UUID session) {
        ServerPlayer reconnect = null;
        try {
            AABB tormentBounds = new AABB(
                    TormentHelper.STRUCTURE_MIN_X, 0, TormentHelper.STRUCTURE_MIN_Z,
                    TormentHelper.STRUCTURE_MAX_X + 1, 128, TormentHelper.STRUCTURE_MAX_Z + 1);
            List<LordOfTormentEntity> allPhase2Bosses = torment.getEntitiesOfClass(
                    LordOfTormentEntity.class, tormentBounds);
            List<LordOfTormentEntity> phase2Bosses = torment.getEntitiesOfClass(
                    LordOfTormentEntity.class,
                    tormentBounds,
                    boss -> boss.isAlive() && session.equals(boss.getTormentSessionId()));
            helper.assertTrue(phase2Bosses.size() == 1,
                    "Exactly one linked phase-two Lord must exist, found " + phase2Bosses.size()
                            + "; all=" + allPhase2Bosses.stream()
                            .map(boss -> boss.getUUID() + "@" + boss.position()
                                    + ":alive=" + boss.isAlive()
                                    + ":removed=" + boss.isRemoved()
                                    + ":session=" + boss.getTormentSessionId())
                            .toList());
            LordOfTormentEntity phase2 = phase2Bosses.getFirst();
            helper.assertTrue(Math.abs(phase2.getHealth() - phase2.getMaxHealth() * 0.5F) < 0.01F,
                    "Phase two must begin at half health");
            helper.assertTrue(phase2.isNoGravity() && !phase2.removeWhenFarAway(4096.0D),
                    "The source flying boss must remain gravity-free and persistent");

            phase2.performRangedAttack(first, 1.0F);
            AABB projectileBounds = phase2.getBoundingBox().inflate(5.0D);
            int spellEffects = torment.getEntitiesOfClass(SpellEffectEntity.class, projectileBounds).size();
            int soulfire = torment.getEntitiesOfClass(SoulfireProjectile.class, projectileBounds).size();
            helper.assertTrue(spellEffects == 1 && (soulfire == 3 || soulfire == 9),
                    "A Lord volley must contain one Ignianima and 3 or 9 Soulfire projectiles; found "
                            + spellEffects + " spell effect(s) and " + soulfire + " Soulfire");
            helper.assertTrue(phase2.getAttackTimer() == 10,
                    "A ranged volley must start the source ten-tick attack pose");

            float healthBeforeCap = phase2.getHealth();
            helper.assertTrue(phase2.hurtServer(torment,
                            first.damageSources().playerAttack(first), 100.0F),
                    "A normal player hit must damage phase two");
            helper.assertTrue(Math.abs(healthBeforeCap - phase2.getHealth() - 5.0F) < 0.01F,
                    "Ordinary hits must retain the source five-damage cap");

            WitcheryPlayerData disconnectedData = new WitcheryPlayerData();
            disconnectedData.copyFrom(WitcheryAttachments.get(second), true);
            removeTestPlayer(second);

            phase2.setHealth(1.0F);
            phase2.invulnerableTime = 0;
            BlockPos deathPos = phase2.blockPosition();
            phase2.hurtServer(torment, first.damageSources().playerAttack(first), 5.0F);
            helper.assertTrue(first.level() == overworld,
                    "Online participant must return when phase two dies");
            helper.assertTrue(TormentReleaseData.get(overworld.getServer()).pendingPlayerCount() == 1,
                    "Disconnected participant must remain in the persisted release queue");

            List<ItemEntity> drops = torment.getEntitiesOfClass(
                    ItemEntity.class, new AABB(deathPos).inflate(4.0D));
            int enchantedBooks = drops.stream().filter(item -> item.getItem().is(Items.ENCHANTED_BOOK))
                    .mapToInt(item -> item.getItem().getCount()).sum();
            boolean booksCarryEnchantments = drops.stream()
                    .filter(item -> item.getItem().is(Items.ENCHANTED_BOOK))
                    .allMatch(item -> EnchantmentHelper.hasAnyEnchantments(item.getItem()));
            int demonHearts = drops.stream().filter(item -> item.getItem().is(WitcheryItems.DEMON_HEART.get()))
                    .mapToInt(item -> item.getItem().getCount()).sum();
            int tormentSouls = drops.stream().filter(item -> item.getItem().is(WitcheryItems.SOUL_OF_TORMENT.get()))
                    .mapToInt(item -> item.getItem().getCount()).sum();
            helper.assertTrue(enchantedBooks == 2 && booksCarryEnchantments
                            && demonHearts == 1 && tormentSouls == 1,
                    "Lord loot must be two enchanted books, one Demon Heart, and one Soul of Torment; found "
                            + enchantedBooks + "/" + demonHearts + "/" + tormentSouls);

            reconnect = createTestPlayer(torment, SECOND_PLAYER_ID, "torment-second");
            WitcheryAttachments.get(reconnect).copyFrom(disconnectedData, true);
            reconnect.snapTo(TormentHelper.ENTRY_X + 0.5D, TormentHelper.getEntryPos(0).getY(),
                    TormentHelper.ENTRY_Z + 0.5D, 0.0F, 0.0F);
            helper.assertTrue(TormentHelper.releaseIfDefeated(reconnect),
                    "Reconnect must consume the persisted boss release");
            helper.assertTrue(reconnect.level() == overworld,
                    "Reconnected participant must return to the source dimension");
            helper.assertTrue(TormentReleaseData.get(overworld.getServer()).pendingPlayerCount() == 0,
                    "All participants returning must clear the release queue");
            helper.succeed();
        } finally {
            cleanupMultiplayerBossReconnect(overworld, torment, first, second, reconnect, phase1);
        }
    }

    private static void cleanupMultiplayerBossReconnect(ServerLevel overworld, ServerLevel torment,
                                                         ServerPlayer first, ServerPlayer second,
                                                         ServerPlayer reconnect,
                                                         LordOfTormentEntity phase1) {
        removeTestPlayer(first);
        removeTestPlayer(second);
        removeTestPlayer(reconnect);
        phase1.discard();
        removeTormentTestEntities(torment);
        TormentReleaseData releases = TormentReleaseData.get(overworld.getServer());
        releases.consume(FIRST_PLAYER_ID, null);
        releases.consume(SECOND_PLAYER_ID, null);
        clearReturnPlatform(overworld);
    }

    private static String describe(TormentCertification.Report report) {
        return "Torment certificate has " + report.mismatchCount() + " mismatch(es): "
                + String.join(" | ", report.failures());
    }

    private static ServerPlayer createTestPlayer(ServerLevel level, UUID id, String name) {
        return HeadlessGameTestPlayer.create(level, id, name);
    }

    private static void removeTestPlayer(ServerPlayer player) {
        HeadlessGameTestPlayer.remove(player);
    }

    private static void prepareReturnPlatform(ServerLevel overworld) {
        for (int x = RETURN_POS.getX() - 2; x <= RETURN_POS.getX() + 4; x++) {
            for (int z = RETURN_POS.getZ() - 2; z <= RETURN_POS.getZ() + 4; z++) {
                overworld.setBlock(new BlockPos(x, RETURN_POS.getY() - 1, z),
                        Blocks.BEDROCK.defaultBlockState(), 3);
                overworld.setBlock(new BlockPos(x, RETURN_POS.getY(), z),
                        Blocks.AIR.defaultBlockState(), 3);
                overworld.setBlock(new BlockPos(x, RETURN_POS.getY() + 1, z),
                        Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static void clearReturnPlatform(ServerLevel overworld) {
        for (int x = RETURN_POS.getX() - 2; x <= RETURN_POS.getX() + 4; x++) {
            for (int z = RETURN_POS.getZ() - 2; z <= RETURN_POS.getZ() + 4; z++) {
                for (int y = RETURN_POS.getY() - 1; y <= RETURN_POS.getY() + 1; y++) {
                    overworld.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void removeTormentTestEntities(ServerLevel torment) {
        AABB bounds = new AABB(TormentHelper.STRUCTURE_MIN_X - 8, 0,
                TormentHelper.STRUCTURE_MIN_Z - 8, TormentHelper.STRUCTURE_MAX_X + 9,
                128, TormentHelper.STRUCTURE_MAX_Z + 9);
        torment.getEntitiesOfClass(Entity.class, bounds, entity -> !(entity instanceof ServerPlayer))
                .forEach(Entity::discard);
    }
}
