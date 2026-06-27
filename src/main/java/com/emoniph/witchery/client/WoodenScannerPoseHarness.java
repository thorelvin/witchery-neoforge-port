package com.emoniph.witchery.client;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

public final class WoodenScannerPoseHarness {
    private static final boolean ENABLED = Boolean.getBoolean("witchery.scannerPoseHarness");
    private static final boolean CAPTURE = Boolean.getBoolean("witchery.scannerPoseHarness.capture");
    private static final boolean EXIT_AFTER_CAPTURE = Boolean.getBoolean("witchery.scannerPoseHarness.exitAfterCapture");
    private static final boolean INVENTORY_CAPTURE = Boolean.getBoolean("witchery.scannerPoseHarness.inventoryCapture");
    private static final boolean POSE_STUDIO = Boolean.getBoolean("witchery.scannerPoseHarness.poseStudio");
    private static final String RUN_ID = System.getProperty("witchery.scannerPoseHarness.runId",
            Long.toString(System.currentTimeMillis()));
    private static final int[] CAPTURE_USE_TICKS = {4, 8, 12, 16, 20, 26};

    private static int nextCaptureIndex;
    private static int exitCountdown = -1;
    private static boolean loggedEnabled;
    private static int commandRetryCooldown;
    private static int debugTicks;
    private static int harnessUseTicks;
    private static int setupTicks;
    private static boolean setupCommandsSent;
    private static int inventoryScreenTicks = -1;
    private static boolean inventoryCaptured;

    private WoodenScannerPoseHarness() {
    }

    public static void clientTick() {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!loggedEnabled) {
            loggedEnabled = true;
            WitcheryMod.LOGGER.info("Wooden scanner pose harness enabled (capture={}, runId={})", CAPTURE, RUN_ID);
        }
        if (!(mc.player instanceof LocalPlayer player) || mc.level == null) {
            if (++debugTicks % 100 == 0) {
                WitcheryMod.LOGGER.info("Wooden scanner pose harness waiting (player={}, level={}, screen={})",
                        mc.player != null, mc.level != null,
                        mc.screen == null ? "none" : mc.screen.getClass().getSimpleName());
            }
            return;
        }
        if (INVENTORY_CAPTURE && mc.screen != null) {
            if (mc.screen instanceof InventoryScreen && inventoryScreenTicks >= 0) {
                inventoryScreenTicks++;
                captureInventoryFrame();
            }
            tickExitCountdown(mc);
            return;
        }
        if (mc.screen != null) {
            tickExitCountdown(mc);
            return;
        }
        if (commandRetryCooldown > 0) {
            commandRetryCooldown--;
        }
        if (POSE_STUDIO && !setupCommandsSent && mc.getConnection() != null) {
            setupCommandsSent = true;
            commandRetryCooldown = 40;
            sendPoseStudioCommands(mc);
            WitcheryMod.LOGGER.info("Wooden scanner pose harness prepared clean pose studio");
            return;
        }
        if (POSE_STUDIO && setupTicks++ < 60) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (!held.is(WitcheryItems.WOODEN_SCANNER.get())) {
            int selectedSlot = player.getInventory().getSelectedSlot();
            ItemStack scanner = new ItemStack(WitcheryItems.WOODEN_SCANNER.get());
            player.getInventory().setItem(player.getInventory().getSelectedSlot(),
                    scanner);
            if (mc.gameMode != null) {
                mc.gameMode.handleCreativeModeItemAdd(scanner, 36 + selectedSlot);
            }
            if (mc.getConnection() != null && commandRetryCooldown <= 0) {
                mc.getConnection().sendCommand("item replace entity @s weapon.mainhand with witchery:wooden_scanner");
                commandRetryCooldown = 40;
            }
            if (debugTicks++ % 40 == 0) {
                WitcheryMod.LOGGER.info("Wooden scanner pose harness installing scanner into hotbar slot {}", selectedSlot);
            }
        }

        if (INVENTORY_CAPTURE) {
            if (player.isUsingItem()) {
                player.stopUsingItem();
            }
            if (player.getMainHandItem().is(WitcheryItems.WOODEN_SCANNER.get())) {
                openInventoryPreview(mc, player);
                inventoryScreenTicks = 0;
                WitcheryMod.LOGGER.info("Wooden scanner pose harness opened inventory preview");
            }
            tickExitCountdown(mc);
            return;
        }

        if (player.getMainHandItem().is(WitcheryItems.WOODEN_SCANNER.get())
                && (!player.isUsingItem() || !player.getUseItem().is(WitcheryItems.WOODEN_SCANNER.get()))
                && mc.gameMode != null) {
            mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            if (!player.isUsingItem()) {
                player.startUsingItem(InteractionHand.MAIN_HAND);
            }
            if (debugTicks++ % 40 == 0) {
                WitcheryMod.LOGGER.info("Wooden scanner pose harness started scanner use (using={}, useTicks={})",
                        player.isUsingItem(), player.getTicksUsingItem());
            }
        } else if (debugTicks++ % 100 == 0) {
            WitcheryMod.LOGGER.info("Wooden scanner pose harness status (heldScanner={}, using={}, useTicks={})",
                    player.getMainHandItem().is(WitcheryItems.WOODEN_SCANNER.get()),
                    player.isUsingItem(), player.getTicksUsingItem());
        }
        if (player.getMainHandItem().is(WitcheryItems.WOODEN_SCANNER.get())) {
            harnessUseTicks++;
        } else {
            harnessUseTicks = 0;
        }

