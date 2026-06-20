package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.blocks.entity.VanishingHutAnchorBlockEntity;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Vanishing Witch Hut's geometry, construction, demolition, observation test, and the ash/debris
 * it leaves behind. The hut is built block-by-block (no NBT asset) around a hidden
 * {@link com.emoniph.witchery.blocks.VanishingHutAnchorBlock} buried one block under the floor, so we
 * own its full footprint and can cleanly pick it up and rebuild it elsewhere.
 *
 * <p>Coordinates use the buried anchor pos {@code A=(ax, ay, az)} as the origin. The floor sits at
 * {@code ay+1} (ground level), walls span {@code ay+2..ay+4}, and a flat roof caps {@code ay+5}.
 */
public final class VanishingHut {

    public static final int HALF = 2;            // 5x5 footprint
    public static final int WALL_HEIGHT = 3;     // wall layers above the floor
    public static final int FLOOR_DY = 1;
    public static final int ROOF_DY = FLOOR_DY + WALL_HEIGHT + 1; // ay+5

    private static final Block WALL = Blocks.SPRUCE_PLANKS;
    private static final Block POST = Blocks.SPRUCE_LOG;
    private static final Block FLOOR = Blocks.SPRUCE_PLANKS;
    private static final Block ROOF = Blocks.SPRUCE_PLANKS;

    /** Blocks the hut itself owns — only these are removed when it relocates (player edits survive). */
    private static final Set<Block> PALETTE = Set.of(
            Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG, Blocks.TORCH);

    private VanishingHut() {}

    // ── Geometry ────────────────────────────────────────────────────────────────

    /** Surface (floor) Y for an anchor buried at {@code anchorPos}. */
    public static int groundY(BlockPos anchorPos) {
        return anchorPos.getY() + FLOOR_DY;
    }

    /** Occupancy box: the interior air a player stands in. */
    public static AABB interiorAabb(BlockPos anchorPos) {
        int ax = anchorPos.getX();
        int az = anchorPos.getZ();
        int floorY = groundY(anchorPos);
        return new AABB(ax - (HALF - 1), floorY + 0.1, az - (HALF - 1),
                ax + HALF, floorY + WALL_HEIGHT + 1.0, az + HALF);
    }

    private static Vec3 center(BlockPos anchorPos) {
        return new Vec3(anchorPos.getX() + 0.5, groundY(anchorPos) + WALL_HEIGHT / 2.0, anchorPos.getZ() + 0.5);
    }

    /** A handful of sample points that count as "the hut" for the look-cone test. */
    private static Vec3[] watchSamples(BlockPos anchorPos) {
        int ax = anchorPos.getX();
        int az = anchorPos.getZ();
        double top = groundY(anchorPos) + WALL_HEIGHT + 0.5;
        double mid = groundY(anchorPos) + 1.5;
        return new Vec3[]{
                center(anchorPos),
                new Vec3(ax + 0.5, top, az + 0.5),
                new Vec3(ax - HALF + 0.5, mid, az - HALF + 0.5),
                new Vec3(ax + HALF + 0.5, mid, az - HALF + 0.5),
                new Vec3(ax - HALF + 0.5, mid, az + HALF + 0.5),
                new Vec3(ax + HALF + 0.5, mid, az + HALF + 0.5)
        };
    }

