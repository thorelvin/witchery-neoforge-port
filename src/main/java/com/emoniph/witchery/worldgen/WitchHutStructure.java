package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;
import java.util.OptionalInt;

public class WitchHutStructure extends Structure {
    public static final MapCodec<WitchHutStructure> CODEC = simpleCodec(WitchHutStructure::new);
    private static final NbtStructurePlacement.RectFootprint FOOTPRINT =
            NbtStructurePlacement.RectFootprint.fromSize(4, 5);
    private static final int TERRAIN_PADDING = 1;
    private static final int MAX_FOOTPRINT_HEIGHT_VARIANCE = 2;

    public WitchHutStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int centerX = ctx.chunkPos().getMiddleBlockX();
        int centerZ = ctx.chunkPos().getMiddleBlockZ();
        int x = centerX - 1;
        int z = centerZ - 2;
        if (!NbtStructurePlacement.hasDryLandFootprint(ctx, FOOTPRINT, x, z, TERRAIN_PADDING)) {
            return Optional.empty();
        }
        OptionalInt groundY = NbtStructurePlacement.findFootprintSurfaceY(ctx, FOOTPRINT, x, z,
                TERRAIN_PADDING, MAX_FOOTPRINT_HEIGHT_VARIANCE,
                NbtStructurePlacement.HeightSelection.HIGHEST);
        if (groundY.isEmpty()) {
            return Optional.empty();
        }
        BlockPos stubPos = new BlockPos(centerX, groundY.getAsInt(), centerZ);
        return Optional.of(new GenerationStub(stubPos, builder -> builder.addPiece(
                new WitchHutPiece(WitcheryStructures.WITCH_HUT_PIECE.get(),
                        x, groundY.getAsInt(), z))));
    }

    @Override
    public StructureType<?> type() {
        return WitcheryStructures.WITCH_HUT_TYPE.get();
    }
}
