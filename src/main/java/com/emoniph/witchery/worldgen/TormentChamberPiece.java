package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * 17×7×17 Torment chamber — mycelium floor, stone brick walls, bedrock ceiling,
 * 4 corridor exits (N/S/E/W), soul fire torches, and a loot chest.
 */
public class TormentChamberPiece extends StructurePiece {

    private static final ResourceKey<LootTable> LOOT_TABLE =
            ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath("witchery", "chests/torment_chamber"));

    public TormentChamberPiece(StructurePieceType type, int x, int y, int z, RandomSource random) {
        super(type, 0, new BoundingBox(x, y, z, x + 16, y + 6, z + 16));
    }

    public TormentChamberPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(WitcheryStructures.TORMENT_CHAMBER_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {}

    @Override
    public void postProcess(WorldGenLevel level, StructureManager mgr, ChunkGenerator gen,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        BlockState mycelium  = Blocks.MYCELIUM.defaultBlockState();
        BlockState stoneBrick = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState crackedBrick = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        BlockState bedrock   = Blocks.BEDROCK.defaultBlockState();
        BlockState air       = Blocks.AIR.defaultBlockState();
        BlockState ironBars  = Blocks.IRON_BARS.defaultBlockState();

        // Y=0 — mycelium floor
        generateBox(level, box, 0, 0, 0, 16, 0, 16, mycelium, mycelium, false);

        // Y=1-5 — stone brick perimeter walls
        generateBox(level, box, 0, 1, 0, 16, 5, 16, stoneBrick, stoneBrick, false);

        // Hollow out interior
        generateBox(level, box, 1, 1, 1, 15, 5, 15, air, air, false);

        // Y=6 — bedrock ceiling
        generateBox(level, box, 0, 6, 0, 16, 6, 16, bedrock, bedrock, false);

        // Cracked bricks scattered on walls (eerie worn look)
        for (int i = 0; i < 20; i++) {
            int wx = random.nextInt(17), wz = random.nextInt(17);
            int wy = 1 + random.nextInt(5);
            placeBlock(level, crackedBrick, wx, wy, wz, box);
        }

        // ── Corridor exits — 3-wide, 3-tall openings at N/S/E/W midpoints ──────
        // North (Z=0) and South (Z=16)
        for (int dx = 7; dx <= 9; dx++) {
            for (int dy = 1; dy <= 3; dy++) {
                placeBlock(level, air, dx, dy, 0,  box);
                placeBlock(level, air, dx, dy, 16, box);
            }
        }
        // East (X=16) and West (X=0)
        for (int dz = 7; dz <= 9; dz++) {
            for (int dy = 1; dy <= 3; dy++) {
                placeBlock(level, air, 0,  dy, dz, box);
                placeBlock(level, air, 16, dy, dz, box);
            }
        }

        // ── Soul fire torches at corners ─────────────────────────────────────
        placeBlock(level, Blocks.SOUL_FIRE.defaultBlockState(), 2,  1, 2,  box);
        placeBlock(level, Blocks.SOUL_FIRE.defaultBlockState(), 14, 1, 2,  box);
        placeBlock(level, Blocks.SOUL_FIRE.defaultBlockState(), 2,  1, 14, box);
        placeBlock(level, Blocks.SOUL_FIRE.defaultBlockState(), 14, 1, 14, box);

        // Mycelium base under soul fire (needs solid block)
        placeBlock(level, mycelium, 2,  0, 2,  box);
        placeBlock(level, mycelium, 14, 0, 2,  box);
        placeBlock(level, mycelium, 2,  0, 14, box);
        placeBlock(level, mycelium, 14, 0, 14, box);

        // ── Iron bar pillars ─────────────────────────────────────────────────
        for (int dy = 1; dy <= 4; dy++) {
            placeBlock(level, ironBars, 4,  dy, 4,  box);
            placeBlock(level, ironBars, 12, dy, 4,  box);
            placeBlock(level, ironBars, 4,  dy, 12, box);
            placeBlock(level, ironBars, 12, dy, 12, box);
        }

        // ── Loot chest in NW corner ───────────────────────────────────────────
        createChest(level, box, random, 2, 1, 2, LOOT_TABLE);

        // ── Optional second chest in SE corner (50% chance) ──────────────────
        if (random.nextBoolean()) {
            createChest(level, box, random, 14, 1, 14, LOOT_TABLE);
        }
    }
}
