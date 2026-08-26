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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

/** Dedicated-server proof that villager identity survives the complete lycanthropy cycle. */
public final class WereVillagerGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STATE_CYCLE =
            TEST_FUNCTIONS.register("were_villager_state_cycle", () -> WereVillagerGameTests::stateCycle);

    private WereVillagerGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(WereVillagerGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "were_villager"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 120, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "were_villager_state_cycle"),
                new FunctionGameTestInstance(STATE_CYCLE.getKey(), data));
    }

    private static void stateCycle(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        Villager villager = EntityType.VILLAGER.create(level, EntitySpawnReason.TRIGGERED);
        if (villager == null) throw new IllegalStateException("Unable to create villager fixture");
        villager.snapTo(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D, 37.0F, 4.0F);
        villager.setVillagerData(villager.getVillagerData()
                .withType(level.registryAccess(), VillagerType.SAVANNA)
                .withProfession(level.registryAccess(), VillagerProfession.WEAPONSMITH)
                .withLevel(4));
        villager.setVillagerXp(173);
        villager.setCustomName(Component.literal("Marta Ironhand"));
        villager.setCustomNameVisible(true);
        villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        villager.getPersistentData().putString("witchery_test_state", "retained");
        villager.setHealth(17.0F);
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 3),
                new ItemStack(Items.IRON_SWORD), 7, 12, 0.05F));
        villager.setOffers(offers);
        level.addFreshEntity(villager);

        WereVillagerEntity cursed = VillagerWerewolfConversion.convertVillager(level, villager, true);
        helper.assertTrue(cursed != null, "A villager bite must create a Were-Villager");
        assertVillagerState(helper, cursed, "initial conversion");
        helper.assertTrue(cursed.isInfectious(), "A level-10 bite must create an infectious Were-Villager");

        WolfmanEntity wolfman = VillagerWerewolfConversion.convertToWolfman(level, cursed);
        helper.assertTrue(wolfman != null && VillagerWerewolfConversion.hasVillagerSnapshot(wolfman),
                "Full-moon conversion must carry the durable villager snapshot");

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        wolfman.saveWithoutId(output);
        CompoundTag savedWolfman = output.buildResult();
        WolfmanEntity loadedWolfman = WitcheryEntities.WOLFMAN.get().create(level, EntitySpawnReason.LOAD);
        if (loadedWolfman == null) throw new IllegalStateException("Unable to reload Wolfman fixture");
        loadedWolfman.load(TagValueInput.create(
                ProblemReporter.DISCARDING, level.registryAccess(), savedWolfman));
        wolfman.discard();

        WereVillagerEntity reverted = VillagerWerewolfConversion.restoreWereVillager(level, loadedWolfman);
        helper.assertTrue(reverted != null, "A reloaded Wolfman must revert to its Were-Villager identity");
        assertVillagerState(helper, reverted, "Wolfman reversion");
        helper.assertTrue(reverted.isInfectious(), "Wolfman reversion must retain infectious state");

        Villager cured = VillagerWerewolfConversion.cure(level, reverted);
        helper.assertTrue(cured != null, "Wolf cure must restore a vanilla villager");
        assertVillagerState(helper, cured, "final cure");
        cured.discard();
        helper.succeed();
    }

    private static void assertVillagerState(GameTestHelper helper, Villager villager, String phase) {
        helper.assertTrue(villager.getVillagerData().type().is(VillagerType.SAVANNA),
                phase + " lost villager type");
        helper.assertTrue(villager.getVillagerData().profession().is(VillagerProfession.WEAPONSMITH),
                phase + " lost profession");
        helper.assertTrue(villager.getVillagerData().level() == 4 && villager.getVillagerXp() == 173,
                phase + " lost level or XP");
        helper.assertTrue(villager.hasCustomName()
                        && "Marta Ironhand".equals(villager.getCustomName().getString())
                        && villager.isCustomNameVisible(),
                phase + " lost name state");
        helper.assertTrue(villager.getMainHandItem().is(Items.IRON_AXE), phase + " lost equipment");
        helper.assertTrue(villager.getOffers().size() == 1
                        && villager.getOffers().getFirst().getResult().is(Items.IRON_SWORD),
                phase + " lost trades");
        helper.assertTrue("retained".equals(villager.getPersistentData()
                        .getStringOr("witchery_test_state", "")),
                phase + " lost persistent state");
        helper.assertTrue(Math.abs(villager.getHealth() - 17.0F) < 0.01F, phase + " lost health");
    }
}
