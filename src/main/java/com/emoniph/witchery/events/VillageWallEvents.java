package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.world.VillageWallData;
import com.emoniph.witchery.worldgen.VillageWallGenerator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    private static final ConcurrentLinkedQueue<PendingWall> QUEUE = new ConcurrentLinkedQueue<>();
    private static final long WALL_GENERATION_DELAY_TICKS = 0L;
    private static final long WALL_GENERATION_RETRY_TICKS = 20L;
    private static final int WALL_GENERATION_MAX_ATTEMPTS = 6;
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
        if (QUEUE.isEmpty()) return;

        long now = level.getGameTime();
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

            generateWalls(level, start, false, "queue");
            done++;
        }
    }

    /**
     * Direct generation hook for commands that already know the village target.
     * This makes /witchery goto village_* build walls before the player arrives,
     * instead of relying on the chunk-load queue to win a timing race.
     */
    public static boolean generateForVillageNear(ServerLevel level, BlockPos pos, boolean force) {
        StructureStart start = findVillageStartNear(level, pos);
        if (start == null || !start.isValid() || start.getPieces().isEmpty()) {
            WitcheryMod.LOGGER.info("[VillageWalls] No ready village start near {} for direct generation.", pos);
            return false;
        }
        return generateWalls(level, start, force, "direct");
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

    private static boolean generateWalls(ServerLevel level, StructureStart start, boolean force, String reason) {
        VillageWallData data = VillageWallData.get(level);
        if (!force && data.isProcessed(start.getBoundingBox())) return true;
        if (!start.isValid() || start.getPieces().isEmpty()) return false;

        try {
            WitcheryMod.LOGGER.info("[VillageWalls] Generating walls ({}) for village at {} ({} pieces).",
                    reason, start.getBoundingBox(), start.getPieces().size());
            forceLoadVillageChunks(level, start);
            VillageWallGenerator.generate(level, start);
            data.markProcessed(start.getBoundingBox());
            WitcheryMod.LOGGER.info("[VillageWalls] Wall generation complete for {}.",
                    start.getBoundingBox());
            return true;
        } catch (Exception e) {
            WitcheryMod.LOGGER.error("[VillageWalls] Wall generation failed for {}",
                    start.getBoundingBox(), e);
            return false;
        }
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
                LevelChunk chunk = level.getChunkSource().getChunk(center.x + dx, center.z + dz, true);
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