    /** True if any non-spectator player within range has the hut inside their look cone. */
    public static boolean isWatched(ServerLevel level, BlockPos anchorPos) {
        Vec3 center = center(anchorPos);
        Vec3[] samples = watchSamples(anchorPos);
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            if (player.distanceToSqr(center) > VanishingHutHelper.WATCH_RANGE_SQ) continue;
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getViewVector(1.0F);
            for (Vec3 sample : samples) {
                if (VanishingHutHelper.isLookingAt(eye, look, sample)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Counterplay: a line of Witches' Salt laid across the doorway threshold holds the hut in place.
     * The door faces +Z; we check the three ground cells just outside it for the salt-line block.
     */
    public static boolean isHeldBySalt(ServerLevel level, BlockPos anchorPos) {
        int floorY = groundY(anchorPos);
        int frontZ = anchorPos.getZ() + HALF + 1;
        for (int dx = -1; dx <= 1; dx++) {
            BlockPos cell = new BlockPos(anchorPos.getX() + dx, floorY, frontZ);
            if (!level.getBlockState(cell).is(WitcheryBlocks.SALT_LINE.get())) {
                return false;
            }
        }
        return true;
    }

    // ── Construction ──────────────────────────────────────────────────────────────

    /**
     * Build a fresh hut around {@code anchorPos} (buried one below the floor) and stamp the given
     * {@code hutId} onto the new anchor. Does not spawn the resident — the caller either spawns a
     * fresh one ({@link #spawnResident}) or moves the existing witch in (so a relocation keeps the
     * same witch).
     */
    public static void build(net.minecraft.world.level.ServerLevelAccessor level, BlockPos anchorPos, UUID hutId) {
        int ax = anchorPos.getX();
        int az = anchorPos.getZ();
        int floorY = groundY(anchorPos);

        // Floor.
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                level.setBlock(new BlockPos(ax + dx, floorY, az + dz), FLOOR.defaultBlockState(), 3);
            }
        }
        // Walls (with a 1-wide, 2-high south doorway at dx=0, dz=+HALF).
        for (int layer = 1; layer <= WALL_HEIGHT; layer++) {
            int y = floorY + layer;
            for (int dx = -HALF; dx <= HALF; dx++) {
                for (int dz = -HALF; dz <= HALF; dz++) {
                    boolean perimeter = Math.abs(dx) == HALF || Math.abs(dz) == HALF;
                    if (!perimeter) continue;
                    boolean doorway = dz == HALF && dx == 0 && layer <= 2;
                    if (doorway) {
                        level.setBlock(new BlockPos(ax + dx, y, az + dz), Blocks.AIR.defaultBlockState(), 3);
                        continue;
                    }
                    boolean corner = Math.abs(dx) == HALF && Math.abs(dz) == HALF;
                    level.setBlock(new BlockPos(ax + dx, y, az + dz),
                            (corner ? POST : WALL).defaultBlockState(), 3);
                }
            }
        }
        // Flat roof.
        int roofY = anchorPos.getY() + ROOF_DY;
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                level.setBlock(new BlockPos(ax + dx, roofY, az + dz), ROOF.defaultBlockState(), 3);
            }
        }
        // A torch inside for light.
        level.setBlock(new BlockPos(ax - HALF + 1, floorY + 1, az - HALF + 1), Blocks.TORCH.defaultBlockState(), 3);

