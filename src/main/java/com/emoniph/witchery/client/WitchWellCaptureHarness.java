package com.emoniph.witchery.client;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;

import java.util.Set;
import java.util.UUID;

/** Property-gated client harness for fixed-seed Witch Well certification captures. */
public final class WitchWellCaptureHarness {
    private static final boolean ENABLED = Boolean.getBoolean("witchery.witchWellCaptureHarness");
    private static final boolean CAPTURE = Boolean.getBoolean("witchery.witchWellCaptureHarness.capture");
    private static final boolean EXIT_AFTER_CAPTURE =
            Boolean.getBoolean("witchery.witchWellCaptureHarness.exitAfterCapture");
    private static final String RUN_ID = System.getProperty("witchery.witchWellCaptureHarness.runId",
            Long.toString(System.currentTimeMillis()));
    private static final String WORLD_NAME =
            System.getProperty("witchery.witchWellCaptureHarness.worldName", "");
    private static final CaptureCase[] CASES = CaptureCase.values();
    private static final int WORLD_OPEN_DELAY_TICKS = 40;
    private static final int SETUP_TICKS = 40;
    private static final int CASE_SETTLE_TICKS = 120;
    private static final InputWithModifiers CONFIRM_INPUT = new InputWithModifiers() {
        @Override
        public int input() {
            return 257;
        }

        @Override
        public int modifiers() {
            return 0;
        }
    };

    private static boolean worldOpenRequested;
    private static boolean compatibilityConfirmed;
    private static int worldOpenTicks;
    private static boolean setupSent;
    private static volatile boolean setupComplete;
    private static boolean caseSent;
    private static boolean readyToCapture;
    private static boolean caseCaptured;
    private static boolean loggedWaiting;
    private static int setupTicks;
    private static int caseTicks;
    private static int transitionTicks;
    private static int caseIndex;
    private static int exitCountdown = -1;

    private WitchWellCaptureHarness() {}

