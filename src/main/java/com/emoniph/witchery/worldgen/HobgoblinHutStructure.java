package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class HobgoblinHutStructure extends Structure {

    public static final MapCodec<HobgoblinHutStructure> CODEC = simpleCodec(HobgoblinHutStructure::new);

    public HobgoblinHutStructure(StructureSettings settings) { super(settings); }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        return onTopOfChunkCenter(ctx, Heightmap.Types.WORLD_SURFACE_WG, builder -> {
            int x = ctx.chunkPos().getMiddleBlockX() - 2;
            int z = ctx.chunkPos().getMiddleBlockZ() - 2;
            int y = ctx.chunkGenerator().getFirstOccupiedHeight(
                    x + 2, z + 2,
                    Heightmap.Types.WORLD_SURFACE_WG,
                    ctx.heightAccessor(), ctx.randomState());
            builder.addPiece(new HobgoblinHutPiece(WitcheryStructures.HOBGOBLIN_HUT_PIECE.get(), x, y, z));
        });
    }

    @Override
    public StructureType<?> type() {
        return WitcheryStructures.HOBGOBLIN_HUT_TYPE.get();
    }
}
