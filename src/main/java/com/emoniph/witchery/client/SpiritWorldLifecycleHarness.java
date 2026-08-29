package com.emoniph.witchery.client;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.CorpseEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.spiritworld.SpiritWorldHelper;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

/** Property-gated two-process live-client certification for Spirit dream persistence. */
public final class SpiritWorldLifecycleHarness {
    private static final boolean ENABLED = Boolean.getBoolean("witchery.spiritWorldLifecycleHarness");
    private static final String RUN_ID = System.getProperty("witchery.spiritWorldLifecycleHarness.runId",
            Long.toString(System.currentTimeMillis()));
    private static final Path REPORT_DIR = Path.of(System.getProperty(
            "witchery.spiritWorldLifecycleHarness.reportDir", "build/runtime-test/spirit-world-lifecycle-live"));
    private static final Phase PHASE = Phase.parse(System.getProperty(
            "witchery.spiritWorldLifecycleHarness.phase", "prepare"));
    private static final int INITIAL_SETTLE_TICKS = 80;
    private static final int TRANSITION_SETTLE_TICKS = 40;

    private static boolean setupSubmitted;
    private static boolean verificationSubmitted;
    private static boolean useSent;
    private static boolean finalVerificationSubmitted;
    private static boolean reportWritten;
    private static int ticks;
    private static int transitionTicks;
    private static int exitCountdown = -1;
    private static volatile boolean setupComplete;
    private static volatile boolean restartVerified;
    private static volatile boolean phaseComplete;
    private static volatile String failure = "";
    private static volatile RestartSnapshot restartSnapshot;

    private SpiritWorldLifecycleHarness() {
    }

