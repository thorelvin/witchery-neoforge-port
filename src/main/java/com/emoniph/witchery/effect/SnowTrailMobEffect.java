package com.emoniph.witchery.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Source 1.7.10 Snow Trail: affected entities leave snow layers in not-too-hot biomes.
 */
public class SnowTrailMobEffect extends MobEffect {
    public static final int TICK_INTERVAL = 10;
    public static final float MAX_TRAIL_TEMPERATURE = 1.6F;
    public static final int SNOW_COVERING_SEARCH_LIMIT = 8;
    public static final int SNOW_GOLEM_EXPLOSION_ROLL = 20;
    public static final float SNOW_GOLEM_EXPLOSION_POWER = 3.0F;
    public static final int SNOW_GOLEM_COVER_RADIUS = 8;

    public SnowTrailMobEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % TICK_INTERVAL == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        for (int l = 0; l < 4; l++) {
            BlockPos pos = footprintPos(entity.getX(), entity.getY(), entity.getZ(), l);
            if (sourceFootprintAllowsSnow(level.isEmptyBlock(pos),
                    sourceBiomeTemperature(level, pos),
                    canPlaceSnow(level, pos))) {
                level.setBlockAndUpdate(pos, Blocks.SNOW.defaultBlockState());
            }
        }

        if (entity instanceof SnowGolem && level.random.nextInt(SNOW_GOLEM_EXPLOSION_ROLL) == 0) {
            level.explode(entity, entity.getX(), entity.getY(), entity.getZ(),
                    SNOW_GOLEM_EXPLOSION_POWER, Level.ExplosionInteraction.MOB);
            createSnowCovering(level, entity.blockPosition(), SNOW_GOLEM_COVER_RADIUS);
            entity.discard();
        }

        return true;
    }

    public static BlockPos footprintPos(double x, double y, double z, int index) {
        double offsetX = footprintOffsetX(index);
        double offsetZ = footprintOffsetZ(index);
        return new BlockPos(Mth.floor(x + offsetX), Mth.floor(y), Mth.floor(z + offsetZ));
    }

    public static double footprintOffsetX(int index) {
        return (float)(index % 2 * 2 - 1) * 0.25F;
    }

    public static double footprintOffsetZ(int index) {
        return (float)(index / 2 % 2 * 2 - 1) * 0.25F;
    }

    public static boolean allowsSnowTrail(float biomeTemperature) {
        return biomeTemperature < MAX_TRAIL_TEMPERATURE;
    }

    public static float sourceHeightAdjustedTemperature(float biomeTemperature, int y, float sourceTemperatureNoise) {
        if (y > 64) {
            return biomeTemperature - (sourceTemperatureNoise * 4.0F + (float) y - 64.0F) * 0.05F / 30.0F;
        }
        return biomeTemperature;
    }

    public static boolean sourceFootprintAllowsSnow(boolean targetAir, float biomeTemperature, boolean snowCanSurvive) {
        return targetAir && allowsSnowTrail(biomeTemperature) && snowCanSurvive;
    }

    public static boolean sourceSnowCoveringSupportAllowsSnow(boolean opaqueCube, boolean iceMaterial) {
        return opaqueCube || iceMaterial;
    }

    public static void createSnowCovering(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos pos : sourceFilledCircle(center, radius)) {
            createSnowAtColumn(level, pos);
        }
    }

    public static List<BlockPos> sourceFilledCircle(BlockPos center, int radius) {
        List<BlockPos> positions = new ArrayList<>();
        if (radius == 1) {
            positions.add(center);
            return positions;
        }

        int x = radius - 1;
        int z = 0;
        int radiusError = 1 - x;
        while (x >= z) {
            addLine(positions, center, -x, x, z);
            addLine(positions, center, -z, z, x);
            addLine(positions, center, -x, x, -z);
            addLine(positions, center, -z, z, -x);
            z++;
            if (radiusError < 0) {
                radiusError += 2 * z + 1;
            } else {
                x--;
                radiusError += 2 * (z - x + 1);
            }
        }
        return positions;
    }

    private static void addLine(List<BlockPos> positions, BlockPos center, int minX, int maxX, int offsetZ) {
        for (int x = minX; x <= maxX; x++) {
            positions.add(center.offset(x, 0, offsetZ));
        }
    }

    private static float sourceBiomeTemperature(ServerLevel level, BlockPos pos) {
        Biome biome = level.getBiome(pos).value();
        Biome.ClimateSettings climate = biome.getModifiedClimateSettings();
        float modifiedTemperature = climate.temperatureModifier().modifyTemperature(pos, climate.temperature());
        return sourceHeightAdjustedTemperature(modifiedTemperature, pos.getY(), 0.0F);
    }

    private static void createSnowAtColumn(ServerLevel level, BlockPos origin) {
        if (level.isEmptyBlock(origin)) {
            for (int i = 1; i < SNOW_COVERING_SEARCH_LIMIT; i++) {
                BlockPos below = origin.below(i);
                BlockState support = level.getBlockState(below);
                if (!support.isAir()) {
                    setBlockToSnow(level, below.above(), support);
                    break;
                }
            }
        } else {
            for (int i = 1; i < SNOW_COVERING_SEARCH_LIMIT; i++) {
                BlockPos above = origin.above(i);
                if (level.isEmptyBlock(above)) {
                    setBlockToSnow(level, above, level.getBlockState(above.below()));
                    break;
                }
            }
        }
    }

    private static boolean canPlaceSnow(ServerLevel level, BlockPos pos) {
        return Blocks.SNOW.defaultBlockState().canSurvive(level, pos);
    }

    private static void setBlockToSnow(ServerLevel level, BlockPos pos, BlockState support) {
        if (sourceSnowCoveringSupportAllowsSnow(support.isSolidRender(), support.is(BlockTags.ICE))
                && canPlaceSnow(level, pos)) {
            level.setBlockAndUpdate(pos, Blocks.SNOW.defaultBlockState());
        }
    }
}
