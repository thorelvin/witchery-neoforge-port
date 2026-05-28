package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class SpanishMossVinesFeature extends Feature<NoneFeatureConfiguration> {
    private static final Direction[] ATTACHMENT_DIRECTIONS = {
            Direction.UP,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
    };

    public SpanishMossVinesFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        if (!level.isEmptyBlock(pos)) {
            return false;
        }

        int start = context.random().nextInt(ATTACHMENT_DIRECTIONS.length);
        for (int i = 0; i < ATTACHMENT_DIRECTIONS.length; i++) {
            Direction direction = ATTACHMENT_DIRECTIONS[(start + i) % ATTACHMENT_DIRECTIONS.length];
            if (VineBlock.isAcceptableNeighbour(level, pos.relative(direction), direction)) {
                level.setBlock(pos, WitcheryBlocks.SPANISH_MOSS.get().defaultBlockState()
                        .setValue(VineBlock.getPropertyForFace(direction), Boolean.TRUE), 2);
                return true;
            }
        }

        return false;
    }
}
