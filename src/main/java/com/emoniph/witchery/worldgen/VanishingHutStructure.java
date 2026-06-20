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

/**
 * Natural worldgen for the {@link VanishingHut}. Places the NBT hut on flat, dry land in
 * forests and swamps. The block placement, resident, and relocation are all handled by
 * {@link VanishingHutPiece} reusing the same {@code VanishingHut} build logic the QA command uses.
 */
public class VanishingHutStructure extends Structure {

    public static final MapCodec<VanishingHutStructure> CODEC = simpleCodec(VanishingHutStructure::new);

    private static final int TERRAIN_PADDING = 1;
    private static final int MAX_FOOTPRINT_HEIGHT_VARIANCE = 2;

    public VanishingHutStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int centerX = ctx.chunkPos().getMiddleBlockX();
        int centerZ = ctx.chunkPos().getMiddleBlockZ();

        if (!hasDryLandFootprint(ctx, centerX, centerZ)) {
            return Optional.empty();
        }
        OptionalInt anchorY = findFootprintAnchorY(ctx, centerX, centerZ);
        if (anchorY.isEmpty()) {
            return Optional.empty();
        }

        // The template's local y=1 is its visible surface/floor layer, so the hidden anchor is one
        // block below the natural top solid block.
        return onTopOfChunkCenter(ctx, Heightmap.Types.WORLD_SURFACE_WG, builder ->
                builder.addPiece(new VanishingHutPiece(centerX, anchorY.getAsInt(), centerZ)));
    }

    @Override
    public StructureType<?> type() {
        return WitcheryStructures.VANISHING_HUT_TYPE.get();
    }

    private static boolean hasDryLandFootprint(GenerationContext ctx, int centerX, int centerZ) {
        for (int dx = VanishingHut.MIN_DX - TERRAIN_PADDING;
             dx <= VanishingHut.MAX_DX + TERRAIN_PADDING; dx++) {
            for (int dz = VanishingHut.MIN_DZ - TERRAIN_PADDING;
                 dz <= VanishingHut.MAX_DZ + TERRAIN_PADDING; dz++) {
                int wx = centerX + dx;
                int wz = centerZ + dz;
                if (isOverWater(ctx, wx, wz) || isWaterBiome(ctx, wx, wz)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static OptionalInt findFootprintAnchorY(GenerationContext ctx, int centerX, int centerZ) {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int dx = VanishingHut.MIN_DX; dx <= VanishingHut.MAX_DX; dx++) {
            for (int dz = VanishingHut.MIN_DZ; dz <= VanishingHut.MAX_DZ; dz++) {
                int y = surfaceY(ctx, centerX + dx, centerZ + dz);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxY - minY > MAX_FOOTPRINT_HEIGHT_VARIANCE) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(minY - 1);
    }

    private static boolean isOverWater(GenerationContext ctx, int x, int z) {
        return ctx.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState())
                > ctx.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
    }

    private static int surfaceY(GenerationContext ctx, int x, int z) {
        return ctx.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
    }

    private static boolean isWaterBiome(GenerationContext ctx, int x, int z) {
        int y = ctx.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        var biome = ctx.chunkGenerator().getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z),
                ctx.randomState().sampler());
        return biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER);
    }
}
