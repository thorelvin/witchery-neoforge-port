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

        int centerX = boundingBox.minX() + 2;
        int centerZ = boundingBox.minZ() + 2;
        boolean sandGround = isSandGroundBiome(level, centerX, centerZ);
        boolean dryShrubBiome = isDryShrubBiome(level, centerX, centerZ);

        BlockState fallbackGround = sandGround ? Blocks.SAND.defaultBlockState()
                                               : Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState fallbackUnderground = sandGround ? Blocks.SAND.defaultBlockState()
                                                    : Blocks.DIRT.defaultBlockState();
        BlockState plant = dryShrubBiome ? Blocks.DEAD_BUSH.defaultBlockState()
                                         : Blocks.DANDELION.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        Direction.Axis bodyAxis = random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
        Direction.Axis armAxis = bodyAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        BlockState verticalHay = hay(Direction.Axis.Y);
        BlockState bodyHay = hay(bodyAxis);
        BlockState armHay = hay(armAxis);

        generateBox(level, box, 1, 1, 0, 4, 7, 4, air, air, false);
        generateBox(level, box, 0, 1, 2, 5, 7, 2, air, air, false);

        fillGround(level, box, sandGround, fallbackGround);

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
                if (isGroundFootprint(x, z)) {
                    fillColumnDown(level, matchingSurfaceState(level, x, z, sandGround, fallbackUnderground),
                            x, -1, z, box);
                }
            }
        }
    }

    private boolean hasDryLandFootprint(WorldGenLevel level) {
        for (int x = 0; x <= 5; x++) {
            for (int z = 0; z <= 4; z++) {
                int wx = boundingBox.minX() + x;
                int wz = boundingBox.minZ() + z;
                int worldSurface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, wx, wz);
                // NOTE: do NOT reject on `worldSurface > oceanFloor`. That is true over any
                // non-motion-blocking surface block (snow layer, grass, flowers), so it
                // silently aborts the whole effigy on perfectly dry land that the
                // structure-level findGenerationPoint already approved — leaving a located
                // but empty Straw Man (the `/witchery goto straw_man` "nothing there" bug).
                // The explicit fluid checks below, plus the structure's water/biome guards,
                // are what actually keep the Straw Man off water.

                int baseY = boundingBox.minY();
                for (int y = baseY - 1; y <= baseY + 1; y++) {
                    if (!level.getFluidState(new BlockPos(wx, y, wz)).isEmpty()) return false;
                }
                if (!level.getFluidState(new BlockPos(wx, worldSurface - 1, wz)).isEmpty()) return false;
            }
        }
        return true;
    }

    private void fillGround(WorldGenLevel level, BoundingBox box, boolean forceSand, BlockState fallbackGround) {
        for (int x = 1; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                setLocal(level, box, x, 0, z, matchingSurfaceState(level, x, z, forceSand, fallbackGround));
            }
        }
        for (int x = 0; x <= 5; x++) {
            setLocal(level, box, x, 0, 2, matchingSurfaceState(level, x, 2, forceSand, fallbackGround));
        }
    }

    private static boolean isGroundFootprint(int x, int z) {
        return (x >= 1 && x <= 4) || z == 2;
    }

    private BlockState matchingSurfaceState(WorldGenLevel level, int localX, int localZ,
                                            boolean forceSand, BlockState fallback) {
        if (forceSand) {
            return fallback;
        }

        BlockPos surfacePos = findNaturalSurface(level, boundingBox.minX() + localX, boundingBox.minZ() + localZ);
        if (surfacePos == null) {
            return fallback;
        }

        BlockState state = level.getBlockState(surfacePos);
        return isUsableGroundState(state) ? state : fallback;
    }

    private static BlockPos findNaturalSurface(WorldGenLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        int minY = level.getMinY();
        while (y > minY) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.getFluidState().isEmpty() && state.blocksMotion()) {
                return pos;
            }
            y--;
        }
        return null;
    }

    private static boolean isUsableGroundState(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.blocksMotion();
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

    private static boolean isSandGroundBiome(WorldGenLevel level, int x, int z) {
        return level.getBiome(new BlockPos(x, 64, z)).is(Biomes.DESERT);
    }

    private static boolean isDryShrubBiome(WorldGenLevel level, int x, int z) {
        net.minecraft.core.Holder<Biome> biome = level.getBiome(new BlockPos(x, 64, z));
        return biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BADLANDS);
    }
}
