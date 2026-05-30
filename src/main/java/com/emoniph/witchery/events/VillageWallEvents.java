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
 * Generation: 200-tick delay, then force-load all village chunks before
 * generating so no wall cells are silently skipped by getChunkNow() checks.
 */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public class VillageWallEvents {

    private record PendingWall(StructureStart start, long processAfter) {}

    private static final ConcurrentLinkedQueue<PendingWall> QUEUE = new ConcurrentLinkedQueue<>();

    static final List<String> VILLAGE_IDS = List.of(
            "village_plains", "village_savanna", "village_taiga",
            "village_snowy", "village_desert");

    // ── Detection: ChunkEvent.Load + getAllStarts() ────────────────────────────

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;

        Map<Structure, StructureStart> starts = chunk.getAllStarts();
        if (starts.isEmpty()) return;

        VillageWallData data = VillageWallData.get(level);
        for (Map.Entry<Structure, StructureStart> entry : starts.entrySet()) {
            StructureStart start = entry.getValue();
            if (!start.isValid()) continue;
            if (!isVillageStructure(entry.getKey(), level)) continue;
            if (data.isProcessed(start.getBoundingBox())) continue;

            BoundingBox bb = start.getBoundingBox();
            if (QUEUE.stream().anyMatch(p -> p.start().getBoundingBox().equals(bb))) continue;

            long processAt = level.getGameTime() + 200L;
            QUEUE.add(new PendingWall(start, processAt));
            WitcheryMod.LOGGER.info("[VillageWalls] Queued village at {} ({} pieces).",
                    bb, start.getPieces().size());
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

            VillageWallData data = VillageWallData.get(level);
            if (data.isProcessed(pending.start().getBoundingBox())) continue;

            try {
                WitcheryMod.LOGGER.info("[VillageWalls] Generating walls for village at {} ({} pieces).",
                        pending.start().getBoundingBox(), pending.start().getPieces().size());
                forceLoadVillageChunks(level, pending.start());
                VillageWallGenerator.generate(level, pending.start());
                data.markProcessed(pending.start().getBoundingBox());
                WitcheryMod.LOGGER.info("[VillageWalls] Wall generation complete for {}.",
                        pending.start().getBoundingBox());
            } catch (Exception e) {
                WitcheryMod.LOGGER.error("[VillageWalls] Wall generation failed for {}",
                        pending.start().getBoundingBox(), e);
            }
            done++;
        }
    }

    // ── Chunk force-loading ────────────────────────────────────────────────────

    /**
     * Force-loads all chunks in the village bounding box plus 12-block padding
     * so every wall cell passes the getChunkNow() check in the generator.
     */
    public static void forceLoadVillageChunks(ServerLevel level, StructureStart start) {
        BoundingBox bb = start.getBoundingBox();
        int pad = 12;
        int minCX = (bb.minX() - pad) >> 4, maxCX = (bb.maxX() + pad) >> 4;
        int minCZ = (bb.minZ() - pad) >> 4, maxCZ = (bb.maxZ() + pad) >> 4;
        int count = 0;
        for (int cx = minCX; cx <= maxCX; cx++)
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                level.getChunkSource().getChunk(cx, cz, true);
                count++;
            }
        WitcheryMod.LOGGER.info("[VillageWalls] Force-loaded {} chunks for village at {}.", count, bb);
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
        return "minecraft".equals(k.identifier().getNamespace())
                && VILLAGE_IDS.contains(k.identifier().getPath());
    }
}
