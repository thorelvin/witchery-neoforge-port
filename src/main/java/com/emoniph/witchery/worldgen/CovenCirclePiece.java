package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Coven stone circle. Source parity: ComponentCoven (1.7.10-0.24.1).
 *
 * 11×4×11 ring of 2-high standing stones surrounding a 3×3 chiseled altar.
 * Materials are biome-adaptive: sandstone in desert/mesa biomes.
 * One Coven Witch spawns at the altar.
 */
public class CovenCirclePiece extends StructurePiece {

    private static final ResourceKey<LootTable> LOOT =
            ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath("witchery", "chests/coven_circle"));

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
        BlockState brick  = isDesert ? Blocks.CUT_SANDSTONE.defaultBlockState()
                                     : Blocks.CHISELED_STONE_BRICKS.defaultBlockState();

        BlockState mossy  = isDesert ? Blocks.SANDSTONE.defaultBlockState()
                                     : Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        BlockState altar  = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
        BlockState air    = Blocks.AIR.defaultBlockState();
        BlockState grass  = isDesert ? Blocks.SAND.defaultBlockState()
                                     : Blocks.GRASS_BLOCK.defaultBlockState();

        // ── Clear interior (source: func_74878_a cross pattern) ───────────────
        generateBox(level, box, 3, 1, 0,  7, 3, 10, air, air, false);
        generateBox(level, box, 0, 1, 3, 10, 3,  7, air, air, false);
        generateBox(level, box, 1, 1, 1,  9, 3,  9, air, air, false);

        // ── Floor (source: func_151549_a cross) ───────────────────────────────
        generateBox(level, box, 3, 0, 0,  7, 0, 10, grass, grass, false);
        generateBox(level, box, 0, 0, 3, 10, 0,  7, grass, grass, false);
        generateBox(level, box, 1, 0, 1,  9, 0,  9, grass, grass, false);

        // ── Altar — 3×3 chiseled bricks centred at (5,5) ─────────────────────
        // Source: altarBrick = STONE_BRICK meta 3 (chiseled), center = CAULDRON equiv
        for (int ax = 4; ax <= 6; ax++) {
            for (int az = 4; az <= 6; az++) {
                placeBlock(level, altar, ax, 1, az, box);
            }
        }
        // Center altar top: use Witchery Kettle as the cauldron-like focal point
        placeBlock(level, WitcheryBlocks.KETTLE.get().defaultBlockState(), 5, 1, 5, box);

        // ── Standing stone ring — y=1 (base, cobblestone) ────────────────────
        // Source exactly — 18 base positions forming the oval ring
        placeBlock(level, stone, 1, 1,  2, box);
        placeBlock(level, stone, 2, 1,  1, box);
        placeBlock(level, mossy, 3, 1,  0, box);   // mossy = special marker stone
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
        placeBlock(level, mossy, 0, 1,  7, box);   // W marker
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
        placeBlock(level, brick, 1, 3,  7, box);

        // ── Foundation downward ───────────────────────────────────────────────
        for (int fx = 0; fx <= 10; fx++) {
            for (int fz = 0; fz <= 10; fz++) {
                fillColumnDown(level, stone, fx, -1, fz, box);
            }
        }

        // ── Loot chest in altar (under the kettle) ────────────────────────────
        createChest(level, box, random, 5, 0, 5, LOOT);

        // ── Coven Witch spawn at altar ────────────────────────────────────────
        BlockPos spawnPos = new BlockPos(
                boundingBox.minX() + 7,
                boundingBox.minY() + 1,
                boundingBox.minZ() + 4);
        if (box.isInside(spawnPos)) {
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

    private static boolean isDesertBiome(WorldGenLevel level, int x, int z) {
        net.minecraft.core.Holder<Biome> biome = level.getBiome(new BlockPos(x, 64, z));
        return biome.is(net.minecraft.tags.BiomeTags.IS_BADLANDS) ||
               biome.value().getBaseTemperature() > 1.5f;
    }
}
