package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;
import java.util.OptionalInt;

public class WarpedCovenStructure extends Structure {
    public static final MapCodec<WarpedCovenStructure> CODEC = simpleCodec(WarpedCovenStructure::new);

    private static final int TEMPLATE_WIDTH = 17;
    private static final int TEMPLATE_DEPTH = 17;
    private static final int MIN_CLEARANCE = 7;

    public WarpedCovenStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int centerX = ctx.chunkPos().getMiddleBlockX();
        int centerZ = ctx.chunkPos().getMiddleBlockZ();
        int x = centerX - TEMPLATE_WIDTH / 2;
        int z = centerZ - TEMPLATE_DEPTH / 2;
        OptionalInt floorY = findNetherFloorY(ctx, x, z);
        if (floorY.isEmpty()) {
            return Optional.empty();
        }

        int y = floorY.getAsInt();
        BlockPos stubPos = new BlockPos(centerX, y, centerZ);
        return Optional.of(new GenerationStub(stubPos, builder ->
                builder.addPiece(new WarpedCovenPiece(ctx.structureTemplateManager(), x, y, z))));
    }

    @Override
    public StructureType<?> type() {
        return WitcheryStructures.WARPED_COVEN_TYPE.get();
    }

    private static OptionalInt findNetherFloorY(GenerationContext ctx, int startX, int startZ) {
        int minY = Math.max(ctx.heightAccessor().getMinY() + 4, 24);
        int maxY = Math.min(ctx.heightAccessor().getMaxY() - MIN_CLEARANCE - 2, 112);

        for (int y = maxY; y >= minY; y--) {
            if (hasStableFootprint(ctx, startX, startZ, y)
                    && hasInteriorClearance(ctx, startX, startZ, y + 1, MIN_CLEARANCE)) {
                return OptionalInt.of(y);
            }
        }
        return OptionalInt.empty();
    }

    private static boolean hasStableFootprint(GenerationContext ctx, int startX, int startZ, int y) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int localX = 0; localX < TEMPLATE_WIDTH; localX++) {
            for (int localZ = 0; localZ < TEMPLATE_DEPTH; localZ++) {
                if (!isFloorFootprint(localX, localZ)) {
                    continue;
                }
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                NoiseColumn column = ctx.chunkGenerator().getBaseColumn(
                        worldX, worldZ, ctx.heightAccessor(), ctx.randomState());
                cursor.set(worldX, y, worldZ);
                if (!isUsableFloor(column.getBlock(y), cursor) || !column.getBlock(y + 1).isAir()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hasInteriorClearance(GenerationContext ctx, int startX, int startZ,
                                                int startY, int height) {
        for (int localX = 4; localX <= 10; localX++) {
            for (int localZ = 6; localZ <= 14; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                NoiseColumn column = ctx.chunkGenerator().getBaseColumn(
                        worldX, worldZ, ctx.heightAccessor(), ctx.randomState());
                for (int y = startY; y < startY + height; y++) {
                    if (!column.getBlock(y).isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isFloorFootprint(int localX, int localZ) {
        if (localX >= 0 && localX <= 2) {
            return localZ >= 7 && localZ <= 11;
        }
        if (localX == 3) {
            return localZ >= 6 && localZ <= 11;
        }
        if (localX == 4) {
            return localZ >= 4 && localZ <= 15;
        }
        if (localX == 5 || localX == 9) {
            return localZ >= 3 && localZ <= 15;
        }
        if (localX >= 6 && localX <= 8) {
            return localZ >= 2 && localZ <= 16;
        }
        if (localX == 10) {
            return localZ >= 4 && localZ <= 15;
        }
        return localX == 11 && (localZ == 7 || localZ == 11);
    }

    private static boolean isUsableFloor(BlockState state, BlockPos pos) {
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && state.isFaceSturdy(EmptyBlockGetter.INSTANCE, pos, Direction.UP);
    }
}
