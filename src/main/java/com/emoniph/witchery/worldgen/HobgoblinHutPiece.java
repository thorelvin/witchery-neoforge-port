package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.entity.HobgoblinEntity;
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
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * 5×5×5 Hobgoblin Hut placed in forest biomes.
 * Contains 1–2 Hobgoblins and a loot chest.
 */
public class HobgoblinHutPiece extends StructurePiece {

    private static final ResourceKey<LootTable> LOOT_TABLE =
            ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath("witchery", "chests/hobgoblin_hut"));

    public HobgoblinHutPiece(StructurePieceType type, int x, int y, int z) {
        super(type, 0, new BoundingBox(x, y, z, x + 4, y + 4, z + 4));
        setOrientation(net.minecraft.core.Direction.NORTH);
    }

    public HobgoblinHutPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(WitcheryStructures.HOBGOBLIN_HUT_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {}

    @Override
    public void postProcess(WorldGenLevel level, StructureManager mgr, ChunkGenerator gen,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {

        BlockState cobble  = Blocks.COBBLESTONE.defaultBlockState();
        BlockState planks  = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState log     = Blocks.OAK_LOG.defaultBlockState();
        BlockState air     = Blocks.AIR.defaultBlockState();
        BlockState glass   = Blocks.GLASS_PANE.defaultBlockState();
        BlockState fence   = Blocks.OAK_FENCE.defaultBlockState();
        BlockState slab    = Blocks.STONE_SLAB.defaultBlockState();

        // Y=0 — cobblestone foundation
        generateBox(level, box, 0, 0, 0, 4, 0, 4, cobble, cobble, false);

        // Y=1-3 — oak plank walls, hollow inside
        for (int y = 1; y <= 3; y++) {
            generateBox(level, box, 0, y, 0, 4, y, 4, planks, planks, false);
            generateBox(level, box, 1, y, 1, 3, y, 3, air,    air,    false);
        }

        // Oak log corner pillars
        for (int y = 1; y <= 3; y++) {
            placeBlock(level, log, 0, y, 0, box); placeBlock(level, log, 4, y, 0, box);
            placeBlock(level, log, 0, y, 4, box); placeBlock(level, log, 4, y, 4, box);
        }

        // Y=4 — plank roof
        generateBox(level, box, 0, 4, 0, 4, 4, 4, planks, planks, false);

        // Door opening: X=2, Z=0, Y=1-2
        placeBlock(level, air, 2, 1, 0, box);
        placeBlock(level, air, 2, 2, 0, box);

        // Windows: glass pane on each side
        placeBlock(level, glass, 1, 2, 0, box);
        placeBlock(level, glass, 3, 2, 0, box);
        placeBlock(level, glass, 0, 2, 2, box);
        placeBlock(level, glass, 4, 2, 2, box);
        placeBlock(level, glass, 1, 2, 4, box);
        placeBlock(level, glass, 3, 2, 4, box);

        // Torches
        placeBlock(level, Blocks.WALL_TORCH.defaultBlockState(), 1, 2, 1, box);
        placeBlock(level, Blocks.WALL_TORCH.defaultBlockState(), 3, 2, 3, box);

        // Interior furnishings (Y=1)
        placeBlock(level, slab, 1, 1, 1, box); // table-slab
        placeBlock(level, slab, 3, 1, 1, box);
        placeBlock(level, fence, 2, 1, 3, box); // fence "chair"

        // Loot chest at back corner
        createChest(level, box, random, 3, 1, 3, LOOT_TABLE);

        // Spawn 1–2 hobgoblins
        int count = 1 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            BlockPos spawnPos = new BlockPos(
                    boundingBox.minX() + 1 + random.nextInt(3),
                    boundingBox.minY() + 1,
                    boundingBox.minZ() + 1 + random.nextInt(3));
            HobgoblinEntity hobgoblin = WitcheryEntities.HOBGOBLIN.get().create(level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (hobgoblin != null) {
                hobgoblin.setPersistenceRequired();
                hobgoblin.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                hobgoblin.finalizeSpawn(level, level.getLevel().getCurrentDifficultyAt(spawnPos),
                        EntitySpawnReason.STRUCTURE, null);
                level.addFreshEntityWithPassengers(hobgoblin);
            }
        }
    }
}
