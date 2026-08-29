package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Coven stone circle. Source parity: ComponentCoven (1.7.10-0.24.1).
 *
 * 11×4×11 ring of 2-high standing stones surrounding a 3×3 chiseled altar.
 * Materials are biome-adaptive: sandstone in desert/mesa biomes.
 * A Coven Witch has the source 90% resident spawn chance at the altar; vanilla
 * Witch spawners, a loot dispenser, and the old cobweb ring details sit like the
 * original component.
 */
public class CovenCirclePiece extends StructurePiece {

    public CovenCirclePiece(StructurePieceType type, int x, int y, int z) {
        super(type, 0, new BoundingBox(x, y, z, x + 10, y + 3, z + 10));
        setOrientation(net.minecraft.core.Direction.NORTH);
    }

    public CovenCirclePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(WitcheryStructures.COVEN_CIRCLE_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {}

    @Override
    public void postProcess(WorldGenLevel level, StructureManager mgr, ChunkGenerator gen,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        if (!hasAboveWaterFootprint(level)) return;

        // ── Clear trees and vegetation in the 11×11 footprint ────────────────────
        // The bounding box height (y=0–3) only covers the structure itself; trees extend
        // higher and have trunks/leaves that the generateBox air pass misses above y=3.
        // Restrict to the current chunk (postProcess is called once per overlapping chunk).
        for (int cx = 0; cx <= 10; cx++) {
            for (int cz = 0; cz <= 10; cz++) {
                int worldX = boundingBox.minX() + cx;
                int worldZ = boundingBox.minZ() + cz;
                if ((worldX >> 4) != chunkPos.x || (worldZ >> 4) != chunkPos.z) continue;
                int floorY = boundingBox.minY() + 1;
                for (int wy = floorY; wy <= floorY + 24; wy++) {
                    BlockPos p = new BlockPos(worldX, wy, worldZ);
                    BlockState bs = level.getBlockState(p);
                    if (bs.isAir()) continue;
                    if (bs.is(BlockTags.LEAVES) || bs.is(BlockTags.LOGS)
                            || bs.is(BlockTags.FLOWERS) || bs.is(BlockTags.SAPLINGS)
                            || bs.canBeReplaced()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        // ── Biome-adaptive materials (source: checks for desert/mesa biomes) ──
        boolean isDesert = isDesertBiome(level, boundingBox.minX() + 5, boundingBox.minZ() + 5);

        BlockState stone  = isDesert ? Blocks.SANDSTONE.defaultBlockState()
                                     : Blocks.COBBLESTONE.defaultBlockState();
        BlockState brick  = isDesert ? Blocks.SMOOTH_SANDSTONE.defaultBlockState()
                                     : Blocks.CHISELED_STONE_BRICKS.defaultBlockState();

        BlockState altar  = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
        BlockState cobweb = Blocks.COBWEB.defaultBlockState();
        BlockState air    = Blocks.AIR.defaultBlockState();
        // Source ComponentCoven used grass as the visible non-desert surface.
        // Keep the modern support stone-backed so uneven terrain cannot expose
        // a one-block dirt skirt underneath the stone circle.
        BlockState floorState = isDesert ? Blocks.SAND.defaultBlockState()
                                         : Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState supportState = isDesert ? Blocks.SAND.defaultBlockState()
                                           : stone;

        // ── Clear interior (source: func_74878_a cross pattern) ───────────────
        generateBox(level, box, 3, 1, 0,  7, 3, 10, air, air, false);
        generateBox(level, box, 0, 1, 3, 10, 3,  7, air, air, false);
        generateBox(level, box, 1, 1, 1,  9, 3,  9, air, air, false);

        // ── Floor — force-placed via level.setBlock to bypass StructurePiece's
        //    canBeReplaced() guard, which silently skips solid terrain blocks.
        //    Without this the terrain grass block at y=0 is never replaced and
        //    the stones at y=1 appear to float one block above the terrain.
        int[][] floorAreas = {{3,0,7,0,10},{0,0,10,3,7},{1,0,9,1,9}};
        for (int[] a : floorAreas) {
            for (int fx = a[0]; fx <= a[2]; fx++) {
                for (int fz = a[3]; fz <= a[4]; fz++) {
                    BlockPos fp = new BlockPos(
                            boundingBox.minX() + fx,
                            boundingBox.minY(),
                            boundingBox.minZ() + fz);
                    if (box.isInside(fp)) level.setBlock(fp, floorState, 3);
                }
            }
        }

        // ── Altar — 3×3 chiseled bricks centred at (5,5) ─────────────────────
        // Source: altarBrick = STONE_BRICK meta 3 (chiseled), center = CAULDRON equiv
        for (int ax = 4; ax <= 6; ax++) {
            for (int az = 4; az <= 6; az++) {
                placeBlock(level, altar, ax, 1, az, box);
            }
        }
        // ComponentCoven places full water at metadata 0 in the altar centre.
        // Use a direct write like the floor: StructurePiece's guarded placement can
        // silently skip this fluid during world generation.
        BlockPos altarCenter = localPos(5, 1, 5);
        if (box.isInside(altarCenter)) {
            level.setBlock(altarCenter, Blocks.WATER.defaultBlockState(), 3);
        }

        // ── Standing stone ring — y=1 (base, cobblestone) ────────────────────
        // Source exactly — 18 base positions forming the oval ring
        placeBlock(level, stone, 1, 1,  2, box);
        placeBlock(level, stone, 2, 1,  1, box);
        placeBlock(level, cobweb, 3, 1,  0, box);
        placeBlock(level, stone, 4, 1,  0, box);
        placeBlock(level, stone, 6, 1,  0, box);
        placeBlock(level, stone, 8, 1,  1, box);
        placeBlock(level, stone, 9, 1,  2, box);
        placeBlock(level, stone,10, 1,  4, box);
        placeBlock(level, stone,10, 1,  6, box);
        placeBlock(level, stone, 9, 1,  8, box);
        placeBlock(level, stone, 8, 1,  9, box);
        placeBlock(level, stone, 6, 1, 10, box);
        placeBlock(level, stone, 4, 1, 10, box);
        placeBlock(level, stone, 2, 1,  9, box);
        placeBlock(level, stone, 1, 1,  8, box);
        placeBlock(level, stone, 0, 1,  4, box);
        placeBlock(level, stone, 0, 1,  6, box);

        // ── Ring y=2 (middle, brick caps on corners) ─────────────────────────
        placeBlock(level, brick, 1, 2,  2, box);
        placeBlock(level, brick, 2, 2,  1, box);
        placeBlock(level, stone, 4, 2,  0, box);
        placeBlock(level, stone, 6, 2,  0, box);
        placeBlock(level, brick, 8, 2,  1, box);
        placeBlock(level, brick, 9, 2,  2, box);
        placeBlock(level, stone,10, 2,  4, box);
        placeBlock(level, stone,10, 2,  6, box);
        placeBlock(level, brick, 9, 2,  8, box);
        placeBlock(level, cobweb,10, 2,  7, box);
        placeBlock(level, brick, 8, 2,  9, box);
        placeBlock(level, stone, 6, 2, 10, box);
        placeBlock(level, stone, 4, 2, 10, box);
        placeBlock(level, brick, 2, 2,  9, box);
        placeBlock(level, brick, 1, 2,  8, box);
        placeBlock(level, stone, 0, 2,  4, box);
        placeBlock(level, stone, 0, 2,  6, box);

        // ── Ring y=3 (top, mixed stone/brick lintels) ─────────────────────────
        placeBlock(level, stone, 1, 3,  2, box);
        placeBlock(level, stone, 2, 3,  1, box);
        placeBlock(level, brick, 3, 3,  1, box);
        placeBlock(level, brick, 4, 3,  0, box);
        placeBlock(level, brick, 5, 3,  0, box);
        placeBlock(level, brick, 6, 3,  0, box);
        placeBlock(level, brick, 7, 3,  1, box);
        placeBlock(level, stone, 8, 3,  1, box);
        placeBlock(level, stone, 9, 3,  2, box);
        placeBlock(level, brick, 9, 3,  3, box);
        placeBlock(level, brick,10, 3,  4, box);
        placeBlock(level, brick,10, 3,  5, box);
        placeBlock(level, brick,10, 3,  6, box);
        placeBlock(level, brick, 9, 3,  7, box);
        placeBlock(level, stone, 9, 3,  8, box);
        placeBlock(level, stone, 8, 3,  9, box);
        placeBlock(level, brick, 7, 3,  9, box);
        placeBlock(level, brick, 6, 3, 10, box);
        placeBlock(level, brick, 5, 3, 10, box);
        placeBlock(level, brick, 4, 3, 10, box);
        placeBlock(level, brick, 3, 3,  9, box);
        placeBlock(level, stone, 2, 3,  9, box);
        placeBlock(level, stone, 1, 3,  8, box);
        placeBlock(level, brick, 1, 3,  3, box);
        placeBlock(level, brick, 0, 3,  4, box);
        placeBlock(level, brick, 0, 3,  5, box);
        placeBlock(level, brick, 0, 3,  6, box);
        placeBlock(level, cobweb, 0, 3,  7, box);
        placeBlock(level, brick, 1, 3,  7, box);

        // ── Foundation downward ──────────────────────────────────────────────
        for (int fx = 0; fx <= 10; fx++) {
            for (int fz = 0; fz <= 10; fz++) {
                fillColumnDown(level, supportState, fx, -1, fz, box);
            }
        }

        // Hidden altar internals.
        // Source ComponentCoven hides a loot dispenser and two vanilla Witch spawners
        // under the 3x3 altar, leaving the Coven Witch as the visible resident.
        placeLootDispenser(level, box, random, 5, 0, 5);
        placeSpawner(level, box, random, 4, 0, 4, EntityType.WITCH);
        placeSpawner(level, box, random, 6, 0, 6, EntityType.WITCH);

        // ── Coven Witch spawn at altar ────────────────────────────────────────
        BlockPos spawnPos = new BlockPos(
                boundingBox.minX() + 7,
                boundingBox.minY() + 1,
                boundingBox.minZ() + 4);
        if (box.isInside(spawnPos) && random.nextInt(10) != 0) {
            CovenWitchEntity witch = WitcheryEntities.COVEN_WITCH.get()
                    .create(level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (witch != null) {
                witch.setPersistenceRequired();
                witch.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                witch.finalizeSpawn(level,
                        new net.minecraft.world.DifficultyInstance(level.getDifficulty(), 0L, 0L, 0.0F),
                        EntitySpawnReason.STRUCTURE, null);
                level.addFreshEntityWithPassengers(witch);
            }
        }
    }

    private boolean hasAboveWaterFootprint(WorldGenLevel level) {
        int floorY = boundingBox.minY();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
                int wx = boundingBox.minX() + x;
                int wz = boundingBox.minZ() + z;
                if (isWaterBiome(level, wx, wz)) {
                    return false;
                }

                int worldSurface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, wx, wz);
                int topY = Math.max(worldSurface, floorY + 1);
                for (int y = floorY - 1; y <= topY; y++) {
                    cursor.set(wx, y, wz);
                    BlockState state = level.getBlockState(cursor);
                    if (!state.getFluidState().isEmpty() || isWaterSurfaceBlock(state)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isWaterSurfaceBlock(BlockState state) {
        return state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.FROSTED_ICE);
    }

    private static boolean isWaterBiome(WorldGenLevel level, int x, int z) {
        net.minecraft.core.Holder<Biome> biome = level.getBiome(new BlockPos(x, 64, z));
        return biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER);
    }

    private static boolean isDesertBiome(WorldGenLevel level, int x, int z) {
        net.minecraft.core.Holder<Biome> biome = level.getBiome(new BlockPos(x, 64, z));
        return biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BADLANDS);
    }

    private void placeLootDispenser(WorldGenLevel level, BoundingBox box, RandomSource random,
                                    int localX, int localY, int localZ) {
        BlockPos blockPos = localPos(localX, localY, localZ);
        if (!box.isInside(blockPos)) return;

        level.setBlock(blockPos,
                Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST),
                3);
        if (level.getBlockEntity(blockPos) instanceof DispenserBlockEntity dispenser) {
            dispenser.setLootTable(BuiltInLootTables.ABANDONED_MINESHAFT, random.nextLong());
            dispenser.setChanged();
        }
    }

    private void placeSpawner(WorldGenLevel level, BoundingBox box, RandomSource random,
                              int localX, int localY, int localZ, EntityType<?> entityType) {
        BlockPos blockPos = localPos(localX, localY, localZ);
        if (!box.isInside(blockPos)) return;

        level.setBlock(blockPos, Blocks.SPAWNER.defaultBlockState(), 3);
        if (level.getBlockEntity(blockPos) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(entityType, random);
            spawner.setChanged();
        }
    }

    private BlockPos localPos(int localX, int localY, int localZ) {
        return new BlockPos(
                boundingBox.minX() + localX,
                boundingBox.minY() + localY,
                boundingBox.minZ() + localZ);
    }
}