        // The hidden controller, buried at the anchor pos.
        level.setBlock(anchorPos, WitcheryBlocks.VANISHING_HUT_ANCHOR.get().defaultBlockState(), 3);
        if (level.getBlockEntity(anchorPos) instanceof VanishingHutAnchorBlockEntity be) {
            be.setHutId(hutId);
            be.markSettled(level.getLevel().getGameTime());
        }
    }

    /** Spawn a fresh resident witch inside the hut at {@code anchorPos}, tethered to its hearth. */
    public static CovenWitchEntity spawnResident(net.minecraft.world.level.ServerLevelAccessor level, BlockPos anchorPos) {
        ServerLevel sl = level.getLevel();
        BlockPos home = new BlockPos(anchorPos.getX(), groundY(anchorPos) + 1, anchorPos.getZ());
        CovenWitchEntity witch = WitcheryEntities.COVEN_WITCH.get().create(sl, EntitySpawnReason.STRUCTURE);
        if (witch == null) {
            return null;
        }
        witch.setPersistenceRequired();
        witch.setPos(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
        witch.finalizeSpawn(level, new DifficultyInstance(sl.getDifficulty(), 0L, 0L, 0.0F),
                EntitySpawnReason.STRUCTURE, null);
        witch.setHutHome(home);
        level.addFreshEntityWithPassengers(witch);
        return witch;
    }

    /** The interior hearth position a resident witch is tethered to (one above the floor centre). */
    public static BlockPos residentHome(BlockPos anchorPos) {
        return new BlockPos(anchorPos.getX(), groundY(anchorPos) + 1, anchorPos.getZ());
    }

    // ── Demolition + traces ─────────────────────────────────────────────────────

    /**
     * Remove the hut's own blocks (player-placed blocks survive), discard the resident witch, then
     * stamp persistent ash/debris and an FX burst, and whisper the witch's farewell to onlookers.
     */
    public static void vanish(ServerLevel level, BlockPos anchorPos) {
        int ax = anchorPos.getX();
        int az = anchorPos.getZ();
        int floorY = groundY(anchorPos);
        int roofY = anchorPos.getY() + ROOF_DY;

        // Orphan safety: discard any resident still tethered here (the relocation normally moves her
        // out first, so this usually no-ops).
        for (CovenWitchEntity witch : level.getEntitiesOfClass(CovenWitchEntity.class, interiorAabb(anchorPos))) {
            witch.discard();
        }

        // The ground block the buried anchor replaced — restore it from an untouched neighbour so the
        // floor doesn't leave a hole (or a dirt cap over where the wooden floor was).
        BlockState restore = level.getBlockState(new BlockPos(ax + HALF, anchorPos.getY(), az));
        if (!restore.isSolidRender()) {
            restore = Blocks.DIRT.defaultBlockState();
        }

        // Remove only the hut's own blocks; the original terrain underneath is left exactly as it was.
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                for (int y = anchorPos.getY(); y <= roofY; y++) {
                    BlockPos pos = new BlockPos(ax + dx, y, az + dz);
                    BlockState state = level.getBlockState(pos);
                    if (pos.equals(anchorPos)) {
                        level.setBlock(pos, restore, 3);
                    } else if (PALETTE.contains(state.getBlock())) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        leaveTraces(level, ax, az, floorY);
    }

    private static void leaveTraces(ServerLevel level, int ax, int az, int floorY) {
        // Debris rests on the ground (floor level), on top of the original terrain — no dirt is laid
        // over where the wooden floor stood. Loose planks, cobwebs, and ash mark where it vanished.
        placeIfAir(level, new BlockPos(ax - 1, floorY, az), Blocks.OAK_SLAB.defaultBlockState());
        placeIfAir(level, new BlockPos(ax + 1, floorY, az + 1), Blocks.OAK_TRAPDOOR.defaultBlockState());
        placeIfAir(level, new BlockPos(ax, floorY, az - 1), Blocks.OAK_SLAB.defaultBlockState());
        placeIfAir(level, new BlockPos(ax - HALF, floorY, az - HALF), Blocks.COBWEB.defaultBlockState());
        placeIfAir(level, new BlockPos(ax + HALF, floorY, az + HALF), Blocks.COBWEB.defaultBlockState());

        // FX burst + sound.
        double cx = ax + 0.5;
        double cy = floorY + 1.0;
        double cz = az + 0.5;
        level.sendParticles(ParticleTypes.WHITE_ASH, cx, cy, cz, 60, 1.6, 1.2, 1.6, 0.02);
        level.sendParticles(ParticleTypes.SMOKE, cx, cy, cz, 40, 1.4, 1.0, 1.4, 0.02);
        level.sendParticles(ParticleTypes.WITCH, cx, cy + 1.0, cz, 24, 1.2, 1.0, 1.2, 0.05);
        level.playSound(null, new BlockPos(ax, floorY, az), SoundEvents.WITCH_CELEBRATE,
                SoundSource.AMBIENT, 0.7F, 0.6F);

        // The witch's farewell to anyone nearby.
        Component farewell = Component.literal("We may not meet again.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        Vec3 here = new Vec3(cx, cy, cz);
        List<ServerPlayer> nearby = level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(here, here).inflate(32.0));
        for (ServerPlayer player : nearby) {
            player.sendSystemMessage(farewell);
        }
    }

    private static void placeIfAir(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos).isAir()) {
            level.setBlock(pos, state, 3);
        }
    }
}
