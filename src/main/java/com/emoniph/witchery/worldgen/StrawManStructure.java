package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class StrawManStructure extends Structure {

    public static final MapCodec<StrawManStructure> CODEC = simpleCodec(StrawManStructure::new);

    public StrawManStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int cx = ctx.chunkPos().getMiddleBlockX();
        int cz = ctx.chunkPos().getMiddleBlockZ();
        int x = cx - 3;
        int z = cz - 2;

        if (isOverWater(ctx, cx, cz)
                || isOverWater(ctx, x, z)
                || isOverWater(ctx, x + 5, z)
                || isOverWater(ctx, x, z + 4)
                || isOverWater(ctx, x + 5, z + 4)) {
            return Optional.empty();
        }

        return onTopOfChunkCenter(ctx, Heightmap.Types.WORLD_SURFACE_WG, builder -> {
            int y = ctx.chunkGenerator().getFirstOccupiedHeight(
                    x + 2, z + 2,
                    Heightmap.Types.WORLD_SURFACE_WG,
                    ctx.heightAccessor(), ctx.randomState()) - 1;
            builder.addPiece(new StrawManPiece(
                    WitcheryStructures.STRAW_MAN_PIECE.get(), x, y, z));
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
}
