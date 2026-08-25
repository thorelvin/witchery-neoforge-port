package com.emoniph.witchery.client;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.entity.DistilleryBlockEntity;
import com.emoniph.witchery.blocks.entity.SpinningWheelBlockEntity;
import com.emoniph.witchery.blocks.entity.WitchesOvenBlockEntity;
import com.emoniph.witchery.client.screen.DistilleryScreen;
import com.emoniph.witchery.client.screen.SpinningWheelScreen;
import com.emoniph.witchery.client.screen.WitchesOvenScreen;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.menu.DistilleryMenu;
import com.emoniph.witchery.menu.SpinningWheelMenu;
import com.emoniph.witchery.menu.WitchesOvenMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Property-gated visual regression harness for source-style machine screens. */
public final class MachinePresentationHarness {
    private static final boolean ENABLED = Boolean.getBoolean("witchery.machinePresentationHarness");
    private static final boolean CAPTURE = Boolean.getBoolean("witchery.machinePresentationHarness.capture");
    private static final boolean EXIT_AFTER_CAPTURE =
            Boolean.getBoolean("witchery.machinePresentationHarness.exitAfterCapture");
    private static final String RUN_ID = System.getProperty("witchery.machinePresentationHarness.runId",
            Long.toString(System.currentTimeMillis()));
    private static final MachineScreen[] SCREENS = MachineScreen.values();
    private static final int SETTLE_TICKS = 20;

    private static int screenIndex;
    private static int screenTicks;
    private static int transitionTicks;
    private static int exitCountdown = -1;
    private static boolean captureComplete;
    private static boolean loggedEnabled;

    private MachinePresentationHarness() {
    }

