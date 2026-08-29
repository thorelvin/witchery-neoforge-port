package com.emoniph.witchery.client;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryDimensions;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;

/** Property-gated live-client harness for matched Spirit World atmosphere captures. */
public final class SpiritWorldCaptureHarness {
    private static final boolean ENABLED = Boolean.getBoolean("witchery.spiritWorldCaptureHarness");
    private static final boolean CAPTURE = Boolean.getBoolean("witchery.spiritWorldCaptureHarness.capture");
    private static final boolean EXIT_AFTER_CAPTURE =
            Boolean.getBoolean("witchery.spiritWorldCaptureHarness.exitAfterCapture");
    private static final String RUN_ID = System.getProperty("witchery.spiritWorldCaptureHarness.runId",
            Long.toString(System.currentTimeMillis()));
    private static final CaptureCase[] CASES = CaptureCase.values();
    private static final double CAMERA_X = 4096.5D;
    private static final double CAMERA_Y = 121.0D;
    private static final double CAMERA_Z = 4086.5D;
    private static final int DESTINATION_SETTLE_TICKS = 80;
    private static final int STUDIO_SETTLE_TICKS = 240;
    private static final int CASE_SETTLE_TICKS = 160;

    private static boolean entrySent;
    private static boolean destinationReached;
    private static boolean studioSent;
    private static boolean caseSent;
    private static boolean readyToCapture;
    private static boolean caseCaptured;
    private static boolean loggedWaiting;
    private static int destinationTicks;
    private static int studioTicks;
    private static int caseTicks;
    private static int transitionTicks;
    private static int caseIndex;
    private static int exitCountdown = -1;

    private SpiritWorldCaptureHarness() {
    }

