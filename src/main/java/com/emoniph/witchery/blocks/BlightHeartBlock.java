package com.emoniph.witchery.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlightHeartBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public BlightHeartBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPE;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlightHelper.spreadFromHeart(level, pos, random);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier effectApplier, boolean isMoved) {
        if (!level.isClientSide() && entity instanceof LivingEntity living
                && !(living instanceof Player player && player.getAbilities().instabuild)
                && level.random.nextInt(80) == 0) {
            living.hurt(level.damageSources().magic(), 1.0F);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (isUndersideExposed(level, pos) && random.nextInt(3) == 0) {
            ParticleUtils.spawnParticleBelow(level, pos, random, ParticleTypes.DRIPPING_OBSIDIAN_TEAR);
        }

        if (random.nextInt(6) != 0) {
            return;
        }
        level.addParticle(ParticleTypes.WITCH,
                pos.getX() + 0.2D + random.nextDouble() * 0.6D,
                pos.getY() + 0.5D + random.nextDouble() * 0.5D,
                pos.getZ() + 0.2D + random.nextDouble() * 0.6D,
                0.0D, 0.02D, 0.0D);
    }

    private static boolean isUndersideExposed(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        return !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
}