    public static void clientTick() {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!loggedEnabled) {
            loggedEnabled = true;
            WitcheryMod.LOGGER.info("Machine presentation harness enabled (capture={}, runId={})", CAPTURE, RUN_ID);
        }
        if (!(mc.player instanceof LocalPlayer player) || mc.level == null) {
            return;
        }
        if (screenIndex >= SCREENS.length) {
            if (EXIT_AFTER_CAPTURE && exitCountdown < 0) {
                exitCountdown = 20;
            }
            tickExitCountdown(mc);
            return;
        }
        if (captureComplete) {
            if (++transitionTicks >= 5) {
                mc.setScreen(null);
                screenIndex++;
                screenTicks = 0;
                transitionTicks = 0;
                captureComplete = false;
            }
            tickExitCountdown(mc);
            return;
        }
        if (mc.screen == null) {
            Screen screen = SCREENS[screenIndex].create(player.getInventory());
            mc.setScreen(screen);
            WitcheryMod.LOGGER.info("Machine presentation harness opened {}", SCREENS[screenIndex].id);
            return;
        }
        if (SCREENS[screenIndex].matches(mc.screen)) {
            screenTicks++;
        }
        tickExitCountdown(mc);
    }

    public static void afterRenderFrame() {
        if (!ENABLED || !CAPTURE || captureComplete || screenIndex >= SCREENS.length
                || screenTicks < SETTLE_TICKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        MachineScreen current = SCREENS[screenIndex];
        if (!current.matches(mc.screen)) {
            return;
        }

        mc.gui.getChat().clearMessages(false);
        String fileName = "machine-gui-" + RUN_ID + "-" + current.id + ".png";
        Screenshot.grab(mc.gameDirectory, fileName, mc.getMainRenderTarget(), 1,
                message -> WitcheryMod.LOGGER.info("Machine presentation harness: {}", message.getString()));
        WitcheryMod.LOGGER.info("Machine presentation harness captured {}", fileName);
        captureComplete = true;
    }

    private static void tickExitCountdown(Minecraft mc) {
        if (exitCountdown > 0) {
            exitCountdown--;
        } else if (exitCountdown == 0) {
            exitCountdown = -1;
            mc.stop();
        }
    }

    private enum MachineScreen {
        OVEN("witches-oven", WitchesOvenScreen.class) {
            @Override
            Screen create(Inventory inventory) {
                SimpleContainer container = new SimpleContainer(WitchesOvenBlockEntity.SLOT_COUNT);
                container.setItem(WitchesOvenBlockEntity.SLOT_INPUT, new ItemStack(Items.OAK_LOG));
                container.setItem(WitchesOvenBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
                container.setItem(WitchesOvenBlockEntity.SLOT_COOKED,
                        new ItemStack(WitcheryItems.WOOD_ASH.get()));
                container.setItem(WitchesOvenBlockEntity.SLOT_BYPRODUCT,
                        new ItemStack(WitcheryItems.EXHALE_OF_HORNED_ONE.get()));
                container.setItem(WitchesOvenBlockEntity.SLOT_JARS,
                        new ItemStack(WitcheryItems.CLAY_JAR.get(), 8));
                SimpleContainerData data = data(100, 200, 100, 200);
                return new WitchesOvenScreen(new WitchesOvenMenu(0, inventory, container, data), inventory,
                        Component.translatable("container.witchery.witches_oven"));
            }
        },
        DISTILLERY("distillery", DistilleryScreen.class) {
            @Override
            Screen create(Inventory inventory) {
                SimpleContainer container = new SimpleContainer(DistilleryBlockEntity.SLOT_COUNT);
                container.setItem(DistilleryBlockEntity.SLOT_INPUT_1,
                        new ItemStack(WitcheryItems.EXHALE_OF_HORNED_ONE.get()));
                container.setItem(DistilleryBlockEntity.SLOT_INPUT_2,
                        new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()));
                container.setItem(DistilleryBlockEntity.SLOT_JARS,
                        new ItemStack(WitcheryItems.CLAY_JAR.get(), 4));
                container.setItem(DistilleryBlockEntity.SLOT_OUTPUT_1,
                        new ItemStack(WitcheryItems.ODOUR_OF_PURITY.get()));
                container.setItem(DistilleryBlockEntity.SLOT_OUTPUT_2,
                        new ItemStack(WitcheryItems.WHIFF_OF_MAGIC.get()));
                container.setItem(DistilleryBlockEntity.SLOT_OUTPUT_3,
                        new ItemStack(WitcheryItems.REFINED_EVIL.get()));
                container.setItem(DistilleryBlockEntity.SLOT_OUTPUT_4,
                        new ItemStack(WitcheryItems.TEAR_OF_GODDESS.get()));
                SimpleContainerData data = data(100, 200, 400, 800, 500, 500, 1000);
                return new DistilleryScreen(new DistilleryMenu(0, inventory, container, data), inventory,
                        Component.translatable("container.witchery.distillery"));
            }
        },
        SPINNING_WHEEL("spinning-wheel", SpinningWheelScreen.class) {
            @Override
            Screen create(Inventory inventory) {
                SimpleContainer container = new SimpleContainer(SpinningWheelBlockEntity.SLOT_COUNT);
                container.setItem(SpinningWheelBlockEntity.SLOT_FIBRE, new ItemStack(Items.STRING));
                container.setItem(SpinningWheelBlockEntity.SLOT_MODIFIER_1,
                        new ItemStack(WitcheryItems.ODOUR_OF_PURITY.get()));
                container.setItem(SpinningWheelBlockEntity.SLOT_OUTPUT, new ItemStack(Items.WHITE_WOOL));
                container.setItem(SpinningWheelBlockEntity.SLOT_MODIFIER_2, new ItemStack(Items.DIAMOND));
                container.setItem(SpinningWheelBlockEntity.SLOT_MODIFIER_3,
                        new ItemStack(WitcheryItems.WHIFF_OF_MAGIC.get()));
                SimpleContainerData data = data(50, 100, 1);
                return new SpinningWheelScreen(new SpinningWheelMenu(0, inventory, container, data), inventory,
                        Component.translatable("container.witchery.spinning_wheel"));
            }
        },
        ITEMS("items", MachineItemScreen.class) {
            @Override
            Screen create(Inventory inventory) {
                return new MachineItemScreen();
            }
        };

        private final String id;
        private final Class<? extends Screen> screenType;

        MachineScreen(String id, Class<? extends Screen> screenType) {
            this.id = id;
            this.screenType = screenType;
        }

        abstract Screen create(Inventory inventory);

        boolean matches(Screen screen) {
            return screenType.isInstance(screen);
        }
    }

    private static SimpleContainerData data(int... values) {
        SimpleContainerData data = new SimpleContainerData(values.length);
        for (int i = 0; i < values.length; i++) {
            data.set(i, values[i]);
        }
        return data;
    }

    private static final class MachineItemScreen extends Screen {
        private static final List<ItemStack> ITEMS = List.of(
                new ItemStack(WitcheryItems.KETTLE.get()),
                new ItemStack(WitcheryItems.DISTILLERY.get()),
                new ItemStack(WitcheryItems.SPINNING_WHEEL.get()),
                new ItemStack(WitcheryItems.WITCHES_OVEN.get()),
                new ItemStack(WitcheryItems.FUME_FUNNEL.get()),
                new ItemStack(WitcheryItems.FILTERED_FUME_FUNNEL.get()),
                new ItemStack(WitcheryItems.SILVER_VAT.get()),
                new ItemStack(WitcheryItems.BRAZIER.get()));

        private MachineItemScreen() {
            super(Component.literal("Machine Items"));
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xB0101010);
            int panelWidth = 224;
            int panelHeight = 64;
            int left = (width - panelWidth) / 2;
            int top = (height - panelHeight) / 2;
            graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xFFE6E6E6);
            graphics.fill(left + 2, top + 2, left + panelWidth - 2, top + panelHeight - 2, 0xFFB8B8B8);
            graphics.drawCenteredString(font, title, width / 2, top + 8, 0xFF303030);
            for (int i = 0; i < ITEMS.size(); i++) {
                int slotX = left + 8 + i * 26;
                int slotY = top + 29;
                graphics.fill(slotX, slotY, slotX + 22, slotY + 22, 0xFF666666);
                graphics.fill(slotX + 1, slotY + 1, slotX + 21, slotY + 21, 0xFF9A9A9A);
                graphics.renderItem(ITEMS.get(i), slotX + 3, slotY + 3);
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
