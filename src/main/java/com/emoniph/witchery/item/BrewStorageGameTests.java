package com.emoniph.witchery.item;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.brewing.BrewContents;
import com.emoniph.witchery.command.HeadlessGameTestPlayer;
import com.emoniph.witchery.entity.ThrownBrewEntity;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.menu.BrewBagMenu;
import com.emoniph.witchery.menu.LeonardsUrnMenu;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Dedicated-server coverage for held-stack menu anchoring, transfers, and radial release. */
public final class BrewStorageGameTests {
    private static final BlockPos CENTER = new BlockPos(8, 3, 8);
    private static final UUID PLAYER_ID = UUID.fromString("076712da-a06e-4cd8-a1f4-f9a85870384e");
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INTERACTION_PARITY =
            TEST_FUNCTIONS.register("brew_storage_interaction_parity",
                    () -> BrewStorageGameTests::interactionParity);

    private BrewStorageGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(BrewStorageGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "brew_storage"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 120, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(Identifier.fromNamespaceAndPath(WitcheryMod.MODID,
                        "brew_storage_interaction_parity"),
                new FunctionGameTestInstance(INTERACTION_PARITY.getKey(), data));
    }

    private static void interactionParity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = createTestPlayer(level, helper.absolutePos(CENTER));
        try {
            Inventory inventory = player.getInventory();
            inventory.setSelectedSlot(0);

            ItemStack bag = new ItemStack(WitcheryItems.BREW_BAG.get());
            ItemStack brew = splashBrew(3);
            inventory.setItem(0, bag);
            inventory.setItem(9, brew);
            player.setShiftKeyDown(true);
            WitcheryItems.BREW_BAG.get().use(level, player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.containerMenu instanceof BrewBagMenu,
                    "Sneak-use must open the Brew Bag menu on the server");
            BrewBagMenu bagMenu = (BrewBagMenu) player.containerMenu;
            bagMenu.quickMoveStack(player, BrewBagItem.SLOT_COUNT);
            helper.assertTrue(BrewBagItem.getStoredBrew(bag, 0).getCount() == 3,
                    "Shift-click must persist the full brew stack into the bag");
            helper.assertTrue(inventory.getItem(9).isEmpty(),
                    "Shift-click must remove transferred brews from the player slot");

            int anchorMenuSlot = BrewBagItem.SLOT_COUNT + 27;
            bagMenu.clicked(anchorMenuSlot, 0, ClickType.THROW, player);
            bagMenu.clicked(0, inventory.getSelectedSlot(), ClickType.SWAP, player);
            helper.assertTrue(inventory.getItem(0) == bag,
                    "Menu clicks must not move or replace the held Brew Bag anchor");
            player.closeContainer();

            player.setShiftKeyDown(false);
            WitcheryItems.BREW_BAG.get().use(level, player, InteractionHand.MAIN_HAND);
            player.setXRot(player.getXRot() - 8.0F);
            WitcheryItems.BREW_BAG.get().onUseTick(level, player, bag, 35_999);
            WitcheryItems.BREW_BAG.get().releaseUsing(bag, level, player, 35_999);
            player.stopUsingItem();
            helper.assertTrue(BrewBagItem.getStoredBrew(bag, 0).getCount() == 2,
                    "Upward radial release must consume one brew from source slot 0");
            helper.assertTrue(level.getEntitiesOfClass(ThrownBrewEntity.class,
                            player.getBoundingBox().inflate(8.0D)).size() == 1,
                    "Radial release must spawn exactly one thrown brew");

            ItemStack urn = LeonardsUrnItem.createWithCapacity(4);
            inventory.setItem(0, urn);
            inventory.setItem(9, splashBrew(1));
            WitcheryItems.LEONARDS_URN.get().use(level, player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.containerMenu instanceof LeonardsUrnMenu,
                    "Use must open Leonard's Urn menu on the server");
            LeonardsUrnMenu urnMenu = (LeonardsUrnMenu) player.containerMenu;
            helper.assertTrue(urnMenu.capacity() == 4, "Upgraded Urn must expose four slots");
            urnMenu.quickMoveStack(player, 4);
            helper.assertTrue(!LeonardsUrnItem.getStoredBrew(urn, 0).isEmpty(),
                    "Urn shift-click must persist a brew in its first source slot");
            helper.assertTrue(LeonardsUrnItem.getStoredBrew(urn, 0).getCount() == 1,
                    "Every Urn slot must remain one-stack");

            ItemStack replacement = new ItemStack(Items.STICK);
            inventory.setItem(0, replacement);
            helper.assertTrue(!urnMenu.stillValid(player),
                    "Replacing the held Urn must invalidate its open menu");
            helper.succeed();
        } finally {
            removeTestPlayer(player);
        }
    }

    private static ItemStack splashBrew(int count) {
        ItemStack brew = BrewContainers.bottle(new BrewContents(List.of(Items.SPIDER_EYE, Items.GUNPOWDER)));
        brew.setCount(count);
        return brew;
    }

    private static ServerPlayer createTestPlayer(ServerLevel level, BlockPos pos) {
        ServerPlayer player = HeadlessGameTestPlayer.create(level, PLAYER_ID, "brew-storage-test");
        player.setGameMode(GameType.SURVIVAL);
        player.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        return player;
    }

    private static void removeTestPlayer(ServerPlayer player) {
        HeadlessGameTestPlayer.remove(player);
    }
}
