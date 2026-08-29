package com.emoniph.witchery.client;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.SpiritPortalBlock;
import com.emoniph.witchery.entity.CorpseEntity;
import com.emoniph.witchery.entity.NightmareEntity;
import com.emoniph.witchery.entity.SpiritEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;
import java.util.UUID;

/** Property-gated real-render capture matrix for Spirit presentation certification. */
public final class SpiritPresentationCaptureHarness {
    private static final boolean ENABLED = Boolean.getBoolean("witchery.spiritPresentationHarness");
    private static final boolean CAPTURE = Boolean.getBoolean("witchery.spiritPresentationHarness.capture");
    private static final boolean EXIT_AFTER_CAPTURE =
            Boolean.getBoolean("witchery.spiritPresentationHarness.exitAfterCapture");
    private static final String RUN_ID = System.getProperty("witchery.spiritPresentationHarness.runId",
            Long.toString(System.currentTimeMillis()));
    private static final CaptureCase[] CASES = CaptureCase.values();
    private static final int SETTLE_TICKS = 80;
    private static final int CENTER_X = 4_096;
    private static final int FLOOR_Y = 120;
    private static final int CENTER_Z = 4_096;
    private static final AABB STUDIO_BOUNDS = new AABB(
            CENTER_X - 12, FLOOR_Y, CENTER_Z - 12,
            CENTER_X + 13, FLOOR_Y + 12, CENTER_Z + 13);

    private static boolean setupSent;
    private static volatile boolean setupComplete;
    private static boolean caseCaptured;
    private static boolean readyToCapture;
    private static boolean loggedWaiting;
    private static int caseIndex;
    private static int caseTicks;
    private static int transitionTicks;
    private static int exitCountdown = -1;

    private SpiritPresentationCaptureHarness() {
    }

