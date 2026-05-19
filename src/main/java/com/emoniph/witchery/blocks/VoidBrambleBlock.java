package com.emoniph.witchery.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Void bramble teleports living entities to a distant safe point in the same
 * dimension, echoing the original block's punishment effect.
 */
public class VoidBrambleBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0.8, 0.0, 0.8, 15.2, 16.0, 15.2);
    private static final int TELEPORT_DISTANCE = 500;

    public VoidBrambleBlock(BlockBehaviour.Properties properties) {
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
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !level.getBlockState(pos.below()).isAir();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier _effectApplier, boolean _isMoved) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) {
            return;
        }

        for (int i = 0; i < 16; i++) {
            int targetX = pos.getX() + level.random.nextInt(TELEPORT_DISTANCE * 2) - TELEPORT_DISTANCE;
            int targetZ = pos.getZ() + level.random.nextInt(TELEPORT_DISTANCE * 2) - TELEPORT_DISTANCE;
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(targetX, pos.getY(), targetZ));
            double x = targetX + 0.5;
            double y = top.getY() + 1.0;
            double z = targetZ + 0.5;
            if (living.randomTeleport(x, y, z, true)) {
                return;
            }
        }
    }
}