        tickExitCountdown(mc);
    }

    private static void tickExitCountdown(Minecraft mc) {
        if (exitCountdown > 0) {
            exitCountdown--;
        } else if (exitCountdown == 0) {
            exitCountdown = -1;
            mc.stop();
        }
    }

    public static void afterRenderFrame() {
        if (ENABLED && CAPTURE && INVENTORY_CAPTURE) {
            captureInventoryFrame();
            return;
        }
        if (!ENABLED || !CAPTURE || nextCaptureIndex >= CAPTURE_USE_TICKS.length) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.player instanceof LocalPlayer player) || mc.level == null || mc.screen != null) {
            return;
        }
        if (!player.isUsingItem() || !player.getUseItem().is(WitcheryItems.WOODEN_SCANNER.get())) {
            return;
        }

        int useTicks = Math.max(player.getTicksUsingItem(), harnessUseTicks);
        int targetTicks = CAPTURE_USE_TICKS[nextCaptureIndex];
        if (useTicks < targetTicks) {
            return;
        }

        mc.gui.getChat().clearMessages(false);
        String fileName = "scanner-pose-" + RUN_ID + "-" + String.format("%02d", targetTicks) + ".png";
        Screenshot.grab(mc.gameDirectory, fileName, mc.getMainRenderTarget(), 1,
                WoodenScannerPoseHarness::logScreenshotMessage);
        WitcheryMod.LOGGER.info("Wooden scanner pose harness captured {}", fileName);
        nextCaptureIndex++;
        if (nextCaptureIndex >= CAPTURE_USE_TICKS.length && EXIT_AFTER_CAPTURE) {
            exitCountdown = 40;
        }
    }

    public static void afterScreenRender(Screen screen) {
        if (!ENABLED || !CAPTURE || !INVENTORY_CAPTURE || !(screen instanceof InventoryScreen)) {
            return;
        }
        if (inventoryScreenTicks >= 0) {
            inventoryScreenTicks++;
        }
        captureInventoryFrame();
    }

    private static void logScreenshotMessage(Component message) {
        WitcheryMod.LOGGER.info("Wooden scanner pose harness: {}", message.getString());
    }

    private static void openInventoryPreview(Minecraft mc, LocalPlayer player) {
        CreativeModeInventoryScreen screen = new CreativeModeInventoryScreen(player, player.connection.enabledFeatures(), false);
        mc.setScreen(screen);
        BuiltInRegistries.CREATIVE_MODE_TAB.getOptional(CreativeModeTabs.INVENTORY)
                .ifPresent(tab -> selectCreativeTab(screen, tab));
    }

    private static void selectCreativeTab(CreativeModeInventoryScreen screen, CreativeModeTab tab) {
        try {
            Method selectTab = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
            selectTab.setAccessible(true);
            selectTab.invoke(screen, tab);
            WitcheryMod.LOGGER.info("Wooden scanner pose harness selected creative inventory tab");
        } catch (ReflectiveOperationException exception) {
            WitcheryMod.LOGGER.warn("Wooden scanner pose harness could not select creative inventory tab", exception);
        }
    }

    private static void captureInventoryFrame() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof InventoryScreen)) {
            return;
        }
        if (inventoryCaptured || inventoryScreenTicks < 20) {
            return;
        }

        String fileName = "scanner-inventory-" + RUN_ID + ".png";
        Screenshot.grab(mc.gameDirectory, fileName, mc.getMainRenderTarget(), 1,
                WoodenScannerPoseHarness::logScreenshotMessage);
        WitcheryMod.LOGGER.info("Wooden scanner pose harness captured {}", fileName);
        inventoryCaptured = true;
        if (EXIT_AFTER_CAPTURE) {
            exitCountdown = 20;
        }
    }

    private static void sendPoseStudioCommands(Minecraft mc) {
        if (mc.getConnection() == null) {
            return;
        }
        sendCommand(mc, "time set noon");
        sendCommand(mc, "weather clear");
        sendCommand(mc, "difficulty peaceful");
        sendCommand(mc, "fill 2000 120 2000 2032 120 2032 minecraft:grass_block");
        sendCommand(mc, "fill 2000 121 2000 2032 132 2032 minecraft:air");
        sendCommand(mc, "tp @s 2016 121 2016 180 0");
    }

    private static void sendCommand(Minecraft mc, String command) {
        mc.getConnection().sendCommand(command);
    }

    public static boolean isRenderingScanner(Player player) {
        return ENABLED && player instanceof LocalPlayer
                && player.getMainHandItem().is(WitcheryItems.WOODEN_SCANNER.get());
    }

    public static float useTicksWithPartial(Player player, float partialTick) {
        if (!isRenderingScanner(player)) {
            return player.getTicksUsingItem() + partialTick;
        }
        return Math.max(player.getTicksUsingItem(), harnessUseTicks) + partialTick;
    }
}
