package com.emoniph.witchery.client;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.entity.DarkMarkEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Property-gated client harness for controlled original-versus-port model captures.
 */
public final class LegacyModelParityHarness {
    private static final boolean ENABLED = Boolean.getBoolean("witchery.modelParityHarness");
    private static final boolean CAPTURE = Boolean.getBoolean("witchery.modelParityHarness.capture");
    private static final boolean EXIT_AFTER_CAPTURE =
            Boolean.getBoolean("witchery.modelParityHarness.exitAfterCapture");
    private static final String RUN_ID = System.getProperty("witchery.modelParityHarness.runId",
            Long.toString(System.currentTimeMillis()));
    private static final String BLOCK_SPEC =
            System.getProperty("witchery.modelParityHarness.block", "").trim();
    private static final boolean BLOCK_CAPTURE = !BLOCK_SPEC.isEmpty();
    private static final List<CaptureCase> CAPTURE_CASES = parseCaptureCases();
    private static final int SETTLE_TICKS = 40;
    private static final double CAMERA_X = 2016.5D;
    private static final double CAMERA_Y = 121.0D;
    private static final double CAMERA_Z = 2007.5D;
    private static final double TARGET_X = 2016.5D;
    private static final double TARGET_Y = 121.0D;
    private static final double TARGET_Z = 2016.5D;
    private static final int TARGET_BLOCK_X = 2016;
    private static final int TARGET_BLOCK_Y = 122;
    private static final int TARGET_BLOCK_Z = 2016;
    private static boolean setupSent;
    private static boolean studioSent;
    private static boolean caseSent;
    private static boolean readyToCapture;
    private static boolean caseCaptured;
    private static boolean loggedWaiting;
    private static int readinessLogTicks;
    private static int setupTicks;
    private static int destinationLoadTicks;
    private static int caseTransitionTicks;
    private static int captureCaseIndex;
    private static int exitCountdown = -1;
    private static Entity harnessOnlyTarget;

    private LegacyModelParityHarness() {
    }

