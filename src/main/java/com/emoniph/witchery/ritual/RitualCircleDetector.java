package com.emoniph.witchery.ritual;

import com.emoniph.witchery.init.WitcheryBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Scans for chalk circle patterns around a given center position.
 *
 * All rings sit one block above the floor tile that was right-clicked.
 * The center tile itself is NOT a circle block — it's where the witch stands.
 *
 * To activate: right-click the floor block at the center with the arthana.
 */
public class RitualCircleDetector {

    /** Source-style first/small circle: 7x7 bounds, 16 chalk marks. */
    private static final int[][] SMALL_RING = {
                         {-1,-3},{0,-3},{1,-3},
                 {-2,-2},                 {2,-2},
        {-3,-1},                                   {3,-1},
        {-3, 0},                                   {3, 0},
        {-3, 1},                                   {3, 1},
                 {-2, 2},                 {2, 2},
                         {-1, 3},{0, 3},{1, 3}
    };

    /** Source-style second/medium circle: 11x11 bounds, 28 chalk marks. */
    private static final int[][] MEDIUM_RING = {
                         {-2,-5},{-1,-5},{0,-5},{1,-5},{2,-5},
                 {-3,-4},                                   {3,-4},
        {-4,-3},                                                     {4,-3},
        {-5,-2},                                                               {5,-2},
        {-5,-1},                                                               {5,-1},
        {-5, 0},                                                               {5, 0},
        {-5, 1},                                                               {5, 1},
        {-5, 2},                                                               {5, 2},
        {-4, 3},                                                     {4, 3},
                 {-3, 4},                                   {3, 4},
                         {-2, 5},{-1, 5},{0, 5},{1, 5},{2, 5}
    };

    /** Source-style third/large circle: 15x15 bounds, 40 chalk marks. */
    private static final int[][] LARGE_RING = {
                         {-3,-7},{-2,-7},{-1,-7},{0,-7},{1,-7},{2,-7},{3,-7},
                 {-4,-6},                                                   {4,-6},
        {-5,-5},                                                                     {5,-5},
        {-6,-4},                                                                               {6,-4},
        {-7,-3},                                                                                         {7,-3},
        {-7,-2},                                                                                         {7,-2},
        {-7,-1},                                                                                         {7,-1},
        {-7, 0},                                                                                         {7, 0},
        {-7, 1},                                                                                         {7, 1},
        {-7, 2},                                                                                         {7, 2},
        {-7, 3},                                                                                         {7, 3},
        {-6, 4},                                                                               {6, 4},
        {-5, 5},                                                                     {5, 5},
                 {-4, 6},                                                   {4, 6},
                         {-3, 7},{-2, 7},{-1, 7},{0, 7},{1, 7},{2, 7},{3, 7}
    };

    public enum CircleSize { SMALL, MEDIUM, LARGE }

    public record DetectionResult(CircleSize size, List<BlockPos> ringBlocks) {}

    public static List<BlockPos> ringPositions(BlockPos floorCenter, CircleSize size) {
        BlockPos markLevel = floorCenter.above();
        int[][] offsets = offsetsFor(size);
        List<BlockPos> positions = new ArrayList<>(offsets.length);
        for (int[] off : offsets) {
            positions.add(markLevel.offset(off[0], 0, off[1]));
        }
        return positions;
    }

    public static DetectionResult detect(Level level, BlockPos floorCenter) {
        BlockPos markLevel = floorCenter.above();

        List<BlockPos> small = collectRing(level, markLevel, SMALL_RING);
        if (small != null) return new DetectionResult(CircleSize.SMALL, small);

        List<BlockPos> medium = collectRing(level, markLevel, MEDIUM_RING);
        if (medium != null) return new DetectionResult(CircleSize.MEDIUM, medium);

        List<BlockPos> large = collectRing(level, markLevel, LARGE_RING);
        if (large != null) return new DetectionResult(CircleSize.LARGE, large);

        return null;
    }

