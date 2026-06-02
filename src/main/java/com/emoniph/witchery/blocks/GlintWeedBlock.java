package com.emoniph.witchery.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Glint Weed is a bright magical weed that spreads over legacy Witchery terrain.
 */
public class GlintWeedBlock extends BushBlock {

    private static final VoxelShape SHAPE = Block.box(0.8, 0.0, 0.8, 15.2, 16.0, 15.2);

    public GlintWeedBlock(BlockBehaviour.Properties properties) {
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
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isFaceSturdy(level, pos, Direction.UP);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) != 0 || tooDense(level, pos)) {
            return;
        }

        BlockPos candidate = pos;
        for (int i = 0; i < 4; i++) {
            BlockPos next = candidate.offset(random.nextInt(3) - 1, random.nextInt(2) - random.nextInt(2), random.nextInt(3) - 1);
            if (canSpreadTo(level, next)) {
                candidate = next;
            }
        }

        if (canSpreadTo(level, candidate)) {
            level.setBlock(candidate, defaultBlockState(), 3);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.4 + random.nextInt(3) * 0.1;
        double y = pos.getY() + 0.4 + random.nextInt(3) * 0.1;
        double z = pos.getZ() + 0.4 + random.nextInt(3) * 0.1;
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
    }

    private boolean tooDense(ServerLevel level, BlockPos pos) {
        int remaining = 5;
        for (BlockPos scan : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 1, 4))) {
            if (level.getBlockState(scan).is(this) && --remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private boolean canSpreadTo(LevelReader level, BlockPos pos) {
        return level.isEmptyBlock(pos)
                && hasSolidVerticalSupport(level, pos)
                && canSpreadOver(level.getBlockState(pos.below()))
                && canSurvive(defaultBlockState(), level, pos);
    }

    private boolean hasSolidVerticalSupport(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                || level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN);
    }

    private boolean canSpreadOver(BlockState state) {
        return state.is(Blocks.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.SAND)
                || state.is(Blocks.FARMLAND);
    }
}
