package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;
import java.util.OptionalInt;

public class StrawManStructure extends Structure {

    public static final MapCodec<StrawManStructure> CODEC = simpleCodec(StrawManStructure::new);
    private static final int FOOTPRINT_WIDTH = 6;
    private static final int FOOTPRINT_DEPTH = 5;
    private static final int TERRAIN_PADDING = 1;
    private static final int MAX_FOOTPRINT_HEIGHT_VARIANCE = 2;

    public StrawManStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int cx = ctx.chunkPos().getMiddleBlockX();
        int cz = ctx.chunkPos().getMiddleBlockZ();
        int x = cx - 3;
        int z = cz - 2;

        if (!hasDryLandFootprint(ctx, x, z)) {
            return Optional.empty();
        }

        OptionalInt groundY = findFootprintGroundY(ctx, x, z);
        if (groundY.isEmpty()) {
            return Optional.empty();
        }

        return onTopOfChunkCenter(ctx, Heightmap.Types.WORLD_SURFACE_WG, builder -> {
            builder.addPiece(new StrawManPiece(
                    WitcheryStructures.STRAW_MAN_PIECE.get(), x, groundY.getAsInt(), z));
        });
    }

    @Override
    public StructureType<?> type() {
        return WitcheryStructures.STRAW_MAN_TYPE.get();
    }

    private static boolean isOverWater(GenerationContext ctx, int x, int z) {
        return ctx.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG,
                ctx.heightAccessor(), ctx.randomState())
                > ctx.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                ctx.heightAccessor(), ctx.randomState());
    }

    private static boolean hasDryLandFootprint(GenerationContext ctx, int x, int z) {
        for (int dx = -TERRAIN_PADDING; dx < FOOTPRINT_WIDTH + TERRAIN_PADDING; dx++) {
            for (int dz = -TERRAIN_PADDING; dz < FOOTPRINT_DEPTH + TERRAIN_PADDING; dz++) {
                int wx = x + dx;
                int wz = z + dz;
                if (isOverWater(ctx, wx, wz) || isWaterBiome(ctx, wx, wz)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static OptionalInt findFootprintGroundY(GenerationContext ctx, int x, int z) {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int dx = -TERRAIN_PADDING; dx < FOOTPRINT_WIDTH + TERRAIN_PADDING; dx++) {
            for (int dz = -TERRAIN_PADDING; dz < FOOTPRINT_DEPTH + TERRAIN_PADDING; dz++) {
                int y = surfaceY(ctx, x + dx, z + dz);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
        }

        if (maxY - minY > MAX_FOOTPRINT_HEIGHT_VARIANCE) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(minY);
    }

    private static int surfaceY(GenerationContext ctx, int x, int z) {
        return ctx.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                ctx.heightAccessor(), ctx.randomState());
    }

    private static boolean isWaterBiome(GenerationContext ctx, int x, int z) {
        int y = ctx.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG,
                ctx.heightAccessor(), ctx.randomState());
        var biome = ctx.chunkGenerator().getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock(x),
                QuartPos.fromBlock(y),
                QuartPos.fromBlock(z),
                ctx.randomState().sampler());
        return biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER);
    }
}