    /**
     * Return every complete ring present at the center, ordered from largest to
     * smallest. This gives the activation layer a stable outer-to-inner retry
     * path until the full multi-circle recipe registry is restored.
     */
    public static List<DetectionResult> detectPresent(Level level, BlockPos floorCenter) {
        BlockPos markLevel = floorCenter.above();
        List<DetectionResult> results = new ArrayList<>(3);

        List<BlockPos> large = collectRing(level, markLevel, LARGE_RING);
        if (large != null) {
            results.add(new DetectionResult(CircleSize.LARGE, large));
        }

        List<BlockPos> medium = collectRing(level, markLevel, MEDIUM_RING);
        if (medium != null) {
            results.add(new DetectionResult(CircleSize.MEDIUM, medium));
        }

        List<BlockPos> small = collectRing(level, markLevel, SMALL_RING);
        if (small != null) {
            results.add(new DetectionResult(CircleSize.SMALL, small));
        }

        return results;
    }

    public static boolean hasRing(Level level, BlockPos floorCenter, CircleSize size) {
        BlockPos markLevel = floorCenter.above();
        return switch (size) {
            case SMALL -> collectRing(level, markLevel, SMALL_RING) != null;
            case MEDIUM -> collectRing(level, markLevel, MEDIUM_RING) != null;
            case LARGE -> collectRing(level, markLevel, LARGE_RING) != null;
        };
    }

    public static int detectVariant(Level level, BlockPos floorCenter, CircleSize size) {
        int[] variants = detectAllVariants(level, floorCenter);
        return switch (size) {
            case SMALL -> variants[0];
            case MEDIUM -> variants[1];
            case LARGE -> variants[2];
        };
    }

    private static List<BlockPos> collectRing(Level level, BlockPos origin, int[][] offsets) {
        List<BlockPos> positions = new ArrayList<>(offsets.length);
        for (int[] off : offsets) {
            BlockPos pos = origin.offset(off[0], 0, off[1]);
            BlockState state = level.getBlockState(pos);
            if (!state.is(WitcheryBlockTags.CHALK_CIRCLES)) return null;
            positions.add(pos);
        }
        return positions;
    }

    private static int[][] offsetsFor(CircleSize size) {
        return switch (size) {
            case SMALL -> SMALL_RING;
            case MEDIUM -> MEDIUM_RING;
            case LARGE -> LARGE_RING;
        };
    }

    /**
     * Scan all three ring sizes independently and return their chalk variants.
     * Returns int[3] = { smallVariant, mediumVariant, largeVariant }.
     * Variant encoding: 0 = absent, 1 = ritual glyph, 2 = otherwhere, 3 = infernal, 4 = golden.
     * Used by CircleTalismanItem to store a pattern.
     */
    public static int[] detectAllVariants(Level level, BlockPos floorCenter) {
        BlockPos markLevel = floorCenter.above();
        int small  = ringVariant(level, markLevel, SMALL_RING);
        int medium = ringVariant(level, markLevel, MEDIUM_RING);
        int large  = ringVariant(level, markLevel, LARGE_RING);
        return new int[]{ small, medium, large };
    }

    /** Returns the chalk variant of a complete ring, or 0 if the ring is absent/incomplete. */
    private static int ringVariant(Level level, BlockPos origin, int[][] offsets) {
        int variant = -1;
        for (int[] off : offsets) {
            BlockPos pos = origin.offset(off[0], 0, off[1]);
            BlockState state = level.getBlockState(pos);
            if (!state.is(WitcheryBlockTags.CHALK_CIRCLES)) return 0;
            int v = variantOf(state);
            if (variant == -1) variant = v;
            else if (variant != v) return 0; // mixed chalk — not storable
        }
        return variant == -1 ? 0 : variant;
    }

    private static int variantOf(BlockState state) {
        if (state.is(com.emoniph.witchery.init.WitcheryBlocks.CIRCLE_GLYPH.get()))      return 1;
        if (state.is(com.emoniph.witchery.init.WitcheryBlocks.CIRCLE_OTHERWHERE.get())) return 2;
        if (state.is(com.emoniph.witchery.init.WitcheryBlocks.CIRCLE_INFERNAL.get()))   return 3;
        if (state.is(com.emoniph.witchery.init.WitcheryBlocks.CIRCLE.get()))            return 4;
        return 0;
    }
}
