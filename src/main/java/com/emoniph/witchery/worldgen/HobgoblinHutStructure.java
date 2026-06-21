package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;
import java.util.OptionalInt;

public class HobgoblinHutStructure extends Structure {

    public static final MapCodec<HobgoblinHutStructure> CODEC = simpleCodec(HobgoblinHutStructure::new);
    private static final int TEMPLATE_WIDTH = 14;
    private static final int TEMPLATE_DEPTH = 14;
    private static final NbtStructurePlacement.RectFootprint FOOTPRINT =
            NbtStructurePlacement.RectFootprint.fromSize(TEMPLATE_WIDTH, TEMPLATE_DEPTH);
    private static final int TERRAIN_PADDING = 1;
    private static final int MAX_FOOTPRINT_HEIGHT_VARIANCE = 1;

    public HobgoblinHutStructure(StructureSettings settings) { super(settings); }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int centerX = ctx.chunkPos().getMiddleBlockX();
        int centerZ = ctx.chunkPos().getMiddleBlockZ();
        int x = centerX - TEMPLATE_WIDTH / 2;
        int z = centerZ - TEMPLATE_DEPTH / 2;

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
        return Optional.of(new GenerationStub(stubPos, builder ->
                builder.addPiece(new HobgoblinHutTemplatePiece(
                        ctx.structureTemplateManager(), x, groundY.getAsInt(), z))));
    }

    @Override
    public StructureType<?> type() {
        return WitcheryStructures.HOBGOBLIN_HUT_TYPE.get();
    }
}
