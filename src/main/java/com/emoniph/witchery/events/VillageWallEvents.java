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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Generates adaptive fortress walls around vanilla villages.
 *
 * Detection: player-tick scan every 400 ticks (20 s) using
 * StructureManager.getAllStructuresAt(BlockPos), which returns
 * Map<Structure, LongSet> (packed ChunkPos references). We then
 * force-load each referenced chunk to find the start chunk via
 * LevelChunk.getAllStarts().
 *
 * Note: LevelChunk.getAllStarts() does NOT work when called from
 * ChunkEvent.Load because the structure data is not yet populated at
 * that moment. But after the chunk is force-loaded later via
 * getChunk(x, z, true), getAllStarts() does return the correct data.
 *
 * Generation: deferred 40 ticks after detection. All village chunks are
 * force-loaded before the generator runs so no wall cells are silently skipped.
 */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public class VillageWallEvents {

    private record PendingWall(StructureStart start, long processAfter) {}

    private static final ConcurrentLinkedQueue<PendingWall> QUEUE = new ConcurrentLinkedQueue<>();

    static final List<String> VILLAGE_IDS = List.of(
            "village_plains", "village_savanna", "village_taiga",
            "village_snowy", "village_desert");

    // ── Player-tick village scan ───────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;
        if (player.tickCount % 400 != 0) return;

        tryQueueVillageAt(level, player.blockPosition());
    }

    /**
     * Checks if pos is inside a vanilla village and queues wall generation.
     * Returns true if a new village was queued, false if already processed/queued or not found.
     * Package-accessible so the /witchery walls command can call it directly.
     */
    public static boolean tryQueueVillageAt(ServerLevel level, BlockPos pos) {
        StructureStart start = findVillageStart(level, pos);
        if (start == null) return false;

        VillageWallData data = VillageWallData.get(level);
        if (data.isProcessed(start.getBoundingBox())) {
            WitcheryMod.LOGGER.debug("[VillageWalls] Village at {} already processed.",
                    start.getBoundingBox());
            return false;
        }

        BoundingBox bb = start.getBoundingBox();
        if (QUEUE.stream().anyMatch(p -> p.start().getBoundingBox().equals(bb))) {
            WitcheryMod.LOGGER.debug("[VillageWalls] Village at {} already queued.", bb);
            return false;
        }

        long processAt = level.getGameTime() + 40L;
        QUEUE.add(new PendingWall(start, processAt));
        WitcheryMod.LOGGER.info("[VillageWalls] Queued village at {}.", bb);
        return true;
    }

    /**
     * Finds the StructureStart for any village at or near the given position.
     *
     * Uses StructureManager.getAllStructuresAt(pos) → Map<Structure, LongSet>.
     * The LongSet contains packed ChunkPos values of all chunks referencing each
     * structure. We force-load each referenced chunk and check getAllStarts() to
     * find the start chunk (the only chunk that has the full StructureStart).
     */
    public static StructureStart findVillageStart(ServerLevel level, BlockPos pos) {
        Map<Structure, LongSet> refs = level.structureManager().getAllStructuresAt(pos);
        if (refs.isEmpty()) return null;

        for (Map.Entry<Structure, LongSet> entry : refs.entrySet()) {
            if (!isVillageStructure(entry.getKey(), level)) continue;

            Structure struct = entry.getKey();
            for (long packed : entry.getValue()) {
                ChunkPos cp = new ChunkPos(packed);
                // Force-load this chunk — only the start chunk will have getAllStarts populated.
                LevelChunk chunk = level.getChunkSource().getChunk(cp.x, cp.z, true);
                if (chunk == null) continue;
                StructureStart start = chunk.getAllStarts().get(struct);
                if (start != null && start.isValid()) return start;
            }
        }
        return null;
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
                forceLoadVillageChunks(level, pending.start());
                VillageWallGenerator.generate(level, pending.start());
                data.markProcessed(pending.start().getBoundingBox());
                WitcheryMod.LOGGER.info("[VillageWalls] Generated walls for village at {}.",
                        pending.start().getBoundingBox());
            } catch (Exception e) {
                WitcheryMod.LOGGER.error("[VillageWalls] Wall generation failed for village at {}",
                        pending.start().getBoundingBox(), e);
            }
            done++;
        }
    }

    // ── Chunk force-loading ────────────────────────────────────────────────────

    /**
     * Force-loads all chunks covered by the village bounding box plus the wall
     * generator's PADDING (10), so no wall cells are silently skipped.
     */
    public static void forceLoadVillageChunks(ServerLevel level, StructureStart start) {
        BoundingBox bb = start.getBoundingBox();
        int pad = 12;
        int minCX = (bb.minX() - pad) >> 4, maxCX = (bb.maxX() + pad) >> 4;
        int minCZ = (bb.minZ() - pad) >> 4, maxCZ = (bb.maxZ() + pad) >> 4;
        for (int cx = minCX; cx <= maxCX; cx++)
            for (int cz = minCZ; cz <= maxCZ; cz++)
                level.getChunkSource().getChunk(cx, cz, true);
    }

    // ── Village structure identification ──────────────────────────────────────

    /**
     * Returns true if the given Structure is a vanilla village variant.
     * Looks up each known village key in the registry and compares by
     * Holder value identity (correct because registry-managed structures
     * always resolve to the same object instance per key).
     */
    static boolean isVillageStructure(Structure structure, ServerLevel level) {
        var reg = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (String id : VILLAGE_IDS) {
            var opt = reg.get(ResourceKey.create(Registries.STRUCTURE,
                    Identifier.fromNamespaceAndPath("minecraft", id)));
            if (opt.isPresent() && opt.get().value() == structure) return true;
        }
        return false;
    }
}
