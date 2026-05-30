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
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Generates adaptive fortress walls around vanilla villages.
 *
 * Detection — two independent triggers, both feed the same queue:
 *
 *  PRIMARY: ChunkEvent.Load + LevelChunk.getAllStarts().
 *    Works when the village's start chunk loads (confirmed working in prior
 *    sessions — produced partial walls when some village chunks were unloaded).
 *    With forceLoadVillageChunks() this now produces full walls.
 *
 *  BACKUP: PlayerTickEvent.Post every 400 ticks, using
 *    StructureManager.getAllStructuresAt() + force-load + getAllStarts().
 *    Catches cases where the player teleports into a village after the start
 *    chunk already loaded (so ChunkEvent.Load already fired and was missed).
 *
 * Generation: 200-tick delay after detection (gives surrounding chunks time to
 * generate). Before generating, ALL village chunks + padding are force-loaded via
 * getChunk(x,z,true) so the generator never silently skips cells.
 */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public class VillageWallEvents {

    private record PendingWall(StructureStart start, long processAfter) {}

    private static final ConcurrentLinkedQueue<PendingWall> QUEUE = new ConcurrentLinkedQueue<>();

    static final List<String> VILLAGE_IDS = List.of(
            "village_plains", "village_savanna", "village_taiga",
            "village_snowy", "village_desert");

    // ── Primary: detect via ChunkEvent.Load + getAllStarts() ───────────────────
    //
    // getAllStarts() DOES return the full StructureStart when called on the
    // START CHUNK of a village structure (confirmed working in prior sessions).
    // It only returns empty when called on non-start chunks or before the chunk
    // has fully deserialized from disk. ChunkEvent.Load fires after full
    // deserialization, so the data is present.

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
            WitcheryMod.LOGGER.info("[VillageWalls] Queued village at {} ({} pieces) via chunk load.",
                    bb, start.getPieces().size());
        }
    }

    // ── Backup: player-tick scan every 20 s ───────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;
        if (player.tickCount % 400 != 0) return;

        tryQueueVillageAt(level, player.blockPosition());
    }

    /**
     * Attempts to find and queue a village at the given position.
     * Uses StructureManager.getAllStructuresAt() to locate structure references,
     * then force-loads each referenced chunk to find the start chunk via
     * getAllStarts(). Returns true if a new village was queued.
     */
    public static boolean tryQueueVillageAt(ServerLevel level, BlockPos pos) {
        StructureStart start = findVillageStart(level, pos);
        if (start == null) return false;

        VillageWallData data = VillageWallData.get(level);
        if (data.isProcessed(start.getBoundingBox())) {
            WitcheryMod.LOGGER.debug("[VillageWalls] Village at {} already processed.", start.getBoundingBox());
            return false;
        }

        BoundingBox bb = start.getBoundingBox();
        if (QUEUE.stream().anyMatch(p -> p.start().getBoundingBox().equals(bb))) {
            WitcheryMod.LOGGER.debug("[VillageWalls] Village at {} already queued.", bb);
            return false;
        }

        long processAt = level.getGameTime() + 40L;
        QUEUE.add(new PendingWall(start, processAt));
        WitcheryMod.LOGGER.info("[VillageWalls] Queued village at {} ({} pieces) via player scan.",
                bb, start.getPieces().size());
        return true;
    }

    /**
     * Finds the full StructureStart for a village at or near the given position.
     * Uses StructureManager to look up structure references, then force-loads
     * the candidate chunks and queries getAllStarts() on each.
     */
    public static StructureStart findVillageStart(ServerLevel level, BlockPos pos) {
        Map<Structure, LongSet> refs = level.structureManager().getAllStructuresAt(pos);
        if (refs.isEmpty()) return null;

        for (Map.Entry<Structure, LongSet> entry : refs.entrySet()) {
            if (!isVillageStructure(entry.getKey(), level)) continue;
            Structure struct = entry.getKey();

            for (long packed : entry.getValue()) {
                ChunkPos cp = new ChunkPos(packed);
                // Force-load — only the start chunk has getAllStarts() populated.
                LevelChunk chunk = level.getChunkSource().getChunk(cp.x, cp.z, true);
                if (chunk == null) continue;
                StructureStart start = chunk.getAllStarts().get(struct);
                if (start != null && start.isValid()) {
                    WitcheryMod.LOGGER.info("[VillageWalls] findVillageStart: found {} pieces at {}",
                            start.getPieces().size(), start.getBoundingBox());
                    return start;
                }
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
                WitcheryMod.LOGGER.info("[VillageWalls] Processing queued village at {} ({} pieces).",
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
     * Force-loads all chunks in the village bounding box plus PADDING (12 blocks)
     * so the generator's getChunkNow() checks pass for every wall cell.
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
     * Returns true if the given Structure is one of the vanilla village variants.
     * Compares by Holder value identity — correct because registry-managed
     * structures always resolve to the same Java object per registry key.
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
