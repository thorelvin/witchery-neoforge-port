package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.world.VillageWallData;
import com.emoniph.witchery.world.WitcheryStructureData;
import com.emoniph.witchery.worldgen.VillageWallGenerator;
import com.emoniph.witchery.worldgen.WitcheryVillageElement;
import com.emoniph.witchery.worldgen.WitchWellPiece;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
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
                                   float yRot, float xRot, String namespace, String structureId,
                                   String targetBuilding) {}
    private record LocateCandidate(Holder<Structure> holder, ChunkPos chunk, StructurePlacement placement,
                                   double distanceSqr) {}
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
        int generationAttempts;
        long nextGenerateAt;

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
        final String targetBuilding;
        final ResourceKey<Biome> targetBiome;
        final boolean generateVillageWalls;
        final boolean allowChunkGeneration;
        final WitchWellPiece.Variant targetWellVariant;
        final List<LocateCandidate> candidates;
        final long queuedAt;
        int cursor;
        long lastLogAt;

        VillageGotoLocateJob(UUID playerId, BlockPos from, float yRot, float xRot,
                             String namespace, String structureId, String targetBuilding,
                             ResourceKey<Biome> targetBiome,
                             boolean generateVillageWalls,
                             boolean allowChunkGeneration,
                             WitchWellPiece.Variant targetWellVariant,
                             List<LocateCandidate> candidates, long queuedAt) {
            this.playerId = playerId;
            this.from = from;
            this.yRot = yRot;
            this.xRot = xRot;
            this.namespace = namespace;
            this.structureId = structureId;
            this.targetBuilding = targetBuilding;
            this.targetBiome = targetBiome;
            this.generateVillageWalls = generateVillageWalls;
            this.allowChunkGeneration = allowChunkGeneration;
            this.targetWellVariant = targetWellVariant;
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
        int ticketCursor;
        long lastLogAt;
        int wallAttempts;
        long nextWallAttemptAt;
        boolean loggedAllChunksReady;

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
    private static final int STRUCTURE_GOTO_LOADED_CANDIDATES_PER_TICK = 32;
    private static final long VILLAGE_GOTO_LOCATE_LOG_INTERVAL_TICKS = 40L;
    private static final int VILLAGE_GOTO_PREP_CHUNKS_PER_TICK = 1;
    private static final int VILLAGE_GOTO_LANDING_CHUNK_RADIUS = 2;
    private static final int STRUCTURE_GOTO_LANDING_CHUNK_RADIUS = 0;
    private static final long VILLAGE_GOTO_PREP_LOG_INTERVAL_TICKS = 40L;
    private static final long VILLAGE_GOTO_PREP_TIMEOUT_TICKS = 20L * 150L;
    private static final long VILLAGE_GOTO_WALL_RETRY_TICKS = 40L;
    private static final int VILLAGE_GOTO_WALL_MAX_ATTEMPTS = 18;
    private static final long WALL_GENERATION_RETRY_AFTER_NO_GATES_TICKS = 40L;
    private static final int WALL_GENERATION_NO_GATE_MAX_ATTEMPTS = 6;
    // 1.21.11 does not expose custom ticket registration to mods. Use a transient
    // FORCED-type radius ticket here, paired with removeTicketWithRadius before
    // the prep job exits; this does not call updateChunkForced or persist chunks.
    private static final TicketType VILLAGE_GOTO_PREP_TICKET = TicketType.FORCED;
    private static long lastQueueDebugAt = Long.MIN_VALUE;

    static final List<String> VILLAGE_IDS = List.of(
            "village_plains", "village_savanna", "village_taiga",
            "village_snowy", "village_desert", "village_meadow");

    public static boolean isVillageId(String namespace, String path) {
        return "minecraft".equals(namespace) && VILLAGE_IDS.contains(path);
    }

    private static boolean debugEnabled() {
        return WitcheryConfig.DEBUG.get();
    }

    private static void debugLog(String message, Object... args) {
        if (debugEnabled()) {
            WitcheryMod.LOGGER.debug("[VillageWalls:Debug] " + message, args);
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
            recordKnownWitcheryStructureStart(level, entry.getKey(), start);
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
        return queueVillageGoto(level, player, target, from, namespace, structureId, start, null);
    }

    private static VillageGotoResult queueVillageGoto(ServerLevel level, ServerPlayer player, BlockPos target,
                                                      BlockPos from, String namespace, String structureId,
                                                      StructureStart start, String targetBuilding) {
        if (start == null || !start.isValid() || start.getPieces().isEmpty()) {
            WitcheryMod.LOGGER.info("[VillageWalls] No ready village start near {} for goto wall queue.", target);
            return VillageGotoResult.NOT_READY;
        }
        PendingTeleport teleport = new PendingTeleport(player.getUUID(), target, from,
                player.getYRot(), player.getXRot(), namespace, structureId, targetBuilding);
        boolean buildingTarget = targetBuilding != null;
        boolean processed = VillageWallData.get(level).isProcessed(start.getBoundingBox());
        boolean generateWalls = !processed;
        List<ChunkPos> chunks = !generateWalls
                ? landingPrepChunks(target)
                : gotoWallPrepChunks(start, target);
        GOTO_PREPS.removeIf(job -> {
            boolean samePlayer = job.teleport.playerId().equals(player.getUUID());
            if (samePlayer) {
                releaseVillageGotoPrepTickets(level, job);
            }
            return samePlayer;
        });
        GOTO_PREPS.add(new VillageGotoPrepJob(start, teleport, chunks, generateWalls, level.getGameTime()));
        WitcheryMod.LOGGER.info("[Goto] Queued village arrival prep for {}:{} at {} with {} chunk(s){}.",
                namespace, structureId, target, chunks.size(),
                generateWalls ? " and wall generation" : buildingTarget ? " (known-village building landing)" : "");
        player.sendSystemMessage(Component.literal(
                buildingTarget
                        ? "The hidden paths find the village " + structureId + ". Hold still while "
                        + (generateWalls ? "its roads and walls settle." : "the doorway stones settle.")
                : processed
                        ? "The hidden paths find a known village. Hold still while the arrival stones settle."
                        : "The hidden paths find a village. Hold still while its walls rise before the crossing.")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return VillageGotoResult.PREPARING;
    }

    public static boolean queueVillageGotoLocate(ServerLevel level, ServerPlayer player, Holder<Structure> holder,
                                                 BlockPos from, String namespace, String structureId,
                                                 int searchRadius) {
        return queueVillageGotoLocate(level, player, holder, from, namespace, structureId, searchRadius, null);
    }

    public static boolean queueVillageGotoLocate(ServerLevel level, ServerPlayer player, Holder<Structure> holder,
                                                 BlockPos from, String namespace, String structureId,
                                                 int searchRadius, ResourceKey<Biome> targetBiome) {
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
                namespace, structureId, null, targetBiome, true, true, null, candidates, level.getGameTime()));
        WitcheryMod.LOGGER.info("[Goto] Queued safe village locate for {}:{} from {} with {} candidate chunk(s){}.",
                namespace, structureId, from, candidates.size(),
                targetBiome != null ? " filtered to " + targetBiome.identifier() : "");
        return true;
    }

    public static boolean queueStructureGotoLocate(ServerLevel level, ServerPlayer player, Holder<Structure> holder,
                                                   BlockPos from, String namespace, String structureId,
                                                   int searchRadius) {
        return queueStructureGotoLocate(level, player, holder, from, namespace, structureId, searchRadius, null);
    }

    public static boolean queueStructureGotoLocate(ServerLevel level, ServerPlayer player, Holder<Structure> holder,
                                                   BlockPos from, String namespace, String structureId,
                                                   int searchRadius, WitchWellPiece.Variant targetWellVariant) {
        List<LocateCandidate> candidates = villageGotoLocateCandidates(level, holder, from, searchRadius);
        if (candidates.isEmpty()) {
            WitcheryMod.LOGGER.info("[Goto] No safe locate candidates for {}:{} from {}.",
                    namespace, structureId, from);
            return false;
        }

        UUID playerId = player.getUUID();
        GOTO_LOCATES.removeIf(job -> job.playerId.equals(playerId));
        GOTO_LOCATES.add(new VillageGotoLocateJob(playerId, from, player.getYRot(), player.getXRot(),
                namespace, structureId, null, null, false, false, targetWellVariant,
                candidates, level.getGameTime()));
        WitcheryMod.LOGGER.info("[Goto] Queued safe structure locate for {}:{} from {} with {} candidate chunk(s){}; unloaded candidates will be skipped.",
                namespace, structureId, from, candidates.size(),
                targetWellVariant != null ? " filtered to witch well " + targetWellVariant.name().toLowerCase(java.util.Locale.ROOT) : "");
        return true;
    }

    public static boolean queueKnownStructureGoto(ServerLevel level, ServerPlayer player, BlockPos target,
                                                  BlockPos from, String namespace, String structureId) {
        PendingTeleport teleport = new PendingTeleport(player.getUUID(), target, from,
                player.getYRot(), player.getXRot(), namespace, structureId, null);
        List<ChunkPos> chunks = structureLandingPrepChunks(target);
        GOTO_PREPS.removeIf(prep -> {
            boolean samePlayer = prep.teleport.playerId().equals(player.getUUID());
            if (samePlayer) {
                releaseVillageGotoPrepTickets(level, prep);
            }
            return samePlayer;
        });
        GOTO_PREPS.add(new VillageGotoPrepJob(null, teleport, chunks, false, level.getGameTime()));
        WitcheryMod.LOGGER.info("[Goto] Queued indexed structure arrival prep for {}:{} at {} with {} chunk(s).",
                namespace, structureId, target, chunks.size());
        return true;
    }

    public static boolean queueVillageBuildingGotoLocate(ServerLevel level, ServerPlayer player,
                                                         BlockPos from, String building, int searchRadius) {
        List<Holder<Structure>> holders = villageStructureHolders(level);
        if (holders.isEmpty()) {
            WitcheryMod.LOGGER.info("[Goto] No village structure holders available for building search {}.", building);
            return false;
        }

        List<LocateCandidate> candidates = new ArrayList<>();
        for (Holder<Structure> holder : holders) {
            candidates.addAll(villageGotoLocateCandidates(level, holder, from, searchRadius));
        }
        if (candidates.isEmpty()) {
            WitcheryMod.LOGGER.info("[Goto] No safe village candidates for building {} from {}.", building, from);
            return false;
        }
        candidates.sort(Comparator.comparingDouble(LocateCandidate::distanceSqr));

        UUID playerId = player.getUUID();
        GOTO_LOCATES.removeIf(job -> job.playerId.equals(playerId));
        GOTO_LOCATES.add(new VillageGotoLocateJob(playerId, from, player.getYRot(), player.getXRot(),
                WitcheryMod.MODID, building, building, null, true, true, null, candidates, level.getGameTime()));
        WitcheryMod.LOGGER.info("[Goto] Queued safe village-building locate for {} from {} with {} candidate chunk(s).",
                building, from, candidates.size());
        return true;
    }

    private static List<Holder<Structure>> villageStructureHolders(ServerLevel level) {
        List<Holder<Structure>> holders = new ArrayList<>();
        level.registryAccess().lookup(Registries.STRUCTURE).ifPresent(registry -> {
            for (String villageId : VILLAGE_IDS) {
                ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE,
                        Identifier.withDefaultNamespace(villageId));
                registry.get(key).ifPresent(holders::add);
            }
        });
        return holders;
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

        int maxChecks = job.allowChunkGeneration
                ? VILLAGE_GOTO_LOCATE_CHUNKS_PER_TICK
                : STRUCTURE_GOTO_LOADED_CANDIDATES_PER_TICK;
        int checked = 0;
        while (job.cursor < job.candidates.size() && checked < maxChecks) {
            LocateCandidate candidate = job.candidates.get(job.cursor++);
            checked++;
            debugLog("Probing goto candidate {}/{} for {}:{} from {}: chunk={}, distanceSqr={}.",
                    job.cursor, job.candidates.size(), job.namespace, job.structureId, job.from,
                    candidate.chunk(), candidate.distanceSqr());

            VillageGotoFound found = probeVillageGotoCandidate(level, candidate, job.allowChunkGeneration);
            if (found == null) continue;
            if (job.targetWellVariant != null && !containsWitchWellVariant(found.start(), job.targetWellVariant)) {
                debugLog("Goto candidate structure {} rejected for witch well variant {}.",
                        found.start().getBoundingBox(), job.targetWellVariant.name().toLowerCase(java.util.Locale.ROOT));
                continue;
            }
            if (job.targetBiome != null && !matchesTargetBiome(level, found.start(), found.target(), job.targetBiome)) {
                debugLog("Goto candidate village {} rejected for biome filter {} at {}.",
                        found.start().getBoundingBox(), job.targetBiome.identifier(), found.target());
                continue;
            }
            if (job.targetBuilding != null) {
                BlockPos buildingTarget = villageBuildingAnchor(found.start(), job.targetBuilding);
                if (buildingTarget == null) {
                    debugLog("Goto candidate village {} does not contain requested building {}.",
                            found.start().getBoundingBox(), job.targetBuilding);
                    continue;
                }
                found = new VillageGotoFound(buildingTarget, found.start());
            }

            WitcheryMod.LOGGER.info("[Goto] Safe {} locate found {}:{} at {} after {}/{} probe(s).",
                    job.generateVillageWalls ? "village" : "structure",
                    job.namespace, job.structureId, found.target(), job.cursor, job.candidates.size());

            if (!job.generateVillageWalls) {
                queueStructureGotoPrep(level, job, found);
                return true;
            }

            VillageGotoResult wallResult = queueVillageGoto(level, player, found.target(), job.from,
                    job.namespace, job.structureId, found.start(), job.targetBuilding);
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
            String message = !job.generateVillageWalls
                    ? job.targetWellVariant == WitchWellPiece.Variant.CAVE
                    ? "The scrying mists reveal no cursed well already known nearby. Wander further and gaze again."
                    : "The scrying mists reveal no matching structure already known nearby. Wander further and gaze again."
                    : job.targetBuilding == null
                    ? "The scrying mists reveal no matching village nearby within the safe range. Wander further and gaze again."
                    : "The scrying mists reveal no village " + job.targetBuilding
                    + " nearby within the safe range. Wander further and gaze again.";
            player.sendSystemMessage(Component.literal(message)
                    .withStyle(ChatFormatting.DARK_PURPLE));
            WitcheryMod.LOGGER.info("[Goto] Safe {} locate found no {}:{} from {} after {} probe(s){}.",
                    job.generateVillageWalls ? "village" : "structure",
                    job.namespace, job.structureId, job.from, job.candidates.size(),
                    job.allowChunkGeneration ? "" : " using loaded chunks only");
            return true;
        }

        if (now - job.lastLogAt >= VILLAGE_GOTO_LOCATE_LOG_INTERVAL_TICKS) {
            job.lastLogAt = now;
            WitcheryMod.LOGGER.info("[Goto] Safe {} locate still probing {}:{} from {}: {}/{} candidate chunk(s).",
                    job.generateVillageWalls ? "village" : "structure",
                    job.namespace, job.structureId, job.from, job.cursor, job.candidates.size());
        }
        return false;
    }

    private static boolean containsWitchWellVariant(StructureStart start, WitchWellPiece.Variant variant) {
        for (net.minecraft.world.level.levelgen.structure.StructurePiece piece : start.getPieces()) {
            if (piece instanceof WitchWellPiece wellPiece && wellPiece.variant() == variant) {
                return true;
            }
        }
        return false;
    }

    private static void recordKnownWitcheryStructureStart(ServerLevel level, Structure structure,
                                                          StructureStart start) {
        Identifier id = structureIdentifier(structure, level);
        if (id == null || !WitcheryStructureData.WITCH_WELL.equals(id.toString())) {
            return;
        }

        for (net.minecraft.world.level.levelgen.structure.StructurePiece piece : start.getPieces()) {
            if (piece instanceof WitchWellPiece wellPiece) {
                WitcheryStructureData.get(level).record(level, WitcheryStructureData.WITCH_WELL,
                        wellPiece.variant().serializedName(), wellPiece.indexedTarget());
            }
        }
    }

    private static void queueStructureGotoPrep(ServerLevel level, VillageGotoLocateJob job, VillageGotoFound found) {
        PendingTeleport teleport = new PendingTeleport(job.playerId, found.target(), job.from,
                job.yRot, job.xRot, job.namespace, job.structureId, null);
        List<ChunkPos> chunks = structureLandingPrepChunks(found.target());
        GOTO_PREPS.removeIf(prep -> prep.teleport.playerId().equals(job.playerId));
        GOTO_PREPS.add(new VillageGotoPrepJob(found.start(), teleport, chunks, false, level.getGameTime()));
        WitcheryMod.LOGGER.info("[Goto] Queued safe structure arrival prep for {}:{} at {} with {} chunk(s).",
                job.namespace, job.structureId, found.target(), chunks.size());
    }

    private static boolean matchesTargetBiome(ServerLevel level, StructureStart start, BlockPos target,
                                              ResourceKey<Biome> targetBiome) {
        int y = Math.clamp(start.getBoundingBox().minY() + 1, level.getMinY(), level.getMaxY() - 1);
        return level.getBiome(new BlockPos(target.getX(), y, target.getZ())).is(targetBiome);
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
                        candidates.add(new LocateCandidate(holder, chunk, placement, locatePos.distSqr(from)));
                    }
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(LocateCandidate::distanceSqr));
        return candidates;
    }

    private static VillageGotoFound probeVillageGotoCandidate(ServerLevel level, LocateCandidate candidate,
                                                              boolean allowChunkGeneration) {
        if (!allowChunkGeneration) {
            return probeLoadedGotoCandidate(level, candidate);
        }

        debugLog("Checking structure presence for goto candidate chunk {}.", candidate.chunk());
        StructureCheckResult result = level.structureManager().checkStructurePresence(
                candidate.chunk(), candidate.holder().value(), candidate.placement(), false);
        debugLog("Structure presence for goto candidate chunk {} -> {}.", candidate.chunk(), result);
        if (result == StructureCheckResult.START_NOT_PRESENT) {
            return null;
        }

        debugLog("Loading/generating STRUCTURE_STARTS for goto candidate chunk {}.", candidate.chunk());
        ChunkAccess chunk = level.getChunk(candidate.chunk().x, candidate.chunk().z,
                ChunkStatus.STRUCTURE_STARTS, true);
        debugLog("Loaded STRUCTURE_STARTS for goto candidate chunk {}.", candidate.chunk());
        StructureStart start = level.structureManager().getStartForStructure(
                SectionPos.bottomOf(chunk), candidate.holder().value(), chunk);
        if (start != null && start.isValid()) {
            BlockPos target = candidate.placement().getLocatePos(start.getChunkPos());
            debugLog("Goto candidate chunk {} resolved start {} with {} piece(s), target {}.",
                    candidate.chunk(), start.getBoundingBox(), start.getPieces().size(), target);
            return new VillageGotoFound(target, start);
        }
        debugLog("Goto candidate chunk {} had no valid start after STRUCTURE_STARTS load.", candidate.chunk());
        return null;
    }

    private static VillageGotoFound probeLoadedGotoCandidate(ServerLevel level, LocateCandidate candidate) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(candidate.chunk().x, candidate.chunk().z);
        if (chunk == null) {
            debugLog("Skipping goto candidate chunk {} because it is not currently loaded.", candidate.chunk());
            return null;
        }

        StructureStart start = chunk.getAllStarts().get(candidate.holder().value());
        if (start != null && start.isValid()) {
            BlockPos target = candidate.placement().getLocatePos(start.getChunkPos());
            debugLog("Loaded goto candidate chunk {} resolved start {} with {} piece(s), target {}.",
                    candidate.chunk(), start.getBoundingBox(), start.getPieces().size(), target);
            return new VillageGotoFound(target, start);
        }
        debugLog("Loaded goto candidate chunk {} had no valid known start.", candidate.chunk());
        return null;
    }

    private static boolean processVillageGotoPrepJob(ServerLevel level, VillageGotoPrepJob job, long now) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(job.teleport.playerId());
        if (player == null) {
            releaseVillageGotoPrepTickets(level, job);
            return true;
        }

        try {
            int loadedThisTick = 0;
            while (job.cursor < job.chunks.size() && loadedThisTick < VILLAGE_GOTO_PREP_CHUNKS_PER_TICK) {
                int chunkIndex = job.cursor;
                ChunkPos cp = job.chunks.get(job.cursor++);
                level.getChunkSource().addTicketWithRadius(VILLAGE_GOTO_PREP_TICKET, cp, 0);
                job.ticketCursor = job.cursor;
                debugLog("Goto prep loading FULL chunk {}/{} {} for {}:{} target {} (generateWalls={}).",
                        chunkIndex + 1, job.chunks.size(), cp, job.teleport.namespace(),
                        job.teleport.structureId(), job.teleport.target(), job.generateWalls);
                level.getChunk(cp.x, cp.z, ChunkStatus.FULL, true);
                debugLog("Goto prep loaded FULL chunk {}/{} {} for target {}.",
                        chunkIndex + 1, job.chunks.size(), cp, job.teleport.target());
                loadedThisTick++;
            }

            if (now - job.lastLogAt >= VILLAGE_GOTO_PREP_LOG_INTERVAL_TICKS
                    || (job.cursor >= job.chunks.size() && !job.loggedAllChunksReady)) {
                job.lastLogAt = now;
                if (job.cursor >= job.chunks.size()) {
                    job.loggedAllChunksReady = true;
                }
                WitcheryMod.LOGGER.info("[Goto] Preparing {} arrival at {}:{} for {}: {}/{} chunk(s) ready.",
                        job.generateWalls ? "village" : "structure",
                        job.teleport.namespace(), job.teleport.structureId(), job.teleport.target(),
                        Math.min(job.cursor, job.chunks.size()), job.chunks.size());
            }

            if (job.cursor < job.chunks.size()) {
                if (now - job.queuedAt > VILLAGE_GOTO_PREP_TIMEOUT_TICKS) {
                    WitcheryMod.LOGGER.error("[Goto] Timed out preparing village arrival at {} after {}/{} chunk(s).",
                            job.teleport.target(), job.cursor, job.chunks.size());
                    return finishVillageGotoPrep(level, job, false, null);
                }
                return false;
            }

            int ready = readyChunkCount(level, job.chunks);
            if (ready < job.chunks.size()) {
                if (now - job.lastLogAt >= VILLAGE_GOTO_PREP_LOG_INTERVAL_TICKS) {
                    job.lastLogAt = now;
                    WitcheryMod.LOGGER.info("[Goto] Holding {} arrival chunks at {}:{} for {}: {}/{} still resident.",
                            job.generateWalls ? "village" : "structure",
                            job.teleport.namespace(), job.teleport.structureId(), job.teleport.target(),
                            ready, job.chunks.size());
                }
                if (now - job.queuedAt > VILLAGE_GOTO_PREP_TIMEOUT_TICKS) {
                    WitcheryMod.LOGGER.error("[Goto] Timed out holding village arrival at {} after {}/{} chunk(s) stayed loaded.",
                            job.teleport.target(), ready, job.chunks.size());
                    return finishVillageGotoPrep(level, job, false, null);
                }
                return false;
            }

            VillageWallData data = VillageWallData.get(level);
            if (job.generateWalls && !data.isProcessed(job.start.getBoundingBox())) {
                if (job.nextWallAttemptAt > now) {
                    return false;
                }
                StructureStart start = refreshVillageStart(level, job.start);
                if (start == null || !start.isValid() || start.getPieces().isEmpty()) {
                    WitcheryMod.LOGGER.info("[Goto] Village start was not ready after arrival prep at {}.",
                            job.teleport.target());
                    return finishVillageGotoPrep(level, job, false, null);
                }
                job.wallAttempts++;
                WitcheryMod.LOGGER.info("[VillageWalls] Generating walls (goto-prep) for village at {} ({} pieces, {} chunks prepared).",
                        start.getBoundingBox(), start.getPieces().size(), job.chunks.size());
                debugLog("Starting goto-prep wall generation for {} after {} prepared chunk(s), attempt {}/{}.",
                        start.getBoundingBox(), job.chunks.size(), job.wallAttempts, VILLAGE_GOTO_WALL_MAX_ATTEMPTS);
                VillageWallGenerator.GenerationResult result = VillageWallGenerator.generate(level, start);
                if (!result.generated()) {
                    if (job.wallAttempts < VILLAGE_GOTO_WALL_MAX_ATTEMPTS) {
                        job.nextWallAttemptAt = now + VILLAGE_GOTO_WALL_RETRY_TICKS;
                        WitcheryMod.LOGGER.info("[Goto] Village roads are not ready for wall gates at {} ({} candidate(s)); retry {}/{} in {} tick(s).",
                                start.getBoundingBox(), result.gateCandidates(), job.wallAttempts,
                                VILLAGE_GOTO_WALL_MAX_ATTEMPTS, VILLAGE_GOTO_WALL_RETRY_TICKS);
                        return false;
                    }
                    WitcheryMod.LOGGER.warn("[Goto] Gave up preparing village walls at {} after {} attempt(s) with {} gate candidate(s).",
                            start.getBoundingBox(), job.wallAttempts, result.gateCandidates());
                    return finishVillageGotoPrep(level, job, false, null);
                }
                data.markProcessed(start.getBoundingBox());
                WitcheryMod.LOGGER.info("[VillageWalls] Wall generation complete for {}.",
                        start.getBoundingBox());
                return finishVillageGotoPrep(level, job, true, start);
            }

            return finishVillageGotoPrep(level, job, true, job.start);
        } catch (Exception e) {
            WitcheryMod.LOGGER.error("[Goto] Village arrival prep failed for {}",
                    job.teleport.target(), e);
            return finishVillageGotoPrep(level, job, false, null);
        }
    }

    private static boolean finishVillageGotoPrep(ServerLevel level, VillageGotoPrepJob job,
                                                 boolean wallsReady, StructureStart start) {
        try {
            completeTeleport(level, job.teleport, wallsReady, start);
        } finally {
            releaseVillageGotoPrepTickets(level, job);
        }
        return true;
    }

    private static void releaseVillageGotoPrepTickets(ServerLevel level, VillageGotoPrepJob job) {
        int ticketed = Math.min(job.ticketCursor, job.chunks.size());
        for (int i = 0; i < ticketed; i++) {
            ChunkPos cp = job.chunks.get(i);
            level.getChunkSource().removeTicketWithRadius(VILLAGE_GOTO_PREP_TICKET, cp, 0);
        }
        if (ticketed > 0) {
            debugLog("Released {} temporary goto-prep chunk ticket(s) for {}:{} target {}.",
                    ticketed, job.teleport.namespace(), job.teleport.structureId(), job.teleport.target());
            job.ticketCursor = 0;
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
        if (GOTO_PREPS.stream().anyMatch(job -> job.start != null && job.start.getBoundingBox().equals(bb))) {
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
            if (job.nextGenerateAt > now) {
                return false;
            }

            WitcheryMod.LOGGER.info("[VillageWalls] Generating walls ({}) for village at {} ({} pieces, {} chunks ready).",
                    job.reason, job.start.getBoundingBox(), job.start.getPieces().size(), job.chunks.size());
            debugLog("Starting wall generator reason={} bb={} chunks={} teleports={} processedBefore={}.",
                    job.reason, job.start.getBoundingBox(), job.chunks.size(), job.teleports.size(),
                    data.isProcessed(job.start.getBoundingBox()));
            job.generationAttempts++;
            VillageWallGenerator.GenerationResult result = VillageWallGenerator.generate(level, job.start);
            if (!result.generated()) {
                if (job.generationAttempts < WALL_GENERATION_NO_GATE_MAX_ATTEMPTS) {
                    job.nextGenerateAt = now + WALL_GENERATION_RETRY_AFTER_NO_GATES_TICKS;
                    job.lastLogAt = now;
                    WitcheryMod.LOGGER.info("[VillageWalls] Wall roads not ready for {} ({} gate candidate(s)); retry {}/{} in {} tick(s).",
                            job.start.getBoundingBox(), result.gateCandidates(), job.generationAttempts,
                            WALL_GENERATION_NO_GATE_MAX_ATTEMPTS, WALL_GENERATION_RETRY_AFTER_NO_GATES_TICKS);
                    return false;
                }
                WitcheryMod.LOGGER.warn("[VillageWalls] Wall generation for {} did not find gate candidates after {} attempt(s); leaving village unprocessed.",
                        job.start.getBoundingBox(), job.generationAttempts);
                finishWallJob(level, job, false);
                return true;
            }
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
        completeTeleport(level, teleport, wallsReady, null);
    }

    private static void completeTeleport(ServerLevel level, PendingTeleport teleport, boolean wallsReady,
                                         StructureStart start) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(teleport.playerId());
        if (player == null) return;

        if (!wallsReady) {
            player.sendSystemMessage(Component.literal(
                    "The hidden paths could not prepare that village safely. Try again in a moment.")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            return;
        }

        BlockPos target = start != null
                ? villageLandingTarget(level, start, teleport)
                : findClearSurfaceNear(level, teleport.target(), 10);
        int surfaceY = target.getY();
        debugLog("Completing goto teleport for {}:{} from target {} to landing {} (wallsReady={}).",
                teleport.namespace(), teleport.structureId(), teleport.target(), target, wallsReady);

        player.teleportTo(level, target.getX() + 0.5, surfaceY, target.getZ() + 0.5,
                java.util.Set.of(), teleport.yRot(), teleport.xRot(), false);
        player.setDeltaMovement(0.0, 0.0, 0.0);
        player.fallDistance = 0.0F;

        int dist = (int) Math.sqrt(target.distSqr(teleport.from()));
        player.sendSystemMessage(Component.literal(
                "The hidden paths carry you away, across " + dist + " blocks, to "
                + target.getX() + " ~ " + target.getZ() + ".").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static BlockPos villageLandingTarget(ServerLevel level, StructureStart start, PendingTeleport teleport) {
        if (teleport.targetBuilding() != null) {
            BlockPos anchor = villageBuildingAnchor(start, teleport.targetBuilding());
            if (anchor != null) {
                BlockPos target = villageBuildingLandingTarget(level, teleport.targetBuilding(), anchor);
                if (isSafeLandingAt(level, target.getX(), target.getY(), target.getZ())) {
                    return target;
                }
            }
        }

        BlockPos fallback = teleport.target();
        BoundingBox bb = start.getBoundingBox();
        BlockPos bestRoad = null;
        double bestRoadDist = Double.MAX_VALUE;
        for (int x = bb.minX(); x <= bb.maxX(); x++) {
            for (int z = bb.minZ(); z <= bb.maxZ(); z++) {
                if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
                int y = safeSurfaceY(level, x, z, fallback.getY());
                if (!isSafeLandingAt(level, x, y, z)) continue;
                BlockState ground = level.getBlockState(new BlockPos(x, y - 1, z));
                if (!isVillageRoadSurface(ground)) continue;
                double dist = fallback.distSqr(new BlockPos(x, fallback.getY(), z));
                if (dist < bestRoadDist) {
                    bestRoadDist = dist;
                    bestRoad = new BlockPos(x, y, z);
                }
            }
        }
        if (bestRoad != null) return bestRoad;

        BlockPos center = new BlockPos((bb.minX() + bb.maxX()) / 2, fallback.getY(), (bb.minZ() + bb.maxZ()) / 2);
        BlockPos nearCenter = findClearSurfaceNear(level, center, 18);
        if (isSafeLandingAt(level, nearCenter.getX(), nearCenter.getY(), nearCenter.getZ())) {
            return nearCenter;
        }
        return findClearSurfaceNear(level, fallback, 18);
    }

    private static BlockPos villageBuildingAnchor(StructureStart start, String building) {
        BlockPos canonicalOrigin = WitcheryVillageElement.canonicalBuildingOrigin(start, building);
        for (net.minecraft.world.level.levelgen.structure.StructurePiece piece : start.getPieces()) {
            if (!WitcheryVillageElement.isBuildingElement(piece, building)) continue;
            if (canonicalOrigin != null && !WitcheryVillageElement.buildingOrigin(piece).equals(canonicalOrigin)) continue;
            BoundingBox bb = piece.getBoundingBox();
            if ("apothecary".equals(building)) {
                return new BlockPos(bb.minX() + 1, bb.minY() + 1, bb.maxZ() + 1);
            }
            return new BlockPos((bb.minX() + bb.maxX()) / 2, bb.minY() + 1,
                    (bb.minZ() + bb.maxZ()) / 2);
        }
        return null;
    }

    private static BlockPos villageBuildingLandingTarget(ServerLevel level, String building, BlockPos anchor) {
        if ("apothecary".equals(building)) {
            BlockPos road = findVillageRoadLandingNear(level, anchor, 12);
            if (road != null) {
                return road;
            }
        }
        return findClearLandingNearY(level, anchor, 12, 4);
    }

    private static BlockPos findVillageRoadLandingNear(ServerLevel level, BlockPos anchor, int radius) {
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    int x = anchor.getX() + dx;
                    int z = anchor.getZ() + dz;
                    if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
                    int y = safeSurfaceY(level, x, z, anchor.getY());
                    if (!isSafeLandingAt(level, x, y, z)) continue;
                    BlockState ground = level.getBlockState(new BlockPos(x, y - 1, z));
                    if (isVillageRoadSurface(ground)) {
                        return new BlockPos(x, y, z);
                    }
                }
            }
        }
        return null;
    }

    private static BlockPos findClearSurfaceNear(ServerLevel level, BlockPos anchor, int radius) {
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    int x = anchor.getX() + dx;
                    int z = anchor.getZ() + dz;
                    if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
                    int y = safeSurfaceY(level, x, z, anchor.getY());
                    if (isSafeLandingAt(level, x, y, z)) {
                        return new BlockPos(x, y, z);
                    }
                }
            }
        }
        int y = safeSurfaceY(level, anchor.getX(), anchor.getZ(), anchor.getY());
        return new BlockPos(anchor.getX(), y, anchor.getZ());
    }

    private static BlockPos findClearLandingNearY(ServerLevel level, BlockPos anchor, int radius, int verticalRange) {
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    int x = anchor.getX() + dx;
                    int z = anchor.getZ() + dz;
                    if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
                    for (int dy = 0; dy <= verticalRange; dy++) {
                        int up = anchor.getY() + dy;
                        if (isSafeLandingAt(level, x, up, z)) return new BlockPos(x, up, z);
                        int down = anchor.getY() - dy;
                        if (dy != 0 && isSafeLandingAt(level, x, down, z)) return new BlockPos(x, down, z);
                    }
                }
            }
        }
        return findClearSurfaceNear(level, anchor, radius);
    }

    private static int safeSurfaceY(ServerLevel level, int x, int z, int fallbackY) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinY() + 2) {
            y = Math.max(fallbackY + 3, level.getMinY() + 2);
        }
        y = Math.min(Math.max(y, level.getMinY() + 2), level.getMaxY() - 2);

        for (int dy = 0; dy <= 16; dy++) {
            int up = y + dy;
            if (up < level.getMaxY() - 1 && isSafeLandingAt(level, x, up, z)) return up;
            int down = y - dy;
            if (down > level.getMinY() + 1 && isSafeLandingAt(level, x, down, z)) return down;
        }
        return y;
    }

    private static boolean isSafeLandingAt(ServerLevel level, int x, int y, int z) {
        if (y <= level.getMinY() || y >= level.getMaxY() - 1) return false;
        BlockPos feet = new BlockPos(x, y, z);
        BlockPos head = feet.above();
        BlockPos groundPos = feet.below();
        BlockState ground = level.getBlockState(groundPos);
        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(head);
        return !ground.isAir() && !ground.liquid() && ground.isSolid()
                && isOpenForPlayer(feetState) && isOpenForPlayer(headState);
    }

    private static boolean isOpenForPlayer(BlockState state) {
        return state.isAir() || (state.canBeReplaced() && !state.liquid());
    }

    private static boolean isVillageRoadSurface(BlockState state) {
        return state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.SMOOTH_SANDSTONE)
                || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.CUT_SANDSTONE);
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
        return landingPrepChunks(target, VILLAGE_GOTO_LANDING_CHUNK_RADIUS);
    }

    private static List<ChunkPos> structureLandingPrepChunks(BlockPos target) {
        return landingPrepChunks(target, STRUCTURE_GOTO_LANDING_CHUNK_RADIUS);
    }

    private static List<ChunkPos> landingPrepChunks(BlockPos target, int radius) {
        ChunkPos center = new ChunkPos(target);
        LinkedHashSet<ChunkPos> chunks = new LinkedHashSet<>();
        for (int ring = 0; ring <= radius; ring++) {
            for (int dz = -ring; dz <= ring; dz++) {
                boolean zEdge = dz == -ring || dz == ring;
                for (int dx = -ring; dx <= ring; dx++) {
                    boolean xEdge = dx == -ring || dx == ring;
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
        Identifier id = structureIdentifier(structure, level);
        return id != null && isVillageId(id.getNamespace(), id.getPath());
    }

    private static Identifier structureIdentifier(Structure structure, ServerLevel level) {
        var opt = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                .listElements()
                .filter(h -> h.value() == structure)
                .findFirst();
        if (opt.isEmpty()) return null;
        ResourceKey<Structure> k = opt.get().key();
        return k.identifier();
    }
}
