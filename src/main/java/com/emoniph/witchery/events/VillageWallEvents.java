package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.world.VillageWallData;
import com.emoniph.witchery.worldgen.VillageWallGenerator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
 * Generation: queued from chunk load and processed on the next level tick, then
 * force-loads the perimeter chunks before generating so wall cells on the outer hull
 * pass getChunkNow() checks.
 */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public class VillageWallEvents {

    private record PendingWall(StructureStart start, long processAfter, int attempts) {}
    private record PendingTeleport(UUID playerId, BlockPos target, BlockPos from,
                                   float yRot, float xRot, String namespace, String structureId) {}

    private static final class WallJob {
        final StructureStart start;
        boolean force;
        final String reason;
        final List<ChunkPos> chunks;
        final List<PendingTeleport> teleports = new ArrayList<>();
        final long queuedAt;
        boolean ticketsAdded;
        long ticketedAt;
        long lastLogAt;

        WallJob(StructureStart start, boolean force, String reason,
                List<ChunkPos> chunks, PendingTeleport teleport, long queuedAt) {
            this.start = start;
            this.force = force;
            this.reason = reason;
            this.chunks = chunks;
            this.queuedAt = queuedAt;
            if (teleport != null) this.teleports.add(teleport);
        }
    }

    private static final ConcurrentLinkedQueue<PendingWall> QUEUE = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<WallJob> JOBS = new ConcurrentLinkedQueue<>();
    private static final long WALL_GENERATION_DELAY_TICKS = 0L;
    private static final long WALL_GENERATION_RETRY_TICKS = 20L;
    private static final int WALL_GENERATION_MAX_ATTEMPTS = 6;
    private static final long WALL_JOB_LOG_INTERVAL_TICKS = 40L;
    private static final long WALL_JOB_TIMEOUT_TICKS = 20L * 30L;
    private static final int FORCE_LOAD_PADDING_BLOCKS = 24;
    private static final int FORCE_LOAD_EDGE_RING_CHUNKS = 1;

    static final List<String> VILLAGE_IDS = List.of(
            "village_plains", "village_savanna", "village_taiga",
            "village_snowy", "village_desert");

    public static boolean isVillageId(String namespace, String path) {
        return "minecraft".equals(namespace) && VILLAGE_IDS.contains(path);
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
            queued |= queueVillage(level, data, start);
        }

        if (!queued) {
            ChunkPos cp = chunk.getPos();
            BlockPos probe = new BlockPos((cp.x << 4) + 8, 64, (cp.z << 4) + 8);
            StructureStart referenced = findVillageStartFromReferences(level, probe);
            if (referenced != null && referenced.isValid()) {
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
        WallJob job = JOBS.peek();
        if (job != null) {
            if (processWallJob(level, job, now)) {
                JOBS.poll();
            }
            return;
        }

        if (QUEUE.isEmpty()) return;

        Iterator<PendingWall> it = QUEUE.iterator();
        int done = 0;

        while (it.hasNext() && done < 1) {
            PendingWall pending = it.next();
            if (pending.processAfter() > now) continue;
            it.remove();

            StructureStart start = refreshVillageStart(level, pending.start());
            if (start == null || !start.isValid() || start.getPieces().isEmpty()) {
                retryPending(level, pending, start);
                continue;
            }

            queueGeneration(level, start, false, "queue");
            done++;
        }
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
     * Queue village walls and teleport the player only after the walls are generated.
     */
    public static boolean queueVillageGoto(ServerLevel level, ServerPlayer player, BlockPos target,
                                           BlockPos from, String namespace, String structureId) {
        StructureStart start = findVillageStartNear(level, target);
        if (start == null || !start.isValid() || start.getPieces().isEmpty()) {
            WitcheryMod.LOGGER.info("[VillageWalls] No ready village start near {} for goto pre-generation.", target);
            return false;
        }
        PendingTeleport teleport = new PendingTeleport(player.getUUID(), target, from,
                player.getYRot(), player.getXRot(), namespace, structureId);
        return queueGeneration(level, start, true, "goto", teleport);
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

    private static boolean queueVillage(ServerLevel level, VillageWallData data, StructureStart start) {
        if (data.isProcessed(start.getBoundingBox())) return false;

        BoundingBox bb = start.getBoundingBox();
        if (QUEUE.stream().anyMatch(p -> p.start().getBoundingBox().equals(bb))) return false;

        long processAt = level.getGameTime() + WALL_GENERATION_DELAY_TICKS;
        QUEUE.add(new PendingWall(start, processAt, 0));
        WitcheryMod.LOGGER.info("[VillageWalls] Queued village at {} ({} pieces).",
                bb, start.getPieces().size());
        return true;
    }

    private static boolean queueGeneration(ServerLevel level, StructureStart start, boolean force,
                                           String reason, PendingTeleport teleport) {
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
                if (teleport != null) job.teleports.add(teleport);
                return true;
            }
        }

        List<ChunkPos> chunks = perimeterChunks(start);
        if (teleport != null) {
            ChunkPos targetChunk = new ChunkPos(teleport.target());
            if (!chunks.contains(targetChunk)) chunks.add(targetChunk);
        }
        JOBS.add(new WallJob(start, force, reason, chunks, teleport, level.getGameTime()));
        WitcheryMod.LOGGER.info("[VillageWalls] Queued wall job ({}) for {} with {} perimeter chunk ticket(s).",
                reason, bb, chunks.size());
        return true;
    }

    private static boolean processWallJob(ServerLevel level, WallJob job, long now) {
        try {
            if (!job.ticketsAdded) {
                for (ChunkPos cp : job.chunks) {
                    level.getChunkSource().updateChunkForced(cp, true);
                }
                job.ticketsAdded = true;
                job.ticketedAt = now;
                job.lastLogAt = now;
                WitcheryMod.LOGGER.info("[VillageWalls] Requested {} perimeter chunk ticket(s) for {}.",
                        job.chunks.size(), job.start.getBoundingBox());
                return false;
            }

            int ready = readyChunkCount(level, job.chunks);
            if (ready < job.chunks.size()) {
                if (now - job.lastLogAt >= WALL_JOB_LOG_INTERVAL_TICKS) {
                    job.lastLogAt = now;
                    WitcheryMod.LOGGER.info("[VillageWalls] Waiting for perimeter chunks at {}: {}/{} ready.",
                            job.start.getBoundingBox(), ready, job.chunks.size());
                }
                if (job.ticketedAt > 0 && now - job.ticketedAt > WALL_JOB_TIMEOUT_TICKS) {
                    WitcheryMod.LOGGER.error("[VillageWalls] Timed out waiting for perimeter chunks at {}: {}/{} ready.",
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
        int ready = 0;
        for (ChunkPos cp : chunks) {
            if (level.getChunkSource().getChunkNow(cp.x, cp.z) != null) ready++;
        }
        return ready;
    }

    private static void finishWallJob(ServerLevel level, WallJob job, boolean success) {
        for (PendingTeleport teleport : job.teleports) {
            completeTeleport(level, teleport, success);
        }
        for (ChunkPos cp : job.chunks) {
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

    private static List<ChunkPos> perimeterChunks(StructureStart start) {
        BoundingBox bb = start.getBoundingBox();
        int pad = FORCE_LOAD_PADDING_BLOCKS;
        int minCX = (bb.minX() - pad) >> 4, maxCX = (bb.maxX() + pad) >> 4;
        int minCZ = (bb.minZ() - pad) >> 4, maxCZ = (bb.maxZ() + pad) >> 4;
        List<ChunkPos> chunks = new ArrayList<>();
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                boolean nearEdge = (cx - minCX) < FORCE_LOAD_EDGE_RING_CHUNKS
                                || (maxCX - cx) < FORCE_LOAD_EDGE_RING_CHUNKS
                                || (cz - minCZ) < FORCE_LOAD_EDGE_RING_CHUNKS
                                || (maxCZ - cz) < FORCE_LOAD_EDGE_RING_CHUNKS;
                if (nearEdge) chunks.add(new ChunkPos(cx, cz));
            }
        }
        return chunks;
    }

    // ── Chunk force-loading ────────────────────────────────────────────────────

    /**
     * Force-loads a narrow perimeter ring around the village bounding box.
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
                level.getChunkSource().getChunk(cx, cz, true);
                count++;
            }
        WitcheryMod.LOGGER.info("[VillageWalls] Force-loaded {} perimeter chunks for village at {}.", count, bb);
    }

    // ── Village structure identification ──────────────────────────────────────

    /**
     * Attempts to find a village StructureStart at or near pos by querying
     * StructureManager references, force-loading candidate chunks, and calling
     * getAllStarts() on each. Returns null if nothing is found.
     * Used by the /witchery walls command as a fallback when the chunk-load
     * event may have been missed.
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
        for (Map.Entry<Structure, LongSet> entry : refs.entrySet()) {
            if (!isVillageStructure(entry.getKey(), level)) continue;
            Structure struct = entry.getKey();
            for (long packed : entry.getValue()) {
                ChunkPos cp = new ChunkPos(packed);
                LevelChunk chunk = level.getChunkSource().getChunk(cp.x, cp.z, true);
                if (chunk == null) continue;
                StructureStart start = chunk.getAllStarts().get(struct);
                if (start != null && start.isValid()) return start;
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
