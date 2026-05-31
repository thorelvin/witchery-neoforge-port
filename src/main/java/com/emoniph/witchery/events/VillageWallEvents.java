package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.world.VillageWallData;
import com.emoniph.witchery.worldgen.VillageWallGenerator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Generates adaptive fortress walls around vanilla villages.
 *
 * Detection: ChunkEvent.Load + LevelChunk.getAllStarts().
 * This was confirmed working in prior testing (produced walls).
 * The start chunk of a village has its full StructureStart deserialized from
 * disk by the time ChunkEvent.Load fires, so getAllStarts() returns all pieces.
 *
 * Generation: queued from chunk load and processed by a wall job. Automatic jobs wait
 * for the padded village footprint to become naturally loaded; manual wall commands can
 * request those chunks in small ticket batches because the player is explicitly waiting.
 * Village goto pre-generates the target village footprint one chunk per tick before
 * teleporting, because dropping a player into a fresh distant village can leave the
 * integrated client hanging in an empty arrival chunk while view-distance loading catches up.
 */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public class VillageWallEvents {

    private record PendingWall(StructureStart start, long processAfter, int attempts) {}
    private record PendingTeleport(UUID playerId, BlockPos target, BlockPos from,
                                   float yRot, float xRot, String namespace, String structureId) {}
    private record LocateCandidate(ChunkPos chunk, StructurePlacement placement, double distanceSqr) {}
    private record VillageGotoFound(BlockPos target, StructureStart start) {}

    public enum VillageGotoResult {
        NOT_READY,
        PREPARING,
        TELEPORTED
    }

    private static final class WallJob {
        final StructureStart start;
        boolean force;
        final String reason;
        final List<ChunkPos> chunks;
        final List<PendingTeleport> teleports = new ArrayList<>();
        final long queuedAt;
        boolean ticketChunks;
        boolean ticketsAdded;
        int ticketCursor;
        long lastProgressAt;
        long lastLogAt;
        int lastReadyCount;

        WallJob(StructureStart start, boolean force, String reason,
                List<ChunkPos> chunks, PendingTeleport teleport, long queuedAt, boolean ticketChunks) {
            this.start = start;
            this.force = force;
            this.reason = reason;
            this.chunks = chunks;
            this.queuedAt = queuedAt;
            this.ticketChunks = ticketChunks;
            if (teleport != null) this.teleports.add(teleport);
        }
    }

    private static final class VillageGotoLocateJob {
        final UUID playerId;
        final BlockPos from;
        final float yRot;
        final float xRot;
        final String namespace;
        final String structureId;
        final Holder<Structure> holder;
        final List<LocateCandidate> candidates;
        final long queuedAt;
        int cursor;
        long lastLogAt;

        VillageGotoLocateJob(UUID playerId, BlockPos from, float yRot, float xRot,
                             String namespace, String structureId, Holder<Structure> holder,
                             List<LocateCandidate> candidates, long queuedAt) {
            this.playerId = playerId;
            this.from = from;
            this.yRot = yRot;
            this.xRot = xRot;
            this.namespace = namespace;
            this.structureId = structureId;
            this.holder = holder;
            this.candidates = candidates;
            this.queuedAt = queuedAt;
        }
    }

    private static final class VillageGotoPrepJob {
        final StructureStart start;
        final PendingTeleport teleport;
        final List<ChunkPos> chunks;
        final boolean generateWalls;
        final long queuedAt;
        int cursor;
        long lastLogAt;

        VillageGotoPrepJob(StructureStart start, PendingTeleport teleport, List<ChunkPos> chunks,
                           boolean generateWalls, long queuedAt) {
            this.start = start;
            this.teleport = teleport;
            this.chunks = chunks;
            this.generateWalls = generateWalls;
            this.queuedAt = queuedAt;
        }
    }

    private static final ConcurrentLinkedQueue<PendingWall> QUEUE = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<WallJob> JOBS = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<VillageGotoLocateJob> GOTO_LOCATES = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<VillageGotoPrepJob> GOTO_PREPS = new ConcurrentLinkedQueue<>();
    private static final long WALL_GENERATION_DELAY_TICKS = 0L;
    private static final long WALL_GENERATION_RETRY_TICKS = 20L;
    private static final int WALL_GENERATION_MAX_ATTEMPTS = 6;
    private static final int WALL_CHUNK_READINESS_MAX_ATTEMPTS = 120;
    private static final long WALL_JOB_LOG_INTERVAL_TICKS = 40L;
    private static final long WALL_JOB_TIMEOUT_TICKS = 20L * 30L;
    private static final long WALL_JOB_STUCK_CHUNK_SKIP_TICKS = 20L * 10L;
    private static final int FORCE_LOAD_PADDING_BLOCKS = 24;
    private static final int FORCE_LOAD_TICKET_PADDING_BLOCKS = 18;
    private static final int FORCE_LOAD_EDGE_RING_CHUNKS = 1;
    private static final int WALL_JOB_CHUNK_TICKET_BATCH = 1;
    private static final int WALL_JOB_FORCED_CHUNK_CAP = 144;
    private static final int WALL_JOB_STEPS_PER_TICK = 2;
    private static final int PENDING_WALL_SCAN_LIMIT_PER_TICK = 16;
    private static final int PENDING_WALL_CHECKS_PER_TICK = 4;
    private static final int PENDING_WALL_QUEUES_PER_TICK = 1;
    private static final int VILLAGE_GOTO_LOCATE_CHUNKS_PER_TICK = 1;
    private static final long VILLAGE_GOTO_LOCATE_LOG_INTERVAL_TICKS = 40L;
    private static final int VILLAGE_GOTO_PREP_CHUNKS_PER_TICK = 1;
    private static final int VILLAGE_GOTO_LANDING_CHUNK_RADIUS = 2;
    private static final long VILLAGE_GOTO_PREP_LOG_INTERVAL_TICKS = 40L;
    private static final long VILLAGE_GOTO_PREP_TIMEOUT_TICKS = 20L * 150L;
    private static long lastQueueDebugAt = Long.MIN_VALUE;

    static final List<String> VILLAGE_IDS = List.of(
            "village_plains", "village_savanna", "village_taiga",
            "village_snowy", "village_desert");

    public static boolean isVillageId(String namespace, String path) {
        return "minecraft".equals(namespace) && VILLAGE_IDS.contains(path);
    }

    private static boolean debugEnabled() {
        return WitcheryConfig.DEBUG.get();
    }

    private static void debugLog(String message, Object... args) {
        if (debugEnabled()) {
            WitcheryMod.LOGGER.info("[VillageWalls:Debug] " + message, args);
        }
    }

    // ── Detection: ChunkEvent.Load + getAllStarts() ────────────────────────────

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;

        VillageWallData data = VillageWallData.get(level);
        boolean queued = false;
        Map<Structure, StructureStart> starts = chunk.getAllStarts();
        for (Map.Entry<Structure, StructureStart> entry : starts.entrySet()) {
            StructureStart start = entry.getValue();
            if (!start.isValid()) continue;
            if (!isVillageStructure(entry.getKey(), level)) continue;
            debugLog("Chunk {} loaded direct village start {} with {} piece(s).",
                    chunk.getPos(), start.getBoundingBox(), start.getPieces().size());
            queued |= queueVillage(level, data, start);
        }

        if (!queued) {
            ChunkPos cp = chunk.getPos();
            BlockPos probe = new BlockPos((cp.x << 4) + 8, 64, (cp.z << 4) + 8);
            StructureStart referenced = findVillageStartFromReferences(level, probe);
            if (referenced != null && referenced.isValid()) {
                debugLog("Chunk {} resolved loaded referenced village start {} with {} piece(s).",
                        cp, referenced.getBoundingBox(), referenced.getPieces().size());
                queueVillage(level, data, referenced);
            }
        }
    }

    // ── Process queue on level tick ────────────────────────────────────────────

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;

        long now = level.getGameTime();
        logQueueDebugState(now);

        VillageGotoPrepJob gotoPrep = GOTO_PREPS.peek();
        if (gotoPrep != null) {
            if (processVillageGotoPrepJob(level, gotoPrep, now)) {
                GOTO_PREPS.poll();
            }
            return;
        }

        int jobSteps = Math.min(JOBS.size(), WALL_JOB_STEPS_PER_TICK);
        for (int i = 0; i < jobSteps; i++) {
            WallJob job = JOBS.poll();
            if (job == null) break;
            if (!processWallJob(level, job, now)) {
                JOBS.add(job);
            }
        }
        if (jobSteps > 0) {
            return;
        }

        VillageGotoLocateJob locateJob = GOTO_LOCATES.peek();
        if (locateJob != null) {
            if (processVillageGotoLocateJob(level, locateJob, now)) {
                GOTO_LOCATES.poll();
            }
            return;
        }

        if (QUEUE.isEmpty()) return;

        Iterator<PendingWall> it = QUEUE.iterator();
        int scanned = 0;
        int checked = 0;
        int queuedJobs = 0;

        while (it.hasNext() && scanned < PENDING_WALL_SCAN_LIMIT_PER_TICK
                && checked < PENDING_WALL_CHECKS_PER_TICK
                && queuedJobs < PENDING_WALL_QUEUES_PER_TICK) {
            PendingWall pending = it.next();
            scanned++;
            if (pending.processAfter() > now) continue;
            it.remove();
            checked++;

            StructureStart start = refreshVillageStart(level, pending.start());
            if (start == null || !start.isValid() || start.getPieces().isEmpty()) {
                retryPending(level, pending, start);
                continue;
            }

            if (queueAutomaticGeneration(level, pending, start)) {
                queuedJobs++;
            }
        }
    }

    private static void logQueueDebugState(long now) {
        if (!debugEnabled() || now - lastQueueDebugAt < WALL_JOB_LOG_INTERVAL_TICKS) return;
        int pending = QUEUE.size();
        int jobs = JOBS.size();
        int locates = GOTO_LOCATES.size();
        int preps = GOTO_PREPS.size();
        if (pending == 0 && jobs == 0 && locates == 0 && preps == 0) return;
        lastQueueDebugAt = now;
        debugLog("tick={} queues: pending={}, wallJobs={}, gotoLocates={}, gotoPreps={}, budgets jobs/tick={}, pendingScan/check/queue={}/{}/{}.",
                now, pending, jobs, locates, preps, WALL_JOB_STEPS_PER_TICK,
                PENDING_WALL_SCAN_LIMIT_PER_TICK, PENDING_WALL_CHECKS_PER_TICK,
                PENDING_WALL_QUEUES_PER_TICK);
    }

    /**
     * Queue a village wall job for commands/debug paths that already resolved the start.
     */
    public static boolean queueGeneration(ServerLevel level, StructureStart start, boolean force, String reason) {
        return queueGeneration(level, start, force, reason, null);
    }

    /**
     * Queue generation near a known position. Used by command fallbacks.
     */
    public static boolean queueGenerationForVillageNear(ServerLevel level, BlockPos pos, boolean force, String reason) {
        StructureStart start = findVillageStartNear(level, pos);
        if (start == null || !start.isValid() || start.getPieces().isEmpty()) {
            WitcheryMod.LOGGER.info("[VillageWalls] No ready village start near {} for queued generation.", pos);
            return false;
        }
        return queueGeneration(level, start, force, reason);
    }

    /**
     * Queue a controlled destination prep for the found village. The prep job loads the
     * needed chunks one per tick, generates walls if needed, then teleports the player.
     */
    public static VillageGotoResult queueVillageGoto(ServerLevel level, ServerPlayer player, BlockPos target,
                                                     BlockPos from, String namespace, String structureId) {
        StructureStart start = findVillageStartNear(level, target);
        return queueVillageGoto(level, player, target, from, namespace, structureId, start);
    }

    private static VillageGotoResult queueVillageGoto(ServerLevel level, ServerPlayer player, BlockPos target,
                                                      BlockPos from, String namespace, String structureId,
                                                      StructureStart start) {
        if (start == null || !start.isValid() || start.getPieces().isEmpty()) {
            WitcheryMod.LOGGER.info("[VillageWalls] No ready village start near {} for goto wall queue.", target);
            return VillageGotoResult.NOT_READY;
        }
        PendingTeleport teleport = new PendingTeleport(player.getUUID(), target, from,
                player.getYRot(), player.getXRot(), namespace, structureId);
        boolean processed = VillageWallData.get(level).isProcessed(start.getBoundingBox());
        List<ChunkPos> chunks = processed
                ? landingPrepChunks(target)
                : gotoWallPrepChunks(start, target);
        GOTO_PREPS.removeIf(job -> job.teleport.playerId().equals(player.getUUID()));
        GOTO_PREPS.add(new VillageGotoPrepJob(start, teleport, chunks, !processed, level.getGameTime()));
        WitcheryMod.LOGGER.info("[Goto] Queued village arrival prep for {}:{} at {} with {} chunk(s){}.",
                namespace, structureId, target, chunks.size(), processed ? "" : " and wall generation");
        player.sendSystemMessage(Component.literal(
                processed
                        ? "The hidden paths find a known village. Hold still while the arrival stones settle."
                        : "The hidden paths find a village. Hold still while its walls rise before the crossing.")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return VillageGotoResult.PREPARING;
    }

    public static boolean queueVillageGotoLocate(ServerLevel level, ServerPlayer player, Holder<Structure> holder,
                                                 BlockPos from, String namespace, String structureId,
                                                 int searchRadius) {
        if (!isVillageId(namespace, structureId)) return false;
        List<LocateCandidate> candidates = villageGotoLocateCandidates(level, holder, from, searchRadius);
        if (candidates.isEmpty()) {
            WitcheryMod.LOGGER.info("[Goto] No safe locate candidates for {}:{} from {}.",
                    namespace, structureId, from);
            return false;
        }

        UUID playerId = player.getUUID();
        GOTO_LOCATES.removeIf(job -> job.playerId.equals(playerId));
        GOTO_LOCATES.add(new VillageGotoLocateJob(playerId, from, player.getYRot(), player.getXRot(),
                namespace, structureId, holder, candidates, level.getGameTime()));
        WitcheryMod.LOGGER.info("[Goto] Queued safe village locate for {}:{} from {} with {} candidate chunk(s).",
                namespace, structureId, from, candidates.size());
        return true;
    }

    private static StructureStart refreshVillageStart(ServerLevel level, StructureStart queued) {
        if (queued == null) return null;
        BoundingBox bb = queued.getBoundingBox();
        BlockPos center = new BlockPos((bb.minX() + bb.maxX()) / 2, 64, (bb.minZ() + bb.maxZ()) / 2);
        StructureStart fresh = findVillageStartNear(level, center);
        return fresh != null ? fresh : queued;
    }

    private static void retryPending(ServerLevel level, PendingWall pending, StructureStart current) {
        if (pending.attempts() + 1 >= WALL_GENERATION_MAX_ATTEMPTS) {
            BoundingBox bb = current != null ? current.getBoundingBox() : pending.start().getBoundingBox();
            WitcheryMod.LOGGER.info("[VillageWalls] Gave up waiting for ready village start at {}.", bb);
            return;
        }
        StructureStart retryStart = current != null ? current : pending.start();
        QUEUE.add(new PendingWall(retryStart,
                level.getGameTime() + WALL_GENERATION_RETRY_TICKS,
                pending.attempts() + 1));
        WitcheryMod.LOGGER.info("[VillageWalls] Village start at {} not ready yet; retry {}/{}.",
                retryStart.getBoundingBox(), pending.attempts() + 1, WALL_GENERATION_MAX_ATTEMPTS);
    }

    private static boolean processVillageGotoLocateJob(ServerLevel level, VillageGotoLocateJob job, long now) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(job.playerId);
        if (player == null) return true;

        int checked = 0;
        while (job.cursor < job.candidates.size() && checked < VILLAGE_GOTO_LOCATE_CHUNKS_PER_TICK) {
            LocateCandidate candidate = job.candidates.get(job.cursor++);
            checked++;
            debugLog("Probing goto candidate {}/{} for {}:{} from {}: chunk={}, distanceSqr={}.",
                    job.cursor, job.candidates.size(), job.namespace, job.structureId, job.from,
                    candidate.chunk(), candidate.distanceSqr());

            VillageGotoFound found = probeVillageGotoCandidate(level, job.holder, candidate);
            if (found == null) continue;

            WitcheryMod.LOGGER.info("[Goto] Safe village locate found {}:{} at {} after {}/{} probe(s).",
                    job.namespace, job.structureId, found.target(), job.cursor, job.candidates.size());

            VillageGotoResult wallResult = queueVillageGoto(level, player, found.target(), job.from,
                    job.namespace, job.structureId, found.start());
            WitcheryMod.LOGGER.info("[Goto] Village wall job for {}:{} at {} -> {}.",
                    job.namespace, job.structureId, found.target(), wallResult.name().toLowerCase(java.util.Locale.ROOT));
            if (wallResult == VillageGotoResult.NOT_READY) {
                player.sendSystemMessage(Component.literal(
                        "The hidden paths found a village, but its walls are not ready to form yet. Try again in a moment.")
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }
            return true;
        }

        if (job.cursor >= job.candidates.size()) {
            player.sendSystemMessage(Component.literal(
                    "The scrying mists reveal no matching village nearby within the safe range. Wander further and gaze again.")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            WitcheryMod.LOGGER.info("[Goto] Safe village locate found no {}:{} from {} after {} probe(s).",
                    job.namespace, job.structureId, job.from, job.candidates.size());
            return true;
        }

        if (now - job.lastLogAt >= VILLAGE_GOTO_LOCATE_LOG_INTERVAL_TICKS) {
            job.lastLogAt = now;
            WitcheryMod.LOGGER.info("[Goto] Safe village locate still probing {}:{} from {}: {}/{} candidate chunk(s).",
                    job.namespace, job.structureId, job.from, job.cursor, job.candidates.size());
        }
        return false;
    }

    private static List<LocateCandidate> villageGotoLocateCandidates(ServerLevel level, Holder<Structure> holder,
                                                                     BlockPos from, int searchRadius) {
        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        int sectionX = SectionPos.blockToSectionCoord(from.getX());
        int sectionZ = SectionPos.blockToSectionCoord(from.getZ());
        List<LocateCandidate> candidates = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        for (StructurePlacement placement : state.getPlacementsForStructure(holder)) {
            if (!(placement instanceof RandomSpreadStructurePlacement spreadPlacement)) continue;
            int spacing = spreadPlacement.spacing();
            for (int radius = 0; radius <= searchRadius; radius++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    boolean zEdge = dz == -radius || dz == radius;
                    for (int dx = -radius; dx <= radius; dx++) {
                        boolean xEdge = dx == -radius || dx == radius;
                        if (!zEdge && !xEdge) continue;
                        int candidateSectionX = sectionX + spacing * dx;
                        int candidateSectionZ = sectionZ + spacing * dz;
                        ChunkPos chunk = spreadPlacement.getPotentialStructureChunk(
                                state.getLevelSeed(), candidateSectionX, candidateSectionZ);
                        if (!seen.add(chunk.toLong())) continue;
                        BlockPos locatePos = placement.getLocatePos(chunk);
                        candidates.add(new LocateCandidate(chunk, placement, locatePos.distSqr(from)));
                    }
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(LocateCandidate::distanceSqr));
        return candidates;
    }

    private static VillageGotoFound probeVillageGotoCandidate(ServerLevel level, Holder<Structure> holder,
                                                              LocateCandidate candidate) {
        debugLog("Checking structure presence for goto candidate chunk {}.", candidate.chunk());
        StructureCheckResult result = level.structureManager().checkStructurePresence(
                candidate.chunk(), holder.value(), candidate.placement(), false);
        debugLog("Structure presence for goto candidate chunk {} -> {}.", candidate.chunk(), result);
        if (result == StructureCheckResult.START_NOT_PRESENT) {
            return null;
        }

        debugLog("Loading STRUCTURE_STARTS for goto candidate chunk {}.", candidate.chunk());
        ChunkAccess chunk = level.getChunk(candidate.chunk().x, candidate.chunk().z,
                ChunkStatus.STRUCTURE_STARTS, true);
        debugLog("Loaded STRUCTURE_STARTS for goto candidate chunk {}.", candidate.chunk());
        StructureStart start = level.structureManager().getStartForStructure(
                SectionPos.bottomOf(chunk), holder.value(), chunk);
        if (start != null && start.isValid()) {
            BlockPos target = candidate.placement().getLocatePos(start.getChunkPos());
            debugLog("Goto candidate chunk {} resolved start {} with {} piece(s), target {}.",
                    candidate.chunk(), start.getBoundingBox(), start.getPieces().size(), target);
            return new VillageGotoFound(target, start);
        }
        debugLog("Goto candidate chunk {} had no valid start after STRUCTURE_STARTS load.", candidate.chunk());
        return null;
    }

    private static boolean processVillageGotoPrepJob(ServerLevel level, VillageGotoPrepJob job, long now) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(job.teleport.playerId());
        if (player == null) return true;

        try {
            int loadedThisTick = 0;
            while (job.cursor < job.chunks.size() && loadedThisTick < VILLAGE_GOTO_PREP_CHUNKS_PER_TICK) {
                int chunkIndex = job.cursor;
                ChunkPos cp = job.chunks.get(job.cursor++);
                debugLog("Goto prep loading FULL chunk {}/{} {} for {}:{} target {} (generateWalls={}).",
                        chunkIndex + 1, job.chunks.size(), cp, job.teleport.namespace(),
                        job.teleport.structureId(), job.teleport.target(), job.generateWalls);
                level.getChunk(cp.x, cp.z, ChunkStatus.FULL, true);
                debugLog("Goto prep loaded FULL chunk {}/{} {} for target {}.",
                        chunkIndex + 1, job.chunks.size(), cp, job.teleport.target());
                loadedThisTick++;
            }

            if (now - job.lastLogAt >= VILLAGE_GOTO_PREP_LOG_INTERVAL_TICKS || job.cursor >= job.chunks.size()) {
                job.lastLogAt = now;
                WitcheryMod.LOGGER.info("[Goto] Preparing village arrival at {}:{} for {}: {}/{} chunk(s) ready.",
                        job.teleport.namespace(), job.teleport.structureId(), job.teleport.target(),
                        Math.min(job.cursor, job.chunks.size()), job.chunks.size());
            }

            if (job.cursor < job.chunks.size()) {
                if (now - job.queuedAt > VILLAGE_GOTO_PREP_TIMEOUT_TICKS) {
                    WitcheryMod.LOGGER.error("[Goto] Timed out preparing village arrival at {} after {}/{} chunk(s).",
                            job.teleport.target(), job.cursor, job.chunks.size());
                    completeTeleport(level, job.teleport, false);
                    return true;
                }
                return false;
            }

            VillageWallData data = VillageWallData.get(level);
            if (job.generateWalls && !data.isProcessed(job.start.getBoundingBox())) {
                StructureStart start = refreshVillageStart(level, job.start);
                if (start == null || !start.isValid() || start.getPieces().isEmpty()) {
                    WitcheryMod.LOGGER.info("[Goto] Village start was not ready after arrival prep at {}.",
                            job.teleport.target());
                    completeTeleport(level, job.teleport, false);
                    return true;
                }
                WitcheryMod.LOGGER.info("[VillageWalls] Generating walls (goto-prep) for village at {} ({} pieces, {} chunks prepared).",
                        start.getBoundingBox(), start.getPieces().size(), job.chunks.size());
                debugLog("Starting goto-prep wall generation for {} after {} prepared chunk(s).",
                        start.getBoundingBox(), job.chunks.size());
                VillageWallGenerator.generate(level, start);
                data.markProcessed(start.getBoundingBox());
                WitcheryMod.LOGGER.info("[VillageWalls] Wall generation complete for {}.",
                        start.getBoundingBox());
            }

            completeTeleport(level, job.teleport, true);
            return true;
        } catch (Exception e) {
            WitcheryMod.LOGGER.error("[Goto] Village arrival prep failed for {}",
                    job.teleport.target(), e);
            completeTeleport(level, job.teleport, false);
            return true;
        }
    }

    private static boolean queueVillage(ServerLevel level, VillageWallData data, StructureStart start) {
        if (data.isProcessed(start.getBoundingBox())) {
            debugLog("Skipping automatic village queue for already processed {}.", start.getBoundingBox());
            return false;
        }

        BoundingBox bb = start.getBoundingBox();
        if (QUEUE.stream().anyMatch(p -> p.start().getBoundingBox().equals(bb))) {
            debugLog("Skipping automatic village queue for {} because it is already pending.", bb);
            return false;
        }
        if (JOBS.stream().anyMatch(job -> job.start.getBoundingBox().equals(bb))) {
            debugLog("Skipping automatic village queue for {} because a wall job is active.", bb);
            return false;
        }
        if (GOTO_PREPS.stream().anyMatch(job -> job.start.getBoundingBox().equals(bb))) {
            debugLog("Skipping automatic village queue for {} because a goto prep owns it.", bb);
            return false;
        }

        long processAt = level.getGameTime() + WALL_GENERATION_DELAY_TICKS;
        QUEUE.add(new PendingWall(start, processAt, 0));
        WitcheryMod.LOGGER.info("[VillageWalls] Queued village at {} ({} pieces).",
                bb, start.getPieces().size());
        return true;
    }

    private static boolean queueGeneration(ServerLevel level, StructureStart start, boolean force,
                                           String reason, PendingTeleport teleport) {
        return queueGeneration(level, start, force, reason, teleport, true);
    }

    private static boolean queueGeneration(ServerLevel level, StructureStart start, boolean force,
                                           String reason, PendingTeleport teleport, boolean ticketChunks) {
        VillageWallData data = VillageWallData.get(level);
        if (!force && data.isProcessed(start.getBoundingBox())) {
            if (teleport != null) completeTeleport(level, teleport, true);
            return true;
        }
        if (!start.isValid() || start.getPieces().isEmpty()) return false;

        BoundingBox bb = start.getBoundingBox();
        for (WallJob job : JOBS) {
            if (job.start.getBoundingBox().equals(bb)) {
                if (force) job.force = true;
                if (ticketChunks && !job.ticketChunks) {
                    job.ticketChunks = true;
                    if (job.ticketsAdded && job.ticketCursor == 0) {
                        job.ticketsAdded = false;
                        job.lastProgressAt = 0L;
                        job.lastReadyCount = 0;
                    }
                }
                if (teleport != null) job.teleports.add(teleport);
                return true;
            }
        }

        BlockPos focus = teleport != null ? teleport.target() : boundingBoxCenter(start.getBoundingBox());
        List<ChunkPos> chunks = ticketChunks ? wallJobTicketChunks(start, focus) : wallJobChunks(start);
        if (teleport != null) {
            ChunkPos targetChunk = new ChunkPos(teleport.target());
            if (!chunks.contains(targetChunk)) chunks.add(targetChunk);
        }
        JOBS.add(new WallJob(start, force, reason, chunks, teleport, level.getGameTime(), ticketChunks));
        debugLog("Queued wall job reason={} force={} ticketChunks={} bb={} chunks={} teleportTarget={}.",
                reason, force, ticketChunks, bb, chunks.size(), teleport != null ? teleport.target() : "none");
        if (ticketChunks) {
            WitcheryMod.LOGGER.info("[VillageWalls] Queued wall job ({}) for {} with {} wall chunk ticket(s).",
                    reason, bb, chunks.size());
        } else {
            WitcheryMod.LOGGER.info("[VillageWalls] Queued wall job ({}) for {}; waiting for {} wall chunk(s) to load naturally.",
                    reason, bb, chunks.size());
        }
        return true;
    }

    private static boolean processWallJob(ServerLevel level, WallJob job, long now) {
        try {
            if (!job.ticketsAdded) {
                if (!job.ticketChunks) {
                    job.ticketsAdded = true;
                    job.lastProgressAt = now;
                    job.lastLogAt = now;
                    WitcheryMod.LOGGER.info("[VillageWalls] Using {} naturally loaded wall chunk(s) for {}.",
                            job.chunks.size(), job.start.getBoundingBox());
                    return false;
                }

                if (job.ticketCursor == 0) {
                    job.lastProgressAt = now;
                    job.lastLogAt = now;
                    WitcheryMod.LOGGER.info("[VillageWalls] Requesting {} wall chunk ticket(s) for {} in batches of {}.",
                            job.chunks.size(), job.start.getBoundingBox(), WALL_JOB_CHUNK_TICKET_BATCH);
                }

                int readyRequested = readyChunkCount(level, job.chunks, 0, job.ticketCursor);
                if (readyRequested < job.ticketCursor) {
                    if (readyRequested > job.lastReadyCount) {
                        job.lastReadyCount = readyRequested;
                        job.lastProgressAt = now;
                    }
                    if (now - job.lastLogAt >= WALL_JOB_LOG_INTERVAL_TICKS) {
                        job.lastLogAt = now;
                        WitcheryMod.LOGGER.info("[VillageWalls] Waiting for requested wall chunk batch at {}: {}/{} requested ready ({} total).",
                                job.start.getBoundingBox(), readyRequested, job.ticketCursor, job.chunks.size());
                    }
                    if (job.ticketChunks && now - job.lastProgressAt > WALL_JOB_STUCK_CHUNK_SKIP_TICKS
                            && dropFirstMissingRequestedChunk(level, job, 0, job.ticketCursor, now,
                            "requested batch")) {
                        return false;
                    }
                    if (now - job.lastProgressAt > WALL_JOB_TIMEOUT_TICKS) {
                        WitcheryMod.LOGGER.error("[VillageWalls] Timed out loading requested wall chunk batch at {}: {}/{} ready.",
                                job.start.getBoundingBox(), readyRequested, job.ticketCursor);
                        finishWallJob(level, job, false);
                        return true;
                    }
                    return false;
                }

                int end = Math.min(job.ticketCursor + WALL_JOB_CHUNK_TICKET_BATCH, job.chunks.size());
                while (job.ticketCursor < end) {
                    ChunkPos cp = job.chunks.get(job.ticketCursor);
                    debugLog("Requesting forced wall chunk {}/{} {} for {} (reason={}).",
                            job.ticketCursor + 1, job.chunks.size(), cp, job.start.getBoundingBox(), job.reason);
                    level.getChunkSource().updateChunkForced(cp, true);
                    job.ticketCursor++;
                    debugLog("Requested forced wall chunk {}/{} {} for {}.",
                            job.ticketCursor, job.chunks.size(), cp, job.start.getBoundingBox());
                }
                job.lastProgressAt = now;
                job.lastReadyCount = readyRequested;

                if (job.ticketCursor < job.chunks.size()) {
                    if (now - job.lastLogAt >= WALL_JOB_LOG_INTERVAL_TICKS) {
                        job.lastLogAt = now;
                        WitcheryMod.LOGGER.info("[VillageWalls] Requested {}/{} wall chunk ticket(s) for {}.",
                                job.ticketCursor, job.chunks.size(), job.start.getBoundingBox());
                    }
                    return false;
                }

                job.ticketsAdded = true;
                job.lastProgressAt = now;
                job.lastLogAt = now;
                WitcheryMod.LOGGER.info("[VillageWalls] Requested all {} wall chunk ticket(s) for {}; waiting for final readiness.",
                        job.chunks.size(), job.start.getBoundingBox());
                return false;
            }

            int ready = readyChunkCount(level, job.chunks);
            if (ready < job.chunks.size()) {
                if (ready > job.lastReadyCount) {
                    job.lastReadyCount = ready;
                    job.lastProgressAt = now;
                }
                if (now - job.lastLogAt >= WALL_JOB_LOG_INTERVAL_TICKS) {
                    job.lastLogAt = now;
                    WitcheryMod.LOGGER.info("[VillageWalls] Waiting for wall chunks at {}: {}/{} ready.",
                            job.start.getBoundingBox(), ready, job.chunks.size());
                }
                if (job.ticketChunks && now - job.lastProgressAt > WALL_JOB_STUCK_CHUNK_SKIP_TICKS
                        && dropFirstMissingRequestedChunk(level, job, 0, job.chunks.size(), now,
                        "final readiness")) {
                    return false;
                }
                if (job.lastProgressAt > 0 && now - job.lastProgressAt > WALL_JOB_TIMEOUT_TICKS) {
                    WitcheryMod.LOGGER.error("[VillageWalls] Timed out waiting for wall chunks at {}: {}/{} ready.",
                            job.start.getBoundingBox(), ready, job.chunks.size());
                    finishWallJob(level, job, false);
                    return true;
                }
                return false;
            }

            VillageWallData data = VillageWallData.get(level);
            if (!job.force && data.isProcessed(job.start.getBoundingBox())) {
                finishWallJob(level, job, true);
                return true;
            }

            WitcheryMod.LOGGER.info("[VillageWalls] Generating walls ({}) for village at {} ({} pieces, {} chunks ready).",
                    job.reason, job.start.getBoundingBox(), job.start.getPieces().size(), job.chunks.size());
            debugLog("Starting wall generator reason={} bb={} chunks={} teleports={} processedBefore={}.",
                    job.reason, job.start.getBoundingBox(), job.chunks.size(), job.teleports.size(),
                    data.isProcessed(job.start.getBoundingBox()));
            VillageWallGenerator.generate(level, job.start);
            data.markProcessed(job.start.getBoundingBox());
            WitcheryMod.LOGGER.info("[VillageWalls] Wall generation complete for {}.",
                    job.start.getBoundingBox());
            finishWallJob(level, job, true);
            return true;
        } catch (Exception e) {
            WitcheryMod.LOGGER.error("[VillageWalls] Wall generation failed for {}",
                    job.start.getBoundingBox(), e);
            finishWallJob(level, job, false);
            return true;
        }
    }

    private static int readyChunkCount(ServerLevel level, List<ChunkPos> chunks) {
        return readyChunkCount(level, chunks, 0, chunks.size());
    }

    private static int readyChunkCount(ServerLevel level, List<ChunkPos> chunks, int from, int to) {
        int ready = 0;
        int end = Math.min(to, chunks.size());
        for (int i = Math.max(0, from); i < end; i++) {
            ChunkPos cp = chunks.get(i);
            if (level.getChunkSource().getChunkNow(cp.x, cp.z) != null) ready++;
        }
        return ready;
    }

    private static boolean dropFirstMissingRequestedChunk(ServerLevel level, WallJob job, int from, int to,
                                                          long now, String stage) {
        int end = Math.min(to, job.chunks.size());
        for (int i = Math.max(0, from); i < end; i++) {
            ChunkPos cp = job.chunks.get(i);
            if (level.getChunkSource().getChunkNow(cp.x, cp.z) != null) continue;

            level.getChunkSource().updateChunkForced(cp, false);
            job.chunks.remove(i);
            if (i < job.ticketCursor) job.ticketCursor--;
            job.lastProgressAt = now;
            job.lastLogAt = now;
            job.lastReadyCount = readyChunkCount(level, job.chunks, 0, job.ticketCursor);
            WitcheryMod.LOGGER.warn("[VillageWalls] Dropped stuck wall chunk {} during {} for {}; continuing with {} chunk(s).",
                    cp, stage, job.start.getBoundingBox(), job.chunks.size());
            return true;
        }
        return false;
    }

    private static boolean queueAutomaticGeneration(ServerLevel level, PendingWall pending, StructureStart start) {
        if (VillageWallData.get(level).isProcessed(start.getBoundingBox())) {
            return true;
        }
        List<ChunkPos> chunks = wallJobChunks(start);
        int ready = readyChunkCount(level, chunks);
        if (ready < chunks.size()) {
            debugLog("Automatic village wall footprint not ready for {}: {}/{} loaded; retry attempt {}.",
                    start.getBoundingBox(), ready, chunks.size(), pending.attempts() + 1);
            retryPendingForChunks(level, pending, start, ready, chunks.size());
            return false;
        }
        debugLog("Automatic village wall footprint ready for {}: {}/{} loaded; queueing generation.",
                start.getBoundingBox(), ready, chunks.size());
        return queueGeneration(level, start, false, "queue", null, false);
    }

    private static void retryPendingForChunks(ServerLevel level, PendingWall pending, StructureStart start,
                                              int ready, int total) {
        int nextAttempt = pending.attempts() + 1;
        BoundingBox bb = start.getBoundingBox();
        if (nextAttempt >= WALL_CHUNK_READINESS_MAX_ATTEMPTS) {
            WitcheryMod.LOGGER.info("[VillageWalls] Gave up waiting for naturally loaded wall chunks at {}: {}/{} ready.",
                    bb, ready, total);
            return;
        }
        QUEUE.add(new PendingWall(start, level.getGameTime() + WALL_GENERATION_RETRY_TICKS, nextAttempt));
        if (nextAttempt == 1 || nextAttempt % 10 == 0) {
            WitcheryMod.LOGGER.info("[VillageWalls] Waiting for naturally loaded wall chunks at {}: {}/{} ready (retry {}/{}).",
                    bb, ready, total, nextAttempt, WALL_CHUNK_READINESS_MAX_ATTEMPTS);
        }
    }

    private static void finishWallJob(ServerLevel level, WallJob job, boolean success) {
        for (PendingTeleport teleport : job.teleports) {
            completeTeleport(level, teleport, success);
        }
        int ticketed = Math.min(job.ticketCursor, job.chunks.size());
        for (int i = 0; i < ticketed; i++) {
            ChunkPos cp = job.chunks.get(i);
            level.getChunkSource().updateChunkForced(cp, false);
        }
    }

    private static void completeTeleport(ServerLevel level, PendingTeleport teleport, boolean wallsReady) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(teleport.playerId());
        if (player == null) return;

        if (!wallsReady) {
            player.sendSystemMessage(Component.literal(
                    "The hidden paths could not prepare that village safely. Try again in a moment.")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            return;
        }

        BlockPos target = teleport.target();
        int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                target.getX(), target.getZ());
        if (surfaceY < 20) {
            surfaceY = Math.max(target.getY() + 3, 20);
        }

        player.teleportTo(level, target.getX() + 0.5, surfaceY, target.getZ() + 0.5,
                java.util.Set.of(), teleport.yRot(), teleport.xRot(), false);
        player.setDeltaMovement(0.0, 0.0, 0.0);
        player.fallDistance = 0.0F;

        int dist = (int) Math.sqrt(target.distSqr(teleport.from()));
        player.sendSystemMessage(Component.literal(
                "The hidden paths carry you away, across " + dist + " blocks, to "
                + target.getX() + " ~ " + target.getZ() + ".").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static List<ChunkPos> wallJobChunks(StructureStart start) {
        return wallJobChunks(start, FORCE_LOAD_PADDING_BLOCKS);
    }

    private static List<ChunkPos> wallJobChunks(StructureStart start, int pad) {
        BoundingBox bb = start.getBoundingBox();
        int minCX = (bb.minX() - pad) >> 4, maxCX = (bb.maxX() + pad) >> 4;
        int minCZ = (bb.minZ() - pad) >> 4, maxCZ = (bb.maxZ() + pad) >> 4;
        LinkedHashSet<ChunkPos> chunks = new LinkedHashSet<>();
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                chunks.add(new ChunkPos(cx, cz));
            }
        }
        return new ArrayList<>(chunks);
    }

    private static List<ChunkPos> landingPrepChunks(BlockPos target) {
        ChunkPos center = new ChunkPos(target);
        LinkedHashSet<ChunkPos> chunks = new LinkedHashSet<>();
        for (int radius = 0; radius <= VILLAGE_GOTO_LANDING_CHUNK_RADIUS; radius++) {
            for (int dz = -radius; dz <= radius; dz++) {
                boolean zEdge = dz == -radius || dz == radius;
                for (int dx = -radius; dx <= radius; dx++) {
                    boolean xEdge = dx == -radius || dx == radius;
                    if (!zEdge && !xEdge) continue;
                    chunks.add(new ChunkPos(center.x + dx, center.z + dz));
                }
            }
        }
        return new ArrayList<>(chunks);
    }

    private static List<ChunkPos> gotoWallPrepChunks(StructureStart start, BlockPos target) {
        List<ChunkPos> chunks = wallJobChunks(start);
        ChunkPos focus = new ChunkPos(target);
        chunks.sort(Comparator
                .comparingLong((ChunkPos cp) -> chunkDistanceSqr(cp, focus))
                .thenComparingInt(cp -> cp.x)
                .thenComparingInt(cp -> cp.z));
        return chunks;
    }

    private static List<ChunkPos> wallJobTicketChunks(StructureStart start, BlockPos focus) {
        List<ChunkPos> chunks = wallJobChunks(start, FORCE_LOAD_TICKET_PADDING_BLOCKS);
        ChunkPos focusChunk = new ChunkPos(focus);
        chunks.sort(Comparator
                .comparingLong((ChunkPos cp) -> chunkDistanceSqr(cp, focusChunk))
                .thenComparingInt(cp -> cp.x)
                .thenComparingInt(cp -> cp.z));
        if (chunks.size() <= WALL_JOB_FORCED_CHUNK_CAP) {
            return chunks;
        }
        List<ChunkPos> capped = new ArrayList<>(chunks.subList(0, WALL_JOB_FORCED_CHUNK_CAP));
        WitcheryMod.LOGGER.info("[VillageWalls] Limiting forced wall chunk tickets for {} from {} to {} nearest chunks.",
                start.getBoundingBox(), chunks.size(), capped.size());
        return capped;
    }

    private static long chunkDistanceSqr(ChunkPos a, ChunkPos b) {
        long dx = a.x - (long) b.x;
        long dz = a.z - (long) b.z;
        return dx * dx + dz * dz;
    }

    private static BlockPos boundingBoxCenter(BoundingBox bb) {
        return new BlockPos((bb.minX() + bb.maxX()) / 2, 64, (bb.minZ() + bb.maxZ()) / 2);
    }

    // ── Chunk force-loading ────────────────────────────────────────────────────

    /**
     * Legacy helper retained for diagnostics. It now only counts already loaded
     * perimeter chunks so passive wall discovery cannot generate chunks here.
     */
    public static void forceLoadVillageChunks(ServerLevel level, StructureStart start) {
        BoundingBox bb = start.getBoundingBox();
        int pad = FORCE_LOAD_PADDING_BLOCKS;
        int minCX = (bb.minX() - pad) >> 4, maxCX = (bb.maxX() + pad) >> 4;
        int minCZ = (bb.minZ() - pad) >> 4, maxCZ = (bb.maxZ() + pad) >> 4;
        // Only force-load the PERIMETER ring of chunks. The orthogonal-convex-hull wall lives
        // on the village's outer boundary, so the interior chunks never hold a wall block —
        // loading them all was the heavy part when several gotos were chained. Road
        // detection can use whatever chunks are already loaded around the player.
        int count = 0;
        for (int cx = minCX; cx <= maxCX; cx++)
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                boolean nearEdge = (cx - minCX) < FORCE_LOAD_EDGE_RING_CHUNKS
                                || (maxCX - cx) < FORCE_LOAD_EDGE_RING_CHUNKS
                                || (cz - minCZ) < FORCE_LOAD_EDGE_RING_CHUNKS
                                || (maxCZ - cz) < FORCE_LOAD_EDGE_RING_CHUNKS;
                if (!nearEdge) continue; // skip interior — no wall there
                if (level.getChunkSource().getChunkNow(cx, cz) != null) {
                    count++;
                }
            }
        WitcheryMod.LOGGER.info("[VillageWalls] Found {} loaded perimeter chunks for village at {}.", count, bb);
    }

    // ── Village structure identification ──────────────────────────────────────

    /**
     * Attempts to find a village StructureStart at or near pos from chunks that
     * are already resident. Automatic wall discovery must never generate extra
     * chunks just to find a referenced village start; that was enough to stall
     * fresh-world exploration when several villages were discovered together.
     */
    public static StructureStart findVillageStartNear(ServerLevel level, BlockPos pos) {
        BlockPos probe = new BlockPos(pos.getX(), 64, pos.getZ());
        StructureStart referenced = findVillageStartFromReferences(level, probe);
        if (referenced != null) return referenced;
        if (pos.getY() != probe.getY()) {
            referenced = findVillageStartFromReferences(level, pos);
            if (referenced != null) return referenced;
        }
        return findVillageStartInNearbyChunks(level, pos, 3);
    }

    private static StructureStart findVillageStartFromReferences(ServerLevel level, BlockPos pos) {
        Map<Structure, LongSet> refs = level.structureManager().getAllStructuresAt(pos);
        if (refs.isEmpty()) return null;
        debugLog("Loaded-only reference lookup at {} found {} structure reference entrie(s).", pos, refs.size());
        for (Map.Entry<Structure, LongSet> entry : refs.entrySet()) {
            if (!isVillageStructure(entry.getKey(), level)) continue;
            Structure struct = entry.getKey();
            for (long packed : entry.getValue()) {
                ChunkPos cp = new ChunkPos(packed);
                LevelChunk chunk = level.getChunkSource().getChunkNow(cp.x, cp.z);
                if (chunk == null) {
                    debugLog("Referenced village start chunk {} for lookup at {} is not loaded; skipping sync load.",
                            cp, pos);
                    continue;
                }
                StructureStart start = chunk.getAllStarts().get(struct);
                if (start != null && start.isValid()) {
                    debugLog("Loaded-only reference lookup at {} resolved {} with {} piece(s).",
                            pos, start.getBoundingBox(), start.getPieces().size());
                    return start;
                }
                debugLog("Referenced village start chunk {} was loaded but had no valid start for lookup at {}.",
                        cp, pos);
            }
        }
        return null;
    }

    private static StructureStart findVillageStartInNearbyChunks(ServerLevel level, BlockPos pos, int radius) {
        ChunkPos center = new ChunkPos(pos);
        StructureStart best = null;
        long bestDist = Long.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(center.x + dx, center.z + dz);
                if (chunk == null) continue;
                for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
                    StructureStart start = entry.getValue();
                    if (!start.isValid()) continue;
                    if (!isVillageStructure(entry.getKey(), level)) continue;
                    BoundingBox bb = start.getBoundingBox();
                    if (containsXZ(bb, pos)) return start;
                    long dist = distToCenterSqr(bb, pos);
                    if (dist < bestDist) {
                        best = start;
                        bestDist = dist;
                    }
                }
            }
        }
        return best;
    }

    private static boolean containsXZ(BoundingBox bb, BlockPos pos) {
        return pos.getX() >= bb.minX() && pos.getX() <= bb.maxX()
                && pos.getZ() >= bb.minZ() && pos.getZ() <= bb.maxZ();
    }

    private static long distToCenterSqr(BoundingBox bb, BlockPos pos) {
        long dx = pos.getX() - (long) ((bb.minX() + bb.maxX()) / 2);
        long dz = pos.getZ() - (long) ((bb.minZ() + bb.maxZ()) / 2);
        return dx * dx + dz * dz;
    }

    // ── Village structure key check ───────────────────────────────────────────

    public static boolean isVillageStructure(Structure structure, ServerLevel level) {
        // listElements() iteration with identity comparison is correct:
        // registry-managed structure instances are the same Java object per key.
        var opt = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                .listElements()
                .filter(h -> h.value() == structure)
                .findFirst();
        if (opt.isEmpty()) return false;
        ResourceKey<Structure> k = opt.get().key();
        return isVillageId(k.identifier().getNamespace(), k.identifier().getPath());
    }
}
