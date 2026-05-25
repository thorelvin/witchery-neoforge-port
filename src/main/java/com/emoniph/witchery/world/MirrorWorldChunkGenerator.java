package com.emoniph.witchery.world;

import com.emoniph.witchery.init.WitcheryBlocks;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.concurrent.CompletableFuture;

/**
 * Generates the Mirror World as an infinite grid of 9×9×9 rooms made of mirror wall.
 *
 * Per 16-block chunk column:
 *   x local 0–3, 13–15  → mirror_wall
 *   x local 4–12         → open (9 blocks)
 *   z local 0–3, 13–15  → mirror_wall
 *   z local 4–12         → open (9 blocks)
 *   y local 0–5          → floor (mirror_wall)
 *   y local 6–14         → open (9 blocks)
 *   y local 15           → ceiling (mirror_wall)
 *
 * Entry point per room at (chunkX*16+8, 7, chunkZ*16+8).
 */
public class MirrorWorldChunkGenerator extends ChunkGenerator {

    public static final MapCodec<MirrorWorldChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(inst ->
            inst.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
            ).apply(inst, MirrorWorldChunkGenerator::new));

    // 1 = mirror_wall, 0 = open
    private static final int[] WALL_XZ = {1,1,1,1, 0,0,0,0,0,0,0,0,0, 1,1,1};
    private static final int[] WALL_Y  = {1,1,1,1,1,1, 0,0,0,0,0,0,0,0,0, 1};

    public MirrorWorldChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender, RandomState randomState, StructureManager structures, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures,
                             RandomState random, ChunkAccess chunk) {
        BlockState wall = WitcheryBlocks.MIRROR_WALL.get().defaultBlockState();
        BlockState air  = Blocks.AIR.defaultBlockState();

        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinY();
        int maxY = chunk.getMinY() + chunk.getHeight(); // exclusive upper bound

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                boolean xOpen = WALL_XZ[lx] == 0;
                boolean zOpen = WALL_XZ[lz] == 0;
                for (int y = minY; y < maxY; y++) {
                    int ly = ((y % 16) + 16) % 16;
                    boolean yOpen = WALL_Y[ly] == 0;
                    BlockState bs = (xOpen && zOpen && yOpen) ? air : wall;
                    chunk.setBlockState(new BlockPos(baseX + lx, y, baseZ + lz), bs, 0);
                }
            }
        }
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState,
                             BiomeManager biomeManager, StructureManager structures,
                             ChunkAccess chunk) {}

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {}

    @Override
    public int getGenDepth() { return 16; }

    @Override
    public int getSeaLevel() { return 0; }

    @Override
    public int getMinY() { return 0; }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmapType,
                             LevelHeightAccessor level, RandomState randomState) {
        return 7;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level,
                                     RandomState randomState) {
        return new NoiseColumn(level.getMinY(), new BlockState[0]);
    }

    @Override
    public void addDebugScreenInfo(java.util.List<String> info, RandomState randomState, BlockPos pos) {}
}
