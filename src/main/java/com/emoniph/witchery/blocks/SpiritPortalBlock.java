package com.emoniph.witchery.blocks;

import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.spiritworld.SpiritWorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Old Witchery formed a 2x2 Spirit Portal inside a 4x4 snow-block frame.
 * Full manifestation travel is still a separate player-state feature, but the
 * portal structure itself can already behave like the original block again.
 */
public class SpiritPortalBlock extends Block {

    private static final VoxelShape X_CONNECTED_SHAPE = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
    private static final VoxelShape Z_CONNECTED_SHAPE = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);

    public SpiritPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return hasEastWestPortalAxis(level, pos) ? X_CONNECTED_SHAPE : Z_CONNECTED_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) {
        return adjacentState.is(this) || super.skipRendering(state, adjacentState, side);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos currentPos,
                                     Direction facing, BlockPos facingPos, BlockState facingState, RandomSource random) {
        if (!isValidPortal(level, currentPos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, currentPos, facing, facingPos, facingState, random);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier effectApplier, boolean isMoved) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }

        if (!level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) {
            return;
        }

        if (SpiritWorldHelper.canManifest(player)) {
            SpiritWorldHelper.manifestPlayerInOverworldAsGhost(player);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            boolean positive = random.nextBoolean();
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            double dx = (random.nextDouble() - 0.5) * 0.5;
            double dy = (random.nextDouble() - 0.5) * 0.5;
            double dz = (random.nextDouble() - 0.5) * 0.5;

            if (hasEastWestPortalAxis(level, pos)) {
                z = pos.getZ() + 0.5 + 0.25 * (positive ? 1.0 : -1.0);
                dz = random.nextFloat() * 2.0F * (positive ? 1.0F : -1.0F);
            } else {
                x = pos.getX() + 0.5 + 0.25 * (positive ? 1.0 : -1.0);
                dx = random.nextFloat() * 2.0F * (positive ? 1.0F : -1.0F);
            }

            level.addParticle(ParticleTypes.DRIPPING_WATER, x, y, z, dx, dy, dz);
        }
    }

    public boolean tryToCreatePortal(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return false;
        }

        int stepX = 0;
        int stepZ = 0;
        if (level.getBlockState(pos.west()).is(Blocks.SNOW_BLOCK) || level.getBlockState(pos.east()).is(Blocks.SNOW_BLOCK)) {
            stepX = 1;
        }
        if (level.getBlockState(pos.north()).is(Blocks.SNOW_BLOCK) || level.getBlockState(pos.south()).is(Blocks.SNOW_BLOCK)) {
            stepZ = 1;
        }
        if (stepX == stepZ) {
            return false;
        }

        BlockPos origin = pos;
        BlockPos shifted = pos.offset(-stepX, 0, -stepZ);
        if (level.getBlockState(shifted).isAir()) {
            origin = shifted;
        }

        if (!matchesPortalFrame(level, origin, stepX, stepZ, false)) {
            return false;
        }

        for (int width = 0; width < 2; width++) {
            for (int height = 0; height < 2; height++) {
                level.setBlock(origin.offset(stepX * width, height, stepZ * width), defaultBlockState(), 3);
            }
        }

        return true;
    }

    private boolean isValidPortal(LevelReader level, BlockPos pos) {
        return isValidPortalAxis(level, pos, 1, 0) || isValidPortalAxis(level, pos, 0, 1);
    }

    private boolean isValidPortalAxis(LevelReader level, BlockPos pos, int stepX, int stepZ) {
        for (int widthOffset = -1; widthOffset <= 0; widthOffset++) {
            for (int heightOffset = -1; heightOffset <= 0; heightOffset++) {
                BlockPos origin = pos.offset(stepX * widthOffset, heightOffset, stepZ * widthOffset);
                if (matchesPortalFrame(level, origin, stepX, stepZ, true)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesPortalFrame(LevelReader level, BlockPos origin, int stepX, int stepZ, boolean allowPortalInterior) {
        for (int width = -1; width <= 2; width++) {
            for (int height = -1; height <= 2; height++) {
                boolean frame = width == -1 || width == 2 || height == -1 || height == 2;
                BlockPos checkPos = origin.offset(stepX * width, height, stepZ * width);
                BlockState checkState = level.getBlockState(checkPos);

                if (frame) {
                    if (!checkState.is(Blocks.SNOW_BLOCK)) {
                        return false;
                    }
                } else if (!checkState.isAir()
                        && !checkState.is(WitcheryBlocks.FLOWING_SPIRIT.get())
                        && !(allowPortalInterior && checkState.is(this))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean hasEastWestPortalAxis(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos.west()).is(this)
                || level.getBlockState(pos.east()).is(this);
    }
}