    public static void clientTick() {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.player instanceof LocalPlayer player) || mc.level == null || mc.getConnection() == null) {
            if (!loggedWaiting) {
                loggedWaiting = true;
                WitcheryMod.LOGGER.info("Spirit presentation harness waiting for an integrated client world");
            }
            return;
        }
        if (mc.screen != null) {
            tickExitCountdown(mc);
            return;
        }
        if (caseIndex >= CASES.length) {
            clearClientManifestation(player);
            if (EXIT_AFTER_CAPTURE && exitCountdown < 0) {
                exitCountdown = 30;
            }
            tickExitCountdown(mc);
            return;
        }

        CaptureCase captureCase = CASES[caseIndex];
        configureView(mc, player, captureCase);
        if (!setupSent) {
            MinecraftServer server = mc.getSingleplayerServer();
            if (server == null) {
                return;
            }
            setupSent = true;
            setupComplete = false;
            submitCase(server, player.getUUID(), captureCase);
            WitcheryMod.LOGGER.info("Spirit presentation harness preparing {}", captureCase.id);
            return;
        }
        if (!setupComplete || !atExpectedPosition(player, captureCase)
                || !caseStateReady(mc, player, captureCase)) {
            tickExitCountdown(mc);
            return;
        }

        if (captureCase == CaptureCase.MANIFESTATION) {
            // Keep the render-side attachment deterministic even between tracking sync packets.
            var data = WitcheryAttachments.get(player);
            data.setInSpiritWorld(true);
            data.setSpiritWorldIsDream(true);
            data.setManifestedGhost(true);
            data.setManifestationTime(900);
        } else {
            configureClientDreamState(player, captureCase);
            pinCamera(player, captureCase);
        }

        if (caseCaptured) {
            if (transitionTicks++ >= 12) {
                caseIndex++;
                caseTicks = 0;
                transitionTicks = 0;
                setupSent = false;
                setupComplete = false;
                caseCaptured = false;
            }
            tickExitCountdown(mc);
            return;
        }
        if (caseTicks++ >= SETTLE_TICKS) {
            readyToCapture = true;
        }
        tickExitCountdown(mc);
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
        String fileName = "spirit-presentation-" + RUN_ID + "-" + CASES[caseIndex].id + ".png";
        Screenshot.grab(mc.gameDirectory, fileName, mc.getMainRenderTarget(), 1,
                SpiritPresentationCaptureHarness::logScreenshotMessage);
        WitcheryMod.LOGGER.info("Spirit presentation harness captured {}", fileName);
    }

    private static void configureView(Minecraft mc, LocalPlayer player, CaptureCase captureCase) {
        mc.options.hideGui = captureCase != CaptureCase.BODY;
        mc.options.setCameraType(captureCase.cameraType);
        mc.options.fov().set(70);
        mc.options.gamma().set(1.0D);
        player.setYRot(captureCase.yaw);
        player.setXRot(captureCase.pitch);
        player.yRotO = captureCase.yaw;
        player.xRotO = captureCase.pitch;
    }

    private static void submitCase(MinecraftServer server, UUID playerId, CaptureCase captureCase) {
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                return;
            }
            ServerLevel level = server.overworld();
            server.setDifficulty(Difficulty.NORMAL, true);
            level.setDayTime(6_000L);
            level.setWeatherParameters(6_000, 0, false, false);
            clearStudio(level);
            clearPlayerState(player);

            if (captureCase == CaptureCase.MANIFESTATION) {
                player.setGameMode(GameType.SURVIVAL);
                player.teleportTo(level, captureCase.x, captureCase.y, captureCase.z,
                        Set.<Relative>of(), captureCase.yaw, captureCase.pitch, false);
            } else {
                player.setGameMode(GameType.SPECTATOR);
                player.teleportTo(level, captureCase.x, captureCase.y, captureCase.z,
                        Set.<Relative>of(), captureCase.yaw, captureCase.pitch, false);
            }

            switch (captureCase) {
                case PORTAL -> buildPortal(level);
                case BODY -> spawnBody(level, player);
                case MANIFESTATION -> configureManifestation(player);
                case SPIRIT -> {
                    configureDreamState(player, true, false);
                    spawnSpirit(level);
                }
                case NIGHTMARE -> {
                    configureDreamState(player, false, false);
                    spawnNightmare(level);
                }
                case DEMONIC_NIGHTMARE -> {
                    configureDreamState(player, false, true);
                    spawnNightmare(level);
                }
            }
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                    SyncPlayerDataPayload.from(player, WitcheryAttachments.get(player)));
            logEvidence(player, captureCase);
            setupComplete = true;
        });
    }

    private static void clearStudio(ServerLevel level) {
        level.getEntitiesOfClass(CorpseEntity.class, STUDIO_BOUNDS).forEach(net.minecraft.world.entity.Entity::discard);
        level.getEntitiesOfClass(SpiritEntity.class, STUDIO_BOUNDS).forEach(net.minecraft.world.entity.Entity::discard);
        level.getEntitiesOfClass(NightmareEntity.class, STUDIO_BOUNDS).forEach(net.minecraft.world.entity.Entity::discard);
        for (int x = CENTER_X - 12; x <= CENTER_X + 12; x++) {
            for (int z = CENTER_Z - 12; z <= CENTER_Z + 12; z++) {
                level.setBlock(new BlockPos(x, FLOOR_Y, z), Blocks.GRASS_BLOCK.defaultBlockState(),
                        Block.UPDATE_CLIENTS);
                for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 8; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static void clearPlayerState(ServerPlayer player) {
        var data = WitcheryAttachments.get(player);
        data.setInSpiritWorld(false);
        data.setSpiritWorldIsDream(false);
        data.setDemonicNightmare(false);
        data.clearManifestationState();
        player.getInventory().clearContent();
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    private static void buildPortal(ServerLevel level) {
        int portalZ = CENTER_Z + 3;
        for (int width = -1; width <= 2; width++) {
            for (int height = -1; height <= 2; height++) {
                boolean corner = SpiritPortalBlock.isFrameCorner(width, height);
                boolean frame = width == -1 || width == 2 || height == -1 || height == 2;
                if (frame && !corner) {
                    level.setBlock(new BlockPos(CENTER_X + width, FLOOR_Y + 1 + height, portalZ),
                            Blocks.SNOW_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        for (int width = 0; width < 2; width++) {
            for (int height = 0; height < 2; height++) {
                level.setBlock(new BlockPos(CENTER_X + width, FLOOR_Y + 1 + height, portalZ),
                        WitcheryBlocks.SPIRIT_PORTAL.get().defaultBlockState()
                                .setValue(SpiritPortalBlock.AXIS, Direction.Axis.X),
                        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    private static void spawnBody(ServerLevel level, ServerPlayer player) {
        var data = WitcheryAttachments.get(player);
        data.setInSpiritWorld(true);
        data.setSpiritWorldIsDream(true);
        CorpseEntity body = WitcheryEntities.CORPSE.get().create(level,
                net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (body == null) {
            throw new IllegalStateException("Could not create Body capture entity");
        }
        body.setOwner(player);
        body.setPos(CENTER_X + 0.5D, FLOOR_Y + 1.0D, CENTER_Z + 3.0D);
        body.setYRot(0.0F);
        level.addFreshEntity(body);
    }

    private static void configureManifestation(ServerPlayer player) {
        configureDreamState(player, true, false);
        var data = WitcheryAttachments.get(player);
        data.setManifestedGhost(true);
        data.setManifestationTime(900);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(WitcheryItems.ICY_NEEDLE.get()));
    }

    private static void configureDreamState(ServerPlayer player, boolean dream, boolean demonic) {
        var data = WitcheryAttachments.get(player);
        data.setInSpiritWorld(true);
        data.setSpiritWorldIsDream(dream);
        data.setDemonicNightmare(demonic);
    }

    private static void spawnSpirit(ServerLevel level) {
        SpiritEntity spirit = WitcheryEntities.SPIRIT.get().create(level,
                net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (spirit == null) {
            throw new IllegalStateException("Could not create Spirit capture entity");
        }
        spirit.setNoAi(true);
        spirit.setNoGravity(true);
        spirit.setPersistenceRequired();
        spirit.setPos(CENTER_X + 0.5D, FLOOR_Y + 2.0D, CENTER_Z + 3.0D);
        level.addFreshEntity(spirit);
    }

    private static void spawnNightmare(ServerLevel level) {
        NightmareEntity nightmare = WitcheryEntities.NIGHTMARE.get().create(level,
                net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (nightmare == null) {
            throw new IllegalStateException("Could not create Nightmare capture entity");
        }
        nightmare.setNoAi(true);
        nightmare.setPersistenceRequired();
        nightmare.setPos(CENTER_X + 0.5D, FLOOR_Y + 1.0D, CENTER_Z + 3.0D);
        nightmare.setYRot(180.0F);
        nightmare.yBodyRot = 180.0F;
        nightmare.yHeadRot = 180.0F;
        level.addFreshEntity(nightmare);
    }

    private static void logEvidence(ServerPlayer player, CaptureCase captureCase) {
        var data = WitcheryAttachments.get(player);
        WitcheryMod.LOGGER.info("Spirit presentation evidence case={} camera={} inSpiritWorld={} "
                        + "manifested={} demonic={} mainHand={} head={} portalTint=#{}",
                captureCase.id, captureCase.cameraType, data.isInSpiritWorld(), data.isManifestedGhost(),
                data.isDemonicNightmare(), player.getMainHandItem().getItem(),
                player.getItemBySlot(EquipmentSlot.HEAD).getItem(),
                String.format("%06x", SpiritPortalBlock.SOURCE_PORTAL_RGB));
    }

    private static void clearClientManifestation(LocalPlayer player) {
        var data = WitcheryAttachments.get(player);
        data.setInSpiritWorld(false);
        data.setSpiritWorldIsDream(false);
        data.setDemonicNightmare(false);
        data.clearManifestationState();
    }

    private static void configureClientDreamState(LocalPlayer player, CaptureCase captureCase) {
        clearClientManifestation(player);
        if (captureCase == CaptureCase.BODY || captureCase == CaptureCase.SPIRIT
                || captureCase == CaptureCase.NIGHTMARE || captureCase == CaptureCase.DEMONIC_NIGHTMARE) {
            var data = WitcheryAttachments.get(player);
            data.setInSpiritWorld(true);
            data.setSpiritWorldIsDream(captureCase == CaptureCase.BODY || captureCase == CaptureCase.SPIRIT);
            data.setDemonicNightmare(captureCase == CaptureCase.DEMONIC_NIGHTMARE);
        }
    }

    private static void pinCamera(LocalPlayer player, CaptureCase captureCase) {
        player.setPos(captureCase.x, captureCase.y, captureCase.z);
        player.xo = captureCase.x;
        player.yo = captureCase.y;
        player.zo = captureCase.z;
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
    }

    private static boolean atExpectedPosition(LocalPlayer player, CaptureCase captureCase) {
        return Math.abs(player.getX() - captureCase.x) <= 0.1D
                && Math.abs(player.getY() - captureCase.y) <= 0.1D
                && Math.abs(player.getZ() - captureCase.z) <= 0.1D;
    }

    private static boolean caseStateReady(Minecraft mc, LocalPlayer player, CaptureCase captureCase) {
        return switch (captureCase) {
            case PORTAL -> mc.level.getBlockState(
                    new BlockPos(CENTER_X, FLOOR_Y + 1, CENTER_Z + 3)).is(WitcheryBlocks.SPIRIT_PORTAL.get());
            case BODY -> hasRenderedEntity(mc, CorpseEntity.class);
            case MANIFESTATION -> WitcheryAttachments.get(player).isManifestedGhost()
                    && player.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET)
                    && player.getMainHandItem().is(Items.DIAMOND_SWORD);
            case SPIRIT -> hasRenderedEntity(mc, SpiritEntity.class);
            case NIGHTMARE, DEMONIC_NIGHTMARE -> hasRenderedEntity(mc, NightmareEntity.class);
        };
    }

    private static boolean hasRenderedEntity(Minecraft mc, Class<?> entityType) {
        for (net.minecraft.world.entity.Entity entity : mc.level.entitiesForRendering()) {
            if (entityType.isInstance(entity)) {
                return true;
            }
        }
        return false;
    }

    private static void logScreenshotMessage(Component message) {
        WitcheryMod.LOGGER.info("Spirit presentation harness: {}", message.getString());
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
        PORTAL("portal", CENTER_X + 0.5D, FLOOR_Y + 2.0D, CENTER_Z - 5.5D,
                0.0F, 0.0F, CameraType.FIRST_PERSON),
        BODY("body", CENTER_X + 0.5D, FLOOR_Y + 2.2D, CENTER_Z - 2.5D,
                0.0F, 13.0F, CameraType.FIRST_PERSON),
        MANIFESTATION("manifestation", CENTER_X + 0.5D, FLOOR_Y + 1.0D, CENTER_Z + 1.0D,
                0.0F, 0.0F, CameraType.THIRD_PERSON_FRONT),
        SPIRIT("spirit", CENTER_X + 0.5D, FLOOR_Y + 2.0D, CENTER_Z + 0.5D,
                0.0F, 0.0F, CameraType.FIRST_PERSON),
        NIGHTMARE("nightmare", CENTER_X + 0.5D, FLOOR_Y + 2.0D, CENTER_Z - 3.5D,
                0.0F, 5.0F, CameraType.FIRST_PERSON),
        DEMONIC_NIGHTMARE("demonic-nightmare", CENTER_X + 0.5D, FLOOR_Y + 2.0D, CENTER_Z - 3.5D,
                0.0F, 5.0F, CameraType.FIRST_PERSON);

        private final String id;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;
        private final CameraType cameraType;

        CaptureCase(String id, double x, double y, double z, float yaw, float pitch, CameraType cameraType) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.cameraType = cameraType;
        }
    }
}
