package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class TormentChamberStructure extends Structure {

    public static final MapCodec<TormentChamberStructure> CODEC = simpleCodec(TormentChamberStructure::new);

    public TormentChamberStructure(StructureSettings settings) { super(settings); }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        // Place chambers at a consistent mid-level in the nether-like terrain
        return onTopOfChunkCenter(ctx, Heightmap.Types.OCEAN_FLOOR_WG, builder -> {
            int x = ctx.chunkPos().getMiddleBlockX() - 8;
            int z = ctx.chunkPos().getMiddleBlockZ() - 8;
            // Scan downward from Y=80 for a suitable air gap
            int y = 55; // default: solid nether-like terrain level
            for (int cy = 80; cy > 10; cy--) {
                var pos = new net.minecraft.core.BlockPos(x + 8, cy, z + 8);
                if (ctx.chunkGenerator().getFirstOccupiedHeight(x + 8, z + 8,
                        Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState()) < cy) {
                    y = cy - 7; // place floor 7 below first occupied surface
                    break;
                }
            }
            builder.addPiece(new TormentChamberPiece(
                    WitcheryStructures.TORMENT_CHAMBER_PIECE.get(), x, y, z, ctx.random()));
        });
    }

    @Override
    public StructureType<?> type() { return WitcheryStructures.TORMENT_CHAMBER_TYPE.get(); }
}
