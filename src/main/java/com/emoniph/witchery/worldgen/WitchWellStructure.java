package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;
import java.util.OptionalInt;

public class WitchWellStructure extends Structure {
    public static final MapCodec<WitchWellStructure> CODEC = simpleCodec(WitchWellStructure::new);

    private static final int TERRAIN_PADDING = 1;
    public WitchWellStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int variantRoll = ctx.random().nextInt(10);
        WitchWellPiece.Variant preferred = variantRoll == 0
                ? WitchWellPiece.Variant.CAVE
                : variantRoll <= 5
                ? WitchWellPiece.Variant.SMALL
                : WitchWellPiece.Variant.REGULAR;

        Optional<GenerationStub> stub = generationStubFor(ctx, preferred);
        if (stub.isPresent() || preferred == WitchWellPiece.Variant.REGULAR) {
            return stub;
        }
        return generationStubFor(ctx, WitchWellPiece.Variant.REGULAR);
    }

    @Override
    public StructureType<?> type() {
        return WitcheryStructures.WITCH_WELL_TYPE.get();
    }

    private static Optional<GenerationStub> generationStubFor(GenerationContext ctx,
                                                              WitchWellPiece.Variant variant) {
        int centerX = ctx.chunkPos().getMiddleBlockX();
        int centerZ = ctx.chunkPos().getMiddleBlockZ();
        int x = centerX - variant.surfaceAnchorX();
        int z = centerZ - variant.surfaceAnchorZ();

        if (!hasDryLandFootprint(ctx, variant, x, z)) {
            return Optional.empty();
        }

        OptionalInt surfaceY = findFootprintSurfaceY(ctx, variant, x, z);
        if (surfaceY.isEmpty()) {
            return Optional.empty();
        }

        int y = surfaceY.getAsInt() - variant.surfaceYOffset();
        BlockPos stubPos = new BlockPos(centerX, surfaceY.getAsInt(), centerZ);
        return Optional.of(new GenerationStub(stubPos, builder ->
                builder.addPiece(new WitchWellPiece(ctx.structureTemplateManager(), variant, x, y, z))));
    }

    private static OptionalInt findFootprintSurfaceY(GenerationContext ctx, WitchWellPiece.Variant variant,
                                                     int x, int z) {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int localX = variant.surfaceMinX() - TERRAIN_PADDING;
             localX <= variant.surfaceMaxX() + TERRAIN_PADDING;
             localX++) {
            for (int localZ = variant.surfaceMinZ() - TERRAIN_PADDING;
                 localZ <= variant.surfaceMaxZ() + TERRAIN_PADDING;
                 localZ++) {
                if (!variant.heightSampleColumn(localX, localZ)) {
                    continue;
                }
                int y = surfaceY(ctx, x + localX, z + localZ);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
        }

        if (minY == Integer.MAX_VALUE) {
            return OptionalInt.empty();
        }
        if (maxY - minY > variant.maxFootprintHeightVariance()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(maxY);
    }

    private static boolean hasDryLandFootprint(GenerationContext ctx, WitchWellPiece.Variant variant,
                                               int x, int z) {
        for (int localX = variant.surfaceMinX() - TERRAIN_PADDING;
             localX <= variant.surfaceMaxX() + TERRAIN_PADDING;
             localX++) {
            for (int localZ = variant.surfaceMinZ() - TERRAIN_PADDING;
                 localZ <= variant.surfaceMaxZ() + TERRAIN_PADDING;
                 localZ++) {
                int wx = x + localX;
                int wz = z + localZ;
                if (isOverWater(ctx, wx, wz) || isWaterBiome(ctx, wx, wz)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isOverWater(GenerationContext ctx, int x, int z) {
        return ctx.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG,
                ctx.heightAccessor(), ctx.randomState())
                > ctx.chunkGenerator().getFirstOccupiedHeight(
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

    private static int surfaceY(GenerationContext ctx, int x, int z) {
        return ctx.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                ctx.heightAccessor(), ctx.randomState());
    }
}
