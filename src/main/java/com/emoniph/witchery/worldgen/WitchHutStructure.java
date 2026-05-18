package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class WitchHutStructure extends Structure {

    public static final MapCodec<WitchHutStructure> CODEC = simpleCodec(WitchHutStructure::new);

    public WitchHutStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        return onTopOfChunkCenter(ctx, Heightmap.Types.WORLD_SURFACE_WG, builder -> {
            int x = ctx.chunkPos().getMiddleBlockX() - 3;
            int z = ctx.chunkPos().getMiddleBlockZ() - 3;
            int y = ctx.chunkGenerator().getFirstOccupiedHeight(
                    x + 3, z + 3,
                    Heightmap.Types.WORLD_SURFACE_WG,
                    ctx.heightAccessor(), ctx.randomState());
            builder.addPiece(new WitchHutPiece(WitcheryStructures.WITCH_HUT_PIECE.get(), x, y, z));
        });
    }

    @Override
    public StructureType<?> type() {
        return WitcheryStructures.WITCH_HUT_TYPE.get();
    }
}