    public static void clientTick() {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.player instanceof LocalPlayer player) || mc.level == null || mc.getConnection() == null) {
            if (!loggedWaiting) {
                loggedWaiting = true;
                WitcheryMod.LOGGER.info("Spirit World capture harness waiting for a client world");
            }
            return;
        }
        if (mc.screen != null) {
            tickExitCountdown(mc);
            return;
        }

        configureView(mc, player);
        player.clearFire();
        hideOtherEntities(mc);
        if (caseIndex >= CASES.length) {
            if (EXIT_AFTER_CAPTURE && exitCountdown < 0) {
                sendCommand(mc, "witchery exit");
                exitCountdown = 30;
            }
            tickExitCountdown(mc);
            return;
        }
        if (!entrySent) {
            entrySent = true;
            sendCommand(mc, "gamemode creative @s");
            sendCommand(mc, "difficulty peaceful");
            sendCommand(mc, "time set noon");
            sendCommand(mc, "weather clear");
            sendCommand(mc, "witchery dream");
            WitcheryMod.LOGGER.info("Spirit World capture harness requested peaceful dream entry");
            return;
        }
        if (!mc.level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) {
            tickExitCountdown(mc);
            return;
        }
        if (!destinationReached) {
            destinationReached = true;
            sendCommand(mc, "tp @s " + CAMERA_X + " " + CAMERA_Y + " " + CAMERA_Z + " 0 -4");
            WitcheryMod.LOGGER.info("Spirit World capture harness requested comparison destination");
            return;
        }
        if (!close(player.getX(), CAMERA_X) || !close(player.getZ(), CAMERA_Z)) {
            destinationTicks = 0;
            tickExitCountdown(mc);
            return;
        }
        if (!studioSent) {
            if (destinationTicks++ < DESTINATION_SETTLE_TICKS) {
                tickExitCountdown(mc);
                return;
            }
            studioSent = true;
            sendStudioCommands(mc);
            WitcheryMod.LOGGER.info("Spirit World capture harness built comparison studio");
            return;
        }

        pinCamera(player);
        if (studioTicks++ < STUDIO_SETTLE_TICKS) {
            tickExitCountdown(mc);
            return;
        }
        if (caseCaptured) {
            if (transitionTicks++ >= 10) {
                caseIndex++;
                caseSent = false;
                caseCaptured = false;
                transitionTicks = 0;
                caseTicks = 0;
            }
            tickExitCountdown(mc);
            return;
        }
        if (!caseSent) {
            caseSent = true;
            sendCaseCommands(mc, currentCase());
            WitcheryMod.LOGGER.info("Spirit World capture harness preparing {}", currentCase().id);
            return;
        }
        if (caseTicks++ >= CASE_SETTLE_TICKS && caseStateReady(mc, player, currentCase())) {
            var probe = mc.gameRenderer.getMainCamera().attributeProbe();
            float skyLightFactor = probe.getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, 1.0F);
            int skyColor = probe.getValue(EnvironmentAttributes.SKY_COLOR, 1.0F);
            WitcheryMod.LOGGER.info("Spirit World capture harness ready {} dayTime={} timelines={} "
                            + "skyLightFactor={} skyColor=#{}",
                    currentCase().id, mc.level.getDayTime(), mc.level.dimensionType().timelines().size(),
                    skyLightFactor, String.format("%06x", skyColor & 0xFFFFFF));
            sendCommand(mc, "time query daytime");
            configureView(mc, player);
            readyToCapture = true;
        }
        tickExitCountdown(mc);
    }

    public static void beforeRenderFrame() {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.clearFire();
        }
        if (mc.level != null) {
            hideOtherEntities(mc);
        }
    }

    public static void afterRenderFrame() {
        if (!ENABLED || !CAPTURE || !readyToCapture || caseCaptured || caseIndex >= CASES.length) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.player instanceof LocalPlayer) || mc.level == null || mc.screen != null) {
            return;
        }

        readyToCapture = false;
        caseCaptured = true;
        mc.gui.getChat().clearMessages(false);
        String fileName = "spirit-world-" + RUN_ID + "-" + currentCase().id + ".png";
        Screenshot.grab(mc.gameDirectory, fileName, mc.getMainRenderTarget(), 1,
                SpiritWorldCaptureHarness::logScreenshotMessage);
        WitcheryMod.LOGGER.info("Spirit World capture harness captured {}", fileName);
    }

    private static void sendStudioCommands(Minecraft mc) {
        sendCommand(mc, "fill 4064 120 4064 4128 120 4128 minecraft:grass_block");
        sendCommand(mc, "fill 4091 120 4104 4101 120 4110 minecraft:water");
        sendCommand(mc, "fill 4088 121 4112 4088 128 4112 minecraft:stone");
        sendCommand(mc, "fill 4104 121 4112 4104 124 4112 minecraft:stone");
        sendCommand(mc, "tp @s " + CAMERA_X + " " + CAMERA_Y + " " + CAMERA_Z + " 0 -4");
    }

    private static void sendCaseCommands(Minecraft mc, CaptureCase captureCase) {
        sendCommand(mc, "time set noon");
        switch (captureCase) {
            case DREAM_CLEAR -> sendCommand(mc, "weather clear");
            case DREAM_RAIN -> sendCommand(mc, "weather rain");
            case NIGHTMARE_CLEAR -> {
                sendCommand(mc, "weather clear");
                sendCommand(mc, "witchery nightmare true");
            }
            case DEMONIC_CLEAR -> {
                sendCommand(mc, "weather clear");
                sendCommand(mc, "witchery nightmare demonic");
            }
        }
    }

    private static boolean caseStateReady(Minecraft mc, LocalPlayer player, CaptureCase captureCase) {
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        boolean dream = data.isInSpiritWorld() && data.isSpiritWorldDream() && !data.isDemonicNightmare();
        boolean nightmare = data.isInSpiritWorld() && !data.isSpiritWorldDream() && !data.isDemonicNightmare();
        boolean demonic = data.isInSpiritWorld() && data.isDemonicNightmare();
        float rain = mc.level.getRainLevel(1.0F);
        return switch (captureCase) {
            case DREAM_CLEAR -> dream && rain <= 0.01F;
            case DREAM_RAIN -> dream && rain >= 0.99F;
            case NIGHTMARE_CLEAR -> nightmare && rain <= 0.01F;
            case DEMONIC_CLEAR -> demonic && rain <= 0.01F;
        };
    }

    private static void configureView(Minecraft mc, LocalPlayer player) {
        mc.options.hideGui = true;
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.options.fov().set(70);
        mc.options.gamma().set(1.0D);
        player.setYRot(0.0F);
        player.setXRot(-4.0F);
        player.yRotO = 0.0F;
        player.xRotO = -4.0F;
    }

    private static void pinCamera(LocalPlayer player) {
        player.setPos(CAMERA_X, CAMERA_Y, CAMERA_Z);
        player.xo = CAMERA_X;
        player.yo = CAMERA_Y;
        player.zo = CAMERA_Z;
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
    }

    private static void hideOtherEntities(Minecraft mc) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity != mc.player) {
                entity.setInvisible(true);
            }
        }
        mc.particleEngine.clearParticles();
    }

    private static void sendCommand(Minecraft mc, String command) {
        mc.getConnection().sendCommand(command);
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) <= 0.01D;
    }

    private static CaptureCase currentCase() {
        return CASES[caseIndex];
    }

    private static void logScreenshotMessage(Component message) {
        WitcheryMod.LOGGER.info("Spirit World capture harness: {}", message.getString());
    }

    private static void tickExitCountdown(Minecraft mc) {
        if (exitCountdown > 0) {
            exitCountdown--;
        } else if (exitCountdown == 0) {
            exitCountdown = -1;
            mc.stop();
        }
    }

    private enum CaptureCase {
        DREAM_CLEAR("dream-clear"),
        DREAM_RAIN("dream-rain"),
        NIGHTMARE_CLEAR("nightmare-clear"),
        DEMONIC_CLEAR("demonic-clear");

        private final String id;

        CaptureCase(String id) {
            this.id = id;
        }
    }
}
