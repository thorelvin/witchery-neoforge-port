package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.entity.VanishingHutAnchorBlockEntity;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.world.VanishingHutData;
import com.emoniph.witchery.worldgen.NbtStructurePlacement;
import com.emoniph.witchery.worldgen.VanishingHut;
import com.emoniph.witchery.worldgen.VanishingHutHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Drives Vanishing Witch Hut relocations off the main tick (so the heavy demolish/rebuild never runs
 * on the trigger tick), picks a destination — usually a nearby Overworld spot, sometimes a crossing
 * into the Spirit World at the mirrored 1:1 coords — and rebuilds the hut there, leaving ash/debris
 * behind. Modeled on {@link VillageWallEvents}' budgeted job queue.
 */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class VanishingHutEvents {

    private record Job(ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos anchorPos,
                       boolean allowCrossDimension) {}

    private static final Map<MinecraftServer, WorkSession> SESSIONS = new WeakHashMap<>();
    private static final int JOBS_PER_TICK = 2;

    private static final class WorkSession {
        private final ConcurrentLinkedQueue<Job> queue = new ConcurrentLinkedQueue<>();
    }

    private static final int RING_MIN = 48;
    private static final int RING_MAX = 96;
    private static final int RING_ATTEMPTS = 12;
    private static final int SETUP_SEARCH_RADIUS = 24;
    private static final int SETUP_RANDOM_ATTEMPTS = 24;
    private static final int FLATNESS_TOLERANCE = 2;

    private VanishingHutEvents() {}

    /** Queue a hut (at its buried anchor) to relocate. Safe to call from a block-entity tick. */
    public static void enqueueRelocation(ServerLevel level, BlockPos anchorPos) {
        work(level.getServer()).queue.add(new Job(level.dimension(), anchorPos.immutable(), true));
    }

    /** Certification hook: exercise the real move path while keeping the destination deterministic. */
    public static void enqueueSameDimensionRelocation(ServerLevel level, BlockPos anchorPos) {
        work(level.getServer()).queue.add(new Job(level.dimension(), anchorPos.immutable(), false));
    }

    /**
     * QA/worldgen entry point: find a valid spot near {@code near} and build a fresh hut there.
     * Returns the buried anchor pos, or null if no level, dry, open footprint opened up nearby.
     */
    public static BlockPos placeNear(ServerLevel level, BlockPos near) {
        RejectionStats stats = new RejectionStats();
        BlockPos anchor = findSetupAnchor(level, near.getX(), near.getZ(), stats);
        if (anchor == null) {
            stats.logSetupFailure(level, near);
            return null;
        }
        UUID hutId = UUID.randomUUID();
        clearClutter(level, anchor);
        VanishingHut.build(level, anchor, hutId);
        CovenWitchEntity witch = VanishingHut.spawnResident(level, anchor);
        if (level.getBlockEntity(anchor) instanceof VanishingHutAnchorBlockEntity be) {
            be.setResident(witch == null ? null : witch.getUUID());
        }
        VanishingHutData.get(level).upsert(hutId, level, anchor, witch == null ? null : witch.getUUID());
        return anchor;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        WorkSession session = existing(level.getServer());
        if (session == null || session.queue.isEmpty()) return;
        int processed = 0;
        var it = session.queue.iterator();
        while (it.hasNext() && processed < JOBS_PER_TICK) {
            Job job = it.next();
            if (!job.dimension().equals(level.dimension())) {
                continue;
            }
            it.remove();
            processed++;
            relocate(level, job.anchorPos(), job.allowCrossDimension());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        synchronized (SESSIONS) {
            SESSIONS.remove(event.getServer());
        }
    }

    private static WorkSession work(MinecraftServer server) {
        synchronized (SESSIONS) {
            return SESSIONS.computeIfAbsent(server, ignored -> new WorkSession());
        }
    }

    private static WorkSession existing(MinecraftServer server) {
        synchronized (SESSIONS) {
            return SESSIONS.get(server);
        }
    }

    private static void relocate(ServerLevel oldLevel, BlockPos oldAnchor, boolean allowCrossDimension) {
        if (!(oldLevel.getBlockEntity(oldAnchor) instanceof VanishingHutAnchorBlockEntity be)) {
            return; // hut already gone
        }
        UUID hutId = be.hutId();
        UUID residentUuid = be.residentUuid();
        MinecraftServer server = oldLevel.getServer();

        boolean cross = allowCrossDimension && VanishingHutHelper.pickCrossDimension(oldLevel.random,
                VanishingHutHelper.CROSS_DIMENSION_CHANCE);
        ServerLevel targetLevel = cross ? crossTarget(server, oldLevel) : oldLevel;
        if (targetLevel == null) {
            targetLevel = oldLevel;
            cross = false;
        }

        BlockPos destAnchor;
        if (cross) {
            // Mirror the 1:1 coords into the other dimension; that destination is intended, so allow
            // loading the single small target area there.
            destAnchor = findValidAnchor(targetLevel, oldAnchor.getX(), oldAnchor.getZ(), 0, 16, 12, true);
        } else {
            // In-world hop: only consider already-loaded ground near the player (no generation spike).
            destAnchor = findValidAnchor(targetLevel, oldAnchor.getX(), oldAnchor.getZ(),
                    RING_MIN, RING_MAX, RING_ATTEMPTS, false);
        }

        if (destAnchor == null) {
            // No safe destination this cycle — stay put and reset the gate so it can try again later.
            be.markSettled(oldLevel.getGameTime());
            return;
        }

        clearClutter(targetLevel, destAnchor);
        VanishingHut.build(targetLevel, destAnchor, hutId);

        // Carry the same witch to the new hut; only spawn a fresh one if she's truly gone.
        UUID newResident = moveResident(oldLevel, residentUuid, targetLevel, destAnchor);
        if (newResident == null) {
            CovenWitchEntity fresh = VanishingHut.spawnResident(targetLevel, destAnchor);
            newResident = fresh == null ? null : fresh.getUUID();
        }
        if (targetLevel.getBlockEntity(destAnchor) instanceof VanishingHutAnchorBlockEntity newBe) {
            newBe.setResident(newResident);
        }

        VanishingHutData.get(oldLevel).relocate(hutId, targetLevel, destAnchor, newResident);
        VanishingHut.vanish(oldLevel, oldAnchor);
        WitcheryMod.LOGGER.info("[VanishingHut] Hut {} moved to {} in {}{}.", hutId, destAnchor,
                targetLevel.dimension().identifier(), cross ? " (crossed)" : "");
    }

    /** Move the existing resident witch to the new hut (same id preserved), or null if she's gone. */
    private static UUID moveResident(ServerLevel oldLevel, UUID residentUuid,
                                     ServerLevel destLevel, BlockPos destAnchor) {
        if (residentUuid == null) {
            return null;
        }
        Entity entity = oldLevel.getEntity(residentUuid);
        if (!(entity instanceof CovenWitchEntity witch)) {
            return null;
        }
        BlockPos home = VanishingHut.residentHome(destAnchor);
        double hx = home.getX() + 0.5;
        double hy = home.getY();
        double hz = home.getZ() + 0.5;
        witch.setHutHome(home);
        if (destLevel == oldLevel) {
            witch.teleportTo(hx, hy, hz);
            return witch.getUUID();
        }
        // Cross-dimension: changeDimension keeps the same UUID, so re-find and re-tether the moved witch.
        witch.teleportTo(destLevel, hx, hy, hz,
                java.util.Set.<net.minecraft.world.entity.Relative>of(), witch.getYRot(), witch.getXRot(), false);
        if (destLevel.getEntity(residentUuid) instanceof CovenWitchEntity moved) {
            moved.setHutHome(home);
            return moved.getUUID();
        }
        return null;
    }

    private static ServerLevel crossTarget(MinecraftServer server, ServerLevel current) {
        ServerLevel spirit = server.getLevel(WitcheryDimensions.SPIRIT_WORLD);
        if (current.dimension() == WitcheryDimensions.SPIRIT_WORLD) {
            return server.overworld();
        }
        return spirit; // may be null if the dimension failed to load; caller falls back
    }

    /**
     * Find a buried-anchor position with a level, dry, open footprint. Searches outward in a ring
     * ({@code minRadius..maxRadius}); pass {@code minRadius==0} to anchor on the exact column first.
     * Returns the anchor pos (one below the floor surface) or null if nothing valid was found.
     *
     * <p>{@code mayForceLoad} controls the cost: the in-world ring relocation passes {@code false}
     * so it only inspects chunks that are <em>already</em> loaded (no synchronous chunk generation —
     * there is almost always loaded ground near the player who just visited; if not, the move simply
     * waits for the next cycle). The cross-dimension and command placements pass {@code true} because
     * the chosen destination is intended and is a single small area, not a 24-chunk scan.
     */
    private static BlockPos findValidAnchor(ServerLevel level, int centerX, int centerZ,
                                            int minRadius, int maxRadius, int attempts, boolean mayForceLoad) {
        return findValidAnchor(level, centerX, centerZ, minRadius, maxRadius, attempts, mayForceLoad, null);
    }

    private static BlockPos findSetupAnchor(ServerLevel level, int centerX, int centerZ,
                                            RejectionStats stats) {
        BlockPos anchor = findDeterministicAnchor(level, centerX, centerZ,
                SETUP_SEARCH_RADIUS, true, stats);
        if (anchor != null) {
            return anchor;
        }
        return findValidAnchor(level, centerX, centerZ, 0, SETUP_SEARCH_RADIUS,
                SETUP_RANDOM_ATTEMPTS, true, stats);
    }

    private static BlockPos findDeterministicAnchor(ServerLevel level, int centerX, int centerZ,
                                                    int maxRadius, boolean mayForceLoad,
                                                    RejectionStats stats) {
        for (int radius = 0; radius <= maxRadius; radius++) {
            if (radius == 0) {
                BlockPos anchor = tryAnchorCandidate(level, centerX, centerZ, mayForceLoad, stats);
                if (anchor != null) {
                    return anchor;
                }
                continue;
            }
            for (int dx = -radius; dx <= radius; dx++) {
                BlockPos north = tryAnchorCandidate(level, centerX + dx, centerZ - radius,
                        mayForceLoad, stats);
                if (north != null) {
                    return north;
                }
                BlockPos south = tryAnchorCandidate(level, centerX + dx, centerZ + radius,
                        mayForceLoad, stats);
                if (south != null) {
                    return south;
                }
            }
            for (int dz = -radius + 1; dz <= radius - 1; dz++) {
                BlockPos west = tryAnchorCandidate(level, centerX - radius, centerZ + dz,
                        mayForceLoad, stats);
                if (west != null) {
                    return west;
                }
                BlockPos east = tryAnchorCandidate(level, centerX + radius, centerZ + dz,
                        mayForceLoad, stats);
                if (east != null) {
                    return east;
                }
            }
        }
        return null;
    }

    private static BlockPos findValidAnchor(ServerLevel level, int centerX, int centerZ,
                                            int minRadius, int maxRadius, int attempts,
                                            boolean mayForceLoad, RejectionStats stats) {
        for (int attempt = 0; attempt < attempts; attempt++) {
            int x;
            int z;
            if (minRadius <= 0 && attempt == 0) {
                x = centerX;
                z = centerZ;
            } else {
                double angle = level.random.nextDouble() * Math.PI * 2.0;
                double dist = minRadius + level.random.nextDouble() * Math.max(1, maxRadius - minRadius);
                x = centerX + (int) Math.round(Math.cos(angle) * dist);
                z = centerZ + (int) Math.round(Math.sin(angle) * dist);
            }
            BlockPos anchor = tryAnchorCandidate(level, x, z, mayForceLoad, stats);
            if (anchor != null) {
                return anchor;
            }
        }
        return null;
    }

    private static BlockPos tryAnchorCandidate(ServerLevel level, int x, int z,
                                               boolean mayForceLoad, RejectionStats stats) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        if (!level.hasChunk(chunkX, chunkZ)) {
            if (!mayForceLoad) {
                if (stats != null) stats.unloadedChunk++;
                return null; // never force-generate chunks during the ring scan
            }
            level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
        }
        Integer floorY = validFloorY(level, x, z, stats);
        return floorY == null ? null : new BlockPos(x, floorY - 2, z);
    }

    /** The first-air Y over a valid, level, dry, open footprint centered at (x,z), or null. */
    private static Integer validFloorY(ServerLevel level, int x, int z, RejectionStats stats) {
        Integer baseFloor = trueGroundSurfaceY(level, x, z, stats);
        if (baseFloor == null) {
            return null;
        }
        for (int dx = VanishingHut.MIN_DX; dx <= VanishingHut.MAX_DX; dx++) {
            for (int dz = VanishingHut.MIN_DZ; dz <= VanishingHut.MAX_DZ; dz++) {
                int cx = x + dx;
                int cz = z + dz;
                Integer surfaceResult = trueGroundSurfaceY(level, cx, cz, stats);
                if (surfaceResult == null) {
                    return null;
                }
                int surface = surfaceResult;
                if (Math.abs(surface - baseFloor) > FLATNESS_TOLERANCE) {
                    if (stats != null) stats.uneven++;
                    return null; // too uneven
                }
                // The template's visible surface replaces the top solid block; everything above
                // that must be open enough for the NBT volume.
                for (int y = surface; y <= surface + VanishingHut.ROOF_DY; y++) {
                    BlockState above = level.getBlockState(new BlockPos(cx, y, cz));
                    if (!above.isAir() && !NbtStructurePlacement.isClearableClutter(above)) {
                        if (stats != null) stats.blockedVolume++;
                        return null;
                    }
                }
            }
        }
        return baseFloor;
    }

    private static Integer trueGroundSurfaceY(ServerLevel level, int x, int z, RejectionStats stats) {
        NbtStructurePlacement.GroundResult result = NbtStructurePlacement.trueGroundSurfaceY(level, x, z);
        if (result.surfaceY().isPresent()) {
            return result.surfaceY().getAsInt();
        }
        if (stats != null) {
            if (result.failure() == NbtStructurePlacement.GroundFailure.FLUID) {
                stats.fluid++;
            } else {
                stats.nonSolidGround++;
            }
        }
        return null;
    }

    private static void clearClutter(ServerLevel level, BlockPos anchor) {
        int floorY = anchor.getY() + 1;
        int roofY = anchor.getY() + VanishingHut.ROOF_DY;
        NbtStructurePlacement.clearClutterBox(level,
                anchor.getX() + VanishingHut.MIN_DX, floorY, anchor.getZ() + VanishingHut.MIN_DZ,
                anchor.getX() + VanishingHut.MAX_DX, roofY, anchor.getZ() + VanishingHut.MAX_DZ);
    }

    private static final class RejectionStats {
        private int unloadedChunk;
        private int uneven;
        private int fluid;
        private int nonSolidGround;
        private int blockedVolume;

        private void logSetupFailure(ServerLevel level, BlockPos near) {
            WitcheryMod.LOGGER.info(
                    "[VanishingHut] Setup failed near {} in {}: unloaded={}, uneven={}, fluid={}, nonSolidGround={}, blocked={}.",
                    near, level.dimension().identifier(), unloadedChunk, uneven, fluid,
                    nonSolidGround, blockedVolume);
        }
    }
}
