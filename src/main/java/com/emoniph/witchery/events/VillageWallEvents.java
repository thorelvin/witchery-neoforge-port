package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.world.VillageWallData;
import com.emoniph.witchery.worldgen.VillageWallGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Detects vanilla villages on chunk load and schedules adaptive wall generation
 * as a deferred server-tick task.
 *
 * Delay: 200 ticks (10 s) after detection — allows surrounding village chunks to
 * finish generating before we read bounding boxes and place wall blocks.
 */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public class VillageWallEvents {

    private record PendingWall(StructureStart start, long processAfter) {}

    private static final ConcurrentLinkedQueue<PendingWall> QUEUE = new ConcurrentLinkedQueue<>();

    // ── Detect village starts on chunk load ────────────────────────────────────

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
            if (!isVanillaVillage(entry.getKey(), level)) continue;
            if (data.isProcessed(start.getBoundingBox())) continue;

            long processAt = level.getGameTime() + 200L;
            QUEUE.add(new PendingWall(start, processAt));
            WitcheryMod.LOGGER.info("[VillageWalls] Queued village at {}.", start.getBoundingBox());
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
                VillageWallGenerator.generate(level, pending.start());
                data.markProcessed(pending.start().getBoundingBox());
            } catch (Exception e) {
                WitcheryMod.LOGGER.error("[VillageWalls] Wall generation failed: {}", e.getMessage());
            }
            done++;
        }
    }

    // ── Village detection ──────────────────────────────────────────────────────

    private static final java.util.Set<String> VILLAGE_IDS = java.util.Set.of(
            "village_plains", "village_savanna", "village_taiga", "village_snowy", "village_desert");

    private static boolean isVanillaVillage(Structure structure, ServerLevel level) {
        // Dynamic registries have no getResourceKey(value); iterate holders to find the key.
        var opt = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                .listElements()
                .filter(h -> h.value() == structure)
                .findFirst();
        if (opt.isEmpty()) return false;
        net.minecraft.resources.ResourceKey<Structure> k = opt.get().key();
        return "minecraft".equals(k.identifier().getNamespace())
                && VILLAGE_IDS.contains(k.identifier().getPath());
    }
}
