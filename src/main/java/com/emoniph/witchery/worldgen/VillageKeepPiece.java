package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.entity.VillageGuardEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Village fortified keep. Source parity: ComponentVillageKeep (1.7.10-0.24.1 / WitcheryWalls 1.12.2).
 *
 * 16x26x16 two-tower keep. Left tower (offsetX=0) and right tower (offsetX=8) connected by
 * bridging walls, corridors, and a central staircase shaft. Twelve guard spawns total.
 *
 * Block role mapping from 1.7.10 srg, remapped through VillageBuildingMaterials by biome:
 *   field_150347_e = foundation    (walls, floors)
 *   field_150344_f = roof solid    (pyramid roof cap)
 *   field_150364_r = trim          (frame beams)
 *   field_150411_aY = IRON_BARS    (windows/arrow slits in walls)
 *   field_150350_a = AIR           (openings)
 *   field_150476_ad = roof stairs  (roof overhang)
 *   field_150390_bg = step stairs  (door steps)
 *   field_150468_ap = LADDER
 *   field_150478_aa = TORCH
 *   field_150333_U  = IRON_BARS    (used in different fill calls but same block)
 *   field_180407_aO = parapet rail
 */
public class VillageKeepPiece extends StructurePiece {

    private static final ResourceKey<LootTable> LOOT =
            ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath("witchery", "chests/village_keep"));

    public VillageKeepPiece(StructurePieceType type, int x, int y, int z) {
        super(type, 0, new BoundingBox(x, y, z, x + 15, y + 25, z + 15));
        setOrientation(Direction.NORTH);
    }

    public VillageKeepPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(WitcheryStructures.VILLAGE_KEEP_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {}

    // ── State helpers ─────────────────────────────────────────────────────────

    // ── Generation ────────────────────────────────────────────────────────────

    @Override
    public void postProcess(WorldGenLevel level, StructureManager mgr, ChunkGenerator gen,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {

        VillageBuildingMaterials materials = VillageBuildingMaterials.forVillage(level, boundingBox, box);
        BlockState cobble = materials.foundation();
        BlockState stone  = materials.roofSolid();
        BlockState log    = materials.trim();
        BlockState logX   = materials.trim(Direction.Axis.X);
        BlockState logZ   = materials.trim(Direction.Axis.Z);
        BlockState bars   = Blocks.IRON_BARS.defaultBlockState();
        BlockState air    = Blocks.AIR.defaultBlockState();
        BlockState stairsN = materials.roofStairs(Direction.NORTH);
        BlockState stairsS = materials.roofStairs(Direction.SOUTH);
        BlockState stairsW = materials.roofStairs(Direction.WEST);
        BlockState stairsE = materials.roofStairs(Direction.EAST);
        BlockState stepN = materials.stepStairs(Direction.NORTH);
        BlockState stepS = materials.stepStairs(Direction.SOUTH);
        BlockState stepW = materials.stepStairs(Direction.WEST);
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
        BlockState torch  = Blocks.TORCH.defaultBlockState();

        // ── Clear interior shell ──────────────────────────────────────────────
        generateBox(level, box, 1, 1, 1, 14, 25, 14, air, air, false);

        // ── Two towers ───────────────────────────────────────────────────────
        drawTower(level, box, cobble, log, logX, logZ, bars, air, ladder, torch, 0, 0);
        drawTower(level, box, cobble, log, logX, logZ, bars, air, ladder, torch, 8, 4);

        // ── Entry bridge (N face, x=7-9, y=0-6, z=2-4) ───────────────────────
        fill(level, box, 7, 0, 2, 3, 1, 3, cobble);
        fill(level, box, 7, 4, 3, 3, 1, 2, cobble);
        fill(level, box, 7, 5, 2, 3, 1, 1, cobble);
        placeBlock(level, cobble, 8, 6, 2, box);

        // Oak log gate lintel
        for (int x = 7; x <= 9; x++) placeBlock(level, log, x, 4, 2, box);
        fill(level, box, 7, 3, 3, 3, 1, 1, bars);   // gate bars

        // Iron bars flanking gate
        placeBlock(level, bars, 7, 3, 2, box);
        placeBlock(level, bars, 7, 3, 4, box);
        placeBlock(level, bars, 9, 3, 2, box);
        placeBlock(level, bars, 9, 3, 4, box);

        // Cobblestone-stair steps to N entrance
        for (int x = 7; x <= 9; x++) {
            placeBlock(level, stepN, x, 0, 1, box);
            placeBlock(level, stepS, x, 0, 4, box);
        }

        // ── S face bridge corridor ────────────────────────────────────────────
        fill(level, box, 2, 0, 9, 4, 16, 1, cobble);
        fill(level, box, 2, 0, 14, 4, 16, 1, cobble);
        fill(level, box, 1, 0, 10, 1, 16, 4, cobble);
        fill(level, box, 6, 0, 10, 1, 16, 4, cobble);
        fill(level, box, 2, 0, 10, 4, 1, 4, cobble);
        fill(level, box, 1, 4, 9, 6, 1, 6, cobble);
        fill(level, box, 1, 9, 9, 6, 1, 6, cobble);
        fill(level, box, 1, 14, 9, 6, 1, 6, cobble);

        // Battlements top of S bridge (y=16 corner caps)
        fill(level, box, 3, 16, 9, 2, 1, 1, cobble);
        fill(level, box, 3, 16, 14, 2, 1, 1, cobble);
        fill(level, box, 1, 16, 11, 1, 1, 2, cobble);
        fill(level, box, 6, 16, 11, 1, 1, 2, cobble);

        // S bridge: log frame on front/sides
        fill(level, box, 3, 1, 14, 2, 3, 1, log);
        fill(level, box, 1, 1, 11, 1, 3, 2, log);

        // S bridge: iron bar windows at each floor
        fill(level, box, 3, 11, 9, 2, 2, 1, bars);
        fill(level, box, 3, 6, 14, 2, 2, 1, bars);
        fill(level, box, 3, 11, 14, 2, 2, 1, bars);
        fill(level, box, 1, 6, 11, 1, 2, 2, bars);
        fill(level, box, 1, 11, 11, 1, 2, 2, bars);
        fill(level, box, 6, 11, 11, 1, 2, 2, bars);

        // S bridge: air for door at base of each wall
        fill(level, box, 4, 1, 9, 1, 2, 1, air);
        fill(level, box, 4, 5, 9, 1, 2, 1, air);
        fill(level, box, 6, 1, 11, 1, 2, 1, air);
        fill(level, box, 6, 5, 11, 1, 2, 1, air);

        // Log roof support beams over S bridge
        placeBlock(level, materials.trim(Direction.Axis.X), 8, 7, 9, box);
        placeBlock(level, materials.trim(Direction.Axis.Z), 6, 7, 11, box);

        // Ladder on S bridge W wall (y=1-14)
        BlockState ladderW = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
        for (int h = 1; h <= 14; h++) placeBlock(level, ladderW, 2, h, 10, box);

        // Torches on S bridge
        placeBlock(level, torch, 2, 2, 13, box);
        placeBlock(level, torch, 2, 6, 13, box);
        placeBlock(level, torch, 2, 11, 13, box);

        // ── E bridge corridor (symmetric, offset x=8) ─────────────────────────
        fill(level, box, 11, 0, 9, 3, 19, 1, cobble);
        fill(level, box, 11, 0, 13, 3, 19, 1, cobble);
        fill(level, box, 10, 0, 10, 1, 19, 3, cobble);
        fill(level, box, 14, 0, 10, 1, 19, 3, cobble);
        fill(level, box, 11, 0, 10, 3, 1, 3, cobble);
        fill(level, box, 10, 4, 9, 5, 1, 5, cobble);
        fill(level, box, 10, 9, 9, 5, 1, 5, cobble);
        fill(level, box, 10, 14, 9, 5, 1, 5, cobble);
        fill(level, box, 10, 19, 9, 5, 1, 5, cobble);

        // Log frame
        fill(level, box, 12, 1, 13, 1, 3, 1, log);
        fill(level, box, 14, 1, 11, 1, 3, 1, log);

        // Iron bar windows
        fill(level, box, 12, 6, 13, 1, 2, 1, bars);
        fill(level, box, 12, 11, 9, 1, 2, 1, bars);
        fill(level, box, 12, 16, 9, 1, 2, 1, bars);
        fill(level, box, 12, 11, 13, 1, 2, 1, bars);
        fill(level, box, 12, 16, 13, 1, 2, 1, bars);
        fill(level, box, 14, 6, 11, 1, 2, 1, bars);
        fill(level, box, 14, 11, 11, 1, 2, 1, bars);
        fill(level, box, 14, 16, 11, 1, 2, 1, bars);
        fill(level, box, 10, 11, 11, 1, 2, 1, bars);
        fill(level, box, 10, 16, 11, 1, 2, 1, bars);

        // Air openings
        fill(level, box, 12, 5, 9, 1, 2, 1, air);
        fill(level, box, 12, 1, 9, 1, 2, 1, air);
        fill(level, box, 10, 5, 11, 1, 2, 1, air);
        fill(level, box, 10, 1, 11, 1, 2, 1, air);

        // Ladder on E bridge E wall (y=1-14)
        BlockState ladderE = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST);
        for (int h = 1; h <= 14; h++) placeBlock(level, ladderE, 11, h, 10, box);

        // Torches on E bridge
        placeBlock(level, torch, 11, 2, 12, box);
        placeBlock(level, torch, 11, 6, 12, box);
        placeBlock(level, torch, 11, 11, 12, box);
        placeBlock(level, torch, 11, 16, 12, box);

        // Oak log at top of E bridge
        placeBlock(level, log, 11, 19, 10, box);

        // ── Upper keep pyramid roof (x=10-14, y=20-25, z=9-13) ───────────────
        fill(level, box, 10, 20, 9, 5, 2, 5, stone);
        fill(level, box, 11, 22, 10, 3, 2, 3, stone);
        fill(level, box, 12, 24, 11, 1, 2, 1, stone);
        fill(level, box, 11, 20, 10, 3, 2, 3, air);   // hollow interior

        // Roof overhang stairs (facing outward)
        for (int x = 9; x <= 15; x++) {
            placeBlock(level, stairsN, x, 20, 8, box);
            placeBlock(level, stairsS, x, 20, 14, box);
        }
        for (int x = 10; x <= 14; x++) {
            placeBlock(level, stairsN, x, 22, 9, box);
            placeBlock(level, stairsS, x, 22, 13, box);
        }
        for (int x = 11; x <= 13; x++) {
            placeBlock(level, stairsN, x, 24, 10, box);
            placeBlock(level, stairsS, x, 24, 12, box);
        }
        for (int z = 9; z <= 13; z++) {
            placeBlock(level, stairsW, 9, 20, z, box);
            placeBlock(level, stairsE, 15, 20, z, box);
        }
        for (int z = 10; z <= 12; z++) {
            placeBlock(level, stairsW, 10, 22, z, box);
            placeBlock(level, stairsE, 14, 22, z, box);
        }
        placeBlock(level, stairsW, 11, 24, 11, box);
        placeBlock(level, stairsE, 13, 24, 11, box);

        // ── N corridor between the two towers (y=0-6, z=11-12) ───────────────
        fill(level, box, 7, 0, 11, 3, 1, 2, cobble);
        fill(level, box, 7, 4, 11, 3, 1, 1, cobble);
        fill(level, box, 7, 1, 12, 3, 5, 1, cobble);
        placeBlock(level, cobble, 8, 6, 12, box);
        // Door frame logs
        fill(level, box, 7, 1, 12, 1, 4, 1, log);
        fill(level, box, 9, 1, 12, 1, 4, 1, log);
        // Torch above N door
        placeBlock(level, torch, 8, 2, 11, box);
        // Steps to N corridor
        for (int x = 7; x <= 9; x++) placeBlock(level, stepN, x, 0, 10, box);
        // Iron bars
        placeBlock(level, bars, 7, 3, 11, box);
        placeBlock(level, bars, 9, 3, 11, box);

        // ── W side rooms ──────────────────────────────────────────────────────
        fill(level, box, 3, 0, 6, 2, 1, 3, cobble);
        fill(level, box, 4, 4, 6, 1, 1, 3, cobble);
        fill(level, box, 3, 1, 6, 1, 5, 3, cobble);
        placeBlock(level, cobble, 3, 6, 7, box);
        fill(level, box, 3, 1, 6, 1, 4, 1, log);
        fill(level, box, 3, 1, 8, 1, 4, 1, log);
        placeBlock(level, torch, 4, 2, 7, box);
        for (int z = 6; z <= 8; z++) placeBlock(level, stepW, 5, 0, z, box);
        placeBlock(level, bars, 4, 3, 6, box);
        placeBlock(level, bars, 4, 3, 8, box);

        // ── E side rooms ──────────────────────────────────────────────────────
        fill(level, box, 12, 0, 6, 2, 1, 3, cobble);
        fill(level, box, 12, 4, 6, 1, 1, 3, cobble);
        fill(level, box, 13, 1, 6, 1, 5, 3, cobble);
        placeBlock(level, cobble, 13, 6, 7, box);
        fill(level, box, 13, 1, 6, 1, 4, 1, log);
        fill(level, box, 13, 1, 8, 1, 4, 1, log);
        placeBlock(level, torch, 12, 2, 7, box);
        for (int z = 6; z <= 8; z++) placeBlock(level, stepW, 11, 0, z, box);
        placeBlock(level, bars, 12, 3, 6, box);
        placeBlock(level, bars, 12, 3, 8, box);

        // ── Loot chest on upper floor ─────────────────────────────────────────
        createChest(level, box, random, 13, 20, 12, LOOT);

        // ── Foundation below y=0 ─────────────────────────────────────────────
        for (int bx = 1; bx < 15; bx++) {
            for (int bz = 1; bz < 15; bz++) {
                fillColumnDown(level, cobble, bx, -1, bz, box);
            }
        }

        // ── Guard spawns ──────────────────────────────────────────────────────
        spawnGuards(level, box, 7, 1, 7, 3, random);
        spawnGuards(level, box, 5, 10, 4, 4, random);
        spawnGuards(level, box, 13, 10, 4, 5, random);
    }

    // ── Sub-tower (called twice with different offsets) ───────────────────────

    private void drawTower(WorldGenLevel level, BoundingBox box,
                           BlockState cobble, BlockState log, BlockState logX, BlockState logZ, BlockState bars,
                           BlockState air, BlockState ladder, BlockState torch,
                           int oX, int fX) {
        // Tower walls (11 blocks tall, 3×3 hollow footprint)
        fill(level, box, 3+oX, 0, 1, 3, 11, 1, cobble);
        fill(level, box, 3+oX, 0, 5, 3, 11, 1, cobble);
        fill(level, box, 2+oX, 0, 2, 1, 11, 3, cobble);
        fill(level, box, 6+oX, 0, 2, 1, 11, 3, cobble);
        fill(level, box, 3+oX, 0, 2, 3, 1, 3, cobble);
        // Floor platforms at y=4 and y=9
        fill(level, box, 2+oX, 4, 1, 5, 1, 5, cobble);
        fill(level, box, 2+oX, 9, 1, 5, 1, 5, cobble);
        // Battlement corner caps at y=11
        placeBlock(level, cobble, 4+oX, 11, 1, box);
        placeBlock(level, cobble, 4+oX, 11, 5, box);
        placeBlock(level, cobble, 2+oX, 11, 3, box);
        placeBlock(level, cobble, 6+oX, 11, 3, box);
        // Log window frames
        fill(level, box, 4+oX, 1, 1, 1, 3, 1, log);
        fill(level, box, 2+oX+fX, 1, 3, 1, 3, 1, log);
        // Iron bar windows above log frames
        fill(level, box, 4+oX, 6, 1, 1, 2, 1, bars);
        fill(level, box, 2+oX+fX, 6, 3, 1, 2, 1, bars);
        // Log roof support beams (horizontal, over tower top)
        placeBlock(level, logZ, 4+oX, 7, 5, box);
        placeBlock(level, logX, 6+oX-fX, 7, 3, box);
        // Air openings (door/window clearances)
        fill(level, box, 4+oX, 5, 5, 1, 2, 1, air);
        fill(level, box, 4+oX, 1, 5, 1, 2, 1, air);
        fill(level, box, 6+oX-fX, 5, 3, 1, 2, 1, air);
        // Ladder up the tower interior (facing south, on the z=2 wall)
        for (int h = 1; h <= 9; h++) placeBlock(level, ladder, 3+oX, h, 2, box);
        // Torches at each floor inside
        placeBlock(level, torch, 3+oX, 2, 4, box);
        placeBlock(level, torch, 3+oX, 6, 4, box);
    }

    // ── fill() shim: size-based → generateBox min/max ──────────────────────

    private void fill(WorldGenLevel level, BoundingBox box,
                      int x, int y, int z, int w, int h, int d, BlockState state) {
        generateBox(level, box, x, y, z, x+w-1, y+h-1, z+d-1, state, state, false);
    }

    // ── Guard spawner ─────────────────────────────────────────────────────────

    private void spawnGuards(WorldGenLevel level, BoundingBox box,
                              int lx, int ly, int lz, int count, RandomSource random) {
        for (int i = 0; i < count; i++) {
            int wx = boundingBox.minX() + lx + (i == 0 ? 0 : random.nextInt(3) - 1);
            int wy = boundingBox.minY() + ly;
            int wz = boundingBox.minZ() + lz + (i == 0 ? 0 : random.nextInt(3) - 1);
            if (!box.isInside(new BlockPos(wx, wy, wz))) continue;
            VillageGuardEntity guard = WitcheryEntities.VILLAGE_GUARD.get()
                    .create(level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (guard == null) continue;
            guard.setPersistenceRequired();
            guard.setPos(wx + 0.5, wy, wz + 0.5);
            guard.finalizeSpawn(level, new net.minecraft.world.DifficultyInstance(level.getDifficulty(), 0L, 0L, 0.0F),
                    EntitySpawnReason.STRUCTURE, null);
            level.addFreshEntityWithPassengers(guard);
        }
    }
}