    public static void clientTick() {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.player instanceof LocalPlayer player) || mc.level == null || mc.getConnection() == null) {
            if (!loggedWaiting) {
                loggedWaiting = true;
                WitcheryMod.LOGGER.info("Legacy model parity harness waiting for a client world");
            }
            return;
        }
        if (mc.screen != null) {
            tickExitCountdown(mc);
            return;
        }

        configureView(mc, player);
        if (captureCaseIndex >= CAPTURE_CASES.size()) {
            if (EXIT_AFTER_CAPTURE && exitCountdown < 0) {
                exitCountdown = 20;
            }
            tickExitCountdown(mc);
            return;
        }
        if (!setupSent) {
            setupSent = true;
            sendInitialCommands(mc);
            WitcheryMod.LOGGER.info("Legacy model parity harness preparing {} capture cases",
                    CAPTURE_CASES.size());
            return;
        }
        if (!studioSent) {
            if (!close(player.getX(), CAMERA_X) || !close(player.getZ(), CAMERA_Z)) {
                destinationLoadTicks = 0;
                tickExitCountdown(mc);
                return;
            }
            if (destinationLoadTicks++ < 60) {
                tickExitCountdown(mc);
                return;
            }
            studioSent = true;
            sendStudioCommands(mc);
            WitcheryMod.LOGGER.info("Legacy model parity harness destination loaded; building studio");
            return;
        }
        pinPosition(player, CAMERA_X, CAMERA_Y, CAMERA_Z);
        if (caseCaptured) {
            if (caseTransitionTicks++ >= 10) {
                captureCaseIndex++;
                caseSent = false;
                caseCaptured = false;
                caseTransitionTicks = 0;
                setupTicks = 0;
                readinessLogTicks = 0;
            }
            tickExitCountdown(mc);
            return;
        }
        if (!caseSent) {
            caseSent = true;
            sendCaseCommands(mc);
            WitcheryMod.LOGGER.info("Legacy model parity harness summoning {}",
                    currentCase());
            return;
        }
        Entity target = findTargetEntity(mc);
        mc.particleEngine.clearParticles();
        hideNonTargetEntities(mc, target);
        configureTargetPose(target);
        if (!isStudioReady(player, target)) {
            setupTicks = 0;
            if (readinessLogTicks++ % 40 == 0) {
                WitcheryMod.LOGGER.info("Legacy model parity harness waiting for studio "
                                + "(player=({}, {}, {}), target={})",
                        player.getX(), player.getY(), player.getZ(), target != null);
            }
            tickExitCountdown(mc);
            return;
        }
        if (setupTicks++ >= SETTLE_TICKS) {
            configureView(mc, player);
            logScene(player, target);
            readyToCapture = true;
        }
        tickExitCountdown(mc);
    }

    public static void afterRenderFrame() {
        if (!ENABLED || !CAPTURE || !readyToCapture || caseCaptured
                || captureCaseIndex >= CAPTURE_CASES.size()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.player instanceof LocalPlayer) || mc.level == null || mc.screen != null) {
            return;
        }

        readyToCapture = false;
        caseCaptured = true;
        mc.gui.getChat().clearMessages(false);
        CaptureCase captureCase = currentCase();
        String fileName = BLOCK_CAPTURE
                ? "model-parity-" + RUN_ID + "-block-" + blockPath() + ".png"
                : "model-parity-" + RUN_ID + "-" + captureCase.entityId().getPath()
                        + "-yaw" + yawLabel(captureCase.yaw()) + ".png";
        Screenshot.grab(mc.gameDirectory, fileName, mc.getMainRenderTarget(), 1,
                LegacyModelParityHarness::logScreenshotMessage);
        WitcheryMod.LOGGER.info("Legacy model parity harness captured {}", fileName);
    }

    private static void configureView(Minecraft mc, LocalPlayer player) {
        mc.options.hideGui = true;
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.options.fov().set(70);
        mc.options.gamma().set(1.0D);
        player.setYRot(0.0F);
        player.setXRot(BLOCK_CAPTURE ? -2.0F : 0.0F);
        player.yRotO = 0.0F;
        player.xRotO = player.getXRot();
    }

    private static void configureTargetPose(Entity target) {
        if (BLOCK_CAPTURE) {
            return;
        }
        if (target == null) {
            return;
        }
        pinPosition(target, TARGET_X, TARGET_Y, TARGET_Z);
        target.setInvisible(false);
        float targetYaw = currentCase().yaw();
        target.tickCount = 0;
        target.setYRot(targetYaw);
        target.setXRot(0.0F);
        target.yRotO = targetYaw;
        target.xRotO = 0.0F;
        if (target instanceof LivingEntity living) {
            if (requiresUnequippedCapture(currentCase().entityId())) {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    living.setItemSlot(slot, ItemStack.EMPTY);
                }
            }
            living.yBodyRot = targetYaw;
            living.yBodyRotO = targetYaw;
            living.yHeadRot = targetYaw;
            living.yHeadRotO = targetYaw;
        }
    }

    private static void hideNonTargetEntities(Minecraft mc, Entity target) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity != null && entity != mc.player && entity != target) {
                if (entity.getType() == EntityType.EXPERIENCE_ORB
                        || entity.getType() == EntityType.ITEM) {
                    pinPosition(entity, TARGET_X, -1024.0D, TARGET_Z);
                    continue;
                }
                entity.setInvisible(true);
            }
        }
    }

    private static void pinPosition(Entity entity, double x, double y, double z) {
        entity.setPos(x, y, z);
        entity.xo = x;
        entity.yo = y;
        entity.zo = z;
        entity.setDeltaMovement(0.0D, 0.0D, 0.0D);
    }

    private static void sendInitialCommands(Minecraft mc) {
        sendCommand(mc, "gamemode creative @s");
        sendCommand(mc, "time set noon");
        sendCommand(mc, "weather clear");
        sendCommand(mc, "tp @s " + CAMERA_X + " " + CAMERA_Y + " " + CAMERA_Z + " 0 0");
    }

    private static void sendStudioCommands(Minecraft mc) {
        sendCommand(mc, "fill 2014 120 2005 2018 120 2019 minecraft:grass_block");
        sendCommand(mc, "fill 2014 121 2005 2018 132 2019 minecraft:air");
        sendCommand(mc, "tp @s " + CAMERA_X + " " + CAMERA_Y + " " + CAMERA_Z + " 0 0");
    }

    private static void sendCaseCommands(Minecraft mc) {
        if (harnessOnlyTarget != null) {
            harnessOnlyTarget.discard();
            harnessOnlyTarget = null;
        }
        String previousTargets = "@e[x=" + TARGET_X + ",y=" + TARGET_Y + ",z=" + TARGET_Z
                + ",distance=..32,type=!minecraft:player]";
        sendCommand(mc, "tp " + previousTargets + " " + TARGET_X + " -1024 " + TARGET_Z);
        sendCommand(mc, "kill @e[x=" + TARGET_X + ",y=-1024,z=" + TARGET_Z
                + ",distance=..32,type=!minecraft:player]");
        CaptureCase captureCase = currentCase();
        if (BLOCK_CAPTURE) {
            sendCommand(mc, "time set noon");
            sendCommand(mc, "setblock " + TARGET_BLOCK_X + " " + TARGET_BLOCK_Y + " "
                    + TARGET_BLOCK_Z + " " + BLOCK_SPEC);
            if ("mirror".equals(blockPath()) && BLOCK_SPEC.contains("part=foot")) {
                sendCommand(mc, "setblock " + TARGET_BLOCK_X + " " + (TARGET_BLOCK_Y + 1) + " "
                        + TARGET_BLOCK_Z + " " + BLOCK_SPEC.replace("part=foot", "part=head"));
            }
            return;
        }
        sendStudioCommands(mc);
        sendCommand(mc, "time set " + (requiresNight(captureCase.entityId()) ? "midnight" : "noon"));
        if (placeHarnessOnlyTarget(mc, captureCase)) {
            return;
        }
        sendCommand(mc, "summon " + captureCase.entityId() + " " + TARGET_X + " " + TARGET_Y
                + " " + TARGET_Z + " {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:["
                + captureCase.yaw() + "f,0f],Tags:[\"witchery_model_parity_target\"]}");
    }

    private static boolean placeHarnessOnlyTarget(Minecraft mc, CaptureCase captureCase) {
        Identifier darkMarkId = Identifier.fromNamespaceAndPath("witchery", "dark_mark");
        if (!darkMarkId.equals(captureCase.entityId())) {
            return false;
        }

        DarkMarkEntity target = WitcheryEntities.DARK_MARK.get()
                .create(mc.level, EntitySpawnReason.COMMAND);
        if (target == null) {
            throw new IllegalStateException("Could not create the Dark Mark parity target");
        }
        target.setNoAi(true);
        target.setSilent(true);
        pinPosition(target, TARGET_X, TARGET_Y, TARGET_Z);
        mc.level.addEntity(target);
        harnessOnlyTarget = target;
        return true;
    }

    private static boolean requiresNight(Identifier entityId) {
        return Identifier.fromNamespaceAndPath("witchery", "vampire").equals(entityId);
    }

    private static boolean requiresUnequippedCapture(Identifier entityId) {
        return Identifier.fromNamespaceAndPath("witchery", "village_guard").equals(entityId)
                || Identifier.fromNamespaceAndPath("witchery", "vampire").equals(entityId);
    }

    private static void sendCommand(Minecraft mc, String command) {
        mc.getConnection().sendCommand(command);
    }

    private static String yawLabel(float yaw) {
        return Float.toString(yaw).replace('-', 'm').replace('.', '_');
    }

    private static Entity findTargetEntity(Minecraft mc) {
        if (BLOCK_CAPTURE) {
            return null;
        }
        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Entity entity : mc.level.entitiesForRendering()) {
            Identifier id = entity.getType().builtInRegistryHolder().key().identifier();
            if (currentCase().entityId().equals(id)) {
                double distance = entity.distanceToSqr(TARGET_X, TARGET_Y, TARGET_Z);
                if (distance < nearestDistance) {
                    nearest = entity;
                    nearestDistance = distance;
                }
            }
        }
        return nearest;
    }

    private static boolean isStudioReady(LocalPlayer player, Entity target) {
        return close(player.getX(), CAMERA_X)
                && close(player.getY(), CAMERA_Y)
                && close(player.getZ(), CAMERA_Z)
                && (BLOCK_CAPTURE || target != null)
                && (BLOCK_CAPTURE || (close(target.getX(), TARGET_X)
                && close(target.getY(), TARGET_Y)
                && close(target.getZ(), TARGET_Z)));
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) <= 0.01D;
    }

    private static void logScene(LocalPlayer player, Entity target) {
        WitcheryMod.LOGGER.info("Legacy model parity player pos=({}, {}, {}) look=({}, {})",
                player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        if (BLOCK_CAPTURE) {
            WitcheryMod.LOGGER.info("Legacy model parity block {} pos=({}, {}, {})",
                    BLOCK_SPEC, TARGET_BLOCK_X, TARGET_BLOCK_Y, TARGET_BLOCK_Z);
            return;
        }
        Identifier id = target.getType().builtInRegistryHolder().key().identifier();
        WitcheryMod.LOGGER.info("Legacy model parity entity {} pos=({}, {}, {}) look=({}, {}) size=({}, {})",
                id, target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot(),
                target.getBbWidth(), target.getBbHeight());
    }

    private static void logScreenshotMessage(Component message) {
        WitcheryMod.LOGGER.info("Legacy model parity harness: {}", message.getString());
    }

    private static void tickExitCountdown(Minecraft mc) {
        if (exitCountdown > 0) {
            exitCountdown--;
        } else if (exitCountdown == 0) {
            exitCountdown = -1;
            mc.stop();
        }
    }

    private static CaptureCase currentCase() {
        return CAPTURE_CASES.get(captureCaseIndex);
    }

    private static List<CaptureCase> parseCaptureCases() {
        if (BLOCK_CAPTURE) {
            return List.of(new CaptureCase(
                    Identifier.fromNamespaceAndPath("minecraft", "marker"), 0.0F));
        }
        String configured = System.getProperty("witchery.modelParityHarness.cases", "").trim();
        if (configured.isEmpty()) {
            Identifier entityId = Identifier.parse(
                    System.getProperty("witchery.modelParityHarness.entity", "witchery:demon"));
            float yaw = Float.parseFloat(
                    System.getProperty("witchery.modelParityHarness.yaw", "0"));
            return List.of(new CaptureCase(entityId, yaw));
        }

        List<CaptureCase> cases = new ArrayList<>();
        for (String value : configured.split(",")) {
            String[] pieces = value.trim().split("@", 2);
            if (pieces.length != 2) {
                throw new IllegalArgumentException("Invalid model parity case: " + value);
            }
            cases.add(new CaptureCase(Identifier.parse(pieces[0]), Float.parseFloat(pieces[1])));
        }
        if (cases.isEmpty()) {
            throw new IllegalArgumentException("At least one model parity capture case is required");
        }
        return List.copyOf(cases);
    }

    private record CaptureCase(Identifier entityId, float yaw) {
    }

    private static String blockPath() {
        int namespace = BLOCK_SPEC.indexOf(':');
        int properties = BLOCK_SPEC.indexOf('[');
        int start = namespace >= 0 ? namespace + 1 : 0;
        int end = properties >= 0 ? properties : BLOCK_SPEC.length();
        return BLOCK_SPEC.substring(start, end);
    }
}
