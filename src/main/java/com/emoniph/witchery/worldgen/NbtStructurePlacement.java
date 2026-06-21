package com.emoniph.witchery.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.OptionalInt;

/**
 * Shared terrain and clutter helpers for designer-authored NBT structures. Keep NBT files standard;
 * put world adaptation here so command placement, natural worldgen, and later structure imports use
 * the same rules.
 */
public final class NbtStructurePlacement {

    public enum HeightSelection {
        LOWEST,
        HIGHEST
    }

    public enum GroundFailure {
        FLUID,
        NON_SOLID
    }

    public record GroundResult(OptionalInt surfaceY, GroundFailure failure) {
        public static GroundResult success(int surfaceY) {
            return new GroundResult(OptionalInt.of(surfaceY), null);
        }

        public static GroundResult failure(GroundFailure failure) {
            return new GroundResult(OptionalInt.empty(), failure);
        }
    }

    public record RectFootprint(int minX, int maxX, int minZ, int maxZ) {
        public static RectFootprint fromSize(int width, int depth) {
            return new RectFootprint(0, width - 1, 0, depth - 1);
        }
    }

    @FunctionalInterface
    public interface LocalToWorld {
        BlockPos apply(int localX, int localY, int localZ);
    }

    private NbtStructurePlacement() {}

    public static boolean hasDryLandFootprint(Structure.GenerationContext ctx,
                                              RectFootprint footprint,
                                              int originX, int originZ,
                                              int padding) {
        for (int dx = footprint.minX() - padding; dx <= footprint.maxX() + padding; dx++) {
            for (int dz = footprint.minZ() - padding; dz <= footprint.maxZ() + padding; dz++) {
                int wx = originX + dx;
                int wz = originZ + dz;
                if (isOverWater(ctx, wx, wz) || isWaterBiome(ctx, wx, wz)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static OptionalInt findFootprintSurfaceY(Structure.GenerationContext ctx,
                                                    RectFootprint footprint,
                                                    int originX, int originZ,
                                                    int padding,
                                                    int maxHeightVariance,
                                                    HeightSelection selection) {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int dx = footprint.minX() - padding; dx <= footprint.maxX() + padding; dx++) {
            for (int dz = footprint.minZ() - padding; dz <= footprint.maxZ() + padding; dz++) {
                int y = terrainSurfaceY(ctx, originX + dx, originZ + dz);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxY - minY > maxHeightVariance) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(selection == HeightSelection.HIGHEST ? maxY : minY);
    }

    public static GroundResult trueGroundSurfaceY(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int limit = Math.max(level.getMinY(), y - 48);
        for (int scanY = y - 1; scanY >= limit; scanY--) {
            BlockState state = level.getBlockState(new BlockPos(x, scanY, z));
            if (!state.getFluidState().isEmpty()) {
                return GroundResult.failure(GroundFailure.FLUID);
            }
            if (state.isAir() || isClearableClutter(state)) {
                continue;
            }
            if (!state.isSolidRender()) {
                return GroundResult.failure(GroundFailure.NON_SOLID);
            }
            return GroundResult.success(scanY + 1);
        }
        return GroundResult.failure(GroundFailure.NON_SOLID);
    }

    public static void clearClutterBox(LevelAccessor level, int minX, int minY, int minZ,
                                       int maxX, int maxY, int maxZ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isClearableClutter(level.getBlockState(pos))) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    public static void clearLocalClutter(LevelAccessor level, BoundingBox box,
                                         int width, int height, int depth,
                                         LocalToWorld localToWorld) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                for (int y = 0; y < height; y++) {
                    BlockPos pos = localToWorld.apply(x, y, z);
                    if (box.isInside(pos) && isClearableClutter(level.getBlockState(pos))) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    public static boolean isClearableClutter(BlockState state) {
        return state.canBeReplaced()
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS);
    }

    private static int terrainSurfaceY(Structure.GenerationContext ctx, int x, int z) {
        return ctx.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                ctx.heightAccessor(), ctx.randomState());
    }

    private static boolean isOverWater(Structure.GenerationContext ctx, int x, int z) {
        return ctx.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG,
                ctx.heightAccessor(), ctx.randomState())
                > ctx.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                ctx.heightAccessor(), ctx.randomState());
    }

    private static boolean isWaterBiome(Structure.GenerationContext ctx, int x, int z) {
        int y = ctx.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG,
                ctx.heightAccessor(), ctx.randomState());
        var biome = ctx.chunkGenerator().getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock(x),
                QuartPos.fromBlock(y),
                QuartPos.fromBlock(z),
                ctx.randomState().sampler());
        return biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER);
    }
}
