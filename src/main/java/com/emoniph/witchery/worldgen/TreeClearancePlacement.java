package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryPlacementModifiers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

public class TreeClearancePlacement extends PlacementFilter {
    public static final MapCodec<TreeClearancePlacement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 16).fieldOf("horizontal_radius").forGetter(TreeClearancePlacement::horizontalRadius),
            Codec.intRange(0, 32).fieldOf("min_y").forGetter(TreeClearancePlacement::minY),
            Codec.intRange(0, 32).fieldOf("max_y").forGetter(TreeClearancePlacement::maxY)
    ).apply(instance, TreeClearancePlacement::new));

    private final int horizontalRadius;
    private final int minY;
    private final int maxY;

    public TreeClearancePlacement(int horizontalRadius, int minY, int maxY) {
        this.horizontalRadius = horizontalRadius;
        this.minY = minY;
        this.maxY = maxY;
    }

    public int horizontalRadius() {
        return horizontalRadius;
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        for (int y = minY; y <= maxY; y++) {
            for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
                for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                    BlockState state = context.getBlockState(pos.offset(x, y, z));
                    if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public PlacementModifierType<?> type() {
        return WitcheryPlacementModifiers.TREE_CLEARANCE.get();
    }
}
