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

public class HobgoblinHutStructure extends Structure {

    public static final MapCodec<HobgoblinHutStructure> CODEC = simpleCodec(HobgoblinHutStructure::new);
    private static final int TEMPLATE_WIDTH = 14;
    private static final int TEMPLATE_DEPTH = 14;
    private static final int TERRAIN_PADDING = 1;
    private static final int MAX_FOOTPRINT_HEIGHT_VARIANCE = 1;

    public HobgoblinHutStructure(StructureSettings settings) { super(settings); }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int centerX = ctx.chunkPos().getMiddleBlockX();
        int centerZ = ctx.chunkPos().getMiddleBlockZ();
        int x = centerX - TEMPLATE_WIDTH / 2;
        int z = centerZ - TEMPLATE_DEPTH / 2;

        if (!hasDryLandFootprint(ctx, x, z)) {
            return Optional.empty();
        }

        OptionalInt groundY = findFootprintGroundY(ctx, x, z);
        if (groundY.isEmpty()) {
            return Optional.empty();
        }

        BlockPos stubPos = new BlockPos(centerX, groundY.getAsInt(), centerZ);
        return Optional.of(new GenerationStub(stubPos, builder ->
                builder.addPiece(new HobgoblinHutTemplatePiece(
                        ctx.structureTemplateManager(), x, groundY.getAsInt(), z))));
    }

    @Override
    public StructureType<?> type() {
        return WitcheryStructures.HOBGOBLIN_HUT_TYPE.get();
    }

    private static OptionalInt findFootprintGroundY(GenerationContext ctx, int x, int z) {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int dx = -TERRAIN_PADDING; dx < TEMPLATE_WIDTH + TERRAIN_PADDING; dx++) {
            for (int dz = -TERRAIN_PADDING; dz < TEMPLATE_DEPTH + TERRAIN_PADDING; dz++) {
                int y = surfaceY(ctx, x + dx, z + dz);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
        }

        if (maxY - minY > MAX_FOOTPRINT_HEIGHT_VARIANCE) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(maxY);
    }

    private static boolean hasDryLandFootprint(GenerationContext ctx, int x, int z) {
        for (int dx = -TERRAIN_PADDING; dx < TEMPLATE_WIDTH + TERRAIN_PADDING; dx++) {
            for (int dz = -TERRAIN_PADDING; dz < TEMPLATE_DEPTH + TERRAIN_PADDING; dz++) {
                int wx = x + dx;
                int wz = z + dz;
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
