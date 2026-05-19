package com.emoniph.witchery.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A tiny floating spirit-light that mostly exists for ambience and altar power.
 */
public class GlowGlobeBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(6.4, 6.4, 6.4, 9.6, 9.6, 9.6);

    public GlowGlobeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            return;
        }

        double x = pos.getX() + 0.45 + random.nextInt(3) * 0.05;
        double y = pos.getY() + 0.4 + random.nextInt(4) * 0.1;
        double z = pos.getZ() + 0.45 + random.nextInt(3) * 0.05;
        level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
    }
}