    public static void clientTick() {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.player instanceof LocalPlayer player) || mc.level == null
                || mc.getConnection() == null || mc.getSingleplayerServer() == null) {
            tickExitCountdown(mc);
            return;
        }

        ticks++;
        if (!failure.isEmpty()) {
            writeFailureReport();
            beginExit();
            tickExitCountdown(mc);
            return;
        }

        if (PHASE == Phase.PREPARE) {
            tickPrepare(mc, player);
        } else {
            tickResume(mc, player);
        }
        if (phaseComplete) {
            beginExit();
        }
        tickExitCountdown(mc);
    }

    private static void tickPrepare(Minecraft mc, LocalPlayer player) {
        if (!setupSubmitted && ticks >= INITIAL_SETTLE_TICKS) {
            setupSubmitted = true;
            submitPrepare(mc.getSingleplayerServer(), player.getUUID());
            return;
        }
        if (!setupComplete || verificationSubmitted
                || !mc.level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)
                || !WitcheryAttachments.get(player).isInSpiritWorld()) {
            return;
        }
        if (transitionTicks++ < TRANSITION_SETTLE_TICKS) {
            return;
        }
        verificationSubmitted = true;
        submitPrepareVerification(mc.getSingleplayerServer(), player.getUUID());
    }

    private static void tickResume(Minecraft mc, LocalPlayer player) {
        if (!verificationSubmitted && ticks >= INITIAL_SETTLE_TICKS) {
            verificationSubmitted = true;
            submitRestartVerification(mc.getSingleplayerServer(), player.getUUID());
            return;
        }
        if (restartVerified && !useSent) {
            if (!mc.level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)
                    || !WitcheryAttachments.get(player).isInSpiritWorld()
                    || mc.gameMode == null) {
                return;
            }
            int needleSlot = findItem(player, WitcheryItems.ICY_NEEDLE.get());
            if (needleSlot < 0) {
                fail("Restarted Spirit inventory did not expose an Icy Needle to the live client");
                return;
            }
            player.getInventory().setSelectedSlot(needleSlot);
            mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            useSent = true;
            transitionTicks = 0;
            WitcheryMod.LOGGER.info("Spirit lifecycle harness used the Icy Needle after process restart");
            return;
        }
        if (!useSent || finalVerificationSubmitted
                || !mc.level.dimension().equals(Level.OVERWORLD)
                || WitcheryAttachments.get(player).isInSpiritWorld()) {
            return;
        }
        if (transitionTicks++ < TRANSITION_SETTLE_TICKS) {
            return;
        }
        finalVerificationSubmitted = true;
        submitFinalVerification(mc.getSingleplayerServer(), player.getUUID());
    }

    private static void submitPrepare(MinecraftServer server, UUID playerId) {
        server.execute(() -> {
            try {
                ServerPlayer player = requirePlayer(server, playerId);
                WitcheryPlayerData data = WitcheryAttachments.get(player);
                if (data.isManifestedGhost()) {
                    SpiritWorldHelper.returnGhostPlayerToSpiritWorld(player);
                }
                if (data.isInSpiritWorld()) {
                    SpiritWorldHelper.exit(player);
                }
                if (!player.level().dimension().equals(Level.OVERWORLD)) {
                    SpiritWorldHelper.recoverInvalidSpiritState(player);
                }
                require(player.level().dimension().equals(Level.OVERWORLD),
                        "Certification copy could not be returned to the Overworld before setup");

                data.setSpiritInventorySnbt("");
                data.clearManifestationState();
                server.setDifficulty(Difficulty.NORMAL, true);
                player.setGameMode(GameType.SURVIVAL);
                player.setInvulnerable(false);
                player.getAbilities().invulnerable = false;
                player.getInventory().clearContent();
                player.getInventory().setItem(0, new ItemStack(WitcheryItems.ICY_NEEDLE.get(), 2));
                player.getInventory().setItem(1, new ItemStack(Items.DIAMOND, 3));
                player.setHealth(13.0F);
                player.getFoodData().setFoodLevel(7);
                player.getFoodData().setSaturation(2.0F);

                ServerLevel overworld = server.overworld();
                int x = player.getBlockX();
                int z = player.getBlockZ();
                int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1;
                BlockPos anchor = new BlockPos(x, y, z);
                overworld.setBlock(anchor.below(), Blocks.STONE.defaultBlockState(), 3);
                overworld.setBlock(anchor, Blocks.AIR.defaultBlockState(), 3);
                overworld.setBlock(anchor.above(), Blocks.AIR.defaultBlockState(), 3);
                overworld.setChunkForced(x >> 4, z >> 4, true);
                player.teleportTo(overworld, x + 0.5D, y, z + 0.5D,
                        java.util.Set.of(), 0.0F, 0.0F, false);
                SpiritWorldHelper.enter(player, 0.0D);
                require(player.level().dimension().equals(WitcheryDimensions.SPIRIT_WORLD),
                        "Production dream entry did not reach the Spirit World");
                setupComplete = true;
                WitcheryMod.LOGGER.info("Spirit lifecycle harness entered a peaceful dream for restart setup");
            } catch (Exception exception) {
                fail(exception.getMessage());
            }
        });
    }

    private static void submitPrepareVerification(MinecraftServer server, UUID playerId) {
        server.execute(() -> {
            try {
                ServerPlayer player = requirePlayer(server, playerId);
                RestartSnapshot snapshot = inspectDream(server, player);
                require(snapshot.icyNeedles() == 2 && snapshot.diamonds() == 0,
                        "Prepared Spirit inventory did not contain exactly two Icy Needles and zero diamonds");
                server.saveEverything(true, true, false);

                JsonObject report = baseReport("passed");
                addSnapshot(report, "prepared", snapshot);
                report.addProperty("fixture_return_chunk_forced", true);
                report.addProperty("save_flushed_while_dreaming", true);
                writeReport(report);
                phaseComplete = true;
                WitcheryMod.LOGGER.info("Spirit lifecycle harness prepared and flushed the live dream state");
            } catch (Exception exception) {
                fail(exception.getMessage());
            }
        });
    }

    private static void submitRestartVerification(MinecraftServer server, UUID playerId) {
        server.execute(() -> {
            try {
                ServerPlayer player = requirePlayer(server, playerId);
                RestartSnapshot snapshot = inspectDream(server, player);
                require(snapshot.icyNeedles() == 2 && snapshot.diamonds() == 0,
                        "Restarted Spirit inventory did not preserve its isolated item counts");
                restartSnapshot = snapshot;
                restartVerified = true;
                WitcheryMod.LOGGER.info("Spirit lifecycle harness verified dream, Body, and escrow after process restart");
            } catch (Exception exception) {
                fail(exception.getMessage());
            }
        });
    }

    private static void submitFinalVerification(MinecraftServer server, UUID playerId) {
        server.execute(() -> {
            try {
                ServerPlayer player = requirePlayer(server, playerId);
                WitcheryPlayerData data = WitcheryAttachments.get(player);
                require(player.level().dimension().equals(Level.OVERWORLD),
                        "Icy Needle wake did not return the player to the Overworld");
                require(!data.isInSpiritWorld() && data.getOverworldInventorySnbt().isEmpty(),
                        "Icy Needle wake did not clear dream state and mortal inventory escrow");
                require(data.getCorpseUUID().isEmpty(), "Icy Needle wake did not clear the Body link");
                require(restartSnapshot != null, "Restart snapshot was unavailable during final verification");

                ServerLevel overworld = server.overworld();
                BlockPos returnPos = restartSnapshot.returnPos();
                overworld.getChunk(returnPos.getX() >> 4, returnPos.getZ() >> 4, ChunkStatus.FULL, true);
                require(overworld.getEntity(restartSnapshot.corpseUuid()) == null,
                        "The persisted Body entity remained after waking");
                require(countItem(player, Items.DIAMOND) == 3,
                        "The waking inventory did not restore exactly three escrowed diamonds");
                require(countItem(player, WitcheryItems.ICY_NEEDLE.get()) == 1,
                        "The consumed Icy Needle count did not transfer back exactly");
                require(close(player.getHealth(), restartSnapshot.savedHealth())
                                && player.getFoodData().getFoodLevel() == restartSnapshot.savedFood(),
                        "Waking health or food did not restore across process restart; current="
                                + player.getHealth() + "/" + player.getFoodData().getFoodLevel()
                                + ", saved=" + restartSnapshot.savedHealth() + "/" + restartSnapshot.savedFood());
                overworld.setChunkForced(returnPos.getX() >> 4, returnPos.getZ() >> 4, false);
                server.saveEverything(true, true, false);

                JsonObject report = baseReport("passed");
                addSnapshot(report, "restarted", restartSnapshot);
                report.addProperty("fixture_return_chunk_forced", true);
                report.addProperty("fixture_return_chunk_released", true);
                report.addProperty("wake_dimension", player.level().dimension().identifier().toString());
                report.addProperty("wake_dreaming", data.isInSpiritWorld());
                report.addProperty("wake_escrow_chars", data.getOverworldInventorySnbt().length());
                report.addProperty("wake_body_link_present", data.getCorpseUUID().isPresent());
                report.addProperty("wake_body_entity_present", false);
                report.addProperty("wake_icy_needles", countItem(player, WitcheryItems.ICY_NEEDLE.get()));
                report.addProperty("wake_diamonds", countItem(player, Items.DIAMOND));
                report.addProperty("wake_health", player.getHealth());
                report.addProperty("wake_food", player.getFoodData().getFoodLevel());
                report.addProperty("save_flushed_after_wake", true);
                writeReport(report);
                phaseComplete = true;
                WitcheryMod.LOGGER.info("Spirit lifecycle harness completed the post-restart Icy Needle wake");
            } catch (Exception exception) {
                fail(exception.getMessage());
            }
        });
    }

    private static RestartSnapshot inspectDream(MinecraftServer server, ServerPlayer player) {
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        require(player.level().dimension().equals(WitcheryDimensions.SPIRIT_WORLD),
                "Persisted player was not in the Spirit World");
        require(data.isInSpiritWorld() && data.isSpiritWorldDream() && !data.isDemonicNightmare(),
                "Persisted attachment was not a peaceful dream");
        require(!data.getOverworldInventorySnbt().isEmpty(), "Mortal inventory escrow was empty");
        UUID corpseId = data.getCorpseUUID().orElseThrow(() ->
                new IllegalStateException("Persisted dream had no Body UUID"));
        BlockPos returnPos = data.getReturnPos().orElseThrow(() ->
                new IllegalStateException("Persisted dream had no return position"));
        ServerLevel overworld = server.overworld();
        overworld.getChunk(returnPos.getX() >> 4, returnPos.getZ() >> 4, ChunkStatus.FULL, true);
        var body = overworld.getEntity(corpseId);
        require(body instanceof CorpseEntity,
                "Persisted Body entity was not present after loading its return chunk");
        CorpseEntity corpse = (CorpseEntity) body;
        require(corpse.getOwnerUUID().filter(player.getUUID()::equals).isPresent(),
                "Persisted Body owner did not match the dreaming player");
        return new RestartSnapshot(
                player.level().dimension().identifier().toString(),
                data.getOverworldInventorySnbt().length(), corpseId, returnPos,
                countItem(player, WitcheryItems.ICY_NEEDLE.get()), countItem(player, Items.DIAMOND),
                data.getSavedHealth(), data.getSavedFoodLevel());
    }

    private static ServerPlayer requirePlayer(MinecraftServer server, UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            throw new IllegalStateException("Integrated server could not resolve the live player");
        }
        return player;
    }

    private static int findItem(LocalPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(item)) {
                return slot;
            }
        }
        return -1;
    }

    private static int countItem(ServerPlayer player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void addSnapshot(JsonObject report, String prefix, RestartSnapshot snapshot) {
        report.addProperty(prefix + "_dimension", snapshot.dimension());
        report.addProperty(prefix + "_dreaming", true);
        report.addProperty(prefix + "_escrow_chars", snapshot.escrowChars());
        report.addProperty(prefix + "_corpse_uuid", snapshot.corpseUuid().toString());
        report.addProperty(prefix + "_corpse_owner_matches", true);
        report.addProperty(prefix + "_return_x", snapshot.returnPos().getX());
        report.addProperty(prefix + "_return_y", snapshot.returnPos().getY());
        report.addProperty(prefix + "_return_z", snapshot.returnPos().getZ());
        report.addProperty(prefix + "_icy_needles", snapshot.icyNeedles());
        report.addProperty(prefix + "_diamonds", snapshot.diamonds());
        report.addProperty(prefix + "_saved_health", snapshot.savedHealth());
        report.addProperty(prefix + "_saved_food", snapshot.savedFood());
    }

    private static JsonObject baseReport(String status) {
        JsonObject report = new JsonObject();
        report.addProperty("status", status);
        report.addProperty("run_id", RUN_ID);
        report.addProperty("phase", PHASE.id);
        return report;
    }

    private static synchronized void fail(String message) {
        if (failure.isEmpty()) {
            failure = message == null || message.isBlank() ? "Unknown lifecycle harness failure" : message;
            WitcheryMod.LOGGER.error("Spirit lifecycle harness failed: {}", failure);
        }
    }

    private static synchronized void writeFailureReport() {
        if (reportWritten) {
            return;
        }
        JsonObject report = baseReport("failed");
        report.addProperty("error", failure);
        writeReport(report);
    }

    private static synchronized void writeReport(JsonObject report) {
        if (reportWritten) {
            return;
        }
        try {
            Files.createDirectories(REPORT_DIR);
            Path reportPath = REPORT_DIR.resolve("spirit-world-lifecycle-" + RUN_ID + "-" + PHASE.id + ".json");
            Files.writeString(reportPath,
                    new GsonBuilder().setPrettyPrinting().create().toJson(report) + System.lineSeparator(),
                    StandardCharsets.UTF_8);
            reportWritten = true;
        } catch (IOException exception) {
            failure = "Could not write lifecycle report: " + exception.getMessage();
            WitcheryMod.LOGGER.error("Spirit lifecycle harness could not write its report", exception);
        }
    }

    private static void beginExit() {
        if (exitCountdown < 0) {
            exitCountdown = 40;
        }
    }

    private static void tickExitCountdown(Minecraft mc) {
        if (exitCountdown > 0) {
            exitCountdown--;
        } else if (exitCountdown == 0) {
            exitCountdown = -1;
            mc.stop();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static boolean close(float actual, float expected) {
        return Math.abs(actual - expected) <= 0.01F;
    }

    private enum Phase {
        PREPARE("prepare"),
        RESUME("resume");

        private final String id;

        Phase(String id) {
            this.id = id;
        }

        private static Phase parse(String value) {
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "prepare" -> PREPARE;
                case "resume" -> RESUME;
                default -> throw new IllegalArgumentException("Unknown Spirit lifecycle phase: " + value);
            };
        }
    }

    private record RestartSnapshot(String dimension, int escrowChars, UUID corpseUuid,
                                   BlockPos returnPos, int icyNeedles, int diamonds,
                                   float savedHealth, int savedFood) {
    }
}
