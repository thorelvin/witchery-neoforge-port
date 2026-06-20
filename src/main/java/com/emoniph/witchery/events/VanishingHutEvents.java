package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.entity.VanishingHutAnchorBlockEntity;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.world.VanishingHutData;
import com.emoniph.witchery.worldgen.VanishingHut;
import com.emoniph.witchery.worldgen.VanishingHutHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Drives Vanishing Witch Hut relocations off the main tick (so the heavy demolish/rebuild never runs
 * on the trigger tick), picks a destination — usually a nearby Overworld spot, sometimes a crossing
 * into the Spirit World at the mirrored 1:1 coords — and rebuilds the hut there, leaving ash/debris
 * behind. Modeled on {@link VillageWallEvents}' budgeted job queue.
 */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class VanishingHutEvents {

    private record Job(ServerLevel level, BlockPos anchorPos) {}

    private static final ConcurrentLinkedQueue<Job> QUEUE = new ConcurrentLinkedQueue<>();
    private static final int JOBS_PER_TICK = 2;

    private static final int RING_MIN = 48;
    private static final int RING_MAX = 96;
    private static final int RING_ATTEMPTS = 12;
    private static final int FLATNESS_TOLERANCE = 2;

    private VanishingHutEvents() {}

    /** Queue a hut (at its buried anchor) to relocate. Safe to call from a block-entity tick. */
    public static void enqueueRelocation(ServerLevel level, BlockPos anchorPos) {
        QUEUE.add(new Job(level, anchorPos.immutable()));
    }

    /**
     * QA/worldgen entry point: find a valid spot near {@code near} and build a fresh hut there.
     * Returns the buried anchor pos, or null if no level, dry, open footprint opened up nearby.
     */
    public static BlockPos placeNear(ServerLevel level, BlockPos near) {
        BlockPos anchor = findValidAnchor(level, near.getX(), near.getZ(), 6, 24, RING_ATTEMPTS, true);
        if (anchor == null) {
            return null;
        }
        UUID hutId = UUID.randomUUID();
        VanishingHut.build(level, anchor, hutId);
        CovenWitchEntity witch = VanishingHut.spawnResident(level, anchor);
        if (level.getBlockEntity(anchor) instanceof VanishingHutAnchorBlockEntity be) {
            be.setResident(witch == null ? null : witch.getUUID());
        }
        VanishingHutData.get(level).upsert(hutId, level, anchor);
        return anchor;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || QUEUE.isEmpty()) {
            return;
        }
        int processed = 0;
        var it = QUEUE.iterator();
        while (it.hasNext() && processed < JOBS_PER_TICK) {
            Job job = it.next();
            if (job.level() != level) {
                continue;
            }
            it.remove();
            processed++;
            relocate(job.level(), job.anchorPos());
        }
    }

    private static void relocate(ServerLevel oldLevel, BlockPos oldAnchor) {
        if (!(oldLevel.getBlockEntity(oldAnchor) instanceof VanishingHutAnchorBlockEntity be)) {
            return; // hut already gone
        }
        UUID hutId = be.hutId();
        UUID residentUuid = be.residentUuid();
        MinecraftServer server = oldLevel.getServer();

        boolean cross = VanishingHutHelper.pickCrossDimension(oldLevel.random,
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

        VanishingHutData.get(oldLevel).upsert(hutId, targetLevel, destAnchor);
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
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            if (!level.hasChunk(chunkX, chunkZ)) {
                if (!mayForceLoad) {
                    continue; // never force-generate chunks during the ring scan
                }
                level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
            }
            Integer floorY = validFloorY(level, x, z);
            if (floorY != null) {
                return new BlockPos(x, floorY - 1, z);
            }
        }
        return null;
    }

    /** The floor Y for a valid, level, dry, open 5x5 footprint centered at (x,z), or null. */
    private static Integer validFloorY(ServerLevel level, int x, int z) {
        int baseFloor = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        for (int dx = -VanishingHut.HALF; dx <= VanishingHut.HALF; dx++) {
            for (int dz = -VanishingHut.HALF; dz <= VanishingHut.HALF; dz++) {
                int cx = x + dx;
                int cz = z + dz;
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, cx, cz);
                if (Math.abs(surface - baseFloor) > FLATNESS_TOLERANCE) {
                    return null; // too uneven
                }
                BlockPos ground = new BlockPos(cx, surface - 1, cz);
                BlockState groundState = level.getBlockState(ground);
                FluidState groundFluid = groundState.getFluidState();
                if (!groundFluid.isEmpty() || !groundState.isSolidRender()) {
                    return null; // water/lava or non-solid footing
                }
                // Floor + the wall column must be open.
                for (int y = surface; y <= surface + VanishingHut.WALL_HEIGHT + 1; y++) {
                    BlockState above = level.getBlockState(new BlockPos(cx, y, cz));
                    if (!above.isAir() && !above.canBeReplaced()) {
                        return null;
                    }
                }
            }
        }
        return baseFloor;
    }
}
