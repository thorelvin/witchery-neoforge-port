package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class CovenCircleStructure extends Structure {

    public static final MapCodec<CovenCircleStructure> CODEC = simpleCodec(CovenCircleStructure::new);

    public CovenCircleStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int cx = ctx.chunkPos().getMiddleBlockX();
        int cz = ctx.chunkPos().getMiddleBlockZ();
        int startX = cx - 5;
        int startZ = cz - 5;

        // WORLD_SURFACE_WG treats water as solid (returns water surface Y).
        // OCEAN_FLOOR_WG ignores fluids and returns the solid floor Y.
        // If they differ, the position is over water — reject it.
        int surfaceY = ctx.chunkGenerator().getFirstOccupiedHeight(
                cx, cz, Heightmap.Types.WORLD_SURFACE_WG,
                ctx.heightAccessor(), ctx.randomState());
        int floorY = ctx.chunkGenerator().getFirstOccupiedHeight(
                cx, cz, Heightmap.Types.OCEAN_FLOOR_WG,
                ctx.heightAccessor(), ctx.randomState());
        if (surfaceY > floorY
                || isOverWater(ctx, startX, startZ)
                || isOverWater(ctx, startX + 10, startZ)
                || isOverWater(ctx, startX, startZ + 10)
                || isOverWater(ctx, startX + 10, startZ + 10)) {
            return Optional.empty();
        }

        return onTopOfChunkCenter(ctx, Heightmap.Types.WORLD_SURFACE_WG, builder -> {
            int x = cx - 5;
            int z = cz - 5;
            // getFirstOccupiedHeight returns the first-air-block Y in this NeoForge build
            // (equivalent to getBaseHeight, not getBaseHeight-1 as the name implies).
            // Subtract 1 so the floor is placed at the surface block, not 1 above it.
            int y = ctx.chunkGenerator().getFirstOccupiedHeight(
                    x + 5, z + 5,
                    Heightmap.Types.WORLD_SURFACE_WG,
                    ctx.heightAccessor(), ctx.randomState()) - 2;
            builder.addPiece(new CovenCirclePiece(
                    WitcheryStructures.COVEN_CIRCLE_PIECE.get(), x, y, z));
        });
    }

    @Override
    public StructureType<?> type() {
        return WitcheryStructures.COVEN_CIRCLE_TYPE.get();
    }

    private static boolean isOverWater(GenerationContext ctx, int x, int z) {
        // WORLD_SURFACE_WG treats water as solid; OCEAN_FLOOR_WG ignores fluids.
        return ctx.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG,
                ctx.heightAccessor(), ctx.randomState())
                > ctx.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                ctx.heightAccessor(), ctx.randomState());
    }
}
