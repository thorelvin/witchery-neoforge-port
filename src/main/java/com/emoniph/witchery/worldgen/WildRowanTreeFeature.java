package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.BelladonnaBlock;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Optional;

public class WildRowanTreeFeature extends Feature<NoneFeatureConfiguration> {
    private static final ResourceKey<ConfiguredFeature<?, ?>> ROWAN_TREE_KEY =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "rowan_tree"));

    public WildRowanTreeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        Optional<? extends net.minecraft.core.Holder.Reference<ConfiguredFeature<?, ?>>> rowanTree =
                context.level().registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(ROWAN_TREE_KEY);
        if (rowanTree.isEmpty()) {
            return false;
        }

        BlockPos origin = context.origin();
        boolean placed = rowanTree.get().value().place(context.level(), context.chunkGenerator(), context.random(), origin);
        if (placed) {
            maybePlaceBelladonna(context.level(), context.random(), origin);
        }
        return placed;
    }

    private void maybePlaceBelladonna(WorldGenLevel level, RandomSource random, BlockPos trunkBase) {
        if (random.nextInt(3) != 0) {
            return;
        }

        BlockState flower = WitcheryBlocks.BELLADONNA.get().defaultBlockState()
                .setValue(BelladonnaBlock.BELLADONNA_AGE, BelladonnaBlock.BELLADONNA_AGE.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(4));
        int desiredFlowers = 1 + random.nextInt(2);
        int placedFlowers = 0;

        for (int attempts = 0; attempts < 12 && placedFlowers < desiredFlowers; attempts++) {
            int dx = Mth.nextInt(random, -2, 2);
            int dz = Mth.nextInt(random, -2, 2);
            if (dx == 0 && dz == 0) {
                continue;
            }
            if (Math.abs(dx) + Math.abs(dz) > 3) {
                continue;
            }

            BlockPos flowerPos = trunkBase.offset(dx, 0, dz);
            if (!level.isEmptyBlock(flowerPos) || !flower.canSurvive(level, flowerPos)) {
                continue;
            }

            level.setBlock(flowerPos, flower, 2);
            placedFlowers++;
        }
    }
}
