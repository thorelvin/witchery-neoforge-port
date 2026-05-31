package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.entity.HobgoblinEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryStructures;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * Source-shaped 7x6x7 ComponentGoblinHut schematic with one resident hobgoblin.
 */
public class HobgoblinHutPiece extends StructurePiece {

    public HobgoblinHutPiece(StructurePieceType type, int x, int y, int z) {
        super(type, 0, new BoundingBox(x, y, z, x + 6, y + 5, z + 6));
        setOrientation(Direction.NORTH);
    }

    public HobgoblinHutPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(WitcheryStructures.HOBGOBLIN_HUT_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {}

    @Override
    public void postProcess(WorldGenLevel level, StructureManager mgr, ChunkGenerator gen,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, net.minecraft.core.BlockPos pos) {
        generateBox(level, box, 0, 0, 0, 6, 5, 6,
                Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);

        placeSourceSchematic(level, box);
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                fillColumnDown(level, Blocks.DIRT.defaultBlockState(), x, -1, z, box);
            }
        }
        spawnResident(level);
    }

    private void placeSourceSchematic(WorldGenLevel level, BoundingBox box) {
        placeBlock(level, stairs(Blocks.OAK_STAIRS.defaultBlockState(), 4), 0, 2, 2, box);
        placeBlock(level, Blocks.OAK_LOG.defaultBlockState(), 0, 3, 2, box);
        placeBlock(level, woodSlab(1), 0, 4, 2, box);
        placeBlock(level, stairs(Blocks.OAK_STAIRS.defaultBlockState(), 0), 0, 0, 3, box);
        placeBlock(level, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), 1), 0, 4, 3, box);
        placeBlock(level, stairs(Blocks.OAK_STAIRS.defaultBlockState(), 4), 0, 2, 4, box);
        placeBlock(level, Blocks.OAK_LOG.defaultBlockState(), 0, 3, 4, box);
        placeBlock(level, woodSlab(1), 0, 4, 4, box);
        placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 1, 0, 1, box);
        placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 1, 1, 1, box);
        placeBlock(level, Blocks.OAK_PLANKS.defaultBlockState(), 1, 2, 1, box);
        placeBlock(level, Blocks.OAK_LOG.defaultBlockState(), 1, 3, 1, box);
        placeBlock(level, woodSlab(1), 1, 4, 1, box);
        placeBlock(level, woodSlab(8), 1, 1, 2, box);
        placeBlock(level, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), 7), 1, 4, 2, box);
        placeBlock(level, stairs(Blocks.OAK_STAIRS.defaultBlockState(), 5), 1, 1, 3, box);
        placeBlock(level, door(0), 1, 2, 3, box);
        placeBlock(level, door(8), 1, 3, 3, box);
        placeBlock(level, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), 5), 1, 4, 3, box);
        placeBlock(level, woodSlab(8), 1, 1, 4, box);
        placeBlock(level, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), 6), 1, 4, 4, box);
        placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 1, 0, 5, box);
        placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 1, 1, 5, box);
        placeBlock(level, Blocks.OAK_PLANKS.defaultBlockState(), 1, 2, 5, box);
        placeBlock(level, Blocks.OAK_LOG.defaultBlockState(), 1, 3, 5, box);
        placeBlock(level, woodSlab(1), 1, 4, 5, box);
        placeBlock(level, stairs(Blocks.OAK_STAIRS.defaultBlockState(), 6), 2, 2, 0, box);
        placeBlock(level, Blocks.OAK_LOG.defaultBlockState(), 2, 3, 0, box);
        placeBlock(level, woodSlab(1), 2, 4, 0, box);
        placeBlock(level, woodSlab(8), 2, 1, 1, box);
        placeBlock(level, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), 5), 2, 4, 1, box);
        placeBlock(level, Blocks.COBBLESTONE_SLAB.defaultBlockState(), 2, 1, 2, box);
        placeBlock(level, woodSlab(9), 2, 4, 2, box);
        placeBlock(level, Blocks.COBBLESTONE_SLAB.defaultBlockState(), 2, 1, 3, box);
        placeBlock(level, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), 1), 2, 5, 3, box);
        placeBlock(level, Blocks.COBBLESTONE_SLAB.defaultBlockState(), 2, 1, 4, box);
        placeBlock(level, woodSlab(9), 2, 4, 4, box);
        placeBlock(level, woodSlab(8), 2, 1, 5, box);
        placeBlock(level, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), 5), 2, 4, 5, box);
        placeBlock(level, stairs(Blocks.OAK_STAIRS.defaultBlockState(), 7), 2, 2, 6, box);
        placeBlock(level, Blocks.OAK_LOG.defaultBlockState(), 2, 3, 6, box);
        placeBlock(level, woodSlab(1), 2, 4, 6, box);
        placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 3, 0, 0, box);
        placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 3, 1, 0, box);
        placeBlock(level, Blocks.OAK_PLANKS.defaultBlockState(), 3, 2, 0, box);
        placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 3, 3, 0, box);
        placeBlock(level, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), 3), 3, 4, 0, box);
        placeBlock(level, woodSlab(8), 3, 1, 1, box);
        placeBlock(level, woodSlab(2), 3, 2, 1, box);
        placeBlock(level, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), 7), 3, 4, 1, box);
        placeBlock(level, Blocks.COBBLESTONE_SLAB.defaultBlockState(), 3, 1, 2, box);
        placeBlock(level, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), 3), 3, 5, 2, box);
        placeBlock(level, Blocks.CAULDRON.defaultBlockState(), 3, 1, 3, box);
        placeBlock(level, Blocks.COBBLESTONE_SLAB.defaultBlockState(), 3, 1, 4, box);
        placeBlock(level, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), 2), 3, 5, 4, box);
        placeBlock(level, woodSlab(8), 3, 1, 5, box);
        placeBlock(level, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), 6), 3, 4, 5, box);
        placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 3, 0, 6, box);
        placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 3, 1, 6, box);
        placeBlock(level, Blocks.OAK_PLANKS.defaultBlockState(), 3, 2, 6, box);
        placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 3, 3, 6, box);
        placeBlock(level, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), 2), 3, 4, 6, box);
        placeBlock(level, stairs(Blocks.OAK_STAIRS.defaultBlockState(), 6), 4, 2, 0, box);
        placeBlock(level, Blocks.OAK_LOG.defaultBlockState(), 4, 3, 0, box);
        placeBlock(level, woodSlab(1), 4, 4, 0, box);
        placeBlock(level, woodSlab(8), 4, 1, 1, box);
        placeBlock(level, stairs(Blocks.BIRCH_STAIRS.defaultBlockState(), 0), 4, 2, 1, box);
        placeBlock(level, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), 4), 4, 4, 1, box);
        placeBlock(level, Blocks.COBBLESTONE_SLAB.defaultBlockState(), 4, 1, 2, box);
        placeBlock(level, woodSlab(9), 4, 4, 2, box);
        placeBlock(level, Blocks.COBBLESTONE_SLAB.defaultBlockState(), 4, 1, 3, box);
        placeBlock(level, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), 0), 4, 5, 3, box);
        placeBlock(level, Blocks.COBBLESTONE_SLAB.defaultBlockState(), 4, 1, 4, box);
        placeBlock(level, woodSlab(9), 4, 4, 4, box);
        placeBlock(level, woodSlab(8), 4, 1, 5, box);
        placeBlock(level, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), 4), 4, 4, 5, box);
        placeBlock(level, stairs(Blocks.OAK_STAIRS.defaultBlockState(), 7), 4, 2, 6, box);
        placeBlock(level, Blocks.OAK_LOG.defaultBlockState(), 4, 3, 6, box);
        placeBlock(level, woodSlab(1), 4, 4, 6, box);
        placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 5, 0, 1, box);
        placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 5, 1, 1, box);
        placeBlock(level, Blocks.OAK_PLANKS.defaultBlockState(), 5, 2, 1, box);
        placeBlock(level, Blocks.OAK_LOG.defaultBlockState(), 5, 3, 1, box);
        placeBlock(level, woodSlab(1), 5, 4, 1, box);
        placeBlock(level, woodSlab(8), 5, 1, 2, box);
        placeBlock(level, woodSlab(9), 5, 2, 2, box);
        placeBlock(level, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), 7), 5, 4, 2, box);
        placeBlock(level, woodSlab(8), 5, 1, 3, box);
        placeBlock(level, woodSlab(9), 5, 2, 3, box);
        placeBlock(level, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), 4), 5, 4, 3, box);
        placeBlock(level, woodSlab(8), 5, 1, 4, box);
        placeBlock(level, woodSlab(9), 5, 2, 4, box);
        placeBlock(level, Blocks.TORCH.defaultBlockState(), 5, 3, 4, box);
        placeBlock(level, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), 6), 5, 4, 4, box);
        placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 5, 0, 5, box);
        placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 5, 1, 5, box);
        placeBlock(level, Blocks.OAK_PLANKS.defaultBlockState(), 5, 2, 5, box);
        placeBlock(level, Blocks.OAK_LOG.defaultBlockState(), 5, 3, 5, box);
        placeBlock(level, woodSlab(1), 5, 4, 5, box);
        placeBlock(level, stairs(Blocks.OAK_STAIRS.defaultBlockState(), 5), 6, 2, 2, box);
        placeBlock(level, Blocks.OAK_LOG.defaultBlockState(), 6, 3, 2, box);
        placeBlock(level, woodSlab(1), 6, 4, 2, box);
        placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 6, 0, 3, box);
        placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 6, 1, 3, box);
        placeBlock(level, Blocks.OAK_PLANKS.defaultBlockState(), 6, 2, 3, box);
        placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 6, 3, 3, box);
        placeBlock(level, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), 0), 6, 4, 3, box);
        placeBlock(level, stairs(Blocks.OAK_STAIRS.defaultBlockState(), 5), 6, 2, 4, box);
        placeBlock(level, Blocks.OAK_LOG.defaultBlockState(), 6, 3, 4, box);
        placeBlock(level, woodSlab(1), 6, 4, 4, box);
    }

    private static BlockState stairs(BlockState base, int oldMeta) {
        Direction[] directions = {Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};
        return base.setValue(StairBlock.FACING, directions[oldMeta & 3])
                .setValue(StairBlock.HALF, (oldMeta & 4) != 0 ? Half.TOP : Half.BOTTOM);
    }

    private static BlockState woodSlab(int oldMeta) {
        BlockState base = switch (oldMeta & 7) {
            case 1 -> Blocks.SPRUCE_SLAB.defaultBlockState();
            case 2 -> Blocks.BIRCH_SLAB.defaultBlockState();
            default -> Blocks.OAK_SLAB.defaultBlockState();
        };
        return base.setValue(SlabBlock.TYPE, (oldMeta & 8) != 0 ? SlabType.TOP : SlabType.BOTTOM);
    }

    private static BlockState door(int oldMeta) {
        Direction[] directions = {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
        return Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, directions[oldMeta & 3])
                .setValue(DoorBlock.HALF, (oldMeta & 8) != 0 ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
    }

    private void spawnResident(WorldGenLevel level) {
        HobgoblinEntity hobgoblin = WitcheryEntities.HOBGOBLIN.get()
                .create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (hobgoblin == null) return;
        hobgoblin.setPersistenceRequired();
        hobgoblin.setPos(boundingBox.minX() + 4.5, boundingBox.minY() + 1, boundingBox.minZ() + 3.5);
        hobgoblin.finalizeSpawn(level,
                new DifficultyInstance(level.getDifficulty(), 0L, 0L, 0.0F),
                EntitySpawnReason.STRUCTURE, null);
        level.addFreshEntityWithPassengers(hobgoblin);
    }
}
