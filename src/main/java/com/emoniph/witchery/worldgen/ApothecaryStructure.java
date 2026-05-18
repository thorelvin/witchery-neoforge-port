package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class ApothecaryStructure extends Structure {

    public static final MapCodec<ApothecaryStructure> CODEC = simpleCodec(ApothecaryStructure::new);

    public ApothecaryStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        return onTopOfChunkCenter(ctx, Heightmap.Types.WORLD_SURFACE_WG, builder -> {
            int x = ctx.chunkPos().getMiddleBlockX() - 4;
            int z = ctx.chunkPos().getMiddleBlockZ() - 2;
            int y = ctx.chunkGenerator().getFirstOccupiedHeight(
                    x + 4, z + 2,
                    Heightmap.Types.WORLD_SURFACE_WG,
                    ctx.heightAccessor(), ctx.randomState());
            builder.addPiece(new ApothecaryPiece(WitcheryStructures.APOTHECARY_PIECE.get(), x, y, z));
        });
    }

    @Override
    public StructureType<?> type() {
        return WitcheryStructures.APOTHECARY_TYPE.get();
    }
}
