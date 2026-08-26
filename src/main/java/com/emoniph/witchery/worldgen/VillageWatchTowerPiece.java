package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.entity.VillageGuardEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Guard watch tower. Source parity: ComponentVillageWatchTower (1.7.10-0.24.1 / WitcheryWalls 1.12.2).
 * 8x23x8 biome-material tower, battlemented parapet, pyramidal roof, 3 guard spawns at the top.
 */
public class VillageWatchTowerPiece extends StructurePiece {

    private static final ResourceKey<LootTable> LOOT =
            ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath("witchery", "chests/village_watch_tower"));

    public VillageWatchTowerPiece(StructurePieceType type, int x, int y, int z) {
        super(type, 0, new BoundingBox(x, y, z, x + 7, y + 22, z + 7));
        setOrientation(Direction.NORTH);
    }

    public VillageWatchTowerPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(WitcheryStructures.VILLAGE_WATCH_TOWER_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {}

    // ── Pre-baked states ──────────────────────────────────────────────────────

    // ── Generation ────────────────────────────────────────────────────────────

    @Override
    public void postProcess(WorldGenLevel level, StructureManager mgr, ChunkGenerator gen,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {

        VillageBuildingMaterials materials = VillageBuildingMaterials.forVillage(level, mgr, boundingBox, box);
        BlockState cobble = materials.foundation();
        BlockState stone  = materials.roofSolid();
        BlockState air    = Blocks.AIR.defaultBlockState();
        BlockState fence  = materials.parapet();
        BlockState slab   = materials.roofSlabBottom();
        // Source roof stair metas 3/2/0/1 map to NORTH/SOUTH/EAST/WEST.
        BlockState stairsN = materials.roofStairs(Direction.NORTH);
        BlockState stairsS = materials.roofStairs(Direction.SOUTH);
        BlockState stairsW = materials.roofStairs(Direction.EAST);
        BlockState stairsE = materials.roofStairs(Direction.WEST);
        BlockState torch  = Blocks.TORCH.defaultBlockState();
        BlockState lowerLadder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
        BlockState upperLadder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH);

        // ── Main tower shell (2-6, 0-17, 2-6) ────────────────────────────────
        generateBox(level, box, 2, 0, 2, 6, 17, 6, cobble, cobble, false);

        // ── Clear upper interior ──────────────────────────────────────────────
        generateBox(level, box, 3, 13, 3, 5, 14, 5, air, air, false);

        // ── Battlements: merlon gaps ──────────────────────────────────────────
        generateBox(level, box, 2, 16, 3, 6, 17, 5, air, air, false);   // N-S gap
        generateBox(level, box, 3, 16, 2, 5, 17, 6, air, air, false);   // E-W gap

        // Merlon teeth: N (z=1)
        generateBox(level, box, 3, 15, 1, 5, 16, 1, cobble, cobble, false);
        placeBlock(level, cobble, 4, 14, 1, box);
        placeBlock(level, cobble, 4, 17, 1, box);
        // Merlon teeth: S (z=7)
        generateBox(level, box, 3, 15, 7, 5, 16, 7, cobble, cobble, false);
        placeBlock(level, cobble, 4, 14, 7, box);
        placeBlock(level, cobble, 4, 17, 7, box);
        // Merlon teeth: W (x=1)
        generateBox(level, box, 1, 15, 3, 1, 16, 5, cobble, cobble, false);
        placeBlock(level, cobble, 1, 14, 4, box);
        placeBlock(level, cobble, 1, 17, 4, box);
        // Merlon teeth: E (x=7)
        generateBox(level, box, 7, 15, 3, 7, 16, 5, cobble, cobble, false);
        placeBlock(level, cobble, 7, 14, 4, box);
        placeBlock(level, cobble, 7, 17, 4, box);

        // ── Fence corner posts at y=18 ────────────────────────────────────────
        placeBlock(level, fence, 2, 18, 2, box);
        placeBlock(level, fence, 2, 18, 6, box);
        placeBlock(level, fence, 6, 18, 2, box);
        placeBlock(level, fence, 6, 18, 6, box);

        // ── Biome roof at y=19-20 ─────────────────────────────────────────────
        generateBox(level, box, 2, 19, 2, 6, 19, 6, stone, stone, false);
        generateBox(level, box, 3, 20, 3, 5, 20, 5, stone, stone, false);

        // ── Pyramidal stair overhang ──────────────────────────────────────────
        // N face (ascending north, z decreases outward)
        for (int sx = 2; sx <= 6; sx++) placeBlock(level, stairsN, sx, 19, 1, box);
        for (int sx = 2; sx <= 6; sx++) placeBlock(level, stairsN, sx, 20, 2, box);
        for (int sx = 3; sx <= 5; sx++) placeBlock(level, stairsN, sx, 21, 3, box);
        // S face
        for (int sx = 2; sx <= 6; sx++) placeBlock(level, stairsS, sx, 19, 7, box);
        for (int sx = 2; sx <= 6; sx++) placeBlock(level, stairsS, sx, 20, 6, box);
        for (int sx = 3; sx <= 5; sx++) placeBlock(level, stairsS, sx, 21, 5, box);
        // W face
        for (int sz = 2; sz <= 6; sz++) placeBlock(level, stairsW, 1, 19, sz, box);
        for (int sz = 2; sz <= 6; sz++) placeBlock(level, stairsW, 2, 20, sz, box);
        for (int sz = 3; sz <= 5; sz++) placeBlock(level, stairsW, 3, 21, sz, box);
        // E face
        for (int sz = 2; sz <= 6; sz++) placeBlock(level, stairsE, 7, 19, sz, box);
        for (int sz = 2; sz <= 6; sz++) placeBlock(level, stairsE, 6, 20, sz, box);
        for (int sz = 3; sz <= 5; sz++) placeBlock(level, stairsE, 5, 21, sz, box);
        placeRoofCornerStairs(level, box, stairsW, stairsE);
        // Peak slab
        placeBlock(level, slab, 4, 22, 4, box);

        // ── Arrow slits (air columns on corner/face walls) ────────────────────
        // Corners
        generateBox(level, box, 2, 6, 2, 2, 14, 2, air, air, false);
        generateBox(level, box, 6, 6, 2, 6, 14, 2, air, air, false);
        generateBox(level, box, 6, 6, 6, 6, 14, 6, air, air, false);
        generateBox(level, box, 2, 6, 6, 2, 14, 6, air, air, false);
        // Center slits
        generateBox(level, box, 4, 6, 2, 4, 12, 2, air, air, false);
        generateBox(level, box, 4, 6, 6, 4, 12, 6, air, air, false);
        generateBox(level, box, 6, 6, 4, 6, 12, 4, air, air, false);
        generateBox(level, box, 2, 6, 4, 2, 12, 4, air, air, false);

        // ── Mid floor at y=9 ─────────────────────────────────────────────────
        // (main shell already fills this, but clear the inner cross for headroom)
        generateBox(level, box, 3, 10, 3, 5, 12, 5, air, air, false);

        // ── Entrance vestibules (N/S/E/W, 3-wide, y=0-4) ─────────────────────
        generateBox(level, box, 3, 0, 1, 5, 4, 1, cobble, cobble, false);
        generateBox(level, box, 4, 1, 1, 4, 3, 1, air, air, false);   // N door

        generateBox(level, box, 3, 0, 7, 5, 4, 7, cobble, cobble, false);
        generateBox(level, box, 4, 1, 7, 4, 3, 7, air, air, false);   // S door

        generateBox(level, box, 1, 0, 3, 1, 4, 5, cobble, cobble, false);
        generateBox(level, box, 1, 1, 4, 1, 3, 4, air, air, false);   // W door

        generateBox(level, box, 7, 0, 3, 7, 4, 5, cobble, cobble, false);
        generateBox(level, box, 7, 1, 4, 7, 3, 4, air, air, false);   // E door

        // ── Interior entrance passage (z=2-3 carved for N access) ─────────────
        generateBox(level, box, 4, 1, 2, 4, 2, 3, air, air, false);

        // ── Torches ───────────────────────────────────────────────────────────
        placeBlock(level, torch, 3, 2, 4, box);
        placeBlock(level, torch, 5, 2, 4, box);
        placeBlock(level, torch, 4, 14, 3, box);
        placeBlock(level, torch, 5, 16, 5, box);

        for (int ly = 1; ly <= 12; ly++) placeBlock(level, lowerLadder, 4, ly, 4, box);
        for (int ly = 10; ly <= 15; ly++) placeBlock(level, upperLadder, 3, ly, 5, box);

        // ── Loot chest on mid platform ───────────────────────────────────────
        createChest(level, box, random, 5, 10, 5, LOOT);

        // ── Foundation below y=0 ─────────────────────────────────────────────
        for (int bx = 1; bx < 7; bx++) {
            for (int bz = 1; bz < 7; bz++) {
                fillColumnDown(level, cobble, bx, -1, bz, box);
            }
        }

        // ── Guard spawns (3 guards on the top parapet) ────────────────────────
        spawnGuards(level, box, 4, 16, 5, 3);
    }

    private void placeRoofCornerStairs(WorldGenLevel level, BoundingBox box,
                                       BlockState westFace, BlockState eastFace) {
        placeBlock(level, westFace.setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT), 2, 20, 2, box);
        placeBlock(level, eastFace.setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT), 6, 20, 2, box);
        placeBlock(level, westFace.setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT), 2, 20, 6, box);
        placeBlock(level, eastFace.setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT), 6, 20, 6, box);

        placeBlock(level, westFace.setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT), 3, 21, 3, box);
        placeBlock(level, eastFace.setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT), 5, 21, 3, box);
        placeBlock(level, westFace.setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT), 3, 21, 5, box);
        placeBlock(level, eastFace.setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT), 5, 21, 5, box);
    }

    private void spawnGuards(WorldGenLevel level, BoundingBox box,
                              int lx, int ly, int lz, int count) {
        int[][] offsets = {{0, 0}, {-1, -1}, {1, -1}, {-1, 0}, {1, 0}, {0, 1}};
        for (int i = 0; i < count; i++) {
            int[] offset = offsets[i % offsets.length];
            int wx = boundingBox.minX() + lx + offset[0];
            int wy = boundingBox.minY() + ly;
            int wz = boundingBox.minZ() + lz + offset[1];
            if (!box.isInside(new BlockPos(wx, wy, wz))) continue;
            VillageGuardEntity guard = WitcheryEntities.VILLAGE_GUARD.get()
                    .create(level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (guard == null) continue;
            guard.setPersistenceRequired();
            guard.setPos(wx + 0.5, wy, wz + 0.5);
            guard.finalizeSpawn(level, new net.minecraft.world.DifficultyInstance(level.getDifficulty(), 0L, 0L, 0.0F),
                    EntitySpawnReason.STRUCTURE, null);
            VillageResidentMarker.mark(guard, "village_watch_tower", boundingBox);
            level.addFreshEntityWithPassengers(guard);
        }
    }
}
