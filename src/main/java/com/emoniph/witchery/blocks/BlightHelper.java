package com.emoniph.witchery.blocks;

import com.emoniph.witchery.init.WitcheryBlockTags;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class BlightHelper {
    public static final int HEART_RADIUS = 16;

    private static final int HEART_RADIUS_SQ = HEART_RADIUS * HEART_RADIUS;
    private static final int LOCAL_SPREAD = 3;

    private BlightHelper() {}

    public static boolean isBlight(BlockState state) {
        return state.is(WitcheryBlocks.BLIGHT_HEART.get())
                || state.is(WitcheryBlocks.BLIGHT_FIBRE.get())
                || state.is(WitcheryBlocks.BLIGHTED_SOIL.get())
                || state.is(WitcheryBlocks.BLIGHTED_STONE.get())
                || state.is(WitcheryBlocks.BLIGHTED_LOG.get());
    }

    public static boolean isNearHeart(LevelReader level, BlockPos pos) {
        for (BlockPos scan : BlockPos.betweenClosed(pos.offset(-HEART_RADIUS, -4, -HEART_RADIUS),
                pos.offset(HEART_RADIUS, 4, HEART_RADIUS))) {
            if (scan.distSqr(pos) <= HEART_RADIUS_SQ
                    && level.getBlockState(scan).is(WitcheryBlocks.BLIGHT_HEART.get())) {
                return true;
            }
        }
        return false;
    }

    public static boolean canBlightSurvive(LevelReader level, BlockPos pos) {
        return hasAdjacentSolid(level, pos) && !isProtected(level, pos);
    }

    public static boolean hasAdjacentSolid(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighbor = level.getBlockState(neighborPos);
            if (neighbor.isFaceSturdy(level, neighborPos, direction.getOpposite()) || isBlight(neighbor)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isProtected(LevelReader level, BlockPos pos) {
        if (isBarrier(level.getBlockState(pos)) || isBarrier(level.getBlockState(pos.below()))) {
            return true;
        }

        for (Direction direction : Direction.values()) {
            if (isBarrier(level.getBlockState(pos.relative(direction)))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBarrier(BlockState state) {
        return state.is(WitcheryBlockTags.BLIGHT_BARRIERS);
    }

    public static void spreadFromHeart(ServerLevel level, BlockPos heartPos, RandomSource random) {
        for (int i = 0; i < 4; i++) {
            BlockPos seed = heartPos.offset(
                    random.nextInt(HEART_RADIUS * 2 + 1) - HEART_RADIUS,
                    random.nextInt(7) - 3,
                    random.nextInt(HEART_RADIUS * 2 + 1) - HEART_RADIUS);
            if (seed.distSqr(heartPos) <= HEART_RADIUS_SQ) {
                trySpreadNear(level, seed, random, true);
            }
        }
    }

    public static void trySpreadNear(ServerLevel level, BlockPos origin, RandomSource random, boolean ignoreChance) {
        if (!ignoreChance && random.nextInt(4) != 0) {
            return;
        }
        if (!isNearHeart(level, origin)) {
            return;
        }

        BlockPos target = origin.offset(
                random.nextInt(LOCAL_SPREAD * 2 + 1) - LOCAL_SPREAD,
                random.nextInt(3) - 1,
                random.nextInt(LOCAL_SPREAD * 2 + 1) - LOCAL_SPREAD);
        if (!target.equals(origin) && hasAdjacentBlight(level, target)) {
            convertTarget(level, target, random);
        }
    }

    public static boolean convertTarget(ServerLevel level, BlockPos pos, RandomSource random) {
        return convertTarget(level, pos, random, true);
    }

    public static boolean seedBlight(ServerLevel level, BlockPos pos, RandomSource random) {
        return convertTarget(level, pos, random, false);
    }

    private static boolean convertTarget(ServerLevel level, BlockPos pos, RandomSource random, boolean requireAdjacentBlight) {
        if (isProtected(level, pos)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (isBlight(state) || state.hasBlockEntity() || state.liquid()) {
            return false;
        }

        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F || hardness > 10.0F) {
            return false;
        }

        if (canBecomeFibre(level, pos, state)) {
            level.setBlock(pos, WitcheryBlocks.BLIGHT_FIBRE.get().defaultBlockState(), Block.UPDATE_ALL);
            blightFeedback(level, pos, 4);
            return true;
        }

        if (requireAdjacentBlight && !hasAdjacentBlight(level, pos)) {
            return false;
        }

        if (state.is(BlockTags.LOGS)) {
            BlockState blightedLog = WitcheryBlocks.BLIGHTED_LOG.get().defaultBlockState();
            if (state.hasProperty(RotatedPillarBlock.AXIS)) {
                blightedLog = blightedLog.setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
            }
            level.setBlock(pos, blightedLog, Block.UPDATE_ALL);
            blightFeedback(level, pos, 6);
            return true;
        }

        if (isSoil(state)) {
            level.setBlock(pos, WitcheryBlocks.BLIGHTED_SOIL.get().defaultBlockState(), Block.UPDATE_ALL);
            blightFeedback(level, pos, 6);
            return true;
        }

        if (isStone(state)) {
            level.setBlock(pos, WitcheryBlocks.BLIGHTED_STONE.get().defaultBlockState(), Block.UPDATE_ALL);
            blightFeedback(level, pos, 6);
            return true;
        }

        return false;
    }

    public static int cleanseArea(ServerLevel level, BlockPos center, int radius) {
        int radiusSq = radius * radius;
        int cleansed = 0;
        for (BlockPos scan : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (scan.distSqr(center) > radiusSq) {
                continue;
            }
            BlockPos pos = scan.immutable();
            BlockState state = level.getBlockState(pos);
            if (isBlight(state)) {
                cleanseBlock(level, pos, state);
                cleansed++;
            }
        }
        if (cleansed > 0) {
            level.playSound(null, center, SoundEvents.GENERIC_EXTINGUISH_FIRE,
                    SoundSource.BLOCKS, 0.8F, 1.3F);
            level.sendParticles(ParticleTypes.POOF,
                    center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D,
                    12 + cleansed, radius * 0.35D, radius * 0.2D, radius * 0.35D, 0.02D);
        }
        return cleansed;
    }

    public static void cleanseBlock(Level level, BlockPos pos, BlockState state) {
        if (state.is(WitcheryBlocks.BLIGHTED_SOIL.get())) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(WitcheryBlocks.BLIGHTED_STONE.get())) {
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(WitcheryBlocks.BLIGHTED_LOG.get())) {
            BlockState log = Blocks.OAK_LOG.defaultBlockState();
            if (state.hasProperty(RotatedPillarBlock.AXIS)) {
                log = log.setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
            }
            level.setBlock(pos, log, Block.UPDATE_ALL);
        } else {
            level.removeBlock(pos, false);
        }
    }

    public static void decayIfOrphaned(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (isNearHeart(level, pos) || random.nextInt(3) != 0) {
            return;
        }
        cleanseBlock(level, pos, state);
    }

    static void blightFeedback(ServerLevel level, BlockPos pos, int particles) {
        level.playSound(null, pos, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS,
                0.2F, 0.7F + level.random.nextFloat() * 0.3F);
        level.sendParticles(ParticleTypes.WITCH,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                particles, 0.25D, 0.25D, 0.25D, 0.01D);
    }

    private static boolean hasAdjacentBlight(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (isBlight(level.getBlockState(pos.relative(direction)))) {
                return true;
            }
        }
        return false;
    }

    private static boolean canBecomeFibre(LevelReader level, BlockPos pos, BlockState state) {
        return (state.isAir()
                || state.canBeReplaced()
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.LEAVES))
                && canBlightSurvive(level, pos);
    }

    private static boolean isSoil(BlockState state) {
        return state.is(Blocks.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.FARMLAND)
                || state.is(Blocks.MUD)
                || state.is(Blocks.CLAY);
    }

    private static boolean isStone(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE);
    }
}
