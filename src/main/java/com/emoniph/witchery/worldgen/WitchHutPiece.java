package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
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
 * 7×7×7 witch hut placed on the surface.
 * Orientation: front door at Z=0, X=3.
 * Contains a Coven Witch and a loot chest.
 */
public class WitchHutPiece extends StructurePiece {

    private static final ResourceKey<LootTable> LOOT_TABLE =
            ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath("witchery", "chests/witch_hut"));

    public WitchHutPiece(StructurePieceType type, int x, int y, int z) {
        super(type, 0, new BoundingBox(x, y, z, x + 6, y + 6, z + 6));
        setOrientation(net.minecraft.core.Direction.NORTH);
    }

    public WitchHutPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(WitcheryStructures.WITCH_HUT_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {}

    @Override
    public void postProcess(WorldGenLevel level, StructureManager mgr, ChunkGenerator gen,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        BlockState planks  = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState cobble  = Blocks.COBBLESTONE.defaultBlockState();
        BlockState air     = Blocks.AIR.defaultBlockState();
        BlockState glass   = Blocks.GLASS_PANE.defaultBlockState();
        BlockState log     = WitcheryBlocks.ROWAN_LOG.get().defaultBlockState();

        // Y=0 — cobblestone foundation
        generateBox(level, box, 0, 0, 0, 6, 0, 6, cobble, cobble, false);

        // Y=1-4 — oak plank walls (full perimeter, hollow interior)
        for (int y = 1; y <= 4; y++) {
            generateBox(level, box, 0, y, 0, 6, y, 6, planks, planks, false);
            generateBox(level, box, 1, y, 1, 5, y, 5, air, air, false);
        }

        // Witch log corner pillars (Y=1-4)
        for (int y = 1; y <= 4; y++) {
            placeBlock(level, log, 0, y, 0, box); placeBlock(level, log, 6, y, 0, box);
            placeBlock(level, log, 0, y, 6, box); placeBlock(level, log, 6, y, 6, box);
        }

        // Y=1-2 — front door opening (X=3, Z=0)
        placeBlock(level, air, 3, 1, 0, box);
        placeBlock(level, air, 3, 2, 0, box);

        // Y=2 — glass pane windows
        placeBlock(level, glass, 1, 2, 0, box); placeBlock(level, glass, 5, 2, 0, box);
        placeBlock(level, glass, 1, 2, 6, box); placeBlock(level, glass, 5, 2, 6, box);
        placeBlock(level, glass, 0, 2, 2, box); placeBlock(level, glass, 0, 2, 4, box);
        placeBlock(level, glass, 6, 2, 2, box); placeBlock(level, glass, 6, 2, 4, box);

        // Y=5 — oak plank ceiling
        generateBox(level, box, 0, 5, 0, 6, 5, 6, planks, planks, false);

        // Y=6 — witch log roof ridge
        generateBox(level, box, 0, 6, 0, 6, 6, 6, log, log, false);

        // ── Interior furnishings (Y=1) ────────────────────────────────────────
        placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), 1, 1, 2, box);
        placeBlock(level, Blocks.BOOKSHELF.defaultBlockState(),      5, 1, 2, box);
        placeBlock(level, Blocks.FLOWER_POT.defaultBlockState(),     1, 1, 4, box);
        placeBlock(level, WitcheryBlocks.KETTLE.get().defaultBlockState(),     3, 1, 3, box);
        placeBlock(level, WitcheryBlocks.ALTAR.get().defaultBlockState(),      3, 1, 1, box);
        placeBlock(level, WitcheryBlocks.CANDELABRA.get().defaultBlockState(), 5, 1, 4, box);

        // Torches on the walls (Y=3)
        placeBlock(level, Blocks.WALL_TORCH.defaultBlockState(), 1, 3, 1, box);
        placeBlock(level, Blocks.WALL_TORCH.defaultBlockState(), 5, 3, 1, box);
        placeBlock(level, Blocks.WALL_TORCH.defaultBlockState(), 1, 3, 5, box);
        placeBlock(level, Blocks.WALL_TORCH.defaultBlockState(), 5, 3, 5, box);

        // ── Loot chest ────────────────────────────────────────────────────────
        createChest(level, box, random, 5, 1, 5, LOOT_TABLE);

        // ── Coven Witch spawn ─────────────────────────────────────────────────
        BlockPos spawnPos = new BlockPos(
                boundingBox.minX() + 3,
                boundingBox.minY() + 1,
                boundingBox.minZ() + 3);
        CovenWitchEntity witch = WitcheryEntities.COVEN_WITCH.get().create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (witch != null) {
            witch.setPersistenceRequired();
            witch.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            witch.finalizeSpawn(level, level.getLevel().getCurrentDifficultyAt(spawnPos), EntitySpawnReason.STRUCTURE, null);
            level.addFreshEntityWithPassengers(witch);
        }
    }
}