    public static void clientTick() {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.player instanceof LocalPlayer player) || mc.level == null || mc.getConnection() == null) {
            if (confirmCompatibilityScreen(mc)) {
                return;
            }
            maybeOpenWorld(mc);
            if (!loggedWaiting) {
                loggedWaiting = true;
                WitcheryMod.LOGGER.info("Witch Well capture harness waiting for a client world");
            }
            return;
        }
        if (mc.screen != null) {
            tickExitCountdown(mc);
            return;
        }

        configureView(mc, player);
        if (caseIndex >= CASES.length) {
            if (EXIT_AFTER_CAPTURE && exitCountdown < 0) {
                exitCountdown = 30;
            }
            tickExitCountdown(mc);
            return;
        }
        if (!setupSent) {
            MinecraftServer server = mc.getSingleplayerServer();
            if (server == null) {
                return;
            }
            setupSent = true;
            submitSetup(server, player.getUUID());
            WitcheryMod.LOGGER.info("Witch Well capture harness prepared the fixed-seed world");
            return;
        }
        if (!setupComplete) {
            tickExitCountdown(mc);
            return;
        }
        if (setupTicks++ < SETUP_TICKS) {
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

        CaptureCase captureCase = currentCase();
        if (!caseSent) {
            caseSent = true;
            submitTeleport(mc.getSingleplayerServer(), player.getUUID(), captureCase);
            WitcheryMod.LOGGER.info("Witch Well capture harness preparing {}", captureCase.id);
            return;
        }
        if (!atCamera(player, captureCase)
                || !mc.level.hasChunkAt(BlockPos.containing(captureCase.x, captureCase.y, captureCase.z))) {
            caseTicks = 0;
            tickExitCountdown(mc);
            return;
        }
        pinCamera(player, captureCase);
        if (caseTicks++ >= CASE_SETTLE_TICKS) {
            readyToCapture = true;
        }
        tickExitCountdown(mc);
    }

    private static boolean confirmCompatibilityScreen(Minecraft mc) {
        if (compatibilityConfirmed || !(mc.screen instanceof BackupConfirmScreen screen)) {
            return false;
        }
        Component skipBackup = Component.translatable("selectWorld.backupJoinSkipButton");
        for (var child : screen.children()) {
            if (child instanceof Button button && button.getMessage().equals(skipBackup)) {
                compatibilityConfirmed = true;
                WitcheryMod.LOGGER.info("Witch Well capture harness accepted the disposable world's compatibility warning");
                button.onPress(CONFIRM_INPUT);
                return true;
            }
        }
        return false;
    }

    private static void maybeOpenWorld(Minecraft mc) {
        if (WORLD_NAME.isBlank() || worldOpenRequested || worldOpenTicks++ < WORLD_OPEN_DELAY_TICKS) {
            return;
        }
        worldOpenRequested = true;
        WitcheryMod.LOGGER.info("Witch Well capture harness opening {}", WORLD_NAME);
        mc.createWorldOpenFlows().openWorld(WORLD_NAME, () -> {
            worldOpenRequested = false;
            worldOpenTicks = 0;
            WitcheryMod.LOGGER.error("Witch Well capture harness could not open {}", WORLD_NAME);
        });
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
        String fileName = "witch-well-" + RUN_ID + "-" + currentCase().id + ".png";
        Screenshot.grab(mc.gameDirectory, fileName, mc.getMainRenderTarget(), 1,
                WitchWellCaptureHarness::logScreenshotMessage);
        WitcheryMod.LOGGER.info("Witch Well capture harness captured {}", fileName);
    }

    private static void configureView(Minecraft mc, LocalPlayer player) {
        mc.options.hideGui = true;
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.options.fov().set(70);
        mc.options.gamma().set(1.0D);
        CaptureCase captureCase = currentCase();
        player.setYRot(captureCase.yaw);
        player.setXRot(captureCase.pitch);
        player.yRotO = captureCase.yaw;
        player.xRotO = captureCase.pitch;
    }

    private static void pinCamera(LocalPlayer player, CaptureCase captureCase) {
        player.setPos(captureCase.x, captureCase.y, captureCase.z);
        player.xo = captureCase.x;
        player.yo = captureCase.y;
        player.zo = captureCase.z;
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
    }

    private static boolean atCamera(LocalPlayer player, CaptureCase captureCase) {
        return Math.abs(player.getX() - captureCase.x) <= 0.05D
                && Math.abs(player.getY() - captureCase.y) <= 0.05D
                && Math.abs(player.getZ() - captureCase.z) <= 0.05D;
    }

    private static void submitSetup(MinecraftServer server, UUID playerId) {
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                return;
            }
            server.setDifficulty(Difficulty.PEACEFUL, true);
            player.setGameMode(GameType.SPECTATOR);
            server.overworld().setDayTime(6_000L);
            server.overworld().setWeatherParameters(6_000, 0, false, false);
            setupComplete = true;
        });
    }

    private static void submitTeleport(MinecraftServer server, UUID playerId,
                                       CaptureCase captureCase) {
        if (server == null) {
            return;
        }
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                player.teleportTo(server.overworld(), captureCase.x, captureCase.y, captureCase.z,
                        Set.<Relative>of(), captureCase.yaw, captureCase.pitch, false);
            }
        });
    }

    private static CaptureCase currentCase() {
        return CASES[Math.min(caseIndex, CASES.length - 1)];
    }

    private static void logScreenshotMessage(Component message) {
        WitcheryMod.LOGGER.info("Witch Well capture harness: {}", message.getString());
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
        REGULAR_EXTERIOR("regular-exterior", -3176.5D, 101.0D, 4713.5D, 0.0F, 22.0F),
        REGULAR_CENTER("regular-center", -3176.5D, 97.0D, 4719.5D, 0.0F, 18.0F),
        SMALL_EXTERIOR("small-exterior", -535.5D, 76.0D, 1303.5D, -90.0F, 20.0F),
        SMALL_CENTER("small-center", -528.0D, 72.5D, 1303.5D, -90.0F, 14.0F),
        CAVE_EXTERIOR("cave-exterior", -568.5D, 78.0D, -2525.5D, 0.0F, 24.0F),
        CAVE_SHAFT("cave-shaft", -568.5D, 59.75D, -2502.75D, 180.0F, -25.0F),
        CAVE_LOWER("cave-lower", -567.5D, 60.0D, -2503.5D, -75.0F, 3.0F);

        private final String id;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;

        CaptureCase(String id, double x, double y, double z, float yaw, float pitch) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
