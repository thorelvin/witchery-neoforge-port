package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryPlacementModifiers;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.stream.Stream;

/** Applies the source Spirit-only chunk roll and randomized flower-patch seed. */
public final class SpiritWorldPlacement extends PlacementModifier {
    public static final SpiritWorldPlacement INSTANCE = new SpiritWorldPlacement();
    public static final MapCodec<SpiritWorldPlacement> CODEC = MapCodec.unit(() -> INSTANCE);

    private SpiritWorldPlacement() {
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        if (!context.getLevel().getLevel().dimension().equals(WitcheryDimensions.SPIRIT_WORLD)
                || random.nextInt(4) == 0) {
            return Stream.empty();
        }

        BlockPos column = pos.offset(random.nextInt(16), 0, random.nextInt(16));
        int minY = sourceMinY(context.getMinGenY());
        int surface = context.getHeight(Heightmap.Types.WORLD_SURFACE_WG, column.getX(), column.getZ());
        int maxExclusive = sourceMaxYExclusive(context.getMinGenY(), context.getGenDepth(), surface);
        if (maxExclusive <= minY) {
            return Stream.empty();
        }
        return Stream.of(column.atY(minY + random.nextInt(maxExclusive - minY)));
    }

    static int sourceMinY(int minGenY) {
        return Math.max(0, minGenY);
    }

    static int sourceMaxYExclusive(int minGenY, int genDepth, int surface) {
        return Math.min(minGenY + genDepth, surface + 32);
    }

    @Override
    public PlacementModifierType<?> type() {
        return WitcheryPlacementModifiers.SPIRIT_WORLD.get();
    }
}
