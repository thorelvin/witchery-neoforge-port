package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Source-shaped 4x6x5 ComponentVillageWitchHut.
 */
public class WitchHutPiece extends StructurePiece {

    private static final ResourceKey<LootTable> LOOT_TABLE =
            ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath("witchery", "chests/witch_hut"));

    public WitchHutPiece(StructurePieceType type, int x, int y, int z) {
        super(type, 0, new BoundingBox(x, y, z, x + 3, y + 5, z + 4));
        setOrientation(Direction.NORTH);
    }

    public WitchHutPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(WitcheryStructures.WITCH_HUT_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {}

    @Override
    public void postProcess(WorldGenLevel level, StructureManager mgr, ChunkGenerator gen,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        VillageBuildingMaterials materials = VillageBuildingMaterials.forVillage(level, boundingBox, box);
        BlockState wall = materials.wall();
        BlockState trim = materials.trim();
        BlockState foundation = materials.floor();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState glassSide = VillageBuildingMaterials.paneNorthSouth();
        BlockState rowanLog = WitcheryBlocks.ROWAN_LOG.get().defaultBlockState();

        generateBox(level, box, 1, 1, 1, 3, 5, 4, air, air, false);
        generateBox(level, box, 0, 0, 0, 3, 0, 4, foundation, foundation, false);
        generateBox(level, box, 1, 0, 1, 2, 0, 3, foundation, foundation, false);

        generateBox(level, box, 1, 5, 1, 2, 5, 3, wall, wall, false);
        for (int x = 0; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                placeBlock(level, wall, x, 4, z, box);
            }
        }
        placeBlock(level, wall, 1, 4, 0, box);
        placeBlock(level, wall, 2, 4, 0, box);
        placeBlock(level, wall, 1, 4, 4, box);
        placeBlock(level, wall, 2, 4, 4, box);

        generateBox(level, box, 0, 1, 0, 0, 3, 0, trim, trim, false);
        generateBox(level, box, 3, 1, 0, 3, 3, 0, trim, trim, false);
        generateBox(level, box, 0, 1, 4, 0, 3, 4, trim, trim, false);
        generateBox(level, box, 3, 1, 4, 3, 3, 4, trim, trim, false);
        generateBox(level, box, 0, 1, 1, 0, 3, 3, wall, wall, false);
        generateBox(level, box, 3, 1, 1, 3, 3, 3, wall, wall, false);
        generateBox(level, box, 1, 1, 0, 2, 3, 0, wall, wall, false);
        generateBox(level, box, 1, 1, 4, 2, 3, 4, wall, wall, false);
        placeBlock(level, glassSide, 0, 2, 2, box);
        placeBlock(level, glassSide, 3, 2, 2, box);

        placeBlock(level, air, 1, 1, 0, box);
        placeBlock(level, air, 1, 2, 0, box);
        placeBlock(level, materials.door(Direction.SOUTH, DoubleBlockHalf.LOWER), 1, 1, 0, box);
        placeBlock(level, materials.door(Direction.SOUTH, DoubleBlockHalf.UPPER), 1, 2, 0, box);
        placeFrontStep(level, box, materials);

        placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), 1, 1, 3, box);
        placeBlock(level, rowanLog, 2, 1, 3, box);
        placeBlock(level, Blocks.FLOWER_POT.defaultBlockState(), 2, 2, 3, box);
        createChest(level, box, random, 2, 1, 2, LOOT_TABLE);

        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 5; z++) {
                fillColumnDown(level, foundation, x, -1, z, box);
            }
        }

        BlockPos spawnPos = new BlockPos(
                boundingBox.minX() + 1,
                boundingBox.minY() + 1,
                boundingBox.minZ() + 2);
        if (!box.isInside(spawnPos)) return;
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

    private void placeFrontStep(WorldGenLevel level, BoundingBox box, VillageBuildingMaterials materials) {
        BlockPos stepPos = getWorldPos(1, 0, -1);
        if (!box.isInside(stepPos)) {
            return;
        }
        if (level.getBlockState(stepPos).isAir() && !level.getBlockState(stepPos.below()).isAir()) {
            level.setBlock(stepPos, materials.roofStairs(Direction.NORTH), 2);
        }
    }
}
