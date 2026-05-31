package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * Natural Straw Man / Wicker Man worldgen from ComponentWickerMan.
 *
 * 6x8x5 hay effigy with a hidden vanilla Zombie/Zombie/Skeleton spawner and
 * biome-adaptive ground plants.
 */
public class StrawManPiece extends StructurePiece {

    public StrawManPiece(StructurePieceType type, int x, int y, int z) {
        super(type, 0, new BoundingBox(x, y, z, x + 5, y + 7, z + 4));
        setOrientation(Direction.NORTH);
    }

    public StrawManPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(WitcheryStructures.STRAW_MAN_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {}

    @Override
    public void postProcess(WorldGenLevel level, StructureManager mgr, ChunkGenerator gen,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        if (!hasDryLandFootprint(level)) return;

        boolean desert = isDesertBiome(level, boundingBox.minX() + 2, boundingBox.minZ() + 2);

        BlockState ground = desert ? Blocks.SAND.defaultBlockState()
                                   : Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState underground = desert ? Blocks.SAND.defaultBlockState()
                                        : Blocks.DIRT.defaultBlockState();
        BlockState plant = desert ? Blocks.DEAD_BUSH.defaultBlockState()
                                  : Blocks.DANDELION.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        Direction.Axis bodyAxis = random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
        Direction.Axis armAxis = bodyAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        BlockState verticalHay = hay(Direction.Axis.Y);
        BlockState bodyHay = hay(bodyAxis);
        BlockState armHay = hay(armAxis);

        generateBox(level, box, 1, 1, 0, 4, 7, 4, air, air, false);
        generateBox(level, box, 0, 1, 2, 5, 7, 2, air, air, false);

        fillGround(level, box, ground);

        placeBlock(level, plant, 1, 1, 0, box);
        placeBlock(level, plant, 4, 1, 0, box);
        placeBlock(level, plant, 0, 1, 2, box);
        placeBlock(level, plant, 5, 1, 2, box);
        placeBlock(level, plant, 4, 1, 4, box);
        placeBlock(level, plant, 1, 1, 4, box);

        placeSpawner(level, box, random, 2, 0, 2);

        placeBlock(level, bodyHay, 2, 1, 2, box);
        placeBlock(level, bodyHay, 3, 1, 2, box);
        placeBlock(level, bodyHay, 2, 2, 2, box);
        placeBlock(level, bodyHay, 3, 2, 2, box);
        placeBlock(level, armHay, 1, 3, 2, box);
        placeBlock(level, bodyHay, 2, 3, 2, box);
        placeBlock(level, bodyHay, 3, 3, 2, box);
        placeBlock(level, armHay, 4, 3, 2, box);
        placeBlock(level, bodyHay, 1, 4, 2, box);
        placeBlock(level, verticalHay, 2, 4, 2, box);
        placeBlock(level, verticalHay, 3, 4, 2, box);
        placeBlock(level, bodyHay, 4, 4, 2, box);
        placeBlock(level, verticalHay, 1, 5, 2, box);
        placeBlock(level, verticalHay, 2, 5, 2, box);
        placeBlock(level, verticalHay, 3, 5, 2, box);
        placeBlock(level, verticalHay, 4, 5, 2, box);
        placeBlock(level, armHay, 2, 6, 2, box);
        placeBlock(level, armHay, 3, 6, 2, box);
        placeBlock(level, armHay, 2, 7, 2, box);
        placeBlock(level, armHay, 3, 7, 2, box);

        for (int x = 0; x <= 5; x++) {
            for (int z = 0; z <= 4; z++) {
                fillColumnDown(level, underground, x, -1, z, box);
            }
        }
    }

    private boolean hasDryLandFootprint(WorldGenLevel level) {
        for (int x = 0; x <= 5; x++) {
            for (int z = 0; z <= 4; z++) {
                int wx = boundingBox.minX() + x;
                int wz = boundingBox.minZ() + z;
                int worldSurface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, wx, wz);
                int oceanFloor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, wx, wz);
                if (worldSurface > oceanFloor) return false;

                int baseY = boundingBox.minY();
                for (int y = baseY - 1; y <= baseY + 1; y++) {
                    if (!level.getFluidState(new BlockPos(wx, y, wz)).isEmpty()) return false;
                }
                if (!level.getFluidState(new BlockPos(wx, worldSurface - 1, wz)).isEmpty()) return false;
            }
        }
        return true;
    }

    private void fillGround(WorldGenLevel level, BoundingBox box, BlockState ground) {
        for (int x = 1; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                setLocal(level, box, x, 0, z, ground);
            }
        }
        for (int x = 0; x <= 5; x++) {
            setLocal(level, box, x, 0, 2, ground);
        }
    }

    private void placeSpawner(WorldGenLevel level, BoundingBox box, RandomSource random,
                              int localX, int localY, int localZ) {
        BlockPos blockPos = localPos(localX, localY, localZ);
        if (!box.isInside(blockPos)) return;

        level.setBlock(blockPos, Blocks.SPAWNER.defaultBlockState(), 3);
        if (level.getBlockEntity(blockPos) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(random.nextInt(3) == 0 ? EntityType.SKELETON : EntityType.ZOMBIE, random);
            spawner.setChanged();
        }
    }

    private void setLocal(WorldGenLevel level, BoundingBox box, int localX, int localY, int localZ,
                          BlockState state) {
        BlockPos blockPos = localPos(localX, localY, localZ);
        if (box.isInside(blockPos)) {
            level.setBlock(blockPos, state, 3);
        }
    }

    private BlockPos localPos(int localX, int localY, int localZ) {
        return new BlockPos(
                boundingBox.minX() + localX,
                boundingBox.minY() + localY,
                boundingBox.minZ() + localZ);
    }

    private static BlockState hay(Direction.Axis axis) {
        return Blocks.HAY_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    private static boolean isDesertBiome(WorldGenLevel level, int x, int z) {
        net.minecraft.core.Holder<Biome> biome = level.getBiome(new BlockPos(x, 64, z));
        return biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BADLANDS);
    }
}
